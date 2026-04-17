package app.honguyen.forge.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import timber.log.Timber

class Camera2Session(
    private val context: Context,
) {
    private val cameraThread = HandlerThread("CameraThread").also { it.start() }
    private val cameraHandler = Handler(cameraThread.looper)

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null

    @SuppressLint("MissingPermission")
    fun open(surface: Surface) {
        val manager = context.getSystemService(CameraManager::class.java)
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager
                .getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: return Timber.e("No back camera found")

        manager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startPreview(camera, surface)
                }

                override fun onDisconnected(camera: CameraDevice) = camera.close()

                override fun onError(
                    camera: CameraDevice,
                    error: Int,
                ) {
                    Timber.e("Camera error: $error")
                    camera.close()
                }
            },
            cameraHandler,
        )
    }

    @Suppress("DEPRECATION")
    private fun startPreview(
        camera: CameraDevice,
        surface: Surface,
    ) {
        camera.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    val request = camera
                        .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        .apply { addTarget(surface) }
                        .build()
                    session.setRepeatingRequest(request, null, cameraHandler)
                    Timber.i("Camera preview started")
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Timber.e("Capture session configuration failed")
                }
            },
            cameraHandler,
        )
    }

    fun close() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        cameraThread.quitSafely()
    }
}
