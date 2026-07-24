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

// Traced coordinates rather than derived ones, the two runs being a straight lift of the source
// and each other's mirror: every point above turns onto its opposite about (12, 12), which is
// what holds the two arrows the same length and the glyph centered without a constant saying so.

// An open outline form, so the glyph is drawn to the 20x20 live area rather than held to the 18
// square keyline. Width is what lands on it — the two barb tips span x 2..22 — while the arc
// crowns stop at 3.17 and the ink runs 17.66 tall, an arc reaching its own live area edge only
// at one point being no way to fill a box.

// How hard the arrows bow, and a judgement against the chord rather than a free number: at 8.09
// against a half chord of 7.6 the arc is half a unit off the tightest it could be, bowing 5.32
// above its own chord. The two arcs pass each other at their ends, where this leaves 4 of
// daylight down the middle — enough that they read as two strokes rather than a closed ring.
private const val ARC_RADIUS = 8.09f

// The lighter of the two pens in the set, which a glyph this open can carry: every unit added
// here comes off that channel twice over, once from each arc.
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
