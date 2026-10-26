package com.tkofficial.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val bg = Color.rgb(8, 13, 24)
    private val panel = Color.rgb(17, 26, 43)
    private val panel2 = Color.rgb(23, 35, 56)
    private val cyan = Color.rgb(0, 217, 255)
    private val red = Color.rgb(255, 63, 92)
    private val white = Color.rgb(238, 244, 255)
    private val muted = Color.rgb(147, 164, 190)
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var tts: TextToSpeech
    private val executor = Executors.newSingleThreadExecutor()
    private val history = mutableListOf<Pair<String, String>>()
    private var provider = "Gemini"
    private var voiceProfile = "Expressive Female"
    private var currentPage = "Home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("darktk", MODE_PRIVATE)
        provider = prefs.getString("provider", "Gemini") ?: "Gemini"
        voiceProfile = prefs.getString("voice", "Expressive Female") ?: "Expressive Female"
        tts = TextToSpeech(this, this)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        showPage("Home")
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts.language = Locale("bn", "BD") }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float = 14f, color: Int = white, bold: Boolean = false): TextView = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)); background = rounded(panel, 18)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
    }
    private fun rounded(color: Int, radius: Int): android.graphics.drawable.GradientDrawable = android.graphics.drawable.GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.rgb(37, 53, 77))
    }
    private fun button(label: String, primary: Boolean = false, action: () -> Unit): TextView = TextView(this).apply {
        text = label; textSize = 14f; setTypeface(typeface, Typeface.BOLD); gravity = Gravity.CENTER
        setTextColor(if (primary) bg else white); background = rounded(if (primary) cyan else panel2, 14)
        setPadding(dp(14), dp(12), dp(14), dp(12)); isClickable = true; isFocusable = true
        setOnClickListener { animate().scaleX(.96f).scaleY(.96f).setDuration(75).withEndAction { animate().scaleX(1f).scaleY(1f).setDuration(110).start(); action() }.start() }
    }
    private fun edit(hintText: String, multi: Boolean = false): EditText = EditText(this).apply {
        hint = hintText; setHintTextColor(muted); setTextColor(white); textSize = 15f; setPadding(dp(12), dp(10), dp(12), dp(10))
        background = rounded(panel2, 12); if (multi) { minLines = 3; gravity = Gravity.TOP; isSingleLine = false } else isSingleLine = true
    }
    private fun label(s: String) { body.addView(text(s, 12f, muted, true).apply { setPadding(0, dp(8), 0, dp(8)) }) }
    private fun title(s: String, sub: String) {
        body.addView(text(s, 26f, white, true).apply { setPadding(0, 0, 0, dp(4)) })
        body.addView(text(sub, 13f, muted).apply { setPadding(0, 0, 0, dp(18)) })
    }
    private fun showPage(page: String) {
        currentPage = page
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(18), dp(12), dp(18), dp(10)) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(8), 0, dp(14)) }
        header.addView(text("◈  DARK TK", 20f, cyan, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(text("NATIVE AI", 10f, muted, true))
        root.addView(header)
        val nav = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val navRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Home", "Chat", "Voice", "Code", "Files", "Settings").forEach { p ->
            val b = button((if (p == page) "● " else "") + p, p == page) { showPage(p) }
            navRow.addView(b, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(7) })
        }
        nav.addView(navRow); root.addView(nav)
        val scroll = ScrollView(this).apply { fillViewport = true; clipToPadding = false; setPadding(0, dp(16), 0, dp(4)) }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        when (page) { "Home" -> home(); "Chat" -> chat(); "Voice" -> voice(); "Code" -> code(); "Files" -> files(); "Settings" -> settings() }
        body.alpha = 0f; body.translationY = dp(8).toFloat(); body.animate().alpha(1f).translationY(0f).setDuration(220).setInterpolator(DecelerateInterpolator()).start()
    }
    private fun home() {
        title("Your AI workspace", "A private-first assistant built for Android.")
        val hero = card(); hero.background = rounded(Color.rgb(18, 43, 63), 20)
        hero.addView(text("DARK TK  /  CORE", 11f, cyan, true)); hero.addView(text("Think. Create. Build.", 24f, white, true).apply { setPadding(0, dp(8), 0, dp(4)) })
        hero.addView(text("Use your own provider key to enable live AI. Without a key, the app clearly uses local helper responses.", 13f, muted))
        body.addView(hero)
        val grid = listOf("✦  AI Chat" to "Chat with Gemini or OpenAI", "◉  Voice" to "Speech input and spoken replies", "⌘  Code Studio" to "Edit and save local text files", "▣  File Vault" to "Manage app-private files")
        grid.forEach { (a,b) -> val c = card(); c.addView(text(a, 17f, cyan, true)); c.addView(text(b, 13f, muted).apply { setPadding(0, dp(4), 0, dp(10)) }); c.addView(button("Open  →") { showPage(when { a.contains("Chat") -> "Chat"; a.contains("Voice") -> "Voice"; a.contains("Code") -> "Code"; else -> "Files" }) }); body.addView(c) }
        val status = card(); status.addView(text("SYSTEM STATUS", 11f, muted, true)); status.addView(text("Provider: $provider", 15f, white, true)); status.addView(text("Storage: app-private /files/Dark TK Lood", 12f, muted)); body.addView(status)
    }
    private fun chat() {
        title("AI Chat", "Live provider calls run on a background thread.")
        val info = card(); info.addView(text("Provider: $provider", 14f, cyan, true)); info.addView(text("API key stays on this device in app preferences. For production-grade secrecy, use a trusted backend proxy.", 12f, muted)); info.addView(button("Configure provider / API key") { showPage("Settings") }); body.addView(info)
        val transcript = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (history.isEmpty()) transcript.addView(text("Start a conversation. Messages are stored in memory for this session.", 13f, muted).apply { setPadding(0, dp(10), 0, dp(16)) })
        history.forEach { (role, msg) -> val c = card(); c.addView(text(if (role == "user") "YOU" else "DARK TK", 10f, if (role == "user") cyan else red, true)); c.addView(text(msg, 14f, white)); transcript.addView(c) }
        body.addView(transcript)
        val input = edit("Message DARK TK…", true); body.addView(input)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(8), 0, 0) }
        row.addView(button("＋ Voice", false) { startSpeech() }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(8) })
        row.addView(button("Send  ↗", true) { val q = input.text.toString().trim(); if (q.isNotEmpty()) { input.setText(""); sendChat(q) } }, LinearLayout.LayoutParams(0, -2, 1f))
        body.addView(row)
        body.addView(button("Clear session history", false) { history.clear(); showPage("Chat") }.apply { setPadding(dp(8), dp(8), dp(8), dp(8)) })
    }
    private fun sendChat(q: String) {
        history.add("user" to q); showPage("Chat")
        val loading = Toast.makeText(this, "DARK TK is thinking…", Toast.LENGTH_SHORT); loading.show()
        executor.execute {
            val answer = try { callProvider(q) } catch (e: Exception) { "AI request failed: ${e.message ?: "network error"}. Check internet, provider, model and API key in Settings." }
            runOnUiThread { history.add("assistant" to answer); showPage("Chat") }
        }
    }
    private fun callProvider(prompt: String): String {
        val key = prefs.getString(if (provider == "Gemini") "gemini_key" else "openai_key", "")?.trim().orEmpty()
        if (key.isBlank()) return "No API key configured. Open Settings and add a $provider key. This is an honest setup notice, not a live AI response."
        val url: String; val payload: JSONObject
        if (provider == "Gemini") {
            val model = prefs.getString("gemini_model", "gemini-2.0-flash") ?: "gemini-2.0-flash"
            url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
            payload = JSONObject().put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        } else {
            val model = prefs.getString("openai_model", "gpt-4o-mini") ?: "gpt-4o-mini"
            url = "https://api.openai.com/v1/chat/completions"
            payload = JSONObject().put("model", model).put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        }
        val conn = (URL(url).openConnection() as HttpURLConnection).apply { requestMethod = "POST"; connectTimeout = 20000; readTimeout = 30000; doOutput = true; setRequestProperty("Content-Type", "application/json") }
        if (provider == "OpenAI") conn.setRequestProperty("Authorization", "Bearer $key")
        conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val raw = stream.bufferedReader().use { it.readText() }; conn.disconnect()
        val json = JSONObject(raw)
        if (code !in 200..299) throw IllegalStateException("HTTP $code: ${json.optJSONObject("error")?.optString("message") ?: raw.take(240)}")
        return if (provider == "Gemini") json.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        else json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
    }
    private fun voice() {
        title("Voice Studio", "Android speech recognition and on-device text-to-speech.")
        val c = card(); c.addView(text("VOICE PROFILE", 11f, muted, true))
        val spinner = Spinner(this); val profiles = listOf("Male", "Female", "Robot", "Expressive Female")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, profiles)
        spinner.setSelection(profiles.indexOf(voiceProfile).coerceAtLeast(0)); c.addView(spinner)
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { voiceProfile = profiles[position]; prefs.edit().putString("voice", voiceProfile).apply(); if (::tts.isInitialized) { tts.setPitch(when(voiceProfile){"Male"->0.8f;"Robot"->0.65f;else->1.15f}); tts.setSpeechRate(if(voiceProfile=="Robot")0.82f else 1f) } }
        }
        c.addView(button("▶ Test voice", true) { tts.speak("হ্যালো, আমি ডার্ক টি কে।", TextToSpeech.QUEUE_FLUSH, null, "darktk-test") }); c.addView(text("These profiles adjust Android TTS pitch/rate; they do not install a separate voice model.", 12f, muted)); body.addView(c)
        val s = card(); s.addView(text("SPEECH INPUT", 11f, muted, true)); s.addView(text("Use your device's speech recognizer. Availability and Bengali support depend on installed speech services.", 13f, white)); s.addView(button("🎙 Start speech recognition", true) { startSpeech() }); body.addView(s)
    }
    private fun startSpeech() {
        try { val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD"); putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to DARK TK") }; startActivityForResult(i, 501) }
        catch (_: Exception) { Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show() }
    }
    @Deprecated("Deprecated Android callback kept for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (requestCode == 501 && resultCode == RESULT_OK) { val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty(); if (result.isNotBlank()) { if (currentPage == "Chat") sendChat(result) else { tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, "speech-echo"); Toast.makeText(this, result, Toast.LENGTH_LONG).show() } } } }
    private fun workspace(): File = File(filesDir, "Dark TK Lood").apply { if (!exists()) mkdirs() }
    private fun code() {
        title("Code Studio", "A lightweight local text editor — not a full compiler/IDE.")
        val c = card(); val filename = edit("Filename, e.g. index.html"); filename.setText("index.html"); c.addView(filename)
        val content = edit("Write HTML, CSS, JavaScript or notes…", true); content.minLines = 9
        val starter = "<!doctype html>\n<html>\n<head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>My project</title></head>\n<body>\n  <h1>Hello, DARK TK!</h1>\n</body>\n</html>"
        content.setText(starter); c.addView(content)
        c.addView(button("Save file", true) { val name = filename.text.toString().trim(); if (!validFileName(name)) Toast.makeText(this, "Use a simple filename without path separators.", Toast.LENGTH_LONG).show() else try { File(workspace(), name).writeText(content.text.toString()); Toast.makeText(this, "Saved to app-private Dark TK Lood", Toast.LENGTH_LONG).show() } catch(e: Exception) { Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show() } })
        c.addView(button("Load saved file") { val name = filename.text.toString().trim(); val f = File(workspace(), name); if (validFileName(name) && f.isFile) content.setText(f.readText()) else Toast.makeText(this, "File not found.", Toast.LENGTH_SHORT).show() })
        body.addView(c)
        body.addView(text("Saved files are inside app-private storage. This screen edits text; it does not compile Kotlin/Java or run arbitrary code.", 12f, muted))
    }
    private fun validFileName(s: String) = s.isNotBlank() && s.length <= 100 && !s.contains("/") && !s.contains("\\") && s != "." && s != ".."
    private fun files() {
        title("File Vault", "Files stored inside this app's private sandbox.")
        val c = card(); c.addView(text("Dark TK Lood", 18f, cyan, true)); c.addView(text(workspace().absolutePath, 11f, muted)); c.addView(button("＋ Create folder") { val input = edit("Folder name"); android.app.AlertDialog.Builder(this).setTitle("Create folder").setView(input).setPositiveButton("Create") { _, _ -> val n=input.text.toString().trim(); if(validFileName(n)) { val f=File(workspace(),n); if(f.mkdirs()) Toast.makeText(this,"Folder created",Toast.LENGTH_SHORT).show() else Toast.makeText(this,"Could not create folder",Toast.LENGTH_SHORT).show(); showPage("Files") } else Toast.makeText(this,"Invalid folder name",Toast.LENGTH_SHORT).show() }.setNegativeButton("Cancel",null).show() }); body.addView(c)
        val listing = card(); val children = workspace().listFiles()?.sortedBy { !it.isDirectory } ?: emptyList()
        if (children.isEmpty()) listing.addView(text("No files yet. Save a file from Code Studio.", 13f, muted))
        children.forEach { f -> val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(0,dp(6),0,dp(6)) }; row.addView(text((if(f.isDirectory) "▰  " else "▤  ")+f.name,14f,white),LinearLayout.LayoutParams(0,-2,1f)); row.addView(button("Open") { if(f.isDirectory) { Toast.makeText(this,"Folder: ${f.name} (${f.list()?.size ?: 0} items)",Toast.LENGTH_SHORT).show() } else { val content=f.readText(); val intent=Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,content); startActivity(Intent.createChooser(intent,"View/share ${f.name}")) } }); row.addView(button("×") { android.app.AlertDialog.Builder(this).setMessage("Delete ${f.name}?").setPositiveButton("Delete") { _, _ -> if(f.isDirectory) f.deleteRecursively() else f.delete(); showPage("Files") }.setNegativeButton("Cancel",null).show() }.apply { setTextColor(red) }); listing.addView(row) }
        body.addView(listing)
        body.addView(text("Android apps cannot password-lock a folder for other file managers. These files are app-private and are removed if the app is uninstalled.", 12f, muted))
    }
    private fun settings() {
        title("Settings & providers", "Configure API access. Never share your keys in chat.")
        val p = card(); p.addView(text("AI PROVIDER", 11f, muted, true)); val spin = Spinner(this); spin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Gemini", "OpenAI")); spin.setSelection(if(provider=="OpenAI")1 else 0); p.addView(spin)
        spin.onItemSelectedListener = object: android.widget.AdapterView.OnItemSelectedListener { override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}; override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { provider=if(position==1)"OpenAI" else "Gemini"; prefs.edit().putString("provider",provider).apply() } }
        val g = edit("Gemini API key"); g.setText(prefs.getString("gemini_key", "") ?: ""); g.inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD; p.addView(g)
        val gm = edit("Gemini model (default gemini-2.0-flash)"); gm.setText(prefs.getString("gemini_model","gemini-2.0-flash")); p.addView(gm)
        val o = edit("OpenAI API key"); o.setText(prefs.getString("openai_key", "") ?: ""); o.inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD; p.addView(o)
        val om = edit("OpenAI model (default gpt-4o-mini)"); om.setText(prefs.getString("openai_model","gpt-4o-mini")); p.addView(om)
        p.addView(button("Save provider settings", true) { prefs.edit().putString("provider",provider).putString("gemini_key",g.text.toString().trim()).putString("gemini_model",gm.text.toString().trim().ifBlank { "gemini-2.0-flash" }).putString("openai_key",o.text.toString().trim()).putString("openai_model",om.text.toString().trim().ifBlank { "gpt-4o-mini" }).apply(); Toast.makeText(this,"Settings saved on this device",Toast.LENGTH_LONG).show(); showPage("Settings") })
        p.addView(button("Delete saved API keys", false) { android.app.AlertDialog.Builder(this).setMessage("Remove both API keys from app preferences?").setPositiveButton("Delete") { _, _ -> prefs.edit().remove("gemini_key").remove("openai_key").apply(); showPage("Settings") }.setNegativeButton("Cancel",null).show() }); body.addView(p)
        val privacy=card(); privacy.addView(text("PRIVACY & LIMITATIONS",15f,cyan,true)); privacy.addView(text("• API calls send your prompt to the selected provider.\n• API keys are stored in ordinary app preferences, not hardware-backed encryption. Do not use this as a production secret vault.\n• Chat history currently lives only in memory and clears when the app process ends.\n• This app does not pretend that unsupported services are working.",13f,muted)); body.addView(privacy)
        val about=card(); about.addView(text("ABOUT DARK TK",16f,white,true)); about.addView(text("Native Kotlin • Package com.tkofficial.assistant • Version 1.0.0",12f,muted)); body.addView(about)
    }
    override fun onDestroy() { if (::tts.isInitialized) { tts.stop(); tts.shutdown() }; executor.shutdownNow(); super.onDestroy() }
}
