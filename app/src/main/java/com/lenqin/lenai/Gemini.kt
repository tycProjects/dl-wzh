package com.lenqin.lenai
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
class Gemini(c:Context){
 private val ctx=c;private val http=OkHttpClient.Builder().callTimeout(45,TimeUnit.SECONDS).build()
 suspend fun ask(q:String)=withContext(Dispatchers.IO){
 val key=SecurePrefs.get(ctx).getString("key","")?:""
 if(key.isBlank()) return@withContext "API Key belum diatur."
 try{
 val prompt="Bantu pengguna belajar. Jelaskan konsep dan langkah penyelesaian dengan ringkas. Jika konteks ujian aktif, berikan petunjuk belajar, bukan mengerjakan atau mengirim jawaban otomatis.\n\n$q"
 val body=JSONObject().put("contents",org.json.JSONArray().put(JSONObject().put("parts",org.json.JSONArray().put(JSONObject().put("text",prompt))))).toString()
 val req=Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key").post(body.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
 http.newCall(req).execute().use{res->val raw=res.body?.string().orEmpty();if(!res.isSuccessful)"Gemini error ${res.code}. Periksa API key/kuota." else JSONObject(raw).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?:"Tidak ada hasil."}
 }catch(e:Exception){"Koneksi gagal: ${e.localizedMessage}"}
 }
}
