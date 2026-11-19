package com.xkeyboard.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.SparseArray
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import java.io.File
import kotlin.math.abs

class XKeyboardView(ctx: Context) : View(ctx) {

    interface Listener {
        fun onText(text: String)
        fun onSpace()
        fun onDelete()
        fun onEnter()
        fun onCursor(dir: Int)
        fun onToggleLanguage()
        fun onOpenEmoji()
    }

    companion object {
        const val MODE_LETTERS = 0
        const val MODE_SYM1 = 1
        const val MODE_SYM2 = 2
        const val MODE_BANGLA_1 = 3
        const val MODE_BANGLA_2 = 4

        const val SHIFT_OFF = 0
        const val SHIFT_ON = 1
        const val SHIFT_CAPS = 2

        const val C_SHIFT = -1
        const val C_SYM = -2
        const val C_ABC = -3
        const val C_ENTER = -4
        const val C_DEL = -5
        const val C_SYM2 = -6
        const val C_SYM1 = -7
        const val C_SPACER = -8
        const val C_LANG = -9
        const val C_EMOJI = -10
        const val C_BN_MORE = -11
        const val C_SPACE = 32
    }

    private class Key(val label: String, val code: Int = 0, val weight: Float = 1f, val alt: String = "") {
        var l = 0f
        var t = 0f
        var r = 0f
        var b = 0f
        val isChar: Boolean get() = code == 0
    }

    private class Ptr(var key: Key, val downX: Float) {
        var lastX = downX
        var longFired = false
        var moved = false
        var accum = 0f
        var run: Runnable? = null
    }

    var listener: Listener? = null
    var enterLabel: String? = null
        set(v) {
            field = v
            invalidate()
        }

    private var mode = MODE_LETTERS
    private var shift = SHIFT_OFF
    private var numRow = false
    private var haptic = true
    private var lang = "en"
    private var heightScale = 1.0f
    private var cornerRadiusDp = 10f
    private var theme: Theme = Themes.resolve(ctx)
    private var rows: List<List<Key>> = emptyList()
    private var padT = 0f
    private var rowH = 0f
    private var lastShiftTap = 0L

    private var wallpaperBmp: Bitmap? = null
    private var loadedWallpaperPath = ""
    private var wallpaperDimAlpha = 115

    private val soundPlayer = SoundPlayer(ctx)
    private val handler = Handler(Looper.getMainLooper())
    private val ptrs = SparseArray<Ptr>()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val rect = RectF()
    private val srcRect = Rect()
    private val dstRect = Rect()
    private val path = Path()

    init {
        applyPrefs()
    }

    fun applyPrefs() {
        theme = Themes.resolve(context)
        numRow = Prefs.numRow(context)
        haptic = Prefs.haptic(context)
        lang = Prefs.lang(context)
        heightScale = Prefs.heightPercent(context) / 100f
        cornerRadiusDp = Prefs.keyRadius(context).toFloat()
        wallpaperDimAlpha = (Prefs.wallpaperDim(context) * 255 / 100).coerceIn(0, 230)
        loadWallpaperIfNeeded()

        if (mode == MODE_LETTERS && lang == "bn") mode = MODE_BANGLA_1
        if ((mode == MODE_BANGLA_1 || mode == MODE_BANGLA_2) && lang == "en") mode = MODE_LETTERS

        rows = buildRows()
        requestLayout()
        layoutKeys()
        invalidate()
    }

    fun setLiveHeightPercent(percent: Int) {
        heightScale = percent.coerceIn(72, 140) / 100f
        requestLayout()
        layoutKeys()
        invalidate()
    }

    private fun loadWallpaperIfNeeded() {
        val wp = Prefs.wallpaperPath(context)
        if (wp == loadedWallpaperPath) return
        loadedWallpaperPath = wp
        wallpaperBmp?.recycle()
        wallpaperBmp = null
        if (wp.isNotEmpty() && File(wp).exists()) {
            wallpaperBmp = runCatching { BitmapFactory.decodeFile(wp) }.getOrNull()
        }
    }

    fun setMode(m: Int) {
        mode = m
        rows = buildRows()
        requestLayout()
        layoutKeys()
        invalidate()
    }

    fun resetToMainLanguageMode() {
        lang = Prefs.lang(context)
        setMode(if (lang == "bn") MODE_BANGLA_1 else MODE_LETTERS)
    }

    fun setAutoShift(on: Boolean) {
        if (shift == SHIFT_CAPS || mode != MODE_LETTERS) return
        val n = if (on) SHIFT_ON else SHIFT_OFF
        if (n != shift) {
            shift = n
            invalidate()
        }
    }

    private fun chars(s: String, alts: String = ""): List<Key> =
        s.mapIndexed { i, ch -> Key(ch.toString(), 0, 1f, if (i < alts.length) alts[i].toString() else "") }

    private fun bottom(left: String, leftCode: Int): List<Key> = listOf(
        Key(left, leftCode, 1.35f),
        Key("☺", C_EMOJI, 1.05f),
        Key(if (lang == "bn") "EN" else "বাং", C_LANG, 1.05f),
        Key(" ", C_SPACE, 4.1f),
        Key(if (lang == "bn") "।" else ".", 0, 0.95f, ","),
        Key("", C_ENTER, 1.5f)
    )

    private fun buildRows(): List<List<Key>> {
        val out = ArrayList<List<Key>>()
        when (mode) {
            MODE_LETTERS -> {
                if (numRow) out.add(chars("1234567890"))
                out.add(chars("qwertyuiop", if (numRow) "" else "1234567890"))
                out.add(listOf(Key("", C_SPACER, 0.5f)) + chars("asdfghjkl", "@#\$%&-+()") + Key("", C_SPACER, 0.5f))
                out.add(listOf(Key("", C_SHIFT, 1.5f)) + chars("zxcvbnm", "*"':/!?") + Key("", C_DEL, 1.5f))
                out.add(bottom("?123", C_SYM))
            }
            MODE_BANGLA_1 -> {
                if (numRow) out.add(chars("১২৩৪৫৬৭৮৯০"))
                out.add(chars("কখগঘঙচছজঝঞ", "১২৩৪৫৬৭৮৯০"))
                out.add(chars("টঠডঢণতথদধন", "অআইঈউঊঋএঐও"))
                out.add(listOf(Key("যুক্ত", C_BN_MORE, 1.45f)) + chars("পফবভমযরলশষ", "ািীুূৃেৈোৌ") + Key("", C_DEL, 1.45f))
                out.add(bottom("?১২৩", C_SYM))
            }
            MODE_BANGLA_2 -> {
                if (numRow) out.add(chars("১২৩৪৫৬৭৮৯০"))
                out.add(chars("সসহড়ঢ়য়ৎংঃঁ", "ঔ্৳॥!?;:-/"))
                out.add(chars("অআইঈউঊঋএঐও", "ািীুূৃেৈোৌ"))
                out.add(listOf(Key("কখগ", C_BN_MORE, 1.45f)) + chars("ািীুূৃেৈোৌ", "ক্ষজ্ঞশ্রত্র") + Key("", C_DEL, 1.45f))
                out.add(bottom("?১২৩", C_SYM))
            }
            MODE_SYM1 -> {
                out.add(chars("1234567890"))
                out.add(chars("@#\$%&-+()/"))
                out.add(listOf(Key("=\<", C_SYM2, 1.5f)) + chars("*"':;!?") + Key("", C_DEL, 1.5f))
                out.add(bottom("ABC", C_ABC))
            }
            else -> {
                out.add(chars("~`|•√π÷×¶∆"))
                out.add(chars("£¢€¥^°={}\"))
                out.add(listOf(Key("?123", C_SYM1, 1.5f)) + chars("%©®™✓[]") + Key("", C_DEL, 1.5f))
                out.add(bottom("ABC", C_ABC))
            }
        }
        return out
    }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val baseKh = dp(52f) * heightScale
        val n = rows.size.coerceAtLeast(4)
        setMeasuredDimension(MeasureSpec.getSize(wSpec), (baseKh * n + dp(12f)).toInt())
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        layoutKeys()
    }

    private fun layoutKeys() {
        if (width == 0 || rows.isEmpty()) return
        padT = dp(6f)
        val padB = dp(6f)
        val padS = dp(3f)
        rowH = (height - padT - padB) / rows.size
        val avail = width - 2 * padS
        rows.forEachIndexed { ri, row ->
            val total = row.fold(0f) { a, k -> a + k.weight }
            var x = padS
            for (k in row) {
                val w = avail * k.weight / total
                k.l = x
                k.r = x + w
                k.t = padT + ri * rowH
                k.b = k.t + rowH
                x += w
            }
        }
    }

    private fun isDown(k: Key): Boolean {
        for (i in 0 until ptrs.size()) if (ptrs.valueAt(i).key === k) return true
        return false
    }

    override fun onDraw(c: Canvas) {
        val wp = wallpaperBmp
        if (wp != null && !wp.isRecycled) {
            srcRect.set(0, 0, wp.width, wp.height)
            dstRect.set(0, 0, width, height)
            c.drawBitmap(wp, srcRect, dstRect, null)
            fill.color = (wallpaperDimAlpha shl 24) or (theme.bg and 0x00FFFFFF)
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fill)
        } else {
            c.drawColor(theme.bg)
        }

        val gap = dp(3f)
        val rad = dp(cornerRadiusDp)
        val sh = dp(1.5f)
        for (row in rows) for (k in row) {
            if (k.code == C_SPACER) continue
            rect.set(k.l + gap, k.t + gap, k.r - gap, k.b - gap)
            val down = isDown(k)

            if (!theme.lightweightGlass && theme.shadow != 0) {
                fill.color = theme.shadow
                c.drawRoundRect(rect.left, rect.top + sh, rect.right, rect.bottom + sh, rad, rad, fill)
            }

            val accentKey = k.code == C_ENTER || (k.code == C_SHIFT && shift != SHIFT_OFF)
            fill.color = when {
                down -> theme.pressed
                accentKey -> theme.accent
                k.code < 0 -> theme.special
                else -> theme.key
            }
            c.drawRoundRect(rect, rad, rad, fill)

            if (theme.isGlass) {
                borderPaint.color = if (accentKey) 0x99FFFFFF.toInt() else theme.glassBorder
                borderPaint.strokeWidth = dp(if (theme.lightweightGlass) 0.8f else 1.2f)
                c.drawRoundRect(rect, rad, rad, borderPaint)
            }

            val cx = rect.centerX()
            val cy = rect.centerY()
            val ink = if (accentKey) theme.onAccent else theme.text
            when (k.code) {
                C_SHIFT -> drawShift(c, cx, cy, ink)
                C_DEL -> drawBackspace(c, cx, cy, ink)
                C_ENTER -> {
                    val el = enterLabel
                    if (el != null) label(c, el, cx, cy, 14f, ink, true, rect.width() - dp(8f)) else drawEnter(c, cx, cy, ink)
                }
                C_SPACE -> label(c, if (lang == "bn") "X Keyboard • বাংলা" else "X Keyboard", cx, cy, 12f, theme.hint, false)
                else -> {
                    val s = if (mode == MODE_LETTERS && shift != SHIFT_OFF) k.label.uppercase() else k.label
                    if (k.isChar) {
                        label(c, s, cx, cy, if (down) 26f else 21f, ink, false)
                        if (k.alt.isNotEmpty()) {
                            txt.color = theme.hint
                            txt.textSize = sp(9.5f)
                            txt.typeface = Typeface.DEFAULT
                            txt.textAlign = Paint.Align.RIGHT
                            c.drawText(k.alt, rect.right - dp(5f), rect.top + dp(13f), txt)
                            txt.textAlign = Paint.Align.CENTER
                        }
                    } else {
                        label(c, s, cx, cy, 14f, ink, true)
                    }
                }
            }
        }
    }

    private fun label(
        c: Canvas, s: String, cx: Float, cy: Float, sizeSp: Float, color: Int, bold: Boolean,
        maxW: Float = Float.MAX_VALUE
    ) {
        txt.color = color
        txt.textSize = sp(sizeSp)
        txt.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        val w = txt.measureText(s)
        if (w > maxW) txt.textSize = txt.textSize * maxW / w
        val fm = txt.fontMetrics
        c.drawText(s, cx, cy - (fm.ascent + fm.descent) / 2f, txt)
    }

    private fun drawShift(c: Canvas, cx: Float, cy: Float, ink: Int) {
        val s = dp(9f)
        path.reset()
        path.moveTo(cx, cy - s)
        path.lineTo(cx + s, cy)
        path.lineTo(cx + s * .45f, cy)
        path.lineTo(cx + s * .45f, cy + s * .9f)
        path.lineTo(cx - s * .45f, cy + s * .9f)
        path.lineTo(cx - s * .45f, cy)
        path.lineTo(cx - s, cy)
        path.close()
        stroke.color = ink
        stroke.strokeWidth = dp(2f)
        if (shift != SHIFT_OFF) {
            fill.color = ink
            c.drawPath(path, fill)
            if (shift == SHIFT_CAPS) {
                stroke.color = theme.onAccent
                c.drawLine(cx - s * .3f, cy + s * .45f, cx + s * .3f, cy + s * .45f, stroke)
            }
        } else {
            c.drawPath(path, stroke)
        }
    }

    private fun drawBackspace(c: Canvas, cx: Float, cy: Float, ink: Int) {
        val s = dp(9f)
        stroke.color = ink
        stroke.strokeWidth = dp(1.8f)
        path.reset()
        path.moveTo(cx - s * 1.2f, cy)
        path.lineTo(cx - s * .4f, cy - s * .8f)
        path.lineTo(cx + s * 1.1f, cy - s * .8f)
        path.lineTo(cx + s * 1.1f, cy + s * .8f)
        path.lineTo(cx - s * .4f, cy + s * .8f)
        path.close()
        c.drawPath(path, stroke)
        val x0 = cx + s * .15f
        val d = s * .35f
        c.drawLine(x0 - d, cy - d, x0 + d, cy + d, stroke)
        c.drawLine(x0 - d, cy + d, x0 + d, cy - d, stroke)
    }

    private fun drawEnter(c: Canvas, cx: Float, cy: Float, ink: Int) {
        val s = dp(8f)
        stroke.color = ink
        stroke.strokeWidth = dp(2f)
        path.reset()
        path.moveTo(cx + s, cy - s * .8f)
        path.lineTo(cx + s, cy + s * .3f)
        path.lineTo(cx - s, cy + s * .3f)
        c.drawPath(path, stroke)
        path.reset()
        path.moveTo(cx - s + s * .6f, cy + s * .3f - s * .6f)
        path.lineTo(cx - s, cy + s * .3f)
        path.lineTo(cx - s + s * .6f, cy + s * .3f + s * .6f)
        c.drawPath(path, stroke)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                down(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until e.pointerCount) move(e.getPointerId(i), e.getX(i), e.getY(i))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> up(e.getPointerId(e.actionIndex))
            MotionEvent.ACTION_CANCEL -> cancelAll()
        }
        return true
    }

    private fun keyAt(x: Float, y: Float): Key? {
        if (rows.isEmpty() || rowH <= 0f) return null
        val ri = ((y - padT) / rowH).toInt().coerceIn(0, rows.size - 1)
        val row = rows[ri]
        var idx = row.indexOfFirst { x < it.r }
        if (idx < 0) idx = row.size - 1
        if (row[idx].code == C_SPACER) idx = if (idx == 0) 1 else idx - 1
        return row[idx]
    }

    private fun down(id: Int, x: Float, y: Float) {
        val k = keyAt(x, y) ?: return
        val p = Ptr(k, x)
        ptrs.put(id, p)
        feedback(k)
        if (k.code == C_DEL) {
            listener?.onDelete()
            var interval = 85L
            val r = object : Runnable {
                override fun run() {
                    listener?.onDelete()
                    if (interval > 32L) interval -= 5L
                    handler.postDelayed(this, interval)
                }
            }
            p.run = r
            handler.postDelayed(r, 360L)
        } else if (k.alt.isNotEmpty()) {
            val r = Runnable {
                if (ptrs.get(id) === p && p.key === k) {
                    p.longFired = true
                    performHap(HapticFeedbackConstants.LONG_PRESS)
                    listener?.onText(k.alt)
                }
            }
            p.run = r
            handler.postDelayed(r, ViewConfiguration.getLongPressTimeout().toLong())
        }
        invalidate()
    }

    private fun move(id: Int, x: Float, y: Float) {
        val p = ptrs.get(id) ?: return
        val k = p.key
        if (k.code == C_SPACE) {
            if (!p.moved && abs(x - p.downX) > dp(12f)) {
                p.moved = true
                p.lastX = x
            }
            if (p.moved) {
                p.accum += x - p.lastX
                p.lastX = x
                val step = dp(14f)
                while (p.accum > step) {
                    p.accum -= step
                    listener?.onCursor(1)
                    performHap(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                while (p.accum < -step) {
                    p.accum += step
                    listener?.onCursor(-1)
                    performHap(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        } else if (k.isChar && !p.longFired) {
            val nk = keyAt(x, y)
            if (nk != null && nk !== k && nk.isChar) {
                p.run?.let { handler.removeCallbacks(it) }
                p.run = null
                p.key = nk
                invalidate()
            }
        }
    }

    private fun up(id: Int) {
        val p = ptrs.get(id) ?: return
        ptrs.remove(id)
        p.run?.let { handler.removeCallbacks(it) }
        val k = p.key
        if (!p.longFired) {
            when {
                k.code == C_DEL -> Unit
                k.code == C_SPACE -> if (!p.moved) listener?.onSpace()
                k.isChar -> emitChar(k)
                k.code == C_SHIFT -> toggleShift()
                k.code == C_SYM -> setMode(MODE_SYM1)
                k.code == C_SYM1 -> setMode(MODE_SYM1)
                k.code == C_SYM2 -> setMode(MODE_SYM2)
                k.code == C_ABC -> resetToMainLanguageMode()
                k.code == C_BN_MORE -> setMode(if (mode == MODE_BANGLA_1) MODE_BANGLA_2 else MODE_BANGLA_1)
                k.code == C_LANG -> listener?.onToggleLanguage()
                k.code == C_EMOJI -> listener?.onOpenEmoji()
                k.code == C_ENTER -> listener?.onEnter()
            }
        }
        invalidate()
    }

    private fun cancelAll() {
        for (i in 0 until ptrs.size()) ptrs.valueAt(i).run?.let { handler.removeCallbacks(it) }
        ptrs.clear()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        cancelAll()
        soundPlayer.release()
        super.onDetachedFromWindow()
    }

    private fun emitChar(k: Key) {
        val t = if (mode == MODE_LETTERS && shift != SHIFT_OFF) k.label.uppercase() else k.label
        if (mode == MODE_LETTERS && shift == SHIFT_ON) shift = SHIFT_OFF
        listener?.onText(t)
    }

    private fun toggleShift() {
        val now = SystemClock.uptimeMillis()
        shift = when {
            shift == SHIFT_CAPS -> SHIFT_OFF
            now - lastShiftTap < 300L -> SHIFT_CAPS
            shift == SHIFT_ON -> SHIFT_OFF
            else -> SHIFT_ON
        }
        lastShiftTap = now
    }

    @Suppress("DEPRECATION")
    private fun performHap(type: Int) {
        if (haptic) performHapticFeedback(type, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
    }

    private fun feedback(k: Key) {
        performHap(HapticFeedbackConstants.KEYBOARD_TAP)
        soundPlayer.playKey(k.code)
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)
}
