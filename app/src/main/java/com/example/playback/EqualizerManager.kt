package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BandInfo(
    val index: Short,
    val centerFreqHz: Int,
    val minLevelMilliBel: Short,
    val maxLevelMilliBel: Short,
    val currentLevelMilliBel: Short
) {
    val displayFrequency: String
        get() = if (centerFreqHz >= 1000) "${centerFreqHz / 1000} kHz" else "$centerFreqHz Hz"
}

data class EqUiState(
    val isEnabled: Boolean = true,
    val audioSessionId: Int = 0,
    val isHardwareSupported: Boolean = true,
    val bands: List<BandInfo> = emptyList(),
    val bassStrength: Int = 300, // 0 to 1000
    val virtualizerStrength: Int = 200, // 0 to 1000
    val clarityStrength: Int = 150, // 0 to 1000
    val currentPreset: String = "Indo Fullbeat",
    val availablePresets: List<String> = listOf(
        "Indo Fullbeat",
        "Bass Horeg",
        "Slow Remix",
        "Club Remix",
        "Vocal Clarity",
        "Flat"
    )
)

class EqualizerManager {
    private val TAG = "JNMF_EqualizerManager"

    @Volatile
    private var equalizer: Equalizer? = null
    @Volatile
    private var bassBoost: BassBoost? = null
    @Volatile
    private var virtualizer: Virtualizer? = null

    private val _uiState = MutableStateFlow(EqUiState())
    val uiState: StateFlow<EqUiState> = _uiState.asStateFlow()

    private var currentSessionId = 0

    init {
        initFallbackBands()
    }

    @Synchronized
    fun attachAudioSession(sessionId: Int) {
        if (sessionId <= 0) {
            Log.w(TAG, "Invalid audioSessionId: $sessionId. Awaiting valid ExoPlayer session.")
            return
        }

        if (currentSessionId == sessionId && equalizer != null) {
            Log.d(TAG, "AudioSession $sessionId already attached.")
            return
        }

        Log.i(TAG, "Attaching audio effects directly to JNMF ExoPlayer session: $sessionId")
        releaseEffects()
        currentSessionId = sessionId

        try {
            // Instantiate directly on active ExoPlayer AudioTrack session
            equalizer = Equalizer(0, sessionId).apply {
                try {
                    enabled = _uiState.value.isEnabled
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set equalizer enabled initially: ${e.message}")
                }
            }

            try {
                bassBoost = BassBoost(0, sessionId).apply {
                    enabled = _uiState.value.isEnabled
                    if (strengthSupported) {
                        setStrength(_uiState.value.bassStrength.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "BassBoost unsupported on this audio session: ${e.message}")
                bassBoost = null
            }

            try {
                virtualizer = Virtualizer(0, sessionId).apply {
                    enabled = _uiState.value.isEnabled
                    if (strengthSupported) {
                        setStrength(_uiState.value.virtualizerStrength.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer unsupported on this audio session: ${e.message}")
                virtualizer = null
            }

            loadBandsFromHardware()
            applyPresetInternal(_uiState.value.currentPreset)

            _uiState.value = _uiState.value.copy(
                audioSessionId = sessionId,
                isHardwareSupported = true
            )
            Log.i(TAG, "Successfully attached hardware equalizer & DSP to session $sessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing hardware equalizer on session $sessionId: ${e.message}", e)
            _uiState.value = _uiState.value.copy(
                audioSessionId = sessionId,
                isHardwareSupported = false
            )
            initFallbackBands()
        }
    }

    private fun loadBandsFromHardware() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val range = eq.bandLevelRange
            if (numBands <= 0 || range == null || range.size < 2) {
                initFallbackBands()
                return
            }

            val minRange = range[0]
            val maxRange = range[1]
            val bandList = mutableListOf<BandInfo>()

            for (i in 0 until numBands) {
                val bandIdx = i.toShort()
                val freq = try {
                    eq.getCenterFreq(bandIdx) / 1000 // Convert mHz to Hz
                } catch (e: Exception) {
                    when (i) {
                        0 -> 60
                        1 -> 230
                        2 -> 910
                        3 -> 3600
                        else -> 14000
                    }
                }
                val level = try {
                    eq.getBandLevel(bandIdx)
                } catch (e: Exception) {
                    0.toShort()
                }
                bandList.add(
                    BandInfo(
                        index = bandIdx,
                        centerFreqHz = freq,
                        minLevelMilliBel = minRange,
                        maxLevelMilliBel = maxRange,
                        currentLevelMilliBel = level
                    )
                )
            }
            if (bandList.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(bands = bandList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bands from hardware: ${e.message}")
            initFallbackBands()
        }
    }

    private fun initFallbackBands() {
        // Standard 5-band fallback representation (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
        val defaultFrequencies = listOf(60, 230, 910, 3600, 14000)
        val fallbackBands = defaultFrequencies.mapIndexed { idx, freq ->
            BandInfo(
                index = idx.toShort(),
                centerFreqHz = freq,
                minLevelMilliBel = -1500,
                maxLevelMilliBel = 1500,
                currentLevelMilliBel = 0
            )
        }
        _uiState.value = _uiState.value.copy(bands = fallbackBands)
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isEnabled = enabled)
        try {
            equalizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling equalizer enabled: ${e.message}")
        }
        try {
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling bassBoost enabled: ${e.message}")
        }
        try {
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling virtualizer enabled: ${e.message}")
        }
    }

    fun setBandLevel(bandIndex: Short, milliBel: Short) {
        val updatedBands = _uiState.value.bands.map { band ->
            if (band.index == bandIndex) band.copy(currentLevelMilliBel = milliBel) else band
        }
        _uiState.value = _uiState.value.copy(bands = updatedBands, currentPreset = "Custom")

        try {
            if (_uiState.value.isEnabled) {
                equalizer?.setBandLevel(bandIndex, milliBel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting band $bandIndex level: ${e.message}")
        }
    }

    fun setBassStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _uiState.value = _uiState.value.copy(bassStrength = clamped)
        try {
            if (_uiState.value.isEnabled && bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting bass strength: ${e.message}")
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _uiState.value = _uiState.value.copy(virtualizerStrength = clamped)
        try {
            if (_uiState.value.isEnabled && virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting virtualizer strength: ${e.message}")
        }
    }

    fun setClarityStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _uiState.value = _uiState.value.copy(clarityStrength = clamped)
        // Adjust higher bands slightly for clarity enhancement
        val bands = _uiState.value.bands
        if (bands.isNotEmpty()) {
            val highBand = bands.last()
            val clarityBoost = (clamped * 0.8f).toInt().toShort()
            setBandLevel(highBand.index, clarityBoost)
        }
    }

    fun applyPreset(presetName: String) {
        _uiState.value = _uiState.value.copy(currentPreset = presetName)
        applyPresetInternal(presetName)
    }

    private fun applyPresetInternal(presetName: String) {
        val bands = _uiState.value.bands
        if (bands.isEmpty()) return

        // Levels in milliBels (-1500 to +1500)
        val levels: List<Short> = when (presetName) {
            "Indo Fullbeat" -> listOf(700, 500, -100, 400, 600)
            "Bass Horeg" -> listOf(1000, 800, 0, 200, 400)
            "Slow Remix" -> listOf(400, 300, 100, 200, 300)
            "Club Remix" -> listOf(600, 300, -200, 300, 500)
            "Vocal Clarity" -> listOf(-300, 100, 600, 700, 400)
            "Flat" -> listOf(0, 0, 0, 0, 0)
            else -> return
        }

        val updatedBands = bands.mapIndexed { idx, band ->
            val targetLevel = if (idx < levels.size) levels[idx] else 0.toShort()
            val clamped = targetLevel.coerceIn(band.minLevelMilliBel, band.maxLevelMilliBel)
            try {
                if (_uiState.value.isEnabled) {
                    equalizer?.setBandLevel(band.index, clamped)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Preset apply error: ${e.message}")
            }
            band.copy(currentLevelMilliBel = clamped)
        }

        val bass = when (presetName) {
            "Indo Fullbeat" -> 700
            "Bass Horeg" -> 950
            "Slow Remix" -> 400
            "Club Remix" -> 600
            "Vocal Clarity" -> 100
            else -> 0
        }

        val virt = when (presetName) {
            "Indo Fullbeat" -> 500
            "Bass Horeg" -> 350
            "Slow Remix" -> 700
            "Club Remix" -> 400
            "Vocal Clarity" -> 200
            else -> 0
        }

        setBassStrength(bass)
        setVirtualizerStrength(virt)

        _uiState.value = _uiState.value.copy(bands = updatedBands)
    }

    @Synchronized
    fun releaseEffects() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing equalizer: ${e.message}")
        }
        equalizer = null

        try {
            bassBoost?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing bassBoost: ${e.message}")
        }
        bassBoost = null

        try {
            virtualizer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing virtualizer: ${e.message}")
        }
        virtualizer = null

        currentSessionId = 0
        Log.d(TAG, "Audio effects safely released.")
    }
}
