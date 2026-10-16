package com.harish.gallery.core.domain.edit

/**
 * The crop box, as fractions of the (already rotated) photo: 0,0 = top-left, 1,1 = bottom-right.
 * Using fractions means the same box works on the small preview and on the full-size photo.
 *
 * Example: CropRect(0.25f, 0f, 0.75f, 1f) keeps the middle half of a photo.
 */
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val isFull: Boolean get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f

    /** The same box after turning the photo 90° clockwise. */
    fun rotatedClockwise(): CropRect = CropRect(1f - bottom, left, 1f - top, right)

    /** The same box after mirroring the photo left <-> right. */
    fun flippedHorizontally(): CropRect = CropRect(1f - right, top, 1f - left, bottom)

    companion object {
        val FULL = CropRect(0f, 0f, 1f, 1f)
        const val MIN_SIZE = 0.08f
    }
}

enum class CropAspect(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    THREE_FOUR("3:4", 3f / 4f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    NINE_SIXTEEN("9:16", 9f / 16f)
}

enum class CropHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

/**
 * The biggest centred box with [ratio] (width / height in pixels) on a photo whose own
 * width / height is [imageAspect].
 */
fun centeredCrop(ratio: Float, imageAspect: Float): CropRect {
    // In fractions: w / h = ratio / imageAspect
    val wantWOverH = ratio / imageAspect
    return if (wantWOverH >= 1f) {
        val h = 1f / wantWOverH
        CropRect(0f, (1f - h) / 2f, 1f, (1f + h) / 2f)
    } else {
        val w = wantWOverH
        CropRect((1f - w) / 2f, 0f, (1f + w) / 2f, 1f)
    }
}

/**
 * Drags one corner (or the whole box) by dx, dy (fractions of the photo).
 * With a fixed [ratio] the box keeps its shape; it never leaves the photo or gets tiny.
 */
fun CropRect.drag(handle: CropHandle, dx: Float, dy: Float, ratio: Float?, imageAspect: Float): CropRect {
    if (handle == CropHandle.MOVE) {
        val nx = dx.coerceIn(-left, 1f - right)
        val ny = dy.coerceIn(-top, 1f - bottom)
        return CropRect(left + nx, top + ny, right + nx, bottom + ny)
    }
    val min = CropRect.MIN_SIZE
    var l = left; var t = top; var r = right; var b = bottom
    when (handle) {
        CropHandle.TOP_LEFT -> { l = (l + dx).coerceIn(0f, r - min); t = (t + dy).coerceIn(0f, b - min) }
        CropHandle.TOP_RIGHT -> { r = (r + dx).coerceIn(l + min, 1f); t = (t + dy).coerceIn(0f, b - min) }
        CropHandle.BOTTOM_LEFT -> { l = (l + dx).coerceIn(0f, r - min); b = (b + dy).coerceIn(t + min, 1f) }
        CropHandle.BOTTOM_RIGHT -> { r = (r + dx).coerceIn(l + min, 1f); b = (b + dy).coerceIn(t + min, 1f) }
        CropHandle.MOVE -> Unit
    }
    if (ratio == null) return CropRect(l, t, r, b)

    // Fixed shape: follow the width, work out the height, and keep the opposite corner still
    val wOverH = ratio / imageAspect
    var w = r - l
    var h = w / wOverH
    val anchorTop = handle == CropHandle.BOTTOM_LEFT || handle == CropHandle.BOTTOM_RIGHT
    val anchorLeft = handle == CropHandle.TOP_RIGHT || handle == CropHandle.BOTTOM_RIGHT
    val room = if (anchorTop) 1f - t else b
    if (h > room) { h = room; w = h * wOverH }
    if (anchorLeft) r = l + w else l = r - w
    if (anchorTop) b = t + h else t = b - h
    return CropRect(l, t, r, b)
}
