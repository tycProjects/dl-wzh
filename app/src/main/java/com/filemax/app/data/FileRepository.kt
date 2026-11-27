package com.filemax.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

/** Real file-system access and basic operations for shared storage. */
class FileRepository(private val context: Context) {
    fun list(path: File, showHidden: Boolean): List<File> {
        if (!path.exists() || !path.isDirectory) return emptyList()
        return path.listFiles()?.filter { showHidden || !it.name.startsWith(".") }
            ?.sortedWith(compareBy<File>({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
            ?: emptyList()
    }

    fun createFolder(parent: File, name: String): File {
        validateName(name)
        val target = File(parent, name)
        check(!target.exists()) { "يوجد عنصر بهذا الاسم بالفعل" }
        check(target.mkdir()) { "تعذر إنشاء المجلد" }
        return target
    }

    fun createFile(parent: File, name: String): File {
        validateName(name)
        val target = File(parent, name)
        check(!target.exists()) { "يوجد عنصر بهذا الاسم بالفعل" }
        check(target.createNewFile()) { "تعذر إنشاء الملف" }
        return target
    }

    fun rename(source: File, newName: String): File {
        validateName(newName)
        val target = File(source.parentFile, newName)
        check(!target.exists()) { "الاسم مستخدم بالفعل" }
        check(source.renameTo(target)) { "فشلت إعادة التسمية" }
        return target
    }

    fun delete(source: File): Boolean = if (source.isDirectory) source.deleteRecursively() else source.delete()

    fun copy(source: File, destination: File): File {
        val target = File(destination, source.name)
        require(!target.canonicalPath.startsWith(source.canonicalPath + File.separator)) { "لا يمكن نسخ مجلد إلى داخله" }
        require(!target.exists()) { "الملف موجود في الوجهة" }
        if (source.isDirectory) {
            target.mkdirs()
            source.listFiles()?.forEach { copy(it, target) }
        } else {
            source.inputStream().use { input -> target.outputStream().use { out -> input.copyTo(out) } }
        }
        return target
    }

    fun move(source: File, destination: File): File {
        val target = File(destination, source.name)
        require(!target.exists()) { "الملف موجود في الوجهة" }
        if (source.renameTo(target)) return target
        copy(source, destination)
        check(delete(source)) { "تم النسخ لكن تعذر حذف الأصل" }
        return target
    }

    fun listSafTree(uri: Uri, showHidden: Boolean): List<DocumentFile> {
        val root = DocumentFile.fromTreeUri(context, uri)
        return root?.listFiles()?.filter { showHidden || !it.name.orEmpty().startsWith(".") }
            ?.sortedWith(compareBy<DocumentFile>({ !it.isDirectory }, { it.name.orEmpty().lowercase(Locale.ROOT) })) ?: emptyList()
    }

    private fun validateName(name: String) {
        require(name.isNotBlank()) { "الاسم لا يمكن أن يكون فارغاً" }
        require(name != "." && name != ".." && !name.contains('/') && !name.contains('\\')) { "اسم غير صالح" }
    }
}

