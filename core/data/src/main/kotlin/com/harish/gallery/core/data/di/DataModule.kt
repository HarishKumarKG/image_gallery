package com.harish.gallery.core.data.di

import com.harish.gallery.core.data.locked.LockedFolderStore
import com.harish.gallery.core.data.media.MediaStoreMediaRepository
import com.harish.gallery.core.data.settings.PrefsSettingsRepository
import com.harish.gallery.core.domain.repository.LockedFolderRepository
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Tells Hilt which class to use for each domain interface. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds abstract fun mediaRepository(impl: MediaStoreMediaRepository): MediaRepository
    @Binds abstract fun settingsRepository(impl: PrefsSettingsRepository): SettingsRepository
    @Binds abstract fun lockedFolderRepository(impl: LockedFolderStore): LockedFolderRepository

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
