package com.example.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.util.Locale

/**
 * Distinct audio item categories supported by the JNMF Radio DJ Scheduler.
 */
enum class RadioItemType(val storageKey: String) {
    MUSIC("MUSIC"),
    ADVERTISEMENT("ADVERTISEMENT"),
    NEWS("NEWS"),
    RADIO_PROGRAM("RADIO_PROGRAM"),
    ANNOUNCEMENT("ANNOUNCEMENT"),
    JINGLE("JINGLE"),
    INSERT("INSERT");

    companion object {
        fun fromKey(raw: String?): RadioItemType {
            if (raw.isNullOrBlank()) return MUSIC
            return entries.find { it.storageKey.equals(raw.trim(), ignoreCase = true) } ?: MUSIC
        }
    }
}

/**
 * Immutable UI / domain representation of a single scheduled item in a Radio DJ program.
 */
@Immutable
data class RadioScheduledItem(
    val id: Long,
    val scheduleId: Long,
    val orderIndex: Int,
    val itemType: RadioItemType,
    val songId: Long,
    val title: String,
    val subtitle: String,
    val contentUri: Uri,
    val durationMs: Long,
    val fileName: String = "",
    val albumArtUri: Uri? = null,
    val isAvailable: Boolean = true,
    val gainPercent: Int = 100
) {
    val formattedDuration: String = RadioTimelineCalculator.formatDurationCompact(durationMs)
}

/**
 * Computed broadcast timeline row for a single [RadioScheduledItem].
 * Times are explicitly estimated assuming continuous sequential playback from program start.
 */
@Immutable
data class RadioTimelineEntry(
    val item: RadioScheduledItem,
    val slotNumber: Int,
    val estimatedStartSecondsOfDay: Long,
    val estimatedEndSecondsOfDay: Long,
    val formattedStartTime: String,
    val formattedEndTime: String,
    val cumulativeOffsetMs: Long,
    val crossesMidnight: Boolean
)

/**
 * Computed summary of an entire Radio DJ program timeline.
 */
@Immutable
data class RadioTimelineSummary(
    val startTimeSecondsOfDay: Int,
    val formattedProgramStartTime: String,
    val formattedEstimatedFinishTime: String,
    val formattedTotalDuration: String,
    val totalDurationMs: Long,
    val entries: List<RadioTimelineEntry>,
    val musicCount: Int,
    val nonMusicInsertCount: Int,
    val missingItemCount: Int,
    val djModeEnabled: Boolean = false,
    val djTransitionSeconds: Int = 0,
    val totalOverlapMs: Long = 0L,
    val newsCount: Int = 0,
    val radioProgramCount: Int = 0
)

/**
 * Pure deterministic calculator for Radio DJ broadcast timelines.
 *
 * Example:
 * - Program Start = 17:00:00 (5:00:00 PM)
 * - Item 1 (2:30 = 150_000 ms, MUSIC) -> Start 5:00:00 PM, Est. End 5:02:30 PM
 * - If DJ Mode is 10s and Item 2 is MUSIC (2:00 = 120_000 ms):
 *   Item 2 starts at 5:02:20 PM (10s before Item 1 ends), Est. End 5:04:20 PM
 *   Total program runtime = 150s + 120s - 10s = 260s (4m 20s).
 * - If Item 2 is an ADVERTISEMENT, NEWS, or ANNOUNCEMENT:
 *   Item 2 starts only after Item 1 finishes completely (no crossfade into ads/announcements).
 */
object RadioTimelineCalculator {
    const val SECONDS_PER_DAY = 24 * 3600
    const val DEFAULT_START_TIME_SECONDS = 17 * 3600 // 5:00:00 PM

    fun normalizeStartTimeSeconds(secondsOfDay: Int): Int {
        val mod = secondsOfDay % SECONDS_PER_DAY
        return if (mod < 0) mod + SECONDS_PER_DAY else mod
    }

    fun hmsToSecondsOfDay(hour24: Int, minute: Int, second: Int = 0): Int {
        val h = hour24.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val s = second.coerceIn(0, 59)
        return h * 3600 + m * 60 + s
    }

    /**
     * Computes safe overlap duration (in milliseconds) between consecutive tracks.
     * Overlap occurs ONLY between two consecutive MUSIC items when DJ Mode is enabled.
     * Transitions involving Advertisements, News, Radio Programs, DJ Announcements, or Jingles
     * always yield 0 overlap so spoken content and inserts never suffer unintended overlap.
     */
    fun computeMusicOverlapMs(
        currDurationMs: Long,
        nextDurationMs: Long,
        djModeEnabled: Boolean,
        djTransitionSeconds: Int
    ): Long {
        if (!djModeEnabled || djTransitionSeconds <= 0) return 0L
        val targetMs = djTransitionSeconds * 1000L
        val maxSafeCurr = (currDurationMs - 1000L).coerceAtMost(currDurationMs / 2L).coerceAtLeast(0L)
        val maxSafeNext = (nextDurationMs - 1000L).coerceAtMost(nextDurationMs / 2L).coerceAtLeast(0L)
        val safeCap = minOf(maxSafeCurr, maxSafeNext)
        return minOf(targetMs, safeCap).coerceAtLeast(0L)
    }

    fun computeTimeline(
        startTimeSecondsOfDay: Int,
        items: List<RadioScheduledItem>,
        djModeEnabled: Boolean = false,
        djTransitionSeconds: Int = 0
    ): RadioTimelineSummary {
        val normalizedStartSec = normalizeStartTimeSeconds(startTimeSecondsOfDay)
        val baseStartMs = normalizedStartSec * 1000L

        var runningStartOffsetMs = 0L
        var musicCount = 0
        var nonMusicCount = 0
        var newsCount = 0
        var radioProgramCount = 0
        var missingCount = 0
        var totalOverlapMs = 0L

        val entries = ArrayList<RadioTimelineEntry>(items.size)
        items.forEachIndexed { index, item ->
            when (item.itemType) {
                RadioItemType.MUSIC -> musicCount++
                RadioItemType.NEWS -> {
                    newsCount++
                    nonMusicCount++
                }
                RadioItemType.RADIO_PROGRAM -> {
                    radioProgramCount++
                    nonMusicCount++
                }
                else -> nonMusicCount++
            }
            if (!item.isAvailable) {
                missingCount++
            }

            val safeDurationMs = item.durationMs.coerceAtLeast(0L)
            val itemStartTotalMs = baseStartMs + runningStartOffsetMs
            val itemEndTotalMs = itemStartTotalMs + safeDurationMs

            val startSecAbs = itemStartTotalMs / 1000L
            val endSecAbs = (itemEndTotalMs + 500L) / 1000L // round to nearest whole second for display

            val entry = RadioTimelineEntry(
                item = item,
                slotNumber = index + 1,
                estimatedStartSecondsOfDay = ((startSecAbs % SECONDS_PER_DAY) + SECONDS_PER_DAY) % SECONDS_PER_DAY,
                estimatedEndSecondsOfDay = ((endSecAbs % SECONDS_PER_DAY) + SECONDS_PER_DAY) % SECONDS_PER_DAY,
                formattedStartTime = formatClockTime12hWithSeconds(startSecAbs),
                formattedEndTime = formatClockTime12hWithSeconds(endSecAbs),
                cumulativeOffsetMs = runningStartOffsetMs,
                crossesMidnight = endSecAbs >= SECONDS_PER_DAY
            )
            entries.add(entry)

            // Overlap happens ONLY when the current item is MUSIC and the next item is MUSIC
            val nextItem = items.getOrNull(index + 1)
            val overlapToNextMs = if (nextItem != null && item.itemType == RadioItemType.MUSIC && nextItem.itemType == RadioItemType.MUSIC) {
                computeMusicOverlapMs(
                    currDurationMs = safeDurationMs,
                    nextDurationMs = nextItem.durationMs.coerceAtLeast(0L),
                    djModeEnabled = djModeEnabled,
                    djTransitionSeconds = djTransitionSeconds
                )
            } else {
                0L
            }

            totalOverlapMs += overlapToNextMs
            runningStartOffsetMs += (safeDurationMs - overlapToNextMs).coerceAtLeast(0L)
        }

        val totalDurationMs = runningStartOffsetMs
        val finishAbsSec = (baseStartMs + totalDurationMs + 500L) / 1000L
        return RadioTimelineSummary(
            startTimeSecondsOfDay = normalizedStartSec,
            formattedProgramStartTime = formatClockTime12hWithSeconds(normalizedStartSec.toLong()),
            formattedEstimatedFinishTime = formatClockTime12hWithSeconds(finishAbsSec),
            formattedTotalDuration = formatTotalProgramDuration(totalDurationMs),
            totalDurationMs = totalDurationMs,
            entries = entries,
            musicCount = musicCount,
            nonMusicInsertCount = nonMusicCount,
            missingItemCount = missingCount,
            djModeEnabled = djModeEnabled,
            djTransitionSeconds = djTransitionSeconds,
            totalOverlapMs = totalOverlapMs,
            newsCount = newsCount,
            radioProgramCount = radioProgramCount
        )
    }

    /**
     * Formats seconds since midnight (wrapping modulo 24h) as `h:mm:ss AM/PM`,
     * e.g. `5:00:00 PM`, `5:02:30 PM`, `12:05:00 AM`.
     */
    fun formatClockTime12hWithSeconds(totalSecondsSinceMidnight: Long): String {
        val wrapped = (((totalSecondsSinceMidnight % SECONDS_PER_DAY) + SECONDS_PER_DAY) % SECONDS_PER_DAY).toInt()
        val hour24 = wrapped / 3600
        val minute = (wrapped % 3600) / 60
        val second = wrapped % 60
        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(Locale.US, "%d:%02d:%02d %s", hour12, minute, second, amPm)
    }

    /**
     * Formats seconds since midnight as `h:mm AM/PM` when seconds are 0, or `h:mm:ss AM/PM` otherwise.
     */
    fun formatClockTimeShort(secondsOfDay: Int): String {
        val wrapped = normalizeStartTimeSeconds(secondsOfDay)
        val second = wrapped % 60
        if (second != 0) {
            return formatClockTime12hWithSeconds(wrapped.toLong())
        }
        val hour24 = wrapped / 3600
        val minute = (wrapped % 3600) / 60
        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
    }

    /**
     * Formats an audio clip duration as `m:ss` (e.g. `2:30`, `0:15`) or `h:mm:ss` if >= 1 hour.
     */
    fun formatDurationCompact(durationMs: Long): String {
        val totalSeconds = ((durationMs.coerceAtLeast(0L) + 500L) / 1000L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats total program duration clearly (e.g., `4m 55s` or `1h 12m 30s`).
     */
    fun formatTotalProgramDuration(totalDurationMs: Long): String {
        val totalSeconds = ((totalDurationMs.coerceAtLeast(0L) + 500L) / 1000L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L -> String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
            else -> String.format(Locale.US, "%dm %02ds", minutes, seconds)
        }
    }
}
