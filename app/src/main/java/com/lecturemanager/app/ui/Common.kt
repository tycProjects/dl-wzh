package com.lecturemanager.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.lecturemanager.app.ui.theme.*

/** لون المادة المميز: في الوضع الداكن نفتّح الألوان الداكنة جداً حتى تبقى ظاهرة. */
fun adaptAccent(c: Color): Color = if (Pal.dark && c.luminance() < 0.3f) lerp(c, Color.White, 0.55f) else c

val TAGS = listOf("مهم للاختبار", "ملخص الأستاذ", "سؤال للمراجعة")

fun formatBytes(b: Long): String = when {
    b >= 1L shl 30 -> "%.2f جيجابايت".format(b / (1024.0 * 1024 * 1024))
    b >= 1L shl 20 -> "%.1f ميجابايت".format(b / (1024.0 * 1024))
    b >= 1024 -> "%.0f كيلوبايت".format(b / 1024.0)
    else -> "$b بايت"
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AppProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = Teal, height: Dp = 8.dp) {
    Box(
        modifier.fillMaxWidth().height(height).clip(CircleShape).background(Border)
    ) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(color))
    }
}

@Composable
fun Badge(text: String, bg: Color, fg: Color) {
    Text(
        text, color = fg, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun TagBadge(tag: String) {
    if (tag == "مهم للاختبار") Badge(tag, CrimsonTint, CrimsonText) else Badge(tag, AmberTint, AmberText)
}

@Composable
fun Donut(progress: Float, color: Color, size: Dp, stroke: Dp, label: String? = null) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val inset = s / 2
            val arc = Size(this.size.width - s, this.size.height - s)
            drawArc(Border, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(s))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, Offset(inset, inset), arc,
                style = Stroke(s, cap = StrokeCap.Round))
        }
        if (label != null) Text(label, style = MaterialTheme.typography.labelLarge, color = Navy)
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) { Column(Modifier.padding(16.dp), content = content) }
}

/** أزرار اختيار ملف PDF من الجهاز أو مسحه بالكاميرا. */
@Composable
fun PdfPickerButtons(onPicked: (Uri) -> Unit) {
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onPicked)
    }
    val scanner = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            GmsDocumentScanningResult.fromActivityResultIntent(r.data)?.pdf?.uri?.let(onPicked)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { picker.launch(arrayOf("application/pdf")) },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.5.dp, Navy),
        ) {
            Icon(Icons.Filled.UploadFile, null); Spacer(Modifier.width(6.dp)); Text("اختيار PDF")
        }
        OutlinedButton(
            onClick = {
                val act = ctx.findActivity()
                if (act == null) return@OutlinedButton
                val opts = GmsDocumentScannerOptions.Builder()
                    .setGalleryImportAllowed(true)
                    .setPageLimit(100)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                    .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                    .build()
                GmsDocumentScanning.getClient(opts).getStartScanIntent(act)
                    .addOnSuccessListener { scanner.launch(IntentSenderRequest.Builder(it).build()) }
                    .addOnFailureListener {
                        Toast.makeText(ctx, "المسح الضوئي غير متاح على هذا الجهاز", Toast.LENGTH_LONG).show()
                    }
            },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.5.dp, Navy),
        ) {
            Icon(Icons.Filled.DocumentScanner, null); Spacer(Modifier.width(6.dp)); Text("مسح بالكاميرا")
        }
    }
}

fun examLabel(days: Long): String = when {
    days < 0 -> "انتهى الامتحان"
    days == 0L -> "الامتحان اليوم"
    days == 1L -> "الامتحان غداً"
    days == 2L -> "الامتحان بعد يومين"
    days in 3..10 -> "الامتحان بعد $days أيام"
    else -> "الامتحان بعد $days يوم"
}

@Composable
fun ExamBadge(days: Long) {
    when {
        days < 0 -> Badge(examLabel(days), Border, Slate)
        days <= 3 -> Badge("⏰ " + examLabel(days), CrimsonTint, CrimsonText)
        days <= 14 -> Badge("📅 " + examLabel(days), AmberTint, AmberText)
        else -> Badge("📅 " + examLabel(days), TealTint, TealText)
    }
}
