package com.nomango.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Real site icons. An icon comes from a page you visited (the browser hands it over), or is downloaded once from the
 * site's own /favicon.ico, then from Google's public icon service as a last resort. Icons are kept in memory and in the
 * cache folder (refreshed after 14 days); a site that has no icon is not asked again for 24 hours.
 */
class Favicons(ctx: Context) {
    private val dir = File(ctx.cacheDir, "favicons")
    private val mem = object : LruCache<String, Bitmap>(3 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val urls = HashMap<String, String>()
    private val pool = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private val waiting = HashMap<String, ArrayList<(Bitmap) -> Unit>>()
    private val failed = HashMap<String, Long>()

    /** false = never go to the internet for icons, use only what visited pages delivered */
    @Volatile
    var internet = true

    /** called (on a background thread) whenever a new icon for a host became available */
    @Volatile
    var onReady: ((String) -> Unit)? = null

    fun key(host: String): String = host.trim().lowercase().removePrefix("www.")

    private fun file(k: String): File {
        if (!dir.exists()) dir.mkdirs()
        return File(dir, k.replace(Regex("[^a-z0-9.-]"), "_") + ".png")
    }

    private fun scaled(b: Bitmap): Bitmap {
        val m = Math.max(b.width, b.height)
        if (m <= 64) return b
        val f = 64f / m
        return Bitmap.createScaledBitmap(b, Math.max(1, (b.width * f).toInt()), Math.max(1, (b.height * f).toInt()), true)
    }

    private fun save(k: String, b: Bitmap) {
        try {
            FileOutputStream(file(k)).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (e: Exception) {
        }
    }

    private fun decode(f: File): Bitmap? = try {
        BitmapFactory.decodeFile(f.absolutePath)
    } catch (e: Throwable) {
        null
    }

    /** From memory only, safe on the UI thread. */
    fun peek(host: String): Bitmap? = mem.get(key(host))

    /** An icon the browser really received from a page of that site. */
    fun put(host: String, bmp: Bitmap) {
        val k = key(host)
        if (k.isEmpty()) return
        val s = scaled(bmp)
        mem.put(k, s)
        synchronized(failed) { failed.remove(k) }
        synchronized(urls) { urls.remove(k) }
        pool.execute { save(k, s) }
        onReady?.invoke(k)
    }

    /** Calls [cb] on the main thread with the icon of [host], fetching it first if it is not known yet. */
    fun load(host: String, cb: (Bitmap) -> Unit) {
        val k = key(host)
        if (k.isEmpty()) return
        val m = mem.get(k)
        if (m != null) {
            cb(m)
            return
        }
        synchronized(waiting) {
            val l = waiting[k]
            if (l != null) {
                l.add(cb)
                return
            }
            val fresh = ArrayList<(Bitmap) -> Unit>()
            fresh.add(cb)
            waiting[k] = fresh
        }
        pool.execute { resolve(k) }
    }

    private fun resolve(k: String) {
        var bmp: Bitmap? = null
        var stale = false
        val f = file(k)
        if (f.exists()) {
            bmp = decode(f)
            if (bmp != null && System.currentTimeMillis() - f.lastModified() > 14L * DAY) stale = true
        }
        if (bmp == null || stale) {
            var skip = false
            synchronized(failed) {
                val t = failed[k]
                if (t != null && System.currentTimeMillis() - t < DAY) skip = true
            }
            if (internet && !skip) {
                val n = download(k)
                if (n != null) {
                    bmp = n
                    save(k, n)
                } else if (bmp == null) {
                    synchronized(failed) { failed[k] = System.currentTimeMillis() }
                }
            }
        }
        val got = bmp
        val list = synchronized(waiting) { waiting.remove(k) }
        if (got != null) {
            mem.put(k, got)
            onReady?.invoke(k)
            if (list != null) main.post { for (c in list) c(got) }
        }
    }

    private fun download(k: String): Bitmap? {
        val a = fetch("https://$k/favicon.ico", 16)
        if (a != null) return scaled(a)
        val b = fetch("https://www.google.com/s2/favicons?domain=$k&sz=64", 24)
        if (b != null) return scaled(b)
        return null
    }

    private fun fetch(u: String, minSide: Int): Bitmap? {
        var con: HttpURLConnection? = null
        try {
            con = URL(u).openConnection() as HttpURLConnection
            con.connectTimeout = 5000
            con.readTimeout = 5000
            con.instanceFollowRedirects = true
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            if (con.responseCode != 200) return null
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            con.inputStream.use { ins ->
                while (out.size() < LIMIT) {
                    val n = ins.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                }
            }
            if (out.size() >= LIMIT || out.size() == 0) return null
            val b = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size()) ?: return null
            return if (b.width >= minSide && b.height >= minSide) b else null
        } catch (e: Throwable) {
            return null
        } finally {
            try {
                con?.disconnect()
            } catch (e: Exception) {
            }
        }
    }

    /** A "data:image/png;base64,..." address the home page can put in an image tag, or "" when there is no icon yet. */
    fun dataUrl(host: String): String {
        val k = key(host)
        if (k.isEmpty()) return ""
        synchronized(urls) {
            val c = urls[k]
            if (c != null) return c
        }
        var b = mem.get(k)
        if (b == null) {
            val f = file(k)
            if (f.exists()) b = decode(f)
            if (b != null) mem.put(k, b)
        }
        if (b == null) return ""
        val out = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.PNG, 100, out)
        val d = "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        synchronized(urls) { urls[k] = d }
        return d
    }

    fun clear() {
        mem.evictAll()
        synchronized(urls) { urls.clear() }
        synchronized(failed) { failed.clear() }
        pool.execute {
            try {
                dir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
            }
        }
    }

    companion object {
        private const val DAY = 24L * 60L * 60L * 1000L
        private const val LIMIT = 262144
    }
}
