package dev.tajim.jarvis

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import dev.tajim.jarvis.assistant.AssistantService
import dev.tajim.jarvis.voice.MicPermission
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tajim.jarvis.data.ThemeMode
import dev.tajim.jarvis.data.UserSettings
import dev.tajim.jarvis.ui.navigation.JarvisShell
import dev.tajim.jarvis.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {
    override fun onStop() {
        super.onStop()
        // Cancel a one-off tap-to-talk when the app is hidden. The background assistant keeps running by design.
        val assistant = (application as JarvisApp).container.assistant
        if (!assistant.isRunning) assistant.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as JarvisApp).container
        // Re-start the background assistant if the user left it switched on (needs the mic permission already granted).
        lifecycleScope.launch {
            if (container.settingsRepository.current().assistantEnabled && MicPermission.isGranted(this@MainActivity) && !container.assistant.isRunning) {
                AssistantService.start(this@MainActivity)
            }
        }
        // Respect the system "remove animations" accessibility setting in addition to the in-app switch.
        val systemAnimationsOff =
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

        setContent {
            val loaded by container.settingsRepository.settings.collectAsStateWithLifecycle<UserSettings?>(initialValue = null)
            val settings = loaded
            val dark = settings?.themeMode != ThemeMode.LIGHT

            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                )
                onDispose { }
            }

            if (settings == null) {
                // Settings not loaded yet: show the neutral dark window colour instead of flashing the wrong theme.
                Box(Modifier.fillMaxSize().background(Color(0xFF03050F)))
            } else {
                JarvisTheme(
                    themeMode = settings.themeMode,
                    reducedMotion = settings.reducedMotion || systemAnimationsOff,
                    animationIntensity = settings.animationIntensity,
                ) {
                    JarvisShell(container)
                }
            }
        }
    }
}
