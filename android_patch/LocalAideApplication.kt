package com.localaide.local_aide

import android.app.Application
import android.net.ConnectivityManager
import android.util.Log
import id.or.oo.pr.engine.NetworkDns
import id.or.oo.pr.engine.ProotHost
import java.net.Inet6Address
import java.io.File

/** App-scoped host paths for the embedded PRoot engine. */
class LocalAideApplication : Application(), ProotHost {
    override val prefixDir: File get() = File(filesDir, "usr")
    override val homeDir: File get() = File(filesDir, "home")

    @get:JvmName("getProotPackageName")
    override val packageName: String get() = super.getPackageName()

    @get:JvmName("getProotCacheDir")
    override val cacheDir: File get() = super.getCacheDir()

    override val dnsServers: List<String>
        get() {
            val manager = getSystemService(ConnectivityManager::class.java) ?: return emptyList()
            val network = manager.activeNetwork ?: return emptyList()
            val link = manager.getLinkProperties(network) ?: return emptyList()
            val hasIpv6DefaultRoute = link.routes.any {
                it.destination.prefixLength == 0 && it.destination.address is Inet6Address
            }
            return NetworkDns.usableServers(link.dnsServers, hasIpv6DefaultRoute)
        }

    override fun onCreate() {
        super.onCreate()
        try {
            LocalAideBootstrap(this, this).run()
        } catch (error: Throwable) {
            Log.e(TAG, "Embedded PRoot bootstrap failed", error)
        }
    }

    private companion object {
        const val TAG = "LocalAIDE"
    }
}
