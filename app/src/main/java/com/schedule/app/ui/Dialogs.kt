package com.schedule.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.schedule.app.data.*
import com.schedule.app.ui.theme.Crimson
import com.schedule.app.ui.theme.SLOT_COLORS
import com.schedule.app.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SlotEditDialog(initial: Slot?, defaultDay: Int, all: List<Slot>, onSave: (Slot) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var instructor by remember { mutableStateOf(initial?.instructor.orEmpty()) }
    var room by remember { mutableStateOf(initial?.room.orEmpty()) }
    var day by remember { mutableIntStateOf(initial?.day ?: defaultDay) }
    var start by remember { mutableIntStateOf(initial?.startMin ?: 9 * 60) }
    var end by remember { mutableIntStateOf(initial?.endMin ?: 10 * 60 + 30) }
    var color by remember { mutableIntStateOf(initial?.color ?: 0) }
    var weekMode by remember { mutableIntStateOf(initial?.weekMode ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }
    val valid = title.isNotBlank() && isValidRange(start, end)
    val clash = all.filter { it.id != (initial?.id ?: -1) && conflicts(day, weekMode, start, end, it.day, it.weekMode, it.startMin, it.endMin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة محاضرة" else "تعديل المحاضرة") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(title, "اسم المادة") { title = it }
                AppField(instructor, "المحاضر") { instructor = it }
                AppField(room, "القاعة") { room = it }
                MutedText("اليوم")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_NAMES.forEachIndexed { i, n -> FilterChip(day == i, { day = i }, { Text(n) }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickTime(ctx, start) { start = it } }, Modifier.weight(1f)) { Text("من ${formatMinutes(start)}") }
                    OutlinedButton({ pickTime(ctx, end) { end = it } }, Modifier.weight(1f)) { Text("إلى ${formatMinutes(end)}") }
                }
                if (!isValidRange(start, end)) Text("وقت النهاية يجب أن يكون بعد البداية", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (clash.isNotEmpty()) Text("تنبيه: تتعارض مع ${clash.joinToString(" و ") { it.title }} في نفس الوقت.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                MutedText("تتكرر")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEK_MODE_LABELS.forEachIndexed { i, l -> FilterChip(weekMode == i, { weekMode = i }, { Text(l) }) }
                }
                MutedText("اللون")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SLOT_COLORS.forEachIndexed { i, c ->
                        Box(Modifier.size(30.dp).clip(CircleShape).background(c).clickable { color = i }, contentAlignment = Alignment.Center) {
                            if (color == i) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (onDelete != null) TextButton({ confirmDelete = true }) { Text("حذف المحاضرة", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = {
            onSave(Slot(id = initial?.id ?: 0, title = title.trim(), instructor = instructor.trim(), room = room.trim(), day = day, startMin = start, endMin = end, color = color, weekMode = weekMode)); onDismiss()
        }) { Text(if (clash.isNotEmpty()) "حفظ رغم التعارض" else "حفظ") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
    if (confirmDelete) ConfirmDialog("حذف المحاضرة", "سيتم حذف المحاضرة مع ملاحظاتها ومرفقاتها وحضورها وواجباتها.", onConfirm = { onDelete?.invoke(); onDismiss() }, onDismiss = { confirmDelete = false })
}

/** تعديل لمرة واحدة: إلغاء موعد بتاريخ معين أو تغيير وقته/قاعته. */
@Composable
fun ExceptionDialog(slot: Slot, onSave: (SlotException) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val today = java.time.LocalDate.now()
    var date by remember { mutableStateOf(nextDateFor(slot.day, today).toEpochDay()) }
    var cancel by remember { mutableStateOf(true) }
    var start by remember { mutableIntStateOf(slot.startMin) }
    var end by remember { mutableIntStateOf(slot.endMin) }
    var room by remember { mutableStateOf(slot.room) }
    val dayOk = dayIndex(java.time.LocalDate.ofEpochDay(date)) == slot.day
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل لمرة واحدة") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton({ pickDate(ctx, date) { date = it } }) { Text("التاريخ: ${formatDate(date)}") }
                if (!dayOk) Text("هذا التاريخ ليس يوم ${DAY_NAMES[slot.day]}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(cancel, { cancel = true }); Text("إلغاء المحاضرة في هذا اليوم") }
                Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(!cancel, { cancel = false }); Text("تغيير الوقت أو القاعة لهذا اليوم فقط") }
                if (!cancel) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ pickTime(ctx, start) { start = it } }, Modifier.weight(1f)) { Text("من ${formatMinutes(start)}") }
                        OutlinedButton({ pickTime(ctx, end) { end = it } }, Modifier.weight(1f)) { Text("إلى ${formatMinutes(end)}") }
                    }
                    AppField(room, "القاعة") { room = it }
                    if (!isValidRange(start, end)) Text("وقت النهاية يجب أن يكون بعد البداية", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(enabled = dayOk && (cancel || isValidRange(start, end)), onClick = {
            onSave(SlotException(slotId = slot.id, dateEpoch = date, cancelled = cancel, newStart = if (cancel) null else start, newEnd = if (cancel) null else end, newRoom = if (cancel) null else room.trim().ifEmpty { null })); onDismiss()
        }) { Text("حفظ") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
}

fun nextDateFor(day: Int, from: java.time.LocalDate): java.time.LocalDate {
    var d = from
    while (dayIndex(d) != day) d = d.plusDays(1)
    return d
}

@Composable
fun TaskDialog(initial: Task?, slotId: Long, onSave: (Task) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var due by remember { mutableStateOf(initial?.dueEpoch) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "واجب جديد" else "تعديل الواجب") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(title, "الواجب", singleLine = false) { title = it }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickDate(ctx, due) { due = it } }) { Text(due?.let { "موعد التسليم: ${formatDate(it)}" } ?: "تحديد موعد التسليم") }
                    if (due != null) TextButton({ due = null }) { Text("إزالة") }
                }
                if (due != null) MutedText("سيصلك تذكير قبل الموعد بيوم، الساعة 9 صباحا.")
                if (onDelete != null) TextButton({ onDelete(); onDismiss() }) { Text("حذف الواجب", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = {
            onSave(Task(id = initial?.id ?: 0, slotId = slotId, title = title.trim(), dueEpoch = due, done = initial?.done ?: false, createdAt = initial?.createdAt ?: System.currentTimeMillis())); onDismiss()
        }) { Text("حفظ") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
}

@Composable
fun NoteDialog(initial: Note?, slotId: Long, day: Int, onSave: (Note) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    var date by remember { mutableStateOf(initial?.dateEpoch) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "ملاحظة جديدة" else "تعديل الملاحظة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(text, "نص الملاحظة", singleLine = false) { text = it }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickDate(ctx, date) { date = it } }) { Text(date?.let { "التاريخ: ${formatDate(it)}" } ?: "تحديد تاريخ للتذكير") }
                    if (date != null) TextButton({ date = null }) { Text("إزالة") }
                }
                if (date != null) MutedText("سيصلك تذكير قبل هذا التاريخ بيوم، الساعة 9 صباحا.")
                if (onDelete != null) TextButton({ onDelete(); onDismiss() }) { Text("حذف الملاحظة", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = text.isNotBlank(), onClick = {
            onSave(Note(id = initial?.id ?: 0, slotId = slotId, day = if (slotId > 0) -1 else day, text = text.trim(), dateEpoch = date, createdAt = initial?.createdAt ?: System.currentTimeMillis())); onDismiss()
        }) { Text("حفظ") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
}

@Composable
fun ExamDialog(initial: Exam?, onSave: (Exam) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember { mutableStateOf(initial?.dateEpoch ?: java.time.LocalDate.now().plusDays(14).toEpochDay()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة امتحان" else "تعديل الامتحان") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(title, "عنوان الامتحان (مثال: ميد تيرم الرياضيات)") { title = it }
                OutlinedButton({ pickDate(ctx, date) { date = it } }) { Text("التاريخ: ${formatDate(date)}") }
                if (onDelete != null) TextButton({ onDelete(); onDismiss() }) { Text("حذف الامتحان", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(Exam(id = initial?.id ?: 0, title = title.trim(), dateEpoch = date)); onDismiss() }) { Text("حفظ") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
}
