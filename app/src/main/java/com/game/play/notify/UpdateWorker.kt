package com.game.play.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.game.play.data.SettingsStore
import com.game.play.data.StoreRepository
import com.game.play.data.install.ApkInstaller
import com.game.play.domain.model.hasUpdate
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/** Cek berkala: aplikasi baru di store + pembaruan untuk aplikasi yang sudah terpasang. */
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val settings = SettingsStore(ctx).flow.first()
        if (!settings.notifications) return Result.success()

        val repo = StoreRepository(ctx)
        val old = repo.cachedApps()
        val fresh = repo.refreshApps() ?: return Result.retry()

        val oldIds = old.map { it.id }.toSet()
        val newApps = if (oldIds.isEmpty()) emptyList() else fresh.filter { it.id !in oldIds }
        if (newApps.isNotEmpty()) {
            Notifier.info(ctx, 2001, "Aplikasi baru di PorStore", newApps.take(3).joinToString(", ") { it.name })
        }

        val installed = ApkInstaller(ctx).installedVersions(fresh.mapNotNull { it.packageName })
        val updates = fresh.filter { app -> app.hasUpdate(app.packageName?.let { installed[it] }) }
        if (updates.isNotEmpty()) {
            val signature = updates.joinToString("|") { it.id + ":" + it.versionCode + ":" + it.versionName }
            val prefs = ctx.getSharedPreferences("update_notified", Context.MODE_PRIVATE)
            if (prefs.getString("last", "") != signature) {
                Notifier.info(ctx, 2002, "Pembaruan tersedia", updates.take(3).joinToString(", ") { it.name })
                prefs.edit().putString("last", signature).apply()
            }
        }
        return Result.success()
    }
}

object UpdateScheduler {
    private const val NAME = "porstore_update_check"

    fun apply(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<UpdateWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
