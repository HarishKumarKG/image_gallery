package com.harish.gallery.core.model

/**
 * "Which list of photos?" — passed to the viewer so swiping left/right stays inside the list
 * you came from. [key] is a short text form used in navigation routes.
 *
 * Examples: All -> "all", Album("123") -> "album:123", Search("beach") -> "search:beach"
 */
sealed interface MediaSource {
    val key: String

    data object All : MediaSource { override val key = "all" }
    data object Favorites : MediaSource { override val key = "favorites" }
    data object Videos : MediaSource { override val key = "videos" }
    data object Trash : MediaSource { override val key = "trash" }
    data class InAlbum(val albumId: String) : MediaSource { override val key = "album:$albumId" }
    data class Search(val query: String) : MediaSource { override val key = "search:$query" }

    companion object {
        fun fromKey(key: String): MediaSource = when {
            key == Favorites.key -> Favorites
            key == Videos.key -> Videos
            key == Trash.key -> Trash
            key.startsWith("album:") -> InAlbum(key.removePrefix("album:"))
            key.startsWith("search:") -> Search(key.removePrefix("search:"))
            else -> All
        }
    }
}
