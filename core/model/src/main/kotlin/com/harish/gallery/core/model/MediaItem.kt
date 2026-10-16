package com.harish.gallery.core.model

enum class MediaType { IMAGE, VIDEO }

/**
 * One photo or video on the phone (a row from Android's MediaStore).
 *
 * Example: a camera photo
 *   name = "IMG_20261014_093012.jpg", albumName = "Camera", relativePath = "DCIM/Camera/"
 */
data class MediaItem(
    val id: Long,
    /** content:// address used to load, share or edit it, e.g. "content://media/external/images/media/42". */
    val uri: String,
    val name: String,
    val mimeType: String,
    val type: MediaType,
    /** When it was taken (falls back to the file date when the camera didn't record one). */
    val dateTakenMs: Long,
    val dateModifiedSec: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    /** Videos only. */
    val durationMs: Long = 0,
    /** The folder it lives in: MediaStore's BUCKET_ID / BUCKET_DISPLAY_NAME. */
    val albumId: String,
    val albumName: String,
    /** Folder path inside the storage, e.g. "DCIM/Camera/". */
    val relativePath: String,
    /** Full file path, only shown in the info sheet. */
    val path: String,
    val isFavorite: Boolean = false,
    /** Items in the trash: when Android deletes them for good (seconds since 1970). 0 = not in trash. */
    val trashExpiresSec: Long = 0
) {
    val isVideo: Boolean get() = type == MediaType.VIDEO
}
