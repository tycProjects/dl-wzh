package com.lenqin.lenai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var apiKeyInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = SecurePrefs.get(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(30), dp(22), dp(24))
            setBackgroundColor(0xFF080F1E.toInt())
        }

        content.addView(TextView(this).apply {
            text = "✦ LEN AI"
            textSize = 32f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "SMART SCREEN ASSISTANT  •  V3.0"
            textSize = 12f
            setTextColor(0xFF38BDF8.toInt())
        }, margin(top = 4, bottom = 22))

        apiKeyInput = EditText(this).apply {
            hint = "Gemini API Key"
            setSingleLine(true)
            inputType = 129
            setText(prefs.getString("key", "") ?: "")
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF8EA2BE.toInt())
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setBackgroundColor(0xFF1B2A42.toInt())
        }
        content.addView(apiKeyInput, margin(bottom = 10))

        content.addView(actionButton("SIMPAN API KEY") {
            val key = apiKeyInput.text.toString().trim()
            if (key.isEmpty()) {
                toast("API Key belum diisi.")
            } else {
                prefs.edit().putString("key", key).apply()
                toast("API Key tersimpan secara terenkripsi.")
            }
        }, margin(bottom = 12))

        content.addView(sectionTitle("PENGATURAN FLOATING"))
        content.addView(actionButton("IZINKAN TAMPIL DI ATAS APLIKASI") {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
            } else toast("Izin overlay sudah aktif.")
        }, margin(top = 8, bottom = 8))

        content.addView(actionButton("AKTIFKAN FLOATING LEN AI") {
            if (!hasKey()) return@actionButton
            if (!Settings.canDrawOverlays(this)) {
                toast("Aktifkan izin overlay terlebih dahulu.")
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
                toast("Floating LEN AI diaktifkan.")
            }
        }, margin(bottom = 8))

        content.addView(actionButton("NONAKTIFKAN FLOATING / SCREEN READING") {
            stopService(Intent(this, OverlayService::class.java))
            toast("Floating dan sesi baca layar dihentikan.")
        }, margin(bottom = 18))

        content.addView(sectionTitle("BACA LAYAR APLIKASI LAIN"))
        content.addView(TextView(this).apply {
            text = "Aktifkan izin baca layar satu kali dari sini. Setelah disetujui, kembali ke Chrome atau aplikasi yang berisi materi. Saat tombol BACA LAYAR ditekan, panel akan menghilang sebentar agar layar aplikasi di belakangnya dapat terbaca."
            textSize = 14f
            setTextColor(0xFFB7C5D9.toInt())
        }, margin(top = 8, bottom = 10))

        content.addView(actionButton("AKTIFKAN IZIN BACA LAYAR") {
            if (!hasKey()) return@actionButton
            if (!Settings.canDrawOverlays(this)) {
                toast("Aktifkan izin overlay terlebih dahulu.")
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
                startActivity(Intent(this, CapturePermissionActivity::class.java))
            }
        }, margin(bottom = 10))

        content.addView(TextView(this).apply {
            text = "Catatan: Android menampilkan dialog persetujuan resmi untuk screen capture. LEN AI hanya membaca layar setelah kamu menyetujuinya. Beberapa aplikasi yang melindungi layar dapat menghasilkan gambar hitam."
            textSize = 12f
            setTextColor(0xFF8293AA.toInt())
        }, margin(top = 8))

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        })
    }

    private fun hasKey(): Boolean {
        val key = apiKeyInput.text.toString().trim()
        if (key.isEmpty()) {
            toast("Masukkan dan simpan Gemini API Key terlebih dahulu.")
            return false
        }
        SecurePrefs.get(this).edit().putString("key", key).apply()
        return true
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 13f
        setTextColor(0xFF38BDF8.toInt())
        setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFF147DD1.toInt())
        setOnClickListener { action() }
    }

    private fun margin(top: Int = 0, bottom: Int = 0) =
        LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, dp(top), 0, dp(bottom))
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
