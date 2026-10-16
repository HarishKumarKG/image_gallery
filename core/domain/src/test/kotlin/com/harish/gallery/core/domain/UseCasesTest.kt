package com.harish.gallery.core.domain

import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.domain.usecase.GetAlbumsUseCase
import com.harish.gallery.core.domain.usecase.GetMediaForSourceUseCase
import com.harish.gallery.core.domain.usecase.GetTimelineUseCase
import com.harish.gallery.core.model.AlbumSort
import com.harish.gallery.core.model.GallerySettings
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource
import com.harish.gallery.core.model.ThemeMode
import com.harish.gallery.core.model.TimelineGrouping
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeMedia(items: List<MediaItem>, trash: List<MediaItem> = emptyList()) : MediaRepository {
    override val media = MutableStateFlow(items)
    override val trashed = MutableStateFlow(trash)
    override val isLoading = MutableStateFlow(false)
    override suspend fun refresh() = Unit
}

private class FakeSettings(initial: GallerySettings = GallerySettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: StateFlow<GallerySettings> = state
    override fun setColumns(columns: Int) { state.value = state.value.copy(columns = columns) }
    override fun setGrouping(grouping: TimelineGrouping) { state.value = state.value.copy(grouping = grouping) }
    override fun setAlbumHidden(albumId: String, hidden: Boolean) {
        val ids = state.value.hiddenAlbumIds
        state.value = state.value.copy(hiddenAlbumIds = if (hidden) ids + albumId else ids - albumId)
    }
    override fun setAlbumSort(sort: AlbumSort) { state.value = state.value.copy(albumSort = sort) }
    override fun setThemeMode(mode: ThemeMode) { state.value = state.value.copy(themeMode = mode) }
    override fun setSlideshowSeconds(seconds: Int) { state.value = state.value.copy(slideshowSeconds = seconds) }
}

class UseCasesTest {
    private val items = listOf(
        media(1, album = "Camera"),
        media(2, album = "Screenshots", favorite = true),
        media(3, album = "Camera", video = true)
    )

    @Test fun `hidden albums leave the timeline but stay in albums`() = runBlocking {
        val settings = FakeSettings()
        settings.setAlbumHidden("screenshots", true)
        val repo = FakeMedia(items)
        val timeline = GetTimelineUseCase(repo, settings)().first()
        assertEquals(listOf(1L, 3L), timeline.flatMap { s -> s.items.map { it.id } })
        assertEquals(2, GetAlbumsUseCase(repo, settings)().first().size)
    }

    @Test fun `sources pick the right items`() = runBlocking {
        val trash = listOf(media(9))
        val get = GetMediaForSourceUseCase(FakeMedia(items, trash), FakeSettings())
        assertEquals(listOf(2L), get(MediaSource.Favorites).first().map { it.id })
        assertEquals(listOf(3L), get(MediaSource.Videos).first().map { it.id })
        assertEquals(listOf(1L, 3L), get(MediaSource.InAlbum("camera")).first().map { it.id })
        assertEquals(listOf(9L), get(MediaSource.Trash).first().map { it.id })
        assertEquals(MediaSource.InAlbum("camera"), MediaSource.fromKey("album:camera"))
        assertEquals(MediaSource.Search("goa trip"), MediaSource.fromKey("search:goa trip"))
    }
}
