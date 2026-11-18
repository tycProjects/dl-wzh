package com.glassdex.analyzer.ui

import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

private val TEXT_EXT = setOf("txt", "xml", "json", "md", "properties", "html", "htm", "js", "css", "mf", "sf", "kt", "java", "smali", "version", "yml", "yaml", "csv", "cfg", "ini", "log")

@Composable
fun FilesScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val dir by vm.browseDir.collectAsState()
    val root by vm.browseRoot.collectAsState()
    val d = dir ?: return
    val entries = remember(d) { (d.listFiles()?.toList() ?: emptyList()).sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) }
    val rel = root?.let { d.relativeTo(it).path.ifEmpty { "/" } } ?: d.name

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassButton("‹ Back", accent = C.blue) { vm.back() }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Files", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(rel, color = C.sub, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entries.isEmpty()) item { Text("Empty folder", color = C.sub) }
            items(entries) { f ->
                GlassCard(Modifier.fillMaxWidth(), radius = 20.dp, onClick = {
                    if (f.isDirectory) vm.browseDir.value = f
                    else if (f.extension.lowercase() in TEXT_EXT || f.name.endsWith(".smali.txt")) vm.openViewer(f)
                    else openExternal(ctx, f)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (f.isDirectory) "📁" else "📄", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(f.name, color = C.text, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1)
                        Text(if (f.isDirectory) "${f.listFiles()?.size ?: 0} items" else fmtSize(f.length()), color = C.sub, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun openExternal(ctx: android.content.Context, f: File) {
    try {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "*/*"
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Open ${f.name}"))
    } catch (_: Throwable) { }
}

@Composable
fun ViewerScreen(vm: AppViewModel) {
    val f by vm.viewFile.collectAsState()
    val file = f ?: return
    val text = remember(file) {
        try {
            val max = 300_000
            val shown = file.inputStream().use { ins ->
                val buf = ByteArray(max); var off = 0
                while (off < max) { val n = ins.read(buf, off, max - off); if (n < 0) break; off += n }
                String(buf, 0, off, Charsets.UTF_8)
            }
            shown + if (file.length() > max) "\n\n… truncated (${fmtSize(file.length())} total). Use Share ZIP for the full file." else ""
        } catch (t: Throwable) { "Cannot read file: ${t.message}" }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassButton("‹ Back", accent = C.blue) { vm.back() }
            Spacer(Modifier.width(10.dp))
            Text(file.name, color = C.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxSize(), radius = 24.dp) {
            Text(text, color = C.text, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }
}

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val history by vm.history.collectAsState()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassButton("‹ Back", accent = C.blue) { vm.go(Screen.HOME) }
            Spacer(Modifier.width(10.dp))
            Text("History", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        if (history.isEmpty()) Text("No scans saved yet.", color = C.sub)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(history) { h -> HistoryRow(h, onDelete = { vm.deleteHistory(h) }) { vm.openFiles(h.outputDir) } }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel) {
    val s by vm.settings.collectAsState()
    val session by vm.session.collectAsState()
    var key by remember(s.vtKey) { mutableStateOf(s.vtKey) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassButton("‹ Back", accent = C.blue) { vm.go(Screen.HOME) }
            Spacer(Modifier.width(10.dp))
            Text("Settings", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Account", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(session?.let { if (it.isGuest) "Guest (history stays on this phone)" else "${it.name}  ·  ${it.email ?: ""}" } ?: "", color = C.sub, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            GlassButton(if (session?.isGuest == true) "Sign in / create account" else "Sign out", accent = C.pink) { vm.signOut() }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Liquid glass", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Transparency of every box: ${(s.glassOpacity * 100).toInt()}%", color = C.sub, fontSize = 13.sp)
            Slider(value = s.glassOpacity, onValueChange = { vm.updateSettings(s.copy(glassOpacity = it)) }, valueRange = 0.04f..0.45f,
                colors = SliderDefaults.colors(thumbColor = C.cyan, activeTrackColor = C.cyan, inactiveTrackColor = Color.White.copy(alpha = 0.2f)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Animated background", color = C.text, modifier = Modifier.weight(1f))
                Switch(s.animatedBackground, { vm.updateSettings(s.copy(animatedBackground = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = C.cyan.copy(alpha = 0.6f)))
            }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("VirusTotal API key", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Free key from virustotal.com (Profile > API key). Enables a 70+ engine reputation check using the file hash only.", color = C.sub, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            GlassField(key, { key = it }, "API key", Modifier.fillMaxWidth(), password = true)
            Spacer(Modifier.height(8.dp))
            GlassButton("Save key") { vm.updateSettings(s.copy(vtKey = key.trim())) }
        }

        GlassCard(Modifier.fillMaxWidth()) {
            Text("About", color = C.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("GlassDex unpacks and inspects files locally on your phone. Class output is a skeleton (names, fields, method signatures). It does not rebuild Java source from bytecode. The malware score is a heuristic, not an antivirus.", color = C.sub, fontSize = 12.sp)
        }
    }
}
