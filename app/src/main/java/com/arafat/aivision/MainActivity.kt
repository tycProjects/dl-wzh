package com.arafat.aivision

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val cyan = 0xFF43F5FF.toInt()
    private val green = 0xFF58FF9A.toInt()
    private val bg = 0xFF05090D.toInt()
    private val panel = 0xFF0B151B.toInt()
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var input: EditText
    private lateinit var orb: OrbView
    private var tts: TextToSpeech? = null
    private var speech: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val clockTask = object : Runnable {
        override fun run() {
            findViewById<TextView>(R.id.clock)?.text =
                SimpleDateFormat("EEE, dd MMM  HH:mm:ss", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        buildUi()
        handler.post(clockTask)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setBackgroundColor(bg)
        }
        val top = TextView(this).apply {
            text = "A R A F A T   /   A I   V I S I O N"
            textSize = 17f; setTextColor(cyan); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; letterSpacing = .04f
        }
        root.addView(top, matchWrap())
        val subtitle = TextView(this).apply {
            text = "OFFLINE ASSISTANT  •  LOCAL MODE"
            textSize = 10f; setTextColor(green); gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, dp(8))
        }
        root.addView(subtitle, matchWrap())
        val clock = TextView(this).apply {
            id = R.id.clock
            textSize = 12f; setTextColor(0xFF91AAB5.toInt()); gravity = Gravity.CENTER
        }
        root.addView(clock, matchWrap())

        orb = OrbView(this)
        root.addView(orb, LinearLayout.LayoutParams(-1, dp(205)).apply {
            topMargin = dp(8); bottomMargin = dp(8)
        })

        status = TextView(this).apply {
            text = "● SYSTEM READY"
            textSize = 12f; setTextColor(green); gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(10))
        }
        root.addView(status, matchWrap())

        output = TextView(this).apply {
            text = "স্বাগতম! আমি ARAFAT AI VISION-এর Offline Assistant।\n\nTry: time, battery, help, hello"
            textSize = 15f; setTextColor(0xFFE1F7FA.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(panel, cyan, 1)
        }
        val scroll = ScrollView(this).apply { addView(output) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f).apply {
            topMargin = dp(4); bottomMargin = dp(10)
        })

        input = EditText(this).apply {
            hint = "বাংলা বা English লিখুন..."
            setHintTextColor(0xFF6D858D.toInt()); setTextColor(-1)
            textSize = 15f; singleLine = false; maxLines = 3
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(panel, green, 1)
        }
        root.addView(input, matchWrap())

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }
        row.addView(makeButton("SEND", cyan) { sendText() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(5) })
        row.addView(makeButton("🎙 VOICE", green) { startVoice() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(5); rightMargin = dp(5) })
        row.addView(makeButton("SPEAK", cyan) { speakOutput() },
            LinearLayout.LayoutParams(0, dp(48), 1f).apply { leftMargin = dp(5) })
        root.addView(row, matchWrap())

        val footer = TextView(this).apply {
            text = "LOCAL COMMANDS ONLY  •  NO API KEY"
            textSize = 9f; setTextColor(0xFF526A74.toInt()); gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(footer, matchWrap())
        setContentView(root)
    }

    private fun sendText() {
        val query = input.text.toString().trim()
        if (query.isEmpty()) return
        input.setText("")
        status.text = "● PROCESSING LOCAL COMMAND"
        orb.mode = "thinking"
        val answer = localAnswer(query)
        output.text = "YOU  ›  $query\n\nARAFAT AI  ›  $answer"
        status.text = "● SYSTEM READY"
        orb.mode = "idle"
    }

    private fun localAnswer(q: String): String {
        val s = q.lowercase(Locale.ROOT)
        return when {
            listOf("time", "সময়", "সময়", "কটা বাজে").any { s.contains(it) } ->
                "এখন " + SimpleDateFormat("hh:mm a, dd MMMM yyyy", Locale("bn", "BD")).format(Date())
            listOf("battery", "ব্যাটারি", "চার্জ").any { s.contains(it) } -> {
                val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (level in 0..100) "ফোনের ব্যাটারি: $level%" else "ব্যাটারির তথ্য পাওয়া যায়নি।"
            }
            listOf("hello", "hi", "হ্যালো", "হাই", "আসসালামু").any { s.contains(it) } ->
                "হ্যালো! আমি ARAFAT AI VISION। আমি অফলাইনে কিছু নির্দিষ্ট কমান্ড চালাতে পারি।"
            listOf("help", "সাহায্য", "কি পারো", "কী পারো").any { s.contains(it) } ->
                "আমি এখন করতে পারি:\n• সময় বলা\n• ব্যাটারি লেভেল দেখা\n• Hello-র উত্তর\n• উত্তর Text-to-Speech-এ পড়া\n\nঅফলাইনে সাধারণ জ্ঞানের সব প্রশ্নের উত্তর দিতে পারি না।"
            listOf("who are you", "তুমি কে").any { s.contains(it) } ->
                "আমি ARAFAT AI VISION — একটি লোকাল অফলাইন সহকারী।"
            else ->
                "এই সংস্করণে এই প্রশ্নের জন্য লোকাল উত্তর নেই। Help লিখে উপলভ্য কমান্ড দেখুন। বড় AI মডেলের মতো উত্তর দিতে আলাদা offline model বা internet-based AI দরকার।"
        }
    }

    private fun startVoice() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "এই ফোনে Speech Recognition উপলভ্য নয়।", Toast.LENGTH_LONG).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
            return
        }
        try {
            speech?.destroy()
            speech = SpeechRecognizer.createSpeechRecognizer(this)
            speech?.setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    status.text = "● LISTENING"; orb.mode = "listening"
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) { orb.level = rmsdB }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { status.text = "● PROCESSING" }
                override fun onError(error: Int) {
                    status.text = "● SYSTEM READY"; orb.mode = "idle"
                    Toast.makeText(this@MainActivity, "Voice input ব্যর্থ। Offline language pack পরীক্ষা করুন।", Toast.LENGTH_LONG).show()
                }
                override fun onResults(results: android.os.Bundle?) {
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!spoken.isNullOrBlank()) { input.setText(spoken); sendText() }
                    status.text = "● SYSTEM READY"; orb.mode = "idle"
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speech?.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech service চালু করা যায়নি।", Toast.LENGTH_LONG).show()
            status.text = "● SYSTEM READY"; orb.mode = "idle"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startVoice()
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            tts?.language = Locale("bn", "BD")
        }
    }

    private fun speakOutput() {
        val text = output.text.toString().substringAfter("ARAFAT AI  ›  ", output.text.toString())
        if (tts?.isSpeaking == true) {
            tts?.stop()
            Toast.makeText(this, "Speech stopped", Toast.LENGTH_SHORT).show()
        } else {
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "arafat_reply")
            if (result == TextToSpeech.ERROR) {
                Toast.makeText(this, "TTS unavailable. Check installed voice/language data.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun makeButton(label: String, color: Int, action: () -> Unit): Button =
        Button(this).apply {
            text = label; textSize = 11f; setTextColor(color)
            isAllCaps = false; setOnClickListener { action() }
            background = rounded(panel, color, 1)
            minHeight = 0; minimumHeight = 0; minWidth = 0; minimumWidth = 0
            setPadding(dp(2), 0, dp(2), 0)
        }

    private fun rounded(color: Int, stroke: Int, width: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(12).toFloat()
        setStroke(dp(width), stroke)
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(clockTask)
        speech?.destroy(); speech = null
        tts?.stop(); tts?.shutdown(); tts = null
        super.onDestroy()
    }

    class OrbView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val cyan = 0xFF43F5FF.toInt()
        private val green = 0xFF58FF9A.toInt()
        var mode = "idle"
            set(value) { field = value; invalidate() }
        var level = 0f
        private var phase = 0f
        private val tick = object : Runnable {
            override fun run() { phase += 0.07f; invalidate(); postDelayed(this, 40) }
        }
        init { post(tick) }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val base = (width.coerceAtMost(height) * 0.22f)
            val pulse = 1f + 0.045f * sin(phase.toDouble()).toFloat() + if (mode == "listening") (level / 100f).coerceIn(0f, .12f) else 0f
            for (i in 5 downTo 1) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (i * 2.2f)
                paint.color = if (i % 2 == 0) cyan else green
                paint.alpha = 18 + i * 8
                canvas.drawCircle(cx, cy, base * pulse + i * 9f, paint)
            }
            paint.alpha = 255
            paint.strokeWidth = 2f
            for (i in 0..2) {
                paint.color = if (i % 2 == 0) cyan else green
                canvas.drawCircle(cx, cy, base * (1.25f + i * .28f), paint)
            }
            paint.style = Paint.Style.FILL
            paint.color = 0xFF0A252A.toInt()
            canvas.drawCircle(cx, cy, base * .92f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.color = cyan
            canvas.drawCircle(cx, cy, base * .92f, paint)
            paint.style = Paint.Style.FILL
            paint.color = green
            canvas.drawCircle(cx, cy, base * .16f * pulse, paint)
            paint.color = cyan
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 13f
            canvas.drawText("AI CORE", cx, cy + base + 36f, paint)
            paint.textSize = 9f
            paint.color = 0xFF9AB7C0.toInt()
            canvas.drawText(mode.uppercase(Locale.ROOT), cx, cy + base + 51f, paint)
        }
        override fun onDetachedFromWindow() {
            removeCallbacks(tick)
            super.onDetachedFromWindow()
        }
    }
}
