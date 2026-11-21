package com.android.island.ai

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device text recognition (ML Kit, Latin script, bundled model) for screenshots.
 *
 * RAM: the recognizer (and its native engine) used to be created and closed on every
 * screenshot, which churns the heap and reloads the engine under rapid analysis loops.
 * One client is now created lazily on first use and released automatically after a
 * quiet minute — idle cost is zero, back-to-back runs reuse a warm engine, and the
 * on-device feature itself is untouched.
 */
object ScreenOcr {
    /** Idle time after which the recognizer is closed; a later call rebuilds it. */
    private const val IDLE_RELEASE_MS = 10_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutex = Mutex()
    private var recognizer: com.google.mlkit.vision.text.TextRecognizer? = null

    private val releaseRunnable = Runnable {
        synchronized(this) {
            recognizer?.close()
            recognizer = null
        }
    }

    /** Main-thread-safe: returns the warm recognizer or null, and postpones idle release. */
    private fun peekWarm(): com.google.mlkit.vision.text.TextRecognizer? =
        synchronized(this) {
            recognizer?.also { scheduleIdleRelease() }
        }

    private fun scheduleIdleRelease() {
        mainHandler.removeCallbacks(releaseRunnable)
        mainHandler.postDelayed(releaseRunnable, IDLE_RELEASE_MS)
    }

    private suspend fun acquire(): com.google.mlkit.vision.text.TextRecognizer =
        mutex.withLock {
            synchronized(this) { recognizer } ?: TextRecognition
                .getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .also {
                    synchronized(this) { recognizer = it }
                    scheduleIdleRelease()
                }
        }

    suspend fun recognizeJpeg(bytes: ByteArray): String = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@withContext ""
        // A warm client is handed out without the mutex; only a cold start serialises.
        val recognizer = peekWarm() ?: acquire()
        try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text.trim()
        } catch (_: Exception) {
            ""
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
}
