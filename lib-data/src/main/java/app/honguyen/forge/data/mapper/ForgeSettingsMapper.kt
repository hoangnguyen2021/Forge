package app.honguyen.forge.data.mapper

import app.honguyen.forge.data.model.ForgeSettings
import app.honguyen.forge.data.model.Theme
import app.honguyen.forge.datastore.proto.ForgeSettingsProto
import app.honguyen.forge.datastore.proto.ThemeProto

/**
 * Converts the persisted proto into its domain form.
 *
 * Proto contributes two states the domain has no use for: `THEME_UNSPECIFIED`, which is
 * what an unwritten field decodes to, and `UNRECOGNIZED`, which is what a value written
 * by a newer build decodes to on an older one. Both mean "no usable choice on disk", so
 * both collapse to the domain default and callers never see them.
 */
internal fun ForgeSettingsProto.toDomain(): ForgeSettings =
    ForgeSettings(
        theme = when (theme) {
            ThemeProto.THEME_LIGHT -> Theme.Light
            ThemeProto.THEME_DARK -> Theme.Dark
            ThemeProto.THEME_UNSPECIFIED,
            ThemeProto.UNRECOGNIZED,
            null,
            -> ForgeSettings().theme
        },
    )

internal fun Theme.toProto(): ThemeProto =
    when (this) {
        Theme.Light -> ThemeProto.THEME_LIGHT
        Theme.Dark -> ThemeProto.THEME_DARK
    }
