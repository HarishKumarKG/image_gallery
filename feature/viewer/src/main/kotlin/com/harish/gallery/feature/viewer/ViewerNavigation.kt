package com.harish.gallery.feature.viewer

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaSource

private const val VIEWER_ROUTE = "viewer/{${ViewerViewModel.ARG_SOURCE}}/{${ViewerViewModel.ARG_ID}}"

/** Opens [item] full screen; swiping moves through [source]. */
fun NavController.navigateToViewer(source: MediaSource, item: MediaItem) =
    navigate("viewer/${Uri.encode(source.key)}/${item.id}")

fun NavGraphBuilder.viewerScreen(onBack: () -> Unit, onEdit: (MediaItem) -> Unit) {
    composable(
        VIEWER_ROUTE,
        arguments = listOf(
            navArgument(ViewerViewModel.ARG_SOURCE) { type = NavType.StringType },
            navArgument(ViewerViewModel.ARG_ID) { type = NavType.StringType }
        )
    ) {
        ViewerScreen(onBack = onBack, onEdit = onEdit)
    }
}
