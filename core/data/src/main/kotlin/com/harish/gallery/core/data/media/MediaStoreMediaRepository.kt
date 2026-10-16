package com.harish.gallery.core.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import com.harish.gallery.core.data.di.ApplicationScope
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.model.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Reads every photo and video from Android's MediaStore (the phone's media database).
 *
 * - One query for normal items, one for the trash.
 * - A ContentObserver watches MediaStore, so a new camera photo or a file deleted by another
 *   app shows up by itself (changes are bundled: one re-read 400 ms after the last change).
 */
@Singleton
class MediaStoreMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope
) : MediaRepository {

    private val _media = MutableStateFlow<List<MediaItem>>(emptyList())
    override val media: StateFlow<List<MediaItem>> = _media.asStateFlow()

    private val _trashed = MutableStateFlow<List<MediaItem>>(emptyList())
    override val trashed: StateFlow<List<MediaItem>> = _trashed.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val refreshLock = Mutex()
    private var pendingRefresh: Job? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            pendingRefresh?.cancel()
            pendingRefresh = scope.launch {
                delay(400)
                refresh()
            }
        }
    }

    init {
        context.contentResolver.registerContentObserver(FILES_URI, true, observer)
        scope.launch { refresh() }
    }

    override suspend fun refresh() {
        refreshLock.withLock {
            withContext(Dispatchers.IO) {
                try {
                    _media.value = query(trashed = false)
                    _trashed.value = query(trashed = true)
                } catch (e: SecurityException) {
                    // No permission yet: the permission screen calls refresh() again once it's granted
                    Log.i(TAG, "No media permission yet")
                } catch (e: Exception) {
                    Log.e(TAG, "MediaStore query failed", e)
                }
            }
            _isLoading.value = false
        }
    }

    private fun query(trashed: Boolean): List<MediaItem> {
        val args = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${FileColumns.MEDIA_TYPE} IN (?, ?)")
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(FileColumns.MEDIA_TYPE_IMAGE.toString(), FileColumns.MEDIA_TYPE_VIDEO.toString())
            )
            putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, "${FileColumns.DATE_MODIFIED} DESC")
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, if (trashed) MediaStore.MATCH_ONLY else MediaStore.MATCH_EXCLUDE)
            putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_EXCLUDE)
        }
        val items = ArrayList<MediaItem>()
        context.contentResolver.query(FILES_URI, PROJECTION, args, null)?.use { c ->
            val cols = Columns(c)
            while (c.moveToNext()) items += cols.read(c)
        }
        // Newest first by the date the photo was TAKEN (MediaStore can only sort by file date)
        items.sortByDescending { it.dateTakenMs }
        return items
    }

    /** Column positions, looked up once per query. */
    private class Columns(c: Cursor) {
        val id = c.getColumnIndexOrThrow(FileColumns._ID)
        val name = c.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
        val mime = c.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
        val type = c.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
        val taken = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
        val modified = c.getColumnIndexOrThrow(FileColumns.DATE_MODIFIED)
        val size = c.getColumnIndexOrThrow(FileColumns.SIZE)
        val width = c.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
        val height = c.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
        val duration = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION)
        val bucketId = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
        val bucketName = c.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        val relativePath = c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
        val data = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        val favorite = c.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE)
        val expires = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES)

        fun read(c: Cursor): MediaItem {
            val itemId = c.getLong(id)
            val isVideo = c.getInt(type) == FileColumns.MEDIA_TYPE_VIDEO
            val modifiedSec = c.getLong(modified)
            val takenMs = c.getLong(taken).takeIf { it > 0 } ?: (modifiedSec * 1000)
            val path = c.getString(data).orEmpty()
            return MediaItem(
                id = itemId,
                uri = contentUriFor(itemId, isVideo).toString(),
                name = c.getString(name) ?: path.substringAfterLast('/'),
                mimeType = c.getString(mime) ?: if (isVideo) "video/*" else "image/*",
                type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                dateTakenMs = takenMs,
                dateModifiedSec = modifiedSec,
                sizeBytes = c.getLong(size),
                width = c.getInt(width),
                height = c.getInt(height),
                durationMs = if (isVideo) c.getLong(duration) else 0,
                albumId = c.getString(bucketId) ?: path.substringBeforeLast('/'),
                albumName = c.getString(bucketName).orEmpty(),
                relativePath = c.getString(relativePath).orEmpty(),
                path = path,
                isFavorite = c.getInt(favorite) == 1,
                trashExpiresSec = c.getLong(expires)
            )
        }
    }

    private companion object {
        const val TAG = "MediaRepository"
        val FILES_URI: Uri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val PROJECTION = arrayOf(
            FileColumns._ID,
            FileColumns.DISPLAY_NAME,
            FileColumns.MIME_TYPE,
            FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.DATE_TAKEN,
            FileColumns.DATE_MODIFIED,
            FileColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.IS_FAVORITE,
            MediaStore.MediaColumns.DATE_EXPIRES
        )
    }
}

/** content:// address of a photo or video by its MediaStore id. */
fun contentUriFor(id: Long, isVideo: Boolean): Uri = ContentUris.withAppendedId(
    if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
    id
)

/** The content:// Uri of an item, ready for ContentResolver / Intents. */
val MediaItem.contentUri: Uri get() = Uri.parse(uri)
