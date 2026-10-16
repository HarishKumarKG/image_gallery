package com.harish.gallery.core.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.GalleryIcon
import com.harish.gallery.core.designsystem.theme.ScrimDark
import com.harish.gallery.core.designsystem.theme.SelectionBlue
import com.harish.gallery.core.model.GallerySettings
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.TimelineSection

/**
 * The photo grid used by Photos, albums, search and trash.
 *
 * - Date headings span the full width; tap a heading's circle to select that whole day.
 * - Pinch in/out with two fingers to change how many photos fit in a row (2..6).
 * - Long-press starts selecting; while selecting, a tap adds/removes a photo.
 */
@Composable
fun MediaGrid(
    sections: List<TimelineSection>,
    columns: Int,
    selection: Set<Long>,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    onColumnsChange: ((Int) -> Unit)? = null,
    onSectionToggle: ((TimelineSection) -> Unit)? = null,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    showHeaders: Boolean = true,
    /** Small text over a thumbnail, e.g. "12 days" in the Trash. */
    badge: ((MediaItem) -> String?)? = null,
    header: (LazyGridScope.() -> Unit)? = null
) {
    val selecting = selection.isNotEmpty()
    val currentColumns by rememberUpdatedState(columns)
    val columnsChange by rememberUpdatedState(onColumnsChange)
    // Thumbnail size in pixels: a cell's width, rounded up so the cache is shared between sizes
    val density = LocalDensity.current
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.takeIf { it > 0 } ?: with(density) { 400.dp.roundToPx() }
    val thumbPx = remember(columns, screenWidthPx) { ((screenWidthPx / columns) / 64 + 1) * 64 }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Two-finger pinch changes the column count. One finger is left alone so the grid scrolls.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var zoom = 1f
                    var changed = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            zoom *= event.calculateZoom()
                            event.changes.forEach { it.consume() }
                            val change = columnsChange
                            if (!changed && change != null) {
                                val next = when {
                                    zoom > 1.25f -> currentColumns - 1   // spread fingers: bigger photos
                                    zoom < 0.8f -> currentColumns + 1    // pinch: more photos per row
                                    else -> currentColumns
                                }.coerceIn(GallerySettings.MIN_COLUMNS, GallerySettings.MAX_COLUMNS)
                                if (next != currentColumns) {
                                    change(next)
                                    changed = true
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        header?.invoke(this)
        for (section in sections) {
            if (showHeaders) {
                item(key = "h:" + section.key, span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                    SectionHeader(
                        title = section.title,
                        selecting = selecting,
                        allSelected = selecting && section.items.all { it.id in selection },
                        onToggle = onSectionToggle?.let { toggle -> { toggle(section) } }
                    )
                }
            }
            items(section.items, key = { it.id }, contentType = { "media" }) { item ->
                MediaCell(
                    item = item,
                    thumbPx = thumbPx,
                    selecting = selecting,
                    selected = item.id in selection,
                    badge = badge?.invoke(item),
                    onClick = { onItemClick(item) },
                    onLongClick = { onItemLongClick(item) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, selecting: Boolean, allSelected: Boolean, onToggle: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (selecting && onToggle != null) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .combinedClickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                GalleryIcon(
                    if (allSelected) GalleryIcons.CheckCircle else GalleryIcons.CircleOutline,
                    contentDescription = if (allSelected) "Deselect day" else "Select day",
                    tint = if (allSelected) SelectionBlue else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaCell(
    item: MediaItem,
    thumbPx: Int,
    selecting: Boolean,
    selected: Boolean,
    badge: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Selected photos shrink a little with rounded corners, like Google Photos
    val scale by animateFloatAsState(if (selected) 0.86f else 1f, label = "select")
    val corner by animateDpAsState(if (selected) 12.dp else 0.dp, label = "corner")
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        MediaThumbnail(
            uri = item.uri,
            sizePx = thumbPx,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(corner))
        )
        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ScrimDark)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatDuration(item.durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(2.dp))
                GalleryIcon(GalleryIcons.PlayArrow, null, Modifier.size(12.dp), tint = Color.White)
            }
        }
        if (item.isFavorite && !selecting) {
            GalleryIcon(
                GalleryIcons.Favorite, "Favourite",
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .size(16.dp),
                tint = Color.White
            )
        }
        if (badge != null) {
            Text(
                badge,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(ScrimDark)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
        if (selecting) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else ScrimDark.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                GalleryIcon(
                    if (selected) GalleryIcons.CheckCircle else GalleryIcons.CircleOutline,
                    contentDescription = if (selected) "Selected" else "Not selected",
                    tint = if (selected) SelectionBlue else Color.White
                )
            }
        }
    }
}
