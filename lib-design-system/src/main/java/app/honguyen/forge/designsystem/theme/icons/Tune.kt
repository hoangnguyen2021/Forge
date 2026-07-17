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
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.IconDefaultSize
import app.honguyen.forge.designsystem.theme.IconViewportSize
import app.honguyen.forge.designsystem.theme.Icons

val Icons.Tune: ImageVector
    get() {
        tuneCache?.let { return it }
        return ImageVector.Builder(
            name = "Tune",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Top and bottom bars ride their right-hand run, the middle one its left, so
                // the cuts stagger rather than stacking into a seam down the glyph.
                slider(trackY = TOP_TRACK_Y, barX = TOP_BAR_X, barRidesRightRun = true)
                slider(trackY = MIDDLE_TRACK_Y, barX = MIDDLE_BAR_X, barRidesRightRun = false)
                slider(trackY = BOTTOM_TRACK_Y, barX = BOTTOM_BAR_X, barRidesRightRun = true)
            }
        }.build().also { tuneCache = it }
    }

private var tuneCache: ImageVector? = null

/**
 * One slider: a track cut either side of its bar, spanning the full live area.
 *
 * The run the bar rides ends under it rather than at its edge. That overlap is what fuses
 * them: every rect here is wound the same way, so a non-zero fill unions them and there is
 * no fused outline to trace. The opposite run stops a gap short and stays separate.
 */
private fun PathBuilder.slider(
    trackY: Float,
    barX: Float,
    barRidesRightRun: Boolean,
) {
    val trackTop = trackY - HALF_THICKNESS
    val trackBottom = trackY + HALF_THICKNESS
    val leftRunEnd = if (barRidesRightRun) barX - TRACK_BAR_CLEARANCE else barX
    val rightRunStart = if (barRidesRightRun) barX else barX + TRACK_BAR_CLEARANCE

    roundedRect(
        left = LIVE_AREA_START,
        top = trackTop,
        right = leftRunEnd,
        bottom = trackBottom,
    )
    roundedRect(
        left = rightRunStart,
        top = trackTop,
        right = LIVE_AREA_END,
        bottom = trackBottom,
    )
    roundedRect(
        left = barX - HALF_THICKNESS,
        top = trackY - BAR_HALF_EXTENT,
        right = barX + HALF_THICKNESS,
        bottom = trackY + BAR_HALF_EXTENT,
    )
}

/** Traces a [CORNER_RADIUS]-rounded rect clockwise from its top-left corner. */
private fun PathBuilder.roundedRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
) {
    moveTo(left + CORNER_RADIUS, top)
    lineTo(right - CORNER_RADIUS, top)
    cornerTo(x = right, y = top + CORNER_RADIUS)
    lineTo(right, bottom - CORNER_RADIUS)
    cornerTo(x = right - CORNER_RADIUS, y = bottom)
    lineTo(left + CORNER_RADIUS, bottom)
    cornerTo(x = left, y = bottom - CORNER_RADIUS)
    lineTo(left, top + CORNER_RADIUS)
    cornerTo(x = left + CORNER_RADIUS, y = top)
    close()
}

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

private const val THICKNESS = 2f
private const val HALF_THICKNESS = THICKNESS / 2f

// Half of a fully round end, which at this thickness would be 1. Carries over tune.svg's
// own ratio, where 17.1 corners sit on a 67.35 thick track.
private const val CORNER_RADIUS = 0.5f

// The 20x20 the rest of the set draws within, inset from the 24x24 viewport. Filled rects
// end where they are told, so unlike a round cap nothing overhangs these.
private const val LIVE_AREA_START = 2f
private const val LIVE_AREA_END = 22f

// How far the bars reach from their track: 3.5 either side puts the top and bottom ones on
// the live area, the same as the track ends.
private const val BAR_HALF_EXTENT = 3.5f

private const val TOP_TRACK_Y = 5.5f
private const val MIDDLE_TRACK_Y = 12f
private const val BOTTOM_TRACK_Y = 18.5f

private const val TOP_BAR_X = 17f
private const val MIDDLE_BAR_X = 7f
private const val BOTTOM_BAR_X = 12f

// Half a thickness to clear the bar's edge, then the visible gap.
private const val TRACK_BAR_GAP = 0.7f
private const val TRACK_BAR_CLEARANCE = HALF_THICKNESS + TRACK_BAR_GAP

@Preview(name = "Tune", showBackground = true)
@Composable
private fun TunePreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
