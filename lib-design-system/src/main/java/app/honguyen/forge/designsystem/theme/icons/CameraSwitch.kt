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

val Icons.CameraSwitch: ImageVector
    get() {
        cameraSwitchCache?.let { return it }
        return ImageVector.Builder(
            name = "CameraSwitch",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8f, 17f)
                quadTo(7.18f, 17f, 6.59f, 16.41f)
                reflectiveQuadTo(6f, 15f)
                verticalLineTo(9f)
                quadTo(6f, 8.17f, 6.59f, 7.59f)
                reflectiveQuadTo(8f, 7f)
                horizontalLineTo(9f)
                lineTo(10f, 6f)
                horizontalLineToRelative(4f)
                lineToRelative(1f, 1f)
                horizontalLineToRelative(1f)
                quadToRelative(0.82f, 0f, 1.41f, 0.59f)
                reflectiveQuadTo(18f, 9f)
                verticalLineToRelative(6f)
                quadToRelative(0f, 0.82f, -0.59f, 1.41f)
                reflectiveQuadTo(16f, 17f)
                horizontalLineTo(8f)
                close()
                moveTo(8f, 15f)
                horizontalLineToRelative(8f)
                verticalLineTo(9f)
                horizontalLineTo(8f)
                verticalLineToRelative(6f)
                close()
                moveToRelative(4f, -1f)
                quadToRelative(0.83f, 0f, 1.41f, -0.59f)
                reflectiveQuadTo(14f, 12f)
                reflectiveQuadTo(13.41f, 10.59f)
                reflectiveQuadTo(12f, 10f)
                reflectiveQuadToRelative(-1.41f, 0.59f)
                quadTo(10f, 11.18f, 10f, 12f)
                reflectiveQuadToRelative(0.59f, 1.41f)
                reflectiveQuadTo(12f, 14f)
                close()
                moveTo(8.55f, 0.5f)
                quadTo(9.4f, 0.22f, 10.26f, 0.11f)
                reflectiveQuadTo(12f, 0f)
                quadToRelative(2.35f, 0f, 4.44f, 0.84f)
                quadToRelative(2.09f, 0.84f, 3.7f, 2.32f)
                reflectiveQuadToRelative(2.64f, 3.5f)
                reflectiveQuadTo(24f, 11f)
                horizontalLineTo(22f)
                quadTo(21.83f, 9.2f, 21.05f, 7.64f)
                quadTo(20.28f, 6.07f, 19.06f, 4.89f)
                reflectiveQuadTo(16.28f, 2.95f)
                reflectiveQuadTo(12.9f, 2.05f)
                lineTo(14.45f, 3.6f)
                lineTo(13.05f, 5f)
                lineTo(8.55f, 0.5f)
                close()
                moveToRelative(6.9f, 23f)
                quadToRelative(-0.85f, 0.27f, -1.71f, 0.39f)
                reflectiveQuadTo(12f, 24f)
                quadTo(9.65f, 24f, 7.56f, 23.16f)
                reflectiveQuadTo(3.86f, 20.84f)
                reflectiveQuadTo(1.23f, 17.34f)
                quadTo(0.2f, 15.33f, 0f, 13f)
                horizontalLineTo(2f)
                quadToRelative(0.2f, 1.8f, 0.96f, 3.36f)
                reflectiveQuadToRelative(1.97f, 2.75f)
                reflectiveQuadToRelative(2.79f, 1.94f)
                reflectiveQuadToRelative(3.38f, 0.9f)
                lineTo(9.55f, 20.4f)
                lineTo(10.95f, 19f)
                lineToRelative(4.5f, 4.5f)
                close()
                moveTo(12f, 12f)
                close()
            }
        }.build().also { cameraSwitchCache = it }
    }

private var cameraSwitchCache: ImageVector? = null

@Preview(name = "CameraSwitch", showBackground = true)
@Composable
private fun CameraSwitchPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(ForgeTheme.dimensions.size6x),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.CameraSwitch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}