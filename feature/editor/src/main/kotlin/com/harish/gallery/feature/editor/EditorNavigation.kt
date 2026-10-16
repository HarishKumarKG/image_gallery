package com.harish.gallery.feature.editor

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.harish.gallery.core.model.MediaItem

private const val EDITOR_ROUTE = "editor/{${EditorViewModel.ARG_ID}}"

fun NavController.navigateToEditor(item: MediaItem) = navigate("editor/${item.id}")

fun NavGraphBuilder.editorScreen(onClose: () -> Unit) {
    composable(EDITOR_ROUTE, arguments = listOf(navArgument(EditorViewModel.ARG_ID) { type = NavType.StringType })) {
        EditorScreen(onClose = onClose)
    }
}
