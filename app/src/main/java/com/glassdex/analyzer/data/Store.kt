package com.glassdex.analyzer.data

import android.content.Context
import com.glassdex.analyzer.analysis.AnalysisReport
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HistoryItem(
    val fileName: String,
    val kind: String,
    val packageName: String?,
    val risk: String,
    val score: Int,
    val sha256: String,
    val outputDir: String?,
    val time: Long
)

data class Settings(
    val vtKey: String = "",
    val glassOpacity: Float = 0.16f,
    val animatedBackground: Boolean = true
)

/** Local persistence. History is stored per signed-in account (or "guest"). */
class Store(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("glassdex", Context.MODE_PRIVATE)

    fun loadSettings() = Settings(
        vtKey = prefs.getString("vt", "") ?: "",
        glassOpacity = prefs.getFloat("opacity", 0.16f),
        animatedBackground = prefs.getBoolean("anim", true)
    )

    fun saveSettings(s: Settings) {
        prefs.edit().putString("vt", s.vtKey).putFloat("opacity", s.glassOpacity)
            .putBoolean("anim", s.animatedBackground).apply()
    }

    var guestMode: Boolean
        get() = prefs.getBoolean("guest", false)
        set(v) { prefs.edit().putBoolean("guest", v).apply() }

    private fun file(userKey: String) = File(ctx.filesDir, "history_" + userKey.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    fun history(userKey: String): List<HistoryItem> = try {
        val f = file(userKey)
        if (!f.exists()) emptyList() else {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                HistoryItem(o.getString("fileName"), o.getString("kind"), o.optString("pkg").ifEmpty { null },
                    o.getString("risk"), o.getInt("score"), o.getString("sha256"),
                    o.optString("out").ifEmpty { null }, o.getLong("time"))
            }
        }
    } catch (_: Throwable) { emptyList() }

    fun addHistory(userKey: String, r: AnalysisReport): List<HistoryItem> {
        val list = (listOf(HistoryItem(r.fileName, r.kind.name, r.appInfo?.packageName, r.risk.label, r.riskScore,
            r.hashes.sha256, r.outputDir, System.currentTimeMillis())) + history(userKey)).take(100)
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("fileName", it.fileName).put("kind", it.kind).put("pkg", it.packageName ?: "")
                .put("risk", it.risk).put("score", it.score).put("sha256", it.sha256).put("out", it.outputDir ?: "").put("time", it.time))
        }
        file(userKey).writeText(arr.toString())
        return list
    }

    fun deleteHistoryItem(userKey: String, item: HistoryItem): List<HistoryItem> {
        item.outputDir?.let { File(it).deleteRecursively(); File("$it.zip").delete() }
        val list = history(userKey).filter { it.time != item.time }
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("fileName", it.fileName).put("kind", it.kind).put("pkg", it.packageName ?: "")
                .put("risk", it.risk).put("score", it.score).put("sha256", it.sha256).put("out", it.outputDir ?: "").put("time", it.time))
        }
        file(userKey).writeText(arr.toString())
        return list
    }
}
