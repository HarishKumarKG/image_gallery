package com.harish.gallery.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.EmptyState
import com.harish.gallery.core.designsystem.component.GalleryIcon
import com.harish.gallery.core.designsystem.component.GalleryIconButton
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource
import com.harish.gallery.core.ui.ActionEffects
import com.harish.gallery.core.ui.bottomBarPadding
import com.harish.gallery.core.ui.MediaGrid
import com.harish.gallery.core.ui.SelectionActionsHost
import com.harish.gallery.core.ui.SelectionTopBar
import com.harish.gallery.core.ui.countLabel

/**
 * Search by words: names, albums, months ("october"), years ("2024"), "video", "favourite".
 * Words combine: "goa 2024 video".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onOpenMedia: (MediaSource, MediaItem) -> Unit,
    contentPadding: PaddingValues,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val columns by viewModel.columns.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val snackbar = remember { SnackbarHostState() }

    BackHandler(enabled = selection.isNotEmpty()) { viewModel.clearSelection() }
    ActionEffects(viewModel.actions, snackbar)

    Scaffold(
        modifier = Modifier.bottomBarPadding(contentPadding),
        topBar = {
            if (selection.isNotEmpty()) {
                SelectionTopBar(selection.size, viewModel::clearSelection) { viewModel.selectAll(results) }
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    placeholder = { Text("Search photos: “beach”, “october 2024”, “video”") },
                    leadingIcon = { GalleryIcon(GalleryIcons.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) GalleryIconButton(GalleryIcons.Close, "Clear") { viewModel.setQuery("") } },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding() // room for the status bar (the app draws edge to edge)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        },
        bottomBar = { SelectionActionsHost(viewModel, results) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            when {
                query.isBlank() -> Column(Modifier.padding(16.dp)) {
                    Text("Try", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { word ->
                            AssistChip(onClick = { viewModel.setQuery(word) }, label = { Text(word) })
                        }
                    }
                }
                results.isEmpty() -> EmptyState(GalleryIcons.Search, "No results", "Nothing matches “${query.trim()}”. Try a month, a year or an album name.")
                else -> MediaGrid(
                    sections = sections,
                    columns = columns,
                    selection = selection,
                    onItemClick = { item ->
                        if (selection.isNotEmpty()) viewModel.toggle(item)
                        else onOpenMedia(MediaSource.Search(query.trim()), item)
                    },
                    onItemLongClick = viewModel::toggle,
                    onColumnsChange = viewModel::setColumns,
                    onSectionToggle = viewModel::toggleSection,
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                    header = {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                countLabel(results.size, "result", "results"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}
