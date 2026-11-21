package com.android.island.island

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Membakar seluruh isi permukaan (latar artwork, kaca, tepi) menjadi SATU bitmap sekali per kunci.
 *
 * Alur:
 * 1. Frame pertama: isi digambar langsung sambil direkam ke GraphicsLayer.
 * 2. Dua frame kemudian: rekaman diubah menjadi ImageBitmap dan disimpan.
 * 3. Frame berikutnya: hanya satu drawImage, isi asli tidak digambar lagi.
 *
 * Kunci berubah (ukuran, aksen, tema, artwork) -> bitmap dibuang dan dibuat ulang.
 * Teks, tombol, dan animasi posisi tetap hidup di atas permukaan ini, jadi tidak ikut dibakar.
 */
@Composable
fun BakedSurface(
    keyBase: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val layer = rememberGraphicsLayer()
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    var baked by remember { mutableStateOf<ImageBitmap?>(null) }
    val fullKey = "$keyBase|${sizePx.width}x${sizePx.height}"

    LaunchedEffect(fullKey) {
        baked = null
        if (sizePx.width < 2 || sizePx.height < 2) return@LaunchedEffect
        // Dua frame: satu untuk menggambar isi ke layer, satu untuk memastikan rekaman selesai.
        withFrameNanos { }
        withFrameNanos { }
        baked = layer.toImageBitmap()
    }

    Box(
        modifier
            .onSizeChanged { sizePx = it }
            .drawWithContent {
                val image = baked
                if (image != null) {
                    drawImage(image, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                } else {
                    recordOnce(layer, this)
                    drawContent()
                }
            }
    ) {
        content()
    }
}

/** Merekam isi ke layer. Hanya dipanggil selama belum dibakar. */
private fun recordOnce(layer: androidx.compose.ui.graphics.GraphicsLayer, scope: androidx.compose.ui.draw.ContentDrawScope) {
    val w = scope.size.width.toInt().coerceAtLeast(1)
    val h = scope.size.height.toInt().coerceAtLeast(1)
    layer.record(IntSize(w, h)) { scope.drawContent() }
}
