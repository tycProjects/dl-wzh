package com.example.scanner

import com.example.model.DuplicateConfidence
import com.example.model.PossibleDuplicateGroup
import com.example.model.SimilarityLevel
import com.example.model.Song
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object DuplicateDetector {

    private val STRIP_REGEX = Regex(
        "(?i)\\b(remix|official|audio|copy|version|original|hq|hd|jedag\\s*jedug|full\\s*bass|breakbeat|slowed|reverb|tik\\s*tok|edit|cut|mix)\\b|[()\\[\\]{}_\\-–—.]|\\b\\d+\\b"
    )

    fun findPossibleDuplicates(songs: List<Song>): List<PossibleDuplicateGroup> {
        val groups = mutableListOf<PossibleDuplicateGroup>()
        val pairedKeys = mutableSetOf<String>()

        for (i in 0 until songs.size) {
            val songA = songs[i]
            for (j in i + 1 until songs.size) {
                val songB = songs[j]

                // Disallow pairing a song with itself
                if (songA.id == songB.id || songA.uriString == songB.uriString) continue

                val pairKey = if (songA.id < songB.id) "${songA.id}_${songB.id}" else "${songB.id}_${songA.id}"
                if (pairedKeys.contains(pairKey)) continue

                val candidate = evaluatePair(songA, songB, groups.size + 1)
                if (candidate != null) {
                    groups.add(candidate)
                    pairedKeys.add(pairKey)
                }
            }
        }

        return groups
    }

    private fun evaluatePair(songA: Song, songB: Song, groupIndex: Int): PossibleDuplicateGroup? {
        val normA = normalizeTitle(songA.title)
        val normB = normalizeTitle(songB.title)

        val nameSimScore = calculateStringSimilarity(normA, normB)
        val durationDiffMs = abs(songA.durationMs - songB.durationMs)
        val bpmDiff = abs(songA.bpm - songB.bpm)

        val artistSimScore = calculateStringSimilarity(
            songA.artist.lowercase(Locale.ROOT).trim(),
            songB.artist.lowercase(Locale.ROOT).trim()
        )

        val hasSize = songA.fileSize > 0 && songB.fileSize > 0
        val sizeDiffPercent = if (hasSize) {
            abs(songA.fileSize - songB.fileSize).toDouble() / max(songA.fileSize, songB.fileSize)
        } else {
            1.0
        }

        // Qualitative descriptors
        val nameSimLevel = when {
            nameSimScore >= 0.82 -> SimilarityLevel.HIGH
            nameSimScore >= 0.60 -> SimilarityLevel.MEDIUM
            else -> SimilarityLevel.LOW
        }

        val beatSimLevel = when {
            bpmDiff <= 2 -> SimilarityLevel.HIGH
            bpmDiff <= 5 -> SimilarityLevel.MEDIUM
            else -> SimilarityLevel.LOW
        }

        val durationDesc = when {
            durationDiffMs <= 1500 -> "Very similar (<2s)"
            durationDiffMs <= 5000 -> "Close (<5s)"
            durationDiffMs <= 15000 -> "Moderate (<15s)"
            else -> "Different (>15s)"
        }

        val metadataSimLevel = when {
            artistSimScore >= 0.80 && (hasSize && sizeDiffPercent < 0.10) -> SimilarityLevel.HIGH
            artistSimScore >= 0.65 || (hasSize && sizeDiffPercent < 0.25) -> SimilarityLevel.MEDIUM
            else -> SimilarityLevel.LOW
        }

        // Conservative Multi-Signal Classification
        val confidence: DuplicateConfidence? = when {
            // Signal 1: High name match + very close duration + matching metadata/size or beat
            durationDiffMs <= 2500 && nameSimScore >= 0.85 && (sizeDiffPercent < 0.15 || artistSimScore >= 0.70 || bpmDiff <= 2) -> {
                DuplicateConfidence.VERY_LIKELY
            }
            // Signal 2: Good name match + close duration + compatible BPM
            durationDiffMs <= 6000 && nameSimScore >= 0.70 && bpmDiff <= 4 -> {
                DuplicateConfidence.POSSIBLE
            }
            // Signal 3: Very similar normalized base title but different version/length
            nameSimScore >= 0.75 && artistSimScore >= 0.70 -> {
                DuplicateConfidence.SIMILAR_SONG
            }
            // Signal 4: Identical file size and duration with similar title
            hasSize && sizeDiffPercent < 0.03 && durationDiffMs <= 1000 && nameSimScore >= 0.55 -> {
                DuplicateConfidence.VERY_LIKELY
            }
            // Signal 5: Moderate match across all signals
            nameSimScore >= 0.62 && durationDiffMs <= 8000 && (artistSimScore >= 0.50 || bpmDiff <= 3) -> {
                DuplicateConfidence.POSSIBLE
            }
            else -> null
        }

        if (confidence == null) return null

        val reason = buildString {
            append("Detected via: ")
            if (nameSimScore >= 0.75) append("Similar title. ")
            if (durationDiffMs <= 2500) append("Duration matches within ${durationDiffMs / 1000}s. ")
            if (bpmDiff <= 2) append("Matching BPM ($bpmDiff diff). ")
            if (hasSize && sizeDiffPercent < 0.10) append("Similar file size. ")
        }.trim()

        val groupName = String.format(Locale.US, "Possible Duplicate Group %02d", groupIndex)

        return PossibleDuplicateGroup(
            groupId = groupName,
            songA = songA,
            songB = songB,
            confidence = confidence,
            nameSimilarity = nameSimLevel,
            beatSimilarity = beatSimLevel,
            durationSimilarity = durationDesc,
            metadataSimilarity = metadataSimLevel,
            detectedReason = reason
        )
    }

    private fun normalizeTitle(raw: String): String {
        return raw
            .replace(STRIP_REGEX, " ")
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
            .trim()
    }

    private fun calculateStringSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        // Substring containment check
        if (s1.contains(s2) || s2.contains(s1)) {
            val ratio = min(s1.length, s2.length).toDouble() / max(s1.length, s2.length)
            return 0.70 + (0.30 * ratio)
        }

        val distance = levenshtein(s1, s2)
        val maxLen = max(s1.length, s2.length)
        return (maxLen - distance).toDouble() / maxLen
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}
