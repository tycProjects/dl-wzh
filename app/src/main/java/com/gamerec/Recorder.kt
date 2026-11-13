package com.gamerec

import android.content.ContentValues
import android.content.Context
import android.media.*
import android.media.projection.MediaProjection
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.view.Surface
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

object Mix { @Volatile var mic = 1f; @Volatile var sys = 1f; @Volatile var micOn = true; @Volatile var sysOn = true }

/** يسجل الشاشة + صوت اللعبة + المايك في ملف MP4 واحد داخل Movies/GameRecorder */
class Recorder(private val ctx: Context, private val proj: MediaProjection, w: Int, h: Int, fps: Int, bps: Int, private val mode: Int) {
    private val venc = MediaCodec.createEncoderByType("video/avc")
    private val aenc = MediaCodec.createEncoderByType("audio/mp4a-latm")
    val surface: Surface
    private val uri: Uri
    private val pfd: ParcelFileDescriptor
    private val muxer: MediaMuxer
    private val mx = Any()
    private val t0 = System.nanoTime() / 1000
    private var vT = -1; private var aT = -1
    @Volatile private var started = false
    @Volatile private var running = true
    private var vBase = -1L; private var vOff = 0L
    private lateinit var vt: Thread
    private lateinit var at: Thread

    init {
        val vf = MediaFormat.createVideoFormat("video/avc", w, h).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bps)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setFloat("max-fps-to-encoder", fps.toFloat())
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        venc.configure(vf, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        surface = venc.createInputSurface(); venc.start()
        val af = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        aenc.configure(af, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); aenc.start()
        val cv = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "game_${stamp()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/GameRecorder")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        uri = ctx.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv)!!
        pfd = ctx.contentResolver.openFileDescriptor(uri, "w")!!
        muxer = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    fun begin() {
        vt = Thread { while (!drain(venc, false, true)) { } }
        at = Thread { audioLoop() }
        vt.start(); at.start()
    }

    private fun audioLoop() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
        val fmt = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(44100)
        fun open(extra: Boolean) = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(
                AudioPlaybackCaptureConfiguration.Builder(proj).apply {
                    addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    addMatchingUsage(AudioAttributes.USAGE_GAME)
                    addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    if (extra) addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                }.build())
            .setAudioFormat(fmt.setChannelMask(AudioFormat.CHANNEL_IN_STEREO).build())
            .setBufferSizeInBytes(32768).build()
        // mode 1 = مكالمة بالسماعة: المايك فقط يلتقط كل شي
        val sys = if (mode == 1) null else try { open(true) } catch (e: Exception) { try { open(false) } catch (e2: Exception) { null } }
        val mic = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 16384).takeIf { it.state == AudioRecord.STATE_INITIALIZED }
        } catch (e: Exception) { null }
        sys?.startRecording(); mic?.startRecording()
        val ps = ShortArray(2048); val pm = ShortArray(1024); var frames = 0L
        val out = ByteArray(4096)
        while (running) {
            Arrays.fill(ps, 0); Arrays.fill(pm, 0)
            sys?.read(ps, 0, 2048, AudioRecord.READ_BLOCKING)
            mic?.read(pm, 0, 1024, AudioRecord.READ_BLOCKING)
            if (sys == null && mic == null) Thread.sleep(23)
            val g1 = if (Mix.sysOn) Mix.sys else 0f
            val g2 = if (Mix.micOn) Mix.mic else 0f
            for (i in 0 until 1024) {
                val l = (ps[2 * i] * g1 + pm[i] * g2).toInt().coerceIn(-32768, 32767)
                val r = (ps[2 * i + 1] * g1 + pm[i] * g2).toInt().coerceIn(-32768, 32767)
                out[4 * i] = l.toByte(); out[4 * i + 1] = (l shr 8).toByte()
                out[4 * i + 2] = r.toByte(); out[4 * i + 3] = (r shr 8).toByte()
            }
            val idx = aenc.dequeueInputBuffer(20000)
            if (idx >= 0) {
                aenc.getInputBuffer(idx)!!.apply { clear(); put(out) }
                aenc.queueInputBuffer(idx, 0, 4096, frames * 1_000_000L / 44100, 0)
                frames += 1024
            }
            drain(aenc, true, false)
        }
        val idx = aenc.dequeueInputBuffer(100000)
        if (idx >= 0) aenc.queueInputBuffer(idx, 0, 0, frames * 1_000_000L / 44100, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
        var n = 0; while (n++ < 100 && !drain(aenc, true, true)) { }
        sys?.stop(); sys?.release(); mic?.stop(); mic?.release()
    }

    private fun drain(c: MediaCodec, audio: Boolean, wait: Boolean): Boolean {
        val info = MediaCodec.BufferInfo()
        while (true) {
            val i = c.dequeueOutputBuffer(info, if (wait) 10000L else 0L)
            if (i == MediaCodec.INFO_TRY_AGAIN_LATER) return false
            if (i == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                synchronized(mx) {
                    if (audio) aT = muxer.addTrack(c.outputFormat) else vT = muxer.addTrack(c.outputFormat)
                    if (aT >= 0 && vT >= 0) { muxer.start(); started = true }
                }
                continue
            }
            if (i < 0) continue
            val eos = (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
            if (info.size > 0 && (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                if (!audio) {
                    if (vBase < 0) { vBase = info.presentationTimeUs; vOff = System.nanoTime() / 1000 - t0 }
                    var n = 0; while (!started && n++ < 50) Thread.sleep(20)
                    info.presentationTimeUs = info.presentationTimeUs - vBase + vOff
                }
                if (started) synchronized(mx) { muxer.writeSampleData(if (audio) aT else vT, c.getOutputBuffer(i)!!, info) }
            }
            c.releaseOutputBuffer(i, false)
            if (eos) return true
        }
    }

    fun stop(done: () -> Unit) {
        running = false
        Thread {
            try { venc.signalEndOfInputStream() } catch (_: Exception) {}
            vt.join(3000); at.join(3000)
            try { if (started) muxer.stop() } catch (_: Exception) {}
            try { muxer.release() } catch (_: Exception) {}
            try { venc.stop(); venc.release(); aenc.stop(); aenc.release() } catch (_: Exception) {}
            try { pfd.close() } catch (_: Exception) {}
            ctx.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
            done()
        }.start()
    }
}
