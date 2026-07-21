package app.honguyen.forge.data.repository

import app.honguyen.forge.data.model.ForgeSettings
import app.honguyen.forge.data.model.Theme
import kotlinx.coroutines.flow.Flow

/**
 * Read and write app-wide settings.
 *
 * [settings] emits the current value immediately on collection and again on every write,
 * so callers observe rather than poll. Writes suspend until they are durable on disk.
 */
interface ForgeSettingsRepository {
    val settings: Flow<ForgeSettings>

    suspend fun setTheme(theme: Theme)
}
