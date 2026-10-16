package com.harish.gallery.feature.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.data.actions.MediaActions
import com.harish.gallery.core.data.locked.LockedFolderStore
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.domain.timeline.groupTimeline
import com.harish.gallery.core.domain.usecase.GetAlbumNameUseCase
import com.harish.gallery.core.domain.usecase.GetAlbumsUseCase
import com.harish.gallery.core.domain.usecase.GetMediaForSourceUseCase
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource
import com.harish.gallery.core.model.TimelineGrouping
import com.harish.gallery.core.model.TimelineSection
import com.harish.gallery.core.ui.MediaSelectionViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** One album, Favourites, Videos or Trash — whichever [MediaSource] the route asked for. */
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getMedia: GetMediaForSourceUseCase,
    getAlbumName: GetAlbumNameUseCase,
    getAlbums: GetAlbumsUseCase,
    private val settingsRepository: SettingsRepository,
    private val mediaActions: MediaActions,
    lockedFolder: LockedFolderStore
) : MediaSelectionViewModel(mediaActions, lockedFolder, getAlbums) {

    val source: MediaSource = MediaSource.fromKey(savedStateHandle.get<String>(ARG_SOURCE).orEmpty()) // Navigation already decoded it
    val isTrash: Boolean get() = source == MediaSource.Trash

    val items: StateFlow<List<MediaItem>> = getMedia(source)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Albums are long lists, so they're split by month (the trash isn't split at all). */
    val sections: StateFlow<List<TimelineSection>> = items.map { list ->
        if (isTrash) listOf(TimelineSection("trash", "Trash", list))
        else groupTimeline(list, TimelineGrouping.MONTH, LocalDate.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val title: StateFlow<String> = titleFlow(getAlbumName)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val columns: StateFlow<Int> = settingsRepository.settings.map { it.columns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settingsRepository.settings.value.columns)

    fun setColumns(columns: Int) = settingsRepository.setColumns(columns)

    fun restore(items: List<MediaItem>) = actions.run(::clearSelection) { mediaActions.restoreFromTrash(items) }
    fun deleteForever(items: List<MediaItem>) = actions.run(::clearSelection) { mediaActions.deleteForever(items) }

    private fun titleFlow(getAlbumName: GetAlbumNameUseCase): Flow<String> = when (source) {
        MediaSource.Favorites -> flowOf("Favourites")
        MediaSource.Videos -> flowOf("Videos")
        MediaSource.Trash -> flowOf("Trash")
        MediaSource.All -> flowOf("Photos")
        is MediaSource.Search -> flowOf(source.query)
        is MediaSource.InAlbum -> getAlbumName(source.albumId).map { it ?: "Album" }
    }

    companion object {
        const val ARG_SOURCE = "source"
    }
}
