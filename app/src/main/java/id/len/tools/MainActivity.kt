package id.len.tools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Bg = Color(0xFF090B12)
private val Surface = Color(0xFF151927)
private val Purple = Color(0xFF8B6CFF)
private val Muted = Color(0xFF9299AD)
private val White = Color(0xFFF5F6FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LenToolsApp() }
    }
}

@Composable
fun LenToolsApp() {
    var page by remember { mutableStateOf("Home") }
    var selectedAi by remember { mutableStateOf("GPT LEN AI") }
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg, surface = Surface, primary = Purple,
            onBackground = White, onSurface = White
        )
    ) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF111522), contentColor = White) {
                    listOf(
                        Triple("Home", Icons.Default.Home, "Home"),
                        Triple("AI Tools", Icons.Default.AutoAwesome, "AI Tools"),
                        Triple("Other", Icons.Default.GridView, "Other"),
                        Triple("Settings", Icons.Default.Settings, "Settings")
                    ).forEach { (label, icon, route) ->
                        NavigationBarItem(
                            selected = page == route,
                            onClick = { page = route },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Purple,
                                selectedTextColor = Purple,
                                indicatorColor = Purple.copy(alpha = .16f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (page) {
                "Home" -> HomeScreen(padding, onAi = { name -> selectedAi = name; page = "Chat" }, onNetflix = { page = "Other" })
                "AI Tools" -> AiToolsScreen(padding) { name -> selectedAi = name; page = "Chat" }
                "Other" -> OtherScreen(padding)
                "Settings" -> SettingsScreen(padding)
                "Chat" -> ChatScreen(selectedAi, padding) { page = "AI Tools" }
            }
        }
    }
}

@Composable
fun HomeScreen(padding: PaddingValues, onAi: (String) -> Unit, onNetflix: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).background(
                        Brush.linearGradient(listOf(Purple, Color(0xFF3B8DFF))),
                        RoundedCornerShape(15.dp)
                    ), contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(27.dp)) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("LEN TOOLS", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = White)
                    Text("Your AI toolkit", color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Surface(color = Color(0xFF17352D), shape = RoundedCornerShape(30.dp)) {
                    Text("● READY", color = Color(0xFF67D9A5), fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
                }
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF272044), Color(0xFF17263D))),
                    RoundedCornerShape(24.dp)
                ).padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("WELCOME BACK", color = Color(0xFFB9A8FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Everything you need.\nIn one place.", color = White, fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Explore your tools and get things done.", color = Color(0xFFC2C5D3), fontSize = 13.sp)
                }
            }
        }
        item { SectionTitle("AI TOOLS", "Powered by your API") }
        item { ToolCard("GPT LEN AI", "Ask anything, get useful answers.", Icons.Default.Chat, Color(0xFF9B83FF)) { onAi("GPT LEN AI") } }
        item { ToolCard("DEEPSEK AI", "Explore ideas and solve problems.", Icons.Default.Psychology, Color(0xFF60C8F5)) { onAi("DEEPSEK AI") } }
        item { SectionTitle("MORE TOOLS", "Service integrations") }
        item { ToolCard("Netflix Token", "Region, API key, dan generate token.", Icons.Default.Shield, Color(0xFFE56B83), onNetflix) }
        item { InfoCard("Alight Motion", "Verifikasi melalui alur resmi penyedia layanan.", Icons.Default.Verified, "Official verification") }
        item {
            Text("LEN TOOLS  •  VERSION 1.1", color = Muted, fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun AiToolsScreen(padding: PaddingValues, onAi: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScreenHeader("AI Tools", "Choose your AI assistant")
        ToolCard("GPT LEN AI", "Chat with GPT endpoint.", Icons.Default.Chat, Purple) { onAi("GPT LEN AI") }
        ToolCard("DEEPSEK AI", "Chat with Deepsek endpoint.", Icons.Default.Psychology, Color(0xFF60C8F5)) { onAi("DEEPSEK AI") }
    }
}

@Composable
fun ChatScreen(ai: String, padding: PaddingValues, onBack: () -> Unit) {
    var prompt by remember(ai) { mutableStateOf("") }
    var answer by remember(ai) { mutableStateOf("Hi! I'm $ai. What would you like to ask?") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = White) }
            Column {
                Text(ai, color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("AI Assistant", color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(Modifier.weight(1f).fillMaxWidth(), color = Surface, shape = RoundedCornerShape(20.dp)) {
            LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { MessageBubble(answer, false) }
                if (error != null) item { Text(error!!, color = Color(0xFFFF8585), fontSize = 12.sp) }
                if (loading) item { Text("LEN AI sedang berpikir…", color = Muted, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(18.dp)).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = prompt, onValueChange = { prompt = it },
                modifier = Modifier.weight(1f).padding(10.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 14.sp),
                decorationBox = { inner ->
                    if (prompt.isEmpty()) Text("Tulis pesan kamu…", color = Muted, fontSize = 14.sp)
                    inner()
                }
            )
            IconButton(
                enabled = prompt.isNotBlank() && !loading,
                onClick = {
                    val question = prompt.trim()
                    prompt = ""; error = null; loading = true
                    scope.launch {
                        try {
                            answer = LenApi.ask(ai, question)
                        } catch (e: Exception) {
                            error = "Gagal menghubungi API. Periksa koneksi atau endpoint. (${e.message ?: "unknown error"})"
                        } finally { loading = false }
                    }
                }
            ) {
                Icon(Icons.Default.Send, null, tint = if (prompt.isNotBlank() && !loading) Purple else Muted)
            }
        }
        Text("Jangan kirim kata sandi atau informasi pribadi.", color = Muted, fontSize = 10.sp,
            modifier = Modifier.padding(start = 5.dp, top = 7.dp))
    }
}

@Composable
fun MessageBubble(text: String, user: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Surface(color = if (user) Color(0xFF302653) else Color(0xFF22283A),
            shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 310.dp)) {
            Text(text, color = White, fontSize = 14.sp, modifier = Modifier.padding(14.dp))
        }
    }
}

@Composable
fun OtherScreen(padding: PaddingValues) {
    var apiKey by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("Indonesia (ID)") }
    var expanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val regions = listOf("Indonesia (ID)", "India (IN)", "United States (US)", "Singapore (SG)")
    val regionCode = region.substringAfterLast("(").substringBefore(")")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NETFLIX TOKEN", color = White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Nexa API • Token generator", color = Muted, fontSize = 11.sp)
                }
                Surface(color = Color(0xFF18352D), shape = RoundedCornerShape(30.dp)) {
                    Text("● API READY", color = Color(0xFF67D9A5), fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp))
                }
            }
        }
        item {
            Box(Modifier.fillMaxWidth().height(142.dp).background(
                Brush.linearGradient(listOf(Color(0xFF26142C), Color(0xFF101827))), RoundedCornerShape(20.dp)
            ).padding(18.dp), contentAlignment = Alignment.BottomStart) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("NETFLIX TOKEN GENERATOR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Create token through your authorized Nexa API access", color = Color(0xFFC0C3D0), fontSize = 11.sp)
                }
            }
        }
        item {
            Surface(color = Surface, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("BUAT SESI TOKEN", color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Masukkan API key NexaDev, lalu pilih region tujuan.", color = Muted, fontSize = 11.sp)
                    Text("TARGET COUNTRY / REGION", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = White)) {
                            Text(region, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            regions.forEach { item ->
                                DropdownMenuItem(text = { Text(item) }, onClick = { region = item; expanded = false })
                            }
                        }
                    }
                    Text("API KEY NEXADEV", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = apiKey, onValueChange = { apiKey = it; error = "" },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Masukkan API key NexaDev", color = Muted) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = White, unfocusedTextColor = White,
                            focusedBorderColor = Purple, unfocusedBorderColor = Color(0xFF343B4E),
                            cursorColor = Purple
                        )
                    )
                    Button(
                        onClick = {
                            loading = true; result = ""; error = ""
                            scope.launch {
                                try { result = LenApi.createNetflixToken(apiKey.trim(), regionCode) }
                                catch (e: Exception) { error = e.message ?: "Request gagal. Periksa API key dan koneksi." }
                                finally { loading = false }
                            }
                        },
                        enabled = apiKey.isNotBlank() && !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple, disabledContainerColor = Color(0xFF39334D))
                    ) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.size(19.dp), color = Color.White, strokeWidth = 2.dp)
                        else Icon(Icons.Default.Bolt, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (loading) "MEMBUAT TOKEN..." else "GENERATE TOKEN NETFLIX", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (error.isNotBlank()) Text(error, color = Color(0xFFFF8585), fontSize = 12.sp)
                    if (result.isNotBlank()) {
                        HorizontalDivider(color = Color(0xFF343B4E))
                        Text("HASIL API", color = Color(0xFFB9A8FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        SelectionContainer { Text(result, color = White, fontSize = 12.sp) }
                    }
                }
            }
        }
        item {
            Text("Key hanya digunakan untuk request ini dan tidak disimpan permanen di perangkat. Gunakan hanya untuk layanan dan akun yang kamu berwenang akses.",
                color = Muted, fontSize = 10.sp)
        }
        item { InfoCard("Alight Motion", "Verifikasi melalui proses resmi penyedia layanan.", Icons.Default.Verified, "Official flow only") }
    }
}

@Composable
fun SettingsScreen(padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ScreenHeader("Settings", "Application information")
        InfoCard("LEN TOOLS", "Version 1.0.0 • Android Native Kotlin", Icons.Default.Info, "About")
        InfoCard("Privacy & Security", "Hindari memasukkan password, OTP, atau data sensitif ke layanan AI.", Icons.Default.Lock, "Safety")
        InfoCard("API configuration", "Endpoint AI saat ini dikonfigurasi di LenApi.kt. Untuk rilis publik, pindahkan request melalui backend.", Icons.Default.Api, "Developer")
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 13.sp)
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.2.sp)
        Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}

@Composable
fun ToolCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Surface, shape = RoundedCornerShape(20.dp)
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(tint.copy(alpha = .14f), RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}

@Composable
fun InfoCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, status: String) {
    Surface(color = Surface, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Purple, modifier = Modifier.size(27.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = Muted, fontSize = 11.sp)
                Text(status, color = Color(0xFFB5A4FF), fontSize = 10.sp)
            }
        }
    }
}
