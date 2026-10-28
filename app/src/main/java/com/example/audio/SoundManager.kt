package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sin

class SoundManager(private val context: Context) {

    private val tag = "SoundManager"

    // Primary standalone player
    private var primaryPlayer: MediaPlayer? = null
    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId = _currentlyPlayingId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    // Mixer channels: channelId -> MediaPlayer
    private val mixerPlayers = mutableMapOf<Int, MediaPlayer>()
    private val channelVolumes = mutableMapOf<Int, Float>()
    private val channelMuted = mutableMapOf<Int, Boolean>()

    private val _masterVolume = MutableStateFlow(0.85f)
    val masterVolume = _masterVolume.asStateFlow()

    private val _mixerChannelPlaying = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val mixerChannelPlaying = _mixerChannelPlaying.asStateFlow()

    fun setMasterVolume(vol: Float) {
        _masterVolume.value = vol.coerceIn(0f, 1f)
        // Refresh all channel volumes
        mixerPlayers.forEach { (channelId, player) ->
            applyChannelVolume(channelId, player)
        }
        primaryPlayer?.let { player ->
            val v = _masterVolume.value
            player.setVolume(v, v)
        }
    }

    private fun applyChannelVolume(channelId: Int, player: MediaPlayer) {
        val isMuted = channelMuted[channelId] ?: false
        val chVol = channelVolumes[channelId] ?: 0.8f
        val actualVol = if (isMuted) 0f else (chVol * _masterVolume.value)
        try {
            player.setVolume(actualVol, actualVol)
        } catch (e: Exception) {
            Log.e(tag, "Error setting volume for channel $channelId", e)
        }
    }

    /**
     * Play or pause a sound by URL or generated procedural sound
     */
    fun playSound(soundId: String, audioUrl: String, onCompletion: () -> Unit = {}) {
        if (_currentlyPlayingId.value == soundId && _isPlaying.value) {
            stopSound()
            return
        }

        stopSound()
        _currentlyPlayingId.value = soundId
        _isPlaying.value = true

        try {
            if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
                primaryPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    setDataSource(context, Uri.parse(audioUrl))
                    val v = _masterVolume.value
                    setVolume(v, v)
                    setOnPreparedListener { mp ->
                        mp.start()
                    }
                    setOnCompletionListener {
                        _isPlaying.value = false
                        _currentlyPlayingId.value = null
                        onCompletion()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.w(tag, "MediaPlayer error ($what, $extra), falling back to synthesized gaming sound")
                        playSynthesizedSound(soundId)
                        true
                    }
                    prepareAsync()
                }
            } else if (audioUrl.startsWith("content://") || audioUrl.startsWith("file://")) {
                primaryPlayer = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(audioUrl))
                    val v = _masterVolume.value
                    setVolume(v, v)
                    setOnPreparedListener { it.start() }
                    setOnCompletionListener {
                        _isPlaying.value = false
                        _currentlyPlayingId.value = null
                        onCompletion()
                    }
                    prepareAsync()
                }
            } else {
                // Synthesized sound effect
                playSynthesizedSound(soundId)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to initialize player for url: $audioUrl, fallback to synth", e)
            playSynthesizedSound(soundId)
        }
    }

    fun stopSound() {
        try {
            primaryPlayer?.stop()
            primaryPlayer?.release()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping primary player", e)
        }
        primaryPlayer = null
        _isPlaying.value = false
        _currentlyPlayingId.value = null
    }

    fun replaySound(soundId: String, audioUrl: String) {
        stopSound()
        playSound(soundId, audioUrl)
    }

    // Mixer controls
    fun playMixerChannel(channelId: Int, audioUrl: String, defaultVol: Float = 0.8f) {
        stopMixerChannel(channelId)
        channelVolumes[channelId] = defaultVol

        val updatedMap = _mixerChannelPlaying.value.toMutableMap()
        updatedMap[channelId] = true
        _mixerChannelPlaying.value = updatedMap

        try {
            if (audioUrl.isNotBlank() && (audioUrl.startsWith("http") || audioUrl.startsWith("content") || audioUrl.startsWith("file"))) {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    setDataSource(context, Uri.parse(audioUrl))
                    isLooping = true
                    setOnPreparedListener { mp ->
                        applyChannelVolume(channelId, mp)
                        mp.start()
                    }
                    setOnErrorListener { _, _, _ ->
                        playSynthesizedMixerLoop(channelId)
                        true
                    }
                    prepareAsync()
                }
                mixerPlayers[channelId] = player
            } else {
                playSynthesizedMixerLoop(channelId)
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to start mixer channel $channelId, using synth loop", e)
            playSynthesizedMixerLoop(channelId)
        }
    }

    fun stopMixerChannel(channelId: Int) {
        try {
            mixerPlayers[channelId]?.stop()
            mixerPlayers[channelId]?.release()
        } catch (e: Exception) {
            Log.w(tag, "Error stopping mixer channel $channelId", e)
        }
        mixerPlayers.remove(channelId)

        val updatedMap = _mixerChannelPlaying.value.toMutableMap()
        updatedMap[channelId] = false
        _mixerChannelPlaying.value = updatedMap
    }

    fun setChannelVolume(channelId: Int, volume: Float) {
        channelVolumes[channelId] = volume.coerceIn(0f, 1f)
        mixerPlayers[channelId]?.let { applyChannelVolume(channelId, it) }
    }

    fun toggleChannelMute(channelId: Int) {
        val currentMuted = channelMuted[channelId] ?: false
        channelMuted[channelId] = !currentMuted
        mixerPlayers[channelId]?.let { applyChannelVolume(channelId, it) }
    }

    fun isChannelMuted(channelId: Int): Boolean = channelMuted[channelId] ?: false

    fun stopAll() {
        stopSound()
        mixerPlayers.keys.toList().forEach { stopMixerChannel(it) }
        _mixerChannelPlaying.value = emptyMap()
    }

    /**
     * Synthesizes audio procedurally using AudioTrack (Lasers, Explosions, Cyber Beeps, 8-bit Win)
     * so that if the network is disconnected or the audio URL is invalid, real audio ALWAYS plays!
     */
    private fun playSynthesizedSound(seedKey: String) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 44100
                val durationMs = 800
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                val hash = seedKey.hashCode()
                val baseFreq = 220.0 + (kotlin.math.abs(hash % 600))
                val soundType = kotlin.math.abs(hash % 4)

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    val sample = when (soundType) {
                        0 -> {
                            // Laser drop
                            val freq = baseFreq * (1.0 - (i.toDouble() / numSamples) * 0.7)
                            sin(2.0 * Math.PI * freq * time)
                        }
                        1 -> {
                            // High energy double beep
                            val freq = if (i < numSamples / 2) baseFreq else baseFreq * 1.5
                            sin(2.0 * Math.PI * freq * time)
                        }
                        2 -> {
                            // Bass gaming thump
                            val freq = (baseFreq / 2) * (1.0 - (i.toDouble() / numSamples))
                            sin(2.0 * Math.PI * freq * time) * (1.0 - (i.toDouble() / numSamples))
                        }
                        else -> {
                            // Synth chord arpeggio
                            val freq = baseFreq * (1 + (i / (sampleRate / 8)) % 3)
                            sin(2.0 * Math.PI * freq * time)
                        }
                    }
                    val decay = 1.0 - (i.toDouble() / numSamples)
                    buffer[i] = (sample * decay * Short.MAX_VALUE * _masterVolume.value).toInt().toShort()
                }

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(buffer.size * 2, minBufferSize))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 100)
                audioTrack.release()
                _isPlaying.value = false
                _currentlyPlayingId.value = null
            } catch (e: Exception) {
                Log.e(tag, "AudioTrack synthesis error", e)
                _isPlaying.value = false
                _currentlyPlayingId.value = null
            }
        }
    }

    private fun playSynthesizedMixerLoop(channelId: Int) {
        // Run a looping synth pulse for this channel
        val isMuted = channelMuted[channelId] ?: false
        if (isMuted) return
        playSynthesizedSound("mixer_channel_$channelId")
    }

    fun release() {
        stopAll()
    }
}
