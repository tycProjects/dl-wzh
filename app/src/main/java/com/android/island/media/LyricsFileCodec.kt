package com.android.island.media

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
 * Reader/writer for the public Lyricsfile 1.0 draft.
 *
 * The format is deliberately kept separate from the playback model: the player can
 * consume line cues cheaply while the full document retains word timing and metadata.
 */
data class LyricsDocument(
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long? = null,
    val language: String? = null,
    val instrumental: Boolean = false,
    val lines: List<LyricCue> = emptyList(),
    val plain: String? = null,
    val source: String = "lyricsfile"
)

data class LyricCue(
    val text: String,
    val startMs: Long,
    val endMs: Long? = null,
    val words: List<LyricWord> = emptyList()
)

data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long? = null
)

data class LyricsValidation(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val score: Int
)

object LyricsFileCodec {
    private const val MAX_BYTES = 1_000_000
    private const val MAX_CUES = 5_000
    private const val MAX_WORDS = 30_000

    fun parse(yamlText: String): LyricsDocument {
        require(yamlText.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Lyricsfile terlalu besar" }
        val options = LoaderOptions().apply {
            // Use explicit Java setters: Kotlin synthetic property assignment can
            // resolve to SnakeYAML's private backing field on some compiler versions.
            setAllowDuplicateKeys(false)
            setMaxAliasesForCollections(0)
            setCodePointLimit(MAX_BYTES)
        }
        val root = Yaml(SafeConstructor(options)).load<Any?>(yamlText)
        val top = root.asStringMap("root")
        require(top["version"] == "1.0") { "Versi Lyricsfile tidak didukung" }
        val meta = top["metadata"].asStringMap("metadata")
        val title = meta.requiredString("title")
        val artist = meta.requiredString("artist")
        val duration = meta.optionalLong("duration_ms")
        require(duration == null || duration >= 0) { "metadata.duration_ms tidak boleh negatif" }
        val instrumental = meta["instrumental"] as? Boolean ?: false
        val plain = top["plain"] as? String
        val rawLines = top["lines"] as? List<*> ?: emptyList<Any?>()
        require(rawLines.size <= MAX_CUES) { "Jumlah baris lirik melewati batas" }
        val lines = rawLines.mapIndexed { index, raw ->
            val item = raw.asStringMap("lines[$index]")
            val text = item.requiredString("text")
            val start = item.requiredLong("start_ms")
            val end = item.optionalLong("end_ms")
            require(start >= 0 && (end == null || end >= start)) { "Timestamp baris $index tidak valid" }
            val rawWords = item["words"] as? List<*> ?: emptyList<Any?>()
            val words = rawWords.mapIndexed { wi, wordRaw ->
                val word = wordRaw.asStringMap("lines[$index].words[$wi]")
                val wordText = word.requiredString("text")
                val wordStart = word.requiredLong("start_ms")
                val wordEnd = word.optionalLong("end_ms")
                require(wordStart >= 0 && (wordEnd == null || wordEnd >= wordStart)) {
                    "Timestamp kata $wi pada baris $index tidak valid"
                }
                LyricWord(wordText, wordStart, wordEnd)
            }
            LyricCue(text, start, end, words)
        }
        require(lines.sumOf { it.words.size } <= MAX_WORDS) { "Jumlah segmen kata melewati batas" }
        require(!instrumental || (lines.isEmpty() && plain.isNullOrBlank())) {
            "Lagu instrumental tidak boleh memiliki lirik"
        }
        require(instrumental || lines.isNotEmpty() || !plain.isNullOrBlank()) {
            "Lyricsfile harus memiliki lines atau plain"
        }
        val document = LyricsDocument(
            title, artist, meta["album"] as? String, duration,
            meta["language"] as? String, instrumental, lines, plain
        )
        val validation = validate(document)
        require(validation.errors.isEmpty()) { validation.errors.joinToString("; ") }
        return document
    }

    fun validate(document: LyricsDocument, actualDurationMs: Long? = document.durationMs): LyricsValidation {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val duration = actualDurationMs?.takeIf { it > 0 }
        if (document.instrumental && document.lines.isNotEmpty()) errors += "Track instrumental memiliki baris lirik"
        if (document.lines.size > MAX_CUES) errors += "Jumlah baris terlalu banyak"
        val seen = HashSet<Pair<Long, String>>()
        var previousStart = -1L
        var score = 100
        document.lines.forEachIndexed { i, line ->
            if (line.startMs < 0 || (line.endMs != null && line.endMs < line.startMs)) {
                errors += "Rentang waktu baris ${i + 1} tidak valid"
            }
            if (line.startMs < previousStart) warnings += "Urutan timestamp baris tidak monoton"
            previousStart = maxOf(previousStart, line.startMs)
            if (!seen.add(line.startMs to line.text)) {
                warnings += "Baris duplikat pada ${line.startMs} ms"
                score -= 8
            }
            if (duration != null && line.startMs > duration + 2_000L) {
                warnings += "Baris ${i + 1} berada di luar durasi lagu"
                score -= 12
            }
            if (duration != null && line.endMs != null && line.endMs > duration + 2_000L) {
                warnings += "Akhir baris ${i + 1} melewati durasi lagu"
                score -= 8
            }
            if (line.words.isNotEmpty()) {
                val joined = line.words.joinToString("") { it.text }
                if (normalize(joined) != normalize(line.text)) {
                    warnings += "Teks kata pada baris ${i + 1} tidak sama dengan teks baris"
                    score -= 12
                }
                var last = -1L
                line.words.forEach { word ->
                    if (word.startMs < last) warnings += "Urutan kata pada baris ${i + 1} tidak monoton"
                    last = maxOf(last, word.startMs)
                    if (word.startMs < line.startMs - 100L) warnings += "Kata mendahului awal baris ${i + 1}"
                    if (line.endMs != null && word.startMs > line.endMs + 100L) warnings += "Kata melewati akhir baris ${i + 1}"
                }
            }
        }
        if (document.lines.size < 3 && !document.instrumental) {
            warnings += "Data tersinkron terlalu sedikit untuk menilai kualitas timing"
            score -= 10
        }
        if (document.lines.isNotEmpty() && duration != null) {
            val last = document.lines.maxOf { it.startMs }
            if (last > duration) score -= 15
            // STAGE 77: coverage is only a soft hint now. Timestamped lyrics that start late or end
            // early are partial but still useful, so they lose a few points and are never rejected.
            val first = document.lines.minOf { it.startMs }
            if (first > duration * 60 / 100) {
                warnings += "Lirik baru mulai di ${(first / 1000)}s dari lagu berdurasi ${(duration / 1000)}s"
                score -= 5
            }
            if (last < duration * 35 / 100) {
                warnings += "Lirik berakhir di ${(last / 1000)}s dari lagu berdurasi ${(duration / 1000)}s"
                score -= 5
            }
        }
        return LyricsValidation(errors.isEmpty(), errors, warnings.distinct(), score.coerceIn(0, 100))
    }

    /** Canonical, loss-minimizing YAML output for the supported Lyricsfile 1.0 fields. */
    fun encode(document: LyricsDocument): String {
        val check = validate(document)
        require(check.errors.isEmpty()) { check.errors.joinToString("; ") }
        val out = StringBuilder()
        out.append("version: '1.0'\nmetadata:\n")
        out.append("  title: ").append(quote(document.title)).append('\n')
        out.append("  artist: ").append(quote(document.artist)).append('\n')
        document.album?.let { out.append("  album: ").append(quote(it)).append('\n') }
        document.durationMs?.let { out.append("  duration_ms: ").append(it).append('\n') }
        document.language?.let { out.append("  language: ").append(quote(it)).append('\n') }
        if (document.instrumental) out.append("  instrumental: true\n")
        if (document.lines.isNotEmpty()) {
            out.append("lines:\n")
            document.lines.forEach { line ->
                out.append("  - text: ").append(quote(line.text)).append('\n')
                out.append("    start_ms: ").append(line.startMs).append('\n')
                line.endMs?.let { out.append("    end_ms: ").append(it).append('\n') }
                if (line.words.isNotEmpty()) {
                    out.append("    words:\n")
                    line.words.forEach { word ->
                        out.append("      - text: ").append(quote(word.text)).append('\n')
                        out.append("        start_ms: ").append(word.startMs).append('\n')
                        word.endMs?.let { out.append("        end_ms: ").append(it).append('\n') }
                    }
                }
            }
        }
        document.plain?.let {
            out.append("plain: ").append(quote(it)).append('\n')
        }
        return out.toString()
    }

    private fun quote(value: String): String = "\"" + value
        .replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "\""

    private fun normalize(value: String) = value.filterNot { it.isWhitespace() }.lowercase()

    private fun Any?.asStringMap(where: String): Map<String, Any?> {
        val map = this as? Map<*, *> ?: error("$where harus berupa mapping")
        return map.entries.associate { (k, v) ->
            (k as? String ?: error("Key $where harus berupa string")) to v
        }
    }

    private fun Map<String, Any?>.requiredString(key: String): String =
        (this[key] as? String)?.takeIf { it.isNotBlank() } ?: error("$key wajib berupa string")

    private fun Map<String, Any?>.requiredLong(key: String): Long =
        (this[key] as? Number)?.toLong() ?: error("$key wajib berupa integer")

    private fun Map<String, Any?>.optionalLong(key: String): Long? =
        (this[key] as? Number)?.toLong()
}
