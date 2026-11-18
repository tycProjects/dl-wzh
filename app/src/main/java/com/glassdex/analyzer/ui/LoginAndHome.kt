package com.glassdex.analyzer.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassdex.analyzer.BuildConfig
import com.glassdex.analyzer.analysis.Risk
import java.text.DateFormat
import java.util.Date

@Composable
fun LoginScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val busy by vm.authBusy.collectAsState()
    val err by vm.error.collectAsState()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("GlassDex", color = C.text, fontSize = 44.sp, fontWeight = FontWeight.Bold)
        Text("Decompile, inspect and scan APK, XAPK and EXE files.", color = C.sub, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Sign in to save your history", color = C.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(12.dp))
            GlassButton("Continue with Google", Modifier.fillMaxWidth(), accent = C.blue, enabled = !busy) {
                (ctx as? Activity)?.let { vm.google(it) }
            }
            Spacer(Modifier.height(14.dp))
            GlassField(email, { email = it }, "Email", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GlassField(pass, { pass = it }, "Password", Modifier.fillMaxWidth(), password = true)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Sign in", Modifier.weight(1f), enabled = !busy) { vm.email(email, pass, false) }
                GlassButton("Create account", Modifier.weight(1f), accent = C.violet, enabled = !busy) { vm.email(email, pass, true) }
            }
            Spacer(Modifier.height(6.dp))
            Text("Forgot password?", color = C.cyan, fontSize = 13.sp,
                modifier = Modifier.padding(6.dp).clickableNoRipple { vm.resetPassword(email) })
            if (busy) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth(), color = C.cyan, trackColor = Color.White.copy(alpha = 0.15f)) }
        }

        Spacer(Modifier.height(12.dp))
        GlassButton("Continue as guest", Modifier.fillMaxWidth(), accent = C.pink) { vm.guest() }

        err?.let {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth(), tint = C.red) { Text(it, color = C.text, fontSize = 13.sp) }
        }
        if (!BuildConfig.FIREBASE_ENABLED) {
            Spacer(Modifier.height(12.dp))
            Text("Google and email login are switched off in this build because app/google-services.json is missing. Guest mode works fully. See README to enable login.",
                color = C.sub, fontSize = 12.sp)
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel) {
    val session by vm.session.collectAsState()
    val history by vm.history.collectAsState()
    val err by vm.error.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.analyze(it) } }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GlassDex", color = C.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(session?.let { if (it.isGuest) "Guest mode" else it.email ?: it.name } ?: "", color = C.sub, fontSize = 13.sp)
            }
            GlassButton("History", accent = C.violet) { vm.go(Screen.HISTORY) }
            Spacer(Modifier.width(8.dp))
            GlassButton("⚙", accent = C.blue) { vm.go(Screen.SETTINGS) }
        }

        GlassCard(Modifier.fillMaxWidth().padding(top = 8.dp), radius = 32.dp) {
            Text("Pick a file to analyze", color = C.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(".apk  ·  .xapk  ·  .exe / .dll", color = C.sub, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text("Unpacks every file, decodes the manifest, lists classes and methods, checks the signature and scores malware risk. Most apps finish in a few seconds.",
                color = C.sub, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            GlassButton("Choose file", Modifier.fillMaxWidth()) { picker.launch(arrayOf("*/*")) }
        }

        err?.let {
            Spacer(Modifier.height(10.dp))
            GlassCard(Modifier.fillMaxWidth(), tint = C.red, onClick = { vm.error.value = null }) { Text(it, color = C.text, fontSize = 13.sp) }
        }

        Spacer(Modifier.height(14.dp))
        Text("Recent", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        if (history.isEmpty()) {
            Text("Nothing yet. Your scans will appear here${if (session?.isGuest == true) " (guest history stays on this phone only)" else ""}.", color = C.sub, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(history.take(8)) { h -> HistoryRow(h) { vm.openFiles(h.outputDir) } }
        }
    }
}

@Composable
fun HistoryRow(h: com.glassdex.analyzer.data.HistoryItem, onDelete: (() -> Unit)? = null, onClick: () -> Unit) {
    val col = when (h.risk) {
        Risk.CLEAN.label -> C.green; Risk.LOW.label -> C.yellow; Risk.MEDIUM.label -> C.orange; else -> C.red
    }
    GlassCard(Modifier.fillMaxWidth(), radius = 24.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(h.fileName, color = C.text, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(listOfNotNull(h.kind, h.packageName).joinToString(" · "), color = C.sub, fontSize = 12.sp, maxLines = 1)
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(h.time)), color = C.sub, fontSize = 11.sp)
            }
            Pill("${h.risk} · ${h.score}", col)
            if (onDelete != null) {
                Spacer(Modifier.width(8.dp))
                Text("✕", color = C.red, fontSize = 18.sp, modifier = Modifier.padding(6.dp).clickableNoRipple(onDelete))
            }
        }
    }
}

@Composable
fun AnalyzingScreen(vm: AppViewModel) {
    val p by vm.progress.collectAsState()
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(Modifier.fillMaxWidth(), radius = 32.dp) {
            Text("Analyzing…", color = C.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(p.stage, color = C.sub, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(progress = { p.fraction }, Modifier.fillMaxWidth().height(10.dp), color = C.cyan, trackColor = Color.White.copy(alpha = 0.15f))
            Spacer(Modifier.height(6.dp))
            Text("${(p.fraction * 100).toInt()}%", color = C.text, fontSize = 13.sp)
        }
    }
}
