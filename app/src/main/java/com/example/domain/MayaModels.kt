package com.example.domain

import androidx.compose.ui.graphics.ImageBitmap

enum class AppDestination(val route: String, val label: String, val labelBn: String) {
    MAYA("maya", "Maya", "মায়া"),
    CONTROLS("controls", "Controls", "কন্ট্রোল"),
    AUTOMATE("automate", "Automate", "অটোমেশন"),
    DEVICE("device", "Device", "ডিভাইস")
}

enum class MayaEmotion(val labelBn: String, val key: String) {
    AFFECTIONATE("স্নেহময়ী", "affectionate"),
    HAPPY("আনন্দিত", "happy"),
    CARING("যত্নশীল", "caring"),
    PLAYFUL("দুষ্টুমিষ্টি", "playful"),
    CALM("শান্ত", "calm"),
    CONCERNED("চিন্তিত", "concerned");

    companion object {
        fun fromKey(raw: String?): MayaEmotion =
            entries.firstOrNull { it.key.equals(raw, ignoreCase = true) } ?: AFFECTIONATE
    }
}

enum class MayaCommandAction {
    NONE,
    OPEN_APP,
    GO_HOME,
    OPEN_RECENTS,
    OPEN_BACK,
    OPEN_QUICK_SETTINGS,
    OPEN_NOTIFICATIONS,
    OPEN_WIFI_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_SYSTEM_SETTINGS,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    FLASHLIGHT_TOGGLE,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    VOLUME_UNMUTE,
    BRIGHTNESS_UP,
    BRIGHTNESS_DOWN,
    BRIGHTNESS_SET,
    SCROLL_UP,
    SCROLL_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    TAP_COORDINATE,
    LONG_PRESS_COORDINATE,
    SHOW_BATTERY_INFO,
    SHOW_STORAGE_INFO,
    SHOW_NETWORK_INFO,
    SHOW_DEVICE_INFO,
    RUN_AUTOMATION_SEQUENCE,
    STOP_AUTOMATION;

    companion object {
        fun fromString(raw: String?): MayaCommandAction =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NONE
    }
}

data class MayaAiParsedResponse(
    val replyBn: String,
    val emotion: MayaEmotion,
    val action: MayaCommandAction,
    val targetAppName: String? = null,
    val brightnessPercent: Int? = null,
    val tapX: Int? = null,
    val tapY: Int? = null,
    val sequenceName: String? = null
)

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val isFromMaya: Boolean,
    val text: String,
    val emotion: MayaEmotion = MayaEmotion.AFFECTIONATE,
    val timestampMillis: Long = System.currentTimeMillis(),
    val actionSummary: String? = null,
    val actionSuccess: Boolean? = null
)

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap? = null
)

enum class AudioStreamType(val label: String, val labelBn: String, val androidStream: Int) {
    MEDIA("Media", "মিডিয়া", android.media.AudioManager.STREAM_MUSIC),
    RING("Ring", "রিংটোন", android.media.AudioManager.STREAM_RING),
    NOTIFICATION("Notification", "নোটিফিকেশন", android.media.AudioManager.STREAM_NOTIFICATION),
    ALARM("Alarm", "অ্যালার্ম", android.media.AudioManager.STREAM_ALARM),
    VOICE_CALL("Call", "কল", android.media.AudioManager.STREAM_VOICE_CALL)
}

data class StreamVolumeState(
    val streamType: AudioStreamType,
    val current: Int,
    val max: Int,
    val isMuted: Boolean
)

data class AutomationFeatureFlags(
    val tapAndLongPressEnabled: Boolean = true,
    val autoScrollEnabled: Boolean = true,
    val autoSwipeEnabled: Boolean = true,
    val appLaunchEnabled: Boolean = true,
    val sequenceExecutionEnabled: Boolean = true,
    val mayaAutoSpeakEnabled: Boolean = true
)

enum class AutomationStepType(val label: String) {
    TAP("Tap (X, Y)"),
    LONG_PRESS("Long Press (X, Y)"),
    SCROLL_DOWN("Scroll Down"),
    SCROLL_UP("Scroll Up"),
    SWIPE_LEFT("Swipe Left"),
    SWIPE_RIGHT("Swipe Right"),
    OPEN_APP("Open App"),
    GO_HOME("Go Home"),
    RECENTS("Open Recents"),
    WAIT("Wait (ms)")
}

data class AutomationStep(
    val type: AutomationStepType,
    val param1: String = "",
    val param2: String = ""
)

data class AutomationSequence(
    val id: Long = 0L,
    val name: String,
    val description: String,
    val steps: List<AutomationStep>
)

data class ActionResult(
    val success: Boolean,
    val message: String,
    val requiresPermission: Boolean = false,
    val permissionType: PermissionRequirement? = null
)

enum class PermissionRequirement {
    ACCESSIBILITY,
    WRITE_SETTINGS,
    RECORD_AUDIO
}

data class DeviceTelemetrySnapshot(
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val batteryStatusText: String = "Unknown",
    val chargingSourceText: String = "Battery",
    val batteryTemperatureC: Float? = null,
    val batteryHealthText: String = "Unknown",
    val batteryVoltageMv: Int = 0,
    val totalStorageBytes: Long = 0L,
    val availableStorageBytes: Long = 0L,
    val usedStorageBytes: Long = 0L,
    val totalRamBytes: Long = 0L,
    val availableRamBytes: Long = 0L,
    val usedRamBytes: Long = 0L,
    val isLowRam: Boolean = false,
    val isWifiConnected: Boolean = false,
    val wifiSsid: String? = null,
    val wifiLinkSpeedMbps: Int? = null,
    val isMobileConnected: Boolean = false,
    val networkTypeText: String = "Offline",
    val isInternetValidated: Boolean = false,
    val deviceBrand: String = "",
    val deviceModel: String = "",
    val deviceManufacturer: String = "",
    val androidVersion: String = "",
    val sdkInt: Int = 0,
    val securityPatch: String = "",
    val flashlightAvailable: Boolean = false,
    val isFlashlightOn: Boolean = false,
    val brightnessPercent: Int = 50,
    val isAutoBrightness: Boolean = false,
    val streamVolumes: List<StreamVolumeState> = emptyList(),
    val isAccessibilityEnabled: Boolean = false,
    val canWriteSettings: Boolean = false,
    val hasRecordAudioPermission: Boolean = false
)
