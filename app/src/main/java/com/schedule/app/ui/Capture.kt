package com.schedule.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.schedule.app.MainViewModel
import com.schedule.app.data.Note
import com.schedule.app.ui.theme.*
import java.io.File

private fun decode(path: String, maxSide: Int = 2000): Bitmap? {
    val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, b)
    var sample = 1
    while (b.outWidth / sample > maxSide || b.outHeight / sample > maxSide) sample *= 2
    val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    // تدوير حسب EXIF
    val deg = runCatching { when (androidx.exifinterface.media.ExifInterface(path).getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 1)) { 6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f } }.getOrDefault(0f)
    if (deg == 0f) return bmp
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, android.graphics.Matrix().apply { postRotate(deg) }, true)
}

/** تحسين مستند/سبورة: تدرج رمادي مع رفع التباين. */
fun enhanceDocument(src: Bitmap, contrast: Float = 1.8f): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val t = (-0.5f * contrast + 0.5f) * 255f
    val cm = ColorMatrix().apply { setSaturation(0f); postConcat(ColorMatrix(floatArrayOf(contrast, 0f, 0f, 0f, t, 0f, contrast, 0f, 0f, t, 0f, 0f, contrast, 0f, t, 0f, 0f, 0f, 1f, 0f))) }
    AndroidCanvas(out).drawBitmap(src, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
    return out
}

private fun saveJpeg(ctx: android.content.Context, bmp: Bitmap, name: String): File =
    File(File(ctx.cacheDir, "shared").also { it.mkdirs() }, name).also { f -> f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) } }

/** مسح مستند أو سبورة: تصوير، تحسين، حفظ في مرفقات المادة، واستخراج النص (إنجليزي وأرقام فقط). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val slots by vm.slots.collectAsStateWithLifecycle()
    var subject by rememberSaveable { mutableLongStateOf(0L) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var ocr by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val pendingFile = remember { mutableStateOf<File?>(null) }

    LaunchedEffect(photoPath) { bmp = photoPath?.let { decode(it) } }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) pendingFile.value?.let { photoPath = it.absolutePath; ocr = "" } }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val f = File(File(ctx.cacheDir, "shared").also { d -> d.mkdirs() }, "pick_${System.currentTimeMillis()}.jpg")
            runCatching { ctx.contentResolver.openInputStream(it)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }; photoPath = f.absolutePath; ocr = "" }
        }
    }

    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("مسح مستند") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MutedText("المادة التي تُحفظ فيها النتيجة")
            SubjectChips(slots, subject) { subject = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({
                    val f = File(File(ctx.cacheDir, "shared").also { it.mkdirs() }, "scan_${System.currentTimeMillis()}.jpg"); pendingFile.value = f
                    camera.launch(fileUri(ctx, f.absolutePath))
                }, colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("التقاط صورة") }
                OutlinedButton({ gallery.launch("image/*") }) { Text("من المعرض") }
            }
            bmp?.let { b ->
                Image(b.asImageBitmap(), null, Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({
                        photoPath?.let { p -> vm.addGeneratedFile(subject, File(p), "مسح-${System.currentTimeMillis() / 1000}.jpg", "image/jpeg", 3) }
                    }, enabled = subject > 0) { Text("حفظ الأصل") }
                    OutlinedButton({
                        val f = saveJpeg(ctx, enhanceDocument(b), "مستند-${System.currentTimeMillis() / 1000}.jpg")
                        vm.addGeneratedFile(subject, f, f.name, "image/jpeg", 3)
                    }, enabled = subject > 0) { Text("تحسين وحفظ") }
                }
                OutlinedButton({
                    busy = true
                    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromBitmap(b, 0))
                        .addOnSuccessListener { ocr = it.text.ifBlank { "لم يُعثر على نص." }; busy = false }
                        .addOnFailureListener { ocr = "تعذّر استخراج النص."; busy = false }
                }, enabled = !busy) { Text(if (busy) "جارٍ الاستخراج..." else "استخراج النص (إنجليزي وأرقام)") }
                MutedText("استخراج النص بنموذج ML Kit المضمّن يدعم الحروف اللاتينية والأرقام فقط، ولا يقرأ العربية.")
            }
            if (ocr.isNotEmpty()) {
                AppField(ocr, "النص المستخرج", singleLine = false) { ocr = it }
                OutlinedButton({ vm.saveNote(Note(slotId = subject, text = ocr.trim())); ctx.toast("أُضيف كملاحظة") }, enabled = subject > 0 && ocr.isNotBlank()) { Text("حفظ كملاحظة للمادة") }
            }
        }
    }
}

private data class DrawStroke(val pts: List<Offset>, val color: Color, val width: Float)

/** لوحة رسم بخط اليد تُحفظ كصورة PNG في مرفقات المادة. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val slots by vm.slots.collectAsStateWithLifecycle()
    var subject by rememberSaveable { mutableLongStateOf(0L) }
    var strokes by remember { mutableStateOf(listOf<DrawStroke>()) }
    var cur by remember { mutableStateOf(listOf<Offset>()) }
    var color by remember { mutableStateOf(Color.Black) }
    var width by remember { mutableFloatStateOf(6f) }
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(1, 1)) }
    val colors = listOf(Color.Black, Color(0xFFB91C3C), Color(0xFF2563EB), Color(0xFF16A34A), Color(0xFFD97706))
    val colorState by rememberUpdatedState(color); val widthState by rememberUpdatedState(width)

    fun path(pts: List<Offset>) = Path().apply { pts.firstOrNull()?.let { moveTo(it.x, it.y) }; pts.drop(1).forEach { lineTo(it.x, it.y) } }

    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("رسم وكتابة") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SubjectChips(slots, subject) { subject = it }
            Canvas(
                Modifier.fillMaxWidth().height(420.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).background(Color.White).onSizeChanged { size = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { cur = listOf(it) },
                            onDrag = { ch, _ -> cur = cur + ch.position; ch.consume() },
                            onDragEnd = { if (cur.size > 1) strokes = strokes + DrawStroke(cur, colorState, widthState); cur = emptyList() },
                            onDragCancel = { cur = emptyList() },
                        )
                    },
            ) {
                strokes.forEach { drawPath(path(it.pts), it.color, style = Stroke(it.width, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                if (cur.size > 1) drawPath(path(cur), color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                colors.forEach { c -> Box(Modifier.size(30.dp).clip(CircleShape).background(c).border(if (c == color) 3.dp else 1.dp, if (c == color) Crimson else Border, CircleShape).clickable { color = c }) }
                Slider(width, { width = it }, valueRange = 2f..20f, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ strokes = strokes.dropLast(1) }, enabled = strokes.isNotEmpty()) { Text("تراجع") }
                OutlinedButton({ strokes = emptyList() }, enabled = strokes.isNotEmpty()) { Text("مسح الكل") }
                Button({
                    val bmp = Bitmap.createBitmap(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    val c = AndroidCanvas(bmp); c.drawColor(android.graphics.Color.WHITE)
                    strokes.forEach { s ->
                        val p = android.graphics.Path().apply { s.pts.firstOrNull()?.let { moveTo(it.x, it.y) }; s.pts.drop(1).forEach { lineTo(it.x, it.y) } }
                        c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.STROKE; strokeWidth = s.width; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                            this.color = android.graphics.Color.argb(255, (s.color.red * 255).toInt(), (s.color.green * 255).toInt(), (s.color.blue * 255).toInt())
                        })
                    }
                    val f = File(File(ctx.cacheDir, "shared").also { it.mkdirs() }, "رسم-${System.currentTimeMillis() / 1000}.png")
                    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    vm.addGeneratedFile(subject, f, f.name, "image/png", 2)
                }, enabled = subject > 0 && strokes.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("حفظ في المادة") }
            }
            if (subject == 0L) MutedText("اختر المادة أولا لتفعيل الحفظ.")
        }
    }
}
