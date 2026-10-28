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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    var type by remember { mutableIntStateOf(initial?.type ?: 0) }
    var bring by remember { mutableStateOf(initial?.bring.orEmpty()) }
    var sound by remember { mutableStateOf(initial?.sound.orEmpty()) }
    var linkedId by remember { mutableLongStateOf(initial?.linkedId ?: 0L) }
    var dates by remember { mutableStateOf(initial?.dateSet()?.sorted() ?: emptyList()) }
    var linkMenu by remember { mutableStateOf(false) }
    val ringtone = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { r ->
        @Suppress("DEPRECATION")
        val u = r.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (r.resultCode == android.app.Activity.RESULT_OK) sound = u?.toString().orEmpty()
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val valid = title.isNotBlank() && isValidRange(start, end) && (weekMode != 5 || dates.isNotEmpty())
    val clash = all.filter { it.id != (initial?.id ?: -1) && conflicts(day, weekMode, start, end, it.day, it.weekMode, it.startMin, it.endMin) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة محاضرة" else "تعديل المحاضرة") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(title, "اسم المادة") { title = it }
                AppField(instructor, "المحاضر") { instructor = it }
                AppField(room, "القاعة") { room = it }
                MutedText("النوع")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SLOT_TYPE_LABELS.forEachIndexed { i, n -> FilterChip(type == i, { type = i }, { Text(n) }) }
                }
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
                if (weekMode == 5) {
                    OutlinedButton({ pickDate(ctx, null) { d -> if (d !in dates) { dates = (dates + d).sorted(); day = dayIndex(java.time.LocalDate.ofEpochDay(d)) } } }) { Text("إضافة تاريخ") }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dates.forEach { d -> InputChip(true, { dates = dates - d }, { Text(formatDate(d)) }) }
                    }
                    if (dates.isEmpty()) Text("أضف تاريخا واحدا على الأقل", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                AppField(bring, "ما يجب إحضاره (اختياري)") { bring = it }
                val others = all.filter { it.id != (initial?.id ?: -1) }
                if (others.isNotEmpty()) {
                    Box {
                        OutlinedButton({ linkMenu = true }) { Text("المرتبطة: " + (others.firstOrNull { it.id == linkedId }?.let { "${it.title} (${SLOT_TYPE_LABELS[it.type.coerceIn(0, 2)]})" } ?: "لا شيء")) }
                        DropdownMenu(linkMenu, { linkMenu = false }) {
                            DropdownMenuItem({ Text("لا شيء") }, { linkedId = 0; linkMenu = false })
                            others.forEach { o -> DropdownMenuItem({ Text("${o.title} - ${DAY_NAMES[o.day]} (${SLOT_TYPE_LABELS[o.type.coerceIn(0, 2)]})") }, { linkedId = o.id; linkMenu = false }) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton({
                        ringtone.launch(android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_NOTIFICATION)
                            .putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, sound.takeIf { it.isNotBlank() }?.let(android.net.Uri::parse)))
                    }) { Text(if (sound.isBlank()) "نغمة خاصة بالمادة" else "تغيير النغمة") }
                    if (sound.isNotBlank()) TextButton({ sound = "" }) { Text("افتراضية") }
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
            onSave(Slot(id = initial?.id ?: 0, title = title.trim(), instructor = instructor.trim(), room = room.trim(), day = day, startMin = start, endMin = end, color = color, weekMode = weekMode,
                type = type, linkedId = linkedId, bring = bring.trim(), sound = sound, dates = if (weekMode == 5) dates.joinToString(",") else "", scheduleId = initial?.scheduleId ?: 1L)); onDismiss()
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteDialog(initial: Note?, slotId: Long, day: Int, onSave: (Note) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var tf by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(initial?.text.orEmpty())) }
    var tags by remember { mutableStateOf(initial?.tags.orEmpty()) }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }
    var date by remember { mutableStateOf(initial?.dateEpoch) }
    val voice = rememberVoiceInput { heard -> tf = tf.copy(text = (tf.text + " " + heard).trim(), selection = androidx.compose.ui.text.TextRange((tf.text + " " + heard).trim().length)) }
    /** يحيط التحديد بعلامات التنسيق أو يضيف بادئة سطر. */
    fun wrap(before: String, after: String = "") {
        val sel = tf.selection; val t = tf.text
        val a = minOf(sel.start, sel.end); val b = maxOf(sel.start, sel.end)
        tf = if (after.isEmpty()) {
            val lineStart = t.lastIndexOf('\n', (a - 1).coerceAtLeast(0)).let { if (it < 0 || a == 0) 0 else it + 1 }
            androidx.compose.ui.text.input.TextFieldValue(t.substring(0, lineStart) + before + t.substring(lineStart), androidx.compose.ui.text.TextRange(a + before.length))
        } else androidx.compose.ui.text.input.TextFieldValue(t.substring(0, a) + before + t.substring(a, b) + after + t.substring(b), androidx.compose.ui.text.TextRange(b + before.length + after.length))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "ملاحظة جديدة" else "تعديل الملاحظة") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip({ wrap("# ") }, { Text("عنوان") })
                    AssistChip({ wrap("**", "**") }, { Text("عريض") })
                    AssistChip({ wrap("- ") }, { Text("نقطة") })
                    AssistChip({ wrap("- [ ] ") }, { Text("مهمة") })
                    AssistChip({ voice() }, { Text("إملاء صوتي") })
                }
                OutlinedTextField(tf, { tf = it }, Modifier.fillMaxWidth(), label = { Text("نص الملاحظة") }, minLines = 3,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Crimson, unfocusedBorderColor = com.schedule.app.ui.theme.Border, focusedLabelColor = Crimson,
                        focusedContainerColor = com.schedule.app.ui.theme.Surface2, unfocusedContainerColor = com.schedule.app.ui.theme.Surface2))
                AppField(tags, "الوسوم (مفصولة بفاصلة)") { tags = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تثبيت الملاحظة في الأعلى", Modifier.weight(1f)); Switch(pinned, { pinned = it }, colors = SwitchDefaults.colors(checkedTrackColor = Crimson))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickDate(ctx, date) { date = it } }) { Text(date?.let { "التاريخ: ${formatDate(it)}" } ?: "تحديد تاريخ للتذكير") }
                    if (date != null) TextButton({ date = null }) { Text("إزالة") }
                }
                if (date != null) MutedText("سيصلك تذكير قبل هذا التاريخ بيوم، الساعة 9 صباحا.")
                if (onDelete != null) TextButton({ onDelete(); onDismiss() }) { Text("حذف الملاحظة", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = tf.text.isNotBlank(), onClick = {
            onSave(Note(id = initial?.id ?: 0, slotId = slotId, day = if (slotId > 0) -1 else day, text = tf.text.trim(), dateEpoch = date, createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                tags = parseTags(tags).joinToString(","), pinned = pinned)); onDismiss()
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
