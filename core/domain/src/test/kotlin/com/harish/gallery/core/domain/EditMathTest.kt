package com.harish.gallery.core.domain

import com.harish.gallery.core.domain.edit.Adjustments
import com.harish.gallery.core.domain.edit.ColorMatrices
import com.harish.gallery.core.domain.edit.CropHandle
import com.harish.gallery.core.domain.edit.CropRect
import com.harish.gallery.core.domain.edit.PhotoFilter
import com.harish.gallery.core.domain.edit.centeredCrop
import com.harish.gallery.core.domain.edit.drag
import com.harish.gallery.core.domain.edit.editColorMatrix
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EditMathTest {
    private val eps = 0.01f

    @Test fun `original filter and neutral sliders change nothing`() {
        val m = editColorMatrix(PhotoFilter.NONE, Adjustments())
        assertArrayEquals(floatArrayOf(10f, 120f, 240f, 255f), ColorMatrices.apply(m, 10f, 120f, 240f), eps)
    }

    @Test fun `mono makes every channel equal`() {
        val (r, g, b) = ColorMatrices.apply(PhotoFilter.MONO.matrix(), 200f, 50f, 10f).toList()
        assertEquals(r, g, eps); assertEquals(g, b, eps)
    }

    @Test fun `brightness lifts and concat applies in order`() {
        val brighter = ColorMatrices.apply(Adjustments(brightness = 0.5f).matrix(), 100f, 100f, 100f)
        assertEquals(140f, brighter[0], eps)
        // first +40, then x2 contrast around 128: (140 - 128) * 2 + 128 = 152
        val both = ColorMatrices.concat(ColorMatrices.brightness(0.5f), ColorMatrices.contrast(1f))
        assertEquals(152f, ColorMatrices.apply(both, 100f, 100f, 100f)[0], eps)
    }

    @Test fun `square crop on a landscape photo is centred`() {
        val c = centeredCrop(1f, imageAspect = 4f / 3f)
        assertEquals(0.75f, c.width, eps)
        assertEquals(0.125f, c.left, eps)
        assertEquals(1f, c.height, eps)
    }

    @Test fun `free corner drag stays inside the photo`() {
        val c = CropRect.FULL.drag(CropHandle.TOP_LEFT, dx = -0.2f, dy = 0.3f, ratio = null, imageAspect = 1f)
        assertEquals(0f, c.left, eps)
        assertEquals(0.3f, c.top, eps)
    }

    @Test fun `fixed ratio drag keeps the shape`() {
        val start = centeredCrop(1f, imageAspect = 1f) // full square photo
        val c = start.drag(CropHandle.BOTTOM_RIGHT, dx = -0.5f, dy = 0f, ratio = 1f, imageAspect = 1f)
        assertEquals(c.width, c.height, eps)
        assertEquals(0f, c.left, eps)
        assertEquals(0f, c.top, eps)
    }

    @Test fun `move never leaves the photo`() {
        val c = CropRect(0.2f, 0.2f, 0.6f, 0.6f).drag(CropHandle.MOVE, dx = 1f, dy = -1f, ratio = null, imageAspect = 1f)
        assertEquals(1f, c.right, eps)
        assertEquals(0f, c.top, eps)
    }

    @Test fun `rotating the crop four times gives it back`() {
        val c = CropRect(0.1f, 0.2f, 0.5f, 0.9f)
        val back = c.rotatedClockwise().rotatedClockwise().rotatedClockwise().rotatedClockwise()
        assertEquals(c.left, back.left, eps); assertEquals(c.bottom, back.bottom, eps)
        val flipped = c.flippedHorizontally().flippedHorizontally()
        assertEquals(c.left, flipped.left, eps); assertEquals(c.right, flipped.right, eps)
    }
}
