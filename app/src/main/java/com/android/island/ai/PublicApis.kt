package com.android.island.ai

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.xmlpull.v1.XmlPullParser
import android.util.Xml

/**
 * Public API candidates from the public-apis directory (MIT, see assets/public_apis_LICENSE.txt).
 * The local catalog does not preserve authentication or pricing metadata, so an entry is not guaranteed
 * to be free or keyless. The directory lists documentation pages, not endpoints. [find] only proposes candidates; [get] calls an
 * endpoint the model composed. A call is allowed only to a host that belongs to a listed API, so a prompt
 * injection cannot use it to send data to an arbitrary server. GET only, no credentials, bounded output.
 */
object PublicApis {
    const val ASSET = "public_apis.txt"
    private const val MAX_BODY = 12_000
    private const val MAX_REDIRECTS = 3
    private val SECRET_QUERY_KEYS = setOf(
        "key", "api_key", "api-key", "apikey", "x-api-key", "access_key", "auth", "authorization", "token",
        "access_token", "client_secret", "secret", "signature", "sig"
    )
    private val TOKEN = Regex("""[\p{L}\p{N}]{3,}""")
    private val WORD = Regex("""[\p{L}\p{N}]+""")

    data class Entry(val category: String, val name: String, val url: String, val desc: String)

    /** One asset line: category|name|docs-url|description. */
    internal fun parse(line: String): Entry? {
        val fields = line.split('|', limit = 4).map(String::trim)
        if (fields.size != 4) return null
        val (category, name, rawUrl, description) = fields
        if (category.isBlank() || name.isBlank() || description.isBlank()) return null
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (!uri.isAbsolute || !uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.userInfo != null || (uri.port != -1 && uri.port != 443)) return null
        if (host == "localhost" || '.' !in host || host.contains(':') || host.all { it.isDigit() || it == '.' }) return null
        return Entry(category, name, rawUrl, description)
    }

    /** Words too common in this directory to say anything about what an API does. */
    private val STOP = setOf("api", "apis", "data", "free", "information", "info", "get", "the", "and", "for", "with", "from", "about", "that", "this", "what", "how", "current")

    /** Crude plural fold so "countries" finds "country" and "rates" finds "rate". */
    private fun stem(w: String): String = when {
        w.length > 4 && w.endsWith("ies") -> w.dropLast(3) + "y"
        w.length > 3 && w.endsWith("s") && !w.endsWith("ss") -> w.dropLast(1)
        else -> w
    }

    private fun words(text: String): List<String> = WORD.findAll(text.lowercase()).map { stem(it.value) }.toList()

    /** Best matches first. A word that starts with a query word counts: name 3, category 2, description 1. */
    internal fun search(lines: Sequence<String>, query: String, limit: Int = 10): List<Entry> {
        val tokens = TOKEN.findAll(query.lowercase()).map { it.value }.filter { it !in STOP }.map(::stem).distinct().toList()
        if (tokens.isEmpty() || limit <= 0) return emptyList()
        val boundedLimit = limit.coerceAtMost(20)
        val seen = HashSet<String>()
        return lines.mapNotNull(::parse).mapNotNull { e ->
            val name = words(e.name); val cat = words(e.category); val desc = words(e.desc)
            val score = tokens.sumOf { t ->
                (if (name.any { it.startsWith(t) }) 3 else 0) +
                    (if (cat.any { it.startsWith(t) }) 2 else 0) +
                    (if (desc.any { it.startsWith(t) }) 1 else 0)
            }
            if (score <= 0) return@mapNotNull null
            val uri = runCatching { URI(e.url) }.getOrNull()
            val key = uri?.let {
                "${it.scheme.lowercase()}://${it.host.lowercase()}${it.rawPath.orEmpty().trimEnd('/')}${it.rawQuery?.let { q -> "?$q" }.orEmpty()}"
            } ?: e.url
            if (seen.add(key)) e to score else null
        }.sortedByDescending { it.second }.take(boundedLimit).map { it.first }.toList()
    }

    private fun hostOf(url: String): String? =
        runCatching { URI(url).host?.lowercase()?.removePrefix("www.") }.getOrNull()

    /** Remove credential-like query values before a URL is copied into the model conversation. */
    internal fun redactUrl(url: String): String = runCatching {
        val uri = URI(url)
        val query = uri.rawQuery?.split('&')?.joinToString("&") { pair ->
            val key = pair.substringBefore('=').lowercase()
            if (key in SECRET_QUERY_KEYS && '=' in pair) "${pair.substringBefore('=')}=[REDACTED]" else pair
        }
        buildString {
            append(uri.scheme).append("://").append(uri.rawAuthority).append(uri.rawPath.orEmpty())
            if (query != null) append('?').append(query)
            if (uri.rawFragment != null) append('#').append(uri.rawFragment)
        }
    }.getOrDefault("[URL disembunyikan]")

    /** A stable, non-speculative explanation for common HTTP failures. */
    internal fun httpFailure(status: Int, retryAfter: String? = null): String = when (status) {
        401 -> "HTTP 401: layanan meminta autentikasi atau API key. Android Island belum mengirim kredensial Public APIs."
        403 -> "HTTP 403: akses ditolak oleh layanan (izin, kebijakan, atau batas wilayah). Jangan menganggap datanya berhasil diambil."
        404 -> "HTTP 404: endpoint tidak ditemukan. Periksa dokumentasi terbaru; URL katalog bukan jaminan endpoint aktif."
        429 -> "HTTP 429: batas permintaan tercapai.${retryAfter?.takeIf { it.isNotBlank() }?.let { " Coba lagi setelah: $it." }.orEmpty()}"
        in 500..599 -> "HTTP $status: server penyedia sedang mengalami masalah."
        else -> "HTTP $status: permintaan tidak berhasil."
    }

    /** Null when [url] may be called, otherwise the reason it is refused. */
    internal fun refusal(lines: Sequence<String>, url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull()
            ?: return "URL tidak valid (spasi dan karakter khusus harus di-encode)."
        if (!uri.scheme.equals("https", ignoreCase = true)) return "Hanya https yang diizinkan."
        if (uri.userInfo != null) return "URL dengan kredensial tidak diizinkan."
        if (uri.port != -1 && uri.port != 443) return "Port selain 443 tidak diizinkan."
        val host = uri.host?.lowercase() ?: return "URL tidak punya host."
        if (host == "localhost" || '.' !in host || host.contains(':') || host.all { it.isDigit() || it == '.' }) {
            return "Alamat lokal atau IP langsung tidak diizinkan."
        }
        val bare = host.removePrefix("www.")
        val listed = lines.mapNotNull(::parse).any { e -> hostOf(e.url)?.let { bare == it || bare.endsWith(".$it") } == true }
        return if (listed) null else "Host $host tidak tercantum dalam katalog public-apis, jadi panggilan diblokir."
    }

    private fun <T> withLines(open: () -> InputStream, block: (Sequence<String>) -> T): T =
        open().bufferedReader(Charsets.UTF_8).useLines(block)

    /** Observation text for the model: candidate APIs for [query]. */
    fun find(open: () -> InputStream, query: String): String {
        val hits = withLines(open) { search(it, query) }
        if (hits.isEmpty()) {
            return "Tidak ada kandidat API yang cocok dengan \"$query\" di katalog lokal. Coba kata kunci bahasa Inggris lain (mis. weather, currency, country, trivia)."
        }
        return buildString {
            appendLine("Kandidat dari snapshot katalog public-apis lokal (bukan status live). Metadata autentikasi, harga, rate limit, CORS, dan uptime tidak tersedia di snapshot ini. Entri bukan bukti API aktif, gratis, atau tanpa API key. Tautan adalah halaman sumber/dokumentasi, bukan selalu endpoint. PublicApis hanya mendukung GET HTTPS tanpa API key/kredensial/header autentikasi; respons harus berupa JSON, XML, atau teks biasa. Verifikasi dokumentasi resmi sebelum memanggil endpoint.")
            hits.forEachIndexed { i, e -> appendLine("${i + 1}. ${e.name} [${e.category}] - ${e.desc} - ${e.url}") }
        }.trim()
    }

    /** Strict RFC 8259 JSON syntax checker; Android's JSONTokener is intentionally lenient. */
    private class StrictJsonParser(private val source: String) {
        private var index = 0

        fun isValid(): Boolean = runCatching {
            whitespace()
            value(0)
            whitespace()
            require(index == source.length) { "trailing content" }
        }.isSuccess

        private fun whitespace() {
            while (index < source.length && (source[index] == ' ' || source[index] == '\t' || source[index] == '\n' || source[index] == '\r')) index++
        }

        private fun value(depth: Int) {
            require(depth <= 64) { "nesting too deep" }
            whitespace()
            require(index < source.length) { "missing value" }
            when (source[index]) {
                '{' -> objectValue(depth + 1)
                '[' -> arrayValue(depth + 1)
                '"' -> stringValue()
                't' -> literal("true")
                'f' -> literal("false")
                'n' -> literal("null")
                '-', in '0'..'9' -> numberValue()
                else -> throw IllegalArgumentException("invalid value")
            }
        }

        private fun objectValue(depth: Int) {
            index++
            whitespace()
            if (take('}')) return
            while (true) {
                whitespace()
                require(index < source.length && source[index] == '\"') { "object key must be a string" }
                stringValue()
                whitespace()
                require(take(':')) { "missing colon" }
                value(depth)
                whitespace()
                if (take('}')) return
                require(take(',')) { "missing object separator" }
            }
        }

        private fun arrayValue(depth: Int) {
            index++
            whitespace()
            if (take(']')) return
            while (true) {
                value(depth)
                whitespace()
                if (take(']')) return
                require(take(',')) { "missing array separator" }
            }
        }

        private fun stringValue() {
            require(take('\"'))
            while (index < source.length) {
                val c = source[index++]
                when {
                    c == '\"' -> return
                    c == '\\' -> {
                        require(index < source.length) { "unfinished escape" }
                        when (source[index++]) {
                            '\"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> Unit
                            'u' -> repeat(4) {
                                require(index < source.length && source[index].digitToIntOrNull(16) != null) { "invalid unicode escape" }
                                index++
                            }
                            else -> throw IllegalArgumentException("invalid escape")
                        }
                    }
                    c.code < 0x20 -> throw IllegalArgumentException("unescaped control character")
                }
            }
            throw IllegalArgumentException("unterminated string")
        }

        private fun numberValue() {
            take('-')
            require(index < source.length) { "unfinished number" }
            if (take('0')) {
                require(index >= source.length || source[index] !in '0'..'9') { "leading zero" }
            } else {
                require(index < source.length && source[index] in '1'..'9') { "invalid integer" }
                while (index < source.length && source[index] in '0'..'9') index++
            }
            if (take('.')) {
                require(index < source.length && source[index] in '0'..'9') { "invalid fraction" }
                while (index < source.length && source[index] in '0'..'9') index++
            }
            if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
                index++
                if (index < source.length && source[index] in "+-") index++
                require(index < source.length && source[index] in '0'..'9') { "invalid exponent" }
                while (index < source.length && source[index] in '0'..'9') index++
            }
        }

        private fun literal(expected: String) {
            require(source.regionMatches(index, expected, 0, expected.length)) { "invalid literal" }
            index += expected.length
        }

        private fun take(expected: Char): Boolean {
            if (index < source.length && source[index] == expected) {
                index++
                return true
            }
            return false
        }
    }

    /**
     * Validate only the response structure, never the truth of its claims.
     * A successful HTTP status is not proof that a payload is meaningful or current.
     */
    internal fun assessBody(body: String, contentType: String): String {
        if (body.isBlank()) return "GAGAL VALIDASI: isi respons kosong."
        val type = contentType.lowercase(Locale.ROOT)
        val trimmed = body.trimStart().removePrefix("\uFEFF")
        val looksHtml = "html" in type || trimmed.startsWith("<!doctype html", ignoreCase = true) ||
            trimmed.startsWith("<html", ignoreCase = true)
        if (looksHtml) return "GAGAL VALIDASI: server mengembalikan halaman HTML, bukan data API terstruktur."
        val looksJson = "json" in type || trimmed.startsWith("{") || trimmed.startsWith("[")
        val looksXml = "xml" in type || trimmed.startsWith("<?xml") || trimmed.startsWith("<rss") || trimmed.startsWith("<feed")
        if (looksJson) {
            return if (StrictJsonParser(body).isValid()) {
                "STRUKTUR: sintaks JSON RFC 8259 valid. Ini hanya validasi sintaks, bukan verifikasi kebenaran atau kebaruan data."
            } else {
                "GAGAL VALIDASI: respons mengaku/terlihat seperti JSON tetapi tidak sesuai sintaks JSON ketat RFC 8259. Jangan perlakukan sebagai data terverifikasi."
            }
        }
        if (looksXml) {
            return try {
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
                parser.setInput(body.reader())
                while (parser.next() != XmlPullParser.END_DOCUMENT) { /* parse entire document */ }
                "STRUKTUR: XML well-formed. Ini hanya validasi sintaks, bukan verifikasi kebenaran atau kebaruan data."
            } catch (_: Exception) {
                "GAGAL VALIDASI: XML tidak well-formed. Jangan perlakukan sebagai data terverifikasi."
            }
        }
        if (type.startsWith("text/") || type.isEmpty()) {
            return "STRUKTUR: teks biasa; tidak ada skema untuk memvalidasi isi. Kebenaran dan kebaruan data belum diverifikasi."
        }
        return "GAGAL VALIDASI: format respons tidak didukung untuk pemeriksaan isi."
    }

    internal fun retrievalMetadata(
        now: Instant, url: String, status: Int, contentType: String, body: String,
        freshnessHeaders: ApiFreshness.Headers = ApiFreshness.Headers()
    ): String = buildString {
        appendLine("WAKTU PENGAMBILAN (UTC): ${DateTimeFormatter.ISO_INSTANT.format(now)}")
        appendLine("SUMBER YANG DIPANGGIL: ${redactUrl(url)}")
        appendLine("STATUS HTTP: $status")
        appendLine("TIPE KONTEN: ${contentType.ifBlank { "tidak dinyatakan server" }}")
        appendLine("KARAKTER YANG DIBACA (batas ${MAX_BODY}): ${body.length}")
        appendLine("CATATAN: waktu pengambilan bukan waktu pembaruan data oleh penyedia.")
        appendLine(ApiFreshness.report(now, body, freshnessHeaders))
    }

    /** Observation text for the model: the response of one GET, or the exact reason it failed. */
    fun get(open: () -> InputStream, url: String): String {
        var current = url.trim()
        var hops = 0
        while (hops++ <= MAX_REDIRECTS) {
            withLines(open) { refusal(it, current) }?.let { return "Ditolak: $it" }
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 12_000
                instanceFollowRedirects = false // every hop is checked against the list again
                setRequestProperty("User-Agent", "AndroidIsland/1.15 (public-apis client)")
                setRequestProperty("Accept", "application/json, text/plain;q=0.9, */*;q=0.1")
            }
            try {
                val status = conn.responseCode
                if (status in 300..399) {
                    val next = conn.getHeaderField("Location") ?: return "Gagal: redirect tanpa Location (HTTP $status)."
                    current = runCatching { URI(current).resolve(next).toString() }.getOrNull()
                        ?: return "Gagal: header Location tidak valid."
                    continue
                }
                val type = (conn.contentType ?: "").substringBefore(';').trim().lowercase()
                val safeUrl = redactUrl(current)
                if (status !in 200..299) {
                    val retryAfter = conn.getHeaderField("Retry-After")
                    // Do not echo error bodies: providers sometimes reflect credentials or request URLs.
                    return "GET $safeUrl -> ${httpFailure(status, retryAfter)}"
                }
                val readable = type.isEmpty() || "json" in type || "xml" in type || (type.startsWith("text/") && "html" !in type)
                if (!readable) return "GET $safeUrl -> HTTP $status, tipe konten $type tidak didukung (hanya JSON, XML, atau teks biasa)."
                val stream = conn.inputStream ?: return "GET $safeUrl -> HTTP $status tanpa isi."
                val (body, cut) = readBounded(stream)
                val assessment = assessBody(body, type)
                val providerEntry = withLines(open) { ApiDomainValidator.entryForUrl(it, current) }
                val domainResult = if (!assessment.startsWith("GAGAL VALIDASI")) {
                    ApiDomainValidator.assess(providerEntry?.category, body, type, providerEntry?.name)
                } else null
                val domainAssessment = domainResult?.asText()
                val gate = if (assessment.startsWith("GAGAL VALIDASI")) {
                    ApiPayloadGate.Decision(true, listOf(assessment.removePrefix("GAGAL VALIDASI:").trim()))
                } else {
                    ApiPayloadGate.assess(body, type, domainResult?.checks.orEmpty(), providerEntry?.name)
                }
                val finalGate = if (cut) {
                    ApiPayloadGate.Decision(true, gate.reasons + "Respons dipotong pada batas $MAX_BODY karakter; isi lengkap tidak tersedia.")
                } else gate
                val retrievedAt = Instant.now()
                return buildString {
                    appendLine("GET $safeUrl -> HTTP $status${if (type.isNotEmpty()) " ($type)" else ""}")
                    val freshnessHeaders = ApiFreshness.Headers(
                        date = conn.getHeaderField("Date"),
                        age = conn.getHeaderField("Age"),
                        cacheControl = conn.getHeaderField("Cache-Control"),
                        expires = conn.getHeaderField("Expires"),
                        lastModified = conn.getHeaderField("Last-Modified"),
                        etag = conn.getHeaderField("ETag")
                    )
                    append(retrievalMetadata(retrievedAt, current, status, type, body, freshnessHeaders))
                    appendLine(assessment)
                    if (domainAssessment != null) appendLine(domainAssessment)
                    if (cut) appendLine("PERINGATAN: respons dipotong di $MAX_BODY karakter; bagian setelahnya tidak ada.")
                    appendLine(finalGate.asText())
                    if (!finalGate.blocked) {
                        append(body.trim())
                    }
                }
            } catch (e: java.io.IOException) {
                return "Gagal: ${e.javaClass.simpleName}: ${e.message ?: "koneksi bermasalah"}"
            } finally {
                runCatching { conn.disconnect() }
            }
        }
        return "Gagal: terlalu banyak redirect."
    }

    private fun readBounded(stream: InputStream): Pair<String, Boolean> {
        val out = StringBuilder()
        val buffer = CharArray(2048)
        var over = false
        stream.bufferedReader(Charsets.UTF_8).use { reader ->
            while (true) {
                val n = reader.read(buffer)
                if (n < 0) break
                val room = MAX_BODY - out.length
                if (room > 0) out.append(buffer, 0, minOf(n, room))
                if (n > room) { over = true; break }
            }
        }
        return out.toString() to over
    }
}
