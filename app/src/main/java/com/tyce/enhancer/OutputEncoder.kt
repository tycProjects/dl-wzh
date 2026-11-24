package com.tyce.enhancer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

enum class SaveFormat(val label: String, val ext: String, val mime: String) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp");
}

/** Quality presets for lossy formats. Balanced is the default. */
enum class SaveQuality(val label: String, val q: Int) { SMALL("Smaller", 85), BALANCED("Balanced", 92), BEST("Best", 97) }

object OutputEncoder {
    @Suppress("DEPRECATION")
    private fun compressFormat(f: SaveFormat, lossy: Boolean) = when (f) {
        SaveFormat.JPEG -> Bitmap.CompressFormat.JPEG
        SaveFormat.PNG -> Bitmap.CompressFormat.PNG
        SaveFormat.WEBP -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
    }

    /** Saves to Pictures/AIEnhancer (kept so History still finds older saves). Metadata is not copied; pixels are sRGB. Partial files are removed on failure. */
    fun save(context: Context, bmp: Bitmap, fmt: SaveFormat, quality: SaveQuality): Uri? {
        var uri: Uri? = null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "alter_${System.currentTimeMillis()}.${fmt.ext}")
                put(MediaStore.Images.Media.MIME_TYPE, fmt.mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AIEnhancer")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val u = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            uri = u
            val src = if (fmt == SaveFormat.JPEG && bmp.hasAlpha()) bmp.copy(Bitmap.Config.ARGB_8888, false).also { it.setHasAlpha(false) } else bmp
            val ok = context.contentResolver.openOutputStream(u)?.use { src.compress(compressFormat(fmt, true), if (fmt == SaveFormat.PNG) 100 else quality.q, it) }
                ?: throw java.io.IOException("no stream")
            if (!ok) throw java.io.IOException("compress failed")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.update(u, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            }
            u
        } catch (t: Throwable) {
            uri?.let { try { context.contentResolver.delete(it, null, null) } catch (_: Throwable) {} }
            null
        }
    }
}
