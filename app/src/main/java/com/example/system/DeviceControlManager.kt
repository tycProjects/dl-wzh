package com.example.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.domain.ActionResult
import com.example.domain.AudioStreamType
import com.example.domain.InstalledAppInfo
import com.example.domain.PermissionRequirement
import com.example.domain.StreamVolumeState
import com.example.service.MayaAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DeviceControlManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val packageManager = context.packageManager

    private val torchCameraId: String? by lazy { findTorchCameraId() }

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _isFlashlightAvailable = MutableStateFlow(torchCameraId != null)
    val isFlashlightAvailable: StateFlow<Boolean> = _isFlashlightAvailable.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                _isFlashlightOn.value = enabled
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == torchCameraId) {
                _isFlashlightOn.value = false
            }
        }
    }

    init {
        try {
            cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register TorchCallback", e)
        }
    }

    fun release() {
        try {
            cameraManager.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister TorchCallback", e)
        }
    }

    private fun findTorchCameraId(): String? {
        return try {
            val ids = cameraManager.cameraIdList
            ids.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: ids.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to inspect camera flash characteristics", e)
            null
        }
    }

    fun setFlashlight(enabled: Boolean): ActionResult {
        val camId = torchCameraId
            ?: return ActionResult(
                success = false,
                message = "No hardware flashlight found on this device."
            )
        return try {
            cameraManager.setTorchMode(camId, enabled)
            _isFlashlightOn.value = enabled
            ActionResult(
                success = true,
                message = if (enabled) "Flashlight turned ON" else "Flashlight turned OFF"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle flashlight", e)
            ActionResult(
                success = false,
                message = "Flashlight control failed: ${e.localizedMessage ?: "Camera unavailable"}"
            )
        }
    }

    fun toggleFlashlight(): ActionResult = setFlashlight(!_isFlashlightOn.value)

    fun readStreamVolumes(): List<StreamVolumeState> {
        return AudioStreamType.entries.map { stream ->
            val current = audioManager.getStreamVolume(stream.androidStream)
            val max = audioManager.getStreamMaxVolume(stream.androidStream).coerceAtLeast(1)
            val muted = audioManager.isStreamMute(stream.androidStream)
            StreamVolumeState(
                streamType = stream,
                current = current,
                max = max,
                isMuted = muted
            )
        }
    }

    fun adjustVolume(stream: AudioStreamType, raise: Boolean): ActionResult {
        return try {
            val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager.adjustStreamVolume(
                stream.androidStream,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            val now = audioManager.getStreamVolume(stream.androidStream)
            val max = audioManager.getStreamMaxVolume(stream.androidStream)
            ActionResult(
                success = true,
                message = "${stream.label} volume set to $now / $max"
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Volume adjustment blocked by Do Not Disturb policy", e)
            ActionResult(
                success = false,
                message = "${stream.label} volume is restricted by Do Not Disturb settings."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust volume", e)
            ActionResult(
                success = false,
                message = "Volume adjustment failed: ${e.localizedMessage ?: "Error"}"
            )
        }
    }

    fun setStreamVolume(stream: AudioStreamType, level: Int): ActionResult {
        return try {
            val max = audioManager.getStreamMaxVolume(stream.androidStream)
            val clamped = level.coerceIn(0, max)
            audioManager.setStreamVolume(
                stream.androidStream,
                clamped,
                AudioManager.FLAG_SHOW_UI
            )
            ActionResult(
                success = true,
                message = "${stream.label} volume set to $clamped / $max"
            )
        } catch (e: SecurityException) {
            ActionResult(
                success = false,
                message = "${stream.label} stream cannot be modified while Do Not Disturb is active."
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                message = "Could not set ${stream.label} volume: ${e.localizedMessage ?: "Error"}"
            )
        }
    }

    fun setStreamMuted(stream: AudioStreamType, mute: Boolean): ActionResult {
        return try {
            val direction = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
            audioManager.adjustStreamVolume(
                stream.androidStream,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            ActionResult(
                success = true,
                message = if (mute) "${stream.label} muted" else "${stream.label} unmuted"
            )
        } catch (e: SecurityException) {
            ActionResult(
                success = false,
                message = "Muting ${stream.label} requires Do Not Disturb access or manual confirmation."
            )
        } catch (e: Exception) {
            ActionResult(
                success = false,
                message = "Failed to change mute state: ${e.localizedMessage ?: "Error"}"
            )
        }
    }

    fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(context)

    fun readBrightnessState(): Pair<Int, Boolean> {
        return try {
            val raw = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            val mode = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            val percent = ((raw.coerceIn(0, 255) / 255f) * 100f).toInt().coerceIn(0, 100)
            val isAuto = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            percent to isAuto
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read brightness", e)
            50 to false
        }
    }

    fun setBrightnessPercent(percent: Int): ActionResult {
        if (!canWriteSystemSettings()) {
            return ActionResult(
                success = false,
                message = "Modify System Settings permission is required to change screen brightness.",
                requiresPermission = true,
                permissionType = PermissionRequirement.WRITE_SETTINGS
            )
        }
        return try {
            val clampedPercent = percent.coerceIn(1, 100)
            val value255 = ((clampedPercent / 100f) * 255f).toInt().coerceIn(2, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value255
            )
            ActionResult(
                success = true,
                message = "Screen brightness set to $clampedPercent%"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write brightness", e)
            ActionResult(
                success = false,
                message = "Unable to set brightness: ${e.localizedMessage ?: "Error"}"
            )
        }
    }

    fun adjustBrightness(increase: Boolean): ActionResult {
        val (currentPercent, _) = readBrightnessState()
        val target = if (increase) currentPercent + 15 else currentPercent - 15
        return setBrightnessPercent(target.coerceIn(5, 100))
    }

    suspend fun loadLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        val myPkg = context.packageName
        resolveInfos
            .asSequence()
            .filter { it.activityInfo?.packageName != null && it.activityInfo.packageName != myPkg }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val pkg = info.activityInfo.packageName
                val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                    .ifEmpty { pkg }
                val iconBitmap = try {
                    drawableToImageBitmap(info.loadIcon(packageManager))
                } catch (_: Exception) {
                    null
                }
                InstalledAppInfo(
                    packageName = pkg,
                    label = label,
                    icon = iconBitmap
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launchAppByNameOrPackage(query: String, installedApps: List<InstalledAppInfo>): ActionResult {
        val normalized = normalizeAppQuery(query)
        if (normalized.isBlank()) {
            return ActionResult(false, "Please specify which app to open.")
        }

        val match = installedApps.firstOrNull {
            it.label.equals(normalized, ignoreCase = true) ||
                it.packageName.equals(normalized, ignoreCase = true)
        } ?: installedApps.firstOrNull {
            it.label.contains(normalized, ignoreCase = true) ||
                normalized.contains(it.label, ignoreCase = true) ||
                it.packageName.contains(normalized, ignoreCase = true)
        }

        if (match == null) {
            return ActionResult(
                success = false,
                message = "Could not find an installed app matching \"$query\"."
            )
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(match.packageName)
            ?: return ActionResult(
                success = false,
                message = "${match.label} does not provide a launchable screen."
            )

        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ActionResult(
                success = true,
                message = "Opened ${match.label}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch ${match.packageName}", e)
            ActionResult(
                success = false,
                message = "Unable to open ${match.label}: ${e.localizedMessage ?: "Error"}"
            )
        }
    }

    private fun normalizeAppQuery(raw: String): String {
        val cleaned = raw.trim().lowercase()
        val bengaliAliases = mapOf(
            "ইউটিউব" to "youtube",
            "ফেসবুক" to "facebook",
            "হোয়াটসঅ্যাপ" to "whatsapp",
            "হোয়াটসঅ্যাপ" to "whatsapp",
            "ক্রোম" to "chrome",
            "ক্যামেরা" to "camera",
            "সেটিংস" to "settings",
            "গ্যালারি" to "gallery",
            "ফটো" to "photos",
            "জিমেইল" to "gmail",
            "ম্যাপ" to "maps",
            "ক্যালকুলেটর" to "calculator",
            "ঘড়ি" to "clock",
            "মেসেজ" to "messages",
            "ফোন" to "phone",
            "প্লে স্টোর" to "play store",
            "ইনস্টাগ্রাম" to "instagram",
            "টেলিগ্রাম" to "telegram",
            "স্পটিফাই" to "spotify"
        )
        return bengaliAliases.entries.firstOrNull { cleaned.contains(it.key) }?.value ?: raw.trim()
    }

    fun openQuickSettingsPanel(): ActionResult {
        if (MayaAccessibilityService.instance == null) {
            return ActionResult(
                success = false,
                message = "Enable Maya Accessibility Service to open Android Quick Settings directly.",
                requiresPermission = true,
                permissionType = PermissionRequirement.ACCESSIBILITY
            )
        }
        val ok = MayaAccessibilityService.performQuickSettings()
        return ActionResult(
            success = ok,
            message = if (ok) "Opened Quick Settings panel" else "Could not open Quick Settings panel"
        )
    }

    fun openNotificationsPanel(): ActionResult {
        if (MayaAccessibilityService.instance == null) {
            return ActionResult(
                success = false,
                message = "Enable Maya Accessibility Service to expand notifications.",
                requiresPermission = true,
                permissionType = PermissionRequirement.ACCESSIBILITY
            )
        }
        val ok = MayaAccessibilityService.performNotifications()
        return ActionResult(
            success = ok,
            message = if (ok) "Opened Notifications shade" else "Failed to open Notifications shade"
        )
    }

    fun openWifiSettings(): ActionResult {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        return launchSettingsIntent(intent, "Wi-Fi Settings")
    }

    fun openInternetPanel(): ActionResult {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }
        return launchSettingsIntent(intent, "Internet & Network Panel")
    }

    fun openBluetoothSettings(): ActionResult =
        launchSettingsIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Bluetooth Settings")

    fun openVolumePanel(): ActionResult {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_VOLUME)
        } else {
            Intent(Settings.ACTION_SOUND_SETTINGS)
        }
        return launchSettingsIntent(intent, "Volume & Sound Panel")
    }

    fun openDisplaySettings(): ActionResult =
        launchSettingsIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS), "Display Settings")

    fun openBatterySettings(): ActionResult {
        val powerIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        val result = launchSettingsIntent(powerIntent, "Battery Usage Settings")
        return if (result.success) result else launchSettingsIntent(
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
            "Battery Saver Settings"
        )
    }

    fun openSystemSettings(): ActionResult =
        launchSettingsIntent(Intent(Settings.ACTION_SETTINGS), "Android Settings")

    fun openAccessibilitySettings(): ActionResult =
        launchSettingsIntent(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            "Accessibility Settings"
        )

    fun openWriteSettingsPermission(): ActionResult {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        return launchSettingsIntent(intent, "Modify System Settings")
    }

    private fun launchSettingsIntent(intent: Intent, label: String): ActionResult {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(success = true, message = "Opened $label")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open $label", e)
            ActionResult(
                success = false,
                message = "Unable to open $label on this device."
            )
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap? {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap.asImageBitmap()
        }
        val width = drawable.intrinsicWidth.coerceIn(48, 128)
        val height = drawable.intrinsicHeight.coerceIn(48, 128)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    companion object {
        private const val TAG = "DeviceControlManager"
    }
}
