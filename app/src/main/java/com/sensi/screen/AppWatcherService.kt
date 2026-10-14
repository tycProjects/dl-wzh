package com.sensi.screen

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * Theo dõi ứng dụng đang ở phía trước.
 * - App nằm trong danh sách chọn  -> áp dụng wm size ngay.
 * - App khác (màn hình chính, ...) -> khôi phục kích thước gốc sau một khoảng ngắn.
 */
class AppWatcherService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val resetRunnable = Runnable { ScreenController.reset(applicationContext) }

    // Các cửa sổ "tạm" không được coi là đổi ứng dụng
    private val ignored = setOf(
        "android",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller"
    )

    // Thời điểm vừa đổi kích thước. Đổi wm size làm app bị tạo lại (config change) và
    // phát ra hàng loạt sự kiện cửa sổ; trong lúc đó KHÔNG được khôi phục, nếu không
    // kích thước sẽ bị đổi qua đổi lại làm app bị tắt/crash.
    private var lastApplyAt = 0L
    private val graceMs = 2000L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Nếu lần trước bị tắt đột ngột khi đang chỉnh, trả màn hình về bình thường
        if (Prefs(this).dirty) ScreenController.reset(applicationContext, force = true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in ignored || pkg in imePackages()) return
        // Bỏ qua cửa sổ không phải Activity (dialog, popup, widget... có className "android.*")
        val cls = event.className?.toString().orEmpty()
        if (cls.startsWith("android.") || cls.startsWith("androidx.")) return

        val prefs = Prefs(this)
        if (!prefs.enabled) {
            scheduleReset(0)
            return
        }

        val profile = prefs.resolve(pkg)
        if (profile != null) {
            handler.removeCallbacks(resetRunnable)
            if (!ScreenController.isAppliedFor(pkg)) lastApplyAt = SystemClock.uptimeMillis()
            ScreenController.apply(applicationContext, pkg, profile)
        } else {
            scheduleReset(300)
        }
    }

    private fun scheduleReset(delayMs: Long) {
        handler.removeCallbacks(resetRunnable)
        val wait = lastApplyAt + graceMs - SystemClock.uptimeMillis()
        handler.postDelayed(resetRunnable, maxOf(delayMs, wait))
    }

    private fun imePackages(): Set<String> = try {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.enabledInputMethodList.map { it.packageName }.toSet()
    } catch (e: Exception) {
        emptySet()
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(resetRunnable)
        ScreenController.reset(applicationContext)
        return super.onUnbind(intent)
    }
}
