package com.meloza.app.playback

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Reads the spectrum of the audio ExoPlayer is playing and turns it into
 * [BARS] levels between 0 and 1. Bass is in the centre and treble at the edges,
 * and the bass gets extra gain, so the centre bars react most to the beat.
 * If the device does not support the visualizer, the levels simply stay at 0.
 */
object AudioVisualizer {
    const val BARS = 28

    private val _levels = MutableStateFlow(FloatArray(BARS))
    val levels: StateFlow<FloatArray> = _levels.asStateFlow()

    private var visualizer: Visualizer? = null
    private val smooth = FloatArray(BARS)

    fun bind(audioSessionId: Int) {
        release()
        if (audioSessionId == 0) return
        runCatching {
            val v = Visualizer(audioSessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(vis: Visualizer?, waveform: ByteArray?, samplingRate: Int) = Unit
                    override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) process(fft)
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                false,
                true,
            )
            v.enabled = true
            visualizer = v
        }
    }

    fun release() {
        runCatching {
            visualizer?.enabled = false
            visualizer?.release()
        }
        visualizer = null
        smooth.fill(0f)
        _levels.value = FloatArray(BARS)
    }

    private fun process(fft: ByteArray) {
        // fft layout: [0] = DC, [1] = Nyquist, then (re, im) pairs for each bin.
        val bins = fft.size / 2
        val mag = FloatArray(bins)
        mag[0] = abs(fft[0].toFloat())
        for (k in 1 until bins) {
            mag[k] = hypot(fft[2 * k].toFloat(), fft[2 * k + 1].toFloat())
        }

        // The top of the spectrum is mostly silence, so only the lower 30% is used.
        val lo = 1.0
        val hi = (bins * 0.3).coerceAtLeast(4.0)
        val half = BARS / 2
        val target = FloatArray(BARS)
        for (i in 0 until BARS) {
            val pos = abs(i - half)                       // 0 at the centre (bass), half at the edges
            val f0 = lo * (hi / lo).pow(pos / half.toDouble())
            val f1 = lo * (hi / lo).pow((pos + 1) / half.toDouble())
            val from = f0.toInt().coerceIn(1, bins - 1)
            val to = f1.toInt().coerceIn(from + 1, bins)
            var sum = 0f
            for (k in from until to) sum += mag[k]
            val avg = sum / (to - from)
            val weighted = avg * (1.8f - 1.2f * pos / half)  // bass gets extra gain
            target[i] = sqrt((weighted / 90f).coerceIn(0f, 1f))
        }
        for (i in 0 until BARS) {
            val t = target[i]
            smooth[i] = if (t > smooth[i]) smooth[i] * 0.3f + t * 0.7f else smooth[i] * 0.85f + t * 0.15f
        }
        _levels.value = smooth.copyOf()
    }
}
