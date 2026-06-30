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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import app.honguyen.forge.designsystem.theme.Dimensions
import app.honguyen.forge.designsystem.theme.Elevations
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
            .padding(Dimensions.Size4x),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(Dimensions.Size3x))
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
            .height(Dimensions.Size12x)
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .border(
                width = Dimensions.SizeUnit,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.small,
            )
            .padding(Dimensions.Size2x),
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
    val cs = MaterialTheme.colorScheme
    val ext = LocalForgeExtendedColors.current
    Column(
        modifier = modifier.padding(Dimensions.Size2x),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Size2x),
    ) {
        Swatch(
            name = "primary",
            color = cs.primary,
            onColor = cs.onPrimary,
        )
        Swatch(
            name = "primaryContainer",
            color = cs.primaryContainer,
            onColor = cs.onPrimaryContainer,
        )
        Swatch(
            name = "secondary",
            color = cs.secondary,
            onColor = cs.onSecondary,
        )
        Swatch(
            name = "tertiary",
            color = cs.tertiary,
            onColor = cs.onTertiary,
        )
        Swatch(
            name = "error",
            color = cs.error,
            onColor = cs.onError,
        )
        Swatch(
            name = "live (extended)",
            color = ext.live,
            onColor = ext.onLive,
        )
        Swatch(
            name = "surface",
            color = cs.surface,
            onColor = cs.onSurface,
        )
        Swatch(
            name = "surfaceContainer",
            color = cs.surfaceContainer,
            onColor = cs.onSurface,
        )
        Swatch(
            name = "surfaceContainerHighest",
            color = cs.surfaceContainerHighest,
            onColor = cs.onSurface,
        )
    }
}

@Composable
fun TypographyCatalog(modifier: Modifier = Modifier) {
    data class Role(
        val name: String,
        val style: TextStyle,
    )

    val t = MaterialTheme.typography
    val roles = listOf(
        Role(name = "displayLarge", style = t.displayLarge),
        Role(name = "displayMedium", style = t.displayMedium),
        Role(name = "displaySmall", style = t.displaySmall),
        Role(name = "headlineLarge", style = t.headlineLarge),
        Role(name = "headlineMedium", style = t.headlineMedium),
        Role(name = "headlineSmall", style = t.headlineSmall),
        Role(name = "titleLarge", style = t.titleLarge),
        Role(name = "titleMedium", style = t.titleMedium),
        Role(name = "titleSmall", style = t.titleSmall),
        Role(name = "bodyLarge", style = t.bodyLarge),
        Role(name = "bodyMedium", style = t.bodyMedium),
        Role(name = "bodySmall", style = t.bodySmall),
        Role(name = "labelLarge", style = t.labelLarge),
        Role(name = "labelMedium", style = t.labelMedium),
        Role(name = "labelSmall", style = t.labelSmall),
    )
    Column(
        modifier = modifier.padding(Dimensions.Size2x),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Size3x),
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
    data class Sh(
        val name: String,
        val shape: RoundedCornerShape,
    )

    val s = MaterialTheme.shapes
    val shapes = listOf(
        Sh(name = "extraSmall", shape = s.extraSmall as RoundedCornerShape),
        Sh(name = "small", shape = s.small as RoundedCornerShape),
        Sh(name = "medium", shape = s.medium as RoundedCornerShape),
        Sh(name = "large", shape = s.large as RoundedCornerShape),
        Sh(name = "extraLarge", shape = s.extraLarge as RoundedCornerShape),
    )
    Column(
        modifier = modifier.padding(Dimensions.Size2x),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Size3x),
    ) {
        shapes.forEach { sh ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.Size14x)
                        .clip(sh.shape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                )
                Spacer(modifier = Modifier.width(Dimensions.Size3x))
                Text(
                    text = sh.name,
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
            modifier = Modifier.width(Dimensions.Size20x),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        swatch(value)
        Spacer(modifier = Modifier.width(Dimensions.Size2x))
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
        "Size1x" to Dimensions.Size1x,
        "Size2x" to Dimensions.Size2x,
        "Size4x" to Dimensions.Size4x,
        "Size6x" to Dimensions.Size6x,
        "Size8x" to Dimensions.Size8x,
        "Size12x" to Dimensions.Size12x,
        "Size16x" to Dimensions.Size16x,
    )
    Column(
        modifier = modifier.padding(Dimensions.Size2x),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Size2x),
    ) {
        steps.forEach { (name, value) ->
            MetricRow(
                name = name,
                value = value,
                swatch = { dp ->
                    Box(
                        modifier = Modifier
                            .height(Dimensions.Size4x)
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
        "Level0" to Elevations.Level0,
        "Level1" to Elevations.Level1,
        "Level2" to Elevations.Level2,
        "Level3" to Elevations.Level3,
        "Level4" to Elevations.Level4,
        "Level5" to Elevations.Level5,
    )
    Column(
        modifier = modifier.padding(Dimensions.Size2x),
        verticalArrangement = Arrangement.spacedBy(Dimensions.Size3x),
    ) {
        levels.forEach { (name, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(
                        width = Dimensions.Size16x,
                        height = Dimensions.Size10x,
                    ),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = value,
                    shadowElevation = value,
                ) {}
                Spacer(modifier = Modifier.width(Dimensions.Size3x))
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
                .padding(vertical = Dimensions.Size4x),
        ) {
            Text(
                text = "Forge Design System",
                modifier = Modifier.padding(horizontal = Dimensions.Size4x),
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
    heightDp = 2400,
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
    heightDp = 2400,
)
@Composable
private fun DesignSystemCatalogLightPreview() {
    ForgeTheme(darkTheme = false) {
        DesignSystemCatalog()
    }
}
