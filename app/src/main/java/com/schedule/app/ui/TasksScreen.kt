package com.schedule.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.data.Task
import com.schedule.app.ui.theme.*
import java.time.LocalDate

/** كل الواجبات من كل المواد: المتأخرة والقادمة أولا. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(vm: MainViewModel, onBack: () -> Unit, onOpenSlot: (Long) -> Unit) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val slots by vm.slots.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Task?>(null) }
    val today = LocalDate.now().toEpochDay()
    val pending = tasks.filter { !it.done }
    val done = tasks.filter { it.done }
    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(title = { Text("الواجبات") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } })
    }) { pad ->
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tasks.isEmpty()) item { MutedText("لا توجد واجبات. أضف واجبا من داخل أي محاضرة.") }
            if (pending.isNotEmpty()) item { SectionTitle("المطلوبة (${pending.size})") }
            items(pending, key = { it.id }) { t -> TaskRow(t, slots.firstOrNull { it.id == t.slotId }?.title, today, { vm.toggleTask(t) }, { editing = t }) }
            if (done.isNotEmpty()) item { SectionTitle("المنجزة (${done.size})") }
            items(done, key = { it.id }) { t -> TaskRow(t, slots.firstOrNull { it.id == t.slotId }?.title, today, { vm.toggleTask(t) }, { editing = t }) }
        }
    }
    editing?.let { t -> TaskDialog(t, t.slotId, onSave = vm::saveTask, onDelete = { vm.deleteTask(t.id) }, onDismiss = { editing = null }) }
}

@Composable
private fun TaskRow(t: Task, course: String?, today: Long, onToggle: () -> Unit, onEdit: () -> Unit) {
    AppCard(Modifier.clickable(onClick = onEdit)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(t.done, { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(t.title, textDecoration = if (t.done) TextDecoration.LineThrough else null, color = if (t.done) Muted else TextMain)
                val sub = listOfNotNull(course, t.dueEpoch?.let { "التسليم: ${formatDate(it)}" }).joinToString("  |  ")
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.labelSmall,
                    color = if (!t.done && t.dueEpoch != null && t.dueEpoch < today) MaterialTheme.colorScheme.error else Muted)
            }
        }
    }
}
