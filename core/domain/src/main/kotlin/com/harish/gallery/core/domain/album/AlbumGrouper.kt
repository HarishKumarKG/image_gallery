package com.harish.gallery.core.domain.album

import com.harish.gallery.core.model.Album
import com.harish.gallery.core.model.AlbumSort
import com.harish.gallery.core.model.MediaItem

/**
 * One album per folder. The newest item becomes the cover.
 * [items] must be sorted newest first.
 *
 * Example: 3 photos in "DCIM/Camera/" + 1 in "Pictures/Screenshots/" -> Camera (3), Screenshots (1)
 */
fun groupAlbums(items: List<MediaItem>, sort: AlbumSort, hiddenIds: Set<String> = emptySet()): List<Album> {
    val albums = items.groupBy { it.albumId }.map { (id, inAlbum) ->
        val newest = inAlbum.first()
        Album(
            id = id,
            name = newest.albumName.ifBlank { "Internal storage" },
            relativePath = newest.relativePath,
            cover = newest,
            count = inAlbum.size,
            latestMs = newest.dateTakenMs,
            isHidden = id in hiddenIds
        )
    }
    return when (sort) {
        AlbumSort.RECENT -> albums.sortedByDescending { it.latestMs }
        AlbumSort.NAME -> albums.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        AlbumSort.SIZE -> albums.sortedByDescending { it.count }
    }
}

/** Days left before Android deletes a trashed item for good (0 = today). */
fun trashDaysLeft(expiresSec: Long, nowSec: Long): Int {
    if (expiresSec <= 0) return 0
    val left = (expiresSec - nowSec).coerceAtLeast(0)
    return ((left + 86_399) / 86_400).toInt() // round up: 1.2 days left -> "2 days"
}
