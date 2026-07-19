package app.honguyen.forge.designsystem.uikit.switches

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.icons.PictureMode
import app.honguyen.forge.designsystem.theme.icons.VideoMode
import kotlin.math.roundToInt

@Composable
fun CameraModeSwitch(
    mode: CameraMode,
    onModeChange: (CameraMode) -> Unit,
    modifier: Modifier = Modifier,
    segmentSize: Dp = ForgeTheme.dimensions.size12x,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    photoContentDescription: String? = null,
    videoContentDescription: String? = null,
) {
    val selection by animateFloatAsState(
        targetValue = if (mode == CameraMode.Video) 1f else 0f,
        animationSpec = tween(SLIDE_DURATION_MILLIS, easing = FastOutSlowInEasing),
        label = "CameraModeSelection",
    )

    Box(
        modifier = modifier
            .height(segmentSize)
            .width(segmentSize * 2)
            .clip(CircleShape)
            .background(containerColor)
            .selectableGroup(),
    ) {
        // The sliding highlight, laid down before the icons so they sit on top of it. Its
        // diameter equals the pill height, so it fills the pill top-to-bottom and spans
        // exactly half the width, traveling a whole segment from photo to video.
        Box(
            modifier = Modifier
                .size(segmentSize)
                .offset { IntOffset(x = (segmentSize.toPx() * selection).roundToInt(), y = 0) }
                .clip(CircleShape)
                .background(selectedContainerColor),
        )

        Row {
            CameraModeSegment(
                imageVector = ForgeTheme.icons.PictureMode,
                contentDescription = photoContentDescription,
                selected = mode == CameraMode.Photo,
                size = segmentSize,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                onClick = { onModeChange(CameraMode.Photo) },
            )
            CameraModeSegment(
                imageVector = ForgeTheme.icons.VideoMode,
                contentDescription = videoContentDescription,
                selected = mode == CameraMode.Video,
                size = segmentSize,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                onClick = { onModeChange(CameraMode.Video) },
            )
        }
    }
}

@Composable
private fun CameraModeSegment(
    imageVector: ImageVector,
    contentDescription: String?,
    selected: Boolean,
    size: Dp,
    selectedContentColor: Color,
    unselectedContentColor: Color,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = tween(TINT_DURATION_MILLIS, easing = FastOutSlowInEasing),
        label = "CameraModeTint",
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * ICON_RATIO),
        )
    }
}

// Icon edge as a fraction of the segment, leaving the glyph comfortably inside the highlight.
private const val ICON_RATIO = 0.5f

private const val SLIDE_DURATION_MILLIS = 300
private const val TINT_DURATION_MILLIS = 200

@Preview(name = "CameraModeSwitch", showBackground = true)
@Composable
private fun CameraModeSwitchPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            var mode by remember { mutableStateOf(CameraMode.Photo) }
            Box(modifier = Modifier.safeDrawingPadding().padding(ForgeTheme.dimensions.size6x)) {
                CameraModeSwitch(
                    mode = mode,
                    onModeChange = { mode = it },
                    photoContentDescription = "Photo mode",
                    videoContentDescription = "Video mode",
                )
            }
        }
    }
}
