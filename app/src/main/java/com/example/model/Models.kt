package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.Locale

enum class BeatCategory(val displayName: String, val bpmRange: String, val tagColorHex: Long) {
    FULL_BEAT("Full Beat", "135+ BPM", 0xFF00FF66),
    FAST_BEAT("Fast Beat", "128 - 134 BPM", 0xFF39FF14),
    MEDIUM_BEAT("Medium Beat", "115 - 127 BPM", 0xFF00E676),
    SLOW_BEAT("Slow Beat", "95 - 114 BPM", 0xFF81C784),
    CHILL("Chill / Slow", "< 95 BPM", 0xFF80CBC4),
    UNKNOWN("Unknown", "--", 0xFF888888);

    val tagColor: Color get() = Color(tagColorHex)

    companion object {
        fun fromBpm(bpm: Int): BeatCategory = when {
            bpm <= 0 -> UNKNOWN
            bpm >= 135 -> FULL_BEAT
            bpm >= 128 -> FAST_BEAT
            bpm >= 115 -> MEDIUM_BEAT
            bpm >= 95 -> SLOW_BEAT
            else -> CHILL
        }

        fun fromName(name: String?): BeatCategory {
            if (name == null) return UNKNOWN
            return entries.firstOrNull {
                it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}

data class AudioAnalysisDetails(
    val bpm: Int,
    val beatCount: Int,
    val beatDensity: String,       // "High", "Medium", "Low", "Unknown"
    val rhythmConsistency: String, // "High", "Moderate", "Variable"
    val beatIntensity: String,     // "High", "Medium", "Low"
    val intensityOverTime: String, // "Heavy / Sustained", "Steady", "Variable"
    val energy: String,            // "High", "Medium", "Low"
    val kickBassActivity: String,  // "Heavy (Sub-Kick)", "Punchy", "Mild"
    val dynamicChanges: String,    // "Frequent Drops", "Moderate", "Smooth"
    val combinedScore: Int,        // 0 - 100
    val confidence: String         // "High", "Medium", "Low"
)

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uriString: String,
    val albumArtUri: String? = null,
    val isSampleTrack: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val bpm: Int = 128,
    val beatCategory: BeatCategory = BeatCategory.FAST_BEAT,
    val analysis: AudioAnalysisDetails? = null,
    val fileSize: Long = 0L,
    val filePath: String? = null,
    val isKicked: Boolean = false
) {
    val formattedDuration: String
        get() = formatDuration(durationMs)

    val formattedFileSize: String
        get() {
            if (fileSize <= 0) return "--"
            val mb = fileSize / (1024.0 * 1024.0)
            return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb)
            else String.format(Locale.US, "%d KB", fileSize / 1024)
        }

    val displayBeatDensity: String
        get() = analysis?.beatDensity ?: when (beatCategory) {
            BeatCategory.FULL_BEAT -> "High"
            BeatCategory.FAST_BEAT -> "High"
            BeatCategory.MEDIUM_BEAT -> "Medium"
            BeatCategory.SLOW_BEAT -> "Low"
            BeatCategory.CHILL -> "Low"
            BeatCategory.UNKNOWN -> "Unknown"
        }

    val displayBeatIntensity: String
        get() = analysis?.beatIntensity ?: when (beatCategory) {
            BeatCategory.FULL_BEAT -> "High"
            BeatCategory.FAST_BEAT -> "High"
            BeatCategory.MEDIUM_BEAT -> "Medium"
            BeatCategory.SLOW_BEAT -> "Low"
            BeatCategory.CHILL -> "Low"
            BeatCategory.UNKNOWN -> "Unknown"
        }

    val displayEnergy: String
        get() = analysis?.energy ?: when (beatCategory) {
            BeatCategory.FULL_BEAT -> "High"
            BeatCategory.FAST_BEAT -> "High"
            BeatCategory.MEDIUM_BEAT -> "Medium"
            BeatCategory.SLOW_BEAT -> "Low"
            BeatCategory.CHILL -> "Low"
            BeatCategory.UNKNOWN -> "Unknown"
        }

    companion object {
        fun formatDuration(durationMs: Long): String {
            if (durationMs <= 0) return "0:00"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }
}

data class KickedSong(
    val id: Long,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val bpm: Int = 128,
    val beatCategory: String = "Fast Beat",
    val fileSize: Long = 0L,
    val filePath: String? = null,
    val dateKicked: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    val formattedDuration: String
        get() = Song.formatDuration(durationMs)

    val formattedFileSize: String
        get() {
            if (fileSize <= 0) return "--"
            val mb = fileSize / (1024.0 * 1024.0)
            return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb)
            else String.format(Locale.US, "%d KB", fileSize / 1024)
        }
}

enum class DuplicateConfidence(val label: String, val badgeColorHex: Long) {
    VERY_LIKELY("Very likely duplicate", 0xFFFF3B30),
    POSSIBLE("Possible duplicate", 0xFFFFCC00),
    SIMILAR_SONG("Similar song", 0xFF00FF66);

    val badgeColor: Color get() = Color(badgeColorHex)
}

enum class SimilarityLevel(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}

data class PossibleDuplicateGroup(
    val groupId: String,
    val songA: Song,
    val songB: Song,
    val confidence: DuplicateConfidence,
    val nameSimilarity: SimilarityLevel,
    val beatSimilarity: SimilarityLevel,
    val durationSimilarity: String,
    val metadataSimilarity: SimilarityLevel,
    val detectedReason: String
)

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int = 0
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class EqualizerPresetType(val label: String) {
    INDO_REMIX("Indo Remix (Jedag Jedug)"),
    FULL_BEAT("Full Beat Drive"),
    BASS("Bass Boost"),
    BREAKBEAT("Club / Breakbeat"),
    ROCK("Rock"),
    POP("Pop"),
    CLASSICAL("Classical"),
    NORMAL("Normal"),
    CUSTOM("Custom");

    companion object {
        fun fromName(name: String): EqualizerPresetType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: INDO_REMIX
        }
    }
}

data class EqualizerBandState(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val minLevelMilliBels: Short,
    val maxLevelMilliBels: Short,
    val currentLevelMilliBels: Short
) {
    val formattedCenterFreq: String
        get() = if (centerFreqHz >= 1000) {
            "${centerFreqHz / 1000} kHz"
        } else {
            "$centerFreqHz Hz"
        }

    val currentGainDb: Float
        get() = currentLevelMilliBels / 100f
}

enum class AppUiColor(
    val id: String,
    val displayName: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val glowHex: Long,
    val borderGlowHex: Long
) {
    GREEN("green", "Green", 0xFF00FF66, 0xFF39FF14, 0x3300FF66, 0x6600FF66),
    BLUE("blue", "Blue", 0xFF0099FF, 0xFF38B6FF, 0x330099FF, 0x660099FF),
    RED("red", "Red", 0xFFFF2A4D, 0xFFFF5277, 0x33FF2A4D, 0x66FF2A4D),
    WHITE("white", "White", 0xFFEEEEEE, 0xFFFFFFFF, 0x33FFFFFF, 0x66FFFFFF),
    CYAN("cyan", "Cyan", 0xFF00F0FF, 0xFF54F4FF, 0x3300F0FF, 0x6600F0FF),
    VIOLET("violet", "Violet", 0xFFBD00FF, 0xFFD754FF, 0x33BD00FF, 0x66BD00FF);

    val primary: Color get() = Color(primaryHex)
    val secondary: Color get() = Color(secondaryHex)
    val glow: Color get() = Color(glowHex)
    val borderGlow: Color get() = Color(borderGlowHex)

    companion object {
        fun fromId(id: String?): AppUiColor =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: GREEN
    }
}

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    CEBUANO("ceb", "Cebuano", "Bisaya"),
    INDONESIAN("id", "Bahasa Indonesia", "Bahasa Indonesia");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

data class AppPreferences(
    val bassBoostStrength: Int = 600,
    val stereoWidthStrength: Int = 650,
    val depth3DStrength: Int = 500,
    val clarityStrength: Int = 600,
    val limiterEnabled: Boolean = true,
    val twsHeadsetMode: Boolean = true,
    val eqEnabled: Boolean = true,
    val eqPreset: EqualizerPresetType = EqualizerPresetType.INDO_REMIX,
    val pauseOnUnplug: Boolean = true,
    val uiColor: AppUiColor = AppUiColor.GREEN,
    val language: AppLanguage = AppLanguage.ENGLISH
)

// Audio Engine Status & Test models
data class AudioEngineStatusInfo(
    val playback: String = "JNMF INTERNAL PLAYER",
    val audioSource: String = "JNMF Library",
    val decoder: String = "ACTIVE",
    val dsp: String = "ACTIVE",
    val equalizer: String = "ACTIVE",
    val outputDevice: String = "Redmi Speaker / Bluetooth / TWS",
    val externalPlayer: String = "NOT USED",
    val audioEngineState: String = "ACTIVE",
    val processingMode: String = "JNMF Internal DSP",
    val systemEqStatus: String = "Internal Stream",
    val fallbackStatus: String = "ACTIVE",
    val sampleRate: String = "48000 Hz",
    val channelConfig: String = "Stereo (2.0)",
    val audioSessionId: Int = 0,
    val bassBoostActive: Boolean = true,
    val virtualizerActive: Boolean = true,
    val limiterActive: Boolean = true,
    val twsModeActive: Boolean = true
)

enum class AudioTestType(val title: String, val description: String) {
    TEST_BASS("Test Bass", "Sub-bass 40-120Hz boost validation"),
    TEST_EQ("Test EQ", "Multi-band frequency response curves"),
    TEST_STEREO_WIDTH("Test Stereo Width", "Binaural stereo widening engine"),
    TEST_3D("Test 3D", "Spatial depth & reverberation phase"),
    TEST_CLARITY("Test Clarity", "High-frequency treble harmonic exciter"),
    TEST_LIMITER("Test Limiter", "Zero-clipping peak limiter & safe headroom"),
    TEST_FULL_PROCESSING("Test Full Processing", "End-to-end Indo Remix audio chain"),
    TEST_STRICT_EQUALIZER_COMPATIBILITY("Strict Equalizer Compatibility", "Exhaustive multi-session hardware & DSP validation")
}

enum class AudioTestStatus(val label: String, val colorHex: Long) {
    PASS("PASS", 0xFF00FF66),
    UNSUPPORTED("UNSUPPORTED", 0xFFFF3B30),
    FALLBACK_ACTIVE("FALLBACK ACTIVE", 0xFFFFCC00)
}

data class EqualizerCompatibilityReport(
    val status: String = "VERIFIED",
    val mode: String = "JNMF Internal Audio Stream",
    val audioSessionId: Int = 0,
    val bandsCount: Int = 5,
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500,
    val isHardwareActive: Boolean = true,
    val isDspFallbackActive: Boolean = false,
    val filterChainDescription: String = "Low Shelf (≤250Hz) • Peaking Mid (1kHz) • High Shelf (≥4kHz)",
    val stepLogs: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
