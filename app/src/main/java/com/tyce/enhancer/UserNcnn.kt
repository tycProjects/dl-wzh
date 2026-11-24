package com.tyce.enhancer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** An ncnn model imported by the user (a .param + .bin pair and its upscale factor). */
data class UserNcnnModel(val id: String, val label: String, val scale: Int, val prepadding: Int, val param: String, val bin: String) {
    fun toEngine() = EngineModel(
        id = id, label = label, desc = "${scale}× upscale · imported", kind = EngineModel.Kind.NCNN,
        scale = scale, prepadding = prepadding, userParam = param, userBin = bin
    )
}

object UserNcnn {
    private const val PREFS = "enhancer"
    private const val KEY = "user_ncnn"
    private fun dir(c: Context) = File(c.filesDir, "user_ncnn").apply { mkdirs() }

    fun list(c: Context): List<UserNcnnModel> = try {
        val a = JSONArray(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        (0 until a.length()).map { a.getJSONObject(it) }.mapNotNull {
            val m = UserNcnnModel(it.getString("id"), it.getString("label"), it.getInt("scale"), it.optInt("pad", 10), it.getString("param"), it.getString("bin"))
            if (File(m.param).exists() && File(m.bin).exists()) m else null
        }
    } catch (_: Throwable) { emptyList() }

    private fun save(c: Context, l: List<UserNcnnModel>) {
        val a = JSONArray()
        l.forEach { a.put(JSONObject().put("id", it.id).put("label", it.label).put("scale", it.scale).put("pad", it.prepadding).put("param", it.param).put("bin", it.bin)) }
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply()
    }

    fun displayName(c: Context, uri: Uri): String =
        c.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()

    private fun isParam(c: Context, uri: Uri): Boolean = try {
        c.contentResolver.openInputStream(uri)!!.use { i ->
            val head = ByteArray(7); val n = i.read(head)
            n == 7 && String(head, Charsets.US_ASCII) == "7767517" // ncnn param magic
        }
    } catch (_: Throwable) { false }

    /** Sorts two picked files into (param, bin) by content, not by file name. Returns null if they are not a valid pair. */
    fun classify(c: Context, a: Uri, b: Uri): Pair<Uri, Uri>? {
        val pa = isParam(c, a); val pb = isParam(c, b)
        return when {
            pa && !pb -> a to b
            pb && !pa -> b to a
            else -> null
        }
    }

    /** Copies, validates (magic, size, real engine load and a test run) and registers the model. Returns an error or null. */
    fun import(c: Context, paramUri: Uri, binUri: Uri, scale: Int, label: String): String? {
        if (!com.kaminari.imagine.Engine.nativeAvailable) return "The native engine is not available on this device."
        if (scale !in 1..8) return "Scale must be between 1 and 8."
        val id = "user_" + System.currentTimeMillis()
        val pf = File(dir(c), "$id.param"); val bf = File(dir(c), "$id.bin")
        try {
            c.contentResolver.openInputStream(paramUri)!!.use { i -> pf.outputStream().use { i.copyTo(it) } }
            c.contentResolver.openInputStream(binUri)!!.use { i -> bf.outputStream().use { i.copyTo(it) } }
            if (pf.length() < 32 || bf.length() < 1024) throw IllegalStateException("The files look empty or truncated.")
            val m = UserNcnnModel(id, label.ifBlank { "Imported model" }.take(40), scale, 10, pf.absolutePath, bf.absolutePath)
            val err = NcnnUpscaler.verify(c, m.toEngine())
            if (err != null) throw IllegalStateException(err)
            save(c, list(c) + m)
            return null
        } catch (t: Throwable) {
            pf.delete(); bf.delete()
            return t.message ?: t.toString()
        }
    }

    fun remove(c: Context, id: String) {
        val l = list(c)
        l.firstOrNull { it.id == id }?.let { File(it.param).delete(); File(it.bin).delete() }
        save(c, l.filter { it.id != id })
    }
}
