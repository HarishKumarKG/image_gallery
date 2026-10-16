package com.harish.gallery.core.data.locked

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.harish.gallery.core.data.actions.ActionResult
import com.harish.gallery.core.data.di.ApplicationScope
import com.harish.gallery.core.data.media.contentUri
import com.harish.gallery.core.domain.repository.LockedFolderRepository
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.rename.extensionOf
import com.harish.gallery.core.model.LockedItem
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The Locked folder: photos moved into the app's PRIVATE storage (files/locked/).
 * Other apps, the system gallery and USB file browsing can't see them; opening the folder
 * needs your fingerprint / face / screen lock (the screen asks, see :feature:locked).
 *
 * Moving in = copy to private storage, then delete the original (Android asks first).
 * If you tap "Deny", the private copies are removed again, so nothing is duplicated.
 *
 * Note: uninstalling the app deletes the Locked folder too (same as Google Photos).
 */
@Singleton
class LockedFolderStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope scope: CoroutineScope,
    private val mediaRepository: MediaRepository
) : LockedFolderRepository {

    private val dir = File(context.filesDir, "locked").apply { mkdirs() }
    private val index = File(dir, "index.json")
    private val lock = Mutex()

    private val _items = MutableStateFlow<List<LockedItem>>(emptyList())
    override val items: StateFlow<List<LockedItem>> = _items.asStateFlow()

    /** The saved index, read once in the background; every change waits for it first. */
    private val loaded = scope.async(Dispatchers.IO) { readIndex().also { _items.value = it } }

    /** Moves photos/videos into the Locked folder. */
    suspend fun lock(media: List<MediaItem>): ActionResult = withContext(Dispatchers.IO) {
        if (media.isEmpty()) return@withContext ActionResult.Done()
        val copies = ArrayList<LockedItem>()
        try {
            for (item in media) {
                val ext = extensionOf(item.name).ifEmpty { if (item.isVideo) "mp4" else "jpg" }
                val target = File(dir, "${UUID.randomUUID()}.$ext")
                context.contentResolver.openInputStream(item.contentUri).use { input ->
                    target.outputStream().use { requireNotNull(input).copyTo(it) }
                }
                copies += LockedItem(
                    id = target.nameWithoutExtension,
                    filePath = target.absolutePath,
                    originalName = item.name,
                    mimeType = item.mimeType,
                    type = item.type,
                    originalRelativePath = item.relativePath,
                    dateTakenMs = item.dateTakenMs,
                    lockedAtMs = System.currentTimeMillis(),
                    sizeBytes = target.length()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Copy into Locked folder failed", e)
            copies.forEach { File(it.filePath).delete() }
            return@withContext ActionResult.Failed("Couldn't move to the Locked folder")
        }

        // Save the copies in the index BEFORE asking to delete the originals: if the app is closed
        // while Android's popup is showing, the worst case is a duplicate, never a lost photo.
        update { it + copies }
        val deleteOriginals = MediaStore.createDeleteRequest(context.contentResolver, media.map { it.contentUri })
        ActionResult.NeedsConsent(
            request = deleteOriginals.intentSender,
            onGranted = {
                mediaRepository.refresh()
                ActionResult.Done("${count(copies.size)} moved to Locked folder")
            },
            onDenied = {
                // "Deny": the originals stay, so remove our copies again
                withContext(Dispatchers.IO) { copies.forEach { File(it.filePath).delete() } }
                val ids = copies.mapTo(HashSet()) { it.id }
                update { list -> list.filterNot { it.id in ids } }
            }
        )
    }

    /** Puts items back where they came from (or Pictures/Restored if Android doesn't allow that folder). */
    suspend fun moveOut(locked: List<LockedItem>): ActionResult = withContext(Dispatchers.IO) {
        var moved = 0
        for (item in locked) {
            if (restoreOne(item)) {
                File(item.filePath).delete()
                update { list -> list.filterNot { it.id == item.id } }
                moved++
            }
        }
        mediaRepository.refresh()
        if (moved == locked.size) ActionResult.Done("${count(moved)} moved out of Locked folder")
        else ActionResult.Failed("Moved $moved of ${locked.size}. Some files couldn't be restored")
    }

    /** Deletes for good (they never go to the Trash). */
    suspend fun deleteForever(locked: List<LockedItem>): ActionResult = withContext(Dispatchers.IO) {
        locked.forEach { File(it.filePath).delete() }
        val ids = locked.mapTo(HashSet()) { it.id }
        update { list -> list.filterNot { it.id in ids } }
        ActionResult.Done("${count(locked.size)} deleted")
    }

    private fun restoreOne(item: LockedItem): Boolean {
        val primary = item.originalRelativePath.substringBefore('/')
        val allowed = if (item.type == MediaType.VIDEO) setOf("DCIM", "Movies", "Pictures") else setOf("DCIM", "Pictures")
        val folder = if (primary in allowed) item.originalRelativePath else "Pictures/Restored/"
        val collection = if (item.type == MediaType.VIDEO) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.originalName)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.DATE_TAKEN, item.dateTakenMs)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = try {
            resolver.insert(collection, values)
        } catch (e: Exception) {
            Log.w(TAG, "Insert failed for ${item.originalName}", e)
            null
        } ?: return false
        return try {
            File(item.filePath).inputStream().use { input ->
                resolver.openOutputStream(uri).use { input.copyTo(requireNotNull(it)) }
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Restore failed for ${item.originalName}", e)
            resolver.delete(uri, null, null)
            false
        }
    }

    private suspend fun update(change: (List<LockedItem>) -> List<LockedItem>) = lock.withLock {
        loaded.await() // never write the index before the saved one has been read
        val next = change(_items.value).sortedByDescending { it.lockedAtMs }
        writeIndex(next)
        _items.value = next
    }

    private fun readIndex(): List<LockedItem> = try {
        if (!index.exists()) emptyList()
        else {
            val array = JSONArray(index.readText())
            (0 until array.length()).map { array.getJSONObject(it).toLockedItem() }
                .filter { File(it.filePath).exists() }
        }
    } catch (e: Exception) {
        Log.w(TAG, "Locked index unreadable", e)
        emptyList()
    }

    private fun writeIndex(list: List<LockedItem>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJson()) }
        val tmp = File(dir, "index.json.tmp")
        tmp.writeText(array.toString())
        tmp.renameTo(index)
    }

    private fun count(n: Int) = if (n == 1) "1 item" else "$n items"

    private companion object {
        const val TAG = "LockedFolder"
    }
}

private fun LockedItem.toJson() = JSONObject()
    .put("id", id)
    .put("filePath", filePath)
    .put("originalName", originalName)
    .put("mimeType", mimeType)
    .put("type", type.name)
    .put("originalRelativePath", originalRelativePath)
    .put("dateTakenMs", dateTakenMs)
    .put("lockedAtMs", lockedAtMs)
    .put("sizeBytes", sizeBytes)

private fun JSONObject.toLockedItem() = LockedItem(
    id = getString("id"),
    filePath = getString("filePath"),
    originalName = getString("originalName"),
    mimeType = getString("mimeType"),
    type = MediaType.valueOf(getString("type")),
    originalRelativePath = getString("originalRelativePath"),
    dateTakenMs = getLong("dateTakenMs"),
    lockedAtMs = getLong("lockedAtMs"),
    sizeBytes = getLong("sizeBytes")
)
