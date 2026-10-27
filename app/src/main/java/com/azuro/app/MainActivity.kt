package com.azuro.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var p: SharedPreferences
    private lateinit var key: EditText
    private lateinit var grid: Switch
    private lateinit var permIcon: ImageView
    private lateinit var permState: TextView

    private fun card(vararg v: View) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; background = shape(T.card, dpf(18f), T.line, dp(1))
        v.forEach { addView(it) }
    }
    private fun section(t: String) = tv(t.uppercase(), 11.5f, T.mut, true).apply {
        letterSpacing = 0.08f; setPadding(dp(4), dp(22), 0, dp(8))
    }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(14), dp(14), dp(14)) }

    private fun modelRow(title: String, sub: String, ic: Int, k: String, def: String, vision: Boolean): View {
        val r = row()
        val wrap = FrameLayout(this).apply { background = shape(T.priSoft, dpf(20f)) }
        wrap.addView(icon(ic, T.pri, 20), FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER))
        r.addView(wrap, LinearLayout.LayoutParams(dp(40), dp(40)))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(tv(title, 15f, T.ink, true))
        col.addView(tv(sub, 12f, T.mut), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(2) })
        val id = tv(p.getString(k, def) ?: def, 12.5f, T.pri, true).apply { isSingleLine = true; ellipsize = TextUtils.TruncateAt.MIDDLE }
        col.addView(id, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6) })
        r.addView(col, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        r.addView(icon(R.drawable.ic_chevron, T.mut, 20))
        r.background = ripple(shape(0, 0f), 0f)
        r.setOnClickListener { ModelPicker.show(this, vision, id.text.toString()) { m -> id.text = m; p.edit().putString(k, m).apply() } }
        return r
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        T.init(this); p = getSharedPreferences("azuro", MODE_PRIVATE)
        window.statusBarColor = T.bg; window.navigationBarColor = T.bg
        if (!T.dark) window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(24), dp(20), dp(28)) }

        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(ImageView(this).apply { setImageResource(R.drawable.logo); background = shape(T.card, dpf(15f)); clipToOutline = true },
            LinearLayout.LayoutParams(dp(52), dp(52)))
        val ht = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ht.addView(tv("Azuro", 26f, T.ink, true))
        ht.addView(tv("Screen assistant", 13.5f, T.mut), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
        head.addView(ht, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(14) })
        col.addView(head)

        col.addView(section("OpenRouter"))
        key = EditText(this).apply {
            hint = "API key  (sk-or-…)"; setHintTextColor(T.mut); setTextColor(T.ink); textSize = 15f; background = null; isSingleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(p.getString("key", BuildConfig.DEFAULT_KEY))
        }
        val kr = row().apply { setPadding(dp(16), dp(4), dp(16), dp(4)) }
        kr.addView(icon(R.drawable.ic_lock, T.mut, 20))
        kr.addView(key, LinearLayout.LayoutParams(0, dp(48), 1f).apply { marginStart = dp(12) })
        col.addView(card(kr))
        col.addView(tv("Get a key at openrouter.ai/keys", 12f, T.mut), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8); marginStart = dp(4) })

        col.addView(section("Models"))
        col.addView(card(
            modelRow("Vision model", "Reads your screenshot", R.drawable.ic_eye, "vmodel", DEF_V, true), divider(),
            modelRow("Chat model", "Writes the answer", R.drawable.ic_chat, "cmodel", DEF_C, false)))
        col.addView(tv("The vision model describes the screen, then the chat model replies. Free models are rate-limited.", 12f, T.mut),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(8); marginStart = dp(4) })

        col.addView(section("Accuracy"))
        grid = Switch(this).apply { isChecked = p.getBoolean("grid", true) }
        val gr = row()
        val gc = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        gc.addView(tv("Precision grid", 15f, T.ink, true))
        gc.addView(tv("Adds faint reference marks to the screenshot so markers land exactly on target.", 12f, T.mut), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(3) })
        gr.addView(gc, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(12) }); gr.addView(grid)
        col.addView(card(gr))

        col.addView(section("Permissions"))
        val pr = row()
        permIcon = icon(R.drawable.ic_check, T.mut, 20)
        pr.addView(permIcon)
        pr.addView(tv("Display over other apps", 15f, T.ink, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(12) })
        permState = tv("", 12.5f, T.mut, true); pr.addView(permState)
        pr.background = ripple(shape(0, 0f), 0f)
        pr.setOnClickListener { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
        col.addView(card(pr))

        val go = tv("Start Azuro", 16f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER; background = ripple(shape(T.pri, dpf(16f)), dpf(16f)); setOnClickListener { go() }
        }
        col.addView(go, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(28) })
        setContentView(ScrollView(this).apply { setBackgroundColor(T.bg); isVerticalScrollBarEnabled = false; addView(col) })
    }

    override fun onResume() {
        super.onResume()
        val ok = Settings.canDrawOverlays(this)
        permIcon.setColorFilter(if (ok) T.ok else T.mut); permState.text = if (ok) "Granted" else "Tap to allow"
        permState.setTextColor(if (ok) T.ok else T.mut)
    }

    private fun go() {
        p.edit().putString("key", key.text.toString().trim()).putBoolean("grid", grid.isChecked).apply()
        if (key.text.isBlank()) { Toast.makeText(this, "Add your OpenRouter key first", Toast.LENGTH_SHORT).show(); return }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow “Display over other apps”, then tap Start again", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1); return
        }
        startActivityForResult(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent(), 100)
    }

    override fun onRequestPermissionsResult(r: Int, perms: Array<out String>, g: IntArray) { go() }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 100 && res == RESULT_OK && data != null) {
            startForegroundService(Intent(this, OverlayService::class.java).putExtra("code", res).putExtra("data", data))
            moveTaskToBack(true)
        }
    }
}
