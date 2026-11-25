package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ComponentName
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

class MayaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        emergencyStopped.set(false)
        Log.d(TAG, "MayaAccessibilityService connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()
        if (!pkg.isNullOrBlank()) {
            _lastForegroundPackage.value = pkg
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "MayaAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isServiceConnected.value = false
    }

    private fun getScreenSize(): Pair<Float, Float> {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width().toFloat() to bounds.height().toFloat()
        } else {
            @Suppress("DEPRECATION")
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels.toFloat() to metrics.heightPixels.toFloat()
        }
    }

    private fun dispatchPathGesture(
        path: Path,
        durationMs: Long,
        onComplete: (Boolean) -> Unit
    ) {
        if (emergencyStopped.get()) {
            onComplete(false)
            return
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    onComplete(!emergencyStopped.get())
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    onComplete(false)
                }
            },
            null
        )
        if (!dispatched) {
            onComplete(false)
        }
    }

    fun tapAt(x: Float, y: Float, onComplete: (Boolean) -> Unit) {
        val (w, h) = getScreenSize()
        val safeX = x.coerceIn(1f, (w - 1f).coerceAtLeast(1f))
        val safeY = y.coerceIn(1f, (h - 1f).coerceAtLeast(1f))
        val path = Path().apply { moveTo(safeX, safeY) }
        dispatchPathGesture(path, 80L, onComplete)
    }

    fun longPressAt(x: Float, y: Float, onComplete: (Boolean) -> Unit) {
        val (w, h) = getScreenSize()
        val safeX = x.coerceIn(1f, (w - 1f).coerceAtLeast(1f))
        val safeY = y.coerceIn(1f, (h - 1f).coerceAtLeast(1f))
        val path = Path().apply { moveTo(safeX, safeY) }
        dispatchPathGesture(path, 900L, onComplete)
    }

    fun scroll(down: Boolean, onComplete: (Boolean) -> Unit) {
        val (w, h) = getScreenSize()
        val centerX = w * 0.5f
        val startY = if (down) h * 0.72f else h * 0.30f
        val endY = if (down) h * 0.28f else h * 0.74f
        val path = Path().apply {
            moveTo(centerX, startY)
            lineTo(centerX, endY)
        }
        dispatchPathGesture(path, 380L, onComplete)
    }

    fun swipe(left: Boolean, onComplete: (Boolean) -> Unit) {
        val (w, h) = getScreenSize()
        val centerY = h * 0.52f
        val startX = if (left) w * 0.82f else w * 0.18f
        val endX = if (left) w * 0.18f else w * 0.82f
        val path = Path().apply {
            moveTo(startX, centerY)
            lineTo(endX, centerY)
        }
        dispatchPathGesture(path, 320L, onComplete)
    }

    companion object {
        private const val TAG = "MayaAccessibility"

        @Volatile
        var instance: MayaAccessibilityService? = null
            private set

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _lastForegroundPackage = MutableStateFlow<String?>(null)
        val lastForegroundPackage: StateFlow<String?> = _lastForegroundPackage.asStateFlow()

        val emergencyStopped = AtomicBoolean(false)

        fun isAccessibilitySettingEnabled(context: Context): Boolean {
            if (instance != null) return true
            val expectedComponent = ComponentName(context, MayaAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(':')
                .mapNotNull { ComponentName.unflattenFromString(it) }
                .any { it == expectedComponent }
        }

        fun resetEmergencyStop() {
            emergencyStopped.set(false)
        }

        fun triggerEmergencyStop() {
            emergencyStopped.set(true)
        }

        fun performHome(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false

        fun performRecents(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false

        fun performBack(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false

        fun performNotifications(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false

        fun performQuickSettings(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) ?: false
    }
}
