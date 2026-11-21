package com.android.island.ai

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import java.io.File

/** Copies a generated image from the app cache into the user's gallery. Only called on an explicit tap. */
object ImageGallery {
    fun save(context: Context, path: String): Boolean = runCatching {
        val file = File(path)
        if (!file.exists()) return false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ai-island-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI Island")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        true
    }.getOrDefault(false)
}
