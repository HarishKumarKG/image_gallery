package com.harish.gallery.core.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.harish.gallery.core.model.MediaItem

/** Opens Android's share sheet (WhatsApp, Gmail, Nearby Share...) for one or more items. */
fun shareMedia(context: Context, items: List<MediaItem>) {
    if (items.isEmpty()) return
    val uris = ArrayList(items.map { Uri.parse(it.uri) })
    val type = when {
        items.all { it.isVideo } -> "video/*"
        items.none { it.isVideo } -> "image/*"
        else -> "*/*"
    }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    }.apply {
        this.type = type
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

/** "Use as" — wallpaper, contact photo... (Android shows the apps that can take it). */
fun useMediaAs(context: Context, item: MediaItem) {
    val intent = Intent(Intent.ACTION_ATTACH_DATA)
        .setDataAndType(Uri.parse(item.uri), item.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .putExtra("mimeType", item.mimeType)
    context.startActivity(Intent.createChooser(intent, "Use as"))
}
