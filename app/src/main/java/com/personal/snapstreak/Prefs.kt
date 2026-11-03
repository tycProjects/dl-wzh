package com.personal.snapstreak

import android.content.Context

/** Settings you can change inside the app without rebuilding. */
object Prefs {
    private const val FILE = "snapstreak"
    private const val KEY_CAMERA_GAP = "camera_gap_ms"

    fun cameraGapMs(ctx: Context): Long =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getLong(KEY_CAMERA_GAP, Steps.CAMERA_GAP_MS)

    fun setCameraGapMs(ctx: Context, ms: Long) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putLong(KEY_CAMERA_GAP, ms.coerceIn(0L, 5000L))
            .apply()
    }
}
