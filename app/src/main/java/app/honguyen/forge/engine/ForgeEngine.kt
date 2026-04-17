package app.honguyen.forge.engine

object ForgeEngine {
    init {
        System.loadLibrary("forge_engine")
    }

    external fun nativeVersion(): String
}
