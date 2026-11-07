package com.example.data.analysis

import android.content.Context
import com.example.data.model.RemixCategory
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class DuplicateCandidatePair(
    val primarySong: Song,
    val candidateSong: Song,
    val similarityPercentage: Int, // e.g. 85%
    val matchReasons: List<String>
)

data class DuplicateScanState(
    val isScanning: Boolean = false,
    val progressPercent: Int = 0,
    val currentSongTitle: String = "",
    val duplicateCandidates: List<DuplicateCandidatePair> = emptyList(),
    val isCompleted: Boolean = false
)

class AudioAnalysisEngine(private val context: Context) {

    private val _scanState = MutableStateFlow(DuplicateScanState())
    val scanState: StateFlow<DuplicateScanState> = _scanState.asStateFlow()

    suspend fun analyzeDuplicates(songs: List<Song>, similarityThreshold: Int = 80) = withContext(Dispatchers.Default) {
        if (songs.size < 2) {
            _scanState.value = DuplicateScanState(isScanning = false, isCompleted = true)
            return@withContext
        }

        _scanState.value = DuplicateScanState(
            isScanning = true,
            progressPercent = 1,
            currentSongTitle = "Starting analysis...",
            duplicateCandidates = emptyList()
        )

        val candidates = mutableListOf<DuplicateCandidatePair>()
        val totalComparisons = (songs.size * (songs.size - 1)) / 2
        var comparisonsDone = 0

        for (i in 0 until songs.size) {
            val songA = songs[i]
            _scanState.value = _scanState.value.copy(
                currentSongTitle = songA.title,
                progressPercent = ((i.toFloat() / songs.size.toFloat()) * 99).toInt().coerceIn(1, 99)
            )

            for (j in i + 1 until songs.size) {
                val songB = songs[j]
                comparisonsDone++

                val similarity = calculateSimilarity(songA, songB)
                if (similarity.score >= similarityThreshold) {
                    candidates.add(
                        DuplicateCandidatePair(
                            primarySong = songA,
                            candidateSong = songB,
                            similarityPercentage = similarity.score,
                            matchReasons = similarity.reasons
                        )
                    )
                }
            }
        }

        _scanState.value = DuplicateScanState(
            isScanning = false,
            progressPercent = 100,
            currentSongTitle = "Scan completed",
            duplicateCandidates = candidates.sortedByDescending { it.similarityPercentage },
            isCompleted = true
        )
    }

    private data class SimilarityResult(val score: Int, val reasons: List<String>)

    private fun calculateSimilarity(a: Song, b: Song): SimilarityResult {
        val reasons = mutableListOf<String>()
        var points = 0
        var maxPoints = 100

        // 1. Duration match (within 3 seconds is heavily weighted)
        val durationDiff = abs(a.durationMs - b.durationMs)
        if (durationDiff <= 1500) {
            points += 35
            reasons.add("Exact playback duration match (±1.5s)")
        } else if (durationDiff <= 4000) {
            points += 25
            reasons.add("Close playback duration (±4s)")
        } else if (durationDiff <= 8000) {
            points += 10
        }

        // 2. Title & Artist String Token Similarity
        val cleanTitleA = cleanTrackName(a.title)
        val cleanTitleB = cleanTrackName(b.title)

        val titleLevenshtein = levenshteinSimilarity(cleanTitleA, cleanTitleB)
        val titleScore = (titleLevenshtein * 35).toInt()
        points += titleScore
        if (titleLevenshtein > 0.85f) {
            reasons.add("Near identical title tokens")
        }

        // 3. Artist similarity
        val cleanArtistA = a.artist.trim().lowercase()
        val cleanArtistB = b.artist.trim().lowercase()
        if (cleanArtistA != "unknown artist" && cleanArtistA == cleanArtistB) {
            points += 20
            reasons.add("Identical artist credit: ${a.artist}")
        } else if (levenshteinSimilarity(cleanArtistA, cleanArtistB) > 0.8f) {
            points += 15
        }

        // 4. Acoustic remix distinction: Check if one is marked Fullbeat and other Slow
        if (a.remixCategory != RemixCategory.UNCLASSIFIED &&
            b.remixCategory != RemixCategory.UNCLASSIFIED &&
            a.remixCategory != b.remixCategory
        ) {
            // They are distinct remix types (e.g. Fullbeat vs Slow remix) -> penalize candidate similarity
            points -= 30
            reasons.add("Distinguished as different remix arrangements (${a.remixCategory.displayName} vs ${b.remixCategory.displayName})")
        } else if (a.remixCategory == b.remixCategory && a.remixCategory != RemixCategory.UNCLASSIFIED) {
            points += 10
            reasons.add("Matching remix classification: ${a.remixCategory.displayName}")
        }

        val finalScore = points.coerceIn(0, 100)
        return SimilarityResult(finalScore, reasons)
    }

    private fun cleanTrackName(name: String): String {
        return name.lowercase()
            .replace(Regex("\\[.*?\\]|\\(.*?\\)"), "") // remove parenthetical tags
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun levenshteinSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val d = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) d[i][0] = i
        for (j in 0..s2.length) d[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                d[i][j] = min(
                    min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                    d[i - 1][j - 1] + cost
                )
            }
        }

        val maxLen = max(s1.length, s2.length)
        val distance = d[s1.length][s2.length]
        return 1.0f - (distance.toFloat() / maxLen.toFloat())
    }

    // Fullbeat vs. Slow Classification Heuristic
    fun classifySong(song: Song): RemixCategory {
        val lower = "${song.title} ${song.artist} ${song.album}".lowercase()

        // 1. Explicit keyword checks
        if (lower.contains("fullbeat") || lower.contains("full beat") || lower.contains("horeg") || lower.contains("sound horeg")) {
            return RemixCategory.FULLBEAT
        }
        if (lower.contains("slow") || lower.contains("santuy") || lower.contains("chill") || lower.contains("lo-fi") || lower.contains("lofi")) {
            return RemixCategory.SLOW
        }
        if (lower.contains("breakbeat") || lower.contains("funkot") || lower.contains("bounce")) {
            return RemixCategory.BREAKBEAT
        }
        if (lower.contains("remix") || lower.contains("jedag") || lower.contains("jedug") || lower.contains("dj ")) {
            return RemixCategory.INDO_REMIX
        }

        // 2. Duration / Density heuristic (Fullbeat tracks typically have higher compression density and typical 2:30-4:30 duration)
        val durationSec = song.durationMs / 1000
        val bytesPerSec = if (durationSec > 0) song.sizeBytes / durationSec else 0
        return if (bytesPerSec > 35000 && durationSec in 150..300) {
            RemixCategory.FULLBEAT
        } else if (durationSec > 330) {
            RemixCategory.SLOW
        } else {
            RemixCategory.UNCLASSIFIED
        }
    }
}
