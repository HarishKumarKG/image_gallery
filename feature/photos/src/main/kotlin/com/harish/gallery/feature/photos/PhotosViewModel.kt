package com.harish.gallery.feature.photos

import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.data.actions.MediaActions
import com.harish.gallery.core.data.locked.LockedFolderStore
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.domain.usecase.GetAlbumsUseCase
import com.harish.gallery.core.domain.usecase.GetTimelineUseCase
import com.harish.gallery.core.model.TimelineSection
import com.harish.gallery.core.ui.MediaSelectionViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PhotosViewModel @Inject constructor(
    getTimeline: GetTimelineUseCase,
    getAlbums: GetAlbumsUseCase,
    mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    mediaActions: MediaActions,
    lockedFolder: LockedFolderStore
) : MediaSelectionViewModel(mediaActions, lockedFolder, getAlbums) {

    val sections: StateFlow<List<TimelineSection>> = getTimeline()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val columns: StateFlow<Int> = settingsRepository.settings.map { it.columns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settingsRepository.settings.value.columns)

    val isLoading: StateFlow<Boolean> = mediaRepository.isLoading

    fun setColumns(columns: Int) = settingsRepository.setColumns(columns)
}
