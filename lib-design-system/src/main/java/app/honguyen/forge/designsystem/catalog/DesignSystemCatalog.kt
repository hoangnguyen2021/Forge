package app.honguyen.forge.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.honguyen.forge.designsystem.catalog.models.ColorRole
import app.honguyen.forge.designsystem.catalog.models.ShapeRole
import app.honguyen.forge.designsystem.catalog.models.TypographyRole
import app.honguyen.forge.designsystem.theme.ForgeTheme
import app.honguyen.forge.designsystem.theme.LocalForgeExtendedColors

@Composable
private fun CatalogSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ForgeTheme.dimensions.size4x),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(ForgeTheme.dimensions.size3x))
        content()
    }
}

@Composable
private fun Swatch(
    name: String,
    color: Color,
    onColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ForgeTheme.dimensions.size12x)
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .border(
                width = ForgeTheme.dimensions.sizeUnit,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.small,
            )
            .padding(ForgeTheme.dimensions.size2x),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = name,
            color = onColor,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun ColorRolesCatalog(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val extendedColors = LocalForgeExtendedColors.current
    val roles = listOf(
        ColorRole(
            name = "primary",
            color = colorScheme.primary,
            onColor = colorScheme.onPrimary,
        ),
        ColorRole(
            name = "onPrimary",
            color = colorScheme.onPrimary,
            onColor = colorScheme.primary,
        ),
        ColorRole(
            name = "primaryContainer",
            color = colorScheme.primaryContainer,
            onColor = colorScheme.onPrimaryContainer,
        ),
        ColorRole(
            name = "onPrimaryContainer",
            color = colorScheme.onPrimaryContainer,
            onColor = colorScheme.primaryContainer,
        ),
        ColorRole(
            name = "inversePrimary",
            color = colorScheme.inversePrimary,
            onColor = colorScheme.onPrimary,
        ),
        ColorRole(
            name = "secondary",
            color = colorScheme.secondary,
            onColor = colorScheme.onSecondary,
        ),
        ColorRole(
            name = "onSecondary",
            color = colorScheme.onSecondary,
            onColor = colorScheme.secondary,
        ),
        ColorRole(
            name = "secondaryContainer",
            color = colorScheme.secondaryContainer,
            onColor = colorScheme.onSecondaryContainer,
        ),
        ColorRole(
            name = "onSecondaryContainer",
            color = colorScheme.onSecondaryContainer,
            onColor = colorScheme.secondaryContainer,
        ),
        ColorRole(
            name = "tertiary",
            color = colorScheme.tertiary,
            onColor = colorScheme.onTertiary,
        ),
        ColorRole(
            name = "onTertiary",
            color = colorScheme.onTertiary,
            onColor = colorScheme.tertiary,
        ),
        ColorRole(
            name = "tertiaryContainer",
            color = colorScheme.tertiaryContainer,
            onColor = colorScheme.onTertiaryContainer,
        ),
        ColorRole(
            name = "onTertiaryContainer",
            color = colorScheme.onTertiaryContainer,
            onColor = colorScheme.tertiaryContainer,
        ),
        ColorRole(
            name = "error",
            color = colorScheme.error,
            onColor = colorScheme.onError,
        ),
        ColorRole(
            name = "onError",
            color = colorScheme.onError,
            onColor = colorScheme.error,
        ),
        ColorRole(
            name = "errorContainer",
            color = colorScheme.errorContainer,
            onColor = colorScheme.onErrorContainer,
        ),
        ColorRole(
            name = "onErrorContainer",
            color = colorScheme.onErrorContainer,
            onColor = colorScheme.errorContainer,
        ),
        ColorRole(
            name = "live",
            color = extendedColors.live,
            onColor = extendedColors.onLive,
        ),
        ColorRole(
            name = "onLive",
            color = extendedColors.onLive,
            onColor = extendedColors.live,
        ),
        ColorRole(
            name = "liveContainer",
            color = extendedColors.liveContainer,
            onColor = extendedColors.onLiveContainer,
        ),
        ColorRole(
            name = "onLiveContainer",
            color = extendedColors.onLiveContainer,
            onColor = extendedColors.liveContainer,
        ),
        ColorRole(
            name = "background",
            color = colorScheme.background,
            onColor = colorScheme.onBackground,
        ),
        ColorRole(
            name = "onBackground",
            color = colorScheme.onBackground,
            onColor = colorScheme.background,
        ),
        ColorRole(
            name = "surface",
            color = colorScheme.surface,
            onColor = colorScheme.onSurface,
        ),
        ColorRole(
            name = "onSurface",
            color = colorScheme.onSurface,
            onColor = colorScheme.surface,
        ),
        ColorRole(
            name = "surfaceVariant",
            color = colorScheme.surfaceVariant,
            onColor = colorScheme.onSurfaceVariant,
        ),
        ColorRole(
            name = "onSurfaceVariant",
            color = colorScheme.onSurfaceVariant,
            onColor = colorScheme.surfaceVariant,
        ),
        ColorRole(
            name = "surfaceTint",
            color = colorScheme.surfaceTint,
            onColor = colorScheme.onPrimary,
        ),
        ColorRole(
            name = "inverseSurface",
            color = colorScheme.inverseSurface,
            onColor = colorScheme.inverseOnSurface,
        ),
        ColorRole(
            name = "inverseOnSurface",
            color = colorScheme.inverseOnSurface,
            onColor = colorScheme.inverseSurface,
        ),
        ColorRole(
            name = "surfaceDim",
            color = colorScheme.surfaceDim,
            onColor = colorScheme.onSurface,
        ),
        ColorRole(
            name = "surfaceBright",
            color = colorScheme.surfaceBright,
            onColor = colorScheme.onSurface,
        ),
        ColorRole(
            name = "surfaceContainerLowest",
            color = colorScheme.surfaceContainerLowest,
            onColor = colorScheme.onSurface,
        ),
        ColorRole(
            name = "surfaceContainerLow",
            color = colorScheme.surfaceContainerLow,
            onColor = colorScheme.onSurface,
        ),
        ColorRole(
            name = "surfaceContainer",
            color = colorScheme.surfaceContainer,
            onColor = colorScheme.onSurface,
        ),
        ColorRole(
            name = "surfaceContainerHigh",
            color = colorScheme.surfaceContainerHigh,
            onColor = colorScheme.onSurface,
        ),
        ColorRole(
            name = "surfaceContainerHighest",
            color = colorScheme.surfaceContainerHighest,
            onColor = colorScheme.onSurface,
        ),
        ColorRole(
            name = "outline",
            color = colorScheme.outline,
            onColor = colorScheme.onSurface,
        ),
        ColorRole(
            name = "outlineVariant",
            color = colorScheme.outlineVariant,
            onColor = colorScheme.onSurfaceVariant,
        ),
        ColorRole(
            name = "scrim",
            color = colorScheme.scrim,
            onColor = colorScheme.inverseSurface,
        ),
    )
    Column(
        modifier = modifier.padding(ForgeTheme.dimensions.size2x),
        verticalArrangement = Arrangement.spacedBy(ForgeTheme.dimensions.size2x),
    ) {
        roles.forEach { role ->
            Swatch(
                name = role.name,
                color = role.color,
                onColor = role.onColor,
            )
        }
    }
}

@Composable
fun TypographyCatalog(modifier: Modifier = Modifier) {
    val t = MaterialTheme.typography
    val roles = listOf(
        TypographyRole(name = "displayLarge", style = t.displayLarge),
        TypographyRole(name = "displayMedium", style = t.displayMedium),
        TypographyRole(name = "displaySmall", style = t.displaySmall),
        TypographyRole(name = "headlineLarge", style = t.headlineLarge),
        TypographyRole(name = "headlineMedium", style = t.headlineMedium),
        TypographyRole(name = "headlineSmall", style = t.headlineSmall),
        TypographyRole(name = "titleLarge", style = t.titleLarge),
        TypographyRole(name = "titleMedium", style = t.titleMedium),
        TypographyRole(name = "titleSmall", style = t.titleSmall),
        TypographyRole(name = "bodyLarge", style = t.bodyLarge),
        TypographyRole(name = "bodyMedium", style = t.bodyMedium),
        TypographyRole(name = "bodySmall", style = t.bodySmall),
        TypographyRole(name = "labelLarge", style = t.labelLarge),
        TypographyRole(name = "labelMedium", style = t.labelMedium),
        TypographyRole(name = "labelSmall", style = t.labelSmall),
    )
    Column(
        modifier = modifier.padding(ForgeTheme.dimensions.size2x),
        verticalArrangement = Arrangement.spacedBy(ForgeTheme.dimensions.size3x),
    ) {
        roles.forEach { role ->
            Column {
                Text(
                    text = role.name,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = "Forge ${role.name}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = role.style,
                )
            }
        }
    }
}

@Composable
fun ShapesCatalog(modifier: Modifier = Modifier) {
    val shapes = MaterialTheme.shapes
    val shapeRoles = listOf(
        ShapeRole(name = "extraSmall", shape = shapes.extraSmall as RoundedCornerShape),
        ShapeRole(name = "small", shape = shapes.small as RoundedCornerShape),
        ShapeRole(name = "medium", shape = shapes.medium as RoundedCornerShape),
        ShapeRole(name = "large", shape = shapes.large as RoundedCornerShape),
        ShapeRole(name = "extraLarge", shape = shapes.extraLarge as RoundedCornerShape),
    )
    Column(
        modifier = modifier.padding(ForgeTheme.dimensions.size2x),
        verticalArrangement = Arrangement.spacedBy(ForgeTheme.dimensions.size3x),
    ) {
        shapeRoles.forEach { shapeRole ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(ForgeTheme.dimensions.size14x)
                        .clip(shapeRole.shape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
                Spacer(modifier = Modifier.width(ForgeTheme.dimensions.size3x))
                Text(
                    text = shapeRole.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MetricRow(
    name: String,
    value: Dp,
    swatch: @Composable (Dp) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            modifier = Modifier.width(ForgeTheme.dimensions.size20x),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        swatch(value)
        Spacer(modifier = Modifier.width(ForgeTheme.dimensions.size2x))
        Text(
            text = "${value.value.toInt()}dp",
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun SpacingCatalog(modifier: Modifier = Modifier) {
    val steps = listOf(
        "size1x" to ForgeTheme.dimensions.size1x,
        "size2x" to ForgeTheme.dimensions.size2x,
        "size4x" to ForgeTheme.dimensions.size4x,
        "size6x" to ForgeTheme.dimensions.size6x,
        "size8x" to ForgeTheme.dimensions.size8x,
        "size12x" to ForgeTheme.dimensions.size12x,
        "size16x" to ForgeTheme.dimensions.size16x,
    )
    Column(
        modifier = modifier.padding(ForgeTheme.dimensions.size2x),
        verticalArrangement = Arrangement.spacedBy(ForgeTheme.dimensions.size2x),
    ) {
        steps.forEach { (name, value) ->
            MetricRow(
                name = name,
                value = value,
                swatch = { dp ->
                    Box(
                        modifier = Modifier
                            .height(ForgeTheme.dimensions.size4x)
                            .width(dp)
                            .background(MaterialTheme.colorScheme.tertiary),
                    )
                },
            )
        }
    }
}

@Composable
fun ElevationCatalog(modifier: Modifier = Modifier) {
    val levels = listOf(
        "level0" to ForgeTheme.elevations.level0,
        "level1" to ForgeTheme.elevations.level1,
        "level2" to ForgeTheme.elevations.level2,
        "level3" to ForgeTheme.elevations.level3,
        "level4" to ForgeTheme.elevations.level4,
        "level5" to ForgeTheme.elevations.level5,
    )
    Column(
        modifier = modifier.padding(ForgeTheme.dimensions.size2x),
        verticalArrangement = Arrangement.spacedBy(ForgeTheme.dimensions.size3x),
    ) {
        levels.forEach { (name, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(
                        width = ForgeTheme.dimensions.size16x,
                        height = ForgeTheme.dimensions.size10x,
                    ),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = value,
                    shadowElevation = value,
                ) {}
                Spacer(modifier = Modifier.width(ForgeTheme.dimensions.size3x))
                Text(
                    text = "$name (${value.value.toInt()}dp)",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun DesignSystemCatalog(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(vertical = ForgeTheme.dimensions.size4x),
        ) {
            Text(
                text = "Forge Design System",
                modifier = Modifier.padding(horizontal = ForgeTheme.dimensions.size4x),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            )
            CatalogSection(title = "Color roles") { ColorRolesCatalog() }
            CatalogSection(title = "Typography") { TypographyCatalog() }
            CatalogSection(title = "Shapes") { ShapesCatalog() }
            CatalogSection(title = "Spacing") { SpacingCatalog() }
            CatalogSection(title = "Elevation") { ElevationCatalog() }
        }
    }
}

@Preview(
    name = "Catalog — Dark",
    showBackground = true,
)
@Composable
private fun DesignSystemCatalogDarkPreview() {
    ForgeTheme(darkTheme = true) {
        DesignSystemCatalog()
    }
}

@Preview(
    name = "Catalog — Light",
    showBackground = true,
)
@Composable
private fun DesignSystemCatalogLightPreview() {
    ForgeTheme(darkTheme = false) {
        DesignSystemCatalog()
    }
}
