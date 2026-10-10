package com.arafat.aivision

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF070B12)
private val Panel = Color(0xFF101A27)
private val Cyan = Color(0xFF46E8FF)
private val Green = Color(0xFF67FFB0)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { VisionApp(::speak, ::stopSpeaking) }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            val result = tts?.setLanguage(Locale("bn", "BD"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "arafat-ai-utterance")
    }

    private fun stopSpeaking() { tts?.stop() }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

private data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
private fun VisionApp(speak: (String) -> Unit, stopSpeaking: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("READY") }
    var mode by remember { mutableStateOf("AUTO") }
    var messages by remember {
        mutableStateOf(listOf(ChatMessage("আমি ARAFAT AI VISION। অনলাইনে AI backend ব্যবহার করতে পারি, আর অফলাইনে basic local commands চালাতে পারি।", false)))
    }
    var pickedFile by remember { mutableStateOf<String?>(null) }
    var voiceError by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            pickedFile = uri.lastPathSegment ?: uri.toString()
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecognition(context,
            onText = { recognized -> input = recognized; status = "READY" },
            onError = { error -> voiceError = error; status = "ERROR" })
        else voiceError = "Microphone permission was denied."
    }

    val online = remember { mutableStateOf(isOnline(context)) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, secondary = Green, background = Bg, surface = Panel
    )) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("ARAFAT AI VISION", color = Cyan, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        Text("DEVELOPED BY ARAFAT", color = Green, style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (isOnline(context)) "ONLINE" else "OFFLINE", color = if (isOnline(context)) Green else Color.Yellow)
                        Text(status, color = Cyan, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Box(
                    Modifier.fillMaxWidth().height(128.dp)
                        .background(Brush.radialGradient(listOf(Color(0xFF14516A), Bg)), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(86.dp).background(Brush.linearGradient(listOf(Cyan, Green)), CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(68.dp).background(Panel, CircleShape), contentAlignment = Alignment.Center) {
                            Text("AI", color = Cyan, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mode", color = Color.White)
                    Spacer(Modifier.width(12.dp))
                    SingleChoiceSegmentedButtonRow {
                        listOf("AUTO", "ONLINE", "OFFLINE").forEachIndexed { index, value ->
                            SegmentedButton(selected = mode == value, onClick = { mode = value }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(value) }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .background(Panel, RoundedCornerShape(18.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
                        ) {
                            Text(if (message.isUser) "YOU" else "ARAFAT AI", color = if (message.isUser) Green else Cyan, style = MaterialTheme.typography.labelSmall)
                            Text(message.text, color = Color.White, modifier = Modifier
                                .background(if (message.isUser) Color(0xFF17352F) else Color(0xFF182838), RoundedCornerShape(12.dp))
                                .padding(10.dp))
                            if (!message.isUser) {
                                TextButton(onClick = { speak(message.text) }) { Icon(Icons.Default.VolumeUp, null); Text(" Speak") }
                            }
                        }
                    }
                }

                pickedFile?.let {
                    Text("Attached: $it", color = Green)
                }
                voiceError?.let { Text(it, color = Color(0xFFFF7B7B)) }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ask or say a command…") },
                    maxLines = 4,
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }) { Icon(Icons.Default.AttachFile, "Attach file", tint = Cyan) }
                            IconButton(onClick = { if (SpeechRecognizer.isRecognitionAvailable(context)) micPermission.launch(Manifest.permission.RECORD_AUDIO) else voiceError = "Speech recognition service is unavailable." }) {
                                Icon(Icons.Default.Mic, "Voice input", tint = Green)
                            }
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { stopSpeaking(); status = "READY" },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Panel)
                    ) { Icon(Icons.Default.Stop, null); Text(" Stop voice") }
                    Button(
                        onClick = {
                            val prompt = input.trim()
                            if (prompt.isNotEmpty() && !busy) {
                                messages = messages + ChatMessage(prompt, true)
                                input = ""
                                busy = true
                                status = "THINKING"
                                scope.launch {
                                    val reply = if (mode == "OFFLINE" || !isOnline(context)) {
                                        localCommand(context, prompt)
                                    } else {
                                        AiRepository.ask(prompt)
                                    }
                                    messages = messages + ChatMessage(reply, false)
                                    status = if (mode == "OFFLINE" || !isOnline(context)) "OFFLINE" else "READY"
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) { Icon(Icons.Default.Send, null); Text(if (busy) "Thinking…" else "Send") }
                }
                Text("Offline commands: time, date, battery, open Google/YouTube/Chrome, search web.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun startRecognition(context: Context, onText: (String) -> Unit, onError: (String) -> Unit) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "বলুন…")
        }
        // Activity-result speech recognizer UI is widely supported by Android speech services.
        val launcherIntent = intent
        context.startActivity(launcherIntent)
    } catch (e: ActivityNotFoundException) {
        onError("No compatible speech recognition activity is installed.")
    } catch (e: Exception) {
        onError(e.message ?: "Voice recognition failed.")
    }
}

private suspend fun localCommand(context: Context, raw: String): String {
    val q = raw.lowercase(Locale.ROOT)
    return when {
        "who created" in q || "developer" in q || "কে বানাই" in raw || "কে তৈরি" in raw ->
            "আমাকে Arafat ভাই তৈরি করেছেন। তিনি আমার Developer."
        "time" in q || "সময়" in raw || "সময়" in raw ->
            "এখন সময় ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())}"
        "date" in q || "তারিখ" in raw ->
            "আজ ${DateFormat.getDateInstance(DateFormat.FULL).format(Date())}"
        "battery" in q || "ব্যাটারি" in raw -> {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            "Battery level: ${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)} percent."
        }
        "open youtube" in q || "ইউটিউব" in raw -> openPackageOrWeb(context, "com.google.android.youtube", "https://www.youtube.com")
        "open chrome" in q || "ক্রোম" in raw -> openPackageOrWeb(context, "com.android.chrome", "https://www.google.com/chrome/")
        "open google" in q || "গুগল" in raw -> openUrl(context, "https://www.google.com")
        "search" in q || "সার্চ" in raw -> {
            val term = raw.replace(Regex("(?i)google|search|সার্চ|করো|কর"), "").trim()
            openUrl(context, "https://www.google.com/search?q=" + Uri.encode(term.ifBlank { raw }))
            "Google search opened."
        }
        "settings" in q || "সেটিংস" in raw -> {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Opening Android Settings."
        }
        else -> "Offline mode-এ এই কমান্ডটি বুঝতে পারিনি। ইন্টারনেট চালু করে AI backend ব্যবহার করুন, অথবা বলুন: Google খোলো, সময় কত, তারিখ বলো, ব্যাটারি কত।"
    }
}

private fun openPackageOrWeb(context: Context, packageName: String, fallback: String): String {
    val launch = context.packageManager.getLaunchIntentForPackage(packageName)
    return if (launch != null) {
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        "App opened."
    } else {
        openUrl(context, fallback)
        "App is not installed; opened its website instead."
    }
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

private object AiRepository {
    // Change to your deployed HTTPS backend. Never put provider API keys in this app.
    private const val BASE_URL = "https://YOUR_BACKEND_HOST"
    suspend fun ask(prompt: String): String = withContext(Dispatchers.IO) {
        try {
            val connection = (URL("$BASE_URL/api/ai/chat").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            val body = JSONObject().put("message", prompt).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            connection.disconnect()
            if (code !in 200..299) return@withContext "AI backend error ($code): ${runCatching { JSONObject(response).optString("error") }.getOrDefault("Request failed")}"
            JSONObject(response).optString("reply", "Backend returned no reply.")
        } catch (e: Exception) {
            "Online AI is not configured or reachable. Set BASE_URL to your deployed HTTPS backend. Details: ${e.message ?: "network error"}"
        }
    }
}
