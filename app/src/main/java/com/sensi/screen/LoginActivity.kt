package com.sensi.screen

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Native login gate. The existing MainActivity remains the real app UI. */
class LoginActivity : AppCompatActivity() {
    private val auth by lazy { getSharedPreferences("ldzed_auth", Context.MODE_PRIVATE) }
    private val blue = Color.rgb(0, 126, 210)
    private val darkBlue = Color.rgb(12, 66, 120)
    private val paleBlue = Color.rgb(232, 244, 255)
    private val ink = Color.rgb(25, 43, 63)
    private val muted = Color.rgb(100, 116, 132)
    private val adminPassword = "LDZED-ADMIN-2026"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun showLogin() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(235, 247, 255), Color.WHITE, Color.rgb(225, 241, 255)))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(16), 0, dp(16))
        }
        val badge = TextView(this).apply {
            text = "LDZEDMODS  ·  BLUE EDITION"
            textSize = 13f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(dp(18), dp(10), dp(18), dp(10))
            background = rounded(blue, 40)
        }
        page.addView(badge, lpWrap())
        val title = TextView(this).apply {
            text = "Chào mừng trở lại 👋"; textSize = 27f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink); gravity = Gravity.CENTER
        }
        page.addView(title, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(26) })
        val subtitle = TextView(this).apply {
            text = "Đăng nhập bằng key để mở ứng dụng"
            textSize = 15f; setTextColor(muted); gravity = Gravity.CENTER
        }
        page.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(24) })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(22), dp(20), dp(20))
            background = rounded(Color.WHITE, 26)
            elevation = dp(5).toFloat()
        }
        val label = TextView(this).apply { text = "KEY ĐĂNG NHẬP"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(darkBlue) }
        card.addView(label, lpWrap())
        val keyInput = EditText(this).apply {
            hint = "Nhập key của bạn"; isSingleLine = true; textSize = 16f
            setTextColor(ink); setHintTextColor(muted); setPadding(dp(14), 0, dp(14), 0)
            background = rounded(paleBlue, 14)
        }
        card.addView(keyInput, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(10) })
        val login = Button(this).apply {
            text = "ĐĂNG NHẬP"; isAllCaps = false; textSize = 16f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE); background = rounded(blue, 16)
        }
        card.addView(login, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(16) })
        val admin = Button(this).apply {
            text = "Khu vực Admin · Tạo key"; isAllCaps = false; textSize = 15f
            setTextColor(darkBlue); background = rounded(paleBlue, 16)
        }
        card.addView(admin, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(10) })
        page.addView(card, LinearLayout.LayoutParams(-1, -2))
        val foot = TextView(this).apply {
            text = "Sau khi xác thực, ứng dụng sẽ mở menu Android gốc với các chức năng hiện có."
            textSize = 13f; setTextColor(muted); gravity = Gravity.CENTER
        }
        page.addView(foot, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22) })
        scroll.addView(page)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(outer)

        login.setOnClickListener { authenticate(keyInput.text.toString().trim()) }
        keyInput.setOnEditorActionListener { _, _, _ -> authenticate(keyInput.text.toString().trim()); true }
        admin.setOnClickListener { showAdminPasswordDialog() }
    }

    private fun authenticate(key: String) {
        if (key.isBlank()) { toast("Vui lòng nhập key"); return }
        val expiry = auth.getLong("key_${key.uppercase(Locale.ROOT)}", 0L)
        if (expiry == 0L) { toast("Key không đúng hoặc chưa được tạo trên thiết bị này"); return }
        if (System.currentTimeMillis() >= expiry) {
            auth.edit().remove("key_${key.uppercase(Locale.ROOT)}").apply()
            toast("Key đã hết hạn"); return
        }
        getSharedPreferences("ldzed_auth", Context.MODE_PRIVATE).edit().putBoolean("session_ok", true).apply()
        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
        startActivity(android.content.Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showAdminPasswordDialog() {
        val input = EditText(this).apply {
            hint = "Mật khẩu Admin"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this).setTitle("Xác thực Admin").setMessage("Nhập mật khẩu Admin để tạo key.")
            .setView(input)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Tiếp tục") { _, _ ->
                if (input.text.toString() == adminPassword) showCreateKeyDialog()
                else toast("Mật khẩu Admin không đúng")
            }.show()
    }

    private fun showCreateKeyDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), 0) }
        val daysInput = EditText(this).apply {
            hint = "Số ngày sử dụng (ví dụ: 30)"; inputType = InputType.TYPE_CLASS_NUMBER
        }
        box.addView(daysInput, LinearLayout.LayoutParams(-1, dp(54)))
        AlertDialog.Builder(this).setTitle("Tạo key mới").setMessage("Key sẽ chỉ được lưu trên thiết bị này.")
            .setView(box).setNegativeButton("Hủy", null)
            .setPositiveButton("Tạo key", null).create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val days = daysInput.text.toString().trim().toIntOrNull()
                        if (days == null || days !in 1..3650) {
                            daysInput.error = "Nhập số ngày từ 1 đến 3650"; return@setOnClickListener
                        }
                        val key = "LDZ-" + UUID.randomUUID().toString().replace("-", "").take(12).uppercase(Locale.ROOT)
                        val expiry = System.currentTimeMillis() + days.toLong() * 24L * 60L * 60L * 1000L
                        auth.edit().putLong("key_$key", expiry).apply()
                        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(expiry))
                        dialog.dismiss()
                        AlertDialog.Builder(this).setTitle("Tạo key thành công")
                            .setMessage("Key: $key\nHết hạn: $date\n\nHãy sao chép và lưu key. Key được lưu cục bộ trên thiết bị này.")
                            .setPositiveButton("Đóng", null).setNeutralButton("Sao chép") { _, _ ->
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("ldzedmods key", key))
                                toast("Đã sao chép key")
                            }.show()
                    }
                }
                dialog.show()
            }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()
    private fun lpWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
