package com.harish.gallery.feature.viewer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.GalleryIcon
import com.harish.gallery.core.domain.rename.NameCheck
import com.harish.gallery.core.domain.rename.baseNameOf
import com.harish.gallery.core.domain.rename.checkNewFileName
import com.harish.gallery.core.domain.rename.extensionOf
import com.harish.gallery.core.model.MediaDetails
import com.harish.gallery.core.model.MediaItem
import com.harish.gallery.core.ui.formatDateTime
import com.harish.gallery.core.ui.formatDuration
import com.harish.gallery.core.ui.formatResolution
import com.harish.gallery.core.ui.formatSize
import java.util.Locale

/** Details: name, date, size, resolution, folder, camera and (if allowed) location. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InfoSheet(item: MediaItem, details: MediaDetails?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            InfoRow(GalleryIcons.Info, "Taken", formatDateTime(item.dateTakenMs))
            InfoRow(GalleryIcons.Photo, if (item.isVideo) "Video" else "Photo",
                formatResolution(item.width, item.height) + " · " + formatSize(item.sizeBytes) +
                    if (item.isVideo) " · " + formatDuration(item.durationMs) else "")
            InfoRow(GalleryIcons.Folder, "Album", "${item.albumName} · ${item.relativePath}")
            if (details?.hasCameraInfo == true) {
                InfoRow(
                    GalleryIcons.Tune, details.camera ?: "Camera",
                    listOfNotNull(details.aperture, details.exposure, details.focalLength, details.iso).joinToString("  ·  ")
                )
            }
            if (details?.hasLocation == true) {
                val lat = details.latitude!!
                val lng = details.longitude!!
                InfoRow(GalleryIcons.Visibility, "Location", String.format(Locale.ROOT, "%.5f, %.5f", lat, lng))
                TextButton(onClick = {
                    val geo = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) }
                }) { Text("Open in Maps") }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(item.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: Int, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        GalleryIcon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (value.isNotBlank()) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Rename box: you type the name, the extension (.jpg) is kept. */
@Composable
internal fun RenameDialog(item: MediaItem, otherNames: List<String>, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    val base = remember(item) { baseNameOf(item.name) }
    val extension = remember(item) { extensionOf(item.name) }
    var input by remember(item) { mutableStateOf(TextFieldValue(base, selection = TextRange(0, base.length))) }
    val check = remember(input.text, otherNames) { checkNewFileName(input.text, item.name, otherNames) }
    val problem = (check as? NameCheck.Problem)?.reason?.takeIf { it.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Name") },
                suffix = if (extension.isNotEmpty()) ({ Text(".$extension") }) else null,
                singleLine = true,
                isError = problem != null,
                supportingText = if (problem != null) ({ Text(problem) }) else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { (check as? NameCheck.Ok)?.let { onRename(it.fileName) } }, enabled = check is NameCheck.Ok) {
                Text("Rename")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
