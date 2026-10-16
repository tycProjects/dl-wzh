package com.smali.fox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread
import java.util.concurrent.atomic.AtomicBoolean

class DeepSeekBatchService : Service() {
    companion object {
        const val ACTION_START = "com.smali.fox.START_DEEPSEEK_BATCH"
        private const val CHANNEL = "deepseek_background"
        private const val NOTIFICATION_ID = 3001
        private val processing = AtomicBoolean(false)
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, notification("جاري تجهيز تحليل DeepSeek…"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START && processing.compareAndSet(false, true)) {
            thread(start = true, name = "deepseek-batch-service") { processQueue() }
        }
        return START_REDELIVER_INTENT
    }

    private fun processQueue() {
        val prefs = getSharedPreferences("deepseek", MODE_PRIVATE)
        val token = prefs.getString("api_key", "").orEmpty().ifBlank { BuildConfig.DEEPSEEK_TOKEN }
        val reports = getSharedPreferences("deepseek_reports", MODE_PRIVATE)
        val queueFile = java.io.File(filesDir, "deepseek_z_queue.json")
        var preserveQueue = false
        try {
            val source = JSONArray(queueFile.readText())
            val functions = (0 until source.length()).map { index ->
                val item = source.getJSONObject(index)
                DeepSeekClient.BatchFunction(item.getString("signature"), item.getString("smali"))
            }
            val batches = functions.asReversed().chunked(500)
            var successCount = 0
            var combined = reports.getString("last_report", "").orEmpty()
            val savedBatch = reports.getInt("batch_current", 1).coerceAtLeast(1)
            val startIndex = (savedBatch - 1).coerceAtMost((batches.size - 1).coerceAtLeast(0))
            reports.edit().putInt("batch_total", batches.size).putString("batch_state", "بدأ التحليل في الخلفية").apply()
            batches.drop(startIndex).forEachIndexed { index, batch ->
                val number = startIndex + index + 1
                reports.edit().putInt("batch_current", number).putString("batch_state", "جاري تحليل الدفعة $number/${batches.size} في الخلفية").apply()
                updateNotification("DeepSeek: الدفعة $number/${batches.size}")
                if (!hasNetwork()) {
                    preserveQueue = true
                    reports.edit().putString("batch_state", "لا يوجد إنترنت — إيقاف الإرسال قبل الدفعة $number").apply()
                    updateNotification("لا يوجد إنترنت — تم إيقاف الإرسال")
                    if (!waitForNetwork()) {
                        reports.edit().putString("batch_state", "توقف آمن: لم تعد الشبكة خلال المهلة").apply()
                        updateNotification("توقف آمن — لا يوجد اتصال")
                        return
                    }
                    Thread.sleep(1200L)
                }
                var chunks = 0
                var result = DeepSeekClient(this).analyzeBatch(token, batch, number, batches.size, onChunk = { chunks++ })
                var networkRetries = 0
                while (result.isFailure && isRetryableNetwork(result.exceptionOrNull()!!)) {
                    preserveQueue = true
                    networkRetries++
                    if (networkRetries > 5) {
                        reports.edit().putString("batch_state", "توقف آمن بعد تكرار فشل الشبكة في الدفعة $number").apply()
                        updateNotification("توقف آمن — يمكنك المحاولة لاحقًا")
                        return
                    }
                    reports.edit().putString("batch_state", "انقطع الإنترنت — انتظار عودته لاستئناف الدفعة $number (محاولة $networkRetries/5)").apply()
                    updateNotification("انقطاع الشبكة — الدفعة محفوظة")
                    if (!waitForNetwork()) {
                        reports.edit().putString("batch_state", "توقف آمن: لم تعد الشبكة خلال المهلة").apply()
                        updateNotification("توقف آمن — لا يوجد اتصال")
                        return
                    }
                    Thread.sleep(1200L)
                    result = DeepSeekClient(this).analyzeBatch(token, batch, number, batches.size, onChunk = { chunks++ })
                }
                if (result.isFailure && isAuthError(result.exceptionOrNull()!!)) {
                    preserveQueue = true
                    reports.edit().putBoolean("auth_required", true).putString("batch_state", "انتهى توكن DeepSeek — افتح التطبيق لتسجيل الدخول").apply()
                    updateNotification("انتهى توكن DeepSeek — افتح التطبيق لتسجيل الدخول")
                    return@forEachIndexed
                }
                val text = result.fold({ it }, { backgroundError(it) })
                if (result.isSuccess) successCount++
                combined += "\n\n===== التقرير $number/${batches.size} =====\n$text"
                reports.edit().putString("last_report", combined).putString("last_status", "اكتمل التقرير $number/${batches.size}").putInt("batch_current", number + 1).putString("batch_state", "اكتمل التقرير $number/${batches.size}").apply()
            }
            reports.edit().putString("batch_state", "اكتمل التحليل في الخلفية: $successCount/${batches.size} تقارير صالحة").apply()
            updateNotification("اكتمل تحليل DeepSeek: $successCount/${batches.size} تقارير صالحة")
        } catch (error: Throwable) {
            preserveQueue = isAuthError(error) || isRetryableNetwork(error)
            if (isAuthError(error)) reports.edit().putBoolean("auth_required", true).apply()
            reports.edit().putString("batch_state", backgroundError(error)).apply()
            updateNotification("تعذر الاتصال بـDeepSeek — تحقق من الإنترنت")
        } finally {
            if (!preserveQueue) queueFile.delete()
            processing.set(false)
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun isAuthError(error: Throwable): Boolean {
        val message = error.message.orEmpty().lowercase()
        return message.contains("http 401") || message.contains("unauthorized") || message.contains("رمز") || message.contains("token")
    }

    private fun hasNetwork(): Boolean {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = manager.activeNetwork ?: return false
            return manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
        @Suppress("DEPRECATION")
        return manager.activeNetworkInfo?.isConnected == true
    }

    private fun waitForNetwork(maxWaitMs: Long = 120_000L): Boolean {
        val deadline = System.currentTimeMillis() + maxWaitMs
        while (!hasNetwork() && !Thread.currentThread().isInterrupted && System.currentTimeMillis() < deadline) {
            Thread.sleep(1000L)
        }
        return hasNetwork() && !Thread.currentThread().isInterrupted
    }

    private fun isRetryableNetwork(error: Throwable): Boolean {
        if (isAuthError(error)) return false
        val message = error.message.orEmpty().lowercase()
        return message.contains("unable to resolve host") || message.contains("unknownhost") || message.contains("no address associated") ||
            message.contains("timeout") || message.contains("timed out") || message.contains("failed to connect") ||
            message.contains("connection reset") || message.contains("connection refused") || message.contains("socket") ||
            message.contains("network is unreachable") || message.contains("broken pipe") || message.contains("http 5")
    }

    private fun backgroundError(error: Throwable): String {
        val message = error.message.orEmpty().lowercase()
        return when {
            message.contains("unable to resolve host") || message.contains("unknownhost") || message.contains("no address associated") -> "تعذر الاتصال بـDeepSeek: لا يوجد اتصال إنترنت أو تعذر الوصول إلى الخادم."
            message.contains("timeout") || message.contains("timed out") -> "تعذر الاتصال بـDeepSeek: انتهت مهلة الاتصال."
            message.contains("http 401") || message.contains("unauthorized") || message.contains("token") -> "انتهى توكن DeepSeek. افتح التطبيق لتسجيل الدخول والحصول على توكن جديد."
            else -> "تعذر إكمال تحليل DeepSeek. يمكنك المحاولة مرة أخرى."
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel(CHANNEL, "تحليل DeepSeek في الخلفية", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(message: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("SmaliFox").setContentText(message).setOngoing(true).build()
    }

    private fun updateNotification(message: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification(message))
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
