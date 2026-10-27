package com.localaide.local_aide

import android.content.Context
import android.system.Os
import android.util.Log
import id.or.oo.pr.engine.ProotHost
import java.io.File
import java.io.FileOutputStream

/** Small app-scoped bootstrap: wire the embedded native libraries into PRoot's prefix. */
internal class LocalAideBootstrap(
    private val context: Context,
    private val host: ProotHost,
) {
    fun run() {
        val bin = File(host.prefixDir, "bin")
        bin.mkdirs()
        host.homeDir.mkdirs()
        File(host.prefixDir, "tmp").mkdirs()
        File(host.prefixDir, "var/lib/pr/installed-rootfs").mkdirs()
        File(host.prefixDir, "var/lib/pr/dlcache").mkdirs()

        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        link(nativeDir, bin, "busybox", "libbusybox.so")
        link(nativeDir, bin, "proot", "libproot.so")
        link(nativeDir, bin, "pr-cli", "libpr-cli.so")

        copyAssetIfPresent("scripts/bootstrap.sh", File(bin, "bootstrap.sh"))
        val appletFile = runCatching { context.assets.open("bin/busybox.applets") }.getOrNull()
        if (appletFile != null) {
            appletFile.bufferedReader().useLines { lines ->
                lines.map { it.trim().removePrefix("\"").removeSuffix("\"").removeSuffix(",") }
                    .filter { it.matches(Regex("[A-Za-z0-9_.+-]+")) }
                    .forEach { applet ->
                        val target = File(bin, applet)
                        if (!target.exists()) {
                            runCatching { Os.symlink("busybox", target.absolutePath) }
                                .onFailure { Log.w(TAG, "Could not link busybox applet $applet", it) }
                        }
                    }
            }
        }
        Log.i(TAG, "Embedded PRoot prefix initialized at ${host.prefixDir}")
    }

    private fun link(nativeDir: File, binDir: File, linkName: String, libraryName: String) {
        val target = File(nativeDir, libraryName)
        if (!target.exists()) {
            Log.e(TAG, "Missing $libraryName in $nativeDir. Build the APK with scripts/build_ide_debug.sh.")
            return
        }
        val link = File(binDir, linkName)
        runCatching { if (link.exists() || link.isSymbolicLinkCompat()) link.delete() }
        try {
            Os.symlink(target.absolutePath, link.absolutePath)
        } catch (error: Exception) {
            if (link.canonicalPath != target.canonicalPath) throw error
        }
    }

    private fun File.isSymbolicLinkCompat(): Boolean = runCatching {
        Os.readlink(absolutePath)
        true
    }.getOrDefault(false)

    private fun copyAssetIfPresent(assetPath: String, destination: File) {
        val stream = runCatching { context.assets.open(assetPath) }.getOrNull() ?: return
        stream.use { input -> FileOutputStream(destination).use { output -> input.copyTo(output) } }
        destination.setExecutable(true, false)
        destination.setReadable(true, false)
    }

    private companion object { const val TAG = "LocalAIDE" }
}
