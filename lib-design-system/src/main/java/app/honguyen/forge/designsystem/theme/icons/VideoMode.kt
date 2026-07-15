package app.honguyen.forge.designsystem.theme.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
                arcTo(BODY_CORNER_RADIUS, BODY_CORNER_RADIUS, 0f, isMoreThanHalf = false, isPositiveArc = true, BODY_RIGHT, BODY_TOP + BODY_CORNER_RADIUS)

                // Down the right edge to the neck, out along the flaring lens, and back in.
                verticalLineTo(LENS_NECK_TOP)
                lineTo(19.5f, 8.833f)
                quadTo(LENS_RIGHT, LENS_FLARE_TOP, LENS_RIGHT, LENS_FLARE_TOP + LENS_CORNER_CUT)
                verticalLineTo(LENS_FLARE_BOTTOM - LENS_CORNER_CUT)
                quadTo(LENS_RIGHT, LENS_FLARE_BOTTOM, 19.5f, 15.167f)
                lineTo(BODY_RIGHT, LENS_NECK_BOTTOM)

                // Down to the bottom-right corner, across the bottom, and up the left side.
                verticalLineTo(BODY_BOTTOM - BODY_CORNER_RADIUS)
                arcTo(BODY_CORNER_RADIUS, BODY_CORNER_RADIUS, 0f, isMoreThanHalf = false, isPositiveArc = true, BODY_RIGHT - BODY_CORNER_RADIUS, BODY_BOTTOM)
                horizontalLineTo(BODY_LEFT + BODY_CORNER_RADIUS)
                arcTo(BODY_CORNER_RADIUS, BODY_CORNER_RADIUS, 0f, isMoreThanHalf = false, isPositiveArc = true, BODY_LEFT, BODY_BOTTOM - BODY_CORNER_RADIUS)
                verticalLineTo(BODY_TOP + BODY_CORNER_RADIUS)
                arcTo(BODY_CORNER_RADIUS, BODY_CORNER_RADIUS, 0f, isMoreThanHalf = false, isPositiveArc = true, BODY_LEFT + BODY_CORNER_RADIUS, BODY_TOP)
                close()
            }
        }.build().also { videoModeCache = it }
    }

private var videoModeCache: ImageVector? = null

private const val BODY_LEFT = 4.0f
private const val BODY_RIGHT = 17.0f
private const val BODY_TOP = 5.5f
private const val BODY_BOTTOM = 18.5f
private const val BODY_CORNER_RADIUS = 1.5f

private const val LENS_RIGHT = 20.0f
private const val LENS_NECK_TOP = 10.5f
private const val LENS_NECK_BOTTOM = 13.5f
private const val LENS_FLARE_TOP = 8.5f
private const val LENS_FLARE_BOTTOM = 15.5f
private const val LENS_CORNER_CUT = 0.6f

@Preview(name = "VideoMode", showBackground = true)
@Composable
private fun VideoModePreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(ForgeTheme.dimensions.size6x),
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
