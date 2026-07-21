package app.honguyen.forge.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.MessageLite
import java.io.InputStream
import java.io.OutputStream

/**
 * Base [Serializer] for a protobuf-backed DataStore. Writing is identical for every
 * message type, and reading only differs by which generated `parseFrom` to call, so
 * subclasses supply just that.
 *
 * Translating protobuf's [InvalidProtocolBufferException] into DataStore's
 * [CorruptionException] is what lets a corruption handler recover; without it DataStore
 * sees an unrecognized exception and rethrows, failing every read for good.
 */
abstract class ProtoSerializer<T : MessageLite> : Serializer<T> {
    protected abstract fun parseFrom(input: InputStream): T

    final override suspend fun readFrom(input: InputStream): T =
        try {
            parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Unable to read proto from DataStore", exception)
        }

    final override suspend fun writeTo(
        t: T,
        output: OutputStream,
    ) = t.writeTo(output)
}
