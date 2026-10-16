package com.smali.fox

import android.content.Context
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.JavascriptInterface
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Uses the exact DeepSeek Android endpoints and PoW flow supplied by the user. */
class DeepSeekClient(private val context: Context) {
    private val chatEndpoint = "https://chat.deepseek.com/api/v0/chat/completion"
    private val createEndpoint = "https://chat.deepseek.com/api/v0/chat_session/create"
    private val powEndpoint = "https://chat.deepseek.com/api/v0/chat/create_pow_challenge"

    private fun headers(token: String) = mutableMapOf(
        "User-Agent" to "DeepSeek/2.1.0 Android/36",
        "Accept" to "application/json",
        "Content-Type" to "application/json",
        "x-client-platform" to "android",
        "x-client-version" to "2.1.0",
        "x-client-locale" to "ar",
        "x-client-bundle-id" to "com.deepseek.chat",
        "authorization" to "Bearer ${token.trim()}"
    )

    data class BatchFunction(val signature: String, val smali: String)

    fun login(email: String, password: String, deviceId: String): Result<String> = runCatching {
        require(email.isNotBlank() && password.isNotBlank()) { "أدخل البريد وكلمة المرور" }
        val connection = (URL("https://chat.deepseek.com/api/v0/users/login").openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000; readTimeout = 30_000; requestMethod = "POST"; doOutput = true
            setRequestProperty("User-Agent", "DeepSeek/2.1.6 Android/35")
            setRequestProperty("Accept", "application/json"); setRequestProperty("Accept-Charset", "UTF-8"); setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept-Encoding", "gzip"); setRequestProperty("x-client-platform", "android")
            setRequestProperty("x-client-version", "2.1.6"); setRequestProperty("x-client-locale", "ar")
            setRequestProperty("x-client-bundle-id", "com.deepseek.chat"); setRequestProperty("x-rangers-id", "7649924219802782214"); setRequestProperty("x-client-timezone-offset", "10800")
        }
        val payload = JSONObject().apply { put("email", email.trim()); put("password", password); put("device_id", deviceId); put("os", "android") }
        connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
        val plain = String(bytes, Charsets.UTF_8)
        val raw = runCatching { JSONObject(plain); plain }.getOrElse { runCatching { GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() } }.getOrElse { plain } }
        if (responseCode !in 200..299) error("DeepSeek login HTTP $responseCode\nالاستجابة الكاملة:\n$raw")
        val root = JSONObject(raw)
        if (root.optInt("code", 0) != 0) error("DeepSeek login code=${root.optInt("code")}\nالرسالة: ${root.optString("msg").ifBlank { root.optString("message") }}\nالاستجابة الكاملة:\n$raw")
        val user = root.optJSONObject("data")?.optJSONObject("biz_data")?.optJSONObject("user")
        user?.optString("token")?.takeIf { it.isNotBlank() } ?: error("لم يرجع الخادم توكنًا\nالاستجابة الكاملة:\n$raw")
    }

    fun createChatSession(token: String): Result<String> = runCatching { newSession(token) }

    fun analyzeBatch(token: String, functions: List<BatchFunction>, batchNumber: Int = 1, totalBatches: Int = 1, onChunk: ((String) -> Unit)? = null, chatSessionId: String? = null): Result<String> = runCatching {
        require(token.isNotBlank()) { "أدخل رمز DeepSeek أولًا" }
        require(functions.isNotEmpty()) { "لا توجد دوال Z للإرسال" }
        val sessionId = chatSessionId?.takeIf { it.isNotBlank() } ?: newSession(token)
        val powResponse = postJson(powEndpoint, headers(token), JSONObject().put("target_path", "/api/v0/chat/completion"))
        val challenge = requireObject(requireObject(requireObject(powResponse, "data", "استجابة تحدي PoW"), "biz_data", "بيانات تحدي PoW"), "challenge", "تحدي PoW")
        val answer = PowSolver(context).solve(challenge)
        val powPayload = JSONObject().apply { put("algorithm", challenge.getString("algorithm")); put("challenge", challenge.getString("challenge")); put("salt", challenge.getString("salt")); put("answer", answer); put("signature", challenge.getString("signature")); put("target_path", challenge.getString("target_path")) }
        val powToken = Base64.encodeToString(powPayload.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val joined = functions.mapIndexed { index, fn -> "### دالة Z ${index + 1}\nالتوقيع: ${fn.signature}\n```smali\n${fn.smali}\n```" }.joinToString("\n\n")
        val prompt = """أنت محلل Android DEX/Smali دفاعي. هذه الدفعة $batchNumber من أصل $totalBatches، وتحتوي كل دالة Z مع كودها الكامل. حلّل الكود نفسه لتحديد الدوال التي تتحقق فعليًا من Premium أو VIP أو Pro أو الاشتراك أو الترخيص أو الوصول المدفوع.

لا تعتمد على اسم الدالة وحده. ابحث عن قراءة حالة أو entitlement/license/purchase، استدعاء تحقق، branch/if، وقيمة Boolean راجعة. فرّق بين الدليل المباشر والاستنتاج. لا تنفذ الكود ولا تقترح تجاوز حماية أو اشتراك.

اكتب تقريرًا عربيًا منظمًا ومختصرًا، وابدأ دائمًا بهذه البطاقة:
الحكم النهائي: [نعم / لا / غير كافٍ للحسم]
دوال Premium المؤكدة: [التواقيع فقط أو لا يوجد]
دوال غير صالحة: [التواقيع فقط أو لا يوجد]
نسبة الثقة: [0-100%] — اجعلها متناسبة مع قوة الدليل، ولا تخمّن.
السبب المختصر: [جملة واحدة]

بعد ذلك استخدم العناوين التالية حرفيًا:
1) الملخص الوظيفي
2) مسار التنفيذ والتعليمات المهمة
3) مؤشرات Premium/شراء/اشتراك
4) الدليل المباشر لكل دالة
5) المخاطر أو السلوك المشبوه
6) حدود التحليل

اذكر كل دالة مرة واحدة. لا تعِد نسخ كود Smali كاملًا في التقرير؛ الكود موجود هنا للتحليل فقط. إذا كان المقطع ناقصًا أو غير قياسي فاذكر ذلك صراحة.

$joined""".trimIndent()
        val payload = JSONObject().apply { put("chat_session_id", sessionId); put("prompt", prompt); put("ref_file_ids", JSONArray()); put("thinking_enabled", false); put("search_enabled", false); put("temperature", 0.1); put("max_tokens", 4096); put("top_p", 0.9); put("stream", true) }
        val response = open(chatEndpoint, headers(token) + ("x-ds-pow-response" to powToken)).apply { doOutput = true; requestMethod = "POST" }
        response.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        if (response.responseCode !in 200..299) error("DeepSeek HTTP ${response.responseCode}: ${readBody(response)}")
        val resultText = StringBuilder(); val rawBody = StringBuilder()
        response.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines -> lines.forEach { line -> rawBody.append(line).append('\n'); val raw = line.trim().removePrefix("data:").trim(); if (line.trimStart().startsWith("data:") && raw.isNotBlank() && raw != "[DONE]") appendStreamEvent(raw, resultText, onChunk) } }
        val extracted = resultText.toString().ifBlank { extractCompletionText(rawBody.toString()) }
        if (extracted.isBlank()) error("لم يُرجع DeepSeek تحليلًا\n\nالاستجابة الخام:\n$rawBody") else extracted
    }

    fun analyze(token: String, signature: String, category: String, keywords: String, smali: String, onChunk: ((String) -> Unit)? = null): Result<String> = runCatching {
        require(token.isNotBlank()) { "أدخل رمز DeepSeek أولًا" }
        val sessionId = newSession(token)
        val powResponse = postJson(powEndpoint, headers(token), JSONObject().put("target_path", "/api/v0/chat/completion"))
        val challenge = requireObject(requireObject(requireObject(powResponse, "data", "استجابة تحدي PoW"), "biz_data", "بيانات تحدي PoW"), "challenge", "تحدي PoW")
        val answer = PowSolver(context).solve(challenge)
        val powPayload = JSONObject().apply {
            put("algorithm", challenge.getString("algorithm"))
            put("challenge", challenge.getString("challenge"))
            put("salt", challenge.getString("salt"))
            put("answer", answer)
            put("signature", challenge.getString("signature"))
            put("target_path", challenge.getString("target_path"))
        }
        val powToken = Base64.encodeToString(powPayload.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val prompt = """
حلّل دالة Android DEX/Smali التالية بالعربية وبأسلوب أمني دفاعي. لا تنفذ الكود ولا تقترح تجاوز حماية أو اشتراك.
اكتب تقريرًا احترافيًا واضحًا، مع سطر فارغ بين الأقسام، وابدأ دائمًا ببطاقة حكم مختصرة بهذه الصيغة:
الحكم النهائي: [نعم / لا / غير كافٍ للحسم]
هل الدالة مرتبطة بـPremium أو الشراء أو الاشتراك؟ [نعم / لا / غير كافٍ للحسم]
نسبة الثقة: [0-100%]
السبب المختصر: [جملة واحدة]

بعد ذلك استخدم العناوين التالية حرفيًا:
1) الملخص الوظيفي
2) مسار التنفيذ والتعليمات المهمة
3) مؤشرات Premium/شراء/اشتراك
4) المخاطر أو السلوك المشبوه
5) درجة الثقة وحدود التحليل
استخدم نقاطًا قصيرة عند الحاجة، وميّز بين الدليل المباشر والاستنتاج. لا تعتبر اسمًا مبهمًا أو إشارة عامة دليلًا كافيًا وحدها. ضع أي تعليمات Smali داخل كتلة كود Markdown مستقلة، ولا تلصق الكلمات أو العناوين ببعضها. إذا كان المقطع ناقصًا أو غير قياسي فاذكر ذلك صراحة دون اختلاق تفاصيل.

بيانات الدالة:
- التوقيع: $signature
- التصنيف: $category
- الكلمات المطابقة: $keywords

كود Smali:
```smali
$smali
```
""".trimIndent()
        val payload = JSONObject().apply {
            put("chat_session_id", sessionId)
            put("prompt", prompt)
            put("ref_file_ids", JSONArray())
            put("thinking_enabled", false)
            put("search_enabled", false)
            put("temperature", 0.2)
            put("max_tokens", 2048)
            put("top_p", 0.9)
            put("stream", true)
        }
        val response = open(chatEndpoint, headers(token) + ("x-ds-pow-response" to powToken)).apply { doOutput = true; requestMethod = "POST" }
        response.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        if (response.responseCode !in 200..299) error("DeepSeek HTTP ${response.responseCode}: ${readBody(response)}")
        val resultText = StringBuilder()
        val rawBody = StringBuilder()
        response.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                rawBody.append(line).append('\n')
                val raw = line.trim().removePrefix("data:").trim()
                if (line.trimStart().startsWith("data:") && raw.isNotBlank() && raw != "[DONE]") {
                    appendStreamEvent(raw, resultText, onChunk)
                }
            }
        }
        val body = rawBody.toString()
        val extracted = resultText.toString().ifBlank { extractCompletionText(body) }
        if (extracted.isBlank()) error("لم يُرجع DeepSeek تحليلًا\n\nالاستجابة الخام:\n$body")
        if (extracted.trim().length <= 6) {
            "استجابة DeepSeek ناقصة؛ هذه الاستجابة الخام الكاملة للتشخيص:\n\n$body"
        } else {
            extracted
        }
    }

    private fun appendStreamEvent(raw: String, result: StringBuilder, onChunk: ((String) -> Unit)?) {
        runCatching {
            val event = JSONObject(raw)
            if (event.optString("p").contains("thinking", true)) return@runCatching
            when (val value = event.opt("v")) {
                is String -> if (value.isNotEmpty()) { result.append(value); onChunk?.invoke(result.toString()) }
                is JSONObject -> {
                    val chunk = extractAssistantText(value.toString())
                    if (chunk.isNotEmpty()) { result.append(chunk); onChunk?.invoke(result.toString()) }
                }
            }
        }
    }

    private fun extractCompletionText(body: String): String {
        val sseText = buildString {
            body.lineSequence().filter { it.trimStart().startsWith("data:") }.forEach { line ->
                val raw = line.trim().removePrefix("data:").trim()
                if (raw.isBlank() || raw == "[DONE]") return@forEach
                runCatching {
                    val event = JSONObject(raw)
                    if (event.optString("p").contains("thinking", true)) return@runCatching
                    when (val value = event.opt("v")) {
                        is String -> if (value.isNotEmpty()) append(value)
                        is JSONObject -> append(extractAssistantText(value.toString()))
                    }
                }
            }
        }
        return if (sseText.isNotBlank()) sseText else extractAssistantText(body)
    }

    /** Removes API envelopes accidentally returned by some streaming responses. */
    private fun extractAssistantText(raw: String): String {
        var current = raw.trim()
        repeat(3) {
            if (!current.startsWith("{") && !current.startsWith("[")) return current
            val parsed = runCatching { JSONTokener(current).nextValue() }.getOrNull() ?: return current
            current = when (parsed) {
                is JSONObject -> when {
                    parsed.optJSONArray("fragments") != null -> parsed.optJSONArray("fragments")!!.let { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let { fragment -> fragment.optString("content").takeIf(String::isNotBlank) ?: fragment.optString("text").takeIf(String::isNotBlank) } }.joinToString("") }
                    parsed.optJSONObject("response") != null -> parsed.optJSONObject("response")!!.toString()
                    parsed.optJSONObject("message") != null -> parsed.optJSONObject("message")!!.toString()
                    parsed.optJSONObject("data") != null -> parsed.optJSONObject("data")!!.toString()
                    parsed.optJSONObject("biz_data") != null -> parsed.optJSONObject("biz_data")!!.toString()
                    parsed.optJSONArray("choices") != null -> parsed.optJSONArray("choices")!!.toString()
                    parsed.optString("content").isNotBlank() -> parsed.optString("content")
                    parsed.optString("v").isNotBlank() -> parsed.optString("v")
                    else -> current
                }
                is JSONArray -> (0 until parsed.length()).mapNotNull { parsed.optString(it).takeIf(String::isNotBlank) }.joinToString("")
                else -> current
            }.trim()
        }
        return current
    }

    private fun newSession(token: String): String {
        val connection = open(createEndpoint, headers(token)).apply { doOutput = true; requestMethod = "POST" }
        connection.outputStream.use { }
        if (connection.responseCode !in 200..299) error("فشل إنشاء جلسة DeepSeek: HTTP ${connection.responseCode}")
        val raw = readBody(connection)
        val root = runCatching { JSONObject(raw) }.getOrElse { error("تعذر قراءة استجابة إنشاء جلسة DeepSeek: ${it.message}\n\nالاستجابة الخام:\n$raw") }
        val session = requireObject(requireObject(requireObject(root, "data", "استجابة إنشاء الجلسة"), "biz_data", "بيانات الجلسة"), "chat_session", "جلسة المحادثة")
        return session.optString("id").takeIf { it.isNotBlank() } ?: error("استجابة DeepSeek لا تحتوي معرف جلسة.\n\nالاستجابة الخام:\n$raw")
    }

    private fun requireObject(parent: JSONObject, key: String, label: String): JSONObject {
        val value = parent.opt(key)
        if (value !is JSONObject) {
            val errorText = parent.optString("msg").ifBlank { parent.optString("message") }.ifBlank { parent.optString("detail") }
            throw IllegalStateException("$label غير متاح: data=$value${if (errorText.isNotBlank()) "\nرسالةالخادم: $errorText" else ""}\n\nالاستجابة الخام:\n$parent")
        }
        return value
    }

    private fun postJson(url: String, requestHeaders: Map<String, String>, body: JSONObject): JSONObject {
        val connection = open(url, requestHeaders).apply { doOutput = true; requestMethod = "POST" }
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        if (connection.responseCode !in 200..299) error("DeepSeek HTTP ${connection.responseCode}: ${readBody(connection)}")
        return JSONObject(readBody(connection))
    }

    private fun open(url: String, requestHeaders: Map<String, String>): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 20_000
        readTimeout = 120_000
        requestHeaders.forEach { (key, value) -> setRequestProperty(key, value) }
    }

    private fun readBody(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }
}

private class PowSolver(private val context: Context) {
    fun solve(challenge: JSONObject): Double {
        val latch = CountDownLatch(1)
        var output: String? = null
        val wasmBase64 = context.assets.open("sha3_wasm_bg.7b9ca65ddd.wasm").use { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        main.post {
            runCatching {
                WebView(context.applicationContext).apply {
                    settings.javaScriptEnabled = true
                    addJavascriptInterface(object {
                        @JavascriptInterface fun done(value: String) { output = value; latch.countDown() }
                    }, "AndroidPow")
                    webViewClient = object : WebViewClient() { override fun onPageFinished(view: WebView?, url: String?) { view?.evaluateJavascript("window.solvePow(${JSONObject.quote(challenge.toString())},${JSONObject.quote(wasmBase64)}).then(function(v){AndroidPow.done(v)}).catch(function(e){AndroidPow.done(JSON.stringify({ok:false,error:String(e)}))})", null) } }
                    loadUrl("file:///android_asset/pow_solver.html")
                }
            }.onFailure {
                output = JSONObject().put("ok", false).put("error", it.message.orEmpty()).toString()
                latch.countDown()
            }
        }
        if (!latch.await(45, TimeUnit.SECONDS)) error("انتهت مهلة حل PoW عبر WASM")
        val text = runCatching {
            when (val parsed = JSONTokener(output ?: "null").nextValue()) {
                is String -> parsed
                is JSONObject -> parsed.toString()
                else -> ""
            }
        }.getOrDefault("")
        val result = JSONObject(text)
        if (!result.optBoolean("ok")) error("فشل PoW: ${result.optString("error")}")
        return result.getDouble("answer")
    }
}
