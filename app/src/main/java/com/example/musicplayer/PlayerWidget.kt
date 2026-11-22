package com.example.musicplayer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.widget.RemoteViews

/** Виджет на главном экране: обложка, название, исполнитель и кнопки управления */
class PlayerWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_CMD = "com.example.musicplayer.WIDGET_CMD"

        fun refresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, PlayerWidget::class.java))
            for (id in ids) mgr.updateAppWidget(id, build(ctx))
        }

        private fun round(src: Bitmap): Bitmap {
            val size = 240
            val scaled = Bitmap.createScaledBitmap(src, size, size, true)
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(out)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            c.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 44f, 44f, p)
            p.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            c.drawBitmap(scaled, 0f, 0f, p)
            return out
        }

        private fun build(ctx: Context): RemoteViews {
            val v = RemoteViews(ctx.packageName, R.layout.widget_player)
            val s = PlayerState
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), flags)
            // Если плеер не запущен, кнопки просто открывают его
            val alive = Bridge.web != null
            fun btn(cmd: String, code: Int): PendingIntent =
                if (!alive) open else PendingIntent.getBroadcast(ctx, code,
                    Intent(ctx, PlayerWidget::class.java).setAction(ACTION_CMD).putExtra("cmd", cmd), flags)

            val empty = s.title.isEmpty()
            v.setTextViewText(R.id.w_title, if (empty) ctx.getString(R.string.app_name) else s.title)
            v.setTextViewText(R.id.w_artist, if (empty) "Нажмите, чтобы открыть" else s.artist)
            val cover = s.cover
            if (cover != null) v.setImageViewBitmap(R.id.w_cover, round(cover))
            else v.setImageViewResource(R.id.w_cover, R.mipmap.ic_launcher)
            v.setImageViewResource(R.id.w_play, if (s.playing) R.drawable.ic_w_pause else R.drawable.ic_w_play)
            v.setInt(R.id.w_play, "setColorFilter", s.accent)

            v.setOnClickPendingIntent(R.id.w_root, open)
            v.setOnClickPendingIntent(R.id.w_cover, open)
            v.setOnClickPendingIntent(R.id.w_prev, btn("prev", 1))
            v.setOnClickPendingIntent(R.id.w_play, btn(if (s.playing) "pause" else "play", 2))
            v.setOnClickPendingIntent(R.id.w_next, btn("next", 3))
            return v
        }
    }

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) mgr.updateAppWidget(id, build(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CMD) Bridge.cmd(intent.getStringExtra("cmd") ?: return)
        else super.onReceive(context, intent)
    }
}
