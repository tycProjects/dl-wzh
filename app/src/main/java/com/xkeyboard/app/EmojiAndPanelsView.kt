package com.xkeyboard.app

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import java.io.File

class EmojiDockView(
    ctx: Context,
    private val onPick: (String) -> Unit,
    private val onBackspace: () -> Unit,
    private val onBackToLetters: () -> Unit
) : LinearLayout(ctx) {

    private val emojis = listOf(
        "😀","😂","🤣","😍","🥰","😎","🤩","🥳","😭","😡","🤔","🙏","🔥","✨","💯","❤️",
        "👍","👏","🎉","🚀","⚡","🌟","💎","🦋","🍀","🍕","☕","🎮","🎧","📱","💻","📸",
        "🌙","☀️","🌈","🫶","🤝","👑","💫","🎯","🏆","🎵","🌸","🌊","🕊️","🛡️","⚙️","🔮"
    )

    private val hiddenSymbols = listOf(
        "✦","✧","★","☆","⚡","∞","≈","≠","±","×","÷","√","∑","∆","Ω","π",
        "→","←","↑","↓","↔","♡","♥","♦","♣","♠","♪","♫","§","¶","†","‡",
        "※","⁂","⌘","⌥","▪","▫","•","◦","℃","℉","№","™","©","®","«","»",
        "◈","◇","❖","⬡","⬢","☀","☾","☯","☮","⚛","⚜","⚔","⚖","✎","✓","✘"
    )

    private var showingHidden = false
    private val grid = GridLayout(ctx).apply { columnCount = 8 }
    private val headerLabel = TextView(ctx).apply {
        textSize = 12f
        setTypeface(null, Typeface.BOLD)
        setPadding(dp(12), dp(6), dp(12), dp(4))
    }
    private val hiddenToggleBtn = Button(ctx).apply {
        isAllCaps = false
        textSize = 12f
    }

    init {
        orientation = VERTICAL
        val t = Themes.resolve(ctx)
        setBackgroundColor(t.bg)
        headerLabel.setTextColor(t.hint)
        addView(headerLabel)

        val scroll = ScrollView(ctx).apply {
            addView(grid, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(176)))

        // Bottom dock: ABC + unique extra Hidden Symbols button + Backspace
        val bottomDock = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(8))
        }

        val abcBtn = Button(ctx).apply {
            text = "ABC"
            isAllCaps = false
            textSize = 12f
            setTextColor(t.text)
            background = pill(t.special, dp(10))
            setOnClickListener { onBackToLetters() }
        }

        hiddenToggleBtn.setOnClickListener {
            showingHidden = !showingHidden
            refresh()
        }

        val delBtn = Button(ctx).apply {
            text = "⌫"
            isAllCaps = false
            textSize = 15f
            setTextColor(t.text)
            background = pill(t.special, dp(10))
            setOnClickListener { onBackspace() }
        }

        bottomDock.addView(abcBtn, LayoutParams(0, dp(38), 1.1f))
        bottomDock.addView(View(ctx), LayoutParams(dp(6), dp(1)))
        bottomDock.addView(hiddenToggleBtn, LayoutParams(0, dp(38), 2.2f))
        bottomDock.addView(View(ctx), LayoutParams(dp(6), dp(1)))
        bottomDock.addView(delBtn, LayoutParams(0, dp(38), 1.0f))
        addView(bottomDock)

        refresh()
    }

    private fun pill(color: Int, radiusDp: Int) = GradientDrawable().apply {
        cornerRadius = dp(radiusDp).toFloat()
        setColor(color)
    }

    fun refresh() {
        val t = Themes.resolve(context)
        setBackgroundColor(t.bg)
        headerLabel.setTextColor(t.hint)
        headerLabel.text = if (showingHidden) "✦ HIDDEN SYMBOLS VAULT" else "EMOJIS"
        hiddenToggleBtn.text = if (showingHidden) "☺ Back to Emojis" else "✦ Hidden Symbols"
        hiddenToggleBtn.setTextColor(if (showingHidden) t.onAccent else t.accent)
        hiddenToggleBtn.background = GradientDrawable().apply {
            cornerRadius = dp(999).toFloat()
            setColor(if (showingHidden) t.accent else t.special)
            setStroke(dp(1), t.accent)
        }

        grid.removeAllViews()
        val list = if (showingHidden) hiddenSymbols else emojis
        for (item in list) {
            val cell = TextView(context).apply {
                text = item
                textSize = 20f
                gravity = Gravity.CENTER
                setTextColor(t.text)
                setOnClickListener { onPick(item) }
            }
            grid.addView(cell, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(42)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

class LiveHeightOverlayView(
    ctx: Context,
    private val onHeightChanged: (Int) -> Unit,
    private val onClose: () -> Unit
) : LinearLayout(ctx) {

    init {
        orientation = VERTICAL
        val t = Themes.resolve(ctx)
        setBackgroundColor(t.bg)
        setPadding(dp(14), dp(10), dp(14), dp(10))

        val cur = Prefs.heightPercent(ctx)
        val label = TextView(ctx).apply {
            text = "Real-Time Keyboard Height: $cur%"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(t.accent)
        }
        val seek = SeekBar(ctx).apply {
            max = 68 // 72% .. 140%
            progress = (cur - 72).coerceIn(0, 68)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val pct = 72 + p
                    label.text = "Real-Time Keyboard Height: $pct%"
                    Prefs.putInt(ctx, "heightPercent", pct)
                    onHeightChanged(pct)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        val done = Button(ctx).apply {
            text = "Done"
            isAllCaps = false
            setTextColor(t.onAccent)
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(t.accent)
            }
            setOnClickListener { onClose() }
        }
        addView(label)
        addView(seek, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(done, LayoutParams(LayoutParams.MATCH_PARENT, dp(38)))
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}

class ClipboardVaultView(
    ctx: Context,
    private val onPasteText: (String) -> Unit,
    private val onClose: () -> Unit
) : LinearLayout(ctx) {

    private val listCol = LinearLayout(ctx).apply { orientation = VERTICAL }

    init {
        orientation = VERTICAL
        val t = Themes.resolve(ctx)
        setBackgroundColor(t.bg)
        setPadding(dp(10), dp(8), dp(10), dp(8))

        val topRow = LinearLayout(ctx).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(ctx).apply {
            text = "Clipboard & Screenshots (Auto-deletes in 24h)"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(t.accent)
        }
        val closeBtn = Button(ctx).apply {
            text = "Back"
            isAllCaps = false
            textSize = 12f
            setOnClickListener { onClose() }
        }
        topRow.addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        topRow.addView(closeBtn, LayoutParams(LayoutParams.WRAP_CONTENT, dp(34)))
        addView(topRow)

        val scroll = ScrollView(ctx).apply { addView(listCol) }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, dp(185)))
        reload()
    }

    fun reload() {
        listCol.removeAllViews()
        val t = Themes.resolve(context)
        val items = ClipboardAndMediaStore.purgeExpired(context)
        if (items.isEmpty()) {
            listCol.addView(TextView(context).apply {
                text = "Nothing saved yet. Copied text & added screenshots appear here and auto-delete after 1 day."
                textSize = 13f
                setTextColor(t.hint)
                setPadding(dp(6), dp(16), dp(6), dp(16))
            })
            return
        }
        for (entry in items) {
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(8), dp(6), dp(8), dp(6))
                background = GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(t.key)
                }
            }
            val minsLeft = (((entry.savedAt + ClipboardAndMediaStore.ONE_DAY_MS) - System.currentTimeMillis()) / 60000L).coerceAtLeast(1)
            val hrsLeft = minsLeft / 60L

            if (entry.kind == "screenshot" && File(entry.content).exists()) {
                val img = ImageView(context).apply {
                    setImageBitmap(BitmapFactory.decodeFile(entry.content))
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
                row.addView(img, LayoutParams(dp(44), dp(44)))
            }

            val info = TextView(context).apply {
                text = if (entry.kind == "screenshot") {
                    "Screenshot saved • expires in ${hrsLeft}h"
                } else {
                    "${entry.content.take(70)} (${hrsLeft}h left)"
                }
                textSize = 13f
                setTextColor(t.text)
                setPadding(dp(8), 0, dp(8), 0)
                setOnClickListener {
                    if (entry.kind == "text") onPasteText(entry.content)
                }
            }
            val del = TextView(context).apply {
                text = "✕"
                textSize = 15f
                setTextColor(t.hint)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener {
                    ClipboardAndMediaStore.deleteEntry(context, entry.id)
                    reload()
                }
            }
            row.addView(info, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            row.addView(del)
            listCol.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(6)
            })
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
