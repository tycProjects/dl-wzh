package com.schedule.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.data.Grade
import com.schedule.app.data.Reference
import com.schedule.app.data.Task
import com.schedule.app.data.Card as StudyCard
import com.schedule.app.ui.theme.*
import com.schedule.app.util.*
import java.time.LocalDate

/** أدوات المادة: الدرجات وحاسبة النهائي، بطاقات المراجعة، المراجع، ربط الواجبات بالملفات، نسخ المحاضرة. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubjectToolsScreen(vm: MainViewModel, slotId: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val slots by vm.slots.collectAsStateWithLifecycle()
    val slot = slots.firstOrNull { it.id == slotId }
    val allGrades by vm.grades.collectAsStateWithLifecycle()
    val credits by vm.credits.collectAsStateWithLifecycle()
    val allCards by vm.cards.collectAsStateWithLifecycle()
    val allRefs by vm.refs.collectAsStateWithLifecycle()
    val allTasks by vm.tasks.collectAsStateWithLifecycle()
    val files by vm.attachments.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val grades = allGrades.filter { it.slotId == slotId }
    val cards = allCards.filter { it.slotId == slotId }
    val refs = allRefs.filter { it.slotId == slotId }
    val tasks = allTasks.filter { it.slotId == slotId }
    val myFiles = files.filter { it.slotId == slotId }
    val hours = credits.firstOrNull { it.slotId == slotId }?.hours ?: 3
    var gradeDialog by remember { mutableStateOf<Grade?>(null) }
    var addGrade by remember { mutableStateOf(false) }
    var cardDialog by remember { mutableStateOf<StudyCard?>(null) }
    var addCard by remember { mutableStateOf(false) }
    var review by remember { mutableStateOf(false) }
    var refDialog by remember { mutableStateOf<Reference?>(null) }
    var addRef by remember { mutableStateOf(false) }
    var target by rememberSaveable { mutableIntStateOf(90) }
    var finalWeight by rememberSaveable { mutableIntStateOf(40) }
    var linkMenu by remember { mutableStateOf<Long?>(null) }
    val today = LocalDate.now().toEpochDay()
    val due = cards.filter { it.dueEpoch <= today }

    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text(slot?.title.orEmpty() + " - أدوات المادة", maxLines = 1) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { pad ->
        if (slot == null) { Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { MutedText("المادة غير موجودة") }; return@Scaffold }
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // ---- الدرجات
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { SectionTitle("الدرجات") }
                    TextButton({ addGrade = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إضافة") }
                }
                if (grades.isEmpty()) MutedText("أضف درجات الأعمال والامتحانات مع وزن كل منها (من 100).")
                grades.forEach { g ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${g.name}: ${trim(g.score)} / ${trim(g.max)}  (الوزن ${trim(g.weight)})" + if (g.isFinal) "  [نهائي]" else "", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton({ gradeDialog = g }) { Text("تعديل") }
                    }
                }
                val gi = grades.map { GradeIn(it.score, it.max, it.weight, it.isFinal) }
                courseAverage(gi)?.let { avg ->
                    Text("المتوسط حتى الآن: ${"%.1f".format(avg)}%  (${percentToLetter(avg)})  -  المحصّل ${"%.1f".format(earnedPoints(gi))} من ${trim(completedWeight(gi))}", color = Crimson, style = MaterialTheme.typography.bodyMedium)
                }
                Stepper2("الساعات المعتمدة", "$hours", { vm.setCredit(slotId, (hours - 1).coerceAtLeast(0)) }, { vm.setCredit(slotId, (hours + 1).coerceAtMost(10)) })
            }
            // ---- حاسبة النهائي
            AppCard {
                SectionTitle("كم أحتاج في النهائي؟")
                val nf = grades.filter { !it.isFinal }.map { GradeIn(it.score, it.max, it.weight) }
                Stepper2("وزن النهائي", "$finalWeight", { finalWeight = (finalWeight - 5).coerceAtLeast(5) }, { finalWeight = (finalWeight + 5).coerceAtMost(100) })
                Stepper2("النسبة المستهدفة للمادة", "$target%", { target = (target - 1).coerceAtLeast(50) }, { target = (target + 1).coerceAtMost(100) })
                val need = neededOnFinal(nf, finalWeight.toDouble(), target.toDouble())
                val txt = when {
                    nf.isEmpty() -> "أضف درجات الأعمال أولا (بدون النهائي)."
                    need == null -> ""
                    need > 100 -> "غير ممكن: تحتاج ${"%.0f".format(need)}% من درجة النهائي، أي أكثر من الدرجة الكاملة."
                    need <= 0 -> "حققت الهدف بالفعل قبل النهائي."
                    else -> "تحتاج ${"%.1f".format(need)}% من درجة النهائي (${percentToLetter(target.toDouble())} تقريبا)."
                }
                if (txt.isNotEmpty()) Text(txt, color = Crimson, style = MaterialTheme.typography.bodyMedium)
            }
            // ---- بطاقات المراجعة
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { SectionTitle("بطاقات المراجعة (تكرار متباعد)") }
                    TextButton({ addCard = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إضافة") }
                }
                MutedText("${cards.size} بطاقة، المستحقة اليوم: ${due.size}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ review = true }, enabled = due.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("ابدأ المراجعة") }
                    OutlinedButton({
                        val qs = makeQuestions(notes.filter { it.slotId == slotId }.joinToString(". ") { it.text }, 8)
                        qs.forEach { vm.saveCard(StudyCard(slotId = slotId, front = it.first, back = it.second)) }
                        ctx.toast(if (qs.isEmpty()) "لا توجد ملاحظات كافية" else "أُضيفت ${qs.size} بطاقة من الملاحظات")
                    }) { Text("توليد من الملاحظات") }
                }
                cards.take(20).forEach { c ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.front, Modifier.weight(1f), maxLines = 2, style = MaterialTheme.typography.bodySmall)
                        TextButton({ cardDialog = c }) { Text("تعديل") }
                    }
                }
            }
            // ---- المراجع
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { SectionTitle("الكتب والمراجع") }
                    TextButton({ addRef = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إضافة") }
                }
                if (refs.isEmpty()) MutedText("سجّل كتب المادة ومراجعها.")
                refs.forEach { r ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.title, style = MaterialTheme.typography.bodyMedium)
                            if (r.author.isNotBlank() || r.note.isNotBlank()) MutedText(listOf(r.author, r.note).filter { it.isNotBlank() }.joinToString(" - "))
                        }
                        TextButton({ refDialog = r }) { Text("تعديل") }
                    }
                }
            }
            // ---- ربط الواجبات بالملفات
            AppCard {
                SectionTitle("ربط الواجبات بالمرفقات")
                if (tasks.isEmpty()) MutedText("لا توجد واجبات لهذه المادة.")
                if (tasks.isNotEmpty() && myFiles.isEmpty()) MutedText("أرفق ملفات من شاشة المحاضرة ثم اربطها هنا.")
                tasks.forEach { t: Task ->
                    val f = myFiles.firstOrNull { it.id == t.attachmentId }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(t.title, style = MaterialTheme.typography.bodyMedium); MutedText(f?.name ?: "بدون ملف") }
                        if (f != null) TextButton({ ctx.openAttachment(f) }) { Text("فتح") }
                        Box {
                            TextButton({ linkMenu = t.id }, enabled = myFiles.isNotEmpty()) { Text("ربط") }
                            DropdownMenu(linkMenu == t.id, { linkMenu = null }) {
                                DropdownMenuItem({ Text("إزالة الربط") }, { vm.linkTaskAttachment(t, 0); linkMenu = null })
                                myFiles.forEach { a -> DropdownMenuItem({ Text(a.name) }, { vm.linkTaskAttachment(t, a.id); linkMenu = null }) }
                            }
                        }
                    }
                }
            }
            // ---- نسخ المحاضرة
            AppCard {
                SectionTitle("نسخ المحاضرة إلى يوم آخر")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_NAMES.forEachIndexed { i, n -> if (i != slot.day) AssistChip({ vm.copySlot(slot, i) }, { Text(n) }) }
                }
                MutedText("تُنشأ نسخة بنفس الوقت والقاعة؛ عدّل وقتها من الجدول عند الحاجة.")
            }
        }
    }

    if (addGrade) GradeDialog(null, slotId, { vm.saveGrade(it) }, null) { addGrade = false }
    gradeDialog?.let { g -> GradeDialog(g, slotId, { vm.saveGrade(it) }, { vm.deleteGrade(g.id) }) { gradeDialog = null } }
    if (addCard) CardDialog(null, slotId, { vm.saveCard(it) }, null) { addCard = false }
    cardDialog?.let { c -> CardDialog(c, slotId, { vm.saveCard(it) }, { vm.deleteCard(c.id) }) { cardDialog = null } }
    if (addRef) RefDialog(null, slotId, { vm.saveRef(it) }, null) { addRef = false }
    refDialog?.let { r -> RefDialog(r, slotId, { vm.saveRef(it) }, { vm.deleteRef(r.id) }) { refDialog = null } }
    if (review) ReviewDialog(due, onRate = { c, q -> vm.reviewCard(c, q) }) { review = false }
}

private fun trim(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else "%.1f".format(d)

@Composable
private fun Stepper2(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onMinus) { Icon(Icons.Filled.Remove, "أقل") }
        Text(value, style = MaterialTheme.typography.titleMedium)
        IconButton(onPlus) { Icon(Icons.Filled.Add, "أكثر") }
    }
}

@Composable
private fun GradeDialog(initial: Grade?, slotId: Long, onSave: (Grade) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var score by remember { mutableStateOf(initial?.score?.let { trim(it) }.orEmpty()) }
    var max by remember { mutableStateOf(initial?.max?.let { trim(it) } ?: "100") }
    var weight by remember { mutableStateOf(initial?.weight?.let { trim(it) } ?: "10") }
    var isFinal by remember { mutableStateOf(initial?.isFinal ?: false) }
    val sc = score.toDoubleOrNull(); val mx = max.toDoubleOrNull(); val w = weight.toDoubleOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "درجة جديدة" else "تعديل الدرجة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppField(name, "الاسم (مثال: ميد تيرم)") { name = it }
                AppField(score, "الدرجة المحصّلة") { score = it }
                AppField(max, "من") { max = it }
                AppField(weight, "الوزن من 100") { weight = it }
                Row(verticalAlignment = Alignment.CenterVertically) { Text("هذا هو الامتحان النهائي", Modifier.weight(1f)); Switch(isFinal, { isFinal = it }, colors = SwitchDefaults.colors(checkedTrackColor = Crimson)) }
                if (onDelete != null) TextButton({ onDelete(); onDismiss() }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && sc != null && mx != null && mx > 0 && w != null && w > 0, onClick = {
            onSave(Grade(id = initial?.id ?: 0, slotId = slotId, name = name.trim(), score = sc!!, max = mx!!, weight = w!!, isFinal = isFinal)); onDismiss()
        }) { Text("حفظ") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
private fun CardDialog(initial: StudyCard?, slotId: Long, onSave: (StudyCard) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var front by remember { mutableStateOf(initial?.front.orEmpty()) }
    var back by remember { mutableStateOf(initial?.back.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "بطاقة جديدة" else "تعديل البطاقة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppField(front, "السؤال", singleLine = false) { front = it }
                AppField(back, "الإجابة", singleLine = false) { back = it }
                if (onDelete != null) TextButton({ onDelete(); onDismiss() }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = front.isNotBlank() && back.isNotBlank(), onClick = { onSave((initial ?: StudyCard(slotId = slotId, front = "", back = "")).copy(front = front.trim(), back = back.trim())); onDismiss() }) { Text("حفظ") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
private fun RefDialog(initial: Reference?, slotId: Long, onSave: (Reference) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var author by remember { mutableStateOf(initial?.author.orEmpty()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "مرجع جديد" else "تعديل المرجع") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppField(title, "اسم الكتاب أو المرجع") { title = it }
                AppField(author, "المؤلف") { author = it }
                AppField(note, "ملاحظة (مثال: الفصول 1-4)") { note = it }
                if (onDelete != null) TextButton({ onDelete(); onDismiss() }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(Reference(id = initial?.id ?: 0, slotId = slotId, title = title.trim(), author = author.trim(), note = note.trim())); onDismiss() }) { Text("حفظ") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } })
}

@Composable
private fun ReviewDialog(due: List<StudyCard>, onRate: (StudyCard, Int) -> Unit, onDismiss: () -> Unit) {
    var i by remember { mutableIntStateOf(0) }
    var shown by remember { mutableStateOf(false) }
    val queue = remember { due }
    val c = queue.getOrNull(i)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (c == null) "انتهت المراجعة" else "بطاقة ${i + 1} من ${queue.size}") },
        text = {
            if (c == null) Text("أحسنت. ستظهر البطاقات مجددا بحسب مستوى تذكرك لها.") else
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(c.front, style = MaterialTheme.typography.titleMedium)
                    if (shown) Text(c.back, color = Crimson, style = MaterialTheme.typography.bodyLarge) else OutlinedButton({ shown = true }) { Text("أظهر الإجابة") }
                }
        },
        confirmButton = {
            if (c == null) TextButton(onDismiss) { Text("إغلاق") } else if (shown) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(1 to "لم أتذكر", 3 to "صعب", 4 to "جيد", 5 to "سهل").forEach { (q, l) -> TextButton({ onRate(c, q); i++; shown = false }) { Text(l) } }
            }
        },
        dismissButton = { if (c != null) TextButton(onDismiss) { Text("إنهاء") } })
}

/** الدرجات والمعدل التراكمي لكل المواد في الجدول النشط. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(vm: MainViewModel, onBack: () -> Unit, onOpenTools: (Long) -> Unit) {
    val slots by vm.slots.collectAsStateWithLifecycle()
    val grades by vm.grades.collectAsStateWithLifecycle()
    val credits by vm.credits.collectAsStateWithLifecycle()
    val subjects = subjectsOf(slots)
    val rows = subjects.mapNotNull { s ->
        val avg = courseAverage(grades.filter { it.slotId == s.id }.map { GradeIn(it.score, it.max, it.weight, it.isFinal) }) ?: return@mapNotNull null
        Triple(s, avg, credits.firstOrNull { it.slotId == s.id }?.hours ?: 3)
    }
    val g = gpa(rows.map { it.second to it.third })
    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("الدرجات والمعدل") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                SectionTitle("المعدل التراكمي المتوقع")
                Text(g?.let { "%.2f من 4.00".format(it) } ?: "لا توجد درجات بعد", color = Crimson, style = MaterialTheme.typography.titleLarge)
                MutedText("محسوب بمقياس 4.0 تقريبي من متوسط كل مادة حتى الآن مرجّحا بالساعات المعتمدة (3 افتراضيا). قد يختلف عن نظام كليتك.")
            }
            subjects.forEach { s ->
                val r = rows.firstOrNull { it.first.id == s.id }
                AppCard(Modifier.then(Modifier)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleMedium)
                            MutedText(r?.let { "${"%.1f".format(it.second)}%  (${percentToLetter(it.second)})  -  ${it.third} ساعات" } ?: "لا توجد درجات")
                        }
                        TextButton({ onOpenTools(s.id) }) { Text("الدرجات") }
                    }
                }
            }
            if (subjects.isEmpty()) MutedText("أضف مواد أولا.")
        }
    }
}
