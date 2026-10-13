package com.flipper.app.xposed

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Dijalankan DI DALAM proses game target (lewat FlipperXposedHook), bukan di
 * proses Flipper. Membuka satu server socket di 127.0.0.1 supaya Flipper
 * (yang jalan sebagai aplikasi biasa di luar virtual space) bisa menyambung
 * dari jauh dan memakai InProcessEngine untuk baca/tulis memori proses ini.
 *
 * Nomor port dihitung dari nama paket (lihat portFor), bukan port tetap,
 * supaya beberapa game yang di-hook bersamaan tidak rebutan port, dan supaya
 * Flipper bisa "menebak" port yang benar tanpa mekanisme penemuan tambahan.
 */
object SelfMemServer {
    private val started = AtomicBoolean(false)

    fun startOnceFor(pkg: String) {
        if (!started.compareAndSet(false, true)) return
        val port = portFor(pkg)
        Thread {
            try {
                val server = ServerSocket(port, 8, InetAddress.getByName("127.0.0.1"))
                while (true) {
                    val sock = server.accept()
                    Thread { handleClient(sock) }.start()
                }
            } catch (_: Exception) {
                // Port dipakai proses lain, atau jaringan diblokir -- Flipper akan
                // melaporkan "gagal konek" ke pengguna kalau ini terjadi.
            }
        }.apply { isDaemon = true }.start()
    }

    /** Port 20000-20998, dihitung deterministik dari nama paket. */
    fun portFor(pkg: String): Int = 20000 + (kotlin.math.abs(pkg.hashCode()) % 999)

    private fun handleClient(sock: Socket) {
        var mem: RandomAccessFile? = null
        try {
            sock.soTimeout = 120_000
            val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
            val writer: BufferedWriter = BufferedWriter(OutputStreamWriter(sock.getOutputStream()))
            mem = RandomAccessFile("/proc/self/mem", "rw")
            val engine = InProcessEngine(mem, writer)
            while (true) {
                val line = reader.readLine() ?: break
                if (!engine.processLine(line)) break
            }
        } catch (_: Exception) {
        } finally {
            try { mem?.close() } catch (_: Exception) {}
            try { sock.close() } catch (_: Exception) {}
        }
    }
}
