package app.honguyen.forge.designsystem.uikit.buttons

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.honguyen.forge.designsystem.modifier.thenIfNotNull
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.LocalForgeExtendedColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    contentDescription: String? = null,
    size: Dp = ForgeTheme.dimensions.size20x,
    outerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    innerColor: Color = MaterialTheme.colorScheme.primary,
    recordingColor: Color = LocalForgeExtendedColors.current.live,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    var state by remember { mutableStateOf(ShutterState.Idle) }

    val innerScale = remember { Animatable(1f) }
    val innerColorAnimated by animateColorAsState(
        targetValue = if (state == ShutterState.Recording) recordingColor else innerColor,
        animationSpec = tween(
            durationMillis = RECORDING_TRANSITION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "shutterInnerColor",
    )

    // Bounce animation on innerScale and state updates based on interactions
    LaunchedEffect(interactionSource) {
        var bounceJob: Job? = null
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    bounceJob?.cancel()
                    bounceJob = launch {
                        // Always play the first half of the bounce on press.
                        innerScale.animateTo(
                            targetValue = BOUNCE_PEAK_RATIO / INNER_RATIO,
                            animationSpec = tween(
                                durationMillis = BOUNCE_GROW_MILLIS,
                                easing = LinearOutSlowInEasing,
                            ),
                        )
                    }
                }

                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    if (state == ShutterState.Recording) {
                        // Finger-up ends recording; LaunchedEffect(state) springs back to rest.
                        state = ShutterState.Idle
                        onStopRecording()
                    } else {
                        // A tap: play the deferred second half of the bounce.
                        bounceJob?.cancel()
                        bounceJob = launch {
                            innerScale.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = BOUNCE_RETURN_MILLIS,
                                    easing = FastOutSlowInEasing,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    // Animation on innerScale between states.
    LaunchedEffect(state) {
        innerScale.animateTo(
            targetValue = if (state == ShutterState.Recording) INNER_RECORDING_RATIO / INNER_RATIO else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .clip(CircleShape)
            .background(color = outerColor)
            .thenIfNotNull(contentDescription) {
                semantics { this.contentDescription = it }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
                onLongClick = {
                    // state update on long click
                    state = ShutterState.Recording
                    onStartRecording()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * INNER_RATIO)
                .graphicsLayer {
                    scaleX = innerScale.value
                    scaleY = innerScale.value
                }
                .clip(CircleShape)
                .background(color = innerColorAnimated)
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(),
                ),
        )
    }
}

// Inner circle's rest diameter as a fraction of the button, leaving a thin ring of the
// outer circle visible around it.
private const val INNER_RATIO = 0.85f

// How close the inner circle comes to the outer edge at the peak of the bounce.
private const val BOUNCE_PEAK_RATIO = 0.9f

// Inner circle's diameter while recording — noticeably smaller than at rest.
private const val INNER_RECORDING_RATIO = 0.65f

private const val BOUNCE_GROW_MILLIS = 90
private const val BOUNCE_RETURN_MILLIS = 130
private const val RECORDING_TRANSITION_MILLIS = 220

@Preview(name = "ShutterButton", showBackground = true)
@Composable
private fun ShutterButtonPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                ShutterButton(
                    onClick = {},
                    contentDescription = "Take photo",
                )
            }
        }
    }
}
