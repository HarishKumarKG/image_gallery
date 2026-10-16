package com.harish.gallery.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.model.MediaItem

/**
 * The bottom bar shown while selecting, plus its pickers and confirmations:
 * Share · Favourite · Delete · More ▸ Move to album, Copy to album, Move to Locked folder.
 *
 * [items] = everything on screen (the selected ones are picked from it).
 */
@Composable
fun SelectionActionsHost(viewModel: MediaSelectionViewModel, items: List<MediaItem>) {
    val context = LocalContext.current
    val selectionIds by viewModel.selection.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val selected = remember(items, selectionIds) { items.selected(selectionIds) }
    var picker by remember { mutableStateOf<PickerMode?>(null) }
    var confirmLock by remember { mutableStateOf(false) }

    SelectionActionsBar(
        visible = selectionIds.isNotEmpty(),
        actions = listOf(
            SelectionAction(GalleryIcons.Share, "Share") { shareMedia(context, selected) },
            SelectionAction(
                if (selected.isNotEmpty() && selected.all { it.isFavorite }) GalleryIcons.Favorite else GalleryIcons.FavoriteBorder,
                "Favourite"
            ) { viewModel.toggleFavorite(selected) },
            SelectionAction(GalleryIcons.Delete, "Delete") { viewModel.moveToTrash(selected) },
            SelectionAction(GalleryIcons.Move, "Move to album") { picker = PickerMode.MOVE },
            SelectionAction(GalleryIcons.Copy, "Copy to album") { picker = PickerMode.COPY },
            SelectionAction(GalleryIcons.Lock, "Move to Locked folder") { confirmLock = true }
        )
    )

    picker?.let { mode ->
        AlbumPickerDialog(
            title = if (mode == PickerMode.MOVE) "Move to album" else "Copy to album",
            albums = albums,
            onPick = { target ->
                picker = null
                if (mode == PickerMode.MOVE) viewModel.moveTo(selected, target) else viewModel.copyTo(selected, target)
            },
            onDismiss = { picker = null }
        )
    }

    if (confirmLock) {
        ConfirmDialog(
            title = "Move to Locked folder?",
            message = "${countLabel(selected.size)} will be hidden from other apps and only open with your " +
                "fingerprint, face or screen lock. Uninstalling this app deletes the Locked folder.",
            confirmLabel = "Move",
            onConfirm = { viewModel.moveToLockedFolder(selected) },
            onDismiss = { confirmLock = false }
        )
    }
}

private enum class PickerMode { MOVE, COPY }
