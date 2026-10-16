package com.harish.gallery.feature.photos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.EmptyState
import com.harish.gallery.core.designsystem.component.GalleryIconButton
import com.harish.gallery.core.designsystem.component.LoadingIndicator
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource
import com.harish.gallery.core.ui.ActionEffects
import com.harish.gallery.core.ui.bottomBarPadding
import com.harish.gallery.core.ui.MediaGrid
import com.harish.gallery.core.ui.SelectionActionsHost
import com.harish.gallery.core.ui.SelectionTopBar

/**
 * The first tab: every photo and video, newest first, under date headings.
 * Pinch to change the grid size; long-press to select.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    onOpenMedia: (MediaSource, MediaItem) -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val columns by viewModel.columns.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val allItems = remember(sections) { sections.flatMap { it.items } }
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    BackHandler(enabled = selection.isNotEmpty()) { viewModel.clearSelection() }
    ActionEffects(viewModel.actions, snackbar)

    Scaffold(
        modifier = Modifier
            .bottomBarPadding(contentPadding)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (selection.isNotEmpty()) {
                SelectionTopBar(selection.size, viewModel::clearSelection) { viewModel.selectAll(allItems) }
            } else {
                TopAppBar(
                    title = { Text("Photos", fontWeight = FontWeight.Bold) },
                    actions = { GalleryIconButton(GalleryIcons.Settings, "Settings", onClick = onOpenSettings) },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = { SelectionActionsHost(viewModel, allItems) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            when {
                isLoading && sections.isEmpty() -> LoadingIndicator()
                sections.isEmpty() -> EmptyState(
                    icon = GalleryIcons.PhotoLibrary,
                    title = "No photos yet",
                    message = "Photos and videos you take or download show up here."
                )
                else -> MediaGrid(
                    sections = sections,
                    columns = columns,
                    selection = selection,
                    onItemClick = { item ->
                        if (selection.isNotEmpty()) viewModel.toggle(item) else onOpenMedia(MediaSource.All, item)
                    },
                    onItemLongClick = viewModel::toggle,
                    onColumnsChange = viewModel::setColumns,
                    onSectionToggle = viewModel::toggleSection,
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding())
                )
            }
        }
    }
}
