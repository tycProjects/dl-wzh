package com.example.smoothcamera.media

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

class MediaStoreRepository(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun insertPendingImage(displayName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SmoothCamera")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    fun finishImage(uri: Uri) {
        if (Build.VERSION.SDK_INT >= 29) resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
    }

    fun delete(uri: Uri) { runCatching { resolver.delete(uri, null, null) } }

    fun saveBitmap(bitmap: Bitmap, displayName: String): Uri? {
        val uri = insertPendingImage(displayName) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out)) { "JPEG compression failed" }
            } ?: error("Unable to open MediaStore output")
            finishImage(uri)
            uri
        }.getOrElse { delete(uri); null }
    }

    fun insertPendingVideo(displayName: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SmoothCamera")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
    }

    fun finishVideo(uri: Uri) {
        if (Build.VERSION.SDK_INT >= 29) resolver.update(uri, ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }, null, null)
    }

    fun deleteVideo(uri: Uri) { delete(uri) }

    fun copyFileToImage(file: File, displayName: String): Uri? {
        val uri = insertPendingImage(displayName) ?: return null
        return runCatching {
            FileInputStream(file).use { input -> resolver.openOutputStream(uri).use { output -> checkNotNull(output); input.copyTo(output, 64 * 1024) } }
            finishImage(uri)
            uri
        }.getOrElse { delete(uri); null }
    }
}
