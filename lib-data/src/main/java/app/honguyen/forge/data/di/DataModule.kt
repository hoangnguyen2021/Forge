package app.honguyen.forge.data.di

import app.honguyen.forge.data.repository.ForgeSettingsRepository
import app.honguyen.forge.data.repository.ForgeSettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    @Singleton
    fun bindsForgeSettingsRepository(impl: ForgeSettingsRepositoryImpl): ForgeSettingsRepository
}
