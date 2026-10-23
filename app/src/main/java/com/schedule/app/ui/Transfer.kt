package com.schedule.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.schedule.app.data.Attachment
import com.schedule.app.data.Note
import com.schedule.app.data.Slot

const val LM_PACKAGE = "com.lecturemanager.app"
const val LM_ACTION = "com.lecturemanager.app.action.IMPORT_LECTURE"

fun isLectureManagerInstalled(ctx: Context) =
    runCatching { ctx.packageManager.getPackageInfo(LM_PACKAGE, 0); true }.getOrDefault(false)

/** يرسل المحاضرة إلى تطبيق مدير المحاضرات: المادة، العنوان، رقم الأسبوع، الملاحظات، وملف PDF اختياري. */
fun sendToLectureManager(ctx: Context, slot: Slot, title: String, week: Int, notes: String, pdf: Attachment?): Boolean {
    if (!isLectureManagerInstalled(ctx)) { ctx.toast("تطبيق مدير المحاضرات غير مثبّت على هذا الجهاز"); return false }
    val intent = Intent(LM_ACTION).setPackage(LM_PACKAGE)
        .putExtra("subject", slot.title).putExtra("instructor", slot.instructor)
        .putExtra("lecture_title", title).putExtra("week", week).putExtra("notes", notes)
    if (pdf != null) {
        val uri = fileUri(ctx, pdf.path)
        intent.setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = android.content.ClipData.newRawUri("pdf", uri)
        ctx.grantUriPermission(LM_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { ctx.startActivity(intent); true }.getOrElse { ctx.toast("تعذّر فتح تطبيق مدير المحاضرات. حدّثه لآخر إصدار."); false }
}

@Composable
fun TransferDialog(slot: Slot, notes: List<Note>, attachments: List<Attachment>, defaultWeek: Int = 1, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val pdfs = attachments.filter { it.mime == "application/pdf" || it.name.endsWith(".pdf", true) }
    var title by remember { mutableStateOf("محاضرة ${slot.title}") }
    var week by remember { mutableStateOf(defaultWeek.toString()) }
    var withNotes by remember { mutableStateOf(notes.isNotEmpty()) }
    var pdfId by remember { mutableStateOf<Long?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نقل إلى مدير المحاضرات") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MutedText("ستُضاف المحاضرة إلى مادة \"${slot.title}\" في تطبيق مدير المحاضرات (وتُنشأ المادة إن لم تكن موجودة).")
                AppField(title, "عنوان المحاضرة") { title = it }
                AppField(week, "رقم الأسبوع") { week = it.filter(Char::isDigit).take(2) }
                if (notes.isNotEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(withNotes, { withNotes = it }); Text("نقل ملاحظات المحاضرة (${notes.size})")
                }
                if (pdfs.isNotEmpty()) {
                    MutedText("ملف PDF للنقل")
                    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(pdfId == null, { pdfId = null }); Text("بدون ملف") }
                    pdfs.forEach { p -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(pdfId == p.id, { pdfId = p.id }); Text(p.name, maxLines = 1) } }
                } else MutedText("لا توجد ملفات PDF مرفقة. يمكن نقل الملاحظات فقط.")
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                val text = if (withNotes) notes.joinToString("\n\n") { it.text } else ""
                if (sendToLectureManager(ctx, slot, title.trim(), week.toIntOrNull()?.coerceAtLeast(1) ?: 1, text, pdfs.firstOrNull { it.id == pdfId })) onDismiss()
            }) { Text("نقل") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
}

/** عنصر في النقل الجماعي: محاضرة واحدة بملاحظاتها وملف PDF اختياري. */
data class BulkItem(val subject: String, val instructor: String, val title: String, val week: Int, val notes: String, val pdf: Attachment?)

/** يرسل عدة محاضرات دفعة واحدة إلى مدير المحاضرات (JSON + ملفات PDF في ClipData). */
fun sendBulkToLectureManager(ctx: Context, items: List<BulkItem>): Boolean {
    if (!isLectureManagerInstalled(ctx)) { ctx.toast("تطبيق مدير المحاضرات غير مثبّت على هذا الجهاز"); return false }
    if (items.isEmpty()) { ctx.toast("لا يوجد ما يُنقل"); return false }
    val pdfs = items.mapNotNull { it.pdf }.distinctBy { it.id }
    val arr = org.json.JSONArray()
    items.forEach { i ->
        arr.put(org.json.JSONObject().put("subject", i.subject).put("instructor", i.instructor).put("lecture_title", i.title)
            .put("week", i.week).put("notes", i.notes).put("pdf_index", i.pdf?.let { p -> pdfs.indexOfFirst { it.id == p.id } } ?: -1))
    }
    val intent = Intent("com.lecturemanager.app.action.IMPORT_LECTURES").setPackage(LM_PACKAGE)
        .putExtra("items_json", arr.toString()).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (pdfs.isNotEmpty()) {
        val uris = pdfs.map { fileUri(ctx, it.path) }
        val clip = android.content.ClipData.newRawUri("pdfs", uris.first())
        uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
        intent.clipData = clip
        uris.forEach { ctx.grantUriPermission(LM_PACKAGE, it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    return runCatching { ctx.startActivity(intent); true }.getOrElse { ctx.toast("تعذّر فتح تطبيق مدير المحاضرات. حدّثه لآخر إصدار."); false }
}

/** نقل جماعي: يوم كامل أو مواد مختارة. لكل ملف PDF محاضرة مستقلة، والملاحظات في محاضرة واحدة للمادة. */
@Composable
fun BulkTransferDialog(slots: List<Slot>, notes: List<Note>, attachments: List<Attachment>, defaultWeek: Int, today: Int, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val subjects = slots.map { it.title }.distinct()
    var scopeDay by remember { mutableStateOf(false) }
    var picked by remember { mutableStateOf(subjects.toSet()) }
    val todaySubjects = slots.filter { it.day == today }.map { it.title }.toSet()
    val effective = if (scopeDay) todaySubjects else picked
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نقل إلى مدير المحاضرات") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MutedText("تُنقل ملاحظات كل مادة كمحاضرة واحدة، وكل ملف PDF كمحاضرة مستقلة، في الأسبوع $defaultWeek.")
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(scopeDay, { scopeDay = it }); Text("مواد اليوم فقط (${todaySubjects.size})") }
                if (!scopeDay) subjects.forEach { n ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(n in picked, { picked = if (it) picked + n else picked - n }); Text(n, maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = effective.isNotEmpty(), onClick = {
                val items = mutableListOf<BulkItem>()
                effective.forEach { name ->
                    val ids = slots.filter { it.title == name }
                    val idSet = ids.map { it.id }.toSet()
                    val instructor = ids.firstOrNull { it.instructor.isNotBlank() }?.instructor.orEmpty()
                    val text = notes.filter { it.slotId in idSet }.joinToString("\n\n") { it.text }
                    if (text.isNotBlank()) items += BulkItem(name, instructor, "ملاحظات $name", defaultWeek, text, null)
                    attachments.filter { it.slotId in idSet && (it.mime == "application/pdf" || it.name.endsWith(".pdf", true)) }.forEach { a ->
                        items += BulkItem(name, instructor, a.name.substringBeforeLast('.'), defaultWeek, "", a)
                    }
                }
                if (items.isEmpty()) ctx.toast("لا توجد ملاحظات أو ملفات PDF في المواد المختارة")
                else if (sendBulkToLectureManager(ctx, items)) onDismiss()
            }) { Text("نقل") }
        },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
}
