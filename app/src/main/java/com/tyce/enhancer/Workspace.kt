package com.tyce.enhancer

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/** Rounded, softly shadowed card used across the redesigned screens. */
@Composable
fun SoftCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .fillMaxWidth()
            .shadow(if (Appearance.palette.dark) 0.dp else 6.dp, shape, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(shape)
            .background(C.Card)
            .then(if (Appearance.palette.dark) Modifier.border(1.dp, C.Line, shape) else Modifier)
            .padding(20.dp)
    ) { content() }
}

@Composable
fun ChipRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .height(52.dp)
                    .pressable(0.96f) { onSelect(i) }
                    .clip(shape)
                    .background(if (on) C.Accent.copy(alpha = 0.16f) else C.Bg)
                    .border(1.5.dp, if (on) C.Accent else C.Line, shape)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) { Text(label, style = inter(16f, FontWeight.SemiBold, if (on) C.Accent else C.Muted)) }
        }
    }
}

@Composable
fun GradientButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(if (enabled) 8.dp else 0.dp, shape, ambientColor = C.Accent.copy(alpha = 0.4f), spotColor = C.Accent.copy(alpha = 0.4f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(C.Accent, C.Accent.copy(alpha = 0.82f))))
            .pressable(0.98f, enabled, onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, style = inter(19f, FontWeight.Bold, Color.White.copy(alpha = if (enabled) 1f else 0.5f))) }
}

private fun decodePreview(context: android.content.Context, uri: Uri, side: Int): Bitmap? = try {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, info, _ ->
        d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val l = max(info.size.width, info.size.height)
        if (l > side) {
            val f = side.toFloat() / l
            d.setTargetSize((info.size.width * f).roundToInt().coerceAtLeast(1), (info.size.height * f).roundToInt().coerceAtLeast(1))
        }
    }
} catch (_: Throwable) { null }

/** Imagine-style workspace: photo preview, model + output cards, Enhance / New Image. */
@Composable
fun WorkspaceScreen(
    uri: Uri,
    maxSide: Int,
    onMaxSide: (Int) -> Unit,
    engines: List<EngineModel>,
    engineId: String,
    onEngine: (String) -> Unit,
    outMode: Int,
    customPx: Int,
    quickScale: Int,
    onOutMode: (Int) -> Unit,
    onCustomPx: (Int) -> Unit,
    useCloud: Boolean,
    onEnhance: () -> Unit,
    onNewImage: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preview by produceState<Bitmap?>(null, uri) { value = withContext(Dispatchers.IO) { decodePreview(context, uri, 1280) } }
    var dropOpen by remember { mutableStateOf(false) }
    var customText by remember(customPx) { mutableStateOf(customPx.toString()) }
    val current = engines.firstOrNull { it.id == engineId } ?: engines.firstOrNull()
    val customOk = outMode != 2 || (customText.toIntOrNull() ?: 0) in 256..8192
    val canRun = (useCloud || current != null) && customOk
    BackHandler(true) { onBack() }

    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.height(44.dp).width(44.dp).pressable(0.92f, onClick = onBack).clip(CircleShape).background(C.Card),
                contentAlignment = Alignment.Center
            ) { Text("‹", style = inter(26f, FontWeight.Medium, C.Text)) }
            Text("Enhance", style = heading(20f, FontWeight.Bold, C.Text), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(44.dp))
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            val shape = RoundedCornerShape(26.dp)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 340.dp).clip(shape).background(C.Card),
                contentAlignment = Alignment.Center
            ) {
                val b = preview
                if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else Text("Loading…", style = inter(14f, FontWeight.Normal, C.Muted))
            }
            Spacer(Modifier.height(14.dp))
            SoftCard {
                Column {
                    Text("Model", style = inter(13f, FontWeight.SemiBold, C.Muted))
                    Spacer(Modifier.height(10.dp))
                    val dshape = RoundedCornerShape(18.dp)
                    Box(
                        Modifier.fillMaxWidth().clip(dshape).border(1.5.dp, C.Line, dshape)
                            .pressable(0.99f) { dropOpen = !dropOpen }.padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Column(Modifier.padding(end = 28.dp)) {
                            Text(current?.label ?: "No model available", style = inter(18f, FontWeight.Medium, C.Text))
                            if (current != null) Text(current.desc, style = inter(12f, FontWeight.Normal, C.Muted))
                        }
                        Text(if (dropOpen) "▴" else "▾", style = inter(16f, FontWeight.Bold, C.Accent), modifier = Modifier.align(Alignment.CenterEnd))
                    }
                    if (dropOpen) {
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(dshape).background(C.Bg)) {
                            engines.forEach { m ->
                                val sel = m.id == current?.id
                                Row(
                                    Modifier.fillMaxWidth().pressable(0.99f) { onEngine(m.id); dropOpen = false }.padding(horizontal = 18.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(m.label, style = inter(16f, if (sel) FontWeight.SemiBold else FontWeight.Normal, C.Text))
                                        Text(m.desc, style = inter(12f, FontWeight.Normal, C.Muted))
                                    }
                                    if (sel) Text("✓", style = inter(18f, FontWeight.Bold, C.Accent))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Photo detail", style = inter(13f, FontWeight.SemiBold, C.Muted))
                    Spacer(Modifier.height(10.dp))
                    Segmented(listOf("Fast · 512 px", "Detailed · 800 px"), if (maxSide == 512) 0 else 1) { onMaxSide(if (it == 0) 512 else 800) }
                }
            }
            Spacer(Modifier.height(14.dp))
            SoftCard {
                Column {
                    Text("Output size", style = inter(13f, FontWeight.SemiBold, C.Muted))
                    Spacer(Modifier.height(12.dp))
                    ChipRow(listOf("${quickScale}×", "Original", "Custom"), outMode, onOutMode)
                    if (outMode == 2) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customText,
                            onValueChange = { t ->
                                customText = t.filter { it.isDigit() }.take(5)
                                customText.toIntOrNull()?.let { if (it in 256..8192) onCustomPx(it) }
                            },
                            label = { Text("Long side in pixels (256–8192)") },
                            singleLine = true,
                            isError = !customOk,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Output is never larger than the model really produces.", style = inter(12f, FontWeight.Normal, C.Muted))
                }
            }
            if (useCloud) {
                Spacer(Modifier.height(14.dp))
                Text("Cloud mode is on: this photo is uploaded to Replicate and your model choice above is not used.",
                    style = inter(12f, FontWeight.Medium, C.Red, 1.35f))
            }
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientButton("Enhance", canRun, onEnhance)
            OutlineButton("New Image", Modifier.fillMaxWidth(), onClick = onNewImage)
        }
    }
}

/** Asks for the upscale factor and name of a freshly picked ncnn .param + .bin pair. */
@Composable
fun NcnnImportDialog(defaultName: String, busy: Boolean, error: String?, onConfirm: (Int, String) -> Unit, onCancel: () -> Unit) {
    var scale by remember { mutableStateOf(2) }
    var name by remember(defaultName) { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        containerColor = C.Card,
        title = { Text("Import ncnn model", style = heading(18f, FontWeight.Bold, C.Text)) },
        text = {
            Column {
                Text("Both files were found. Choose how much this model upscales (it must match the model, or the import is rejected).",
                    style = inter(13f, FontWeight.Normal, C.Muted, 1.35f))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                ChipRow(listOf("2×", "3×", "4×"), scale - 2) { scale = it + 2 }
                if (busy) { Spacer(Modifier.height(12.dp)); Text("Testing the model…", style = inter(13f, FontWeight.Medium, C.Accent)) }
                if (error != null) { Spacer(Modifier.height(12.dp)); Text(error, style = inter(13f, FontWeight.Medium, C.Red, 1.3f)) }
            }
        },
        confirmButton = { TextButton(onClick = { if (!busy) onConfirm(scale, name) }) { Text("Import", color = C.Accent) } },
        dismissButton = { TextButton(onClick = { if (!busy) onCancel() }) { Text("Cancel", color = C.Muted) } }
    )
}
