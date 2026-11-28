package com.filemax.manager

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.webkit.MimeTypeMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val BrandYellow = Color(0xFFFFC62E)
val PaleYellow = Color(0xFFFFF4CC)
val Ink = Color(0xFF252525)
val Soft = Color(0xFFF5F5F2)
val Line = Color(0xFFECECE8)

fun fmtDate(t: Long): String = if (t > 0) SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(t)) else "—"

class ExistsPrompt(val file: File, val answer: java.util.concurrent.CompletableFuture<Pair<ExistsPolicy, Boolean>>)

/** Runs one background job at a time with progress, pause and cancel; a foreground service keeps it alive in the background. */
class TaskRunner(private val scope: CoroutineScope, private val appContext: Context) {
    companion object { @Volatile var current: TaskRunner? = null }
    var active by mutableStateOf(false); private set
    var ctl by mutableStateOf(TaskControl()); private set
    var title by mutableStateOf("")
    var tick by mutableStateOf(0f)
    var paused by mutableStateOf(false)
    var prompt by mutableStateOf<ExistsPrompt?>(null)
    fun <T> run(name: String, work: (TaskControl) -> T, done: (Result<T>) -> Unit) {
        if (active) return
        val c = TaskControl()
        c.onExists = { f ->
            val fut = java.util.concurrent.CompletableFuture<Pair<ExistsPolicy, Boolean>>()
            prompt = ExistsPrompt(f, fut)
            val (p, all) = fut.get(); prompt = null
            if (all) c.stickyPolicy = p
            p
        }
        ctl = c; title = name; active = true; paused = false; current = this
        OperationService.start(appContext, name)
        scope.launch {
            val poll = launch { var n = 0; while (true) { tick = c.fraction; if (n++ % 6 == 0) OperationService.update(appContext, name, c.fraction, c.label); kotlinx.coroutines.delay(150) } }
            val r = withContext(Dispatchers.IO) { runCatching { work(c) } }
            poll.cancel(); active = false; current = null
            OperationService.stop(appContext)
            done(if (r.exceptionOrNull() is CancelledByUser) Result.failure(CancelledByUser()) else r)
        }
    }
}

@Composable
fun TaskDialog(r: TaskRunner) {
    if (!r.active) return
    r.prompt?.let { pr ->
        var all by remember(pr) { mutableStateOf(false) }
        AlertDialog(onDismissRequest = {}, title = { Text("الملف موجود بالفعل") }, text = { Column {
            Text(pr.file.absolutePath, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(all, { all = it }); Text("تطبيق على الكل", fontSize = 13.sp) }
        } }, confirmButton = { Row {
            TextButton(onClick = { pr.answer.complete(ExistsPolicy.OVERWRITE to all) }) { Text("استبدال") }
            TextButton(onClick = { pr.answer.complete(ExistsPolicy.RENAME to all) }) { Text("إعادة تسمية") }
            TextButton(onClick = { pr.answer.complete(ExistsPolicy.SKIP to all) }) { Text("تخطي") }
        } })
    }
    AlertDialog(onDismissRequest = {}, title = { Text(r.title) }, text = {
        Column {
            val f = r.tick
            if (f >= 0f) LinearProgressIndicator(progress = { f }, Modifier.fillMaxWidth()) else LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Text(r.ctl.label, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (r.ctl.total > 0) Text("${formatBytes(r.ctl.done)} / ${formatBytes(r.ctl.total)}", fontSize = 11.sp, color = Color.Gray)
        }
    }, confirmButton = { TextButton(onClick = { r.ctl.cancelled = true; r.prompt?.answer?.complete(ExistsPolicy.SKIP to false) }) { Text("إلغاء") } },
        dismissButton = { TextButton(onClick = { r.paused = !r.paused; r.ctl.paused = r.paused }) { Text(if (r.paused) "استئناف ▶" else "إيقاف مؤقت ⏸") } })
}

@Composable
fun PasswordDialog(wrong: Boolean, onOk: (CharArray) -> Unit, onCancel: () -> Unit) {
    var pw by remember { mutableStateOf("") }; var show by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onCancel, title = { Text("كلمة المرور") }, text = { Column {
        if (wrong) Text("كلمة المرور غير صحيحة", color = Color(0xFFE53935), fontSize = 12.sp)
        OutlinedTextField(pw, { pw = it }, singleLine = true, label = { Text("كلمة المرور") },
            visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } })
    } }, confirmButton = { TextButton(enabled = pw.isNotEmpty(), onClick = { onOk(pw.toCharArray()) }) { Text("موافق") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("إلغاء") } })
}

fun iconFor(name: String, dir: Boolean): ImageVector = when {
    dir -> Icons.Default.Folder
    ArchiveReader.isArchiveName(name) -> Icons.Default.Archive
    name.endsWith(".apk", true) -> Icons.Default.Android
    FileTools.category(name) == "صور" -> Icons.Default.Image
    FileTools.category(name) == "فيديو" -> Icons.Default.Movie
    FileTools.category(name) == "صوت" -> Icons.Default.MusicNote
    else -> Icons.Default.InsertDriveFile
}

private val thumbCache = object : LruCache<String, Bitmap>(60) {}
fun loadBitmap(f: File, max: Int): Bitmap? = runCatching {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeFile(f.path, o)
    var s = 1; while (o.outWidth / s > max * 2 || o.outHeight / s > max * 2) s *= 2
    BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = s })
}.getOrNull()

@Composable
fun NodeIcon(node: StorageNode, size: Int, onClick: () -> Unit) {
    val file = (node as? StorageNode.Local)?.file
    val isImg = file != null && !node.isDirectory && FileTools.category(node.name) == "صور"
    val bmp by produceState<Bitmap?>(null, file?.path) {
        if (isImg && file != null) value = thumbCache.get(file.path) ?: withContext(Dispatchers.IO) { loadBitmap(file, 96) }?.also { thumbCache.put(file.path, it) }
    }
    IconButton(onClick = onClick, modifier = Modifier.size((size + 15).dp)) {
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, Modifier.size(size.dp), contentScale = ContentScale.Crop)
        else Icon(iconFor(node.name, node.isDirectory), null, tint = if (node.isDirectory) Color(0xFFE0A100) else Color(0xFF696969), modifier = Modifier.size(size.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(node: StorageNode, subtitle: String, selected: Boolean, onIcon: () -> Unit, onClick: () -> Unit, onLong: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
        .combinedClickable(onClick = onClick, onLongClick = onLong).padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        NodeIcon(node, 27, onIcon)
        Column(Modifier.weight(1f)) {
            Text(node.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            Text(subtitle, fontSize = 10.sp, color = Color.Gray)
        }
        if (selected) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF26844A), modifier = Modifier.size(19.dp))
    }
    HorizontalDivider(color = Line, thickness = .6.dp)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridTile(node: StorageNode, selected: Boolean, onIcon: () -> Unit, onClick: () -> Unit, onLong: () -> Unit) {
    Column(Modifier.padding(5.dp).fillMaxWidth().background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
        .combinedClickable(onClick = onClick, onLongClick = onLong).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        NodeIcon(node, 34, onIcon)
        Text(node.name, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SmallAction(label: String, icon: ImageVector, action: () -> Unit) {
    TextButton(onClick = action, contentPadding = PaddingValues(horizontal = 5.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, modifier = Modifier.size(18.dp)); Text(label, fontSize = 10.sp) }
    }
}

@Composable
fun TopBar(title: String, subtitle: String? = null, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().background(BrandYellow).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Ink) }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = Ink.copy(alpha = .72f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        actions()
    }
}

@Composable fun SettingsHeader(text: String) { Text(text, Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 10.dp), color = Color(0xFFB8860B), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
@Composable fun SettingSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), fontSize = 14.sp); Switch(checked, onChange) }; HorizontalDivider(color = Line) }
@Composable fun SettingsInfo(title: String, detail: String) { Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium); Text(detail, fontSize = 11.sp, color = Color.Gray) }; HorizontalDivider(color = Line) }
@Composable fun SettingsClick(title: String, detail: String, onClick: () -> Unit) { TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) { Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) { Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp); Text(detail, color = Color.Gray, fontSize = 11.sp) }; Icon(Icons.Default.ChevronRight, null) }; HorizontalDivider(color = Line) }
@Composable fun SettingsChoice(title: String, value: String, choices: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Box {
            TextButton(onClick = { expanded = true }) { Text(value); Icon(Icons.Default.ArrowDropDown, null) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { choices.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { onChange(c); expanded = false }) } }
        }
    }
    HorizontalDivider(color = Line)
}

@Composable
fun Dropdown(label: String, value: String, choices: List<String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(88.dp), fontSize = 13.sp)
        Box { OutlinedButton(onClick = { open = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text(value, fontSize = 13.sp); Icon(Icons.Default.ArrowDropDown, null) }
            DropdownMenu(open, { open = false }) { choices.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { onChange(c); open = false }) } } }
    }
}

fun openFile(context: Context, file: File) {
    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun shareFiles(context: Context, files: List<File>) {
    val uris = ArrayList<Uri>(files.filter { it.isFile }.map { FileProvider.getUriForFile(context, "${context.packageName}.provider", it) })
    if (uris.isEmpty()) return
    val i = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).setType("*/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (uris.size == 1) i.putExtra(Intent.EXTRA_STREAM, uris[0]) else i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    runCatching { context.startActivity(Intent.createChooser(i, "مشاركة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
