package com.filemax.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.documentfile.provider.DocumentFile
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.filemax.app.archive.ZipEngine
import com.filemax.app.data.FileRepository
import com.filemax.app.util.FileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.security.MessageDigest

private val Brand = Color(0xFFF3D76B)
private val Ink = Color(0xFF252525)
private val Canvas = Color(0xFFF5F5F2)

private fun hasSharedStorageAccess(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
    else {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val write = Build.VERSION.SDK_INT > 28 || ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        read && write
    }

/** Main FILE MAX activity: permission onboarding, real file explorer and operations. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = FileRepository(this)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFB58D16), secondary = Brand, background = Canvas)) {
            FileMaxApp(repository)
        } }
    }
}

@Composable
private fun FileMaxApp(repository: FileRepository) {
    val context = LocalContext.current
    val activity = context as Activity
    var hasAccess by remember { mutableStateOf(hasSharedStorageAccess(context)) }
    var currentPath by remember { mutableStateOf(Environment.getExternalStorageDirectory()) }
    var files by remember { mutableStateOf(emptyList<File>()) }
    var safCurrent by remember { mutableStateOf<DocumentFile?>(null) }
    var safEntries by remember { mutableStateOf(emptyList<DocumentFile>()) }
    var selected by remember { mutableStateOf(setOf<File>()) }
    var showHidden by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("files") }
    var dialog by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(false) }
    var grid by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("الاسم") }

    val refresh = {
        hasAccess = hasSharedStorageAccess(context)
        files = if (hasAccess) repository.list(currentPath, showHidden) else emptyList()
        safEntries = if (!hasAccess && safCurrent != null) runCatching { safCurrent!!.listFiles().filter { showHidden || !it.name.orEmpty().startsWith(".") }.sortedWith(compareBy<DocumentFile>({ !it.isDirectory }, { it.name.orEmpty().lowercase() })) }.getOrDefault(emptyList()) else emptyList()
        selected = selected.intersect(files.toSet())
    }
    LaunchedEffect(hasAccess, currentPath, showHidden, safCurrent) { refresh() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            safCurrent = DocumentFile.fromTreeUri(context, uri)
            message = "تم فتح المجلد المختار. في الوضع المحدود يمكنك تصفح هذا المجلد ومجلداته الفرعية."
        }
    }

    val legacyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        hasAccess = result[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (!hasAccess) message = "لم يتم منح إذن قراءة الملفات. يمكنك إعادة المحاولة من الإعدادات."
    }

    fun requestFullAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))
            runCatching { activity.startActivity(intent) }.onFailure { activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
        } else if (Build.VERSION.SDK_INT >= 23) {
            legacyPermission.launch(if (Build.VERSION.SDK_INT <= 28) arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    LaunchedEffect(Unit) { if (!hasAccess) requestFullAccess() }

    fun runFileAction(action: suspend () -> String) {
        if (busy) return
        busy = true
        (context as ComponentActivity).lifecycleScope.launch {
            message = runCatching { withContext(Dispatchers.IO) { action() } }.fold({ it }, { it.message ?: "حدث خطأ" })
            busy = false
            refresh()
        }
    }

    val shownFiles = files.filter { it.name.contains(query, ignoreCase = true) }.let { list ->
        when (sortMode) { "الحجم" -> list.sortedByDescending { if (it.isDirectory) -1L else it.length() }; "التاريخ" -> list.sortedByDescending { it.lastModified() }; else -> list.sortedBy { it.name.lowercase() } }
    }
    fun openFile(file: File) {
        if (file.isDirectory) { currentPath = file; query = ""; selected = emptySet() }
        else if (file.extension.lowercase() in listOf("zip", "jar", "apk")) { input = file.name; selected = setOf(file); dialog = "archiveMenu" }
        else {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(contentUri, "*/*"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            runCatching { context.startActivity(intent) }.onFailure { message = "لا يوجد تطبيق مناسب لفتح هذا الملف" }
        }
    }
    val archives = files.filter { it.isFile && it.extension.lowercase() in listOf("zip", "jar", "apk") }

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Brand, background = Color(0xFF171717), surface = Color(0xFF242424), onSurface = Color.White, onBackground = Color.White) else lightColorScheme(primary = Color(0xFFB58D16), secondary = Brand, background = Canvas, surface = Color.White, onSurface = Ink)) {
    Column(Modifier.fillMaxSize().background(if (dark) Color(0xFF171717) else Canvas)) {
        Row(Modifier.fillMaxWidth().background(if (dark) Color(0xFF242424) else Color.White).padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("FILE MAX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (dark) Color.White else Ink)
                Text("مدير الملفات والأرشيفات", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "تحديث", tint = if (dark) Color.White else Ink) }
            IconButton(onClick = { tab = "settings" }) { Icon(Icons.Default.Settings, "الإعدادات", tint = if (dark) Color.White else Ink) }
        }
        if (!hasAccess && safCurrent == null) {
            Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FolderOpen, null, Modifier.size(68.dp), tint = Color(0xFFB58D16))
                Spacer(Modifier.height(16.dp))
                Text("اسمح بالوصول إلى الملفات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (dark) Color.White else Ink)
                Text("يحتاج FILE MAX إلى إذن إدارة الملفات المشتركة لعرض مجلدات الهاتف وملفات Download وDCIM وغيرها. لن يتم رفع ملفاتك إلى الإنترنت.", color = Color.Gray)
                Spacer(Modifier.height(18.dp))
                Button(onClick = { requestFullAccess() }, colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Ink)) { Text("منح الوصول الكامل") }
                OutlinedButton(onClick = { treePicker.launch(null) }) { Text("اختيار مجلد يدوياً") }
                Text("إذا اخترت رفض الإذن، يمكنك تصفح المجلدات التي تمنح التطبيق إذناً لها فقط.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            when (tab) {
                "files" -> {
                    if (!hasAccess && safCurrent != null) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { safCurrent = safCurrent?.parentFile }) { Icon(Icons.Default.ArrowBack, "رجوع") }
                            Text(safCurrent?.name ?: "المجلد المختار", Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "تحديث") }
                        }
                        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 14.dp), placeholder = { Text("ابحث في هذا المجلد") }, singleLine = true)
                        LazyColumn(Modifier.weight(1f)) {
                            items(safEntries.filter { it.name.orEmpty().contains(query, true) }, key = { it.uri.toString() }) { doc ->
                                SafFileRow(doc, dark) { if (doc.isDirectory) { safCurrent = doc; query = "" } else {
                                    val mime = doc.type ?: "*/*"
                                    val intent = Intent(Intent.ACTION_VIEW).setDataAndType(doc.uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    runCatching { context.startActivity(intent) }.onFailure { message = "لا يوجد تطبيق مناسب لفتح الملف" }
                                } }
                            }
                        }
                    } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (currentPath.parentFile != null && currentPath.path != Environment.getExternalStorageDirectory().path) { currentPath = currentPath.parentFile!!; selected = emptySet() } }) { Icon(Icons.Default.ArrowBack, "رجوع") }
                        Text(currentPath.absolutePath, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { grid = !grid }) { Icon(if (grid) Icons.Default.ViewList else Icons.Default.GridView, "تغيير العرض") }
                    }
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 14.dp), placeholder = { Text("ابحث في هذا المجلد") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallAction("مجلد جديد", Icons.Default.CreateNewFolder) { input = ""; dialog = "folder" }
                        SmallAction("ملف جديد", Icons.Default.NoteAdd) { input = ""; dialog = "newfile" }
                        SmallAction("ضغط ZIP", Icons.Default.Archive) { if (selected.isEmpty()) message = "حدد ملفاً أو مجلداً أولاً" else { input = "archive.zip"; dialog = "zip" } }
                        SmallAction("ترتيب: $sortMode", Icons.Default.Sort) { sortMode = when (sortMode) { "الاسم" -> "الحجم"; "الحجم" -> "التاريخ"; else -> "الاسم" } }
                        SmallAction("إظهار المخفي", Icons.Default.Visibility) { showHidden = !showHidden }
                    }
                    if (selected.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${selected.size} محدد", Modifier.weight(1f), color = if (dark) Color.White else Ink)
                            if (selected.size == 1) TextButton(onClick = { val first = selected.first(); input = first.name; dialog = "rename" }) { Text("إعادة تسمية") }
                            TextButton(onClick = { input = currentPath.absolutePath; dialog = "copy" }) { Text("نسخ") }
                            TextButton(onClick = { input = currentPath.absolutePath; dialog = "move" }) { Text("نقل") }
                            TextButton(onClick = { dialog = "delete" }) { Text("حذف", color = Color(0xFFB3261E)) }
                            if (selected.size == 1) TextButton(onClick = { dialog = "properties" }) { Text("خصائص") }
                            if (selected.size == 1) TextButton(onClick = { dialog = "extract" }) { Text("فك ZIP") }
                            TextButton(onClick = {
                                runCatching {
                                    val uris = ArrayList(selected.map { FileProvider.getUriForFile(context, "${context.packageName}.provider", it) })
                                    val share = if (uris.size == 1) Intent(Intent.ACTION_SEND).apply { type = "*/*"; putExtra(Intent.EXTRA_STREAM, uris[0]); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                    else Intent(Intent.ACTION_SEND_MULTIPLE).apply { type = "*/*"; putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                    context.startActivity(Intent.createChooser(share, "مشاركة الملفات"))
                                }.onFailure { message = it.message ?: "تعذرت المشاركة" }
                            }) { Text("مشاركة") }
                        }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Brand)
                    if (grid) {
                        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f)) {
                            items(shownFiles, key = { it.absolutePath }) { file ->
                                FileGridCard(file, selected.contains(file), dark, { selected = if (selected.contains(file)) selected - file else selected + file }) { openFile(file) }
                            }
                        }
                    } else {
                        LazyColumn(Modifier.weight(1f)) {
                            items(shownFiles, key = { it.absolutePath }) { file ->
                                FileRow(file, selected.contains(file), dark, onSelect = { selected = if (selected.contains(file)) selected - file else selected + file }, onOpen = { openFile(file) })
                            }
                        }
                    }
                    }
                }
                "home" -> HomeScreen(files, currentPath, { tab = "files" }, { currentPath = Environment.getExternalStorageDirectory(); tab = "files" })
                "archives" -> {
                    Text("الأرشيفات المتاحة", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    LazyColumn { items(archives) { file -> FileRow(file, selected.contains(file), dark, { selected = setOf(file) }, { selected = setOf(file); input = file.name; dialog = "archiveMenu" }) } }
                }
                "tools" -> ToolsScreen(currentPath, { message = it })
                "settings" -> SettingsScreen(showHidden, { showHidden = it }, dark, { dark = it }, { requestFullAccess() }, { treePicker.launch(null) })
            }
        }
        NavigationBar(containerColor = if (dark) Color(0xFF242424) else Color.White) {
            listOf(Triple("home", "الرئيسية", Icons.Default.Home), Triple("files", "الملفات", Icons.Default.Folder), Triple("archives", "الأرشيفات", Icons.Default.Archive), Triple("tools", "الأدوات", Icons.Default.Build), Triple("settings", "الإعدادات", Icons.Default.Settings)).forEach { (key, label, icon) ->
                NavigationBarItem(selected = tab == key, onClick = { tab = key }, icon = { Icon(icon, null) }, label = { Text(label) })
            }
        }
    }

    if (message.isNotBlank()) AlertDialog(onDismissRequest = { message = "" }, confirmButton = { TextButton(onClick = { message = "" }) { Text("حسناً") } }, title = { Text("FILE MAX") }, text = { Text(message) })

    if (dialog.isNotBlank()) {
        val title = when (dialog) { "folder" -> "إنشاء مجلد"; "newfile" -> "إنشاء ملف"; "rename" -> "إعادة تسمية"; "delete" -> "تأكيد الحذف"; "zip" -> "إنشاء أرشيف ZIP"; "extract" -> "فك الأرشيف"; "archiveMenu" -> "خيارات الأرشيف"; "copy" -> "نسخ إلى مجلد"; "move" -> "نقل إلى مجلد"; "properties" -> "خصائص العنصر"; else -> "الإعدادات" }
        AlertDialog(onDismissRequest = { dialog = "" }, title = { Text(title) }, text = {
            when (dialog) {
                "folder", "newfile", "rename", "zip" -> OutlinedTextField(input, { input = it }, label = { Text("الاسم") }, singleLine = true)
                "copy", "move" -> OutlinedTextField(input, { input = it }, label = { Text("مسار مجلد الوجهة") }, singleLine = true)
                "properties" -> {
                    val file = selected.firstOrNull()
                    if (file != null) Text("الاسم: ${file.name}\nالمسار: ${file.absolutePath}\nالنوع: ${FileFormat.kind(file)}\nالحجم: ${if (file.isDirectory) "مجلد" else FileFormat.size(file.length())}\nآخر تعديل: ${Date(file.lastModified())}\nقابل للقراءة: ${file.canRead()}\nقابل للكتابة: ${file.canWrite()}") else Text("لم يتم تحديد ملف")
                }
                "delete" -> Text("سيتم حذف ${selected.size} عنصر فعلياً من مساحة التخزين. لا يمكن التراجع عن ذلك.")
                "extract" -> Text("سيتم استخراج الأرشيف المحدد إلى مجلد جديد بجانب الملف.")
                "archiveMenu" -> Text("افتح الأرشيف أو استخرجه إلى مجلد جديد. يدعم هذا الإصدار ZIP فقط.")
                else -> Text("إعدادات FILE MAX")
            }
        }, confirmButton = {
            TextButton(onClick = {
                when (dialog) {
                    "properties" -> Unit
                    "folder" -> runFileAction { repository.createFolder(currentPath, input); "تم إنشاء المجلد" }
                    "newfile" -> runFileAction { repository.createFile(currentPath, input); "تم إنشاء الملف" }
                    "rename" -> runFileAction { require(selected.size == 1); repository.rename(selected.first(), input); selected = emptySet(); "تمت إعادة التسمية" }
                    "delete" -> runFileAction { selected.forEach { check(repository.delete(it)) { "تعذر حذف ${it.name}" } }; selected = emptySet(); "تم الحذف" }
                    "copy" -> runFileAction { val destination = File(input); require(destination.isDirectory) { "مسار الوجهة غير موجود أو ليس مجلداً" }; selected.forEach { repository.copy(it, destination) }; selected = emptySet(); "اكتملت عملية النسخ" }
                    "move" -> runFileAction { val destination = File(input); require(destination.isDirectory) { "مسار الوجهة غير موجود أو ليس مجلداً" }; selected.forEach { repository.move(it, destination) }; selected = emptySet(); "اكتملت عملية النقل" }
                    "zip" -> runFileAction { val out = File(currentPath, input); ZipEngine.create(selected.toList(), out); selected = emptySet(); "تم إنشاء ${out.name} بنجاح" }
                    "extract" -> runFileAction { val source = selected.firstOrNull() ?: error("حدد أرشيف ZIP"); require(source.extension.equals("zip", true)) { "يدعم هذا الإصدار استخراج ZIP فقط" }; val dest = File(source.parentFile, source.nameWithoutExtension); val count = ZipEngine.extract(source, dest); "تم استخراج $count ملفاً إلى ${dest.name}" }
                    "archiveMenu" -> { val source = selected.firstOrNull(); if (source != null) { if (source.extension.equals("zip", true)) { dialog = "extract"; return@TextButton } else message = "هذه الصيغة غير مدعومة حالياً في محرك ZIP" } }
                }
                dialog = ""
            }) { Text(if (dialog == "delete") "حذف" else if (dialog == "properties") "إغلاق" else "تنفيذ") }
        }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("إلغاء") } })
    }
    }
}

@Composable
private fun SmallAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)) { Icon(icon, null, Modifier.size(16.dp)); Spacer(Modifier.width(3.dp)); Text(label, style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun FileRow(file: File, selected: Boolean, dark: Boolean, onSelect: () -> Unit, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = { onSelect() })
        Icon(if (file.isDirectory) Icons.Default.Folder else when (file.extension.lowercase()) { "zip", "7z", "rar" -> Icons.Default.Archive; "jpg", "png", "jpeg", "webp" -> Icons.Default.Image; "mp4", "mkv" -> Icons.Default.VideoFile; "mp3", "wav" -> Icons.Default.AudioFile; "pdf" -> Icons.Default.PictureAsPdf; else -> Icons.Default.InsertDriveFile }, null, Modifier.size(29.dp), tint = if (file.isDirectory) Color(0xFFB58D16) else if (dark) Color.LightGray else Color.DarkGray)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, color = if (dark) Color.White else Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(if (file.isDirectory) "مجلد" else "${FileFormat.size(file.length())}  •  ${FileFormat.kind(file)}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Text(SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(file.lastModified())), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }
    HorizontalDivider(color = if (dark) Color(0xFF333333) else Color(0xFFE9E9E5))
}

@Composable
private fun SafFileRow(file: DocumentFile, dark: Boolean, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile, null, tint = if (file.isDirectory) Color(0xFFB58D16) else Color.Gray)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name ?: "عنصر بدون اسم", color = if (dark) Color.White else Ink)
            Text(if (file.isDirectory) "مجلد" else FileFormat.size(file.length()), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
        Text("فتح", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }
    HorizontalDivider()
}

@Composable
private fun FileGridCard(file: File, selected: Boolean, dark: Boolean, onSelect: () -> Unit, onOpen: () -> Unit) {
    Card(Modifier.padding(6.dp).fillMaxWidth().clickable(onClick = onOpen), colors = CardDefaults.cardColors(containerColor = if (dark) Color(0xFF292929) else Color.White)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile, null, Modifier.size(34.dp), tint = if (file.isDirectory) Color(0xFFB58D16) else Color.Gray)
            Spacer(Modifier.height(6.dp))
            Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis, color = if (dark) Color.White else Ink)
            Text(if (file.isDirectory) "مجلد" else FileFormat.size(file.length()), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            Checkbox(selected, { onSelect() })
        }
    }
}

@Composable
private fun HomeScreen(files: List<File>, path: File, goFiles: () -> Unit, goRoot: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("مرحباً بك في FILE MAX", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("إدارة ملفاتك من مكان واحد", color = Color.Gray)
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable(onClick = goRoot)) {
            Column(Modifier.padding(18.dp)) { Text("التخزين الداخلي", fontWeight = FontWeight.Bold); Text(path.absolutePath, color = Color.Gray); Text("${files.size} عنصر في المجلد الحالي") }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = goFiles, colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = Ink)) { Icon(Icons.Default.FolderOpen, null); Text("تصفح الملفات") }
        Text("الوظائف الأساسية تعمل محلياً على الجهاز.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ToolsScreen(path: File, onResult: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val digest = withContext(Dispatchers.IO) {
                val md = MessageDigest.getInstance("SHA-256")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) { val n = input.read(buffer); if (n < 0) break; md.update(buffer, 0, n) }
                } ?: error("تعذر قراءة الملف")
                md.digest().joinToString("") { "%02x".format(it) }
            }
            onResult("SHA-256\n$digest")
        }
    }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("الأدوات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        ToolCard("تحليل المجلد الحالي", "حساب عدد الملفات والمجلدات والحجم الإجمالي") {
            scope.launch(Dispatchers.IO) {
                val all = path.walkTopDown().filter { it.isFile }.toList()
                withContext(Dispatchers.Main) { onResult("عدد الملفات: ${all.size}\nالحجم الإجمالي: ${FileFormat.size(all.sumOf { it.length() })}") }
            }
        }
        ToolCard("حاسبة SHA-256", "اختر أي ملف لحساب بصمته") { picker.launch(arrayOf("*/*")) }
        ToolCard("البحث عن الملفات الكبيرة", "عرض أكبر 10 ملفات في المجلد الحالي") {
            scope.launch(Dispatchers.IO) {
                val biggest = path.walkTopDown().filter { it.isFile }.sortedByDescending { it.length() }.take(10).joinToString("\n") { "${it.name}: ${FileFormat.size(it.length())}" }
                withContext(Dispatchers.Main) { onResult(biggest.ifBlank { "لم يتم العثور على ملفات" }) }
            }
        }
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall) } }
}

@Composable
private fun SettingsScreen(hidden: Boolean, onHidden: (Boolean) -> Unit, dark: Boolean, onDark: (Boolean) -> Unit, onPermission: () -> Unit, onSaf: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("الإعدادات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("إظهار الملفات المخفية", Modifier.weight(1f)); Switch(hidden, onHidden) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("الوضع الداكن", Modifier.weight(1f)); Switch(dark, onDark) }
        HorizontalDivider()
        Text("التخزين والأذونات", Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold)
        Button(onClick = onPermission) { Text("إدارة إذن الوصول الكامل") }
        OutlinedButton(onClick = onSaf) { Text("اختيار مجلد عبر أداة أندرويد") }
        Text("FILE MAX لا يرفع ملفاتك. الوصول يقتصر على المواقع التي تسمح بها أندرويد.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(18.dp))
        Text("الإصدار 1.0.0 • FILE MAX", color = Color.Gray)
    }
}
