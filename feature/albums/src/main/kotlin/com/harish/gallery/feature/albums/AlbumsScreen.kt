package com.harish.gallery.feature.albums

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.EmptyState
import com.harish.gallery.core.designsystem.component.GalleryIcon
import com.harish.gallery.core.designsystem.component.GalleryIconButton
import com.harish.gallery.core.designsystem.theme.ScrimDark
import com.harish.gallery.core.model.Album
import com.harish.gallery.core.model.AlbumSort
import com.harish.gallery.core.model.MediaSource
import com.harish.gallery.core.ui.MediaThumbnail
import com.harish.gallery.core.ui.bottomBarPadding
import com.harish.gallery.core.ui.countLabel

/**
 * Albums tab: Favourites / Videos / Locked folder / Trash on top, then one card per folder.
 * Long-press an album to hide it from the Photos timeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onOpenSource: (MediaSource) -> Unit,
    onOpenLocked: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    var sortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.bottomBarPadding(contentPadding),
        topBar = {
            TopAppBar(
                title = { Text("Albums", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        GalleryIconButton(GalleryIcons.Sort, "Sort albums") { sortMenu = true }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            AlbumSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    trailingIcon = { if (option == sort) GalleryIcon(GalleryIcons.Check, null) },
                                    onClick = { viewModel.setSort(option); sortMenu = false }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = padding.calculateTopPadding(), bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CollectionsRow(counts, onOpenSource, onOpenLocked)
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("On this phone", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            }
            if (albums.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(GalleryIcons.PhotoAlbum, "No albums", "Folders with photos or videos appear here.", Modifier.height(320.dp))
                }
            }
            items(albums, key = { it.id }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onOpenSource(MediaSource.InAlbum(album.id)) },
                    onToggleHidden = { viewModel.setHidden(album, !album.isHidden) }
                )
            }
        }
    }
}

@Composable
private fun CollectionsRow(counts: CollectionCounts, onOpenSource: (MediaSource) -> Unit, onOpenLocked: () -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        item { CollectionChip("Favourites", counts.favorites, GalleryIcons.Favorite, counts.favoriteCover) { onOpenSource(MediaSource.Favorites) } }
        item { CollectionChip("Videos", counts.videos, GalleryIcons.Videocam, counts.videoCover) { onOpenSource(MediaSource.Videos) } }
        item { CollectionChip("Locked folder", counts.locked, GalleryIcons.Lock, null, showCount = false) { onOpenLocked() } }
        item { CollectionChip("Trash", counts.trash, GalleryIcons.Delete, null) { onOpenSource(MediaSource.Trash) } }
    }
}

@Composable
private fun CollectionChip(label: String, count: Int, icon: Int, coverUri: String?, showCount: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (coverUri != null) {
                MediaThumbnail(coverUri, Modifier.fillMaxSize(), sizePx = 256)
                Box(Modifier.fillMaxSize().background(ScrimDark.copy(alpha = 0.35f)))
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (coverUri != null) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                GalleryIcon(icon, null, tint = if (coverUri != null) Color.Black else MaterialTheme.colorScheme.onSecondary)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (showCount) Text(countLabel(count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit, onToggleHidden: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(
        Modifier.combinedClickable(onClick = onClick, onLongClick = { menu = true })
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
        ) {
            MediaThumbnail(album.cover.uri, Modifier.fillMaxSize(), sizePx = 384)
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.6f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.35f)))
            )
            if (album.isHidden) {
                Row(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ScrimDark)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GalleryIcon(GalleryIcons.VisibilityOff, "Hidden from Photos", Modifier.size(14.dp), tint = Color.White)
                }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (album.isHidden) "Show in Photos" else "Hide from Photos") },
                    leadingIcon = { GalleryIcon(if (album.isHidden) GalleryIcons.Visibility else GalleryIcons.VisibilityOff, null) },
                    onClick = { menu = false; onToggleHidden() }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(album.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
        Text(countLabel(album.count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
    }
}
