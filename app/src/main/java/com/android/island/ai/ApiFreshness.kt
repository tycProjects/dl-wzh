package com.android.island.ai

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Reports source-provided time hints and HTTP cache metadata without confusing
 * retrieval time with provider data time. It deliberately does not declare a
 * response fresh solely because it was fetched successfully.
 */
internal object ApiFreshness {
    private val timestampKeys = listOf(
        "updated_at", "last_updated", "lastUpdate", "last_updated_at", "modified_at",
        "published_at", "publishedAt", "observation_time", "observed_at", "current_time",
        "timestamp", "datetime", "date", "time"
    )
    private val isoDate = Regex("\\d{4}-\\d{2}-\\d{2}(?:[Tt ][^\\\"\\s,}\\]]+)?")
    private val numericEpoch = Regex("-?\\d{10,13}")

    data class Headers(
        val date: String? = null,
        val age: String? = null,
        val cacheControl: String? = null,
        val expires: String? = null,
        val lastModified: String? = null,
        val etag: String? = null
    )

    internal fun parseInstant(raw: String?): Instant? {
        val value = raw?.trim()?.trim('"')?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching { LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC) }.getOrNull()
            ?: runCatching {
                val n = value.toLong()
                if (n >= 100_000_000_000L || n <= -100_000_000_000L) Instant.ofEpochMilli(n)
                else if (numericEpoch.matches(value)) Instant.ofEpochSecond(n) else null
            }.getOrNull()
    }

    /** Extracts a timestamp candidate only; key names do not prove its semantics. */
    internal fun payloadTimestamp(body: String): Pair<String, Instant>? {
        for (key in timestampKeys) {
            val escaped = Regex.escape(key)
            val pattern = Regex("\\\"$escaped\\\"\\s*:\\s*(?:\\[\\s*)?(\\\"[^\\\"]{4,64}\\\"|-?\\d{10,13})", RegexOption.IGNORE_CASE)
            val raw = pattern.find(body)?.groupValues?.getOrNull(1) ?: continue
            val parsed = parseInstant(raw) ?: continue
            // Ignore obviously implausible timestamps; do not turn them into a false freshness signal.
            val now = Instant.now()
            if (parsed.isAfter(now.plus(Duration.ofDays(2))) || parsed.isBefore(Instant.parse("1970-01-01T00:00:00Z"))) continue
            return key to parsed
        }
        // Some feeds expose an ISO date/time without a recognized timestamp key.
        val fallback = isoDate.find(body)?.value?.let(::parseInstant)
        val now = Instant.now()
        return fallback?.takeIf { !it.isAfter(now.plus(Duration.ofDays(2))) && !it.isBefore(Instant.parse("1970-01-01T00:00:00Z")) }
            ?.let { "tanggal/waktu tak berlabel" to it }
    }

    internal fun report(now: Instant, body: String, headers: Headers): String = buildString {
        val candidate = payloadTimestamp(body)
        if (candidate != null) {
            val (key, instant) = candidate
            appendLine("WAKTU DARI PAYLOAD (kandidat '$key', UTC): ${DateTimeFormatter.ISO_INSTANT.format(instant)}")
            val age = Duration.between(instant, now)
            if (!age.isNegative) appendLine("UMUR TIMESTAMP PAYLOAD: ${formatAge(age)}")
            else appendLine("PERINGATAN: timestamp payload berada di masa depan; perlu pemeriksaan zona waktu/sumber.")
            appendLine("CATATAN: nama field tidak menjamin bahwa ini waktu pembaruan seluruh dataset; bisa berupa waktu observasi, jadwal, atau tanggal item.")
        } else {
            appendLine("WAKTU DATA DARI PENYEDIA: tidak ditemukan timestamp yang dapat dikenali; kebaruan data TIDAK DIKETAHUI.")
        }

        val serverDate = parseHttpDate(headers.date)
        if (serverDate != null) appendLine("HTTP DATE (waktu respons server, bukan waktu pembaruan data): ${DateTimeFormatter.ISO_INSTANT.format(serverDate)}")
        val ageSeconds = headers.age?.trim()?.toLongOrNull()?.takeIf { it >= 0 }
        if (ageSeconds != null) appendLine("HTTP AGE: ${formatAge(Duration.ofSeconds(ageSeconds))} (perkiraan umur objek cache menurut perantara/server).")
        headers.cacheControl?.takeIf { it.isNotBlank() }?.let { appendLine("CACHE-CONTROL: ${sanitize(it)}") }
        headers.expires?.takeIf { it.isNotBlank() }?.let { appendLine("EXPIRES: ${sanitize(it)}") }
        headers.lastModified?.takeIf { it.isNotBlank() }?.let { raw ->
            val parsed = parseHttpDate(raw)
            appendLine("LAST-MODIFIED (metadata HTTP, bukan jaminan data terbaru): ${parsed?.let(DateTimeFormatter.ISO_INSTANT::format) ?: sanitize(raw)}")
        }
        headers.etag?.takeIf { it.isNotBlank() }?.let { appendLine("ETAG: ${sanitize(it)} (validator versi/cache, bukan timestamp pembaruan).") }
        if (listOf(headers.date, headers.age, headers.cacheControl, headers.expires, headers.lastModified, headers.etag).all { it.isNullOrBlank() }) {
            appendLine("METADATA CACHE HTTP: tidak disediakan server.")
        }
        append("STATUS KEBARUAN: tidak diberi label 'terbaru' hanya berdasarkan waktu pengambilan atau HTTP 200. Bandingkan timestamp payload dengan jadwal pembaruan resmi penyedia.")
    }

    private fun parseHttpDate(raw: String?): Instant? = raw?.let {
        runCatching { java.time.ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
    }

    private fun formatAge(age: Duration): String {
        val seconds = age.seconds.coerceAtLeast(0)
        val days = seconds / 86_400
        val hours = (seconds % 86_400) / 3_600
        val minutes = (seconds % 3_600) / 60
        return when {
            days > 0 -> "$days hari $hours jam"
            hours > 0 -> "$hours jam $minutes menit"
            minutes > 0 -> "$minutes menit"
            else -> "$seconds detik"
        }
    }

    private fun sanitize(value: String): String = value.replace(Regex("[\\r\\n\\t]+"), " ").take(300)
}
