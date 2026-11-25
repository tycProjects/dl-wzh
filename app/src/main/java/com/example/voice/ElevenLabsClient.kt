package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class ElevenLabsVoiceOption(
    val voiceId: String,
    val name: String,
    val category: String = "premade",
    val description: String = ""
)

class ElevenLabsClient(private val context: Context) {
    companion object {
        private const val TAG = "ElevenLabsClient"
        val PRESET_FEMALE_VOICES = listOf(
            ElevenLabsVoiceOption("21m00Tcm4TlvDq8ikWAM", "Rachel (Warm Female)", "premade", "উষ্ণ ও স্পষ্ট নারী কণ্ঠ"),
            ElevenLabsVoiceOption("EXAVITQu4vr4xnSDxMaL", "Bella (Soft Expressive)", "premade", "কোমল ও আন্তরিক নারী কণ্ঠ"),
            ElevenLabsVoiceOption("MF3mGyEYCl7XYWbV9V6O", "Elli (Bright Youthful)", "premade", "প্রাণবন্ত ও দ্রুত নারী কণ্ঠ"),
            ElevenLabsVoiceOption("AZnzlk1XvdvUeBnXmlld", "Domi (Confident)", "premade", "আত্মবিশ্বাসী নারী কণ্ঠ"),
            ElevenLabsVoiceOption("pFZP5JQG7iQjIQuC4Bku", "Lily (Warm Storyteller)", "premade", "মধুর ও সাবলীল নারী কণ্ঠ")
        )
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Volatile
    private var activePlayer: MediaPlayer? = null

    suspend fun fetchVoices(apiKey: String): Result<List<ElevenLabsVoiceOption>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/voices")
                .addHeader("xi-api-key", apiKey.trim())
                .get()
                .build()

            withTimeout(15_000L) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string().orEmpty()
                        return@withTimeout Result.failure(
                            IllegalStateException("ElevenLabs API ত্রুটি (${response.code}): ${err.take(100)}")
                        )
                    }
                    val json = JSONObject(response.body?.string().orEmpty())
                    val arr = json.optJSONArray("voices")
                    val list = mutableListOf<ElevenLabsVoiceOption>()
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            val labels = obj.optJSONObject("labels")
                            val gender = labels?.optString("gender", "").orEmpty().lowercase()
                            if (gender.isEmpty() || gender == "female") {
                                list.add(
                                    ElevenLabsVoiceOption(
                                        voiceId = obj.optString("voice_id"),
                                        name = obj.optString("name"),
                                        category = obj.optString("category", "custom"),
                                        description = labels?.optString("description", "").orEmpty()
                                    )
                                )
                            }
                        }
                    }
                    Result.success(list.ifEmpty { PRESET_FEMALE_VOICES })
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch ElevenLabs voices", e)
            Result.failure(e)
        }
    }

    suspend fun synthesizeAndPlay(
        text: String,
        apiKey: String,
        voiceId: String,
        modelId: String,
        speed: Float,
        volume: Float,
        onAmplitudeTick: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanText = text.replace(Regex("[*#`_]"), "").trim()
        if (cleanText.isBlank()) return@withContext Result.success(Unit)

        var tempFile: File? = null
        try {
            val payload = JSONObject().apply {
                put("text", cleanText)
                put("model_id", modelId)
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.48)
                    put("similarity_boost", 0.78)
                    put("style", 0.35)
                    put("use_speaker_boost", true)
                })
            }

            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId/stream?optimize_streaming_latency=3")
                .addHeader("xi-api-key", apiKey.trim())
                .addHeader("Accept", "audio/mpeg")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            tempFile = File.createTempFile("maya_el_", ".mp3", context.cacheDir)

            withTimeout(25_000L) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string().orEmpty()
                        throw IllegalStateException("ElevenLabs HTTP ${response.code}: ${err.take(100)}")
                    }
                    val stream = response.body?.byteStream()
                        ?: throw IllegalStateException("ElevenLabs empty audio stream")
                    FileOutputStream(tempFile).use { out ->
                        stream.copyTo(out)
                    }
                }
            }

            playAudioFile(tempFile, speed.coerceIn(0.75f, 1.5f), volume.coerceIn(0f, 1f), onAmplitudeTick)
            Result.success(Unit)
        } catch (e: CancellationException) {
            stop()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs playback failed", e)
            stop()
            Result.failure(e)
        } finally {
            try {
                tempFile?.delete()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun playAudioFile(
        file: File,
        speed: Float,
        volume: Float,
        onAmplitudeTick: (Float) -> Unit
    ) {
        stop()
        suspendCancellableCoroutine { cont ->
            val player = MediaPlayer()
            activePlayer = player
            try {
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                player.setDataSource(file.absolutePath)
                player.setVolume(volume, volume)
                player.prepare()
                try {
                    player.playbackParams = PlaybackParams().setSpeed(speed)
                } catch (_: Exception) {
                }

                player.setOnCompletionListener {
                    onAmplitudeTick(0f)
                    runCatching { it.release() }
                    if (activePlayer === it) activePlayer = null
                    if (cont.isActive) cont.resume(Unit)
                }

                player.setOnErrorListener { mp, _, _ ->
                    onAmplitudeTick(0f)
                    runCatching { mp.release() }
                    if (activePlayer === mp) activePlayer = null
                    if (cont.isActive) cont.resume(Unit)
                    true
                }

                cont.invokeOnCancellation {
                    onAmplitudeTick(0f)
                    stop()
                }

                player.start()
                onAmplitudeTick(0.7f)
            } catch (e: Exception) {
                onAmplitudeTick(0f)
                runCatching { player.release() }
                if (activePlayer === player) activePlayer = null
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    fun stop() {
        val player = activePlayer
        activePlayer = null
        if (player != null) {
            runCatching {
                if (player.isPlaying) player.stop()
                player.release()
            }
        }
    }
}
