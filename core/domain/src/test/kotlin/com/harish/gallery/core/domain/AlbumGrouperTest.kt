package com.harish.gallery.core.domain

import com.harish.gallery.core.domain.album.groupAlbums
import com.harish.gallery.core.domain.album.trashDaysLeft
import com.harish.gallery.core.model.AlbumSort
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumGrouperTest {
    private val items = listOf(
        media(1, LocalDateTime.of(2026, 10, 14, 10, 0), album = "Camera"),
        media(2, LocalDateTime.of(2026, 10, 10, 10, 0), album = "Screenshots"),
        media(3, LocalDateTime.of(2026, 10, 9, 10, 0), album = "Camera"),
        media(4, LocalDateTime.of(2026, 1, 1, 10, 0), album = "Camera")
    )

    @Test fun `one album per folder with the newest as cover`() {
        val albums = groupAlbums(items, AlbumSort.RECENT)
        assertEquals(listOf("Camera", "Screenshots"), albums.map { it.name })
        assertEquals(3, albums[0].count)
        assertEquals(1L, albums[0].cover.id)
    }

    @Test fun `sorting by name and size`() {
        assertEquals(listOf("Camera", "Screenshots"), groupAlbums(items, AlbumSort.NAME).map { it.name })
        assertEquals("Camera", groupAlbums(items, AlbumSort.SIZE).first().name)
    }

    @Test fun `hidden albums are marked`() {
        val albums = groupAlbums(items, AlbumSort.RECENT, hiddenIds = setOf("screenshots"))
        assertTrue(albums.single { it.name == "Screenshots" }.isHidden)
    }

    @Test fun `trash days left rounds up`() {
        assertEquals(2, trashDaysLeft(expiresSec = 100_000 + 86_400 + 10, nowSec = 100_000))
        assertEquals(0, trashDaysLeft(expiresSec = 50, nowSec = 100))
    }
}
