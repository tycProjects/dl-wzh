package com.glassdex.analyzer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.glassdex.analyzer.analysis.*
import kotlinx.coroutines.launch
import java.io.File

private val TABS = listOf("Overview", "App info", "Permissions", "Findings", "Network", "Hashes")

@Composable
fun ReportScreen(vm: AppViewModel) {
    val r by vm.report.collectAsState()
    val report = r ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassButton("‹ Back", accent = C.blue) { vm.go(Screen.HOME) }
                Spacer(Modifier.width(10.dp))
                Text("Report", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TABS.size) { i ->
                GlassButton(TABS[i], accent = if (i == tab) C.cyan else C.violet) { tab = i }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (tab) {
                0 -> Overview(report, vm, ctx, scope)
                1 -> InfoTab(report)
                2 -> PermTab(report)
                3 -> FindingsTab(report)
                4 -> NetworkTab(report)
                5 -> HashTab(report, ctx)
            }
        }
    }
}

@Composable
private fun Overview(r: AnalysisReport, vm: AppViewModel, ctx: Context, scope: kotlinx.coroutines.CoroutineScope) {
    val a = r.appInfo
    GlassCard(Modifier.fillMaxWidth(), radius = 32.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bmp = remember(a?.iconPath) { a?.iconPath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() } }
            if (bmp != null) {
                Image(bmp.asImageBitmap(), null, Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(a?.label ?: r.fileName, color = C.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(a?.packageName ?: r.fileName, color = C.sub, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(r.kind.name, C.blue)
                    Pill(fmtSize(r.sizeBytes), C.violet)
                    Pill("${r.tookMs} ms", C.cyan)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { RiskGauge(r.riskScore, r.risk) }
        Text("Heuristic score. It estimates risk and is not a guarantee that a file is safe or harmful.", color = C.sub, fontSize = 11.sp)
    }

    VtCard(r.vt, ctx)

    GlassCard(Modifier.fillMaxWidth()) {
        Text("Decompiled output", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("${r.fileCount} files extracted. Includes decoded AndroidManifest.xml, readable resource XML, class/method skeletons and a strings dump.", color = C.sub, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("Browse files", Modifier.weight(1f)) { vm.openFiles(r.outputDir) }
            GlassButton("Share ZIP", Modifier.weight(1f), accent = C.pink) {
                scope.launch {
                    val dir = r.outputDir?.let { File(it) } ?: return@launch
                    val zip = vm.zipOutput(dir)
                    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", zip)
                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "application/zip"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, "Share decompiled files"))
                }
            }
        }
    }

    a?.let {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Code stats", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            KeyValue("DEX files", it.dexCount.toString()); KeyValue("Classes", it.classCount.toString())
            KeyValue("Methods", it.methodCount.toString()); KeyValue("Strings", it.stringCount.toString())
            KeyValue("Native libs", it.nativeLibs.size.toString()); KeyValue("ABIs", it.abis.joinToString())
        }
    }
    r.peInfo?.let { PeCards(it) }
    if (r.subApks.isNotEmpty()) GlassCard(Modifier.fillMaxWidth()) {
        Text("Packages in this XAPK (${r.subApks.size})", color = C.text, fontWeight = FontWeight.Bold)
        r.subApks.forEach { Text(it, color = C.sub, fontSize = 12.sp) }
    }
}

@Composable
private fun VtCard(vt: VtResult, ctx: Context) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("VirusTotal", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        when (vt.state) {
            "found" -> {
                val bad = vt.malicious + vt.suspicious
                val total = vt.malicious + vt.suspicious + vt.harmless + vt.undetected
                Text("$bad of $total engines flag this file", color = if (bad == 0) C.green else if (vt.malicious >= 3) C.red else C.orange, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Malicious ${vt.malicious} · Suspicious ${vt.suspicious} · Harmless ${vt.harmless} · Undetected ${vt.undetected}", color = C.sub, fontSize = 12.sp)
            }
            else -> Text(vt.message ?: "", color = C.sub, fontSize = 13.sp)
        }
        vt.permalink?.let {
            Spacer(Modifier.height(8.dp))
            GlassButton("Open on VirusTotal", accent = C.blue) { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(it))) }
        }
        Text("Only the file hash is sent. The file is never uploaded.", color = C.sub, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun InfoTab(r: AnalysisReport) {
    val a = r.appInfo
    if (a == null) { r.peInfo?.let { PeCards(it) }; return }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("App details", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        KeyValue("Name", a.label); KeyValue("Package", a.packageName, true)
        KeyValue("Version", listOfNotNull(a.versionName, a.versionCode?.let { "($it)" }).joinToString(" "))
        KeyValue("Min SDK", a.minSdk?.let { "$it  (Android ${androidName(it.toIntOrNull())})" })
        KeyValue("Target SDK", a.targetSdk?.let { "$it  (Android ${androidName(it.toIntOrNull())})" })
        KeyValue("Debuggable", if (a.debuggable) "Yes" else "No")
        KeyValue("Backup allowed", if (a.allowBackup) "Yes" else "No")
        KeyValue("Cleartext HTTP", if (a.cleartextTraffic) "Allowed" else "Blocked / default")
        KeyValue("File", r.fileName); KeyValue("Size", fmtSize(r.sizeBytes))
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Signature (${a.certs.size})", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (a.certs.isEmpty()) Text("No v1 certificate found. APK may use v2/v3 signing only or be unsigned.", color = C.sub, fontSize = 13.sp)
        a.certs.forEach { c ->
            KeyValue("Subject", c.subject); KeyValue("Issuer", c.issuer)
            KeyValue("Valid", "${c.validFrom} → ${c.validTo}${if (c.expired) "  (expired)" else ""}")
            KeyValue("Self-signed", if (c.selfSigned) "Yes" else "No"); KeyValue("SHA-256", c.sha256, true)
            Spacer(Modifier.height(6.dp))
        }
    }
    ComponentCard("Activities", a.activities); ComponentCard("Services", a.services)
    ComponentCard("Receivers", a.receivers); ComponentCard("Providers", a.providers)
    if (a.nativeLibs.isNotEmpty()) ComponentCard("Native libraries", a.nativeLibs)
}

@Composable
private fun ComponentCard(title: String, items: List<String>) {
    if (items.isEmpty()) return
    GlassCard(Modifier.fillMaxWidth()) {
        Text("$title (${items.size})", color = C.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        items.take(40).forEach { Text(it, color = C.sub, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        if (items.size > 40) Text("+ ${items.size - 40} more (see decoded/AndroidManifest.xml)", color = C.sub, fontSize = 11.sp)
    }
}

@Composable
private fun PermTab(r: AnalysisReport) {
    val p = r.appInfo?.permissions ?: emptyList()
    if (p.isEmpty()) { GlassCard(Modifier.fillMaxWidth()) { Text("No permissions declared.", color = C.sub) }; return }
    val (bad, ok) = p.partition { it.dangerous }
    GlassCard(Modifier.fillMaxWidth(), tint = C.orange) {
        Text("Sensitive (${bad.size})", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        bad.forEach { Text(it.name.substringAfterLast('.'), color = C.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text(it.note, color = C.sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp)) }
        if (bad.isEmpty()) Text("None", color = C.sub, fontSize = 13.sp)
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Other (${ok.size})", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        ok.forEach { Text(it.name, color = C.sub, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
    }
}

@Composable
private fun FindingsTab(r: AnalysisReport) {
    if (r.findings.isEmpty()) GlassCard(Modifier.fillMaxWidth(), tint = C.green) { Text("No suspicious patterns detected.", color = C.text) }
    r.findings.forEach { f ->
        val col = when { f.points >= 15 -> C.red; f.points >= 8 -> C.orange; f.points > 0 -> C.yellow; else -> C.blue }
        GlassCard(Modifier.fillMaxWidth(), tint = col) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.title, color = C.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Pill("+${f.points}", col)
            }
            Text(f.detail, color = C.sub, fontSize = 13.sp)
        }
    }
}

@Composable
private fun NetworkTab(r: AnalysisReport) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Endpoints found in code (${r.urls.size})", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (r.urls.isEmpty()) Text("None found.", color = C.sub, fontSize = 13.sp)
        r.urls.forEach { Text(it, color = C.sub, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 2.dp)) }
    }
}

@Composable
private fun HashTab(r: AnalysisReport, ctx: Context) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("File hashes", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        listOf("MD5" to r.hashes.md5, "SHA-1" to r.hashes.sha1, "SHA-256" to r.hashes.sha256).forEach { (k, v) ->
            Text(k, color = C.sub, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            Text(v, color = C.text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(4.dp))
            GlassButton("Copy $k", accent = C.violet) {
                (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(k, v))
            }
        }
    }
}

@Composable
private fun PeCards(p: PeInfo) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Windows executable", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        KeyValue("Architecture", "${p.machine} (${if (p.is64) "PE32+" else "PE32"})"); KeyValue("Subsystem", p.subsystem)
        KeyValue("Type", if (p.isDll) "DLL" else "EXE"); KeyValue(".NET", if (p.dotNet) "Yes (managed)" else "No (native)")
        KeyValue("Compiled", p.timestamp); KeyValue("Entry point", p.entryPoint)
        KeyValue("Signed", if (p.signed) "Has Authenticode signature (not verified)" else "No")
        KeyValue("Packer", p.packer ?: "None detected")
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Sections", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        p.sections.forEach {
            Text("${it.name}  ${fmtSize(it.rawSize)}  entropy ${"%.2f".format(it.entropy)}  ${if (it.executable) "X" else "-"}${if (it.writable) "W" else "-"}",
                color = if (it.entropy > 7.2) C.orange else C.sub, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Imports (${p.imports.size} DLLs)", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        if (p.suspiciousImports.isNotEmpty()) Text("Risky: " + p.suspiciousImports.joinToString(", "), color = C.orange, fontSize = 12.sp)
        p.imports.forEach { (d, f) -> Text("$d  (${f.size})", color = C.sub, fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
    }
}

fun fmtSize(b: Long): String = when {
    b >= 1L shl 30 -> "%.2f GB".format(b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> "%.1f MB".format(b / (1L shl 20).toDouble())
    b >= 1L shl 10 -> "%.0f KB".format(b / 1024.0)
    else -> "$b B"
}

private fun androidName(api: Int?): String = when (api) {
    21 -> "5.0"; 22 -> "5.1"; 23 -> "6"; 24 -> "7"; 25 -> "7.1"; 26 -> "8"; 27 -> "8.1"; 28 -> "9"; 29 -> "10"
    30 -> "11"; 31 -> "12"; 32 -> "12L"; 33 -> "13"; 34 -> "14"; 35 -> "15"; 36 -> "16"
    null -> "?"; else -> if (api < 21) "4.x or older" else "API $api"
}
