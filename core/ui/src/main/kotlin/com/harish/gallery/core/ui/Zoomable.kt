package com.harish.gallery.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Zoom and pan for one photo. Scale 1 = fits the screen; up to 5x.
 * Offset is how far the zoomed photo is moved from the centre (in pixels).
 */
@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    val isZoomed: Boolean get() = scale > 1.01f

    internal var size = IntSize.Zero

    /** Pinch / drag: keep the point between your fingers under your fingers. */
    internal fun transform(zoom: Float, pan: Offset, centroid: Offset) {
        val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val center = Offset(size.width / 2f, size.height / 2f)
        val fromCenter = centroid - center
        val contentPoint = (fromCenter - offset) / scale
        offset = clamp(fromCenter - contentPoint * newScale + pan, newScale)
        scale = newScale
    }

    internal fun set(newScale: Float, newOffset: Offset) {
        scale = newScale
        offset = clamp(newOffset, newScale)
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /** The photo can't be dragged further than its own edges. */
    private fun clamp(o: Offset, s: Float): Offset {
        val maxX = size.width * (s - 1) / 2f
        val maxY = size.height * (s - 1) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 5f
        const val DOUBLE_TAP_SCALE = 2.5f
    }
}

/**
 * Pinch to zoom and drag to move when zoomed (taps are handled by [doubleTapZoom]).
 * A one-finger swipe on an un-zoomed photo is left alone, so the viewer can swipe to the next one.
 */
fun Modifier.zoomable(state: ZoomState): Modifier = this
    .pointerInput(state) {
        state.size = size
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val fingers = event.changes.count { it.pressed }
                if (fingers >= 2 || state.isZoomed) {
                    state.size = size
                    state.transform(event.calculateZoom(), event.calculatePan(), event.calculateCentroid(useCurrent = true))
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }
    .graphicsLayer {
        scaleX = state.scale
        scaleY = state.scale
        translationX = state.offset.x
        translationY = state.offset.y
    }

/** Double-tap handling with a smooth zoom animation. Put it on the same element as [zoomable]. */
@Composable
fun Modifier.doubleTapZoom(state: ZoomState, onTap: () -> Unit): Modifier {
    val scope = rememberCoroutineScope()
    return this.pointerInput(state) {
        detectTapGestures(
            onTap = { onTap() },
            onDoubleTap = { tap ->
                scope.launch {
                    val from = state.scale
                    val to = if (state.isZoomed) 1f else ZoomState.DOUBLE_TAP_SCALE
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val target = if (to > 1f) (center - tap) * (to - 1f) else Offset.Zero
                    val startOffset = state.offset
                    val anim = Animatable(0f)
                    anim.animateTo(1f) {
                        val t = value
                        state.set(from + (to - from) * t, startOffset + (target - startOffset) * t)
                    }
                }
            }
        )
    }
}

/**
 * A photo filling the screen, sharp enough to zoom into.
 * Shows the (cached) thumbnail at once, then swaps in the big version when it's decoded.
 * [uri] may be content:// or file:// (Locked folder).
 */
@Composable
fun ZoomableImage(
    uri: String,
    state: ZoomState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailUri: String? = uri
) {
    val context = LocalContext.current
    val window = LocalWindowInfo.current.containerSize
    // 2x the screen's longest side (so zooming stays sharp), capped to keep memory sane
    val maxSide = (max(window.width, window.height) * 2).coerceIn(1080, 4096)
    val full: ImageBitmap? by produceState<ImageBitmap?>(null, uri, maxSide) {
        value = decodeImage(context, uri, maxSide)?.asImageBitmap()
    }
    val thumb: ImageBitmap? by produceState(thumbnailUri?.let { ThumbnailCache.get("$it@512") }, thumbnailUri) {
        if (value == null && thumbnailUri != null && thumbnailUri.startsWith("content://")) value = loadThumbnail(context, thumbnailUri, 512)
    }
    Box(
        modifier
            .fillMaxSize()
            .doubleTapZoom(state, onTap)
            .zoomable(state),
        contentAlignment = Alignment.Center
    ) {
        val shown = full ?: thumb
        if (shown != null) {
            Image(shown, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        }
    }
}

/** One ZoomState per page; resets when the page is swiped away. */
@Composable
fun rememberZoomState(isCurrentPage: Boolean): ZoomState {
    val state = remember { ZoomState() }
    LaunchedEffect(isCurrentPage) { if (!isCurrentPage) state.reset() }
    return state
}
