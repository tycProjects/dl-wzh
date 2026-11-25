package com.example.system

import android.Manifest
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.domain.DeviceTelemetrySnapshot
import com.example.service.MayaAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceTelemetryManager(
    private val context: Context,
    private val deviceControlManager: DeviceControlManager
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _snapshot = MutableStateFlow(collectSnapshot())
    val snapshot: StateFlow<DeviceTelemetrySnapshot> = _snapshot.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            refresh(intent)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refresh()
        override fun onLost(network: Network) = refresh()
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) = refresh()
    }

    private var registered = false

    fun startObserving() {
        if (registered) return
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val sticky = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
            registered = true
            refresh(sticky)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering telemetry receivers", e)
            refresh()
        }
    }

    fun stopObserving() {
        if (!registered) return
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
        }
        registered = false
    }

    fun refresh(batteryIntentOverride: Intent? = null) {
        _snapshot.value = collectSnapshot(batteryIntentOverride)
    }

    private fun collectSnapshot(batteryIntentOverride: Intent? = null): DeviceTelemetrySnapshot {
        val batteryIntent = batteryIntentOverride ?: try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
            null
        }

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) (level * 100) / scale else 0

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val pluggedText = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "On Battery"
        }

        val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?: Int.MIN_VALUE
        val tempC = if (rawTemp != Int.MIN_VALUE && rawTemp > -1000) rawTemp / 10.0f else null

        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthText = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalStorage = statFs.totalBytes
        val availStorage = statFs.availableBytes
        val usedStorage = (totalStorage - availStorage).coerceAtLeast(0L)

        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem
        val availRam = memInfo.availMem
        val usedRam = (totalRam - availRam).coerceAtLeast(0L)

        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isMobile = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

        val networkTypeText = when {
            isWifi -> "Wi-Fi"
            isMobile -> "Mobile Data"
            isEthernet -> "Ethernet"
            else -> "Offline"
        }

        @Suppress("DEPRECATION")
        val wifiInfo = if (isWifi) {
            try {
                wifiManager.connectionInfo
            } catch (_: Exception) {
                null
            }
        } else null

        val rawSsid = wifiInfo?.ssid?.replace("\"", "")
        val cleanSsid = if (rawSsid.isNullOrBlank() || rawSsid == "<unknown ssid>") null else rawSsid
        val linkSpeed = wifiInfo?.linkSpeed?.takeIf { it > 0 }

        val (brightnessPct, isAutoBrightness) = deviceControlManager.readBrightnessState()
        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return DeviceTelemetrySnapshot(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryStatusText = statusText,
            chargingSourceText = pluggedText,
            batteryTemperatureC = tempC,
            batteryHealthText = healthText,
            batteryVoltageMv = voltageMv,
            totalStorageBytes = totalStorage,
            availableStorageBytes = availStorage,
            usedStorageBytes = usedStorage,
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            usedRamBytes = usedRam,
            isLowRam = memInfo.lowMemory,
            isWifiConnected = isWifi,
            wifiSsid = cleanSsid,
            wifiLinkSpeedMbps = linkSpeed,
            isMobileConnected = isMobile,
            networkTypeText = networkTypeText,
            isInternetValidated = isValidated,
            deviceBrand = Build.BRAND.replaceFirstChar { it.uppercase() },
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            flashlightAvailable = deviceControlManager.isFlashlightAvailable.value,
            isFlashlightOn = deviceControlManager.isFlashlightOn.value,
            brightnessPercent = brightnessPct,
            isAutoBrightness = isAutoBrightness,
            streamVolumes = deviceControlManager.readStreamVolumes(),
            isAccessibilityEnabled = MayaAccessibilityService.isAccessibilitySettingEnabled(context),
            canWriteSettings = deviceControlManager.canWriteSystemSettings(),
            hasRecordAudioPermission = hasAudioPerm
        )
    }

    companion object {
        private const val TAG = "DeviceTelemetry"
    }
}
