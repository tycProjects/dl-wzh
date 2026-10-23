package com.lecturemanager.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturemanager.app.MainViewModel
import com.lecturemanager.app.data.Flashcard
import com.lecturemanager.app.data.Subject
import com.lecturemanager.app.data.todayEpochDay
import com.lecturemanager.app.ui.theme.*
import kotlinx.coroutines.delay

/** تبويب البطاقات: ملخص المستحق، قائمة المواد، إضافة وإدارة البطاقات. */
@Composable
fun CardsScreen(vm: MainViewModel, onReview: (Long) -> Unit) {
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val cards by vm.flashcards.collectAsStateWithLifecycle()
    val today = todayEpochDay()
    var addOpen by remember { mutableStateOf(false) }
    var manage by remember { mutableStateOf<Subject?>(null) }
    val dueTotal = cards.count { it.dueDay <= today }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            AppCard {
                Text("مراجعة البطاقات", style = MaterialTheme.typography.titleLarge, color = Navy)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (cards.isEmpty()) "لا توجد بطاقات بعد. أنشئها من ملاحظات المحاضرة أو أضفها يدوياً."
                    else "$dueTotal بطاقة مستحقة اليوم من ${cards.size}",
                    style = MaterialTheme.typography.bodyMedium, color = Slate,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onReview(0) }, enabled = dueTotal > 0,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
                    ) { Text("ابدأ المراجعة") }
                    OutlinedButton(
                        onClick = { addOpen = true }, enabled = subjects.isNotEmpty(),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("بطاقة جديدة") }
                }
            }
        }
        items(subjects, key = { it.id }) { s ->
            val mine = cards.filter { it.subjectId == s.id }
            if (mine.isNotEmpty()) {
                val due = mine.count { it.dueDay <= today }
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.icon, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleMedium, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${mine.size} بطاقة", style = MaterialTheme.typography.labelMedium, color = Slate)
                        }
                        if (due > 0) Badge("$due مستحقة", AmberTint, AmberText) else Badge("مكتملة ✓", TealTint, TealText)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onReview(s.id) }, enabled = due > 0, modifier = Modifier.weight(1f)) { Text("مراجعة") }
                        OutlinedButton(onClick = { manage = s }, modifier = Modifier.weight(1f)) { Text("إدارة البطاقات") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (addOpen) CardEditorDialog(
        subjects = subjects, initial = null,
        onSave = { sid, f, b -> vm.addCard(sid, f, b); addOpen = false },
        onDelete = null, onDismiss = { addOpen = false },
    )
    manage?.let { s -> ManageCardsDialog(vm, s, cards.filter { it.subjectId == s.id }, subjects) { manage = null } }
}

@Composable
private fun ManageCardsDialog(vm: MainViewModel, s: Subject, cards: List<Flashcard>, subjects: List<Subject>, onDismiss: () -> Unit) {
    var editing by remember { mutableStateOf<Flashcard?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بطاقات ${s.title}") },
        text = {
            if (cards.isEmpty()) Text("لا توجد بطاقات.", color = Slate)
            else LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(cards, key = { it.id }) { c ->
                    Column(Modifier.fillMaxWidth().clickable { editing = c }.padding(vertical = 8.dp)) {
                        Text(c.front, style = MaterialTheme.typography.titleMedium, color = Navy, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(c.back, style = MaterialTheme.typography.bodySmall, color = Slate, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    HorizontalDivider(color = Border)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
    editing?.let { c ->
        CardEditorDialog(
            subjects = subjects, initial = c,
            onSave = { _, f, b -> vm.updateCard(c.copy(front = f, back = b)); editing = null },
            onDelete = { vm.deleteCard(c.id); editing = null },
            onDismiss = { editing = null },
        )
    }
}

/** إضافة بطاقة (initial = null) أو تعديلها وحذفها. */
@Composable
private fun CardEditorDialog(
    subjects: List<Subject>, initial: Flashcard?,
    onSave: (subjectId: Long, front: String, back: String) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit,
) {
    var sid by remember { mutableStateOf(initial?.subjectId ?: subjects.firstOrNull()?.id ?: 0L) }
    var front by remember { mutableStateOf(initial?.front ?: "") }
    var back by remember { mutableStateOf(initial?.back ?: "") }
    var menu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "بطاقة جديدة" else "تعديل البطاقة") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (initial == null) Box {
                    OutlinedButton(onClick = { menu = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, InputBorder)) {
                        Text(subjects.firstOrNull { it.id == sid }?.title ?: "اختر المادة", color = Navy, modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ArrowDropDown, null, tint = Slate)
                    }
                    DropdownMenu(menu, { menu = false }) {
                        subjects.forEach { s -> DropdownMenuItem(text = { Text("${s.icon} ${s.title}") }, onClick = { sid = s.id; menu = false }) }
                    }
                }
                AppField(front, { front = it }, "السؤال / المصطلح", minLines = 2)
                AppField(back, { back = it }, "الجواب / التعريف", minLines = 3)
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp)); Text("حذف البطاقة", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = front.isNotBlank() && back.isNotBlank() && sid != 0L, onClick = { onSave(sid, front.trim(), back.trim()) }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

/** جلسة مراجعة: subjectId = 0 يعني كل المواد. الأخطاء تعود لآخر الطابور في نفس الجلسة. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(vm: MainViewModel, subjectId: Long, onBack: () -> Unit) {
    val cards by vm.flashcards.collectAsStateWithLifecycle()
    val queue = remember { mutableStateListOf<Flashcard>() }
    var initialized by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var revealed by remember { mutableStateOf(false) }
    var reviewed by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(cards) {
        if (!initialized) {
            val today = todayEpochDay()
            val due = cards.filter { it.dueDay <= today && (subjectId == 0L || it.subjectId == subjectId) }
            if (due.isNotEmpty()) { queue.addAll(due.shuffled()); initialized = true; loaded = true }
        }
    }
    LaunchedEffect(Unit) { delay(700); loaded = true }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("مراجعة البطاقات", color = Navy) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas),
        )
        val current = queue.firstOrNull()
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (current != null) {
                Text("المتبقية: ${queue.size}", style = MaterialTheme.typography.labelLarge, color = Slate)
                AppCard(Modifier.weight(1f).clickable { revealed = true }) {
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            Modifier.align(Alignment.Center).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(current.front, style = MaterialTheme.typography.headlineMedium, color = Navy, textAlign = TextAlign.Center)
                            if (revealed) {
                                HorizontalDivider(color = Border)
                                Text(current.back, style = MaterialTheme.typography.bodyLarge, color = Navy, textAlign = TextAlign.Center)
                            } else Text("اضغط لإظهار الإجابة", style = MaterialTheme.typography.labelMedium, color = SlateLight)
                        }
                    }
                }
                if (revealed) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            vm.reviewCard(current, false); reviewed++
                            queue.removeAt(0); queue.add(current); revealed = false
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    ) { Text("لم أعرفها", color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = {
                            vm.reviewCard(current, true); reviewed++; correctCount++
                            queue.removeAt(0); revealed = false
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
                    ) { Text("عرفتها ✓") }
                } else Spacer(Modifier.height(52.dp))
            } else if (loaded) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (reviewed > 0) "🎉 أحسنت!" else "لا توجد بطاقات مستحقة الآن", style = MaterialTheme.typography.headlineMedium, color = Navy)
                    if (reviewed > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text("راجعت $reviewed بطاقة، وعرفت $correctCount منها من أول مرة أو بعد التكرار.", color = Slate, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("رجوع", onClick = onBack)
                }
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Teal) }
        }
    }
}
