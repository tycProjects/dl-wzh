package com.leon.htmlwebhost

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var api: EditText
    private lateinit var title: EditText
    private lateinit var result: TextView
    private lateinit var userText: TextView

    private var token = ""
    private var htmlUri: Uri? = null
    private val pick = 7001
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        api = findViewById(R.id.api)
        title = findViewById(R.id.title)
        result = findViewById(R.id.result)
        userText = findViewById(R.id.userText)

        api.setText("http://10.0.2.2:8080")

        findViewById<Button>(R.id.register).setOnClickListener { auth(false) }
        findViewById<Button>(R.id.login).setOnClickListener { auth(true) }
        findViewById<Button>(R.id.select).setOnClickListener { pickFile() }
        findViewById<Button>(R.id.upload).setOnClickListener { upload() }
        findViewById<Button>(R.id.list).setOnClickListener { listSites() }
    }

    private fun base(): String = api.text.toString().trim().removeSuffix("/")

    private fun auth(login: Boolean) {
        val user = username.text.toString().trim()
        val pass = password.text.toString()
        if (user.isEmpty() || pass.isEmpty()) { result.text = "Nhập tài khoản và mật khẩu."; return }

        executor.execute {
            try {
                val endpoint = if (login) "/api/auth/login" else "/api/auth/register"
                val body = JSONObject().apply { put("username", user); put("password", pass) }.toString()
                val r = request("POST", endpoint, body, null)
                val obj = JSONObject(r)
                token = obj.getString("token")
                getSharedPreferences("session", MODE_PRIVATE).edit().putString("token", token).apply()
                runOnUiThread { userText.text = "Đang đăng nhập: $user"; result.text = "Đăng nhập thành công." }
            } catch (e: Exception) { runOnUiThread { result.text = e.message ?: "Lỗi" } }
        }
    }

    private fun pickFile() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "text/html"
        }, pick)
    }

    @Deprecated("Compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pick && resultCode == Activity.RESULT_OK) {
            htmlUri = data?.data
            result.text = if (htmlUri != null) "Đã chọn file HTML." else "Chưa chọn file."
        }
    }

    private fun upload() {
        val uri = htmlUri ?: run { result.text = "Hãy chọn file HTML."; return }
        if (token.isEmpty()) { result.text = "Hãy đăng nhập trước."; return }

        executor.execute {
            try {
                val conn = URL(base() + "/api/sites/upload").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("Authorization", "Bearer $token")
                val boundary = "----HTMLHost${System.currentTimeMillis()}"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                DataOutputStream(conn.outputStream).use { out ->
                    val fileName = "index.html"
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"title\"\r\n\r\n")
                    out.writeBytes(title.text.toString().trim().ifEmpty { "My Website" } + "\r\n")
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                    out.writeBytes("Content-Type: text/html\r\n\r\n")
                    contentResolver.openInputStream(uri)!!.use { input -> input.copyTo(out) }
                    out.writeBytes("\r\n--$boundary--\r\n")
                }

                val text = readResponse(conn)
                val obj = JSONObject(text)
                val publicUrl = obj.optString("url")
                runOnUiThread { result.text = "Đăng thành công!\n\nLINK PUBLIC:\n$publicUrl" }
            } catch (e: Exception) { runOnUiThread { result.text = e.message ?: "Upload lỗi" } }
        }
    }

    private fun listSites() {
        if (token.isEmpty()) { result.text = "Hãy đăng nhập trước."; return }
        executor.execute {
            try {
                val text = request("GET", "/api/sites", null, token)
                val arr = JSONArray(text)
                val sb = StringBuilder("WEBSITE CỦA BẠN\n\n")
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    sb.append("• ").append(o.getString("title")).append("\n")
                    sb.append(o.getString("url")).append("\n\n")
                }
                runOnUiThread { result.text = sb.toString() }
            } catch (e: Exception) { runOnUiThread { result.text = e.message ?: "Lỗi" } }
        }
    }

    private fun request(method: String, endpoint: String, body: String?, bearer: String?): String {
        val c = URL(base() + endpoint).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("Content-Type", "application/json")
        if (!bearer.isNullOrEmpty()) c.setRequestProperty("Authorization", "Bearer $bearer")
        if (body != null) { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray()) } }
        return readResponse(c)
    }

    private fun readResponse(c: HttpURLConnection): String {
        val stream = if (c.responseCode in 200..399) c.inputStream else c.errorStream
        val text = stream.bufferedReader().readText()
        if (c.responseCode !in 200..399) throw Exception(JSONObject(text).optString("error", "HTTP ${c.responseCode}"))
        return text
    }
}
