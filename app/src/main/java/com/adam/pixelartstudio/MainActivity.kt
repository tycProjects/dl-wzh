package com.adam.pixelartstudio

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioScreen() }
    }
}

private enum class DrawTool { Pencil, Eraser, Fill }
private val colors = listOf(Color(0xFF171923), Color.White, Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFACC15), Color(0xFF22C55E), Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF9CA3AF), Color(0xFF8B5E3C))

@Composable
private fun StudioScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var grid by remember { mutableIntStateOf(32) }
    var bitmapPixels by remember { mutableStateOf(IntArray(grid * grid) { android.graphics.Color.TRANSPARENT }) }
    var chosen by remember { mutableStateOf(colors[2]) }
    var tool by remember { mutableStateOf(DrawTool.Pencil) }
    var title by remember { mutableStateOf("My Pixel Art") }
    var status by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<IntArray>() }

    fun saveUndo() { history.add(bitmapPixels.copyOf()); if (history.size > 25) history.removeAt(0) }
    fun changeSize(n: Int) { grid = n; bitmapPixels = IntArray(n * n) { android.graphics.Color.TRANSPARENT }; history.clear() }
    fun paint(x: Int, y: Int) {
        if (x !in 0 until grid || y !in 0 until grid) return
        val idx = y * grid + x
        val next = bitmapPixels.copyOf()
        when (tool) {
            DrawTool.Pencil -> next[idx] = chosen.toArgb()
            DrawTool.Eraser -> next[idx] = android.graphics.Color.TRANSPARENT
            DrawTool.Fill -> {
                val target = next[idx]; val replacement = chosen.toArgb(); if (target == replacement) return
                val queue = ArrayDeque<Int>(); queue.add(idx)
                while (queue.isNotEmpty()) {
                    val p = queue.removeFirst(); if (next[p] != target) continue
                    next[p] = replacement
                    val px = p % grid; val py = p / grid
                    if (px > 0) queue.add(p - 1); if (px < grid - 1) queue.add(p + 1)
                    if (py > 0) queue.add(p - grid); if (py < grid - 1) queue.add(p + grid)
                }
            }
        }
        bitmapPixels = next
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null) try {
            val bmp = Bitmap.createBitmap(grid, grid, Bitmap.Config.ARGB_8888)
            for (y in 0 until grid) for (x in 0 until grid) bmp.setPixel(x, y, bitmapPixels[y * grid + x])
            context.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            status = "تم تصدير PNG"
        } catch (e: Exception) { status = "فشل التصدير: ${e.localizedMessage}" }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF8B5CF6), secondary = Color(0xFF38BDF8), background = Color(0xFF10121A), surface = Color(0xFF191C27))) {
        Column(Modifier.fillMaxSize().background(Color(0xFF10121A)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("PIXEL ART STUDIO", color = Color(0xFFB9A0FF), fontSize = 12.sp); Text("استوديو فن البكسل", color = Color.White, fontSize = 22.sp) }
                Button(onClick = { export.launch("$title.png") }) { Text("تصدير PNG") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(16, 32, 64).forEach { n -> FilterChip(selected = grid == n, onClick = { changeSize(n) }, label = { Text("${n}×$n") }) }
                Spacer(Modifier.weight(1f))
                OutlinedButton(enabled = history.isNotEmpty(), onClick = { bitmapPixels = history.removeAt(history.lastIndex) }) { Text("تراجع") }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                DrawingBoard(grid, bitmapPixels, onStart = { saveUndo(); paint(it.first, it.second) }, onMove = { paint(it.first, it.second) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DrawTool.entries.forEach { t -> FilterChip(selected = tool == t, onClick = { tool = t }, label = { Text(when(t) { DrawTool.Pencil -> "قلم"; DrawTool.Eraser -> "ممحاة"; DrawTool.Fill -> "تعبئة" }) }) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { saveUndo(); bitmapPixels = IntArray(grid * grid) { android.graphics.Color.TRANSPARENT } }) { Text("مسح") }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { c -> Surface(Modifier.size(34.dp), shape = RoundedCornerShape(9.dp), color = c, border = androidx.compose.foundation.BorderStroke(if (c == chosen) 3.dp else 1.dp, if (c == chosen) Color.White else Color.DarkGray), onClick = { chosen = c }) {} }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("اسم التصميم") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text(status.ifBlank { "ارسم باللمس أو اسحب إصبعك على الشبكة" }, color = Color(0xFFB7B9C8), fontSize = 12.sp)
        }
    }
}

@Composable
private fun DrawingBoard(grid: Int, pixels: IntArray, onStart: (Pair<Int, Int>) -> Unit, onMove: (Pair<Int, Int>) -> Unit) {
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(4.dp)
        .pointerInput(grid) { detectTapGestures { p -> val cell = size.width.toFloat() / grid; onStart((p.x / cell).toInt() to (p.y / cell).toInt()) } }
        .pointerInput(grid) { detectDragGestures(onDragStart = { p -> val cell = size.width.toFloat() / grid; onStart((p.x / cell).toInt() to (p.y / cell).toInt()) }) { change, _ -> val cell = size.width.toFloat() / grid; onMove((change.position.x / cell).toInt() to (change.position.y / cell).toInt()) } }) {
        val cell = size.width / grid
        drawRect(Color(0xFF252938))
        for (y in 0 until grid) for (x in 0 until grid) {
            val c = pixels[y * grid + x]
            if (c != android.graphics.Color.TRANSPARENT) drawRect(Color(c), Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell, cell))
            drawRect(Color(0xFF62687A).copy(alpha = 0.35f), Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell, cell), style = androidx.compose.ui.graphics.drawscope.Stroke(0.5f))
        }
    }
}
