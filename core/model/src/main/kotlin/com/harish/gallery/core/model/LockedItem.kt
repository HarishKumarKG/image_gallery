package com.harish.gallery.core.model

/**
 * A photo or video moved into the Locked folder. The file lives in the app's private storage,
 * so other apps (and the system gallery) can't see it.
 */
data class LockedItem(
    val id: String,
    /** Absolute path of the private copy. */
    val filePath: String,
    val originalName: String,
    val mimeType: String,
    val type: MediaType,
    /** Where it came from, so "Move out" can put it back, e.g. "DCIM/Camera/". */
    val originalRelativePath: String,
    val dateTakenMs: Long,
    val lockedAtMs: Long,
    val sizeBytes: Long
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO
}
