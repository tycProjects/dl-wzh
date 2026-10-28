package com.schedule.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.ui.theme.*
import com.schedule.app.util.formatMinutes

@Composable
private fun ExtraSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Crimson))
    }
}

@Composable
private fun HourRow(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Muted)
        IconButton({ onChange((hour + 23) % 24) }) { Icon(Icons.Filled.Remove, "أقل") }
        Text(formatMinutes(hour * 60), style = MaterialTheme.typography.titleSmall)
        IconButton({ onChange((hour + 1) % 24) }) { Icon(Icons.Filled.Add, "أكثر") }
    }
}

/** إعدادات الميزات الإضافية: المظهر، الملخصات، التنقل، النسخ الخارجي. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtraSettings(vm: MainViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    val tree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching { ctx.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            vm.updateSettings { copy(extTree = it.toString()) }
        }
    }
    AppCard {
        SectionTitle("الألوان والخط")
        MutedText("لون التمييز")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ACCENTS.forEachIndexed { i, c ->
                Box(Modifier.size(32.dp).clip(CircleShape).background(c).border(if (s.accent == i) 3.dp else 0.dp, TextMain, CircleShape).clickable { vm.updateSettings { copy(accent = i) } })
            }
        }
        MutedText(ACCENT_NAMES[s.accent.coerceIn(0, ACCENT_NAMES.lastIndex)])
        ExtraSwitch("خط Cairo (إيقافه يستخدم خط النظام)", s.fontCairo) { vm.updateSettings { copy(fontCairo = it) } }
        ExtraSwitch("وضع داكن تلقائي من 6 مساء إلى 6 صباحا", s.autoDark) { vm.updateSettings { copy(autoDark = it) } }
    }
    AppCard {
        SectionTitle("الملخصات والتنبيهات الذكية")
        ExtraSwitch("ملخص صباحي بمحاضرات اليوم", s.morningSummary) { vm.updateSettings { copy(morningSummary = it) } }
        if (s.morningSummary) HourRow("وقت الملخص الصباحي", s.morningHour) { h -> vm.updateSettings { copy(morningHour = h) } }
        ExtraSwitch("ملخص مسائي بجدول الغد", s.eveningSummary) { vm.updateSettings { copy(eveningSummary = it) } }
        ExtraSwitch("تذكير مسائي إن نسيت تسجيل الحضور", s.forgotAttendance) { vm.updateSettings { copy(forgotAttendance = it) } }
        if (s.eveningSummary || s.forgotAttendance) HourRow("وقت الملخص المسائي", s.eveningHour) { h -> vm.updateSettings { copy(eveningHour = h) } }
        ExtraSwitch("تنبيه الامتحانات قبلها بـ7 و3 و1 أيام", s.examAlerts) { vm.updateSettings { copy(examAlerts = it) } }
        MutedText("يظهر ما تكتبه في خانة \"ما يجب إحضاره\" ضمن التنبيهات والملخصات. الإشعارات تصل تلقائيا للساعة الذكية المقترنة بالهاتف.")
    }
    AppCard {
        SectionTitle("زمن التنقل بين القاعات")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0 to "متوقف", 5 to "5 دقائق", 10 to "10 دقائق", 15 to "15 دقيقة", 20 to "20 دقيقة").forEach { (v, l) ->
                FilterChip(s.travelMinutes == v, { vm.updateSettings { copy(travelMinutes = v) } }, { Text(l) })
            }
        }
        MutedText("يظهر تحذير في الجدول إذا كان الفاصل بين محاضرتين في قاعتين مختلفتين أقل من هذه المدة.")
    }
    AppCard {
        SectionTitle("نسخة احتياطية خارجية")
        MutedText("اختر مجلدا (مثلا داخل Google Drive إن كان تطبيقه مثبتا) لتُنسخ إليه كل نسخة احتياطية جديدة. هذه نسخ احتياطي أحادي الاتجاه، وليست مزامنة بين الأجهزة.")
        MutedText(if (s.extTree.isBlank()) "لم يُحدد مجلد" else "المجلد محدد")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ tree.launch(null) }) { Text(if (s.extTree.isBlank()) "اختيار مجلد" else "تغيير المجلد") }
            if (s.extTree.isNotBlank()) TextButton({ vm.updateSettings { copy(extTree = "") } }) { Text("إيقاف") }
        }
    }
}
