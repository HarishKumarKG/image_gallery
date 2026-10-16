package com.harish.gallery.core.domain.usecase

import com.harish.gallery.core.domain.album.groupAlbums
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.domain.search.searchMedia
import com.harish.gallery.core.domain.timeline.groupTimeline
import com.harish.gallery.core.model.Album
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource
import com.harish.gallery.core.model.TimelineSection
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/*
 * Use cases = one small job each, written as plain Kotlin so they can be tested without a phone.
 * ViewModels call these instead of talking to repositories directly.
 */

/** The Photos tab: every visible photo/video in date sections (hidden albums left out). */
class GetTimelineUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<List<TimelineSection>> =
        combine(mediaRepository.media, settingsRepository.settings) { items, settings ->
            val visible = items.filter { it.albumId !in settings.hiddenAlbumIds }
            groupTimeline(visible, settings.grouping, LocalDate.now())
        }.flowOn(Dispatchers.Default)
}

/** The Albums tab: one album per folder, in the chosen order. */
class GetAlbumsUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<List<Album>> =
        combine(mediaRepository.media, settingsRepository.settings) { items, settings ->
            groupAlbums(items, settings.albumSort, settings.hiddenAlbumIds)
        }.flowOn(Dispatchers.Default)
}

/** The list behind an album, favourites, videos, trash or a search — and behind the viewer's swiping. */
class GetMediaForSourceUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(source: MediaSource): Flow<List<MediaItem>> = when (source) {
        MediaSource.Trash -> mediaRepository.trashed
        else -> combine(mediaRepository.media, settingsRepository.settings) { items, settings ->
            when (source) {
                MediaSource.All -> items.filter { it.albumId !in settings.hiddenAlbumIds }
                MediaSource.Favorites -> items.filter { it.isFavorite }
                MediaSource.Videos -> items.filter { it.isVideo }
                is MediaSource.InAlbum -> items.filter { it.albumId == source.albumId }
                is MediaSource.Search -> searchMedia(items, source.query)
                MediaSource.Trash -> emptyList() // handled above
            }
        }.flowOn(Dispatchers.Default)
    }
}

/** Album name for a title bar ("Camera"), or null if the album is gone. */
class GetAlbumNameUseCase @Inject constructor(private val mediaRepository: MediaRepository) {
    operator fun invoke(albumId: String): Flow<String?> =
        mediaRepository.media.map { items -> items.firstOrNull { it.albumId == albumId }?.albumName }
}
