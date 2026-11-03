package com.ahnaf.ff

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    // Reference suggestions only. Apply them manually in game settings.
    private val presets = mapOf(
        "HIGH" to listOf("General" to 95, "Red Dot" to 90, "2x Scope" to 85, "4x Scope" to 80, "AWM" to 50, "Free Look" to 70),
        "MEDIUM" to listOf("General" to 85, "Red Dot" to 80, "2x Scope" to 75, "4x Scope" to 70, "AWM" to 45, "Free Look" to 60),
        "LOW" to listOf("General" to 70, "Red Dot" to 65, "2x Scope" to 60, "4x Scope" to 55, "AWM" to 40, "Free Look" to 50)
    )

    private lateinit var presetText: TextView
    private lateinit var statsText: TextView
    private lateinit var swCross: Switch
    private lateinit var swStats: Switch
    private val handler = Handler(Looper.getMainLooper())

    private val refresh = object : Runnable {
        override fun run() {
            statsText.text = StatsReader.read(this@MainActivity)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        handler.post(refresh)
    }

    override fun onPause() {
        handler.removeCallbacks(refresh)
        super.onPause()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun label(text: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(32), dp(20), dp(24))
            setBackgroundColor(Color.parseColor("#0E0E12"))
        }

        root.addView(label("AHNAF FF", 30f, Color.parseColor("#FF3B3B")).apply { gravity = Gravity.CENTER })
        root.addView(label("Gaming Assistant Panel", 13f, Color.GRAY).apply {
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(20))
        })

        // Sensitivity
        root.addView(label("Sensitivity Suggestions", 16f))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        presets.keys.forEach { name ->
            row.addView(Button(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { showPreset(name) }
            })
        }
        root.addView(row)
        presetText = label("Select a preset", 14f, Color.LTGRAY).apply { setPadding(0, dp(8), 0, dp(20)) }
        root.addView(presetText)

        // Switches
        swCross = Switch(this).apply {
            text = "Crosshair Overlay"
            setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _, _ -> applyService() }
        }
        swStats = Switch(this).apply {
            text = "Performance Monitor Overlay"
            setTextColor(Color.WHITE)
            setOnCheckedChangeListener { _, _ -> applyService() }
        }
        root.addView(swCross)
        root.addView(swStats)

        // Live stats
        root.addView(label("Device Stats", 16f).apply { setPadding(0, dp(24), 0, dp(6)) })
        statsText = label("", 14f, Color.GREEN)
        root.addView(statsText)

        return ScrollView(this).apply { addView(root) }
    }

    private fun showPreset(name: String) {
        presetText.text = presets[name]!!.joinToString("\n") { (k, v) -> "$k : $v" }
    }

    private fun applyService() {
        val wantCross = swCross.isChecked
        val wantStats = swStats.isChecked
        val intent = Intent(this, OverlayService::class.java)

        if (!wantCross && !wantStats) {
            stopService(intent)
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            swCross.isChecked = false
            swStats.isChecked = false
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        intent.putExtra(OverlayService.EXTRA_CROSSHAIR, wantCross)
        intent.putExtra(OverlayService.EXTRA_STATS, wantStats)
        startForegroundService(intent)
    }
}
