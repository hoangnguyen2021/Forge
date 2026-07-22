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
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.IconDefaultSize
import app.honguyen.forge.designsystem.theme.IconViewportSize
import app.honguyen.forge.designsystem.theme.Icons
import kotlin.math.sqrt

val Icons.CameraFlip2: ImageVector
    get() {
        cameraFlip2Cache?.let { return it }
        return ImageVector.Builder(
            name = "CameraFlip2",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            arm()

            // The second arm is the first turned about the glyph center — arc under the
            // bottom, head pointing up the left flank. A rotation says that; a second trace
            // would only be the same coordinates written out negated.
            group(
                rotate = HALF_TURN,
                pivotX = GLYPH_CENTER,
                pivotY = GLYPH_CENTER,
            ) {
                arm()
            }
        }.build().also { cameraFlip2Cache = it }
    }

private var cameraFlip2Cache: ImageVector? = null

/**
 * One arm of the loop: an arc over the crown, a shaft down the right flank, and a single
 * outward barb at its foot, drawn as one unbroken run.
 */
private fun ImageVector.Builder.arm() =
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        // From the tail at half past ten clockwise to three o'clock — 135 degrees, which is
        // what leaves the arc pointing straight down, so the shaft carries on from it unbent.
        moveTo(GLYPH_CENTER - ARC_TAIL_OFFSET, ARC_CENTER_Y - ARC_TAIL_OFFSET)
        arcTo(
            horizontalEllipseRadius = ARC_RADIUS,
            verticalEllipseRadius = ARC_RADIUS,
            theta = 0f,
            isMoreThanHalf = false,
            isPositiveArc = true,
            x1 = SHAFT_X,
            y1 = ARC_CENTER_Y,
        )
        lineTo(SHAFT_X, TIP_Y)

        // Half a head: the barb the loop turns away from, kicking back out over the flank.
        // The inward one would close across the daylight the other arm's arc swings through.
        lineTo(SHAFT_X + BARB_REACH, TIP_Y - BARB_REACH)
    }

private const val HALF_TURN = 180f
private const val GLYPH_CENTER = IconViewportSize / 2f

private const val STROKE_WIDTH = 2f
private const val HALF_STROKE = STROKE_WIDTH / 2f

// Sizes the glyph across, the head being the widest thing on an arm: at 5.75 the barbs reach
// 21.25, three quarters shy of the live area, against a crown that sits right on it.
private const val ARC_RADIUS = 5.75f

// An open outline form, so the glyph is drawn to the 20x20 live area rather than held to the
// 18 square keyline. Standing the crown's outer edge on the live area is what fills it, and
// that fixes the arc center a stroke and a radius below.
private const val LIVE_AREA_TOP = 2f
private const val ARC_CENTER_Y = LIVE_AREA_TOP + HALF_STROKE + ARC_RADIUS

// The tail cuts the arc at 45 degrees, where its offsets from the center are equal.
private val ARC_TAIL_OFFSET = ARC_RADIUS / sqrt(2f)

// Three o'clock on the arc, which the shaft leaves along.
private const val SHAFT_X = GLYPH_CENTER + ARC_RADIUS

// The rotation lands the other arm's arc center here, so the shaft spans exactly the gap
// between the two center lines and the arms interlock rather than overrunning each other.
private const val TIP_Y = IconViewportSize - ARC_CENTER_Y

// Reach along each axis, the barb sitting at 45 degrees to the shaft: 2.5 puts 3.54 of barb
// against a 6.5 shaft, enough that one of them still reads as a head at 24dp.
private const val BARB_REACH = 2.5f

@Preview(name = "CameraFlip2", showBackground = true)
@Composable
private fun CameraFlip2Preview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.CameraFlip2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
