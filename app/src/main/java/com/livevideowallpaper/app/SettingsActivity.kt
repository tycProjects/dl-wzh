package com.livevideowallpaper.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.format.Formatter
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : BaseActivity() {

    private lateinit var txtStorage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupToolbar(R.string.settings_title)

        val loop = findViewById<MaterialSwitch>(R.id.swLoop)
        loop.isChecked = Prefs.loop(this)
        loop.setOnCheckedChangeListener { _, v -> Prefs.setLoop(this, v) }

        val saver = findViewById<MaterialSwitch>(R.id.swSaver)
        saver.isChecked = Prefs.pauseInSaver(this)
        saver.setOnCheckedChangeListener { _, v -> Prefs.setPauseInSaver(this, v) }

        val fill = findViewById<RadioButton>(R.id.rbFill)
        val fit = findViewById<RadioButton>(R.id.rbFit)
        if (Prefs.fit(this)) fit.isChecked = true else fill.isChecked = true
        fill.setOnClickListener { Prefs.setFit(this, false) }
        fit.setOnClickListener { Prefs.setFit(this, true) }

        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e: ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        txtStorage = findViewById(R.id.txtStorage)
        findViewById<Button>(R.id.btnDeleteAll).setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.delete_all_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    Bg.io.execute { VideoRepository.deleteAll(this); runOnUiThread { updateStorage() } }
                }.show()
        }

        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" }
        findViewById<TextView>(R.id.txtVersion).text = "Version $version"
    }

    override fun onResume() {
        super.onResume()
        updateStorage()
    }

    private fun updateStorage() {
        Bg.io.execute {
            val size = VideoRepository.totalSize(this)
            val count = VideoRepository.dir(this).listFiles()?.size ?: 0
            runOnUiThread {
                if (!isFinishing && !isDestroyed)
                    txtStorage.text = "${Formatter.formatFileSize(this, size)} used by $count video(s)"
            }
        }
    }
}
