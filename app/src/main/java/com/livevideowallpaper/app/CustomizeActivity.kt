package com.livevideowallpaper.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import java.io.File

/** Upload photos/videos for the dialer, call screen and lock screen; lock-video and default-dialer setup. */
class CustomizeActivity : BaseActivity() {

    private var pendingSlot = Prefs.SLOT_DIALER
    private val labels = HashMap<String, TextView>()
    private lateinit var swLock: MaterialSwitch

    private val pickMedia = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        val slot = pendingSlot
        Bg.io.execute {
            val f = BgRepository.import(this, uri, slot)
            runOnUiThread {
                say(if (f != null) "Background saved" else "Could not use this file")
                refresh()
            }
        }
    }
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customize)
        setupToolbar(R.string.customize)

        val rows = findViewById<LinearLayout>(R.id.rows)
        val items = listOf(
            Prefs.SLOT_DIALER to "Dialer background (keypad, contacts)",
            Prefs.SLOT_CALL to "Incoming / in-call screen background",
            Prefs.SLOT_LOCK to "Lock-screen video"
        )
        for ((slot, title) in items) {
            val row = layoutInflater.inflate(R.layout.item_bg_row, rows, false)
            row.findViewById<TextView>(R.id.rowTitle).text = title
            labels[slot] = row.findViewById(R.id.rowCurrent)
            row.findViewById<Button>(R.id.btnUpload).setOnClickListener {
                pendingSlot = slot
                pickMedia.launch(arrayOf("image/*", "video/*"))
            }
            row.findViewById<Button>(R.id.btnMyVideos).setOnClickListener { chooseFromVideos(slot) }
            row.findViewById<Button>(R.id.btnClear).setOnClickListener {
                BgRepository.clear(this, slot)
                refresh()
            }
            rows.addView(row)
        }

        swLock = findViewById(R.id.swLock)
        swLock.isChecked = Prefs.lockEnabled(this) && Settings.canDrawOverlays(this)
        swLock.setOnCheckedChangeListener { _, on ->
            if (on) {
                if (!Settings.canDrawOverlays(this)) {
                    say("Turn on \"Display over other apps\" for this app, then come back.")
                    swLock.isChecked = false
                    openOverlaySettings()
                } else {
                    Prefs.setLockEnabled(this, true)
                    askNotificationPermission()
                    LockVideoService.start(this)
                }
            } else {
                Prefs.setLockEnabled(this, false)
                LockVideoService.stop(this)
            }
        }
        findViewById<Button>(R.id.btnOverlay).setOnClickListener { openOverlaySettings() }
        findViewById<Button>(R.id.btnAppInfo).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        findViewById<Button>(R.id.btnDefaultPhone).setOnClickListener {
            DefaultDialer.requestIntent(this)?.let { roleLauncher.launch(it) } ?: say("Not available on this phone")
        }

        findViewById<Button>(R.id.btnStatusStyle).setOnClickListener {
            startActivity(Intent(this, StatusStyleActivity::class.java))
        }

        val rbGradient = findViewById<RadioButton>(R.id.rbGradient)
        val rbGlass = findViewById<RadioButton>(R.id.rbGlass)
        if (Prefs.btnStyle(this) == 1) rbGlass.isChecked = true else rbGradient.isChecked = true
        rbGradient.setOnClickListener { Prefs.setBtnStyle(this, 0) }
        rbGlass.setOnClickListener { Prefs.setBtnStyle(this, 1) }
    }

    override fun onResume() {
        super.onResume()
        refresh()
        if (Prefs.lockEnabled(this) && Settings.canDrawOverlays(this)) LockVideoService.start(this)
    }

    private fun openOverlaySettings() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun refresh() {
        for ((slot, tv) in labels) {
            val p = Prefs.bgPath(this, slot)
            tv.text = if (p != null && File(p).isFile) File(p).name
            else if (slot == Prefs.SLOT_LOCK) "Default: your wallpaper video"
            else "Default: dark blue gradient"
        }
    }

    private fun chooseFromVideos(slot: String) {
        Bg.io.execute {
            val list = VideoRepository.list(this)
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (list.isEmpty()) {
                    say("No videos imported yet. Import some from the home screen first.")
                    return@runOnUiThread
                }
                AlertDialog.Builder(this)
                    .setTitle("Pick a video")
                    .setItems(list.map { it.name }.toTypedArray()) { _, i ->
                        BgRepository.clear(this, slot)
                        Prefs.setBgPath(this, slot, list[i].file.absolutePath)
                        refresh()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }
}
