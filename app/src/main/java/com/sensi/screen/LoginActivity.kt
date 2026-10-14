package com.sensi.screen

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {
    private lateinit var apiUrl: EditText
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var actionButton: Button
    private lateinit var switchMode: TextView
    private lateinit var message: TextView
    private var registering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AuthSession.token(this).isNullOrBlank()) {
            openMain()
            return
        }
        window.statusBarColor = android.graphics.Color.rgb(7, 94, 86)
        window.navigationBarColor = android.graphics.Color.rgb(243, 246, 250)
        val teal = 0xFF0F9688.toInt()
        val dark = 0xFF17212F.toInt()
        val muted = 0xFF697586.toInt()
        val bg = 0xFFF3F6FA.toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(24)); setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply { addView(root); isFillViewport = true }
        val brand = TextView(this).apply {
            text = "SensiScreen"; textSize = 30f; setTextColor(teal); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, dp(28), 0, dp(4))
        }
        root.addView(brand, match())
        root.addView(TextView(this).apply {
            text = "Đăng nhập tài khoản"; textSize = 23f; setTextColor(dark); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, dp(4), 0, dp(8))
        }, match())
        root.addView(TextView(this).apply {
            text = "Đăng nhập để tiếp tục sử dụng các công cụ tùy chỉnh màn hình."; textSize = 14f
            setTextColor(muted); gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(22))
        }, match())

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(20), dp(18), dp(18))
            setBackgroundColor(android.graphics.Color.WHITE); elevation = dp(3).toFloat()
        }
        root.addView(panel, LinearLayout.LayoutParams(-1, -2))
        apiUrl = field("Địa chỉ máy chủ API (https://...)", false).also {
            it.setText(AuthSession.apiUrl(this)); panel.addView(it, fieldParams())
        }
        username = field("Tên tài khoản", false).also { panel.addView(it, fieldParams()) }
        password = field("Mật khẩu", true).also { panel.addView(it, fieldParams()) }
        password.imeOptions = EditorInfo.IME_ACTION_DONE
        message = TextView(this).apply { textSize = 13f; setTextColor(0xFFB42318.toInt()); setPadding(0, dp(4), 0, dp(8)); visibility = View.GONE }
        panel.addView(message, match())
        actionButton = Button(this).apply {
            text = "Đăng nhập"; setTextColor(-1); backgroundTintList = android.content.res.ColorStateList.valueOf(teal)
            setOnClickListener { submit() }
        }
        panel.addView(actionButton, LinearLayout.LayoutParams(-1, dp(50)))
        switchMode = TextView(this).apply {
            text = "Chưa có tài khoản? Tạo tài khoản"; textSize = 14f; setTextColor(teal)
            gravity = Gravity.CENTER; setPadding(0, dp(18), 0, dp(4)); setOnClickListener { toggleMode() }
        }
        panel.addView(switchMode, match())
        root.addView(TextView(this).apply {
            text = "Cần kết nối máy chủ xác thực của bạn. Tài khoản được xác minh ở máy chủ; ứng dụng không lưu mật khẩu." 
            textSize = 12f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(0, dp(18), 0, 0)
        }, match())
        setContentView(scroll)
    }

    private fun field(hintText: String, secret: Boolean): EditText = EditText(this).apply {
        hint = hintText; textSize = 15f; setSingleLine(true)
        setPadding(dp(12), dp(8), dp(12), dp(8))
        if (secret) inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        else if (hintText == "Tên tài khoản") inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else { inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI; setText("https://") }
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFFF8FAFC.toInt()); setStroke(dp(1), 0xFFD6DEE7.toInt()); cornerRadius = dp(10).toFloat()
        }
    }
    private fun fieldParams() = LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(12) }
    private fun match() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toggleMode() {
        registering = !registering
        actionButton.text = if (registering) "Tạo tài khoản" else "Đăng nhập"
        switchMode.text = if (registering) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Tạo tài khoản"
        message.visibility = View.GONE
    }

    private fun submit() {
        val base = apiUrl.text.toString().trim().trimEnd('/')
        val user = username.text.toString().trim()
        val pass = password.text.toString()
        if (!base.startsWith("https://")) {
            showError("Nhập URL API bắt đầu bằng https:// để bảo vệ mật khẩu khi truyền."); return
        }
        if (user.length < 3 || pass.length < 8) { showError("Tên tài khoản cần ít nhất 3 ký tự, mật khẩu ít nhất 8 ký tự."); return }
        AuthSession.saveApiUrl(this, base)
        actionButton.isEnabled = false
        actionButton.text = "Đang kết nối…"
        message.visibility = View.GONE
        Thread {
            var error: String? = null
            var token: String? = null
            try {
                val endpoint = if (registering) "/api/auth/register" else "/api/auth/login"
                val conn = (URL(base + endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"; connectTimeout = 12000; readTimeout = 12000
                    doOutput = true; setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                val body = JSONObject().put("username", user).put("password", pass).toString()
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = JSONObject(response.ifBlank { "{}" })
                if (code in 200..299) token = json.optString("token").takeIf { it.isNotBlank() }
                else error = json.optString("error", "Yêu cầu thất bại (HTTP $code)")
                if (code in 200..299 && token == null) error = "Máy chủ không trả về token đăng nhập."
                conn.disconnect()
            } catch (e: Exception) { error = "Không kết nối được máy chủ: ${e.message ?: "haykiem tra URL vamang"}" }
            runOnUiThread {
                actionButton.isEnabled = true
                actionButton.text = if (registering) "Tạo tài khoản" else "Đăng nhập"
                if (token != null) { AuthSession.saveToken(this, token!!); openMain() }
                else showError(error ?: "Không thể xác thực.")
            }
        }.start()
    }

    private fun showError(text: String) { message.text = text; message.visibility = View.VISIBLE }
    private fun openMain() { startActivity(Intent(this, MainActivity::class.java)); finish() }
}