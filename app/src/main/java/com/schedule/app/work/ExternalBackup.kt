package com.schedule.app.work

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.schedule.app.data.Prefs
import java.io.File

/**
 * نسخة احتياطية إلى مجلد يختاره المستخدم عبر مزوّد المستندات (Storage Access Framework).
 * إن كان تطبيق Google Drive مثبتا فيظهر كمزوّد، وبذلك تُرفع النسخة إلى حسابك. هذه نسخ احتياطي أحادي الاتجاه وليست مزامنة بين أجهزة.
 */
object ExternalBackup {
    fun copyTo(ctx: Context, src: File): Boolean {
        val tree = Prefs(ctx).read().extTree.takeIf { it.isNotBlank() }?.let(Uri::parse) ?: return false
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val doc = DocumentsContract.createDocument(ctx.contentResolver, parent, "application/zip", src.name) ?: return false
        ctx.contentResolver.openOutputStream(doc)?.use { o -> src.inputStream().use { it.copyTo(o, 64 * 1024) } } ?: return false
        return true
    }
}
