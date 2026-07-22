package app.honguyen.forge.designsystem.theme.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.IconDefaultSize
import app.honguyen.forge.designsystem.theme.IconViewportSize
import app.honguyen.forge.designsystem.theme.Icons

val Icons.FlashOn: ImageVector
    get() {
        flashOnCache?.let { return it }
        return ImageVector.Builder(
            name = "FlashOn",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // The bolt as one closed run, clockwise from the apex: down the upper stem,
                // out along its ledge, then the long diagonal to the foot. The lower half is
                // that turned about the glyph center, and close() draws the second diagonal.
                moveTo(UPPER_STEM_X, APEX_Y)
                lineTo(UPPER_STEM_X, UPPER_ELBOW_Y)
                lineTo(UPPER_TIP_X, UPPER_ELBOW_Y)
                lineTo(LOWER_STEM_X, FOOT_Y)
                lineTo(LOWER_STEM_X, LOWER_ELBOW_Y)
                lineTo(LOWER_TIP_X, LOWER_ELBOW_Y)
                close()
            }
        }.build().also { flashOnCache = it }
    }

private var flashOnCache: ImageVector? = null

private const val STROKE_WIDTH = 1.75f
private const val HALF_STROKE = STROKE_WIDTH / 2f

// An open outline form, so the bolt is drawn to the 20x20 live area rather than held to the
// 18 square keyline. The apex and the foot are points, so it is the round join standing off
// each of them that reaches the live area and the centerline that stops a half stroke short.
private const val LIVE_AREA_TOP = 2f
private const val APEX_Y = LIVE_AREA_TOP + HALF_STROKE
private const val FOOT_Y = IconViewportSize - APEX_Y

// The lean: how far the upper stem stands right of the lower one, and with it how far the
// bolt travels sideways over its drop. Width is what fixes it — at 4.75 the tips reach 19.125
// and the ink spans 16, flash.svg's own width against the live area's 20 of height.
private const val UPPER_STEM_X = 14.375f
private const val LOWER_STEM_X = IconViewportSize - UPPER_STEM_X

// Each ledge juts a lean past its own stem, which is also a lean past the opposite one — the
// source's proportion, and what keeps a tip, a stem and the far stem evenly spaced.
private const val LEAN = UPPER_STEM_X - LOWER_STEM_X
private const val UPPER_TIP_X = UPPER_STEM_X + LEAN
private const val LOWER_TIP_X = IconViewportSize - UPPER_TIP_X

// Splits the drop between stem and diagonal: at 11 the stems run a little over 8 and the two
// ledges sit 2 apart across the waist, close enough to read as one break in the bolt rather
// than two separate steps.
private const val UPPER_ELBOW_Y = 11f
private const val LOWER_ELBOW_Y = IconViewportSize - UPPER_ELBOW_Y

@Preview(name = "FlashOn", showBackground = true)
@Composable
private fun FlashOnPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
