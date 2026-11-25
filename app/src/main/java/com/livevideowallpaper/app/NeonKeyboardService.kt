package com.livevideowallpaper.app

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView

/** A lightweight, real Android IME. Text is committed only to the focused editor. */
class NeonKeyboardService : InputMethodService() {
    private var shifted = false
    private var symbols = false
    private val rows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("?123","☻",",","English"," . ","↵")
    )

    override fun onCreateInputView(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(8), dp(6), dp(10))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(17, 24, 45), Color.rgb(31, 22, 57))
            ).apply { cornerRadii = FloatArray(8) { dp(18).toFloat() } }
        }
        rows.forEachIndexed { index, row ->
            val line = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            row.forEach { label ->
                val key = TextView(this).apply {
                    text = if (shifted && label.length == 1) label.uppercase() else label
                    gravity = Gravity.CENTER
                    textSize = if (label.length > 2) 12f else 18f
                    setTextColor(Color.WHITE)
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                    setPadding(dp(2), 0, dp(2), 0)
                    background = GradientDrawable().apply {
                        setColor(if (label in listOf("⇧","⌫","?123","↵")) 0x665b6b91 else 0x553c4968)
                        setStroke(dp(1), 0x66d8e5ff)
                        cornerRadius = dp(9).toFloat()
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { handleKey(label) }
                }
                val weight = when (label) {
                    "English" -> 4.2f
                    " . " -> 1.3f
                    "⇧", "⌫", "?123", "↵" -> 1.35f
                    else -> 1f
                }
                line.addView(key, LinearLayout.LayoutParams(0, dp(if (index == 3) 46 else 48), weight).apply {
                    setMargins(dp(2), dp(3), dp(2), dp(3))
                })
            }
            panel.addView(line)
        }
        val footer = TextView(this).apply {
            text = "✦ NEONKEY  •  VIDEO WALLPAPER READY"
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(0xffaab8d8.toInt())
            setPadding(0, dp(3), 0, 0)
        }
        panel.addView(footer)
        return panel
    }

    private fun handleKey(key: String) {
        val ic = currentInputConnection ?: return
        when (key) {
            "⇧" -> { shifted = !shifted; setInputView(onCreateInputView()) }
            "⌫" -> ic.deleteSurroundingText(1, 0)
            "?123" -> { symbols = !symbols; shifted = false; setInputView(onCreateInputView()) }
            "☻" -> ic.commitText("😊", 1)
            "English" -> ic.commitText(" ", 1)
            "↵" -> {
                val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (action != null && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED)
                    ic.performEditorAction(action)
                else ic.commitText("\\n", 1)
            }
            else -> {
                val value = when {
                    key == " . " -> "."
                    symbols -> mapOf("q" to "1","w" to "2","e" to "3","r" to "4","t" to "5","y" to "6","u" to "7","i" to "8","o" to "9","p" to "0")[key] ?: key
                    shifted -> key.uppercase()
                    else -> key
                }
                ic.commitText(value, 1)
                if (shifted) { shifted = false; setInputView(onCreateInputView()) }
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
