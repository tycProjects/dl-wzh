package com.logalyze.app.data

import com.logalyze.app.ui.AnalysisResult
import com.logalyze.app.ui.HistoryItem
import org.json.JSONArray
import org.json.JSONObject

private fun JSONArray.toStringList(): List<String> = List(length()) { getString(it) }

fun AnalysisResult.toJson(): String = JSONObject().apply {
    put("errorType", errorType)
    put("summary", summary)
    put("confidence", confidence)
    put("rootCause", rootCause)
    put("evidence", JSONArray(evidence))
    put("fixes", JSONArray(fixes))
    launcher?.let { put("launcher", it) }
    if (mods.isNotEmpty()) put("mods", JSONArray(mods))
    if (deviceInfo.isNotEmpty()) {
        put("deviceInfo", JSONArray().apply {
            deviceInfo.forEach { (k, v) -> put(JSONObject().put("k", k).put("v", v)) }
        })
    }
}.toString()

fun resultFromJson(json: String): AnalysisResult {
    val o = JSONObject(json)
    return AnalysisResult(
        errorType = o.getString("errorType"),
        summary = o.getString("summary"),
        confidence = o.getInt("confidence"),
        rootCause = o.getString("rootCause"),
        evidence = o.getJSONArray("evidence").toStringList(),
        fixes = o.getJSONArray("fixes").toStringList(),
        launcher = o.optString("launcher", "").ifEmpty { null },
        deviceInfo = o.optJSONArray("deviceInfo")?.let { arr ->
            List(arr.length()) { i ->
                val item = arr.getJSONObject(i)
                item.getString("k") to item.getString("v")
            }
        } ?: emptyList(),
        mods = o.optJSONArray("mods")?.toStringList() ?: emptyList()
    )
}

fun HistoryItem.toEntity() = HistoryEntity(id, fileName, result.toJson(), createdAt, resolved)

fun HistoryEntity.toItem() = HistoryItem(id, fileName, resultFromJson(resultJson), createdAt, resolved)
