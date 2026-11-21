package com.android.island.ai

/** Pure classification for deciding when to leave one explicitly configured API route. */
object ApiFailoverPolicy {
    private val quotaHints = listOf(
        "quota", "rate limit", "rate_limit", "rate-limit", "resource_exhausted",
        "insufficient_quota", "billing hard limit", "billing limit", "tokens per minute",
        "requests per minute", "exceeded your current", "limit exceeded", "limit reached",
        "quota reached", "daily limit", "usage limit", "tokens exhausted", "rate exceeded",
        "out of credits", "credits exhausted", "payment required", "saldo habis",
        "kuota habis", "batas penggunaan", "batas permintaan"
    )

    fun isQuotaExhausted(status: Int, message: String): Boolean =
        status == 429 || status == 402 ||
            ((status == 400 || status == 403) && quotaHints.any { message.contains(it, ignoreCase = true) })

    fun shouldTryNextRoute(status: Int, message: String): Boolean =
        status in setOf(401, 403, 408, 425, 429, 500, 502, 503, 504) ||
            isQuotaExhausted(status, message)

    /** Includes the credential only in an in-memory key; callers must never log this value. */
    fun routeKey(endpoint: AiEndpoint): String =
        "${endpoint.provider}|${endpoint.baseUrl}|${endpoint.model}|${endpoint.apiKey}"
}
