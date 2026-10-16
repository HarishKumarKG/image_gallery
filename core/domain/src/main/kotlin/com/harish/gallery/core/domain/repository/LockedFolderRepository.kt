package com.harish.gallery.core.domain.repository

import com.harish.gallery.core.model.LockedItem
import kotlinx.coroutines.flow.StateFlow

/** Read side of the Locked folder (moving files in and out lives in :core:data). */
interface LockedFolderRepository {
    /** Locked items, most recently locked first. */
    val items: StateFlow<List<LockedItem>>
}
