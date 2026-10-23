package com.lecturemanager.app.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
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
import com.lecturemanager.app.MainViewModel
import com.lecturemanager.app.ui.theme.*

fun canUseBiometric(ctx: android.content.Context): Boolean =
    BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

private fun promptBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
    val prompt = BiometricPrompt(
        activity, ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("فتح مدير المحاضرات")
        .setNegativeButtonText("استخدام الرمز")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
}

@Composable
fun LockScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val bioReady = settings.biometric && canUseBiometric(ctx)

    fun tryBio() { (ctx.findActivity() as? FragmentActivity)?.let { promptBiometric(it) { vm.unlock() } } }
    LaunchedEffect(Unit) { if (bioReady) tryBio() }

    fun press(d: String) {
        if (pin.length >= 6) return
        pin += d; error = false
        if (pin.length >= 4 && vm.tryUnlock(pin)) return
        if (pin.length == 6) { error = true; pin = "" }
    }

    Column(
        Modifier.fillMaxSize().background(Canvas).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Lock, null, tint = Teal, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text("أدخل رمز القفل", style = MaterialTheme.typography.titleLarge, color = Navy)
        Spacer(Modifier.height(4.dp))
        Text(if (error) "الرمز غير صحيح" else " ", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(6) { i ->
                Box(Modifier.size(14.dp).clip(CircleShape).background(if (i < pin.length) Teal else Border))
            }
        }
        Spacer(Modifier.height(28.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                r.forEach { d -> PadKey(d) { press(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            if (bioReady) PadKey(icon = Icons.Filled.Fingerprint) { tryBio() } else Spacer(Modifier.size(68.dp))
            PadKey("0") { press("0") }
            PadKey(icon = Icons.Filled.Backspace) { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
        }
    }
}

@Composable
private fun PadKey(label: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Box(
        Modifier.size(68.dp).clip(CircleShape).background(CardBg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (label != null) Text(label, style = MaterialTheme.typography.headlineMedium, color = Navy)
        if (icon != null) Icon(icon, null, tint = Navy)
    }
}
