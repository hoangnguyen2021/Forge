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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.IconDefaultSize
import app.honguyen.forge.designsystem.theme.IconViewportSize
import app.honguyen.forge.designsystem.theme.Icons

val Icons.CameraFlip: ImageVector
    get() {
        cameraFlipCache?.let { return it }
        return ImageVector.Builder(
            name = "CameraFlip",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Arrow arcing over the top, left to right, head at the right end.
                moveTo(4.40f, 9.24f)
                arcTo(
                    horizontalEllipseRadius = ARC_RADIUS,
                    verticalEllipseRadius = ARC_RADIUS,
                    theta = 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 19.60f,
                    y1 = 9.24f,
                )
                lineTo(21.25f, 5.67f)

                // The same arrow, rotated 180 degrees about (12, 12): under the bottom,
                // right to left, head at the left end.
                moveTo(19.60f, 14.76f)
                arcTo(
                    horizontalEllipseRadius = ARC_RADIUS,
                    verticalEllipseRadius = ARC_RADIUS,
                    theta = 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 4.40f,
                    y1 = 14.76f,
                )
                lineTo(2.75f, 18.33f)
            }
        }.build().also { cameraFlipCache = it }
    }

private var cameraFlipCache: ImageVector? = null

private const val ARC_RADIUS = 8.09f
private const val STROKE_WIDTH = 1.5f

@Preview(name = "CameraFlip", showBackground = true)
@Composable
private fun CameraFlipPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.CameraFlip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
