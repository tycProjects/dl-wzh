package com.flipper.app.xposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

/**
 * Titik masuk modul Xposed (didaftarkan lewat assets/xposed_init). Hanya
 * aktif kalau Flipper dipasang di VirtualXposed (atau Xposed/LSPosed asli)
 * DAN modul ini diaktifkan secara manual di sana untuk game target -- lihat
 * README bagian "Jalan Tanpa Root (lewat VirtualXposed)".
 *
 * Saat aplikasi target dimuat, cukup nyalakan SelfMemServer (sekali per
 * proses) -- server itu yang mengurus sisanya (baca/tulis/scan memori proses
 * ini sendiri, tanpa butuh root sama sekali).
 */
class FlipperXposedHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName == "android") return
        if (lpparam.packageName == lpparam.appInfo?.packageName && lpparam.packageName.startsWith("com.flipper.app")) {
            // Jangan hook diri sendiri (Flipper tidak perlu di-scan dirinya sendiri).
            return
        }
        SelfMemServer.startOnceFor(lpparam.packageName)
    }
}
