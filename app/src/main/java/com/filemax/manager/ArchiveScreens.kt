package com.filemax.manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/** Browses an archive like a folder without extracting it. [baseDir] is where extractions go. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArchiveBrowserScreen(archive: File, baseDir: File, runner: TaskRunner, policy: ExistsPolicy, repo: FileRepository, exportTo: StorageNode.Saf?, onClose: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    var password by remember { mutableStateOf<CharArray?>(null) }
    var entries by remember { mutableStateOf<List<ArchiveEntryInfo>?>(null) }
    var askPw by remember { mutableStateOf(false) }
    var wrongPw by remember { mutableStateOf(false) }
    var path by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var attempt by remember { mutableIntStateOf(0) }
    var info by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(attempt) {
        runner.run("فتح الأرشيف", { c -> ArchiveReader.list(ArchiveReader.joinVolumes(archive, context.cacheDir), password, c) }) { r ->
            r.fold({ entries = it; askPw = false }, { e ->
                if (e is PasswordException) {
                    val saved = if (!e.wrong && password == null) SecureStore.load(context, archive.name) else null
                    if (saved != null) { password = saved; attempt++ } else { if (e.wrong) SecureStore.forget(context, archive.name); wrongPw = e.wrong; askPw = true }
                } else { onMessage(e.message ?: "تعذر فتح الأرشيف"); onClose() } })
        }
    }
    BackHandler { if (selected.isNotEmpty()) selected = emptySet() else if (path.isNotEmpty()) path = path.substringBeforeLast('/', "") else onClose() }

    // Build the visible level: direct children of [path], inferring folders from deeper paths.
    val all = entries.orEmpty()
    val level = remember(all, path) {
        val prefix = if (path.isEmpty()) "" else "$path/"
        val map = LinkedHashMap<String, ArchiveEntryInfo>()
        for (e in all) {
            if (!e.path.startsWith(prefix) || e.path == path) continue
            val rest = e.path.removePrefix(prefix); val first = rest.substringBefore('/')
            val childPath = prefix + first
            if ('/' in rest) { if (childPath !in map) map[childPath] = ArchiveEntryInfo(childPath, true, 0) }
            else map[childPath] = e
        }
        map.values.sortedWith(compareBy<ArchiveEntryInfo> { !it.isDir }.thenBy { it.name.lowercase() })
    }
    fun src() = ArchiveReader.joinVolumes(archive, context.cacheDir)
    fun extract(sel: Set<String>?, dest0: File) {
        val tmp = if (exportTo != null) File(context.cacheDir, "saf_out/${System.nanoTime()}") else null
        val dest = if (tmp == null) dest0 else if (sel == null) File(tmp, dest0.name) else tmp
        runner.run("استخراج", { c ->
            val n = ArchiveReader.extract(src(), dest, password, sel, false, policy, c)
            if (tmp != null && exportTo != null) { try { tmp.listFiles()?.forEach { repo.copyTo(StorageNode.Local(it), exportTo, false) } } finally { tmp.deleteRecursively() } }
            n
        }) { r ->
            tmp?.deleteRecursively()
            r.fold({ onMessage("تم استخراج $it عنصر إلى\n" + (if (exportTo != null) exportTo.name else dest0.absolutePath)) }, { onMessage(if (it is CancelledByUser) "أُلغي الاستخراج" else it.message ?: "فشل الاستخراج") }) }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(archive.name, "/" + path, onBack = { if (path.isNotEmpty()) path = path.substringBeforeLast('/', "") else onClose() }) {
            IconButton(onClick = {
                runner.run("اختبار السلامة", { c -> ArchiveReader.test(src(), password, c) }) { r ->
                    r.fold({ onMessage("الأرشيف سليم ✓ ($it عنصر)") }, { onMessage("فشل الاختبار: ${it.message}") }) } }) { Icon(Icons.Default.VerifiedUser, "اختبار", tint = Ink) }
            IconButton(onClick = {
                val files = all.filter { !it.isDir }
                info = "الصيغة: ${ArchiveReader.detect(archive).label}\nحجم الأرشيف: ${formatBytes(archive.length())}\nالعناصر: ${all.size}\n" +
                    "الحجم الأصلي: ${formatBytes(files.sumOf { maxOf(it.size, 0L) })}\n" +
                    (files.sumOf { maxOf(it.size, 0L) }.takeIf { it > 0 }?.let { "نسبة الضغط: ${(archive.length() * 100 / it)}%\n" } ?: "") +
                    (if (ArchiveReader.extensionMismatch(archive)) "⚠ الامتداد لا يطابق محتوى الملف\n" else "") +
                    (ArchiveReader.comment(archive)?.let { "التعليق: $it" } ?: "") }) { Icon(Icons.Default.Info, "معلومات", tint = Ink) }
        }
        if (entries == null) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("جارٍ القراءة…", color = Color.Gray) }
        else if (level.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("لا توجد عناصر", color = Color.Gray) }
        else LazyColumn(Modifier.weight(1f)) {
            items(level, key = { it.path }) { e ->
                val sel = e.path in selected
                Row(Modifier.fillMaxWidth().background(if (sel) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent).combinedClickable(
                    onClick = {
                        if (selected.isNotEmpty()) selected = if (sel) selected - e.path else selected + e.path
                        else if (e.isDir) path = e.path
                        else runner.run("معاينة", { c ->
                            val d = File(context.cacheDir, "preview").apply { deleteRecursively(); mkdirs() }
                            ArchiveReader.extract(src(), d, password, setOf(e.path), true, ExistsPolicy.OVERWRITE, c); File(d, e.name)
                        }) { r -> r.fold({ openOrEdit(context, it) }, { onMessage(it.message ?: "تعذرت المعاينة") }) }
                    }, onLongClick = { selected = selected + e.path }).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconFor(e.name, e.isDir), null, tint = if (e.isDir) Color(0xFFE0A100) else Color(0xFF696969), modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        if (!e.isDir) Text(buildString {
                            append(formatBytes(maxOf(e.size, 0))); if (e.packed >= 0) append(" ← ${formatBytes(e.packed)}")
                            if (e.size > 0 && e.packed >= 0) append(" (${e.packed * 100 / e.size}%)")
                            if (e.modified > 0) append(" · ${fmtDate(e.modified)}"); if (e.method.isNotEmpty()) append(" · ${e.method}")
                            if (e.crc >= 0) append(" · CRC ${java.lang.Long.toHexString(e.crc).uppercase()}"); if (e.encrypted) append(" 🔒") }, fontSize = 10.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { extract(setOf(e.path), baseDir) }) { Icon(Icons.Default.Unarchive, "استخراج", tint = Color.Gray) }
                }
                HorizontalDivider(color = Line, thickness = .6.dp)
            }
        }
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            SmallAction("استخراج الكل", Icons.Default.Unarchive) { extract(null, File(baseDir, archive.name.removeSuffix(".001").substringBeforeLast('.').removeSuffix(".tar"))) }
            SmallAction("استخراج المحدد", Icons.Default.FileDownload) { if (selected.isEmpty()) onMessage("حدد عناصر بالضغط المطوّل") else extract(selected, baseDir) }
            SmallAction("تحديد الكل", Icons.Default.SelectAll) { selected = level.map { it.path }.toSet() }
            SmallAction("إلغاء التحديد", Icons.Default.Close) { selected = emptySet() }
        }
    }
    if (askPw) PasswordDialog(wrongPw, onOk = { password = it; askPw = false; attempt++ }, onCancel = { askPw = false; onClose() }, archiveKey = archive.name)
    info?.let { AlertDialog(onDismissRequest = { info = null }, title = { Text("معلومات الأرشيف") }, text = { Text(it, fontSize = 13.sp) }, confirmButton = { TextButton(onClick = { info = null }) { Text("إغلاق") } }) }
}

@Composable
fun CreateArchiveDialog(defaultBase: String, folderSelection: Boolean, defaultFormat: String, defaultLevel: Int, onDismiss: () -> Unit, onCreate: (String, CreateOptions) -> Unit) {
    var base by remember { mutableStateOf(defaultBase) }
    var format by remember { mutableStateOf(defaultFormat) }
    var level by remember { mutableStateOf(defaultLevel) }
    var pw by remember { mutableStateOf("") }; var pw2 by remember { mutableStateOf("") }; var show by remember { mutableStateOf(false) }
    var crypto by remember { mutableStateOf(false) }; var comment by remember { mutableStateOf("") }
    var hidden by remember { mutableStateOf(false) }; var split by remember { mutableStateOf("بدون تقسيم") }
    val levels = linkedMapOf("تخزين فقط" to 0, "سريع" to 1, "عادي" to 5, "أقصى" to 9)
    val splits = linkedMapOf("بدون تقسيم" to 0L, "10 MB" to 10L shl 20, "50 MB" to 50L shl 20, "100 MB" to 100L shl 20, "500 MB" to 500L shl 20, "1 GB" to 1L shl 30, "2 GB" to 2L shl 30, "4 GB" to 4L shl 30)
    val canPw = ArchiveWriter.supportsPassword(format)
    val mismatch = canPw && pw != pw2
    AlertDialog(onDismissRequest = onDismiss, title = { Text("إنشاء أرشيف") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(base, { base = it }, label = { Text("اسم الأرشيف") }, singleLine = true, suffix = { Text(".$format") })
            Dropdown("الصيغة", format.uppercase(), ArchiveWriter.FORMATS.map { it.uppercase() }) { format = it.lowercase() }
            Dropdown("الضغط", levels.entries.first { it.value == level }.key, levels.keys.toList()) { level = levels[it] ?: 5 }
            Dropdown("التقسيم", split, splits.keys.toList()) { split = it }
            if (ArchiveWriter.singleFileOnly(format) && folderSelection) Text("هذه الصيغة تضغط ملفاً واحداً فقط", color = Color(0xFFFB8C00), fontSize = 12.sp)
            if (canPw) {
                OutlinedTextField(pw, { pw = it }, label = { Text("كلمة المرور (اختياري)") }, singleLine = true, visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
                if (pw.isNotEmpty()) {
                    OutlinedTextField(pw2, { pw2 = it }, label = { Text("تأكيد كلمة المرور") }, singleLine = true, isError = mismatch, visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation())
                    if (format == "zip") Row(verticalAlignment = Alignment.CenterVertically) { Text("ZipCrypto (توافق قديم) بدل AES-256", Modifier.weight(1f), fontSize = 12.sp); Switch(crypto, { crypto = it }) }
                    else Text("تشفير AES-256 مع إخفاء أسماء الملفات", fontSize = 11.sp, color = Color.Gray)
                }
                if (format == "zip") OutlinedTextField(comment, { comment = it }, label = { Text("تعليق الأرشيف") }, maxLines = 2)
            } else Text("هذه الصيغة لا تدعم كلمة المرور.", fontSize = 11.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("استبعاد الملفات المخفية", Modifier.weight(1f), fontSize = 13.sp); Switch(hidden, { hidden = it }) }
        }
    }, confirmButton = { TextButton(enabled = base.isNotBlank() && !mismatch && !(ArchiveWriter.singleFileOnly(format) && folderSelection), onClick = {
        onCreate("$base.$format", CreateOptions(format, level, if (canPw && pw.isNotEmpty()) pw.toCharArray() else null, crypto && format == "zip", if (format == "zip") comment else "", hidden, splits[split] ?: 0L)) }) { Text("إنشاء") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

