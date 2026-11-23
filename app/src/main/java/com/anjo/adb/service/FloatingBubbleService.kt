package com.anjo.adb.service

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anjo.adb.R
import com.anjo.adb.core.ToggleActionMapper
import com.anjo.adb.data.Categoria
import com.anjo.adb.data.SettingsRepository
import com.anjo.adb.data.ToggleRegistry
import com.anjo.adb.ui.ToggleCardAdapter

class FloatingBubbleService : Service() {

    private lateinit var wm: WindowManager
    private var bolhaView: View? = null
    private var painelView: View? = null
    private lateinit var repo: SettingsRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repo = SettingsRepository(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        iniciarForeground()
        criarBolha()
    }

    private fun iniciarForeground() {
        val canal = "anjo_adb_bolinha"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nc = NotificationChannel(canal, "Painel flutuante", NotificationManager.IMPORTANCE_MIN)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(nc)
        }
        val notif = NotificationCompat.Builder(this, canal)
            .setContentTitle("Anjo ADB ativo")
            .setContentText("Painel flutuante de desempenho ligado")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
        startForeground(421, notif)
    }

    private fun tipoOverlay() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun criarBolha() {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_bubble, null)
        bolhaView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoOverlay(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 300

        var dX = 0f; var dY = 0f; var inicioX = 0; var inicioY = 0; var arrastou = false

        view.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    inicioX = params.x; inicioY = params.y
                    dX = ev.rawX; dY = ev.rawY
                    arrastou = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val novoX = inicioX + (ev.rawX - dX).toInt()
                    val novoY = inicioY + (ev.rawY - dY).toInt()
                    if (Math.abs(novoX - inicioX) > 8 || Math.abs(novoY - inicioY) > 8) arrastou = true
                    params.x = novoX
                    params.y = novoY
                    wm.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!arrastou) alternarPainel()
                    true
                }
                else -> false
            }
        }
        wm.addView(view, params)
    }

    private fun alternarPainel() {
        if (painelView != null) {
            fecharPainel()
            return
        }
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_panel, null)
        painelView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoOverlay(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 70
        params.y = 300

        // os toggles mais usados: funciona dentro e fora do jogo, muda só dentro do app
        val rapidos = ToggleRegistry.todos.filter {
            it.id in setOf("foco_jogo", "espremer_ram", "limpar_cache_geral", "congelar_fundo",
                "bloqueio_alarmes", "gps_off", "bluetooth_off", "sistema_mais_leve")
        }

        view.findViewById<RecyclerView>(R.id.recyclerPainelRapido).apply {
            layoutManager = LinearLayoutManager(this@FloatingBubbleService)
            adapter = ToggleCardAdapter(rapidos, repo) { item, ativo ->
                ToggleActionMapper.aplicar(this@FloatingBubbleService, item.id, ativo)
            }
        }
        view.findViewById<View>(R.id.btnFecharPainel).setOnClickListener { fecharPainel() }

        wm.addView(view, params)
    }

    private fun fecharPainel() {
        painelView?.let { wm.removeView(it) }
        painelView = null
    }

    override fun onDestroy() {
        fecharPainel()
        bolhaView?.let { wm.removeView(it) }
        bolhaView = null
        super.onDestroy()
    }
}
