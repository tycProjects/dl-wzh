package com.schedule.app.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.app.MainViewModel
import com.schedule.app.ui.theme.*

fun canUseBiometric(ctx: Context) = BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

@Composable
fun LockScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    val bioAvailable = s.biometric && canUseBiometric(ctx)

    fun askBiometric() {
        val act = ctx as? FragmentActivity ?: return
        val prompt = BiometricPrompt(act, ContextCompat.getMainExecutor(ctx), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { vm.unlock() }
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("فتح منظم الجدول").setNegativeButtonText("استخدام الرمز").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK).build())
    }
    LaunchedEffect(bioAvailable) { if (bioAvailable) askBiometric() }

    // يفتح تلقائيا عند تطابق الرمز (4 إلى 6 أرقام)
    LaunchedEffect(pin) {
        if (pin.length >= 4) { if (vm.tryUnlock(pin)) pin = "" else if (pin.length == 6) { bad = true; pin = "" } }
    }

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Filled.Lock, null, tint = Crimson, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("أدخل رمز القفل", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) { i -> Box(Modifier.size(14.dp).clip(CircleShape).then(if (i < pin.length) Modifier.background(Crimson) else Modifier.border(1.dp, Muted, CircleShape))) }
        }
        Box(Modifier.height(28.dp), contentAlignment = Alignment.Center) { if (bad) Text("الرمز غير صحيح", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("bio", "0", "del")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                row.forEach { k ->
                    Box(Modifier.size(72.dp).clip(CircleShape).background(if (k == "bio" && !bioAvailable) Bg else Surface1)
                        .clickable(enabled = !(k == "bio" && !bioAvailable)) {
                            bad = false
                            when (k) { "del" -> pin = pin.dropLast(1); "bio" -> askBiometric(); else -> if (pin.length < 6) pin += k }
                        }, contentAlignment = Alignment.Center) {
                        when (k) {
                            "del" -> Icon(Icons.AutoMirrored.Filled.Backspace, "حذف")
                            "bio" -> if (bioAvailable) Icon(Icons.Filled.Fingerprint, "البصمة", tint = Crimson)
                            else -> Text(k, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}
