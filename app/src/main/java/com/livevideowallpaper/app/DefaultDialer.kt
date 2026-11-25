package com.livevideowallpaper.app

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager

object DefaultDialer {
    fun isDefault(c: Context): Boolean =
        c.getSystemService(TelecomManager::class.java)?.defaultDialerPackage == c.packageName

    /** Intent that asks the user to make this app the default phone app, or null if impossible. */
    fun requestIntent(c: Context): Intent? {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = c.getSystemService(RoleManager::class.java)
            return if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) rm.createRequestRoleIntent(RoleManager.ROLE_DIALER) else null
        }
        return Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, c.packageName)
    }
}
