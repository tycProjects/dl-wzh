package com.schedule.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import com.schedule.app.data.Slot
import com.schedule.app.util.DAY_NAMES
import com.schedule.app.util.formatRange
import java.io.File

private val PALETTE = intArrayOf(0xFFB91C3C.toInt(), 0xFF2563EB.toInt(), 0xFF0D9488.toInt(), 0xFFD97706.toInt(), 0xFF7C3AED.toInt(), 0xFF16A34A.toInt(), 0xFFDB2777.toInt(), 0xFF64748B.toInt())

/** يرسم الجدول الأسبوعي كصورة (قائمة لكل يوم) لمشاركتها. */
fun renderScheduleBitmap(slots: List<Slot>): Bitmap {
    val w = 1080
    val pad = 40f
    val rowH = 112f
    val headH = 74f
    val days = (0..6).map { d -> d to slots.filter { it.day == d }.sortedBy { it.startMin } }.filter { it.second.isNotEmpty() }
    var h = pad * 2 + 90f
    days.forEach { h += headH + it.second.size * rowH + 16f }
    if (days.isEmpty()) h += 120f
    val bmp = Bitmap.createBitmap(w, h.toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(0xFF0A0A0B.toInt())
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    p.color = 0xFFEDEDED.toInt(); p.textSize = 54f; p.typeface = Typeface.DEFAULT_BOLD; p.textAlign = Paint.Align.RIGHT
    c.drawText("جدولي الأسبوعي", w - pad, pad + 54f, p)
    var y = pad + 90f
    if (days.isEmpty()) { p.textSize = 36f; p.color = 0xFF8E8E96.toInt(); c.drawText("لا توجد محاضرات", w - pad, y + 70f, p) }
    days.forEach { (d, list) ->
        p.color = 0xFFE5486B.toInt(); p.textSize = 40f; p.typeface = Typeface.DEFAULT_BOLD
        c.drawText(DAY_NAMES[d], w - pad, y + 46f, p)
        y += headH
        list.forEach { s ->
            val r = RectF(pad, y, w - pad, y + rowH - 12f)
            p.color = 0xFF141416.toInt(); c.drawRoundRect(r, 18f, 18f, p)
            p.color = PALETTE[s.color.coerceIn(0, PALETTE.lastIndex)]; c.drawRoundRect(RectF(w - pad - 10f, y, w - pad, y + rowH - 12f), 6f, 6f, p)
            p.typeface = Typeface.DEFAULT_BOLD; p.color = 0xFFEDEDED.toInt(); p.textSize = 36f; p.textAlign = Paint.Align.RIGHT
            c.drawText(s.title, w - pad - 28f, y + 42f, p)
            p.typeface = Typeface.DEFAULT; p.color = 0xFF8E8E96.toInt(); p.textSize = 28f
            val sub = listOf(s.instructor, s.room.takeIf { it.isNotBlank() }?.let { "قاعة $it" }).filterNotNull().filter { it.isNotBlank() }.joinToString(" - ")
            c.drawText(sub.ifEmpty { " " }, w - pad - 28f, y + 82f, p)
            p.textAlign = Paint.Align.LEFT; p.color = 0xFFE5486B.toInt(); p.textSize = 30f; p.typeface = Typeface.DEFAULT_BOLD
            c.drawText(formatRange(s.startMin, s.endMin), pad + 24f, y + 52f, p)
            p.textAlign = Paint.Align.RIGHT
            y += rowH
        }
        y += 16f
    }
    return bmp
}

fun Context.shareBitmap(bmp: Bitmap) {
    val dir = File(cacheDir, "shared").also { it.mkdirs() }
    val f = File(dir, "schedule.png")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(this, "$packageName.files", f)
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "مشاركة الجدول"))
}

/** مشاركة الجدول كملف CSV يستوردونه في التطبيق عند زملائك. */
fun Context.shareCsv(csv: String) {
    val dir = File(cacheDir, "shared").also { it.mkdirs() }
    val f = File(dir, "schedule.csv")
    f.writeText(csv)
    val uri = FileProvider.getUriForFile(this, "$packageName.files", f)
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }, "مشاركة ملف الجدول"))
}
