package app.honguyen.forge.engine

import android.view.Surface

object ForgeEngine {

    init {
        System.loadLibrary("forge_engine")
    }

    external fun nativeVersion(): String

    external fun nativeSurfaceCreated(surface: Surface)
    external fun nativeDrawFrame()
    external fun nativeSurfaceDestroyed()
}
