package com.lecturemanager.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturemanager.app.MainViewModel
import com.lecturemanager.app.data.Lecture
import com.lecturemanager.app.data.Subject
import com.lecturemanager.app.data.todayEpochDay
import com.lecturemanager.app.ui.theme.*
import com.lecturemanager.app.util.daysUntilExam

@Composable
fun HomeScreen(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val lectures by vm.lectures.collectAsStateWithLifecycle()
    val cards by vm.flashcards.collectAsStateWithLifecycle()
    var expanded by rememberSaveable(
        stateSaver = listSaver<Set<Long>, Long>(save = { it.toList() }, restore = { it.toSet() })
    ) { mutableStateOf(setOf<Long>()) }
    var deleteSubject by remember { mutableStateOf<Subject?>(null) }
    var deleteLecture by remember { mutableStateOf<Lecture?>(null) }
    var editSubject by remember { mutableStateOf<Subject?>(null) }
    var editLecture by remember { mutableStateOf<Lecture?>(null) }

    // تصدير ملاحظات مادة (PDF أو نص) إلى مكان يختاره المستخدم
    var exportTarget by remember { mutableStateOf<Subject?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val s = exportTarget; if (uri != null && s != null) vm.exportNotes(s, true, uri)
    }
    val txtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val s = exportTarget; if (uri != null && s != null) vm.exportNotes(s, false, uri)
    }

    val today = todayEpochDay()
    val dueCards = cards.count { it.dueDay <= today }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { SummaryCard(subjects, lectures, dueCards, today) }
        if (subjects.isEmpty()) item {
            Text(
                "لا توجد مواد بعد. اضغط على + لإضافة مادة ثم أضف محاضراتها.",
                color = Slate, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(24.dp),
            )
        }
        items(subjects, key = { it.id }) { s ->
            val ls = lectures.filter { it.subjectId == s.id }
            SubjectCard(
                s, ls, today, expanded = s.id in expanded,
                onToggleExpand = { expanded = if (s.id in expanded) expanded - s.id else expanded + s.id },
                onOpen = onOpen, vm = vm,
                onEditSubject = { editSubject = s }, onDeleteSubject = { deleteSubject = s },
                onExportPdf = { exportTarget = s; pdfLauncher.launch("ملاحظات_${s.title}.pdf") },
                onExportTxt = { exportTarget = s; txtLauncher.launch("ملاحظات_${s.title}.txt") },
                onEditLecture = { editLecture = it }, onDeleteLecture = { deleteLecture = it },
            )
        }
        if (lectures.isNotEmpty()) item {
            Text(
                "اضغط مطولاً على أي محاضرة لعرض خياراتها",
                color = SlateLight, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        item { Spacer(Modifier.height(72.dp)) }
    }

    deleteSubject?.let { s ->
        ConfirmDialog("حذف المادة", "سيتم حذف «${s.title}» وجميع محاضراتها وملفاتها نهائياً.",
            onConfirm = { vm.deleteSubject(s); deleteSubject = null }, onDismiss = { deleteSubject = null })
    }
    deleteLecture?.let { l ->
        ConfirmDialog("حذف المحاضرة", "سيتم حذف «${l.title}» وملفها نهائياً.",
            onConfirm = { vm.deleteLecture(l); deleteLecture = null }, onDismiss = { deleteLecture = null })
    }
    editSubject?.let { s -> EditSubjectDialog(s, onSave = { vm.updateSubject(it); editSubject = null }, onDismiss = { editSubject = null }) }
    editLecture?.let { l -> EditLectureDialog(l, onSave = { vm.updateLecture(it); editLecture = null }, onDismiss = { editLecture = null }) }
}

@Composable
fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("حذف", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun SummaryCard(subjects: List<Subject>, lectures: List<Lecture>, dueCards: Int, today: Long) {
    val done = lectures.count { it.isCompleted }
    val total = lectures.size
    val progress = if (total == 0) 0f else done.toFloat() / total
    val nextExam = subjects.mapNotNull { s -> s.examDate?.let { s to daysUntilExam(it, today) } }
        .filter { it.second >= 0 }.minByOrNull { it.second }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyFill),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("تقدّمك الدراسي", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("المواد", subjects.size); Stat("المحاضرات", total)
                Stat("المنجزة", done); Stat("المتبقية", total - done)
            }
            Spacer(Modifier.height(14.dp))
            AppProgressBar(progress, color = Color(0xFF2DD4BF))
            Spacer(Modifier.height(6.dp))
            Text("${(progress * 100).toInt()}٪ من الفصل الدراسي", color = Color(0xFFBBC5EB),
                style = MaterialTheme.typography.labelMedium)
            if (nextExam != null) {
                Spacer(Modifier.height(8.dp))
                Text("⏰ أقرب امتحان: ${nextExam.first.title} — ${examLabel(nextExam.second).removePrefix("الامتحان ")}",
                    color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
            if (dueCards > 0) {
                Spacer(Modifier.height(4.dp))
                Text("🃏 $dueCards بطاقة مراجعة مستحقة اليوم", color = Color(0xFFBBC5EB), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$value", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Text(label, color = Color(0xFFBBC5EB), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SubjectCard(
    s: Subject, ls: List<Lecture>, today: Long, expanded: Boolean,
    onToggleExpand: () -> Unit, onOpen: (Long) -> Unit, vm: MainViewModel,
    onEditSubject: () -> Unit, onDeleteSubject: () -> Unit, onExportPdf: () -> Unit, onExportTxt: () -> Unit,
    onEditLecture: (Lecture) -> Unit, onDeleteLecture: (Lecture) -> Unit,
) {
    val done = ls.count { it.isCompleted }
    val progress = if (ls.isEmpty()) 0f else done.toFloat() / ls.size
    val accent = adaptAccent(Color(s.themeColor.toInt()))
    // أسبوع واحد فقط مفتوح في كل مادة: فتح أسبوع يغلق السابق تلقائياً.
    var openWeek by rememberSaveable(s.id) { mutableStateOf<Int?>(null) }
    var menu by remember { mutableStateOf(false) }

    AppCard(Modifier.animateContentSize()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggleExpand), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Text(s.icon, style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.title, style = MaterialTheme.typography.titleMedium, color = Navy, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val meta = listOf(s.code, s.instructor).filter { it.isNotBlank() }.joinToString(" • ")
                if (meta.isNotEmpty()) Text(meta, style = MaterialTheme.typography.bodySmall, color = Slate)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "خيارات المادة", tint = Slate) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem(text = { Text("تعديل المادة") }, leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = { menu = false; onEditSubject() })
                    DropdownMenuItem(text = { Text("نقل لأعلى") }, leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) },
                        onClick = { menu = false; vm.moveSubject(s.id, -1) })
                    DropdownMenuItem(text = { Text("نقل لأسفل") }, leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) },
                        onClick = { menu = false; vm.moveSubject(s.id, 1) })
                    DropdownMenuItem(text = { Text("تصدير الملاحظات (PDF)") }, leadingIcon = { Icon(Icons.Filled.PictureAsPdf, null) },
                        onClick = { menu = false; onExportPdf() })
                    DropdownMenuItem(text = { Text("تصدير الملاحظات (نص)") }, leadingIcon = { Icon(Icons.Filled.Description, null) },
                        onClick = { menu = false; onExportTxt() })
                    DropdownMenuItem(
                        text = { Text("حذف المادة", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDeleteSubject() },
                    )
                }
            }
            val rot by animateFloatAsState(if (expanded) 180f else 0f, label = "subjectChevron")
            Icon(Icons.Filled.ExpandMore, null, tint = Slate, modifier = Modifier.rotate(rot))
        }
        Spacer(Modifier.height(10.dp))
        AppProgressBar(progress, color = accent)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$done من ${ls.size} منجز", style = MaterialTheme.typography.labelMedium, color = Slate, modifier = Modifier.weight(1f))
            s.examDate?.let { ExamBadge(daysUntilExam(it, today)) }
        }

        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ls.isEmpty()) Text("لا توجد محاضرات بعد.", color = Slate, style = MaterialTheme.typography.bodySmall)
                ls.groupBy { it.weekNumber }.toSortedMap().forEach { (week, items) ->
                    WeekSection(
                        week, items.sortedWith(compareBy({ it.sortOrder }, { it.id })), accent, open = openWeek == week,
                        onToggleOpen = { openWeek = if (openWeek == week) null else week },
                        onOpen = onOpen, vm = vm, onEdit = onEditLecture, onDelete = onDeleteLecture,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekSection(
    week: Int, items: List<Lecture>, accent: Color, open: Boolean, onToggleOpen: () -> Unit,
    onOpen: (Long) -> Unit, vm: MainViewModel, onEdit: (Lecture) -> Unit, onDelete: (Lecture) -> Unit,
) {
    val wDone = items.count { it.isCompleted }
    val allDone = wDone == items.size
    val rot by animateFloatAsState(if (open) 180f else 0f, label = "weekChevron")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (open) CardBg else Canvas)
            .border(1.dp, if (open) accent.copy(alpha = 0.5f) else Border, RoundedCornerShape(12.dp))
            .animateContentSize(),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggleOpen).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("الأسبوع $week", style = MaterialTheme.typography.titleMedium, color = Navy, modifier = Modifier.weight(1f))
            Badge("$wDone من ${items.size}", if (allDone) TealTint else AmberTint, if (allDone) TealText else AmberText)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.ExpandMore, null, tint = Slate, modifier = Modifier.rotate(rot))
        }
        if (open) {
            HorizontalDivider(color = Border)
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                items.forEachIndexed { i, l ->
                    LectureRow(l, onOpen, vm, onEdit, onDelete)
                    if (i < items.lastIndex) HorizontalDivider(color = Border.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LectureRow(l: Lecture, onOpen: (Long) -> Unit, vm: MainViewModel, onEdit: (Lecture) -> Unit, onDelete: (Lecture) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                .combinedClickable(onClick = { onOpen(l.id) }, onLongClick = { menu = true })
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.toggle(l) }, modifier = Modifier.size(44.dp)) {
                if (l.isCompleted) Icon(Icons.Filled.CheckCircle, "مكتملة", tint = Teal)
                else Icon(Icons.Outlined.RadioButtonUnchecked, "غير مكتملة", tint = SlateLight)
            }
            Column(Modifier.weight(1f).padding(end = 8.dp, start = 4.dp, top = 4.dp, bottom = 4.dp)) {
                Text(
                    l.title, style = MaterialTheme.typography.bodyMedium,
                    color = if (l.isCompleted) SlateLight else Navy, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (l.duration > 0) Text("${l.duration} د", style = MaterialTheme.typography.labelMedium, color = Slate)
                    if (l.pdfPath != null) Badge("PDF • ${formatBytes(l.fileSize)}", TealTint, TealText)
                    if (l.tag.isNotBlank()) TagBadge(l.tag)
                }
                if (l.pdfPath != null && l.pageCount > 0 && l.lastPage > 0) {
                    val read = ((l.lastPage + 1).toFloat() / l.pageCount).coerceIn(0f, 1f)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppProgressBar(read, Modifier.weight(1f), color = Ochre, height = 4.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("صفحة ${l.lastPage + 1}/${l.pageCount}", style = MaterialTheme.typography.labelSmall, color = Slate)
                    }
                }
            }
            Icon(Icons.Filled.ChevronLeft, null, tint = SlateLight, modifier = Modifier.padding(end = 6.dp))
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text("فتح المحاضرة") }, leadingIcon = { Icon(Icons.Filled.FolderOpen, null) },
                onClick = { menu = false; onOpen(l.id) })
            DropdownMenuItem(text = { Text(if (l.isCompleted) "إلغاء الإنجاز" else "تحديد كمكتملة") },
                leadingIcon = { Icon(Icons.Filled.CheckCircle, null) }, onClick = { menu = false; vm.toggle(l) })
            DropdownMenuItem(text = { Text("تعديل") }, leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { menu = false; onEdit(l) })
            DropdownMenuItem(text = { Text("نقل لأعلى") }, leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) },
                onClick = { menu = false; vm.moveLecture(l.id, -1) })
            DropdownMenuItem(text = { Text("نقل لأسفل") }, leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) },
                onClick = { menu = false; vm.moveLecture(l.id, 1) })
            DropdownMenuItem(
                text = { Text("حذف المحاضرة", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { menu = false; onDelete(l) },
            )
        }
    }
}
