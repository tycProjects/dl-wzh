package com.adam.phoneguardian

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import java.util.Locale

private val Navy = Color(0xFF07111F)
private val Panel = Color(0xFF101F32)
private val Cyan = Color(0xFF37D8F5)
private val Muted = Color(0xFF9FB3C8)
private val Good = Color(0xFF54E0A5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GuardianApp(this) }
    }
}

data class AppEntry(val label: String, val packageName: String, val requested: List<String>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianApp(activity: MainActivity) {
    var page by remember { mutableStateOf("home") }
    var scanDone by remember { mutableStateOf(false) }
    var scannedApps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var scanMessage by remember { mutableStateOf("لم يتم إجراء فحص بعد") }
    var link by remember { mutableStateOf("") }
    var linkResult by remember { mutableStateOf("") }
    var chosenTree by remember { mutableStateOf<DocumentFile?>(null) }
    var largeFiles by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }
    var cleanerMessage by remember { mutableStateOf("اختر مجلدًا بنفسك لتحليل الملفات داخله") }
    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                activity.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                chosenTree = DocumentFile.fromTreeUri(activity, uri)
                val found = mutableListOf<DocumentFile>()
                fun walk(dir: DocumentFile, depth: Int) {
                    if (depth > 4) return
                    dir.listFiles().forEach { f ->
                        if (f.isDirectory) walk(f, depth + 1)
                        else if (f.isFile && f.length() >= 50L * 1024 * 1024) found.add(f)
                    }
                }
                chosenTree?.let { walk(it, 0) }
                largeFiles = found
                cleanerMessage = "تم تحليل المجلد الذي اخترته. الملفات الكبيرة فقط معروضة؛ راجعها قبل الحذف."
            }.onFailure { cleanerMessage = "تعذر قراءة المجلد: ${it.localizedMessage ?: "خطأغيرمعروف"}" }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Navy, surface = Panel, onSurface = Color.White)) {
        Scaffold(containerColor = Navy, bottomBar = {
            NavigationBar(containerColor = Panel) {
                val items = listOf("home" to "الرئيسية", "apps" to "التطبيقات", "links" to "الروابط", "clean" to "التنظيف")
                items.forEach { (key, title) ->
                    NavigationBarItem(selected = page == key, onClick = { page = key }, icon = {
                        Icon(when (key) { "home" -> Icons.Default.Shield; "apps" -> Icons.Default.Apps; "links" -> Icons.Default.Link; else -> Icons.Default.DeleteSweep }, null)
                    }, label = { Text(title, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Cyan, indicatorColor = Cyan.copy(alpha = .14f), selectedTextColor = Cyan, unselectedTextColor = Muted))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                when (page) {
                    "home" -> HomePage(scanDone, scannedApps.size, scanMessage, onScan = {
                        scanMessage = "جارٍ فحص التطبيقات..."
                        runCatching {
                            val pm = activity.packageManager
                            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                                .filter { it.packageName != activity.packageName }
                                .map { info ->
                                    val label = runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(info.packageName)
                                    val perms = runCatching { pm.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions?.toList() ?: emptyList() }.getOrDefault(emptyList())
                                    AppEntry(label, info.packageName, perms)
                                }
                            scannedApps = apps
                            scanDone = true
                            val needsReview = apps.count { app -> app.requested.any { it in sensitivePermissions } }
                            scanMessage = "اكتمل الفحص: ${apps.size} تطبيقًا، و$needsReview تطبيقًا لديه إذن حساس واحد أو أكثر للمراجعة."
                        }.onFailure { scanMessage = "تعذر إكمال الفحص: ${it.localizedMessage ?: "خطأ"}" }
                    }, onNavigate = { page = it })
                    "apps" -> AppsPage(scannedApps, onOpen = { pkg ->
                        runCatching { activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))) }
                    }, onRefresh = {
                        val pm = activity.packageManager
                        scannedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA).map { info ->
                            AppEntry(runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(info.packageName), info.packageName,
                                runCatching { pm.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions?.toList() ?: emptyList() }.getOrDefault(emptyList()))
                        }
                    })
                    "links" -> LinksPage(link, { link = it }, linkResult, { linkResult = analyzeLink(link) })
                    "clean" -> CleanerPage(largeFiles, cleanerMessage, onPick = { treePicker.launch(null) }, onDelete = { file ->
                        if (file.delete()) { largeFiles = largeFiles.filterNot { it.uri == file.uri }; cleanerMessage = "تم حذف الملف الذي اخترته." }
                        else cleanerMessage = "لم يتم حذف الملف. قد لا يسمح مزود الملفات بذلك."
                    })
                    else -> HomePage(scanDone, scannedApps.size, scanMessage, {}, { page = it })
                }
            }
        }
    }
}

private val sensitivePermissions = setOf(
    "android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.ACCESS_FINE_LOCATION",
    "android.permission.ACCESS_COARSE_LOCATION", "android.permission.READ_CONTACTS", "android.permission.READ_CALL_LOG",
    "android.permission.READ_SMS", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.MANAGE_EXTERNAL_STORAGE"
)

@Composable
fun HomePage(done: Boolean, appCount: Int, message: String, onScan: () -> Unit, onNavigate: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(22.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, null, tint = Cyan, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("PHONE GUARDIAN PRO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("حارس الهاتف الذكي", color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(26.dp))
        Box(Modifier.size(206.dp).border(5.dp, Cyan.copy(alpha = .5f), CircleShape).padding(13.dp).border(2.dp, Cyan, CircleShape), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Security, null, tint = Cyan, modifier = Modifier.size(44.dp))
                Text(if (done) "تم الفحص" else "جاهز للفحص", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(if (done) "${appCount} تطبيق" else "حماية وخصوصية", color = Muted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onScan, colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Navy), modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.Search, null); Spacer(Modifier.width(8.dp)); Text("ابدأ فحص الهاتف", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Text(message, color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))
        Text("أدوات الحماية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(10.dp))
        ToolCard("مدير التطبيقات والأذونات", "راجع التطبيقات والصلاحيات الحساسة", Icons.Default.Apps) { onNavigate("apps") }
        ToolCard("فحص الروابط", "تحليل مؤشرات التصيد قبل فتح الرابط", Icons.Default.Link) { onNavigate("links") }
        ToolCard("منظف الملفات", "اعرض الملفات الكبيرة داخل مجلد تختاره", Icons.Default.DeleteSweep) { onNavigate("clean") }
        Spacer(Modifier.height(10.dp))
        Text("احمِ هاتفك، تحكم في خصوصيتك، وابقَ آمنًا.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun ToolCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 12.sp) }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
fun AppsPage(apps: List<AppEntry>, onOpen: (String) -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(20.dp)); Text("فحص التطبيقات", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("الأذونات الحساسة ليست دليلًا على وجود فيروس؛ راجع سبب استخدامها.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Navy)) { Text("تحديث القائمة") }
        if (apps.isEmpty()) Text("اضغط تحديث القائمة لعرض التطبيقات التي يستطيع النظام إظهارها.", color = Muted, modifier = Modifier.padding(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(apps, key = { it.packageName }) { app ->
                val sensitive = app.requested.filter { it in sensitivePermissions }
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(app.label, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(app.packageName, color = Muted, fontSize = 10.sp)
                        Text(if (sensitive.isEmpty()) "لم تُرصد أذونات ضمن قائمة المراجعة" else "أذونات تستحق المراجعة: ${sensitive.size}", color = if (sensitive.isEmpty()) Good else Cyan, fontSize = 12.sp)
                        sensitive.take(4).forEach { Text("• ${permissionLabel(it)}", color = Muted, fontSize = 11.sp) }
                        TextButton(onClick = { onOpen(app.packageName) }) { Text("فتح إعدادات التطبيق", color = Cyan) }
                    }
                }
            }
        }
    }
}

@Composable
fun LinksPage(link: String, onLink: (String) -> Unit, result: String, onCheck: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp)); Text("Link Scanner", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("فحص محلي لمؤشرات الرابط فقط؛ لا يتصل بقواعد بيانات تهديدات مباشرة.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(value = link, onValueChange = onLink, modifier = Modifier.fillMaxWidth(), label = { Text("الصق الرابط هنا") }, placeholder = { Text("https://example.com") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Cyan, unfocusedBorderColor = Muted, focusedLabelColor = Cyan))
        Spacer(Modifier.height(12.dp))
        Button(onClick = onCheck, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Navy)) { Text("افحص الرابط") }
        Spacer(Modifier.height(16.dp))
        if (result.isNotBlank()) Card(colors = CardDefaults.cardColors(containerColor = Panel)) { Text(result, color = Color.White, modifier = Modifier.padding(16.dp)) }
        Spacer(Modifier.height(12.dp)); Text("نصيحة: لا تدخل كلمة المرور أو بيانات الدفع في صفحة وصلت إليها من رسالة غير متوقعة.", color = Muted, fontSize = 13.sp)
    }
}

fun analyzeLink(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return "أدخل رابطًا أولًا."
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return "تعذر تحليل الرابط."
    val host = uri.host?.lowercase(Locale.ROOT) ?: return "الرابط لا يحتوي على اسم نطاق واضح. النتيجة: مشبوه أو غير صالح."
    val issues = mutableListOf<String>()
    if (uri.scheme !in listOf("http", "https")) issues += "بروتوكول غير معتاد"
    if (uri.scheme != "https") issues += "لا يستخدم HTTPS"
    if (host.startsWith("xn--") || ".xn--" in host) issues += "اسم نطاق دولي قد يخفي تشابهًا بصريًا"
    if (host.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))) issues += "يستخدم عنوان IP بدل اسم نطاق"
    if (host.count { it == '.' } >= 4) issues += "اسم نطاق طويل أو متعدد المستويات"
    if (listOf("login", "verify", "secure", "update", "account", "wallet").any { it in host } && host.count { it == '-' } >= 2) issues += "تركيبة اسم قد تستحق الحذر"
    if (issues.isEmpty()) return "آمن مبدئيًا: لم تظهر مؤشرات محلية واضحة. هذا لا يضمن أن الموقع آمن.\nالنطاق: $host"
    return "مشبوه أو يحتاج مراجعة:\n• ${issues.joinToString("\n• ")}\n\nلا تفتح الرابط إذا لم تكن تتوقعه. هذا فحص محلي وليس فحصًا شاملًا.\nالنطاق: $host"
}

@Composable
fun CleanerPage(files: List<DocumentFile>, message: String, onPick: () -> Unit, onDelete: (DocumentFile) -> Unit) {
    var confirmFile by remember { mutableStateOf<DocumentFile?>(null) }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(22.dp)); Text("Smart Cleaner", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("اختر مجلدًا بنفسك. لا يتم حذف ملفات تلقائيًا.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Navy)) { Text("اختيار مجلد للفحص") }
        Text(message, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files, key = { it.uri.toString() }) { file ->
                Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(file.name ?: "ملف بدون اسم", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("${file.length() / (1024 * 1024)} MB", color = Muted, fontSize = 12.sp)
                        }
                        TextButton(onClick = { confirmFile = file }) { Text("حذف", color = Color(0xFFFF8080)) }
                    }
                }
            }
        }
    }
    confirmFile?.let { file ->
        AlertDialog(onDismissRequest = { confirmFile = null }, title = { Text("تأكيد الحذف") }, text = { Text("هل تريد حذف الملف ${file.name ?: "المحدد"}؟ لا يمكن التراجع عن الحذف.") }, confirmButton = { TextButton(onClick = { onDelete(file); confirmFile = null }) { Text("حذف") } }, dismissButton = { TextButton(onClick = { confirmFile = null }) { Text("إلغاء") } })
    }
}

fun permissionLabel(permission: String): String = when (permission) {
    "android.permission.CAMERA" -> "الكاميرا"
    "android.permission.RECORD_AUDIO" -> "الميكروفون"
    "android.permission.ACCESS_FINE_LOCATION" -> "الموقع الدقيق"
    "android.permission.ACCESS_COARSE_LOCATION" -> "الموقع التقريبي"
    "android.permission.READ_CONTACTS" -> "جهات الاتصال"
    "android.permission.READ_CALL_LOG" -> "سجل المكالمات"
    "android.permission.READ_SMS" -> "الرسائل النصية"
    "android.permission.READ_EXTERNAL_STORAGE" -> "قراءة التخزين"
    "android.permission.MANAGE_EXTERNAL_STORAGE" -> "إدارة جميع الملفات"
    else -> permission.substringAfterLast('.')
}
