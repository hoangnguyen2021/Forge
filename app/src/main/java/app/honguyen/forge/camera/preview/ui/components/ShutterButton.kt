package app.honguyen.forge.camera.preview.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.honguyen.forge.designsystem.theme.ForgeTheme

/**
 * The camera capture button: a thin outer ring around a filled inner disc, floating over
 * the live preview. The inner disc scales down while pressed for tactile feedback.
 *
 * Colors default to white, the camera-overlay convention that reads over any preview
 * content regardless of theme; callers can override for state (e.g. a live-red disc).
 */
@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "Capture",
    ringColor: Color = Color.White,
    innerColor: Color = Color.White,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val innerScale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_INNER_SCALE else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "shutterInnerScale",
    )

    Box(
        modifier = modifier
            .size(ForgeTheme.dimensions.size18x)
            .graphicsLayer { alpha = if (enabled) 1f else DISABLED_ALPHA }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = ForgeTheme.dimensions.size1x,
                    color = ringColor,
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .size(ForgeTheme.dimensions.size14x)
                .graphicsLayer {
                    scaleX = innerScale
                    scaleY = innerScale
                }
                .clip(CircleShape)
                .background(innerColor),
        )
    }
}

private const val PRESSED_INNER_SCALE = 0.86f
private const val DISABLED_ALPHA = 0.4f

@Preview(
    name = "Shutter — over preview",
    showBackground = true,
    backgroundColor = 0xFF131313,
)
@Composable
private fun ShutterButtonPreview() {
    ForgeTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(ForgeTheme.dimensions.size6x)) {
            ShutterButton(onClick = {})
        }
    }
}
