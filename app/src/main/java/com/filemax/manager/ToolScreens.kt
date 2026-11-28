package com.filemax.manager

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

val TEXT_EXT = setOf("txt", "md", "json", "xml", "html", "htm", "css", "js", "kt", "java", "log", "csv", "ini", "conf", "properties", "yml", "yaml", "sh", "gradle", "srt")
fun isTextName(n: String) = n.substringAfterLast('.', "").lowercase() in TEXT_EXT
fun isImageName(n: String) = FileTools.category(n) == "صور"
fun openOrEdit(context: Context, f: File) = openFile(context, f)

@Composable
fun ToolsHome(onBack: () -> Unit, go: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("الأدوات", onBack = onBack)
        LazyColumn {
            item { SettingsClick("المفضلة", "المجلدات التي حفظتها للوصول السريع") { go("favs") } }
            item { SettingsClick("سلة المحذوفات", "استرجاع الملفات المحذوفة أو تفريغ السلة") { go("trash") } }
            item { SettingsClick("باحث التكرار", "العثور على الملفات المتطابقة في المجلد الحالي وما تحته") { go("dups") } }
            item { SettingsClick("محلل التخزين", "أكبر الملفات وتوزيع المساحة حسب النوع") { go("analyzer") } }
        }
    }
}

@Composable
fun FavoritesScreen(favs: Set<String>, onBack: () -> Unit, onOpen: (File) -> Unit, onRemove: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("المفضلة", onBack = onBack)
        if (favs.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("لا توجد مجلدات مفضلة بعد. حدد مجلداً واختر «إضافة للمفضلة».", color = Color.Gray) }
        LazyColumn { items(favs.sorted()) { p ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(File(p)) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = BrandYellow); Spacer(Modifier.width(10.dp)); Text(p, Modifier.weight(1f), fontSize = 13.sp)
                IconButton(onClick = { onRemove(p) }) { Icon(Icons.Default.Close, null) } }
            HorizontalDivider(color = Line) } }
    }
}

@Composable
fun TrashScreen(onBack: () -> Unit, onMessage: (String) -> Unit) {
    var items by remember { mutableStateOf(FileTools.listTrash()) }
    Column(Modifier.fillMaxSize()) {
        TopBar("سلة المحذوفات", "${items.size} عنصر", onBack) {
            IconButton(onClick = { FileTools.emptyTrash(); items = emptyList(); onMessage("تم تفريغ السلة") }) { Icon(Icons.Default.DeleteForever, "تفريغ", tint = Ink) } }
        if (items.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("السلة فارغة", color = Color.Gray) }
        LazyColumn { items(items, key = { it.dir.name }) { t ->
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(iconFor(t.file.name, t.file.isDirectory), null); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text(t.file.name, fontSize = 14.sp); Text(t.origin, fontSize = 10.sp, color = Color.Gray) }
                IconButton(onClick = { runCatching { FileTools.restore(t) }.onFailure { onMessage(it.message ?: "تعذر الاسترجاع") }; items = FileTools.listTrash() }) { Icon(Icons.Default.RestoreFromTrash, "استرجاع") }
                IconButton(onClick = { t.dir.deleteRecursively(); items = FileTools.listTrash() }) { Icon(Icons.Default.DeleteForever, "حذف نهائي") } }
            HorizontalDivider(color = Line) } }
    }
}

@Composable
fun DuplicatesScreen(root: File, hidden: Boolean, onBack: () -> Unit, onMessage: (String) -> Unit) {
    var groups by remember { mutableStateOf<List<List<File>>?>(null) }
    LaunchedEffect(root) { groups = withContext(Dispatchers.IO) { runCatching { FileTools.findDuplicates(root, hidden) }.getOrDefault(emptyList()) } }
    Column(Modifier.fillMaxSize()) {
        TopBar("باحث التكرار", root.absolutePath, onBack)
        val g = groups
        if (g == null) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        else if (g.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("لا توجد ملفات مكررة", color = Color.Gray) }
        else LazyColumn { items(g) { grp ->
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("${grp.size} نسخ · ${formatBytes(grp[0].length())} للنسخة", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 13.sp)
                grp.forEach { f -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(f.absolutePath, Modifier.weight(1f), fontSize = 11.sp)
                    IconButton(onClick = { runCatching { FileTools.moveToTrash(f) }.onFailure { onMessage(it.message ?: "فشل") }
                        groups = groups?.map { l -> l.filter { it != f } }?.filter { it.size > 1 } }) { Icon(Icons.Default.Delete, "حذف") } } }
            }
            HorizontalDivider(color = Line) } }
    }
}

@Composable
fun AnalyzerScreen(root: File, onBack: () -> Unit, onOpen: (File) -> Unit) {
    var res by remember { mutableStateOf<Pair<Map<String, Long>, List<File>>?>(null) }
    LaunchedEffect(root) { res = withContext(Dispatchers.IO) { runCatching { FileTools.analyze(root) }.getOrDefault(emptyMap<String, Long>() to emptyList()) } }
    Column(Modifier.fillMaxSize()) {
        TopBar("محلل التخزين", root.absolutePath, onBack)
        val r = res
        if (r == null) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        else {
            val total = r.first.values.sum().coerceAtLeast(1)
            LazyColumn {
                item { SettingsHeader("حسب النوع") }
                items(r.first.entries.sortedByDescending { it.value }) { (k, v) ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row { Text(k, Modifier.weight(1f), fontSize = 13.sp); Text(formatBytes(v), fontSize = 12.sp, color = Color.Gray) }
                        LinearProgressIndicator(progress = { v.toFloat() / total }, Modifier.fillMaxWidth()) } }
                item { SettingsHeader("أكبر الملفات") }
                items(r.second) { f -> Row(Modifier.fillMaxWidth().clickable { onOpen(f.parentFile ?: f) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(f.name, Modifier.weight(1f), fontSize = 13.sp, maxLines = 1); Text(formatBytes(f.length()), fontSize = 12.sp, color = Color.Gray) }
                    HorizontalDivider(color = Line) }
            }
        }
    }
}

@Composable
fun PropertiesDialog(files: List<File>, onClose: () -> Unit) {
    var text by remember { mutableStateOf("جارٍ الحساب…") }
    LaunchedEffect(files) {
        text = withContext(Dispatchers.IO) {
            if (files.size == 1) {
                val f = files[0]; val s = FileTools.stats(f)
                buildString {
                    append("الاسم: ${f.name}\nالمسار: ${f.parent}\nالنوع: ${if (f.isDirectory) "مجلد" else FileTools.category(f.name)}\n")
                    append("الحجم: ${formatBytes(s.bytes)}\n"); if (f.isDirectory) append("الملفات: ${s.files} · المجلدات: ${s.folders}\n")
                    append("آخر تعديل: ${fmtDate(f.lastModified())}\nقراءة: ${f.canRead()} · كتابة: ${f.canWrite()}")
                    if (f.isFile && f.length() < 512L shl 20) append("\nMD5: ${FileTools.hash(f, "MD5")}\nSHA-256: ${FileTools.hash(f)}")
                }
            } else { val t = files.map { FileTools.stats(it) }; "العناصر: ${files.size}\nالحجم الكلي: ${formatBytes(t.sumOf { it.bytes })}\nالملفات: ${t.sumOf { it.files }}" }
        }
    }
    AlertDialog(onDismissRequest = onClose, title = { Text("خصائص") }, text = { Text(text, fontSize = 12.sp, modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onClose) { Text("إغلاق") } })
}

@Composable
fun BatchRenameDialog(files: List<File>, onClose: () -> Unit, onDone: (String) -> Unit) {
    var pattern by remember { mutableStateOf("{name}_{n}") }; var start by remember { mutableStateOf("1") }
    val preview = FileTools.batchPreview(files, pattern, start.toIntOrNull() ?: 1)
    AlertDialog(onDismissRequest = onClose, title = { Text("إعادة تسمية جماعية") }, text = { Column {
        OutlinedTextField(pattern, { pattern = it }, label = { Text("النمط ({name} الاسم، {n} الرقم)") }, singleLine = true)
        OutlinedTextField(start, { start = it.filter(Char::isDigit) }, label = { Text("البداية") }, singleLine = true)
        Text(preview.take(4).joinToString("\n") { "${it.first.name} ← ${it.second}" }, fontSize = 11.sp, color = Color.Gray)
    } }, confirmButton = { TextButton(onClick = {
        var ok = 0; val names = preview.map { it.second }
        if (names.toSet().size != names.size) { onDone("الأسماء الناتجة مكررة"); return@TextButton }
        preview.forEach { (f, n) -> if (n != f.name && !File(f.parentFile, n).exists() && f.renameTo(File(f.parentFile, n))) ok++ }
        onDone("تمت إعادة تسمية $ok عنصر") }) { Text("تطبيق") } }, dismissButton = { TextButton(onClick = onClose) { Text("إلغاء") } })
}

@Composable
fun TextEditorScreen(file: File, onClose: () -> Unit, onMessage: (String) -> Unit) {
    var text by remember { mutableStateOf<String?>(null) }; var dirty by remember { mutableStateOf(false) }
    val tooBig = file.length() > 2L shl 20
    LaunchedEffect(file) { text = if (tooBig) "" else withContext(Dispatchers.IO) { runCatching { file.readText() }.getOrDefault("") } }
    BackHandler { onClose() }
    Column(Modifier.fillMaxSize()) {
        TopBar(file.name, if (dirty) "تعديلات غير محفوظة" else formatBytes(file.length()), onClose) {
            IconButton(enabled = dirty && !tooBig, onClick = { runCatching { file.writeText(text.orEmpty()) }.onSuccess { dirty = false; onMessage("تم الحفظ") }.onFailure { onMessage(it.message ?: "تعذر الحفظ") } }) { Icon(Icons.Default.Save, "حفظ", tint = Ink) } }
        if (tooBig) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("الملف أكبر من 2MB؛ افتحه بتطبيق خارجي", color = Color.Gray) }
        else BasicTextField(text.orEmpty(), { text = it; dirty = true }, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface))
    }
}

@Composable
fun ImageViewerScreen(file: File, onClose: () -> Unit) {
    val bmp by produceState<android.graphics.Bitmap?>(null, file) { value = withContext(Dispatchers.IO) { loadBitmap(file, 1400) } }
    BackHandler { onClose() }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        TopBar(file.name, onBack = onClose)
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            val b = bmp
            if (b == null) CircularProgressIndicator() else Image(b.asImageBitmap(), file.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}
