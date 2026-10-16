package com.harish.gallery.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.GalleryIconButton
import com.harish.gallery.core.model.GallerySettings
import com.harish.gallery.core.model.ThemeMode
import com.harish.gallery.core.model.TimelineGrouping
import com.harish.gallery.core.ui.MediaThumbnail
import com.harish.gallery.core.ui.countLabel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hidden by viewModel.hiddenAlbums.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permission switches can change in Android's settings: re-check every time we come back
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val canManageMedia = remember(refresh) { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && MediaStore.canManageMedia(context) }
    var hasLocation by remember(refresh) {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocation = it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { GalleryIconButton(GalleryIcons.ArrowBack, "Back", onClick = onBack) }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card("Look") {
                Label("Theme")
                Segmented(ThemeMode.entries, settings.themeMode, { it.label }, viewModel::setTheme)
            }

            Card("Photos") {
                Label("Group the timeline by")
                Segmented(TimelineGrouping.entries, settings.grouping, { it.label }, viewModel::setGrouping)
                Spacer(Modifier.height(12.dp))
                Label("Photos per row: ${settings.columns}  (or pinch the grid)")
                Slider(
                    value = settings.columns.toFloat(),
                    onValueChange = { viewModel.setColumns(it.roundToInt()) },
                    valueRange = GallerySettings.MIN_COLUMNS.toFloat()..GallerySettings.MAX_COLUMNS.toFloat(),
                    steps = GallerySettings.MAX_COLUMNS - GallerySettings.MIN_COLUMNS - 1
                )
                Label("Slideshow: ${settings.slideshowSeconds} s per photo")
                Slider(
                    value = settings.slideshowSeconds.toFloat(),
                    onValueChange = { viewModel.setSlideshowSeconds(it.roundToInt()) },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }

            Card("Hidden from Photos") {
                if (hidden.isEmpty()) {
                    Hint("Long-press an album in Albums and choose “Hide from Photos” to keep it out of the timeline (e.g. Screenshots or WhatsApp).")
                } else {
                    hidden.forEach { album ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            MediaThumbnail(album.cover.uri, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)), sizePx = 128)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(album.name, style = MaterialTheme.typography.bodyLarge)
                                Text(countLabel(album.count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { viewModel.showAlbum(album) }) { Text("Show") }
                        }
                    }
                }
            }

            Card("Permissions") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PermissionRow(
                        title = "Skip confirmations",
                        text = "Let this app move to Trash, favourite and move photos without asking every time (Android's “Media management” access).",
                        granted = canManageMedia,
                        actionLabel = "Allow"
                    ) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA, Uri.parse("package:${context.packageName}")))
                    }
                }
                PermissionRow(
                    title = "Photo locations",
                    text = "Show where a photo was taken in its details.",
                    granted = hasLocation,
                    actionLabel = "Allow"
                ) { askLocation.launch(Manifest.permission.ACCESS_MEDIA_LOCATION) }
            }

            Card("About") {
                val version = remember {
                    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
                }
                Text("Harish Gallery $version", style = MaterialTheme.typography.bodyLarge)
                Hint("Works fully offline. Nothing leaves your phone.")
            }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp))
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Label(text: String) = Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))

@Composable
private fun Hint(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size)
            ) { Text(label(option)) }
        }
    }
}

@Composable
private fun PermissionRow(title: String, text: String, granted: Boolean, actionLabel: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Hint(text)
        }
        if (granted) Text("On", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp))
        else TextButton(onClick = onAction) { Text(actionLabel) }
    }
}
