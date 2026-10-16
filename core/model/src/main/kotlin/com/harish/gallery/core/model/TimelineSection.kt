package com.harish.gallery.core.model

/** One date heading in the timeline ("Today", "Yesterday", "October 2026") and its items. */
data class TimelineSection(
    val key: String,
    val title: String,
    val items: List<MediaItem>
)

enum class TimelineGrouping(val label: String) {
    DAY("Day"),
    MONTH("Month")
}
