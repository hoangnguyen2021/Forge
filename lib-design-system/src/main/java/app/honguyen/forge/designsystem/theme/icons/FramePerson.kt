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

val Icons.FramePerson: ImageVector
    get() {
        framePersonCache?.let { return it }
        return ImageVector.Builder(
            name = "FramePerson",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Head, then its counter wound back the other way so a non-zero fill reads the
                // second as a hole in the first. Both are quarter-by-quarter traces of a circle
                // rather than arcs, which is the source's own construction.
                moveTo(12f, 12f)
                quadTo(10.73f, 12f, 9.86f, 11.14f)
                reflectiveQuadTo(9f, 9f)
                quadTo(9f, 7.75f, 9.86f, 6.88f)
                reflectiveQuadTo(12f, 6f)
                quadToRelative(1.25f, 0f, 2.13f, 0.88f)
                reflectiveQuadTo(15f, 9f)
                quadToRelative(0f, 1.27f, -0.88f, 2.14f)
                reflectiveQuadTo(12f, 12f)
                close()
                moveToRelative(0f, -2f)
                quadToRelative(0.43f, 0f, 0.71f, -0.29f)
                reflectiveQuadTo(13f, 9f)
                quadTo(13f, 8.57f, 12.71f, 8.29f)
                reflectiveQuadTo(12f, 8f)
                reflectiveQuadTo(11.29f, 8.29f)
                reflectiveQuadTo(11f, 9f)
                quadToRelative(0f, 0.42f, 0.29f, 0.71f)
                reflectiveQuadTo(12f, 10f)
                close()
                // Shoulders, and their counter after it: a bust squared off at the bottom, its
                // top edge rising to the shoulder line either side of the neck. The same winding
                // rule opens this one out as well.
                moveTo(6f, 18f)
                verticalLineTo(16.1f)
                quadTo(6f, 15.58f, 6.26f, 15.11f)
                reflectiveQuadTo(6.98f, 14.38f)
                quadTo(8.13f, 13.7f, 9.39f, 13.35f)
                reflectiveQuadTo(12f, 13f)
                reflectiveQuadToRelative(2.61f, 0.35f)
                reflectiveQuadToRelative(2.41f, 1.03f)
                quadToRelative(0.45f, 0.28f, 0.71f, 0.74f)
                quadTo(18f, 15.58f, 18f, 16.1f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                close()
                moveToRelative(4f, -2.75f)
                quadTo(9.03f, 15.5f, 8.15f, 16f)
                horizontalLineToRelative(7.7f)
                quadTo(14.98f, 15.5f, 14f, 15.25f)
                quadTo(13.03f, 15f, 12f, 15f)
                reflectiveQuadToRelative(-2f, 0.25f)
                close()
                moveTo(12f, 9f)
                close()
                moveToRelative(2f, 7f)
                quadToRelative(0.98f, 0f, 1.85f, 0f)
                horizontalLineTo(8.15f)
                quadTo(9.03f, 16f, 10f, 16f)
                reflectiveQuadToRelative(2f, 0f)
                quadToRelative(1.03f, 0f, 2f, 0f)
                close()
                // The four corner brackets, each an L of the frame with its outer corner rounded
                // and its inner one square. They run bottom-left, top-left, bottom-right,
                // top-right, and the gaps between them are the frame — there is no rectangle
                // here for them to be cut out of.
                moveTo(4f, 22f)
                quadTo(3.18f, 22f, 2.59f, 21.41f)
                reflectiveQuadTo(2f, 20f)
                verticalLineTo(16f)
                horizontalLineTo(4f)
                verticalLineToRelative(4f)
                horizontalLineTo(8f)
                verticalLineToRelative(2f)
                horizontalLineTo(4f)
                close()
                moveTo(2f, 8f)
                verticalLineTo(4f)
                quadTo(2f, 3.17f, 2.59f, 2.59f)
                reflectiveQuadTo(4f, 2f)
                horizontalLineTo(8f)
                verticalLineTo(4f)
                horizontalLineTo(4f)
                verticalLineTo(8f)
                horizontalLineTo(2f)
                close()
                moveTo(16f, 22f)
                verticalLineTo(20f)
                horizontalLineToRelative(4f)
                verticalLineTo(16f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(4f)
                quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                reflectiveQuadTo(20f, 22f)
                horizontalLineTo(16f)
                close()
                moveTo(20f, 8f)
                verticalLineTo(4f)
                horizontalLineTo(16f)
                verticalLineTo(2f)
                horizontalLineToRelative(4f)
                quadToRelative(0.83f, 0f, 1.41f, 0.59f)
                reflectiveQuadTo(22f, 4f)
                verticalLineTo(8f)
                horizontalLineTo(20f)
                close()
            }
        }.build().also { framePersonCache = it }
    }

private var framePersonCache: ImageVector? = null

@Preview(name = "FramePerson", showBackground = true)
@Composable
private fun FramePersonPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.FramePerson,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
