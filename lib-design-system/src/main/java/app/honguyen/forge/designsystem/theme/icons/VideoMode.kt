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
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.IconDefaultSize
import app.honguyen.forge.designsystem.theme.IconViewportSize
import app.honguyen.forge.designsystem.theme.Icons

val Icons.VideoMode: ImageVector
    get() {
        videoModeCache?.let { return it }
        return ImageVector.Builder(
            name = "VideoMode",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Body top edge, left to right, into the top-right corner.
                moveTo(BODY_LEFT + BODY_CORNER_RADIUS, BODY_TOP)
                horizontalLineTo(BODY_RIGHT - BODY_CORNER_RADIUS)
                arcTo(
                    BODY_CORNER_RADIUS,
                    BODY_CORNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    BODY_RIGHT,
                    BODY_TOP + BODY_CORNER_RADIUS,
                )

                // Down the right edge to the neck, out along the flaring lens, and back in.
                verticalLineTo(LENS_NECK_TOP)
                lineTo(LENS_TIP_X, LENS_TIP_TOP)
                quadTo(LENS_RIGHT, LENS_FLARE_TOP, LENS_RIGHT, LENS_FLARE_TOP + LENS_CORNER_CUT)
                verticalLineTo(LENS_FLARE_BOTTOM - LENS_CORNER_CUT)
                quadTo(LENS_RIGHT, LENS_FLARE_BOTTOM, LENS_TIP_X, LENS_TIP_BOTTOM)
                lineTo(BODY_RIGHT, LENS_NECK_BOTTOM)

                // Down to the bottom-right corner, across the bottom, and up the left side.
                verticalLineTo(BODY_BOTTOM - BODY_CORNER_RADIUS)
                arcTo(
                    BODY_CORNER_RADIUS,
                    BODY_CORNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    BODY_RIGHT - BODY_CORNER_RADIUS,
                    BODY_BOTTOM,
                )
                horizontalLineTo(BODY_LEFT + BODY_CORNER_RADIUS)
                arcTo(
                    BODY_CORNER_RADIUS,
                    BODY_CORNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    BODY_LEFT,
                    BODY_BOTTOM - BODY_CORNER_RADIUS,
                )
                verticalLineTo(BODY_TOP + BODY_CORNER_RADIUS)
                arcTo(
                    BODY_CORNER_RADIUS,
                    BODY_CORNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    BODY_LEFT + BODY_CORNER_RADIUS,
                    BODY_TOP,
                )
                close()
            }
        }.build().also { videoModeCache = it }
    }

private var videoModeCache: ImageVector? = null

// A solid mass, so it sits on the 20x16 horizontal keyline rather than filling the live area:
// body plus lens spans x 2..22, and the body squares off the full 16 of height.
private const val BODY_LEFT = 2.0f
private const val BODY_RIGHT = 18.0f
private const val BODY_TOP = 4.0f
private const val BODY_BOTTOM = 20.0f
private const val BODY_CORNER_RADIUS = 2.0f

private const val LENS_RIGHT = 22.0f
private const val LENS_NECK_TOP = 10.0f
private const val LENS_NECK_BOTTOM = 14.0f
private const val LENS_FLARE_TOP = 8.0f
private const val LENS_FLARE_BOTTOM = 16.0f

// The lens tip is rounded by a quad through the corner it would otherwise make. The cut is
// how far short of that corner the curve ends; the diagonal stops short by its own amount,
// and LENS_TIP_X rides the neck-to-flare slope to wherever that leaves it.
private const val LENS_CORNER_CUT = 0.75f
private const val LENS_DIAGONAL_CUT = 0.4f
private const val LENS_FLARE_RISE = LENS_NECK_TOP - LENS_FLARE_TOP
private const val LENS_TIP_X =
    LENS_RIGHT - (LENS_RIGHT - BODY_RIGHT) * (LENS_DIAGONAL_CUT / LENS_FLARE_RISE)
private const val LENS_TIP_TOP = LENS_FLARE_TOP + LENS_DIAGONAL_CUT
private const val LENS_TIP_BOTTOM = LENS_FLARE_BOTTOM - LENS_DIAGONAL_CUT

@Preview(name = "VideoMode", showBackground = true)
@Composable
private fun VideoModePreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.VideoMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
