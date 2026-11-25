package com.livevideowallpaper.app

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.LruCache
import android.widget.ImageView
import java.io.File

object ThumbnailLoader {
    private val cache = LruCache<String, Bitmap>(48)

    fun load(file: File, view: ImageView) {
        val key = file.absolutePath + file.lastModified()
        view.tag = key
        val cached = cache.get(key)
        if (cached != null) {
            view.setImageBitmap(cached)
            return
        }
        view.setImageResource(R.drawable.thumb_placeholder)
        Bg.io.execute {
            val bmp = frame(file, 480, atMiddle = false)
            if (bmp != null) {
                cache.put(key, bmp)
                view.post { if (view.tag == key) view.setImageBitmap(bmp) }
            }
        }
    }

    /** Grabs one frame, scaled so the longest side is at most [maxSide]. */
    fun frame(file: File, maxSide: Int, atMiddle: Boolean): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(file.absolutePath)
            val durMs = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val timeUs = if (atMiddle) durMs * 500 else if (durMs > 1500) 1_000_000L else 0L
            val src = r.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            val longest = maxOf(src.width, src.height)
            if (longest <= maxSide) src else {
                val s = maxSide.toFloat() / longest
                Bitmap.createScaledBitmap(src, (src.width * s).toInt(), (src.height * s).toInt(), true)
            }
        } catch (e: Exception) {
            null
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }
}
