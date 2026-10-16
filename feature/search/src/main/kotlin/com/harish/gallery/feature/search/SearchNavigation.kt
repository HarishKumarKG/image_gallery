package com.harish.gallery.feature.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource

const val SEARCH_ROUTE = "search"

fun NavGraphBuilder.searchScreen(contentPadding: PaddingValues, onOpenMedia: (MediaSource, MediaItem) -> Unit) {
    composable(SEARCH_ROUTE) { SearchScreen(onOpenMedia = onOpenMedia, contentPadding = contentPadding) }
}
