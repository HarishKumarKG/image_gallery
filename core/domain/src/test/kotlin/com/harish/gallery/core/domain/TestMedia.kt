package com.harish.gallery.core.domain

import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaType
import java.time.LocalDateTime
import java.time.ZoneOffset

val UTC: ZoneOffset = ZoneOffset.UTC

/** Small helper so tests can make a photo in one line. */
fun media(
    id: Long,
    taken: LocalDateTime = LocalDateTime.of(2026, 10, 14, 10, 0),
    name: String = "IMG_$id.jpg",
    album: String = "Camera",
    albumId: String = album.lowercase(),
    video: Boolean = false,
    favorite: Boolean = false
) = MediaItem(
    id = id,
    uri = "content://media/external/images/media/$id",
    name = name,
    mimeType = if (video) "video/mp4" else "image/jpeg",
    type = if (video) MediaType.VIDEO else MediaType.IMAGE,
    dateTakenMs = taken.toInstant(UTC).toEpochMilli(),
    dateModifiedSec = taken.toEpochSecond(UTC),
    sizeBytes = 1_000_000,
    width = 4000,
    height = 3000,
    durationMs = if (video) 30_000 else 0,
    albumId = albumId,
    albumName = album,
    relativePath = "DCIM/$album/",
    path = "/storage/emulated/0/DCIM/$album/$name",
    isFavorite = favorite
)
