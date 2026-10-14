package com.sensi.screen

import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Admin UI is only a client; all privileged actions are authorized by the backend. */
class AdminActivity : AppCompatActivity() {
    private lateinit var content: LinearLayout
    private lateinit var refreshButton: Button
    private val teal = 0xFF0F9688.toInt()
    private val dark = 0xFF17212F.toInt()
    private val muted = 0xFF697586.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 94, 86)
        window.navigationBarColor = 0xFFF3F6FA.toInt()
        if (AuthSession.token(this).isNullOrBlank()) { finish(); return }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(0xFFF3F6FA.toInt())
        }
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        refreshButton = Button(this).apply {
            text = "Làm mới danh sách"
            setOnClickListener { loadUsers() }
        }
        root.addView(refreshButton, LinearLayout.LayoutParams(-1, dp(48)))
        setContentView(root)
        content.addView(TextView(this).apply {
            text = "SensiScreen Admin"; textSize = 26f; setTextColor(teal)
            typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(4), 0, dp(4))
        })
        content.addView(TextView(this).apply {
            text = "Quản lý tài khoản · Các thay đổi được xác thực trên máy chủ"
            textSize = 14f; setTextColor(muted); setPadding(0, 0, 0, dp(16))
        })
        if (AuthSession.role(this) != "admin") {
            showMessage("Tài khoản hiện tại không có quyền Admin. Máy chủ sẽ từ chối mọi thao tác quản trị.")
        } else loadUsers()
    }

    private fun loadUsers() {
        setBusy(true)
        request("GET", "/api/admin/users", null) { code, body ->
            setBusy(false)
            if (code !in 200..299) {
                showMessage(JSONObject(body.ifBlank { "{}" }).optString("error", "Không tải được danh sách (HTTP $code)."))
                return@request
            }
            val users = JSONObject(body).optJSONArray("users")
            content.removeViews(2, (content.childCount - 2).coerceAtLeast(0))
            if (users == null || users.length() == 0) { showMessage("Chưa có tài khoản."); return@request }
            for (i in 0 until users.length()) {
                val user = users.getJSONObject(i)
                val id = user.optInt("id")
                val name = user.optString("username", "(không tên)")
                val role = user.optString("role", "user")
                val active = !user.optBoolean("locked", false)
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.WHITE); cornerRadius = dp(14).toFloat()
                        setStroke(dp(1), 0xFFE0E6ED.toInt())
                    }
                    elevation = dp(1).toFloat()
                }
                card.addView(TextView(this).apply {
                    text = name; textSize = 18f; setTextColor(dark); typeface = Typeface.DEFAULT_BOLD
                })
                card.addView(TextView(this).apply {
                    text = "ID: $id  ·  Quyền: $role  ·  Trạng thái: ${if (active) "Hoatdong" else "Dakhoa"}"
                    textSize = 13f; setTextColor(if (active) muted else 0xFFB42318.toInt())
                    setPadding(0, dp(4), 0, dp(8))
                })
                val action = Button(this).apply {
                    text = if (active) "Khóa tài khoản" else "Mở khóa tài khoản"
                    setOnClickListener { setUserActive(id, !active) }
                }
                card.addView(action, LinearLayout.LayoutParams(-1, dp(44)))
                content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
            }
        }
    }

    private fun setUserActive(id: Int, active: Boolean) {
        setBusy(true)
        request("PATCH", "/api/admin/users/$id/status", JSONObject().put("locked", !active)) { code, body ->
            setBusy(false)
            if (code in 200..299) {
                toast(if (active) "Đã mở khóa tài khoản." else "Đã khóa tài khoản.")
                loadUsers()
            } else {
                val message = JSONObject(body.ifBlank { "{}" }).optString("error", "Thao tác thất bại (HTTP $code).")
                toast(message)
                loadUsers()
            }
        }
    }

    private fun request(method: String, path: String, body: JSONObject?, done: (Int, String) -> Unit) {
        Thread {
            var code = 0
            var response = ""
            try {
                val base = AuthSession.apiUrl(this)
                if (!base.startsWith("https://")) throw IllegalStateException("URL API chưa được cấu hình hoặc không dùng HTTPS.")
                val conn = (URL(base.trimEnd('/') + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method; connectTimeout = 12000; readTimeout = 12000
                    setRequestProperty("Authorization", "Bearer ${AuthSession.token(this)}")
                    setRequestProperty("Accept", "application/json")
                    if (body != null) {
                        doOutput = true; setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                }
                if (body != null) conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()
            } catch (e: Exception) {
                response = JSONObject().put("error", e.message ?: "Không thể kết nối máy chủ.").toString()
            }
            runOnUiThread { done(code, response) }
        }.start()
    }

    private fun showMessage(text: String) {
        content.addView(TextView(this).apply {
            this.text = text; textSize = 14f; setTextColor(muted); setPadding(0, dp(10), 0, dp(12))
        })
    }
    private fun setBusy(busy: Boolean) { refreshButton.isEnabled = !busy; refreshButton.text = if (busy) "Đang xử lý…" else "Làm mới danh sách" }
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
