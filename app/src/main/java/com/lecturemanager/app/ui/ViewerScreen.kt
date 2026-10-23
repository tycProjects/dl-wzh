package com.lecturemanager.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lecturemanager.app.MainViewModel
import com.lecturemanager.app.data.Annotation
import com.lecturemanager.app.data.Bookmark
import com.lecturemanager.app.data.Lecture
import com.lecturemanager.app.data.PageHit
import com.lecturemanager.app.ui.theme.*
import com.lecturemanager.app.util.decodePoints
import com.lecturemanager.app.util.encodePoints
import com.lecturemanager.app.util.normalizeArabic
import com.lecturemanager.app.util.snippet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private val INK_COLORS = listOf(0xFFFACC15, 0xFF22C55E, 0xFFEC4899, 0xFF3B82F6, 0xFFEF4444, 0xFF111827)
private const val TOOL_PEN = 0
private const val TOOL_HIGHLIGHT = 1
private const val TOOL_ERASER = 2

/**
 * غلاف لـ PdfRenderer: صفحة واحدة مفتوحة في كل مرة (لذا Mutex)، والرسم على خيط خلفي،
 * مع ذاكرة تخزين مؤقت للصفحات المرسومة حتى لا يُعاد رسمها عند التمرير ذهاباً وإياباً.
 */
private class PdfHolder(path: String) {
    private val pfd = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(pfd)
    private val mutex = Mutex()
    @Volatile private var closed = false

    val pageCount = renderer.pageCount
    private val ratios = FloatArray(pageCount)
    private var defaultRatio = 0.707f
    /** يزداد كلما وصلت نسب أبعاد جديدة، فتُعاد قياسات الصفحات في الواجهة. */
    var version by mutableIntStateOf(0)

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 5).toInt().coerceIn(32 * 1024 * 1024, 160 * 1024 * 1024)
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun ratio(i: Int) = ratios.getOrNull(i)?.takeIf { it > 0f } ?: defaultRatio
    fun cached(i: Int, w: Int): Bitmap? = cache.get("$i@$w")

    /** نسب أبعاد الصفحات [from, to) تُحسب مسبقاً حتى لا تقفز القائمة أثناء التمرير. */
    suspend fun loadRatios(from: Int, to: Int) = withContext(Dispatchers.IO) {
        for (i in from until minOf(to, pageCount)) {
            mutex.withLock {
                if (closed) return@withContext
                renderer.openPage(i).use { p -> ratios[i] = p.width.toFloat() / p.height }
            }
            if (i == 0) defaultRatio = ratios[0]
            if (i % 40 == 39) version++
        }
        version++
    }

    suspend fun render(index: Int, widthPx: Int): Bitmap? {
        cached(index, widthPx)?.let { return it }
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                if (closed) return@withLock null
                cached(index, widthPx)?.let { return@withLock it }
                renderer.openPage(index).use { page ->
                    val h = (widthPx * page.height / page.width.toFloat()).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(widthPx, h, Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(AColor.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put("$index@$widthPx", bmp)
                    bmp
                }
            }
        }
    }

    fun close() = runBlocking {
        mutex.withLock {
            closed = true
            cache.evictAll()
            runCatching { renderer.close(); pfd.close() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(vm: MainViewModel, id: Long, startPage: Int, onBack: () -> Unit, onOpenOther: (Long) -> Unit) {
    val lecture by remember(id) { vm.lecture(id) }.collectAsStateWithLifecycle(initialValue = null)
    val all by vm.lectures.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val annotations by remember(id) { vm.annotations(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val bookmarks by remember(id) { vm.bookmarks(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val timer by vm.timer.collectAsStateWithLifecycle()
    val l = lecture
    if (l == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Teal) }
        return
    }
    val subject = subjects.firstOrNull { it.id == l.subjectId }
    val accent = adaptAccent(subject?.let { Color(it.themeColor.toInt()) } ?: Navy)

    val ctx = LocalContext.current
    val view = LocalView.current
    var fullscreen by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableStateOf(0) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var night by rememberSaveable { mutableStateOf(Pal.dark) }
    var tool by rememberSaveable { mutableStateOf(-1) }          // -1 = وضع الرسم متوقف
    var inkColor by rememberSaveable { mutableStateOf(INK_COLORS[0]) }
    var jump by remember { mutableStateOf<Int?>(null) }
    val initialPage = remember(id) { if (startPage >= 0) startPage else l.lastPage }

    BackHandler(enabled = fullscreen) { fullscreen = false }
    DisposableEffect(fullscreen) {
        val window = (ctx.findActivity() as? Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        if (fullscreen) ctrl?.hide(WindowInsetsCompat.Type.systemBars()) else ctrl?.show(WindowInsetsCompat.Type.systemBars())
        onDispose { ctrl?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    DisposableEffect(timer.running) {
        view.keepScreenOn = timer.running
        onDispose { view.keepScreenOn = false }
    }

    val siblings = all.filter { it.subjectId == l.subjectId }.sortedWith(compareBy({ it.weekNumber }, { it.sortOrder }, { it.id }))
    val next = siblings.getOrNull(siblings.indexOfFirst { it.id == l.id } + 1)
    val hasPdf = l.pdfPath != null

    Column(Modifier.fillMaxSize().background(Canvas)) {
        if (!fullscreen) {
            TopAppBar(
                title = {
                    Column {
                        Text(l.title, maxLines = 1, color = Navy, style = MaterialTheme.typography.titleMedium)
                        subject?.let { Text("${it.title} • الأسبوع ${l.weekNumber}", maxLines = 1, color = Slate, style = MaterialTheme.typography.labelMedium) }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع") } },
                actions = {
                    if (hasPdf) IconButton(onClick = { tab = 0; tool = if (tool < 0) TOOL_HIGHLIGHT else -1 }) {
                        Icon(Icons.Filled.Brush, "الرسم والتظليل", tint = if (tool >= 0) accent else Slate)
                    }
                    if (timer.running && timer.lectureId == l.id) {
                        TextButton(onClick = { showTimer = true }) {
                            Text(formatClock(timer.seconds), color = if (timer.work) accent else Ochre, style = MaterialTheme.typography.labelLarge)
                        }
                    } else IconButton(onClick = { showTimer = true }) { Icon(Icons.Filled.Timer, "مؤقّت المذاكرة", tint = Slate) }
                    if (hasPdf) IconButton(onClick = { fullscreen = true; tab = 0 }) { Icon(Icons.Filled.Fullscreen, "ملء الشاشة") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "المزيد") }
                        DropdownMenu(menu, { menu = false }) {
                            if (hasPdf) {
                                DropdownMenuItem(text = { Text("بحث داخل الملف") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                                    onClick = { menu = false; showSearch = true })
                                DropdownMenuItem(text = { Text("الإشارات المرجعية") }, leadingIcon = { Icon(Icons.Filled.Bookmarks, null) },
                                    onClick = { menu = false; showBookmarks = true })
                                DropdownMenuItem(
                                    text = { Text(if (night) "إيقاف وضع القراءة الليلي" else "وضع القراءة الليلي") },
                                    leadingIcon = { Icon(Icons.Filled.NightsStay, null) },
                                    onClick = { menu = false; night = !night },
                                )
                                DropdownMenuItem(
                                    text = { Text("مشاركة الملف") }, leadingIcon = { Icon(Icons.Filled.Share, null) },
                                    onClick = {
                                        menu = false
                                        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", File(l.pdfPath!!))
                                        val send = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        ctx.startActivity(Intent.createChooser(send, "مشاركة المحاضرة"))
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("حذف المحاضرة", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardBg),
            )
            Box(Modifier.fillMaxWidth().height(3.dp).background(accent))
            TabRow(selectedTabIndex = tab, containerColor = CardBg, contentColor = accent) {
                Tab(tab == 0, { tab = 0 }, text = { Text("الملف") }, selectedContentColor = accent, unselectedContentColor = Slate)
                Tab(tab == 1, { tab = 1 }, text = { Text("الملاحظات") }, selectedContentColor = accent, unselectedContentColor = Slate)
            }
        }
        Box(Modifier.weight(1f)) {
            if (tab == 0 || fullscreen) {
                if (hasPdf) PdfPane(
                    path = l.pdfPath!!, lectureId = l.id, accent = accent, initialPage = initialPage,
                    annotations = annotations, bookmarks = bookmarks, tool = tool, inkColor = inkColor, night = night,
                    jump = jump, onJumpDone = { jump = null },
                    onPageChanged = { p, n -> vm.saveProgress(l.id, p, n) },
                    onAddAnnotation = { page, t, c, w, pts ->
                        vm.addAnnotation(Annotation(lectureId = l.id, page = page, tool = t, color = c, width = w, points = pts))
                    },
                    onErase = { vm.deleteAnnotation(it) },
                    onToggleBookmark = { page ->
                        val existing = bookmarks.firstOrNull { it.page == page }
                        if (existing != null) vm.deleteBookmark(existing) else vm.addBookmark(l.id, page)
                    },
                    onShowBookmarks = { showBookmarks = true },
                    modifier = Modifier.fillMaxSize(),
                ) else NoPdf { vm.attachPdf(l.id, it) }
                if (fullscreen) FilledTonalIconButton(
                    onClick = { fullscreen = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                ) { Icon(Icons.Filled.FullscreenExit, "إنهاء ملء الشاشة") }
            } else NotesPane(vm, l, accent, subject?.title ?: "")
        }
        if (!fullscreen && tab == 0 && tool >= 0) AnnotationBar(
            tool = tool, color = inkColor, accent = accent,
            onTool = { tool = it }, onColor = { inkColor = it },
            onUndo = { annotations.maxByOrNull { it.id }?.let { vm.deleteAnnotation(it.id) } },
            onClose = { tool = -1 },
        )
        if (!fullscreen) Surface(shadowElevation = 8.dp, color = CardBg) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.setCompleted(l.id, !l.isCompleted) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    border = BorderStroke(1.5.dp, accent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                ) { Text(if (l.isCompleted) "إلغاء الإنجاز" else "تحديد كمكتملة") }
                Button(
                    onClick = {
                        vm.setCompleted(l.id, true)
                        if (next != null) onOpenOther(next.id) else onBack()
                    },
                    modifier = Modifier.weight(1.4f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
                ) { Text(if (next != null) "إتمام والتالية" else "إتمام") }
            }
        }
    }

    if (confirmDelete) ConfirmDialog(
        "حذف المحاضرة", "سيتم حذف «${l.title}» وملفها نهائياً.",
        onConfirm = { confirmDelete = false; vm.deleteLecture(l); onBack() },
        onDismiss = { confirmDelete = false },
    )
    if (showSearch) SearchInFileDialog(vm, l, onJump = { jump = it; tab = 0 }, onDismiss = { showSearch = false })
    if (showBookmarks) BookmarksDialog(
        bookmarks, onJump = { jump = it; tab = 0; showBookmarks = false },
        onDelete = { vm.deleteBookmark(it) }, onDismiss = { showBookmarks = false },
    )
    if (showTimer) TimerDialog(vm, l, onDismiss = { showTimer = false })
}

private fun formatClock(sec: Int): String = "%02d:%02d".format(sec / 60, sec % 60)

// ---------- نوافذ مساعدة ----------

@Composable
private fun SearchInFileDialog(vm: MainViewModel, l: Lecture, onJump: (Int) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<PageHit>>(emptyList()) }
    LaunchedEffect(q) {
        delay(250)
        hits = if (q.trim().length >= 2) vm.searchInLecture(l.id, q) else emptyList()
    }
    val nq = normalizeArabic(q)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("بحث داخل الملف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(q, { q = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("كلمة أو جملة") })
                if (!l.indexed) Text("جارٍ فهرسة الملف في الخلفية… قد تكون النتائج ناقصة الآن.", style = MaterialTheme.typography.labelMedium, color = Ochre)
                else if (q.trim().length >= 2 && hits.isEmpty()) Text("لا توجد نتائج.", color = Slate)
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(hits) { h ->
                        Column(Modifier.fillMaxWidth().clickable { onJump(h.page); onDismiss() }.padding(vertical = 8.dp)) {
                            Text("صفحة ${h.page + 1}", style = MaterialTheme.typography.labelLarge, color = Teal)
                            Text(snippet(h.text, nq), style = MaterialTheme.typography.bodySmall, color = Navy, maxLines = 3)
                        }
                        HorizontalDivider(color = Border)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}

@Composable
private fun BookmarksDialog(items: List<Bookmark>, onJump: (Int) -> Unit, onDelete: (Bookmark) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإشارات المرجعية") },
        text = {
            if (items.isEmpty()) Text("لا توجد إشارات. اضغط أيقونة الإشارة أسفل الملف لإضافة الصفحة الحالية.", color = Slate)
            else LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(items) { b ->
                    Row(Modifier.fillMaxWidth().clickable { onJump(b.page) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bookmark, null, tint = Ochre)
                        Spacer(Modifier.width(10.dp))
                        Text("صفحة ${b.page + 1}", Modifier.weight(1f), color = Navy)
                        IconButton(onClick = { onDelete(b) }) { Icon(Icons.Filled.Close, "حذف الإشارة", tint = Slate) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}

@Composable
private fun TimerDialog(vm: MainViewModel, l: Lecture, onDismiss: () -> Unit) {
    val t by vm.timer.collectAsStateWithLifecycle()
    val cfg by vm.settings.collectAsStateWithLifecycle()
    val mine = t.running && t.lectureId == l.id
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مؤقّت المذاكرة") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (mine) {
                    Text(formatClock(t.seconds), style = MaterialTheme.typography.headlineLarge, color = if (t.work) Teal else Ochre)
                    Text(
                        if (!t.pomodoro) "مذاكرة حرة (يُسجَّل الوقت عند الإيقاف)" else if (t.work) "فترة مذاكرة" else "استراحة",
                        color = Slate, style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text("اختر النمط، وسيُسجَّل وقت مذاكرتك في الإحصائيات.", color = Slate, style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { vm.startTimer(l.id, l.subjectId, true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyFill, contentColor = Color.White),
                    ) { Text("بومودورو (${cfg.pomodoroWork} د مذاكرة + ${cfg.pomodoroBreak} د راحة)") }
                    OutlinedButton(onClick = { vm.startTimer(l.id, l.subjectId, false) }, modifier = Modifier.fillMaxWidth()) { Text("مؤقّت حر") }
                }
            }
        },
        confirmButton = { if (mine) TextButton(onClick = { vm.stopTimer(); onDismiss() }) { Text("إيقاف وحفظ", color = Teal) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
    )
}

@Composable
private fun AnnotationBar(
    tool: Int, color: Long, accent: Color,
    onTool: (Int) -> Unit, onColor: (Long) -> Unit, onUndo: () -> Unit, onClose: () -> Unit,
) {
    Surface(color = CardBg, shadowElevation = 4.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(TOOL_PEN to "قلم", TOOL_HIGHLIGHT to "تظليل", TOOL_ERASER to "ممحاة").forEach { (t, label) ->
                    FilterChip(
                        selected = tool == t, onClick = { onTool(t) }, label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = accent.copy(alpha = 0.18f), selectedLabelColor = accent),
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onUndo) { Text("تراجع") }
                TextButton(onClick = onClose) { Text("تم", color = accent) }
            }
            if (tool != TOOL_ERASER) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                INK_COLORS.forEach { c ->
                    Box(
                        Modifier.size(30.dp).background(Color(c.toInt()), RoundedCornerShape(50))
                            .then(if (c == color) Modifier.background(Color.Transparent) else Modifier)
                            .clickable { onColor(c) },
                        contentAlignment = Alignment.Center,
                    ) { if (c == color) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NoPdf(onPicked: (android.net.Uri) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("لا يوجد ملف PDF مرفق بهذه المحاضرة.", color = Slate, modifier = Modifier.padding(bottom = 12.dp))
        PdfPickerButtons(onPicked)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotesPane(vm: MainViewModel, l: Lecture, accent: Color, subjectTitle: String) {
    var notes by remember(l.id) { mutableStateOf(l.notes) }
    var tag by remember(l.id) { mutableStateOf(l.tag) }
    var saved by remember(l.id) { mutableStateOf(true) }
    val latest by rememberUpdatedState(notes to tag)

    LaunchedEffect(notes, tag) {
        if (notes != l.notes || tag != l.tag) {
            saved = false
            delay(600)
            vm.saveNotes(l.id, notes, tag)
            saved = true
        }
    }
    DisposableEffect(l.id) { onDispose { vm.saveNotes(l.id, latest.first, latest.second) } }

    Column(
        Modifier.fillMaxSize().background(accent.copy(alpha = 0.07f)).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.6f)),
        ) {
            Row(Modifier.fillMaxWidth().background(accent).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.EditNote, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (subjectTitle.isNotBlank()) "ملاحظات • $subjectTitle" else "ملاحظاتي",
                    color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1,
                )
                Text(if (saved) "تم الحفظ ✓" else "جارٍ الحفظ…", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الوسم", style = MaterialTheme.typography.labelLarge, color = accent)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TAGS.forEach { t ->
                        FilterChip(
                            selected = tag == t, onClick = { tag = if (tag == t) "" else t }, label = { Text(t) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.18f), selectedLabelColor = accent,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = tag == t,
                                borderColor = Border, selectedBorderColor = accent,
                            ),
                        )
                    }
                }
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 12,
                    placeholder = { Text("اكتب ملاحظاتك هنا… تُحفظ تلقائياً", color = SlateLight) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent, unfocusedBorderColor = accent.copy(alpha = 0.3f),
                        cursorColor = accent, focusedContainerColor = accent.copy(alpha = 0.04f),
                        unfocusedContainerColor = accent.copy(alpha = 0.04f),
                        focusedTextColor = Navy, unfocusedTextColor = Navy,
                    ),
                )
                OutlinedButton(
                    onClick = { vm.generateCards(l, notes) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    border = BorderStroke(1.5.dp, accent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                ) { Icon(Icons.Filled.Style, null); Spacer(Modifier.width(8.dp)); Text("إنشاء بطاقات مراجعة من الملاحظات") }
                Text(
                    "الصيغ المدعومة: «مصطلح :: تعريف» أو «س: السؤال ج: الجواب» (كل بطاقة في سطر أو سطرين).",
                    style = MaterialTheme.typography.labelMedium, color = Slate,
                )
            }
        }
    }
}

// ---------- عارض الـ PDF ----------

@Composable
private fun PdfPane(
    path: String, lectureId: Long, accent: Color, initialPage: Int,
    annotations: List<Annotation>, bookmarks: List<Bookmark>, tool: Int, inkColor: Long, night: Boolean,
    jump: Int?, onJumpDone: () -> Unit,
    onPageChanged: (Int, Int) -> Unit,
    onAddAnnotation: (page: Int, tool: Int, color: Long, width: Float, points: String) -> Unit,
    onErase: (Long) -> Unit, onToggleBookmark: (Int) -> Unit, onShowBookmarks: () -> Unit,
    modifier: Modifier,
) {
    var holder by remember(path) { mutableStateOf<PdfHolder?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    val holderRef = rememberUpdatedState(holder)

    LaunchedEffect(path) {
        val h = withContext(Dispatchers.IO) { runCatching { PdfHolder(path) }.getOrNull() }
        if (h == null || h.pageCount == 0) { failed = true; return@LaunchedEffect }
        // نعرض الملف بعد أول دفعة من الصفحات، ونكمل بقية النسب في الخلفية (مهم للملفات الضخمة)
        h.loadRatios(0, maxOf(40, initialPage + 20))
        holder = h
        h.loadRatios(maxOf(40, initialPage + 20), h.pageCount)
    }
    DisposableEffect(path) { onDispose { holderRef.value?.close() } }

    val h = holder
    if (failed) { Box(modifier, Alignment.Center) { Text("تعذّر فتح الملف", color = Slate) }; return }
    if (h == null) { Box(modifier, Alignment.Center) { CircularProgressIndicator(color = Teal) }; return }
    PdfViewer(
        h, accent, initialPage, annotations, bookmarks, tool, inkColor, night, jump, onJumpDone,
        onPageChanged, onAddAnnotation, onErase, onToggleBookmark, onShowBookmarks, modifier,
    )
}

@Composable
private fun PdfViewer(
    h: PdfHolder, accent: Color, initialPage: Int,
    annotations: List<Annotation>, bookmarks: List<Bookmark>, tool: Int, inkColor: Long, night: Boolean,
    jump: Int?, onJumpDone: () -> Unit,
    onPageChanged: (Int, Int) -> Unit,
    onAddAnnotation: (page: Int, tool: Int, color: Long, width: Float, points: String) -> Unit,
    onErase: (Long) -> Unit, onToggleBookmark: (Int) -> Unit, onShowBookmarks: () -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val count = h.pageCount

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var viewW by remember { mutableIntStateOf(0) }
    var viewH by remember { mutableIntStateOf(0) }
    var renderBucket by remember { mutableIntStateOf(1) }
    var showGoto by remember { mutableStateOf(false) }
    var restored by remember { mutableStateOf(false) }
    val drawing = tool >= 0
    val toolState by rememberUpdatedState(tool)

    // بعد توقف التكبير نعيد رسم الصفحات بدقة أعلى حتى يبقى النص حاداً.
    LaunchedEffect(Unit) {
        snapshotFlow { scale }.collectLatest { s -> delay(250); renderBucket = if (s > 1.3f) 2 else 1 }
    }

    val currentPage by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val mid = (info.viewportEndOffset - info.viewportStartOffset) / (2f * scale)
            (info.visibleItemsInfo.firstOrNull { it.offset + it.size > mid } ?: info.visibleItemsInfo.lastOrNull())?.index ?: 0
        }
    }

    // العودة لآخر صفحة قُرئت، ثم حفظ التقدّم عند التوقف عن التمرير
    LaunchedEffect(Unit) {
        if (initialPage > 0) listState.scrollToItem(initialPage.coerceIn(0, count - 1))
        restored = true
    }
    LaunchedEffect(Unit) {
        snapshotFlow { currentPage }.filter { restored }.collectLatest { p -> delay(800); onPageChanged(p, count) }
    }
    LaunchedEffect(jump) {
        jump?.let { listState.scrollToItem(it.coerceIn(0, count - 1)); onJumpDone() }
    }
    // تحميل مسبق للصفحة التالية لسلاسة التصفح
    LaunchedEffect(currentPage, viewW, renderBucket) {
        if (viewW > 0 && restored) {
            val w = (viewW * renderBucket).coerceAtLeast(400)
            if (currentPage + 1 < count) runCatching { h.render(currentPage + 1, w) }
        }
    }

    /** يطبّق تكبيراً/تحريكاً مع إبقاء نقطة اللمس ثابتة. التحريك العمودي يتم عبر تمرير القائمة نفسها. */
    fun transform(zoom: Float, pan: Offset, centroid: Offset) {
        val old = scale
        val new = (old * zoom).coerceIn(1f, 4f)
        val minX = -viewW * (new - 1f)
        offsetX = (centroid.x - (centroid.x - offsetX) * (new / old) + pan.x).coerceIn(minX, 0f)
        val dy = centroid.y / old - (centroid.y + pan.y) / new
        scale = new
        listState.dispatchRawDelta(dy)
        if (new == 1f) offsetX = 0f
    }

    val byPage = remember(annotations) { annotations.groupBy { it.page } }
    val bookmarked = bookmarks.any { it.page == currentPage }

    Column(modifier.background(PdfBg)) {
        Box(
            Modifier.weight(1f).fillMaxWidth()
                .onSizeChanged { viewW = it.width; viewH = it.height }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        do {
                            val e = awaitPointerEvent(PointerEventPass.Initial)
                            val pressed = e.changes.count { it.pressed }
                            if (pressed >= 2) {
                                transform(e.calculateZoom(), e.calculatePan(), e.calculateCentroid(useCurrent = true))
                                e.changes.forEach { if (it.positionChanged()) it.consume() }
                            } else if (pressed == 1 && scale > 1.01f && toolState < 0) {
                                transform(1f, e.calculatePan(), e.calculateCentroid(useCurrent = true))
                                e.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (e.changes.any { it.pressed })
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { pos ->
                        if (toolState >= 0) return@detectTapGestures
                        if (scale > 1.05f) transform(1f / scale, Offset.Zero, pos) else transform(2.5f, Offset.Zero, pos)
                    })
                },
        ) {
            val extraBottom = with(density) { (viewH * (1f - 1f / scale)).toDp() }
            LazyColumn(
                state = listState,
                userScrollEnabled = scale <= 1.01f && !drawing,
                modifier = Modifier.fillMaxSize().graphicsLayer(
                    scaleX = scale, scaleY = scale, translationX = offsetX,
                    transformOrigin = TransformOrigin(0f, 0f),
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp + extraBottom),
            ) {
                items(count, key = { it }) { i ->
                    PdfPage(
                        h, i, (viewW * renderBucket).coerceAtLeast(400), byPage[i].orEmpty(), tool, inkColor, night,
                        onAdd = { t, c, w, pts -> onAddAnnotation(i, t, c, w, pts) }, onErase = onErase,
                    )
                }
            }
            if (scale > 1.05f) FilledTonalButton(
                onClick = { transform(1f / scale, Offset.Zero, Offset.Zero) },
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) { Icon(Icons.Filled.ZoomOutMap, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("${(scale * 100).toInt()}٪") }
        }
        Surface(color = CardBg, shadowElevation = 6.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { scope.launch { listState.animateScrollToItem((currentPage - 1).coerceAtLeast(0)) } }, enabled = currentPage > 0) {
                    Icon(Icons.Filled.KeyboardArrowRight, "الصفحة السابقة")
                }
                Surface(
                    shape = RoundedCornerShape(8.dp), color = accent.copy(alpha = 0.12f),
                    modifier = Modifier.clickable { showGoto = true },
                ) {
                    Text("${currentPage + 1} / $count", color = accent, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                IconButton(onClick = { scope.launch { listState.animateScrollToItem((currentPage + 1).coerceAtMost(count - 1)) } }, enabled = currentPage < count - 1) {
                    Icon(Icons.Filled.KeyboardArrowLeft, "الصفحة التالية")
                }
                if (count > 1) Slider(
                    value = currentPage.toFloat(),
                    onValueChange = { v -> scope.launch { listState.scrollToItem(v.toInt().coerceIn(0, count - 1)) } },
                    valueRange = 0f..(count - 1).toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                ) else Spacer(Modifier.weight(1f))
                IconButton(onClick = { onToggleBookmark(currentPage) }) {
                    Icon(if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "إشارة مرجعية", tint = if (bookmarked) Ochre else Slate)
                }
                IconButton(onClick = onShowBookmarks) { Icon(Icons.Filled.Bookmarks, "قائمة الإشارات", tint = Slate) }
            }
        }
    }

    if (showGoto) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGoto = false },
            title = { Text("الانتقال إلى صفحة") },
            text = {
                OutlinedTextField(
                    text, { text = it.filter(Char::isDigit).take(5) }, singleLine = true,
                    label = { Text("رقم الصفحة (1 – $count)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    text.toIntOrNull()?.let { n -> scope.launch { listState.scrollToItem((n - 1).coerceIn(0, count - 1)) } }
                    showGoto = false
                }) { Text("انتقال") }
            },
            dismissButton = { TextButton(onClick = { showGoto = false }) { Text("إلغاء") } },
        )
    }
}

private val INVERT = ColorFilter.colorMatrix(
    ColorMatrix(floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f,
    ))
)

@Composable
private fun PdfPage(
    h: PdfHolder, index: Int, widthPx: Int, annots: List<Annotation>, tool: Int, inkColor: Long, night: Boolean,
    onAdd: (tool: Int, color: Long, width: Float, points: String) -> Unit, onErase: (Long) -> Unit,
) {
    // نحتفظ بآخر صورة معروضة أثناء إعادة الرسم بدقة أعلى لتجنّب الوميض.
    var bmp by remember(h, index) { mutableStateOf(h.cached(index, widthPx)) }
    LaunchedEffect(h, index, widthPx) {
        h.cached(index, widthPx)?.let { bmp = it; return@LaunchedEffect }
        val r = runCatching { h.render(index, widthPx) }.getOrNull()
        if (r != null) bmp = r
    }
    val b = bmp
    val ratio = run { h.version; h.ratio(index) }
    Surface(
        shape = RoundedCornerShape(4.dp), color = if (night) Color(0xFF111111) else Color.White, shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (b != null) Image(
                b.asImageBitmap(), "صفحة ${index + 1}", Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds, colorFilter = if (night) INVERT else null,
            ) else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal, modifier = Modifier.size(28.dp))
            }
            AnnotationLayer(annots, tool, inkColor, onAdd, onErase, Modifier.fillMaxSize())
        }
    }
}

/** طبقة الرسم فوق الصفحة. الإحداثيات نسبية (0..1) حتى تبقى الرسوم في مكانها مع أي تكبير أو دوران. */
@Composable
private fun AnnotationLayer(
    items: List<Annotation>, tool: Int, inkColor: Long,
    onAdd: (tool: Int, color: Long, width: Float, points: String) -> Unit, onErase: (Long) -> Unit,
    modifier: Modifier,
) {
    val decoded = remember(items) { items.map { it to decodePoints(it.points) } }
    var live by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val gesture = if (tool < 0) Modifier else Modifier.pointerInput(tool, inkColor, decoded) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            val w = size.width.toFloat().coerceAtLeast(1f)
            val h = size.height.toFloat().coerceAtLeast(1f)
            val pts = mutableListOf(down.position)
            val erased = mutableSetOf<Long>()
            fun erase(p: Offset) {
                val r = maxOf(w, h) * 0.02f
                for ((a, ps) in decoded) {
                    if (a.id in erased) continue
                    if (ps.any { (x, y) -> (x * w - p.x) * (x * w - p.x) + (y * h - p.y) * (y * h - p.y) <= r * r }) {
                        erased += a.id; onErase(a.id)
                    }
                }
            }
            if (tool == TOOL_ERASER) erase(down.position) else live = pts.toList()
            var cancelled = false
            while (true) {
                val ev = awaitPointerEvent()
                if (ev.changes.size > 1) { cancelled = true; break }   // إصبع ثانٍ = تكبير، نلغي الخط
                val ch = ev.changes.first()
                if (!ch.pressed) break
                if (ch.positionChanged()) {
                    ch.consume()
                    if (tool == TOOL_ERASER) erase(ch.position)
                    else {
                        val last = pts.last()
                        if ((ch.position - last).getDistance() >= 2f) { pts += ch.position; live = pts.toList() }
                    }
                }
            }
            live = emptyList()
            if (!cancelled && tool != TOOL_ERASER && pts.isNotEmpty()) {
                val width = if (tool == TOOL_HIGHLIGHT) 0.026f else 0.004f
                onAdd(tool, inkColor, width, encodePoints(FloatArray(pts.size) { pts[it].x / w }, FloatArray(pts.size) { pts[it].y / h }))
            }
        }
    }

    Canvas(modifier.then(gesture)) {
        fun draw(points: List<Offset>, toolType: Int, color: Color, widthFrac: Float) {
            val alpha = if (toolType == TOOL_HIGHLIGHT) 0.35f else 1f
            val sw = widthFrac * size.width
            if (points.size == 1) {
                drawCircle(color, radius = sw / 2f, center = points[0], alpha = alpha)
            } else if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                }
                drawPath(path, color, alpha = alpha, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        for ((a, ps) in decoded) {
            draw(ps.map { Offset(it.first * size.width, it.second * size.height) }, a.tool, Color(a.color.toInt()), a.width)
        }
        if (live.isNotEmpty() && tool >= 0 && tool != TOOL_ERASER) {
            draw(live, tool, Color(inkColor.toInt()), if (tool == TOOL_HIGHLIGHT) 0.026f else 0.004f)
        }
    }
}
