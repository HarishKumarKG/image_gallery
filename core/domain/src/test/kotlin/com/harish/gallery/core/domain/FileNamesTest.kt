package com.harish.gallery.core.domain

import com.harish.gallery.core.domain.rename.NameCheck
import com.harish.gallery.core.domain.rename.checkNewAlbumName
import com.harish.gallery.core.domain.rename.checkNewFileName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamesTest {
    @Test fun `extension is kept`() {
        assertEquals(NameCheck.Ok("Ooty.jpg"), checkNewFileName("Ooty", "IMG_1.jpg", emptyList()))
        assertEquals(NameCheck.Ok("Ooty.jpg"), checkNewFileName("Ooty.jpg", "IMG_1.jpg", emptyList()))
    }

    @Test fun `bad names are refused`() {
        assertTrue(checkNewFileName("a/b", "IMG_1.jpg", emptyList()) is NameCheck.Problem)
        assertTrue(checkNewFileName(".x", "IMG_1.jpg", emptyList()) is NameCheck.Problem)
        assertTrue(checkNewFileName("ooty", "IMG_1.jpg", listOf("Ooty.JPG")) is NameCheck.Problem)
        assertEquals(NameCheck.Problem(""), checkNewFileName("IMG_1", "IMG_1.jpg", emptyList()))
    }

    @Test fun `album names`() {
        assertEquals(NameCheck.Ok("Trip 2026"), checkNewAlbumName(" Trip 2026 ", listOf("Camera")))
        assertTrue(checkNewAlbumName("camera", listOf("Camera")) is NameCheck.Problem)
    }
}
