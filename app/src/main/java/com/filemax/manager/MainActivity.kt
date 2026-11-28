package com.filemax.manager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File

class MainActivity : ComponentActivity() {
    var incoming by mutableStateOf<File?>(null)
    var openFolder by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent { FileMaxApp(this) }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleIntent(intent) }

    /** Archives opened from other apps (file:// or content://) are copied to cache and browsed in place. */
    private fun handleIntent(i: Intent?) {
        if (i?.action == ACTION_OPEN_FOLDER) { openFolder = i.getStringExtra("path"); return }
        val uri = i?.data ?: return
        if (i.action != Intent.ACTION_VIEW) return
        Thread {
            runCatching {
                val f = if (uri.scheme == "file") File(uri.path!!) else {
                    val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "archive"
                    val dir = File(cacheDir, "incoming").apply { deleteRecursively(); mkdirs() }
                    File(dir, name.replace('/', '_')).also { out -> contentResolver.openInputStream(uri)!!.use { ins -> out.outputStream().use { ins.copyTo(it) } } }
                }
                runOnUiThread { incoming = f }
            }
        }.start()
    }
}

private fun archiveBase(name: String): String {
    val n = name.removeSuffix(".001")
    return listOf(".tar.gz", ".tar.bz2", ".tar.xz").firstOrNull { n.endsWith(it, true) }?.let { n.dropLast(it.length) } ?: n.substringBeforeLast('.')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileMaxApp(activity: MainActivity) {
    val context = LocalContext.current
    val repo = remember { FileRepository(context) }
    val prefs = remember { context.getSharedPreferences("filemax", 0) }
    val scope = rememberCoroutineScope()
    remember { Lang.apply(prefs.getString("lang", "system")); 0 }
    val runner = remember { TaskRunner(AppScope.scope, context.applicationContext) }
    var current by remember { mutableStateOf<StorageNode?>(null) }
    var root by remember { mutableStateOf<StorageNode?>(null) }
    var navigationStack by remember { mutableStateOf(listOf<StorageNode>()) }
    var entries by remember { mutableStateOf<List<StorageNode>>(emptyList()) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showHidden by remember { mutableStateOf(prefs.getBoolean("hidden", false)) }
    var grid by remember { mutableStateOf(prefs.getBoolean("grid", false)) }
    var themeMode by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
    var favs by remember { mutableStateOf(prefs.getStringSet("favs", emptySet()) ?: emptySet()) }
    var search by remember { mutableStateOf("") }
    var recursive by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<StorageNode>?>(null) }
    var screen by remember { mutableStateOf("main") }
    var menu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var createMenu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var pendingArchive by remember { mutableStateOf<File?>(null) }
    var safTarget by remember { mutableStateOf<StorageNode.Saf?>(null) }
    var pendingContext by remember { mutableStateOf<StorageNode?>(null) }
    var browse by remember { mutableStateOf<File?>(null) }
    var editFile by remember { mutableStateOf<File?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var propFiles by remember { mutableStateOf<List<File>?>(null) }
    var pwPrompt by remember { mutableStateOf<((CharArray) -> Unit)?>(null) }
    var pwWrong by remember { mutableStateOf(false) }
    var pwKey by remember { mutableStateOf<String?>(null) }
    var clipboard by remember { mutableStateOf<List<StorageNode>>(emptyList()) }
    var clipboardMove by remember { mutableStateOf(false) }

    fun key(n: StorageNode): String = when (n) { is StorageNode.Local -> n.file.absolutePath; is StorageNode.Saf -> n.document.uri.toString(); is StorageNode.Priv -> n.path }
    fun policy() = when (prefs.getString("policy", "ask")) { "ask" -> ExistsPolicy.ASK; "skip" -> ExistsPolicy.SKIP; "overwrite" -> ExistsPolicy.OVERWRITE; else -> ExistsPolicy.RENAME }
    fun refresh() {
        val dir = current ?: return
        entries = runCatching {
            val found = repo.children(dir, showHidden)
            when (prefs.getString("sort", "الاسم")) {
                "الحجم" -> found.sortedWith(compareBy<StorageNode> { !it.isDirectory }.thenByDescending { it.size })
                "التاريخ" -> found.sortedWith(compareBy<StorageNode> { !it.isDirectory }.thenByDescending { it.modified })
                "النوع" -> found.sortedWith(compareBy<StorageNode> { !it.isDirectory }.thenBy { it.name.substringAfterLast('.', "").lowercase() }.thenBy { it.name.lowercase() })
                else -> found
            }
        }.onFailure { message = it.message ?: "تعذر قراءة المجلد" }.getOrDefault(emptyList())
        selected = selected.intersect(entries.map(::key).toSet())
    }
    fun reload() { Lang.apply(prefs.getString("lang", "system")); showHidden = prefs.getBoolean("hidden", false); grid = prefs.getBoolean("grid", false); themeMode = prefs.getString("theme", "system") ?: "system"; refresh() }
    fun navigate(node: StorageNode) { current = node; navigationStack = navigationStack + node; search = ""; selected = emptySet(); refresh() }
    fun up() {
        if (navigationStack.size > 1) { navigationStack = navigationStack.dropLast(1); current = navigationStack.last(); selected = emptySet(); refresh() }
        else if (root != null) { current = root; refresh() }
    }
    fun useRoot(node: StorageNode) { root = node; current = node; navigationStack = listOf(node); screen = "main"; search = ""; selected = emptySet(); refresh() }
    fun goLocal(f: File) { screen = "main"; useRoot(StorageNode.Local(f)) }
    fun openPrivPath(path: String) {
        runner.run("اتصال Root/Shizuku", { _ -> Priv.connect(context).label }) { r ->
            r.fold({ useRoot(StorageNode.Priv(path, true, 0, 0)) }, { message = it.message ?: "تعذر الاتصال" })
        }
    }
    fun openPriv(sub: String) = openPrivPath(Environment.getExternalStorageDirectory().absolutePath + "/Android/" + sub)

    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            runCatching { useRoot(repo.safRoot(uri)) }.onFailure { message = it.message ?: "تعذر فتح المجلد" }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) useRoot(repo.defaultRoot()) else if (current == null) treePicker.launch(null)
    }
    val legacyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { g ->
        if (g[Manifest.permission.READ_EXTERNAL_STORAGE] == true) useRoot(repo.defaultRoot()) else treePicker.launch(null)
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 30) {
            if (Environment.isExternalStorageManager()) useRoot(repo.defaultRoot())
            else permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}")))
        } else if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) useRoot(repo.defaultRoot())
        else legacyLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(activity.openFolder) {
        activity.openFolder?.let { p -> activity.openFolder = null; if (Priv.restricted(p)) openPrivPath(p) else if (File(p).isDirectory) goLocal(File(p)) }
    }
    LaunchedEffect(activity.incoming) { activity.incoming?.let { browse = it; activity.incoming = null } }
    LaunchedEffect(search, recursive, current) {
        val dir = (current as? StorageNode.Local)?.file
        if (recursive && search.length >= 2 && dir != null) {
            delay(400)
            searchResults = withContext(Dispatchers.IO) { runCatching { FileTools.searchRecursive(dir, search, showHidden).map { StorageNode.Local(it) as StorageNode } }.getOrDefault(emptyList()) }
        } else searchResults = null
    }
    BackHandler(enabled = screen != "main" || selected.isNotEmpty() || navigationStack.size > 1) {
        if (screen != "main") screen = if (screen in listOf("trash", "dups", "analyzer", "favs")) "tools" else "main"
        else if (selected.isNotEmpty()) selected = emptySet() else up()
    }

    val visible = searchResults ?: entries.filter { it.name.contains(search, true) }
    val selNodes = visible.filter { key(it) in selected }
    val dirFile = (current as? StorageNode.Local)?.file

    // ---- archive jobs (password aware) ----
    fun archiveJob(file: File, title: String, pw: CharArray?, work: (File, CharArray?, TaskControl) -> String) {
        runner.run(title, { c -> work(ArchiveReader.joinVolumes(file, context.cacheDir), pw?.copyOf(), c) }) { r ->
            r.fold({ message = it; refresh() }, { e -> when (e) {
                is PasswordException -> {
                    val saved = if (!e.wrong && pw == null) SecureStore.load(context, file.name) else null
                    if (saved != null) archiveJob(file, title, saved, work)
                    else { if (e.wrong) SecureStore.forget(context, file.name); pwWrong = e.wrong; pwKey = file.name; pwPrompt = { p -> archiveJob(file, title, p, work) } }
                }
                is CancelledByUser -> message = "أُلغيت العملية"
                else -> message = e.message ?: "فشلت العملية" } })
        }
    }
    fun extractArchive(f: File, toFolder: Boolean) {
        val saf = safTarget
        if (saf != null) {
            val tmp = File(context.cacheDir, "saf_out/${System.nanoTime()}")
            val dest = if (toFolder) File(tmp, archiveBase(f.name)) else tmp
            archiveJob(f, "استخراج ${f.name}", null) { src, pw, c ->
                try { val n = ArchiveReader.extract(src, dest, pw, null, false, policy(), c); tmp.listFiles()?.forEach { repo.copyTo(StorageNode.Local(it), saf, false) }; "تم استخراج $n عنصر إلى\n${saf.name}" }
                finally { tmp.deleteRecursively() }
            }
            return
        }
        val parent = f.parentFile ?: return
        val dest = if (toFolder) FileTools.uniqueFile(parent, archiveBase(f.name)) else parent
        archiveJob(f, "استخراج ${f.name}", null) { src, pw, c -> "تم استخراج ${ArchiveReader.extract(src, dest, pw, null, false, policy(), c)} عنصر إلى\n${dest.absolutePath}" }
    }
    fun open(node: StorageNode) {
        if (selected.isNotEmpty()) { selected = if (key(node) in selected) selected - key(node) else selected + key(node); return }
        if (node.isDirectory) { navigate(node); return }
        if (node is StorageNode.Priv) {
            fun openStaged(f2: File) { if (ArchiveReader.isArchiveName(f2.name)) { safTarget = null; pendingArchive = f2; dialog = "archiveMenu" } else openFile(context, f2) }
            runner.run("نسخ للمعالجة", { c -> c.label = node.name
                File(context.cacheDir, "priv_stage/${System.nanoTime()}").apply { mkdirs() }.let { d -> File(d, node.name).also { out ->
                    Priv.openRead(node.path).use { i -> out.outputStream().use { i.copyTo(it, 64 * 1024) } } } } }) { r ->
                r.fold({ openStaged(it) }, { message = it.message ?: "تعذر النسخ" }) }
            return
        }
        val f = (node as? StorageNode.Local)?.file
        if (f == null && ArchiveReader.isArchiveName(node.name)) {
            val doc = (node as StorageNode.Saf).document; val parentSaf = current as? StorageNode.Saf
            runner.run("نسخ للمعالجة", { c -> c.label = node.name
                File(context.cacheDir, "saf_stage/${System.nanoTime()}").apply { mkdirs() }.let { d -> File(d, node.name).also { out ->
                    context.contentResolver.openInputStream(doc.uri)!!.use { i -> out.outputStream().use { i.copyTo(it, 64 * 1024) } } } } }) { r ->
                r.fold({ safTarget = parentSaf; pendingArchive = it; dialog = "archiveMenu" }, { message = it.message ?: "تعذر النسخ" }) }
            return
        }
        if (f == null) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, (node as StorageNode.Saf).document.uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }; return }
        when {
            ArchiveReader.isArchiveName(f.name) -> { safTarget = null; pendingArchive = f; dialog = "archiveMenu" }
            isTextName(f.name) -> editFile = f
            isImageName(f.name) -> imageFile = f
            else -> openFile(context, f)
        }
    }
    fun toggle(n: StorageNode) { selected = if (key(n) in selected) selected - key(n) else selected + key(n) }

    val dark = themeMode == "dark" || (themeMode == "system" && isSystemInDarkTheme())
    val scheme = if (dark) darkColorScheme(primary = BrandYellow, onPrimary = Ink, background = Color(0xFF171717), surface = Color(0xFF222222), onSurface = Color.White,
        secondaryContainer = Color(0xFF4A3F10), surfaceVariant = Color(0xFF2B2B2B), onSurfaceVariant = Color(0xFFCCCCCC))
    else lightColorScheme(primary = Ink, background = Color.White, surface = Color.White, onSurface = Ink, secondaryContainer = PaleYellow, surfaceVariant = Soft, onSurfaceVariant = Color(0xFF555555))

    MaterialTheme(colorScheme = scheme) {
      CompositionLocalProvider(LocalLayoutDirection provides if (Lang.english) LayoutDirection.Ltr else LayoutDirection.Rtl) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val b = browse; val ef = editFile; val img = imageFile
            when {
                b != null -> androidx.compose.runtime.key(b) { ArchiveBrowserScreen(b, if (b.absolutePath.startsWith(context.cacheDir.absolutePath)) File(Environment.getExternalStorageDirectory(), "FileMax") else b.parentFile ?: b,
                    runner, policy(), repo, safTarget, onClose = { browse = null; refresh() }, onMessage = { message = it }) }
                ef != null -> TextEditorScreen(ef, { editFile = null }, { message = it })
                img != null -> ImageViewerScreen(img) { imageFile = null }
                screen == "settings" -> SettingsScreen(prefs, { screen = "main" }, { treePicker.launch(null) }, ::reload)
                screen == "tools" -> ToolsHome({ screen = "main" }) { screen = it }
                screen == "favs" -> FavoritesScreen(favs, { screen = "tools" }, { goLocal(it) }) { p -> favs = favs - p; prefs.edit().putStringSet("favs", favs).apply() }
                screen == "trash" -> TrashScreen({ screen = "tools" }) { message = it }
                screen == "dups" -> DuplicatesScreen(dirFile ?: Environment.getExternalStorageDirectory(), showHidden, { screen = "tools" }) { message = it }
                screen == "analyzer" -> AnalyzerScreen(dirFile ?: Environment.getExternalStorageDirectory(), { screen = "tools" }) { goLocal(it) }
                else -> Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().background(BrandYellow).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { up() }) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Ink) }
                        Image(painterResource(R.drawable.filemax_logo), null, Modifier.size(30.dp)); Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("File Max", fontWeight = FontWeight.Bold, color = Ink, fontSize = 15.sp)
                            Text(current?.let { when (it) { is StorageNode.Local -> it.file.absolutePath; is StorageNode.Saf -> it.document.uri.lastPathSegment ?: it.name; is StorageNode.Priv -> it.path } } ?: "اختر مجلداً",
                                color = Ink.copy(alpha = .72f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { selected = if (selected.size == visible.size) emptySet() else visible.map(::key).toSet() }) { Icon(Icons.Default.SelectAll, "تحديد الكل", tint = Ink) }
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Default.Menu, "القائمة", tint = Ink) }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                FileTools.storageRoots(context).forEachIndexed { i, r -> DropdownMenuItem(text = { Text(if (i == 0) "التخزين الداخلي" else "بطاقة SD (${r.name})") }, leadingIcon = { Icon(if (i == 0) Icons.Default.Storage else Icons.Default.SdCard, null) }, onClick = { menu = false; goLocal(r) }) }
                                DropdownMenuItem(text = { Text("Android/data (Root/Shizuku)") }, leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) }, onClick = { menu = false; openPriv("data") })
                                DropdownMenuItem(text = { Text("Android/obb (Root/Shizuku)") }, leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) }, onClick = { menu = false; openPriv("obb") })
                                DropdownMenuItem(text = { Text("الأدوات") }, leadingIcon = { Icon(Icons.Default.Build, null) }, onClick = { menu = false; screen = "tools" })
                                DropdownMenuItem(text = { Text("تحديث") }, leadingIcon = { Icon(Icons.Default.Refresh, null) }, onClick = { menu = false; refresh() })
                                DropdownMenuItem(text = { Text(if (grid) "عرض القائمة" else "عرض الشبكة") }, leadingIcon = { Icon(Icons.Default.ViewModule, null) }, onClick = { grid = !grid; prefs.edit().putBoolean("grid", grid).apply(); menu = false })
                                DropdownMenuItem(text = { Text("اختيار مجلد (SAF)") }, leadingIcon = { Icon(Icons.Default.FolderOpen, null) }, onClick = { menu = false; treePicker.launch(null) })
                                DropdownMenuItem(text = { Text("الإعدادات") }, leadingIcon = { Icon(Icons.Default.Settings, null) }, onClick = { menu = false; screen = "settings" })
                                DropdownMenuItem(text = { Text("حول File Max") }, leadingIcon = { Icon(Icons.Default.Info, null) }, onClick = { menu = false; dialog = "about" })
                                DropdownMenuItem(text = { Text("خروج") }, leadingIcon = { Icon(Icons.Default.ExitToApp, null) }, onClick = { menu = false; activity.finish() })
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Color.Gray); Spacer(Modifier.width(8.dp))
                        BasicTextField(value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f), singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.onSurface),
                            decorationBox = { inner -> if (search.isEmpty()) Text(if (recursive) "بحث داخل المجلدات" else "بحث في المجلد", color = Color.Gray); inner() })
                        FilterChip(selected = recursive, onClick = { recursive = !recursive }, label = { Text("متعمّق", fontSize = 11.sp) })
                        if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, null) }
                    }
                    if (current == null) {
                        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painterResource(R.drawable.filemax_logo), null, Modifier.size(96.dp))
                            Text("لم يتم منح الوصول إلى التخزين", fontWeight = FontWeight.Bold)
                            Text("اختر مجلداً للتصفح، أو امنح إذن الوصول الكامل من إعدادات أندرويد.", color = Color.Gray)
                            Button(onClick = { if (Build.VERSION.SDK_INT >= 30) permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}"))) else treePicker.launch(null) }) { Text("منح الوصول") }
                            OutlinedButton(onClick = { treePicker.launch(null) }) { Text("اختيار مجلد") }
                        }
                    } else {
                        if (visible.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(if (search.isEmpty()) "هذا المجلد فارغ" else "لا توجد نتائج", color = Color.Gray) }
                        else if (grid) LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp)) {
                            gridItems(visible, key = { key(it) }) { n -> FileGridTile(n, key(n) in selected, { toggle(n) }, { open(n) }, { selected = selected + key(n) }) }
                        } else LazyColumn(Modifier.weight(1f)) {
                            items(visible, key = { key(it) }) { n ->
                                val sub = if (n.isDirectory) "مجلد · ${fmtDate(n.modified)}" else "${formatBytes(n.size)} · ${fmtDate(n.modified)}"
                                FileRow(n, sub, key(n) in selected, { toggle(n) }, { open(n) }, { selected = selected + key(n) })
                            }
                        }
                        val free = runCatching { formatBytes((dirFile ?: Environment.getExternalStorageDirectory()).usableSpace) }.getOrDefault("—")
                        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${visible.size} عنصر" + if (selected.isNotEmpty()) " · محدد ${selected.size}" else "", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("المتاح $free", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selected.isNotEmpty()) Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(5.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            SmallAction("نسخ", Icons.Default.ContentCopy) { clipboard = selNodes; clipboardMove = false; selected = emptySet(); message = "تم تحديد ${clipboard.size} عنصر للنسخ؛ انتقل للوجهة واضغط لصق" }
                            SmallAction("نقل", Icons.Default.DriveFileMove) { clipboard = selNodes; clipboardMove = true; selected = emptySet(); message = "انتقل إلى المجلد المطلوب واضغط لصق" }
                            SmallAction("حذف", Icons.Default.Delete) { dialog = "delete" }
                            SmallAction("ضغط", Icons.Default.Archive) { dialog = "archive" }
                            SmallAction("مشاركة", Icons.Default.Share) { shareFiles(context, selNodes.mapNotNull(repo::localFile)) }
                            Box {
                                SmallAction("المزيد", Icons.Default.MoreVert) { moreMenu = true }
                                DropdownMenu(moreMenu, { moreMenu = false }) {
                                    if (selNodes.size == 1) DropdownMenuItem(text = { Text("إعادة تسمية") }, onClick = { moreMenu = false; pendingContext = selNodes[0]; input = selNodes[0].name; dialog = "rename" })
                                    if (selNodes.size > 1) DropdownMenuItem(text = { Text("إعادة تسمية جماعية") }, onClick = { moreMenu = false; dialog = "batch" })
                                    DropdownMenuItem(text = { Text("خصائص") }, onClick = { moreMenu = false; propFiles = selNodes.mapNotNull(repo::localFile).ifEmpty { null } })
                                    DropdownMenuItem(text = { Text("إضافة للمفضلة") }, onClick = { moreMenu = false
                                        favs = favs + selNodes.filter { it.isDirectory }.mapNotNull(repo::localFile).map { it.absolutePath }; prefs.edit().putStringSet("favs", favs).apply(); message = "تمت الإضافة للمفضلة" })
                                    DropdownMenuItem(text = { Text("عكس التحديد") }, onClick = { moreMenu = false; selected = visible.map(::key).toSet() - selected })
                                    DropdownMenuItem(text = { Text("إلغاء التحديد") }, onClick = { moreMenu = false; selected = emptySet() })
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            if (clipboard.isNotEmpty()) {
                                OutlinedButton(onClick = {
                                    val dest = current ?: return@OutlinedButton
                                    val items = clipboard; val mv = clipboardMove
                                    runner.run(if (mv) "نقل" else "نسخ", { c -> items.forEach { c.label = it.name; repo.copyTo(it, dest, mv, policy(), c) } }) { r ->
                                        message = r.fold({ "اكتملت العملية" }, { it.message ?: "فشلت العملية" }); if (r.isSuccess) clipboard = emptyList(); refresh() }
                                }) { Icon(Icons.Default.ContentPaste, null); Text("لصق (${clipboard.size})") }
                                TextButton(onClick = { clipboard = emptyList() }) { Text("إلغاء") }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box {
                                FilledTonalButton(onClick = { createMenu = true }, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = BrandYellow, contentColor = Ink), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("إنشاء", fontWeight = FontWeight.SemiBold)
                                }
                                DropdownMenu(expanded = createMenu, onDismissRequest = { createMenu = false }) {
                                    DropdownMenuItem(text = { Text("مجلد جديد") }, leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) }, onClick = { createMenu = false; input = ""; dialog = "folder" })
                                    DropdownMenuItem(text = { Text("ملف جديد") }, leadingIcon = { Icon(Icons.Default.InsertDriveFile, null) }, onClick = { createMenu = false; input = ""; dialog = "file" })
                                    DropdownMenuItem(text = { Text("أرشيف جديد") }, leadingIcon = { Icon(Icons.Default.Archive, null) }, onClick = { createMenu = false; if (selected.isEmpty()) message = "حدد الملفات أولاً ثم اختر إنشاء أرشيف" else dialog = "archive" })
                                }
                            }
                        }
                    }
                }
            }
        }
      }

      when (dialog) {
        "folder", "file" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text(if (dialog == "folder") "مجلد جديد" else "ملف جديد") },
            text = { OutlinedTextField(input, { input = it }, label = { Text("الاسم") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { runCatching { if (dialog == "folder") repo.createFolder(current!!, input) else repo.createFile(current!!, input) }.onFailure { message = it.message ?: "تعذر الإنشاء" }; dialog = ""; refresh() }) { Text("إنشاء") } },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text("إلغاء") } })
        "rename" -> AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("إعادة تسمية") },
            text = { OutlinedTextField(input, { input = it }, singleLine = true, label = { Text("الاسم الجديد") }) },
            confirmButton = { TextButton(onClick = { runCatching { pendingContext?.let { repo.rename(it, input) } }.onFailure { message = it.message ?: "تعذرت إعادة التسمية" }; dialog = ""; selected = emptySet(); refresh() }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { dialog = "" }) { Text("إلغاء") } })
        "delete" -> {
            val trash = prefs.getBoolean("trash", true)
            AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("حذف ${selNodes.size} عنصر؟") },
                text = { Text(if (trash) "ستنتقل العناصر إلى سلة المحذوفات ويمكن استرجاعها من الأدوات." else "سيتم الحذف نهائياً.") },
                confirmButton = { TextButton(onClick = {
                    val targets = selNodes; dialog = ""
                    runner.run("حذف", { c -> targets.forEach { c.label = it.name; val f = repo.localFile(it); if (trash && f != null) FileTools.moveToTrash(f) else repo.delete(it) } }) { r ->
                        r.exceptionOrNull()?.let { message = it.message ?: "فشل الحذف" }; selected = emptySet(); refresh() }
                }) { Text("حذف") } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("إلغاء") } })
        }
        "archive" -> {
            val locals = selNodes.mapNotNull(repo::localFile)
            val allLocal = locals.size == selNodes.size && dirFile != null
            val first = selNodes.firstOrNull()?.name ?: "Archive"
            CreateArchiveDialog(if (selNodes.size > 1) (current?.name ?: "Archive").ifBlank { "Archive" } else archiveBase(first).ifBlank { "Archive" },
                selNodes.any { it.isDirectory } || selNodes.size > 1, prefs.getString("fmt", "zip") ?: "zip", prefs.getInt("level", 5), { dialog = "" }) { name, opts ->
                dialog = ""
                val nodes = selNodes; val cur = current
                runner.run("إنشاء $name", { c ->
                    val t0 = System.currentTimeMillis()
                    val work = File(context.cacheDir, "saf_work/${System.nanoTime()}")
                    try {
                        val inputs: List<File>; val outDir: File
                        if (allLocal) { inputs = locals; outDir = dirFile!! }
                        else { val inDir = File(work, "in").apply { mkdirs() }; outDir = File(work, "out").apply { mkdirs() }
                            nodes.forEach { c.label = it.name; repo.copyTo(it, StorageNode.Local(inDir), false) }; inputs = inDir.listFiles().orEmpty().toList() }
                        val out = FileTools.uniqueFile(outDir, name)
                        ArchiveWriter.create(inputs, out, opts, c)
                        val parts = outDir.listFiles().orEmpty().filter { it.name == out.name || it.name.startsWith(out.name + ".") }
                        val size = parts.sumOf { it.length() }
                        if (!allLocal && cur != null) parts.forEach { repo.copyTo(StorageNode.Local(it), cur, false) }
                        "تم إنشاء ${out.name}\nالحجم: ${formatBytes(size)}\nالأصل: ${formatBytes(c.total)}" + (if (c.total > 0) " · نسبة ${size * 100 / c.total}%" else "") + "\nالوقت: ${(System.currentTimeMillis() - t0) / 1000.0} ث"
                    } finally { work.deleteRecursively() }
                }) { r -> r.fold({ message = it }, { message = if (it is CancelledByUser) "أُلغي الإنشاء" else it.message ?: "تعذر إنشاء الأرشيف" }); selected = emptySet(); refresh() }
            }
        }
        "archiveMenu" -> pendingArchive?.let { f ->
            AlertDialog(onDismissRequest = { dialog = "" }, title = { Text(f.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }, text = { Column {
                if (ArchiveReader.extensionMismatch(f)) Text("⚠ امتداد الملف لا يطابق محتواه (${ArchiveReader.detect(f).label})", color = Color(0xFFFB8C00), fontSize = 12.sp)
                TextButton(onClick = { dialog = ""; browse = f }) { Text("فتح الأرشيف (تصفح المحتويات)") }
                TextButton(onClick = { dialog = ""; extractArchive(f, false) }) { Text("استخراج هنا") }
                TextButton(onClick = { dialog = ""; extractArchive(f, true) }) { Text("استخراج إلى مجلد باسم الأرشيف") }
                TextButton(onClick = { dialog = ""; archiveJob(f, "اختبار السلامة", null) { s, pw, c -> "الأرشيف سليم ✓ (${ArchiveReader.test(s, pw, c)} عنصر)" } }) { Text("اختبار السلامة") }
            } }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("إغلاق") } })
        }
        "batch" -> BatchRenameDialog(selNodes.mapNotNull(repo::localFile), { dialog = "" }) { message = it; dialog = ""; selected = emptySet(); refresh() }
        "about" -> AlertDialog(onDismissRequest = { dialog = "" }, icon = { Image(painterResource(R.drawable.filemax_logo), null, Modifier.size(64.dp)) }, title = { Text("File Max") },
            text = { Text("مدير ملفات وأرشيف يعمل دون إنترنت.\nالإصدار 1.2.0") }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("إغلاق") } })
      }
      propFiles?.let { PropertiesDialog(it) { propFiles = null } }
      pwPrompt?.let { act -> PasswordDialog(pwWrong, onOk = { pwPrompt = null; act(it) }, onCancel = { pwPrompt = null }, archiveKey = pwKey) }
      TaskDialog(runner)
      if (message.isNotEmpty()) AlertDialog(onDismissRequest = { message = "" }, text = { Text(message) }, confirmButton = { TextButton(onClick = { message = "" }) { Text("حسناً") } })
    }
}

@Composable
private fun SettingsScreen(prefs: SharedPreferences, onBack: () -> Unit, onChooseFolder: () -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    val shizuku = remember { runCatching { Shizuku.pingBinder() }.getOrDefault(false) }
    fun bool(k: String, d: Boolean) = mutableStateOf(prefs.getBoolean(k, d))
    var hidden by remember { bool("hidden", false) }; var grid by remember { bool("grid", false) }; var trash by remember { bool("trash", true) }
    var theme by remember { mutableStateOf(prefs.getString("theme", "system")!!) }; var sort by remember { mutableStateOf(prefs.getString("sort", "الاسم")!!) }
    var lang by remember { mutableStateOf(prefs.getString("lang", "system")!!) }
    val langs = mapOf("system" to "حسب النظام", "ar" to "العربية", "en" to "English")
    var fmt by remember { mutableStateOf(prefs.getString("fmt", "zip")!!) }; var level by remember { mutableStateOf(prefs.getInt("level", 5)) }
    var policy by remember { mutableStateOf(prefs.getString("policy", "ask")!!) }
    val themes = mapOf("system" to "حسب النظام", "light" to "فاتح", "dark" to "داكن")
    val levels = linkedMapOf("تخزين فقط" to 0, "سريع" to 1, "عادي" to 5, "أقصى" to 9)
    val policies = mapOf("ask" to "اسأل كل مرة", "rename" to "إعادة تسمية تلقائية", "skip" to "تخطي", "overwrite" to "استبدال")
    fun put(block: SharedPreferences.Editor.() -> Unit) { prefs.edit().apply(block).apply(); onChanged() }
    Column(Modifier.fillMaxSize()) {
        TopBar("الإعدادات", onBack = onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item { SettingsHeader("مدير الملفات") }
            item { SettingSwitch("إظهار الملفات المخفية", hidden) { hidden = it; put { putBoolean("hidden", it) } } }
            item { SettingSwitch("عرض الشبكة", grid) { grid = it; put { putBoolean("grid", it) } } }
            item { SettingSwitch("سلة المحذوفات (حذف قابل للاسترجاع)", trash) { trash = it; put { putBoolean("trash", it) } } }
            item { SettingsChoice("المظهر", themes[theme] ?: "", themes.values.toList()) { v -> theme = themes.entries.first { it.value == v }.key; put { putString("theme", theme) } } }
            item { SettingsChoice("اللغة", langs[lang] ?: "", langs.values.toList()) { v -> lang = langs.entries.first { it.value == v }.key; put { putString("lang", lang) } } }
            item { SettingsChoice("ترتيب الملفات", sort, listOf("الاسم", "الحجم", "التاريخ", "النوع")) { sort = it; put { putString("sort", it) } } }
            item { SettingsHeader("الضغط والأرشفة") }
            item { SettingsChoice("الصيغة الافتراضية", fmt.uppercase(), ArchiveWriter.FORMATS.map { it.uppercase() }) { fmt = it.lowercase(); put { putString("fmt", fmt) } } }
            item { SettingsChoice("مستوى الضغط الافتراضي", levels.entries.first { it.value == level }.key, levels.keys.toList()) { level = levels[it] ?: 5; put { putInt("level", level) } } }
            item { SettingsChoice("عند وجود ملف بنفس الاسم في الاستخراج", policies[policy] ?: "", policies.values.toList()) { v -> policy = policies.entries.first { it.value == v }.key; put { putString("policy", policy) } } }
            item { SettingsInfo("الصيغ", "إنشاء: ZIP (AES-256/ZipCrypto) · 7Z · TAR · TAR.GZ/BZ2/XZ · GZ · BZ2 · XZ + تقسيم لأجزاء .001\nفك/تصفح: كل ما سبق + RAR/RAR5 · ARJ · JAR · APK · EPUB · 7Z المشفّر") }
            item { SettingsHeader("التخزين") }
            item { SettingsClick("اختيار مجلد للتصفح", "منح وصول عبر منتقي أندرويد", onChooseFolder) }
            item { SettingsHeader("ROOT و Shizuku") }
            item { SettingsInfo("حالة Shizuku", if (shizuku) "الخدمة متصلة" else "غير متصل") }
            item { SettingsClick("طلب صلاحية Shizuku", "إظهار طلب الإذن عندما تكون الخدمة متصلة") { runCatching { if (Shizuku.pingBinder()) Shizuku.requestPermission(41) } } }
            item { SettingsClick("فتح Shizuku", "إدارة الخدمة من التطبيق المثبت") { runCatching { context.startActivity(context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api") ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:moe.shizuku.privileged.api"))) } } }
            item { SettingsClick("مسح كلمات المرور المحفوظة", "حذف كل كلمات المرور المخزّنة بأمان في Keystore") { SecureStore.clear(context) } }
            item { SettingsInfo("Android/data و Android/obb", "من القائمة الرئيسية اختر Android/data أو Android/obb؛ يعمل عبر Shizuku (أو Root عند توفره)") }
            item { SettingsHeader("حول") }
            item { SettingsInfo("File Max", "الإصدار 1.2.0") }
            item { SettingsInfo("الخصوصية", "الملفات تبقى على جهازك؛ لا يوجد رفع للملفات ولا اتصال بالإنترنت") }
        }
    }
}
