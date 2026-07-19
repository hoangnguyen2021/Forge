package app.honguyen.forge.designsystem.uikit.carousels

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.honguyen.forge.designsystem.theme.ForgeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A horizontally draggable strip of capture-mode labels, in the style of the Pixel camera app.
 * The selected mode sits at the horizontal center under a pill that grows and shrinks toward the
 * neighboring mode's width as the strip moves.
 *
 * The whole strip is drawn onto a single canvas rather than laid out as children, because the
 * pill has to split labels by color rather than by element: every label is painted twice, once
 * clipped to the outside of the pill in [unselectedContentColor] and once clipped to the inside in
 * [selectedContentColor], so a label straddling the pill edge changes color mid-glyph.
 */
@Composable
fun CaptureModeCarousel(
    modes: List<CaptureMode>,
    selectedMode: CaptureMode,
    onModeSelected: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modeSpacing: Dp = ForgeTheme.dimensions.size2x,
    pillHorizontalPadding: Dp = ForgeTheme.dimensions.size3x,
    pillVerticalPadding: Dp = ForgeTheme.dimensions.size2x,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    val metrics = remember(modes, textStyle, density, textMeasurer, modeSpacing, pillHorizontalPadding) {
        CarouselMetrics(
            labels = modes.map { textMeasurer.measure(text = it.label, style = textStyle) },
            spacingPx = with(density) { modeSpacing.toPx() },
            horizontalPaddingPx = with(density) { pillHorizontalPadding.toPx() },
        )
    }
    val height = with(density) { metrics.labelHeightPx.toDp() } + pillVerticalPadding * 2

    // Scroll position in the strip's own coordinate space: the center offset of whatever currently
    // sits under the container's center line.
    var scroll by remember(metrics) {
        mutableFloatStateOf(metrics.centerOf(modes.indexOf(selectedMode)))
    }
    val decay = remember(density) { splineBasedDecay<Float>(density) }
    val settledIndex = remember(metrics) { derivedStateOf { metrics.nearestIndexTo(scroll) } }
    var settleJob by remember(metrics) { mutableStateOf<Job?>(null) }

    /**
     * Animates the strip to [target], canceling any settle already in flight.
     *
     * Routing every animation through the one job keeps this the only writer of [scroll] besides
     * the drag itself. Drag deltas are applied synchronously for the same reason: a delta queued
     * onto a coroutine could land after a settle had begun, cancel it, and strand the strip
     * between two modes.
     */
    fun settleTo(
        target: Float,
        initialVelocity: Float = 0f,
    ) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue = scroll,
                targetValue = target,
                initialVelocity = initialVelocity,
                animationSpec = SnapSpec,
            ) { value, _ -> scroll = value }
        }
    }

    // Report and tick the moment the center line crosses into a new mode rather than waiting for
    // the strip to come to rest, so the selection feels attached to the finger.
    val currentOnModeSelected by rememberUpdatedState(onModeSelected)
    LaunchedEffect(metrics, modes) {
        snapshotFlow { settledIndex.value }
            .drop(1)
            .collect { index ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentOnModeSelected(modes[index])
            }
    }

    // Follow selection changes driven from outside the carousel (restored state, a tap elsewhere).
    LaunchedEffect(selectedMode, metrics) {
        val target = modes.indexOf(selectedMode)
        if (target != metrics.nearestIndexTo(scroll) && settleJob?.isActive != true) {
            settleTo(target = metrics.centerOf(target))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .draggable(
                state = rememberDraggableState { delta ->
                    scroll = metrics.clamp(scroll - delta)
                },
                orientation = Orientation.Horizontal,
                onDragStarted = { settleJob?.cancel() },
                onDragStopped = { velocity ->
                    // Settle on the mode nearest where the strip would have coasted to under its
                    // own momentum, so a hard flick can cross several modes. Handing the fling
                    // velocity to the spring keeps finger and animation continuous.
                    val projected = decay.calculateTargetValue(
                        initialValue = scroll,
                        initialVelocity = -velocity,
                    )
                    settleTo(
                        target = metrics.centerOf(metrics.nearestIndexTo(projected)),
                        initialVelocity = -velocity,
                    )
                },
            )
            .pointerInput(metrics) {
                detectTapGestures { tap ->
                    val tapped = metrics.indexAt(
                        x = scroll + (tap.x - size.width / 2f),
                    ) ?: return@detectTapGestures
                    settleTo(target = metrics.centerOf(tapped))
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val centerX = size.width / 2f
            val pillWidth = metrics.pillWidthAt(scroll)
            val pill = Rect(
                offset = Offset(x = centerX - pillWidth / 2f, y = 0f),
                size = Size(width = pillWidth, height = size.height),
            )
            val radius = CornerRadius(size.height / 2f)

            drawRoundRect(
                color = selectedContainerColor,
                topLeft = pill.topLeft,
                size = pill.size,
                cornerRadius = radius,
            )

            val insidePill = Path().apply { addRoundRect(RoundRect(pill, radius)) }
            // Even-odd winding over the full bounds punches the pill out, leaving its complement.
            // Clipping the two passes to complementary regions rather than overdrawing the second
            // on top keeps the seam clean: overlapping anti-aliased glyphs would blend both
            // colors along the pill edge.
            val outsidePill = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(offset = Offset.Zero, size = size))
                addRoundRect(RoundRect(pill, radius))
            }

            clipPath(path = outsidePill) {
                drawLabels(
                    metrics = metrics,
                    scroll = scroll,
                    centerX = centerX,
                    color = unselectedContentColor,
                )
            }
            clipPath(path = insidePill) {
                drawLabels(
                    metrics = metrics,
                    scroll = scroll,
                    centerX = centerX,
                    color = selectedContentColor,
                )
            }
        }
    }
}

/**
 * Paints every label in a single [color], positioned for the given [scroll]. Called once per
 * clip region, so the caller decides which half of the strip this pass ends up coloring.
 */
private fun DrawScope.drawLabels(
    metrics: CarouselMetrics,
    scroll: Float,
    centerX: Float,
    color: Color,
) {
    metrics.labels.forEachIndexed { index, label ->
        val left = centerX + metrics.centerOf(index) - scroll - label.size.width / 2f
        if (left > size.width || left + label.size.width < 0f) return@forEachIndexed
        drawText(
            textLayoutResult = label,
            color = color,
            topLeft = Offset(x = left, y = (size.height - label.size.height) / 2f),
        )
    }
}

/**
 * Pre-measured label geometry for the strip. Every position is a center offset in the strip's own
 * coordinate space, with mode 0's center at zero, so nothing here depends on the container width.
 */
private class CarouselMetrics(
    val labels: List<TextLayoutResult>,
    spacingPx: Float,
    private val horizontalPaddingPx: Float,
) {
    val labelHeightPx: Int = labels.maxOf { it.size.height }

    /** Width the pill would take were each mode selected: its label plus the pill's padding. */
    private val slotWidths = labels.map { it.size.width + horizontalPaddingPx * 2f }

    // Spacing is measured between slot edges, so consecutive centers are half of each neighboring
    // slot apart plus the gap. Labels of differing widths then sit evenly spaced rather than
    // evenly pitched.
    private val centers = FloatArray(labels.size).apply {
        for (index in 1 until size) {
            this[index] = this[index - 1] +
                (slotWidths[index - 1] + slotWidths[index]) / 2f + spacingPx
        }
    }

    fun centerOf(index: Int): Float = centers[index.coerceIn(centers.indices)]

    fun clamp(value: Float): Float = value.coerceIn(centers.first(), centers.last())

    fun nearestIndexTo(value: Float): Int = centers.indices.minBy { abs(centers[it] - value) }

    /**
     * Pill width at an arbitrary scroll position, interpolated between the slots either side of
     * it so the highlight grows into the next label's width as the strip moves.
     */
    fun pillWidthAt(scroll: Float): Float {
        val clamped = clamp(scroll)
        val lower = centers.indexOfLast { it <= clamped }.coerceAtLeast(0)
        if (lower == centers.lastIndex) return slotWidths.last()
        val fraction = (clamped - centers[lower]) / (centers[lower + 1] - centers[lower])
        return slotWidths[lower] + (slotWidths[lower + 1] - slotWidths[lower]) * fraction
    }

    /** The mode whose slot contains [x], or null for the gaps between slots. */
    fun indexAt(x: Float): Int? =
        centers.indices.firstOrNull {
            abs(centers[it] - x) <= slotWidths[it] / 2f
        }
}

private val SnapSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = 0.5f,
)

@Preview(name = "CaptureModeCarousel", showBackground = true, widthDp = 412)
@Composable
private fun CaptureModeCarouselPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            var mode by remember { mutableStateOf(CaptureMode.Photo) }
            Box(modifier = Modifier.safeDrawingPadding()) {
                CaptureModeCarousel(
                    modes = CaptureMode.entries,
                    selectedMode = mode,
                    onModeSelected = { mode = it },
                )
            }
        }
    }
}
