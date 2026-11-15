package com.zewmod.engine

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.zewmod.engine.bridge.ZewModBridge
import com.zewmod.engine.model.AppConfig
import com.zewmod.engine.model.ZewModFeatures
import com.zewmod.engine.util.PreferenceHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ZewModApp : Application() {

    companion object {
        const val CHANNEL_ID_VPN     = "zewmod_vpn"
        const val CHANNEL_ID_FLOAT   = "zewmod_float"
        lateinit var instance: ZewModApp private set
    }

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    lateinit var prefs: PreferenceHelper private set
    var config: AppConfig = AppConfig()
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferenceHelper(this)
        createNotificationChannels()
        loadLocalConfig()
        ZewModBridge.init(this)
    }

    private fun loadLocalConfig() {
        appScope.launch(Dispatchers.IO) {
            runCatching {
                val raw = assets.open("local_config.json")
                    .bufferedReader().readText()
                config = json.decodeFromString(AppConfig.serializer(), raw)
                ZewModFeatures.applyEnabledFromConfig(config.features)
            }
        }
    }

    fun updateConfig(newConfig: AppConfig) {
        config = newConfig
        ZewModFeatures.applyEnabledFromConfig(newConfig.features)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            NotificationChannel(
                CHANNEL_ID_VPN,
                "ZewMod VPN",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ZewMod Engine VPN đang chạy"
                setShowBadge(false)
                nm.createNotificationChannel(this)
            }

            NotificationChannel(
                CHANNEL_ID_FLOAT,
                "ZewMod Menu",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ZewMod floating menu"
                setShowBadge(false)
                nm.createNotificationChannel(this)
            }
        }
    }
}
