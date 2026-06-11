package app.honguyen.forge.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import kotlin.math.abs

/*
 * Manages the Camera2 lifecycle: opening the back camera, starting a repeating preview,
 * and tearing everything down cleanly.
 *
 */
class Camera2Session(
    private val context: Context,
) : CameraSession {
    // Dedicated background thread with a Looper (message queue) for Camera2
    private val cameraThread = HandlerThread(CAMERA_THREAD_NAME).also { it.start() }

    // API < 28: Handler tied to cameraThread's Looper
    private val cameraHandler = Handler(cameraThread.looper)

    // API 28+: Executor wrapping cameraHandler — required by SessionConfiguration API.
    // Posts callbacks onto the same cameraThread as the Handler.
    private val cameraExecutor = Executor { command -> cameraHandler.post(command) }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    private var isSessionClosed = false

    /*
     * Opens the back camera and starts streaming frames into the provided Surface.
     * The Surface is backed by a SurfaceTexture, making each frame available as an OES texture.
     */
    @SuppressLint("MissingPermission")
    override fun open(surface: Surface) {
        val manager = context.getSystemService(CameraManager::class.java)
        val cameraId = findBackCameraId(manager) ?: return Timber.e("No back camera found")

        manager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {

                // when the hardware is ready to use
                override fun onOpened(camera: CameraDevice) {
                    // close() may have run before the camera finished opening — release it now
                    // rather than hold a device that will never be closed.
                    if (isSessionClosed) {
                        camera.close()
                        return
                    }
                    cameraDevice = camera
                    startPreview(camera, surface)
                }

                // when another app takes the camera or the device is unplugged.
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                // when the camera hits a fatal driver, service, or policy error.
                override fun onError(
                    camera: CameraDevice,
                    error: Int,
                ) {
                    Timber.e("Camera error: $error")
                    camera.close()
                    cameraDevice = null
                }
            },
            // callbacks run on cameraHandler's thread
            cameraHandler,
        )
    }

    /*
     * Creates a capture session and starts a repeating preview request.
     * A CameraCaptureSession is the pipeline between the camera hardware and our output Surface.
     * The repeating request tells the camera to keep producing frames continuously until stopped.
     */
    private fun startPreview(
        camera: CameraDevice,
        surface: Surface,
    ) {
        val callback = object : CameraCaptureSession.StateCallback() {

            // when the capture session is ready to accept requests
            override fun onConfigured(session: CameraCaptureSession) {
                // close() may have run while configuration was in flight — discard the session.
                if (isSessionClosed) {
                    session.close()
                    return
                }
                captureSession = session

                // Build a capture request optimized for preview (auto-exposure, auto-focus, etc.)
                // and direct its output to our Surface.
                val request = camera
                    .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    .apply { addTarget(surface) }
                    .build()

                // setRepeatingRequest keeps the camera streaming frames indefinitely.
                // Each new frame is written into the Surface → SurfaceTexture → OES texture.
                session.setRepeatingRequest(request, null, cameraHandler)
                Timber.i("Camera preview started")
            }

            // when the camera service rejects the session configuration (incompatible outputs, etc.)
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Timber.e("Capture session configuration failed")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // API 28+: uses OutputConfiguration and Executor.
            // OutputConfiguration wraps the surface and enables advanced features (multi-camera,
            // physical streams, shared outputs) unavailable in the old API.
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(OutputConfiguration(surface)),
                cameraExecutor,
                callback,
            )
            camera.createCaptureSession(sessionConfig)
        } else {
            // API 24-27: deprecated path, only option below API 28.
            @Suppress("DEPRECATION")
            camera.createCaptureSession(listOf(surface), callback, cameraHandler)
        }
    }

    /*
     * Stops the camera and releases all resources in reverse order of acquisition.
     * Closing the session before the device prevents the driver from receiving requests
     * on a device that is already being torn down.
     */
    override fun close() {
        val latch = CountDownLatch(1)
        cameraHandler.post {
            isSessionClosed = true
            captureSession?.close() // stop the repeating request and release the pipeline
            captureSession = null
            cameraDevice?.close() // release the camera hardware handle
            cameraDevice = null
            latch.countDown()
        }
        latch.await()
        cameraThread.quitSafely() // drain pending messages then stop the thread's Looper
    }

    companion object {
        private const val CAMERA_THREAD_NAME = "CameraThread"
        private const val DEFAULT_PREVIEW_WIDTH = 1920
        private const val DEFAULT_PREVIEW_HEIGHT = 1080
        private const val DEFAULT_SENSOR_ORIENTATION = 90

        private val DEFAULT_PREVIEW_FALLBACK =
            PreviewSize(Size(DEFAULT_PREVIEW_WIDTH, DEFAULT_PREVIEW_HEIGHT), DEFAULT_SENSOR_ORIENTATION)

        /*
         * Returns the best output size and sensor orientation for the back camera.
         * "Best" means the size whose aspect ratio (after accounting for sensor rotation)
         * is closest to the target surface dimensions — minimizing crop or letterbox.
         */
        fun selectPreviewSize(
            context: Context,
            targetWidth: Int,
            targetHeight: Int,
        ): PreviewSize {
            val manager = context.getSystemService(CameraManager::class.java)
            val cameraId = findBackCameraId(manager) ?: return DEFAULT_PREVIEW_FALLBACK

            val characteristics = manager.getCameraCharacteristics(cameraId)

            // sensorOrientation is the degrees the sensor is rotated relative to the device's
            // natural orientation. On most phones this is 90° — the sensor is mounted sideways.
            val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                ?: DEFAULT_SENSOR_ORIENTATION
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return DEFAULT_PREVIEW_FALLBACK

            // Query sizes supported for SurfaceTexture output (the format used for preview).
            val sizes = map.getOutputSizes(android.graphics.SurfaceTexture::class.java)

            val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()

            // If the sensor is mounted sideways the camera's width and height are swapped
            // relative to the screen — account for this before comparing aspect ratios.
            val sideways = PreviewSize.isSensorSideways(sensorOrientation)
            val best = sizes.minByOrNull { size ->
                val (pW, pH) = if (sideways) {
                    size.height.toFloat() to size.width.toFloat()
                } else {
                    size.width.toFloat() to size.height.toFloat()
                }
                abs(pW / pH - targetAspect)
            } ?: sizes.first()

            Timber.i("Selected preview size: ${best.width}x${best.height} sensorOrientation=$sensorOrientation")
            return PreviewSize(best, sensorOrientation)
        }

        /*
         * The back-facing camera's string ID, or null if the device has none.
         */
        private fun findBackCameraId(manager: CameraManager): String? =
            manager.cameraIdList.firstOrNull { id ->
                manager
                    .getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
    }
}
