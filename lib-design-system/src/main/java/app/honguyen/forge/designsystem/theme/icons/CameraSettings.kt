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
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.IconDefaultSize
import app.honguyen.forge.designsystem.theme.IconViewportSize
import app.honguyen.forge.designsystem.theme.Icons
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

val Icons.CameraSettings: ImageVector
    get() {
        cameraSettingsCache?.let { return it }
        return ImageVector.Builder(
            name = "CameraSettings",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            // The notch is a clip rather than part of the traced outline: it cuts a tooth,
            // two stretches of root circle and the hollow, so tracing it would thread the
            // camera's outline through the middle of the gear's.
            group(clipPathData = PathData { viewportOutsideNotch() }) {
                path(fill = SolidColor(Color.Black)) {
                    gear()
                }
            }
            path(fill = SolidColor(Color.Black)) {
                body()
                viewfinderBump()
                circle(LENS_CENTER_X, LENS_CENTER_Y, LENS_RADIUS)
            }
        }.build().also { cameraSettingsCache = it }
    }

private var cameraSettingsCache: ImageVector? = null

/**
 * Everything outside the notch: the viewport wound clockwise, then the notch wound
 * anticlockwise so a non-zero rule cancels it back out.
 *
 * Only the top and left edges cut anything — they are the two the gear reaches. The other two
 * run off the viewport, which is why the camera may be smaller than the notch.
 */
private fun PathBuilder.viewportOutsideNotch() {
    moveTo(0f, 0f)
    horizontalLineTo(IconViewportSize)
    verticalLineTo(IconViewportSize)
    horizontalLineTo(0f)
    close()

    moveTo(NOTCH_LEFT, NOTCH_TOP)
    verticalLineTo(IconViewportSize)
    horizontalLineTo(IconViewportSize)
    verticalLineTo(NOTCH_TOP)
    close()
}

/**
 * The gear, drawn whole and left for the clip above to bite into.
 *
 * Six teeth on an even 60 degree pitch from straight up. The notch claims the quadrant below
 * and right of the centre, so the third tooth never reaches the canvas and the fourth keeps
 * only what lies left of the camera.
 *
 * The outline alternates between climbing a tooth and walking the root circle to the next,
 * closing as one ring that the hollow is punched out of. The notch cuts that hollow open as
 * well, which is intended: both are empty, so they run together rather than stranding it.
 */
private fun PathBuilder.gear() {
    val start = FIRST_TOOTH_ANGLE - TOOTH_ROOT_HALF_ANGLE
    moveTo(gearX(GEAR_ROOT_RADIUS, start), gearY(GEAR_ROOT_RADIUS, start))
    repeat(TOOTH_COUNT) { index ->
        val toothAngle = FIRST_TOOTH_ANGLE + index * TOOTH_PITCH
        tooth(toothAngle)

        // Across the daylight to where the next tooth starts.
        val gapEnd = toothAngle + TOOTH_PITCH - TOOTH_ROOT_HALF_ANGLE
        sweepTo(
            GEAR_ROOT_RADIUS,
            gearX(GEAR_ROOT_RADIUS, gapEnd),
            gearY(GEAR_ROOT_RADIUS, gapEnd),
        )
    }
    close()
    circle(GEAR_CENTER_X, GEAR_CENTER_Y, GEAR_HOLLOW_RADIUS)
}

/**
 * One tooth, entered and left on the root circle. The flanks are parallel — see
 * [TOOTH_TIP_HALF_ANGLE] — so it is a square-sided prong with both crown corners rounded.
 */
private fun PathBuilder.tooth(toothAngle: Float) {
    val leadingRoot = toothAngle - TOOTH_ROOT_HALF_ANGLE
    val leadingTip = toothAngle - TOOTH_TIP_HALF_ANGLE
    val trailingTip = toothAngle + TOOTH_TIP_HALF_ANGLE
    val trailingRoot = toothAngle + TOOTH_ROOT_HALF_ANGLE
    val leadTipX = gearX(GEAR_TIP_RADIUS, leadingTip)
    val leadTipY = gearY(GEAR_TIP_RADIUS, leadingTip)
    val trailTipX = gearX(GEAR_TIP_RADIUS, trailingTip)
    val trailTipY = gearY(GEAR_TIP_RADIUS, trailingTip)

    // Up the leading flank, stopping short of the crown so the corner can be rounded.
    lineTo(
        flankCorner(leadTipX, gearX(GEAR_ROOT_RADIUS, leadingRoot)),
        flankCorner(leadTipY, gearY(GEAR_ROOT_RADIUS, leadingRoot)),
    )
    quadTo(
        leadTipX,
        leadTipY,
        gearX(GEAR_TIP_RADIUS, leadingTip + TOOTH_CORNER_ANGLE),
        gearY(GEAR_TIP_RADIUS, leadingTip + TOOTH_CORNER_ANGLE),
    )

    // Across the crown, then back down the trailing flank the way we came up.
    sweepTo(
        GEAR_TIP_RADIUS,
        gearX(GEAR_TIP_RADIUS, trailingTip - TOOTH_CORNER_ANGLE),
        gearY(GEAR_TIP_RADIUS, trailingTip - TOOTH_CORNER_ANGLE),
    )
    quadTo(
        trailTipX,
        trailTipY,
        flankCorner(trailTipX, gearX(GEAR_ROOT_RADIUS, trailingRoot)),
        flankCorner(trailTipY, gearY(GEAR_ROOT_RADIUS, trailingRoot)),
    )
    lineTo(gearX(GEAR_ROOT_RADIUS, trailingRoot), gearY(GEAR_ROOT_RADIUS, trailingRoot))
}

/** The camera body, traced clockwise from its top-left corner. */
private fun PathBuilder.body() {
    moveTo(BODY_LEFT + BODY_CORNER_RADIUS, BODY_TOP)
    horizontalLineTo(BODY_RIGHT - BODY_CORNER_RADIUS)
    sweepTo(BODY_CORNER_RADIUS, BODY_RIGHT, BODY_TOP + BODY_CORNER_RADIUS)
    verticalLineTo(BODY_BOTTOM - BODY_CORNER_RADIUS)
    sweepTo(BODY_CORNER_RADIUS, BODY_RIGHT - BODY_CORNER_RADIUS, BODY_BOTTOM)
    horizontalLineTo(BODY_LEFT + BODY_CORNER_RADIUS)
    sweepTo(BODY_CORNER_RADIUS, BODY_LEFT, BODY_BOTTOM - BODY_CORNER_RADIUS)
    verticalLineTo(BODY_TOP + BODY_CORNER_RADIUS)
    sweepTo(BODY_CORNER_RADIUS, BODY_LEFT + BODY_CORNER_RADIUS, BODY_TOP)
    close()
}

/**
 * The viewfinder bump: a trapezoid, narrower at the crown, with its base sunk [BUMP_OVERLAP]
 * into the body. Both are wound the same way, so a non-zero fill unions them and the buried
 * edge leaves no seam.
 */
private fun PathBuilder.viewfinderBump() {
    // The flanks are the same slope mirrored, so one cut serves both corners.
    val flankRun = BUMP_CROWN_LEFT - BUMP_BASE_LEFT
    val flankLength = hypot(flankRun, BUMP_HEIGHT)
    val cutX = BUMP_CORNER_CUT * flankRun / flankLength
    val cutY = BUMP_CORNER_CUT * BUMP_HEIGHT / flankLength

    moveTo(BUMP_BASE_LEFT, BODY_TOP + BUMP_OVERLAP)
    lineTo(BUMP_CROWN_LEFT - cutX, BUMP_TOP + cutY)
    quadTo(BUMP_CROWN_LEFT, BUMP_TOP, BUMP_CROWN_LEFT + BUMP_CORNER_CUT, BUMP_TOP)
    lineTo(BUMP_CROWN_RIGHT - BUMP_CORNER_CUT, BUMP_TOP)
    quadTo(BUMP_CROWN_RIGHT, BUMP_TOP, BUMP_CROWN_RIGHT + cutX, BUMP_TOP + cutY)
    lineTo(BUMP_BASE_RIGHT, BODY_TOP + BUMP_OVERLAP)
    close()
}

/** A circle as two half arcs, wound anticlockwise so a non-zero fill reads it as a hole. */
private fun PathBuilder.circle(
    centerX: Float,
    centerY: Float,
    radius: Float,
) {
    moveTo(centerX + radius, centerY)
    sweepTo(radius, centerX - radius, centerY, clockwise = false)
    sweepTo(radius, centerX + radius, centerY, clockwise = false)
    close()
}

/**
 * An arc of a true circle — the only kind this glyph draws. [clockwise] decides whether a
 * closed run of arcs reads as solid or as a hole under a non-zero fill.
 */
private fun PathBuilder.sweepTo(
    radius: Float,
    x: Float,
    y: Float,
    clockwise: Boolean = true,
) = arcTo(
    horizontalEllipseRadius = radius,
    verticalEllipseRadius = radius,
    theta = 0f,
    isMoreThanHalf = false,
    isPositiveArc = clockwise,
    x1 = x,
    y1 = y,
)

// One coordinate of the point where a flank stops so the crown corner can be rounded.
private fun flankCorner(
    tip: Float,
    root: Float,
) = tip + (root - tip) * FLANK_CORNER_FRACTION

// Angles run clockwise from three o'clock, so -90 is straight up.
private fun gearX(
    radius: Float,
    angle: Float,
) = GEAR_CENTER_X + radius * cos(angle * DEGREES_TO_RADIANS)

private fun gearY(
    radius: Float,
    angle: Float,
) = GEAR_CENTER_Y + radius * sin(angle * DEGREES_TO_RADIANS)

private const val HALF_TURN = 180f
private val DEGREES_TO_RADIANS = PI.toFloat() / HALF_TURN

// The glyph is symmetric about this point — the teeth at 150 and 210 mirror the one at -30,
// and the clipped bottom tooth all but mirrors the top — so centring the gear centres the
// whole icon. It sits on the viewport centre for exactly that reason.
private const val GEAR_CENTER_X = 12f
private const val GEAR_CENTER_Y = 12f

// Tip radius sizes the icon, so depth is changed by moving the root circle instead: the span
// between the two is how far the teeth stand out.
private const val GEAR_TIP_RADIUS = 8.9f
private const val GEAR_ROOT_RADIUS = 6.5f

// Sized so the band carrying the teeth stays substantial.
private const val GEAR_HOLLOW_RADIUS = 2.2f

private const val TOOTH_COUNT = 6
private const val TOOTH_PITCH = 360f / TOOTH_COUNT
private const val FIRST_TOOTH_ANGLE = -90f

// Half-width where a tooth meets the root circle — the value that shapes the gear, since it
// is what closes the gaps. Half the 60 degree pitch is the ceiling: at 30 a tooth would run
// into its neighbour. At 26 the teeth take 84% of the pitch, leaving 8 degrees of daylight.
private const val TOOTH_ROOT_HALF_ANGLE = 26f

// Not a free choice: the half-width at the tip is whatever makes the flanks parallel, so
// GEAR_TIP_RADIUS * sin(tip) == GEAR_ROOT_RADIUS * sin(root). Derived rather than written
// down because a stale value silently splays the teeth and still compiles.
private val TOOTH_TIP_HALF_ANGLE =
    asin(GEAR_ROOT_RADIUS * sin(TOOTH_ROOT_HALF_ANGLE * DEGREES_TO_RADIANS) / GEAR_TIP_RADIUS) /
        DEGREES_TO_RADIANS

// How far back down the flank the rounding at the crown corner reaches.
private const val TOOTH_CORNER_CUT = 0.28f

// The same distance round the crown, where it must be an angle: an angle covers
// radius * angle of arc, so this is the only value that keeps the corner's two arms equal.
// Let the crown arm outgrow the flank one and the corner domes over.
private val TOOTH_CORNER_ANGLE = TOOTH_CORNER_CUT / GEAR_TIP_RADIUS / DEGREES_TO_RADIANS

// Every flank is the same length, so the fraction of it the corner eats is constant too.
private val FLANK_LENGTH = hypot(
    GEAR_TIP_RADIUS * cos(TOOTH_TIP_HALF_ANGLE * DEGREES_TO_RADIANS) -
        GEAR_ROOT_RADIUS * cos(TOOTH_ROOT_HALF_ANGLE * DEGREES_TO_RADIANS),
    GEAR_TIP_RADIUS * sin(TOOTH_TIP_HALF_ANGLE * DEGREES_TO_RADIANS) -
        GEAR_ROOT_RADIUS * sin(TOOTH_ROOT_HALF_ANGLE * DEGREES_TO_RADIANS),
)
private val FLANK_CORNER_FRACTION = TOOTH_CORNER_CUT / FLANK_LENGTH

// The notch, and the gap it holds the gear off the camera by. Its corner is the gear's own
// centre, so the body's left edge falls on the centre line and the crown clears it by the gap.
private const val CLEARANCE = 0.6f
private const val NOTCH_LEFT = GEAR_CENTER_X - CLEARANCE
private const val NOTCH_TOP = GEAR_CENTER_Y

// The same [CLEARANCE] boxes the camera in on its three occupied sides — notch above and
// left, gear's lowest reach below — pinning every edge but the right, which follows from the
// height via BODY_ASPECT. So the badge resizes as one piece and the gaps stay even.
private const val BUMP_HEIGHT = 0.6f
private const val BUMP_TOP = NOTCH_TOP + CLEARANCE

private const val BODY_LEFT = NOTCH_LEFT + CLEARANCE
private const val BODY_TOP = BUMP_TOP + BUMP_HEIGHT
private const val BODY_BOTTOM = GEAR_CENTER_Y + GEAR_TIP_RADIUS - CLEARANCE

// Width follows height, so the body keeps its shape whatever the pinned edges do to it.
// Squarer than the source glyph's 1.4, which at this height overhung the gear on the right.
private const val BODY_ASPECT = 1.2f
private const val BODY_WIDTH = (BODY_BOTTOM - BODY_TOP) * BODY_ASPECT
private const val BODY_RIGHT = BODY_LEFT + BODY_WIDTH
private const val BODY_CORNER_RADIUS = 0.65f

// Bump corners inset from the body's edges as fractions of its width, so the taper survives
// any rescale of the body.
private const val BUMP_BASE_INSET = 0.3f
private const val BUMP_CROWN_INSET = 0.39f
private const val BUMP_BASE_LEFT = BODY_LEFT + BUMP_BASE_INSET * BODY_WIDTH
private const val BUMP_BASE_RIGHT = BODY_RIGHT - BUMP_BASE_INSET * BODY_WIDTH
private const val BUMP_CROWN_LEFT = BODY_LEFT + BUMP_CROWN_INSET * BODY_WIDTH
private const val BUMP_CROWN_RIGHT = BODY_RIGHT - BUMP_CROWN_INSET * BODY_WIDTH
private const val BUMP_CORNER_CUT = 0.35f
private const val BUMP_OVERLAP = 0.4f

private const val LENS_CENTER_X = (BODY_LEFT + BODY_RIGHT) / 2f
private const val LENS_CENTER_Y = (BODY_TOP + BODY_BOTTOM) / 2f

// Sized so the body does not look punched through.
private const val LENS_RADIUS = 1.25f

@Preview(name = "CameraSettings", showBackground = true)
@Composable
private fun CameraSettingsPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.CameraSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
