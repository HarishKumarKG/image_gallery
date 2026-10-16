package com.harish.gallery.feature.locked

import android.content.Context
import android.graphics.ImageDecoder
import android.media.ThumbnailUtils
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.harish.gallery.core.model.LockedItem
import com.harish.gallery.core.ui.ThumbnailCache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Thumbnails for private files (Android's MediaStore doesn't know about them, so we make our own). */
@Composable
internal fun LockedThumbnail(item: LockedItem, modifier: Modifier = Modifier, sizePx: Int = 256) {
    val context = LocalContext.current
    val key = "locked:${item.id}@$sizePx"
    val bitmap by produceState(ThumbnailCache.get(key), key) {
        if (value == null) value = makeThumbnail(context, item, sizePx)?.also { ThumbnailCache.put(key, it) }
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

private suspend fun makeThumbnail(context: Context, item: LockedItem, sizePx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
    try {
        val file = File(item.filePath)
        val bitmap = if (item.isVideo) {
            ThumbnailUtils.createVideoThumbnail(file, Size(sizePx, sizePx), null)
        } else {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > sizePx) {
                    val scale = sizePx.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
                }
            }
        }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
