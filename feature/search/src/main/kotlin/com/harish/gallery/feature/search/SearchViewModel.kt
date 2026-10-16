package com.harish.gallery.feature.search

import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.data.actions.MediaActions
import com.harish.gallery.core.data.locked.LockedFolderStore
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.domain.search.searchSuggestions
import com.harish.gallery.core.domain.timeline.groupTimeline
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getMedia: GetMediaForSourceUseCase,
    getAlbums: GetAlbumsUseCase,
    mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository,
    mediaActions: MediaActions,
    lockedFolder: LockedFolderStore
) : MediaSelectionViewModel(mediaActions, lockedFolder, getAlbums) {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Results for what you typed (waits 250 ms after the last key, so typing stays smooth). */
    val results: StateFlow<List<MediaItem>> = _query
        .debounce(250)
        .flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else getMedia(MediaSource.Search(q.trim())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sections: StateFlow<List<TimelineSection>> = results
        .map { groupTimeline(it, TimelineGrouping.MONTH, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Chips under the search box: Videos, Favourites, years, then your biggest albums. */
    val suggestions: StateFlow<List<String>> = combine(mediaRepository.media, albums) { media, albums ->
        searchSuggestions(media) + albums.sortedByDescending { it.count }.take(6).map { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val columns: StateFlow<Int> = settingsRepository.settings.map { it.columns }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), settingsRepository.settings.value.columns)

    fun setQuery(text: String) {
        _query.value = text
        clearSelection()
    }

    fun setColumns(columns: Int) = settingsRepository.setColumns(columns)
}
