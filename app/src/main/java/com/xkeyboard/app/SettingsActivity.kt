package com.xkeyboard.app

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : Activity() {

    companion object {
        private const val REQ_WALLPAPER = 101
        private const val REQ_SOUND = 102
        private const val REQ_SCREENSHOT = 103
        private const val REQ_AUDIO_PERM = 104
    }

    private lateinit var status: TextView
    private lateinit var livePreview: XKeyboardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ClipboardAndMediaStore.purgeExpired(this)
        buildUi()
    }

    private fun buildUi() {
        val t = Themes.resolve(this)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(32), dp(20), dp(40))
        }
        val sv = ScrollView(this).apply {
            setBackgroundColor(t.bg)
            isFillViewport = true
            addView(col)
        }

        fun tv(s: String, size: Float, color: Int = t.text, bold: Boolean = false) = TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(5))
        }

        fun btn(s: String, accentStyle: Boolean = false, f: () -> Unit) = Button(this).apply {
            text = s
            isAllCaps = false
            setTextColor(if (accentStyle) t.onAccent else t.text)
            background = GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(if (accentStyle) t.accent else t.key)
            }
            setOnClickListener { f() }
        }

        fun header(s: String) = tv(s, 13f, t.accent, true).apply { setPadding(0, dp(22), 0, dp(6)) }

        fun radios(options: List<String>, cur: Int, horizontal: Boolean, pick: (Int) -> Unit) {
            val g = RadioGroup(this).apply {
                orientation = if (horizontal) RadioGroup.HORIZONTAL else RadioGroup.VERTICAL
            }
            options.forEachIndexed { i, s ->
                g.addView(RadioButton(this).apply {
                    text = s
                    setTextColor(t.text)
                    id = View.generateViewId()
                    isChecked = i == cur
                    setOnClickListener { pick(i) }
                })
            }
            col.addView(g)
        }

        fun sw(s: String, key: String, def: Boolean, onChange: ((Boolean) -> Unit)? = null) = Switch(this).apply {
            text = s
            setTextColor(t.text)
            isChecked = Prefs.bool(this@SettingsActivity, key, def)
            setOnCheckedChangeListener { _, v ->
                Prefs.putBool(this@SettingsActivity, key, v)
                livePreview.applyPrefs()
                onChange?.invoke(v)
            }
            setPadding(0, dp(8), 0, dp(8))
        }

        col.addView(tv("X Keyboard", 32f, t.accent, true))
        col.addView(tv("Fast • Liquid Glass • English & বাংলা", 15f, t.hint))
        status = tv("", 14f, t.text).apply { setPadding(0, dp(12), 0, dp(8)) }
        col.addView(status)

        col.addView(btn("1. Enable X Keyboard", true) { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) })
        col.addView(View(this), LinearLayout.LayoutParams(1, dp(8)))
        col.addView(btn("2. Switch to X Keyboard") {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        })

        // LIVE KEYBOARD PREVIEW + REAL-TIME HEIGHT
        col.addView(header("REAL-TIME KEYBOARD HEIGHT & LIVE PREVIEW"))
        val heightLabel = tv("Keyboard Height: ${Prefs.heightPercent(this)}%", 14f, t.text, true)
        col.addView(heightLabel)

        livePreview = XKeyboardView(this).apply {
            listener = object : XKeyboardView.Listener {
                override fun onText(text: String) {}
                override fun onSpace() {}
                override fun onDelete() {}
                override fun onEnter() {}
                override fun onCursor(dir: Int) {}
                override fun onToggleLanguage() {
                    val next = if (Prefs.lang(this@SettingsActivity) == "bn") "en" else "bn"
                    Prefs.putStr(this@SettingsActivity, "lang", next)
                    livePreview.resetToMainLanguageMode()
                }
                override fun onOpenEmoji() {
                    Toast.makeText(this@SettingsActivity, "Emoji + Bottom Hidden Symbols opens on keyboard", Toast.LENGTH_SHORT).show()
                }
            }
        }

        col.addView(SeekBar(this).apply {
            max = 68 // 72% .. 140%
            progress = (Prefs.heightPercent(this@SettingsActivity) - 72).coerceIn(0, 68)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val pct = 72 + p
                    heightLabel.text = "Keyboard Height: $pct%"
                    Prefs.putInt(this@SettingsActivity, "heightPercent", pct)
                    livePreview.setLiveHeightPercent(pct)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })
        col.addView(livePreview, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8)
            bottomMargin = dp(8)
        })

        // LANGUAGE
        col.addView(header("LANGUAGE"))
        radios(listOf("English (US)", "বাংলা (Bangla)"), if (Prefs.lang(this) == "bn") 1 else 0, true) {
            Prefs.putStr(this, "lang", if (it == 1) "bn" else "en")
            livePreview.resetToMainLanguageMode()
        }

        // THEMES
        col.addView(header("THEMES (INCLUDING LIQUID GLASS)"))
        radios(Themes.names, Prefs.theme(this), false) {
            Prefs.putInt(this, "theme", it)
            buildUi()
        }

        // KEY CORNER ROUNDNESS
        val radLabel = tv("Key Corner Roundness: ${Prefs.keyRadius(this)}dp", 14f, t.text)
        col.addView(radLabel)
        col.addView(SeekBar(this).apply {
            max = 18
            progress = (Prefs.keyRadius(this@SettingsActivity) - 4).coerceIn(0, 18)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val r = 4 + p
                    radLabel.text = "Key Corner Roundness: ${r}dp"
                    Prefs.putInt(this@SettingsActivity, "keyRadius", r)
                    livePreview.applyPrefs()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })

        // CUSTOM WALLPAPER
        col.addView(header("CUSTOM KEYBOARD WALLPAPER"))
        col.addView(btn("Choose Custom Wallpaper Image") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, REQ_WALLPAPER)
        })
        col.addView(View(this), LinearLayout.LayoutParams(1, dp(6)))
        col.addView(btn("Clear Custom Wallpaper") {
            Prefs.putStr(this, "wallpaperPath", "")
            livePreview.applyPrefs()
            Toast.makeText(this, "Wallpaper cleared", Toast.LENGTH_SHORT).show()
        })

        // TYPING SOUND & HAPTICS
        col.addView(header("TYPING SOUND & FEEL"))
        col.addView(sw("Sound on keypress", "sound", false))
        radios(listOf("System Click", "Soft Pop", "Mechanical Tick", "Custom Audio File"), Prefs.soundStyle(this), false) {
            Prefs.putInt(this, "soundStyle", it)
        }
        col.addView(btn("Pick Custom Typing Sound (.mp3 / .wav / .ogg)") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
            }
            startActivityForResult(intent, REQ_SOUND)
        })
        col.addView(sw("Vibrate on keypress", "haptic", true))
        col.addView(sw("Number row", "numRow", false))
        col.addView(sw("Auto-capitalize", "autoCap", true))
        col.addView(sw("Word suggestions", "suggest", true))

        // VOICE TYPING PERMISSION
        col.addView(header("VOICE TYPING"))
        col.addView(btn("Enable Microphone for Voice Typing") {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO_PERM)
            } else {
                Toast.makeText(this, "Microphone already enabled for Voice Typing!", Toast.LENGTH_SHORT).show()
            }
        })

        // 1-DAY CLIPBOARD & SCREENSHOT VAULT
        col.addView(header("CLIPBOARD & SCREENSHOTS (AUTO-DELETE IN 1 DAY)"))
        col.addView(btn("Save Screenshot to 24h Vault") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            startActivityForResult(intent, REQ_SCREENSHOT)
        })
        col.addView(tv("All copied text & saved screenshots stay accessible via 📋 on the keyboard bar and automatically delete after 24 hours.", 13f, t.hint))

        // TRY IT
        col.addView(header("TEST TYPING HERE"))
        col.addView(EditText(this).apply {
            hint = "Tap here to type, test Liquid Glass, Bangla, Voice, or hold X…"
            setTextColor(t.text)
            setHintTextColor(t.hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
        })

        // ABOUT SECTION WITH GLOWING PLAY HIGHLIGHT
        col.addView(header("ABOUT"))
        val aboutCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(t.key)
                setStroke(dp(1), t.accent)
            }
        }
        aboutCard.addView(tv("App by Abdur Rahman Nihal (NicarX)", 16f, t.text, true))
        aboutCard.addView(tv("Build by : an AI", 14f, t.hint, true))

        val glowingPlayBadge = TextView(this).apply {
            text = "▶  PLAY HIGHLIGHT"
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            setShadowLayer(20f, 0f, 0f, t.accent)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(t.accent)
            }
            setOnClickListener {
                Toast.makeText(this@SettingsActivity, "X Keyboard by Abdur Rahman Nihal (NicarX) ✨", Toast.LENGTH_SHORT).show()
            }
        }
        ObjectAnimator.ofFloat(glowingPlayBadge, "alpha", 0.72f, 1.0f).apply {
            duration = 1100L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
        aboutCard.addView(glowingPlayBadge, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(10)
        })
        col.addView(aboutCard)

        col.addView(tv("Tip: Long-press the 'X' key on the top-left of the keyboard anytime to jump straight back to these settings.", 13f, t.hint))

        setContentView(sv)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri: Uri = data?.data ?: return
        if (resultCode != RESULT_OK) return

        when (requestCode) {
            REQ_WALLPAPER -> {
                val out = File(filesDir, "custom_wallpaper.png")
                contentResolver.openInputStream(uri)?.use { inp ->
                    FileOutputStream(out).use { outp -> inp.copyTo(outp) }
                }
                Prefs.putStr(this, "wallpaperPath", out.absolutePath)
                livePreview.applyPrefs()
                Toast.makeText(this, "Custom keyboard wallpaper applied!", Toast.LENGTH_SHORT).show()
            }
            REQ_SOUND -> {
                val out = File(filesDir, "custom_keypress_sound.audio")
                contentResolver.openInputStream(uri)?.use { inp ->
                    FileOutputStream(out).use { outp -> inp.copyTo(outp) }
                }
                Prefs.putStr(this, "customSoundPath", out.absolutePath)
                Prefs.putInt(this, "soundStyle", 3)
                Prefs.putBool(this, "sound", true)
                Toast.makeText(this, "Custom typing sound applied!", Toast.LENGTH_SHORT).show()
            }
            REQ_SCREENSHOT -> {
                ClipboardAndMediaStore.addScreenshotFromUri(this, uri)
                Toast.makeText(this, "Screenshot saved (auto-deletes in 24h)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_INPUT_METHODS)
            ?.contains(packageName) == true
        val active = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.startsWith(packageName) == true
        status.text = when {
            active -> "✓ X Keyboard is active"
            enabled -> "Enabled. Now tap step 2 to switch to it."
            else -> "Not enabled yet. Tap step 1 first."
        }
    }
}
