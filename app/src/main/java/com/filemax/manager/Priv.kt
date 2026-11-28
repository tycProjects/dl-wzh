package com.filemax.manager

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import rikka.shizuku.Shizuku
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Privileged file access (Android/data, Android/obb, ...) through Shizuku's UserService or a root `su` shell. */
object Priv {
    interface Backend {
        val label: String
        fun exec(cmd: String): Pair<Int, String>
        fun openRead(path: String): InputStream
        fun openWrite(path: String): OutputStream
    }

    @Volatile var backend: Backend? = null
    val available: Boolean get() = backend != null
    private var conn: ServiceConnection? = null
    private val RESTRICTED = Regex("/Android/(data|obb)(/|\$)")

    fun restricted(path: String) = RESTRICTED.containsMatchIn(path)
    fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"
    private fun req(): Backend = backend ?: throw IOException("لا يوجد اتصال مميّز (Root/Shizuku)")
    private fun sh(cmd: String): String {
        val (code, out) = req().exec(cmd)
        if (code != 0) throw IOException("فشل الأمر المميّز ($code)")
        return out
    }

    // ---- connection ----
    fun connect(context: Context): Backend {
        backend?.let { return it }
        synchronized(this) {
            backend?.let { return it }
            var hint: String? = null
            val b = try { tryShizuku(context) } catch (e: IOException) { hint = e.message; null } ?: tryRoot()
                ?: throw IOException(hint ?: "لا يوجد Shizuku متصل ولا صلاحية Root على هذا الجهاز")
            backend = b
            return b
        }
    }

    private fun tryShizuku(context: Context): Backend? {
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return null
        if (runCatching { Shizuku.checkSelfPermission() }.getOrDefault(-1) != PackageManager.PERMISSION_GRANTED) {
            runCatching { Shizuku.requestPermission(41) }
            throw IOException("امنح صلاحية Shizuku للتطبيق ثم أعد المحاولة")
        }
        var svc: IPrivService? = null
        val latch = CountDownLatch(1)
        val c = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) { svc = IPrivService.Stub.asInterface(binder); latch.countDown() }
            override fun onServiceDisconnected(name: ComponentName) { backend = null }
        }
        conn = c
        val args = Shizuku.UserServiceArgs(ComponentName(context.packageName, PrivService::class.java.name))
            .daemon(false).processNameSuffix("priv").debuggable(false).version(4)
        Shizuku.bindUserService(args, c)
        if (!latch.await(10, TimeUnit.SECONDS)) throw IOException("انتهت مهلة الاتصال بخدمة Shizuku")
        val s = svc ?: return null
        return object : Backend {
            override val label = "Shizuku"
            override fun exec(cmd: String): Pair<Int, String> {
                val r = s.exec(cmd); val i = r.indexOf('\n')
                return r.substring(0, i).toInt() to r.substring(i + 1)
            }
            override fun openRead(path: String): InputStream = ParcelFileDescriptor.AutoCloseInputStream(s.openRead(path))
            override fun openWrite(path: String): OutputStream = ParcelFileDescriptor.AutoCloseOutputStream(s.openWrite(path))
        }
    }

    private class RootBackend : Backend {
        override val label = "Root"
        private fun start(cmd: String): Process = ProcessBuilder("su", "-c", cmd).start().also { p -> Thread { runCatching { p.errorStream.readBytes() } }.start() }
        override fun exec(cmd: String): Pair<Int, String> {
            val p = start(cmd)
            val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
            return p.waitFor() to out
        }
        override fun openRead(path: String): InputStream {
            val p = start("cat ${q(path)}")
            return object : java.io.FilterInputStream(p.inputStream) { override fun close() { super.close(); p.destroy() } }
        }
        override fun openWrite(path: String): OutputStream {
            val p = start("cat > ${q(path)}")
            return object : java.io.FilterOutputStream(p.outputStream) {
                override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len) }
                override fun close() { runCatching { out.close() }; if (p.waitFor() != 0) throw IOException("فشل الكتابة بصلاحية Root") }
            }
        }
    }

    private fun tryRoot(): Backend? = runCatching {
        val p = ProcessBuilder("su", "-c", "id").start()
        Thread { runCatching { p.errorStream.readBytes() } }.start()
        val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
        if (p.waitFor(30, TimeUnit.SECONDS) && p.exitValue() == 0 && out.contains("uid=0")) RootBackend() else null
    }.getOrNull()

    // ---- file operations ----
    fun list(path: String): List<StorageNode.Priv> {
        val base = path.trimEnd('/')
        val out = sh("cd ${q(base)} || exit 3; for f in .[!.]* ..?* *; do if [ -e \"\$f\" ]; then stat -L -c '%F|%s|%Y|%n' \"./\$f\"; fi; done")
        return out.lineSequence().mapNotNull { line ->
            val p = line.split('|', limit = 4)
            if (p.size < 4) return@mapNotNull null
            val size = p[1].toLongOrNull() ?: return@mapNotNull null
            val name = p[3].removePrefix("./")
            StorageNode.Priv("$base/$name", p[0].startsWith("directory"), size, (p[2].toLongOrNull() ?: 0L) * 1000)
        }.toList()
    }
    fun stat(path: String): StorageNode.Priv? {
        val (code, out) = req().exec("stat -L -c '%F|%s|%Y' ${q(path)}")
        if (code != 0) return null
        val p = out.trim().split('|')
        if (p.size < 3) return null
        return StorageNode.Priv(path, p[0].startsWith("directory"), p[1].toLongOrNull() ?: 0L, (p[2].toLongOrNull() ?: 0L) * 1000)
    }
    fun parentNode(path: String): StorageNode? {
        val p = File(path).parent ?: return null
        return if (restricted(p)) StorageNode.Priv(p, true, 0, 0) else StorageNode.Local(File(p))
    }
    fun exists(path: String) = req().exec("[ -e ${q(path)} ]").first == 0
    fun mkdir(path: String) { sh("mkdir ${q(path)}") }
    fun createFile(path: String) { sh("[ ! -e ${q(path)} ] && : > ${q(path)}") }
    fun delete(path: String) { sh("rm -rf ${q(path)}") }
    fun rename(from: String, to: String) { sh("[ ! -e ${q(to)} ] && mv ${q(from)} ${q(to)}") }
    fun openRead(path: String): InputStream = req().openRead(path)
    fun openWrite(path: String): OutputStream = req().openWrite(path)
}
