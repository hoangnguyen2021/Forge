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
import kotlin.math.hypot
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
 * The half the slash grows out of: up the stem and round the apex, down the long diagonal,
 * round the tip and back along the ledge, round the elbow and onto the far stem.
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
        noseAt(
            entryX = UPPER_STEM_X,
            entryY = APEX_Y + APEX_TANGENT,
            vertexX = UPPER_STEM_X,
            vertexY = APEX_Y,
            exitX = UPPER_STEM_X + APEX_DIAGONAL_X,
            exitY = APEX_Y + APEX_DIAGONAL_Y,
        )
        noseAt(
            entryX = LOWER_TIP_X - TIP_DIAGONAL_X,
            entryY = LOWER_ELBOW_Y - TIP_DIAGONAL_Y,
            vertexX = LOWER_TIP_X,
            vertexY = LOWER_ELBOW_Y,
            exitX = LOWER_TIP_X + TIP_TANGENT,
            exitY = LOWER_ELBOW_Y,
        )
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
        noseAt(
            entryX = UPPER_TIP_X - TIP_TANGENT,
            entryY = UPPER_ELBOW_Y,
            vertexX = UPPER_TIP_X,
            vertexY = UPPER_ELBOW_Y,
            exitX = UPPER_TIP_X + TIP_DIAGONAL_X,
            exitY = UPPER_ELBOW_Y + TIP_DIAGONAL_Y,
        )
        noseAt(
            entryX = LOWER_STEM_X - APEX_DIAGONAL_X,
            entryY = FOOT_Y - APEX_DIAGONAL_Y,
            vertexX = LOWER_STEM_X,
            vertexY = FOOT_Y,
            exitX = LOWER_STEM_X,
            exitY = FOOT_Y - APEX_TANGENT,
        )
        lineTo(LOWER_STEM_X, STEM_CUT_Y)
    }

/**
 * The straight leg into one of the bolt's acute corners, and the blend that carries it round.
 *
 * The leg stops at ([entryX], [entryY]) and the next one picks up at ([exitX], [exitY]);
 * ([vertexX], [vertexY]) is the corner the two would have met at, which the ink never reaches.
 * A symmetric cubic spans between them with both control points [NOSE_TAPER] of the way from
 * the vertex back toward the leg they belong to. See [FlashOn] for why the corners are blended
 * rather than left to the join.
 */
private fun PathBuilder.noseAt(
    entryX: Float,
    entryY: Float,
    vertexX: Float,
    vertexY: Float,
    exitX: Float,
    exitY: Float,
) {
    lineTo(entryX, entryY)
    curveTo(
        x1 = vertexX + (entryX - vertexX) * NOSE_TAPER,
        y1 = vertexY + (entryY - vertexY) * NOSE_TAPER,
        x2 = vertexX + (exitX - vertexX) * NOSE_TAPER,
        y2 = vertexY + (exitY - vertexY) * NOSE_TAPER,
        x3 = exitX,
        y3 = exitY,
    )
}

/**
 * How far short of an acute vertex its blend starts, where the diagonal meets the unit ray
 * ([rayX], [rayY]). Every corner blends at the same [NOSE_RADIUS], so the sharper it is the
 * further back the blend has to reach.
 */
private fun diagonalTangent(
    rayX: Float,
    rayY: Float,
) = NOSE_RADIUS * hypot(DIAGONAL_X + rayX, DIAGONAL_Y + rayY) /
    hypot(DIAGONAL_X - rayX, DIAGONAL_Y - rayY)

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
private const val LIVE_AREA_TOP = 2f

// The bolt, as FlashOn draws it: an open outline form taken to the 20x20 live area, its apex
// and foot the corners the ink never arrives at, so the blend lands on the live area and the
// vertex sits outside it.
private const val APEX_Y = 1.25f
private const val FOOT_Y = IconViewportSize - APEX_Y

private const val UPPER_STEM_X = 14.875f
private const val LOWER_STEM_X = IconViewportSize - UPPER_STEM_X

private const val LEAN = UPPER_STEM_X - LOWER_STEM_X
private const val UPPER_TIP_X = UPPER_STEM_X + LEAN
private const val LOWER_TIP_X = IconViewportSize - UPPER_TIP_X

private const val UPPER_ELBOW_Y = 11f
private const val LOWER_ELBOW_Y = IconViewportSize - UPPER_ELBOW_Y

// Both diagonals run parallel, so one unit vector serves every blend that leaves on one. It
// points foot-ward, which is down and to the left.
private const val DIAGONAL_RUN = LOWER_STEM_X - UPPER_TIP_X
private const val DIAGONAL_DROP = FOOT_Y - UPPER_ELBOW_Y
private val DIAGONAL_LENGTH = hypot(DIAGONAL_RUN, DIAGONAL_DROP)
private val DIAGONAL_X = DIAGONAL_RUN / DIAGONAL_LENGTH
private val DIAGONAL_Y = DIAGONAL_DROP / DIAGONAL_LENGTH

// flash.svg's blend proportions, as FlashOn sets them out.
private const val NOSE_RADIUS = 0.77f * STROKE_WIDTH
private const val NOSE_TAPER = 0.45f

// The foot is the apex turned about the glyph center and the lower tip is the upper one, so
// each pair shares a tangent length. The slash splits them a corner apiece: apex and lower tip
// to the attached half, foot and upper tip to the severed one.
private val APEX_TANGENT = diagonalTangent(rayX = 0f, rayY = 1f)
private val TIP_TANGENT = diagonalTangent(rayX = -1f, rayY = 0f)

private val APEX_DIAGONAL_X = DIAGONAL_X * APEX_TANGENT
private val APEX_DIAGONAL_Y = DIAGONAL_Y * APEX_TANGENT
private val TIP_DIAGONAL_X = DIAGONAL_X * TIP_TANGENT
private val TIP_DIAGONAL_Y = DIAGONAL_Y * TIP_TANGENT

// The slash's ends are round caps, so its centerline stops a half stroke inside the live area
// to put them on the same edge the bolt's noses reach. It runs corner to corner, so both ends
// carry that inset on each axis rather than sitting on 2 and 22 outright.
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
