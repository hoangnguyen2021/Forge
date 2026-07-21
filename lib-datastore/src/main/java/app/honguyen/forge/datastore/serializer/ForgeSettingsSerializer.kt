package app.honguyen.forge.datastore.serializer

import app.honguyen.forge.datastore.proto.ForgeSettingsProto
import java.io.InputStream

/**
 * Reads and writes [ForgeSettingsProto]. The default instance has every field at its
 * proto zero value, which the domain mapping layer folds into real defaults.
 */
object ForgeSettingsSerializer : ProtoSerializer<ForgeSettingsProto>() {
    override val defaultValue: ForgeSettingsProto = ForgeSettingsProto.getDefaultInstance()

    override fun parseFrom(input: InputStream): ForgeSettingsProto = ForgeSettingsProto.parseFrom(input)
}
