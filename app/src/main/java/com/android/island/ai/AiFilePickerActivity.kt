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
 * Proxy activity for the system file picker. The island overlay has no Activity
 * context, so picking needs a real activity that:
 *  1. Opens the system picker (GET_CONTENT — most stable from overlays)
 *  2. Copies the chosen URI into app memory **before** finishing (avoids grant revoke)
 *  3. Never crashes the host process on cancel / bad URI / OEM quirks
 */
class AiFilePickerActivity : Activity() {

    private var launchedPicker = false
    private var finishedCleanly = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IslandBus.setAiTyping(true)
        if (savedInstanceState != null) {
            launchedPicker = true
            return
        }
        try {
            if (!launchPicker()) {
                toast("Tidak bisa membuka pemilih file.")
                done(keepTyping = false)
            }
        } catch (t: Throwable) {
            toast("Gagal membuka pemilih: ${t.message ?: "error"}")
            done(keepTyping = false)
        }
    }

    private fun launchPicker(): Boolean {
        // Prefer plain GET_CONTENT without createChooser — chooser has crashed on some OEMs
        // when started from an accessibility-triggered activity.
        val intents = listOf(
            Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
        for (intent in intents) {
            try {
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQ_PICK)
                launchedPicker = true
                return true
            } catch (_: Throwable) {
                // try next
            }
        }
        return false
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_PICK) return
        if (resultCode != RESULT_OK || data?.data == null) {
            done(keepTyping = true) // user cancelled — keep AI panel open
            return
        }
        val uri = data.data!!
        val appCtx = applicationContext
        // Read while this activity (and its temporary URI grant) is still alive.
        CoroutineScope(Dispatchers.Main.immediate).launch {
            val hint = try {
                withContext(Dispatchers.IO) {
                    // Optional persistable grant — never required for success.
                    runCatching {
                        contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                    AiSession.stage(appCtx, uri)
                }
            } catch (t: Throwable) {
                "Gagal membaca file: ${t.message ?: "kesalahan tidak diketahui"}"
            }
            if (hint != null) toast(hint) else toast("File dilampirkan.")
            done(keepTyping = true)
        }
    }

    private fun done(keepTyping: Boolean) {
        finishedCleanly = true
        IslandBus.setAiTyping(keepTyping)
        try {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        } catch (_: Throwable) {
        }
    }

    private fun toast(msg: String) {
        runCatching { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        if (launchedPicker && !finishedCleanly && !isChangingConfigurations) {
            IslandBus.setAiTyping(true) // keep panel; do not collapse on process quirks
        }
        IslandBus.endExternalAi()
        super.onDestroy()
    }

    companion object {
        private const val REQ_PICK = 4711
    }
}
