package com.harish.gallery.core.model

/** Extra details read from a photo's EXIF data for the info sheet. Every field may be missing. */
data class MediaDetails(
    val camera: String? = null,        // "Google Pixel 9"
    val aperture: String? = null,      // "f/1.7"
    val exposure: String? = null,      // "1/120 s"
    val iso: String? = null,           // "ISO 100"
    val focalLength: String? = null,   // "6.9 mm"
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val hasCameraInfo: Boolean get() = listOf(camera, aperture, exposure, iso, focalLength).any { it != null }
    val hasLocation: Boolean get() = latitude != null && longitude != null
}
