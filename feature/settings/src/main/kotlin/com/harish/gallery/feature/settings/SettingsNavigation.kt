package com.harish.gallery.feature.settings

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val SETTINGS_ROUTE = "settings"

fun NavController.navigateToSettings() = navigate(SETTINGS_ROUTE)

fun NavGraphBuilder.settingsScreen(onBack: () -> Unit) {
    composable(SETTINGS_ROUTE) { SettingsScreen(onBack = onBack) }
}
