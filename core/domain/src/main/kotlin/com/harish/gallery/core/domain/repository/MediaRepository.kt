package com.harish.gallery.core.domain.repository

import com.harish.gallery.core.model.MediaItem
import kotlinx.coroutines.flow.StateFlow

/**
 * Where photos and videos come from. The domain only knows this interface;
 * :core:data implements it with Android's MediaStore (dependency inversion).
 */
interface MediaRepository {
    /** Every photo and video on the phone (not in the trash), newest first. Updates by itself. */
    val media: StateFlow<List<MediaItem>>

    /** Items in the trash, newest first. */
    val trashed: StateFlow<List<MediaItem>>

    /** True until the first scan has finished. */
    val isLoading: StateFlow<Boolean>

    /** Re-read everything now (also happens automatically when files change). */
    suspend fun refresh()
}
