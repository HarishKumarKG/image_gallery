package com.harish.gallery.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.data.actions.MediaActions
import com.harish.gallery.core.data.locked.LockedFolderStore
import com.harish.gallery.core.domain.usecase.GetAlbumsUseCase
import com.harish.gallery.core.model.Album
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.TimelineSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Shared by every screen that shows a grid you can select from (Photos, an album, search).
 * Holds the selection and runs the bottom-bar actions; each screen only adds what it shows.
 */
abstract class MediaSelectionViewModel(
    private val mediaActions: MediaActions,
    private val lockedFolder: LockedFolderStore,
    getAlbums: GetAlbumsUseCase
) : ViewModel() {

    val actions = ActionController(viewModelScope)

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    /** Albums for the "Move to album" / "Copy to album" picker. */
    val albums: StateFlow<List<Album>> = getAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(item: MediaItem) = _selection.update { it.toggle(item.id) }
    fun toggleSection(section: TimelineSection) = _selection.update { it.toggleSection(section) }
    fun selectAll(items: List<MediaItem>) = _selection.update { items.mapTo(HashSet()) { it.id } }
    fun clearSelection() { _selection.value = emptySet() }

    fun moveToTrash(items: List<MediaItem>) = actions.run(::clearSelection) { mediaActions.moveToTrash(items) }

    /** Favourite them all, or un-favourite if every one already is. */
    fun toggleFavorite(items: List<MediaItem>) {
        val makeFavorite = !items.all { it.isFavorite }
        actions.run(::clearSelection) { mediaActions.setFavorite(items, makeFavorite) }
    }

    fun moveTo(items: List<MediaItem>, target: AlbumTarget) =
        actions.run(::clearSelection) { mediaActions.moveTo(items, target.relativePath, target.name) }

    fun copyTo(items: List<MediaItem>, target: AlbumTarget) =
        actions.run(::clearSelection) { mediaActions.copyTo(items, target.relativePath, target.name) }

    fun moveToLockedFolder(items: List<MediaItem>) = actions.run(::clearSelection) { lockedFolder.lock(items) }
}
