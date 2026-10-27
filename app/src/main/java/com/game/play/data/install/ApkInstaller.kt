package com.game.play.data.install

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.game.play.data.api.ApiClient
import com.game.play.domain.model.AppInfo
import com.game.play.domain.model.InstalledInfo
import com.game.play.notify.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException

object Net {
    private fun caps(context: Context): NetworkCapabilities? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = cm.activeNetwork ?: return null
        return cm.getNetworkCapabilities(network)
    }

    fun isOnline(context: Context): Boolean =
        caps(context)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    /** Wi-Fi atau Ethernet (bukan data seluler). */
    fun isWifi(context: Context): Boolean {
        val c = caps(context) ?: return false
        return c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}

class ApkInstaller(private val context: Context) {

    suspend fun download(app: AppInfo, url: String, notify: Boolean, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "apks").apply { mkdirs() }
            val out = File(dir, app.id.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".apk")
            val nid = Notifier.idFor(app.id)
            try {
                val request = Request.Builder().url(url).build()
                ApiClient.http.newCall(request).execute().use { res ->
                    if (!res.isSuccessful) throw IOException("Server unduhan membalas ${res.code}")
                    val body = res.body ?: throw IOException("Respons unduhan kosong")
                    val total = body.contentLength()
                    body.byteStream().use { input ->
                        out.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var readTotal = 0L
                            var lastPct = -1
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                readTotal += n
                                ensureActive()
                                if (total > 0) {
                                    val pct = (readTotal * 100 / total).toInt()
                                    onProgress(pct / 100f)
                                    if (pct != lastPct && pct % 5 == 0) {
                                        lastPct = pct
                                        if (notify) Notifier.progress(context, nid, app.name, pct)
                                    }
                                }
                            }
                        }
                    }
                }
                val header = ByteArray(4)
                val read = out.inputStream().use { it.read(header) }
                if (read != 4 || header[0] != 0x50.toByte() || header[1] != 0x4b.toByte()) {
                    throw IOException("File yang diunduh bukan APK yang valid.")
                }
            } catch (e: Exception) {
                out.delete()
                Notifier.cancel(context, nid)
                throw e
            }
            out
        }

    fun installIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun needsInstallPermission(): Boolean =
        Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()

    fun permissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** package -> info versi, hanya untuk yang benar-benar terpasang. */
    fun installedVersions(packages: Collection<String>): Map<String, InstalledInfo> {
        val result = HashMap<String, InstalledInfo>()
        for (pkg in packages.toSet()) {
            try {
                val info = context.packageManager.getPackageInfo(pkg, 0)
                result[pkg] = InstalledInfo(info.versionName ?: "", PackageInfoCompat.getLongVersionCode(info))
            } catch (e: PackageManager.NameNotFoundException) {
                // belum terpasang
            }
        }
        return result
    }

    fun launch(pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
