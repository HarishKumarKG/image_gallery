package com.harish.gallery.core.domain

import com.harish.gallery.core.domain.timeline.groupTimeline
import com.harish.gallery.core.model.TimelineGrouping
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineGrouperTest {
    private val today = LocalDate.of(2026, 10, 14)
    private fun at(y: Int, m: Int, d: Int, h: Int = 12) = LocalDateTime.of(y, m, d, h, 0)

    private val items = listOf(
        media(1, at(2026, 10, 14, 18)),
        media(2, at(2026, 10, 14, 9)),
        media(3, at(2026, 10, 13)),
        media(4, at(2026, 9, 2)),
        media(5, at(2025, 12, 25))
    )

    @Test fun `days get friendly titles`() {
        val sections = groupTimeline(items, TimelineGrouping.DAY, today, UTC, Locale.ENGLISH)
        assertEquals(listOf("Today", "Yesterday", "Wed, 2 Sept", "25 Dec 2025").map { it.take(3) }, sections.map { it.title.take(3) })
        assertEquals(listOf(1L, 2L), sections[0].items.map { it.id })
        assertEquals("25 Dec 2025", sections[3].title)
    }

    @Test fun `months group a whole month together`() {
        val sections = groupTimeline(items, TimelineGrouping.MONTH, today, UTC, Locale.ENGLISH)
        assertEquals(listOf("This month", "September", "December 2025"), sections.map { it.title })
        assertEquals(3, sections[0].items.size)
    }

    @Test fun `nothing in, nothing out`() {
        assertEquals(emptyList<Any>(), groupTimeline(emptyList(), TimelineGrouping.DAY, today, UTC))
    }
}
