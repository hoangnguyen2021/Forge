package app.honguyen.forge.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import app.honguyen.forge.engine.ForgeEngine

// Sensor orientation is reported in degrees. If it is 90° or 270°, the sensor is mounted
// sideways relative to the device's natural orientation, so width and height are swapped.
private const val SENSOR_ORIENTATION_HALF_TURN = 180
private const val SENSOR_ORIENTATION_QUARTER_TURN = 90

/*
 * Handles the SurfaceView lifecycle and wires the camera preview into the OpenGL pipeline.
 *
 * The three-phase surface lifecycle drives everything:
 *   surfaceCreated   → allocate the OES texture and SurfaceTexture
 *   surfaceChanged   → pick the best camera resolution and open the camera
 *   surfaceDestroyed → tear down camera, texture, and the native EGL surface
 *
 * Frame delivery path once the camera is open:
 *   Camera hardware → SurfaceTexture (OES texture updated on each frame)
 *                   → onFrameAvailable fires on the main thread
 *                   → updateTexImage latches the latest frame into the OES texture
 *                   → nativeDrawFrame samples the texture via OpenGL and composites it
 */
internal class CameraSurfaceCallback(
    private val context: Context,
) : SurfaceHolder.Callback {
    private var cameraSession: Camera2Session? = null
    private var surfaceTexture: SurfaceTexture? = null

    // Guards against updateTexImage being called after the SurfaceTexture is released.
    private var surfaceReleased = false

    /*
     * surfaceCreated: the Surface now exists but has no known size yet.
     *
     * OES (GL_TEXTURE_EXTERNAL_OES) is a special texture type that accepts camera frames
     * directly — no pixel copy needed. The SurfaceTexture wraps it, giving the camera a
     * Surface to write frames into and triggering onFrameAvailable when each frame lands.
     */
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReleased = false
        ForgeEngine.nativeSurfaceCreated(holder.surface)

        // allocate OES texture
        val texId = ForgeEngine.nativeCreateOesTexture()
        if (texId < 0) return

        // allocate SurfaceTexture
        val surfaceTexture = SurfaceTexture(texId)
        this.surfaceTexture = surfaceTexture

        // onFrameAvailable is queued as a message on the main Looper
        surfaceTexture.setOnFrameAvailableListener({ tex ->
            // guard against a queued frame firing after surfaceDestroyed releases the texture
            if (surfaceReleased) return@setOnFrameAvailableListener

            // latches and binds the latest frame to the OES texture; skipped frames are dropped
            tex.updateTexImage()

            // 4x4 matrix correcting for sensor orientation and HAL crop — passed to the shader
            val texMatrix = FloatArray(16)
            tex.getTransformMatrix(texMatrix)

            // draws the OES texture onto the EGL surface using the transform matrix
            ForgeEngine.nativeDrawFrame(texMatrix)
        }, Handler(Looper.getMainLooper()))
    }

    /*
     * surfaceChanged: called immediately after surfaceCreated (with initial dimensions)
     * and again whenever the surface is resized or its format changes.
     *
     * selectPreviewSize picks the camera resolution whose aspect ratio best matches the
     * surface, accounting for sensor rotation. setDefaultBufferSize tells the SurfaceTexture
     * how large each incoming frame will be, so it can allocate the right buffer.
     */
    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        val surfaceTexture = this.surfaceTexture ?: return

        // pick the camera output size whose aspect ratio best matches the surface
        val (size, sensorOrientation) = Camera2Session.selectPreviewSize(
            context = context,
            targetWidth = width,
            targetHeight = height,
        )
        // tell SurfaceTexture how large each incoming frame will be so it allocates the right buffer
        surfaceTexture.setDefaultBufferSize(size.width, size.height)

        // sensors mounted at 90° or 270° have their width and height swapped relative to the screen;
        // flip them so the viewport receives dimensions in portrait orientation
        val isSideways =
            sensorOrientation % SENSOR_ORIENTATION_HALF_TURN == SENSOR_ORIENTATION_QUARTER_TURN
        val (camPortraitW, camPortraitH) = if (isSideways) {
            size.height to size.width
        } else {
            size.width to size.height
        }

        // pass camera and surface dimensions to the renderer so it can compute the crop scale
        ForgeEngine.nativeSetViewport(
            cameraPortraitW = camPortraitW,
            cameraPortraitH = camPortraitH,
            surfaceW = width,
            surfaceH = height,
        )

        // surfaceChanged can fire multiple times (e.g. on rotation), so close any existing session
        cameraSession?.close()
        cameraSession = Camera2Session(context).also { it.open(Surface(surfaceTexture)) }
    }

    /*
     * surfaceDestroyed: the Surface is about to be torn down (app backgrounded, Activity
     * stopped). Resources must be released in reverse order of acquisition so no component
     * tries to use something that's already gone.
     */
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        cameraSession?.close()
        surfaceReleased = true // must be set before release() to block any queued callbacks
        surfaceTexture?.release()
        ForgeEngine.nativeSurfaceDestroyed()
    }
}
