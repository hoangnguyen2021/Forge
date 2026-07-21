package app.honguyen.forge.datastore.ext

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import java.io.IOException

/**
 * Reads [DataStore.data], substituting [default] when the read fails with an
 * [IOException].
 *
 * A read failure means the file is unreadable right now — not that the caller passed
 * anything wrong — so surfacing it as a thrown exception would take down whichever
 * screen happens to be collecting. Anything that is not an IO failure is a real bug and
 * is rethrown.
 */
fun <T> DataStore<T>.dataOrDefault(default: T): Flow<T> =
    data.catch { cause ->
        if (cause is IOException) {
            Timber.e(cause, "Failed to read from DataStore, falling back to default")
            emit(default)
        } else {
            throw cause
        }
    }
