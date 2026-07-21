package app.honguyen.forge.data.repository

import androidx.datastore.core.DataStore
import app.honguyen.forge.data.mapper.toDomain
import app.honguyen.forge.data.mapper.toProto
import app.honguyen.forge.data.model.ForgeSettings
import app.honguyen.forge.data.model.Theme
import app.honguyen.forge.datastore.ext.dataOrDefault
import app.honguyen.forge.datastore.proto.ForgeSettingsProto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ForgeSettingsRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<ForgeSettingsProto>,
    ) : ForgeSettingsRepository {
        override val settings: Flow<ForgeSettings> =
            dataStore
                .dataOrDefault(ForgeSettingsProto.getDefaultInstance())
                .map(ForgeSettingsProto::toDomain)

        /**
         * [DataStore.updateData] reads, transforms and writes atomically, so concurrent
         * writers cannot clobber each other's fields the way a read-then-write would.
         */
        override suspend fun setTheme(theme: Theme) {
            dataStore.updateData { current ->
                current.toBuilder()
                    .setTheme(theme.toProto())
                    .build()
            }
        }
    }
