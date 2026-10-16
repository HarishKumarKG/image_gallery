package com.harish.gallery.feature.locked

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.EmptyState
import com.harish.gallery.core.designsystem.component.GalleryIcon
import com.harish.gallery.core.designsystem.component.GalleryIconButton
import com.harish.gallery.core.designsystem.theme.ScrimDark
import com.harish.gallery.core.designsystem.theme.SelectionBlue
import com.harish.gallery.core.model.LockedItem
import com.harish.gallery.core.ui.ActionEffects
import com.harish.gallery.core.ui.ConfirmDialog
import com.harish.gallery.core.ui.SelectionAction
import com.harish.gallery.core.ui.SelectionActionsBar
import com.harish.gallery.core.ui.SelectionTopBar
import com.harish.gallery.core.ui.VideoPlayer
import com.harish.gallery.core.ui.ZoomableImage
import com.harish.gallery.core.ui.countLabel
import com.harish.gallery.core.ui.findActivity
import com.harish.gallery.core.ui.rememberZoomState
import java.io.File

/**
 * Locked folder: asks for your fingerprint / face / screen lock first, blocks screenshots
 * and the recent-apps preview while open, and locks again as soon as you leave the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedScreen(onBack: () -> Unit, viewModel: LockedViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var openIndex by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf<List<LockedItem>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    ActionEffects(viewModel.actions, snackbar)
    SecureWindow()

    val unlock = {
        error = null
        if (canUnlock(context)) askToUnlock(context, viewModel::onUnlocked) { error = it }
        else error = "Set up a screen lock (PIN, pattern or fingerprint) to use the Locked folder."
    }
    val currentUnlock by rememberUpdatedState(unlock)

    // Ask when the screen opens (and every time you come back to the app);
    // lock again as soon as the app goes to the background
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!viewModel.unlocked.value) currentUnlock()
                Lifecycle.Event.ON_STOP -> viewModel.lock()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = openIndex != null || selection.isNotEmpty()) {
        if (openIndex != null) openIndex = null else viewModel.clearSelection()
    }

    if (!unlocked) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            GalleryIconButton(GalleryIcons.ArrowBack, "Back", Modifier.statusBarsPadding().padding(4.dp), onClick = onBack)
            EmptyState(
                icon = GalleryIcons.Lock,
                title = "Locked folder",
                message = error ?: "Photos here are hidden from other apps and need your fingerprint, face or screen lock.",
                actionLabel = if (canUnlock(context)) "Unlock" else "Open security settings",
                onAction = {
                    if (canUnlock(context)) unlock()
                    else context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }
            )
        }
        return
    }

    val opened = openIndex
    if (opened != null && items.isNotEmpty()) {
        LockedViewer(items, opened.coerceIn(0, items.lastIndex), onClose = { openIndex = null })
        return
    }

    Scaffold(
        topBar = {
            if (selection.isNotEmpty()) {
                SelectionTopBar(selection.size, viewModel::clearSelection, onSelectAll = null)
            } else {
                TopAppBar(
                    title = { Text("Locked folder") },
                    navigationIcon = { GalleryIconButton(GalleryIcons.ArrowBack, "Back", onClick = onBack) }
                )
            }
        },
        bottomBar = {
            val chosen = items.filter { it.id in selection }
            SelectionActionsBar(
                visible = selection.isNotEmpty(),
                actions = listOf(
                    SelectionAction(GalleryIcons.LockOpen, "Move out") { viewModel.moveOut(chosen) },
                    SelectionAction(GalleryIcons.DeleteForever, "Delete") { confirmDelete = chosen }
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(
                GalleryIcons.Lock, "Nothing here yet",
                "Select photos anywhere in the app and choose “Move to Locked folder”.",
                Modifier.padding(padding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    LockedCell(
                        item = item,
                        selecting = selection.isNotEmpty(),
                        selected = item.id in selection,
                        onClick = { if (selection.isNotEmpty()) viewModel.toggle(item) else openIndex = items.indexOf(item) },
                        onLongClick = { viewModel.toggle(item) }
                    )
                }
            }
        }
    }

    confirmDelete?.let { chosen ->
        ConfirmDialog(
            title = "Delete for good?",
            message = "${countLabel(chosen.size)} will be deleted. Locked items don't go to the Trash, so this can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.deleteForever(chosen) },
            onDismiss = { confirmDelete = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LockedCell(item: LockedItem, selecting: Boolean, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(Modifier.aspectRatio(1f).combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        LockedThumbnail(item, Modifier.fillMaxSize())
        if (item.isVideo) {
            GalleryIcon(GalleryIcons.PlayArrow, "Video", Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp), tint = Color.White)
        }
        if (selecting) {
            Box(
                Modifier.align(Alignment.TopStart).padding(6.dp).size(24.dp).clip(CircleShape)
                    .background(if (selected) Color.White else ScrimDark.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                GalleryIcon(
                    if (selected) GalleryIcons.CheckCircle else GalleryIcons.CircleOutline, null,
                    tint = if (selected) SelectionBlue else Color.White
                )
            }
        }
    }
}

/** Full-screen viewing of locked items (stays inside this screen, so it stays locked). */
@Composable
private fun LockedViewer(items: List<LockedItem>, start: Int, onClose: () -> Unit) {
    val pager = rememberPagerState(initialPage = start) { items.size }
    var zoomed by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(pager, key = { items[it].id }, userScrollEnabled = !zoomed, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            val uri = Uri.fromFile(File(item.filePath)).toString()
            val isCurrent = pager.settledPage == page
            if (item.isVideo) {
                if (isCurrent) VideoPlayer(uri, Modifier.fillMaxSize()) else LockedThumbnail(item, Modifier.fillMaxSize(), 512)
            } else {
                val zoom = rememberZoomState(isCurrent)
                if (isCurrent) LaunchedEffect(zoom.isZoomed) { zoomed = zoom.isZoomed }
                ZoomableImage(uri = uri, state = zoom, onTap = {}, thumbnailUri = null)
            }
        }
        GalleryIconButton(GalleryIcons.Close, "Close", Modifier.statusBarsPadding().padding(4.dp), tint = Color.White, onClick = onClose)
    }
}

/** No screenshots, screen recording or recent-apps preview while the Locked folder is open. */
@Composable
private fun SecureWindow() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
