package com.lecturemanager.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturemanager.app.MainViewModel
import com.lecturemanager.app.data.DayTotal
import com.lecturemanager.app.data.Lecture
import com.lecturemanager.app.data.Subject
import com.lecturemanager.app.data.SubjectTotal
import com.lecturemanager.app.data.todayEpochDay
import com.lecturemanager.app.ui.theme.*
import com.lecturemanager.app.util.formatSeconds
import com.lecturemanager.app.util.normalizeArabic
import com.lecturemanager.app.util.snippet
import java.time.LocalDate

private enum class Filter(val label: String) { ALL("الكل"), DONE("المكتملة"), TODO("المتبقية"), PDF("بها PDF") }

/** البحث (أسماء، ملاحظات، ومحتوى ملفات PDF) + الإحصائيات ووقت المذاكرة. */
@Composable
fun SearchScreen(vm: MainViewModel, onOpen: (lectureId: Long, page: Int) -> Unit) {
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val lectures by vm.lectures.collectAsStateWithLifecycle()
    val hits by vm.pdfHits.collectAsStateWithLifecycle()
    val dayTotals by vm.dayTotals.collectAsStateWithLifecycle()
    val subjectTotals by vm.subjectTotals.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.ALL) }
    val subjectById = subjects.associateBy { it.id }
    val lectureById = lectures.associateBy { it.id }

    LaunchedEffect(query) { vm.searchPdfText(query) }

    val nq = normalizeArabic(query)
    val results = lectures.filter { l ->
        val s = subjectById[l.subjectId]
        val matchQ = nq.isEmpty() || normalizeArabic(l.title).contains(nq) || normalizeArabic(l.notes).contains(nq) ||
            (s != null && (normalizeArabic(s.title).contains(nq) || normalizeArabic(s.code).contains(nq)))
        val matchF = when (filter) {
            Filter.ALL -> true; Filter.DONE -> l.isCompleted; Filter.TODO -> !l.isCompleted; Filter.PDF -> l.pdfPath != null
        }
        matchQ && matchF
    }
    val searching = nq.isNotEmpty() || filter != Filter.ALL
    val pending = lectures.count { it.pdfPath != null && !it.indexed }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("ابحث في المواد والملاحظات ونصوص الملفات") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton({ query = "" }) { Icon(Icons.Filled.Close, "مسح") } },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Navy, unfocusedBorderColor = InputBorder,
                    focusedContainerColor = CardBg, unfocusedContainerColor = CardBg),
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Filter.values().forEach { f ->
                    FilterChip(filter == f, { filter = f }, { Text(f.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyFill, selectedLabelColor = Color.White))
                }
            }
        }
        if (pending > 0) item {
            Text("⏳ جارٍ فهرسة نص $pending ملف في الخلفية — نتائج البحث داخل الملفات ستكتمل تدريجياً.",
                style = MaterialTheme.typography.labelMedium, color = Ochre)
        }
        if (searching) {
            item { Text("المحاضرات (${results.size})", style = MaterialTheme.typography.titleMedium, color = Navy) }
            if (results.isEmpty()) item { Text("لا توجد محاضرات مطابقة.", color = Slate) }
            items(results.size) { i ->
                val l = results[i]
                val s = subjectById[l.subjectId]
                AppCard(Modifier.clickable { onOpen(l.id, -1) }) {
                    Text(l.title, style = MaterialTheme.typography.titleMedium, color = Navy)
                    Text("${s?.title ?: ""} • الأسبوع ${l.weekNumber}", style = MaterialTheme.typography.bodySmall, color = Slate)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        if (l.isCompleted) Badge("مكتملة", TealTint, TealText) else Badge("متبقية", AmberTint, AmberText)
                        if (l.pdfPath != null) Badge("PDF", TealTint, TealText)
                        if (l.tag.isNotBlank()) TagBadge(l.tag)
                    }
                }
            }
            if (nq.length >= 2) {
                item { Text("داخل ملفات PDF (${hits.size})", style = MaterialTheme.typography.titleMedium, color = Navy) }
                if (hits.isEmpty()) item { Text("لا توجد نتائج داخل نصوص الملفات.", color = Slate) }
                items(hits.size) { i ->
                    val h = hits[i]
                    val l = lectureById[h.lectureId]
                    if (l != null) AppCard(Modifier.clickable { onOpen(l.id, h.page) }) {
                        Text("${l.title} — صفحة ${h.page + 1}", style = MaterialTheme.typography.titleMedium, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(subjectById[l.subjectId]?.title ?: "", style = MaterialTheme.typography.labelMedium, color = Teal)
                        Spacer(Modifier.height(4.dp))
                        Text(snippet(h.text, nq), style = MaterialTheme.typography.bodySmall, color = Slate, maxLines = 3)
                    }
                }
            }
        }
        item { StatsCard(subjects, lectures) }
        item { StudyTimeCard(subjects, dayTotals, subjectTotals) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatsCard(subjects: List<Subject>, lectures: List<Lecture>) {
    val total = lectures.size
    val done = lectures.count { it.isCompleted }
    val overall = if (total == 0) 0f else done.toFloat() / total
    AppCard {
        Text("إحصائيات الإنجاز", style = MaterialTheme.typography.titleMedium, color = Navy)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Donut(overall, Teal, 96.dp, 12.dp, "${(overall * 100).toInt()}٪")
            Spacer(Modifier.width(16.dp))
            Column {
                Text("الإنجاز الكلي", style = MaterialTheme.typography.titleMedium, color = Navy)
                Text("$done من $total محاضرة", style = MaterialTheme.typography.bodyMedium, color = Slate)
            }
        }
        subjects.forEach { s ->
            val ls = lectures.filter { it.subjectId == s.id }
            val p = if (ls.isEmpty()) 0f else ls.count { it.isCompleted }.toFloat() / ls.size
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Donut(p, adaptAccent(Color(s.themeColor.toInt())), 44.dp, 6.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, style = MaterialTheme.typography.bodyMedium, color = Navy)
                    Text("${(p * 100).toInt()}٪ • ${ls.count { it.isCompleted }} من ${ls.size}", style = MaterialTheme.typography.labelMedium, color = Slate)
                }
            }
        }
    }
}

@Composable
private fun StudyTimeCard(subjects: List<Subject>, days: List<DayTotal>, bySubject: List<SubjectTotal>) {
    val today = todayEpochDay()
    val map = days.associate { it.day to it.total }
    val series = (today - 6..today).map { it to (map[it] ?: 0L) }
    val max = (series.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    AppCard {
        Text("وقت المذاكرة", style = MaterialTheme.typography.titleMedium, color = Navy)
        Spacer(Modifier.height(4.dp))
        val weekTotal = series.sumOf { it.second }
        Text(
            if (weekTotal == 0L) "ابدأ المؤقّت من داخل أي محاضرة لتسجيل وقت مذاكرتك."
            else "اليوم: ${formatSeconds(series.last().second)} • آخر 7 أيام: ${formatSeconds(weekTotal)}",
            style = MaterialTheme.typography.bodyMedium, color = Slate,
        )
        if (weekTotal > 0) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                series.forEach { (day, sec) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            Modifier.fillMaxWidth().height((80f * sec / max).coerceAtLeast(if (sec > 0) 6f else 2f).dp)
                                .clip(RoundedCornerShape(4.dp)).background(if (day == today) Teal else Teal.copy(alpha = 0.45f)),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${LocalDate.ofEpochDay(day).dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = Slate)
                    }
                }
            }
            val named = bySubject.sortedByDescending { it.total }.mapNotNull { t -> subjects.firstOrNull { it.id == t.subjectId }?.let { it to t.total } }
            if (named.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("حسب المادة (آخر 30 يوماً)", style = MaterialTheme.typography.labelLarge, color = Navy)
                named.forEach { (s, sec) ->
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("${s.icon} ${s.title}", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = Navy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatSeconds(sec), style = MaterialTheme.typography.bodyMedium, color = Slate)
                    }
                }
            }
        }
    }
}
