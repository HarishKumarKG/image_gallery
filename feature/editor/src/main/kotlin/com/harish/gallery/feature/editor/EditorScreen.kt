package com.harish.gallery.feature.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.harish.gallery.core.designsystem.GalleryIcons
import com.harish.gallery.core.designsystem.component.GalleryIconButton
import com.harish.gallery.core.designsystem.component.LoadingIndicator
import com.harish.gallery.core.domain.edit.Adjustments
import com.harish.gallery.core.domain.edit.CropAspect
import com.harish.gallery.core.domain.edit.CropHandle
import com.harish.gallery.core.domain.edit.CropRect
import com.harish.gallery.core.domain.edit.PhotoFilter
import com.harish.gallery.core.ui.ActionEffects
import com.harish.gallery.core.ui.ConfirmDialog
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Photo editor: Crop (free or fixed shapes, turn, mirror) · Filters · Adjust.
 * Save always makes a NEW copy ("IMG_1_edited.jpg"); the original stays untouched.
 */
@Composable
fun EditorScreen(onClose: () -> Unit, viewModel: EditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preview by viewModel.orientedPreview.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val busy by viewModel.actions.busy.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }

    ActionEffects(viewModel.actions) // Toast, so "Saved" still shows after the screen closes
    LaunchedEffect(saved) { if (saved) onClose() }
    BackHandler { if (state.hasChanges) confirmDiscard = true else onClose() }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top: ✕  ·  Reset  ·  Save copy
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            GalleryIconButton(GalleryIcons.Close, "Cancel", tint = Color.White) {
                if (state.hasChanges) confirmDiscard = true else onClose()
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = viewModel::reset, enabled = state.hasChanges && !busy) { Text("Reset") }
            Button(onClick = viewModel::save, enabled = state.hasChanges && !busy && preview != null, modifier = Modifier.padding(end = 8.dp)) {
                Text(if (busy) "Saving…" else "Save copy")
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            val bitmap = preview
            if (bitmap == null) LoadingIndicator()
            else EditPreview(bitmap, state, onDrag = viewModel::dragCrop)
        }

        SecondaryTabRow(selectedTabIndex = state.tab.ordinal, containerColor = Color.Black, contentColor = Color.White) {
            EditorTab.entries.forEach { tab ->
                Tab(selected = state.tab == tab, onClick = { viewModel.selectTab(tab) }, text = { Text(tab.label) })
            }
        }
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            when (state.tab) {
                EditorTab.CROP -> CropTools(state.aspect, viewModel::setAspect, viewModel::rotate, viewModel::flip)
                EditorTab.FILTERS -> preview?.let { FilterRow(it, state.filter, viewModel::setFilter) }
                EditorTab.ADJUST -> AdjustSliders(state.adjustments, viewModel::setAdjustments)
            }
        }
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            message = "Your edits will be lost.",
            confirmLabel = "Discard",
            onConfirm = onClose,
            onDismiss = { confirmDiscard = false }
        )
    }
}

/**
 * The photo with the current colours. On the Crop tab the whole photo shows with a draggable box;
 * on the other tabs you see just the cropped part.
 */
@Composable
private fun EditPreview(bitmap: Bitmap, state: EditState, onDrag: (CropHandle, Float, Float) -> Unit) {
    val image: ImageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val colorFilter = remember(state.filter, state.adjustments) { ColorFilter.colorMatrix(ColorMatrix(state.colorMatrix())) }
    val cropping = state.tab == EditorTab.CROP
    // Show the cropped part when not cropping: aspect of the crop box in pixels
    val shownAspect = if (cropping) bitmap.width.toFloat() / bitmap.height
    else (state.crop.width * bitmap.width) / (state.crop.height * bitmap.height)

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val boxAspect = maxWidth / maxHeight
        val (w, h) = if (shownAspect > boxAspect) maxWidth to maxWidth / shownAspect else maxHeight * shownAspect to maxHeight
        Box(Modifier.size(w, h)) {
            if (cropping) {
                Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds, colorFilter = colorFilter)
                CropOverlay(state.crop, onDrag)
            } else {
                CroppedImage(image, state.crop, colorFilter)
            }
        }
    }
}

@Composable
private fun CroppedImage(image: ImageBitmap, crop: CropRect, colorFilter: ColorFilter) {
    Canvas(Modifier.fillMaxSize()) {
        val src = IntOffset((crop.left * image.width).roundToInt(), (crop.top * image.height).roundToInt())
        val srcSize = IntSize(
            (crop.width * image.width).roundToInt().coerceIn(1, image.width - src.x),
            (crop.height * image.height).roundToInt().coerceIn(1, image.height - src.y)
        )
        drawImage(
            image, srcOffset = src, srcSize = srcSize,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            colorFilter = colorFilter
        )
    }
}

/** Dimmed outside, a frame with thirds lines, and four corner handles. Drag a corner or the middle. */
@Composable
private fun CropOverlay(crop: CropRect, onDrag: (CropHandle, Float, Float) -> Unit) {
    val handleRadius = with(LocalDensity.current) { 28.dp.toPx() }
    val currentCrop by rememberUpdatedState(crop)
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var handle = CropHandle.MOVE
                detectDragGestures(
                    onDragStart = { start ->
                        val c = currentCrop
                        val corners = listOf(
                            CropHandle.TOP_LEFT to Offset(c.left * size.width, c.top * size.height),
                            CropHandle.TOP_RIGHT to Offset(c.right * size.width, c.top * size.height),
                            CropHandle.BOTTOM_LEFT to Offset(c.left * size.width, c.bottom * size.height),
                            CropHandle.BOTTOM_RIGHT to Offset(c.right * size.width, c.bottom * size.height)
                        )
                        val nearest = corners.minBy { (_, p) -> hypot(p.x - start.x, p.y - start.y) }
                        handle = if (hypot(nearest.second.x - start.x, nearest.second.y - start.y) < handleRadius * 1.5f) nearest.first
                        else CropHandle.MOVE
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(handle, amount.x / size.width, amount.y / size.height)
                    }
                )
            }
    ) {
        val r = Rect(crop.left * size.width, crop.top * size.height, crop.right * size.width, crop.bottom * size.height)
        clipRect(r.left, r.top, r.right, r.bottom, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.6f))
        }
        drawRect(Color.White, r.topLeft, r.size, style = Stroke(2.dp.toPx()))
        for (i in 1..2) {
            val x = r.left + r.width * i / 3
            val y = r.top + r.height * i / 3
            drawLine(Color.White.copy(alpha = 0.5f), Offset(x, r.top), Offset(x, r.bottom), 1.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.5f), Offset(r.left, y), Offset(r.right, y), 1.dp.toPx())
        }
        // Round handles on the corners: drag them to resize
        listOf(r.topLeft, Offset(r.right, r.top), Offset(r.left, r.bottom), r.bottomRight).forEach { p ->
            drawCircle(Color.White, radius = 7.dp.toPx(), center = p)
        }
    }
}

@Composable
private fun CropTools(aspect: CropAspect, onAspect: (CropAspect) -> Unit, onRotate: () -> Unit, onFlip: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ToolButton(GalleryIcons.RotateRight, "Rotate", onRotate)
            ToolButton(GalleryIcons.Flip, "Mirror", onFlip)
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(CropAspect.entries) { option ->
                FilterChip(selected = option == aspect, onClick = { onAspect(option) }, label = { Text(option.label) })
            }
        }
    }
}

@Composable
private fun ToolButton(icon: Int, label: String, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        com.harish.gallery.core.designsystem.component.GalleryIcon(icon, null, tint = Color.White)
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

/** One small preview per filter, so you can see each look before choosing. */
@Composable
private fun FilterRow(preview: Bitmap, selected: PhotoFilter, onSelect: (PhotoFilter) -> Unit) {
    val small = remember(preview) {
        val scale = 160f / maxOf(preview.width, preview.height)
        Bitmap.createScaledBitmap(preview, (preview.width * scale).toInt().coerceAtLeast(1), (preview.height * scale).toInt().coerceAtLeast(1), true).asImageBitmap()
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(PhotoFilter.entries) { filter ->
            val filterColors = remember(filter) { ColorFilter.colorMatrix(ColorMatrix(filter.matrix())) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(filter) }) {
                Image(
                    small, filter.label,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .then(if (filter == selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier),
                    contentScale = ContentScale.Crop,
                    colorFilter = filterColors
                )
                Spacer(Modifier.height(4.dp))
                Text(filter.label, color = if (filter == selected) MaterialTheme.colorScheme.primary else Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun AdjustSliders(adjustments: Adjustments, onChange: (Adjustments) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        AdjustRow("Brightness", adjustments.brightness) { onChange(adjustments.copy(brightness = it)) }
        AdjustRow("Contrast", adjustments.contrast) { onChange(adjustments.copy(contrast = it)) }
        AdjustRow("Saturation", adjustments.saturation) { onChange(adjustments.copy(saturation = it)) }
        AdjustRow("Warmth", adjustments.warmth) { onChange(adjustments.copy(warmth = it)) }
    }
}

@Composable
private fun AdjustRow(label: String, value: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(84.dp))
        Slider(
            value = value,
            onValueChange = { onChange(if (abs(it) < 0.04f) 0f else it) }, // snaps to 0 near the middle
            valueRange = -1f..1f,
            modifier = Modifier.weight(1f).height(32.dp)
        )
        Text("${(value * 100).roundToInt()}", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp).offset(x = 6.dp))
    }
}
