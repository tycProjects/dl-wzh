package com.shadowyi.translator

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val pickFileCode = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/x-subrip", "text/plain"))
                }
                startActivityForResult(intent, pickFileCode)
                return true
            }
        }
        web.addJavascriptInterface(NativeGemini(), "NativeGemini")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickFileCode) {
            val uri = if (resultCode == RESULT_OK) data?.data else null
            fileCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else null)
            fileCallback = null
        }
    }

    inner class NativeGemini {
        @JavascriptInterface
        fun request(apiKey: String, prompt: String, optionsJson: String, callbackId: String) {
            worker.execute {
                val result = try {
                    val options = JSONObject(optionsJson)
                    val model = options.optString("model", "gemini-2.5-flash")
                    val temperature = options.optDouble("temperature", 0.15)
                    val maxTokens = options.optInt("maxOutputTokens", 8192)
                    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${Uri.encode(apiKey)}"
                    val body = JSONObject().apply {
                        put("contents", org.json.JSONArray().put(JSONObject().apply {
                            put("role", "user")
                            put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt)))
                        }))
                        put("generationConfig", JSONObject().apply {
                            put("temperature", temperature)
                            put("maxOutputTokens", maxTokens)
                            if (options.optBoolean("json", false)) put("responseMimeType", "application/json")
                        })
                    }
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 20000
                        readTimeout = 90000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    conn.disconnect()
                    if (raw.isBlank()) throw Exception("Gemini trả về phản hồi rỗng (HTTP $code).")
                    val parsed = JSONObject(raw)
                    if (code !in 200..299) {
                        throw Exception(parsed.optJSONObject("error")?.optString("message") ?: "HTTP $code")
                    }
                    val candidates = parsed.optJSONArray("candidates")
                    val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                    val text = buildString {
                        if (parts != null) for (i in 0 until parts.length()) append(parts.optJSONObject(i)?.optString("text").orEmpty())
                    }.trim()
                    if (text.isBlank()) throw Exception("Gemini không trả về nội dung.")
                    JSONObject().put("ok", true).put("text", text).toString()
                } catch (e: Exception) {
                    JSONObject().put("ok", false).put("error", e.message ?: "Lỗi không xác định").toString()
                }
                runOnUiThread {
                    val safe = JSONObject.quote(result)
                    web.evaluateJavascript("window.__nativeGeminiResult('$callbackId',$safe)", null)
                }
            }
        }
    }

    override fun onDestroy() {
        web.removeJavascriptInterface("NativeGemini")
        worker.shutdownNow()
        super.onDestroy()
    }
}
