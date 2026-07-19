package app.honguyen.forge.designsystem.uikit.carousels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A horizontally draggable strip of capture-mode labels, in the style of the Pixel camera app.
 *
 * The selected mode is always parked at the horizontal centre under a pill highlight that grows
 * and shrinks towards the neighbouring mode's width as the strip is dragged. Labels are painted
 * twice — once clipped to the outside of the pill in [unselectedContentColor], once clipped to the
 * inside in [selectedContentColor] — so a label straddling the pill edge is split cleanly between
 * the two colours instead of switching all at once.
 *
 * Dragging moves the strip 1:1 with the finger; on release a spline decay projects where the strip
 * would coast to and the nearest mode to that projection is snapped to, so a flick can cross
 * several modes while a short drag falls back to where it started.
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

    // Scroll position expressed in the strip's own coordinate space: the value is the centre of
    // whatever currently sits under the container's centre line.
    val scroll = remember(metrics) { Animatable(metrics.centreOf(modes.indexOf(selectedMode))) }
    val decay = remember(density) { splineBasedDecay<Float>(density) }
    val settledIndex by remember { derivedStateOf { metrics.nearestIndexTo(scroll.value) } }

    // Report and tick as the centre line crosses into a new mode, rather than waiting for the
    // strip to come to rest — the selection should feel attached to the finger. The callback is
    // read through a snapshot so this collector can run for the carousel's whole lifetime.
    val currentOnModeSelected by rememberUpdatedState(onModeSelected)
    LaunchedEffect(metrics, modes) {
        snapshotFlow { settledIndex }
            .drop(1)
            .collect { index ->
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentOnModeSelected(modes[index])
            }
    }

    // Follow selection changes driven from outside the carousel (restored state, a tap elsewhere).
    LaunchedEffect(selectedMode, metrics) {
        val target = modes.indexOf(selectedMode)
        if (target != metrics.nearestIndexTo(scroll.value) && !scroll.isRunning) {
            scroll.animateTo(targetValue = metrics.centreOf(target), animationSpec = SnapSpec)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .draggable(
                state = rememberDraggableState { delta ->
                    scope.launch { scroll.snapTo(metrics.clamp(scroll.value - delta)) }
                },
                orientation = Orientation.Horizontal,
                onDragStarted = { scroll.stop() },
                onDragStopped = { velocity ->
                    val projected = decay.calculateTargetValue(
                        initialValue = scroll.value,
                        initialVelocity = -velocity,
                    )
                    val target = metrics.centreOf(metrics.nearestIndexTo(projected))
                    scroll.animateTo(targetValue = target, animationSpec = SnapSpec)
                },
            )
            .pointerInput(metrics) {
                detectTapGestures { tap ->
                    val tapped = metrics.indexAt(
                        x = scroll.value + (tap.x - size.width / 2f),
                    ) ?: return@detectTapGestures
                    scope.launch {
                        scroll.animateTo(
                            targetValue = metrics.centreOf(tapped),
                            animationSpec = SnapSpec,
                        )
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val centreX = size.width / 2f
            val pill = Rect(
                offset = Offset(
                    x = centreX - metrics.pillWidthAt(scroll.value) / 2f,
                    y = 0f,
                ),
                size = Size(width = metrics.pillWidthAt(scroll.value), height = size.height),
            )
            val radius = CornerRadius(size.height / 2f)

            drawRoundRect(
                color = selectedContainerColor,
                topLeft = pill.topLeft,
                size = pill.size,
                cornerRadius = radius,
            )

            val insidePill = Path().apply { addRoundRect(RoundRect(pill, radius)) }
            // Even-odd over the full bounds punches the pill out, leaving everything outside it.
            val outsidePill = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(offset = Offset.Zero, size = size))
                addRoundRect(RoundRect(pill, radius))
            }

            clipPath(path = outsidePill) {
                drawLabels(
                    metrics = metrics,
                    scroll = scroll.value,
                    centreX = centreX,
                    color = unselectedContentColor,
                )
            }
            clipPath(path = insidePill) {
                drawLabels(
                    metrics = metrics,
                    scroll = scroll.value,
                    centreX = centreX,
                    color = selectedContentColor,
                )
            }
        }
    }
}

private fun DrawScope.drawLabels(
    metrics: CarouselMetrics,
    scroll: Float,
    centreX: Float,
    color: Color,
) {
    metrics.labels.forEachIndexed { index, label ->
        val left = centreX + metrics.centreOf(index) - scroll - label.size.width / 2f
        if (left > size.width || left + label.size.width < 0f) return@forEachIndexed
        drawText(
            textLayoutResult = label,
            color = color,
            topLeft = Offset(x = left, y = (size.height - label.size.height) / 2f),
        )
    }
}

/**
 * Pre-measured label geometry for the strip. Every position is a centre offset in the strip's own
 * coordinate space, with mode 0's centre at zero, so nothing here depends on the container width.
 */
private class CarouselMetrics(
    val labels: List<TextLayoutResult>,
    spacingPx: Float,
    private val horizontalPaddingPx: Float,
) {
    val labelHeightPx: Int = labels.maxOf { it.size.height }

    /** Width of the pill were each mode selected: its label plus the pill's own padding. */
    private val slotWidths = labels.map { it.size.width + horizontalPaddingPx * 2f }

    private val centres = FloatArray(labels.size).apply {
        for (index in 1 until size) {
            this[index] = this[index - 1] +
                (slotWidths[index - 1] + slotWidths[index]) / 2f + spacingPx
        }
    }

    fun centreOf(index: Int): Float = centres[index.coerceIn(centres.indices)]

    fun clamp(value: Float): Float = value.coerceIn(centres.first(), centres.last())

    fun nearestIndexTo(value: Float): Int = centres.indices.minBy { abs(centres[it] - value) }

    /**
     * Pill width for an arbitrary scroll position, interpolated between the two modes it sits
     * between so the highlight grows into the next label's width as the strip moves.
     */
    fun pillWidthAt(scroll: Float): Float {
        val clamped = clamp(scroll)
        val lower = centres.indexOfLast { it <= clamped }.coerceAtLeast(0)
        if (lower == centres.lastIndex) return slotWidths.last()
        val fraction = (clamped - centres[lower]) / (centres[lower + 1] - centres[lower])
        return slotWidths[lower] + (slotWidths[lower + 1] - slotWidths[lower]) * fraction
    }

    /** The mode whose slot contains [x], or null for the gaps between slots. */
    fun indexAt(x: Float): Int? =
        centres.indices.firstOrNull {
            abs(centres[it] - x) <= slotWidths[it] / 2f
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
