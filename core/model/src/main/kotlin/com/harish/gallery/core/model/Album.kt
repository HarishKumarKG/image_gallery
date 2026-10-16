package com.harish.gallery.core.model

/** A folder that contains photos or videos, e.g. "Camera", "Screenshots", "WhatsApp Images". */
data class Album(
    val id: String,
    val name: String,
    val relativePath: String,
    /** Newest item, shown as the album's picture. */
    val cover: MediaItem,
    val count: Int,
    val latestMs: Long,
    /** Hidden albums still show in Albums, just not in the Photos timeline. */
    val isHidden: Boolean = false
)

enum class AlbumSort(val label: String) {
    RECENT("Most recent"),
    NAME("Name"),
    SIZE("Most items")
}
