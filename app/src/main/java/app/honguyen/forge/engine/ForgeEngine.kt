package app.honguyen.forge.engine

import android.view.Surface

object ForgeEngine {

    init {
        System.loadLibrary("forge_engine")
    }

    external fun nativeVersion(): String

    external fun nativeSurfaceCreated(surface: Surface)
    external fun nativeCreateOesTexture(): Int
    external fun nativeSetViewport(cameraPortraitW: Int, cameraPortraitH: Int, surfaceW: Int, surfaceH: Int)
    external fun nativeDrawFrame(texMatrix: FloatArray)
    external fun nativeSurfaceDestroyed()
}
