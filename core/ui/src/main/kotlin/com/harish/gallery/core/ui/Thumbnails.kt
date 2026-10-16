package com.harish.gallery.core.ui

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.LruCache
import android.util.Size
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * Small pictures for the grids, made by Android itself (ContentResolver.loadThumbnail) and kept
 * in a memory cache, so scrolling back up is instant. No image library needed.
 *
 * Memory: the cache uses 1/8 of the app's memory, e.g. ~64 MB on a phone that gives the app 512 MB
 * — enough for a few hundred 256 px thumbnails.
 */
object ThumbnailCache {
    private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4 / 1024
    }

    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, bitmap: ImageBitmap) { cache.put(key, bitmap) }
    fun clear() = cache.evictAll()
}

// At most 4 thumbnails decode at the same time, so a fast fling doesn't flood the phone
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
private val thumbnailDispatcher = Dispatchers.IO.limitedParallelism(4)

suspend fun loadThumbnail(context: Context, uri: String, sizePx: Int): ImageBitmap? = withContext(thumbnailDispatcher) {
    val key = "$uri@$sizePx"
    ThumbnailCache.get(key)?.let { return@withContext it }
    try {
        context.contentResolver.loadThumbnail(Uri.parse(uri), Size(sizePx, sizePx), null)
            .asImageBitmap()
            .also { it.prepareToDraw(); ThumbnailCache.put(key, it) }
    } catch (e: Exception) {
        null // deleted file, no permission, broken image: the grey box stays
    }
}

/** A square-cropped thumbnail that fades in once it's ready. */
@Composable
fun MediaThumbnail(
    uri: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 256,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val key = "$uri@$sizePx"
    val bitmap by produceState(initialValue = ThumbnailCache.get(key), key) {
        // New size (after a pinch): keep showing the old picture until the sharper one is ready
        value = ThumbnailCache.get(key) ?: loadThumbnail(context, uri, sizePx) ?: value
    }
    val alpha by animateFloatAsState(if (bitmap != null) 1f else 0f, tween(180), label = "thumb")
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        bitmap?.let {
            Image(it, contentDescription = null, contentScale = contentScale, modifier = Modifier.fillMaxSize().alpha(alpha))
        }
    }
}

/**
 * A big, sharp version of a photo for the viewer and editor, scaled so its longest side is at
 * most [maxSidePx] (decoding a 50 MP photo at full size would waste ~200 MB).
 * [mutable] = a normal bitmap the editor can draw on (otherwise a faster GPU "hardware" bitmap).
 */
suspend fun decodeImage(context: Context, uri: String, maxSidePx: Int, mutable: Boolean = false): android.graphics.Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val longest = max(w, h)
                if (longest > maxSidePx) {
                    val scale = maxSidePx.toFloat() / longest
                    decoder.setTargetSize((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1))
                }
                if (mutable) {
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            }
        } catch (e: Exception) {
            null
        }
    }
