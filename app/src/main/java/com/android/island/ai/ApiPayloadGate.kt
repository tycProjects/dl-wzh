package com.android.island.ai

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Fail-closed gate for responses that are demonstrably unusable.
 * It does not claim to prove factual truth; it prevents obvious error/empty payloads
 * and known provider-contract failures from reaching the model as usable data.
 */
internal object ApiPayloadGate {
    data class Decision(val blocked: Boolean, val reasons: List<String>) {
        fun asText(): String = if (!blocked) {
            "GERBANG DATA: lolos pemeriksaan dasar; ini bukan jaminan kebenaran faktual."
        } else {
            buildString {
                appendLine("DIBLOKIR OLEH VALIDATOR DATA:")
                reasons.distinct().forEach { appendLine("- $it") }
                append("ISI RESPONS TIDAK DITERUSKAN KE AI/PENGGUNA.")
            }
        }
    }

    fun assess(
        body: String,
        contentType: String,
        domainChecks: List<String> = emptyList(),
        provider: String? = null
    ): Decision {
        val reasons = mutableListOf<String>()
        val type = contentType.lowercase(Locale.ROOT)
        val trimmed = body.trim().removePrefix("\uFEFF")

        if (trimmed.isBlank()) return Decision(true, listOf("Respons kosong."))
        val looksJson = "json" in type || trimmed.startsWith("{") || trimmed.startsWith("[")
        val looksXml = "xml" in type || trimmed.startsWith("<?xml") || trimmed.startsWith("<")
        if ((type.startsWith("text/") && "html" !in type) ||
            (type.isBlank() && !looksJson && !looksXml)) {
            return Decision(true, listOf("Respons hanya berupa teks bebas; tidak ada struktur data yang dapat diperiksa dengan aman."))
        }

        if ("json" in type || trimmed.startsWith("{") || trimmed.startsWith("[")) {
            val rootObject = runCatching { JSONObject(trimmed) }.getOrNull()
            val rootArray = runCatching { JSONArray(trimmed) }.getOrNull()
            if (rootObject == null && rootArray == null) {
                return Decision(true, listOf("JSON tidak menghasilkan objek atau array data yang dapat diperiksa."))
            }
            if (rootObject != null) {
                if (rootObject.length() == 0) reasons += "Objek JSON kosong."
                val topKeys = mutableSetOf<String>()
                val keyIterator = rootObject.keys()
                while (keyIterator.hasNext()) topKeys += keyIterator.next().lowercase(Locale.ROOT)
                val metadataOnly = setOf("success", "ok", "status", "message", "meta", "request_id", "timestamp")
                if (topKeys.isNotEmpty() && topKeys.all { it in metadataOnly }) {
                    reasons += "Respons hanya berisi metadata/status tanpa informasi data yang dapat digunakan."
                }
                inspectObject(rootObject, reasons, 0)
            }
            if (rootArray != null && rootArray.length() == 0) reasons += "Array JSON kosong."
            if (rootArray != null && rootArray.length() > 0) {
                val nonNull = (0 until rootArray.length()).count { rootArray.opt(it) != null && rootArray.opt(it) != JSONObject.NULL }
                if (nonNull == 0) reasons += "Array JSON hanya berisi nilai null."
            }
            val allValues = mutableListOf<Any?>()
            if (rootObject != null) collectValues(rootObject, allValues, 0)
            if (rootArray != null) collectValues(rootArray, allValues, 0)
            val nullCount = allValues.count { it == null || it == JSONObject.NULL }
            if (allValues.size >= 4 && nullCount >= 3 && nullCount.toDouble() / allValues.size >= 0.6) {
                reasons += "Terlalu banyak field bernilai null sehingga respons tidak cukup berisi data."
            }
        }

        // These are definite integrity failures, not mere absence of optional fields.
        domainChecks.forEach { check ->
            val c = check.lowercase(Locale.ROOT)
            if (
                "objek rates kosong" in c ||
                "bukan angka positif yang valid" in c ||
                "berada di luar rentang" in c ||
                "negatif atau tidak finite" in c ||
                "field population bukan angka non-negatif" in c ||
                "respons memuat field error/errors" in c ||
                "nilai kurs untuk" in c
            ) reasons += check
        }

        // A named provider's required response contract is stricter than a generic category guess.
        val providerKnown = !provider.isNullOrBlank() && provider.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "") in setOf("openmeteo", "frankfurter", "restcountries")
        if (providerKnown) {
            domainChecks.filter { it.startsWith("Profil ", ignoreCase = true) }.forEach { reasons += it }
        }

        return Decision(reasons.isNotEmpty(), reasons.distinct())
    }

    private fun inspectObject(obj: JSONObject, reasons: MutableList<String>, depth: Int) {
        if (depth > 24) return
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key)
            val normalized = key.lowercase(Locale.ROOT)
            if (normalized in setOf("success", "ok") && value == false) {
                reasons += "Respons menyatakan '$key=false'."
            }
            if (normalized in setOf("status", "state") && value is String &&
                value.lowercase(Locale.ROOT) in setOf("error", "failed", "failure", "invalid")) {
                reasons += "Status respons menyatakan '$value'."
            }
            if (normalized in setOf("error", "errors") && value != null &&
                value != JSONObject.NULL && value.toString().isNotBlank() &&
                value.toString() != "[]" && value.toString() != "{}") {
                reasons += "Respons memuat field '$key' yang menunjukkan kegagalan."
            }
            if (normalized in setOf("data", "results", "result", "items", "records", "payload")) {
                val empty = value == null || value == JSONObject.NULL ||
                    (value is JSONObject && value.length() == 0) ||
                    (value is JSONArray && value.length() == 0) ||
                    (value is String && value.isBlank())
                if (empty) reasons += "Kontainer '$key' kosong atau null."
            }
            when (value) {
                is JSONObject -> inspectObject(value, reasons, depth + 1)
                is JSONArray -> for (i in 0 until minOf(value.length(), 1000)) {
                    val child = value.opt(i)
                    if (child is JSONObject) inspectObject(child, reasons, depth + 1)
                }
            }
        }
    }

    private fun collectValues(value: Any?, out: MutableList<Any?>, depth: Int) {
        if (depth > 24 || out.size >= 5000) return
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext() && out.size < 5000) {
                    val child = value.opt(keys.next())
                    out += child
                    if (child is JSONObject || child is JSONArray) collectValues(child, out, depth + 1)
                }
            }
            is JSONArray -> for (i in 0 until minOf(value.length(), 1000)) {
                val child = value.opt(i)
                out += child
                if (child is JSONObject || child is JSONArray) collectValues(child, out, depth + 1)
            }
        }
    }
}
