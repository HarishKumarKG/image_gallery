package com.harish.gallery.core.model

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

/** Everything the user can change in Settings. Defaults = what a new install looks like. */
data class GallerySettings(
    /** Photos per row in the grids (pinch to change). */
    val columns: Int = DEFAULT_COLUMNS,
    val grouping: TimelineGrouping = TimelineGrouping.DAY,
    /** Albums left out of the Photos timeline (they still show in Albums). */
    val hiddenAlbumIds: Set<String> = emptySet(),
    val albumSort: AlbumSort = AlbumSort.RECENT,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Seconds each photo stays on screen in a slideshow. */
    val slideshowSeconds: Int = 3
) {
    companion object {
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 6
        const val DEFAULT_COLUMNS = 4
    }
}
