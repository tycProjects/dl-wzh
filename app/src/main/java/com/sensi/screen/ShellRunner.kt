package com.sensi.screen

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Chạy lệnh shell bằng Shizuku hoặc root. Luôn gọi từ luồng nền. */
object ShellRunner {

    data class Result(val ok: Boolean, val output: String)

    @Volatile
    private var service: IShellService? = null

    @Volatile
    private var latch: CountDownLatch? = null

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IShellService.Stub.asInterface(binder)
            latch?.countDown()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

    fun shizukuRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    fun shizukuReady(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    fun rootAvailable(): Boolean {
        val r = runRoot("id")
        return r.ok && r.output.contains("uid=0")
    }

    fun runRoot(cmd: String): Result = try {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        val code = p.waitFor()
        Result(code == 0, if (code == 0) out.trim() else (err + out).trim())
    } catch (e: Exception) {
        Result(false, e.message ?: "Không chạy được su")
    }

    @Synchronized
    private fun shizukuService(ctx: Context): IShellService? {
        service?.let { if (it.asBinder().pingBinder()) return it }
        val l = CountDownLatch(1)
        latch = l
        val args = Shizuku.UserServiceArgs(
            ComponentName(ctx.packageName, ShellService::class.java.name)
        ).daemon(false).processNameSuffix("shell").version(1)
        Shizuku.bindUserService(args, conn)
        l.await(5, TimeUnit.SECONDS)
        return service
    }

    fun runShizuku(ctx: Context, cmd: String): Result {
        if (!shizukuReady()) return Result(false, "Shizuku chưa chạy hoặc chưa cấp quyền")
        return try {
            val s = shizukuService(ctx) ?: return Result(false, "Không kết nối được dịch vụ Shizuku")
            val out = s.exec(cmd)
            Result(!out.startsWith("ERR"), out)
        } catch (t: Throwable) {
            Result(false, t.message ?: "Lỗi Shizuku")
        }
    }

    /** mode: "auto" (ưu tiên Shizuku, không có thì root) | "shizuku" | "root" */
    fun run(ctx: Context, mode: String, cmd: String): Result = when (mode) {
        "shizuku" -> runShizuku(ctx, cmd)
        "root" -> runRoot(cmd)
        else -> if (shizukuReady()) runShizuku(ctx, cmd) else runRoot(cmd)
    }
}
