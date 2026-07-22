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
import kotlin.math.sqrt

val Icons.FlashOff: ImageVector
    get() {
        flashOffCache?.let { return it }
        return ImageVector.Builder(
            name = "FlashOff",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            // The slash grows out of one half of the bolt and stands clear of the other, as
            // no-flash.svg draws it: welded along the apex half, a clean break at the foot
            // half. Held off both, the bolt would read as sliced in two rather than struck
            // through, and the two halves are cut on different edges because of it.
            attachedHalf()
            severedHalf()
            slash()
        }.build().also { flashOffCache = it }
    }

private var flashOffCache: ImageVector? = null

/**
 * The half the slash grows out of: up the stem to the apex, down the long diagonal, out to the
 * tip and back along the ledge, round the elbow and onto the far stem.
 *
 * Both ends run to where the slash's centerline crosses the stem they are on, which is as far
 * as they can go — a round cap sitting on that centerline is exactly the slash's own half
 * stroke in every direction, so the slash swallows it whole and the two read as one shape.
 *
 * [FlashOn] draws the same bolt whole. The two are a toggle pair and share every constant down
 * to the stroke, so a change to one belongs in both or the bolt jumps as the button flips.
 */
private fun ImageVector.Builder.attachedHalf() =
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(UPPER_STEM_X, SLASH_ON_UPPER_STEM_Y)
        lineTo(UPPER_STEM_X, APEX_Y)
        lineTo(LOWER_TIP_X, LOWER_ELBOW_Y)
        lineTo(LOWER_STEM_X, LOWER_ELBOW_Y)
        lineTo(LOWER_STEM_X, SLASH_ON_LOWER_STEM_Y)
    }

/**
 * The half the slash cuts free: a stub of ledge out to the tip, the long diagonal down to the
 * foot, and back up the stem to the second cut. Its elbow goes with the channel, which is why
 * this half is a corner shorter than the one the slash is welded to.
 */
private fun ImageVector.Builder.severedHalf() =
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(LEDGE_CUT_X, UPPER_ELBOW_Y)
        lineTo(UPPER_TIP_X, UPPER_ELBOW_Y)
        lineTo(LOWER_STEM_X, FOOT_Y)
        lineTo(LOWER_STEM_X, STEM_CUT_Y)
    }

/** The bar of the negation, corner to corner of the live area through the glyph center. */
private fun ImageVector.Builder.slash() =
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
    ) {
        moveTo(SLASH_LOW, SLASH_HIGH)
        lineTo(SLASH_HIGH, SLASH_LOW)
    }

private const val STROKE_WIDTH = 1.75f
private const val HALF_STROKE = STROKE_WIDTH / 2f

// The bolt, as FlashOn draws it: an open outline form taken to the 20x20 live area, its apex
// and foot points held a half stroke inside so their round joins land on the edge.
private const val LIVE_AREA_TOP = 2f
private const val APEX_Y = LIVE_AREA_TOP + HALF_STROKE
private const val FOOT_Y = IconViewportSize - APEX_Y

private const val UPPER_STEM_X = 14.375f
private const val LOWER_STEM_X = IconViewportSize - UPPER_STEM_X

private const val LEAN = UPPER_STEM_X - LOWER_STEM_X
private const val UPPER_TIP_X = UPPER_STEM_X + LEAN
private const val LOWER_TIP_X = IconViewportSize - UPPER_TIP_X

private const val UPPER_ELBOW_Y = 11f
private const val LOWER_ELBOW_Y = IconViewportSize - UPPER_ELBOW_Y

// The slash's caps are held off the trim by the same half stroke as the bolt's points. It
// reaches the live area corner to corner, so both ends carry that inset on each axis rather
// than sitting on 2 and 22 outright.
private const val SLASH_LOW = LIVE_AREA_TOP + HALF_STROKE
private const val SLASH_HIGH = IconViewportSize - SLASH_LOW

// Every point of the slash's centerline sums to this, which is what makes the crossings and
// the cuts below arithmetic rather than geometry.
private const val SLASH_SUM = SLASH_LOW + SLASH_HIGH

// The centerline meets the bolt in exactly two places, one on each stem: the ledges lie wholly
// to one side of it and the diagonals never approach it. So these two are the whole of what
// the attached half runs to, and the elbows are what the severed half loses.
private const val SLASH_ON_UPPER_STEM_Y = SLASH_SUM - UPPER_STEM_X
private const val SLASH_ON_LOWER_STEM_Y = SLASH_SUM - LOWER_STEM_X

// Daylight left on the severed side, the gap being the only thing that says the slash lies
// over that half rather than growing out of it too. Both strokes spend a half on their own
// edge first, so its cuts sit this far out from the centerline.
private const val SLASH_GAP = 0.5f
private const val CUT_CLEARANCE = HALF_STROKE + SLASH_GAP + HALF_STROKE

// Measured down an axis instead of across the slash, which at 45 degrees costs a root two.
private val CUT_SUM = SLASH_SUM + CUT_CLEARANCE * sqrt(2f)

// Pulling back that far carries the cut past the elbow and onto the ledge, so the severed half
// starts along the ledge rather than on the stem the crossing is on.
private val LEDGE_CUT_X = CUT_SUM - UPPER_ELBOW_Y
private val STEM_CUT_Y = CUT_SUM - LOWER_STEM_X

@Preview(name = "FlashOff", showBackground = true)
@Composable
private fun FlashOffPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.FlashOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
