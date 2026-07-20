package app.honguyen.forge.designsystem.theme.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.honguyen.forge.designsystem.theme.ForgeTheme
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Golden images for the icon set, rendered through the real Compose renderer.
 *
 * These exist because the icons are geometry, not markup: several of them lean on winding
 * direction to punch holes, and CameraSettings additionally leans on a clip path to bite a
 * notch out of the gear. None of that is visible to the compiler or to lint — reverse a
 * sweep and it still builds, still passes detekt, and quietly fills a hole in. A rendered
 * pixel is the only thing that catches it.
 *
 * Record with `./gradlew :lib-design-system:recordPaparazziDebug`, which writes the PNGs
 * under src/test/snapshots for review and commit. Check with `verifyPaparazziDebug`.
 */
@RunWith(Parameterized::class)
class ForgeIconGoldenTest(
    private val iconName: String,
    private val icon: ImageVector,
) {
    // SHRINK crops the snapshot to the content instead of rendering a whole device screen,
    // so a diff is the glyph rather than a few pixels adrift in a phone-sized image.
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun matchesGolden() {
        paparazzi.snapshot(name = iconName) {
            ForgeTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(GOLDEN_PADDING),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(GOLDEN_SIZE),
                    )
                }
            }
        }
    }

    companion object {
        // Rendered far larger than the 24dp the icons ship at: a tooth corner or a hairline
        // gap that drifts is a couple of pixels here and invisible at shipping size.
        private val GOLDEN_SIZE = 96.dp
        private val GOLDEN_PADDING = 8.dp

        // Adding an icon to the set means adding it here, or it ships unguarded.
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun icons(): List<Array<Any>> =
            listOf(
                arrayOf("CameraFlip", ForgeTheme.icons.CameraFlip),
                arrayOf("CameraSettings", ForgeTheme.icons.CameraSettings),
                arrayOf("Debug", ForgeTheme.icons.Debug),
                arrayOf("FramePerson", ForgeTheme.icons.FramePerson),
                arrayOf("PictureMode", ForgeTheme.icons.PictureMode),
                arrayOf("Tune", ForgeTheme.icons.Tune),
                arrayOf("VideoMode", ForgeTheme.icons.VideoMode),
            )
    }
}
