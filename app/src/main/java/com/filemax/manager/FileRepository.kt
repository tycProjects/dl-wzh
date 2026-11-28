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
    /** Entry reached through Root/Shizuku (e.g. Android/data); [path] is an absolute filesystem path. */
    data class Priv(val path: String, override val isDirectory: Boolean, override val size: Long, override val modified: Long) : StorageNode() {
        override val name get() = path.trimEnd('/').substringAfterLast('/').ifBlank { "/" }
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
            is StorageNode.Local -> if (Priv.available && Priv.restricted(node.file.absolutePath)) Priv.list(node.file.absolutePath) else {
                val files = node.file.listFiles() ?: throw IOException(
                    if (node.file.absolutePath.contains("/Android/data") || node.file.absolutePath.contains("/Android/obb"))
                        "أندرويد قيّد الوصول إلى هذا المجلد. يلزم تكامل Root أو Shizuku فعلي على هذا الجهاز."
                    else "تعذر قراءة محتويات هذا المجلد"
                )
                files.map { StorageNode.Local(it) }
            }
            is StorageNode.Saf -> node.document.listFiles().map { StorageNode.Saf(it) }
            is StorageNode.Priv -> Priv.list(node.path)
        }
        return items.filter { showHidden || !it.name.startsWith(".") }
            .sortedWith(compareBy<StorageNode> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }
    fun localFile(node: StorageNode): File? = (node as? StorageNode.Local)?.file
    fun parent(node: StorageNode): StorageNode? = when (node) {
        is StorageNode.Local -> node.file.parentFile?.let { StorageNode.Local(it) }
        is StorageNode.Saf -> null
        is StorageNode.Priv -> Priv.parentNode(node.path)
    }
    fun createFolder(parent: StorageNode, name: String) {
        validateName(name)
        when (parent) {
            is StorageNode.Local -> if (!File(parent.file, name).mkdir()) error("Could not create folder")
            is StorageNode.Saf -> if (parent.document.findFile(name) != null || parent.document.createDirectory(name) == null) error("Could not create folder")
            is StorageNode.Priv -> Priv.mkdir(parent.path.trimEnd('/') + "/" + name)
        }
    }
    fun createFile(parent: StorageNode, name: String) {
        validateName(name)
        when (parent) {
            is StorageNode.Local -> if (!File(parent.file, name).createNewFile()) error("Could not create file")
            is StorageNode.Saf -> if (parent.document.findFile(name) != null || parent.document.createFile("application/octet-stream", name) == null) error("Could not create file")
            is StorageNode.Priv -> Priv.createFile(parent.path.trimEnd('/') + "/" + name)
        }
    }
    fun rename(node: StorageNode, newName: String) {
        validateName(newName)
        val ok = when (node) {
            is StorageNode.Local -> node.file.renameTo(File(node.file.parentFile, newName))
            is StorageNode.Saf -> node.document.renameTo(newName)
            is StorageNode.Priv -> runCatching { Priv.rename(node.path, node.path.trimEnd('/').substringBeforeLast('/') + "/" + newName) }.isSuccess
        }
        if (!ok) error("Rename failed")
    }
    fun delete(node: StorageNode) {
        val ok = when (node) {
            is StorageNode.Local -> node.file.deleteRecursively()
            is StorageNode.Saf -> node.document.delete()
            is StorageNode.Priv -> runCatching { Priv.delete(node.path) }.isSuccess
        }
        if (!ok) error("Delete failed")
    }
    fun copyTo(node: StorageNode, destination: StorageNode, move: Boolean = false, policy: ExistsPolicy = ExistsPolicy.RENAME, ctl: TaskControl? = null) {
        if (node is StorageNode.Local && destination is StorageNode.Local && node.file.isDirectory) {
            val sourcePath = node.file.canonicalPath
            val destinationPath = destination.file.canonicalPath
            if (destinationPath == sourcePath || destinationPath.startsWith(sourcePath + File.separator)) {
                throw IOException("Cannot copy a folder into itself")
            }
        }
        val complete = copyNode(node, destination, policy, ctl)
        // A source is only removed when everything in it was really copied (nothing skipped).
        if (move && complete && !deleteSource(node)) throw IOException("Copied but source could not be deleted")
    }
    private fun findChild(dest: StorageNode, name: String): StorageNode? = when (dest) {
        is StorageNode.Local -> File(dest.file, name).takeIf { it.exists() }?.let { StorageNode.Local(it) }
        is StorageNode.Saf -> dest.document.findFile(name)?.let { StorageNode.Saf(it) }
        is StorageNode.Priv -> Priv.stat(dest.path.trimEnd('/') + "/" + name)
    }
    private fun sameNode(a: StorageNode, b: StorageNode) = when {
        a is StorageNode.Local && b is StorageNode.Local -> a.file.canonicalPath == b.file.canonicalPath
        a is StorageNode.Priv && b is StorageNode.Priv -> a.path == b.path
        a is StorageNode.Saf && b is StorageNode.Saf -> a.document.uri == b.document.uri
        else -> false
    }
    private fun uniqueChildName(dest: StorageNode, name: String): String {
        val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
        val base = name.substring(0, dot); val ext = name.substring(dot)
        var i = 1
        while (findChild(dest, "$base ($i)$ext") != null) i++
        return "$base ($i)$ext"
    }
    /** Returns true when the node was copied completely (false when something was skipped). */
    private fun copyNode(source: StorageNode, destination: StorageNode, policy: ExistsPolicy, ctl: TaskControl?): Boolean {
        ctl?.check()
        if (source.isDirectory) {
            val target = findChild(destination, source.name)?.takeIf { it.isDirectory } ?: createDirectory(destination, source.name)
            var all = true
            children(source, true).forEach { child -> if (!copyNode(child, target, policy, ctl)) all = false }
            return all
        }
        var name = source.name
        val existing = findChild(destination, name)
        if (existing != null) {
            ctl?.label = name
            val pol = if (policy == ExistsPolicy.ASK) (ctl?.stickyPolicy ?: ctl?.onExists?.invoke(File(destination.name, name)) ?: ExistsPolicy.RENAME) else policy
            when {
                pol == ExistsPolicy.SKIP -> return false
                pol == ExistsPolicy.RENAME || sameNode(source, existing) -> name = uniqueChildName(destination, name)
                else -> if (!deleteSource(existing)) throw IOException("Cannot replace existing file")
            }
        }
        val target = createDocumentOrFile(destination, name)
        openInput(source).use { input -> openOutput(target).use { output ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; output.write(buf, 0, n); ctl?.add(n.toLong()) }
        } }
        return true
    }
    private fun createDirectory(dest: StorageNode, name: String): StorageNode = when (dest) {
        is StorageNode.Local -> StorageNode.Local(File(dest.file, name).apply { if (exists() || !mkdir()) error("Cannot create folder") })
        is StorageNode.Saf -> StorageNode.Saf(dest.document.createDirectory(name) ?: error("Cannot create folder"))
        is StorageNode.Priv -> { val p = dest.path.trimEnd('/') + "/" + name; Priv.mkdir(p); StorageNode.Priv(p, true, 0, 0) }
    }
    private fun createDocumentOrFile(dest: StorageNode, name: String): StorageNode = when (dest) {
        is StorageNode.Local -> StorageNode.Local(File(dest.file, name).apply { if (exists()) error("Destination already exists") })
        is StorageNode.Saf -> StorageNode.Saf(dest.document.createFile("application/octet-stream", name) ?: error("Cannot create file"))
        is StorageNode.Priv -> { val p = dest.path.trimEnd('/') + "/" + name; if (Priv.exists(p)) error("Destination already exists"); StorageNode.Priv(p, false, 0, 0) }
    }
    private fun openInput(node: StorageNode): java.io.InputStream = when (node) {
        is StorageNode.Local -> FileInputStream(node.file)
        is StorageNode.Saf -> context.contentResolver.openInputStream(node.document.uri) ?: throw IOException("Cannot read source")
        is StorageNode.Priv -> Priv.openRead(node.path)
    }
    private fun openOutput(node: StorageNode): java.io.OutputStream = when (node) {
        is StorageNode.Local -> FileOutputStream(node.file)
        is StorageNode.Saf -> context.contentResolver.openOutputStream(node.document.uri, "w") ?: throw IOException("Cannot write destination")
        is StorageNode.Priv -> Priv.openWrite(node.path)
    }
    private fun deleteSource(node: StorageNode): Boolean = when (node) {
        is StorageNode.Local -> node.file.deleteRecursively()
        is StorageNode.Saf -> node.document.delete()
        is StorageNode.Priv -> runCatching { Priv.delete(node.path) }.isSuccess
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
