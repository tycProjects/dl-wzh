package com.lecturemanager.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.lecturemanager.app.data.Prefs
import com.lecturemanager.app.work.Scheduler

class LectureApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_REMINDERS, "تذكيرات المذاكرة", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        // إعادة ضبط المهام الدورية حسب الإعدادات المحفوظة
        val s = Prefs(this).read()
        Scheduler.applyReminder(this, s)
        Scheduler.applyBackup(this, s)
        Scheduler.enqueueIndex(this)
    }

    companion object { const val CHANNEL_REMINDERS = "study_reminders" }
}
