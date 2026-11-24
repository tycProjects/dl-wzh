package com.tyce.enhancer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Modular cloud provider contract. Add a class + register it in [CloudRegistry] to support more APIs. */
interface CloudProvider {
    val id: String
    val displayName: String
    val keyHelpUrl: String
    val docsUrl: String
    /** Plain-language steps shown in the "How to get API keys" section. */
    val howTo: List<String>
    val privacyNote: String
    /** Returns null if the key is accepted, otherwise a readable error. */
    fun validate(apiKey: String): String?
    fun enhance(apiKey: String, src: Bitmap, scale: Int, faceEnhance: Boolean,
                onStatus: (String) -> Unit, isCancelled: () -> Boolean): Bitmap
}

object CloudRegistry {
    val providers: List<CloudProvider> = listOf(ReplicateProvider)
    fun byId(id: String?) = providers.firstOrNull { it.id == id }
}

class CloudException(msg: String) : Exception(msg)

/** Replicate (nightmareai/real-esrgan), the same service Imagine's cloud mode used. */
object ReplicateProvider : CloudProvider {
    override val id = "replicate"
    override val displayName = "Replicate"
    override val keyHelpUrl = "https://replicate.com/account/api-tokens"
    override val docsUrl = "https://replicate.com/docs/topics/security/api-tokens"
    override val howTo = listOf(
        "Create or sign in to an account at replicate.com.",
        "Open Account → API tokens (button below).",
        "Create a token and copy it.",
        "Paste it here and tap Save & verify.",
    )
    override val privacyNote =
        "Cloud mode uploads the selected photo to Replicate to be processed there. " +
        "Replicate may charge your account per run; check your billing and their terms."

    private const val API = "https://api.replicate.com/v1"
    private const val MODEL = "nightmareai/real-esrgan"

    private fun open(path: String, key: String, method: String): HttpURLConnection =
        (URL(API + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15000; readTimeout = 60000
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Content-Type", "application/json")
        }

    private fun HttpURLConnection.body(): String {
        val s = if (responseCode in 200..299) inputStream else (errorStream ?: return "")
        return s.bufferedReader().use { it.readText() }
    }

    private fun httpError(code: Int, body: String): String = when (code) {
        401, 403 -> "The API key was rejected. Check that you copied the whole token."
        402 -> "Replicate needs billing set up on this account."
        429 -> "Too many requests. Wait a moment and try again."
        in 500..599 -> "Replicate is having trouble (HTTP $code). Try again later."
        else -> "Request failed (HTTP $code)."
    }

    override fun validate(apiKey: String): String? = try {
        val c = open("/account", apiKey.trim(), "GET")
        val code = c.responseCode
        c.body()
        if (code in 200..299) null else httpError(code, "")
    } catch (e: java.io.IOException) {
        "Could not reach Replicate. Check your internet connection."
    }

    override fun enhance(apiKey: String, src: Bitmap, scale: Int, faceEnhance: Boolean,
                         onStatus: (String) -> Unit, isCancelled: () -> Boolean): Bitmap {
        val key = apiKey.trim()
        val jpeg = ByteArrayOutputStream().also { src.compress(Bitmap.CompressFormat.JPEG, 95, it) }.toByteArray()
        val uri = "data:image/jpeg;base64," + Base64.encodeToString(jpeg, Base64.NO_WRAP)
        val input = JSONObject().put("image", uri).put("scale", scale.coerceIn(1, 10)).put("face_enhance", faceEnhance)
        val c = open("/models/$MODEL/predictions", key, "POST").apply {
            doOutput = true
            outputStream.use { it.write(JSONObject().put("input", input).toString().toByteArray()) }
        }
        val code = c.responseCode; val txt = c.body()
        if (code !in 200..299) throw CloudException(httpError(code, txt))
        var pred = JSONObject(txt)
        val getUrl = pred.getJSONObject("urls").getString("get")
        var waited = 0
        while (true) {
            if (isCancelled()) throw CloudException("Cancelled")
            when (pred.optString("status")) {
                "succeeded" -> break
                "failed", "canceled" -> throw CloudException("Cloud processing failed: " + pred.optString("error", "unknown error").take(160))
            }
            onStatus("Processing in the cloud…")
            Thread.sleep(1500); waited += 1500
            if (waited > 180_000) throw CloudException("Cloud processing timed out.")
            val g = (URL(getUrl).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Authorization", "Bearer $key"); connectTimeout = 15000; readTimeout = 30000
            }
            val gc = g.responseCode; val gb = g.body()
            if (gc !in 200..299) throw CloudException(httpError(gc, gb))
            pred = JSONObject(gb)
        }
        val out = pred.opt("output")
        val outUrl = (if (out is org.json.JSONArray) out.optString(0) else out?.toString()).orEmpty()
        if (!outUrl.startsWith("https://")) throw CloudException("Cloud returned no image.")
        val d = (URL(outUrl).openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 60000 }
        val bmp = d.inputStream.use { BitmapFactory.decodeStream(it) } ?: throw CloudException("Could not read the cloud result.")
        return bmp
    }
}
