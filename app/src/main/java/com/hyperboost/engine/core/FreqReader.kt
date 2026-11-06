// app/src/main/java/com/hyperboost/engine/core/FreqReader.kt
package com.hyperboost.engine.core

/**
 * Reads the live CPU (GHz) and GPU (MHz) clocks through Shizuku. The sysfs paths differ by chip
 * (Qualcomm kgsl, Mali devfreq, Samsung/MediaTek nodes); anything the device does not expose
 * comes back null instead of a made-up number.
 */
object FreqReader {

    data class Freq(
        val cpuGhz: Float? = null,
        val cpuMaxGhz: Float? = null,
        val gpuMhz: Float? = null,
        val gpuMaxMhz: Float? = null
    )

    private const val SCRIPT =
        "cm=0; cx=0; " +
        "for d in /sys/devices/system/cpu/cpu[0-9]*/cpufreq; do " +
        "v=\$(cat \$d/scaling_cur_freq 2>/dev/null); [ -n \"\$v\" ] && [ \"\$v\" -gt \"\$cm\" ] && cm=\$v; " +
        "x=\$(cat \$d/cpuinfo_max_freq 2>/dev/null); [ -n \"\$x\" ] && [ \"\$x\" -gt \"\$cx\" ] && cx=\$x; done; " +
        "echo CPU=\$cm; echo CPUMAX=\$cx; " +
        "g=; gx=; " +
        "for f in /sys/class/kgsl/kgsl-3d0/gpuclk /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq " +
        "/sys/class/devfreq/*mali*/cur_freq /sys/class/devfreq/*gpu*/cur_freq /sys/kernel/gpu/gpu_clock; do " +
        "[ -r \$f ] && g=\$(cat \$f 2>/dev/null) && [ -n \"\$g\" ] && break; done; " +
        "for f in /sys/class/kgsl/kgsl-3d0/max_gpuclk /sys/class/kgsl/kgsl-3d0/devfreq/max_freq " +
        "/sys/class/devfreq/*mali*/max_freq /sys/class/devfreq/*gpu*/max_freq; do " +
        "[ -r \$f ] && gx=\$(cat \$f 2>/dev/null) && [ -n \"\$gx\" ] && break; done; " +
        "echo GPU=\$g; echo GPUMAX=\$gx"

    /** Hz, kHz or MHz all show up in the wild; normalise to MHz. */
    private fun toMhz(raw: Long): Float? = when {
        raw <= 0L -> null
        raw > 10_000_000L -> raw / 1_000_000f
        raw > 10_000L -> raw / 1_000f
        else -> raw.toFloat()
    }

    suspend fun read(): Freq {
        val out = ShizukuShellExecutor.execute(SCRIPT).getOrNull()?.takeIf { it.exitCode == 0 }?.stdout ?: return Freq()
        val m = out.lines().mapNotNull { l ->
            val i = l.indexOf('=')
            if (i < 0) null else l.substring(0, i).trim() to (l.substring(i + 1).trim().toLongOrNull() ?: 0L)
        }.toMap()
        fun cpu(key: String) = (m[key] ?: 0L).takeIf { it > 0 }?.let { it / 1_000_000f } // kHz -> GHz
        return Freq(cpu("CPU"), cpu("CPUMAX"), toMhz(m["GPU"] ?: 0L), toMhz(m["GPUMAX"] ?: 0L))
    }
}
