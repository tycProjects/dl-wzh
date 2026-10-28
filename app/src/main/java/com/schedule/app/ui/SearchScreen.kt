package com.schedule.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.ui.theme.*
import com.schedule.app.util.*

/** بحث شامل: المواد والمحاضر والقاعة، الملاحظات، أسماء المرفقات، والواجبات (مع توحيد الحروف العربية). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(vm: MainViewModel, onBack: () -> Unit, onOpenSlot: (Long) -> Unit) {
    val slots by vm.slots.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val files by vm.attachments.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var q by remember { mutableStateOf("") }
    val nq = normalizeArabic(q)
    fun has(s: String) = normalizeArabic(s).contains(nq)
    val titleOf = slots.associate { it.id to it.title }

    Scaffold(containerColor = Bg, topBar = {
        TopAppBar(
            title = {
                OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("ابحث في الجدول والملاحظات والمرفقات") },
                    trailingIcon = { if (q.isNotEmpty()) IconButton({ q = "" }) { Icon(Icons.Filled.Close, "مسح") } },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Crimson, unfocusedBorderColor = Border))
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
        )
    }) { pad ->
        val rSlots = if (nq.isEmpty()) emptyList() else slots.filter { has(it.title) || has(it.instructor) || has(it.room) }
        val rNotes = if (nq.isEmpty()) emptyList() else notes.filter { has(it.text) }
        val rFiles = if (nq.isEmpty()) emptyList() else files.filter { has(it.name) }
        val rTasks = if (nq.isEmpty()) emptyList() else tasks.filter { has(it.title) }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (nq.isEmpty()) item { MutedText("اكتب كلمة للبحث.") }
            else if (rSlots.isEmpty() && rNotes.isEmpty() && rFiles.isEmpty() && rTasks.isEmpty()) item { MutedText("لا توجد نتائج.") }
            if (rSlots.isNotEmpty()) item { SectionTitle("المحاضرات") }
            rSlots.forEach { s -> item(key = "s${s.id}") {
                AppCard(Modifier.clickable { onOpenSlot(s.id) }) {
                    Text(s.title); MutedText("${DAY_NAMES[s.day]} - ${formatRange(s.startMin, s.endMin)}" + (if (s.room.isNotBlank()) " - ${s.room}" else ""))
                } } }
            if (rNotes.isNotEmpty()) item { SectionTitle("الملاحظات") }
            rNotes.forEach { n -> item(key = "n${n.id}") {
                AppCard(Modifier.clickable { if (n.slotId > 0) onOpenSlot(n.slotId) }) {
                    Text(n.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    MutedText(if (n.slotId > 0) titleOf[n.slotId].orEmpty() else "ملاحظة يوم ${DAY_NAMES.getOrNull(n.day).orEmpty()}")
                } } }
            if (rFiles.isNotEmpty()) item { SectionTitle("المرفقات") }
            rFiles.forEach { a -> item(key = "f${a.id}") {
                AppCard(Modifier.clickable { onOpenSlot(a.slotId) }) { Text(a.name, maxLines = 2, overflow = TextOverflow.Ellipsis); MutedText(titleOf[a.slotId].orEmpty()) } } }
            if (rTasks.isNotEmpty()) item { SectionTitle("الواجبات") }
            rTasks.forEach { t -> item(key = "t${t.id}") {
                AppCard(Modifier.clickable { onOpenSlot(t.slotId) }) { Text(t.title); MutedText(titleOf[t.slotId].orEmpty()) } } }
        }
    }
}
