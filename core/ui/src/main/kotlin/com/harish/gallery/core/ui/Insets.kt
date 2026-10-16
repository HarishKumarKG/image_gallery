package com.harish.gallery.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

/**
 * Leaves room for the app's bottom navigation bar, and tells the screen's own Scaffold that the
 * phone's navigation bar is already taken care of (so it isn't padded twice).
 */
fun Modifier.bottomBarPadding(padding: PaddingValues): Modifier {
    val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
    return this.padding(bottom).consumeWindowInsets(bottom)
}
