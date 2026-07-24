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
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

val Icons.Filter: ImageVector
    get() {
        filterCache?.let { return it }
        return ImageVector.Builder(
            name = "Filter",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                // Every end in this glyph is a ring disappearing behind another, and an occlusion
                // is cut square. A round cap would round the end off inside the covering band and
                // read as a terminal — the ring stopping rather than passing behind.
                strokeLineCap = StrokeCap.Butt,
            ) {
                ring(TOP_CENTER_X, TOP_CENTER_Y, UNDER_RIGHT_BEARING)
                ring(LEFT_CENTER_X, LEFT_CENTER_Y, UNDER_TOP_BEARING)
                ring(RIGHT_CENTER_X, RIGHT_CENTER_Y, UNDER_LEFT_BEARING)
            }
        }.build().also { filterCache = it }
    }

private var filterCache: ImageVector? = null

/**
 * One ring, less the whole arc of it that falls inside the ring it passes behind.
 *
 * [bearing] aims at that ring's center, so the buried arc is the [CROSSING_ANGLE] either side of
 * it and what remains is the rest of the turn. Both ends land on the covering ring's centerline,
 * which is the far side of its band from the approaching arc, so the cut is buried under half a
 * stroke of ink and no clearance has to be spent on it.
 *
 * Split into [ARC_SEGMENTS]: what is left is most of a full turn, and no single sweep may pass a
 * half one.
 */
private fun PathBuilder.ring(
    centerX: Float,
    centerY: Float,
    bearing: Float,
) {
    val from = bearing + CROSSING_ANGLE
    val step = (FULL_TURN - 2f * CROSSING_ANGLE) / ARC_SEGMENTS

    moveTo(ringX(centerX, from), ringY(centerY, from))
    repeat(ARC_SEGMENTS.toInt()) { index ->
        val angle = from + (index + 1) * step
        sweepTo(ringX(centerX, angle), ringY(centerY, angle))
    }
}

/** An arc of a true circle — every curve here belongs to one of the three rings. */
private fun PathBuilder.sweepTo(
    x: Float,
    y: Float,
) = arcTo(
    horizontalEllipseRadius = RING_RADIUS,
    verticalEllipseRadius = RING_RADIUS,
    theta = 0f,
    isMoreThanHalf = false,
    isPositiveArc = true,
    x1 = x,
    y1 = y,
)

// Angles run clockwise from three o'clock, so -90 is straight up.
private fun ringX(
    centerX: Float,
    angle: Float,
) = centerX + RING_RADIUS * cos(angle * DEGREES_TO_RADIANS)

private fun ringY(
    centerY: Float,
    angle: Float,
) = centerY + RING_RADIUS * sin(angle * DEGREES_TO_RADIANS)

// A busier glyph than the rest of the set — three rings lapping each other — so it takes the
// lighter of the two pens in use, which is also the weight rgb-print.svg draws it at.
private const val STROKE_WIDTH = 1.5f

private const val HALF_TURN = 180f
private const val FULL_TURN = 360f

// Enough to keep every sweep under a half turn at any spacing: what survives of a ring is 303
// degrees at the spacing below and approaches a full turn as the rings come apart, so thirds
// top out at 120.
private const val ARC_SEGMENTS = 3f
private val DEGREES_TO_RADIANS = PI.toFloat() / HALF_TURN

// How far apart the centers sit, as a fraction of a ring's diameter: at 1 the rings are tangent
// and at 0 they coincide. rgb-print.svg draws them at 0.648, which buries each ring deep in its
// neighbors and leaves the three of them reading as one knot. At 0.88 they lap each other by
// 1.18, which is enough for the order to be legible and little enough to still read as three
// circles. Push it much further and the buried arc shrinks past noticing: at 0.88 it is 57
// degrees of the turn, and it goes to nothing as the rings come apart.
private const val RING_SPACING = 0.88f

// The trio is an open outline form, so it fills the 20 of live area. Width is the binding
// dimension — three circles on an equilateral triangle stand wider than they are tall — and it
// comes to one center span plus a diameter plus the stroke the two outer edges add.
private const val LIVE_AREA = 20f
private const val RING_RADIUS = (LIVE_AREA - STROKE_WIDTH) / (2f * (RING_SPACING + 1f))
private const val CENTER_SPAN = 2f * RING_SPACING * RING_RADIUS

// The triangle is equilateral, so its height is a root three over two of the span. Split evenly
// above and below the glyph center rather than about the triangle's own centroid, which sits
// lower: it is the ink that has to center on 12, not the centers.
private val RING_OFFSET_Y = CENTER_SPAN * sqrt(3f) / 4f
private const val GLYPH_CENTER = IconViewportSize / 2f

private const val TOP_CENTER_X = GLYPH_CENTER
private val TOP_CENTER_Y = GLYPH_CENTER - RING_OFFSET_Y
private const val LEFT_CENTER_X = GLYPH_CENTER - CENTER_SPAN / 2f
private val LEFT_CENTER_Y = GLYPH_CENTER + RING_OFFSET_Y
private const val RIGHT_CENTER_X = GLYPH_CENTER + CENTER_SPAN / 2f
private val RIGHT_CENTER_Y = GLYPH_CENTER + RING_OFFSET_Y

// The order is a cycle, not a stack: top over left, left over right, right over top. No ring is
// wholly above or below another, so no arrangement in depth could produce it — which is what
// makes three flat circles read as threaded through one another. Each bearing points from a ring
// at the one it passes behind, and so at the arc of itself that is dropped.
private const val UNDER_RIGHT_BEARING = 60f
private const val UNDER_TOP_BEARING = -60f
private const val UNDER_LEFT_BEARING = HALF_TURN

// Half the arc one ring buries in its neighbor, measured at that neighbor's center. Two radii and
// the span between the centers make an isosceles triangle, so it falls out of the spacing alone —
// which is why the spacing is the only number this glyph is really shaped by.
private val CROSSING_ANGLE = acos(RING_SPACING) / DEGREES_TO_RADIANS

@Preview(name = "Filter", showBackground = true)
@Composable
private fun FilterPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.Filter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
