package com.aetherisland.platform.display

import android.view.DisplayCutout
import android.view.WindowInsets

/** Converts platform cutout data into a stable model; manual calibration remains an override. */
data class CutoutInfo(val safeTop: Int, val safeLeft: Int, val safeRight: Int, val hasCutout: Boolean) {
    companion object {
        fun from(insets: WindowInsets?): CutoutInfo {
            val cutout: DisplayCutout? = insets?.displayCutout
            return CutoutInfo(
                safeTop = cutout?.safeInsetTop ?: 0,
                safeLeft = cutout?.safeInsetLeft ?: 0,
                safeRight = cutout?.safeInsetRight ?: 0,
                hasCutout = cutout != null
            )
        }
    }
}
