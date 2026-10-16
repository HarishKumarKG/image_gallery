package com.harish.gallery.feature.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.domain.repository.LockedFolderRepository
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.domain.usecase.GetAlbumsUseCase
import com.harish.gallery.core.model.Album
import com.harish.gallery.core.model.AlbumSort
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Counts for the "Collections" row at the top of Albums. */
data class CollectionCounts(
    val favorites: Int = 0,
    val videos: Int = 0,
    val trash: Int = 0,
    val locked: Int = 0,
    val favoriteCover: String? = null,
    val videoCover: String? = null
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    getAlbums: GetAlbumsUseCase,
    mediaRepository: MediaRepository,
    lockedFolder: LockedFolderRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = getAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val counts: StateFlow<CollectionCounts> = combine(
        mediaRepository.media, mediaRepository.trashed, lockedFolder.items
    ) { media, trash, locked ->
        val favorites = media.filter { it.isFavorite }
        val videos = media.filter { it.isVideo }
        CollectionCounts(
            favorites = favorites.size,
            videos = videos.size,
            trash = trash.size,
            locked = locked.size,
            favoriteCover = favorites.firstOrNull()?.uri,
            videoCover = videos.firstOrNull()?.uri
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionCounts())

    val sort: StateFlow<AlbumSort> = settingsRepository.settings.map { it.albumSort }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumSort.RECENT)

    fun setSort(sort: AlbumSort) = settingsRepository.setAlbumSort(sort)
    fun setHidden(album: Album, hidden: Boolean) = settingsRepository.setAlbumHidden(album.id, hidden)
}
