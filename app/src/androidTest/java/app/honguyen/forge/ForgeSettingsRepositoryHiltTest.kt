package app.honguyen.forge

import app.honguyen.forge.data.model.Theme
import app.honguyen.forge.data.repository.ForgeSettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Proves the settings layer resolves through the real Hilt graph and survives a round
 * trip to the real on-device file.
 *
 * Nothing in the UI consumes the repository yet, so without this the DataStore and
 * dispatcher bindings would never be requested and a missing one would go unnoticed.
 */
@HiltAndroidTest
class ForgeSettingsRepositoryHiltTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: ForgeSettingsRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun writesAndReadsBackTheme() =
        runBlocking {
            repository.setTheme(Theme.Light)

            assertEquals(Theme.Light, repository.settings.first().theme)
        }
}
