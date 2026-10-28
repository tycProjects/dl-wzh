package com.schedule.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.ui.theme.*
import com.schedule.app.util.*
import java.time.LocalDate
import java.time.YearMonth

private val GOLD = Color(0xFFD97706)

/** تقويم شهري (يعرض المحاضرات والامتحانات والواجبات) مع عرض أجندة للأيام القادمة. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: MainViewModel, onBack: () -> Unit, onOpenSlot: (Long) -> Unit) {
    val slots by vm.slots.collectAsStateWithLifecycle()
    val excs by vm.exceptions.collectAsStateWithLifecycle()
    val holidays by vm.holidays.collectAsStateWithLifecycle()
    val exams by vm.exams.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sem = LocalDate.ofEpochDay(settings.semesterStart)
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selected by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var agenda by rememberSaveable { mutableStateOf(false) }
    val ym = YearMonth.parse(month)

    fun lecturesOf(d: LocalDate) = itemsOn(d, slots, excs, holidays, sem)

    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("التقويم") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
            actions = { TextButton({ agenda = !agenda }) { Text(if (agenda) "عرض الشهر" else "عرض الأجندة", color = Crimson) } })
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!agenda) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton({ month = ym.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الشهر السابق") }
                        Text("${MONTHS[ym.monthValue - 1]} ${ym.year}", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                        IconButton({ month = ym.plusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الشهر التالي") }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth()) { DAY_NAMES.forEach { Text(it.take(3), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Muted) } }
                }
                item {
                    val first = ym.atDay(1)
                    val offset = dayIndex(first)
                    val total = ((offset + ym.lengthOfMonth() + 6) / 7) * 7
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (row in 0 until total / 7) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (col in 0..6) {
                                val n = row * 7 + col - offset + 1
                                if (n < 1 || n > ym.lengthOfMonth()) { Spacer(Modifier.weight(1f).height(46.dp)); continue }
                                val date = ym.atDay(n); val e = date.toEpochDay()
                                val lec = lecturesOf(date).count { !it.occ.cancelled }
                                val hasExam = exams.any { it.dateEpoch == e }
                                val hasTask = tasks.any { !it.done && it.dueEpoch == e }
                                val sel = e == selected
                                Column(
                                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(8.dp)).background(if (sel) Crimson else Surface1).clickable { selected = e },
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                ) {
                                    Text("$n", style = MaterialTheme.typography.labelLarge, color = if (sel) Color.White else if (date == LocalDate.now()) Crimson else TextMain)
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        if (lec > 0) Dot(if (sel) Color.White else Crimson)
                                        if (hasExam) Dot(if (sel) Color.White else GOLD)
                                        if (hasTask) Dot(if (sel) Color.White else Muted)
                                    }
                                }
                            }
                        }
                    }
                }
                item { MutedText("نقطة بلون التمييز: محاضرات  |  ذهبية: امتحان  |  رمادية: واجب") }
                item { DayDetails(LocalDate.ofEpochDay(selected), ::lecturesOf, exams, tasks, onOpenSlot) }
            } else {
                val today = LocalDate.now()
                var any = false
                for (off in 0..30) {
                    val d = today.plusDays(off.toLong()); val e = d.toEpochDay()
                    val has = lecturesOf(d).isNotEmpty() || exams.any { it.dateEpoch == e } || tasks.any { !it.done && it.dueEpoch == e }
                    if (!has) continue
                    any = true
                    item(key = e) { DayDetails(d, ::lecturesOf, exams, tasks, onOpenSlot) }
                }
                if (!any) item { MutedText("لا توجد أحداث خلال 30 يوما القادمة.") }
            }
        }
    }
}

private val MONTHS = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

@Composable
private fun Dot(c: Color) = Box(Modifier.size(5.dp).clip(CircleShape).background(c))

@Composable
private fun DayDetails(
    date: LocalDate, lecturesOf: (LocalDate) -> List<DayItem>, exams: List<com.schedule.app.data.Exam>,
    tasks: List<com.schedule.app.data.Task>, onOpenSlot: (Long) -> Unit,
) {
    val e = date.toEpochDay()
    AppCard {
        SectionTitle("${DAY_NAMES[dayIndex(date)]} ${formatDate(e)}")
        val items = lecturesOf(date)
        items.forEach { it ->
            Row(Modifier.fillMaxWidth().clickable { onOpenSlot(it.slot.id) }.padding(vertical = 3.dp)) {
                Text(formatMinutes(it.occ.start), Modifier.width(80.dp), color = if (it.occ.cancelled) Muted else Crimson, style = MaterialTheme.typography.bodyMedium)
                Text(it.slot.title + ((it.occ.room ?: it.slot.room).takeIf { r -> r.isNotBlank() }?.let { r -> " - $r" } ?: "") + if (it.occ.cancelled) " (ملغاة)" else "", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        exams.filter { it.dateEpoch == e }.forEach { Text("امتحان: ${it.title}", color = GOLD, style = MaterialTheme.typography.bodyMedium) }
        tasks.filter { !it.done && it.dueEpoch == e }.forEach { Text("تسليم: ${it.title}", style = MaterialTheme.typography.bodyMedium) }
        if (items.isEmpty() && exams.none { it.dateEpoch == e } && tasks.none { !it.done && it.dueEpoch == e }) MutedText("لا توجد أحداث.")
    }
}
