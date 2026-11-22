package com.example.musicplayer

import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView

/** Команды из уведомления / экрана блокировки -> JavaScript плеера */
object Bridge {
    @Volatile var web: WebView? = null
    fun cmd(c: String) { web?.post { web?.evaluateJavascript("window.__cmd&&window.__cmd('$c')", null) } }
}

/** Состояние трека, которое присылает плеер (JavaScript -> Android) */
object PlayerState {
    @Volatile var title = ""
    @Volatile var artist = ""
    @Volatile var playing = false
    @Volatile var pos = 0L
    @Volatile var dur = 0L
    @Volatile var cover: Bitmap? = null
    @Volatile var accent: Int = 0xFF5FA4B8.toInt()
}

/** Объект window.AndroidMedia, доступный из страницы плеера */
class AndroidBridge(private val act: MainActivity) {
    private val main = Handler(Looper.getMainLooper())

    // Сервис запускается при ЛЮБОМ первом обновлении (загрузка трека, play, pause),
    // поэтому управление в шторке и на экране блокировки есть независимо от того, чем запущен трек.
    private fun push() = main.post {
        val s = MediaPlaybackService.instance
        if (s != null) s.refresh()
        else if (PlayerState.title.isNotEmpty() || PlayerState.playing) {
            try { act.startForegroundService(Intent(act, MediaPlaybackService::class.java)) } catch (e: Exception) { }
        }
        PlayerWidget.refresh(act)
    }

    @JavascriptInterface
    fun update(title: String, artist: String, playing: Boolean, pos: Long, dur: Long) {
        PlayerState.title = title; PlayerState.artist = artist; PlayerState.playing = playing
        PlayerState.pos = pos; PlayerState.dur = dur
        push()
    }

    @JavascriptInterface
    fun setCover(dataUrl: String) {
        try {
            val b = Base64.decode(dataUrl.substringAfter(','), Base64.DEFAULT)
            PlayerState.cover = BitmapFactory.decodeByteArray(b, 0, b.size)
            push()
        } catch (e: Exception) { }
    }

    @JavascriptInterface
    fun setTheme(accent: String) {
        try { PlayerState.accent = Color.parseColor(accent); push() } catch (e: Exception) { }
    }

    /** Кнопка «Добавить виджет на главный экран» в настройках плеера */
    @JavascriptInterface
    fun addWidget() = act.runOnUiThread {
        val mgr = AppWidgetManager.getInstance(act)
        if (Build.VERSION.SDK_INT >= 26 && mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(act, PlayerWidget::class.java), null, null)
        } else {
            Toast.makeText(act, "Долгое нажатие на главном экране → Виджеты → Плеер", Toast.LENGTH_LONG).show()
        }
    }

    @JavascriptInterface fun pickFiles() { act.runOnUiThread { act.openFiles() } }
    @JavascriptInterface fun pickFolder() { act.runOnUiThread { act.openFolder() } }
}
