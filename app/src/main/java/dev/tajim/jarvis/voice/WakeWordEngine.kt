package dev.tajim.jarvis.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

/**
 * Offline wake word using Vosk with a tiny grammar ("jarvis" + unknown), so it only listens for that word.
 * Needs the English Vosk model in assets/model-en-us (copy it from your legacy APK; see README).
 * Audio is processed on the device and is not stored or sent anywhere. Always-on listening costs battery.
 */
class WakeWordEngine(context: Context) {
    private val app = context.applicationContext
    private var model: Model? = null
    private val assetDir = "model-en-us"

    fun isModelBundled(): Boolean = runCatching { app.assets.list(assetDir)?.isNotEmpty() == true }.getOrDefault(false)

    /** Copies the model out of assets once, then loads it. Returns false if the model is missing or fails to load. */
    suspend fun ensureReady(): Boolean = withContext(Dispatchers.IO) {
        if (model != null) return@withContext true
        if (!isModelBundled()) return@withContext false
        runCatching {
            val root = File(app.filesDir, "vosk").apply { mkdirs() }
            val dest = File(root, assetDir)
            val marker = File(root, "$assetDir.uuid")
            val uuid = runCatching { app.assets.open("$assetDir/uuid").bufferedReader().use { it.readText().trim() } }.getOrDefault("v1")
            if (!(dest.exists() && marker.exists() && marker.readText() == uuid)) {
                dest.deleteRecursively()
                copyAssets(assetDir, dest)
                marker.writeText(uuid)
            }
            model = Model(dest.absolutePath)
            true
        }.getOrDefault(false)
    }

    private fun copyAssets(path: String, dest: File) {
        val children = app.assets.list(path)
        if (children.isNullOrEmpty()) {
            dest.parentFile?.mkdirs()
            app.assets.open(path).use { input -> dest.outputStream().use { out -> input.copyTo(out) } }
        } else {
            dest.mkdirs()
            children.forEach { copyAssets("$path/$it", File(dest, it)) }
        }
    }

    /** Listens until one of [words] is heard (returns it) or the coroutine is cancelled (returns null). Releases the mic on exit. */
    suspend fun listenFor(words: Set<String>): String? = withContext(Dispatchers.IO) {
        val m = model ?: return@withContext null
        val rate = 16_000
        val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        var rec: AudioRecord? = null
        var recognizer: Recognizer? = null
        try {
            recognizer = Recognizer(m, rate.toFloat(), JSONArray(words.toList() + "[unk]").toString())
            rec = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, rate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, 8192),
            )
            if (rec.state != AudioRecord.STATE_INITIALIZED) return@withContext null
            rec.startRecording()
            val buf = ByteArray(4096)
            while (isActive) {
                val n = rec.read(buf, 0, buf.size)
                if (n < 0) return@withContext null
                if (n == 0) continue
                val text = if (recognizer.acceptWaveForm(buf, n)) {
                    JSONObject(recognizer.result).optString("text")
                } else {
                    JSONObject(recognizer.partialResult).optString("partial")
                }
                val hit = text.split(' ').firstOrNull { it in words }
                if (hit != null) return@withContext hit
            }
            null
        } catch (_: SecurityException) {
            null
        } finally {
            runCatching { rec?.stop() }
            rec?.release()
            recognizer?.close()
        }
    }
}
