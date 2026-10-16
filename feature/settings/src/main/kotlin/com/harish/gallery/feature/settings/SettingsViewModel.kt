package com.harish.gallery.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.domain.usecase.GetAlbumsUseCase
import com.harish.gallery.core.model.Album
import com.harish.gallery.core.model.GallerySettings
import com.harish.gallery.core.model.ThemeMode
import com.harish.gallery.core.model.TimelineGrouping
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    getAlbums: GetAlbumsUseCase
) : ViewModel() {

    val settings: StateFlow<GallerySettings> = settingsRepository.settings

    val hiddenAlbums: StateFlow<List<Album>> = getAlbums().map { albums -> albums.filter { it.isHidden } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTheme(mode: ThemeMode) = settingsRepository.setThemeMode(mode)
    fun setGrouping(grouping: TimelineGrouping) = settingsRepository.setGrouping(grouping)
    fun setColumns(columns: Int) = settingsRepository.setColumns(columns)
    fun setSlideshowSeconds(seconds: Int) = settingsRepository.setSlideshowSeconds(seconds)
    fun showAlbum(album: Album) = settingsRepository.setAlbumHidden(album.id, false)
}
