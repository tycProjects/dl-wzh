package com.lecturemanager.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturemanager.app.MainViewModel
import com.lecturemanager.app.ui.theme.*
import com.lecturemanager.app.work.BackupWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val lectures by vm.lectures.collectAsStateWithLifecycle()
    var pinSetup by remember { mutableStateOf(false) }
    var pinCheck by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refreshStorage() }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // نفعّل التذكير في كل الأحوال؛ إن رُفض الإذن لن تظهر الإشعارات حتى يُمنح من إعدادات النظام
        vm.updateSettings { copy(reminderEnabled = true) }
        if (!granted) vm.message.value = "فعّلت التذكير، لكن الإشعارات متوقفة من إعدادات الجهاز"
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { it?.let(vm::export) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { u -> pendingImport = u } }
    pendingImport?.let { uri ->
        ConfirmDialog("استيراد نسخة احتياطية", "سيتم استبدال جميع المواد والمحاضرات الحالية بمحتوى النسخة.",
            onConfirm = { vm.import(uri); pendingImport = null }, onDismiss = { pendingImport = null })
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ---- المظهر
        AppCard {
            SectionTitle("المظهر")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "حسب النظام", 1 to "فاتح", 2 to "داكن").forEach { (m, label) ->
                    FilterChip(
                        selected = s.themeMode == m, onClick = { vm.updateSettings { copy(themeMode = m) } }, label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyFill, selectedLabelColor = Color.White),
                    )
                }
            }
        }

        // ---- التذكير اليومي
        AppCard {
            SectionTitle("تذكير المذاكرة اليومي")
            SwitchRow("إشعار يومي بالمتبقي والامتحانات وبطاقات المراجعة", s.reminderEnabled) { on ->
                if (!on) vm.updateSettings { copy(reminderEnabled = false) }
                else if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else vm.updateSettings { copy(reminderEnabled = true) }
            }
            if (s.reminderEnabled) OutlinedButton(
                onClick = {
                    TimePickerDialog(ctx, { _, h, m -> vm.updateSettings { copy(reminderHour = h, reminderMinute = m) } }, s.reminderHour, s.reminderMinute, false).show()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Icon(Icons.Filled.Schedule, null); Spacer(Modifier.width(8.dp)); Text("وقت التذكير: %02d:%02d".format(s.reminderHour, s.reminderMinute)) }
        }

        // ---- بومودورو
        AppCard {
            SectionTitle("مؤقّت بومودورو")
            Stepper("مدة المذاكرة", s.pomodoroWork, 15, 60, 5) { v -> vm.updateSettings { copy(pomodoroWork = v) } }
            Stepper("مدة الاستراحة", s.pomodoroBreak, 3, 20, 1) { v -> vm.updateSettings { copy(pomodoroBreak = v) } }
        }

        // ---- الأمان
        AppCard {
            SectionTitle("الأمان")
            SwitchRow("قفل التطبيق برمز (4–6 أرقام)", s.lockEnabled) { on -> if (on) pinSetup = true else pinCheck = true }
            if (s.lockEnabled && canUseBiometric(ctx)) SwitchRow("فتح بالبصمة", s.biometric) { on -> vm.updateSettings { copy(biometric = on) } }
            if (s.lockEnabled) Text("يُقفل التطبيق بعد 30 ثانية من مغادرته.", style = MaterialTheme.typography.labelMedium, color = Slate)
        }

        // ---- النسخ الاحتياطي
        AppCard {
            SectionTitle("النسخ الاحتياطي")
            Text("نسخة تلقائية تُحفظ في مجلد التطبيق (آخر 3 نسخ):", style = MaterialTheme.typography.bodySmall, color = Slate)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "إيقاف", 1 to "يومياً", 2 to "أسبوعياً").forEach { (m, label) ->
                    FilterChip(
                        selected = s.autoBackup == m, onClick = { vm.updateSettings { copy(autoBackup = m) } }, label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyFill, selectedLabelColor = Color.White),
                    )
                }
            }
            val last = if (s.lastBackup > 0) SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(s.lastBackup)) else "لم تُنشأ بعد"
            Text("آخر نسخة: $last", style = MaterialTheme.typography.labelMedium, color = Slate)
            Text("المسار: ${BackupWorker.backupDir(ctx).absolutePath}", style = MaterialTheme.typography.labelSmall, color = SlateLight)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(vm::backupNow, Modifier.weight(1f)) { Text("نسخ الآن") }
                OutlinedButton({ exporter.launch("lecture-manager-backup.zip") }, Modifier.weight(1f)) { Text("تصدير") }
                OutlinedButton({ importer.launch(arrayOf("application/zip", "application/octet-stream")) }, Modifier.weight(1f)) { Text("استيراد") }
            }
        }

        // ---- الفهرسة
        AppCard {
            SectionTitle("فهرسة نصوص الملفات (للبحث)")
            val withPdf = lectures.count { it.pdfPath != null }
            val done = lectures.count { it.pdfPath != null && it.indexed }
            Text("تمت فهرسة $done من $withPdf ملف. تعمل الفهرسة في الخلفية، وتستخدم OCR للصفحات الممسوحة ضوئياً (عربي/إنجليزي).",
                style = MaterialTheme.typography.bodySmall, color = Slate)
            if (done < withPdf) OutlinedButton(onClick = vm::reindexAll, modifier = Modifier.fillMaxWidth()) { Text("تشغيل الفهرسة الآن") }
        }

        // ---- التخزين
        AppCard {
            SectionTitle("التخزين")
            Text("ملفات PDF: ${formatBytes(storage.pdfBytes)}", style = MaterialTheme.typography.bodyMedium, color = Navy)
            Text("قاعدة البيانات والملاحظات: ${formatBytes(storage.dbBytes)}", style = MaterialTheme.typography.bodyMedium, color = Navy)
            Text("الذاكرة المؤقتة: ${formatBytes(storage.cacheBytes)}", style = MaterialTheme.typography.bodyMedium, color = Navy)
            Text("الإجمالي: ${formatBytes(storage.total)}", style = MaterialTheme.typography.titleMedium, color = Teal)
            TextButton(onClick = vm::clearCache, modifier = Modifier.fillMaxWidth()) { Text("مسح الذاكرة المؤقتة") }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (pinSetup) PinSetupDialog(onSet = { vm.enableLock(it); pinSetup = false }, onDismiss = { pinSetup = false })
    if (pinCheck) PinCheckDialog(vm, onOk = { vm.disableLock(); pinCheck = false }, onDismiss = { pinCheck = false })
}

private var pendingImport by mutableStateOf<android.net.Uri?>(null)

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = Navy)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Navy)
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Teal))
    }
}

@Composable
private fun Stepper(label: String, value: Int, min: Int, max: Int, step: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Navy)
        IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min) { Icon(Icons.Filled.Remove, "أقل") }
        Text("$value د", style = MaterialTheme.typography.titleMedium, color = Navy)
        IconButton(onClick = { onChange((value + step).coerceAtMost(max)) }, enabled = value < max) { Icon(Icons.Filled.Add, "أكثر") }
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
                PinField(a, "الرمز (4–6 أرقام)") { a = it }
                PinField(b, "تأكيد الرمز") { b = it }
                if (b.isNotEmpty() && a != b) Text("الرمزان غير متطابقين", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Text("إن نسيت الرمز فلا توجد طريقة لاسترجاعه إلا بمسح بيانات التطبيق.", style = MaterialTheme.typography.labelMedium, color = Slate)
            }
        },
        confirmButton = { TextButton(enabled = ok, onClick = { onSet(a) }) { Text("تفعيل") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

@Composable
private fun PinField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, { onChange(it.filter(Char::isDigit).take(6)) }, singleLine = true, label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}
