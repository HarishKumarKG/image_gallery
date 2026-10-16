package com.harish.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.repository.SettingsRepository
import com.harish.gallery.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.settings.map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, settingsRepository.settings.value.themeMode)

    /** After the permission is granted (or changed in Android's settings): read the photos again. */
    fun reloadMedia() {
        viewModelScope.launch { mediaRepository.refresh() }
    }
}
