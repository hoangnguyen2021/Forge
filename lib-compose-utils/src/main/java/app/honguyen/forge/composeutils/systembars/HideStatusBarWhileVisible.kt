package app.honguyen.forge.composeutils.systembars

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Hides the status bar for as long as the calling composable stays in the composition, restoring
 * both the bar and the window's previous behavior on the way out. The navigation bar is left
 * alone, so gesture affordances survive.
 *
 * While hidden the bar is revealed by a swipe from the top edge and drawn as a transient overlay,
 * which reports no insets — content underneath keeps its size instead of reflowing as the bar
 * comes and goes. Insets for the display cutout are unaffected and still need handling.
 *
 * No-ops outside an activity, which includes previews.
 */
@Composable
fun HideStatusBarWhileVisible() {
    val window = LocalActivity.current?.window ?: return

    DisposableEffect(window) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val previousBehavior = controller.systemBarsBehavior

        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.statusBars())

        onDispose {
            controller.show(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = previousBehavior
        }
    }
}
