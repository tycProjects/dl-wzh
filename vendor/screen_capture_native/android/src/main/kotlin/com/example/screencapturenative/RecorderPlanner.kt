package com.example.screencapturenative

import android.app.ActivityManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.util.Size
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val MIME_AVC = "video/avc"

enum class Tier { LOW, MID, HIGH }

enum class Quality(val key: String, val maxShortSide: Int, val fps: Int, val bpp: Float) {
    SMOOTH("smooth", 720, 30, 0.10f),
    BALANCED("balanced", 1080, 30, 0.10f),
    HIGH("high", 1080, 60, 0.10f),
    MAX("max", Int.MAX_VALUE, 60, 0.12f);

    companion object {
        fun from(key: String?): Quality = values().firstOrNull { it.key == key } ?: BALANCED
    }
}

data class Plan(
    val encoderName: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int,
    val bitrateMode: Int,   // -1 = leave encoder default
    val software: Boolean
) {
    val summary: String
        get() = "${width}×${height} • ${fps} fps • ${"%.1f".format(bitrate / 1_000_000.0)} Mbps"
}

object RecorderPlanner {

    fun tier(ctx: Context): Tier {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val gib = mi.totalMem / 1073741824.0
        return when {
            am.isLowRamDevice || gib < 3.2 -> Tier.LOW
            gib < 6.5 -> Tier.MID
            else -> Tier.HIGH
        }
    }

    fun resolve(ctx: Context, key: String?): Quality =
        if (key == null || key == "auto") {
            when (tier(ctx)) {
                Tier.LOW -> Quality.SMOOTH
                Tier.MID -> Quality.BALANCED
                Tier.HIGH -> Quality.MAX
            }
        } else Quality.from(key)

    fun displaySize(ctx: Context): Size {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= 30) {
            val b = wm.maximumWindowMetrics.bounds
            Size(b.width(), b.height())
        } else {
            val p = android.graphics.Point()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(p)
            Size(p.x, p.y)
        }
    }

    private fun refreshRate(ctx: Context): Float {
        val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return dm.getDisplay(0)?.refreshRate ?: 60f
    }

    private fun isSoftware(info: MediaCodecInfo): Boolean =
        if (Build.VERSION.SDK_INT >= 29) info.isSoftwareOnly || !info.isHardwareAccelerated
        else info.name.startsWith("OMX.google.") || info.name.startsWith("c2.android.")

    private fun encoders(): Pair<List<MediaCodecInfo>, List<MediaCodecInfo>> {
        val all = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { info ->
            info.isEncoder && info.supportedTypes.any { it.equals(MIME_AVC, true) }
        }
        val (sw, hw) = all.partition { isSoftware(it) }
        return hw to sw
    }

    private fun alignDown(v: Int, a: Int) = max(a, v / a * a)

    private fun fit(
        info: MediaCodecInfo, targetW: Int, targetH: Int, fps: Int, bpp: Float, software: Boolean
    ): Plan? {
        return try {
            val caps = info.getCapabilitiesForType(MIME_AVC)
            val vc = caps.videoCapabilities
            val wa = max(2, vc.widthAlignment)
            val ha = max(2, vc.heightAlignment)
            var w = alignDown(targetW, wa)
            var h = alignDown(targetH, ha)
            var guard = 0
            while (!vc.areSizeAndRateSupported(w, h, fps.toDouble())) {
                if (++guard > 14) return null
                w = alignDown((w * 0.9f).toInt(), wa)
                h = alignDown((h * 0.9f).toInt(), ha)
                if (w < 320 || h < 320) return null
            }
            val range = vc.bitrateRange
            val lo = max(1_500_000, range.lower)
            val hi = min(40_000_000, range.upper)
            val raw = (w.toLong() * h * fps * bpp).toLong()
            val br = raw.coerceIn(lo.toLong(), max(lo, hi).toLong()).toInt()
            val enc = caps.encoderCapabilities
            val mode = when {
                enc.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR) ->
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
                enc.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR) ->
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
                else -> -1
            }
            Plan(info.name, w, h, fps, br, mode, software)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Ordered list of configurations to try. The first is the best the chosen
     * quality can do on this device; later ones step down (fps, then size) so
     * recording always starts instead of failing. Software encoders are only
     * used as a last resort because they would lag a low-end phone.
     */
    fun candidates(ctx: Context, qualityKey: String?): List<Plan> {
        val q = resolve(ctx, qualityKey)
        val real = displaySize(ctx)
        val short = min(real.width, real.height).toFloat()
        val baseFps = min(q.fps, min(60, refreshRate(ctx).roundToInt().coerceAtLeast(30)))
        val scale0 = if (q.maxShortSide >= short) 1f else q.maxShortSide / short

        data class Step(val scale: Float, val fps: Int)
        val steps = listOf(
            Step(scale0, baseFps),
            Step(scale0, min(baseFps, 30)),
            Step(scale0 * 0.75f, min(baseFps, 30)),
            Step(min(scale0, 720f / short), 30),
            Step(min(scale0, 540f / short), 30)
        ).distinctBy { Triple((it.scale * 1000).toInt(), it.fps, 0) }

        val (hw, sw) = encoders()
        val out = ArrayList<Plan>()
        for (list in listOf(hw to false, sw to true)) {
            for (step in steps) {
                val w = (real.width * step.scale).toInt()
                val h = (real.height * step.scale).toInt()
                for (info in list.first) {
                    fit(info, w, h, step.fps, q.bpp, list.second)?.let { out.add(it) }
                }
            }
        }
        return out.distinctBy { listOf(it.encoderName, it.width, it.height, it.fps) }
    }
}
