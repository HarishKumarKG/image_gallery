package com.harish.gallery.core.data.actions

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.harish.gallery.core.data.media.contentUri
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Every change to photos/videos goes through here, using MediaStore's official requests:
 *
 *  - Trash / restore / delete forever / favourite: MediaStore.create*Request -> Android's own
 *    popup ("Allow Gallery to move 3 items to trash?"). Android does the change itself.
 *  - Rename / move: we update the file; if it belongs to another app, Android first asks for
 *    write access (createWriteRequest), then we try again.
 *  - Copy: a brand-new file owned by us, so no popup at all.
 *
 * Tip: Settings -> "Media management" lets the app skip these popups (Android 12+).
 */
@Singleton
class MediaActions @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {
    private val resolver get() = context.contentResolver

    fun moveToTrash(items: List<MediaItem>): ActionResult = systemRequest(items, "${itemCount(items.size)} moved to Trash") {
        MediaStore.createTrashRequest(resolver, it, true)
    }

    fun restoreFromTrash(items: List<MediaItem>): ActionResult = systemRequest(items, "${itemCount(items.size)} restored") {
        MediaStore.createTrashRequest(resolver, it, false)
    }

    fun deleteForever(items: List<MediaItem>): ActionResult = systemRequest(items, "${itemCount(items.size)} deleted") {
        MediaStore.createDeleteRequest(resolver, it)
    }

    fun setFavorite(items: List<MediaItem>, favorite: Boolean): ActionResult = systemRequest(
        items, if (favorite) "Added to Favourites" else "Removed from Favourites"
    ) { MediaStore.createFavoriteRequest(resolver, it, favorite) }

    /** Renames the file on the phone (the extension should already be in [newFileName]). */
    suspend fun rename(item: MediaItem, newFileName: String): ActionResult =
        writeWithConsent(listOf(item), "Renamed to $newFileName") {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName) }
            resolver.update(item.contentUri, values, null, null) > 0
        }

    /**
     * Moves items into another folder, e.g. "Pictures/Ooty trip/".
     * Android only allows photos in DCIM/ and Pictures/ (videos also in Movies/).
     */
    suspend fun moveTo(items: List<MediaItem>, relativePath: String, albumName: String): ActionResult =
        writeWithConsent(items, "${itemCount(items.size)} moved to $albumName") {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath) }
            items.all { resolver.update(it.contentUri, values, null, null) > 0 }
        }

    /** Copies items into a folder. The copies are new files owned by this app (no popup). */
    suspend fun copyTo(items: List<MediaItem>, relativePath: String, albumName: String): ActionResult =
        withContext(Dispatchers.IO) {
            try {
                items.forEach { copyOne(it, relativePath) }
                mediaRepository.refresh()
                ActionResult.Done("${itemCount(items.size)} copied to $albumName")
            } catch (e: Exception) {
                Log.w(TAG, "Copy failed", e)
                ActionResult.Failed(e.toUserMessage("Couldn't copy"))
            }
        }

    private fun copyOne(item: MediaItem, relativePath: String) {
        val collection = if (item.isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.DATE_TAKEN, item.dateTakenMs)
            put(MediaStore.MediaColumns.IS_PENDING, 1) // hidden from other apps until the copy is complete
        }
        val target: Uri = resolver.insert(collection, values) ?: error("No new file")
        try {
            resolver.openInputStream(item.contentUri).use { input ->
                resolver.openOutputStream(target).use { output ->
                    requireNotNull(input).copyTo(requireNotNull(output))
                }
            }
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(target, null, null) // don't leave half a file behind
            throw e
        }
    }

    /** Trash, delete and favourite: Android shows its popup and makes the change itself. */
    private fun systemRequest(
        items: List<MediaItem>,
        doneMessage: String,
        build: (List<Uri>) -> android.app.PendingIntent
    ): ActionResult {
        if (items.isEmpty()) return ActionResult.Done()
        return try {
            val pending = build(items.map { it.contentUri })
            ActionResult.NeedsConsent(pending.intentSender, onGranted = {
                mediaRepository.refresh()
                ActionResult.Done(doneMessage)
            })
        } catch (e: Exception) {
            Log.w(TAG, "Request failed", e)
            ActionResult.Failed(e.toUserMessage("Android didn't allow this"))
        }
    }

    /** Rename / move: try straight away; if the files belong to another app, ask once and retry. */
    private suspend fun writeWithConsent(
        items: List<MediaItem>,
        doneMessage: String,
        write: () -> Boolean
    ): ActionResult = withContext(Dispatchers.IO) {
        suspend fun attempt(): ActionResult = try {
            if (write()) {
                mediaRepository.refresh()
                ActionResult.Done(doneMessage)
            } else {
                ActionResult.Failed("Couldn't change this file")
            }
        } catch (e: SecurityException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Write failed", e)
            ActionResult.Failed(e.toUserMessage("Couldn't change this file"))
        }

        try {
            attempt()
        } catch (e: SecurityException) {
            val request = MediaStore.createWriteRequest(resolver, items.map { it.contentUri })
            ActionResult.NeedsConsent(request.intentSender, onGranted = {
                withContext(Dispatchers.IO) {
                    try {
                        attempt()
                    } catch (e: SecurityException) {
                        ActionResult.Failed("Android didn't allow changing this file")
                    }
                }
            })
        }
    }

    private companion object {
        const val TAG = "MediaActions"
    }
}

/** MediaStore errors are technical; turn the common ones into plain words. */
internal fun Exception.toUserMessage(fallback: String): String {
    val text = message.orEmpty()
    return when {
        "Primary directory" in text || "not allowed" in text ->
            "Android only allows photos in DCIM or Pictures (videos also in Movies)"
        "already exists" in text -> "A file with this name already exists there"
        else -> fallback
    }
}
