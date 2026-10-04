package com.example.audio

import android.content.Context
import com.example.model.BeatCategory
import com.example.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

object SampleAudioGenerator {

    suspend fun generateSampleTracksIfEmpty(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val sampleDir = File(context.filesDir, "sample_tracks").apply { mkdirs() }

        val trackSpecs = listOf(
            SampleSpec(
                filename = "dj_jedag_jedug_bass.wav",
                id = -101L,
                title = "DJ Jedag Jedug Indo Bass",
                artist = "JNMF Sound Lab",
                album = "Indo Remix Vol. 1",
                durationSeconds = 24,
                bpm = 138,
                baseFreq = 42.0 // Heavy sub-bass
            ),
            SampleSpec(
                filename = "breakbeat_cyber_bounce.wav",
                id = -102L,
                title = "Indo Breakbeat Cyber Bounce",
                artist = "DJ Cyber Remix",
                album = "Remix Terminal",
                durationSeconds = 22,
                bpm = 130,
                baseFreq = 55.0
            ),
            SampleSpec(
                filename = "funk_koplo_bassline.wav",
                id = -103L,
                title = "Funk Koplo Bassline Drop",
                artist = "Subwoofer Hacker",
                album = "Bassline Protocol",
                durationSeconds = 20,
                bpm = 122,
                baseFreq = 50.0
            ),
            SampleSpec(
                filename = "mellow_chill_remix.wav",
                id = -104L,
                title = "Mellow Indo Chill Remix",
                artist = "Neo Jakarta",
                album = "Midnight Frequency",
                durationSeconds = 18,
                bpm = 92,
                baseFreq = 65.4
            )
        )

        val createdSongs = mutableListOf<Song>()

        for (spec in trackSpecs) {
            val audioFile = File(sampleDir, spec.filename)
            if (!audioFile.exists() || audioFile.length() < 1000) {
                generateSynthesizedWav(audioFile, spec)
            }

            createdSongs.add(
                Song(
                    id = spec.id,
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    durationMs = spec.durationSeconds * 1000L,
                    uriString = audioFile.absolutePath,
                    albumArtUri = null,
                    isSampleTrack = true,
                    dateAdded = System.currentTimeMillis(),
                    bpm = spec.bpm,
                    beatCategory = BeatCategory.fromBpm(spec.bpm),
                    analysis = BpmAnalyzer.analyzeSong(spec.title, spec.artist, spec.durationSeconds * 1000L, audioFile.absolutePath).second,
                    fileSize = audioFile.length(),
                    filePath = audioFile.absolutePath,
                    isKicked = false
                )
            )
        }

        createdSongs
    }

    private fun generateSynthesizedWav(outputFile: File, spec: SampleSpec) {
        val sampleRate = 44100
        val numChannels = 2
        val bitsPerSample = 16
        val totalSamples = sampleRate * spec.durationSeconds

        val beatDurationSec = 60.0 / spec.bpm
        val samplesPerBeat = (sampleRate * beatDurationSec).toInt().coerceAtLeast(1)

        FileOutputStream(outputFile).use { fos ->
            writeWavHeader(fos, totalSamples * numChannels * 2, sampleRate, numChannels, bitsPerSample)

            val buffer = ByteArray(4096)
            var bufferIndex = 0

            var phaseBass = 0.0
            var phaseLead = 0.0
            var phasePad = 0.0

            val scale = doubleArrayOf(1.0, 1.189, 1.334, 1.498, 1.781, 2.0)

            for (sampleIdx in 0 until totalSamples) {
                val beatIndex = sampleIdx / samplesPerBeat
                val sampleInBeat = sampleIdx % samplesPerBeat
                val beatProgress = sampleInBeat.toDouble() / samplesPerBeat

                // Indo Remix Punchy Kick with sub-bass drop
                val kickEnv = (1.0 - beatProgress * 1.8).coerceIn(0.0, 1.0)
                val kickFreq = (spec.baseFreq * 2.2 * kickEnv + 38.0)
                val kickPhaseInc = 2.0 * PI * kickFreq / sampleRate
                val kickVal = sin(phaseBass) * kickEnv * 0.65
                phaseBass += kickPhaseInc

                // Syncopated Bass / Lead synth
                val noteIdx = (beatIndex % scale.size)
                val currentFreq = spec.baseFreq * scale[noteIdx]
                val leadPhaseInc = 2.0 * PI * currentFreq / sampleRate
                val leadVal = sin(phaseLead) * 0.28
                phaseLead += leadPhaseInc

                // Crisp Hi-Hat / Sizzle for clarity
                val isOffBeat = ((sampleIdx / (samplesPerBeat / 2)) % 2) == 1
                val hatEnv = if (isOffBeat) (1.0 - (sampleIdx % (samplesPerBeat / 2)).toDouble() / (samplesPerBeat / 2)).coerceIn(0.0, 1.0) else 0.0
                val hatVal = (Math.random() * 2.0 - 1.0) * hatEnv * 0.15

                // Pad
                val padFreq = spec.baseFreq * 3.0
                val padPhaseInc = 2.0 * PI * padFreq / sampleRate
                val padVal = sin(phasePad) * 0.12
                phasePad += padPhaseInc

                val mixed = (kickVal + leadVal + hatVal + padVal).coerceIn(-1.0, 1.0)
                val sampleShort = (mixed * 32767.0).toInt().toShort()

                buffer[bufferIndex++] = (sampleShort.toInt() and 0xFF).toByte()
                buffer[bufferIndex++] = ((sampleShort.toInt() shr 8) and 0xFF).toByte()
                buffer[bufferIndex++] = (sampleShort.toInt() and 0xFF).toByte()
                buffer[bufferIndex++] = ((sampleShort.toInt() shr 8) and 0xFF).toByte()

                if (bufferIndex >= buffer.size) {
                    fos.write(buffer, 0, bufferIndex)
                    bufferIndex = 0
                }
            }

            if (bufferIndex > 0) {
                fos.write(buffer, 0, bufferIndex)
            }
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        dataSize: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = dataSize + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = ((channels * bitsPerSample) / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (dataSize and 0xff).toByte()
        header[41] = ((dataSize shr 8) and 0xff).toByte()
        header[42] = ((dataSize shr 16) and 0xff).toByte()
        header[43] = ((dataSize shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }

    private data class SampleSpec(
        val filename: String,
        val id: Long,
        val title: String,
        val artist: String,
        val album: String,
        val durationSeconds: Int,
        val bpm: Int,
        val baseFreq: Double
    )
}
