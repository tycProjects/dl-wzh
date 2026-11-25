package com.livevideowallpaper.app

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale

/**
 * Status row drawn by the app: time, animated notification icons, network speed,
 * signal bars, Wi-Fi and a battery.
 * Battery: green when charging, black body when not charging.
 * style 0 = classic outline battery, style 1 = pill battery (like the reference screenshot).
 */
class StatusBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var style: Int = 0
        set(v) { field = v; invalidate() }

    var notifAnim: Boolean = true
        set(v) { field = v; invalidate() }

    /** Preview only: true/false forces the charging look, null uses the real charger state. */
    var previewCharging: Boolean? = null
        set(v) { field = v; invalidate() }

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val rect = RectF()
    private val path = Path()

    private var level = 85
    private var realCharging = false
    private var hasWifi = false
    private var hasCell = false
    private var speedText = "0.0"
    private var lastBytes = 0L
    private var lastTime = 0L
    private var phase = 0f
    private var anim: ValueAnimator? = null
    private var registered = false
    private val handler = Handler(Looper.getMainLooper())

    private val ticker = object : Runnable {
        override fun run() {
            updateNetwork()
            updateSpeed()
            invalidate()
            handler.postDelayed(this, 1000L)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            readBattery(i)
        }
    }

    private val notifColors = intArrayOf(0xFF25D366.toInt(), 0xFF2AABEE.toInt(), 0xFF4285F4.toInt())
    private val notifLetters = arrayOf("W", "T", "C")

    private val green = 0xFF27D35B.toInt()
    private val white30 = 0x4DFFFFFF
    private val white80 = 0xCCFFFFFF.toInt()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!registered) {
            try {
                val sticky = ContextCompat.registerReceiver(
                    context,
                    batteryReceiver,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                if (sticky != null) readBattery(sticky)
                registered = true
            } catch (_: Exception) {
            }
        }
        lastBytes = trafficBytes()
        lastTime = System.currentTimeMillis()
        updateNetwork()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        anim?.cancel()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400L
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(ticker)
        anim?.cancel()
        anim = null
        if (registered) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (_: Exception) {
            }
            registered = false
        }
        super.onDetachedFromWindow()
    }

    private fun readBattery(i: Intent) {
        val lv = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val sc = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (lv >= 0 && sc > 0) level = (lv * 100f / sc).toInt()
        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        realCharging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        invalidate()
    }

    private fun updateNetwork() {
        try {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val net = cm.activeNetwork
            val caps: NetworkCapabilities? = if (net != null) cm.getNetworkCapabilities(net) else null
            hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            hasCell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        } catch (_: Exception) {
            hasWifi = false
            hasCell = false
        }
    }

    private fun trafficBytes(): Long {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        return if (rx < 0 || tx < 0) 0L else rx + tx
    }

    private fun updateSpeed() {
        val now = System.currentTimeMillis()
        val total = trafficBytes()
        val dt = (now - lastTime).coerceAtLeast(1L) / 1000f
        val kb = (total - lastBytes).coerceAtLeast(0L) / 1024f / dt
        lastBytes = total
        lastTime = now
        speedText = String.format(Locale.getDefault(), "%.1f", kb)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cy = h / 2f
        val pad = 10 * dp
        val charging = previewCharging ?: realCharging

        // ---- left: time + notification icons ----
        val cal = Calendar.getInstance()
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        text.textAlign = Paint.Align.LEFT
        text.isFakeBoldText = true
        text.textSize = 16 * dp
        c.drawText(timeStr, pad, cy + text.textSize * 0.35f, text)
        var x = pad + text.measureText(timeStr) + 9 * dp
        val r = 6.5f * dp
        for (k in notifColors.indices) {
            val scale = if (notifAnim) 0.9f + 0.1f * kotlin.math.sin(2 * Math.PI * (phase + k * 0.33)).toFloat() else 1f
            paint.style = Paint.Style.FILL
            paint.color = notifColors[k]
            c.drawCircle(x + r, cy, r * scale, paint)
            text.textAlign = Paint.Align.CENTER
            text.isFakeBoldText = true
            text.textSize = 8 * dp
            c.drawText(notifLetters[k], x + r, cy + text.textSize * 0.35f, text)
            x += 2 * r + 4 * dp
        }

        // ---- right side, drawn from the right edge ----
        val battW = 30 * dp
        val battH = 15 * dp
        val bx = w - pad - battW - 3 * dp
        drawBattery(c, bx, cy - battH / 2, battW, battH, charging)

        val wifiCx = bx - 11 * dp
        drawWifi(c, wifiCx, cy)

        val barsRight = wifiCx - 10 * dp
        drawBars(c, barsRight, cy)
        val barsLeft = barsRight - (4 * 3 + 3 * 1.5f) * dp

        text.textAlign = Paint.Align.RIGHT
        text.isFakeBoldText = true
        text.textSize = 9.5f * dp
        c.drawText(speedText, barsLeft - 6 * dp, cy - 0.5f * dp, text)
        text.isFakeBoldText = false
        text.textSize = 7 * dp
        c.drawText("KB/s", barsLeft - 6 * dp, cy + 7 * dp, text)
    }

    private fun drawBattery(c: Canvas, x: Float, y: Float, bw: Float, bh: Float, charging: Boolean) {
        val frac = level.coerceIn(0, 100) / 100f
        val pillR = bh / 2

        if (style == 1) {
            // Pill battery: solid green when charging, solid black when not
            rect.set(x, y, x + bw, y + bh)
            paint.style = Paint.Style.FILL
            paint.color = if (charging) green else Color.BLACK
            c.drawRoundRect(rect, pillR, pillR, paint)
            if (!charging) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1 * dp
                paint.color = white80
                c.drawRoundRect(rect, pillR, pillR, paint)
            }
            if (charging) {
                drawBolt(c, x + 7 * dp, y + bh / 2, bh * 0.7f, Color.WHITE)
                text.textAlign = Paint.Align.CENTER
                text.isFakeBoldText = true
                text.textSize = 9 * dp
                c.drawText("$level", x + bw * 0.62f, y + bh / 2 + 9 * dp * 0.35f, text)
            } else {
                text.textAlign = Paint.Align.CENTER
                text.isFakeBoldText = true
                text.textSize = 9 * dp
                c.drawText("$level", x + bw / 2, y + bh / 2 + 9 * dp * 0.35f, text)
            }
            return
        }

        // Classic battery: outline, tip, and interior (green level when charging, black otherwise)
        val stroke = 1.4f * dp
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.color = Color.WHITE
        rect.set(x + stroke / 2, y + stroke / 2, x + bw - stroke / 2, y + bh - stroke / 2)
        c.drawRoundRect(rect, 3 * dp, 3 * dp, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        rect.set(x + bw + 1 * dp, y + bh * 0.3f, x + bw + 3 * dp, y + bh * 0.7f)
        c.drawRoundRect(rect, 1 * dp, 1 * dp, paint)

        val inset = 2.2f * dp
        val ix = x + inset
        val iy = y + inset
        val iw = bw - 2 * inset
        val ih = bh - 2 * inset

        if (charging) {
            val fillW = iw * frac
            paint.style = Paint.Style.FILL
            paint.color = green
            rect.set(ix, iy, ix + fillW, iy + ih)
            c.drawRoundRect(rect, 1.5f * dp, 1.5f * dp, paint)

            // moving shine across the green fill
            c.save()
            c.clipRect(rect)
            paint.color = 0x59FFFFFF
            val sx = ix - 6 * dp + (iw + 12 * dp) * phase
            rect.set(sx, iy, sx + 5 * dp, iy + ih)
            c.drawRect(rect, paint)
            c.restore()

            drawBolt(c, x + bw / 2, y + bh / 2, ih * 0.95f, Color.WHITE)
        } else {
            // black interior with the percentage
            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            rect.set(ix, iy, ix + iw, iy + ih)
            c.drawRoundRect(rect, 1.5f * dp, 1.5f * dp, paint)
            text.textAlign = Paint.Align.CENTER
            text.isFakeBoldText = true
            text.textSize = 9 * dp
            c.drawText("$level", x + bw / 2, y + bh / 2 + 9 * dp * 0.35f, text)
        }
    }

    /** Lightning bolt centred at (cx, cy) with height [hgt]. */
    private fun drawBolt(c: Canvas, cx: Float, cy: Float, hgt: Float, color: Int) {
        val wdt = hgt * 0.8f
        val x0 = cx - wdt / 2
        val y0 = cy - hgt / 2
        path.reset()
        path.moveTo(x0 + wdt * 0.60f, y0 + hgt * 0.00f)
        path.lineTo(x0 + wdt * 0.15f, y0 + hgt * 0.58f)
        path.lineTo(x0 + wdt * 0.45f, y0 + hgt * 0.58f)
        path.lineTo(x0 + wdt * 0.35f, y0 + hgt * 1.00f)
        path.lineTo(x0 + wdt * 0.85f, y0 + hgt * 0.40f)
        path.lineTo(x0 + wdt * 0.55f, y0 + hgt * 0.40f)
        path.lineTo(x0 + wdt * 0.65f, y0 + hgt * 0.00f)
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = color
        c.drawPath(path, paint)
    }

    private fun drawWifi(c: Canvas, cx: Float, cy: Float) {
        val base = cy + 4 * dp
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 1.6f * dp
        paint.color = if (hasWifi) Color.WHITE else white30
        for (k in 1..3) {
            val rr = k * 3.2f * dp
            rect.set(cx - rr, base - rr, cx + rr, base + rr)
            c.drawArc(rect, 225f, 90f, false, paint)
        }
        paint.style = Paint.Style.FILL
        c.drawCircle(cx, base, 1.4f * dp, paint)
        paint.strokeCap = Paint.Cap.BUTT
    }

    private fun drawBars(c: Canvas, right: Float, cy: Float) {
        val bw = 3 * dp
        val gap = 1.5f * dp
        val heights = floatArrayOf(4 * dp, 6 * dp, 8 * dp, 11 * dp)
        var x = right - (4 * bw + 3 * gap)
        val bottom = cy + 5.5f * dp
        paint.style = Paint.Style.FILL
        paint.color = if (hasCell) Color.WHITE else white30
        for (hh in heights) {
            rect.set(x, bottom - hh, x + bw, bottom)
            c.drawRoundRect(rect, 0.8f * dp, 0.8f * dp, paint)
            x += bw + gap
        }
    }
}
