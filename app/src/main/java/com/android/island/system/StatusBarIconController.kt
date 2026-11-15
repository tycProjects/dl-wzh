package com.android.island.system

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass
import com.android.island.util.IslandDiagnostics
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/** Shizuku status-bar control aligned with Expressive Cutout 0.3.1-beta. */
object StatusBarIconController {
    private const val TAG = "AndroidIslandStatusBar"
    // IStatusBarService.DISABLE_NOTIFICATION_ICONS (framework bitmask).
    // Do not use 1 shl 1: that is NOT the notification-icons flag.
    private const val DISABLE_NONE = 0x00000000
    private const val DISABLE_NOTIFICATION_ICONS = 0x00020000

    private val token = Binder()
    @Volatile private var service: Any? = null
    @Volatile private var lastApplied: Boolean? = null

    @Synchronized
    fun onShizukuUnavailable() {
        service = null
        lastApplied = null
    }

    @Synchronized
    fun apply(enabled: Boolean, packageName: String): Boolean {
        if (ShizukuState.status.value != ShizukuStatus.READY) {
            service = null
            lastApplied = null
            IslandDiagnostics.record("SHIZUKU_ICON_SKIPPED", "enabled=$enabled status=${ShizukuState.status.value}")
            return false
        }
        if (lastApplied == enabled && service != null) return true
        return runCatching {
            HiddenApiBypass.addHiddenApiExemptions("")
            val statusBar = service ?: buildService().also { service = it }
            val flags = if (enabled) DISABLE_NOTIFICATION_ICONS else DISABLE_NONE
            statusBar.disable(flags, packageName)
            lastApplied = enabled
            IslandDiagnostics.record(
                "SHIZUKU_ICON_APPLIED",
                "notificationIcons=$enabled flag=$flags systemInfoPreserved=true clockPreserved=true nowBar=notification-policy user=${android.os.Process.myUid() / 100000}"
            )
            true
        }.getOrElse { error ->
            IslandDiagnostics.record("SHIZUKU_ICON_FAILED", "${error.javaClass.simpleName}: ${error.message}")
            Log.w(TAG, "Unable to update notification icon visibility", error)
            service = null
            lastApplied = null
            false
        }
    }

    private fun buildService(): Any {
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("statusbar"))
        return Class.forName("com.android.internal.statusbar.IStatusBarService\$Stub")
            .getMethod("asInterface", IBinder::class.java)
            .invoke(null, binder) ?: error("IStatusBarService.asInterface returned null")
    }

    private fun Any.disable(flags: Int, packageName: String) {
        val method = runCatching {
            javaClass.getMethod(
                "disable", Int::class.javaPrimitiveType,
                IBinder::class.java, String::class.java
            )
        }.getOrNull()
        if (method != null) {
            method.invoke(this, flags, token, packageName)
            IslandDiagnostics.record("SHIZUKU_API", "used disable")
            return
        }
        javaClass.getMethod(
            "disableForUser", Int::class.javaPrimitiveType,
            IBinder::class.java, String::class.java, Int::class.javaPrimitiveType
        ).invoke(this, flags, token, packageName, android.os.Process.myUid() / 100000)
        IslandDiagnostics.record("SHIZUKU_API", "used disableForUser")
    }
}
