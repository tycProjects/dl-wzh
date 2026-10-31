package com.fakesocial.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/** Saves a bitmap as PNG into Pictures/FakeSocial and returns a shareable content Uri. */
object GallerySaver {
    private const val FOLDER = "FakeSocial"

    suspend fun save(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
        val name = "FakeSocial_${System.currentTimeMillis()}.png"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveScoped(context, bitmap, name)
            else saveLegacy(context, bitmap, name)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveScoped(context: Context, bitmap: Bitmap, name: String): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        val ok = try {
            resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: false
        } catch (e: Exception) {
            false
        }
        if (!ok) {
            resolver.delete(uri, null, null)
            return null
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    // Android 8-9: needs WRITE_EXTERNAL_STORAGE (requested at runtime by the UI).
    @Suppress("DEPRECATION")
    private suspend fun saveLegacy(context: Context, bitmap: Bitmap, name: String): Uri? {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER)
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return suspendCancellableCoroutine { cont ->
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png")) { _, uri ->
                if (cont.isActive) cont.resume(uri)
            }
        }
    }
}
