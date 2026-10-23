package com.lecturemanager.app.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturemanager.app.MainViewModel
import com.lecturemanager.app.data.Lecture
import com.lecturemanager.app.data.Subject
import com.lecturemanager.app.ui.theme.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

val SUBJECT_COLORS = listOf(0xFF1A2542, 0xFF0D9488, 0xFFD97706, 0xFF7C3AED, 0xFFDC2626, 0xFF2563EB, 0xFF059669)
val SUBJECT_ICONS = listOf("📘", "💻", "🧪", "📐", "⚖️", "🩺", "🌍", "🎨", "📊", "🔬")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(vm: MainViewModel, onDone: () -> Unit) {
    var mode by remember { mutableIntStateOf(0) }
    val busy by vm.busy.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("إضافة محتوى", color = Navy) },
            navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas),
        )
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Teal)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("محاضرة جديدة", "مادة جديدة").forEachIndexed { i, label ->
                SegmentedButton(
                    selected = mode == i, onClick = { mode = i },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text(label) }
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (mode == 0) LectureForm(vm, onDone) else SubjectForm(vm, onDone)
        }
    }
}

@Composable
fun ColorPicker(selected: Long, onSelect: (Long) -> Unit) {
    Text("اللون المميز", style = MaterialTheme.typography.labelLarge, color = Navy)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SUBJECT_COLORS.forEach { c ->
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(adaptAccent(Color(c.toInt())))
                    .border(if (c == selected) 3.dp else 0.dp, Navy.copy(alpha = 0.5f), CircleShape)
                    .clickable { onSelect(c) },
            )
        }
    }
}

@Composable
fun IconPicker(selected: String, onSelect: (String) -> Unit) {
    Text("الأيقونة", style = MaterialTheme.typography.labelLarge, color = Navy)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SUBJECT_ICONS.forEach { i ->
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (i == selected) Navy.copy(alpha = 0.12f) else CardBg)
                    .border(1.dp, if (i == selected) Navy else Border, RoundedCornerShape(10.dp))
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(i) }
        }
    }
}

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
fun formatExamDate(millis: Long): String = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(DATE_FMT)

/** حقل موعد الامتحان. القيمة بداية اليوم بتوقيت UTC (كما يعيدها DatePicker). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamDateField(millis: Long?, onChange: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { open = true }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, InputBorder),
        ) {
            Icon(Icons.Filled.Event, null, tint = Slate); Spacer(Modifier.width(8.dp))
            Text(if (millis != null) "موعد الامتحان: ${formatExamDate(millis)}" else "تحديد موعد الامتحان (اختياري)", color = if (millis != null) Navy else Slate)
        }
        if (millis != null) IconButton(onClick = { onChange(null) }) { Icon(Icons.Filled.Close, "إزالة الموعد", tint = Slate) }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { onChange(state.selectedDateMillis); open = false }) { Text("تأكيد") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("إلغاء") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun SubjectForm(vm: MainViewModel, onDone: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var instructor by remember { mutableStateOf("") }
    var color by remember { mutableLongStateOf(SUBJECT_COLORS[0]) }
    var icon by remember { mutableStateOf(SUBJECT_ICONS[0]) }
    var exam by remember { mutableStateOf<Long?>(null) }

    AppField(title, { title = it }, "اسم المادة *")
    AppField(code, { code = it }, "الرمز الأكاديمي (مثل CS402)")
    AppField(instructor, { instructor = it }, "اسم المحاضر")
    ExamDateField(exam) { exam = it }
    ColorPicker(color) { color = it }
    IconPicker(icon) { icon = it }
    PrimaryButton("حفظ المادة", enabled = title.isNotBlank()) {
        vm.addSubject(Subject(title = title.trim(), code = code.trim(), instructor = instructor.trim(), themeColor = color, icon = icon, examDate = exam))
        onDone()
    }
}

@Composable
private fun LectureForm(vm: MainViewModel, onDone: () -> Unit) {
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val lectures by vm.lectures.collectAsStateWithLifecycle()
    var subjectId by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var week by remember { mutableStateOf("1") }
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var pdf by remember { mutableStateOf<Uri?>(null) }
    var notes by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf(false) }
    val chosen = subjects.firstOrNull { it.id == subjectId }

    if (subjects.isEmpty()) {
        Text("أضف مادة أولاً من تبويب «مادة جديدة».", color = Slate)
        return
    }
    Box {
        OutlinedButton(
            onClick = { menu = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, InputBorder),
        ) {
            Text(chosen?.title ?: "اختر المادة *", color = if (chosen == null) Slate else Navy, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, null, tint = Slate)
        }
        DropdownMenu(menu, { menu = false }) {
            subjects.forEach { s ->
                DropdownMenuItem(text = { Text("${s.icon} ${s.title}") }, onClick = { subjectId = s.id; menu = false })
            }
        }
    }
    val existing = lectures.filter { it.subjectId == subjectId }.map { it.weekNumber }.distinct().sorted()
    AppField(week, { week = it.filter(Char::isDigit).take(3) }, "رقم الأسبوع *", KeyboardType.Number,
        supporting = if (existing.isEmpty()) "سيُنشأ الأسبوع تلقائياً" else "أسابيع موجودة: ${existing.joinToString("، ")} — أو اكتب رقماً جديداً")
    AppField(title, { title = it }, "عنوان المحاضرة *")
    AppField(duration, { duration = it.filter(Char::isDigit).take(4) }, "المدة التقديرية (بالدقائق)", KeyboardType.Number)

    Text("ملف المحاضرة (اختياري — يمكن إرفاقه لاحقاً)", style = MaterialTheme.typography.labelLarge, color = Navy)
    PdfPickerButtons { pdf = it }
    if (pdf != null) Badge("تم اختيار ملف PDF ✓", TealTint, TealText)

    AppField(notes, { notes = it }, "ملاحظات أولية للمذاكرة", minLines = 3)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { completed = !completed }) {
        Checkbox(completed, { completed = it }); Text("تحديدها كمكتملة فوراً", color = Navy)
    }
    PrimaryButton("حفظ المحاضرة", enabled = subjectId != null && title.isNotBlank() && week.isNotBlank()) {
        vm.addLecture(
            Lecture(
                subjectId = subjectId!!, weekNumber = week.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                title = title.trim(), duration = duration.toIntOrNull() ?: 0,
                isCompleted = completed, notes = notes,
            ), pdf, onDone,
        )
    }
}

@Composable
fun AppField(
    value: String, onChange: (String) -> Unit, label: String,
    keyboard: KeyboardType = KeyboardType.Text, minLines: Int = 1, supporting: String? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(8.dp),
        supportingText = supporting?.let { { Text(it) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Navy, unfocusedBorderColor = InputBorder,
            focusedContainerColor = CardBg, unfocusedContainerColor = CardBg,
        ),
    )
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NavyFill, contentColor = Color.White),
    ) { Text(text) }
}

/** نافذة تعديل مادة موجودة. */
@Composable
fun EditSubjectDialog(s: Subject, onSave: (Subject) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(s.title) }
    var code by remember { mutableStateOf(s.code) }
    var instructor by remember { mutableStateOf(s.instructor) }
    var color by remember { mutableLongStateOf(s.themeColor) }
    var icon by remember { mutableStateOf(s.icon) }
    var exam by remember { mutableStateOf(s.examDate) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل المادة") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(title, { title = it }, "اسم المادة *")
                AppField(code, { code = it }, "الرمز الأكاديمي")
                AppField(instructor, { instructor = it }, "اسم المحاضر")
                ExamDateField(exam) { exam = it }
                ColorPicker(color) { color = it }
                IconPicker(icon) { icon = it }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onSave(s.copy(title = title.trim(), code = code.trim(), instructor = instructor.trim(), themeColor = color, icon = icon, examDate = exam))
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

/** نافذة تعديل محاضرة موجودة. */
@Composable
fun EditLectureDialog(l: Lecture, onSave: (Lecture) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(l.title) }
    var week by remember { mutableStateOf(l.weekNumber.toString()) }
    var duration by remember { mutableStateOf(if (l.duration > 0) l.duration.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل المحاضرة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(title, { title = it }, "عنوان المحاضرة *")
                AppField(week, { week = it.filter(Char::isDigit).take(3) }, "رقم الأسبوع", KeyboardType.Number)
                AppField(duration, { duration = it.filter(Char::isDigit).take(4) }, "المدة (بالدقائق)", KeyboardType.Number)
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && week.isNotBlank(), onClick = {
                onSave(l.copy(title = title.trim(), weekNumber = week.toIntOrNull()?.coerceAtLeast(1) ?: l.weekNumber, duration = duration.toIntOrNull() ?: 0))
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}
