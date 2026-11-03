package com.personal.snapstreak

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var statusView: TextView
    private lateinit var gapView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        val title = TextView(this).apply {
            text = "SnapStreak"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        }

        val help = TextView(this).apply {
            text = "1. Turn on the \"SnapStreak tapper\" accessibility service.\n" +
                "2. In Developer options, switch OFF Pointer location and Show taps.\n" +
                "3. Press Start and keep your hands off the phone. It loops until you stop it.\n" +
                "Stop: the red STOP button on screen (drag it anywhere), Volume Down, or Stop below."
            textSize = 15f
        }

        statusView = TextView(this).apply {
            text = Status.last
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
        }

        // Camera gap: too small and Snapchat drops photos, too big and it is slow.
        gapView = TextView(this).apply { textSize = 16f }
        val gapMinus = Button(this).apply {
            text = "- 100"
            setOnClickListener { changeGap(-100L) }
        }
        val gapPlus = Button(this).apply {
            text = "+ 100"
            setOnClickListener { changeGap(100L) }
        }
        val gapRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(gapView, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(gapMinus)
            addView(gapPlus)
        }
        val gapHelp = TextView(this).apply {
            text = "Fewer than 16 photos? Press + 100. Getting all 16? Try - 100 to go faster."
            textSize = 13f
        }

        val openSettings = Button(this).apply {
            text = "Open accessibility settings"
            setOnClickListener { openAccessibilitySettings() }
        }
        val start = Button(this).apply {
            text = "Start (launch Snapchat + loop)"
            setOnClickListener { startBot() }
        }
        val stop = Button(this).apply {
            text = "Stop"
            setOnClickListener { TapService.instance?.stopRun("Stopped.") }
        }

        root.addRow(title, 0)
        root.addRow(help, 12)
        root.addRow(statusView, 20)
        root.addRow(gapRow, 20)
        root.addRow(gapHelp, 2)
        root.addRow(openSettings, 20)
        root.addRow(start, 8)
        root.addRow(stop, 8)

        setContentView(root)
        refreshGap()
    }

    override fun onResume() {
        super.onResume()
        Status.listener = { text -> statusView.text = text }
        statusView.text = Status.last
        refreshGap()
    }

    override fun onPause() {
        Status.listener = null
        super.onPause()
    }

    private fun changeGap(delta: Long) {
        Prefs.setCameraGapMs(this, Prefs.cameraGapMs(this) + delta)
        refreshGap()
    }

    private fun refreshGap() {
        gapView.text = "Gap between camera taps: ${Prefs.cameraGapMs(this)} ms"
    }

    private fun startBot() {
        val service = TapService.instance
        if (service == null) {
            Toast.makeText(
                this,
                "Turn on the SnapStreak accessibility service first.",
                Toast.LENGTH_LONG
            ).show()
            openAccessibilitySettings()
            return
        }
        if (service.isRunning()) {
            Status.post("Already running.")
            return
        }

        // Step 1: launch Snapchat (fresh, so it opens on the camera)
        val launch = packageManager.getLaunchIntentForPackage(Steps.SNAPCHAT_PACKAGE)
        if (launch == null) {
            Status.post("Snapchat isn't installed.")
            return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(launch)

        // Then the looping steps
        service.startRun(Steps.LAUNCH_SETTLE_MS)
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun LinearLayout.addRow(view: View, topMarginDp: Int) {
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = (topMarginDp * resources.displayMetrics.density).toInt()
        addView(view, lp)
    }
}
