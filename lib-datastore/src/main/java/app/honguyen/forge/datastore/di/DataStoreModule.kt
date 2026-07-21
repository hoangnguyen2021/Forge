package app.honguyen.forge.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import app.honguyen.forge.coroutines.ApplicationScope
import app.honguyen.forge.coroutines.Dispatcher
import app.honguyen.forge.coroutines.ForgeDispatcher
import app.honguyen.forge.datastore.proto.ForgeSettingsProto
import app.honguyen.forge.datastore.serializer.ForgeSettingsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

private const val FORGE_SETTINGS_FILE = "forge_settings.pb"

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    /**
     * DataStore requires exactly one instance per file for the whole process — a second
     * one over the same file throws — so this binding must stay [Singleton].
     */
    @Provides
    @Singleton
    fun providesForgeSettingsDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(ForgeDispatcher.IO) ioDispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
    ): DataStore<ForgeSettingsProto> =
        DataStoreFactory.create(
            serializer = ForgeSettingsSerializer,
            // A corrupt file cannot be repaired, and rethrowing would fail every read for the
            // life of the install. Settings are cheap to lose, so reset to defaults instead.
            corruptionHandler = ReplaceFileCorruptionHandler { ForgeSettingsProto.getDefaultInstance() },
            scope = CoroutineScope(scope.coroutineContext + ioDispatcher),
        ) {
            context.dataStoreFile(FORGE_SETTINGS_FILE)
        }
}
