package com.harish.gallery.feature.albums

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource

const val ALBUMS_ROUTE = "albums"
private const val ALBUM_DETAIL_ROUTE = "album/{${AlbumDetailViewModel.ARG_SOURCE}}"

/** Opens an album, Favourites, Videos or Trash. */
fun NavController.navigateToAlbum(source: MediaSource) = navigate("album/${Uri.encode(source.key)}")

fun NavGraphBuilder.albumsScreens(
    contentPadding: PaddingValues,
    onOpenSource: (MediaSource) -> Unit,
    onOpenLocked: () -> Unit,
    onOpenMedia: (MediaSource, MediaItem) -> Unit,
    onBack: () -> Unit
) {
    composable(ALBUMS_ROUTE) {
        AlbumsScreen(onOpenSource = onOpenSource, onOpenLocked = onOpenLocked, contentPadding = contentPadding)
    }
    composable(
        ALBUM_DETAIL_ROUTE,
        arguments = listOf(navArgument(AlbumDetailViewModel.ARG_SOURCE) { type = NavType.StringType })
    ) {
        AlbumDetailScreen(onBack = onBack, onOpenMedia = onOpenMedia)
    }
}
