package com.lordofdarkness.ruijiecleaner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF09090B)
private val Panel = Color(0xFF18181B)
private val Gold = Color(0xFFD6A84F)
private val Red = Color(0xFF7F1D1D)
private val Muted = Color(0xFFA1A1AA)
private const val CHANNEL = "https://t.me/+_BWDsHfxJFVmYTY9"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Bg, surface = Panel)) { AppRoot() } }
    }
}

private enum class Page(val label: String) { CHANNEL("Channel"), CLEANER("Cleaner"), HISTORY("History") }
private data class HistoryItem(val url: String, val time: Long)

@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("lod_url_cleaner", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(Page.CLEANER) }
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<CleanOutcome?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var validation by remember { mutableStateOf<String?>(null) }
    val parameterOptions = listOf("timestamp", "sign", "token", "auth", "sid", "session", "nonce", "t")
    var selectedParams by remember { mutableStateOf(setOf("timestamp", "sign")) }
    var history by remember { mutableStateOf(loadHistory(prefs)) }

    fun runClean() {
        when (val r = UrlCleaner.clean(input, selectedParams)) {
            is CleanResult.Success -> {
                result = r.outcome; error = null
                history = (listOf(HistoryItem(r.outcome.cleaned, System.currentTimeMillis())) + history.filterNot { it.url == r.outcome.cleaned }).take(100)
                saveHistory(prefs, history)
                validation = UrlCleaner.validate(input)
            }
            is CleanResult.Failure -> { result = null; error = r.message; validation = UrlCleaner.validate(input) }
        }
    }
    fun copy(text: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("URL", text))
        Toast.makeText(ctx, "Copied", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF111113), tonalElevation = 0.dp) {
                Page.values().forEach { p ->
                    NavigationBarItem(
                        selected = page == p,
                        onClick = { page = p },
                        icon = { Icon(when (p) { Page.CHANNEL -> Icons.Default.Campaign; Page.CLEANER -> Icons.Default.Link; Page.HISTORY -> Icons.Default.History }, null) },
                        label = { Text(p.label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Gold, selectedTextColor = Gold, indicatorColor = Red, unselectedIconColor = Muted, unselectedTextColor = Muted)
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Bg)) {
            Header()
            when (page) {
                Page.CLEANER -> CleanerPage(
                    input, { input = it; error = null }, result, error, validation,
                    parameterOptions, selectedParams, { selectedParams = it }, { runClean() },
                    { copy(it) }, { text -> runCatching { ctx.startActivity(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }) } }
                )
                Page.CHANNEL -> ChannelPage { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CHANNEL))) } }
                Page.HISTORY -> HistoryPage(history, { copy(it) }, { input = it; page = Page.CLEANER }, { item ->
                    history = history.filterNot { it.time == item.time && it.url == item.url }; saveHistory(prefs, history)
                }, {
                    history = emptyList(); saveHistory(prefs, history)
                })
            }
        }
    }
}

@Composable
private fun Header() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(id = com.lordofdarkness.ruijiecleaner.R.drawable.lord_of_darkness_logo),
            contentDescription = "LORD OF DARKNESS logo",
            modifier = Modifier.size(46.dp).clip(CircleShape).border(1.dp, Gold, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("LORD OF DARKNESS", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = 1.sp)
            Text("RUIJIE URL CLEANER", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
        }
        Icon(Icons.Default.Shield, contentDescription = null, tint = Gold, modifier = Modifier.size(25.dp))
    }
    HorizontalDivider(color = Gold.copy(alpha = .25f))
}

@Composable
private fun CleanerPage(
    input: String, onInput: (String) -> Unit, result: CleanOutcome?, error: String?, validation: String?,
    parameterOptions: List<String>, selectedParams: Set<String>, onSelectedParams: (Set<String>) -> Unit,
    onClean: () -> Unit, onCopy: (String) -> Unit, onShare: (String) -> Unit
) {
    var toolReport by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("URL WORKSPACE", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Paste portal URL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                OutlinedTextField(
                    value = input, onValueChange = onInput, modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
                    placeholder = { Text("https://portal.example/...?timestamp=...&sign=...", color = Muted, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Color(0xFF3F3F46), focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Gold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { onInput(""); }, modifier = Modifier.weight(1f), border = androidx.compose.foundation.BorderStroke(1.dp, Muted)) { Icon(Icons.Default.DeleteSweep, null); Spacer(Modifier.width(4.dp)); Text("Clear") }
                    Button(onClick = onClean, modifier = Modifier.weight(1.5f), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)) { Icon(Icons.Default.AutoFixHigh, null); Spacer(Modifier.width(6.dp)); Text("CLEAN URL", fontWeight = FontWeight.Bold) }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF45434B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🗑️", fontSize = 19.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("ဖျက်လိုသော Parameters", color = Color(0xFFE7E2EC), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("ဖယ်ရှားလိုသော Parameter များကို ရွေးချယ်ပါ", color = Color(0xFFC9C4D0), fontSize = 12.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            parameterOptions.chunked(2).forEach { rowParams ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowParams.forEach { param ->
                                        Row(
                                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onSelectedParams(
                                                        if (param in selectedParams) selectedParams - param
                                                        else selectedParams + param
                                                    )
                                                }.padding(horizontal = 2.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = param in selectedParams,
                                                onCheckedChange = { checked ->
                                                    onSelectedParams(if (checked) selectedParams + param else selectedParams - param)
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = Color(0xFF38BDF8),
                                                    uncheckedColor = Color(0xFFD6D0DC),
                                                    checkmarkColor = Color(0xFF0F172A)
                                                )
                                            )
                                            Text(param, color = Color(0xFFE7E2EC), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    if (rowParams.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onSelectedParams(parameterOptions.toSet()) }) {
                                Text("Select all", color = Color(0xFF7DD3FC), fontSize = 12.sp)
                            }
                            TextButton(onClick = { onSelectedParams(emptySet()) }) {
                                Text("Clear selection", color = Color(0xFFD6D0DC), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        if (error != null) InfoCard("CLEAN FAILED", error, Color(0xFFF87171))
        if (validation != null) InfoCard("URL CHECK", validation, if (validation.startsWith("URL ပုံစံမှန်")) Color(0xFF86EFAC) else Color(0xFFFCA5A5))
        if (result != null) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF86EFAC))
                        Spacer(Modifier.width(8.dp))
                        Text("CLEAN RESULT", color = Color(0xFF86EFAC), fontWeight = FontWeight.Bold)
                    }
                    Text(result.cleaned, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Removed: ${result.removed.joinToString().ifBlank { "None" }}", color = Muted, fontSize = 11.sp)
                    Text("Remaining query parameters: ${result.keptParameterCount}", color = Muted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onCopy(result.cleaned) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534))) { Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(4.dp)); Text("Copy") }
                        OutlinedButton(onClick = { onShare(result.cleaned) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(4.dp)); Text("Share") }
                    }
                }
            }
        }
        Text("QUICK URL TOOLS", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickToolButton("URL CHECK", Icons.Default.Verified, Modifier.weight(1f)) {
                        toolReport = UrlCleaner.validate(input)
                    }
                    QuickToolButton("PARAMETERS", Icons.Default.Tune, Modifier.weight(1f)) {
                        toolReport = try {
                            val raw = input.trim().let { if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it" }
                            val uri = java.net.URI(raw)
                            val params = uri.rawQuery.orEmpty().split("&").filter { it.isNotBlank() }
                            if (params.isEmpty()) "Query parameter မတွေ့ပါ။" else "တွေ့ရှိသော parameter ${params.size} ခု:\n" + params.joinToString("\n") { "• " + it.substringBefore("=") }
                        } catch (_: Exception) { "URL format မှားနေသဖြင့် parameter မဖတ်နိုင်ပါ။" }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickToolButton("ENCODE", Icons.Default.Code, Modifier.weight(1f)) {
                        toolReport = try { onInput(java.net.URLEncoder.encode(input, "UTF-8")); "URL text ကို encode လုပ်ပြီး Input ထဲသို့ ထည့်ပြီးပါပြီ။" } catch (_: Exception) { "Encode မလုပ်နိုင်ပါ။" }
                    }
                    QuickToolButton("DECODE", Icons.Default.DataObject, Modifier.weight(1f)) {
                        toolReport = try { onInput(java.net.URLDecoder.decode(input, "UTF-8")); "URL text ကို decode လုပ်ပြီး Input ထဲသို့ ထည့်ပြီးပါပြီ။" } catch (_: Exception) { "Decode မလုပ်နိုင်ပါ။" }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickToolButton("DUPLICATE CHECK", Icons.Default.ContentCopy, Modifier.weight(1f)) {
                        toolReport = try {
                            val uri = java.net.URI(if (input.startsWith("http", true)) input else "https://$input")
                            val keys = uri.rawQuery.orEmpty().split("&").filter { it.isNotBlank() }.map { it.substringBefore("=").lowercase() }
                            val duplicates = keys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
                            if (duplicates.isEmpty()) "Duplicate parameter မတွေ့ပါ။" else "Duplicate parameter: " + duplicates.joinToString()
                        } catch (_: Exception) { "URL format မှားနေပါတယ်။" }
                    }
                    QuickToolButton("COPY INPUT", Icons.Default.ContentPaste, Modifier.weight(1f)) {
                        onCopy(input); toolReport = "Input URL ကို clipboard သို့ ကူးယူပြီးပါပြီ။"
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickToolButton("CLEAR ALL", Icons.Default.DeleteSweep, Modifier.weight(1f)) {
                        onInput(""); toolReport = null
                    }
                    QuickToolButton("SHARE INPUT", Icons.Default.Share, Modifier.weight(1f)) {
                        if (input.isNotBlank()) onShare(input) else toolReport = "မျှဝေရန် URL ထည့်ပေးပါ။"
                    }
                }
            }
        }
        if (toolReport != null) InfoCard("TOOL RESULT", toolReport!!, Gold)
        Text("Local URL format check only — it cannot confirm portal/session validity. Network tools may need internet access.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun QuickToolButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .45f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 9.sp, maxLines = 2, lineHeight = 11.sp)
    }
}

@Composable
private fun InfoCard(title: String, body: String, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Text(body, color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ChannelPage(onJoin: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(112.dp).clip(CircleShape).background(Panel).border(2.dp, Gold, CircleShape), contentAlignment = Alignment.Center) {
            Text("LOD", color = Gold, fontWeight = FontWeight.Black, fontSize = 26.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("LORD OF DARKNESS", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
        Text("CYBER TOOLS • TUTORIALS • UPDATES", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.height(22.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Campaign, null, tint = Gold, modifier = Modifier.size(38.dp))
                Spacer(Modifier.height(10.dp))
                Text("Join our Telegram Channel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text("Get channel updates, tools and learning resources.", color = Muted)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onJoin, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text("JOIN CHANNEL", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HistoryPage(
    history: List<HistoryItem>, onCopy: (String) -> Unit, onUse: (String) -> Unit,
    onDelete: (HistoryItem) -> Unit, onClearAll: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CLEAN HISTORY", color = Gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("${history.size} saved URL(s) • stored on this device", color = Muted, fontSize = 11.sp)
            }
            TextButton(onClick = onClearAll, enabled = history.isNotEmpty()) { Text("Clear all", color = Color(0xFFF87171)) }
        }
        Spacer(Modifier.height(10.dp))
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, tint = Muted, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp)); Text("No history yet", color = Muted)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.url, color = Color.White, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(item.time)), color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onUse(item.url) }) { Icon(Icons.Default.Edit, "Use", tint = Gold) }
                                IconButton(onClick = { onCopy(item.url) }) { Icon(Icons.Default.ContentCopy, "Copy", tint = Gold) }
                                IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFF87171)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadHistory(prefs: android.content.SharedPreferences): List<HistoryItem> = try {
    val arr = JSONArray(prefs.getString("history", "[]"))
    (0 until arr.length()).mapNotNull { i ->
        val o = arr.optJSONObject(i) ?: return@mapNotNull null
        HistoryItem(o.optString("url"), o.optLong("time"))
    }
} catch (_: Exception) { emptyList() }

private fun saveHistory(prefs: android.content.SharedPreferences, items: List<HistoryItem>) {
    val arr = JSONArray()
    items.forEach { arr.put(JSONObject().put("url", it.url).put("time", it.time)) }
    prefs.edit().putString("history", arr.toString()).apply()
}
