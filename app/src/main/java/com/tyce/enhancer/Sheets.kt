package com.tyce.enhancer

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppearanceSheet(visible: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    SheetHost(visible, onDismiss) {
        Text("Appearance", style = heading(20f, FontWeight.SemiBold, C.Text), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        Text("AMOLED Black uses pure #000000 so black pixels stay off on OLED screens.",
            style = inter(14f, FontWeight.Normal, C.Muted, 1.35f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        val modes = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.AMOLED to "AMOLED Black")
        Segmented(modes.take(2).map { it.second }, modes.take(2).indexOfFirst { it.first == Appearance.mode }) { Appearance.set(context, modes[it].first) }
        Spacer(Modifier.height(8.dp))
        Segmented(modes.drop(2).map { it.second }, modes.drop(2).indexOfFirst { it.first == Appearance.mode }) { Appearance.set(context, modes[it + 2].first) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun CloudSheet(visible: Boolean, useCloud: Boolean, onUseCloud: (Boolean) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val provider = ReplicateProvider
    SheetHost(visible, onDismiss) {
        var saved by remember(visible) { mutableStateOf(SecureKeyStore.has(context, provider.id)) }
        var key by remember(visible) { mutableStateOf("") }
        var show by remember { mutableStateOf(false) }
        var busy by remember { mutableStateOf(false) }
        var status by remember(visible) { mutableStateOf<String?>(null) }
        var statusOk by remember { mutableStateOf(false) }
        var help by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState())) {
            Text("Cloud & API keys", style = heading(20f, FontWeight.SemiBold, C.Text), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text("Local models run on this phone. Cloud mode sends the photo to ${provider.displayName}.",
                style = inter(14f, FontWeight.Normal, C.Muted, 1.35f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            SheetSection("Processing", if (saved) "Cloud uses your ${provider.displayName} key and may cost money." else "Save a valid key to enable Cloud.") {
                Segmented(listOf("On this phone", "Cloud"), if (useCloud && saved) 1 else 0) { i ->
                    if (i == 1 && !saved) Toast.makeText(context, "Add and verify an API key first.", Toast.LENGTH_SHORT).show()
                    else onUseCloud(i == 1)
                }
            }
            Spacer(Modifier.height(18.dp))
            SheetSection("${provider.displayName} API key", if (saved) "A key is stored encrypted on this device." else "No key saved.") {
                OutlinedTextField(
                    value = key, onValueChange = { key = it.trim() }, singleLine = true,
                    label = { Text(if (saved) "Enter a new key to replace" else "Paste API key") },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineButton(if (show) "Hide" else "Show", Modifier.weight(1f)) { show = !show }
                    BrandButton(if (busy) "Checking…" else "Save & verify", Modifier.weight(2f), {
                        if (busy || key.length < 8) { status = "Paste a key first."; statusOk = false; return@BrandButton }
                        busy = true; status = null
                        scope.launch {
                            val err = withContext(Dispatchers.IO) { provider.validate(key) }
                            busy = false
                            if (err == null) {
                                SecureKeyStore.put(context, provider.id, key)
                                saved = true; key = ""; statusOk = true; status = "Key verified and saved."
                            } else { statusOk = false; status = err }
                        }
                    })
                }
                if (saved) {
                    Spacer(Modifier.height(8.dp))
                    OutlineButton("Remove saved key", Modifier.fillMaxWidth()) {
                        SecureKeyStore.remove(context, provider.id); saved = false; onUseCloud(false)
                        statusOk = true; status = "Key removed."
                    }
                }
                status?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = inter(13f, FontWeight.Medium, if (statusOk) C.Accent else C.Red))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(provider.privacyNote, style = inter(12f, FontWeight.Normal, C.Muted, 1.35f))
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().pressable(0.99f) { help = !help }.padding(vertical = 8.dp)) {
                Text("How to get API keys", style = inter(16f, FontWeight.Bold, C.Text), modifier = Modifier.weight(1f))
                Text(if (help) "Hide" else "Show", style = inter(14f, FontWeight.SemiBold, C.Accent))
            }
            if (help) {
                provider.howTo.forEachIndexed { i, step ->
                    Text("${i + 1}. $step", style = inter(14f, FontWeight.Normal, C.Text, 1.35f), modifier = Modifier.padding(vertical = 3.dp))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineButton("Open API tokens", Modifier.weight(1f)) { Links.open(context, provider.keyHelpUrl, null) }
                    OutlineButton("Official docs", Modifier.weight(1f)) { Links.open(context, provider.docsUrl, null) }
                }
                Text("Usage limits and pricing are set by ${provider.displayName}; see your account's billing page.",
                    style = inter(12f, FontWeight.Normal, C.Muted, 1.35f), modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
