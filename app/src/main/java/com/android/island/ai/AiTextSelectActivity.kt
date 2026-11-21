package com.android.island.ai

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.android.island.core.IslandBus

/**
 * Layar pemilih teks: jawaban AI tampil di activity yang punya fokus penuh, sehingga seleksi per
 * kata, pegangan seleksi, dan menu Salin bawaan Android bekerja seperti di browser. Window overlay
 * Island tidak fokus, jadi seleksi di sana tidak bisa dipakai.
 */
class AiTextSelectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = pendingText.orEmpty()
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#E6070A12"))
        }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#151A2C"))
            setPadding(px(16), px(14), px(16), px(14))
        }
        val title = TextView(this).apply {
            this.text = "Pilih teks jawaban"
            setTextColor(Color.parseColor("#EDF1FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, 0, 0, px(4))
        }
        val hint = TextView(this).apply {
            this.text = "Tekan lama satu kata untuk memilih, lalu pakai menu Salin. Ketuk Salin semua untuk menyalin seluruh jawaban."
            setTextColor(Color.parseColor("#A7B0CC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, 0, 0, px(10))
        }
        val body = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#EDF1FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setLineSpacing(0f, 1.25f)
            setTextIsSelectable(true)
            setPadding(px(2), px(2), px(2), px(2))
        }
        val scroll = ScrollView(this).apply {
            addView(body)
            isFillViewport = false
        }
        val copyAll = TextView(this).apply {
            this.text = "Salin semua"
            setTextColor(Color.parseColor("#0A0D16"))
            setBackgroundColor(Color.parseColor("#5EE7FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(px(14), px(12), px(14), px(12))
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("AI Island", text))
                Toast.makeText(this@AiTextSelectActivity, "Disalin", Toast.LENGTH_SHORT).show()
            }
        }
        val close = TextView(this).apply {
            this.text = "Tutup"
            setTextColor(Color.parseColor("#EDF1FF"))
            setBackgroundColor(Color.parseColor("#1F2740"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(px(14), px(12), px(14), px(12))
            setOnClickListener { finish() }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, px(10), 0, 0)
            addView(close, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = px(8) })
            addView(copyAll, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        card.addView(title)
        card.addView(hint)
        card.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        card.addView(buttons)
        root.addView(
            card,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                setMargins(px(12), px(40), px(12), px(40))
            }
        )
        setContentView(root)
    }

    override fun onDestroy() {
        if (isFinishing) {
            pendingText = null
            IslandBus.endExternalAi()
        }
        super.onDestroy()
    }

    companion object {
        /** Teks dikirim lewat variabel, bukan extra intent, karena jawaban AI bisa sangat panjang. */
        @Volatile
        private var pendingText: String? = null

        fun open(context: Context, text: String): Boolean {
            if (text.isBlank()) return false
            pendingText = text
            IslandBus.beginExternalAi()
            val ok = runCatching {
                context.applicationContext.startActivity(
                    Intent(context.applicationContext, AiTextSelectActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
                )
            }.isSuccess
            if (!ok) {
                pendingText = null
                IslandBus.endExternalAi()
            }
            return ok
        }
    }
}
