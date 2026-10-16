package com.harish.gallery.feature.locked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.data.locked.LockedFolderStore
import com.harish.gallery.core.model.LockedItem
import com.harish.gallery.core.ui.ActionController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class LockedViewModel @Inject constructor(
    private val store: LockedFolderStore
) : ViewModel() {

    val items: StateFlow<List<LockedItem>> = store.items
    val actions = ActionController(viewModelScope)

    /** Only true after a successful fingerprint / face / PIN check; cleared when you leave the app. */
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _selection = MutableStateFlow<Set<String>>(emptySet())
    val selection: StateFlow<Set<String>> = _selection.asStateFlow()

    fun onUnlocked() { _unlocked.value = true }
    fun lock() {
        _unlocked.value = false
        _selection.value = emptySet()
    }

    fun toggle(item: LockedItem) = _selection.update { if (item.id in it) it - item.id else it + item.id }
    fun clearSelection() { _selection.value = emptySet() }

    fun moveOut(items: List<LockedItem>) = actions.run(::clearSelection) { store.moveOut(items) }
    fun deleteForever(items: List<LockedItem>) = actions.run(::clearSelection) { store.deleteForever(items) }
}
