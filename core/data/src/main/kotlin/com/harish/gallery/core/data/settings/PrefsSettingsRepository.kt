package com.harish.gallery.core.data.settings

import android.content.Context
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.model.AlbumSort
import com.harish.gallery.core.model.GallerySettings
import com.harish.gallery.core.model.ThemeMode
import com.harish.gallery.core.model.TimelineGrouping
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Settings saved in SharedPreferences ("gallery_settings"), exposed as a StateFlow. */
@Singleton
class PrefsSettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val prefs = context.getSharedPreferences("gallery_settings", Context.MODE_PRIVATE)
    private val defaults = GallerySettings()

    private val _settings = MutableStateFlow(
        GallerySettings(
            columns = prefs.getInt(KEY_COLUMNS, defaults.columns)
                .coerceIn(GallerySettings.MIN_COLUMNS, GallerySettings.MAX_COLUMNS),
            grouping = enumOr(prefs.getString(KEY_GROUPING, null), defaults.grouping),
            hiddenAlbumIds = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet(),
            albumSort = enumOr(prefs.getString(KEY_ALBUM_SORT, null), defaults.albumSort),
            themeMode = enumOr(prefs.getString(KEY_THEME, null), defaults.themeMode),
            slideshowSeconds = prefs.getInt(KEY_SLIDESHOW, defaults.slideshowSeconds)
        )
    )
    override val settings: StateFlow<GallerySettings> = _settings.asStateFlow()

    override fun setColumns(columns: Int) {
        val value = columns.coerceIn(GallerySettings.MIN_COLUMNS, GallerySettings.MAX_COLUMNS)
        prefs.edit().putInt(KEY_COLUMNS, value).apply()
        _settings.update { it.copy(columns = value) }
    }

    override fun setGrouping(grouping: TimelineGrouping) {
        prefs.edit().putString(KEY_GROUPING, grouping.name).apply()
        _settings.update { it.copy(grouping = grouping) }
    }

    override fun setAlbumHidden(albumId: String, hidden: Boolean) {
        val ids = _settings.value.hiddenAlbumIds.let { if (hidden) it + albumId else it - albumId }
        prefs.edit().putStringSet(KEY_HIDDEN, ids).apply()
        _settings.update { it.copy(hiddenAlbumIds = ids) }
    }

    override fun setAlbumSort(sort: AlbumSort) {
        prefs.edit().putString(KEY_ALBUM_SORT, sort.name).apply()
        _settings.update { it.copy(albumSort = sort) }
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.update { it.copy(themeMode = mode) }
    }

    override fun setSlideshowSeconds(seconds: Int) {
        val value = seconds.coerceIn(1, 30)
        prefs.edit().putInt(KEY_SLIDESHOW, value).apply()
        _settings.update { it.copy(slideshowSeconds = value) }
    }

    private companion object {
        const val KEY_COLUMNS = "columns"
        const val KEY_GROUPING = "grouping"
        const val KEY_HIDDEN = "hidden_albums"
        const val KEY_ALBUM_SORT = "album_sort"
        const val KEY_THEME = "theme"
        const val KEY_SLIDESHOW = "slideshow_seconds"

        inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: fallback
    }
}
