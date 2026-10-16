package com.harish.gallery.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.GalleryIcon
import com.harish.gallery.feature.albums.ALBUMS_ROUTE
import com.harish.gallery.feature.albums.albumsScreens
import com.harish.gallery.feature.albums.navigateToAlbum
import com.harish.gallery.feature.editor.editorScreen
import com.harish.gallery.feature.editor.navigateToEditor
import com.harish.gallery.feature.locked.lockedScreen
import com.harish.gallery.feature.locked.navigateToLocked
import com.harish.gallery.feature.photos.PHOTOS_ROUTE
import com.harish.gallery.feature.photos.photosScreen
import com.harish.gallery.feature.search.SEARCH_ROUTE
import com.harish.gallery.feature.search.searchScreen
import com.harish.gallery.feature.settings.navigateToSettings
import com.harish.gallery.feature.settings.settingsScreen
import com.harish.gallery.feature.viewer.navigateToViewer
import com.harish.gallery.feature.viewer.viewerScreen

/** The three tabs at the bottom. */
private enum class TopLevel(val route: String, val label: String, val icon: Int) {
    PHOTOS(PHOTOS_ROUTE, "Photos", GalleryIcons.Photo),
    ALBUMS(ALBUMS_ROUTE, "Albums", GalleryIcons.PhotoAlbum),
    SEARCH(SEARCH_ROUTE, "Search", GalleryIcons.Search)
}

/**
 * The app shell: permission gate, bottom tabs and the navigation graph.
 * This is the only place that knows about every feature — the features don't know each other,
 * they just call lambdas like onOpenMedia(...) and the shell decides where that goes.
 */
@Composable
fun GalleryRoot(onMediaAccessChanged: () -> Unit) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(mediaAccess(context)) }
    var askedBefore by rememberSaveable { mutableStateOf(false) }
    val accessChanged by rememberUpdatedState(onMediaAccessChanged)

    val askPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        askedBefore = true
        access = mediaAccess(context)
        accessChanged()
    }

    // The user may change the permission in Android's settings while we're in the background
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = mediaAccess(context)
                if (now != access) {
                    access = now
                    accessChanged()
                }
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (access == MediaAccess.NONE) {
            PermissionScreen(askedBefore) { askPermissions.launch(mediaPermissions()) }
        } else {
            GalleryNavigation(
                partialAccess = access == MediaAccess.PARTIAL,
                onAllowAll = { askPermissions.launch(mediaPermissions()) }
            )
        }
    }
}

@Composable
private fun PartialAccessBanner(onAllowAll: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .statusBarsPadding()
            .clickable(onClick = onAllowAll)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GalleryIcon(GalleryIcons.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(12.dp))
        Text(
            "You've shared only some photos. Tap to allow all or pick more.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun GalleryNavigation(partialAccess: Boolean, onAllowAll: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = TopLevel.entries.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0), // each screen handles the status bar itself
        bottomBar = {
            AnimatedVisibility(showBottomBar, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                NavigationBar {
                    TopLevel.entries.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    // One copy of each tab; tapping a tab again keeps its scroll position
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { GalleryIcon(tab.icon, null) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val tabPadding = PaddingValues(bottom = padding.calculateBottomPadding())
        // The banner only shows on the tabs (never over the full-screen viewer or editor). It
        // already sits below the status bar, so the screen under it mustn't add that gap again.
        val banner = partialAccess && showBottomBar
        Column(Modifier.fillMaxSize()) {
            if (banner) PartialAccessBanner(onAllowAll)
            NavHost(
                navController,
                startDestination = PHOTOS_ROUTE,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (banner) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier)
            ) {
                photosScreen(
                    contentPadding = tabPadding,
                    onOpenMedia = navController::navigateToViewer,
                    onOpenSettings = navController::navigateToSettings
                )
                albumsScreens(
                    contentPadding = tabPadding,
                    onOpenSource = navController::navigateToAlbum,
                    onOpenLocked = navController::navigateToLocked,
                    onOpenMedia = navController::navigateToViewer,
                    onBack = navController::popBackStack
                )
                searchScreen(contentPadding = tabPadding, onOpenMedia = navController::navigateToViewer)
                viewerScreen(onBack = navController::popBackStack, onEdit = navController::navigateToEditor)
                editorScreen(onClose = navController::popBackStack)
                lockedScreen(onBack = navController::popBackStack)
                settingsScreen(onBack = navController::popBackStack)
            }
        }
    }
}
