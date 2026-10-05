package com.example.smoothcamera.processing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.roundToInt

/**
 * Small, deterministic post-capture profiles. Natural intentionally returns the source unchanged.
 * Profiles are not Apple's processing and do not claim to reproduce proprietary ISP behavior.
 */
object ColorProfileProcessor {
    enum class Profile { NATURAL, VIBRANT, WARM, COOL, CINEMATIC, SOFT, NEUTRAL }

    fun process(source: File, profile: Profile): Bitmap {
        val original = BitmapFactory.decodeFile(source.absolutePath) ?: error("Unable to decode captured image")
        if (profile == Profile.NATURAL) return original
        val matrix = when (profile) {
            Profile.VIBRANT -> ColorMatrix(floatArrayOf(1.05f,0f,0f,0f,0f, 0f,1.04f,0f,0f,0f, 0f,0f,1.03f,0f,0f, 0f,0f,0.98f,0f,0f))
            Profile.WARM -> ColorMatrix(floatArrayOf(1.04f,0f,0f,0f,4f, 0f,1.01f,0f,0f,1f, 0f,0f,0.95f,0f,-2f, 0f,0f,0f,1f,0f))
            Profile.COOL -> ColorMatrix(floatArrayOf(0.98f,0f,0f,0f,0f, 0f,1.01f,0f,0f,0f, 0f,0f,1.06f,0f,4f, 0f,0f,0f,1f,0f))
            Profile.CINEMATIC -> ColorMatrix(floatArrayOf(1.04f,0f,0f,0f,-3f, 0f,1.02f,0f,0f,0f, 0f,0f,0.98f,0f,2f, 0f,0f,0f,0.98f,0f))
            Profile.SOFT -> ColorMatrix(floatArrayOf(0.96f,0f,0f,0f,7f, 0f,0.96f,0f,0f,7f, 0f,0f,0.96f,0f,7f, 0f,0f,0f,1f,0f))
            Profile.NEUTRAL -> ColorMatrix(floatArrayOf(1.01f,0f,0f,0f,-1f, 0f,1.01f,0f,0f,-1f, 0f,0f,1.01f,0f,-1f, 0f,0f,0f,1f,0f))
            Profile.NATURAL -> ColorMatrix()
        }
        val output = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(original, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(matrix) })
        original.recycle()
        return output
    }
}
