package com.harish.gallery.feature.photos

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource

const val PHOTOS_ROUTE = "photos"

/** Adds the Photos tab to the app's navigation graph. */
fun NavGraphBuilder.photosScreen(
    contentPadding: PaddingValues,
    onOpenMedia: (MediaSource, MediaItem) -> Unit,
    onOpenSettings: () -> Unit
) {
    composable(PHOTOS_ROUTE) {
        PhotosScreen(onOpenMedia = onOpenMedia, onOpenSettings = onOpenSettings, contentPadding = contentPadding)
    }
}
