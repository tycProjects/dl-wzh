package com.android.island.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.island.IslandApplication
import com.android.island.data.IslandSettings
import com.android.island.core.IslandBus
import com.android.island.core.IslandActivity
import com.android.island.core.LyricLine
import com.android.island.service.IslandAccessibilityService
import com.android.island.service.WhatsAppNotificationListener
import com.android.island.ui.screens.HomeScreen
import com.android.island.ui.theme.DynamicIslandTheme
import com.android.island.ui.theme.Ui
import com.android.island.system.ShizukuState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    // Result handler for READ_PHONE_STATE: recreate so the permission row shows its new state.
    private val phoneStateLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { recreate() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as IslandApplication

        setContent {
            DynamicIslandTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Ui.Bg) {
                    var tick by remember { mutableIntStateOf(0) }

                    DisposableEffect(this@MainActivity) {
                        val observer = object : DefaultLifecycleObserver {
                            override fun onResume(owner: LifecycleOwner) {
                                tick++
                                ShizukuState.refresh()
                            }
                        }
                        lifecycle.addObserver(observer)
                        onDispose { lifecycle.removeObserver(observer) }
                    }

                    key(tick) {
                        val settings by app.settingsRepository.settings.collectAsStateWithLifecycle(
                            initialValue = IslandSettings()
                        )
                        HomeScreen(
                            settings = settings,
                            onUpdateSettings = { transform ->
                                lifecycleScope.launch {
                                    app.settingsRepository.update(transform)
                                }
                            },
                            onRequestAccessibility = { openAccessibility() },
                            onRequestNotificationListener = { openNotificationListener() },
                            onRequestShizuku = { ShizukuState.openOrRequest(this@MainActivity) },
                            hasPhoneState = com.android.island.service.PhoneStateMonitor.hasPermission(this@MainActivity),
                            onRequestPhoneState = {
                                phoneStateLauncher.launch(android.Manifest.permission.READ_PHONE_STATE)
                            },
                            onRestartIsland = { restartIsland(app) },
                            onDemoScenario = { scenario -> showDemo(scenario) },
                            onApplyRecommended = {
                                lifecycleScope.launch {
                                    app.settingsRepository.resetAll()
                                    com.android.island.util.IslandDiagnostics.setEnabled(false)
                                }
                            },
                            hasAccessibility = isAccessibilityEnabled(),
                            hasNotifListener = isNotificationListenerEnabled()
                        )
                    }
                }
            }
        }
    }

    private fun restartIsland(app: IslandApplication) {
        lifecycleScope.launch {
            val wasEnabled = app.settingsRepository.settings.let { flow ->
                flow.first().enabled
            }
            if (!wasEnabled) return@launch
            app.settingsRepository.update { it.copy(enabled = false) }
            kotlinx.coroutines.delay(350)
            app.settingsRepository.update { it.copy(enabled = true) }
        }
    }

    private fun showDemo(scenario: String) {
        val media = IslandActivity.Media(
            id = "demo-media",
            title = "Midnight City (Demo)",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            isPlaying = true,
            position = 42_000L,
            duration = 240_000L,
            packageName = packageName,
            lyrics = listOf(
                LyricLine(0L, "Waiting in the car"),
                LyricLine(18_000L, "Waiting for the ride in the dark"),
                LyricLine(42_000L, "The city is my church")
            ),
            currentLyricIndex = 2
        )
        val pausedMedia = media.copy(isPlaying = false, position = 96_000L)
        fun makeChat(i: Int, name: String, text: String, group: Boolean = false) = IslandActivity.WhatsApp(
            id = "demo-wa-$i",
            senderName = name,
            message = text,
            isGroup = group,
            groupName = if (group) "Keluarga" else null,
            conversationKey = "demo-wa-$i"
        )
        val chatMain = makeChat(1, "Alex", "Hai! Ini contoh preview notifikasi WhatsApp untuk Android Island.")
        val longChat = makeChat(4, "Sinta", "Ini pesan yang sengaja dibuat panjang supaya kita bisa melihat bagaimana banner memotong teks, lalu menampilkan sisanya saat kartu diperluas di layar.")
        val openApp = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val callIn = IslandActivity.Call(
            id = "demo-call-in",
            callerName = "Ibu",
            subtitle = "Panggilan masuk · mobile",
            isIncoming = true,
            answerAction = openApp,
            declineAction = openApp,
            contentIntent = openApp
        )
        val callActive = IslandActivity.Call(
            id = "demo-call-active",
            callerName = "Budi Santoso",
            isIncoming = false,
            declineAction = openApp,
            contentIntent = openApp,
            timestamp = System.currentTimeMillis() - 65_000L
        )
        when (scenario) {
            "media" -> IslandBus.pushActivity(media)
            "media_paused" -> IslandBus.pushActivity(pausedMedia)
            "whatsapp" -> IslandBus.pushActivity(chatMain)
            "whatsapp_group" -> listOf(
                makeChat(1, "Alex", "Sudah sampai?"),
                makeChat(2, "Rina", "Foto tadi dikirim ya"),
                makeChat(3, "Keluarga", "Nanti malam kumpul", group = true)
            ).forEach { IslandBus.pushActivity(it) }
            "whatsapp_long" -> IslandBus.pushActivity(longChat)
            "call_in" -> IslandBus.pushActivity(callIn)
            "call_active" -> IslandBus.pushActivity(callActive)
            "both" -> { IslandBus.pushActivity(media); IslandBus.pushActivity(chatMain) }
            "stop" -> {
                IslandBus.clearCall("demo-call-in")
                IslandBus.clearCall("demo-call-active")
                IslandBus.clearWhatsApp()
                IslandBus.clearMedia()
            }
        }
    }

    private fun openAccessibility() {
        startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun openNotificationListener() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(this, WhatsAppNotificationListener::class.java).flattenToString()
            )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
        runCatching {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, IslandAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    }
}
