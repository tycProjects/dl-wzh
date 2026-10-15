package com.nomango.browser

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small JSON store on top of SharedPreferences.
 * The home page (assets/index.html) reads/writes its data through the JavaScript
 * bridge using the same keys, so History, Bookmarks, Shortcuts and Settings are
 * shared between the native toolbar and the home page.
 */
class Store(ctx: Context) {

    private val sp = ctx.applicationContext.getSharedPreferences("noman_store", Context.MODE_PRIVATE)

    fun get(k: String): String? = sp.getString(k, null)

    fun set(k: String, v: String) {
        sp.edit().putString(k, v).apply()
    }

    fun clearAll() {
        sp.edit().clear().apply()
    }

    fun settings(): JSONObject = try {
        JSONObject(get(K_ST) ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }

    private fun arr(k: String): JSONArray = try {
        JSONArray(get(k) ?: "[]")
    } catch (e: Exception) {
        JSONArray()
    }

    // ---- history: [{q: title, u: url, t: millis}]
    fun history(): JSONArray = arr(K_HIST)

    fun addHistory(title: String, url: String) {
        val old = arr(K_HIST)
        val out = JSONArray()
        out.put(
            JSONObject()
                .put("q", if (title.isBlank()) url else title)
                .put("u", url)
                .put("t", System.currentTimeMillis())
        )
        for (i in 0 until old.length()) {
            if (out.length() >= MAX_HIST) break
            val o = old.optJSONObject(i) ?: continue
            if (o.optString("u") != url) out.put(o)
        }
        set(K_HIST, out.toString())
    }

    fun clearHistory() {
        set(K_HIST, "[]")
    }

    // ---- bookmarks: [{t: title, u: url}]
    fun bookmarks(): JSONArray = arr(K_BM)

    fun isBookmarked(url: String): Boolean {
        val b = arr(K_BM)
        for (i in 0 until b.length()) {
            if (b.optJSONObject(i)?.optString("u") == url) return true
        }
        return false
    }

    /** Returns true when the page is bookmarked after the call. */
    fun toggleBookmark(title: String, url: String): Boolean {
        val b = arr(K_BM)
        val out = JSONArray()
        var found = false
        for (i in 0 until b.length()) {
            val o = b.optJSONObject(i) ?: continue
            if (o.optString("u") == url) found = true else out.put(o)
        }
        if (!found) {
            val fresh = JSONArray()
            fresh.put(JSONObject().put("t", if (title.isBlank()) url else title).put("u", url))
            for (i in 0 until out.length()) fresh.put(out.get(i))
            set(K_BM, fresh.toString())
            return true
        }
        set(K_BM, out.toString())
        return false
    }

    // ---- recent searches: [string]
    fun recents(): List<String> {
        val a = arr(K_REC)
        val l = ArrayList<String>()
        for (i in 0 until a.length()) {
            val s = a.optString(i)
            if (s.isNotBlank()) l.add(s)
        }
        return l
    }

    fun addRecent(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        val out = JSONArray()
        out.put(t)
        for (s in recents()) {
            if (out.length() >= 10) break
            if (!s.equals(t, ignoreCase = true)) out.put(s)
        }
        set(K_REC, out.toString())
    }

    // ---- desktop site exceptions: {"example.com": true|false}
    fun deskSites(): JSONObject = try {
        JSONObject(get(K_DESK) ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }

    fun deskOverride(host: String): Boolean? {
        val o = deskSites()
        return if (o.has(host)) o.optBoolean(host) else null
    }

    /** value == null removes the exception for [host] */
    fun setDeskSite(host: String, value: Boolean?) {
        val o = deskSites()
        if (value == null) o.remove(host) else o.put(host, value)
        set(K_DESK, o.toString())
    }

    // ---- downloads: [{id, n, u, t}]
    fun downloads(): JSONArray = arr(K_DL)

    fun addDownload(id: Long, name: String, url: String) {
        val old = arr(K_DL)
        val out = JSONArray()
        out.put(JSONObject().put("id", id).put("n", name).put("u", url).put("t", System.currentTimeMillis()))
        for (i in 0 until old.length()) out.put(old.get(i))
        set(K_DL, out.toString())
    }

    /** A file the app wrote itself (blob:/data: downloads, backups). It has no DownloadManager id, so it keeps its own uri. */
    fun addLocalDownload(id: Long, name: String, source: String, uri: String, mime: String, size: Long) {
        val old = arr(K_DL)
        val out = JSONArray()
        out.put(
            JSONObject().put("id", id).put("n", name).put("u", source).put("t", System.currentTimeMillis())
                .put("uri", uri).put("m", mime).put("z", size)
        )
        for (i in 0 until old.length()) out.put(old.get(i))
        set(K_DL, out.toString())
    }

    fun findDownload(id: Long): JSONObject? {
        val a = arr(K_DL)
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            if (o.optLong("id") == id) return o
        }
        return null
    }

    fun removeDownload(id: Long) {
        val old = arr(K_DL)
        val out = JSONArray()
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optLong("id") != id) out.put(o)
        }
        set(K_DL, out.toString())
    }

    companion object {
        const val K_ST = "ng5s"
        const val K_HIST = "ng5h"
        const val K_BM = "ng5b"
        const val K_DL = "dl"
        const val K_REC = "rec"
        const val K_LAST = "last"
        const val K_DESK = "desk"
        const val MAX_HIST = 100

        /** Keys the home page is allowed to read and write through the bridge. */
        val BRIDGE_KEYS: Set<String> = setOf("ng5s", "ng5h", "ng5b", "ng4", "ng3")
    }
}
