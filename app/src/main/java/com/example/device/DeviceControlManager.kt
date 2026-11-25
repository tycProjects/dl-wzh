package com.example.device

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class BatteryStatus(
    val levelPercent: Int,
    val isCharging: Boolean,
    val chargeSource: String,
    val temperatureC: Float?,
    val healthLabel: String
)

data class StorageStatus(
    val totalGb: Float,
    val usedGb: Float,
    val freeGb: Float,
    val usedPercent: Int
)

data class NetworkStatus(
    val isConnected: Boolean,
    val transportType: String,
    val isWifi: Boolean,
    val isCellular: Boolean,
    val isMetered: Boolean,
    val downstreamMbps: Int
)

data class DeviceTelemetry(
    val brand: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val totalRamGb: Float,
    val availableRamGb: Float,
    val battery: BatteryStatus,
    val storage: StorageStatus,
    val network: NetworkStatus,
    val mediaVolumePercent: Int,
    val isMediaMuted: Boolean,
    val brightnessPercent: Int,
    val canWriteSystemSettings: Boolean,
    val hasFlashlight: Boolean,
    val isFlashlightOn: Boolean
)

class DeviceControlManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val activityManager =
        appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val torchCameraId: String? by lazy {
        runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                _isFlashlightOn.value = enabled
            }
        }
    }

    init {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.registerTorchCallback(torchCallback, null)
            }
        }
    }

    fun hasFlashlight(): Boolean =
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) &&
            torchCameraId != null

    fun setFlashlight(enabled: Boolean): Result<Boolean> {
        val camId = torchCameraId
            ?: return Result.failure(IllegalStateException("এই ফোনে ফ্ল্যাশলাইট সাপোর্ট করছে না।"))
        return runCatching {
            cameraManager.setTorchMode(camId, enabled)
            _isFlashlightOn.value = enabled
            enabled
        }
    }

    fun toggleFlashlight(): Result<Boolean> = setFlashlight(!_isFlashlightOn.value)

    fun getMediaVolumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((current * 100f) / max).toInt().coerceIn(0, 100)
    }

    fun setMediaVolumePercent(percent: Int): Int {
        val clamped = percent.coerceIn(0, 100)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = ((clamped / 100f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return getMediaVolumePercent()
    }

    fun adjustMediaVolume(deltaPercent: Int): Int =
        setMediaVolumePercent(getMediaVolumePercent() + deltaPercent)

    fun setMediaMuted(mute: Boolean): Boolean {
        val dir = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
        return isMediaMuted()
    }

    fun isMediaMuted(): Boolean =
        audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0

    fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(appContext)

    fun getBrightnessPercent(): Int {
        val raw = runCatching {
            Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(128)
        return ((raw / 255f) * 100f).toInt().coerceIn(0, 100)
    }

    fun setBrightnessPercent(percent: Int, hostActivity: Activity? = null): Result<Int> {
        val clamped = percent.coerceIn(5, 100)
        // Always update in-app window brightness immediately if Activity is available
        hostActivity?.let { act ->
            act.runOnUiThread {
                val lp = act.window.attributes
                lp.screenBrightness = clamped / 100f
                act.window.attributes = lp
            }
        }
        return if (canWriteSystemSettings()) {
            runCatching {
                Settings.System.putInt(
                    appContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                val value255 = ((clamped / 100f) * 255f).toInt().coerceIn(10, 255)
                Settings.System.putInt(
                    appContext.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    value255
                )
                clamped
            }
        } else {
            Result.failure(SecurityException("সিস্টেম ব্রাইটনেস বদলাতে Modify System Settings অনুমতি লাগবে।"))
        }
    }

    fun openWriteSettingsPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${appContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(intent) }
    }

    fun getBatteryStatus(): BatteryStatus {
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC চার্জার"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB চার্জার"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "ওয়্যারলেস"
            else -> if (isCharging) "চার্জিং" else "ব্যাটারি"
        }
        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempC = if (tempTenths > 0) tempTenths / 10f else null
        val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "ভালো"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "গরম"
            BatteryManager.BATTERY_HEALTH_DEAD -> "দুর্বল"
            else -> "স্বাভাবিক"
        }
        return BatteryStatus(pct, isCharging, source, tempC, health)
    }

    fun getStorageStatus(): StorageStatus {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes.toDouble()
        val free = stat.availableBytes.toDouble()
        val used = (total - free).coerceAtLeast(0.0)
        val gb = 1024.0 * 1024.0 * 1024.0
        val usedPct = if (total > 0) ((used / total) * 100).toInt() else 0
        return StorageStatus(
            totalGb = (total / gb).toFloat(),
            usedGb = (used / gb).toFloat(),
            freeGb = (free / gb).toFloat(),
            usedPercent = usedPct
        )
    }

    fun getNetworkStatus(): NetworkStatus {
        val active = connectivityManager.activeNetwork
        val caps = active?.let { connectivityManager.getNetworkCapabilities(it) }
        if (caps == null) {
            return NetworkStatus(false, "সংযোগ নেই", false, false, false, 0)
        }
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        val type = when {
            isWifi && isVpn -> "Wi-Fi + VPN"
            isWifi -> "Wi-Fi"
            isCellular && isVpn -> "Mobile Data + VPN"
            isCellular -> "Mobile Data"
            isVpn -> "VPN"
            else -> "অন্যান্য নেটওয়ার্ক"
        }
        val metered = connectivityManager.isActiveNetworkMetered
        val downMbps = (caps.linkDownstreamBandwidthKbps / 1000).coerceAtLeast(0)
        return NetworkStatus(hasInternet, type, isWifi, isCellular, metered, downMbps)
    }

    fun collectTelemetry(): DeviceTelemetry {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val gb = 1024f * 1024f * 1024f
        val brand = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        return DeviceTelemetry(
            brand = brand,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            totalRamGb = memInfo.totalMem / gb,
            availableRamGb = memInfo.availMem / gb,
            battery = getBatteryStatus(),
            storage = getStorageStatus(),
            network = getNetworkStatus(),
            mediaVolumePercent = getMediaVolumePercent(),
            isMediaMuted = isMediaMuted(),
            brightnessPercent = getBrightnessPercent(),
            canWriteSystemSettings = canWriteSystemSettings(),
            hasFlashlight = hasFlashlight(),
            isFlashlightOn = _isFlashlightOn.value
        )
    }

    fun openSettingsShortcut(target: String): Boolean {
        val action = when (target.lowercase()) {
            "internet", "panel" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Settings.Panel.ACTION_INTERNET_CONNECTIVITY
            } else {
                Settings.ACTION_WIFI_SETTINGS
            }
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "notifications" -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return runCatching {
            val intent = Intent(action).apply {
                if (action == Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
                    putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openDialer(phoneNumber: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openSmsComposer(recipient: String, body: String): Boolean {
        return runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$recipient")).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun buildContextSummary(): String {
        val t = collectTelemetry()
        val netText = if (t.network.isConnected) t.network.transportType else "অফলাইন"
        return "ব্যাটারি ${t.battery.levelPercent}% (${t.battery.chargeSource}), " +
            "স্টোরেজ ফ্রি ${String.format(Locale.US, "%.1f", t.storage.freeGb)} GB, " +
            "নেটওয়ার্ক: $netText, ভলিউম ${t.mediaVolumePercent}%, ব্রাইটনেস ${t.brightnessPercent}%"
    }
}
