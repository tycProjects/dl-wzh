package com.android.island.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

/**
 * Conservative, sector-aware sanity checks for common public API payloads.
 * These checks can detect malformed or suspicious fields; they cannot prove factual truth.
 */
internal object ApiDomainValidator {
    data class Result(val sector: String, val checks: List<String>, val provider: String? = null) {
        fun asText(): String = buildString {
            appendLine("PENYEDIA TERDETEKSI: ${provider?.takeIf { it.isNotBlank() } ?: "tidak teridentifikasi"}")
            appendLine("PEMERIKSAAN SEKTOR: $sector")
            appendLine("TINGKAT VALIDASI: pemeriksaan umum + aturan sektor yang tersedia; bukan kontrak resmi penuh")
            if (checks.isEmpty()) appendLine("Tidak ditemukan anomali dasar pada bidang yang diperiksa; kebenaran faktual tetap belum terverifikasi.")
            else checks.forEach { appendLine("- $it") }
            append("PENTING: respons ini tidak boleh diberi label 'terverifikasi benar'. Ini pemeriksaan struktur data, bukan verifikasi fakta. Skema resmi, kelengkapan, sumber pembanding, dan kebaruan data hanya dapat dinyatakan lolos jika benar-benar diuji.")
        }
    }

    private data class Field(val key: String, val value: Any?)

    fun entryForUrl(lines: Sequence<String>, url: String): PublicApis.Entry? {
        val host = runCatching { URI(url).host?.lowercase(Locale.ROOT)?.removePrefix("www.") }.getOrNull() ?: return null
        return lines.mapNotNull(PublicApis::parse).firstOrNull { entry ->
            val listedHost = runCatching { URI(entry.url).host?.lowercase(Locale.ROOT)?.removePrefix("www.") }.getOrNull()
            listedHost != null && (host == listedHost || host.endsWith(".$listedHost"))
        }
    }

    fun sectorForUrl(lines: Sequence<String>, url: String): String? = entryForUrl(lines, url)?.category

    fun assess(category: String?, body: String, contentType: String, provider: String? = null): Result {
        val sector = category?.trim().orEmpty().ifBlank { "Tidak diketahui" }
        val normalized = sector.lowercase(Locale.ROOT)
        val checks = mutableListOf<String>()
        val normalizedType = contentType.lowercase(Locale.ROOT)
        val looksJson = normalizedType.contains("json") || body.trimStart().startsWith("{") || body.trimStart().startsWith("[")
        if (!looksJson) {
            checks += "Pemeriksaan sektor berbasis JSON dilewati karena respons bukan JSON; struktur teks/XML tidak membuktikan makna atau kebenaran datanya."
            return Result(sector, checks, provider)
        }
        val root = runCatching { JSONObject(body) }.getOrNull()
            ?: runCatching { JSONArray(body) }.getOrNull()
            ?: return Result(sector, listOf("Struktur JSON tidak dapat dibaca sebagai objek/array untuk pemeriksaan sektor."), provider)
        val fields = mutableListOf<Field>()
        collect(root, fields)
        val byKey = fields.groupBy { it.key.lowercase(Locale.ROOT) }
        val allKeys = byKey.keys
        val providerId = provider.orEmpty().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "")

        // Universal checks applied to every JSON API response, independent of catalog category.
        when (root) {
            is JSONObject -> if (root.length() == 0) checks += "Objek JSON utama kosong; tidak ada data untuk ditafsirkan."
            is JSONArray -> if (root.length() == 0) checks += "Array JSON utama kosong; tidak ada hasil yang dikembalikan."
        }
        val resultContainers = fields.filter { it.key.lowercase(Locale.ROOT) in setOf("data", "results", "result", "items", "records", "entries") }
        resultContainers.forEach { field ->
            when (val value = field.value) {
                is JSONArray -> if (value.length() == 0) checks += "Kontainer '${field.key}' kosong; jangan menyimpulkan ada hasil."
                is JSONObject -> if (value.length() == 0) checks += "Kontainer '${field.key}' kosong; jangan menyimpulkan ada data."
                null, JSONObject.NULL -> checks += "Kontainer '${field.key}' bernilai null."
            }
        }
        val nullCount = fields.count { it.value == null || it.value === JSONObject.NULL }
        if (fields.size >= 4 && nullCount * 4 > fields.size) {
            checks += "Lebih dari 25% field yang diperiksa bernilai null; periksa apakah respons tidak lengkap atau field opsional memang kosong."
        }
        val successField = fields.firstOrNull { it.key.equals("success", true) || it.key.equals("ok", true) }
        if (successField?.value == false) checks += "Flag success/ok bernilai false walaupun HTTP mungkin sukses."
        val statusField = fields.firstOrNull { it.key.equals("status", true) }
        val statusValue = statusField?.value?.toString()?.lowercase(Locale.ROOT)
        if (statusValue != null && statusValue in setOf("error", "failed", "failure", "invalid")) {
            checks += "Field status menunjukkan error/failed/invalid; respons tidak boleh diperlakukan sebagai hasil sukses."
        }
        val timeFields = fields.filter { f ->
            val key = f.key.lowercase(Locale.ROOT)
            key in setOf("timestamp", "updated_at", "last_updated", "lastupdate", "published_at", "observed_at", "datetime") || key.endsWith("_timestamp")
        }
        timeFields.forEach { field ->
            val parsed = parseTimestamp(field.value)
            if (field.value != null && field.value !== JSONObject.NULL && parsed == null) {
                checks += "Field waktu '${field.key}' tidak dapat dikenali sebagai ISO-8601 atau epoch; kebaruan data tidak dapat diperiksa."
            } else if (parsed != null && parsed.isAfter(Instant.now().plusSeconds(300))) {
                checks += "Field waktu '${field.key}' berada lebih dari 5 menit di masa depan; periksa zona waktu atau nilai timestamp."
            }
        }
        if (timeFields.isEmpty()) {
            checks += "Tidak ditemukan field waktu pembaruan/publikasi yang umum; waktu pengambilan bukan bukti bahwa isi data masih terbaru."
        }

        // Provider-specific contracts are deliberately narrow and only enabled for known catalog entries.
        when (providerId) {
            "openmeteo" -> {
                if (allKeys.none { it == "latitude" } || allKeys.none { it == "longitude" }) checks += "Profil Open-Meteo: koordinat latitude/longitude tidak lengkap."
                if (allKeys.none { it == "timezone" }) checks += "Profil Open-Meteo: field timezone tidak ditemukan."
                if (allKeys.none { it == "current" || it == "current_weather" || it == "hourly" || it == "daily" }) checks += "Profil Open-Meteo: tidak ditemukan blok current/current_weather/hourly/daily yang umum."
                if (allKeys.none { it.contains("units") }) checks += "Profil Open-Meteo: metadata unit tidak ditemukan."
            }
            "frankfurter" -> {
                if (allKeys.none { it == "base" }) checks += "Profil Frankfurter: field base tidak ditemukan."
                if (allKeys.none { it == "date" }) checks += "Profil Frankfurter: field date tidak ditemukan."
                if (allKeys.none { it == "rates" }) checks += "Profil Frankfurter: field rates tidak ditemukan."
            }
            "restcountries" -> {
                if (allKeys.none { it == "name" }) checks += "Profil REST Countries: field name tidak ditemukan."
                if (allKeys.none { it == "cca2" }) checks += "Profil REST Countries: kode cca2 tidak ditemukan."
                if (allKeys.none { it == "cca3" }) checks += "Profil REST Countries: kode cca3 tidak ditemukan."
                if (allKeys.none { it == "population" }) checks += "Profil REST Countries: field population tidak ditemukan."
            }
        }

        val errorField = fields.firstOrNull { it.key.equals("error", true) || it.key.equals("errors", true) }
        if (errorField != null && errorField.value != null && errorField.value !== JSONObject.NULL && errorField.value.toString().isNotBlank() && errorField.value.toString() != "[]" && errorField.value.toString() != "{}") {
            checks += "Respons memuat field error/errors; HTTP sukses tidak otomatis berarti operasi API berhasil."
        }

        when {
            normalized.contains("weather") || normalized.contains("climate") -> {
                val numericWeather = fields.filter { f ->
                    val k = f.key.lowercase(Locale.ROOT)
                    (k.contains("temp") || k.contains("wind") || k.contains("precip") || k.contains("humidity")) && f.value is Number
                }
                numericWeather.forEach { f ->
                    val key = f.key.lowercase(Locale.ROOT)
                    val n = (f.value as Number).toDouble()
                    val bad = when {
                        key.contains("temp") -> n < -100.0 || n > 70.0
                        key.contains("wind") || key.contains("precip") -> n < 0.0
                        key.contains("humidity") -> n !in 0.0..100.0
                        else -> false
                    }
                    if (bad) checks += "Nilai '${f.key}' berada di luar rentang kewajaran dasar; periksa unit dan dokumentasi penyedia."
                }
                if (numericWeather.isNotEmpty() && allKeys.none { it.contains("unit") }) checks += "Field cuaca numerik ditemukan, tetapi metadata unit tidak terdeteksi; jangan menebak Celsius, Fahrenheit, km/jam, atau mm."
                if (allKeys.none { it in setOf("time", "timestamp", "date", "updated", "updated_at", "last_updated") || it.contains("time") }) checks += "Waktu pengamatan/pembaruan tidak terdeteksi; kebaruan data belum diketahui."
            }
            normalized.contains("currency") || normalized.contains("exchange") -> {
                val rates = fields.firstOrNull { it.key.equals("rates", true) && it.value is JSONObject }?.value as? JSONObject
                if (rates != null) {
                    if (rates.length() == 0) checks += "Objek rates kosong."
                    val keys = rates.keys()
                    while (keys.hasNext()) {
                        val code = keys.next()
                        val value = rates.opt(code)
                        if (!code.matches(Regex("[A-Z]{3}"))) checks += "Kode mata uang '$code' tidak mengikuti bentuk kode 3 huruf; verifikasi terhadap ISO 4217."
                        if (value !is Number || !value.toDouble().isFinite() || value.toDouble() <= 0.0) checks += "Nilai kurs untuk '$code' bukan angka positif yang valid."
                    }
                } else checks += "Objek rates standar tidak terdeteksi; bentuk respons kurs belum dapat diperiksa otomatis."
                if (allKeys.none { it in setOf("base", "base_code", "from", "source") }) checks += "Mata uang dasar/sumber kurs tidak terdeteksi."
                if (allKeys.none { it == "date" || it.contains("time") || it.contains("updated") }) checks += "Tanggal/waktu kurs tidak terdeteksi; jangan menyebutnya kurs terkini."
            }
            normalized.contains("geocod") || normalized.contains("location") || normalized.contains("maps") -> {
                val lat = fields.firstOrNull { it.key.lowercase(Locale.ROOT) in setOf("lat", "latitude") }?.value
                val lon = fields.firstOrNull { it.key.lowercase(Locale.ROOT) in setOf("lon", "lng", "longitude") }?.value
                if (lat is Number && lat.toDouble() !in -90.0..90.0) checks += "Latitude berada di luar rentang -90 hingga 90."
                if (lon is Number && lon.toDouble() !in -180.0..180.0) checks += "Longitude berada di luar rentang -180 hingga 180."
                if ((lat == null) != (lon == null)) checks += "Hanya satu dari pasangan koordinat latitude/longitude yang ditemukan."
                if (lat == null && lon == null) checks += "Pasangan koordinat tidak terdeteksi; endpoint mungkin mengembalikan format lain."
            }
            normalized.contains("finance") || normalized.contains("crypto") || normalized.contains("stock") || normalized.contains("bank") -> {
                val numericMeasures = fields.filter { f ->
                    val k = f.key.lowercase(Locale.ROOT)
                    (k.contains("price") || k.contains("rate") || k.contains("amount") || k.contains("balance") || k.contains("volume") || k.contains("market_cap")) && f.value is Number
                }
                numericMeasures.forEach { f ->
                    val n = (f.value as Number).toDouble()
                    if (!n.isFinite() || n < 0.0) checks += "Nilai '${f.key}' negatif atau tidak finite; periksa definisi field penyedia."
                }
                if (numericMeasures.isEmpty()) checks += "Field angka harga/kurs/saldo/volume tidak terdeteksi; struktur sektor keuangan belum dapat diperiksa."
                if (allKeys.none { it.contains("currency") || it == "symbol" || it.contains("unit") }) checks += "Mata uang atau unit nilai tidak terdeteksi; jangan menambahkan simbol/unit berdasarkan tebakan."
                if (allKeys.none { it.contains("time") || it.contains("date") || it.contains("updated") }) checks += "Waktu data pasar tidak terdeteksi; status real-time/terbaru belum terbukti."
            }
            normalized.contains("health") || normalized.contains("medical") -> {
                checks += "Data kesehatan memerlukan interpretasi sesuai konteks klinis dan sumber berwenang; pemeriksaan otomatis ini tidak menilai diagnosis, keamanan, atau rekomendasi medis."
                if (allKeys.none { it.contains("unit") }) checks += "Unit pengukuran kesehatan tidak terdeteksi; jangan menafsirkan angka tanpa unit dan rentang rujukan penyedia."
            }
            normalized.contains("country") || normalized.contains("government") -> {
                val population = fields.firstOrNull { it.key.equals("population", true) }?.value
                if (population != null && (population !is Number || !population.toDouble().isFinite() || population.toDouble() < 0.0)) checks += "Field population bukan angka non-negatif yang valid."
                if (allKeys.none { it in setOf("name", "official_name", "common") }) checks += "Nama negara tidak terdeteksi pada field umum."
                if (allKeys.none { it in setOf("cca2", "cca3", "iso2", "iso3", "alpha2", "alpha3") }) checks += "Kode negara standar tidak terdeteksi; identitas negara perlu diperiksa dari sumber."
            }
            normalized.contains("sports") || normalized.contains("sport") || normalized.contains("fitness") -> {
                if (allKeys.none { it.contains("date") || it.contains("time") || it.contains("timestamp") }) checks += "Tanggal/waktu pertandingan atau aktivitas tidak terdeteksi."
                if (allKeys.none { it.contains("team") || it.contains("player") || it.contains("athlete") }) checks += "Identitas tim/pemain/atlet tidak terdeteksi pada field umum."
            }
            normalized.contains("transport") || normalized.contains("vehicle") || normalized.contains("transit") -> {
                if (allKeys.none { it.contains("time") || it.contains("date") || it.contains("updated") }) checks += "Waktu jadwal/status transportasi tidak terdeteksi."
                if (allKeys.none { it.contains("status") || it.contains("delay") || it.contains("arrival") || it.contains("departure") }) checks += "Field status, keterlambatan, kedatangan, atau keberangkatan tidak terdeteksi."
            }
            else -> if (providerId !in setOf("openmeteo", "frankfurter", "restcountries")) {
                checks += "Belum ada aturan khusus untuk kategori ini; hanya validasi struktur JSON umum yang dilakukan."
            }
        }
        return Result(sector, checks.distinct(), provider)
    }

    private fun parseTimestamp(value: Any?): Instant? {
        if (value == null || value === JSONObject.NULL) return null
        if (value is Number) {
            val raw = value.toLong()
            return runCatching {
                if (raw >= 100_000_000_000L || raw <= -100_000_000_000L) Instant.ofEpochMilli(raw)
                else Instant.ofEpochSecond(raw)
            }.getOrNull()
        }
        val text = value.toString().trim()
        if (text.isEmpty()) return null
        return runCatching { Instant.parse(text) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull()
            ?: runCatching { LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC) }.getOrNull()
    }

    private fun collect(value: Any?, out: MutableList<Field>, key: String = "", depth: Int = 0) {
        if (depth > 24 || out.size >= 5_000) return
        when (value) {
            is JSONObject -> {
                val iterator = value.keys()
                while (iterator.hasNext() && out.size < 5_000) {
                    val childKey = iterator.next()
                    val child = value.opt(childKey)
                    out += Field(childKey, child)
                    if (child is JSONObject || child is JSONArray) collect(child, out, childKey, depth + 1)
                }
            }
            is JSONArray -> for (i in 0 until minOf(value.length(), 1_000)) {
                val child = value.opt(i)
                if (child is JSONObject || child is JSONArray) collect(child, out, key, depth + 1)
            }
        }
    }
}
