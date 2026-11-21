package com.android.island.ai

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.android.island.IslandApplication
import com.android.island.core.IslandBus
import com.android.island.service.IslandAccessibilityService
import com.android.island.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Transparent one-shot entry point used to summon the AI island.
 *
 * This is the target of the "tekan ganda tombol power" trigger: on One UI set
 * Settings > Advanced features > Side button > Double press > Open app > "AI Island".
 * It is also a regular launcher alias entry in the app drawer.
 *
 * Deliberately NOT a VoiceInteractionSession/RecognitionService: the keyboard chat panel
 * is the interface (voice assistant integration is intentionally deferred).
 */
class AiLauncherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as? IslandApplication
        val aiEnabled = app?.let { readAiEnabledBlocking(it) } ?: false

        when {
            !IslandAccessibilityService.isRunning -> {
                Toast.makeText(
                    this,
                    "Aktifkan Accessibility \u201CAndroid Island\u201D dulu agar kapsul bisa muncul.",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            !aiEnabled -> {
                Toast.makeText(
                    this,
                    "Aktifkan dulu \u201CAsisten AI\u201D di pengaturan Android Island.",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            else -> {
                // Expand the island straight into the AI chat card.
                IslandBus.pushAi()
            }
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    /** Short bounded read of the DataStore-backed setting; fine for a one-shot toggle decision. */
    private fun readAiEnabledBlocking(app: IslandApplication): Boolean {
        var enabled = false
        val latch = CountDownLatch(1)
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                enabled = app.settingsRepository.settings.firstOrNull()?.aiEnabled ?: false
            }
            latch.countDown()
        }
        latch.await(2, TimeUnit.SECONDS)
        return enabled
    }
}
