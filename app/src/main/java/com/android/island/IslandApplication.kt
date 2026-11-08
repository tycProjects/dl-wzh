package com.android.island

import android.app.Application
import com.android.island.data.SettingsRepository
import com.android.island.core.IslandBus
import com.android.island.system.ShizukuState
import com.android.island.system.StatusBarIconController
import com.android.island.util.IslandDiagnostics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class IslandApplication : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Background / memory-pressure levels only. UI_HIDDEN (20) fires every time the settings screen
        // closes, and the island must keep its current artwork ready for the next expand.
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            com.android.island.island.trimArtCaches()
        }
    }

    override fun onCreate() {
        super.onCreate()
        com.android.island.core.DeviceProfile.init(this)
        // Track screen on/off so repeating animation loops can pause while the screen is off.
        com.android.island.core.ScreenState.set(
            (getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager).isInteractive
        )
        com.android.island.core.ScreenState.refreshLocked(this)
        val lockHandler = android.os.Handler(android.os.Looper.getMainLooper())
        androidx.core.content.ContextCompat.registerReceiver(this, object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
                when (intent.action) {
                    android.content.Intent.ACTION_SCREEN_ON -> com.android.island.core.ScreenState.set(true)
                    android.content.Intent.ACTION_SCREEN_OFF -> com.android.island.core.ScreenState.set(false)
                }
                // Keyguard state settles a moment after screen on/off, so check now and again shortly after.
                com.android.island.core.ScreenState.refreshLocked(ctx)
                lockHandler.removeCallbacksAndMessages(null)
                for (d in longArrayOf(250L, 800L, 2000L)) {
                    lockHandler.postDelayed({ com.android.island.core.ScreenState.refreshLocked(ctx) }, d)
                }
            }
        }, android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SCREEN_ON)
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
            addAction(android.content.Intent.ACTION_USER_PRESENT)
        }, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        com.android.island.island.LikedTracks.init(this)
        settingsRepository = SettingsRepository(this)
        IslandBus.init(this)
        ShizukuState.start(this)
        val processScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        processScope.launch {
            combine(settingsRepository.settings, ShizukuState.status) { settings, status ->
                Triple(settings.hideStatusIcons, settings.diagnosticsEnabled, status)
            }.collect { (hideIcons, diagnosticsEnabled, status) ->
                IslandDiagnostics.setEnabled(diagnosticsEnabled)
                if (status == com.android.island.system.ShizukuStatus.READY) {
                    StatusBarIconController.apply(hideIcons, packageName)
                } else {
                    StatusBarIconController.onShizukuUnavailable()
                }
            }
        }
    }
}
