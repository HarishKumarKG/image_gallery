package com.harish.gallery.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.GalleryIcon
import com.harish.gallery.core.designsystem.component.GalleryIconButton
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.TimelineSection

/** Adds or removes one id. */
fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id

/** Tapping a day's circle: select the whole day, or clear it if it was all selected. */
fun Set<Long>.toggleSection(section: TimelineSection): Set<Long> {
    val ids = section.items.map { it.id }
    return if (ids.all { it in this }) this - ids.toSet() else this + ids
}

/** The selected items, in the order they appear in [all]. */
fun List<MediaItem>.selected(ids: Set<Long>): List<MediaItem> = filter { it.id in ids }

/** Top bar while selecting: ✕  "3 selected"  [select all] */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(count: Int, onClear: () -> Unit, onSelectAll: (() -> Unit)?) {
    TopAppBar(
        title = { Text("$count selected") },
        navigationIcon = { GalleryIconButton(GalleryIcons.Close, "Cancel selection", onClick = onClear) },
        actions = {
            if (onSelectAll != null) GalleryIconButton(GalleryIcons.SelectAll, "Select all", onClick = onSelectAll)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    )
}

/** One action in the bottom selection bar. */
data class SelectionAction(val icon: Int, val label: String, val onClick: () -> Unit)

/**
 * Bottom bar while selecting: the first actions as big buttons, the rest under "More".
 * Example: Share · Favourite · Delete · More (Move to album, Copy to album, Move to Locked folder)
 */
@Composable
fun SelectionActionsBar(visible: Boolean, actions: List<SelectionAction>, mainCount: Int = 3) {
    AnimatedVisibility(visible, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                actions.take(mainCount).forEach { ActionButton(it.icon, it.label, it.onClick) }
                val more = actions.drop(mainCount)
                if (more.isNotEmpty()) {
                    var open by remember { mutableStateOf(false) }
                    Box {
                        ActionButton(GalleryIcons.MoreVert, "More") { open = true }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                            more.forEach { action ->
                                DropdownMenuItem(
                                    text = { Text(action.label) },
                                    leadingIcon = { GalleryIcon(action.icon, null) },
                                    onClick = { open = false; action.onClick() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GalleryIcon(icon, null, Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
