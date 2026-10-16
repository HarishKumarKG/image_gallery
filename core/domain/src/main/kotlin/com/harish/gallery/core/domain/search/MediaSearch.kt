package com.harish.gallery.core.domain.search

import com.harish.gallery.core.domain.timeline.localDate
import com.harish.gallery.core.model.MediaItem
import java.text.Normalizer
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Search that understands simple words. Every word you type must match something:
 *
 *   "beach"            -> file or album name contains "beach"
 *   "october" / "oct"  -> taken in October (any year)
 *   "2024"             -> taken in 2024
 *   "video" / "photo"  -> only videos / only photos
 *   "favourite"        -> only favourites
 *
 * Words combine: "october 2024 video" = videos taken in October 2024.
 * Accents and case don't matter ("café" finds "Cafe").
 */
fun searchMedia(
    items: List<MediaItem>,
    query: String,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.ENGLISH
): List<MediaItem> {
    val words = normalize(query).split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    val tests = words.map { word -> matcherFor(word, zone, locale) }
    return items.filter { item -> tests.all { it(item) } }
}

private fun matcherFor(word: String, zone: ZoneId, locale: Locale): (MediaItem) -> Boolean {
    val inName: (MediaItem) -> Boolean = { word in normalize(it.name) || word in normalize(it.albumName) }
    when (word) {
        "video", "videos" -> return { it.isVideo }
        "photo", "photos", "image", "images", "picture", "pictures" -> return { !it.isVideo }
        "favourite", "favourites", "favorite", "favorites", "fav" -> return { it.isFavorite }
    }
    // A month or a year also matches names, so "jan" still finds "Jan's birthday.jpg"
    monthOf(word, locale)?.let { month -> return { it.localDate(zone).month == month || inName(it) } }
    if (word.length == 4 && word.all(Char::isDigit)) {
        val year = word.toInt()
        if (year in 1900..2200) return { it.localDate(zone).year == year || inName(it) }
    }
    return inName
}

/** "october", "oct", "sept" -> the month; anything shorter than 3 letters is not a month. */
private fun monthOf(word: String, locale: Locale): Month? {
    if (word.length < 3) return null
    return Month.entries.firstOrNull { month ->
        val full = normalize(month.getDisplayName(TextStyle.FULL, locale))
        word == full || full.startsWith(word)
    }
}

/** "Café_Beach-2024.JPG" -> "cafe beach 2024 jpg" */
internal fun normalize(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

/** Quick-pick chips for the search screen: types, then the years that actually have photos. */
fun searchSuggestions(items: List<MediaItem>, zone: ZoneId = ZoneId.systemDefault()): List<String> {
    val years = items.asSequence().map { it.localDate(zone).year }.distinct().sortedDescending().take(6).map { it.toString() }
    val fixed = buildList {
        if (items.any { it.isVideo }) add("Videos")
        if (items.any { it.isFavorite }) add("Favourites")
        add("Photos")
    }
    return fixed + years
}
