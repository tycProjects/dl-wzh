package com.johan.jugb185.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.johan.jugb185.R

class JugbWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val views = RemoteViews(ctx.packageName, android.R.layout.simple_list_item_1)
            mgr.updateAppWidget(id, views)
        }
    }
}
