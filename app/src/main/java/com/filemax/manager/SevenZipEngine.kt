package com.filemax.manager

import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.ICryptoGetTextPassword
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.IOutCreateArchive7z
import net.sf.sevenzipjbinding.IOutCreateCallback
import net.sf.sevenzipjbinding.IOutItem7z
import net.sf.sevenzipjbinding.ISequentialInStream
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.OutItemFactory
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.Date

/**
 * Secondary engine built on 7-Zip-JBinding-4Android: reads CAB / LZH (and acts as a fallback for 7Z methods the
 * pure-Java reader does not support) and writes AES-256 encrypted 7Z with encrypted file names.
 * Passwords are only held as a String for the duration of the native call and never logged.
 */
object SevenZipEngine {
    private const val UNAVAILABLE = "محرك 7-Zip غير متاح على هذا الجهاز"

    private class ExtractCb(val out: OutputStream, val pw: String?, val ctl: TaskControl) : IArchiveExtractCallback, ICryptoGetTextPassword {
        @Volatile var result: ExtractOperationResult? = null
        @Volatile var error: Throwable? = null
        override fun getStream(index: Int, mode: ExtractAskMode): ISequentialOutStream? =
            if (mode != ExtractAskMode.EXTRACT) null else object : ISequentialOutStream {
                override fun write(data: ByteArray): Int {
                    try { out.write(data); ctl.add(data.size.toLong()) }
                    catch (t: Throwable) { error = t; throw SevenZipException("aborted") }
                    return data.size
                }
            }
        override fun prepareOperation(mode: ExtractAskMode) {}
        override fun setOperationResult(r: ExtractOperationResult) { result = r }
        override fun setTotal(total: Long) {}
        override fun setCompleted(done: Long) {}
        override fun cryptoGetTextPassword(): String = pw ?: ""
    }

    fun visit(file: File, password: CharArray?, ctl: TaskControl, visitor: (ArchiveEntryInfo, (OutputStream) -> Unit) -> Unit) {
        val pw = password?.takeIf { it.isNotEmpty() }?.let { String(it) }
        val raf = RandomAccessFile(file, "r")
        var arch: IInArchive? = null
        try {
            arch = try {
                if (pw == null) SevenZip.openInArchive(null, RandomAccessFileInStream(raf))
                else SevenZip.openInArchive(null, RandomAccessFileInStream(raf), pw)
            } catch (e: LinkageError) { throw IOException(UNAVAILABLE, e)
            } catch (e: SevenZipException) { throw IOException("تعذر فتح الأرشيف: ${e.message.orEmpty()}", e) }
            val a = arch!!
            for (i in 0 until a.numberOfItems) {
                ctl.check()
                val isDir = a.getProperty(i, PropID.IS_FOLDER) as? Boolean ?: false
                val path = (a.getProperty(i, PropID.PATH) as? String).orEmpty().replace('\\', '/').trimEnd('/')
                val enc = a.getProperty(i, PropID.ENCRYPTED) as? Boolean ?: false
                val info = ArchiveEntryInfo(path, isDir,
                    (a.getProperty(i, PropID.SIZE) as? Number)?.toLong() ?: 0L,
                    (a.getProperty(i, PropID.PACKED_SIZE) as? Number)?.toLong() ?: -1L,
                    (a.getProperty(i, PropID.LAST_MODIFICATION_TIME) as? Date)?.time ?: 0L,
                    (a.getProperty(i, PropID.CRC) as? Number)?.toLong()?.and(0xFFFFFFFFL) ?: -1L,
                    (a.getProperty(i, PropID.METHOD) as? String).orEmpty(), enc)
                if (!isDir && enc && pw == null) throw PasswordException(false)
                visitor(info) { out ->
                    val cb = ExtractCb(out, pw, ctl)
                    try { a.extract(intArrayOf(i), false, cb) }
                    catch (e: SevenZipException) {
                        cb.error?.let { throw it }
                        throw IOException("فشل الاستخراج: ${e.message.orEmpty()}", e)
                    }
                    cb.error?.let { throw it }
                    when (val r = cb.result?.name) {
                        null, "OK" -> {}
                        "UNSUPPORTEDMETHOD" -> throw IOException("طريقة الضغط غير مدعومة")
                        "DATAERROR", "CRCERROR", "WRONGPASSWORD" -> if (enc) throw PasswordException(pw != null) else throw IOException("بيانات الأرشيف تالفة")
                        else -> throw IOException("فشل الاستخراج: $r")
                    }
                }
            }
        } finally {
            runCatching { arch?.close() }
            runCatching { raf.close() }
        }
    }

    private class CreateCb(val entries: List<Pair<File, String>>, val pw: String, val ctl: TaskControl, val opened: MutableList<RandomAccessFile>) :
        IOutCreateCallback<IOutItem7z>, ICryptoGetTextPassword {
        @Volatile var error: Throwable? = null
        override fun setTotal(total: Long) {}
        override fun setCompleted(done: Long) {}
        override fun setOperationResult(ok: Boolean) {}
        override fun cryptoGetTextPassword(): String = pw
        override fun getItemInformation(index: Int, factory: OutItemFactory<IOutItem7z>): IOutItem7z {
            val (f, name) = entries[index]
            val item = factory.createOutItem()
            if (f.isDirectory) item.setPropertyIsDir(true) else item.setDataSize(f.length())
            item.setPropertyPath(name)
            item.setPropertyLastModificationTime(Date(f.lastModified()))
            return item
        }
        override fun getStream(index: Int): ISequentialInStream? {
            val f = entries[index].first
            if (f.isDirectory) return null
            return try {
                val r = RandomAccessFile(f, "r").also { opened += it }
                object : ISequentialInStream {
                    override fun read(data: ByteArray): Int {
                        val n = r.read(data)
                        if (n <= 0) return 0
                        try { ctl.add(n.toLong()) } catch (t: Throwable) { error = t; throw SevenZipException("aborted") }
                        return n
                    }
                }
            } catch (t: Throwable) { error = t; throw SevenZipException("aborted") }
        }
    }

    /** Creates an AES-256 7Z whose headers (file names) are encrypted too. [entries] are (file, path inside archive). */
    fun create7z(entries: List<Pair<File, String>>, output: File, o: CreateOptions, ctl: TaskControl) {
        val pw = String(requireNotNull(o.password) { "كلمة المرور مطلوبة" })
        val opened = ArrayList<RandomAccessFile>()
        var raf: RandomAccessFile? = null
        var arch: IOutCreateArchive7z? = null
        val cb = CreateCb(entries, pw, ctl, opened)
        try {
            raf = RandomAccessFile(output, "rw")
            arch = SevenZip.openOutArchive7z()
            arch.setLevel(if (o.level <= 0) 0 else if (o.level <= 1) 1 else if (o.level >= 9) 9 else 5)
            arch.setHeaderEncryption(true)
            arch.createArchive(RandomAccessFileOutStream(raf), entries.size, cb)
        } catch (e: LinkageError) { throw IOException(UNAVAILABLE, e)
        } catch (e: SevenZipException) {
            cb.error?.let { throw it }
            throw IOException("فشل إنشاء الأرشيف: ${e.message.orEmpty()}", e)
        } finally {
            opened.forEach { runCatching { it.close() } }
            runCatching { arch?.close() }
            runCatching { raf?.close() }
        }
        cb.error?.let { throw it }
    }
}