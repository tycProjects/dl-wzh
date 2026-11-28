package com.filemax.manager

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

sealed class StorageNode {
    abstract val name: String
    abstract val isDirectory: Boolean
    abstract val size: Long
    abstract val modified: Long
    data class Local(val file: File) : StorageNode() {
        override val name get() = file.name.ifBlank { "Internal storage" }
        override val isDirectory get() = file.isDirectory
        override val size get() = if (file.isFile) file.length() else 0L
        override val modified get() = file.lastModified()
    }
    data class Saf(val document: DocumentFile) : StorageNode() {
        override val name get() = document.name ?: "Unnamed"
        override val isDirectory get() = document.isDirectory
        override val size get() = document.length()
        override val modified get() = document.lastModified()
    }
}

class FileRepository(private val context: Context) {
    fun defaultRoot(): StorageNode = StorageNode.Local(Environment.getExternalStorageDirectory())
    fun safRoot(uri: Uri): StorageNode.Saf {
        val doc = DocumentFile.fromTreeUri(context, uri) ?: error("Invalid folder")
        return StorageNode.Saf(doc)
    }
    fun children(node: StorageNode, showHidden: Boolean): List<StorageNode> {
        val items = when (node) {
            is StorageNode.Local -> {
                val files = node.file.listFiles() ?: throw IOException(
                    if (node.file.absolutePath.contains("/Android/data") || node.file.absolutePath.contains("/Android/obb"))
                        "أندرويد قيّد الوصول إلى هذا المجلد. يلزم تكامل Root أو Shizuku فعلي على هذا الجهاز."
                    else "تعذر قراءة محتويات هذا المجلد"
                )
                files.map { StorageNode.Local(it) }
            }
            is StorageNode.Saf -> node.document.listFiles().map { StorageNode.Saf(it) }
        }
        return items.filter { showHidden || !it.name.startsWith(".") }
            .sortedWith(compareBy<StorageNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }
    fun localFile(node: StorageNode): File? = (node as? StorageNode.Local)?.file
    fun parent(node: StorageNode): StorageNode? = when (node) {
        is StorageNode.Local -> node.file.parentFile?.let { StorageNode.Local(it) }
        is StorageNode.Saf -> null
    }
    fun createFolder(parent: StorageNode, name: String) {
        validateName(name)
        when (parent) {
            is StorageNode.Local -> if (!File(parent.file, name).mkdir()) error("Could not create folder")
            is StorageNode.Saf -> if (parent.document.findFile(name) != null || parent.document.createDirectory(name) == null) error("Could not create folder")
        }
    }
    fun createFile(parent: StorageNode, name: String) {
        validateName(name)
        when (parent) {
            is StorageNode.Local -> if (!File(parent.file, name).createNewFile()) error("Could not create file")
            is StorageNode.Saf -> if (parent.document.findFile(name) != null || parent.document.createFile("application/octet-stream", name) == null) error("Could not create file")
        }
    }
    fun rename(node: StorageNode, newName: String) {
        validateName(newName)
        val ok = when (node) {
            is StorageNode.Local -> node.file.renameTo(File(node.file.parentFile, newName))
            is StorageNode.Saf -> node.document.renameTo(newName)
        }
        if (!ok) error("Rename failed")
    }
    fun delete(node: StorageNode) {
        val ok = when (node) {
            is StorageNode.Local -> node.file.deleteRecursively()
            is StorageNode.Saf -> node.document.delete()
        }
        if (!ok) error("Delete failed")
    }
    fun copyTo(node: StorageNode, destination: StorageNode, move: Boolean = false) {
        if (node is StorageNode.Local && destination is StorageNode.Local && node.file.isDirectory) {
            val sourcePath = node.file.canonicalPath
            val destinationPath = destination.file.canonicalPath
            if (destinationPath == sourcePath || destinationPath.startsWith(sourcePath + File.separator)) {
                throw IOException("Cannot copy a folder into itself")
            }
        }
        copyNode(node, destination)
        if (move && !deleteSource(node)) throw IOException("Copied but source could not be deleted")
    }
    private fun copyNode(source: StorageNode, destination: StorageNode): StorageNode {
        if (source.isDirectory) {
            val target = createDirectory(destination, source.name)
            children(source, true).forEach { child -> copyNode(child, target) }
            return target
        }
        val target = createDocumentOrFile(destination, source.name)
        openInput(source).use { input -> openOutput(target).use { output -> input.copyTo(output, 64 * 1024) } }
        return target
    }
    private fun createDirectory(dest: StorageNode, name: String): StorageNode = when (dest) {
        is StorageNode.Local -> StorageNode.Local(File(dest.file, name).apply { if (exists() || !mkdir()) error("Cannot create folder") })
        is StorageNode.Saf -> StorageNode.Saf(dest.document.createDirectory(name) ?: error("Cannot create folder"))
    }
    private fun createDocumentOrFile(dest: StorageNode, name: String): StorageNode = when (dest) {
        is StorageNode.Local -> StorageNode.Local(File(dest.file, name).apply { if (exists()) error("Destination already exists") })
        is StorageNode.Saf -> StorageNode.Saf(dest.document.createFile("application/octet-stream", name) ?: error("Cannot create file"))
    }
    private fun openInput(node: StorageNode): java.io.InputStream = when (node) {
        is StorageNode.Local -> FileInputStream(node.file)
        is StorageNode.Saf -> context.contentResolver.openInputStream(node.document.uri) ?: throw IOException("Cannot read source")
    }
    private fun openOutput(node: StorageNode): java.io.OutputStream = when (node) {
        is StorageNode.Local -> FileOutputStream(node.file)
        is StorageNode.Saf -> context.contentResolver.openOutputStream(node.document.uri, "w") ?: throw IOException("Cannot write destination")
    }
    private fun deleteSource(node: StorageNode): Boolean = when (node) {
        is StorageNode.Local -> node.file.deleteRecursively()
        is StorageNode.Saf -> node.document.delete()
    }
    private fun copyDirectory(source: File, target: File) {
        if (!target.exists() && !target.mkdirs()) error("Cannot create destination")
        source.listFiles()?.forEach { child ->
            val out = File(target, child.name)
            if (child.isDirectory) copyDirectory(child, out) else child.inputStream().use { input -> out.outputStream().use { output -> input.copyTo(output) } }
        }
    }
    private fun validateName(name: String) {
        require(name.isNotBlank() && name != "." && name != ".." && !name.contains('/') && !name.contains('\\')) { "Invalid name" }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = -1
    do { value /= 1024.0; index++ } while (value >= 1024 && index < units.lastIndex)
    return String.format(java.util.Locale.US, "%.1f %s", value, units[index])
}
