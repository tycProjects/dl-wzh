package com.fakecall.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** متصل واحد: اسم + رقم + (صورة وصوت كملفات باسم الـ id). */
data class Profile(
    val id: String,
    val name: String,
    val number: String,
    val voiceName: String
)

/** التخزين الدائم: SharedPreferences للبيانات، ومجلد التطبيق للصور والأصوات. */
object Store {
    const val DEF_NAME = "اخوويا الكبيير ❤️❤️"
    const val DEF_NUMBER = "01155763302"

    private fun prefs(c: Context) = c.getSharedPreferences("fakecall", Context.MODE_PRIVATE)

    // ───────── البروفايلات ─────────

    fun profiles(c: Context): List<Profile> {
        val raw = prefs(c).getString("profiles", null)
        if (raw != null) {
            try {
                val a = JSONArray(raw)
                val out = (0 until a.length()).map {
                    val o = a.getJSONObject(it)
                    Profile(
                        o.getString("id"),
                        o.optString("name"),
                        o.optString("number"),
                        o.optString("voiceName")
                    )
                }
                if (out.isNotEmpty()) return out
            } catch (_: Exception) {
            }
        }
        val def = listOf(Profile("default", DEF_NAME, DEF_NUMBER, ""))
        saveProfiles(c, def)
        prefs(c).edit().putString("active", "default").apply()
        return def
    }

    fun saveProfiles(c: Context, list: List<Profile>) {
        val a = JSONArray()
        for (p in list) {
            val o = JSONObject()
            o.put("id", p.id)
            o.put("name", p.name)
            o.put("number", p.number)
            o.put("voiceName", p.voiceName)
            a.put(o)
        }
        prefs(c).edit().putString("profiles", a.toString()).apply()
    }

    fun activeId(c: Context): String {
        val list = profiles(c)
        val id = prefs(c).getString("active", "") ?: ""
        return if (list.any { it.id == id }) id else list.first().id
    }

    fun setActive(c: Context, id: String) {
        prefs(c).edit().putString("active", id).apply()
    }

    fun newId(): String = java.lang.Long.toString(System.currentTimeMillis(), 36)

    // ───────── الملفات ─────────

    fun photoFile(c: Context, id: String) = File(c.filesDir, "p_${id}_photo.jpg")
    fun thumbFile(c: Context, id: String) = File(c.filesDir, "p_${id}_thumb.jpg")
    fun voiceFile(c: Context, id: String) = File(c.filesDir, "p_${id}_voice")
    fun ringFile(c: Context) = File(c.filesDir, "ring_custom")

    fun deleteFiles(c: Context, id: String) {
        photoFile(c, id).delete()
        thumbFile(c, id).delete()
        voiceFile(c, id).delete()
    }

    fun loadBitmap(f: File): Bitmap? =
        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null

    // ───────── الإعدادات العامة ─────────

    fun vibrate(c: Context) = prefs(c).getBoolean("vibrate", true)
    fun setVibrate(c: Context, v: Boolean) = prefs(c).edit().putBoolean("vibrate", v).apply()

    fun delay(c: Context) = prefs(c).getInt("delay", 10)
    fun setDelay(c: Context, v: Int) = prefs(c).edit().putInt("delay", v).apply()

    fun ringName(c: Context): String =
        if (ringFile(c).exists()) (prefs(c).getString("ring_name", "ملف مخصص") ?: "ملف مخصص") else ""

    fun setRingName(c: Context, n: String) = prefs(c).edit().putString("ring_name", n).apply()

    fun resetRing(c: Context) {
        ringFile(c).delete()
        prefs(c).edit().remove("ring_name").apply()
    }

    fun scheduledAt(c: Context): Long {
        val at = prefs(c).getLong("scheduled_at", 0L)
        return if (at > System.currentTimeMillis()) at else 0L
    }

    fun setScheduledAt(c: Context, at: Long) = prefs(c).edit().putLong("scheduled_at", at).apply()

    // ───────── نسخ الملفات والصور ─────────

    fun copyUri(c: Context, uri: Uri, dest: File) {
        val input = c.contentResolver.openInputStream(uri) ?: throw IllegalStateException("no stream")
        input.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
    }

    fun displayName(c: Context, uri: Uri): String {
        try {
            c.contentResolver.query(uri, null, null, null, null)?.use { cur ->
                if (cur.moveToFirst()) {
                    val i = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i >= 0) {
                        val n = cur.getString(i)
                        if (!n.isNullOrEmpty()) return n
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "ملف مخصص"
    }

    /** بنصغّر الصورة (أقصى 800px) ونصلّح الدوران ونخزنها JPEG + نسخة مصغّرة للقايمة. */
    fun savePhoto(c: Context, uri: Uri, id: String) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        var sample = 1
        val max = maxOf(bounds.outWidth, bounds.outHeight)
        while (max / sample > 800) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = c.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("decode failed")

        try {
            c.contentResolver.openInputStream(uri)?.use { s ->
                val ex = ExifInterface(s)
                val deg = when (ex.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                if (deg != 0f) {
                    val m = Matrix()
                    m.postRotate(deg)
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
                }
            }
        } catch (_: Exception) {
        }

        photoFile(c, id).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }

        val k = 160f / maxOf(bmp.width, bmp.height)
        val thumb = if (k < 1f) {
            Bitmap.createScaledBitmap(bmp, Math.round(bmp.width * k), Math.round(bmp.height * k), true)
        } else bmp
        thumbFile(c, id).outputStream().use { thumb.compress(Bitmap.CompressFormat.JPEG, 80, it) }
    }
}
