package app.honguyen.forge.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(ForgeTheme.dimensions.size2x),
    small = RoundedCornerShape(ForgeTheme.dimensions.size3x),
    medium = RoundedCornerShape(ForgeTheme.dimensions.size4x),
    large = RoundedCornerShape(ForgeTheme.dimensions.size6x),
    extraLarge = RoundedCornerShape(ForgeTheme.dimensions.size8x),
)
