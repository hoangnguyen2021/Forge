package app.honguyen.forge.data.mapper

import app.honguyen.forge.data.model.Theme
import app.honguyen.forge.datastore.proto.ForgeSettingsProto
import app.honguyen.forge.datastore.proto.ThemeProto
import org.junit.Assert.assertEquals
import org.junit.Test

/** A theme number no build has ever written, standing in for one a newer build might. */
private const val UNKNOWN_THEME_NUMBER = 99

class ForgeSettingsMapperTest {
    @Test
    fun `maps each known theme`() {
        assertEquals(Theme.Light, protoWithTheme(ThemeProto.THEME_LIGHT).toDomain().theme)
        assertEquals(Theme.Dark, protoWithTheme(ThemeProto.THEME_DARK).toDomain().theme)
    }

    @Test
    fun `unset theme falls back to the default`() {
        val unset = ForgeSettingsProto.getDefaultInstance()

        assertEquals(Theme.Dark, unset.toDomain().theme)
    }

    /**
     * Reading a value written by a newer build: proto decodes the unknown number to
     * `UNRECOGNIZED` rather than failing, and the domain must not leak that state.
     */
    @Test
    fun `unrecognized theme falls back to the default`() {
        val fromNewerBuild = ForgeSettingsProto.newBuilder()
            .setThemeValue(UNKNOWN_THEME_NUMBER)
            .build()

        assertEquals(ThemeProto.UNRECOGNIZED, fromNewerBuild.theme)
        assertEquals(Theme.Dark, fromNewerBuild.toDomain().theme)
    }

    @Test
    fun `round trips every domain theme`() {
        Theme.entries.forEach { theme ->
            assertEquals(theme, protoWithTheme(theme.toProto()).toDomain().theme)
        }
    }

    private fun protoWithTheme(theme: ThemeProto): ForgeSettingsProto =
        ForgeSettingsProto.newBuilder()
            .setTheme(theme)
            .build()
}
