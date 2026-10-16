package com.harish.gallery.core.domain

import com.harish.gallery.core.domain.search.searchMedia
import com.harish.gallery.core.domain.search.searchSuggestions
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSearchTest {
    private val items = listOf(
        media(1, LocalDateTime.of(2024, 10, 3, 10, 0), name = "Café_Beach.jpg", album = "Goa"),
        media(2, LocalDateTime.of(2024, 10, 5, 10, 0), name = "VID_01.mp4", video = true),
        media(3, LocalDateTime.of(2026, 3, 1, 10, 0), name = "Jan birthday.jpg", favorite = true),
        media(4, LocalDateTime.of(2026, 1, 9, 10, 0), name = "IMG_4.jpg", album = "Screenshots")
    )
    private fun ids(q: String) = searchMedia(items, q, UTC).map { it.id }

    @Test fun `name and album words`() {
        assertEquals(listOf(1L), ids("beach"))
        assertEquals(listOf(1L), ids("cafe"))   // accents don't matter
        assertEquals(listOf(1L), ids("GOA"))    // case doesn't matter
        assertEquals(listOf(4L), ids("screenshot"))
    }

    @Test fun `months years and types combine`() {
        assertEquals(listOf(1L, 2L), ids("october 2024"))
        assertEquals(listOf(1L, 2L), ids("oct"))
        assertEquals(listOf(2L), ids("oct video"))
        assertEquals(listOf(3L), ids("favourites"))
    }

    @Test fun `a month word also finds names`() {
        // "jan": January photos AND "Jan birthday"
        assertEquals(listOf(3L, 4L), ids("jan"))
    }

    @Test fun `blank query finds nothing`() {
        assertEquals(emptyList<Long>(), ids("   "))
    }

    @Test fun `suggestions list types then years`() {
        assertEquals(listOf("Videos", "Favourites", "Photos", "2026", "2024"), searchSuggestions(items, UTC))
    }
}
