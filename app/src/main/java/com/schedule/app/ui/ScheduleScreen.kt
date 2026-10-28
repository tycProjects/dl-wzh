package com.schedule.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.data.Note
import com.schedule.app.data.Slot
import com.schedule.app.data.SlotException
import com.schedule.app.data.Holiday
import com.schedule.app.ui.theme.*
import com.schedule.app.util.*
import java.time.LocalDate
import java.time.LocalTime

/** محاضرة في يوم محدد بعد تطبيق الأسابيع والاستثناءات والإجازات. */
data class DayItem(val slot: Slot, val occ: Occurrence, val conflict: Boolean)

fun itemsOn(date: LocalDate, slots: List<Slot>, excs: List<SlotException>, holidays: List<Holiday>, sem: LocalDate): List<DayItem> {
    val hol = holidays.map { HolidayRange(it.startEpoch, it.endEpoch, it.title) }
    val list = slots.mapNotNull { s ->
        val ex = excs.filter { it.slotId == s.id }.map { ExcInfo(it.dateEpoch, it.cancelled, it.newStart, it.newEnd, it.newRoom) }
        occurrence(s.day, s.weekMode, s.startMin, s.endMin, date, sem, ex, hol, s.dateSet())?.let { s to it }
    }.sortedBy { it.second.start }
    return list.map { (s, o) ->
        val clash = !o.cancelled && list.any { (s2, o2) -> s2.id != s.id && !o2.cancelled && o.start < o2.end && o2.start < o.end }
        DayItem(s, o, clash)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    vm: MainViewModel, onOpenSlot: (Long) -> Unit, onProfile: () -> Unit, onSettings: () -> Unit,
    onSearch: () -> Unit, onTasks: () -> Unit, onBulkTransfer: () -> Unit, onGo: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val slots by vm.slots.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val attachments by vm.attachments.collectAsStateWithLifecycle()
    val excs by vm.exceptions.collectAsStateWithLifecycle()
    val holidays by vm.holidays.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sem = LocalDate.ofEpochDay(settings.semesterStart)
    val today = LocalDate.now()
    val todayIdx = dayIndex(today)
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }
    var day by rememberSaveable { mutableIntStateOf(todayIdx) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var gridMode by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Slot?>(null) }
    var adding by remember { mutableStateOf(false) }
    var noteDialog by remember { mutableStateOf<Note?>(null) }
    var addingNote by remember { mutableStateOf(false) }
    val wkStart = weekStart(today).plusWeeks(weekOffset.toLong())
    val date = wkStart.plusDays(day.toLong())
    val dayItems = itemsOn(date, slots, excs, holidays, sem)
    val dayNotes = notes.filter { it.slotId == 0L && it.day == day }.sortedByDescending { it.pinned }
    val tight = travelTight(dayItems.filter { !it.occ.cancelled }.map { TravelItem(it.slot.id, it.occ.start, it.occ.end, it.occ.room ?: it.slot.room) }, settings.travelMinutes)
    val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
    val holidayTitle = dayItems.firstOrNull { it.occ.holiday != null }?.occ?.holiday
        ?: holidays.firstOrNull { date.toEpochDay() in it.startEpoch..it.endEpoch }?.title

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Column { Text("الجدول"); Text("الأسبوع ${weekNumber(date, sem)}", style = MaterialTheme.typography.labelSmall, color = Muted) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
                actions = {
                    // زر واحد في الأعلى يُظهر/يخفي أزرار التعديل (القلم) في الجدول
                    TextButton({ editMode = !editMode }) { Text(if (editMode) "تم" else "تعديل", color = Crimson) }
                    IconButton(onSearch) { Icon(Icons.Filled.Search, "بحث") }
                    Box {
                        IconButton({ menuOpen = true }) { Icon(Icons.Filled.MoreVert, "المزيد") }
                        DropdownMenu(menuOpen, { menuOpen = false }) {
                            DropdownMenuItem({ Text(if (gridMode) "العرض اليومي" else "العرض الأسبوعي") }, { gridMode = !gridMode; menuOpen = false })
                            DropdownMenuItem({ Text("التقويم") }, { menuOpen = false; onGo("calendar") })
                            DropdownMenuItem({ Text("المساعد") }, { menuOpen = false; onGo("assistant") })
                            DropdownMenuItem({ Text("المذاكرة") }, { menuOpen = false; onGo("study") })
                            DropdownMenuItem({ Text("الدرجات والمعدل") }, { menuOpen = false; onGo("grades") })
                            DropdownMenuItem({ Text("الأدوات") }, { menuOpen = false; onGo("tools") })
                            DropdownMenuItem({ Text("الواجبات") }, { menuOpen = false; onTasks() })
                            DropdownMenuItem({ Text("نقل إلى مدير المحاضرات") }, { menuOpen = false; onBulkTransfer() })
                            DropdownMenuItem({ Text("مشاركة الجدول كصورة") }, { menuOpen = false; ctx.shareBitmap(renderScheduleBitmap(slots)) })
                            DropdownMenuItem({ Text("الملف الشخصي") }, { menuOpen = false; onProfile() })
                            DropdownMenuItem({ Text("الإعدادات") }, { menuOpen = false; onSettings() })
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (editMode) ExtendedFloatingActionButton({ adding = true }, containerColor = Crimson, contentColor = Color.White) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("إضافة محاضرة")
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad)) {
            // تنقل بين الأسابيع
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ weekOffset-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "الأسبوع السابق") }
                Text("${wkStart.dayOfMonth}/${wkStart.monthValue} - ${wkStart.plusDays(6).dayOfMonth}/${wkStart.plusDays(6).monthValue}",
                    Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                if (weekOffset != 0) TextButton({ weekOffset = 0; day = todayIdx }) { Text("اليوم", color = Crimson) }
                IconButton({ weekOffset++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "الأسبوع التالي") }
            }
            if (gridMode) WeekGrid(wkStart, slots, excs, holidays, sem, editMode, onOpenSlot, onEdit = { editing = it })
            else {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAY_NAMES.forEachIndexed { i, n ->
                        val sel = i == day
                        val d = wkStart.plusDays(i.toLong())
                        val isToday = d == today
                        Column(
                            Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) Crimson else Surface1).clickable { day = i }.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(n, style = MaterialTheme.typography.labelLarge, color = if (sel) Color.White else TextMain)
                            Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = if (sel) Color.White else if (isToday) Crimson else Muted)
                        }
                    }
                }
                LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (holidayTitle != null) item {
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CrimsonSoft).padding(12.dp)) { Text("إجازة: $holidayTitle", color = Crimson) }
                    }
                    if (dayItems.isEmpty()) item { MutedText("لا توجد محاضرات في هذا اليوم.", Modifier.padding(vertical = 12.dp)) }
                    items(dayItems, key = { it.slot.id }) { it ->
                        SlotCard(
                            it, live = date == today && !it.occ.cancelled && nowMin in it.occ.start until it.occ.end, editMode = editMode,
                            notesCount = notes.count { n -> n.slotId == it.slot.id }, filesCount = attachments.count { a -> a.slotId == it.slot.id }, tight = it.slot.id in tight,
                            onClick = { onOpenSlot(it.slot.id) }, onEdit = { editing = it.slot },
                        )
                    }
                    item {
                        AppCard(Modifier.padding(top = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { SectionTitle("ملاحظات ${DAY_NAMES[day]}") }
                                TextButton({ addingNote = true }) { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إضافة") }
                            }
                            if (dayNotes.isEmpty()) MutedText("لا توجد ملاحظات لهذا اليوم.")
                            dayNotes.forEach { n -> NoteRow(n) { noteDialog = n } }
                        }
                    }
                }
            }
        }
    }

    if (adding) SlotEditDialog(null, day, slots, onSave = vm::saveSlot, onDelete = null, onDismiss = { adding = false })
    editing?.let { s -> SlotEditDialog(s, day, slots, onSave = vm::saveSlot, onDelete = { vm.deleteSlot(s.id) }, onDismiss = { editing = null }) }
    if (addingNote) NoteDialog(null, 0, day, onSave = vm::saveNote, onDelete = null, onDismiss = { addingNote = false })
    noteDialog?.let { n -> NoteDialog(n, 0, day, onSave = vm::saveNote, onDelete = { vm.deleteNote(n.id) }, onDismiss = { noteDialog = null }) }
}

@Composable
private fun SlotCard(it: DayItem, live: Boolean, editMode: Boolean, notesCount: Int, filesCount: Int, tight: Boolean, onClick: () -> Unit, onEdit: () -> Unit) {
    val s = it.slot; val o = it.occ
    val accent = SLOT_COLORS[s.color.coerceIn(0, SLOT_COLORS.lastIndex)]
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (live) CrimsonSoft else Surface1),
        border = BorderStroke(1.dp, if (live) Crimson else Border),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(accent))
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.width(78.dp)) {
                    Text(formatMinutes(o.start), style = MaterialTheme.typography.titleSmall, color = if (o.cancelled) Muted else Crimson)
                    Text(formatMinutes(o.end), style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        textDecoration = if (o.cancelled) TextDecoration.LineThrough else null, color = if (o.cancelled) Muted else TextMain)
                    val room = o.room ?: s.room
                    val sub = listOf(s.instructor, room.takeIf { r -> r.isNotBlank() }?.let { r -> "قاعة $r" }).filterNotNull().filter { x -> x.isNotBlank() }.joinToString(" - ")
                    if (sub.isNotEmpty()) MutedText(sub)
                    val tags = buildList {
                        if (o.cancelled) add(if (o.holiday != null) "إجازة" else "ملغاة")
                        if (o.changed) add("تغيّر لهذا اليوم")
                        if (live) add("جارية الآن")
                        if (s.type != 0) add(com.schedule.app.data.SLOT_TYPE_LABELS[s.type.coerceIn(0, 2)])
                        if (s.weekMode != 0) add(WEEK_MODE_LABELS[s.weekMode.coerceIn(0, WEEK_MODE_LABELS.lastIndex)])
                        if (notesCount > 0) add("$notesCount ملاحظات")
                        if (filesCount > 0) add("$filesCount مرفقات")
                    }
                    if (tags.isNotEmpty()) Text(tags.joinToString("  |  "), style = MaterialTheme.typography.labelSmall, color = if (live || o.cancelled) Crimson else Muted)
                    if (s.bring.isNotBlank()) Text("أحضر: ${s.bring}", style = MaterialTheme.typography.labelSmall, color = Muted)
                    if (tight) Text("وقت التنقل بين القاعتين قصير", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    if (it.conflict) Text("تعارض مع محاضرة أخرى في نفس الوقت", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                if (editMode) IconButton(onEdit) { Icon(Icons.Filled.Edit, "تعديل", tint = Crimson) }
            }
        }
    }
}

@Composable
fun NoteRow(n: Note, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface2).clickable(onClick = onClick).padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (n.pinned) Text("مثبتة", style = MaterialTheme.typography.labelSmall, color = Crimson)
        RichText(n.text)
        val tags = parseTags(n.tags)
        if (tags.isNotEmpty()) Text(tags.joinToString("  ") { "#$it" }, style = MaterialTheme.typography.labelSmall, color = Muted)
        n.dateEpoch?.let { Text("التاريخ: ${formatDate(it)} - تذكير قبله بيوم", style = MaterialTheme.typography.labelSmall, color = Crimson) }
    }
}

/** عرض النص المنسق: # عنوان، - نقطة، - [ ] مهمة، **عريض**. */
@Composable
fun RichText(text: String) {
    parseRich(text).forEach { line ->
        val prefix = when (line.kind) { 2 -> "• "; 3 -> "[  ] "; 4 -> "[x] "; else -> "" }
        val str = androidx.compose.ui.text.buildAnnotatedString {
            append(prefix)
            line.spans.forEach { sp ->
                if (sp.bold) withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) { append(sp.text) } else append(sp.text)
            }
        }
        Text(str, style = if (line.kind == 1) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            textDecoration = if (line.kind == 4) TextDecoration.LineThrough else null, color = if (line.kind == 4) Muted else TextMain)
    }
}

/** عرض الأسبوع كاملا: أعمدة الأيام وصفوف الساعات، مع تكبير وتصغير. */
@Composable
private fun WeekGrid(
    wkStart: LocalDate, slots: List<Slot>, excs: List<SlotException>, holidays: List<Holiday>, sem: LocalDate,
    editMode: Boolean, onOpen: (Long) -> Unit, onEdit: (Slot) -> Unit,
) {
    var hourH by rememberSaveable { mutableIntStateOf(56) }
    val perDay = (0..6).map { itemsOn(wkStart.plusDays(it.toLong()), slots, excs, holidays, sem) }
    val all = perDay.flatten()
    val firstHour = ((all.minOfOrNull { it.occ.start } ?: 8 * 60) / 60).coerceAtMost(8)
    val lastHour = (((all.maxOfOrNull { it.occ.end } ?: 16 * 60) + 59) / 60).coerceAtLeast(firstHour + 4)
    val colW = 112.dp
    val hh: Dp = hourH.dp
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            MutedText("تكبير", Modifier.padding(end = 8.dp))
            IconButton({ hourH = (hourH - 8).coerceAtLeast(36) }) { Icon(Icons.Filled.Remove, "تصغير") }
            IconButton({ hourH = (hourH + 8).coerceAtMost(110) }) { Icon(Icons.Filled.Add, "تكبير") }
        }
        Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            // محور الساعات
            Column(Modifier.width(46.dp)) {
                Spacer(Modifier.height(34.dp))
                for (h in firstHour until lastHour) Box(Modifier.height(hh)) { Text(formatMinutes(h * 60).replace(":00", ""), style = MaterialTheme.typography.labelSmall, color = Muted) }
            }
            DAY_NAMES.forEachIndexed { i, n ->
                Column(Modifier.width(colW).padding(horizontal = 2.dp)) {
                    val d = wkStart.plusDays(i.toLong())
                    Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) {
                        Text("$n ${d.dayOfMonth}", style = MaterialTheme.typography.labelMedium, color = if (d == LocalDate.now()) Crimson else TextMain)
                    }
                    Box(Modifier.fillMaxWidth().height(hh * (lastHour - firstHour)).background(Surface1).border1()) {
                        for (h in 1 until (lastHour - firstHour)) Box(Modifier.fillMaxWidth().height(1.dp).offset(y = hh * h).background(Border))
                        perDay[i].forEach { it ->
                            val top = hh * ((it.occ.start - firstHour * 60) / 60f)
                            val height = (hh * ((it.occ.end - it.occ.start) / 60f)).coerceAtLeast(26.dp)
                            val c = SLOT_COLORS[it.slot.color.coerceIn(0, SLOT_COLORS.lastIndex)]
                            Column(
                                Modifier.offset(y = top).fillMaxWidth().height(height).padding(1.dp).clip(RoundedCornerShape(6.dp))
                                    .background(c.copy(alpha = if (it.occ.cancelled) 0.18f else 0.85f))
                                    .clickable { if (editMode) onEdit(it.slot) else onOpen(it.slot.id) }.padding(4.dp),
                            ) {
                                Text(it.slot.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (it.occ.cancelled) TextDecoration.LineThrough else null)
                                if (height > 44.dp) Text(formatMinutes(it.occ.start), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.border1(): Modifier = this.then(Modifier.clip(RoundedCornerShape(6.dp)))
