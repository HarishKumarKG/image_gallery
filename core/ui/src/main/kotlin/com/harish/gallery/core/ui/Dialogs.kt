package com.harish.gallery.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.GalleryIcon
import com.harish.gallery.core.domain.rename.NameCheck
import com.harish.gallery.core.domain.rename.checkNewAlbumName
import com.harish.gallery.core.model.Album

/** Simple "Are you sure?" dialog. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Where to move / copy photos: an existing album, or a new one in Pictures/. */
data class AlbumTarget(val relativePath: String, val name: String)

/**
 * "Move to album" / "Copy to album" picker.
 * Example: pick "Camera" -> AlbumTarget("DCIM/Camera/", "Camera");
 *          New album "Ooty" -> AlbumTarget("Pictures/Ooty/", "Ooty")
 */
@Composable
fun AlbumPickerDialog(
    title: String,
    albums: List<Album>,
    onPick: (AlbumTarget) -> Unit,
    onDismiss: () -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val check = remember(newName, albums) { checkNewAlbumName(newName, albums.map { it.name }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (creating) "New album" else title) },
        text = {
            if (creating) {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Album name") },
                        singleLine = true,
                        isError = (check as? NameCheck.Problem)?.reason?.isNotEmpty() == true,
                        supportingText = { Text((check as? NameCheck.Problem)?.reason?.takeIf { it.isNotEmpty() } ?: "Saved in Pictures/") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    item {
                        AlbumRow(name = "New album", subtitle = "Create a folder in Pictures", icon = GalleryIcons.Add, coverUri = null) {
                            creating = true
                        }
                    }
                    items(albums, key = { it.id }) { album ->
                        AlbumRow(name = album.name, subtitle = countLabel(album.count), icon = null, coverUri = album.cover.uri) {
                            onPick(AlbumTarget(album.relativePath, album.name))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(
                    onClick = { (check as? NameCheck.Ok)?.let { onPick(AlbumTarget("Pictures/${it.fileName}/", it.fileName)) } },
                    enabled = check is NameCheck.Ok
                ) { Text("Done") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (creating) creating = false else onDismiss() }) { Text(if (creating) "Back" else "Cancel") }
        }
    )
}

@Composable
private fun AlbumRow(name: String, subtitle: String, icon: Int?, coverUri: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (coverUri != null) MediaThumbnail(coverUri, Modifier.size(48.dp), sizePx = 128)
            else if (icon != null) GalleryIcon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
