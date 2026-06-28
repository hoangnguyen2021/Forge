package app.honguyen.forge.ml

import android.content.Context
import android.util.Log
import kotlin.concurrent.thread

/**
 * Throwaway smoke test for the LiteRT C++ integration (Milestone 2a). Loads the
 * bundled segmentation model and runs one inference on a dummy input — off the main
 * thread — then logs the native result. Remove once the real segmenter lands.
 */
object LiteRtSmokeTest {
    init {
        System.loadLibrary("forge_engine")
    }

    private const val TAG = "LiteRtSmoke"
    private const val MODEL_ASSET = "selfie_multiclass_256x256.tflite"

    /**
     * Read the model from assets and run one inference on a background thread, logging
     * the native result string.
     */
    fun runAndLog(context: Context) {
        val appContext = context.applicationContext
        thread(name = "litert-smoke") {
            val bytes = appContext.assets.open(MODEL_ASSET).use { it.readBytes() }
            Log.i(TAG, nativeRun(bytes))
        }
    }

    private external fun nativeRun(modelBytes: ByteArray): String
}
