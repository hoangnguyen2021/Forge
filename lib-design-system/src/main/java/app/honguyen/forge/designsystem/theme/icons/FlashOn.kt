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

val Icons.FlashOn: ImageVector
    get() {
        flashOnCache?.let { return it }
        return ImageVector.Builder(
            name = "FlashOn",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // The bolt as one closed run, clockwise from where the apex rejoins the stem:
                // down the stem, out along its ledge and round the tip, then the long diagonal
                // to the foot. The lower half is that turned about the glyph center, and the
                // run ends back in the apex it started out of.
                moveTo(UPPER_STEM_X, APEX_Y + APEX_TANGENT)
                lineTo(UPPER_STEM_X, UPPER_ELBOW_Y)
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
                lineTo(LOWER_STEM_X, LOWER_ELBOW_Y)
                noseAt(
                    entryX = LOWER_TIP_X + TIP_TANGENT,
                    entryY = LOWER_ELBOW_Y,
                    vertexX = LOWER_TIP_X,
                    vertexY = LOWER_ELBOW_Y,
                    exitX = LOWER_TIP_X - TIP_DIAGONAL_X,
                    exitY = LOWER_ELBOW_Y - TIP_DIAGONAL_Y,
                )
                noseAt(
                    entryX = UPPER_STEM_X + APEX_DIAGONAL_X,
                    entryY = APEX_Y + APEX_DIAGONAL_Y,
                    vertexX = UPPER_STEM_X,
                    vertexY = APEX_Y,
                    exitX = UPPER_STEM_X,
                    exitY = APEX_Y + APEX_TANGENT,
                )
                close()
            }
        }.build().also { flashOnCache = it }
    }

private var flashOnCache: ImageVector? = null

/**
 * The straight leg into one of the bolt's four acute corners, and the blend that carries it
 * round.
 *
 * The leg stops at ([entryX], [entryY]) and the next one picks up at ([exitX], [exitY]);
 * ([vertexX], [vertexY]) is the corner the two would have met at, which the ink never reaches.
 * A symmetric cubic spans between them with both control points [NOSE_TAPER] of the way from
 * the vertex back toward the leg they belong to.
 *
 * A stroke rounds only the outside of a join, so a corner this acute left to [StrokeJoin.Round]
 * has a needle for an inside. Blending the centerline instead eases both edges at once — which
 * is what flash.svg does, and the only reason the glyph has curves in it at all.
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
 * ([rayX], [rayY]).
 *
 * Every corner blends at the same [NOSE_RADIUS], so the sharper it is the further back the
 * blend has to reach: the tangent of half the angle between two unit rays is the length of
 * their difference over the length of their sum. Derived rather than written down, because a
 * value left stale by a change of lean shows up as one nose fatter than the other three.
 */
private fun diagonalTangent(
    rayX: Float,
    rayY: Float,
) = NOSE_RADIUS * hypot(DIAGONAL_X + rayX, DIAGONAL_Y + rayY) /
    hypot(DIAGONAL_X - rayX, DIAGONAL_Y - rayY)

private const val STROKE_WIDTH = 1.75f

// An open outline form, so the bolt is drawn to the 20x20 live area rather than held to the
// 18 square keyline. The apex and the foot are corners the ink never arrives at — the nose is
// blunted well short of them — so it is the blend that has to land on the live area, and the
// vertex that sits outside it. At 1.25 the ink tops out on 1.98.
private const val APEX_Y = 1.25f
private const val FOOT_Y = IconViewportSize - APEX_Y

// The lean: how far the upper stem stands right of the lower one, and with it how far the
// bolt travels sideways over its drop. Width is what fixes it — at 5.75 the tips reach 20.625
// and the blunted ink spans 15.9, flash.svg's own width against the live area's 20 of height.
private const val UPPER_STEM_X = 14.875f
private const val LOWER_STEM_X = IconViewportSize - UPPER_STEM_X

// Each ledge juts a lean past its own stem, which is also a lean past the opposite one — the
// source's proportion, and what keeps a tip, a stem and the far stem evenly spaced.
private const val LEAN = UPPER_STEM_X - LOWER_STEM_X
private const val UPPER_TIP_X = UPPER_STEM_X + LEAN
private const val LOWER_TIP_X = IconViewportSize - UPPER_TIP_X

// Splits the drop between stem and diagonal: at 11 the two ledges sit 2 apart across the
// waist, close enough to read as one break in the bolt rather than two separate steps, and
// the stem still has six and a half of straight left once the apex blend has taken its share.
private const val UPPER_ELBOW_Y = 11f
private const val LOWER_ELBOW_Y = IconViewportSize - UPPER_ELBOW_Y

// Both diagonals run parallel, so one unit vector serves every blend that leaves on one. It
// points foot-ward, which is down and to the left.
private const val DIAGONAL_RUN = LOWER_STEM_X - UPPER_TIP_X
private const val DIAGONAL_DROP = FOOT_Y - UPPER_ELBOW_Y
private val DIAGONAL_LENGTH = hypot(DIAGONAL_RUN, DIAGONAL_DROP)
private val DIAGONAL_X = DIAGONAL_RUN / DIAGONAL_LENGTH
private val DIAGONAL_Y = DIAGONAL_DROP / DIAGONAL_LENGTH

// The blend, in flash.svg's proportions: it rounds every acute corner at 1.15 against a 1.5
// stroke, and holds its control points at a little under half the tangent length. A circle
// would want about 0.62 there, so 0.45 is the taut side of one — enough of a point left in
// the nose that the bolt still reads as struck rather than inflated. Tight enough, too, that
// the blend curves a shade harder than a half stroke at its sharpest, which closes the inner
// edge to a soft crease rather than a curve. The source does the same.
private const val NOSE_RADIUS = 0.77f * STROKE_WIDTH
private const val NOSE_TAPER = 0.45f

// The foot is the apex turned about the glyph center and the lower tip is the upper one, so
// each pair shares a tangent length; only the sign of the step along the diagonal differs.
private val APEX_TANGENT = diagonalTangent(rayX = 0f, rayY = 1f)
private val TIP_TANGENT = diagonalTangent(rayX = -1f, rayY = 0f)

private val APEX_DIAGONAL_X = DIAGONAL_X * APEX_TANGENT
private val APEX_DIAGONAL_Y = DIAGONAL_Y * APEX_TANGENT
private val TIP_DIAGONAL_X = DIAGONAL_X * TIP_TANGENT
private val TIP_DIAGONAL_Y = DIAGONAL_Y * TIP_TANGENT

@Preview(name = "FlashOn", showBackground = true)
@Composable
private fun FlashOnPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
