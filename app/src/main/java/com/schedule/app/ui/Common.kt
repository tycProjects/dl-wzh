package com.schedule.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.schedule.app.data.Attachment
import com.schedule.app.ui.theme.*
import java.io.File
import java.time.LocalDate

fun Context.toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

fun pickTime(ctx: Context, minute: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPick(h * 60 + m) }, minute / 60, minute % 60, false).show()
}

fun pickDate(ctx: Context, epochDay: Long?, onPick: (Long) -> Unit) {
    val d = epochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
    DatePickerDialog(ctx, { _, y, m, day -> onPick(LocalDate.of(y, m + 1, day).toEpochDay()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
}

fun formatDate(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).let { "%04d/%02d/%02d".format(it.year, it.monthValue, it.dayOfMonth) }

fun formatSize(b: Long): String = when {
    b >= 1 shl 20 -> "%.1f م.ب".format(b / 1048576.0)
    b >= 1 shl 10 -> "%d ك.ب".format(b shr 10)
    else -> "$b بايت"
}

fun fileUri(ctx: Context, path: String): Uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", File(path))

fun Context.openAttachment(a: Attachment) {
    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(fileUri(this, a.path), a.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { startActivity(Intent.createChooser(intent, "فتح بواسطة").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
        .onFailure { toast("لا يوجد تطبيق يفتح هذا النوع من الملفات") }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Surface1),
        border = BorderStroke(1.dp, Border)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content) }
}

@Composable
fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, color = TextMain)

@Composable
fun MutedText(text: String, modifier: Modifier = Modifier) = Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = com.schedule.app.ui.theme.Muted)

@Composable
fun AppField(value: String, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = singleLine, minLines = if (singleLine) 1 else 3,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Crimson, unfocusedBorderColor = Border, focusedLabelColor = Crimson,
            focusedContainerColor = Surface2, unfocusedContainerColor = Surface2))
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "حذف", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

/** مشاركة مرفق مع زميل عبر أي تطبيق (واتساب وغيره). */
fun Context.shareAttachment(a: Attachment) {
    val i = Intent(Intent.ACTION_SEND).setType(a.mime).putExtra(Intent.EXTRA_STREAM, fileUri(this, a.path)).putExtra(Intent.EXTRA_SUBJECT, a.name).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { startActivity(Intent.createChooser(i, "مشاركة الملف").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }.onFailure { toast("تعذّرت المشاركة") }
}
