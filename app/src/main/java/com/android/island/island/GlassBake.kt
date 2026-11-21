package com.android.island.island

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Satu lapis kaca: artwork, scrim, sorotan, pendar aksen, dan kilau digabung menjadi SATU bitmap
 * di setengah resolusi. Sebelumnya tiap bagian adalah layer terpisah dengan offscreen texture
 * sendiri, dan setiap layer menggandakan memori GPU. Bitmap ini dibuat sekali per artwork dan
 * ukuran, lalu di-cache; saat animasi hanya alpha satu gambar yang berubah.
 */
object GlassBake {
    /** Skala render: 0.5 = setengah lebar dan tinggi, seperempat memori. */
    const val SCALE = 0.5f

    private val cache = object : LinkedHashMap<String, ImageBitmap>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 4
    }

    @Synchronized
    fun cached(key: String): ImageBitmap? = cache[key]

    @Synchronized
    fun store(key: String, image: ImageBitmap) {
        cache[key] = image
    }

    /**
     * Bentuk satu bitmap. [scrim] 0..1 = seberapa gelap; [accent] dipakai untuk pendar kanan-bawah.
     * Dipanggil di thread UI sekali per kunci cache; ukuran kecil (setengah resolusi) menjaga biayanya.
     */
    fun bake(art: Bitmap, widthPx: Int, heightPx: Int, scrim: Float, accent: Int): ImageBitmap {
        val w = (widthPx * SCALE).toInt().coerceAtLeast(2)
        val h = (heightPx * SCALE).toInt().coerceAtLeast(2)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1) Artwork, cover-crop ke kotak tujuan.
        val src = if (art.config == Bitmap.Config.HARDWARE) art.copy(Bitmap.Config.ARGB_8888, false) ?: art else art
        val scale = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
        val sw = w / scale
        val sh = h / scale
        val sl = (src.width - sw) / 2f
        val st = (src.height - sh) / 2f
        c.drawBitmap(
            src,
            android.graphics.Rect(sl.toInt(), st.toInt(), (sl + sw).toInt(), (st + sh).toInt()),
            android.graphics.Rect(0, 0, w, h),
            paint
        )
        if (src !== art) src.recycle()

        // 2) Scrim gelap (sama seperti lapisan scrim lama, sudah digabung).
        if (scrim > 0.001f) {
            paint.shader = null
            paint.color = AColor.argb((scrim.coerceIn(0f, 1f) * 255).toInt(), 0, 0, 0)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }

        val big = maxOf(w, h).toFloat()
        // 3) Sorotan putih kiri-atas.
        paint.shader = RadialGradient(
            w * 0.18f, h * 0.12f, big * 0.55f,
            intArrayOf(AColor.argb(26, 255, 255, 255), AColor.argb(0, 255, 255, 255)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        // 4) Pendar aksen kanan-bawah.
        paint.shader = RadialGradient(
            w * 0.86f, h * 0.92f, big * 0.50f,
            intArrayOf(
                AColor.argb(28, AColor.red(accent), AColor.green(accent), AColor.blue(accent)),
                AColor.argb(0, AColor.red(accent), AColor.green(accent), AColor.blue(accent))
            ),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        // 5) Kilau diagonal tipis.
        paint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(
                AColor.argb(0, 255, 255, 255),
                AColor.argb(9, 255, 255, 255),
                AColor.argb(2, 255, 255, 255),
                AColor.argb(0, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.42f, 0.58f, 1f), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        return out.asImageBitmap()
    }
}
