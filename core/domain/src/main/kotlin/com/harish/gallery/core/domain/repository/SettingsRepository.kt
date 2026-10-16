package com.harish.gallery.core.domain.repository

import com.harish.gallery.core.model.AlbumSort
import com.harish.gallery.core.model.GallerySettings
import com.harish.gallery.core.model.ThemeMode
import com.harish.gallery.core.model.TimelineGrouping
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val settings: StateFlow<GallerySettings>

    fun setColumns(columns: Int)
    fun setGrouping(grouping: TimelineGrouping)
    fun setAlbumHidden(albumId: String, hidden: Boolean)
    fun setAlbumSort(sort: AlbumSort)
    fun setThemeMode(mode: ThemeMode)
    fun setSlideshowSeconds(seconds: Int)
}
