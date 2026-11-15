package com.zewmod.engine.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object PermissionHelper {

    /** Overlay (SYSTEM_ALERT_WINDOW) */
    fun hasOverlay(ctx: Context): Boolean =
        Settings.canDrawOverlays(ctx)

    fun requestOverlay(activity: Activity, requestCode: Int = 100) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivityForResult(intent, requestCode)
    }

    /** VPN */
    fun prepareVpn(activity: Activity, requestCode: Int = 101): Boolean {
        val intent = VpnService.prepare(activity) ?: return true   // already granted
        activity.startActivityForResult(intent, requestCode)
        return false
    }

    fun hasVpnPrepared(ctx: Context): Boolean =
        VpnService.prepare(ctx) == null

    /** Battery optimization */
    fun isBatteryOptimized(ctx: Context): Boolean {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    fun requestIgnoreBattery(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${ctx.packageName}")
            }
            ctx.startActivity(intent)
        }
    }

    /** POST_NOTIFICATIONS (Android 13+) */
    fun needsNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Check all 3 required permissions */
    fun allGranted(ctx: Context): Boolean =
        hasOverlay(ctx) && hasVpnPrepared(ctx) && !isBatteryOptimized(ctx)

    data class PermissionState(
        val overlay: Boolean,
        val vpn: Boolean,
        val battery: Boolean
    ) {
        val allOk: Boolean get() = overlay && vpn && battery
    }

    fun getState(ctx: Context) = PermissionState(
        overlay  = hasOverlay(ctx),
        vpn      = hasVpnPrepared(ctx),
        battery  = !isBatteryOptimized(ctx)
    )
}
