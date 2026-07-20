package app.honguyen.forge.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(ForgeTheme.dimensions.size1x),
    small = RoundedCornerShape(ForgeTheme.dimensions.size2x),
    medium = RoundedCornerShape(ForgeTheme.dimensions.size3x),
    large = RoundedCornerShape(ForgeTheme.dimensions.size4x),
    extraLarge = RoundedCornerShape(ForgeTheme.dimensions.size8x),
)
