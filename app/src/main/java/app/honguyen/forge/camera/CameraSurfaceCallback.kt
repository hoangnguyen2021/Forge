package app.honguyen.forge.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.SurfaceHolder
import app.honguyen.forge.engine.ForgeEngine
import java.util.concurrent.CountDownLatch

// Sensor orientation is reported in degrees. If it is 90° or 270°, the sensor is mounted
// sideways relative to the device's natural orientation, so width and height are swapped.
private const val SENSOR_ORIENTATION_HALF_TURN = 180
private const val SENSOR_ORIENTATION_QUARTER_TURN = 90

/*
 * Handles the SurfaceView lifecycle and wires the camera preview into the OpenGL pipeline.
 *
 * The three-phase surface lifecycle drives everything:
 *   surfaceCreated   → allocate the OES texture and SurfaceTexture on the GL thread
 *   surfaceChanged   → pick the best camera resolution and open the camera
 *   surfaceDestroyed → tear down camera, texture, and the native EGL surface
 *
 * Frame delivery path once the camera is open:
 *   Camera hardware → SurfaceTexture (OES texture updated on each frame)
 *                   → onFrameAvailable fires on the GL thread
 *                   → updateTexImage latches the latest frame into the OES texture
 *                   → nativeDrawFrame samples the texture via OpenGL and composites it
 *
 * All EGL/GL calls run on a dedicated GL thread so frame rendering never touches the
 * main thread. surfaceCreated starts the thread; surfaceDestroyed tears it down.
 */
internal class CameraSurfaceCallback(
    private val context: Context,
) : SurfaceHolder.Callback {

    private var glThread: HandlerThread? = null
    private var glHandler: Handler? = null

    // Only ever accessed on the GL thread
    private var cameraSession: Camera2Session? = null
    private var surfaceTexture: SurfaceTexture? = null

    /*
     * Written on the main thread in surfaceDestroyed, read on the GL thread in
     * onFrameAvailable — @Volatile ensures the GL thread sees the update immediately
     * without needing a lock.
     */
    @Volatile private var surfaceReleased = false

    /*
     * surfaceCreated: the Surface now exists but has no known size yet.
     *
     * OES (GL_TEXTURE_EXTERNAL_OES) is a special texture type that accepts camera frames
     * directly — no pixel copy needed. The SurfaceTexture wraps it, giving the camera a
     * Surface to write frames into and triggering onFrameAvailable when each frame lands.
     *
     * A fresh GL thread is started here and torn down in surfaceDestroyed, matching the
     * Surface's own lifetime. The EGL context is created on this thread and must stay
     * there — all subsequent GL calls are posted to glHandler.
     */
    @SuppressLint("Recycle") // SurfaceTexture released in surfaceDestroyed via glHandler
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReleased = false
        val thread = HandlerThread(GL_THREAD_NAME).also { it.start() }
        val handler = Handler(thread.looper)
        glThread = thread
        glHandler = handler

        glHandler?.post {
            ForgeEngine.nativeSurfaceCreated(holder.surface)

            // allocate OES texture on the GL thread — texId is only valid on this thread
            val texId = ForgeEngine.nativeCreateOesTexture()
            if (texId < 0) return@post

            // allocate SurfaceTexture backed by the OES texture
            val st = SurfaceTexture(texId).also { surfaceTexture = it }

            // dispatch onFrameAvailable to the GL thread so rendering never hits the main thread
            st.setOnFrameAvailableListener({ tex ->
                // guard against a queued frame firing after surfaceDestroyed releases the texture
                if (surfaceReleased) return@setOnFrameAvailableListener

                // latches the latest frame into the OES texture; skipped frames are dropped
                tex.updateTexImage()

                // 4x4 matrix correcting for sensor orientation and HAL crop — passed to the shader
                val texMatrix = FloatArray(16)
                tex.getTransformMatrix(texMatrix)

                // draws the OES texture onto the EGL surface using the transform matrix
                ForgeEngine.nativeDrawFrame(texMatrix)
            }, handler)
        }
    }

    /*
     * surfaceChanged: called immediately after surfaceCreated (with initial dimensions)
     * and again whenever the surface is resized or its format changes.
     *
     * selectPreviewSize is a CameraManager query with no GL dependency — it runs on the
     * main thread so the resolved dimensions are captured before posting to the GL thread.
     * The GL post waits behind surfaceCreated's post, so surfaceTexture is guaranteed
     * to be non-null by the time it runs.
     */
    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        // pick the camera output size whose aspect ratio best matches the surface
        val (size, sensorOrientation) = Camera2Session.selectPreviewSize(
            context = context,
            targetWidth = width,
            targetHeight = height,
        )

        // sensors mounted at 90° or 270° have their width and height swapped relative to the screen;
        // flip them so the viewport receives dimensions in portrait orientation
        val isSideways =
            sensorOrientation % SENSOR_ORIENTATION_HALF_TURN == SENSOR_ORIENTATION_QUARTER_TURN
        val (camPortraitW, camPortraitH) = if (isSideways) {
            size.height to size.width
        } else {
            size.width to size.height
        }

        glHandler?.post {
            val st = surfaceTexture ?: return@post

            // tell SurfaceTexture how large each incoming frame will be so it allocates the right buffer
            st.setDefaultBufferSize(size.width, size.height)

            // pass camera and surface dimensions to the renderer so it can compute the crop scale
            ForgeEngine.nativeSetViewport(
                cameraPortraitW = camPortraitW,
                cameraPortraitH = camPortraitH,
                surfaceW = width,
                surfaceH = height,
            )

            // surfaceChanged can fire multiple times (e.g. on rotation), so close any existing session
            cameraSession?.close()
            cameraSession = Camera2Session(context).also { it.open(Surface(st)) }
        }
    }

    /*
     * surfaceDestroyed: the Surface is about to be torn down (app backgrounded, Activity
     * stopped). Resources must be released in reverse order of acquisition so no component
     * tries to use something that's already gone.
     *
     * A CountDownLatch blocks the main thread until the GL thread finishes cleanup —
     * the Surface becomes invalid as soon as this method returns, so we can't let the
     * GL thread lag behind.
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReleased = true // must be set before the GL post to block any queued onFrameAvailable
        val handler = glHandler
        if (handler != null) {
            val latch = CountDownLatch(1)
            handler.post {
                cameraSession?.close()
                cameraSession = null
                surfaceTexture?.release()
                surfaceTexture = null
                ForgeEngine.nativeSurfaceDestroyed()
                latch.countDown()
            }
            latch.await()
        }
        glThread?.quitSafely()
        glThread = null
        glHandler = null
    }

    companion object {
        private const val GL_THREAD_NAME = "GL_THREAD"
    }
}