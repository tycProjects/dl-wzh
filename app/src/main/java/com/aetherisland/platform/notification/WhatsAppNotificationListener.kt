package com.aetherisland.platform.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Strict package gate: all non-WhatsApp notifications are ignored before parsing. */
class WhatsAppNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!WhatsAppPackageFilter.accepts(sbn.packageName)) return
        // TODO(stage WhatsApp): parse MessagingStyle/RemoteInput and publish a sanitized event.
    }

    companion object {
        fun isWhatsAppPackage(packageName: String): Boolean = WhatsAppPackageFilter.accepts(packageName)
    }
}

object WhatsAppPackageFilter {
    private val allowedPackages = setOf("com.whatsapp", "com.whatsapp.w4b")
    fun accepts(packageName: String): Boolean = packageName in allowedPackages
}
