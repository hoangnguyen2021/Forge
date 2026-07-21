package app.honguyen.forge.data.repository

import androidx.datastore.core.DataStoreFactory
import app.honguyen.forge.data.model.Theme
import app.honguyen.forge.datastore.serializer.ForgeSettingsSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Exercises the repository against a real DataStore over a temp file rather than a fake,
 * so the serializer, the atomic write path and real disk I/O are all under test. Each
 * test gets a fresh file, so nothing leaks between them.
 */
class ForgeSettingsRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun repository(): ForgeSettingsRepository =
        ForgeSettingsRepositoryImpl(
            DataStoreFactory.create(
                serializer = ForgeSettingsSerializer,
                scope = scope,
                // Named, not created: DataStore treats a missing file as "no value yet".
            ) { File(temporaryFolder.root, "forge_settings.pb") },
        )

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `emits the default theme when nothing has been written`() =
        runBlocking {
            assertEquals(Theme.Dark, repository().settings.first().theme)
        }

    @Test
    fun `persists a written theme`() =
        runBlocking {
            val repository = repository()

            repository.setTheme(Theme.Light)

            assertEquals(Theme.Light, repository.settings.first().theme)
        }

    @Test
    fun `later writes win`() =
        runBlocking {
            val repository = repository()

            repository.setTheme(Theme.Light)
            repository.setTheme(Theme.Dark)

            assertEquals(Theme.Dark, repository.settings.first().theme)
        }
}
