package com.harish.gallery.core.ui

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** 75_000 -> "1:15", 3_725_000 -> "1:02:05" */
fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%d:%02d", m, s)
}

/** 1_536 -> "1.5 KB", 4_200_000 -> "4.0 MB" */
fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.getDefault(), "%.1f GB", bytes / 1e9)
    bytes >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1e6)
    bytes >= 1_000 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1e3)
    else -> "$bytes B"
}

/** "14 Oct 2026, 9:30 am" in the phone's own style. */
fun formatDateTime(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

/** 4000 x 3000 -> "12 MP · 4000 × 3000" */
fun formatResolution(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return "Unknown"
    val mp = width.toLong() * height / 1_000_000.0
    val mpLabel = if (mp >= 1) String.format(Locale.getDefault(), "%.0f MP · ", mp) else ""
    return "$mpLabel$width × $height"
}

/** "1 photo", "3 items" etc. */
fun countLabel(count: Int, one: String = "item", many: String = "items"): String =
    if (count == 1) "1 $one" else "$count $many"
