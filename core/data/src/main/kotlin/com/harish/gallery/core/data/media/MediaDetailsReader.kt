package com.harish.gallery.core.data.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.harish.gallery.core.model.MediaDetails
import com.harish.gallery.core.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads camera details and location from a photo's EXIF data.
 * Location needs the ACCESS_MEDIA_LOCATION permission (Android hides it otherwise).
 */
@Singleton
class MediaDetailsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun read(item: MediaItem): MediaDetails = withContext(Dispatchers.IO) {
        if (item.isVideo) return@withContext MediaDetails()
        try {
            var uri = item.contentUri
            val canSeeLocation = context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if (canSeeLocation) uri = MediaStore.setRequireOriginal(uri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                val camera = listOfNotNull(make, model).joinToString(" ")
                    .let { if (model != null && make != null && model.startsWith(make, ignoreCase = true)) model else it }
                    .ifBlank { null }
                val latLong = if (canSeeLocation) exif.latLong else null
                MediaDetails(
                    camera = camera,
                    aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0).takeIf { it > 0 }?.let { "f/%.1f".format(it) },
                    exposure = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0).takeIf { it > 0 }?.let(::exposureLabel),
                    iso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, 0).takeIf { it > 0 }?.let { "ISO $it" },
                    focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0).takeIf { it > 0 }?.let { "%.1f mm".format(it) },
                    latitude = latLong?.getOrNull(0),
                    longitude = latLong?.getOrNull(1)
                )
            } ?: MediaDetails()
        } catch (e: Exception) {
            MediaDetails()
        }
    }

    /** 0.008 s -> "1/125 s", 2.0 s -> "2 s" */
    private fun exposureLabel(seconds: Double): String =
        if (seconds >= 1) "%.0f s".format(seconds) else "1/${Math.round(1 / seconds)} s"
}
