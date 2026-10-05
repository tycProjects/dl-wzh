package com.example.smoothcamera.settings

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        buildUi()
    }

    override fun onResume() { super.onResume(); if (::content.isInitialized) rebuildContent() }

    private lateinit var content: LinearLayout

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK) }
        val header = TextView(this).apply { text = "Settings"; setTextColor(Color.WHITE); textSize = 28f; setPadding(24, 24, 24, 18) }
        root.addView(header)
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 0, 16, 32) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        rebuildContent()
    }

    private fun rebuildContent() {
        content.removeAllViews()
        addChoice("Video resolution", prefs.getString("resolution", "Auto") ?: "Auto", arrayOf("Auto", "4K", "1080p", "720p"), "resolution")
        addChoice("Video FPS", prefs.getString("fps", "Auto") ?: "Auto", arrayOf("Auto", "30", "60", "120"), "fps")
        addChoice("Video codec", "Auto", arrayOf("Auto"), null)
        addChoice("Video stabilization", if (prefs.getBoolean("stabilization", true)) "On when supported" else "Off", arrayOf("On when supported", "Off"), "stabilization")
        addChoice("Grid", if (prefs.getBoolean("grid", false)) "On" else "Off", arrayOf("On", "Off"), "grid")
        addChoice("Mirror front camera", if (prefs.getBoolean("mirror", true)) "On" else "Off", arrayOf("On", "Off"), "mirror")
        addChoice("HDR video", if (prefs.getBoolean("hdr", false)) "On when HLG is supported" else "Off", arrayOf("On when HLG is supported", "Off"), "hdr")
        addChoice("Camera timer", "Off", arrayOf("Off", "3 seconds", "10 seconds"), null)
        addChoice("Default camera mode", "Photo", arrayOf("Photo", "Video"), null)
        addChoice("Haptic feedback", "On", arrayOf("On", "Off"), null)
        addChoice("Sound", "System setting", arrayOf("System setting"), null)
        addChoice("Color profile", prefs.getString("profile", "NATURAL")!!.lowercase().replaceFirstChar { it.uppercase() }, arrayOf("Natural", "Vibrant", "Warm", "Cool", "Cinematic", "Soft", "Neutral"), "profile")
        addChoice("Storage", "MediaStore / Pictures & Movies / SmoothCamera", arrayOf("MediaStore / Pictures & Movies / SmoothCamera"), null)
    }

    private fun addChoice(title: String, value: String, options: Array<String>, key: String?) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 15, 18, 15); setBackgroundColor(0xFF171717.toInt()) }
        row.addView(TextView(this).apply { text = title; setTextColor(Color.WHITE); textSize = 16f })
        row.addView(TextView(this).apply { text = value; setTextColor(0xFFAAAAAA.toInt()); textSize = 13f; setPadding(0, 5, 0, 0) })
        row.setOnClickListener {
            if (options.size <= 1 || key == null) return@setOnClickListener
            AlertDialog.Builder(this).setTitle(title).setSingleChoiceItems(options, options.indexOf(value).coerceAtLeast(0)) { dialog, which ->
                val selected = options[which]
                when (key) {
                    "stabilization" -> prefs.edit().putBoolean(key, selected.startsWith("On")).apply()
                    "grid" -> prefs.edit().putBoolean(key, selected == "On").apply()
                    "mirror" -> prefs.edit().putBoolean(key, selected == "On").apply()
                    "hdr" -> prefs.edit().putBoolean(key, selected.startsWith("On")).apply()
                    "profile" -> prefs.edit().putString(key, selected.uppercase()).apply()
                    else -> prefs.edit().putString(key, selected).apply()
                }
                dialog.dismiss(); rebuildContent()
            }.show()
        }
        content.addView(row, ViewGroup.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(TextView(this).apply { setBackgroundColor(Color.BLACK) }, ViewGroup.LayoutParams(-1, 8))
    }
}
