package app.honguyen.forge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.honguyen.forge.camera.CameraPreviewScreen
import app.honguyen.forge.designsystem.theme.ForgeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the system splash up until the first Compose frame is laid out, so the
        // handoff is seamless rather than flashing an empty window. Both lambdas run on
        // the main thread, so the captured flag needs no synchronization.
        var contentReady = false
        splashScreen.setKeepOnScreenCondition { !contentReady }

        setContent {
            ForgeTheme {
                LaunchedEffect(Unit) { contentReady = true }
                CameraPreviewScreen()
            }
        }
    }
}
