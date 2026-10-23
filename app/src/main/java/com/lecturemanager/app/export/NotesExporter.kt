package com.lecturemanager.app.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.lecturemanager.app.R
import com.lecturemanager.app.data.Lecture
import com.lecturemanager.app.data.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private fun withNotes(lectures: List<Lecture>) =
    lectures.filter { it.notes.isNotBlank() }
        .sortedWith(compareBy({ it.weekNumber }, { it.sortOrder }, { it.id }))

fun buildNotesText(subject: Subject, lectures: List<Lecture>): String {
    val sb = StringBuilder("${subject.title}\n")
    if (subject.code.isNotBlank()) sb.append(subject.code).append('\n')
    sb.append("\n")
    var week = -1
    for (l in withNotes(lectures)) {
        if (l.weekNumber != week) { week = l.weekNumber; sb.append("— الأسبوع $week —\n\n") }
        sb.append("▪ ${l.title}")
        if (l.tag.isNotBlank()) sb.append("  [${l.tag}]")
        sb.append("\n${l.notes.trim()}\n\n")
    }
    return sb.toString().trim()
}

/** يصدّر ملاحظات مادة إلى PDF (A4) بخط Tajawal ويعيد الملف في كاش التطبيق. */
suspend fun exportNotesPdf(ctx: Context, subject: Subject, lectures: List<Lecture>): File = withContext(Dispatchers.IO) {
    val pageW = 595; val pageH = 842; val margin = 40
    val contentW = pageW - margin * 2
    val regular = ResourcesCompat.getFont(ctx, R.font.tajawal_regular)
    val bold = ResourcesCompat.getFont(ctx, R.font.tajawal_bold)
    fun paint(size: Float, color: Int, bolded: Boolean) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size; this.color = color; typeface = if (bolded) bold else regular
    }
    val title = paint(22f, 0xFF1A2542.toInt(), true)
    val week = paint(16f, 0xFF0D9488.toInt(), true)
    val head = paint(14f, 0xFF1A2542.toInt(), true)
    val tag = paint(11f, 0xFFB45309.toInt(), false)
    val body = paint(13f, 0xFF0B1C30.toInt(), false)

    val doc = PdfDocument()
    var pageNo = 0
    var page: PdfDocument.Page? = null
    var y = margin.toFloat()
    fun newPage() {
        page?.let { doc.finishPage(it) }
        pageNo++
        page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
        y = margin.toFloat()
    }
    newPage()

    fun block(text: String, p: TextPaint, spaceAfter: Float) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, p, contentW)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setLineSpacing(4f, 1f).build()
        var line = 0
        while (line < layout.lineCount) {
            val avail = pageH - margin - y
            var end = line
            while (end < layout.lineCount && layout.getLineBottom(end) - layout.getLineTop(line) <= avail) end++
            if (end == line) {
                if (y > margin) { newPage(); continue }
                end = line + 1 // سطر أطول من الصفحة: نرسمه رغم ذلك لتفادي حلقة لا نهائية
            }
            val top = layout.getLineTop(line); val bottom = layout.getLineBottom(end - 1)
            val c = page!!.canvas
            c.save()
            c.translate(margin.toFloat(), y - top)
            c.clipRect(0f, top.toFloat(), contentW.toFloat(), bottom.toFloat())
            layout.draw(c)
            c.restore()
            y += (bottom - top)
            line = end
            if (line < layout.lineCount) newPage()
        }
        y += spaceAfter
    }

    block(subject.title + if (subject.code.isNotBlank()) "  •  ${subject.code}" else "", title, 14f)
    val list = withNotes(lectures)
    if (list.isEmpty()) block("لا توجد ملاحظات لهذه المادة بعد.", body, 0f)
    var w = -1
    for (l in list) {
        if (l.weekNumber != w) { w = l.weekNumber; block("الأسبوع $w", week, 6f) }
        block(l.title, head, 2f)
        if (l.tag.isNotBlank()) block(l.tag, tag, 2f)
        block(l.notes.trim(), body, 16f)
    }
    page?.let { doc.finishPage(it) }

    val dir = File(ctx.cacheDir, "exports").also { it.mkdirs() }
    val safe = subject.title.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').ifEmpty { "notes" }
    val out = File(dir, "ملاحظات_$safe.pdf")
    out.outputStream().use { doc.writeTo(it) }
    doc.close()
    out
}
