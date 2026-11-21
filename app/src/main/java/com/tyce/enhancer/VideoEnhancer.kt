package com.tyce.enhancer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/** video enhance, kada frame */
object VideoEnhancer {

    /** ubos paspas, taas hinay */
    const val IN_SIDE = 480

    /** segundo lang, ayaw hakog */
    const val MAX_SECONDS = 15

    /** limit sa fps, hinay-hinay */
    const val OUT_FPS = 15

    class Info(val firstFrame: Bitmap, val durationMs: Long)

    /** silip sa video, una lang */
    fun inspect(context: Context, uri: Uri): Info? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val f = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            Info(shrink(f, 900), dur)
        } catch (t: Throwable) {
            t.printStackTrace(); null
        } finally {
            try { r.release() } catch (_: Throwable) {}
        }
    }

    /** duha o upat, pili lang */
    fun run(
        context: Context,
        uri: Uri,
        scale: Int,
        best: Boolean,
        smooth: Boolean,
        onProgress: (Float) -> Unit,
        isCancelled: () -> Boolean
    ): Uri? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        val useMs = min(durMs, MAX_SECONDS * 1000L).coerceAtLeast(1000L)

        // fps ug audio, ayaw kalimti
        var srcFps = 30
        var audioFormat: MediaFormat? = null
        var audioIndex = -1
        val probe = MediaExtractor()
        probe.setDataSource(context, uri, null)
        for (i in 0 until probe.trackCount) {
            val f = probe.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") && f.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                srcFps = f.getInteger(MediaFormat.KEY_FRAME_RATE).coerceIn(1, 120)
            }
            if (mime.startsWith("audio/") && audioIndex < 0) { audioIndex = i; audioFormat = f }
        }
        probe.release()
        val fps = min(srcFps, OUT_FPS).coerceAtLeast(1)
        val frameCount = max(1, (useMs * fps / 1000L).toInt())

        val engine = Enhancer(context, best = best && Enhancer.hasBest(context))
        val tmp = File(context.cacheDir, "video_out_${System.currentTimeMillis()}.mp4")
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxStarted = false
        var videoTrack = -1
        var audioTrack = -1
        var cancelled = false
        var prev: Bitmap? = null
        var prevT = 0L
        var prevSmall: Bitmap? = null   // frame kaniadto, para kopya
        var prevRaw: Bitmap? = null     // resulta, hilaw pa
        // kuhaa na ang sunod, uy
        val fetcher = Executors.newSingleThreadExecutor()
        fun fetch(i: Int): Future<Bitmap?> = fetcher.submit<Bitmap?> {
            retriever.getFrameAtTime(i * 1_000_000L / fps, MediaMetadataRetriever.OPTION_CLOSEST)
                ?.let { shrink(it, IN_SIDE) }
        }
        var nextFrame: Future<Bitmap?>? = null
        // rife o built-in, pili-a
        var rife: RifeInterp? = if (smooth && RifeInterp.hasModel(context)) {
            try { RifeInterp(context) } catch (t: Throwable) { t.printStackTrace(); null }
        } else null
        try {
            muxer = MediaMuxer(tmp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val info = MediaCodec.BufferInfo()

            fun drain(endOfStream: Boolean) {
                val enc = encoder ?: return
                var waits = 0
                while (true) {
                    val idx = enc.dequeueOutputBuffer(info, if (endOfStream) 10_000 else 0)
                    when {
                        idx == MediaCodec.INFO_TRY_AGAIN_LATER -> { if (!endOfStream || ++waits > 300) return }
                        idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            videoTrack = muxer!!.addTrack(enc.outputFormat)
                            if (audioFormat != null) {
                                audioTrack = try { muxer!!.addTrack(audioFormat!!) } catch (_: Throwable) { -1 }
                            }
                            muxer!!.start()
                            muxStarted = true
                        }
                        idx >= 0 -> {
                            val buf = enc.getOutputBuffer(idx)!!
                            if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
                            if (info.size > 0 && muxStarted) {
                                buf.position(info.offset)
                                buf.limit(info.offset + info.size)
                                muxer!!.writeSampleData(videoTrack, buf, info)
                            }
                            enc.releaseOutputBuffer(idx, false)
                            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                        }
                    }
                }
            }

            var outW = 0
            var outH = 0
            fun encodeBitmap(bmp: Bitmap, ptsUs: Long) {
                val enc = encoder!!
                var inIdx = enc.dequeueInputBuffer(10_000)
                while (inIdx < 0) { drain(false); inIdx = enc.dequeueInputBuffer(10_000) }
                fillImage(enc.getInputImage(inIdx)!!, bmp)
                enc.queueInputBuffer(inIdx, 0, outW * outH * 3 / 2, ptsUs, 0)
                drain(false)
            }
            nextFrame = fetch(0)
            for (i in 0 until frameCount) {
                if (isCancelled()) { cancelled = true; break }
                val tUs = i * 1_000_000L / fps
                val small = nextFrame?.get()
                nextFrame = if (i + 1 < frameCount) fetch(i + 1) else null
                if (small == null) continue
                val big = engine.enhance(small, isCancelled = isCancelled, ref = prevSmall, refOut = prevRaw)
                if (big == null) { small.recycle(); cancelled = true; break }
                prevSmall?.recycle(); prevSmall = small
                prevRaw?.recycle(); prevRaw = big

                // paliit-a, even lang ang sukod
                var tw = (big.width / 4) * scale
                var th = (big.height / 4) * scale
                tw -= tw % 2; th -= th % 2
                val frame = if (big.width != tw || big.height != th) {
                    Bitmap.createScaledBitmap(big, tw, th, true)
                } else big.copy(Bitmap.Config.ARGB_8888, false)

                if (encoder == null) {
                    outW = frame.width; outH = frame.height
                    val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outW, outH).apply {
                        setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                        setInteger(MediaFormat.KEY_BIT_RATE, (outW.toLong() * outH * fps * (if (smooth) 2 else 1) * 0.15).toInt().coerceIn(1_000_000, 20_000_000))
                        setInteger(MediaFormat.KEY_FRAME_RATE, if (smooth) fps * 2 else fps)
                        setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                    }
                    encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).also {
                        it.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                        it.start()
                    }
                }
                val p = prev
                if (smooth && p != null) {
                    val r = rife
                    val m = (if (r != null) {
                        try { r.mid(p, frame) } catch (t: Throwable) {
                            t.printStackTrace()
                            try { r.close() } catch (_: Throwable) {}
                            rife = null
                            null
                        }
                    } else null) ?: FrameInterp.mid(p, frame)
                    encodeBitmap(m, prevT + (tUs - prevT) / 2)
                    m.recycle()
                }
                encodeBitmap(frame, tUs)
                p?.recycle()
                prev = frame
                prevT = tUs
                onProgress((i + 1f) / frameCount * 0.97f)
            }

            val enc = encoder
            if (enc != null && !cancelled) {
                var inIdx = enc.dequeueInputBuffer(10_000)
                while (inIdx < 0) { drain(false); inIdx = enc.dequeueInputBuffer(10_000) }
                enc.queueInputBuffer(inIdx, 0, 0, frameCount * 1_000_000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                drain(true)
            }

            if (!cancelled && muxStarted && audioTrack >= 0) {
                copyAudio(context, uri, audioIndex, muxer!!, audioTrack, useMs * 1000L)
            }
        } finally {
            try { prev?.recycle() } catch (_: Throwable) {}
            try { rife?.close() } catch (_: Throwable) {}
            try { prevSmall?.recycle(); prevRaw?.recycle() } catch (_: Throwable) {}
            try { fetcher.shutdownNow(); fetcher.awaitTermination(5, TimeUnit.SECONDS) } catch (_: Throwable) {}
            try { encoder?.stop() } catch (_: Throwable) {}
            try { encoder?.release() } catch (_: Throwable) {}
            try { if (muxStarted) muxer?.stop() } catch (_: Throwable) {}
            try { muxer?.release() } catch (_: Throwable) {}
            try { retriever.release() } catch (_: Throwable) {}
            engine.close()
        }

        if (cancelled || !tmp.exists() || tmp.length() == 0L) {
            tmp.delete()
            return null
        }
        val saved = saveToMovies(context, tmp)
        tmp.delete()
        onProgress(1f)
        return saved
    }

    private fun copyAudio(context: Context, uri: Uri, index: Int, muxer: MediaMuxer, track: Int, limitUs: Long) {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(context, uri, null)
            ex.selectTrack(index)
            val buf = ByteBuffer.allocate(1 shl 18)
            val bi = MediaCodec.BufferInfo()
            while (true) {
                buf.clear()
                val n = ex.readSampleData(buf, 0)
                if (n < 0) break
                val t = ex.sampleTime
                if (t > limitUs) break
                bi.set(0, n, t, if ((ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0)
                muxer.writeSampleData(track, buf, bi)
                ex.advance()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            ex.release()
        }
    }

    /** ARGB ngadto YUV, hala bira */
    private fun fillImage(img: Image, bmp: Bitmap) {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val yP = img.planes[0]; val uP = img.planes[1]; val vP = img.planes[2]
        val yb = yP.buffer; val ub = uP.buffer; val vb = vP.buffer
        for (y in 0 until h) {
            val row = y * w
            for (x in 0 until w) {
                val p = px[row + x]
                val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
                yb.put(y * yP.rowStride + x * yP.pixelStride, (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255).toByte())
            }
        }
        for (y in 0 until h / 2) {
            for (x in 0 until w / 2) {
                var r = 0; var g = 0; var b = 0
                for (dy in 0..1) for (dx in 0..1) {
                    val p = px[(2 * y + dy) * w + 2 * x + dx]
                    r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF
                }
                r /= 4; g /= 4; b /= 4
                val u = (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                val v = (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                ub.put(y * uP.rowStride + x * uP.pixelStride, u.toByte())
                vb.put(y * vP.rowStride + x * vP.pixelStride, v.toByte())
            }
        }
    }

    private fun shrink(src: Bitmap, side: Int): Bitmap {
        val long = max(src.width, src.height)
        if (long <= side) return src
        val f = side.toFloat() / long
        val out = Bitmap.createScaledBitmap(
            src, (src.width * f).toInt().coerceAtLeast(2), (src.height * f).toInt().coerceAtLeast(2), true
        )
        if (out !== src) src.recycle()
        return out
    }

    private fun saveToMovies(context: Context, file: File): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "enhanced_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AIEnhancer")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return try {
            context.contentResolver.openOutputStream(uri)!!.use { out -> file.inputStream().use { it.copyTo(out) } }
            if (Build.VERSION.SDK_INT >= 29) {
                val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
                context.contentResolver.update(uri, done, null, null)
            }
            uri
        } catch (t: Throwable) {
            try { context.contentResolver.delete(uri, null, null) } catch (_: Throwable) {}
            null
        }
    }
}
