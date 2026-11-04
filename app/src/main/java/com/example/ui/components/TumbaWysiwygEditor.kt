package com.example.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.LocalTumbaColors
import com.example.utils.RichTextConverter
import kotlin.math.max
import kotlin.math.min

class TumbaWysiwygState {
    var editText: EditText? = null
    var onContentChange: ((String) -> Unit)? = null
    private var isUpdatingInternally = false

    // Undo / Redo stacks
    val undoStack = mutableListOf<CharSequence>()
    val redoStack = mutableListOf<CharSequence>()

    fun setContent(rawContent: String, textColor: Int, accentColor: Int, mutedColor: Int, codeBgColor: Int) {
        val et = editText ?: return
        isUpdatingInternally = true
        val styled = RichTextConverter.fromFormattedString(
            rawContent = rawContent,
            textColor = textColor,
            accentColor = accentColor,
            mutedColor = mutedColor,
            codeBgColor = codeBgColor
        )
        et.setText(styled)
        et.setSelection(et.text?.length ?: 0)
        isUpdatingInternally = false
    }

    fun getContent(): String {
        val et = editText ?: return ""
        val text = et.text ?: return ""
        return RichTextConverter.toFormattedString(text)
    }

    private fun snapshotForUndo() {
        val et = editText ?: return
        val current = et.text?.toString() ?: return
        if (undoStack.isEmpty() || undoStack.last() != current) {
            undoStack.add(SpannableStringBuilder(et.text))
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }
    }

    fun undo() {
        val et = editText ?: return
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(SpannableStringBuilder(et.text))
            isUpdatingInternally = true
            et.setText(prev)
            et.setSelection(et.text?.length ?: 0)
            isUpdatingInternally = false
            onContentChange?.invoke(getContent())
        }
    }

    fun redo() {
        val et = editText ?: return
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(SpannableStringBuilder(et.text))
            isUpdatingInternally = true
            et.setText(next)
            et.setSelection(et.text?.length ?: 0)
            isUpdatingInternally = false
            onContentChange?.invoke(getContent())
        }
    }

    fun toggleBold() {
        val et = editText ?: return
        val text = et.text ?: return
        val start = min(et.selectionStart, et.selectionEnd)
        val end = max(et.selectionStart, et.selectionEnd)
        if (start == end) return

        snapshotForUndo()
        val existing = text.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
        if (existing.isNotEmpty()) {
            existing.forEach { text.removeSpan(it) }
        } else {
            text.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        et.setSelection(start, end)
        onContentChange?.invoke(getContent())
    }

    fun toggleItalic() {
        val et = editText ?: return
        val text = et.text ?: return
        val start = min(et.selectionStart, et.selectionEnd)
        val end = max(et.selectionStart, et.selectionEnd)
        if (start == end) return

        snapshotForUndo()
        val existing = text.getSpans(start, end, StyleSpan::class.java).filter { it.style == Typeface.ITALIC }
        if (existing.isNotEmpty()) {
            existing.forEach { text.removeSpan(it) }
        } else {
            text.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        et.setSelection(start, end)
        onContentChange?.invoke(getContent())
    }

    fun toggleStrikethrough() {
        val et = editText ?: return
        val text = et.text ?: return
        val start = min(et.selectionStart, et.selectionEnd)
        val end = max(et.selectionStart, et.selectionEnd)
        if (start == end) return

        snapshotForUndo()
        val existing = text.getSpans(start, end, StrikethroughSpan::class.java)
        if (existing.isNotEmpty()) {
            existing.forEach { text.removeSpan(it) }
        } else {
            text.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        }
        et.setSelection(start, end)
        onContentChange?.invoke(getContent())
    }

    fun applyHeading(level: Int) {
        val et = editText ?: return
        val text = et.text ?: return
        val cursor = et.selectionStart
        val fullString = text.toString()
        val lineStart = fullString.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = fullString.indexOf('\n', cursor).let { if (it == -1) fullString.length else it }

        snapshotForUndo()
        // Remove existing sizes & heading bold on this line
        val oldSizes = text.getSpans(lineStart, lineEnd, RelativeSizeSpan::class.java)
        oldSizes.forEach { text.removeSpan(it) }

        when (level) {
            1 -> {
                text.setSpan(RelativeSizeSpan(1.45f), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text.setSpan(StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            2 -> {
                text.setSpan(RelativeSizeSpan(1.28f), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text.setSpan(StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            0 -> {
                // Clear headings
                val bolds = text.getSpans(lineStart, lineEnd, StyleSpan::class.java).filter { it.style == Typeface.BOLD }
                bolds.forEach { text.removeSpan(it) }
            }
        }

        et.setSelection(cursor.coerceIn(lineStart, lineEnd))
        onContentChange?.invoke(getContent())
    }

    fun toggleChecklist(mutedColor: Int) {
        val et = editText ?: return
        val text = et.text ?: return
        val cursor = et.selectionStart
        val fullString = text.toString()
        val lineStart = fullString.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }

        snapshotForUndo()
        val lineText = fullString.substring(lineStart)
        when {
            lineText.startsWith(RichTextConverter.CHECKBOX_UNCHECKED) -> {
                // Toggle to checked
                text.replace(lineStart, lineStart + RichTextConverter.CHECKBOX_UNCHECKED.length, RichTextConverter.CHECKBOX_CHECKED)
                val lineEnd = text.toString().indexOf('\n', lineStart).let { if (it == -1) text.length else it }
                text.setSpan(StrikethroughSpan(), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                text.setSpan(ForegroundColorSpan(mutedColor), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            lineText.startsWith(RichTextConverter.CHECKBOX_CHECKED) -> {
                // Toggle to unchecked
                text.replace(lineStart, lineStart + RichTextConverter.CHECKBOX_CHECKED.length, RichTextConverter.CHECKBOX_UNCHECKED)
                val lineEnd = text.toString().indexOf('\n', lineStart).let { if (it == -1) text.length else it }
                text.getSpans(lineStart, lineEnd, StrikethroughSpan::class.java).forEach { text.removeSpan(it) }
                text.getSpans(lineStart, lineEnd, ForegroundColorSpan::class.java).forEach { text.removeSpan(it) }
            }
            else -> {
                // Insert new unchecked checkbox at line start
                text.insert(lineStart, RichTextConverter.CHECKBOX_UNCHECKED)
            }
        }
        et.setSelection(min(cursor + RichTextConverter.CHECKBOX_UNCHECKED.length, text.length))
        onContentChange?.invoke(getContent())
    }

    fun insertBulletList(accentColor: Int) {
        val et = editText ?: return
        val text = et.text ?: return
        val cursor = et.selectionStart
        val fullString = text.toString()
        val lineStart = fullString.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }

        snapshotForUndo()
        text.insert(lineStart, "• ")
        et.setSelection(min(cursor + 2, text.length))
        onContentChange?.invoke(getContent())
    }

    fun insertNumberedList() {
        val et = editText ?: return
        val text = et.text ?: return
        val cursor = et.selectionStart
        val fullString = text.toString()
        val lineStart = fullString.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }

        snapshotForUndo()
        text.insert(lineStart, "1. ")
        et.setSelection(min(cursor + 3, text.length))
        onContentChange?.invoke(getContent())
    }

    fun insertQuote(accentColor: Int) {
        val et = editText ?: return
        val text = et.text ?: return
        val cursor = et.selectionStart
        val fullString = text.toString()
        val lineStart = fullString.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = fullString.indexOf('\n', cursor).let { if (it == -1) fullString.length else it }

        snapshotForUndo()
        val existing = text.getSpans(lineStart, lineEnd, QuoteSpan::class.java)
        if (existing.isNotEmpty()) {
            existing.forEach { text.removeSpan(it) }
        } else {
            text.setSpan(QuoteSpan(accentColor), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(StyleSpan(Typeface.ITALIC), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        et.setSelection(cursor)
        onContentChange?.invoke(getContent())
    }

    fun insertCodeBlock(codeBgColor: Int) {
        val et = editText ?: return
        val text = et.text ?: return
        val start = min(et.selectionStart, et.selectionEnd)
        val end = max(et.selectionStart, et.selectionEnd)

        snapshotForUndo()
        if (start == end) {
            val insert = "code"
            text.insert(start, insert)
            text.setSpan(TypefaceSpan("monospace"), start, start + insert.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(BackgroundColorSpan(codeBgColor), start, start + insert.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            et.setSelection(start + insert.length)
        } else {
            text.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(BackgroundColorSpan(codeBgColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            et.setSelection(end)
        }
        onContentChange?.invoke(getContent())
    }

    fun insertDivider() {
        val et = editText ?: return
        val text = et.text ?: return
        val cursor = et.selectionStart
        snapshotForUndo()
        text.insert(cursor, "\n――――――――――――――――――――\n")
        et.setSelection(min(cursor + 22, text.length))
        onContentChange?.invoke(getContent())
    }

    fun insertLink(title: String, url: String, accentColor: Int) {
        val et = editText ?: return
        val text = et.text ?: return
        val start = min(et.selectionStart, et.selectionEnd)
        val end = max(et.selectionStart, et.selectionEnd)

        snapshotForUndo()
        if (start == end) {
            text.insert(start, title)
            val linkEnd = start + title.length
            text.setSpan(URLSpan(url), start, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(ForegroundColorSpan(accentColor), start, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(UnderlineSpan(), start, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            et.setSelection(linkEnd)
        } else {
            text.setSpan(URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(ForegroundColorSpan(accentColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            et.setSelection(end)
        }
        onContentChange?.invoke(getContent())
    }
}

@Composable
fun rememberTumbaWysiwygState(): TumbaWysiwygState {
    return remember { TumbaWysiwygState() }
}

@Composable
fun TumbaWysiwygEditor(
    state: TumbaWysiwygState,
    initialContent: String,
    fontSize: Float = 16f,
    fontFamilyStyle: String = "sans",
    placeholderText: String = "",
    onContentChanged: (String) -> Unit,
    onLinkTapped: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current

    val primaryTextColorInt = remember(colors.textPrimary) {
        val c = colors.textPrimary
        Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }
    val accentColorInt = remember(colors.accent) {
        val c = colors.accent
        Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }
    val mutedColorInt = remember(colors.textMuted) {
        val c = colors.textMuted
        Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
    }
    val codeBgColorInt = Color.parseColor("#26282C")

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            TumbaRichEditText(ctx).apply {
                background = null // Transparent background
                setTextColor(primaryTextColorInt)
                setHintTextColor(mutedColorInt)
                hint = placeholderText
                textSize = fontSize
                gravity = Gravity.TOP or Gravity.START
                imeOptions = EditorInfo.IME_ACTION_NONE
                isVerticalScrollBarEnabled = true
                setLineSpacing(0f, 1.4f)

                typeface = when (fontFamilyStyle) {
                    "serif" -> Typeface.SERIF
                    "mono" -> Typeface.MONOSPACE
                    else -> Typeface.SANS_SERIF
                }

                // Checkbox tap detection
                setOnCheckboxTapListener { tappedLineStart ->
                    state.toggleChecklist(mutedColorInt)
                }

                // URL tap detection
                onLinkClickListener = { url ->
                    if (url.startsWith("note://")) {
                        val targetTitle = url.removePrefix("note://")
                        onLinkTapped?.invoke(targetTitle)
                    }
                }

                state.editText = this
                state.onContentChange = onContentChanged

                // Load initial formatted content
                state.setContent(
                    rawContent = initialContent,
                    textColor = primaryTextColorInt,
                    accentColor = accentColorInt,
                    mutedColor = mutedColorInt,
                    codeBgColor = codeBgColorInt
                )

                // Text change watcher
                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        state.onContentChange?.invoke(state.getContent())
                    }
                })
            }
        },
        update = { editText ->
            editText.setTextColor(primaryTextColorInt)
            editText.setHintTextColor(mutedColorInt)
            editText.textSize = fontSize
            editText.typeface = when (fontFamilyStyle) {
                "serif" -> Typeface.SERIF
                "mono" -> Typeface.MONOSPACE
                else -> Typeface.SANS_SERIF
            }
        }
    )
}

class TumbaRichEditText(context: Context) : EditText(context) {

    private var onCheckboxTap: ((Int) -> Unit)? = null
    var onLinkClickListener: ((String) -> Unit)? = null

    fun setOnCheckboxTapListener(listener: (Int) -> Unit) {
        onCheckboxTap = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val x = event.x.toInt() - totalPaddingLeft + scrollX
            val y = event.y.toInt() - totalPaddingTop + scrollY

            val line = layout?.getLineForVertical(y) ?: -1
            if (line != -1 && layout != null) {
                val off = layout?.getOffsetForHorizontal(line, x.toFloat()) ?: -1
                val text = text
                if (text != null && off in 0..text.length) {
                    // Check if tapped on a link
                    val urls = text.getSpans(off, off, URLSpan::class.java)
                    if (urls.isNotEmpty()) {
                        val url = urls[0].url
                        if (url != null) {
                            onLinkClickListener?.invoke(url)
                            return true
                        }
                    }

                    // Check if tapped on checkbox at start of line
                    val lineStart = layout?.getLineStart(line) ?: 0
                    if (off in lineStart..(lineStart + 3)) {
                        val full = text.toString()
                        if (full.startsWith(RichTextConverter.CHECKBOX_UNCHECKED, lineStart) ||
                            full.startsWith(RichTextConverter.CHECKBOX_CHECKED, lineStart)
                        ) {
                            onCheckboxTap?.invoke(lineStart)
                            return true
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
