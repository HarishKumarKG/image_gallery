package com.harish.gallery.feature.editor

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.harish.gallery.core.data.actions.ActionResult
import com.harish.gallery.core.data.actions.EditedImageSaver
import com.harish.gallery.core.domain.edit.Adjustments
import com.harish.gallery.core.domain.edit.CropAspect
import com.harish.gallery.core.domain.edit.CropHandle
import com.harish.gallery.core.domain.edit.CropRect
import com.harish.gallery.core.domain.edit.PhotoFilter
import com.harish.gallery.core.domain.edit.centeredCrop
import com.harish.gallery.core.domain.edit.drag
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.ui.ActionController
import com.harish.gallery.core.ui.decodeImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository,
    private val saver: EditedImageSaver
) : ViewModel() {

    private val mediaId: Long = savedStateHandle.get<String>(ARG_ID)?.toLongOrNull() ?: -1L

    /** The photo being edited (found once the media list has loaded). */
    private val _original = MutableStateFlow<MediaItem?>(null)
    val original: StateFlow<MediaItem?> = _original.asStateFlow()

    val actions = ActionController(viewModelScope)

    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state.asStateFlow()

    /** A screen-sized copy for the live preview (the full-size photo is only decoded on Save). */
    private var preview: Bitmap? = null
    private val _orientedPreview = MutableStateFlow<Bitmap?>(null)
    val orientedPreview: StateFlow<Bitmap?> = _orientedPreview.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch {
            // After Android restarts the app the list starts empty, so wait for the first scan
            mediaRepository.isLoading.first { !it }
            val item = mediaRepository.media.value.firstOrNull { it.id == mediaId }
            _original.value = item
            preview = item?.let { decodeImage(context, it.uri, PREVIEW_SIDE, mutable = true) }
            reorient()
            if (preview == null) actions.message("Couldn't open this photo")
        }
    }

    fun selectTab(tab: EditorTab) = _state.update { it.copy(tab = tab) }

    fun rotate() {
        _state.update { it.rotated() }
        viewModelScope.launch { reorient() }
    }

    fun flip() {
        _state.update { it.flippedHorizontally() }
        viewModelScope.launch { reorient() }
    }

    fun setAspect(aspect: CropAspect) = _state.update {
        val ratio = aspect.ratio
        it.copy(aspect = aspect, crop = if (ratio == null) it.crop else centeredCrop(ratio, imageAspect()))
    }

    fun dragCrop(handle: CropHandle, dx: Float, dy: Float) = _state.update {
        it.copy(crop = it.crop.drag(handle, dx, dy, it.aspect.ratio, imageAspect()))
    }

    fun setFilter(filter: PhotoFilter) = _state.update { it.copy(filter = filter) }
    fun setAdjustments(adjustments: Adjustments) = _state.update { it.copy(adjustments = adjustments) }

    fun reset() {
        _state.value = EditState(tab = _state.value.tab)
        viewModelScope.launch { reorient() }
    }

    /** Applies everything to the full-size photo and saves it as a NEW file. */
    fun save() {
        val item = _original.value ?: return
        val edits = _state.value
        actions.run(onSuccess = { _saved.value = true }) {
            val full = decodeImage(context, item.uri, SAVE_SIDE, mutable = true)
                ?: return@run ActionResult.Failed("Couldn't open this photo")
            val result = withContext(Dispatchers.Default) { full.applyEdits(edits) }
            saver.saveCopy(result, item)
        }
    }

    private fun imageAspect(): Float = _orientedPreview.value?.let { it.width.toFloat() / it.height } ?: 1f

    private suspend fun reorient() {
        val base = preview ?: return
        val edits = _state.value
        _orientedPreview.value = withContext(Dispatchers.Default) { base.oriented(edits) }
    }

    companion object {
        const val ARG_ID = "id"
        private const val PREVIEW_SIDE = 1600
        private const val SAVE_SIDE = 4096
    }
}
