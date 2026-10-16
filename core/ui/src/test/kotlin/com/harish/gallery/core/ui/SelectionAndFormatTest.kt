package com.harish.gallery.core.ui

import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaType
import com.harish.gallery.core.model.TimelineSection
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionAndFormatTest {
    private fun item(id: Long) = MediaItem(
        id = id, uri = "content://media/external/images/media/$id", name = "IMG_$id.jpg", mimeType = "image/jpeg",
        type = MediaType.IMAGE, dateTakenMs = 0, dateModifiedSec = 0, sizeBytes = 0, width = 0, height = 0,
        albumId = "camera", albumName = "Camera", relativePath = "DCIM/Camera/", path = ""
    )

    @Test fun `toggle adds then removes`() {
        val once = emptySet<Long>().toggle(3)
        assertEquals(setOf(3L), once)
        assertEquals(emptySet<Long>(), once.toggle(3))
    }

    @Test fun `tapping a day selects it all, tapping again clears it`() {
        val day = TimelineSection("d", "Today", listOf(item(1), item(2)))
        val partly = setOf(1L, 9L)
        val all = partly.toggleSection(day)
        assertEquals(setOf(1L, 2L, 9L), all)
        assertEquals(setOf(9L), all.toggleSection(day))
    }

    @Test fun `selected keeps screen order`() {
        val list = listOf(item(5), item(6), item(7))
        assertEquals(listOf(5L, 7L), list.selected(setOf(7L, 5L)).map { it.id })
    }

    @Test fun `durations and sizes read nicely`() {
        assertEquals("1:15", formatDuration(75_000))
        assertEquals("1:02:05", formatDuration(3_725_000))
        assertEquals("3 items", countLabel(3))
        assertEquals("1 photo", countLabel(1, "photo", "photos"))
    }
}
