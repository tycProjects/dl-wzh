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

    override fun onCreate() {
        super.onCreate()
        // Track screen on/off so repeating animation loops can pause while the screen is off.
        com.android.island.core.ScreenState.set(
            (getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager).isInteractive
        )
        registerReceiver(object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context, intent: android.content.Intent) {
                when (intent.action) {
                    android.content.Intent.ACTION_SCREEN_ON -> com.android.island.core.ScreenState.set(true)
                    android.content.Intent.ACTION_SCREEN_OFF -> com.android.island.core.ScreenState.set(false)
                }
            }
        }, android.content.IntentFilter().apply {
            addAction(android.content.Intent.ACTION_SCREEN_ON)
            addAction(android.content.Intent.ACTION_SCREEN_OFF)
        })
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
