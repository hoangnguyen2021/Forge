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
                moveTo(5.42f, 9.61f)
                arcTo(
                    horizontalEllipseRadius = ARC_RADIUS,
                    verticalEllipseRadius = ARC_RADIUS,
                    theta = 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 18.58f,
                    y1 = 9.61f,
                )
                lineTo(20.01f, 6.52f)

                // The same arrow, rotated 180 degrees about (12, 12): under the bottom,
                // right to left, head at the left end.
                moveTo(18.58f, 14.39f)
                arcTo(
                    horizontalEllipseRadius = ARC_RADIUS,
                    verticalEllipseRadius = ARC_RADIUS,
                    theta = 0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 5.42f,
                    y1 = 14.39f,
                )
                lineTo(3.99f, 17.48f)
            }
        }.build().also { cameraFlipCache = it }
    }

private var cameraFlipCache: ImageVector? = null

private const val ARC_RADIUS = 7f
private const val STROKE_WIDTH = 2f

@Preview(name = "CameraFlip", showBackground = true)
@Composable
private fun CameraFlipPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(ForgeTheme.dimensions.size6x),
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
