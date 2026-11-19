package com.xkeyboard.app

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class XKeyboardService : InputMethodService(), XKeyboardView.Listener {

    private lateinit var root: LinearLayout
    private lateinit var deckContainer: FrameLayout
    private lateinit var kb: XKeyboardView
    private lateinit var emojiDock: EmojiDockView
    private lateinit var heightPanel: LiveHeightOverlayView
    private lateinit var clipPanel: ClipboardVaultView
    private lateinit var brand: TextView
    private lateinit var voiceBtn: TextView
    private lateinit var clipBtn: TextView
    private lateinit var heightBtn: TextView
    private lateinit var predictor: Predictor
    private val slots = ArrayList<TextView>()
    private val composing = StringBuilder()
    private var words: List<String> = emptyList()
    private var lastWord = ""
    private var suggestOn = true
    private var autoCap = true
    private var lastSpace = 0L

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate() {
        super.onCreate()
        predictor = Predictor(this)
        ClipboardAndMediaStore.purgeExpired(this)
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipListener = ClipboardManager.OnPrimaryClipChangedListener {
            val txt = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
            if (!txt.isNullOrBlank()) {
                ClipboardAndMediaStore.addText(this, txt)
            }
        }
        cm.addPrimaryClipChangedListener(clipListener)
    }

    override fun onDestroy() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipListener?.let { cm.removePrimaryClipChangedListener(it) }
        speechRecognizer?.destroy()
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        kb = XKeyboardView(this).also { it.listener = this }
        emojiDock = EmojiDockView(
            this,
            onPick = { sym -> onText(sym) },
            onBackspace = { onDelete() },
            onBackToLetters = { showDeck(kb) }
        )
        heightPanel = LiveHeightOverlayView(
            this,
            onHeightChanged = { pct -> kb.setLiveHeightPercent(pct) },
            onClose = { showDeck(kb) }
        )
        clipPanel = ClipboardVaultView(
            this,
            onPasteText = { txt ->
                currentInputConnection?.commitText(txt, 1)
                showDeck(kb)
            },
            onClose = { showDeck(kb) }
        )

        brand = TextView(this).apply {
            text = "X"
            textSize = 20f
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            setOnClickListener {
                if (deckContainer.getChildAt(0) !== kb) {
                    showDeck(kb)
                } else {
                    startActivity(Intent(this@XKeyboardService, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            setOnLongClickListener {
                startActivity(Intent(this@XKeyboardService, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            }
        }

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bar.addView(brand, LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.MATCH_PARENT))

        slots.clear()
        for (i in 0 until 3) {
            val tv = TextView(this).apply {
                gravity = Gravity.CENTER
                textSize = 16f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                if (i == 0) setTypeface(null, Typeface.BOLD)
                setOnClickListener { onSuggestion(i) }
            }
            slots.add(tv)
            bar.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f))
        }

        heightBtn = TextView(this).apply {
            text = "↕"
            textSize = 17f
            gravity = Gravity.CENTER
            setOnClickListener {
                if (deckContainer.getChildAt(0) === heightPanel) showDeck(kb) else showDeck(heightPanel)
            }
        }
        clipBtn = TextView(this).apply {
            text = "📋"
            textSize = 15f
            gravity = Gravity.CENTER
            setOnClickListener {
                if (deckContainer.getChildAt(0) === clipPanel) {
                    showDeck(kb)
                } else {
                    clipPanel.reload()
                    showDeck(clipPanel)
                }
            }
        }
        voiceBtn = TextView(this).apply {
            text = "🎙"
            textSize = 16f
            gravity = Gravity.CENTER
            setOnClickListener { toggleVoiceTyping() }
        }

        bar.addView(heightBtn, LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.MATCH_PARENT))
        bar.addView(clipBtn, LinearLayout.LayoutParams(dp(36), ViewGroup.LayoutParams.MATCH_PARENT))
        bar.addView(voiceBtn, LinearLayout.LayoutParams(dp(40), ViewGroup.LayoutParams.MATCH_PARENT))

        deckContainer = FrameLayout(this).apply {
            addView(kb, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)))
        root.addView(deckContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        applyTheme()
        return root
    }

    private fun showDeck(v: View) {
        deckContainer.removeAllViews()
        deckContainer.addView(v, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun applyTheme() {
        val t = Themes.resolve(this)
        root.setBackgroundColor(t.bg)
        brand.setTextColor(t.accent)
        heightBtn.setTextColor(t.hint)
        clipBtn.setTextColor(t.hint)
        voiceBtn.setTextColor(if (isListening) 0xFFEF4444.toInt() else t.accent)
        slots.forEach { it.setTextColor(t.text) }
        kb.applyPrefs()
        emojiDock.refresh()
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        composing.clear()
        lastWord = ""
        ClipboardAndMediaStore.purgeExpired(this)
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        val v = info.inputType and InputType.TYPE_MASK_VARIATION
        suggestOn = Prefs.suggest(this) &&
            cls == InputType.TYPE_CLASS_TEXT &&
            (info.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) == 0 &&
            v != InputType.TYPE_TEXT_VARIATION_PASSWORD &&
            v != InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
            v != InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD &&
            v != InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS &&
            v != InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS &&
            v != InputType.TYPE_TEXT_VARIATION_URI
        autoCap = Prefs.autoCap(this)
        applyTheme()
        showDeck(kb)
        if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE || cls == InputType.TYPE_CLASS_DATETIME) {
            kb.setMode(XKeyboardView.MODE_SYM1)
        } else {
            kb.resetToMainLanguageMode()
        }
        kb.enterLabel = if ((info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) null else
            when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
                EditorInfo.IME_ACTION_GO -> "Go"
                EditorInfo.IME_ACTION_SEARCH -> "Search"
                EditorInfo.IME_ACTION_SEND -> "Send"
                EditorInfo.IME_ACTION_NEXT -> "Next"
                EditorInfo.IME_ACTION_DONE -> "Done"
                else -> null
            }
        kb.setAutoShift(false)
        refreshAutoCap()
        updateSuggestions()
    }

    override fun onFinishInput() {
        composing.clear()
        stopVoiceTyping()
        super.onFinishInput()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (composing.isNotEmpty() && (newSelStart != candidatesEnd || newSelEnd != candidatesEnd)) {
            composing.clear()
            currentInputConnection?.finishComposingText()
            updateSuggestions()
        }
    }

    // ---------- Voice Typing ----------

    private fun toggleVoiceTyping() {
        if (isListening) {
            stopVoiceTyping()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Grant Microphone permission in X Keyboard settings", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition unavailable on this device", Toast.LENGTH_SHORT).show()
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    voiceBtn.text = "🔴"
                    Toast.makeText(this@XKeyboardService, "Listening…", Toast.LENGTH_SHORT).show()
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                    voiceBtn.text = "🎙"
                }
                override fun onError(error: Int) {
                    isListening = false
                    voiceBtn.text = "🎙"
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    voiceBtn.text = "🎙"
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim() ?: return
                    val ic = currentInputConnection ?: return
                    commitWord(ic)
                    val formatted = if (Prefs.lang(this@XKeyboardService) == "en") {
                        best.replaceFirstChar { it.uppercase() }
                    } else best
                    ic.commitText("$formatted ", 1)
                    refreshAutoCap()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        currentInputConnection?.setComposingText(partial, 1)
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val localeTag = if (Prefs.lang(this) == "bn") "bn-BD" else Locale.getDefault().toLanguageTag()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopVoiceTyping() {
        isListening = false
        if (::voiceBtn.isInitialized) voiceBtn.text = "🎙"
        runCatching { speechRecognizer?.stopListening() }
    }

    // ---------- Key Events ----------

    override fun onText(text: String) {
        val ic = currentInputConnection ?: return
        val ch = text[0]
        if (text.length == 1 && suggestOn && (ch.isLetter() || (ch == '\'' && composing.isNotEmpty()))) {
            composing.append(text)
            ic.setComposingText(composing, 1)
        } else {
            commitWord(ic)
            if (suggestOn && text.length == 1 && ",.!?;:।".indexOf(ch) >= 0) {
                val before = ic.getTextBeforeCursor(2, 0)
                if (before != null && before.length == 2 && before[1] == ' ' && !before[0].isWhitespace()) {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            ic.commitText(text, 1)
            if (text == "." || text == "!" || text == "?" || text == "।") lastWord = ""
        }
        updateSuggestions()
        refreshAutoCap()
    }

    override fun onSpace() {
        val ic = currentInputConnection ?: return
        commitWord(ic)
        val now = SystemClock.uptimeMillis()
        val before = ic.getTextBeforeCursor(2, 0)
        if (suggestOn && now - lastSpace < 350 && before != null && before.length == 2 &&
            before[1] == ' ' && before[0].isLetterOrDigit()
        ) {
            ic.deleteSurroundingText(1, 0)
            val endPunct = if (Prefs.lang(this) == "bn") "। " else ". "
            ic.commitText(endPunct, 1)
            lastWord = ""
        } else {
            ic.commitText(" ", 1)
        }
        lastSpace = now
        updateSuggestions()
        refreshAutoCap()
    }

    override fun onDelete() {
        val ic = currentInputConnection ?: return
        if (composing.isNotEmpty()) {
            composing.deleteCharAt(composing.length - 1)
            if (composing.isEmpty()) ic.commitText("", 1) else ic.setComposingText(composing, 1)
        } else {
            val sel = ic.getSelectedText(0)
            if (!sel.isNullOrEmpty()) ic.commitText("", 1) else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        updateSuggestions()
        refreshAutoCap()
    }

    override fun onEnter() {
        val ic = currentInputConnection ?: return
        commitWord(ic)
        val ei = currentInputEditorInfo
        val action = ei?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noAction = ei != null && (ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (!noAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        }
        lastWord = ""
        updateSuggestions()
        refreshAutoCap()
    }

    override fun onCursor(dir: Int) {
        val ic = currentInputConnection ?: return
        if (composing.isNotEmpty()) {
            composing.clear()
            ic.finishComposingText()
        }
        sendDownUpKeyEvents(if (dir < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
        updateSuggestions()
    }

    override fun onToggleLanguage() {
        val next = if (Prefs.lang(this) == "bn") "en" else "bn"
        Prefs.putStr(this, "lang", next)
        kb.resetToMainLanguageMode()
        updateSuggestions()
        Toast.makeText(this, if (next == "bn") "ভাষা: বাংলা" else "Language: English", Toast.LENGTH_SHORT).show()
    }

    override fun onOpenEmoji() {
        emojiDock.refresh()
        showDeck(emojiDock)
    }

    private fun commitWord(ic: InputConnection) {
        if (composing.isEmpty()) return
        val w = composing.toString()
        ic.finishComposingText()
        composing.clear()
        predictor.learn(w, lastWord)
        lastWord = w.lowercase()
    }

    private fun onSuggestion(i: Int) {
        val s = words.getOrNull(i) ?: return
        val ic = currentInputConnection ?: return
        ic.commitText("$s ", 1)
        composing.clear()
        predictor.learn(s, lastWord)
        lastWord = s.lowercase()
        updateSuggestions()
        refreshAutoCap()
    }

    private fun updateSuggestions() {
        val curLang = Prefs.lang(this)
        words = when {
            !suggestOn -> emptyList()
            composing.isEmpty() -> predictor.next(lastWord, curLang)
            else -> predictor.complete(composing.toString(), curLang).ifEmpty { listOf(composing.toString()) }
        }
        for (i in 0 until slots.size) slots[i].text = words.getOrNull(i) ?: ""
    }

    private fun refreshAutoCap() {
        if (!autoCap || Prefs.lang(this) == "bn") return
        val ic = currentInputConnection ?: return
        val ei = currentInputEditorInfo ?: return
        kb.setAutoShift(ic.getCursorCapsMode(ei.inputType) != 0)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
