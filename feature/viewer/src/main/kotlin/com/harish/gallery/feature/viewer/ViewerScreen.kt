package com.harish.gallery.feature.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.GalleryIcon
import com.harish.gallery.core.designsystem.component.GalleryIconButton
import com.harish.gallery.core.designsystem.component.LoadingIndicator
import com.harish.gallery.core.designsystem.theme.ScrimDark
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.ui.ActionEffects
import com.harish.gallery.core.ui.AlbumPickerDialog
import com.harish.gallery.core.ui.ConfirmDialog
import com.harish.gallery.core.ui.MediaThumbnail
import com.harish.gallery.core.ui.SystemBarsVisibility
import com.harish.gallery.core.ui.VideoPlayer
import com.harish.gallery.core.ui.ZoomableImage
import com.harish.gallery.core.ui.rememberZoomState
import com.harish.gallery.core.ui.shareMedia
import com.harish.gallery.core.ui.useMediaAs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

/**
 * Full-screen photos and videos.
 *  - Swipe left/right through the list; pinch or double-tap to zoom.
 *  - Tap once to hide/show the buttons (true full screen).
 *  - Slideshow from the ⋮ menu; tap to stop.
 */
@Composable
fun ViewerScreen(
    onBack: () -> Unit,
    onEdit: (MediaItem) -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val list = items
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            list == null -> LoadingIndicator()
            list.isEmpty() -> LaunchedEffect(Unit) { onBack() } // the last item was deleted
            else -> ViewerContent(list, viewModel, onBack, onEdit)
        }
    }
}

@Composable
private fun ViewerContent(
    items: List<MediaItem>,
    viewModel: ViewerViewModel,
    onBack: () -> Unit,
    onEdit: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    val startPage = remember { items.indexOfFirst { it.id == viewModel.startId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = startPage) { items.size }
    val current = items.getOrNull(pagerState.currentPage.coerceIn(0, items.lastIndex)) ?: return
    val snackbar = remember { SnackbarHostState() }
    val slideshowSeconds by viewModel.slideshowSeconds.collectAsStateWithLifecycle()

    var chromeVisible by remember { mutableStateOf(true) }
    var slideshow by remember { mutableStateOf(false) }
    var zoomed by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<ViewerDialog?>(null) }

    ActionEffects(viewModel.actions, snackbar)
    SystemBarsVisibility(visible = chromeVisible && !slideshow)

    // Slideshow: next photo every few seconds, stop at the end
    LaunchedEffect(slideshow, pagerState.currentPage) {
        if (!slideshow) return@LaunchedEffect
        delay(slideshowSeconds * 1000L)
        if (pagerState.currentPage < items.lastIndex) pagerState.animateScrollToPage(pagerState.currentPage + 1)
        else slideshow = false
    }

    val onTap = {
        if (slideshow) { slideshow = false; chromeVisible = true } else chromeVisible = !chromeVisible
    }

    MediaPager(
        items = items,
        pagerState = pagerState,
        scrollEnabled = !zoomed,
        onTap = onTap,
        onZoomChange = { zoomed = it },
        onVideoControls = { visible -> if (!slideshow) chromeVisible = visible }
    )

    // ---- top bar: back · date · info
    AnimatedVisibility(chromeVisible && !slideshow, enter = fadeIn(), exit = fadeOut()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GalleryIconButton(GalleryIcons.ArrowBack, "Back", tint = Color.White, onClick = onBack)
            Column(Modifier.weight(1f)) {
                Text(dateLabel(current), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(timeLabel(current), color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            }
            GalleryIconButton(GalleryIcons.Info, "Details", tint = Color.White) {
                viewModel.loadDetails(current)
                dialog = ViewerDialog.Info
            }
        }
    }

    // ---- bottom bar
    AnimatedVisibility(
        chromeVisible && !slideshow,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewModel.isTrash) {
                    BarButton(GalleryIcons.Restore, "Restore") { viewModel.restore(current) }
                    BarButton(GalleryIcons.DeleteForever, "Delete") { dialog = ViewerDialog.DeleteForever }
                } else {
                    BarButton(GalleryIcons.Share, "Share") { shareMedia(context, listOf(current)) }
                    if (!current.isVideo) BarButton(GalleryIcons.Tune, "Edit") { onEdit(current) }
                    BarButton(if (current.isFavorite) GalleryIcons.Favorite else GalleryIcons.FavoriteBorder, "Favourite") {
                        viewModel.toggleFavorite(current)
                    }
                    BarButton(GalleryIcons.Delete, "Delete") { viewModel.moveToTrash(current) }
                    MoreMenu(
                        onRename = { dialog = ViewerDialog.Rename },
                        onMove = { dialog = ViewerDialog.Move },
                        onCopy = { dialog = ViewerDialog.Copy },
                        onLock = { dialog = ViewerDialog.Lock },
                        onUseAs = { useMediaAs(context, current) },
                        onSlideshow = { chromeVisible = false; slideshow = true }
                    )
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 72.dp), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(snackbar)
    }

    // ---- dialogs
    when (dialog) {
        ViewerDialog.Info -> {
            val details by viewModel.details.collectAsStateWithLifecycle()
            InfoSheet(current, details, onDismiss = { dialog = null })
        }
        ViewerDialog.Rename -> RenameDialog(
            item = current,
            otherNames = remember(items, current) { items.filter { it.albumId == current.albumId && it.id != current.id }.map { it.name } },
            onRename = { viewModel.rename(current, it); dialog = null },
            onDismiss = { dialog = null }
        )
        ViewerDialog.Move, ViewerDialog.Copy -> {
            val albums by viewModel.albums.collectAsStateWithLifecycle()
            val move = dialog == ViewerDialog.Move
            AlbumPickerDialog(
                title = if (move) "Move to album" else "Copy to album",
                albums = albums.filter { it.id != current.albumId },
                onPick = { target ->
                    dialog = null
                    if (move) viewModel.moveTo(current, target) else viewModel.copyTo(current, target)
                },
                onDismiss = { dialog = null }
            )
        }
        ViewerDialog.Lock -> ConfirmDialog(
            title = "Move to Locked folder?",
            message = "It will be hidden from other apps and only open with your fingerprint, face or screen lock.",
            confirmLabel = "Move",
            onConfirm = { viewModel.moveToLockedFolder(current) },
            onDismiss = { dialog = null }
        )
        ViewerDialog.DeleteForever -> ConfirmDialog(
            title = "Delete for good?",
            message = "This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.deleteForever(current) },
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}

private enum class ViewerDialog { Info, Rename, Move, Copy, Lock, DeleteForever }

/** The swipeable pages. Videos play only on the page you're looking at. */
@Composable
private fun MediaPager(
    items: List<MediaItem>,
    pagerState: PagerState,
    scrollEnabled: Boolean,
    onTap: () -> Unit,
    onZoomChange: (Boolean) -> Unit,
    onVideoControls: (Boolean) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        key = { items[it].id },
        userScrollEnabled = scrollEnabled,
        beyondViewportPageCount = 1, // the next photo is already decoded when you swipe
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val item = items[page]
        val isCurrent = pagerState.settledPage == page
        if (item.isVideo) {
            if (isCurrent) {
                // Leave room under the video for the viewer's buttons, so the seek bar stays usable
                VideoPlayer(
                    uri = item.uri,
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(bottom = 72.dp),
                    onControllerVisibilityChange = onVideoControls
                )
            } else {
                VideoPoster(item)
            }
        } else {
            val zoom = rememberZoomState(isCurrent)
            if (isCurrent) LaunchedEffect(zoom.isZoomed) { onZoomChange(zoom.isZoomed) }
            ZoomableImage(uri = item.uri, state = zoom, onTap = onTap)
        }
    }
}

@Composable
private fun VideoPoster(item: MediaItem) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MediaThumbnail(item.uri, Modifier.fillMaxSize(), sizePx = 512, contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(ScrimDark),
            contentAlignment = Alignment.Center
        ) { GalleryIcon(GalleryIcons.PlayArrow, "Play", Modifier.size(36.dp), tint = Color.White) }
    }
}

@Composable
private fun BarButton(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GalleryIcon(icon, label, tint = Color.White)
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MoreMenu(
    onRename: () -> Unit,
    onMove: () -> Unit,
    onCopy: () -> Unit,
    onLock: () -> Unit,
    onUseAs: () -> Unit,
    onSlideshow: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        BarButton(GalleryIcons.MoreVert, "More") { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                Triple("Slideshow", GalleryIcons.Slideshow, onSlideshow),
                Triple("Rename", GalleryIcons.Edit, onRename),
                Triple("Move to album", GalleryIcons.Move, onMove),
                Triple("Copy to album", GalleryIcons.Copy, onCopy),
                Triple("Move to Locked folder", GalleryIcons.Lock, onLock),
                Triple("Use as…", GalleryIcons.Photo, onUseAs)
            ).forEach { (label, icon, action) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    leadingIcon = { GalleryIcon(icon, null) },
                    onClick = { open = false; action() }
                )
            }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun dateLabel(item: MediaItem): String =
    Instant.ofEpochMilli(item.dateTakenMs).atZone(ZoneId.systemDefault()).format(dateFormat)

private fun timeLabel(item: MediaItem): String =
    Instant.ofEpochMilli(item.dateTakenMs).atZone(ZoneId.systemDefault()).format(timeFormat)
