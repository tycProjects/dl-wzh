package com.schedule.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.data.Slot
import com.schedule.app.data.Task
import com.schedule.app.ui.theme.*
import com.schedule.app.util.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

/** مادة واحدة لكل اسم (أول محاضرة بالاسم) لاستخدامها كمعرّف للمادة. */
fun subjectsOf(slots: List<Slot>): List<Slot> = slots.distinctBy { it.title }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubjectChips(slots: List<Slot>, selectedId: Long, onSelect: (Long) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        subjectsOf(slots).forEach { s -> FilterChip(selectedId == s.id, { onSelect(s.id) }, { Text(s.title) }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(vm: MainViewModel, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(containerColor = Bg, topBar = {
        Column {
            TopAppBar(title = { Text("المذاكرة") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
            TabRow(tab, containerColor = Bg, contentColor = Crimson) {
                listOf("المؤقت", "الإحصائيات", "الخطة والفراغ").forEachIndexed { i, t -> Tab(tab == i, { tab = i }, text = { Text(t) }) }
            }
        }
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (tab) { 0 -> PomodoroTab(vm); 1 -> StatsTab(vm); else -> PlanTab(vm) }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        IconButton(onMinus) { Icon(Icons.Filled.Remove, "أقل") }
        Text(value, style = MaterialTheme.typography.titleMedium)
        IconButton(onPlus) { Icon(Icons.Filled.Add, "أكثر") }
    }
}

@Composable
private fun PomodoroTab(vm: MainViewModel) {
    val slots by vm.slots.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val pomo by vm.pomo.collectAsStateWithLifecycle()
    var subject by rememberSaveable { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pomo.running) { while (pomo.running) { now = System.currentTimeMillis(); vm.pomoTick(); delay(500) } }
    val sid = if (pomo.running) pomo.slotId else subject
    val remaining = if (pomo.running) ((pomo.endAt - now) / 1000).coerceAtLeast(0) else (if (pomo.breakPhase) settings.pomoBreak else settings.pomoWork) * 60L
    AppCard {
        SectionTitle("مؤقت المذاكرة (بومودورو)")
        if (slots.isEmpty()) MutedText("أضف محاضرات أولا لتربط الجلسة بمادة.") else SubjectChips(slots, sid) { if (!pomo.running) subject = it }
        Text("%02d:%02d".format(remaining / 60, remaining % 60), Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 56.sp, color = Crimson, style = MaterialTheme.typography.headlineSmall)
        MutedText(if (pomo.running) (if (pomo.breakPhase) "استراحة" else "مذاكرة") else if (pomo.lastSavedMin > 0) "انتهت الجلسة وسُجّلت ${pomo.lastSavedMin} دقيقة" else "جاهز", Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (!pomo.running) {
                Button({ vm.pomoStart(sid, true) }, Modifier.weight(1f), enabled = sid > 0, colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("ابدأ المذاكرة") }
                OutlinedButton({ vm.pomoStart(sid, false) }, Modifier.weight(1f), enabled = sid > 0) { Text("استراحة") }
            } else Button({ vm.pomoStop() }, Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text(if (pomo.breakPhase) "إنهاء الاستراحة" else "إيقاف وتسجيل ما مضى") }
        }
        MutedText("ينبّهك إشعار عند انتهاء الجلسة حتى لو أغلقت الشاشة.")
    }
    AppCard {
        SectionTitle("مدة الجلسات")
        Stepper("المذاكرة (دقيقة)", "${settings.pomoWork}", { vm.updateSettings { copy(pomoWork = (pomoWork - 5).coerceAtLeast(5)) } }, { vm.updateSettings { copy(pomoWork = (pomoWork + 5).coerceAtMost(120)) } })
        Stepper("الاستراحة (دقيقة)", "${settings.pomoBreak}", { vm.updateSettings { copy(pomoBreak = (pomoBreak - 1).coerceAtLeast(1)) } }, { vm.updateSettings { copy(pomoBreak = (pomoBreak + 1).coerceAtMost(30)) } })
    }
}

@Composable
private fun StatsTab(vm: MainViewModel) {
    val slots by vm.slots.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val weekStartMs = weekStart(LocalDate.now()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val all = sessions.map { SessionIn(it.slotId, it.startedAt, it.minutes) }
    val week = minutesBySlot(all, weekStartMs)
    val total = minutesBySlot(all)
    val weekSum = week.values.sum()
    AppCard {
        SectionTitle("هدف هذا الأسبوع")
        LinearProgressIndicator(progress = { goalProgress(weekSum, settings.weeklyGoalMin) }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Crimson, trackColor = Surface2)
        MutedText("${formatMinutesLong(weekSum)} من ${formatMinutesLong(settings.weeklyGoalMin)}")
        Stepper("الهدف الأسبوعي", formatMinutesLong(settings.weeklyGoalMin), { vm.updateSettings { copy(weeklyGoalMin = (weeklyGoalMin - 60).coerceAtLeast(60)) } }, { vm.updateSettings { copy(weeklyGoalMin = (weeklyGoalMin + 60).coerceAtMost(3000)) } })
    }
    AppCard {
        SectionTitle("وقت المذاكرة لكل مادة")
        if (slots.isEmpty()) MutedText("لا توجد مواد.")
        val max = (total.values.maxOrNull() ?: 1).coerceAtLeast(1)
        subjectsOf(slots).forEach { s ->
            val m = total[s.id] ?: 0
            Text("${s.title}: ${formatMinutesLong(m)} (هذا الأسبوع ${formatMinutesLong(week[s.id] ?: 0)})", style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(progress = { m.toFloat() / max }, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = Crimson, trackColor = Surface2)
        }
    }
    AppCard {
        SectionTitle("إنجاز الواجبات لكل مادة")
        val ts: List<Task> = tasks
        subjectsOf(slots).forEach { s ->
            val mine = ts.filter { it.slotId == s.id }
            if (mine.isNotEmpty()) {
                val r = completionRate(mine.count { it.done }, mine.size)
                Text("${s.title}: $r% (${mine.count { it.done }} من ${mine.size})", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { r / 100f }, Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)), color = Crimson, trackColor = Surface2)
            }
        }
        if (ts.none { t -> slots.any { it.id == t.slotId } }) MutedText("لا توجد واجبات مرتبطة بمواد.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanTab(vm: MainViewModel) {
    val exams by vm.exams.collectAsStateWithLifecycle()
    val slots by vm.slots.collectAsStateWithLifecycle()
    var hours by rememberSaveable { mutableIntStateOf(6) }
    var plan by remember { mutableStateOf<List<PlanItem>?>(null) }
    val busy = vm.busyByDayIndex()
    AppCard {
        SectionTitle("خطة مذاكرة قبل الامتحانات")
        if (exams.isEmpty()) MutedText("أضف تواريخ الامتحانات من الملف الشخصي لتوليد خطة.")
        Stepper("ساعات المذاكرة لكل امتحان", "$hours", { hours = (hours - 1).coerceAtLeast(1) }, { hours = (hours + 1).coerceAtMost(40) })
        Button({ plan = vm.studyPlan(hours) }, enabled = exams.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("توليد الخطة") }
        MutedText("توزّع الجلسات (90 دقيقة) على أيام ما قبل الامتحان في فترات خالية من المحاضرات، من 9 صباحا إلى 10 مساء.")
        plan?.let { p ->
            if (p.isEmpty()) MutedText("لا توجد مساحة كافية أو الامتحانات قريبة جدا.")
            p.forEach { Text("${formatDate(it.dateEpoch)}  ${formatRange(it.start, it.end)}  -  ${it.title}", style = MaterialTheme.typography.bodyMedium) }
            if (p.isNotEmpty()) OutlinedButton({ p.forEach { vm.saveTask(Task(slotId = 0, title = "مذاكرة ${it.title} ${formatMinutes(it.start)}", dueEpoch = it.dateEpoch)) } }) { Text("حفظ الجلسات كواجبات (مع تذكير)") }
        }
    }
    AppCard {
        SectionTitle("أفضل أوقات المذاكرة أسبوعيا")
        val sug = suggestStudyTimes(busy)
        if (sug.isEmpty()) MutedText("لا توجد فترات فراغ كافية.")
        sug.forEach { Text("${DAY_NAMES[it.day]}: ${formatRange(it.start, it.end)}", style = MaterialTheme.typography.bodyMedium) }
    }
    AppCard {
        SectionTitle("فترات الفراغ بين المحاضرات (8 ص - 6 م)")
        if (slots.isEmpty()) MutedText("لا توجد محاضرات.")
        (0..6).forEach { d ->
            val g = freeGaps(busy[d].orEmpty(), 8 * 60, 18 * 60, 30)
            if (g.isNotEmpty() && busy[d] != null) Text("${DAY_NAMES[d]}: " + g.joinToString("  |  ") { formatRange(it.first, it.second) }, style = MaterialTheme.typography.bodySmall)
        }
    }
}
