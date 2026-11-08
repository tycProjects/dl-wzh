package com.android.island.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuStatus { NOT_INSTALLED, NOT_RUNNING, PERMISSION_REQUIRED, READY }

object ShizukuState {
    private val _status = MutableStateFlow(ShizukuStatus.NOT_INSTALLED)
    val status = _status.asStateFlow()
    @Volatile private var appContext: Context? = null
    @Volatile private var started = false
    private const val REQUEST_CODE = 7135
    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    @Synchronized
    fun start(context: Context) {
        appContext = context.applicationContext
        if (!started) {
            started = runCatching {
                Shizuku.addBinderReceivedListenerSticky(binderReceived)
                Shizuku.addBinderDeadListener(binderDead)
                Shizuku.addRequestPermissionResultListener(permissionResult)
            }.isSuccess
        }
        refresh()
    }

    fun refresh() {
        val context = appContext ?: return
        val managerInstalled = listOf("moe.shizuku.privileged.api", "com.hamondev.shevery")
            .any { pkg -> runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess }
        _status.value = when {
            !managerInstalled -> ShizukuStatus.NOT_INSTALLED
            !runCatching { Shizuku.pingBinder() }.getOrDefault(false) -> ShizukuStatus.NOT_RUNNING
            runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false) ->
                ShizukuStatus.READY
            else -> ShizukuStatus.PERMISSION_REQUIRED
        }
    }

    fun requestPermission() {
        refresh()
        if (status.value == ShizukuStatus.READY) return
        if (status.value != ShizukuStatus.PERMISSION_REQUIRED) return
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
            .onFailure { refresh() }
    }

    fun openOrRequest(context: Context) {
        refresh()
        when (status.value) {
            ShizukuStatus.READY -> Toast.makeText(context, "Shizuku sudah siap", Toast.LENGTH_SHORT).show()
            ShizukuStatus.PERMISSION_REQUIRED -> requestPermission()
            ShizukuStatus.NOT_RUNNING -> {
                val manager = listOf("moe.shizuku.privileged.api", "com.hamondev.shevery")
                    .firstNotNullOfOrNull { pkg -> context.packageManager.getLaunchIntentForPackage(pkg) }
                if (manager != null) {
                    manager.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(manager)
                } else {
                    Toast.makeText(context, "Buka dan jalankan Shizuku terlebih dahulu", Toast.LENGTH_LONG).show()
                }
            }
            ShizukuStatus.NOT_INSTALLED -> {
                val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(market) }.onFailure {
                    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(web) }
                }
            }
        }
    }
}
