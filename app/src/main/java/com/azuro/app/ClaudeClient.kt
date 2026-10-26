package com.azuro.app

import android.graphics.Bitmap
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ClaudeClient {
    const val SYSTEM = """Bạn là Azuro, trợ lý AI nổi trên màn hình điện thoại của người dùng. Bạn nhận ảnh chụp màn hình hiện tại cùng câu hỏi. Trả lời ngắn gọn, đúng ngôn ngữ người dùng, nói rõ bước cần bấm.
Khi cần chỉ chỗ trên màn hình, thêm CUỐI câu trả lời một khối:
```draw
[{"t":"arrow","x1":0.2,"y1":0.3,"x2":0.5,"y2":0.5},{"t":"circle","x":0.5,"y":0.5,"r":0.08},{"t":"rect","x":0.1,"y":0.2,"w":0.3,"h":0.1},{"t":"text","x":0.1,"y":0.15,"s":"Bấm vào đây"}]
```
Toạ độ là tỉ lệ 0..1 theo chiều rộng/cao của ảnh (r là tỉ lệ theo chiều rộng). Chỉ vẽ khi thật sự hữu ích. Không tự thực hiện thao tác thay người dùng."""

    fun chat(key: String, model: String, hist: JSONArray, text: String, bmp: Bitmap?): String {
        val content = JSONArray()
        if (bmp != null) {
            val s = 1280f / maxOf(bmp.width, bmp.height)
            val sc = if (s < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt(), (bmp.height * s).toInt(), true) else bmp
            val bo = ByteArrayOutputStream(); sc.compress(Bitmap.CompressFormat.JPEG, 80, bo)
            content.put(JSONObject().put("type", "image").put("source", JSONObject()
                .put("type", "base64").put("media_type", "image/jpeg")
                .put("data", Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP))))
        }
        content.put(JSONObject().put("type", "text").put("text", text))
        val msgs = JSONArray()
        for (i in 0 until hist.length()) msgs.put(hist.get(i))
        msgs.put(JSONObject().put("role", "user").put("content", content))
        val body = JSONObject().put("model", model).put("max_tokens", 1200).put("system", SYSTEM).put("messages", msgs)
        val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.connectTimeout = 20000; c.readTimeout = 90000; c.doOutput = true
        c.setRequestProperty("content-type", "application/json")
        c.setRequestProperty("x-api-key", key)
        c.setRequestProperty("anthropic-version", "2023-06-01")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = c.responseCode
        val s = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().readText()
        if (code !in 200..299) throw Exception("HTTP $code: ${s.take(300)}")
        val arr = JSONObject(s).getJSONArray("content")
        val sb = StringBuilder()
        for (i in 0 until arr.length()) arr.getJSONObject(i).optString("text").let { sb.append(it) }
        return sb.toString()
    }
}
