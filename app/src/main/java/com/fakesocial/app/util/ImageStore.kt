package com.fakesocial.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.fakesocial.app.data.Format
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Stores profile pictures inside the app's private storage (no storage permission needed). */
object ImageStore {
    private const val MAX_SIDE = 512

    private fun avatarDir(context: Context): File =
        File(context.filesDir, "avatars").apply { mkdirs() }

    /** Copies a picked image (content Uri) into app storage, downscaled. Returns the file path. */
    fun saveFromUri(context: Context, uri: Uri): String? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = Format.sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE)
        }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
        bitmap?.let { saveBitmap(context, it) }
    } catch (e: Exception) {
        null
    }

    fun saveBitmap(context: Context, bitmap: Bitmap): String? = try {
        val file = File(avatarDir(context), "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        file.absolutePath
    } catch (e: Exception) {
        null
    }

    fun load(path: String?): Bitmap? =
        if (path.isNullOrEmpty()) null else try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
}
