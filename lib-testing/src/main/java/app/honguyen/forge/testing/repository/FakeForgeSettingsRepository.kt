package app.honguyen.forge.testing.repository

import app.honguyen.forge.data.model.ForgeSettings
import app.honguyen.forge.data.model.Theme
import app.honguyen.forge.data.repository.ForgeSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [ForgeSettingsRepository] for tests that need settings without touching disk.
 *
 * Backed by a [MutableStateFlow], so it matches the real repository's contract of
 * replaying the current value to every new collector.
 */
class FakeForgeSettingsRepository(
    initial: ForgeSettings = ForgeSettings(),
) : ForgeSettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<ForgeSettings> = state.asStateFlow()

    override suspend fun setTheme(theme: Theme) {
        state.value = state.value.copy(theme = theme)
    }
}
