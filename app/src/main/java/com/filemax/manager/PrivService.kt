package com.filemax.manager

import android.os.ParcelFileDescriptor
import java.io.File

/** Runs inside the Shizuku process (shell or root uid) as a UserService. Keep it free of app/UI dependencies. */
class PrivService : IPrivService.Stub() {
    override fun destroy() { System.exit(0) }

    override fun exec(cmd: String): String {
        val p = ProcessBuilder("sh", "-c", cmd).start()
        Thread { runCatching { p.errorStream.readBytes() } }.start()
        val out = p.inputStream.readBytes().toString(Charsets.UTF_8)
        return "${p.waitFor()}\n$out"
    }

    override fun openRead(path: String): ParcelFileDescriptor = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)

    override fun openWrite(path: String): ParcelFileDescriptor = ParcelFileDescriptor.open(File(path),
        ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE)
}
