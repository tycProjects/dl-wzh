package com.example.screencapturenative

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

/**
 * One encoder + one MP4 file. The encoder thread only copies finished frames
 * into a queue; a separate writer thread does the (possibly slow) storage I/O,
 * so a slow flash write can never stall the encoder and drop frames.
 */
class RecordingSession(
    private val ctx: Context,
    val plan: Plan,
    private val fileName: String
) {
    private class Sample(val data: ByteArray?, val info: MediaCodec.BufferInfo?, val format: MediaFormat?)

    private val end = Sample(null, null, null)
    private val queue = LinkedBlockingQueue<Sample>(300)

    private var encoder: MediaCodec? = null
    var surface: Surface? = null
        private set
    private var muxer: MediaMuxer? = null
    @Volatile private var muxerStarted = false
    @Volatile private var failed = false
    private var drainThread: Thread? = null
    private var writerThread: Thread? = null

    private var videoUri: Uri? = null
    private var pfd: ParcelFileDescriptor? = null
    private var legacyFile: File? = null
    private var legacyOut: FileOutputStream? = null

    val displayName: String get() = fileName

    fun start() {
        val fd = openOutput() ?: throw IllegalStateException("Could not create output file")
        try {
            val format = MediaFormat.createVideoFormat(MIME_AVC, plan.width, plan.height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, plan.bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, plan.fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                if (plan.bitrateMode >= 0) setInteger(MediaFormat.KEY_BITRATE_MODE, plan.bitrateMode)
                if (Build.VERSION.SDK_INT >= 23) {
                    setInteger(MediaFormat.KEY_PRIORITY, 0) // real-time
                    // Only ask the SoC to boost clocks when we really need 60 fps.
                    if (plan.fps >= 60) setInteger(MediaFormat.KEY_OPERATING_RATE, plan.fps)
                }
                // Keep the stream alive on a static screen without burning encode work.
                try { setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 500_000L) } catch (_: Throwable) { }
            }
            val c = MediaCodec.createByCodecName(plan.encoderName)
            encoder = c
            c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            surface = c.createInputSurface()
            muxer = MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            c.start()

            writerThread = Thread({ writeLoop() }, "Rec-Writer").also { it.start() }
            drainThread = Thread({
                try { Process.setThreadPriority(Process.THREAD_PRIORITY_VIDEO) } catch (_: Throwable) { }
                drainLoop(c)
            }, "Rec-Encoder").also { it.start() }
        } catch (t: Throwable) {
            abort()
            throw t
        }
    }

    fun setBitrate(bps: Int) {
        try {
            encoder?.setParameters(Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps)
            })
        } catch (e: Exception) {
            Log.w(TAG, "Could not change bitrate", e)
        }
    }

    private fun drainLoop(c: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        try {
            while (true) {
                val idx = c.dequeueOutputBuffer(info, 50_000)
                when {
                    idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ->
                        queue.put(Sample(null, null, c.outputFormat))
                    idx >= 0 -> {
                        val isEos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        val isConfig = (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        if (info.size > 0 && !isConfig) {
                            val buf = c.getOutputBuffer(idx)
                            if (buf != null) {
                                buf.position(info.offset)
                                buf.limit(info.offset + info.size)
                                val bytes = ByteArray(info.size)
                                buf.get(bytes)
                                val copy = MediaCodec.BufferInfo()
                                copy.set(0, info.size, info.presentationTimeUs, info.flags)
                                queue.put(Sample(bytes, copy, null))
                            }
                        }
                        c.releaseOutputBuffer(idx, false)
                        if (isEos) break
                    }
                }
            }
        } catch (e: InterruptedException) {
            // finish() gave up waiting; fall through
        } catch (e: Exception) {
            Log.e(TAG, "Encoder drain failed", e)
            failed = true
        } finally {
            queue.offer(end)
        }
    }

    private fun writeLoop() {
        var track = -1
        var base = -1L
        try {
            while (true) {
                val s = queue.take()
                if (s === end) break
                if (failed) continue
                try {
                    if (s.format != null) {
                        track = muxer!!.addTrack(s.format)
                        muxer!!.start()
                        muxerStarted = true
                        continue
                    }
                    if (!muxerStarted) continue
                    val info = s.info!!
                    if (base < 0) base = info.presentationTimeUs
                    info.presentationTimeUs = (info.presentationTimeUs - base).coerceAtLeast(0)
                    muxer!!.writeSampleData(track, ByteBuffer.wrap(s.data!!), info)
                } catch (e: Exception) {
                    Log.e(TAG, "Muxer write failed", e)
                    failed = true
                }
            }
        } catch (_: InterruptedException) { }
    }

    /** Blocking. Call from a background thread. Finalises the MP4. */
    fun finish() {
        try { encoder?.signalEndOfInputStream() } catch (e: Exception) { Log.w(TAG, "EOS failed", e) }
        drainThread?.join(4_000L)
        if (drainThread?.isAlive == true) drainThread?.interrupt()
        writerThread?.join(4_000L)
        if (writerThread?.isAlive == true) writerThread?.interrupt()
        releaseCodec()
        val hadData = muxerStarted
        if (hadData) { try { muxer?.stop() } catch (e: Exception) { Log.w(TAG, "Muxer stop", e) } }
        try { muxer?.release() } catch (_: Exception) { }
        muxer = null
        closeOutput(keep = hadData)
    }

    /** Blocking cleanup for a session that never produced a valid file. */
    fun abort() {
        drainThread?.interrupt()
        writerThread?.interrupt()
        releaseCodec()
        try { muxer?.release() } catch (_: Exception) { }
        muxer = null
        closeOutput(keep = false)
    }

    private fun releaseCodec() {
        try { encoder?.stop() } catch (_: Exception) { }
        try { encoder?.release() } catch (_: Exception) { }
        encoder = null
        try { surface?.release() } catch (_: Exception) { }
        surface = null
    }

    // ---------- storage ----------

    private fun openOutput(): FileDescriptor? {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/$SUBFOLDER")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = ctx.contentResolver.insert(collection, values) ?: return null
            videoUri = uri
            pfd = ctx.contentResolver.openFileDescriptor(uri, "rw")
            if (pfd == null) {
                ctx.contentResolver.delete(uri, null, null)
                videoUri = null
                return null
            }
            return pfd!!.fileDescriptor
        }
        @Suppress("DEPRECATION")
        var dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), SUBFOLDER)
        if (!dir.exists() && !dir.mkdirs()) {
            // No storage permission: fall back to the app's own folder.
            dir = File(ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES), SUBFOLDER)
            if (!dir.exists() && !dir.mkdirs()) return null
        }
        val f = File(dir, fileName)
        legacyFile = f
        legacyOut = FileOutputStream(f)
        return legacyOut!!.fd
    }

    private fun closeOutput(keep: Boolean) {
        try { legacyOut?.fd?.sync() } catch (_: Exception) { }
        try { legacyOut?.close() } catch (_: Exception) { }
        legacyOut = null
        try { pfd?.close() } catch (_: Exception) { }
        pfd = null

        videoUri?.let { uri ->
            try {
                if (keep) {
                    ctx.contentResolver.update(uri,
                        ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
                } else {
                    ctx.contentResolver.delete(uri, null, null)
                }
            } catch (e: Exception) { Log.w(TAG, "Finalize MediaStore failed", e) }
        }
        videoUri = null

        legacyFile?.let { f ->
            if (keep && f.exists() && f.length() > 0L) {
                MediaScannerConnection.scanFile(ctx, arrayOf(f.absolutePath), arrayOf("video/mp4"), null)
            } else if (!keep) {
                f.delete()
            }
        }
        legacyFile = null
    }

    companion object {
        private const val TAG = "RecordingSession"
        const val SUBFOLDER = "ScreenRecorder"
    }
}
