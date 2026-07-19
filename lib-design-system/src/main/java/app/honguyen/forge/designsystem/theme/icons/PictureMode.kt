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

val Icons.PictureMode: ImageVector
    get() {
        pictureModeCache?.let { return it }
        return ImageVector.Builder(
            name = "PictureMode",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Body outline, traced counterclockwise from the top-right corner: left across
                // the top and over the bump, down the left side, right along the bottom,
                // back up the right side. The four body corners share CORNER_RADIUS; the
                // bump keeps its own softer radii and stays centered on x = 12 regardless.
                moveTo(RIGHT - CORNER_RADIUS, TOP)
                horizontalLineTo(17.0f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16.35f, 4.0f)
                lineTo(15.85f, 3.25f)
                arcTo(1.2f, 1.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 15.0f, 3.0f)
                horizontalLineTo(9.0f)
                arcTo(1.2f, 1.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8.15f, 3.25f)
                lineTo(7.65f, 4.0f)
                arcTo(0.8f, 0.8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7.0f, TOP)
                horizontalLineTo(LEFT + CORNER_RADIUS)
                arcTo(
                    CORNER_RADIUS,
                    CORNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    LEFT,
                    TOP + CORNER_RADIUS,
                )
                verticalLineTo(BOTTOM - CORNER_RADIUS)
                arcTo(
                    CORNER_RADIUS,
                    CORNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    LEFT + CORNER_RADIUS,
                    BOTTOM,
                )
                horizontalLineTo(RIGHT - CORNER_RADIUS)
                arcTo(
                    CORNER_RADIUS,
                    CORNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    RIGHT,
                    BOTTOM - CORNER_RADIUS,
                )
                verticalLineTo(TOP + CORNER_RADIUS)
                arcTo(
                    CORNER_RADIUS,
                    CORNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    RIGHT - CORNER_RADIUS,
                    TOP,
                )
                close()

                // Lens ring: a full circle wound opposite the body, opening a hole.
                moveTo(LENS_CENTER_X, LENS_CENTER_Y + LENS_OUTER_RADIUS)
                arcTo(
                    LENS_OUTER_RADIUS,
                    LENS_OUTER_RADIUS,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    LENS_CENTER_X + LENS_OUTER_RADIUS,
                    LENS_CENTER_Y,
                )
                arcTo(
                    LENS_OUTER_RADIUS,
                    LENS_OUTER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    LENS_CENTER_X,
                    LENS_CENTER_Y + LENS_OUTER_RADIUS,
                )
                close()

                // Lens dot filling the ring's center.
                moveTo(LENS_CENTER_X, LENS_CENTER_Y - LENS_INNER_RADIUS)
                arcToRelative(
                    LENS_INNER_RADIUS,
                    LENS_INNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    0f,
                    LENS_INNER_RADIUS * 2,
                )
                arcToRelative(
                    LENS_INNER_RADIUS,
                    LENS_INNER_RADIUS,
                    0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    0f,
                    -LENS_INNER_RADIUS * 2,
                )
                close()
            }
        }.build().also { pictureModeCache = it }
    }

private var pictureModeCache: ImageVector? = null

private const val LEFT = 3.0f
private const val RIGHT = 21.0f
private const val TOP = 4.25f
private const val BOTTOM = 21.0f
private const val CORNER_RADIUS = 1.5f

private const val LENS_CENTER_X = 12f
private const val LENS_CENTER_Y = (TOP + BOTTOM) / 2f
private const val LENS_OUTER_RADIUS = 4.5f
private const val LENS_INNER_RADIUS = 3.5f

@Preview(name = "PictureMode", showBackground = true)
@Composable
private fun PictureModePreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.PictureMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
