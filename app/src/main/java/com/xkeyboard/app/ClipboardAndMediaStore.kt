package com.xkeyboard.app

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class ClipEntry(
    val id: String,
    val kind: String, // "text" or "screenshot"
    val content: String,
    val savedAt: Long
)

object ClipboardAndMediaStore {
    const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    private const val PREFS_NAME = "xkb_clip_1d"
    private const val KEY_ITEMS = "items"

    fun purgeExpired(ctx: Context, now: Long = System.currentTimeMillis()): List<ClipEntry> {
        val sp = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY_ITEMS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val kept = ArrayList<ClipEntry>()
        val keptJson = JSONArray()
        var changed = false

        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val savedAt = obj.optLong("savedAt", 0L)
            val kind = obj.optString("kind", "text")
            val content = obj.optString("content", "")
            if (now - savedAt <= ONE_DAY_MS) {
                kept.add(ClipEntry(obj.optString("id", "$i"), kind, content, savedAt))
                keptJson.put(obj)
            } else {
                changed = true
                if (kind == "screenshot" && content.isNotEmpty()) {
                    runCatching { File(content).delete() }
                }
            }
        }
        if (changed) {
            sp.edit().putString(KEY_ITEMS, keptJson.toString()).apply()
        }
        return kept.sortedByDescending { it.savedAt }
    }

    fun addText(ctx: Context, text: String) {
        if (text.isBlank()) return
        val now = System.currentTimeMillis()
        val current = purgeExpired(ctx, now)
            .filterNot { it.kind == "text" && it.content == text }
            .toMutableList()
        current.add(0, ClipEntry("txt_$now", "text", text, now))
        save(ctx, current.take(40))
    }

    fun addScreenshotFromUri(ctx: Context, uri: Uri): File? {
        val now = System.currentTimeMillis()
        purgeExpired(ctx, now)
        val dir = File(ctx.filesDir, "shots_1d").apply { mkdirs() }
        val outFile = File(dir, "shot_$now.png")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        } ?: return null

        val current = purgeExpired(ctx, now).toMutableList()
        current.add(0, ClipEntry("img_$now", "screenshot", outFile.absolutePath, now))
        save(ctx, current.take(25))
        return outFile
    }

    fun deleteEntry(ctx: Context, id: String) {
        val current = purgeExpired(ctx).filterNot {
            if (it.id == id) {
                if (it.kind == "screenshot") runCatching { File(it.content).delete() }
                true
            } else false
        }
        save(ctx, current)
    }

    private fun save(ctx: Context, list: List<ClipEntry>) {
        val arr = JSONArray()
        for (item in list) {
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("kind", item.kind)
                    .put("content", item.content)
                    .put("savedAt", item.savedAt)
            )
        }
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_ITEMS, arr.toString()).apply()
    }
}
