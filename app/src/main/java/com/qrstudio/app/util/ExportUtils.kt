package com.qrstudio.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ExportUtils {

    fun fileName(prefix: String = "QRStudio"): String =
        prefix + "_" + System.currentTimeMillis()

    fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        extension: String,
        mime: String,
        displayName: String,
    ): String {
        val fileName = "$displayName.$extension"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStoreDisplayName, fileName)
                put(MediaStoreMimeType, mime)
                put(MediaStoreRelativePath, Environment.DIRECTORY_PICTURES + "/QRStudio")
            }
            val uri: Uri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values,
            ) ?: throw IOException("تعذر إنشاء ملف في التخزين")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(format, 95, out)) throw IOException("فشل ضغط الصورة")
            } ?: throw IOException("تعذر فتح ملف الإخراج")
            return Environment.DIRECTORY_PICTURES + "/QRStudio/" + fileName
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                if (!bitmap.compress(format, 95, out)) throw IOException("فشل ضغط الصورة")
            }
            return file.absolutePath
        }
    }

    fun saveSvg(context: Context, svg: String, displayName: String): String {
        val fileName = "$displayName.svg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStoreDisplayName, fileName)
                put(MediaStoreMimeType, "image/svg+xml")
                put(MediaStoreRelativePath, Environment.DIRECTORY_DOCUMENTS + "/QRStudio")
            }
            val uri: Uri = context.contentResolver.insert(
                android.provider.MediaStore.Files.getContentUri("external"), values,
            ) ?: throw IOException("تعذر إنشاء ملف في التخزين")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(svg.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("تعذر فتح ملف الإخراج")
            return Environment.DIRECTORY_DOCUMENTS + "/QRStudio/" + fileName
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            dir.mkdirs()
            val file = File(dir, fileName)
            file.writeText(svg, Charsets.UTF_8)
            return file.absolutePath
        }
    }

    fun shareImage(context: Context, bitmap: Bitmap, title: String) {
        val dir = File(context.cacheDir, "images")
        dir.mkdirs()
        val file = File(dir, "qr_share.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri: Uri = FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "qr", uri)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openUri(context: Context, raw: String): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(raw)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    // ثوابت أعمدة MediaStore (تجنب استيراد الأسماء الطويلة في كل مكان)
    private const val MediaStoreDisplayName = android.provider.MediaStore.MediaColumns.DISPLAY_NAME
    private const val MediaStoreMimeType = android.provider.MediaStore.MediaColumns.MIME_TYPE
    private const val MediaStoreRelativePath = android.provider.MediaStore.MediaColumns.RELATIVE_PATH
}
