package com.sensi.screen

import android.content.Context
import java.util.concurrent.Executors

/** Áp dụng / khôi phục kích thước màn hình (wm size, wm density) theo thứ tự, trên 1 luồng nền. */
object ScreenController {
    private val exec = Executors.newSingleThreadExecutor()

    @Volatile
    private var currentPkg: String? = null

    /** true = do nút "Thử ngay" bật tay, dịch vụ tự động sẽ không khôi phục */
    @Volatile
    private var manualMode = false

    fun isAppliedFor(pkg: String): Boolean = currentPkg == pkg

    fun apply(
        ctx: Context,
        pkg: String,
        p: Profile,
        isManual: Boolean = false,
        onDone: ((ShellRunner.Result) -> Unit)? = null
    ) {
        val app = ctx.applicationContext
        exec.execute {
            val prefs = Prefs(app)
            if (!isManual && prefs.dirty && currentPkg == pkg) return@execute
            val cmd = buildString {
                append("wm size ${p.w}x${p.h}")
                if (p.d > 0) append("; wm density ${p.d}")
            }
            val r = ShellRunner.run(app, prefs.mode, cmd)
            if (r.ok) {
                prefs.dirty = true
                currentPkg = pkg
                manualMode = isManual
            }
            onDone?.invoke(r)
        }
    }

    fun reset(
        ctx: Context,
        force: Boolean = false,
        onDone: ((ShellRunner.Result) -> Unit)? = null
    ) {
        val app = ctx.applicationContext
        exec.execute {
            val prefs = Prefs(app)
            if (!force && (!prefs.dirty || manualMode)) return@execute
            val r = ShellRunner.run(app, prefs.mode, "wm size reset; wm density reset")
            if (r.ok) {
                prefs.dirty = false
                currentPkg = null
                manualMode = false
            }
            onDone?.invoke(r)
        }
    }
}
