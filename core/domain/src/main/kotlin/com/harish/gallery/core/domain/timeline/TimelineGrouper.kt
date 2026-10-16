package com.harish.gallery.core.domain.timeline

import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.TimelineGrouping
import com.harish.gallery.core.model.TimelineSection
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Splits photos into date sections, newest first.
 *
 * DAY:   "Today", "Yesterday", "Mon, 14 Oct" (this year), "14 Oct 2025" (older)
 * MONTH: "This month", "September", "December 2025"
 *
 * [items] must already be sorted newest first (MediaRepository does that).
 */
fun groupTimeline(
    items: List<MediaItem>,
    grouping: TimelineGrouping,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): List<TimelineSection> {
    if (items.isEmpty()) return emptyList()
    val dayThisYear = DateTimeFormatter.ofPattern("EEE, d MMM", locale)
    val dayOlder = DateTimeFormatter.ofPattern("d MMM yyyy", locale)
    val monthThisYear = DateTimeFormatter.ofPattern("MMMM", locale)
    val monthOlder = DateTimeFormatter.ofPattern("MMMM yyyy", locale)

    val sections = ArrayList<TimelineSection>()
    var currentKey: String? = null
    var current = ArrayList<MediaItem>()
    var currentTitle = ""

    fun flush() {
        val key = currentKey ?: return
        sections += TimelineSection(key, currentTitle, current)
    }

    for (item in items) {
        val date = item.localDate(zone)
        val key: String
        val title: String
        when (grouping) {
            TimelineGrouping.DAY -> {
                key = date.toString() // 2026-10-14
                title = when {
                    date == today -> "Today"
                    date == today.minusDays(1) -> "Yesterday"
                    date.year == today.year -> dayThisYear.format(date)
                    else -> dayOlder.format(date)
                }
            }
            TimelineGrouping.MONTH -> {
                val month = YearMonth.from(date)
                key = month.toString() // 2026-10
                title = when {
                    month == YearMonth.from(today) -> "This month"
                    month.year == today.year -> monthThisYear.format(month)
                    else -> monthOlder.format(month)
                }
            }
        }
        if (key != currentKey) {
            flush()
            currentKey = key
            currentTitle = title
            current = ArrayList()
        }
        current += item
    }
    flush()
    return sections
}

fun MediaItem.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(dateTakenMs).atZone(zone).toLocalDate()
