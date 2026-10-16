package com.harish.gallery.feature.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.harish.gallery.core.domain.edit.Adjustments
import com.harish.gallery.core.domain.edit.CropAspect
import com.harish.gallery.core.domain.edit.CropRect
import com.harish.gallery.core.domain.edit.PhotoFilter
import com.harish.gallery.core.domain.edit.editColorMatrix

enum class EditorTab(val label: String) { CROP("Crop"), FILTERS("Filters"), ADJUST("Adjust") }

/**
 * Everything you've changed so far. The original photo is never touched;
 * these settings are applied to a copy when you tap Save.
 *
 * Turning and mirroring are stored as "first mirror (if [flipped]), then turn [quarterTurns] x 90°".
 * The crop box is in the turned photo's coordinates (what you see on screen).
 */
data class EditState(
    val quarterTurns: Int = 0,
    val flipped: Boolean = false,
    val crop: CropRect = CropRect.FULL,
    val aspect: CropAspect = CropAspect.FREE,
    val filter: PhotoFilter = PhotoFilter.NONE,
    val adjustments: Adjustments = Adjustments(),
    val tab: EditorTab = EditorTab.CROP
) {
    val hasChanges: Boolean
        get() = quarterTurns != 0 || flipped || !crop.isFull || filter != PhotoFilter.NONE || !adjustments.isNeutral

    /** Turn 90° clockwise (the crop box turns with the photo). */
    fun rotated() = copy(quarterTurns = (quarterTurns + 1) % 4, crop = crop.rotatedClockwise())

    /** Mirror left <-> right. Mirroring after turning = turning the other way after mirroring. */
    fun flippedHorizontally() = copy(flipped = !flipped, quarterTurns = (4 - quarterTurns) % 4, crop = crop.flippedHorizontally())

    fun colorMatrix(): FloatArray = editColorMatrix(filter, adjustments)
}

/** Turns / mirrors a bitmap the same way the preview shows it. */
fun Bitmap.oriented(state: EditState): Bitmap {
    if (state.quarterTurns == 0 && !state.flipped) return this
    val matrix = Matrix().apply {
        if (state.flipped) postScale(-1f, 1f)
        postRotate(90f * state.quarterTurns)
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/** The final photo: turn/mirror, crop, then colours. */
fun Bitmap.applyEdits(state: EditState): Bitmap {
    val turned = oriented(state)
    val c = state.crop
    val x = (c.left * turned.width).toInt().coerceIn(0, turned.width - 1)
    val y = (c.top * turned.height).toInt().coerceIn(0, turned.height - 1)
    val w = (c.width * turned.width).toInt().coerceIn(1, turned.width - x)
    val h = (c.height * turned.height).toInt().coerceIn(1, turned.height - y)
    val cropped = if (c.isFull) turned else Bitmap.createBitmap(turned, x, y, w, h)
    if (state.filter == PhotoFilter.NONE && state.adjustments.isNeutral) return cropped

    val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(state.colorMatrix())) }
    Canvas(out).drawBitmap(cropped, 0f, 0f, paint)
    return out
}
