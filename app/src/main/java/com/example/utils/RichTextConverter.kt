package com.example.utils

import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
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
import kotlin.math.max
import kotlin.math.min

object RichTextConverter {

    const val CHECKBOX_UNCHECKED = "☐ "
    const val CHECKBOX_CHECKED = "☑ "

    /**
     * Converts a raw stored string (Markdown or HTML) into an interactive Spannable
     * with visual styling applied and syntax markers hidden.
     */
    fun fromFormattedString(
        rawContent: String,
        textColor: Int = Color.parseColor("#F1F1F1"),
        accentColor: Int = Color.parseColor("#B7B9BE"),
        mutedColor: Int = Color.parseColor("#92959B"),
        codeBgColor: Int = Color.parseColor("#26282C")
    ): SpannableStringBuilder {
        val ssb = SpannableStringBuilder()
        if (rawContent.isBlank()) return ssb

        val lines = rawContent.lines()
        for (lineIdx in lines.indices) {
            val line = lines[lineIdx]
            val lineStart = ssb.length

            val trimmed = line.trimStart()
            when {
                // H1 Heading
                trimmed.startsWith("# ") -> {
                    val text = trimmed.removePrefix("# ")
                    ssb.append(text)
                    val lineEnd = ssb.length
                    ssb.setSpan(RelativeSizeSpan(1.45f), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                // H2 Heading
                trimmed.startsWith("## ") -> {
                    val text = trimmed.removePrefix("## ")
                    ssb.append(text)
                    val lineEnd = ssb.length
                    ssb.setSpan(RelativeSizeSpan(1.28f), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                // H3 Heading
                trimmed.startsWith("### ") -> {
                    val text = trimmed.removePrefix("### ")
                    ssb.append(text)
                    val lineEnd = ssb.length
                    ssb.setSpan(RelativeSizeSpan(1.15f), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.BOLD), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                // Checkbox unchecked
                trimmed.startsWith("[ ] ") || trimmed.startsWith("☐ ") -> {
                    val text = if (trimmed.startsWith("[ ] ")) trimmed.removePrefix("[ ] ") else trimmed.removePrefix("☐ ")
                    ssb.append(CHECKBOX_UNCHECKED).append(text)
                }
                // Checkbox checked
                trimmed.startsWith("[x] ") || trimmed.startsWith("[X] ") || trimmed.startsWith("☑ ") -> {
                    val text = when {
                        trimmed.startsWith("[x] ") -> trimmed.removePrefix("[x] ")
                        trimmed.startsWith("[X] ") -> trimmed.removePrefix("[X] ")
                        else -> trimmed.removePrefix("☑ ")
                    }
                    val itemStart = ssb.length
                    ssb.append(CHECKBOX_CHECKED).append(text)
                    val itemEnd = ssb.length
                    ssb.setSpan(StrikethroughSpan(), itemStart, itemEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(ForegroundColorSpan(mutedColor), itemStart, itemEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                // Blockquote
                trimmed.startsWith("> ") -> {
                    val text = trimmed.removePrefix("> ")
                    ssb.append(text)
                    val lineEnd = ssb.length
                    ssb.setSpan(QuoteSpan(accentColor), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(StyleSpan(Typeface.ITALIC), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(ForegroundColorSpan(mutedColor), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                // Bullet list item
                trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val text = when {
                        trimmed.startsWith("• ") -> trimmed.removePrefix("• ")
                        trimmed.startsWith("- ") -> trimmed.removePrefix("- ")
                        else -> trimmed.removePrefix("* ")
                    }
                    ssb.append(text)
                    val lineEnd = ssb.length
                    ssb.setSpan(BulletSpan(24, accentColor), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                // Numbered list item: e.g. "1. Task"
                trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                    val prefix = trimmed.takeWhile { it != ' ' } + " "
                    val text = trimmed.removePrefix(prefix)
                    ssb.append(prefix).append(text)
                }
                // Code fence
                trimmed.startsWith("```") -> {
                    val text = trimmed.removePrefix("```").trim()
                    if (text.isNotEmpty()) {
                        ssb.append(text)
                        val lineEnd = ssb.length
                        ssb.setSpan(TypefaceSpan("monospace"), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        ssb.setSpan(BackgroundColorSpan(codeBgColor), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
                // Divider
                trimmed == "---" || trimmed == "***" -> {
                    ssb.append("――――――――――――――――――――")
                    val lineEnd = ssb.length
                    ssb.setSpan(ForegroundColorSpan(mutedColor), lineStart, lineEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                // Regular line
                else -> {
                    parseInlineMarkdown(line, ssb, accentColor, codeBgColor)
                }
            }

            // Append newline between lines except after the last line
            if (lineIdx < lines.size - 1) {
                ssb.append("\n")
            }
        }

        return ssb
    }

    private fun parseInlineMarkdown(
        line: String,
        ssb: SpannableStringBuilder,
        accentColor: Int,
        codeBgColor: Int
    ) {
        var cursor = 0
        while (cursor < line.length) {
            when {
                // Bold: **text**
                line.startsWith("**", cursor) -> {
                    val endToken = line.indexOf("**", cursor + 2)
                    if (endToken != -1) {
                        val text = line.substring(cursor + 2, endToken)
                        val start = ssb.length
                        ssb.append(text)
                        val end = ssb.length
                        ssb.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        cursor = endToken + 2
                    } else {
                        ssb.append(line[cursor])
                        cursor++
                    }
                }
                // Strikethrough: ~~text~~
                line.startsWith("~~", cursor) -> {
                    val endToken = line.indexOf("~~", cursor + 2)
                    if (endToken != -1) {
                        val text = line.substring(cursor + 2, endToken)
                        val start = ssb.length
                        ssb.append(text)
                        val end = ssb.length
                        ssb.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        cursor = endToken + 2
                    } else {
                        ssb.append(line[cursor])
                        cursor++
                    }
                }
                // Inline Code: `code`
                line[cursor] == '`' -> {
                    val endToken = line.indexOf('`', cursor + 1)
                    if (endToken != -1) {
                        val text = line.substring(cursor + 1, endToken)
                        val start = ssb.length
                        ssb.append(text)
                        val end = ssb.length
                        ssb.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        ssb.setSpan(BackgroundColorSpan(codeBgColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        cursor = endToken + 1
                    } else {
                        ssb.append(line[cursor])
                        cursor++
                    }
                }
                // Internal link: [[Note Title]]
                line.startsWith("[[", cursor) -> {
                    val endToken = line.indexOf("]]", cursor + 2)
                    if (endToken != -1) {
                        val target = line.substring(cursor + 2, endToken)
                        val start = ssb.length
                        ssb.append(target)
                        val end = ssb.length
                        ssb.setSpan(URLSpan("note://$target"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        ssb.setSpan(ForegroundColorSpan(accentColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        ssb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        cursor = endToken + 2
                    } else {
                        ssb.append(line[cursor])
                        cursor++
                    }
                }
                // Standard markdown link: [label](url)
                line[cursor] == '[' && line.indexOf(']', cursor) != -1 && line.indexOf('(', cursor) == line.indexOf(']', cursor) + 1 -> {
                    val closeBracket = line.indexOf(']', cursor)
                    val closeParen = line.indexOf(')', closeBracket)
                    if (closeParen != -1) {
                        val label = line.substring(cursor + 1, closeBracket)
                        val url = line.substring(closeBracket + 2, closeParen)
                        val start = ssb.length
                        ssb.append(label)
                        val end = ssb.length
                        ssb.setSpan(URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        ssb.setSpan(ForegroundColorSpan(accentColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        ssb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        cursor = closeParen + 1
                    } else {
                        ssb.append(line[cursor])
                        cursor++
                    }
                }
                // Italic: *text* (when not preceded or followed by *)
                line[cursor] == '*' && (cursor == 0 || line[cursor - 1] != '*') -> {
                    val endToken = line.indexOf('*', cursor + 1)
                    if (endToken != -1 && (endToken == line.length - 1 || line[endToken + 1] != '*')) {
                        val text = line.substring(cursor + 1, endToken)
                        val start = ssb.length
                        ssb.append(text)
                        val end = ssb.length
                        ssb.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        cursor = endToken + 1
                    } else {
                        ssb.append(line[cursor])
                        cursor++
                    }
                }
                else -> {
                    ssb.append(line[cursor])
                    cursor++
                }
            }
        }
    }

    /**
     * Converts the edited Spannable back to a clean, durable Markdown/HTML string
     * for persistence in the SQLite database.
     */
    fun toFormattedString(spanned: Spanned): String {
        val fullText = spanned.toString()
        if (fullText.isEmpty()) return ""

        val lines = fullText.split('\n')
        val sb = StringBuilder()
        var currentOffset = 0

        for (lineIdx in lines.indices) {
            val line = lines[lineIdx]
            val lineStart = currentOffset
            val lineEnd = lineStart + line.length

            // Check line-level spans
            val sizeSpans = spanned.getSpans(lineStart, lineEnd, RelativeSizeSpan::class.java)
            val quoteSpans = spanned.getSpans(lineStart, lineEnd, QuoteSpan::class.java)
            val bulletSpans = spanned.getSpans(lineStart, lineEnd, BulletSpan::class.java)

            val maxRelativeSize = sizeSpans.maxOfOrNull { it.sizeChange } ?: 1.0f
            val isQuote = quoteSpans.isNotEmpty()
            val isBullet = bulletSpans.isNotEmpty()

            val linePrefix = when {
                maxRelativeSize >= 1.4f -> "# "
                maxRelativeSize >= 1.25f -> "## "
                maxRelativeSize > 1.05f -> "### "
                isQuote -> "> "
                isBullet -> "- "
                line.startsWith(CHECKBOX_UNCHECKED) -> "[ ] "
                line.startsWith(CHECKBOX_CHECKED) -> "[x] "
                else -> ""
            }

            val cleanLine = when {
                line.startsWith(CHECKBOX_UNCHECKED) -> line.removePrefix(CHECKBOX_UNCHECKED)
                line.startsWith(CHECKBOX_CHECKED) -> line.removePrefix(CHECKBOX_CHECKED)
                line == "――――――――――――――――――――" -> "---"
                else -> line
            }

            if (linePrefix == "# " || linePrefix == "## " || linePrefix == "### " || linePrefix == "> " || linePrefix == "- ") {
                sb.append(linePrefix).append(cleanLine)
            } else if (linePrefix == "[ ] " || linePrefix == "[x] ") {
                sb.append(linePrefix).append(cleanLine)
            } else {
                // Serialize inline spans within the line
                sb.append(serializeInlineSpans(spanned, lineStart, lineEnd))
            }

            if (lineIdx < lines.size - 1) {
                sb.append("\n")
            }
            currentOffset = lineEnd + 1 // +1 for the newline
        }

        return sb.toString()
    }

    private fun serializeInlineSpans(spanned: Spanned, lineStart: Int, lineEnd: Int): String {
        if (lineStart >= lineEnd) return ""

        val lineText = spanned.subSequence(lineStart, lineEnd).toString()
        if (lineText.isEmpty()) return ""

        // Collect inline ranges for bold, italic, strikethrough, links
        val styles = spanned.getSpans(lineStart, lineEnd, StyleSpan::class.java)
        val strikes = spanned.getSpans(lineStart, lineEnd, StrikethroughSpan::class.java)
        val urls = spanned.getSpans(lineStart, lineEnd, URLSpan::class.java)
        val codes = spanned.getSpans(lineStart, lineEnd, TypefaceSpan::class.java).filter { it.family == "monospace" }

        // If no inline spans, return raw line text
        if (styles.isEmpty() && strikes.isEmpty() && urls.isEmpty() && codes.isEmpty()) {
            return lineText
        }

        val sb = StringBuilder()
        var charIdx = lineStart

        while (charIdx < lineEnd) {
            val localChar = spanned[charIdx]

            // Check if bold begins here
            val activeBold = styles.filter { it.style == Typeface.BOLD }
            val boldStarts = activeBold.any { spanned.getSpanStart(it) == charIdx }
            val boldEnds = activeBold.any { spanned.getSpanEnd(it) == charIdx }

            val activeItalic = styles.filter { it.style == Typeface.ITALIC }
            val italicStarts = activeItalic.any { spanned.getSpanStart(it) == charIdx }
            val italicEnds = activeItalic.any { spanned.getSpanEnd(it) == charIdx }

            val strikeStarts = strikes.any { spanned.getSpanStart(it) == charIdx }
            val strikeEnds = strikes.any { spanned.getSpanEnd(it) == charIdx }

            val codeStarts = codes.any { spanned.getSpanStart(it) == charIdx }
            val codeEnds = codes.any { spanned.getSpanEnd(it) == charIdx }

            val activeUrl = urls.firstOrNull { spanned.getSpanStart(it) <= charIdx && charIdx < spanned.getSpanEnd(it) }
            val urlStarts = activeUrl != null && spanned.getSpanStart(activeUrl) == charIdx

            if (urlStarts && activeUrl != null) {
                val uStart = max(lineStart, spanned.getSpanStart(activeUrl))
                val uEnd = min(lineEnd, spanned.getSpanEnd(activeUrl))
                val uText = spanned.subSequence(uStart, uEnd).toString()
                val url = activeUrl.url ?: ""
                if (url.startsWith("note://")) {
                    sb.append("[[").append(url.removePrefix("note://")).append("]]")
                } else {
                    sb.append("[").append(uText).append("](").append(url).append(")")
                }
                charIdx = uEnd
                continue
            }

            if (boldStarts) sb.append("**")
            if (italicStarts) sb.append("*")
            if (strikeStarts) sb.append("~~")
            if (codeStarts) sb.append("`")

            sb.append(localChar)

            if (codeEnds) sb.append("`")
            if (strikeEnds) sb.append("~~")
            if (italicEnds) sb.append("*")
            if (boldEnds) sb.append("**")

            charIdx++
        }

        return sb.toString()
    }
}
