package com.zewmod.ui

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import com.zewmod.hack.*

class OverlayRenderer(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var renderView: EspOverlayView? = null

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 0 }

    fun show() {
        if (renderView != null) return
        val v = EspOverlayView(context)
        wm.addView(v, params)
        v.startRenderLoop()
        renderView = v
    }

    fun hide() {
        renderView?.stopRenderLoop()
        renderView?.let { wm.removeView(it) }
        renderView = null
    }

    // Enemies di-set via HackState.setEnemies() dari ProxyRelay langsung
    // Tidak perlu route lewat OverlayRenderer
    fun updateEnemies(enemies: List<EnemyData>) = HackState.setEnemies(enemies)
}
