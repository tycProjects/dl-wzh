package com.schedule.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.data.RoomInfo
import com.schedule.app.data.ScheduleInfo
import com.schedule.app.ui.theme.*
import com.schedule.app.util.*
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text(title) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { pad -> Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content) }
}

@Composable
private fun ToolRow(title: String, desc: String, onClick: () -> Unit) {
    AppCard(Modifier.clickable(onClick = onClick)) { Text(title, style = MaterialTheme.typography.titleMedium); MutedText(desc) }
}

/** مركز الأدوات: كل الميزات الإضافية في مكان واحد. */
@Composable
fun ToolsScreen(vm: MainViewModel, onBack: () -> Unit, go: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val calPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) vm.importDeviceCalendar() else ctx.toast("لم يُمنح إذن قراءة التقويم") }
    ToolScaffold("الأدوات", onBack) {
        ToolRow("المساعد", "أضف وعدّل واسأل بالأوامر النصية أو الصوتية") { go("assistant") }
        ToolRow("التقويم", "عرض شهري وأجندة للمحاضرات والامتحانات والواجبات") { go("calendar") }
        ToolRow("المذاكرة", "مؤقت لكل مادة، إحصائيات، هدف أسبوعي، خطة قبل الامتحانات، فترات الفراغ") { go("study") }
        ToolRow("الدرجات والمعدل", "درجات كل مادة، المعدل المتوقع، وكم تحتاج في النهائي") { go("grades") }
        ToolRow("الجداول المتعددة", "فصول أو جداول منفصلة مع تبديل سريع") { go("schedules") }
        ToolRow("قوالب جاهزة", "أضف جدولا نموذجيا ثم عدّل الأسماء") { go("templates") }
        ToolRow("مقارنة مع زميل", "استورد جدول زميل (CSV) واعرف أوقات الفراغ المشتركة") { go("compare") }
        ToolRow("القاعات", "وصف الوصول وفتح الموقع في الخرائط") { go("rooms") }
        ToolRow("مسح مستند بالكاميرا", "تصوير وتحسين السبورة والأوراق واستخراج النص الإنجليزي") { go("scan") }
        ToolRow("رسم وكتابة بخط اليد", "ارسم ملاحظة واحفظها كصورة في مرفقات المادة") { go("draw") }
        ToolRow("تصدير إلى تقويم الهاتف (ICS)", "ملف يفتحه تقويم جوجل أو أي تقويم؛ يشمل المحاضرات 18 أسبوعا والامتحانات والواجبات") {
            scope.launchIcs(ctx, vm)
        }
        ToolRow("استيراد من تقويم الهاتف", "يقرأ أحداث الـ60 يوما القادمة من التقويمات المزامنة على الجهاز (ومنها جوجل) ويضيفها كواجبات") {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED) vm.importDeviceCalendar()
            else calPerm.launch(Manifest.permission.READ_CALENDAR)
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchIcs(ctx: android.content.Context, vm: MainViewModel) {
    launch {
        val text = vm.icsText()
        val dir = File(ctx.cacheDir, "shared").also { it.mkdirs() }
        val f = File(dir, "schedule.ics").also { it.writeText(text) }
        val i = Intent(Intent.ACTION_SEND).setType("text/calendar").putExtra(Intent.EXTRA_STREAM, fileUri(ctx, f.absolutePath)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { ctx.startActivity(Intent.createChooser(i, "تصدير الجدول").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
    }
}

@Composable
fun SchedulesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val list by vm.schedules.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()
    val all by vm.allSlots.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ScheduleInfo?>(null) }
    var deleting by remember { mutableStateOf<ScheduleInfo?>(null) }
    ToolScaffold("الجداول المتعددة", onBack) {
        MutedText("لكل جدول محاضراته وتنبيهاته؛ المفعّل فقط هو الذي تصلك تنبيهاته ويظهر في الودجت.")
        list.forEach { sc ->
            AppCard {
                Text(sc.name + if (sc.id == s.activeSchedule) "  (المفعّل)" else "", style = MaterialTheme.typography.titleMedium, color = if (sc.id == s.activeSchedule) Crimson else TextMain)
                MutedText("${all.count { it.scheduleId == sc.id }} محاضرة")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sc.id != s.activeSchedule) Button({ vm.switchSchedule(sc.id) }, colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("تفعيل") }
                    OutlinedButton({ renaming = sc }) { Text("إعادة تسمية") }
                    if (sc.id != 1L) TextButton({ deleting = sc }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        Button({ adding = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("جدول جديد") }
    }
    if (adding) NameDialog("جدول جديد", "") { if (it.isNotBlank()) vm.addSchedule(it); adding = false }
    renaming?.let { r -> NameDialog("إعادة تسمية", r.name) { if (it.isNotBlank()) vm.renameSchedule(r.copy(name = it)); renaming = null } }
    deleting?.let { d -> ConfirmDialog("حذف الجدول", "سيُحذف \"${d.name}\" بكل محاضراته وملاحظاته ومرفقاته.", onConfirm = { vm.deleteSchedule(d.id); deleting = null }, onDismiss = { deleting = null }) }
}

@Composable
private fun NameDialog(title: String, initial: String, onOk: (String) -> Unit) {
    var t by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = { onOk("") }, title = { Text(title) }, text = { AppField(t, "الاسم") { t = it } },
        confirmButton = { TextButton(enabled = t.isNotBlank(), onClick = { onOk(t.trim()) }) { Text("حفظ") } },
        dismissButton = { TextButton({ onOk("") }) { Text("إلغاء") } })
}

@Composable
fun TemplatesScreen(vm: MainViewModel, onBack: () -> Unit) {
    ToolScaffold("قوالب جاهزة", onBack) {
        MutedText("تُضاف محاضرات القالب إلى الجدول المفعّل دون حذف الموجود. أسماء المواد عامة، عدّلها بعد الإضافة.")
        SCHEDULE_TEMPLATES.forEach { t ->
            AppCard {
                Text(t.name, style = MaterialTheme.typography.titleMedium); MutedText(t.description)
                OutlinedButton({ vm.addTemplate(t) }) { Text("إضافة القالب") }
            }
        }
    }
}

@Composable
fun CompareScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var result by remember { mutableStateOf<Map<Int, List<Pair<Int, Int>>>?>(null) }
    var count by remember { mutableIntStateOf(0) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val text = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            val r = parseScheduleCsv(text)
            count = r.slots.size
            result = commonFree(vm.mySpans(), r.slots.map { DaySpan(it.day, it.startMin, it.endMin) })
        }.onFailure { ctx.toast("تعذّرت قراءة الملف") }
    }
    ToolScaffold("مقارنة مع زميل", onBack) {
        MutedText("اطلب من زميلك مشاركة جدوله كملف CSV (من الإعدادات: مشاركة كملف) ثم اختره هنا. يُحسب الفراغ المشترك من 8 صباحا إلى 6 مساء ولا تقل الفترة عن 45 دقيقة.")
        Button({ picker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) }, colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("اختيار جدول الزميل") }
        result?.let { r ->
            AppCard {
                SectionTitle("الفراغ المشترك (جدول زميلك: $count محاضرة)")
                if (r.isEmpty()) MutedText("لا توجد فترات مشتركة.")
                r.forEach { (d, g) -> Text("${DAY_NAMES[d]}: " + g.joinToString("  |  ") { formatRange(it.first, it.second) }, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
fun RoomsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val slots by vm.slots.collectAsStateWithLifecycle()
    val saved by vm.rooms.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf<RoomInfo?>(null) }
    val names = (slots.map { it.room.trim() }.filter { it.isNotEmpty() } + saved.map { it.name }).distinct().sorted()
    ToolScaffold("القاعات", onBack) {
        MutedText("أضف وصفا للوصول (المبنى والدور) وإحداثيات اختيارية. زر الخريطة يفتح تطبيق الخرائط عندك.")
        if (names.isEmpty()) MutedText("لا توجد قاعات. اكتب القاعة في المحاضرات لتظهر هنا.")
        names.forEach { n ->
            val info = saved.firstOrNull { it.name == n } ?: RoomInfo(n)
            AppCard {
                Text(n, style = MaterialTheme.typography.titleMedium)
                if (info.description.isNotBlank()) MutedText(info.description)
                if (info.lat != null && info.lng != null) MutedText("الإحداثيات: ${info.lat}, ${info.lng}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ edit = info }) { Text("تعديل") }
                    OutlinedButton({ openMap(ctx, info) }) { Text("الخريطة") }
                }
            }
        }
    }
    edit?.let { r ->
        var desc by remember(r) { mutableStateOf(r.description) }
        var lat by remember(r) { mutableStateOf(r.lat?.toString().orEmpty()) }
        var lng by remember(r) { mutableStateOf(r.lng?.toString().orEmpty()) }
        AlertDialog(onDismissRequest = { edit = null }, title = { Text(r.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppField(desc, "وصف الوصول", singleLine = false) { desc = it }
                    AppField(lat, "خط العرض (اختياري)") { lat = it }
                    AppField(lng, "خط الطول (اختياري)") { lng = it }
                    MutedText("يمكنك نسخ الإحداثيات من تطبيق الخرائط بالضغط المطول على المكان.")
                }
            },
            confirmButton = { TextButton({ vm.saveRoom(RoomInfo(r.name, desc.trim(), lat.toDoubleOrNull(), lng.toDoubleOrNull())); edit = null }) { Text("حفظ") } },
            dismissButton = { TextButton({ edit = null }) { Text("إلغاء") } })
    }
}

private fun openMap(ctx: android.content.Context, r: RoomInfo) {
    val label = Uri.encode(r.name)
    val uri = if (r.lat != null && r.lng != null) Uri.parse("geo:${r.lat},${r.lng}?q=${r.lat},${r.lng}($label)") else Uri.parse("geo:0,0?q=" + Uri.encode((r.name + " " + r.description).trim()))
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.onFailure { ctx.toast("لا يوجد تطبيق خرائط") }
}
