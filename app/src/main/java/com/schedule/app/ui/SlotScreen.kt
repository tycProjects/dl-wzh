package com.schedule.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.data.Attachment
import com.schedule.app.data.Note
import com.schedule.app.data.Task
import com.schedule.app.data.SlotException
import com.schedule.app.ui.theme.*
import com.schedule.app.util.*
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlotScreen(vm: MainViewModel, slotId: Long, onBack: () -> Unit, onRecord: (Long) -> Unit) {
    val ctx = LocalContext.current
    val slots by vm.slots.collectAsStateWithLifecycle()
    val allNotes by vm.notes.collectAsStateWithLifecycle()
    val allFiles by vm.attachments.collectAsStateWithLifecycle()
    val slot = slots.firstOrNull { it.id == slotId }
    val notes = allNotes.filter { it.slotId == slotId }
    val files = allFiles.filter { it.slotId == slotId }
    val settings by vm.settings.collectAsStateWithLifecycle()
    val allAttendance by vm.attendance.collectAsStateWithLifecycle()
    val allTasks by vm.tasks.collectAsStateWithLifecycle()
    val allExc by vm.exceptions.collectAsStateWithLifecycle()
    val sem = java.time.LocalDate.ofEpochDay(settings.semesterStart)
    val tasks = allTasks.filter { it.slotId == slotId }
    var taskDialog by remember { mutableStateOf<Task?>(null) }
    var addingTask by remember { mutableStateOf(false) }
    var excDialog by remember { mutableStateOf(false) }
    var noteDialog by remember { mutableStateOf<Note?>(null) }
    var addingNote by remember { mutableStateOf(false) }
    var transfer by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<Attachment?>(null) }
    var menuFor by remember { mutableStateOf<Long?>(null) }
    var confirmDelete by remember { mutableStateOf<Attachment?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.addAttachment(slotId, it) } }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val a = pendingSave; pendingSave = null
        if (uri != null && a != null) vm.saveAttachmentTo(a, uri)
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(title = { Text(slot?.title ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
        },
    ) { pad ->
        if (slot == null) { Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { MutedText("المحاضرة غير موجودة") }; return@Scaffold }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard {
                    Text(slot.title, style = MaterialTheme.typography.titleLarge)
                    Text("${DAY_NAMES[slot.day]}  |  ${formatRange(slot.startMin, slot.endMin)}", color = Crimson, style = MaterialTheme.typography.bodyMedium)
                    if (slot.instructor.isNotBlank()) MutedText("المحاضر: ${slot.instructor}")
                    if (slot.room.isNotBlank()) MutedText("القاعة: ${slot.room}")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button({ onRecord(slotId) }, Modifier.weight(1f).heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = Crimson)) {
                        Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(6.dp)); Text("تسجيل المحاضرة")
                    }
                    OutlinedButton({ transfer = true }, Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.Send, null); Spacer(Modifier.width(6.dp)); Text("نقل إلى مدير المحاضرات", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            item { AttendanceCard(vm, slot, allSlots = slots, attendance = allAttendance, limit = settings.absenceLimit, sem = sem) }
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { SectionTitle("الواجبات") }
                        TextButton({ addingTask = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إضافة") }
                    }
                    if (tasks.isEmpty()) MutedText("لا توجد واجبات لهذه المادة.")
                    tasks.forEach { t ->
                        Row(Modifier.fillMaxWidth().clickable { taskDialog = t }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(t.done, { vm.toggleTask(t) })
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.bodyMedium, textDecoration = if (t.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                    color = if (t.done) Muted else TextMain)
                                t.dueEpoch?.let { Text("التسليم: ${formatDate(it)}", style = MaterialTheme.typography.labelSmall, color = if (!t.done && it < java.time.LocalDate.now().toEpochDay()) MaterialTheme.colorScheme.error else Crimson) }
                            }
                        }
                    }
                }
            }
            item {
                val upcoming = allExc.filter { it.slotId == slotId && it.dateEpoch >= java.time.LocalDate.now().toEpochDay() }.sortedBy { it.dateEpoch }
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { SectionTitle("تعديلات لمرة واحدة") }
                        TextButton({ excDialog = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إضافة") }
                    }
                    if (upcoming.isEmpty()) MutedText("ألغِ محاضرة بتاريخ معين أو غيّر وقتها ليوم واحد دون تعديل الجدول الأساسي.")
                    upcoming.forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${formatDate(e.dateEpoch)}: " + if (e.cancelled) "ملغاة" else "${formatRange(e.newStart ?: slot.startMin, e.newEnd ?: slot.endMin)}${e.newRoom?.let { r -> " - $r" } ?: ""}",
                                Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton({ vm.deleteException(e) }) { Text("إزالة") }
                        }
                    }
                }
            }
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { SectionTitle("ملاحظات المحاضرة") }
                        TextButton({ addingNote = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إضافة") }
                    }
                    if (notes.isEmpty()) MutedText("لا توجد ملاحظات.")
                    notes.forEach { n -> NoteRow(n) { noteDialog = n } }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { SectionTitle("المرفقات") }
                    TextButton({ picker.launch(arrayOf("*/*")) }) { Icon(Icons.Filled.AttachFile, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إرفاق ملف") }
                }
                if (files.isEmpty()) MutedText("يمكنك إرفاق أي نوع من الملفات: صور، PDF، إكسل، بوربوينت، صوت وغيرها.")
            }
            items(files, key = { it.id }) { a ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(if (a.kind == 1) Icons.Filled.Mic else Icons.Filled.Description, null, tint = Crimson)
                        Column(Modifier.weight(1f)) {
                            Text(a.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                            MutedText(formatSize(a.size))
                        }
                        Box {
                            IconButton({ menuFor = a.id }) { Icon(Icons.Filled.MoreVert, "المزيد") }
                            DropdownMenu(menuFor == a.id, { menuFor = null }) {
                                DropdownMenuItem({ Text("حذف المرفق", color = MaterialTheme.colorScheme.error) }, { menuFor = null; confirmDelete = a })
                            }
                        }
                    }
                    // زران منفصلان: فتح (للعرض فقط) وحفظ (نسخة في مكان تختاره)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ ctx.openAttachment(a) }, Modifier.weight(1f)) { Text("فتح") }
                        OutlinedButton({ pendingSave = a; saver.launch(a.name) }, Modifier.weight(1f)) { Text("حفظ") }
                    }
                }
            }
        }
        if (addingNote) NoteDialog(null, slotId, -1, onSave = vm::saveNote, onDelete = null, onDismiss = { addingNote = false })
        noteDialog?.let { n -> NoteDialog(n, slotId, -1, onSave = vm::saveNote, onDelete = { vm.deleteNote(n.id) }, onDismiss = { noteDialog = null }) }
        if (addingTask) TaskDialog(null, slotId, onSave = vm::saveTask, onDelete = null, onDismiss = { addingTask = false })
        taskDialog?.let { t -> TaskDialog(t, slotId, onSave = vm::saveTask, onDelete = { vm.deleteTask(t.id) }, onDismiss = { taskDialog = null }) }
        if (excDialog) ExceptionDialog(slot, onSave = vm::saveException, onDismiss = { excDialog = false })
        if (transfer) TransferDialog(slot, notes, files, defaultWeek = weekNumber(java.time.LocalDate.now(), sem).coerceAtLeast(1)) { transfer = false }
        confirmDelete?.let { a -> ConfirmDialog("حذف المرفق", "هل تريد حذف \"${a.name}\"؟", onConfirm = { vm.deleteAttachment(a); confirmDelete = null }, onDismiss = { confirmDelete = null }) }
    }
}

/** تسجيل الحضور لتاريخ محدد (افتراضيا آخر موعد للمحاضرة) مع عدّاد الغياب وتحذير الحد. */
@Composable
private fun AttendanceCard(vm: MainViewModel, slot: com.schedule.app.data.Slot, allSlots: List<com.schedule.app.data.Slot>, attendance: List<com.schedule.app.data.Attendance>, limit: Int, sem: java.time.LocalDate) {
    val ctx = LocalContext.current
    val today = java.time.LocalDate.now()
    // آخر موعد للمحاضرة حتى اليوم (مع مراعاة الأسابيع الفردية/الزوجية)
    val latest = remember(slot.day, slot.weekMode, sem) {
        var d = today
        var guard = 0
        while (guard++ < 60 && !(dayIndex(d) == slot.day && weekActive(slot.weekMode, weekNumber(d, sem)))) d = d.minusDays(1)
        d.toEpochDay()
    }
    var date by remember(latest) { mutableLongStateOf(latest) }
    // نجمع حضور كل جلسات نفس المادة (نفس الاسم) لحساب الغياب الكلي
    val ids = allSlots.filter { it.title == slot.title }.map { it.id }.toSet()
    val mine = attendance.filter { it.slotId in ids }
    val absent = mine.count { it.status == 1 }
    val present = mine.count { it.status == 0 }
    val excused = mine.count { it.status == 2 }
    val current = attendance.firstOrNull { it.slotId == slot.id && it.dateEpoch == date }?.status
    val warn = absenceWarning(absent, limit)
    AppCard {
        SectionTitle("الحضور والغياب")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton({ pickDate(ctx, date) { date = it } }) { Text("التاريخ: ${formatDate(date)}") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(0 to "حاضر", 1 to "غائب", 2 to "بعذر").forEach { (st, label) ->
                if (current == st) Button({ vm.setAttendance(slot.id, date, null) }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text(label) }
                else OutlinedButton({ vm.setAttendance(slot.id, date, st) }, Modifier.weight(1f)) { Text(label) }
            }
        }
        MutedText("حضور: $present  |  غياب: $absent  |  بعذر: $excused" + if (limit > 0) "  |  الحد المسموح: $limit" else "")
        if (warn == 2) Text("وصلت إلى حد الغياب المسموح في هذه المادة.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        else if (warn == 1) Text("اقتربت من حد الغياب: متبقي غياب واحد فقط.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
