package app.honguyen.forge.designsystem.uikit.buttons

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.honguyen.forge.designsystem.modifier.thenIfNotNull
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.icons.CameraFlip

/**
 * An icon button with two states, whose icon spins a half turn each time the state
 * flips.
 *
 * [imageVector] should be a glyph that is unchanged by a 180 degree rotation, such as
 * [ForgeTheme.icons].CameraFlip; anything else comes to rest upside down. The glyph is the
 * same in both states, so the spin — not a change of icon — is what signals the flip.
 *
 * The rotation is driven by [checked] rather than accumulated per tap, so a state change
 * the button did not originate still animates: if the camera fails to switch and the caller
 * reverts [checked], the icon spins back on its own. Each flip turns the same direction
 * rather than unwinding, and a tap that lands mid-spin animates on from wherever the icon
 * currently is.
 *
 * [interactionSource] is hoisted so press-driven flourishes (a scale-down while held, a
 * bounce on release) can be layered on later by reading it and folding the result into the
 * same graphics layer the rotation already uses.
 */
@Composable
fun SpinToggleButton(
    imageVector: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = ForgeTheme.dimensions.size13x,
    shape: Shape = MaterialTheme.shapes.small,
    containerColor: Color = Color.Transparent,
    contentColor: Color = LocalContentColor.current,
    borderColor: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val rotation = remember { Animatable(0f) }
    var spunFor by remember { mutableStateOf(checked) }

    LaunchedEffect(checked) {
        if (checked == spunFor) return@LaunchedEffect
        spunFor = checked
        rotation.animateTo(
            targetValue = rotation.value + HALF_TURN,
            animationSpec = tween(
                durationMillis = SPIN_DURATION_MILLIS,
                easing = FastOutSlowInEasing,
            ),
        )
        // Same pose, bounded value: keeps the angle from drifting up without end across a
        // long session of toggling.
        rotation.snapTo(rotation.value % FULL_TURN)
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            // Clipped before the ripple is attached, so the indication stays inside [shape].
            .clip(shape)
            .background(color = containerColor)
            .thenIfNotNull(borderColor) {
                border(
                    width = ForgeTheme.dimensions.sizeUnit,
                    color = it,
                    shape = shape,
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = { onCheckedChange(!checked) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size * INSCRIBED_SQUARE_RATIO)
                .graphicsLayer { rotationZ = rotation.value },
            tint = contentColor,
        )
    }
}

// 1 / sqrt(2), trimmed for optical breathing room between glyph and container.
private const val INSCRIBED_SQUARE_RATIO = 0.68f

private const val HALF_TURN = 180f
private const val FULL_TURN = 360f
private const val SPIN_DURATION_MILLIS = 300

@Preview(name = "SpinToggleButton", showBackground = true)
@Composable
private fun SpinToggleButtonPreview() {
    ForgeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            var frontFacing by remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                SpinToggleButton(
                    imageVector = ForgeTheme.icons.CameraFlip,
                    checked = frontFacing,
                    onCheckedChange = { frontFacing = it },
                    contentDescription = if (frontFacing) {
                        "Switch to back camera"
                    } else {
                        "Switch to front camera"
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
