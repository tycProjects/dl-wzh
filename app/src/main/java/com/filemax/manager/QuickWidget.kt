package com.filemax.manager

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

const val ACTION_OPEN_FOLDER = "com.filemax.manager.OPEN_FOLDER"

/** Home-screen widget with three one-tap folder buttons (internal storage, Downloads, Android/data). */
class QuickWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val root = android.os.Environment.getExternalStorageDirectory().absolutePath
        val views = RemoteViews(context.packageName, R.layout.widget_quick)
        listOf(R.id.w_internal to root, R.id.w_downloads to "$root/Download", R.id.w_appdata to "$root/Android/data").forEachIndexed { i, (id, path) ->
            val intent = Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_FOLDER).putExtra("path", path)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            views.setOnClickPendingIntent(id, PendingIntent.getActivity(context, 100 + i, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        }
        ids.forEach { manager.updateAppWidget(it, views) }
    }
}
