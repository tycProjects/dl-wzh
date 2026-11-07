package com.fpsoverlay

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.graphics.Outline
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun round(color: Int, r: Float, stroke: Int = 0) = GradientDrawable().apply {
    setColor(color); cornerRadius = r; if (stroke != 0) setStroke(2, stroke)
}

class MainActivity : Activity() {
    private fun hasOverlay() = Settings.canDrawOverlays(this)
    private fun hasNotif() = Build.VERSION.SDK_INT < 33 ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = Color.parseColor("#0F0C29")
        window.navigationBarColor = Color.parseColor("#24243E")
        if (!hasNotif()) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }

    override fun onResume() { super.onResume(); setContentView(buildUi()) }
    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) { setContentView(buildUi()) }

    private fun tv(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun card(title: String, desc: String, ok: Boolean, act: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = round(Color.parseColor("#1AFFFFFF"), dp(20).toFloat(), Color.parseColor("#33FFFFFF"))
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(tv(title, 16f, Color.WHITE, true))
            addView(tv(desc, 12f, Color.parseColor("#B0B3D6")).apply { setPadding(0, dp(4), 0, 0) })
        }
        addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        addView(tv(if (ok) "✓ Đã cấp" else "Cấp quyền", 13f, if (ok) Color.parseColor("#0F0C29") else Color.WHITE, true).apply {
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = round(Color.parseColor(if (ok) "#4ADE80" else "#7C3AED"), dp(14).toFloat())
            if (!ok) setOnClickListener { act() }
        })
        (layoutParams ?: LinearLayout.LayoutParams(-1, -2)).also { (it as LinearLayout.LayoutParams).bottomMargin = dp(12); layoutParams = it }
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(48), dp(20), dp(24))
        }
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.logo); scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, dp(24).toFloat()) }
            }
            elevation = 12f
            layoutParams = LinearLayout.LayoutParams(dp(96), dp(96)).also { it.bottomMargin = dp(14) }
        })
        root.addView(tv("T•HẢI PCM", 13f, Color.WHITE, true).apply {
            letterSpacing = 0.15f; setPadding(dp(14), dp(6), dp(14), dp(6))
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#06B6D4"))).apply { cornerRadius = dp(14).toFloat() }
            (LinearLayout.LayoutParams(-2, -2)).also { it.bottomMargin = dp(12); layoutParams = it }
        })
        root.addView(tv("FPS Overlay", 30f, Color.WHITE, true))
        root.addView(tv("Overlay FPS gaming, dính cứng góc trái dưới màn hình", 13f, Color.parseColor("#B0B3D6")).apply { setPadding(0, dp(4), 0, dp(24)) })
        root.addView(card("Hiển thị trên ứng dụng khác", "Bắt buộc để vẽ FPS và bong bóng nổi", hasOverlay()) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(card("Thông báo", "Giữ dịch vụ FPS chạy ổn định khi chơi game", hasNotif()) {
            if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        })
        val ready = hasOverlay() && hasNotif()
        val running = OverlayService.running
        root.addView(tv(if (running) "Tắt FPS" else "Bật FPS", 17f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(16), 0, dp(16)); alpha = if (ready) 1f else 0.4f
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#06B6D4"))).apply { cornerRadius = dp(22).toFloat() }
            setOnClickListener {
                if (!ready) return@setOnClickListener
                val i = Intent(this@MainActivity, OverlayService::class.java)
                if (running) stopService(i) else startForegroundService(i)
                it.postDelayed({ setContentView(buildUi()) }, 300)
            }
            (LinearLayout.LayoutParams(-1, -2)).also { it.topMargin = dp(12); layoutParams = it }
        })
        root.addView(tv("© 2026 T•HẢI PCM\nBản quyền thuộc về T•HẢI PCM. Bảo lưu mọi quyền.", 11f, Color.parseColor("#8A8DB5")).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(28), 0, 0)
            layoutParams = LinearLayout.LayoutParams(-1, -2)
        })
        return ScrollView(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#0F0C29"), Color.parseColor("#302B63"), Color.parseColor("#24243E")))
            addView(root)
        }
    }
}
