package com.azuro.app

import android.graphics.Bitmap
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

const val DEF_V = "nvidia/nemotron-3.5-super-vl:free"
const val DEF_C = "openrouter/free"

object Prompts {
    const val VISION = """You are the eyes of another AI that cannot see the screen. Read the screenshot and reply with ONLY one JSON object, no markdown:
{"app":"app or site name","summary":"2-4 factual sentences: what is on screen and its current state","elements":[{"id":1,"label":"exact visible text or short description","kind":"button|input|text|icon|tab|image|toggle|list_item|other","box":[x1,y1,x2,y2]}]}
Rules: box values are integers 0-1000 relative to the image (x1,y1 = top-left, x2,y2 = bottom-right, origin top-left). If small plus marks and numbers appear on the image they form a reference grid every 100 units; use them to be exact. Each box must tightly enclose the whole element. List elements relevant to the user's question first, then other actionable elements, max 40. Never invent elements."""

    const val CHAT = """You are Azuro, a calm assistant that floats over the user's phone screen. You cannot see the screen yourself: a vision model describes it in a JSON "Screen" object. Answer using it. Be concise and practical: short steps, the exact label to tap. Reply in the user's language. Use Markdown sparingly (bold for labels, fenced code blocks for code or commands).
To point at things, finish your reply with ONE block:
```draw
[{"t":"box","id":3,"label":"Tap here"},{"t":"arrow","id":5,"label":"Then here"},{"t":"step","id":2,"n":1},{"t":"circle","id":4}]
```
Use only element ids from the Screen JSON, never coordinates. t is box, arrow, circle or step. Point at 1-3 targets that matter. If you cannot find the element, say so instead of guessing."""
}

object Api {
    fun sys(s: String) = JSONObject().put("role", "system").put("content", s)

    fun user(text: String, bmp: Bitmap?): JSONObject {
        if (bmp == null) return JSONObject().put("role", "user").put("content", text)
        val bo = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.JPEG, 82, bo)
        val url = "data:image/jpeg;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP)
        val parts = JSONArray()
            .put(JSONObject().put("type", "text").put("text", text))
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", url)))
        return JSONObject().put("role", "user").put("content", parts)
    }

    fun chat(key: String, model: String, msgs: JSONArray, maxTokens: Int): String {
        val body = JSONObject().put("model", model).put("max_tokens", maxTokens).put("messages", msgs)
        val c = URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.connectTimeout = 20000; c.readTimeout = 120000; c.doOutput = true
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("Authorization", "Bearer $key")
        c.setRequestProperty("X-Title", "Azuro")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = c.responseCode
        val s = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().readText()
        if (code !in 200..299) throw Exception("HTTP $code: ${s.take(200)}")
        val o = JSONObject(s)
        if (o.has("error")) throw Exception("HTTP 500: ${o.optJSONObject("error")?.optString("message")}")
        return o.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content")
    }

    fun json(s: String): JSONObject? {
        val a = s.indexOf('{'); val b = s.lastIndexOf('}')
        if (a < 0 || b <= a) return null
        return try { JSONObject(s.substring(a, b + 1)) } catch (e: Exception) { null }
    }
}
