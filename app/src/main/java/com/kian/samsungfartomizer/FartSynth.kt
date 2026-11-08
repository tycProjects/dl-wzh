package com.kian.samsungfartomizer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.tanh
import kotlin.random.Random

/** Describes one synthesized sound. Everything is generated in code, so no audio files are needed. */
data class Preset(
    val name: String,
    val emoji: String,
    val startHz: Float,
    val endHz: Float,
    val burstMs: Int,
    val bursts: Int = 1,
    val gapMs: Int = 0,
    val jitter: Float = 0.35f,
    val brightness: Float = 0.35f
)

val PRESETS = listOf(
    Preset("Classic", "💨", 110f, 70f, 700),
    Preset("Squeaker", "🎈", 600f, 300f, 500, jitter = 0.15f, brightness = 0.6f),
    Preset("Long Rumble", "🌋", 70f, 45f, 1800, jitter = 0.5f, brightness = 0.2f),
    Preset("Machine Gun", "🔫", 130f, 110f, 110, bursts = 7, gapMs = 40, jitter = 0.3f),
    Preset("Wet One", "💦", 90f, 60f, 900, jitter = 0.8f, brightness = 0.3f),
    Preset("Tiny Toot", "🎺", 900f, 700f, 180, jitter = 0.1f, brightness = 0.7f),
    Preset("Triple Tap", "🥁", 100f, 70f, 220, bursts = 3, gapMs = 90),
    Preset("Galaxy Blast", "🌌", 150f, 40f, 1400, jitter = 0.45f, brightness = 0.25f)
)

object FartSynth {
    const val SAMPLE_RATE = 22050

    fun render(p: Preset, intensity: Float): ShortArray {
        val lenScale = 0.6f + 0.8f * intensity
        val gain = 0.4f + 0.6f * intensity
        val burstSamples = (SAMPLE_RATE * p.burstMs * lenScale / 1000f).toInt().coerceAtLeast(200)
        val gapSamples = SAMPLE_RATE * p.gapMs / 1000
        val total = (burstSamples + gapSamples) * p.bursts
        val out = ShortArray(total)
        val rnd = Random(System.nanoTime())

        var idx = 0
        var lp = 0f
        var wobble = 0f
        for (b in 0 until p.bursts) {
            var phase = 0f
            for (i in 0 until burstSamples) {
                val t = i / burstSamples.toFloat()
                wobble += 0.04f * ((rnd.nextFloat() * 2f - 1f) - wobble)
                val f = (p.startHz + (p.endHz - p.startHz) * t) * (1f + p.jitter * wobble * 2f)
                phase += max(f, 20f) / SAMPLE_RATE
                if (phase >= 1f) phase -= 1f

                val saw = 2f * phase - 1f
                val tone = tanh(3f * saw)
                val noise = (rnd.nextFloat() * 2f - 1f) * 0.25f
                val raw = tone * 0.8f + noise

                lp += p.brightness * (raw - lp)

                val attack = min(1f, t * 25f)
                val decay = (1f - t).pow(0.6f)
                val s = lp * attack * decay * gain
                out[idx++] = (s.coerceIn(-1f, 1f) * 32767f * 0.9f).toInt().toShort()
            }
            idx += gapSamples
        }
        return out
    }
}

class FartPlayer {
    private var track: AudioTrack? = null

    fun play(p: Preset, intensity: Float) {
        stop()
        val data = FartSynth.render(p, intensity)
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(FartSynth.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(data.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        t.write(data, 0, data.size)
        t.play()
        track = t
    }

    fun stop() {
        track?.let {
            try { it.stop() } catch (_: Exception) {}
            it.release()
        }
        track = null
    }
}
