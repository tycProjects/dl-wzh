package com.filemax.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val Ink = Color(0xFF252525)
private val Gold = Color(0xFFF0D36B)
private val PaleGold = Color(0xFFFFF4C9)
private val Canvas = Color(0xFFF5F5F2)
private const val PREFS = "file_max_preferences"
private const val TREE_URI = "selected_tree_uri"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FileMaxApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileMaxApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var currentFolder by remember { mutableStateOf<DocumentFile?>(null) }
    val folderStack = remember { mutableStateListOf<DocumentFile>() }
    var entries by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<DocumentFile?>(null) }
    var message by remember { mutableStateOf("اختر مجلد التخزين للبدء") }
    var newFolderDialog by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("الاسم") }
    var sortMenu by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DocumentFile?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val folder = DocumentFile.fromTreeUri(context, uri)
                if (folder == null || !folder.canRead()) {
                    message = "تعذر فتح المجلد المختار"
                } else {
                    currentFolder = folder
                    folderStack.clear()
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putString(TREE_URI, uri.toString()).apply()
                    query = ""
                    selected = null
                    message = "تم ربط مجلد التخزين بنجاح"
                }
            } catch (e: Exception) {
                message = "تعذر منح صلاحية الوصول: ${e.localizedMessage ?: "خطأغيرمعروف"}"
            }
        }
    }

    suspend fun loadEntries() {
        val folder = currentFolder ?: run { entries = emptyList(); return }
        val result = withContext(Dispatchers.IO) {
            runCatching {
                folder.listFiles()
                    .filter { showHidden || !it.name.orEmpty().startsWith(".") }
                    .filter { query.isBlank() || it.name.orEmpty().contains(query, ignoreCase = true) }
                    .sortedWith(
                        when (sortMode) {
                            "الحجم" -> compareBy<DocumentFile> { !it.isDirectory }.thenByDescending { it.length() }
                            "الأحدث" -> compareBy<DocumentFile> { !it.isDirectory }.thenByDescending { it.lastModified() }
                            else -> compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty().lowercase(Locale.ROOT) }
                        }
                    )
            }
        }
        entries = result.getOrElse {
            message = "تعذر قراءة المجلد: ${it.localizedMessage ?: "خطأ"}"
            emptyList()
        }
    }

    LaunchedEffect(Unit) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TREE_URI, null)
        if (saved != null) {
            runCatching {
                val uri = Uri.parse(saved)
                val restored = DocumentFile.fromTreeUri(context, uri)
                if (restored?.canRead() == true) {
                    currentFolder = restored
                    message = "تم استعادة مجلد التخزين السابق"
                }
            }
        }
    }

    LaunchedEffect(currentFolder, query, showHidden, sortMode) { loadEntries() }

    fun perform(action: suspend () -> String) {
        if (busy) return
        scope.launch {
            busy = true
            message = "جارٍ تنفيذ العملية..."
            message = try {
                withContext(Dispatchers.IO) { action() }
            } catch (e: Exception) {
                "فشلت العملية: ${e.localizedMessage ?: "خطأغيرمعروف"}"
            }
            busy = false
            selected = null
            loadEntries()
        }
    }

    fun navigateUp() {
        if (folderStack.isNotEmpty()) {
            currentFolder = folderStack.removeAt(folderStack.lastIndex)
            query = ""
            selected = null
        } else {
            message = "أنت في المجلد الرئيسي الذي اخترته"
        }
    }

    BackHandler(enabled = currentFolder != null) { navigateUp() }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink, secondary = Gold, background = Canvas,
            surface = Color.White, onPrimary = Color.White, onSurface = Ink
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Canvas,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text("FILE MAX", fontWeight = FontWeight.Black, color = Ink)
                                Text(currentFolder?.name ?: "مدير الملفات", style = MaterialTheme.typography.labelSmall, color = Ink.copy(.6f))
                            }
                        },
                        actions = {
                            IconButton(onClick = { showHidden = !showHidden }) {
                                Icon(if (showHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff, "إظهار الملفات المخفية")
                            }
                            IconButton(onClick = { scope.launch { loadEntries() } }) {
                                Icon(Icons.Default.Refresh, "تحديث")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { if (currentFolder == null) picker.launch(null) else newFolderDialog = true },
                        containerColor = Gold, contentColor = Ink
                    ) { Icon(Icons.Default.Add, "إضافة") }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("ابحث في الملفات والمجلدات") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, "مسح البحث")
                            }
                        },
                        singleLine = true, shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { picker.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Ink)
                        ) {
                            Icon(Icons.Default.FolderOpen, null)
                            Spacer(Modifier.width(5.dp))
                            Text("اختيار التخزين")
                        }
                        OutlinedButton(onClick = { navigateUp() }, enabled = currentFolder != null) {
                            Icon(Icons.Default.ArrowUpward, null)
                            Text("للأعلى")
                        }
                        Box {
                            OutlinedButton(onClick = { sortMenu = true }) {
                                Icon(Icons.Default.Sort, null)
                                Text("ترتيب")
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                listOf("الاسم", "الأحدث", "الحجم").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = { sortMode = option; sortMenu = false }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        currentFolder?.name ?: "لم يتم اختيار مجلد",
                        style = MaterialTheme.typography.labelMedium,
                        color = Ink.copy(.6f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(8.dp))

                    selected?.let { item ->
                        SelectedActions(
                            item = item,
                            onOpen = { openFile(context, item) { message = it } },
                            onShare = { shareFile(context, item) { message = it } },
                            onRename = { newName ->
                                if (newName.isBlank() || newName.contains("/") || newName.contains("\\")) {
                                    message = "اسم الملف غير صالح"
                                } else perform {
                                    if (item.renameTo(newName)) "تم تغيير الاسم بنجاح" else "تعذر تغيير الاسم"
                                }
                            },
                            onDelete = { deleteTarget = item },
                            onZip = { perform { zipItem(context, item) } },
                            onExtract = { perform { extractZip(context, item, currentFolder ?: error("لا يوجد مجلد وجهة")) } },
                            onCopy = { perform {
                                val folder = currentFolder ?: error("لا يوجد مجلد وجهة")
                                copyItem(context, item, folder, uniqueName(folder, item.name.orEmpty() + " - نسخة"))
                            } },
                            onClear = { selected = null }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (currentFolder == null) {
                        EmptyState("ابدأ باختيار مجلد من ذاكرة الهاتف أو بطاقة SD")
                    } else if (entries.isEmpty()) {
                        EmptyState(if (query.isBlank()) "هذا المجلد فارغ" else "لا توجد نتائج مطابقة")
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(entries, key = { it.uri.toString() }) { file ->
                                FileRow(
                                    file = file,
                                    isSelected = selected?.uri == file.uri,
                                    onClick = {
                                        if (file.isDirectory) {
                                            currentFolder?.let { folderStack.add(it) }
                                            currentFolder = file
                                            query = ""
                                            selected = null
                                        } else selected = file
                                    },
                                    onSelect = { selected = file }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = Ink.copy(.7f))
                    Text(
                        "يعمل التطبيق ضمن المجلدات التي تمنحها إذن الوصول عبر أندرويد.",
                        style = MaterialTheme.typography.labelSmall, color = Ink.copy(.5f)
                    )
                }
            }

            if (newFolderDialog) {
                AlertDialog(
                    onDismissRequest = { newFolderDialog = false },
                    title = { Text("إنشاء مجلد جديد") },
                    text = {
                        OutlinedTextField(
                            value = folderName, onValueChange = { folderName = it },
                            label = { Text("اسم المجلد") }, singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val name = folderName.trim()
                            if (name.isNotEmpty() && !name.contains("/") && !name.contains("\\")) {
                                perform {
                                    val folder = currentFolder ?: error("اختر مجلداً أولاً")
                                    if (folder.findFile(name) != null) "يوجد عنصر بهذا الاسم بالفعل"
                                    else if (folder.createDirectory(name) != null) "تم إنشاء المجلد بنجاح"
                                    else "تعذر إنشاء المجلد"
                                }
                            } else message = "أدخل اسماً صالحاً للمجلد"
                            folderName = ""
                            newFolderDialog = false
                        }) { Text("إنشاء") }
                    },
                    dismissButton = { TextButton(onClick = { newFolderDialog = false }) { Text("إلغاء") } }
                )
            }

            deleteTarget?.let { target ->
                AlertDialog(
                    onDismissRequest = { deleteTarget = null },
                    title = { Text("تأكيد الحذف") },
                    text = { Text("هل تريد حذف «${target.name}»؟ لا يمكن التراجع عن هذه العملية.") },
                    confirmButton = {
                        TextButton(onClick = {
                            val item = deleteTarget
                            deleteTarget = null
                            if (item != null) perform { if (item.delete()) "تم الحذف" else "تعذر الحذف" }
                        }) { Text("حذف", color = Color(0xFFB3261E)) }
                    },
                    dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("إلغاء") } }
                )
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().heightIn(min = 240.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.FolderOpen, null, tint = Gold, modifier = Modifier.size(52.dp))
            Text(text, color = Ink.copy(.65f))
        }
    }
}

@Composable
private fun FileRow(
    file: DocumentFile,
    onClick: () -> Unit,
    onSelect: () -> Unit,
    isSelected: Boolean
) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (isSelected) PaleGold else Color.White, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 5.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            null,
            tint = if (file.isDirectory) Color(0xFFC49A16) else Ink.copy(.65f),
            modifier = Modifier.size(30.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name ?: "بدون اسم", fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                if (file.isDirectory) "مجلد" else "${formatSize(file.length())} • ${file.typeLabel()}",
                style = MaterialTheme.typography.labelSmall, color = Ink.copy(.55f), maxLines = 1
            )
        }
        IconButton(onClick = onSelect) { Icon(Icons.Default.MoreVert, "خيارات", tint = Ink) }
    }
}

@Composable
private fun SelectedActions(
    item: DocumentFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onZip: () -> Unit,
    onExtract: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    var renameDialog by remember(item.uri) { mutableStateOf(false) }
    var name by remember(item.uri) { mutableStateOf(item.name.orEmpty()) }
    Card(colors = CardDefaults.cardColors(containerColor = PaleGold)) {
        Column(Modifier.padding(10.dp)) {
            Text("العنصر المحدد: ${item.name}", fontWeight = FontWeight.Bold, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!item.isDirectory) TextButton(onClick = onOpen) { Text("فتح") }
                TextButton(onClick = { renameDialog = true }) { Text("تسمية") }
                TextButton(onClick = onCopy) { Text("نسخ") }
                TextButton(onClick = onZip) { Text("ضغط ZIP") }
                TextButton(onClick = onDelete) { Text("حذف") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!item.isDirectory && item.name.orEmpty().endsWith(".zip", true)) {
                    TextButton(onClick = onExtract) { Text("فك الضغط") }
                }
                if (!item.isDirectory) TextButton(onClick = onShare) { Text("مشاركة") }
                TextButton(onClick = onClear) { Text("إلغاء التحديد") }
            }
        }
    }
    if (renameDialog) {
        AlertDialog(
            onDismissRequest = { renameDialog = false },
            title = { Text("إعادة تسمية") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { onRename(name.trim()); renameDialog = false }) { Text("حفظ") }
            },
            dismissButton = { TextButton(onClick = { renameDialog = false }) { Text("إلغاء") } }
        )
    }
}

private fun openFile(context: Context, file: DocumentFile, report: (String) -> Unit) {
    try {
        val mime = file.type ?: mimeFromName(file.name)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(file.uri, mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
        report("تم إرسال الملف إلى التطبيق المناسب")
    } catch (_: ActivityNotFoundException) {
        report("لا يوجد تطبيق مثبت لفتح هذا النوع من الملفات")
    } catch (e: Exception) {
        report("تعذر فتح الملف: ${e.localizedMessage ?: "خطأ"}")
    }
}

private fun shareFile(context: Context, file: DocumentFile, report: (String) -> Unit) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file.type ?: mimeFromName(file.name)
            putExtra(Intent.EXTRA_STREAM, file.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الملف"))
        report("تم فتح قائمة المشاركة")
    } catch (e: Exception) {
        report("تعذرت مشاركة الملف: ${e.localizedMessage ?: "خطأ"}")
    }
}

private fun mimeFromName(name: String?): String {
    val extension = name?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
}

private fun DocumentFile.typeLabel(): String =
    name?.substringAfterLast('.', "ملف")?.uppercase(Locale.ROOT) ?: "ملف"

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(Locale.ROOT, bytes / 1024.0)
    else -> "%.1f MB".format(Locale.ROOT, bytes / (1024.0 * 1024.0))
}

private fun uniqueName(folder: DocumentFile, requested: String): String {
    if (folder.findFile(requested) == null) return requested
    val dot = requested.lastIndexOf('.')
    val base = if (dot > 0) requested.substring(0, dot) else requested
    val extension = if (dot > 0) requested.substring(dot) else ""
    var index = 2
    while (folder.findFile("$base ($index)$extension") != null) index++
    return "$base ($index)$extension"
}

private fun zipItem(context: Context, item: DocumentFile): String {
    val parent = item.parentFile ?: return "لا يوجد مجلد أب لإنشاء الأرشيف"
    val base = (item.name ?: "archive").substringBeforeLast('.', item.name ?: "archive") + ".zip"
    val zipName = uniqueName(parent, base)
    val target = parent.createFile("application/zip", zipName) ?: return "تعذر إنشاء ملف ZIP"
    try {
        val output = context.contentResolver.openOutputStream(target.uri)
            ?: throw IOException("تعذر فتح ملف الإخراج")
        ZipOutputStream(BufferedOutputStream(output)).use { zos ->
            addToZip(context, item, item.name ?: "item", zos)
        }
    } catch (e: Exception) {
        target.delete()
        throw e
    }
    return "تم إنشاء الأرشيف: ${target.name}"
}

private fun addToZip(context: Context, file: DocumentFile, path: String, zos: ZipOutputStream) {
    if (file.isDirectory) {
        val dirPath = if (path.endsWith("/")) path else "$path/"
        zos.putNextEntry(ZipEntry(dirPath))
        zos.closeEntry()
        file.listFiles().forEach { child ->
            addToZip(context, child, "$dirPath${child.name ?: "item"}", zos)
        }
    } else {
        zos.putNextEntry(ZipEntry(path))
        context.contentResolver.openInputStream(file.uri)?.use { input -> input.copyTo(zos) }
            ?: throw IOException("تعذر قراءة ${file.name}")
        zos.closeEntry()
    }
}

private fun extractZip(context: Context, archive: DocumentFile, destination: DocumentFile): String {
    val input = context.contentResolver.openInputStream(archive.uri)
        ?: throw IOException("تعذر فتح الأرشيف")
    var count = 0
    var totalBytes = 0L
    val maxEntries = 5000
    val maxEntryBytes = 128L * 1024L * 1024L
    val maxTotalBytes = 512L * 1024L * 1024L

    ZipInputStream(BufferedInputStream(input)).use { zis ->
        while (true) {
            val entry = zis.nextEntry ?: break
            count++
            if (count > maxEntries) throw IOException("الأرشيف يحتوي على عدد عناصر يتجاوز الحد الآمن")
            val clean = entry.name.replace('\\\\', '/')
            val parts = clean.split('/').filter { it.isNotBlank() }
            if (clean.startsWith("/") || clean.startsWith("../") ||
                parts.any { it == "." || it == ".." } || parts.isEmpty()
            ) {
                zis.closeEntry()
                continue
            }

            var directory = destination
            val directoryParts = if (entry.isDirectory) parts else parts.dropLast(1)
            for (part in directoryParts) {
                directory = directory.findFile(part) ?: directory.createDirectory(part)
                    ?: throw IOException("تعذر إنشاء المجلد $part")
            }

            if (!entry.isDirectory) {
                val outputName = uniqueName(directory, parts.last())
                val outputFile = directory.createFile("application/octet-stream", outputName)
                    ?: throw IOException("تعذر إنشاء الملف $outputName")
                try {
                    val output = context.contentResolver.openOutputStream(outputFile.uri)
                        ?: throw IOException("تعذر الكتابة إلى $outputName")
                    output.use { stream ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var entryBytes = 0L
                        while (true) {
                            val read = zis.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalBytes += read
                            if (entryBytes > maxEntryBytes || totalBytes > maxTotalBytes) {
                                throw IOException("تجاوز الأرشيف حدود الحجم الآمن")
                            }
                            stream.write(buffer, 0, read)
                        }
                    }
                } catch (e: Exception) {
                    outputFile.delete()
                    throw e
                }
            }
            zis.closeEntry()
        }
    }
    return "اكتمل فك الضغط: $count عنصر"
}

private fun copyItem(context: Context, source: DocumentFile, destination: DocumentFile, newName: String): String {
    if (source.isDirectory) {
        val safeName = uniqueName(destination, newName)
        val newDirectory = destination.createDirectory(safeName)
            ?: throw IOException("تعذر إنشاء مجلد النسخة")
        source.listFiles().forEach { child ->
            copyItem(context, child, newDirectory, child.name ?: "item")
        }
    } else {
        val safeName = uniqueName(destination, newName)
        val outputFile = destination.createFile(source.type ?: "application/octet-stream", safeName)
            ?: throw IOException("تعذر إنشاء نسخة الملف")
        try {
            val input = context.contentResolver.openInputStream(source.uri)
                ?: throw IOException("تعذر قراءة الملف الأصلي")
            val output = context.contentResolver.openOutputStream(outputFile.uri)
                ?: throw IOException("تعذر إنشاء ملف النسخة")
            input.use { sourceStream -> output.use { targetStream -> sourceStream.copyTo(targetStream) } }
        } catch (e: Exception) {
            outputFile.delete()
            throw e
        }
    }
    return "تم نسخ العنصر بنجاح"
}
