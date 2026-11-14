package com.example.gditroll

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.Image
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.View
import android.graphics.Typeface
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Desenha os efeitos. Segurança visual: nenhuma cor alterna mais rápido que ~2 Hz
 * (limite bem abaixo da faixa de 3–30 Hz associada a crises fotossensíveis);
 * as transições são suaves (senoides), nunca piscadas bruscas.
 */
class EffectView(
    context: Context,
    private val effect: Effect,
    private val intensity: Intensity
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val start = SystemClock.uptimeMillis()
    private var running = true
    // Densidade ajustada pelo tipo de tela: em tablets e, principalmente, em TVs (vistas de
    // longe), os elementos dos efeitos (texto do Matrix, ícones, espessura dos tubos etc.)
    // precisam ser maiores para ficarem proporcionais.
    private val d = resources.displayMetrics.density * UiScale.factor(context)
    private val hsv = FloatArray(3)
    private var frame: Bitmap? = null
    private var frameW = 0
    private var frameH = 0
    private var lastDraw = 0L
    private val srcRect = Rect()
    private val dstRect = RectF()
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        // Satura as cores da tela capturada. É um filtro FIXO (sempre o mesmo, em todo
        // quadro) — deixa as cores mais fortes sem criar nenhuma alternância/flash.
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(2.1f) })
    }
    private val addPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private fun channel(r: Float, g: Float, b: Float) = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f)))
    private val chR = channel(1f, 0f, 0f)
    private val chG = channel(0f, 1f, 0f)
    private val chB = channel(0f, 0f, 1f)
    private var layer = 1f // multiplicador de opacidade usado pelo efeito combinado
    private val icons by lazy {
        listOfNotNull(
            context.getDrawable(R.drawable.icon_robot1),
            context.getDrawable(R.drawable.icon_robot2),
            context.getDrawable(R.drawable.icon_robot3),
            context.getDrawable(R.drawable.icon_error),
            context.getDrawable(R.drawable.icon_warning)
        )
    }
    // Marcas permanentes deixadas no caminho dos ícones (não desaparecem). O tamanho é
    // limitado para não pesar o aparelho numa sessão longa; ao atingir o limite, a marca
    // mais antiga é substituída pela mais nova (a trilha recente continua sempre visível).
    private class Stamp(val iconIndex: Int, val x: Float, val y: Float)
    private val trail = ArrayDeque<Stamp>()
    private val maxTrail = 500
    private val lastStamp = FloatArray(6) { -999f }
    private val stampIntervalSec = 0.10f

    // --- Matrix ---
    private val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE; textAlign = Paint.Align.LEFT
    }
    private val matChars = "ｦｧｨｩｪｫｬｭｮｯｰｱｲｳｴｵｶｷｸｹｺ0123456789ABCDEF@#\$%&!?".toCharArray()

    // --- 3D Pipes ---
    private data class PSeg(val x1:Float,val y1:Float,val x2:Float,val y2:Float,val col:Int)
    private val pipeSegs = ArrayList<PSeg>(300)
    // [x, y, dx, dy, colorIdx, nextTurnMs]
    private val pipeHeads = ArrayList<FloatArray>()
    private var pipesReady = false
    private var pipeLastMs = 0L
    private val PIPE_COLS = intArrayOf(0xFF0000,0x00FF00,0x0033FF,0xFFFF00,0xFF00FF,0x00FFFF,0xFFFFFF,0xFF6600)

    // --- Eyes ---
    // [cx, cy, size, pupilAngleOffset, blinkPhase] — tudo determinístico pelo hash
    private val eyePairs by lazy { List(7) { i ->
        floatArrayOf(hash(i*11+1), hash(i*11+2), 0.07f + hash(i*11+3)*0.06f,
            (hash(i*11+4)*2f*PI.toFloat()), hash(i*11+5)*6.28f)
    }}

    private val frame = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            postOnAnimation(this)
        }
    }

    /** Recebe um quadro da captura (thread principal). Nada é salvo: só é redesenhado. */
    fun updateFrame(image: Image) {
        val plane = image.planes[0]
        val bw = plane.rowStride / plane.pixelStride
        var bmp = frame
        if (bmp == null || bmp.width != bw || bmp.height != image.height) {
            bmp = Bitmap.createBitmap(bw, image.height, Bitmap.Config.ARGB_8888)
            frame = bmp
        }
        plane.buffer.rewind()
        bmp.copyPixelsFromBuffer(plane.buffer)
        frameW = image.width
        frameH = image.height
    }

    init { setLayerType(LAYER_TYPE_HARDWARE, null) }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        postOnAnimation(frame)
    }

    override fun onDetachedFromWindow() {
        running = false
        removeCallbacks(frame)
        super.onDetachedFromWindow()
    }

    private fun a(alpha: Float) = (alpha * intensity.alpha * layer * 255f).toInt().coerceIn(0, 255)
    private fun argb(alpha: Float, rgb: Int) = (a(alpha) shl 24) or (rgb and 0xFFFFFF)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        // Tempo em segundos, independente da taxa de quadros.
        val now = SystemClock.uptimeMillis()
        val raw = (now - start) / 1000f
        val t = raw * intensity.speed
        val dt = ((now - lastDraw) / 1000f).coerceIn(0.001f, 0.05f)
        lastDraw = now
        paint.style = Paint.Style.FILL
        val bmp = frame
        if (effect.needsCapture() && bmp != null && frameW > 0) {
            drawScreen(canvas, bmp, w, h, raw, t, dt)
            drawGlobalStamps(canvas, w, h, raw)
            return
        }
        when (effect) {
            Effect.RGB -> drawWash(canvas, w, h, t)
            Effect.SPIRAL -> drawSpiral(canvas, w, h, t)
            Effect.GLITCH -> drawGlitch(canvas, w, h, t)
            Effect.WAVE -> drawWave(canvas, w, h, t)
            Effect.CHAOS -> drawChaos(canvas, w, h, t)
            Effect.TUNNEL -> drawTunnel(canvas, w, h, t)
            Effect.MELT -> drawMelt(canvas, w, h, t)
            Effect.PLASMA -> drawPlasma(canvas, w, h, t)
            Effect.MOIRE -> drawMoire(canvas, w, h, t)
            Effect.BLOCKS -> drawBlocks(canvas, w, h, t)
            Effect.ICONS -> drawIcons(canvas, w, h, t)
            Effect.HYPNO -> drawHypno(canvas, w, h, t)
            Effect.MEMZ_MATRIX -> drawMemzMatrix(canvas, w, h, t)
            Effect.MEMZ_PIPES -> drawMemzPipes(canvas, w, h)
            Effect.MEMZ_EYES -> drawMemzEyes(canvas, w, h, t)
            Effect.MIX, Effect.SCR_MELT, Effect.SCR_WAVE, Effect.SCR_TUNNEL,
            Effect.SCR_GLITCH, Effect.SCR_TOTAL,
            Effect.SCR_TILES, Effect.SCR_MIRROR, Effect.SCR_MEMZ_TOTAL,
            Effect.SCR_MULTIPLY, Effect.SCR_SPIN -> drawMix(canvas, w, h, t)
        }
        drawGlobalStamps(canvas, w, h, raw)
    }

    // ---------- marca fixa a cada 1 s, com as imagens enviadas; some só ao parar ----------
    private val stampIcons by lazy {
        listOf(R.drawable.icon_robot1, R.drawable.icon_robot2, R.drawable.icon_robot3,
            R.drawable.icon_error, R.drawable.icon_warning).mapNotNull { context.getDrawable(it) }
    }
    private val globalStamps = ArrayList<FloatArray>() // [iconIndex, x, y]
    private var lastGlobalStampSec = -1

    private fun drawGlobalStamps(c: Canvas, w: Float, h: Float, raw: Float) {
        if (stampIcons.isEmpty() || w <= 0f || h <= 0f) return
        val sec = raw.toInt()
        if (sec != lastGlobalStampSec) {
            lastGlobalStampSec = sec
            val size = (90f * d)
            val x = hash(sec * 7 + 3) * (w - size)
            val y = hash(sec * 7 + 9) * (h - size)
            val idx = (hash(sec * 7 + 15) * stampIcons.size).toInt().coerceIn(0, stampIcons.size - 1)
            globalStamps.add(floatArrayOf(idx.toFloat(), x, y))
        }
        val size = (90f * d).toInt()
        for (s in globalStamps) {
            val icon = stampIcons[s[0].toInt()]
            icon.setBounds(s[1].toInt(), s[2].toInt(), s[1].toInt() + size, s[2].toInt() + size)
            icon.alpha = 255 // sempre cheio: não desaparece até o usuário parar o efeito
            icon.draw(c)
        }
    }

    private fun drawWash(c: Canvas, w: Float, h: Float, t: Float) {
        // Mistura suave entre ciano e magenta (ciclo de ~2,5 s).
        val k = (sin(t * 2.5f) + 1f) / 2f
        val r = (0x33 + (0xFF - 0x33) * k).toInt()
        val g = (0xFF - (0xFF - 0x33) * k).toInt()
        val b = (0xCC - (0xCC - 0xAA) * k).toInt()
        c.drawColor(argb(0.42f, (r shl 16) or (g shl 8) or b))
        paint.color = argb(0.30f, 0xFFFFFF)
        val bars = 9
        val shift = (t * 40f) % (w / bars * 2f)
        for (i in -1..bars) {
            val x = i * w / bars + shift
            c.drawRect(x, 0f, x + w / (bars * 2f), h, paint)
        }
    }

    private fun drawSpiral(c: Canvas, w: Float, h: Float, t: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        val maxR = kotlin.math.hypot(w, h) / 2f
        for (arm in 0..2) {
            paint.color = argb(0.80f, intArrayOf(0xFF00D0, 0x00FFE0, 0xFFE000)[arm])
            path.reset()
            val phase = arm * 2.094f
            for (i in 0..220) {
                val ang = i * 0.13f + t * 1.6f + phase
                val r = i / 220f * maxR
                val x = w / 2 + cos(ang) * r
                val y = h / 2 + sin(ang) * r
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            c.drawPath(path, paint)
        }
    }

    private fun drawGlitch(c: Canvas, w: Float, h: Float, t: Float) {
        for (i in 0..13) {
            val y = (i * 79f + t * 160f) % h
            paint.color = argb(0.65f, if (i % 2 == 0) 0x00FFFF else 0xFF0099)
            val shift = sin(t * 1.8f + i) * 40f
            c.drawRect(shift, y, w - shift, y + 9f + (i % 3) * 8f, paint)
        }
    }

    private fun drawWave(c: Canvas, w: Float, h: Float, t: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        for (line in 0..17) {
            paint.color = argb(0.65f, 0x00CCFF + line * 0x070000)
            path.reset()
            var x = 0f
            var first = true
            while (x <= w) {
                val y = h * (line + 1) / 19f + sin(x / 48f + t * 3f + line) * 26f
                if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                x += 12f
            }
            c.drawPath(path, paint)
        }
    }

    private fun drawChaos(c: Canvas, w: Float, h: Float, t: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        val colors = intArrayOf(0xFF00FF, 0x33FFFF, 0xFFFF00, 0xFFFFFF)
        for (i in 0..25) {
            val x = (i * 137f + t * (i % 4 + 1) * 90f) % w
            val y = (i * 83f + t * (i % 3 + 1) * 70f) % h
            paint.color = argb(0.72f, colors[i % 4])
            c.drawCircle(x, y, 12f + (i % 5) * 9f, paint)
            c.drawLine(x - 25f, y, x + 25f, y, paint)
        }
    }

    private fun drawTunnel(c: Canvas, w: Float, h: Float, t: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        val rings = 12
        val flow = (t * 0.6f) % 1f
        for (i in 0 until rings) {
            val f = ((i + flow) / rings)
            val grow = f * f
            val hw = w / 2f * grow
            val hh = h / 2f * grow
            paint.color = argb(0.25f + 0.7f * f, if (i % 2 == 0) 0xFF00D0 else 0x00E0FF)
            c.drawRoundRect(w / 2 - hw, h / 2 - hh, w / 2 + hw, h / 2 + hh, 24f * grow, 24f * grow, paint)
        }
    }

    private fun drawMelt(c: Canvas, w: Float, h: Float, t: Float) {
        val cols = 18
        val cw = w / cols
        val colors = intArrayOf(0xFF40D0, 0x40FFE0, 0xFFE040, 0xFFFFFF)
        for (i in 0 until cols) {
            val speed = 0.12f + (i * 37 % 7) * 0.03f
            val len = h * (0.15f + (i * 53 % 5) * 0.06f)
            val y = ((t * speed + i * 0.17f) % 1f) * (h + len) - len
            paint.color = argb(0.68f, colors[i % 4])
            c.drawRect(i * cw, y, i * cw + cw - 3f, y + len, paint)
            c.drawCircle(i * cw + cw / 2f - 1.5f, y + len, cw / 2f - 1.5f + abs(sin(t + i)) * 2f, paint)
        }
    }

    // ---------- efeitos mais complexos ----------

    private fun tri(p: Float): Float = abs((((p % 2f) + 2f) % 2f) - 1f)

    private fun hash(n: Int): Float {
        var x = n * 374761393 + 668265263
        x = (x xor (x ushr 13)) * 1274126177.toInt()
        x = x xor (x ushr 16)
        return (x and 0xFFFF) / 65535f
    }

    private fun drawPlasma(c: Canvas, w: Float, h: Float, t: Float) {
        val cell = 26f * d
        val cols = ceil(w / cell).toInt()
        val rows = ceil(h / cell).toInt()
        paint.style = Paint.Style.FILL
        for (ry in 0 until rows) {
            for (rx in 0 until cols) {
                val v = sin(rx * 0.35f + t * 1.3f) + sin(ry * 0.30f - t * 1.1f) +
                    sin((rx + ry) * 0.20f + t * 0.9f) +
                    sin(hypot(rx - cols / 2f, ry - rows / 2f) * 0.45f - t * 1.5f)
                hsv[0] = (((v + 4f) / 8f) * 360f + t * 25f) % 360f
                hsv[1] = 1.0f
                hsv[2] = 1f
                paint.color = Color.HSVToColor(a(0.5f), hsv)
                c.drawRect(rx * cell, ry * cell, (rx + 1) * cell, (ry + 1) * cell, paint)
            }
        }
    }

    private fun drawMoire(c: Canvas, w: Float, h: Float, t: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * d
        val spacing = 30f * d
        val maxR = hypot(w, h)
        val rings = ceil(maxR / spacing).toInt()
        val x1 = w / 2 + cos(t * 0.7f) * w * 0.20f
        val y1 = h / 2 + sin(t * 0.9f) * h * 0.15f
        val x2 = w / 2 - cos(t * 0.5f) * w * 0.25f
        val y2 = h / 2 + sin(t * 0.6f) * h * 0.20f
        val drift = (t * 22f * d) % spacing
        paint.color = argb(0.70f, 0xFF00D0)
        for (k in 0..rings) c.drawCircle(x1, y1, k * spacing + drift, paint)
        paint.color = argb(0.70f, 0x00FFE8)
        for (k in 0..rings) c.drawCircle(x2, y2, k * spacing, paint)
    }

    private fun drawBlocks(c: Canvas, w: Float, h: Float, t: Float) {
        // Blocos aparecem e somem com fade suave; trocam de lugar ~2,5x por segundo.
        val rate = t * 2.5f
        val step = floor(rate).toInt()
        val fade = sin((rate - step) * PI.toFloat())
        val colors = intArrayOf(0xFF40D0, 0x40FFE8, 0xFFE040, 0xFFFFFF)
        paint.style = Paint.Style.FILL
        for (i in 0 until 14) {
            val seed = step * 16 + i
            val x = hash(seed) * w
            val y = hash(seed + 1000) * h
            val bw = (0.10f + hash(seed + 2000) * 0.5f) * w
            val bh = (0.02f + hash(seed + 3000) * 0.10f) * h
            val col = colors[(hash(seed + 4000) * 4f).toInt() % 4]
            val split = 8f * d
            paint.color = argb(0.55f * fade, 0x00FFFF)
            c.drawRect(x - split, y, x - split + bw, y + bh, paint)
            paint.color = argb(0.55f * fade, 0xFF00FF)
            c.drawRect(x + split, y, x + split + bw, y + bh, paint)
            paint.color = argb(0.75f * fade, col)
            c.drawRect(x, y, x + bw, y + bh, paint)
        }
    }

    private fun drawIcons(c: Canvas, w: Float, h: Float, t: Float) {
        if (icons.isEmpty()) return
        val size = (56f * d).toInt()

        // Marcas já deixadas no caminho: ficam na tela, cheias, sem sumir.
        for (stamp in trail) {
            val icon = icons[stamp.iconIndex % icons.size]
            icon.setBounds(stamp.x.toInt(), stamp.y.toInt(), stamp.x.toInt() + size, stamp.y.toInt() + size)
            icon.alpha = a(1f)
            icon.draw(c)
        }

        // Ícones em movimento; a cada ~0,1 s de posição nova, vira marca permanente.
        for (i in 0 until 6) {
            val vx = 0.30f + i * 0.07f
            val vy = 0.22f + i * 0.05f
            val x = tri(t * vx + hash(i) * 2f) * (w - size)
            val y = tri(t * vy + hash(i + 50) * 2f) * (h - size)
            if (t - lastStamp[i] >= stampIntervalSec) {
                lastStamp[i] = t
                trail.addLast(Stamp(i, x, y))
                while (trail.size > maxTrail) trail.removeFirst()
            }
            val icon = icons[i % icons.size]
            icon.setBounds(x.toInt(), y.toInt(), x.toInt() + size, y.toInt() + size)
            icon.alpha = a(1f)
            icon.draw(c)
        }
    }

    private fun drawHypno(c: Canvas, w: Float, h: Float, t: Float) {
        // Cunhas girando devagar (menos de 1 ciclo de cor por segundo em qualquer ponto).
        val spokes = 16
        val rot = t * 0.6f
        val r = hypot(w, h)
        val cx = w / 2
        val cy = h / 2
        paint.style = Paint.Style.FILL
        for (i in 0 until spokes step 2) {
            val a0 = rot + i * (2f * PI.toFloat() / spokes)
            val a1 = a0 + 2f * PI.toFloat() / spokes
            path.reset()
            path.moveTo(cx, cy)
            path.lineTo(cx + cos(a0) * r, cy + sin(a0) * r)
            path.lineTo(cx + cos(a1) * r, cy + sin(a1) * r)
            path.close()
            paint.color = argb(0.55f, if ((i / 2) % 2 == 0) 0xFF00D0 else 0x00E0FF)
            c.drawPath(path, paint)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f * d
        paint.color = argb(0.7f, 0xFFFFFF)
        val pulse = 0.5f + 0.5f * sin(t * 2f)
        c.drawCircle(cx, cy, 40f * d + pulse * 60f * d, paint)
    }

    private fun drawScanlines(c: Canvas, w: Float, h: Float, t: Float) {
        paint.style = Paint.Style.FILL
        paint.color = argb(0.30f, 0x000000)
        val gap = 6f * d
        var y = -gap + (t * 30f * d) % gap
        while (y < h) {
            c.drawRect(0f, y, w, y + 2f * d, paint)
            y += gap
        }
    }

    private fun drawMix(c: Canvas, w: Float, h: Float, t: Float) {
        layer = 0.55f; drawPlasma(c, w, h, t)
        layer = 0.80f; drawMoire(c, w, h, t)
        layer = 0.85f; drawBlocks(c, w, h, t)
        layer = 0.80f; drawGlitch(c, w, h, t)
        layer = 1.00f; drawIcons(c, w, h, t)
        layer = 1.00f; drawScanlines(c, w, h, t)
        layer = 1f
    }

    // ---------- efeitos que distorcem a tela real (estilo GDI de PC) ----------
    // Todos usam só transformações espaciais (sem inverter ou alternar cores), então a
    // realimentação da própria sobreposição na captura gera rastros, não piscadas.
    // Os deslocamentos são por velocidade (px/s * dt), logo ficam limitados e suaves.

    private fun drawScreen(c: Canvas, bmp: Bitmap, w: Float, h: Float, raw: Float, t: Float, dt: Float) {
        srcRect.set(0, 0, frameW, frameH)
        // Deixa um pouco da tela real vazar a cada quadro, para o conteúdo vivo reaparecer.
        bmpPaint.alpha = (185 + 60 * intensity.alpha).toInt().coerceIn(0, 255)
        // O efeito começa mais leve e vai escalando nos primeiros ~20 s.
        val amp = (0.35f + 0.65f * (raw / 20f).coerceIn(0f, 1f)) * intensity.speed
        when (effect) {
            Effect.SCR_MELT -> screenMelt(c, bmp, w, h, amp, dt)
            Effect.SCR_WAVE -> screenWave(c, bmp, w, h, t, amp, dt)
            Effect.SCR_TUNNEL -> screenTunnel(c, bmp, w, h, t, amp)
            Effect.SCR_GLITCH -> screenGlitch(c, bmp, w, h, t, amp, dt)
            Effect.SCR_TILES -> screenTiles(c, bmp, w, h, t, amp)
            Effect.SCR_MIRROR -> screenMirror(c, bmp, w, h, t)
            Effect.SCR_MULTIPLY -> screenMultiply(c, bmp, w, h, raw)
            Effect.SCR_SPIN -> screenSpin(c, bmp, w, h, t)
            Effect.SCR_MEMZ_TOTAL -> {
                // Ciclo tipo MEMZ: cada fase 7 s, escalando
                val phase = (raw / 7f).toInt() % 8
                when (phase) {
                    0 -> screenTiles(c, bmp, w, h, t, amp)
                    1 -> screenGlitch(c, bmp, w, h, t, amp, dt)
                    2 -> { screenMelt(c, bmp, w, h, amp, dt); layer=0.7f; drawMemzMatrix(c, w, h, t) }
                    3 -> screenMirror(c, bmp, w, h, t)
                    4 -> { screenWave(c, bmp, w, h, t, amp, dt); layer=0.9f; drawMemzEyes(c, w, h, t) }
                    5 -> screenMultiply(c, bmp, w, h, raw)
                    6 -> screenSpin(c, bmp, w, h, t)
                    else -> { screenTunnel(c, bmp, w, h, t, amp); layer=0.8f; drawMemzMatrix(c, w, h, t) }
                }
                layer = 1f
            }
            else -> {
                when ((raw / 6f).toInt() % 4) {
                    0 -> screenMelt(c, bmp, w, h, amp, dt)
                    1 -> screenWave(c, bmp, w, h, t, amp, dt)
                    2 -> screenTunnel(c, bmp, w, h, t, amp)
                    else -> screenGlitch(c, bmp, w, h, t, amp, dt)
                }
                layer = 1f
                drawIcons(c, w, h, t)
            }
        }
    }

    private fun screenMelt(c: Canvas, bmp: Bitmap, w: Float, h: Float, amp: Float, dt: Float) {
        val cols = 40
        val colW = w / cols
        val sCol = frameW.toFloat() / cols
        for (i in 0 until cols) {
            val dy = (25f + 110f * hash(i + 7)) * d * amp * dt
            srcRect.set((i * sCol).toInt(), 0, ((i + 1) * sCol).toInt().coerceAtLeast((i * sCol).toInt() + 1), frameH)
            dstRect.set(i * colW, dy, (i + 1) * colW + 1f, h + dy)
            c.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
        }
    }

    private fun screenWave(c: Canvas, bmp: Bitmap, w: Float, h: Float, t: Float, amp: Float, dt: Float) {
        val strip = 6f * d
        val n = ceil(h / strip).toInt()
        val sStrip = frameH.toFloat() / n
        for (i in 0 until n) {
            val y = i * strip
            val dx = cos(y / (45f * d) + t * 2.2f) * 40f * d * amp * dt
            val top = (i * sStrip).toInt()
            srcRect.set(0, top, frameW, ((i + 1) * sStrip).toInt().coerceAtLeast(top + 1).coerceAtMost(frameH))
            dstRect.set(dx, y, w + dx, y + strip + 1f)
            c.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
        }
    }

    private fun screenTunnel(c: Canvas, bmp: Bitmap, w: Float, h: Float, t: Float, amp: Float) {
        dstRect.set(0f, 0f, w, h)
        c.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
        bmpPaint.alpha = 200
        val k0 = 0.6f + 0.4f * amp.coerceAtMost(1.5f)
        for (k in 1..4) {
            val s = (1f - 0.11f * k * k0 + 0.02f * sin(t * 1.5f + k)).coerceAtLeast(0.2f)
            c.save()
            c.translate(w / 2, h / 2)
            c.rotate(sin(t * 0.6f + k) * 2f * k * amp)
            c.scale(s, s)
            c.translate(-w / 2, -h / 2)
            c.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
            c.restore()
        }
    }

    private fun screenGlitch(c: Canvas, bmp: Bitmap, w: Float, h: Float, t: Float, amp: Float, dt: Float) {
        // Separação de canais R/G/B (aberração cromática) com deslocamento oscilante.
        val dxc = cos(t * 1.7f) * 20f * d * amp * dt
        dstRect.set(-dxc, 0f, w - dxc, h)
        addPaint.colorFilter = chR
        c.drawBitmap(bmp, srcRect, dstRect, addPaint)
        dstRect.set(0f, 0f, w, h)
        addPaint.colorFilter = chG
        c.drawBitmap(bmp, srcRect, dstRect, addPaint)
        dstRect.set(dxc, 0f, w + dxc, h)
        addPaint.colorFilter = chB
        c.drawBitmap(bmp, srcRect, dstRect, addPaint)
        // Faixas horizontais que deslizam para os lados.
        for (i in 0 until 8) {
            val y0 = (hash(i + 30) * h + t * 20f * d * (i % 3 + 1)) % h
            val hh = (0.03f + hash(i + 60) * 0.07f) * h
            val dx = cos(t * 1.3f + i * 1.7f) * 60f * d * amp * dt
            val top = (y0 / h * frameH).toInt()
            if (top >= frameH - 1) continue
            val bot = ((y0 + hh) / h * frameH).toInt().coerceAtLeast(top + 1).coerceAtMost(frameH)
            srcRect.set(0, top, frameW, bot)
            dstRect.set(dx, y0, w + dx, y0 + (bot - top) * h / frameH)
            c.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
        }
        srcRect.set(0, 0, frameW, frameH)
    }

    // ── screenTiles: grid de tiles da tela embaralhados (estilo MEMZ) ──────────────
    private fun screenTiles(c: Canvas, bmp: Bitmap, w: Float, h: Float, t: Float, amp: Float) {
        val cols = 8; val rows = 14
        val tw = w / cols; val th = h / rows
        val stw = frameW.toFloat() / cols; val sth = frameH.toFloat() / rows
        for (ry in 0 until rows) for (rx in 0 until cols) {
            val phase = rx * 0.9f + ry * 0.5f
            val dx = sin(phase + t * 1.4f) * 55f * amp
            val dy = cos(phase * 1.1f + t * 1.1f) * 40f * amp
            val rot = sin(phase * 0.7f + t * 0.8f) * 6f * amp
            val sx = (rx * stw).toInt(); val sy = (ry * sth).toInt()
            val ex = (sx + stw).toInt().coerceAtMost(frameW); val ey = (sy + sth).toInt().coerceAtMost(frameH)
            if (ex <= sx || ey <= sy) continue
            srcRect.set(sx, sy, ex, ey)
            val dstX = rx * tw + dx; val dstY = ry * th + dy
            c.save()
            c.rotate(rot, dstX + tw / 2, dstY + th / 2)
            dstRect.set(dstX, dstY, dstX + tw, dstY + th)
            c.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
            c.restore()
        }
        srcRect.set(0, 0, frameW, frameH)
    }

    // ── screenMirror: caleidoscópio 4 quadrantes da tela ─────────────────────────
    private fun screenMirror(c: Canvas, bmp: Bitmap, w: Float, h: Float, t: Float) {
        val hw = w / 2; val hh = h / 2
        val shw = frameW / 2; val shh = frameH / 2
        // Q1 normal
        srcRect.set(0, 0, shw, shh)
        dstRect.set(0f, 0f, hw, hh)
        c.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
        // Q2 espelhado horizontalmente
        c.save(); c.scale(-1f, 1f, hw, 0f)
        c.drawBitmap(bmp, srcRect, dstRect, bmpPaint); c.restore()
        // Q3 espelhado verticalmente
        c.save(); c.scale(1f, -1f, 0f, hh)
        c.drawBitmap(bmp, srcRect, dstRect, bmpPaint); c.restore()
        // Q4 espelhado em ambos
        c.save(); c.scale(-1f, -1f, hw, hh)
        c.drawBitmap(bmp, srcRect, dstRect, bmpPaint); c.restore()
        // Rotação lenta no centro (costura os quadrantes)
        val rot = (t * 8f) % 360f
        c.save(); c.rotate(rot, w/2, h/2); c.scale(0.18f, 0.18f, w/2, h/2)
        srcRect.set(0, 0, frameW, frameH); dstRect.set(0f, 0f, w, h)
        c.drawBitmap(bmp, srcRect, dstRect, bmpPaint); c.restore()
        srcRect.set(0, 0, frameW, frameH)
    }

    // ── screenMultiply: a tela se multiplica em uma grade que cresce a cada ~1 s ──
    // Giro e cópias, sem nenhuma cor piscando — seguro para fotossensibilidade.
    private fun screenMultiply(c: Canvas, bmp: Bitmap, w: Float, h: Float, raw: Float) {
        // Cresce 1 -> 2 -> 3 ... a cada segundo, até 6x6, depois recomeça.
        val step = (raw.toInt() % 6) + 1
        val frac = (raw - floor(raw)) // 0..1, usado para crescer suavemente até o próximo tamanho
        val n = (step + frac).coerceAtMost(6f)
        val cols = ceil(n).toInt().coerceIn(1, 6)
        val cw = w / cols; val ch = h / cols
        for (ry in 0 until cols) for (rx in 0 until cols) {
            dstRect.set(rx * cw, ry * ch, (rx + 1) * cw, (ry + 1) * ch)
            c.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
        }
    }

    // ── screenSpin: a tela inteira girando devagar e continuamente ───────────────
    // Rotação lisa e constante — não é flash de cor, então é seguro para fotossensibilidade.
    private fun screenSpin(c: Canvas, bmp: Bitmap, w: Float, h: Float, t: Float) {
        val rot = (t * 25f) % 360f // 25°/s * velocidade da intensidade: giro suave, nunca abrupto
        val scale = 1.42f // cobre os cantos durante o giro
        c.save()
        c.rotate(rot, w / 2, h / 2)
        c.scale(scale, scale, w / 2, h / 2)
        dstRect.set(0f, 0f, w, h)
        c.drawBitmap(bmp, srcRect, dstRect, bmpPaint)
        c.restore()
    }

    // ── drawMemzMatrix: chuva de caracteres estilo Matrix/MEMZ ───────────────────
    private fun drawMemzMatrix(c: Canvas, w: Float, h: Float, t: Float) {
        val cw = 17f * d; val rh = 20f * d
        txtPaint.textSize = 16f * d
        val cols = ceil(w / cw).toInt(); val rows = ceil(h / rh).toInt()
        for (col in 0 until cols) {
            val spd = 8f + hash(col) * 11f
            val head = ((t * spd + hash(col + 77) * rows) % (rows + 24)).toInt()
            for (row in 0 until rows) {
                val dist = head - row
                if (dist < 0 || dist > 26) continue
                val bright = (1f - dist / 26f).coerceAtLeast(0f)
                val ci = (hash(col * 999 + row + (t * 1.5f).toInt()) * matChars.size).toInt()
                    .coerceIn(0, matChars.size - 1)
                txtPaint.color = if (dist == 0)
                    Color.argb(a(bright), 210, 255, 210)
                else
                    Color.argb(a((bright * 0.95f).coerceAtLeast(0.15f)), 0, 255, 40)
                c.drawText(matChars[ci].toString(), col * cw, (row + 1) * rh, txtPaint)
            }
        }
    }

    // ── drawMemzPipes: screensaver Windows 3D Pipes ───────────────────────────────
    private fun initPipes(w: Float, h: Float) {
        if (pipesReady) return; pipesReady = true
        val dirs = arrayOf(floatArrayOf(1f,0f),floatArrayOf(-1f,0f),floatArrayOf(0f,1f),floatArrayOf(0f,-1f))
        val now = SystemClock.uptimeMillis()
        for (i in 0 until 8) {
            val x = hash(i*13+1) * w; val y = hash(i*13+2) * h
            val dir = dirs[i % 4]
            // nextTurnMs entre 500ms e 2500ms
            val nextTurn = now + 500L + (hash(i*13+5) * 2000f).toLong()
            pipeHeads.add(floatArrayOf(x, y, dir[0], dir[1], (i % PIPE_COLS.size).toFloat(), nextTurn.toFloat()))
        }
        pipeLastMs = now
    }

    private fun drawMemzPipes(c: Canvas, w: Float, h: Float) {
        initPipes(w, h)
        val now = SystemClock.uptimeMillis()
        val dt = ((now - pipeLastMs) / 1000f).coerceIn(0f, 0.05f); pipeLastMs = now
        val spd = 90f * d * intensity.speed

        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND
        val pw = 11f * d; paint.strokeWidth = pw

        // Desenha segmentos acumulados
        for (seg in pipeSegs) {
            paint.color = argb(0.88f, seg.col)
            c.drawLine(seg.x1, seg.y1, seg.x2, seg.y2, paint)
            paint.style = Paint.Style.FILL
            c.drawCircle(seg.x2, seg.y2, pw * 0.9f, paint) // junção
            paint.style = Paint.Style.STROKE
        }

        // Move e possivelmente vira cada cabeça
        val dirs = arrayOf(floatArrayOf(1f,0f),floatArrayOf(-1f,0f),floatArrayOf(0f,1f),floatArrayOf(0f,-1f))
        for (head in pipeHeads) {
            val col = PIPE_COLS[head[4].toInt() % PIPE_COLS.size]
            val step = spd * dt
            val nx = head[0] + head[2] * step; val ny = head[1] + head[3] * step
            pipeSegs.add(PSeg(head[0], head[1], nx, ny, col))
            head[0] = nx; head[1] = ny
            val shouldTurn = nx < 0 || nx > w || ny < 0 || ny > h || now >= head[5].toLong()
            if (shouldTurn) {
                head[0] = nx.coerceIn(60f, w - 60f); head[1] = ny.coerceIn(60f, h - 60f)
                val perp = dirs.filter { it[0] != head[2] && it[1] != head[3] }
                val pick = perp[(hash((nx * 7 + ny + now).toInt() and 0x7FFFFFFF) * perp.size).toInt().coerceIn(0, perp.size-1)]
                head[2] = pick[0]; head[3] = pick[1]
                head[5] = (now + 400L + (hash((now + head[4]).toInt() and 0x7FFFFFFF) * 2200f).toLong()).toFloat()
            }
            paint.style = Paint.Style.FILL
            paint.color = argb(1f, col)
            c.drawCircle(head[0], head[1], pw * 1.1f, paint)
        }
        while (pipeSegs.size > 2500) pipeSegs.removeAt(0)
    }

    // ── drawMemzEyes: olhos inquietantes estilo MEMZ ──────────────────────────────
    private fun drawMemzEyes(c: Canvas, w: Float, h: Float, t: Float) {
        // Ponto de foco que os olhos seguem (Lissajous lento)
        val tx = w / 2 + cos(t * 0.6f) * w * 0.3f
        val ty = h / 2 + sin(t * 0.5f * 1.3f) * h * 0.25f

        paint.style = Paint.Style.FILL
        for (ep in eyePairs) {
            val cx = ep[0] * w; val cy = ep[1] * h; val sz = ep[2] * (w.coerceAtMost(h))
            // Piscar suave (não flash: só fecha o olho, não inverte cores)
            val blinkK = ((sin(t * 0.9f + ep[4]) + 1f) / 2f).coerceIn(0.05f, 1f)

            // Branco do olho com brilho
            paint.color = argb(0.90f, 0xFFFFFF)
            c.drawOval(cx - sz, cy - sz * blinkK, cx + sz, cy + sz * blinkK, paint)

            // Íris vermelha/escura
            val iris = sz * 0.52f
            paint.color = argb(1.0f, 0xFF1100)
            c.drawCircle(cx, cy, iris, paint)

            // Pupila: aponta para o alvo
            val ang = kotlin.math.atan2(ty - cy, tx - cx)
            val pupOff = iris * 0.35f
            val px = cx + cos(ang) * pupOff; val py = cy + sin(ang) * pupOff
            paint.color = argb(1f, 0x000000)
            c.drawCircle(px, py, iris * 0.48f, paint)

            // Reflexo
            paint.color = argb(0.8f, 0xFFFFFF)
            c.drawCircle(px - iris * 0.15f, py - iris * 0.15f, iris * 0.14f, paint)
        }
    }
}
