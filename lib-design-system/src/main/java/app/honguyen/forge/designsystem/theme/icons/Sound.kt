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
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.IconDefaultSize
import app.honguyen.forge.designsystem.theme.IconViewportSize
import app.honguyen.forge.designsystem.theme.Icons
import kotlin.math.sqrt

val Icons.Sound: ImageVector
    get() {
        soundCache?.let { return it }
        return ImageVector.Builder(
            name = "Sound",
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
                // Up the left stem, on through the beam's left end without a break, across the
                // upper rule and down the right stem, turning both corners at the top of the
                // glyph over a fillet. Each stem stops at its note's center, where the head's
                // own stroke carries on from it.
                moveTo(LEFT_STEM_X, LEFT_NOTE_CENTER_Y)
                lineTo(LEFT_STEM_X, LEFT_CORNER_STEM_Y)
                cornerTo(LEFT_CORNER_RULE_X, LEFT_CORNER_RULE_Y)
                lineTo(RIGHT_CORNER_RULE_X, RIGHT_CORNER_RULE_Y)
                cornerTo(RIGHT_STEM_X, RIGHT_CORNER_STEM_Y)
                lineTo(RIGHT_STEM_X, RIGHT_NOTE_CENTER_Y)

                // The lower rule is a run of its own: it meets both stems side-on, so there is
                // no corner to turn at either end and its caps die inside their stroke.
                moveTo(LEFT_STEM_X, BEAM_LOWER_LEFT_Y)
                lineTo(RIGHT_STEM_X, BEAM_LOWER_RIGHT_Y)

                noteHead(LEFT_NOTE_CENTER_X, LEFT_NOTE_CENTER_Y)
                noteHead(RIGHT_NOTE_CENTER_X, RIGHT_NOTE_CENTER_Y)
            }
        }.build().also { soundCache = it }
    }

private var soundCache: ImageVector? = null

/**
 * One note head, as two half arcs from the point its stem lands on. A head is tangent to its
 * stem, so that point is the head's equator and the two strokes meet without crossing.
 */
private fun PathBuilder.noteHead(
    centerX: Float,
    centerY: Float,
) {
    moveTo(centerX + NOTE_RADIUS, centerY)
    halfCircleTo(centerX - NOTE_RADIUS, centerY)
    halfCircleTo(centerX + NOTE_RADIUS, centerY)
    close()
}

private fun PathBuilder.halfCircleTo(
    x: Float,
    y: Float,
) = arcTo(
    horizontalEllipseRadius = NOTE_RADIUS,
    verticalEllipseRadius = NOTE_RADIUS,
    theta = 0f,
    isMoreThanHalf = false,
    isPositiveArc = true,
    x1 = x,
    y1 = y,
)

/**
 * One of the beam's top corners. Both turn clockwise, the pen going up the left stem, right
 * along the beam and down the right stem.
 */
private fun PathBuilder.cornerTo(
    x: Float,
    y: Float,
) = arcTo(
    horizontalEllipseRadius = CORNER_RADIUS,
    verticalEllipseRadius = CORNER_RADIUS,
    theta = 0f,
    isMoreThanHalf = false,
    isPositiveArc = true,
    x1 = x,
    y1 = y,
)

/**
 * How far back from a corner its fillet starts, measured along either leg. A fillet leaves
 * both legs tangentially, so the tighter the corner the further back it has to begin;
 * [cosLegAngle] is the cosine of the angle the two legs make at the vertex.
 */
private fun cornerTangent(cosLegAngle: Float): Float {
    val sinLegAngle = sqrt(1f - cosLegAngle * cosLegAngle)
    return CORNER_RADIUS * (1f + cosLegAngle) / sinLegAngle
}

private const val STROKE_WIDTH = 1.75f
private const val HALF_STROKE = STROKE_WIDTH / 2f
private const val GLYPH_CENTER = IconViewportSize / 2f

// An open outline form, so the glyph is drawn to the 20x20 live area rather than held to the
// 18 square keyline.
private const val LIVE_AREA_TOP = 2f

// Carried over from double-note.svg, whose heads run a little over two fifths of the span
// between the stems across. At this radius the counter inside a head is 3.3 wide against a
// 1.75 wall, so the head still reads as a ring rather than a blob at 24dp.
private const val NOTE_RADIUS = 2.51f

// Stem to stem, which is also head center to head center — a head is tangent to its own stem.
// Both ends of the glyph stand a radius out from a head center, so centering the two centers
// on the viewport centers the ink with them.
private const val NOTE_SPACING = 11.47f
private const val LEFT_NOTE_CENTER_X = GLYPH_CENTER - NOTE_SPACING / 2f
private const val RIGHT_NOTE_CENTER_X = GLYPH_CENTER + NOTE_SPACING / 2f
private const val LEFT_STEM_X = LEFT_NOTE_CENTER_X + NOTE_RADIUS
private const val RIGHT_STEM_X = RIGHT_NOTE_CENTER_X + NOTE_RADIUS

// The beam's fall per unit across, the source's own 1 in 4. Steep enough to read as a beam
// rather than a bracket, shallow enough that its rules stay clear of the right note.
private const val BEAM_SLOPE = 0.25f

// A step along a rule, split into its across and down parts. The rules rise to the right, so
// the second is negative.
private val RULE_UNIT_X = 1f / sqrt(1f + BEAM_SLOPE * BEAM_SLOPE)
private val RULE_UNIT_Y = -BEAM_SLOPE * RULE_UNIT_X

// The two corners at the top of the glyph are rounded well past what the stroke's own join
// would give. double-note.svg turns them over 3 outside and 1 in, which is a centerline
// radius of 2; scaled onto this glyph that is 1.43.
private const val CORNER_RADIUS = 1.43f

// The right fillet cuts its vertex away, so the highest ink on the glyph is that arc's crown
// rather than the corner it replaces — this is how far below the vertex the crown sits, and
// fitting the glyph to the live area means fitting that instead.
private val CORNER_CROWN_DROP = CORNER_RADIUS * (BEAM_SLOPE + 1f / RULE_UNIT_X - 1f)

// Crown of the right corner and underside of the left note: the only two points that reach
// the live area, each held a half stroke inside it so its own stroke lands on the edge.
private val BEAM_UPPER_RIGHT_Y = LIVE_AREA_TOP + HALF_STROKE - CORNER_CROWN_DROP
private val BEAM_UPPER_LEFT_Y = BEAM_UPPER_RIGHT_Y + BEAM_SLOPE * NOTE_SPACING
private const val LEFT_NOTE_BOTTOM_Y = IconViewportSize - (LIVE_AREA_TOP + HALF_STROKE)

// How much higher the right head hangs than the left. The beam falls faster than this over
// the same span, which is the whole reason the two stems are visibly different lengths.
private const val NOTE_RISE = 2.15f
private const val LEFT_NOTE_CENTER_Y = LEFT_NOTE_BOTTOM_Y - NOTE_RADIUS
private const val RIGHT_NOTE_CENTER_Y = LEFT_NOTE_CENTER_Y - NOTE_RISE

// Rule to rule down the page. Measured this way rather than across the beam because that is
// how it is drawn; square to the rules it is 3.49, so 1.74 of daylight.
private const val BEAM_DEPTH = 3.6f
private val BEAM_LOWER_LEFT_Y = BEAM_UPPER_LEFT_Y + BEAM_DEPTH
private val BEAM_LOWER_RIGHT_Y = BEAM_UPPER_RIGHT_Y + BEAM_DEPTH

// The stem stands square to the page and the rule leans, so the two corners are not the same
// angle and cannot share a tangent: the left opens to 104 degrees and starts its turn 1.12
// back, the right closes to 76 and has to start 1.83 back.
private val LEFT_CORNER_TANGENT = cornerTangent(RULE_UNIT_Y)
private val RIGHT_CORNER_TANGENT = cornerTangent(-RULE_UNIT_Y)

private val LEFT_CORNER_STEM_Y = BEAM_UPPER_LEFT_Y + LEFT_CORNER_TANGENT
private val LEFT_CORNER_RULE_X = LEFT_STEM_X + LEFT_CORNER_TANGENT * RULE_UNIT_X
private val LEFT_CORNER_RULE_Y = BEAM_UPPER_LEFT_Y + LEFT_CORNER_TANGENT * RULE_UNIT_Y

private val RIGHT_CORNER_RULE_X = RIGHT_STEM_X - RIGHT_CORNER_TANGENT * RULE_UNIT_X
private val RIGHT_CORNER_RULE_Y = BEAM_UPPER_RIGHT_Y - RIGHT_CORNER_TANGENT * RULE_UNIT_Y
private val RIGHT_CORNER_STEM_Y = BEAM_UPPER_RIGHT_Y + RIGHT_CORNER_TANGENT

@Preview(name = "Sound", showBackground = true)
@Composable
private fun SoundPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.Sound,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
