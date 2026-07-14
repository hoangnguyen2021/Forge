package app.honguyen.forge.designsystem.uikit.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.honguyen.forge.designsystem.modifier.thenIfNotNull
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.icons.FramePerson

@Composable
fun RoundIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = ForgeTheme.dimensions.size10x,
    containerColor: Color = Color.Transparent,
    contentColor: Color = LocalContentColor.current,
    borderColor: Color? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(
                color = containerColor,
                shape = CircleShape,
            )
            .thenIfNotNull(borderColor) {
                border(
                    width = ForgeTheme.dimensions.sizeUnit,
                    color = it,
                    shape = CircleShape,
                )
            },
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

// 1 / sqrt(2), trimmed for optical breathing room between glyph and circle.
private const val INSCRIBED_SQUARE_RATIO = 0.68f

@Preview(name = "RoundIcon", showBackground = true)
@Composable
private fun RoundIconPreview() {
    ForgeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier.safeDrawingPadding(),
            ) {
                RoundIcon(
                    imageVector = ForgeTheme.icons.FramePerson,
                    size = ForgeTheme.dimensions.size10x,
                )
            }
        }
    }
}
