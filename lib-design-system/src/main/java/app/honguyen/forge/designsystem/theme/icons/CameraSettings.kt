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
            // The gear is drawn whole and then clipped, rather than traced with the notch
            // already in it: the cut runs through a tooth, two stretches of root circle and
            // the hollow, so tracing it would thread the notch through the middle of the
            // gear's own outline. Clipping states it once, as the rectangle it actually is.
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
 * Everything outside the notch: the whole viewport wound clockwise, then the notch wound
 * anticlockwise so a non-zero rule cancels it back out.
 *
 * Only the notch's top and left edges do any cutting — they are the two the gear reaches. The
 * other two run off the bottom-right of the viewport instead of wrapping the camera, since
 * there is no gear out there to cut, which is why the camera is free to be smaller than this.
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
 * The gear, drawn whole; the clip above is what lets the camera sit over its lower right.
 *
 * Six teeth on an even 60 degree pitch, starting straight up: one at the top, one at the
 * bottom and a pair down each side. The notch claims the quadrant below and right of the
 * gear's centre, so the third tooth never reaches the canvas and the fourth keeps only the
 * part of itself lying left of the camera — the same collision the source glyph settles by
 * giving up a tooth of its own.
 *
 * The outline alternates between climbing a tooth and walking the root circle across to the
 * next one, closing as a single ring, which the hollow is then punched out of.
 *
 * The notch cuts the hollow open too, since its corner sits on the gear's own centre and the
 * hollow reaches a radius past that on both axes. Nothing is stranded by it: the notch and
 * the hollow are both empty, so where they meet they simply run together, and the gear reads
 * as a ring with the same quadrant bitten out of it as the teeth.
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
 * One tooth, entered on the root circle at its leading flank and left on the root circle at
 * its trailing one. The flanks are parallel — see [TOOTH_TIP_HALF_ANGLE] — so the tooth is a
 * square-sided prong, and both corners where a flank meets the crown are rounded off.
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
 * into the body. The source glyph floats it a little clear of the body; sinking it lands it
 * on the roof instead, and closing that float is what let the badge shrink without the bump
 * growing to fill the gap. Both shapes are wound the same way, so a non-zero fill unions them
 * and the buried edge leaves no seam where they meet.
 */
private fun PathBuilder.viewfinderBump() {
    // The flanks are the same slope mirrored, so one cut serves both corners.
    val flankRun = BUMP_CROWN_LEFT - BUMP_BASE_LEFT
    val flankRise = BODY_TOP - BUMP_TOP
    val flankLength = hypot(flankRun, flankRise)
    val cutX = BUMP_CORNER_CUT * flankRun / flankLength
    val cutY = BUMP_CORNER_CUT * flankRise / flankLength

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
 * An arc of a true circle, which is the only kind this glyph draws: never an ellipse, never
 * rotated, never the long way round. [clockwise] is what decides whether a closed run of arcs
 * reads as solid or as a hole once a non-zero fill is applied.
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

// One coordinate of the point where a flank stops so the crown corner above it can be
// rounded: [FLANK_CORNER_FRACTION] of the way back from the tip corner toward the root one.
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

// Centre and tip radius carry over the source glyph's, scaled into the 20x20 live area the
// rest of the set draws within: the twelve o'clock tooth lands on it exactly.
private const val GEAR_CENTER_X = 10.9f
private const val GEAR_CENTER_Y = 10.9f

// Tip radius is what the icon is sized by, so it stays put and the root circle moves instead:
// the span between the two is how far the teeth stand out, so pulling the root in deepens
// them without pushing the gear past the live area.
private const val GEAR_TIP_RADIUS = 8.9f
private const val GEAR_ROOT_RADIUS = 7.1f

// The source glyph sits a solid disc inside this, leaving a thin ring; dropped here, so the
// hollow reads as one opening, and sized so the band carrying the teeth stays substantial.
private const val GEAR_HOLLOW_RADIUS = 2.5f

private const val TOOTH_COUNT = 6
private const val TOOTH_PITCH = 360f / TOOTH_COUNT
private const val FIRST_TOOTH_ANGLE = -90f

// Half-widths of a tooth where it meets each circle. The root angle is the wider of the two
// only because it is measured on the smaller circle: the pair is picked so the flanks come
// out parallel, GEAR_TIP_RADIUS * sin(tip) == GEAR_ROOT_RADIUS * sin(root), which is what
// squares the teeth off. Change either radius and the tip angle has to be re-solved, or the
// teeth go back to splaying or tapering.
//
// The root angle is also what closes the gaps, and half the 60 degree pitch is its ceiling —
// at 30 a tooth would run into its neighbour. At 26 the teeth take 84% of the pitch where
// they meet the root circle, leaving the 8 degrees of daylight between them.
private const val TOOTH_TIP_HALF_ANGLE = 20.47f
private const val TOOTH_ROOT_HALF_ANGLE = 26f

// How far round the crown the rounding runs, and how far back down the flank it reaches. An
// angle is an arc length at tip radius, so the two only stay in step while the angle is about
// the cut over GEAR_TIP_RADIUS in degrees — let the crown arm outgrow the flank one and the
// corner domes over instead of reading as a radius.
private const val TOOTH_CORNER_ANGLE = 1.8f
private const val TOOTH_CORNER_CUT = 0.28f

// Every flank is the same length, so the fraction of it the corner eats is constant too.
private val FLANK_LENGTH = hypot(
    GEAR_TIP_RADIUS * cos(TOOTH_TIP_HALF_ANGLE * DEGREES_TO_RADIANS) -
        GEAR_ROOT_RADIUS * cos(TOOTH_ROOT_HALF_ANGLE * DEGREES_TO_RADIANS),
    GEAR_TIP_RADIUS * sin(TOOTH_TIP_HALF_ANGLE * DEGREES_TO_RADIANS) -
        GEAR_ROOT_RADIUS * sin(TOOTH_ROOT_HALF_ANGLE * DEGREES_TO_RADIANS),
)
private val FLANK_CORNER_FRACTION = TOOTH_CORNER_CUT / FLANK_LENGTH

// The notch bitten out of the gear, and the gap it holds the gear off the camera by. Its
// corner is the gear's own centre, so the camera lands squarely in the lower-right quadrant:
// the body's left edge falls on the centre line, and the bump's crown clears it by the gap.
private const val CLEARANCE = 0.6f
private const val NOTCH_LEFT = GEAR_CENTER_X - CLEARANCE
private const val NOTCH_TOP = GEAR_CENTER_Y

// The camera is boxed in by the same [CLEARANCE] on all three sides that face something: the
// notch above and to its left, and the gear's lowest reach below. Those three pin every edge
// but the right, which then falls out of the height by way of the body's aspect — so the
// badge resizes as one piece and the gaps stay even.
private const val BUMP_HEIGHT = 1.4f
private const val BUMP_TOP = NOTCH_TOP + CLEARANCE

private const val BODY_LEFT = NOTCH_LEFT + CLEARANCE
private const val BODY_TOP = BUMP_TOP + BUMP_HEIGHT
private const val BODY_BOTTOM = GEAR_CENTER_Y + GEAR_TIP_RADIUS - CLEARANCE

// The source glyph's own proportion, which the body holds on to at any size.
private const val BODY_ASPECT = 1.4f
private const val BODY_WIDTH = (BODY_BOTTOM - BODY_TOP) * BODY_ASPECT
private const val BODY_RIGHT = BODY_LEFT + BODY_WIDTH
private const val BODY_CORNER_RADIUS = 0.65f

// How far the bump's corners sit in from the body's edges, as fractions of its width. Both
// are the source glyph's, so the bump keeps its taper whatever the body is scaled to.
private const val BUMP_BASE_INSET = 0.241f
private const val BUMP_CROWN_INSET = 0.337f
private const val BUMP_BASE_LEFT = BODY_LEFT + BUMP_BASE_INSET * BODY_WIDTH
private const val BUMP_BASE_RIGHT = BODY_RIGHT - BUMP_BASE_INSET * BODY_WIDTH
private const val BUMP_CROWN_LEFT = BODY_LEFT + BUMP_CROWN_INSET * BODY_WIDTH
private const val BUMP_CROWN_RIGHT = BODY_RIGHT - BUMP_CROWN_INSET * BODY_WIDTH
private const val BUMP_CORNER_CUT = 0.35f
private const val BUMP_OVERLAP = 0.4f

private const val LENS_CENTER_X = (BODY_LEFT + BODY_RIGHT) / 2f
private const val LENS_CENTER_Y = (BODY_TOP + BODY_BOTTOM) / 2f

// The source glyph rings this with a solid centre; dropped here, so the lens reads as one
// hole, and sized so the body does not look punched through.
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
