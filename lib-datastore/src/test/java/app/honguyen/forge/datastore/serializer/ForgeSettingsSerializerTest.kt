package app.honguyen.forge.datastore.serializer

import androidx.datastore.core.CorruptionException
import app.honguyen.forge.datastore.proto.ForgeSettingsProto
import app.honguyen.forge.datastore.proto.ThemeProto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ForgeSettingsSerializerTest {
    @Test
    fun `default value leaves theme unspecified`() {
        assertEquals(ThemeProto.THEME_UNSPECIFIED, ForgeSettingsSerializer.defaultValue.theme)
    }

    @Test
    fun `writes and reads back the same value`() =
        runBlocking {
            val written = ForgeSettingsProto.newBuilder()
                .setTheme(ThemeProto.THEME_LIGHT)
                .build()

            val bytes = ByteArrayOutputStream()
                .also { ForgeSettingsSerializer.writeTo(written, it) }
                .toByteArray()
            val read = ForgeSettingsSerializer.readFrom(ByteArrayInputStream(bytes))

            assertEquals(written, read)
        }

    /**
     * A lone tag byte promises a varint that never arrives, so the parser fails. It has to
     * surface as [CorruptionException] specifically — that is the only exception the
     * DataStore corruption handler acts on.
     */
    @Test(expected = CorruptionException::class)
    fun `truncated input is reported as corruption`(): Unit =
        runBlocking {
            val truncated = byteArrayOf(0x08)

            ForgeSettingsSerializer.readFrom(ByteArrayInputStream(truncated))
            Unit
        }
}
