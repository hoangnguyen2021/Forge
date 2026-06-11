package app.honguyen.forge.engine

import android.view.Surface

/*
 * Kotlin wrapper around the native C++ RenderEngine. Each instance owns an opaque
 * handle to a heap-allocated C++ RenderEngine and must be paired with a destroy()
 * call to free it. Methods are not thread-safe — call them on the same thread
 * that created the EGL context (typically the GL thread).
 */
class RenderEngine private constructor(
    private val handle: Long,
) {
    /* Initialize the EGL context on the calling thread, bound to the given Android Surface. */
    fun surfaceCreated(surface: Surface) = nativeSurfaceCreated(handle, surface)

    /* Allocate the OES texture used as the camera output target. Returns 0 on failure. */
    fun createOesTexture(): Int = nativeCreateOesTexture(handle)

    /*
     * Build the render graph that samples the camera texture. Call after
     * createOesTexture(); returns false on failure.
     */
    fun initPipeline(): Boolean = nativeInitPipeline(handle)

    /* Configure camera + surface dimensions; drives the cover-style crop math in the renderer. */
    fun setViewport(
        cameraPortraitW: Int,
        cameraPortraitH: Int,
        surfaceW: Int,
        surfaceH: Int,
    ) = nativeSetViewport(handle, cameraPortraitW, cameraPortraitH, surfaceW, surfaceH)

    /* Render one frame; texMatrix is the 4x4 transform from SurfaceTexture.getTransformMatrix(). */
    fun drawFrame(texMatrix: FloatArray) = nativeDrawFrame(handle, texMatrix)

    /* Tear down EGL + renderer state. Must run on the GL thread before destroy(). */
    fun surfaceDestroyed() = nativeSurfaceDestroyed(handle)

    /* Free the C++ RenderEngine. The wrapper is unusable after this call. */
    fun destroy() = nativeDestroy(handle)

    companion object {
        init {
            System.loadLibrary("forge_engine")
        }

        /* Allocate a new native RenderEngine and return its Kotlin wrapper. */
        fun create(): RenderEngine = RenderEngine(nativeCreate())

        /* Debug version string from the native library; primarily for startup logging. */
        fun version(): String = nativeVersion()

        @JvmStatic private external fun nativeVersion(): String

        @JvmStatic private external fun nativeCreate(): Long

        @JvmStatic private external fun nativeDestroy(handle: Long)

        @JvmStatic private external fun nativeSurfaceCreated(
            handle: Long,
            surface: Surface,
        )

        @JvmStatic private external fun nativeCreateOesTexture(handle: Long): Int

        @JvmStatic private external fun nativeInitPipeline(handle: Long): Boolean

        @JvmStatic private external fun nativeSetViewport(
            handle: Long,
            cameraPortraitW: Int,
            cameraPortraitH: Int,
            surfaceW: Int,
            surfaceH: Int,
        )

        @JvmStatic private external fun nativeDrawFrame(
            handle: Long,
            texMatrix: FloatArray,
        )

        @JvmStatic private external fun nativeSurfaceDestroyed(handle: Long)
    }
}
