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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.IconDefaultSize
import app.honguyen.forge.designsystem.theme.IconViewportSize
import app.honguyen.forge.designsystem.theme.Icons

val Icons.Debug: ImageVector
    get() {
        debugCache?.let { return it }
        return ImageVector.Builder(
            name = "Debug",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            // bug.svg's 12x12 viewBox doubles onto the 24x24 viewport all but edge to edge, so
            // the trace keeps the source's proportions and this group holds it to the 20x20
            // live area — an open outline glyph, which fills that rather than the square keyline.
            group(
                pivotX = GLYPH_CENTER,
                pivotY = GLYPH_CENTER,
                scaleX = LIVE_AREA_FIT,
                scaleY = LIVE_AREA_FIT,
            ) {
                path(fill = SolidColor(Color.Black)) {
                    // The shell splits into a left and a right half sharing the seam down the
                    // middle; non-zero fill unions them back into one body.
                    moveTo(23f, 13.75f)
                    curveToRelative(0.41f, 0f, 0.75f, -0.34f, 0.75f, -0.75f)
                    reflectiveCurveToRelative(-0.34f, -0.75f, -0.75f, -0.75f)
                    horizontalLineToRelative(-4.25f)
                    verticalLineToRelative(-2.5f)
                    horizontalLineToRelative(2.25f)
                    curveToRelative(1.52f, 0f, 2.75f, -1.23f, 2.75f, -2.75f)
                    curveToRelative(0f, -0.41f, -0.34f, -0.75f, -0.75f, -0.75f)
                    reflectiveCurveToRelative(-0.75f, 0.34f, -0.75f, 0.75f)
                    curveToRelative(0f, 0.69f, -0.56f, 1.25f, -1.25f, 1.25f)
                    horizontalLineToRelative(-2.33f)
                    curveToRelative(-0.23f, -2.02f, -1.32f, -3.78f, -2.92f, -4.86f)
                    verticalLineToRelative(-0.39f)
                    curveToRelative(0f, -0.69f, 0.56f, -1.25f, 1.25f, -1.25f)
                    curveToRelative(0.41f, 0f, 0.75f, -0.34f, 0.75f, -0.75f)
                    reflectiveCurveToRelative(-0.34f, -0.75f, -0.75f, -0.75f)
                    curveToRelative(-1.4f, 0f, -2.55f, 1.06f, -2.72f, 2.42f)
                    curveToRelative(-0.72f, -0.26f, -1.48f, -0.42f, -2.28f, -0.42f)
                    curveToRelative(-0.8f, 0f, -1.57f, 0.17f, -2.28f, 0.42f)
                    curveToRelative(-0.16f, -1.36f, -1.31f, -2.42f, -2.72f, -2.42f)
                    curveToRelative(-0.41f, 0f, -0.75f, 0.34f, -0.75f, 0.75f)
                    reflectiveCurveToRelative(0.34f, 0.75f, 0.75f, 0.75f)
                    curveToRelative(0.69f, 0f, 1.25f, 0.56f, 1.25f, 1.25f)
                    verticalLineToRelative(0.39f)
                    curveToRelative(-1.61f, 1.08f, -2.7f, 2.84f, -2.92f, 4.86f)
                    horizontalLineToRelative(-2.33f)
                    curveToRelative(-0.69f, 0f, -1.25f, -0.56f, -1.25f, -1.25f)
                    curveToRelative(0f, -0.41f, -0.34f, -0.75f, -0.75f, -0.75f)
                    reflectiveCurveToRelative(-0.75f, 0.34f, -0.75f, 0.75f)
                    curveToRelative(0f, 1.52f, 1.23f, 2.75f, 2.75f, 2.75f)
                    horizontalLineToRelative(2.25f)
                    verticalLineToRelative(2.5f)
                    horizontalLineToRelative(-4.25f)
                    curveToRelative(-0.41f, 0f, -0.75f, 0.34f, -0.75f, 0.75f)
                    reflectiveCurveToRelative(0.34f, 0.75f, 0.75f, 0.75f)
                    horizontalLineToRelative(4.25f)
                    verticalLineToRelative(2.5f)
                    horizontalLineToRelative(-2.25f)
                    curveToRelative(-1.52f, 0f, -2.75f, 1.23f, -2.75f, 2.75f)
                    curveToRelative(0f, 0.41f, 0.34f, 0.75f, 0.75f, 0.75f)
                    reflectiveCurveToRelative(0.75f, -0.34f, 0.75f, -0.75f)
                    curveToRelative(0f, -0.69f, 0.56f, -1.25f, 1.25f, -1.25f)
                    horizontalLineToRelative(2.33f)
                    curveToRelative(0.38f, 3.37f, 3.21f, 6f, 6.67f, 6f)
                    reflectiveCurveToRelative(6.3f, -2.63f, 6.67f, -6f)
                    horizontalLineToRelative(2.33f)
                    curveToRelative(0.69f, 0f, 1.25f, 0.56f, 1.25f, 1.25f)
                    curveToRelative(0f, 0.41f, 0.34f, 0.75f, 0.75f, 0.75f)
                    reflectiveCurveToRelative(0.75f, -0.34f, 0.75f, -0.75f)
                    curveToRelative(0f, -1.52f, -1.23f, -2.75f, -2.75f, -2.75f)
                    horizontalLineToRelative(-2.25f)
                    verticalLineToRelative(-2.5f)
                    close()
                    moveToRelative(-11f, -10f)
                    curveToRelative(2.64f, 0f, 4.81f, 1.96f, 5.17f, 4.5f)
                    horizontalLineToRelative(-10.35f)
                    curveToRelative(0.37f, -2.54f, 2.54f, -4.5f, 5.17f, -4.5f)
                    close()
                    moveToRelative(-5.25f, 13.25f)
                    verticalLineToRelative(-7.25f)
                    horizontalLineToRelative(4.5f)
                    verticalLineToRelative(12.42f)
                    curveToRelative(-2.54f, -0.37f, -4.5f, -2.54f, -4.5f, -5.17f)
                    close()
                    moveToRelative(6f, 5.17f)
                    verticalLineToRelative(-12.42f)
                    horizontalLineToRelative(4.5f)
                    verticalLineToRelative(7.25f)
                    curveToRelative(0f, 2.64f, -1.96f, 4.81f, -4.5f, 5.17f)
                    close()
                }
            }
        }.build().also { debugCache = it }
    }

private var debugCache: ImageVector? = null

private const val GLYPH_CENTER = IconViewportSize / 2f

// The traced glyph is inset by an eighth of a unit either side of bug.svg's 12x12 viewBox, so
// doubled it spans 23.5. The set's live area is 20, inset 2 from the 24 viewport.
private const val TRACED_GLYPH_SIZE = 23.5f
private const val LIVE_AREA_SIZE = 20f
private const val LIVE_AREA_FIT = LIVE_AREA_SIZE / TRACED_GLYPH_SIZE

@Preview(name = "Debug", showBackground = true)
@Composable
private fun DebugPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.Debug,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
