package app.honguyen.forge.designsystem.uikit.buttons

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import app.honguyen.forge.designsystem.modifier.thenIfNotNull
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.icons.CameraSettings
import app.honguyen.forge.designsystem.theme.icons.Tune

@Composable
fun CameraMenuButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = ForgeTheme.dimensions.size10x,
    shape: CornerBasedShape = CircleShape,
    pressedShape: CornerBasedShape = MaterialTheme.shapes.small,
    containerColor: Color = Color.Transparent,
    contentColor: Color = LocalContentColor.current,
    borderColor: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val morph by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(
            durationMillis = MORPH_DURATION_MILLIS,
            easing = FastOutSlowInEasing,
        ),
        label = "CameraMenuButtonMorph",
    )

    // At rest this hands back [shape] itself, so the resting outline stays a stable instance
    // and only an in-flight morph pays for a new one each frame.
    val currentShape = if (morph == 0f) shape else lerp(shape, pressedShape, morph)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            // Clipped before the ripple is attached, so the indication stays inside the
            // container and follows it through the morph.
            .clip(currentShape)
            .background(color = containerColor)
            .thenIfNotNull(borderColor) {
                border(
                    width = ForgeTheme.dimensions.sizeUnit,
                    color = it,
                    shape = currentShape,
                )
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(size * INSCRIBED_SQUARE_RATIO),
            tint = contentColor,
        )
    }
}

/**
 * Interpolates each corner of [start] toward [stop], resolving both to pixels at draw time
 * so that a percentage corner and an absolute one can be mixed — the circle default is 50%,
 * while the themed shapes it morphs into are fixed dp.
 */
private fun lerp(
    start: CornerBasedShape,
    stop: CornerBasedShape,
    fraction: Float,
): CornerBasedShape =
    start.copy(
        topStart = lerp(start.topStart, stop.topStart, fraction),
        topEnd = lerp(start.topEnd, stop.topEnd, fraction),
        bottomEnd = lerp(start.bottomEnd, stop.bottomEnd, fraction),
        bottomStart = lerp(start.bottomStart, stop.bottomStart, fraction),
    )

private fun lerp(
    start: CornerSize,
    stop: CornerSize,
    fraction: Float,
): CornerSize =
    object : CornerSize {
        override fun toPx(
            shapeSize: Size,
            density: Density,
        ): Float =
            lerp(
                start = start.toPx(shapeSize, density),
                stop = stop.toPx(shapeSize, density),
                fraction = fraction,
            )
    }

// 1 / sqrt(2), trimmed for optical breathing room between glyph and container.
private const val INSCRIBED_SQUARE_RATIO = 0.58f

private const val MORPH_DURATION_MILLIS = 200

@Preview(name = "CameraMenuButton", showBackground = true)
@Composable
private fun CameraMenuButtonPreview() {
    ForgeTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                CameraMenuButton(
                    imageVector = ForgeTheme.icons.CameraSettings,
                    onClick = {},
                    contentDescription = "Camera settings",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CameraMenuButton(
                    imageVector = ForgeTheme.icons.Tune,
                    onClick = {},
                    contentDescription = "Tune",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
