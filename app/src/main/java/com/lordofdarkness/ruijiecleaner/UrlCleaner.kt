package com.lordofdarkness.ruijiecleaner

import java.net.URI
import java.net.URLDecoder

data class CleanOutcome(
    val original: String,
    val cleaned: String,
    val removed: List<String>,
    val keptParameterCount: Int
)

sealed class CleanResult {
    data class Success(val outcome: CleanOutcome) : CleanResult()
    data class Failure(val message: String) : CleanResult()
}

object UrlCleaner {
    fun clean(input: String, parametersToRemove: Set<String> = setOf("timestamp", "sign")): CleanResult {
        val raw = input.trim()
        if (raw.isBlank()) return CleanResult.Failure("URL ထည့်ပေးပါ။")
        val normalized = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"
        return try {
            val uri = URI(normalized)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host
            if (scheme !in setOf("http", "https") || host.isNullOrBlank()) {
                return CleanResult.Failure("URL ပုံစံမမှန်ပါ။ http:// သို့မဟုတ် https:// URL ကို စစ်ဆေးပါ။")
            }
            if (host.contains(" ")) return CleanResult.Failure("Domain/Host ထဲမှာ space ပါနေပါတယ်။")

            val removeSet = parametersToRemove.map { it.lowercase() }.toSet()
            val removed = mutableListOf<String>()
            val kept = mutableListOf<String>()
            val query = uri.rawQuery.orEmpty()
            if (query.isNotEmpty()) {
                query.split("&").forEach { pair ->
                    if (pair.isEmpty()) return@forEach
                    val eq = pair.indexOf('=')
                    val rawKey = if (eq >= 0) pair.substring(0, eq) else pair
                    val key = decode(rawKey)
                    if (key.lowercase() in removeSet) removed.add(key) else kept.add(pair)
                }
            }
            val rebuilt = buildString {
                append(scheme).append("://").append(host)
                if (uri.port != -1) append(":").append(uri.port)
                uri.rawPath?.takeIf { it.isNotEmpty() }?.let { append(it) }
                if (kept.isNotEmpty()) append("?").append(kept.joinToString("&"))
                uri.rawFragment?.let { append("#").append(it) }
            }
            CleanResult.Success(CleanOutcome(raw, rebuilt, removed, kept.size))
        } catch (e: Exception) {
            CleanResult.Failure("URL ကို ဖတ်မရပါ။ စာလုံးပေါင်းနဲ့ URL format ကို ပြန်စစ်ပါ။")
        }
    }

    fun validate(input: String): String {
        val raw = input.trim()
        if (raw.isBlank()) return "URL မထည့်ရသေးပါ။"
        val normalized = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"
        return try {
            val uri = URI(normalized)
            when {
                uri.scheme?.lowercase() !in setOf("http", "https") -> "HTTP/HTTPS protocol မဟုတ်ပါ။"
                uri.host.isNullOrBlank() -> "Domain/host မတွေ့ပါ။"
                else -> "URL ပုံစံမှန်ကန်ပါတယ်။ Server/Portal က လက်ခံမလက်ခံကို ဒီစစ်ဆေးမှုက အတည်မပြုနိုင်ပါ။"
            }
        } catch (_: Exception) {
            "URL format မှားနေပါတယ်။"
        }
    }

    private fun decode(value: String): String = try {
        URLDecoder.decode(value, "UTF-8")
    } catch (_: Exception) { value }
}
