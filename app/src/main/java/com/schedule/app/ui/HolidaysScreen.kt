package com.schedule.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.data.Holiday
import com.schedule.app.ui.theme.*
import java.time.LocalDate

/** الإجازات وأسابيع الامتحانات: لا تصل أي إشعارات للمحاضرات خلال هذه الفترات. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaysScreen(vm: MainViewModel, onBack: () -> Unit) {
    val list by vm.holidays.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Holiday?>(null) }
    var adding by remember { mutableStateOf(false) }
    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("الإجازات") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }, floatingActionButton = {
        ExtendedFloatingActionButton({ adding = true }, containerColor = Crimson, contentColor = Color.White) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("إضافة إجازة") }
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (list.isEmpty()) item { MutedText("أضف فترات الإجازة أو أسابيع الامتحانات لإيقاف الإشعارات وإخفاء المحاضرات فيها.") }
            items(list, key = { it.id }) { h ->
                AppCard(Modifier.clickable { editing = h }) {
                    Text(h.title, style = MaterialTheme.typography.titleMedium)
                    MutedText("من ${formatDate(h.startEpoch)} إلى ${formatDate(h.endEpoch)}")
                }
            }
        }
    }
    if (adding) HolidayDialog(null, onSave = vm::saveHoliday, onDelete = null, onDismiss = { adding = false })
    editing?.let { h -> HolidayDialog(h, onSave = vm::saveHoliday, onDelete = { vm.deleteHoliday(h.id) }, onDismiss = { editing = null }) }
}

@Composable
private fun HolidayDialog(initial: Holiday?, onSave: (Holiday) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var start by remember { mutableStateOf(initial?.startEpoch ?: LocalDate.now().toEpochDay()) }
    var end by remember { mutableStateOf(initial?.endEpoch ?: LocalDate.now().plusDays(6).toEpochDay()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة إجازة" else "تعديل الإجازة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppField(title, "الاسم (مثال: إجازة منتصف الفصل)") { title = it }
                OutlinedButton({ pickDate(ctx, start) { start = it; if (end < it) end = it } }) { Text("من: ${formatDate(start)}") }
                OutlinedButton({ pickDate(ctx, end) { end = it } }) { Text("إلى: ${formatDate(end)}") }
                if (end < start) Text("تاريخ النهاية قبل البداية", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (onDelete != null) TextButton({ onDelete(); onDismiss() }) { Text("حذف الإجازة", color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank() && end >= start, onClick = { onSave(Holiday(id = initial?.id ?: 0, title = title.trim(), startEpoch = start, endEpoch = end)); onDismiss() }) { Text("حفظ") } },
        dismissButton = { TextButton(onDismiss) { Text("إلغاء") } },
    )
}
