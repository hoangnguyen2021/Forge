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

val Icons.Grid: ImageVector
    get() {
        gridCache?.let { return it }
        return ImageVector.Builder(
            name = "Grid",
            defaultWidth = IconDefaultSize,
            defaultHeight = IconDefaultSize,
            viewportWidth = IconViewportSize,
            viewportHeight = IconViewportSize,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = STROKE_WIDTH,
                // Only the two rules carry caps, and both run to the frame's centerline, where a
                // round cap is exactly a half stroke in every direction. The frame's band takes
                // it whole, so the rules meet it without a seam.
                strokeLineCap = StrokeCap.Round,
            ) {
                frame()
                rules()
            }
        }.build().also { gridCache = it }
    }

private var gridCache: ImageVector? = null

/** The frame, clockwise from the top-left corner's end: each leg, then the corner it runs into. */
private fun PathBuilder.frame() {
    moveTo(FRAME_LEFT + CORNER_RADIUS, FRAME_TOP)
    corner(
        entryX = FRAME_RIGHT - CORNER_RADIUS,
        entryY = FRAME_TOP,
        cornerX = FRAME_RIGHT,
        cornerY = FRAME_TOP,
        exitX = FRAME_RIGHT,
        exitY = FRAME_TOP + CORNER_RADIUS,
    )
    corner(
        entryX = FRAME_RIGHT,
        entryY = FRAME_BOTTOM - CORNER_RADIUS,
        cornerX = FRAME_RIGHT,
        cornerY = FRAME_BOTTOM,
        exitX = FRAME_RIGHT - CORNER_RADIUS,
        exitY = FRAME_BOTTOM,
    )
    corner(
        entryX = FRAME_LEFT + CORNER_RADIUS,
        entryY = FRAME_BOTTOM,
        cornerX = FRAME_LEFT,
        cornerY = FRAME_BOTTOM,
        exitX = FRAME_LEFT,
        exitY = FRAME_BOTTOM - CORNER_RADIUS,
    )
    corner(
        entryX = FRAME_LEFT,
        entryY = FRAME_TOP + CORNER_RADIUS,
        cornerX = FRAME_LEFT,
        cornerY = FRAME_TOP,
        exitX = FRAME_LEFT + CORNER_RADIUS,
        exitY = FRAME_TOP,
    )
    close()
}

/** The two rules that quarter the frame, each run from one centerline across to the other. */
private fun PathBuilder.rules() {
    moveTo(GLYPH_CENTER, FRAME_TOP)
    lineTo(GLYPH_CENTER, FRAME_BOTTOM)

    moveTo(FRAME_LEFT, GLYPH_CENTER)
    lineTo(FRAME_RIGHT, GLYPH_CENTER)
}

/**
 * The leg into one corner of the frame, and the corner itself.
 *
 * The leg stops at ([entryX], [entryY]) and the next one picks up at ([exitX], [exitY]);
 * ([cornerX], [cornerY]) is the square corner the two would have met at. A symmetric cubic spans
 * between them with both control points [CORNER_TAUT] of the way from the corner back toward the
 * leg they belong to, which is the whole of what shapes it.
 */
private fun PathBuilder.corner(
    entryX: Float,
    entryY: Float,
    cornerX: Float,
    cornerY: Float,
    exitX: Float,
    exitY: Float,
) {
    lineTo(entryX, entryY)
    curveTo(
        x1 = cornerX + (entryX - cornerX) * CORNER_TAUT,
        y1 = cornerY + (entryY - cornerY) * CORNER_TAUT,
        x2 = cornerX + (exitX - cornerX) * CORNER_TAUT,
        y2 = cornerY + (exitY - cornerY) * CORNER_TAUT,
        x3 = exitX,
        y3 = exitY,
    )
}

private const val STROKE_WIDTH = 1.5f
private const val HALF_STROKE = STROKE_WIDTH / 2f
private const val GLYPH_CENTER = IconViewportSize / 2f

// An open outline form, so the ink runs to the live area on the long axis. Across it the frame
// takes the 16 of Google's vertical rectangle instead of the full 20: grid-lines.svg draws the
// frame square, and square is the one proportion a four-pane grid cannot afford, since it leaves
// nothing but the rules to say which way round the panes go.
private const val INK_WIDTH = 16f
private const val INK_HEIGHT = 20f

// The rect the stroke is struck on, a half stroke inside the ink on every side.
private const val FRAME_LEFT = GLYPH_CENTER - INK_WIDTH / 2f + HALF_STROKE
private const val FRAME_RIGHT = GLYPH_CENTER + INK_WIDTH / 2f - HALF_STROKE
private const val FRAME_TOP = GLYPH_CENTER - INK_HEIGHT / 2f + HALF_STROKE
private const val FRAME_BOTTOM = GLYPH_CENTER + INK_HEIGHT / 2f - HALF_STROKE

// How far back from each corner the rounding reaches. The source holds it to just over a third
// of the side it is turning, and at 5 against the frame's 14.5 of width this does the same —
// which is what keeps the corner the source's shape rather than the square's. Measured on the
// narrow axis, since that is the one it can run out of: it leaves 4.5 of straight across the top
// and 8.5 down each side.
private const val CORNER_RADIUS = 5f

// Both control points sit this far from the corner, as a fraction of [CORNER_RADIUS]. A true
// quarter circle wants 0.448; holding them in at 0.299 pulls the curve toward the corner it is
// cutting, so the frame keeps square shoulders and rounds off only late. That squircle is the
// source's corner, and the only thing about the glyph that is not straight lines.
private const val CORNER_TAUT = 0.299f

@Preview(name = "Grid", showBackground = true)
@Composable
private fun GridPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.Grid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ForgeTheme.dimensions.size12x),
                )
            }
        }
    }
}
