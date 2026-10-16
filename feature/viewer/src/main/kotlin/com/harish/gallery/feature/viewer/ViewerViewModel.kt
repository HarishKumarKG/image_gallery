package com.harish.gallery.feature.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.data.actions.MediaActions
import com.harish.gallery.core.data.locked.LockedFolderStore
import com.harish.gallery.core.data.media.MediaDetailsReader
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.domain.usecase.GetAlbumsUseCase
import com.harish.gallery.core.domain.usecase.GetMediaForSourceUseCase
import com.harish.gallery.core.model.Album
import com.harish.gallery.core.model.MediaDetails
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource
import com.harish.gallery.core.ui.ActionController
import com.harish.gallery.core.ui.AlbumTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Full-screen viewer. Swipes through the same list you opened it from
 * (all photos, an album, favourites, a search...).
 */
@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getMedia: GetMediaForSourceUseCase,
    getAlbums: GetAlbumsUseCase,
    settingsRepository: SettingsRepository,
    mediaRepository: MediaRepository,
    private val mediaActions: MediaActions,
    private val lockedFolder: LockedFolderStore,
    private val detailsReader: MediaDetailsReader
) : ViewModel() {

    val source: MediaSource = MediaSource.fromKey(savedStateHandle.get<String>(ARG_SOURCE).orEmpty())
    val startId: Long = savedStateHandle.get<String>(ARG_ID)?.toLongOrNull() ?: -1L
    val isTrash: Boolean get() = source == MediaSource.Trash

    val actions = ActionController(viewModelScope)

    /**
     * null = still loading, so the pager doesn't open at the wrong photo (or close itself because
     * the list looks empty right after Android restarted the app).
     */
    val items: StateFlow<List<MediaItem>?> = combine(mediaRepository.isLoading, getMedia(source)) { loading, list ->
        if (loading) null else list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val albums: StateFlow<List<Album>> = getAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val slideshowSeconds: StateFlow<Int> = settingsRepository.settings.map { it.slideshowSeconds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 3)

    private val _details = MutableStateFlow<MediaDetails?>(null)
    val details: StateFlow<MediaDetails?> = _details.asStateFlow()

    fun loadDetails(item: MediaItem) {
        _details.value = null
        viewModelScope.launch { _details.value = detailsReader.read(item) }
    }

    fun toggleFavorite(item: MediaItem) = actions.run { mediaActions.setFavorite(listOf(item), !item.isFavorite) }
    fun moveToTrash(item: MediaItem) = actions.run { mediaActions.moveToTrash(listOf(item)) }
    fun restore(item: MediaItem) = actions.run { mediaActions.restoreFromTrash(listOf(item)) }
    fun deleteForever(item: MediaItem) = actions.run { mediaActions.deleteForever(listOf(item)) }
    fun rename(item: MediaItem, newName: String) = actions.run { mediaActions.rename(item, newName) }
    fun moveTo(item: MediaItem, target: AlbumTarget) = actions.run { mediaActions.moveTo(listOf(item), target.relativePath, target.name) }
    fun copyTo(item: MediaItem, target: AlbumTarget) = actions.run { mediaActions.copyTo(listOf(item), target.relativePath, target.name) }
    fun moveToLockedFolder(item: MediaItem) = actions.run { lockedFolder.lock(listOf(item)) }

    companion object {
        const val ARG_SOURCE = "source"
        const val ARG_ID = "id"
    }
}
