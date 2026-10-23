package com.sensigreen.ffoverlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class FloatingOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var locked = true
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!SettingsCompat.canDraw(this)) { stopSelf(); return START_NOT_STICKY }
        if (bubble == null) createBubble()
        return START_NOT_STICKY
    }

    private fun createBubble() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val view = TextView(this).apply {
            text = "SG"; textSize = 16f; setTextColor(Color.BLACK); gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(124,255,78)); setStroke(3, Color.WHITE) }
            elevation = 10f
        }
        val lp = WindowManager.LayoutParams(58, 58, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 240 }
        view.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0; var startY = 0; var touchX = 0f; var touchY = 0f; var moved = false
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { startX = lp.x; startY = lp.y; touchX = e.rawX; touchY = e.rawY; moved = false; return true }
                    MotionEvent.ACTION_MOVE -> {
                        if (!locked && (kotlin.math.abs(e.rawX - touchX) > 4 || kotlin.math.abs(e.rawY - touchY) > 4)) {
                            lp.x = startX + (e.rawX - touchX).toInt(); lp.y = startY + (e.rawY - touchY).toInt(); wm.updateViewLayout(view, lp); moved = true
                        }; return true
                    }
                    MotionEvent.ACTION_UP -> { if (!moved) { if (panel == null) showPanel() else hidePanel() }; return true }
                }; return true
            }
        })
        bubble = view; params = lp; wm.addView(view, lp)
    }

    private fun showPanel() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 14, 18, 14); background = GradientDrawable().apply { setColor(Color.rgb(15,25,19)); cornerRadius = 24f; setStroke(2, Color.rgb(124,255,78)) } }
        box.addView(TextView(this).apply { text = "SENSIGREEN OVERLAY"; textSize = 15f; setTextColor(Color.rgb(124,255,78)); setPadding(0,0,0,8) })
        fun button(label: String, action: () -> Unit) { box.addView(Button(this).apply { text = label; setOnClickListener { action() } }) }
        button(if (locked) "UNLOCK POSISI" else "LOCK POSISI") { locked = !locked; hidePanel(); showPanel() }
        button("SENSITIVITY") { launchApp() }
        button("AIM MERAH / CROSSHAIR") { launchApp() }
        button("TUTUP PANEL") { hidePanel() }
        val lp = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.END; x = 18; y = 180 }
        panel = box; wm.addView(box, lp)
    }
    private fun hidePanel() { panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }; panel = null }
    private fun launchApp() { val i = packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); if (i != null) startActivity(i) }
    override fun onDestroy() { hidePanel(); bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }; bubble = null; super.onDestroy() }
}

private object SettingsCompat {
    fun canDraw(context: android.content.Context): Boolean = android.provider.Settings.canDrawOverlays(context)
}
