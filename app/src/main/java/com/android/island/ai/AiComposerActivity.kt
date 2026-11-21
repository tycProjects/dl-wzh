package com.android.island.ai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.android.island.IslandApplication
import com.android.island.core.IslandBus
import com.android.island.data.IslandSettings
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Dedicated typing Activity. Overlay windows cannot host a reliable IME; this one can.
 * The card is pinned to the bottom and lifts above the soft keyboard via window insets.
 */
class AiComposerActivity : Activity() {

    private lateinit var edit: EditText
    private lateinit var card: LinearLayout
    private lateinit var cardLp: FrameLayout.LayoutParams
    private lateinit var micView: TextView
    private var speech: com.android.island.ai.voice.SpeechInput? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IslandBus.setAiTyping(true)
        // The panel behind would collide with this card and the keyboard; hide it while typing.
        com.android.island.service.IslandAccessibilityService.setOverlayHidden(true)

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(true)
        }
        // The composer window is only as tall as its card, with no full-screen dim, in every mode.
        // Tap outside closes it.
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.BOTTOM)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        setFinishOnTouchOutside(true)

        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            fitsSystemWindows = true
        }

        card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F2141418"))
                cornerRadius = 18 * density
                setStroke(dp(1), Color.parseColor("#22FFFFFF"))
            }
            setPadding(dp(14), dp(12), dp(14), dp(12))
            elevation = 10 * density
        }

        card.addView(TextView(this).apply {
            text = "Tulis perintah AI"
            setTextColor(Color.parseColor("#FFECECF1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, 0, 0, dp(8))
        })

        val counterView = TextView(this).apply {
            text = "0"
            setTextColor(Color.parseColor("#FF8B8B9A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }

        edit = EditText(this).apply {
            hint = intent.getStringExtra(EXTRA_HINT) ?: "Tanya apa saja…"
            setHintTextColor(Color.parseColor("#FF8B8B9A"))
            setTextColor(Color.parseColor("#FFECECF1"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setBackgroundColor(Color.parseColor("#FF1C1C22"))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            minLines = 2
            maxLines = 6
            gravity = Gravity.TOP or Gravity.START
            // No TYPE_TEXT_FLAG_MULTI_LINE: with it, the keyboard turns Enter into a newline and the
            // Send action never fires. Plain text keeps the Send/Search key on the IME.
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_FULLSCREEN
            setText(intent.getStringExtra(EXTRA_PREFILL).orEmpty())
            setSelection(text?.length ?: 0)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    submit(); true
                } else false
            }
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    counterView.text = "${s?.length ?: 0}"
                }
            })
        }
        card.addView(
            edit,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(10), 0, 0)
        }
        row.addView(counterView)
        row.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })
        micView = TextView(this).apply {
            text = "Bicara"
            setTextColor(Color.parseColor("#FFB9A8FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener { toggleVoiceOrStop() }
        }
        row.addView(micView)
        row.addView(TextView(this).apply {
            text = "Batal"
            setTextColor(Color.parseColor("#FF8B8B9A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setOnClickListener { finishQuiet() }
        })
        row.addView(TextView(this).apply {
            text = "Kirim"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FF7C5CFC"))
                cornerRadius = 12 * density
            }
            setPadding(dp(18), dp(10), dp(18), dp(10))
            setOnClickListener { submit() }
        })
        card.addView(row)

        cardLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
            leftMargin = dp(12)
            rightMargin = dp(12)
            bottomMargin = dp(12)
        }
        root.addView(card, cardLp)
        setContentView(root)

        // Lift the card above the IME when insets change (resize is not enough on all OEMs).
        root.setOnApplyWindowInsetsListener { _, insets ->
            val imeBottom = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(WindowInsets.Type.ime()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            val nav = if (Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else 0
            val lift = maxOf(imeBottom - nav, 0)
            cardLp.bottomMargin = dp(12) + lift
            card.layoutParams = cardLp
            insets
        }
        root.requestApplyInsets()

        edit.post {
            edit.requestFocus()
            if (intent.getBooleanExtra(EXTRA_VOICE, false)) {
                toggleVoice()
            } else {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun toggleVoice() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), REQ_MIC)
            return
        }
        startVoice()
    }

    private fun startVoice() {
        val s = speech ?: com.android.island.ai.voice.SpeechInput(
            context = this,
            onPartial = { partial ->
                edit.setText(partial)
                edit.setSelection(partial.length)
            },
            onFinal = { text ->
                // A spoken command goes straight through, same as pressing Kirim.
                edit.setText(text)
                edit.setSelection(text.length)
                submit()
            },
            onEnd = { micView.text = "Bicara" },
            onFailure = { code -> onSpeechError(code) }
        ).also { speech = it }
        if (!s.isAvailable()) {
            launchSystemSpeech()
            return
        }
        micView.text = "Mendengarkan…"
        s.start()
    }

    /** Tap "Mendengarkan…" again to stop; tap "Bicara" to start. */
    private fun toggleVoiceOrStop() {
        if (micView.text.toString() == "Mendengarkan…") {
            speech?.stop()
            micView.text = "Bicara"
        } else {
            toggleVoice()
        }
    }

    private fun onSpeechError(code: Int) {
        micView.text = "Bicara"
        val message = when (code) {
            android.speech.SpeechRecognizer.ERROR_NO_MATCH,
            android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Suara tidak terdengar. Coba lagi."
            android.speech.SpeechRecognizer.ERROR_NETWORK,
            android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Pengenalan suara butuh koneksi internet."
            android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon belum diberikan."
            android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Pengenal suara sedang sibuk. Coba lagi sebentar."
            else -> null
        }
        if (message != null) {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
        } else {
            // Service-level failure: use the system voice input, which runs in its own window.
            launchSystemSpeech()
        }
    }

    /** Fallback when the in-app recognizer cannot start: the system voice dialog returns the text. */
    @Suppress("DEPRECATION")
    private fun launchSystemSpeech() {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Bicara perintah")
        }
        try {
            startActivityForResult(intent, REQ_SYSTEM_SPEECH)
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, "Pengenalan suara tidak tersedia di perangkat ini.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_SYSTEM_SPEECH) return
        val text = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        if (resultCode == RESULT_OK && !text.isNullOrBlank()) {
            edit.setText(text)
            edit.setSelection(text.length)
            submit()
        } else {
            android.widget.Toast.makeText(this, "Tidak ada perintah yang terdengar.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_MIC && grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startVoice()
        } else if (requestCode == REQ_MIC) {
            android.widget.Toast.makeText(this, "Izin mikrofon diperlukan untuk perintah suara.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        speech?.destroy()
        IslandBus.endExternalAi()
        com.android.island.service.IslandAccessibilityService.setOverlayHidden(false)
        super.onDestroy()
    }

    private fun submit() {
        val text = edit.text?.toString().orEmpty().trim()
        if (text.isEmpty() && AiSession.pending.value.isEmpty()) {
            finishQuiet()
            return
        }
        val app = application as? IslandApplication
        val settings = runBlocking {
            withTimeoutOrNull(800) {
                app?.settingsRepository?.settings?.firstOrNull()
            } ?: IslandSettings()
        }
        AiSession.send(text, settings)
        finishQuiet()
    }

    private fun finishQuiet() {
        IslandBus.setAiTyping(true)
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        const val EXTRA_HINT = "hint"
        const val EXTRA_PREFILL = "prefill"
        const val EXTRA_VOICE = "voice"
        private const val REQ_MIC = 4311
        private const val REQ_SYSTEM_SPEECH = 4312

        fun intent(
            context: android.content.Context,
            hint: String,
            prefill: String = "",
            voice: Boolean = false
        ): Intent =
            Intent(context, AiComposerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_HINT, hint)
                .putExtra(EXTRA_PREFILL, prefill)
                .putExtra(EXTRA_VOICE, voice)
    }
}
