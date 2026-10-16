package com.harish.gallery.core.data.actions

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import com.harish.gallery.core.domain.repository.MediaRepository
import com.harish.gallery.core.domain.rename.baseNameOf
import com.harish.gallery.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Saves an edited photo as a NEW file next to the original ("IMG_1_edited.jpg"),
 * so the original is never lost. Photos outside DCIM/Pictures go to "Pictures/Edited/".
 */
@Singleton
class EditedImageSaver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRepository: MediaRepository
) {
    suspend fun saveCopy(bitmap: Bitmap, original: MediaItem): ActionResult = withContext(Dispatchers.IO) {
        val png = original.mimeType == "image/png"
        val folder = original.relativePath.takeIf { it.startsWith("DCIM/") || it.startsWith("Pictures/") }
            ?: "Pictures/Edited/"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, baseNameOf(original.name) + "_edited" + if (png) ".png" else ".jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, if (png) "image/png" else "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.DATE_TAKEN, original.dateTakenMs)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: return@withContext ActionResult.Failed("Couldn't save the photo")
        try {
            resolver.openOutputStream(uri).use { out ->
                bitmap.compress(if (png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG, 95, requireNotNull(out))
            }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            mediaRepository.refresh()
            ActionResult.Done("Saved as a copy in ${folder.trimEnd('/').substringAfterLast('/')}")
        } catch (e: Exception) {
            Log.w("EditedImageSaver", "Save failed", e)
            resolver.delete(uri, null, null)
            ActionResult.Failed("Couldn't save the photo")
        }
    }
}
