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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.IconDefaultSize
import app.honguyen.forge.designsystem.theme.IconViewportSize
import app.honguyen.forge.designsystem.theme.Icons
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

val Icons.Timer: ImageVector
    get() {
        timerCache?.let { return it }
        return ImageVector.Builder(
            name = "Timer",
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
            ) {
                rim()
            }
            path(fill = SolidColor(Color.Black)) {
                hand()
            }
        }.build().also { timerCache = it }
    }

private var timerCache: ImageVector? = null

/**
 * The dial, broken by one gap on the bearing the hand points along.
 *
 * Drawn as two arcs rather than one because a single sweep of nearly a full turn leaves the
 * renderer to pick between two arcs that differ by a hair; splitting it at the point opposite
 * the gap keeps both halves comfortably under a half turn.
 */
private fun PathBuilder.rim() {
    moveTo(dialX(DIAL_RADIUS, RIM_START_ANGLE), dialY(DIAL_RADIUS, RIM_START_ANGLE))
    sweepTo(DIAL_RADIUS, dialX(DIAL_RADIUS, RIM_MID_ANGLE), dialY(DIAL_RADIUS, RIM_MID_ANGLE))
    sweepTo(DIAL_RADIUS, dialX(DIAL_RADIUS, RIM_END_ANGLE), dialY(DIAL_RADIUS, RIM_END_ANGLE))
}

/**
 * The hand: the hull of two circles on the bearing, a pivot on the glyph center and a far
 * smaller one at the far end, with the flanks running as the outer tangents between them.
 *
 * One construction gives the needle its taper and both of its ends their roundness, and it is
 * why the flanks meet the arcs at [HAND_TANGENT_ANGLE] rather than square on. Traced up the
 * leading flank, round the tip, back down the trailing one, and the long way round the pivot.
 */
private fun PathBuilder.hand() {
    val leading = HAND_BEARING + HAND_TANGENT_ANGLE
    val trailing = HAND_BEARING - HAND_TANGENT_ANGLE

    moveTo(dialX(HAND_PIVOT_RADIUS, leading), dialY(HAND_PIVOT_RADIUS, leading))
    lineTo(handTipX(leading), handTipY(leading))
    sweepTo(
        radius = HAND_TIP_RADIUS,
        x = handTipX(trailing),
        y = handTipY(trailing),
        clockwise = false,
    )
    lineTo(dialX(HAND_PIVOT_RADIUS, trailing), dialY(HAND_PIVOT_RADIUS, trailing))
    sweepTo(
        radius = HAND_PIVOT_RADIUS,
        x = dialX(HAND_PIVOT_RADIUS, leading),
        y = dialY(HAND_PIVOT_RADIUS, leading),
        moreThanHalf = true,
        clockwise = false,
    )
    close()
}

/**
 * An arc of a true circle — the only kind this glyph draws. [moreThanHalf] is what picks the
 * pivot's long way round from its short one, and [clockwise] which side of the chord it takes.
 */
private fun PathBuilder.sweepTo(
    radius: Float,
    x: Float,
    y: Float,
    moreThanHalf: Boolean = false,
    clockwise: Boolean = true,
) = arcTo(
    horizontalEllipseRadius = radius,
    verticalEllipseRadius = radius,
    theta = 0f,
    isMoreThanHalf = moreThanHalf,
    isPositiveArc = clockwise,
    x1 = x,
    y1 = y,
)

// Angles run clockwise from three o'clock, so -90 is straight up. The pivot shares the dial's
// center, so these carry the hand's wide end as well as the rim.
private fun dialX(
    radius: Float,
    angle: Float,
) = DIAL_CENTER_X + radius * cos(angle * DEGREES_TO_RADIANS)

private fun dialY(
    radius: Float,
    angle: Float,
) = DIAL_CENTER_Y + radius * sin(angle * DEGREES_TO_RADIANS)

private fun handTipX(angle: Float) = HAND_TIP_CENTER_X + HAND_TIP_RADIUS * cos(angle * DEGREES_TO_RADIANS)

private fun handTipY(angle: Float) = HAND_TIP_CENTER_Y + HAND_TIP_RADIUS * sin(angle * DEGREES_TO_RADIANS)

private const val STROKE_WIDTH = 1.75f
private const val HALF_STROKE = STROKE_WIDTH / 2f

private const val HALF_TURN = 180f
private const val FULL_TURN = 360f
private val DEGREES_TO_RADIANS = PI.toFloat() / HALF_TURN

// The glyph is a dial with everything else hung off it, so centering the dial centers the icon.
private const val DIAL_CENTER_X = 12f
private const val DIAL_CENTER_Y = 12f

// A circle is the one keyline shape that earns the full 20 of live area, the extra 2 offsetting
// the optical shrink a round form suffers against a square one. So the ink runs to radius 10
// and the centerline the arc is struck on sits a half stroke inside that.
private const val LIVE_AREA_RADIUS = 10f
private const val DIAL_RADIUS = LIVE_AREA_RADIUS - HALF_STROKE

// Up and to the left, on the diagonal. The hand and the gap in the rim both run on it, which is
// the whole of what makes the gap read as the place the hand is pointing rather than a nick.
private const val HAND_BEARING = -135f

// The gap is sized by the daylight left between the two caps rather than by an angle, since
// daylight is what the eye reads. Two strokes of it opens the rim wide enough to be read as a
// mouth the hand points out of rather than a break in the line, and the 33 degrees that costs
// still leaves the rim plainly a circle. Each cap spends a half stroke of the span on itself,
// so the centerlines stand a whole stroke further apart than the daylight asks for.
private const val GAP_DAYLIGHT = 2f * STROKE_WIDTH
private val GAP_HALF_ANGLE =
    (GAP_DAYLIGHT + STROKE_WIDTH) / (2f * DIAL_RADIUS) / DEGREES_TO_RADIANS

private val RIM_START_ANGLE = HAND_BEARING + GAP_HALF_ANGLE
private val RIM_END_ANGLE = HAND_BEARING + FULL_TURN - GAP_HALF_ANGLE
private val RIM_MID_ANGLE = (RIM_START_ANGLE + RIM_END_ANGLE) / 2f

// The two circles the hand is drawn between, and the span between their centers. The pivot is
// close to twice the stroke across, which is what keeps the hand reading as a solid needle
// rather than one more stroke of the same pen.
private const val HAND_PIVOT_RADIUS = 1.55f
private const val HAND_TIP_RADIUS = 0.4f
private const val HAND_REACH = 6.4f

private val HAND_TIP_CENTER_X = dialX(HAND_REACH, HAND_BEARING)
private val HAND_TIP_CENTER_Y = dialY(HAND_REACH, HAND_BEARING)

// Where the flanks touch. An outer tangent between two circles leans off the perpendicular by
// the arcsine of their difference over the span between them, and that lean is what tapers the
// needle: hold the two radii equal and it goes to zero, leaving a capsule.
private const val QUARTER_TURN = 90f
private val HAND_TANGENT_ANGLE =
    QUARTER_TURN - asin((HAND_PIVOT_RADIUS - HAND_TIP_RADIUS) / HAND_REACH) / DEGREES_TO_RADIANS

@Preview(name = "Timer", showBackground = true)
@Composable
private fun TimerPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
