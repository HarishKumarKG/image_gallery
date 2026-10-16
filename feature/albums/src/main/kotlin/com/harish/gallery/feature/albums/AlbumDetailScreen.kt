package com.harish.gallery.feature.albums

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.EmptyState
import com.harish.gallery.core.designsystem.component.GalleryIconButton
import com.harish.gallery.core.domain.album.trashDaysLeft
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource
import com.harish.gallery.core.ui.ActionEffects
import com.harish.gallery.core.ui.ConfirmDialog
import com.harish.gallery.core.ui.MediaGrid
import com.harish.gallery.core.ui.SelectionAction
import com.harish.gallery.core.ui.SelectionActionsBar
import com.harish.gallery.core.ui.SelectionActionsHost
import com.harish.gallery.core.ui.SelectionTopBar
import com.harish.gallery.core.ui.countLabel
import com.harish.gallery.core.ui.selected

/** The photos inside one album (or Favourites / Videos / Trash). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    onOpenMedia: (MediaSource, MediaItem) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val columns by viewModel.columns.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<List<MediaItem>?>(null) }
    val nowSec = remember { System.currentTimeMillis() / 1000 }

    BackHandler(enabled = selection.isNotEmpty()) { viewModel.clearSelection() }
    ActionEffects(viewModel.actions, snackbar)

    Scaffold(
        topBar = {
            if (selection.isNotEmpty()) {
                SelectionTopBar(selection.size, viewModel::clearSelection) { viewModel.selectAll(items) }
            } else {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = { GalleryIconButton(GalleryIcons.ArrowBack, "Back", onClick = onBack) },
                    actions = {
                        if (viewModel.isTrash && items.isNotEmpty()) {
                            TextButton(onClick = { confirmEmpty = true }) { Text("Empty trash") }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (viewModel.isTrash) {
                val chosen = items.selected(selection)
                SelectionActionsBar(
                    visible = selection.isNotEmpty(),
                    actions = listOf(
                        SelectionAction(GalleryIcons.Restore, "Restore") { viewModel.restore(chosen) },
                        SelectionAction(GalleryIcons.DeleteForever, "Delete") { confirmDelete = chosen }
                    )
                )
            } else {
                SelectionActionsHost(viewModel, items)
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            if (items.isEmpty()) {
                if (viewModel.isTrash) {
                    EmptyState(GalleryIcons.Delete, "Trash is empty", "Deleted items stay here for 30 days before they're removed for good.")
                } else {
                    EmptyState(GalleryIcons.PhotoLibrary, "Nothing here", "This album is empty.")
                }
            } else {
                MediaGrid(
                    sections = sections,
                    columns = columns,
                    selection = selection,
                    onItemClick = { item ->
                        if (selection.isNotEmpty()) viewModel.toggle(item) else onOpenMedia(viewModel.source, item)
                    },
                    onItemLongClick = viewModel::toggle,
                    onColumnsChange = viewModel::setColumns,
                    onSectionToggle = viewModel::toggleSection,
                    showHeaders = !viewModel.isTrash,
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                    badge = if (viewModel.isTrash) { item -> "${trashDaysLeft(item.trashExpiresSec, nowSec)} d" } else null,
                    header = if (viewModel.isTrash) {
                        {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "Items are deleted for good after 30 days. Tap one to look at it, long-press to restore.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(12.dp)
                                )
                            }
                        }
                    } else null
                )
            }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = "Empty trash?",
            message = "${countLabel(items.size)} will be deleted for good. This can't be undone.",
            confirmLabel = "Empty trash",
            onConfirm = { viewModel.deleteForever(items) },
            onDismiss = { confirmEmpty = false }
        )
    }
    confirmDelete?.let { chosen ->
        ConfirmDialog(
            title = "Delete for good?",
            message = "${countLabel(chosen.size)} will be deleted. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.deleteForever(chosen) },
            onDismiss = { confirmDelete = null }
        )
    }
}
