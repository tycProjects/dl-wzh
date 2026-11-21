package com.android.island.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.widget.Toast
import com.android.island.core.IslandBus
import com.android.island.service.IslandAccessibilityService
import com.android.island.ui.MainActivity

/**
 * Entry point when the user triggers the system digital assistant
 * (side-key long press on One UI, gesture, or assist button) while AI Island
 * is the selected assistant.
 *
 * Shows the AI island card and immediately hides this session's own window so
 * only the island UI is visible.
 */
class AiAssistSession(private val appContext: Context) : VoiceInteractionSession(appContext) {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        // Proof the long-press / side-key assist reaches this app (diagnostics only; off by default).
        com.android.island.util.IslandDiagnostics.record(
            "ASSIST_SESSION_SHOW",
            "flags=$showFlags accessibility=${IslandAccessibilityService.isRunning}"
        )
        try {
            if (!IslandAccessibilityService.isRunning) {
                Toast.makeText(
                    appContext,
                    "Aktifkan Accessibility “Android Island” dulu agar kapsul bisa muncul.",
                    Toast.LENGTH_LONG
                ).show()
                appContext.startActivity(
                    Intent(appContext, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                hide()
            } else {
                IslandBus.pushAi()
                // Delay hide so the island can measure adaptive width/height before the
                // system assist window tears down (immediate hide caused a flattened layout).
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    runCatching { hide() }
                }, 280)
            }
        } catch (e: Exception) {
            Toast.makeText(
                appContext,
                "Gagal membuka AI Island: ${e.message ?: "error"}",
                Toast.LENGTH_SHORT
            ).show()
            hide()
        }
    }

    override fun onHide() {
        super.onHide()
    }
}
