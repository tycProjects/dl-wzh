package com.teahack.gamehub

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private var crosshairEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val background = Color.rgb(14, 18, 31)
        val panel = Color.rgb(27, 34, 53)
        val accent = Color.rgb(0, 220, 170)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(background)
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "TEAHACK"
            textSize = 32f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "GAME HUB  •  CUSTOM SETTINGS"
            textSize = 12f
            letterSpacing = 0.12f
            setTextColor(accent)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "Ready. Choose a setting below."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(12))
        }

        content.addView(title, matchWrap())
        content.addView(subtitle, matchWrap())
        content.addView(status, matchWrap())

        addAction(content, "CROSSHAIR PREVIEW", panel, accent) {
            crosshairEnabled = !crosshairEnabled
            status.text = if (crosshairEnabled)
                "Crosshair preview enabled in this app."
            else "Crosshair preview disabled."
        }
        addAction(content, "SENSITIVITY SETTINGS", panel, accent) {
            status.text = "Sensitivity preset: Balanced. Game controls are not modified."
        }
        addAction(content, "GRAPHICS PRESETS", panel, accent) {
            status.text = "Graphics preset selected: Smooth + High FPS (display only)."
        }
        addAction(content, "PRACTICE MODE", panel, accent) {
            status.text = "Practice mode is a placeholder for your own game project."
        }
        addAction(content, "ABOUT", panel, accent) {
            status.text = "TeaHack Game Hub v1.0 • Independent companion app."
        }

        val footer = TextView(this).apply {
            text = "\nThis app does not modify Free Fire or interact with online matches."
            textSize = 11f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        content.addView(footer, matchWrap())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun addAction(
        parent: LinearLayout,
        label: String,
        panelColor: Int,
        accent: Int,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(panelColor)
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), accent)
            }
            setOnClickListener { action() }
        }
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(58)
        ).apply {
            topMargin = dp(9)
        }
        parent.addView(button, params)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
