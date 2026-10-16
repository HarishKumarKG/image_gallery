package com.harish.gallery.feature.locked

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val LOCKED_ROUTE = "locked"

fun NavController.navigateToLocked() = navigate(LOCKED_ROUTE)

fun NavGraphBuilder.lockedScreen(onBack: () -> Unit) {
    composable(LOCKED_ROUTE) { LockedScreen(onBack = onBack) }
}
