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
import kotlin.math.cos
import kotlin.math.sin

val Icons.Retouch: ImageVector
    get() {
        retouchCache?.let { return it }
        return ImageVector.Builder(
            name = "Retouch",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                // Only the shoulders carry caps; the head closes on itself.
                strokeLineCap = StrokeCap.Round,
            ) {
                head()
                shoulders()
            }
            // The sparkles are solid where the person is drawn — the contrast is what keeps
            // them reading as light on the subject rather than as more of the subject.
            path(fill = SolidColor(Color.Black)) {
                sparkle(RISING_SPARKLE_X, RISING_SPARKLE_Y)
                sparkle(FALLING_SPARKLE_X, FALLING_SPARKLE_Y)
            }
        }.build().also { retouchCache = it }
    }

private var retouchCache: ImageVector? = null

/** The head, as two half circles so that neither sweep is a full turn's worth of ambiguity. */
private fun PathBuilder.head() {
    moveTo(HEAD_CENTER_X + HEAD_RADIUS, HEAD_CENTER_Y)
    sweepTo(HEAD_RADIUS, HEAD_RADIUS, HEAD_CENTER_X - HEAD_RADIUS, HEAD_CENTER_Y)
    sweepTo(HEAD_RADIUS, HEAD_RADIUS, HEAD_CENTER_X + HEAD_RADIUS, HEAD_CENTER_Y)
    close()
}

/**
 * The shoulders: the upper half of an ellipse, in two quarters, capped round at both ends.
 *
 * The half is where it stops because the half is where the two ends run parallel. Both land on
 * the ellipse's widest, the one place its tangent stands vertical, so the caps face straight
 * down and the bust sits square on its own base. Carried past it the ends turn under and close
 * on each other, and the arch starts reading as a bowl.
 */
private fun PathBuilder.shoulders() {
    moveTo(HEAD_CENTER_X - SHOULDER_RADIUS_X, SHOULDER_CENTER_Y)
    sweepTo(SHOULDER_RADIUS_X, SHOULDER_RADIUS_Y, HEAD_CENTER_X, SHOULDER_CENTER_Y - SHOULDER_RADIUS_Y)
    sweepTo(SHOULDER_RADIUS_X, SHOULDER_RADIUS_Y, HEAD_CENTER_X + SHOULDER_RADIUS_X, SHOULDER_CENTER_Y)
}

/**
 * One four-point star, tips on the axes.
 *
 * Each quarter is the same three moves: out to a tip, back down the flank to where it meets the
 * concave side at [SPARKLE_WAIST], and round that side to the next flank. The side is struck from
 * well outside the star, which is what makes it bite inward and leave the tips as spikes rather
 * than as the corners of a diamond.
 *
 * sparkle.svg eases each tip with a rounding a twentieth of its reach, which at the size these
 * are drawn comes to a seventh of a unit and is gone long before the icon reaches a screen. The
 * tips are left as points, and [SPARKLE_TIP] is measured to them rather than to where that
 * rounding would have cut them back to.
 */
private fun PathBuilder.sparkle(
    centerX: Float,
    centerY: Float,
) {
    repeat(TIP_COUNT) { index ->
        val axis = index * QUARTER_TURN
        val tipX = orbit(centerX, SPARKLE_TIP, axis, ::cos)
        val tipY = orbit(centerY, SPARKLE_TIP, axis, ::sin)
        if (index == 0) moveTo(tipX, tipY) else lineTo(tipX, tipY)

        val leaving = axis + SPARKLE_FLANK_ANGLE
        val arriving = axis + QUARTER_TURN - SPARKLE_FLANK_ANGLE
        lineTo(orbit(centerX, SPARKLE_WAIST, leaving, ::cos), orbit(centerY, SPARKLE_WAIST, leaving, ::sin))
        sweepTo(
            radiusX = SPARKLE_ARC_RADIUS,
            radiusY = SPARKLE_ARC_RADIUS,
            x = orbit(centerX, SPARKLE_WAIST, arriving, ::cos),
            y = orbit(centerY, SPARKLE_WAIST, arriving, ::sin),
            // Struck the other way about, so the side falls away from the star's center.
            clockwise = false,
        )
    }
    close()
}

/**
 * An arc, circular where the two radii agree and elliptical where they do not. [clockwise] is
 * what picks the concave side of a sparkle from the convex one.
 */
private fun PathBuilder.sweepTo(
    radiusX: Float,
    radiusY: Float,
    x: Float,
    y: Float,
    clockwise: Boolean = true,
) = arcTo(
    horizontalEllipseRadius = radiusX,
    verticalEllipseRadius = radiusY,
    theta = 0f,
    isMoreThanHalf = false,
    isPositiveArc = clockwise,
    x1 = x,
    y1 = y,
)

// Angles run clockwise from three o'clock, so -90 is straight up.
private fun orbit(
    center: Float,
    radius: Float,
    angle: Float,
    axis: (Float) -> Float,
) = center + radius * axis(angle * DEGREES_TO_RADIANS)

private const val STROKE_WIDTH = 1.5f
private const val HALF_STROKE = STROKE_WIDTH / 2f
private const val GLYPH_CENTER = IconViewportSize / 2f

private const val HALF_TURN = 180f
private const val QUARTER_TURN = 90f
private const val TIP_COUNT = 4
private val DEGREES_TO_RADIANS = PI.toFloat() / HALF_TURN

// The head sizes everything else, so it is the one measure taken by eye. At 3.42 the sparkles
// carry the ink out to the full 20 of live area across, and the bust takes it to 18.7 down.
private const val HEAD_RADIUS = 3.42f
private const val HEAD_INK_RADIUS = HEAD_RADIUS + HALF_STROKE
private const val HEAD_CENTER_X = GLYPH_CENTER

// The bust, an ellipse hung below the head and measured in head radii like everything else here.
//
// The three are one setting, not three: depth climbs toward the chin as fast as the drop lowers
// it, so the gap under the chin is what the two of them leave over. At these it comes to 1.24,
// under the head's own stroke — close enough that head and shoulders read as one figure rather
// than two shapes stacked, open enough that the chin still has daylight beneath it.
private const val SHOULDER_DROP = 3.45f * HEAD_RADIUS
private const val SHOULDER_RADIUS_X = 2f * HEAD_RADIUS
private const val SHOULDER_RADIUS_Y = 1.65f * HEAD_RADIUS

// Across the star, three fifths of the way across the head, as the ink of each is measured —
// tip to opposite tip against the head's outer edge. Small enough to read as light thrown off
// the subject rather than as a second subject.
private const val SPARKLE_TIP = 0.6f * HEAD_INK_RADIUS

// Where the sparkles sit: two head radii out from the head's center, one up and to the right and
// one down and to the left. Two radii is what clears the head, leaving the nearer tip of each
// 1.67 off it.
//
// The bearing trades the glyph's height against its width, the pair being what the width is
// measured across. At 26 they sit nearer level than diagonal, so they flank the head rather than
// corner it. Steeper squares the glyph up but drops the falling sparkle toward the shoulder,
// which is the one collision this arrangement has to dodge.
private const val SPARKLE_REACH = 2f * HEAD_INK_RADIUS
private const val SPARKLE_BEARING = 26f
private val SPARKLE_OFFSET_X = SPARKLE_REACH * cos(SPARKLE_BEARING * DEGREES_TO_RADIANS)
private val SPARKLE_OFFSET_Y = SPARKLE_REACH * sin(SPARKLE_BEARING * DEGREES_TO_RADIANS)

// The ink is not symmetric about the head: above it there is only the rising sparkle's tip, below
// it the whole bust. Centering the glyph therefore means centering that span, not the head.
//
// The arch's own lowest ink is its two capped ends, not the curve between them: stopping on the
// half leaves both on the ellipse's center line, and only the caps reach below it.
private val INK_ABOVE_HEAD = SPARKLE_OFFSET_Y + SPARKLE_TIP
private const val INK_BELOW_HEAD = SHOULDER_DROP + HALF_STROKE
private val HEAD_CENTER_Y = GLYPH_CENTER - (INK_BELOW_HEAD - INK_ABOVE_HEAD) / 2f
private val SHOULDER_CENTER_Y = HEAD_CENTER_Y + SHOULDER_DROP

private val RISING_SPARKLE_X = HEAD_CENTER_X + SPARKLE_OFFSET_X
private val RISING_SPARKLE_Y = HEAD_CENTER_Y - SPARKLE_OFFSET_Y
private val FALLING_SPARKLE_X = HEAD_CENTER_X - SPARKLE_OFFSET_X
private val FALLING_SPARKLE_Y = HEAD_CENTER_Y + SPARKLE_OFFSET_Y

// sparkle.svg's own star, measured against the reach of its tips: the flanks meet the concave
// side a little over half way in and a hair off the axis, and that side is struck at a radius
// two thirds of the reach. Ratios rather than lengths, so the star holds its shape at any size.
private const val SPARKLE_WAIST = 0.6015f * SPARKLE_TIP
private const val SPARKLE_FLANK_ANGLE = 11.261f
private const val SPARKLE_ARC_RADIUS = 0.6888f * SPARKLE_TIP

@Preview(name = "Retouch", showBackground = true)
@Composable
private fun RetouchPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.Retouch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
