package com.example.phoneassistant

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("p", MODE_PRIVATE)
        val agent = Agent({ prefs.getString("key", "") ?: "" }, PhoneActions(applicationContext))
        setContent { MaterialTheme { Chat(agent, prefs) } }
    }
}

@Composable
fun Chat(agent: Agent, prefs: SharedPreferences) {
    val scope = rememberCoroutineScope()
    val msgs = remember { mutableStateListOf<Pair<Boolean, String>>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf(prefs.getString("key", "") ?: "") }
    var keyDraft by remember { mutableStateOf("") }

    fun send(t: String) {
        if (t.isBlank() || busy) return
        msgs += true to t; input = ""; busy = true
        scope.launch {
            val r = try { agent.ask(t) } catch (e: Exception) { "Ошибка: ${e.message}" }
            msgs += false to r; busy = false
        }
    }

    val mic = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { send(it) }
    }
    val micIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(12.dp)) {
        if (key.isBlank()) {
            Text("Вставь API-ключ Anthropic (хранится только на устройстве)")
            OutlinedTextField(keyDraft, { keyDraft = it }, Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = {
                prefs.edit().putString("key", keyDraft.trim()).apply(); key = keyDraft.trim()
            }) { Text("Сохранить") }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(msgs) { (mine, text) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (mine) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                        ) { Text(text, Modifier.padding(10.dp)) }
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Что сделать?") })
                TextButton(onClick = { mic.launch(micIntent) }) { Text("🎤") }
                TextButton(onClick = { send(input) }) { Text("➤") }
            }
        }
    }
}
