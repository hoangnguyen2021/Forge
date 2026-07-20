package app.honguyen.forge.composeutils.systembars

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Hides the status bar for as long as the calling composable stays in the composition, restoring
 * it and the window's previous behavior on the way out. The navigation bar is left alone.
 *
 * A swipe from the top edge reveals the bar as a transient overlay, which reports no insets, so
 * content underneath never reflows. Display cutout insets are unaffected and still need handling.
 *
 * No-ops outside an activity, which includes previews.
 */
@Composable
fun HideStatusBar() {
    val window = LocalActivity.current?.window ?: return
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(window, lifecycleOwner) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val previousBehavior = controller.systemBarsBehavior

        // The window is shared with the system, which can restore the bar while this screen is
        // still composed but not resumed. Re-asserting per resume rather than once at first
        // composition covers that; registering replays the current state, so this also serves as
        // the initial hide.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.show(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = previousBehavior
        }
    }
}
