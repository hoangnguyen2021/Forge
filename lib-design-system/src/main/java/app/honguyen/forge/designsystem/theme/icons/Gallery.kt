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
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.Icons
import kotlin.math.PI

val Icons.Gallery: ImageVector
    get() {
        galleryCache?.let { return it }
        return ImageVector.Builder(
            name = "Gallery",
            defaultWidth = GalleryIconDefaultWidth,
            defaultHeight = GalleryIconDefaultHeight,
            viewportWidth = GalleryIconViewportWidth,
            viewportHeight = GalleryIconViewportHeight,
        ).apply {
            // Tilting a rectangle swells the box it needs, so the two cards are laid out square
            // and fanned afterward, and this group takes the result back down onto the live
            // area. Fitting rather than redrawing, as Debug does.
            group(
                pivotX = LAYOUT_CENTER,
                pivotY = LAYOUT_CENTER,
                scaleX = GLYPH_FIT,
                scaleY = GLYPH_FIT,
                translationX = GLYPH_FIT_SHIFT_X,
                translationY = GLYPH_FIT_SHIFT_Y,
            ) {
                group(
                    rotate = BACK_CARD_TILT,
                    pivotX = BACK_CARD_PIVOT_X,
                    pivotY = LAYOUT_CENTER,
                ) {
                    strokedPath { backCard() }
                }
                // The horizon and the sun are pinned to the card they are printed on, so they
                // turn with it rather than standing level inside a leaning frame.
                group(
                    rotate = FRONT_CARD_TILT,
                    pivotX = FRONT_CARD_PIVOT_X,
                    pivotY = LAYOUT_CENTER,
                ) {
                    strokedPath {
                        frontCard()
                        horizon()
                    }
                    // The sun is the one solid in the glyph. Outlined at this size it would be
                    // a ring of counter barely wider than the pen drawing it.
                    path(fill = SolidColor(Color.Black)) {
                        sun()
                    }
                }
            }
        }.build().also { galleryCache = it }
    }

private var galleryCache: ImageVector? = null

/**
 * The one pen the glyph is drawn with. Both cards and the horizon share it, and only the back
 * card's two arms ever end in the open — the front card closes on itself and the horizon dies
 * into the walls it runs between.
 */
private fun ImageVector.Builder.strokedPath(run: PathBuilder.() -> Unit) =
    path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = run,
    )

/**
 * The card behind, which only its left edge and the stubs of its two long sides ever show.
 *
 * It is a second card of the same corner, not a bracket: each arm stops where the front card's
 * ink begins, and what would carry on past that is behind that card rather than absent. The two
 * stop in different places — see [BACK_CARD_TOP_REACH]. Traced from the top arm, left along it
 * and down the spine.
 */
private fun PathBuilder.backCard() {
    moveTo(BACK_CARD_TOP_CUT_X, BACK_CARD_TOP)
    lineTo(BACK_CARD_LEFT + CORNER_RADIUS, BACK_CARD_TOP)
    cornerTo(BACK_CARD_LEFT, BACK_CARD_TOP + CORNER_RADIUS, clockwise = false)
    lineTo(BACK_CARD_LEFT, BACK_CARD_BOTTOM - CORNER_RADIUS)
    cornerTo(BACK_CARD_LEFT + CORNER_RADIUS, BACK_CARD_BOTTOM, clockwise = false)
    lineTo(BACK_CARD_BOTTOM_CUT_X, BACK_CARD_BOTTOM)
}

/** The card in front, a closed rounded rect traced clockwise from its top-left corner. */
private fun PathBuilder.frontCard() {
    moveTo(FRONT_CARD_LEFT + CORNER_RADIUS, FRONT_CARD_TOP)
    lineTo(FRONT_CARD_RIGHT - CORNER_RADIUS, FRONT_CARD_TOP)
    cornerTo(FRONT_CARD_RIGHT, FRONT_CARD_TOP + CORNER_RADIUS)
    lineTo(FRONT_CARD_RIGHT, FRONT_CARD_BOTTOM - CORNER_RADIUS)
    cornerTo(FRONT_CARD_RIGHT - CORNER_RADIUS, FRONT_CARD_BOTTOM)
    lineTo(FRONT_CARD_LEFT + CORNER_RADIUS, FRONT_CARD_BOTTOM)
    cornerTo(FRONT_CARD_LEFT, FRONT_CARD_BOTTOM - CORNER_RADIUS)
    lineTo(FRONT_CARD_LEFT, FRONT_CARD_TOP + CORNER_RADIUS)
    cornerTo(FRONT_CARD_LEFT + CORNER_RADIUS, FRONT_CARD_TOP)
    close()
}

/**
 * The landscape inside the front card: one full period of a sine, wall to wall.
 *
 * Three cubics, because that is what a period divides into at its own turning points — a quarter
 * down from the left wall into the valley, a half from valley to crest, a quarter from the crest
 * down to the right wall. Every handle below is that segment's own span times a fixed fraction,
 * so the curve is a sine by construction and not by eye; see [HORIZON_HALF_HANDLE].
 *
 * The two crossings are the only places the curve is not level, and they carry the sine's own
 * slope there — added leaving the left wall, subtracted arriving at the right, since the wave is
 * falling through both.
 *
 * Both ends run onto a wall's centerline, where a round cap is exactly the wall's own half
 * stroke in every direction, so the wall takes the cap whole and the horizon reads as meeting
 * it rather than stopping against it.
 */
private fun PathBuilder.horizon() {
    moveTo(FRONT_CARD_LEFT, HORIZON_MIDLINE_Y)
    curveTo(
        x1 = FRONT_CARD_LEFT + HORIZON_QUARTER_HANDLE,
        y1 = HORIZON_MIDLINE_Y + HORIZON_CROSSING_FALL,
        x2 = HORIZON_VALLEY_X - HORIZON_QUARTER_HANDLE,
        y2 = HORIZON_VALLEY_Y,
        x3 = HORIZON_VALLEY_X,
        y3 = HORIZON_VALLEY_Y,
    )
    curveTo(
        x1 = HORIZON_VALLEY_X + HORIZON_HALF_HANDLE,
        y1 = HORIZON_VALLEY_Y,
        x2 = HORIZON_CREST_X - HORIZON_HALF_HANDLE,
        y2 = HORIZON_CREST_Y,
        x3 = HORIZON_CREST_X,
        y3 = HORIZON_CREST_Y,
    )
    curveTo(
        x1 = HORIZON_CREST_X + HORIZON_QUARTER_HANDLE,
        y1 = HORIZON_CREST_Y,
        x2 = FRONT_CARD_RIGHT - HORIZON_QUARTER_HANDLE,
        y2 = HORIZON_MIDLINE_Y - HORIZON_CROSSING_FALL,
        x3 = FRONT_CARD_RIGHT,
        y3 = HORIZON_MIDLINE_Y,
    )
}

/** The sun, as two half arcs. */
private fun PathBuilder.sun() {
    moveTo(SUN_CENTER_X + SUN_RADIUS, SUN_CENTER_Y)
    sunHalfTo(SUN_CENTER_X - SUN_RADIUS)
    sunHalfTo(SUN_CENTER_X + SUN_RADIUS)
    close()
}

private fun PathBuilder.sunHalfTo(x: Float) =
    arcTo(
        horizontalEllipseRadius = SUN_RADIUS,
        verticalEllipseRadius = SUN_RADIUS,
        theta = 0f,
        isMoreThanHalf = false,
        isPositiveArc = true,
        x1 = x,
        y1 = SUN_CENTER_Y,
    )

/**
 * A corner of either card, both sharing [CORNER_RADIUS].
 *
 * The front card is traced clockwise and the back one counterclockwise — its visible run starts
 * at the cut and works left — so the two turn opposite ways round the same corner shape, and
 * [clockwise] is what says which.
 */
private fun PathBuilder.cornerTo(
    x: Float,
    y: Float,
    clockwise: Boolean = true,
) = arcTo(
    horizontalEllipseRadius = CORNER_RADIUS,
    verticalEllipseRadius = CORNER_RADIUS,
    theta = 0f,
    isMoreThanHalf = false,
    isPositiveArc = clockwise,
    x1 = x,
    y1 = y,
)

private const val STROKE_WIDTH = 1.75f
private const val HALF_STROKE = STROKE_WIDTH / 2f

// The one icon in the set that is not square: a portrait box, 24 by 26, with the set's 2 of
// padding kept on all four sides. Width is what the pair of cards binds against — two of them
// abreast will always be the wide way round — so the height goes on the cards themselves
// standing taller rather than on the whole glyph growing.
//
// 26 is the height this glyph wants. The cards are drawn at a proportion that carries the fanned
// pair to 21.73 down against the 20 it is allowed across, and a 22 live area is the shortest
// that clears it — the ink lands an eighth of a unit inside filling both axes at once.
private val GalleryIconDefaultWidth = 24.dp
private val GalleryIconDefaultHeight = 26.dp
private const val GalleryIconViewportWidth = 24f
private const val GalleryIconViewportHeight = 26f

// What a caller has to multiply a height by to get this icon's width. Every other glyph in the
// set is square and can be given one measure; this one cannot.
private const val GALLERY_ASPECT = GalleryIconViewportWidth / GalleryIconViewportHeight

private const val GLYPH_PADDING = 2f
private const val LIVE_AREA_WIDTH = GalleryIconViewportWidth - 2f * GLYPH_PADDING

// The cards are laid out square and to their own scale, then fitted into the viewport by the
// group above. This is that working frame's center, and the pivot every rotation and the fit
// itself turn about; it is not the viewport's center, which the fit shifts them onto.
private const val LAYOUT_SIZE = 24f
private const val LAYOUT_CENTER = LAYOUT_SIZE / 2f

// An open outline form, so the pair is drawn to the 20x20 live area rather than held to the 18
// square keyline. Width is the binding dimension — two cards side by side stand wider than one
// card is tall — so the fanned ink runs the full 20 across and 18.5 down.
private const val LAYOUT_RIGHT = LAYOUT_SIZE - GLYPH_PADDING

// The fan, measured off the source: the front card leans right and the back one leans left, and
// it is the two leaning opposite ways that reads as a pair of loose photographs rather than one
// card with a bracket beside it. Neither angle is the other's — they were traced by hand, and
// squaring them to a shared value costs the arrangement the last of its looseness.
private const val FRONT_CARD_TILT = 8f
private const val BACK_CARD_TILT = -7.5f

// Fanned, the layout below comes to 24.81 across by 25.23 down. Width binds — 20 over 24.81
// against 22 over 25.23 — and both axes take that same scale, since anything else would make
// the sun an ellipse. The pair ends up 20.34 tall, twenty-two twenty-thirds of the height.
private const val GLYPH_FIT = LIVE_AREA_WIDTH / 24.807f

// The layout frame is square and centered on 12; the viewport is not, and a tilted pair does not
// sit symmetrically in its own box either. These carry both corrections at once — the shift down
// is the viewport's center standing one below the frame's.
private const val GLYPH_FIT_SHIFT_X = 0.9494f
private const val GLYPH_FIT_SHIFT_Y = GalleryIconViewportHeight / 2f - LAYOUT_CENTER

// The front card, ink measured. Its right edge is pinned to the live area, so width is spent
// leftward, into the room the back card was using; height is where the portrait viewport goes.
//
// At 24.05 by 15.5 the card is a little over three to two, which is the source's own proportion
// arrived at from the other side. Standing a card up is the only way a pair of them abreast can
// reach down a portrait box — scaling the whole glyph would only run it out of width — and past
// about two to one it stops reading as a photograph, which is what bounds the height.
private const val FRONT_CARD_INK_WIDTH = 15.5f
private const val FRONT_CARD_INK_HEIGHT = 24.05f

// The stroke is struck a half in from the ink on every side. The card is pushed hard right so
// what is left over on the other side is all the back card has to show in.
private const val FRONT_CARD_RIGHT = LAYOUT_RIGHT - HALF_STROKE
private const val FRONT_CARD_LEFT = LAYOUT_RIGHT - FRONT_CARD_INK_WIDTH + HALF_STROKE
private const val FRONT_CARD_TOP = LAYOUT_CENTER - FRONT_CARD_INK_HEIGHT / 2f + HALF_STROKE
private const val FRONT_CARD_BOTTOM = LAYOUT_CENTER + FRONT_CARD_INK_HEIGHT / 2f - HALF_STROKE

// It turns about its own middle, so the lean costs the layout no more room on one side than the
// other and the card stays where it was put.
private const val FRONT_CARD_PIVOT_X = (FRONT_CARD_LEFT + FRONT_CARD_RIGHT) / 2f

// Wall to wall on the card's own centerlines, which is the box everything printed on the card is
// placed within.
private const val FRONT_CARD_SPAN_X = FRONT_CARD_RIGHT - FRONT_CARD_LEFT
private const val FRONT_CARD_SPAN_Y = FRONT_CARD_BOTTOM - FRONT_CARD_TOP

// The 2 the set turns exterior corners at, which both cards can afford: it takes 2 out of the
// front card's 11.25 of centerline width, leaving over 7 of straight down each side.
private const val CORNER_RADIUS = 2f

// The back card stands shorter, the way the further of two stacked photographs does, and its
// spine sits where the source puts it: far enough left that what shows of it is 6.79 wide
// against the front card's 13, the proportion the two are drawn at.
private const val BACK_CARD_INK_HEIGHT = 22.13f
private const val BACK_CARD_LEFT = 0.38f
private const val BACK_CARD_TOP = LAYOUT_CENTER - BACK_CARD_INK_HEIGHT / 2f + HALF_STROKE
private const val BACK_CARD_BOTTOM = LAYOUT_CENTER + BACK_CARD_INK_HEIGHT / 2f - HALF_STROKE

// It turns about the middle of its own spine, the one part of it that shows whole.
private const val BACK_CARD_PIVOT_X = BACK_CARD_LEFT

// How far each arm runs past its corner before the front card covers it. The two are nothing
// alike because what occludes them is itself leaning: the front card's left edge carries away
// from the top arm and in toward the bottom one. Cut them level instead and the bottom collides.
//
// The bottom one is down to a stub, and it is the card's width that took it — every unit the
// card gains leftward is a unit that edge sweeps further across the arm below. Widening the card
// costs this arm first, then the spine, which is why the spine had to move left with it.
private const val BACK_CARD_TOP_REACH = 3.8f
private const val BACK_CARD_BOTTOM_REACH = 0.1f
private const val BACK_CARD_TOP_CUT_X = BACK_CARD_LEFT + CORNER_RADIUS + BACK_CARD_TOP_REACH
private const val BACK_CARD_BOTTOM_CUT_X = BACK_CARD_LEFT + CORNER_RADIUS + BACK_CARD_BOTTOM_REACH

// High in the card and left of its center, in the same fractions of the card as the horizon, so
// the two keep their spacing if the card is ever resized.
//
// The sun and the wave are the two things inside the card, and how far apart they read is the
// whole of what stops the card looking crowded. They are pushed to opposite ends of it — the sun
// up into the quarter, the wave's midline down past three fifths — which buys 3.24 of daylight
// between the sun's edge and the nearest the wave climbs to it. Closer to a third of the card
// each way and the two start reading as one mass at 24dp.
private const val SUN_CENTER_X = FRONT_CARD_LEFT + 0.371f * FRONT_CARD_SPAN_X
private const val SUN_CENTER_Y = FRONT_CARD_TOP + 0.25f * FRONT_CARD_SPAN_Y

// Solid, so it is sized by ink rather than by a wall: at 1.45 it reads as a disc at 24dp without
// crowding the wall it sits nearest.
private const val SUN_RADIUS = 1.45f

// The line the wave is measured from, a little below the card's own middle so the valley has
// somewhere to go without crowding the bottom wall, and how far it swings either side of it.
//
// Amplitude is the whole of what makes the wave read as one, and the whole of how restless it
// looks doing it. At 0.145 the crest stands a little under three tenths of the card clear of the
// valley — enough that the two turns are unmistakable, calm enough that the line reads as a
// landscape rather than a signal. Under about a tenth it flattens toward a rule across the card.
private const val HORIZON_MIDLINE_Y = FRONT_CARD_TOP + 0.6f * FRONT_CARD_SPAN_Y
private const val HORIZON_AMPLITUDE = 0.145f * FRONT_CARD_SPAN_Y

// Exactly one period, wall to wall, which puts the turns on the quarter and the three quarter.
// Evenly spaced turns are most of what separates a sine from a line that merely bends twice.
private const val HORIZON_VALLEY_X = FRONT_CARD_LEFT + 0.25f * FRONT_CARD_SPAN_X
private const val HORIZON_CREST_X = FRONT_CARD_LEFT + 0.75f * FRONT_CARD_SPAN_X
private const val HORIZON_VALLEY_Y = HORIZON_MIDLINE_Y + HORIZON_AMPLITUDE
private const val HORIZON_CREST_Y = HORIZON_MIDLINE_Y - HORIZON_AMPLITUDE

// What makes the three cubics a sine rather than a guess at one. Each is its segment's span
// times a fixed fraction, both derived by forcing the cubic's midpoint onto the sine's own:
// a half period between two level turns wants 1 - 2/pi of its span, and a quarter running from
// a crossing to a turn wants a shade less. Together they hold the curve within a thousandth of
// an amplitude of a true sine, which is a quarter of a hundredth of a unit here.
private const val HORIZON_HALF_HANDLE = 0.3634f * 0.5f * FRONT_CARD_SPAN_X
private const val HORIZON_QUARTER_HANDLE = 0.3516f * 0.25f * FRONT_CARD_SPAN_X

// How far the curve has fallen by the time its handle runs out at a crossing — the sine's own
// slope there, spent over [HORIZON_QUARTER_HANDLE]. The crossings are the steepest the wave
// gets, and the only points on it that are not level.
private val HORIZON_CROSSING_FALL =
    HORIZON_AMPLITUDE * 2f * PI.toFloat() / FRONT_CARD_SPAN_X * HORIZON_QUARTER_HANDLE

@Preview(name = "Gallery", showBackground = true)
@Composable
private fun GalleryPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                Icon(
                    imageVector = ForgeTheme.icons.Gallery,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    // Sized by height, the axis this glyph shares with the rest of the set;
                    // width follows from the viewport so the cards keep their proportion.
                    modifier = Modifier.size(
                        width = ForgeTheme.dimensions.size12x * GALLERY_ASPECT,
                        height = ForgeTheme.dimensions.size12x,
                    ),
                )
            }
        }
    }
}
