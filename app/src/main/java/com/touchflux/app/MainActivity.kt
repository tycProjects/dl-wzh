package com.touchflux.app

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.touchflux.app.data.Prefs
import com.touchflux.app.fx.EffectType
import com.touchflux.app.fx.EffectView
import com.touchflux.app.fx.FxConfig
import com.touchflux.app.fx.Palette
import com.touchflux.app.service.FxState
import com.touchflux.app.service.TouchFluxAccessibilityService
import com.touchflux.app.service.TouchFluxWallpaperService
import java.util.Locale

/**
 * Single-screen UI, built in code so the project needs no AppCompat / Material dependency.
 */
class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var cfg: FxConfig
    private lateinit var playground: EffectView
    private lateinit var everywhereStatus: TextView

    private val effectChips = ArrayList<TextView>()
    private val swatches = ArrayList<View>()

    private val density: Float get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        cfg = prefs.config()
        setContentView(buildUi())
        playground.engine.config = cfg
        refreshSelection()
    }

    override fun onResume() {
        super.onResume()
        FxState.appInForeground = true
        updateEverywhereStatus()
    }

    override fun onPause() {
        FxState.appInForeground = false
        super.onPause()
    }

    // ------------------------------------------------------------------ UI

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setOnApplyWindowInsetsListener { v, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    v.setPadding(b.left, b.top, b.right, b.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    v.setPadding(
                        insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight, insets.systemWindowInsetBottom
                    )
                }
                insets
            }
        }

        // header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(14), dp(22), dp(10))
        }
        header.addView(TextView(this).apply {
            text = getString(R.string.app_name)
            setTextColor(TEXT)
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        })
        header.addView(TextView(this).apply {
            text = "A quiet layer of motion."
            setTextColor(MUTED)
            textSize = 13f
        })
        root.addView(header)

        // playground
        playground = EffectView(this).apply {
            showBackdrop = true
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dp(24).toFloat())
                }
            }
        }
        root.addView(
            playground,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.31f).toInt()
            ).apply { setMargins(dp(16), 0, dp(16), dp(6)) }
        )

        // settings
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(28))
        }

        content.addView(sectionLabel("Hiệu ứng"))
        content.addView(effectRow())

        content.addView(sectionLabel("Màu sắc"))
        content.addView(colorRow())

        content.addView(sectionLabel("Tinh chỉnh"))
        content.addView(
            card(
                slider(getString(R.string.size), 0.6f, 2.0f, cfg.size) { cfg.copy(size = it) },
                slider(getString(R.string.duration), 0.6f, 2.0f, cfg.duration) { cfg.copy(duration = it) },
                slider(getString(R.string.intensity), 0.5f, 2.0f, cfg.intensity) { cfg.copy(intensity = it) },
                toggle(getString(R.string.haptics), cfg.haptics) { cfg.copy(haptics = it) },
                toggle(getString(R.string.trails), cfg.trails) { cfg.copy(trails = it) },
                toggle("Hình học 3D", cfg.geometry) { cfg.copy(geometry = it) }
            )
        )

        content.addView(sectionLabel("Sử dụng toàn hệ thống"))
        everywhereStatus = TextView(this).apply {
            textSize = 13f
            setPadding(0, dp(6), 0, dp(12))
        }
        content.addView(
            card(
                TextView(this).apply {
                    text = "Hiệu ứng trên ứng dụng khác"
                    setTextColor(TEXT)
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                },
                TextView(this).apply {
                    text = "Hiển thị Nova khi chạm trên ứng dụng khác. Cần bật Trợ năng Android; thao tác chạm vẫn đi xuyên qua lớp hiệu ứng."
                    setTextColor(MUTED)
                    textSize = 13f
                    setPadding(0, dp(4), 0, 0)
                },
                everywhereStatus,
                button("Bật trong cài đặt Trợ năng", primary = true) { openAccessibilitySettings() }
            )
        )
        content.addView(spacer(12))
        content.addView(
            card(
                TextView(this).apply {
                    text = "Hình nền tương tác"
                    setTextColor(TEXT)
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                },
                TextView(this).apply {
                    text = "Lưới mảnh trắng trên nền đen. Chạm hoặc kéo để tạo vệt Nova."
                    setTextColor(MUTED)
                    textSize = 13f
                    setPadding(0, dp(4), 0, dp(12))
                },
                button("Đặt hình nền", primary = false) { openWallpaperPicker() }
            )
        )

        content.addView(TextView(this).apply {
            text = "Ngoại tuyến · Không quảng cáo · Không theo dõi"
            setTextColor(MUTED)
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(22), dp(8), 0)
        })

        root.addView(
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                addView(content)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        return root
    }

    private fun effectRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(2), 0, dp(2))
        }
        for (type in EffectType.values()) {
            val chip = TextView(this).apply {
                text = type.label
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(18), dp(11), dp(18), dp(11))
                setOnClickListener {
                    applyConfig(cfg.copy(effect = type), demo = true)
                }
            }
            effectChips.add(chip)
            row.addView(chip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { rightMargin = dp(8) })
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(row)
        }
    }

    private fun colorRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(4), 0, dp(4))
        }
        for (p in Palette.values()) {
            val dot = View(this).apply {
                contentDescription = p.label
                setOnClickListener { applyConfig(cfg.copy(palette = p), demo = true) }
            }
            swatches.add(dot)
            row.addView(dot, LinearLayout.LayoutParams(dp(42), dp(42)).apply { rightMargin = dp(12) })
        }
        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(row)
        }
    }

    private fun slider(
        title: String, lo: Float, hi: Float, value: Float, update: (Float) -> FxConfig
    ): View {
        val valueText = TextView(this).apply {
            setTextColor(ACCENT)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            text = format(value)
        }
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(TextView(this@MainActivity).apply {
                text = title
                setTextColor(TEXT)
                textSize = 14f
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(valueText)
        }
        val bar = SeekBar(this).apply {
            max = 100
            progress = (((value - lo) / (hi - lo)) * 100f).toInt()
            progressTintList = ColorStateList.valueOf(ACCENT)
            thumbTintList = ColorStateList.valueOf(ACCENT)
            progressBackgroundTintList = ColorStateList.valueOf(0x33FFFFFF)
        }
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val v = lo + (hi - lo) * progress / 100f
                valueText.text = format(v)
                if (fromUser) applyConfig(update(v), demo = false)
            }

            override fun onStartTrackingTouch(sb: SeekBar) = Unit

            override fun onStopTrackingTouch(sb: SeekBar) {
                playground.demo()
            }
        })
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
            addView(titleRow)
            addView(bar)
        }
    }

    private fun toggle(title: String, checked: Boolean, update: (Boolean) -> FxConfig): View {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        return Switch(this).apply {
            text = title
            setTextColor(TEXT)
            textSize = 14f
            isChecked = checked
            thumbTintList = ColorStateList(states, intArrayOf(ACCENT, 0xFF9AA3C4.toInt()))
            trackTintList = ColorStateList(states, intArrayOf(0x6622D3EE, 0x44FFFFFF))
            setPadding(0, dp(10), 0, dp(10))
            setOnCheckedChangeListener { _, on -> applyConfig(update(on), demo = false) }
        }
    }

    // ------------------------------------------------------------------ small view helpers

    private fun sectionLabel(text: String) = TextView(this).apply {
        this.text = text.uppercase(Locale.ROOT)
        setTextColor(MUTED)
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.12f
        setPadding(dp(6), dp(18), 0, dp(8))
    }

    private fun card(vararg children: View): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(CARD, 20f)
        setPadding(dp(18), dp(14), dp(18), dp(14))
        for (c in children) addView(c)
    }

    private fun button(label: String, primary: Boolean, onClick: () -> Unit): View = TextView(this).apply {
        text = label
        gravity = Gravity.CENTER
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(16), dp(14), dp(16), dp(14))
        if (primary) {
            setTextColor(0xFF080808.toInt())
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFFF4F4F4.toInt(), 0xFFBFC3CA.toInt())
            ).apply { cornerRadius = dp(16).toFloat() }
        } else {
            setTextColor(TEXT)
            background = rounded(0x1FFFFFFF, 16f)
        }
        setOnClickListener { onClick() }
    }

    private fun spacer(heightDp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    private fun rounded(color: Int, radiusDp: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun dp(v: Int): Int = (v * density + 0.5f).toInt()

    private fun dp(v: Float): Int = (v * density + 0.5f).toInt()

    private fun format(v: Float) = String.format(Locale.US, "%.1f×", v)

    // ------------------------------------------------------------------ state

    private fun applyConfig(newCfg: FxConfig, demo: Boolean) {
        cfg = newCfg
        prefs.save(cfg)
        playground.engine.config = cfg
        refreshSelection()
        if (demo) playground.demo()
    }

    private fun refreshSelection() {
        EffectType.values().forEachIndexed { i, type ->
            val chip = effectChips[i]
            val on = type == cfg.effect
            chip.setTextColor(if (on) 0xFF080808.toInt() else TEXT)
            chip.background = rounded(if (on) ACCENT else 0x1FFFFFFF, 18f)
        }
        Palette.values().forEachIndexed { i, p ->
            val d = if (p.rainbow) {
                GradientDrawable().apply {
                    gradientType = GradientDrawable.SWEEP_GRADIENT
                    colors = intArrayOf(
                        0xFFFF4D4D.toInt(), 0xFFFFD93D.toInt(), 0xFF3DDC97.toInt(),
                        0xFF4D8DFF.toInt(), 0xFFD04DFF.toInt(), 0xFFFF4D4D.toInt()
                    )
                }
            } else {
                GradientDrawable(GradientDrawable.Orientation.TL_BR, p.colors)
            }
            d.shape = GradientDrawable.OVAL
            d.setStroke(dp(3), if (p == cfg.palette) Color.WHITE else Color.TRANSPARENT)
            swatches[i].background = d
        }
    }

    private fun updateEverywhereStatus() {
        val on = isEverywhereEnabled()
        everywhereStatus.text = getString(if (on) R.string.status_on else R.string.status_off)
        everywhereStatus.setTextColor(if (on) 0xFF4ADE80.toInt() else 0xFFFBBF24.toInt())
    }

    private fun isEverywhereEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = ComponentName(this, TouchFluxAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    private fun openAccessibilitySettings() {
        Toast.makeText(this, R.string.toast_find_service, Toast.LENGTH_LONG).show()
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.toast_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWallpaperPicker() {
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, TouchFluxWallpaperService::class.java)
        )
        try {
            startActivity(direct)
        } catch (e: Exception) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
            } catch (e2: Exception) {
                Toast.makeText(this, R.string.toast_unavailable, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private companion object {
        const val BG = 0xFF050505.toInt()
        const val CARD = 0xFF111111.toInt()
        const val ACCENT = 0xFFF2F2F2.toInt()
        const val TEXT = 0xFFF2F2F2.toInt()
        const val MUTED = 0xFF858585.toInt()
    }
}
