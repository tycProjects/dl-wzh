package com.schedule.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.ui.theme.*
import com.schedule.app.work.BackupWorker
import com.schedule.app.work.Notifier
import com.schedule.app.work.Scheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onHolidays: () -> Unit) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val csv by vm.csvPreview.collectAsStateWithLifecycle()
    var notifOk by remember { mutableStateOf(Notifier.canPost(ctx)) }
    var exactOk by remember { mutableStateOf(Scheduler.canExact(ctx)) }
    var confirmImport by remember { mutableStateOf<Uri?>(null) }
    var pinSetup by remember { mutableStateOf(false) }
    var pinCheck by remember { mutableStateOf(false) }
    val owner = LocalLifecycleOwner.current
    // نعيد فحص الأذونات عند العودة من إعدادات النظام
    DisposableEffect(owner) {
        val ob = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { notifOk = Notifier.canPost(ctx); exactOk = Scheduler.canExact(ctx) } }
        owner.lifecycle.addObserver(ob); onDispose { owner.lifecycle.removeObserver(ob) }
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(vm::exportTo) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { confirmImport = it }
    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::previewCsv) }
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("الإعدادات") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppCard {
                SectionTitle("المظهر")
                SwitchRow("الوضع الفاتح", s.lightTheme) { vm.updateSettings { copy(lightTheme = it) } }
            }
            AppCard {
                SectionTitle("إشعارات المحاضرات")
                MutedText("تنبيه قبل البداية")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "متوقف", 5 to "5 دقائق", 10 to "10 دقائق", 15 to "15 دقيقة", 30 to "30 دقيقة").forEach { (v, l) ->
                        FilterChip(s.leadMinutes == v, { vm.updateSettings { copy(leadMinutes = v) } }, { Text(l) })
                    }
                }
                SwitchRow("إشعار عند بداية المحاضرة", s.notifyStart) { vm.updateSettings { copy(notifyStart = it) } }
                SwitchRow("سؤال: هل تريد تسجيل المحاضرة؟", s.askRecord && s.notifyStart, enabled = s.notifyStart) { vm.updateSettings { copy(askRecord = it) } }
                SwitchRow("إشعار عند نهاية المحاضرة (مع تسجيل الحضور)", s.notifyEnd) { vm.updateSettings { copy(notifyEnd = it) } }
                SwitchRow("تذكير قبل تاريخ الملاحظة أو الواجب بيوم", s.noteReminder) { vm.updateSettings { copy(noteReminder = it) } }
                SwitchRow("تحويل الهاتف للاهتزاز أثناء المحاضرة", s.silentMode) { vm.updateSettings { copy(silentMode = it) } }
                SwitchRow("إشعار دائم بالمحاضرة القادمة مع عدّ تنازلي", s.ongoing) { vm.updateSettings { copy(ongoing = it) } }
            }
            AppCard {
                SectionTitle("الفصل الدراسي")
                OutlinedButton({ pickDate(ctx, s.semesterStart) { d -> vm.updateSettings { copy(semesterStart = d) } } }) { Text("بداية الأسبوع الأول: ${formatDate(s.semesterStart)}") }
                MutedText("يُستخدم لحساب رقم الأسبوع والأسابيع الفردية والزوجية.")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("حد الغياب المسموح لكل مادة", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton({ vm.updateSettings { copy(absenceLimit = (absenceLimit - 1).coerceAtLeast(0)) } }) { Icon(Icons.Filled.Remove, "أقل") }
                    Text(if (s.absenceLimit == 0) "متوقف" else "${s.absenceLimit}", style = MaterialTheme.typography.titleMedium)
                    IconButton({ vm.updateSettings { copy(absenceLimit = (absenceLimit + 1).coerceAtMost(20)) } }) { Icon(Icons.Filled.Add, "أكثر") }
                }
                OutlinedButton(onHolidays, Modifier.fillMaxWidth()) { Text("إدارة الإجازات وأسابيع الامتحانات") }
            }
            AppCard {
                SectionTitle("الأذونات")
                if (notifOk) MutedText("إذن الإشعارات: مفعّل") else {
                    MutedText("إذن الإشعارات غير مفعّل، ولن تصلك التنبيهات.")
                    Button({ if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) else ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("تفعيل الإشعارات") }
                }
                if (exactOk) MutedText("المنبهات الدقيقة: مفعّلة") else {
                    MutedText("المنبهات الدقيقة غير مفعّلة، وقد تتأخر الإشعارات بضع دقائق.")
                    if (Build.VERSION.SDK_INT >= 31) Button({ ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))) },
                        colors = ButtonDefaults.buttonColors(containerColor = Crimson)) { Text("السماح بالمنبهات الدقيقة") }
                }
            }
            AppCard {
                SectionTitle("القفل")
                SwitchRow("قفل التطبيق برمز (4 إلى 6 أرقام)", s.lockEnabled) { on -> if (on) pinSetup = true else pinCheck = true }
                if (s.lockEnabled && canUseBiometric(ctx)) SwitchRow("فتح بالبصمة", s.biometric) { vm.updateSettings { copy(biometric = it) } }
                if (s.lockEnabled) MutedText("يُقفل التطبيق بعد 30 ثانية من مغادرته.")
            }
            AppCard {
                SectionTitle("النسخ الاحتياطي")
                MutedText("يشمل الجدول والملاحظات والمرفقات والحضور والواجبات والإجازات والامتحانات والملف الشخصي.")
                MutedText("نسخة تلقائية (آخر 3 نسخ)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "إيقاف", 1 to "يوميا", 2 to "أسبوعيا").forEach { (m, l) -> FilterChip(s.autoBackup == m, { vm.updateSettings { copy(autoBackup = m) } }, { Text(l) }) }
                }
                val last = if (s.lastBackup > 0) SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(s.lastBackup)) else "لم تُنشأ بعد"
                MutedText("آخر نسخة: $last")
                MutedText("المسار: ${BackupWorker.backupDir(ctx).absolutePath}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(vm::backupNow, Modifier.weight(1f)) { Text("نسخ الآن") }
                    OutlinedButton({ exporter.launch("schedule-backup.zip") }, Modifier.weight(1f)) { Text("تصدير") }
                    OutlinedButton({ importer.launch(arrayOf("application/zip", "application/octet-stream")) }, Modifier.weight(1f)) { Text("استيراد") }
                }
            }
            AppCard {
                SectionTitle("استيراد ومشاركة الجدول")
                MutedText("استورد جدولك من ملف CSV أو نص (احفظ ملف Excel بصيغة CSV). الأعمدة: المادة، اليوم، من، إلى، المحاضر، القاعة، الأسابيع (اختياري: فردي أو زوجي). تُضاف المحاضرات دون حذف الموجود.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ csvPicker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) }, Modifier.weight(1f)) { Text("استيراد CSV") }
                    OutlinedButton({ scope.launchShare(ctx, vm) }, Modifier.weight(1f)) { Text("مشاركة كملف") }
                }
            }
            AppCard {
                SectionTitle("الودجت ومدير المحاضرات")
                MutedText("أضف ودجت \"منظم الجدول\" من قائمة الودجت في الشاشة الرئيسية لعرض محاضرات اليوم.")
                MutedText(if (isLectureManagerInstalled(ctx)) "تطبيق مدير المحاضرات مثبّت. استخدم زر النقل داخل أي محاضرة أو من قائمة الجدول." else "تطبيق مدير المحاضرات غير مثبّت. ثبّته لتتمكن من نقل المحاضرات والملاحظات إليه.")
            }
        }
    }
    confirmImport?.let { u -> ConfirmDialog("استيراد نسخة احتياطية", "سيتم استبدال كل بيانات التطبيق الحالية بمحتوى النسخة.", confirm = "استيراد",
        onConfirm = { vm.importFrom(u); confirmImport = null }, onDismiss = { confirmImport = null }) }
    csv?.let { r ->
        AlertDialog(
            onDismissRequest = vm::dismissCsv,
            title = { Text("معاينة الاستيراد") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("سيتم إضافة ${r.slots.size} محاضرة.")
                    r.slots.take(6).forEach { MutedText("${it.title} - ${com.schedule.app.util.DAY_NAMES[it.day]} - ${com.schedule.app.util.formatRange(it.startMin, it.endMin)}") }
                    if (r.slots.size > 6) MutedText("و${r.slots.size - 6} أخرى...")
                    if (r.errors.isNotEmpty()) { Text("أسطر تم تجاهلها (${r.errors.size}):", color = MaterialTheme.colorScheme.error); r.errors.take(5).forEach { MutedText(it) } }
                }
            },
            confirmButton = { TextButton(enabled = r.slots.isNotEmpty(), onClick = vm::confirmCsv) { Text("إضافة") } },
            dismissButton = { TextButton(vm::dismissCsv) { Text("إلغاء") } },
        )
    }
    if (pinSetup) PinSetupDialog(onSet = { vm.enableLock(it); pinSetup = false }, onDismiss = { pinSetup = false })
    if (pinCheck) PinCheckDialog(vm, onOk = { vm.disableLock(); pinCheck = false }, onDismiss = { pinCheck = false })
}

private fun kotlinx.coroutines.CoroutineScope.launchShare(ctx: android.content.Context, vm: MainViewModel) {
    launch { ctx.shareCsv(vm.scheduleCsv()) }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onChange, enabled = enabled, colors = SwitchDefaults.colors(checkedTrackColor = Crimson))
    }
}

@Composable
private fun PinSetupDialog(onSet: (String) -> Unit, onDismiss: () -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val ok = a.length in 4..6 && a == b
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعيين رمز القفل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField(a, "الرمز (4 إلى 6 أرقام)") { a = it }
                PinField(b, "تأكيد الرمز") { b = it }
                if (b.isNotEmpty() && a != b) Text("الرمزان غير متطابقين", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                MutedText("إن نسيت الرمز فلا توجد طريقة لاسترجاعه إلا بمسح بيانات التطبيق.")
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onSet(a) }) { Text("تفعيل") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun PinCheckDialog(vm: MainViewModel, onOk: () -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إيقاف القفل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField(pin, "أدخل الرمز الحالي") { pin = it; bad = false }
                if (bad) Text("الرمز غير صحيح", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(enabled = pin.length >= 4, onClick = { if (vm.checkPin(pin)) onOk() else bad = true }) { Text("إيقاف") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun PinField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(it.filter(Char::isDigit).take(6)) }, singleLine = true, label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
}
