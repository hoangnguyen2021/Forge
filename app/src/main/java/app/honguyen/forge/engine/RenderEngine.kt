package app.honguyen.forge.engine

import android.view.Surface

/**
 * Kotlin wrapper around the native C++ RenderEngine. Each instance owns an opaque
 * handle to a heap-allocated C++ RenderEngine and must be paired with a destroy()
 * call to free it. Methods are not thread-safe — call them on the same thread
 * that created the EGL context (typically the GL thread).
 */
class RenderEngine private constructor(
    private val handle: Long,
) {
    fun surfaceCreated(surface: Surface) = nativeSurfaceCreated(handle, surface)

    fun createOesTexture(): Int = nativeCreateOesTexture(handle)

    fun setViewport(
        cameraPortraitW: Int,
        cameraPortraitH: Int,
        surfaceW: Int,
        surfaceH: Int,
    ) = nativeSetViewport(handle, cameraPortraitW, cameraPortraitH, surfaceW, surfaceH)

    fun drawFrame(texMatrix: FloatArray) = nativeDrawFrame(handle, texMatrix)

    fun surfaceDestroyed() = nativeSurfaceDestroyed(handle)

    fun destroy() = nativeDestroy(handle)

    companion object {
        init {
            System.loadLibrary("forge_engine")
        }

        fun create(): RenderEngine = RenderEngine(nativeCreate())

        fun version(): String = nativeVersion()

        @JvmStatic private external fun nativeVersion(): String

        @JvmStatic private external fun nativeCreate(): Long

        @JvmStatic private external fun nativeDestroy(handle: Long)

        @JvmStatic private external fun nativeSurfaceCreated(
            handle: Long,
            surface: Surface,
        )

        @JvmStatic private external fun nativeCreateOesTexture(handle: Long): Int

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
