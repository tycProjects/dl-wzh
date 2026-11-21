package com.nomango.browser

import android.content.Context
import android.net.Uri
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

    /** Secrets (the translation key) live in their own file, which is left out of every backup. */
    private val secret = ctx.applicationContext.getSharedPreferences("noman_secret", Context.MODE_PRIVATE)

    fun secretGet(k: String): String? = secret.getString(k, null)

    fun secretSet(k: String, v: String) {
        secret.edit().putString(k, v).apply()
    }

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
        set(K_FREQ, "{}")
    }

    // ---- frequently visited sites: {host: {n: visits, t: last visit, h: title, u: address of the site}}
    private fun freqObj(): JSONObject = try {
        JSONObject(get(K_FREQ) ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }

    /** "example.com" for https://www.example.com/a/b, or "" when it is not a web address. */
    fun siteKey(url: String): String {
        if (!(url.startsWith("http://") || url.startsWith("https://"))) return ""
        val h = try {
            Uri.parse(url).host
        } catch (e: Exception) {
            null
        }
        return (h ?: "").lowercase().removePrefix("www.")
    }

    /** Counts one visit of the site behind [url]. Only the counters and one title per site are stored, on this phone. */
    fun visit(title: String, url: String) {
        val host = siteKey(url)
        if (host.isEmpty()) return
        val all = freqObj()
        val e = all.optJSONObject(host) ?: JSONObject()
        val u = Uri.parse(url)
        var name = e.optString("h", "")
        val path = u.path ?: ""
        if (title.isNotBlank() && (name.isEmpty() || path.isEmpty() || path == "/")) name = title.trim()
        val root = u.buildUpon().path("/").clearQuery().fragment(null).build().toString()
        e.put("n", e.optInt("n", 0) + 1).put("t", System.currentTimeMillis()).put("h", name).put("u", root)
        all.put(host, e)
        if (all.length() > 200) {
            val rows = ArrayList<JSONObject>()
            val ks = all.keys()
            while (ks.hasNext()) {
                val k = ks.next()
                val x = all.optJSONObject(k) ?: continue
                rows.add(JSONObject().put("k", k).put("n", x.optInt("n")).put("t", x.optLong("t")))
            }
            rows.sortWith(Comparator { a, b ->
                val c = a.optInt("n").compareTo(b.optInt("n"))
                if (c != 0) c else a.optLong("t").compareTo(b.optLong("t"))
            })
            var drop = all.length() - 180
            for (r in rows) {
                if (drop <= 0) break
                all.remove(r.optString("k"))
                drop--
            }
        }
        set(K_FREQ, all.toString())
    }

    /** The most visited sites, most visited first. [minVisits] keeps a site seen once from being "frequent". */
    fun frequent(limit: Int, minVisits: Int): JSONArray {
        val all = freqObj()
        val rows = ArrayList<JSONObject>()
        val ks = all.keys()
        while (ks.hasNext()) {
            val k = ks.next()
            val x = all.optJSONObject(k) ?: continue
            if (x.optInt("n") >= minVisits) {
                rows.add(
                    JSONObject().put("h", k).put("u", x.optString("u")).put("t", x.optString("h"))
                        .put("n", x.optInt("n")).put("last", x.optLong("t"))
                )
            }
        }
        rows.sortWith(Comparator { a, b ->
            val c = b.optInt("n").compareTo(a.optInt("n"))
            if (c != 0) c else b.optLong("last").compareTo(a.optLong("last"))
        })
        val out = JSONArray()
        for (r in rows) {
            if (out.length() >= limit) break
            out.put(r)
        }
        return out
    }

    /** "allow", "block" or "" (not asked yet) for the site's access to the text you copied. */
    fun clipRule(host: String): String = try {
        JSONObject(get(K_CLIP) ?: "{}").optString(host, "")
    } catch (e: Exception) {
        ""
    }

    fun setClipRule(host: String, rule: String) {
        val o = try {
            JSONObject(get(K_CLIP) ?: "{}")
        } catch (e: Exception) {
            JSONObject()
        }
        if (rule.isEmpty()) o.remove(host) else o.put(host, rule)
        set(K_CLIP, o.toString())
    }

    fun removeFrequent(host: String) {
        val all = freqObj()
        all.remove(siteKey("https://$host/").ifEmpty { host })
        set(K_FREQ, all.toString())
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

    fun removeRecent(q: String) {
        val out = JSONArray()
        for (s in recents()) {
            if (!s.equals(q.trim(), ignoreCase = true)) out.put(s)
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
        const val K_FREQ = "freq"
        const val K_CLIP = "clip"
        const val K_LAST = "last"
        const val K_DESK = "desk"
        const val MAX_HIST = 100

        /** Keys the home page is allowed to read and write through the bridge. */
        val BRIDGE_KEYS: Set<String> = setOf("ng5s", "ng5h", "ng5b", "ng4", "ng3")
    }
}
