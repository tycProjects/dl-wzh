package com.smali.fox

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.zip.ZipInputStream
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 11, 18); private val card = Color.rgb(18, 23, 34)
    private val primary = Color.rgb(137, 255, 175); private val purple = Color.rgb(161, 132, 255)
    private val white = Color.rgb(244, 247, 250); private val muted = Color.rgb(145, 157, 172)
    private val cyan = Color.rgb(100, 215, 255); private val amber = Color.rgb(255, 196, 92)
    private lateinit var root: LinearLayout; private lateinit var resultsBox: LinearLayout
    private lateinit var search: EditText; private lateinit var noResults: TextView
    private lateinit var dexValue: TextView; private lateinit var apkValue: TextView; private lateinit var stringsValue: TextView; private lateinit var foundValue: TextView
    private lateinit var progress: ProgressBar; private lateinit var status: TextView
    private var allResults = listOf<ResultItem>()
    private var filterMode = "premium"
    private val liveResults = mutableListOf<ResultItem>()
    private val liveLock = Any()
    private var liveFlushScheduled = false
    private var scanInProgress = false
    private val scanUiHandler = Handler(Looper.getMainLooper())
    private var progressUiScheduled = false
    private var pendingProgressText = ""
    private var allZMethods = listOf<DexScanner.ZMethodResult>()
    private lateinit var sendZButton: TextView
    private lateinit var batchReportsBox: LinearLayout
    private val reportPrefs by lazy { getSharedPreferences("deepseek_reports", MODE_PRIVATE) }
    private val accountPrefs by lazy { getSharedPreferences("deepseek_accounts", MODE_PRIVATE) }
    private val defaultDeepSeekDeviceId = "BFP9d7e3MiBf67ujqPsLPJlmjVlZQa37tAvGLAHQT9hak7uhSJuQ/kTG1NGoiotru8oXQTnSlxgaEQXDCZI3LmA=="

    data class ResultItem(val signature: String, val evidence: String, val confidence: Int, val category: String, val keywords: List<String> = emptyList())
    data class DeepSeekAccount(val id: String, val email: String, val token: String, val deviceId: String, val status: String, val lastError: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // السماح بلقطات الشاشة وتسجيل الشاشة؛ لا تستخدم هذه الواجهة FLAG_SECURE.
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        buildUi()
        if (reportPrefs.getBoolean("auth_required", false)) window.decorView.postDelayed({ showDeepSeekLogin() }, 350L)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float, color: Int = white): TextView = TextView(this).apply { text = value; textSize = size; setTextColor(color); setPadding(dp(2), dp(2), dp(2), dp(2)) }
    private fun rounded(color: Int, radius: Int = 14, stroke: Int? = null): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != null) setStroke(dp(1), stroke) }
    private fun cardView(view: View, marginTop: Int = 6): LinearLayout { return LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(card, 12, Color.rgb(58,58,66)); setPadding(dp(14), dp(10), dp(14), dp(12)); addView(view); val p = LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(marginTop), 0, dp(6)); root.addView(this, p) } }

    private fun buildUi() {
        window.statusBarColor = Color.rgb(8, 13, 22); window.navigationBarColor = Color.rgb(8, 13, 22)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(8, 13, 22)); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val scroll = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_NEVER }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(40)) }


        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(Color.rgb(18, 28, 42), 24, Color.rgb(44, 76, 92)); setPadding(dp(20), dp(20), dp(20), dp(20)) }
        hero.addView(text("فحص ذكي، واضح، ودفاعي", 12f, cyan).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD; letterSpacing = .08f })
        hero.addView(text("اكتشف دوال Premium داخل أي APK", 22f, white).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(0, dp(6), 0, dp(4)) })
        hero.addView(text("تحليل DEX وSmali عالي الدقة مع تقارير DeepSeek اختيارية.", 13f, muted).apply { setLineSpacing(0f, 1.2f); setPadding(0, 0, 0, dp(18)) })
        val pick = TextView(this).apply { text = "اختيار ملف APK     〉"; textSize = 15f; gravity = Gravity.CENTER; setTextColor(Color.rgb(7, 18, 22)); typeface = android.graphics.Typeface.DEFAULT_BOLD; background = rounded(primary, 15); setPadding(0, dp(15), 0, dp(15)); isClickable = true; setOnClickListener { chooseApk() } }
        hero.addView(pick, LinearLayout.LayoutParams(-1, dp(54)))
        val accountsButton = TextView(this).apply { text = "إدارة حسابات DeepSeek"; textSize = 13f; gravity = Gravity.CENTER; setTextColor(purple); background = rounded(Color.TRANSPARENT, 14, Color.rgb(70, 57, 105)); setPadding(dp(12), dp(11), dp(12), dp(11)); isClickable = true; setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_account, 0); compoundDrawablePadding = dp(8); setOnClickListener { showAccountsDialog() } }
        hero.addView(accountsButton, LinearLayout.LayoutParams(-1, dp(44)).apply { setMargins(0, dp(7), 0, 0) })
        sendZButton = TextView(this).apply { text = "إرسال الدوال إلى DeepSeek"; textSize = 14f; gravity = Gravity.CENTER; setTextColor(muted); typeface = android.graphics.Typeface.DEFAULT_BOLD; background = rounded(Color.rgb(29, 39, 54), 15); setPadding(0, dp(13), 0, dp(13)); isEnabled = false; setOnClickListener { runCatching { sendAllZToDeepSeek() }.onFailure { status.visibility = View.VISIBLE; status.text = "تعذر بدء الإرسال:\n${it.message.orEmpty().ifBlank { "خطأغيرمعروف" }}" } } }
        hero.addView(sendZButton, LinearLayout.LayoutParams(-1, dp(50)).apply { setMargins(0, dp(9), 0, 0) })
        content.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(18)) })
        val telegram = TextView(this).apply {
            text = "قناة editor true  •  @editortrue"; textSize = 13f; gravity = Gravity.CENTER; setTextColor(cyan);
            background = rounded(Color.rgb(13, 32, 45), 14, Color.rgb(38, 101, 125)); setPadding(dp(12), dp(11), dp(12), dp(11));
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_telegram, 0); compoundDrawablePadding = dp(8); isClickable = true
            setOnClickListener { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/editortrue"))) }.onFailure { Toast.makeText(this@MainActivity, "تعذر فتح قناة تيليجرام", Toast.LENGTH_SHORT).show() } }
        }
        content.addView(telegram, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(8)) })
        content.addView(text("© 2026 محمود عادل  |  جميع الحقوق محفوظة", 11f, muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(3), 0, dp(14)) })

        progress = ProgressBar(this).apply { visibility = View.GONE; indeterminateTintList = android.content.res.ColorStateList.valueOf(primary) }
        content.addView(progress, LinearLayout.LayoutParams(-1, dp(4)))
        status = text("", 12f, cyan).apply { gravity = Gravity.CENTER; visibility = View.GONE; background = rounded(Color.rgb(13, 32, 40), 14, Color.rgb(32, 88, 101)); setPadding(dp(14), dp(12), dp(14), dp(12)); setLineSpacing(0f, 1.15f) }
        content.addView(status, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(12), 0, dp(18)) })

        dexValue = text("0", 24f); apkValue = text("—", 13f); stringsValue = text("0", 24f); foundValue = text("0", 24f, primary)
        content.addView(text("نتائج الفحص", 20f, white).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(0, dp(24), 0, dp(9)) })
        val searchBox = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = rounded(Color.rgb(15, 23, 35), 16, Color.rgb(43, 64, 80)); setPadding(dp(14), 0, dp(8), 0) }
        searchBox.addView(text("⌕", 23f, primary), LinearLayout.LayoutParams(dp(32), -1)); search = EditText(this).apply { hint = "ابحث في التوقيع أو الكلمات..."; setHintTextColor(Color.rgb(100, 119, 137)); setTextColor(white); textSize = 14f; setSingleLine(true); background = null; layoutDirection = View.LAYOUT_DIRECTION_LTR; addTextChangedListener(SimpleWatcher { renderResults() }) }; searchBox.addView(search, LinearLayout.LayoutParams(0, dp(54), 1f)); searchBox.addView(text("×", 22f, muted).apply { setOnClickListener { search.setText("") } }, LinearLayout.LayoutParams(dp(32), -1)); content.addView(searchBox, LinearLayout.LayoutParams(-1, dp(54)).apply { setMargins(0, 0, 0, dp(12)) })
        resultsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_LTR }; content.addView(resultsBox)

        content.addView(text("تقارير DeepSeek", 19f, white).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(0, dp(24), 0, dp(3)) })
        content.addView(text("دفعات من 500 دالة مع استئناف تلقائي بعد عودة الإنترنت.", 12f, muted).apply { setPadding(0, 0, 0, dp(8)) })
        batchReportsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_RTL }; content.addView(batchReportsBox)
        noResults = text("اختر ملف APK لبدء الفحص", 14f, muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(30), 0, dp(14)) }; content.addView(noResults)
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        restoreLastReport()
    }

    private fun chooseApk() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "application/vnd.android.package-archive"; addCategory(Intent.CATEGORY_OPENABLE) }, 42) }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (requestCode == 42 && resultCode == RESULT_OK) data?.data?.let { scan(it) } }

    private fun scan(uri: Uri) {
        progress.visibility = View.VISIBLE; status.visibility = View.VISIBLE; status.text = "المرحلة 1/3: تجهيز الملف…\nبنك Premium جاهز: ${DexScanner.PREMIUM_KEYWORD_COUNT} كلمة مفتاحية"; noResults.visibility = View.GONE; resultsBox.removeAllViews()
        allResults = emptyList(); allZMethods = emptyList(); sendZButton.isEnabled = false; sendZButton.setTextColor(muted); sendZButton.text = "إرسال الدوال | اختيار الذكاء"; synchronized(liveLock) { liveResults.clear(); liveFlushScheduled = false }; scanInProgress = true; foundValue.text = "0"
        thread {
            val result = runCatching { analyze(uri, onDexStart = { file, index, total ->
                postScanProgress("المرحلة 2/3: فحص ملف DEX $index/$total\nالملف: $file\nدوال Z المكتشفة: ${liveResults.size}\nالكلمات المستخدمة: ${DexScanner.PREMIUM_KEYWORD_COUNT}")
            }, onProgress = { file, scanned, zCount ->
                if (scanned == 1 || scanned % 100 == 0) postScanProgress("المرحلة 2/3: تحليل $file\nتم فحص $scanned دالة — دوال Z: $zCount\nنتائج Premium حتى الآن: ${liveResults.size}")
            }, onZMethodFound = { method ->
                allZMethods = allZMethods + method
                Handler(Looper.getMainLooper()).post { sendZButton.text = "إرسال ${allZMethods.size} دالة | اختيار الذكاء"; sendZButton.isEnabled = true; sendZButton.setTextColor(white) }
            }) { method ->
                val live = ResultItem("${method.owner}->${method.name}${method.descriptor}", method.body, method.confidence, method.category, method.keywords)
                var shouldSchedule = false
                synchronized(liveLock) { liveResults.add(live); if (!liveFlushScheduled) { liveFlushScheduled = true; shouldSchedule = true } }
                if (shouldSchedule) flushLiveResults()
            } }
            Handler(Looper.getMainLooper()).post { scanInProgress = false; progress.visibility = View.GONE; result.onSuccess { r -> dexValue.text = r.dex.toString(); stringsValue.text = r.strings.toString(); apkValue.text = r.name; allZMethods = r.zMethods; sendZButton.text = "إرسال ${r.zMethods.size} دالة | اختيار الذكاء"; sendZButton.isEnabled = r.zMethods.isNotEmpty(); sendZButton.setTextColor(if (r.zMethods.isNotEmpty()) white else muted); allResults = r.items; foundValue.text = filteredItems(r.items).size.toString(); status.text = "المرحلة 3/3: اكتمل الفحص بنجاح\nتم تحليل ${r.dex} ملف DEX والعثور على ${r.items.size} نتيجة\nتم استخدام بنك Premium: ${DexScanner.PREMIUM_KEYWORD_COUNT} كلمة"; renderResults() }.onFailure { status.text = "فشل الفحص بعد تجهيز بنك ${DexScanner.PREMIUM_KEYWORD_COUNT} كلمة: ${it.message}" } }
        }
    }

    private fun postScanProgress(message: String) {
        synchronized(liveLock) { pendingProgressText = message; if (progressUiScheduled) return; progressUiScheduled = true }
        scanUiHandler.postDelayed({
            val text = synchronized(liveLock) { progressUiScheduled = false; pendingProgressText }
            if (scanInProgress) status.text = text
        }, 180L)
    }

    private fun flushLiveResults() {
        Handler(Looper.getMainLooper()).postDelayed({
            val snapshot = synchronized(liveLock) { liveResults.distinctBy { it.signature }.toList() }
            allResults = snapshot; foundValue.text = filteredItems(snapshot).size.toString(); noResults.visibility = View.GONE
            status.text = "المرحلة 2/3: ما زال الفحص يعمل…\nتم العثور على ${filteredItems(snapshot).size} دالة مرتبطة بـ Premium من بنك ${DexScanner.PREMIUM_KEYWORD_COUNT} كلمة"; renderResults(); if (scanInProgress) noResults.visibility = View.GONE
            synchronized(liveLock) { if (scanInProgress) { liveFlushScheduled = true; Handler(Looper.getMainLooper()).postDelayed({ flushLiveResults() }, 250L) } else liveFlushScheduled = false }
        }, 350L)
    }

    private data class ScanResult(val dex: Int, val strings: Int, val name: String, val items: List<ResultItem>, val zMethods: List<DexScanner.ZMethodResult>)
    private fun analyze(uri: Uri, onDexStart: (String, Int, Int) -> Unit = { _, _, _ -> }, onProgress: (String, Int, Int) -> Unit = { _, _, _ -> }, onZMethodFound: (DexScanner.ZMethodResult) -> Unit = {}, onMethodFound: (DexScanner.MethodResult) -> Unit = {}): ScanResult {
        val temp = java.io.File(cacheDir, "selected-analysis.apk")
        contentResolver.openInputStream(uri).use { input -> temp.outputStream().use { output -> input?.copyTo(output) } }
            val parsed = DexScanner.scanApk(temp.absolutePath, onMethodFound, onDexStart, onProgress, onZMethodFound)
        val items = parsed.methods.map { method ->
            ResultItem("${method.owner}->${method.name}${method.descriptor}", method.body, method.confidence, method.category, method.keywords)
        }
        return ScanResult(parsed.dexCount, parsed.stringsCount, queryName(uri), items, parsed.zMethods)
    }
    private fun queryName(uri: Uri): String { return runCatching { contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else "APK" } ?: "APK" }.getOrDefault("APK").removeSuffix(".apk") }

    private fun filteredItems(source: List<ResultItem> = allResults): List<ResultItem> { val q = search.text.toString().trim(); return source.filter { (q.isEmpty() || it.signature.contains(q, true)) && matchesFilter(it) } }
    private fun renderResults() {
        if (!::resultsBox.isInitialized) return
        resultsBox.removeAllViews()
        val shown = filteredItems()
        foundValue.text = shown.size.toString()
        noResults.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        if (allResults.isNotEmpty() && shown.isEmpty()) noResults.text = "لا توجد مطابقة بهذا الفلتر"
        shown.take(60).forEach { item ->
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; background = rounded(Color.rgb(17, 23, 33), 17, Color.rgb(39, 55, 69)); setPadding(dp(15), dp(13), dp(15), dp(13)); isClickable = true; setOnClickListener { showMethodDetails(item) }
            }
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            line.addView(text("✓", 18f, primary).apply { gravity = Gravity.CENTER; background = rounded(Color.rgb(29, 57, 45), 12); setPadding(dp(8), dp(4), dp(8), dp(4)) }, LinearLayout.LayoutParams(dp(34), dp(34)))
            line.addView(text(item.signature, 13f, white).apply { typeface = android.graphics.Typeface.MONOSPACE; setTextIsSelectable(true); setPadding(dp(10), 0, dp(8), 0) }, LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(text("${item.confidence}%", 12f, primary).apply { gravity = Gravity.CENTER; background = rounded(Color.rgb(27, 54, 42), 12); setPadding(dp(8), dp(6), dp(8), dp(6)) })
            box.addView(line)
            val meta = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(44), dp(9), 0, 0) }
            meta.addView(text(item.category, 11f, cyan), LinearLayout.LayoutParams(0, -2, 1f))
            meta.addView(text("${item.keywords.size} مؤشرات  ›", 11f, muted))
            box.addView(meta)
            val p = LinearLayout.LayoutParams(-1, -2); p.setMargins(0, dp(5), 0, dp(5)); resultsBox.addView(box, p)
        }
    }
    private fun showMethodDetails(item: ResultItem) {
        val owner = item.signature.substringBefore("->")
        val method = item.signature.substringAfter("->").substringBefore("(")
        val descriptor = item.signature.substringAfter("->").substringAfter(method)
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(12)); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        fun addInfo(label: String, value: String, color: Int = white) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(Color.rgb(24, 35, 50), 10); setPadding(dp(12), dp(8), dp(12), dp(8)) }
            row.addView(text(label, 11f, muted))
            row.addView(text(value.ifBlank { "غير متاح" }, 13f, color).apply { typeface = android.graphics.Typeface.MONOSPACE; setTextIsSelectable(true); setPadding(0, dp(3), 0, 0) })
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(7)) })
        }
        addInfo("CLASS / الكلاس", owner, purple)
        addInfo("FUNCTION / الدالة", method)
        addInfo("DESCRIPTOR / الوصف", descriptor, muted)
        addInfo("CATEGORY / التصنيف", item.category, Color.rgb(255, 193, 7))
        addInfo("CONFIDENCE / نسبة الثقة", "${item.confidence}%", Color.rgb(76, 210, 130))
        addInfo("MATCHED KEYWORDS / الكلمات المطابقة", item.keywords.joinToString("  •  ").ifEmpty { "لا توجد" }, Color.rgb(255, 193, 7))
        panel.addView(text("SMALI CODE / كود DEX", 11f, muted).apply { setPadding(dp(2), dp(8), dp(2), dp(6)) })
        val codeView = text(item.evidence.trim(), 12f, Color.rgb(220, 230, 240)).apply { typeface = android.graphics.Typeface.MONOSPACE; setTextIsSelectable(true); setLineSpacing(0f, 1.08f); setHorizontallyScrolling(false); setPadding(dp(16), dp(16), dp(16), dp(16)); background = rounded(Color.rgb(7, 13, 22), 10, Color.rgb(28, 91, 112)) }
        panel.addView(codeView, LinearLayout.LayoutParams(-1, -2))
        val scroll = ScrollView(this).apply { addView(panel) }
        val dialog = AlertDialog.Builder(this).setTitle("Premium · تفاصيل الدالة").setView(scroll).setNegativeButton("إغلاق", null).setNeutralButton("نسخ الكود", null).setPositiveButton("تحليل ذكي", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Smali ${item.signature}", item.evidence)); Toast.makeText(this, "تم نسخ كود الدالة", Toast.LENGTH_SHORT).show() }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { dialog.dismiss(); analyzeWithDeepSeek(item) }
        }
        dialog.show()
    }
    private fun analyzeWithDeepSeek(item: ResultItem) {
        val prefs = getSharedPreferences("deepseek", MODE_PRIVATE)
        val saved = prefs.getString("api_key", "").orEmpty().ifBlank { BuildConfig.DEEPSEEK_TOKEN }
        if (saved.isNotBlank()) { runDeepSeekAnalysis(saved, item); return }
        val input = EditText(this).apply { hint = "sk-..."; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setSingleLine(true) }
        AlertDialog.Builder(this).setTitle("رمز DeepSeek").setMessage("أدخل رمز Bearer المستخدم مع الاتصال المرفق. سيُحفظ محليًا على هذا الجهاز فقط ولن يُضمَّن داخل APK.").setView(input).setNegativeButton("إلغاء", null).setPositiveButton("حفظ وتحليل") { _, _ ->
            val key = input.text.toString().trim()
            if (key.isBlank()) Toast.makeText(this, "لم يتم إدخال مفتاح", Toast.LENGTH_SHORT).show() else { prefs.edit().putString("api_key", key).apply(); runDeepSeekAnalysis(key, item) }
        }.show()
    }
    private fun runDeepSeekAnalysis(apiKey: String, item: ResultItem) {
        val report = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(10), dp(14), dp(18)); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        renderMarkdown(report, "جاري إرسال كود الدالة إلى DeepSeek الرسمي…")
        val outputScroll = ScrollView(this).apply { isFillViewport = true; addView(report, ViewGroup.LayoutParams(-1, -2)) }
        var reportText = ""
        var pendingReport = ""
        var liveUpdateScheduled = false
        var userIsScrolling = false
        val dialog = AlertDialog.Builder(this).setTitle("تحليل DeepSeek · تقرير الدالة").setView(outputScroll).setNegativeButton("إغلاق", null).setNeutralButton("نسخ التقرير", null).create()
        dialog.show()
        outputScroll.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> userIsScrolling = true
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> userIsScrolling = false
            }
            false
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("DeepSeek report", reportText)); Toast.makeText(this, "تم نسخ التقرير كاملًا", Toast.LENGTH_SHORT).show() }
        thread {
            val result = DeepSeekClient(this@MainActivity).analyze(apiKey, item.signature, item.category, item.keywords.joinToString(", "), item.evidence) { partial ->
                Handler(Looper.getMainLooper()).post {
                    pendingReport = formatAnalysis(partial)
                    if (!liveUpdateScheduled) {
                        liveUpdateScheduled = true
                        Handler(Looper.getMainLooper()).postDelayed({
                            liveUpdateScheduled = false
                            if (userIsScrolling) return@postDelayed
                            val currentScrollY = outputScroll.scrollY
                            reportText = pendingReport
                            renderMarkdown(report, reportText)
                            outputScroll.post { outputScroll.scrollTo(0, currentScrollY.coerceAtMost((report.height - outputScroll.height).coerceAtLeast(0))) }
                        }, 120L)
                    }
                }
            }
            Handler(Looper.getMainLooper()).post {
                reportText = result.fold({ formatAnalysis(it) }, { formatDeepSeekError(it) })
                renderMarkdown(report, reportText)
            }
        }
    }
    private fun zPromptItems(): List<DeepSeekClient.BatchFunction> = allZMethods.map { DeepSeekClient.BatchFunction("${it.owner}->${it.name}${it.descriptor}", it.body) }

    private fun sendAllZToDeepSeek() {
        if (allZMethods.isEmpty()) { Toast.makeText(this, "لم يتم العثور على دوال Z", Toast.LENGTH_SHORT).show(); return }
        val prefs = getSharedPreferences("deepseek", MODE_PRIVATE)
        val token = prefs.getString("api_key", "").orEmpty().ifBlank { BuildConfig.DEEPSEEK_TOKEN }
        if (token.isBlank()) {
            val input = EditText(this).apply { hint = "Bearer token"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setSingleLine(true) }
            AlertDialog.Builder(this).setTitle("رمز DeepSeek").setMessage("أدخل رمز Bearer. سيُحفظ محليًا على هذا الجهاز فقط.").setView(input).setNegativeButton("إلغاء", null).setPositiveButton("حفظ وإرسال") { _, _ ->
                val entered = input.text.toString().trim()
                if (entered.isBlank()) Toast.makeText(this, "لم يتم إدخال مفتاح", Toast.LENGTH_SHORT).show() else { prefs.edit().putString("api_key", entered).apply(); runBatchAnalysis(entered) }
            }.show()
        } else runBatchAnalysis(token)
    }

    private fun loadAccounts(): MutableList<DeepSeekAccount> {
        val raw = accountPrefs.getString("accounts", "[]").orEmpty()
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return (0 until array.length()).mapNotNull { i -> runCatching {
            val o = array.getJSONObject(i)
            DeepSeekAccount(o.optString("id"), o.optString("email"), o.optString("token"), o.optString("deviceId"), o.optString("status", "active"), o.optString("lastError"))
        }.getOrNull() }.toMutableList()
    }

    private fun saveAccounts(accounts: List<DeepSeekAccount>) {
        val array = JSONArray()
        accounts.forEach { a -> array.put(JSONObject().apply { put("id", a.id); put("email", a.email); put("token", a.token); put("deviceId", a.deviceId); put("status", a.status); put("lastError", a.lastError) }) }
        accountPrefs.edit().putString("accounts", array.toString()).apply()
    }

    private fun updateAccount(id: String, status: String, error: String = "") {
        val updated = loadAccounts().map { if (it.id == id) it.copy(status = status, lastError = error) else it }
        saveAccounts(updated)
    }

    private fun showAccountsDialog() {
        val accounts = loadAccounts()
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(4), dp(16), dp(4)) }
        val summary = text("", 13f, cyan).apply { setPadding(0, 0, 0, dp(10)) }
        panel.addView(summary)
        fun refresh() {
            val current = loadAccounts(); val active = current.count { it.status == "active" }; val stopped = current.size - active
            summary.text = "النشطة: $active    |    المتوقفة: $stopped\nالحساب المستخدم تلقائيًا هو أول حساب نشط."
            while (panel.childCount > 1) panel.removeViewAt(1)
            current.forEach { a ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(Color.rgb(24, 35, 50), 10); setPadding(dp(10), dp(8), dp(10), dp(8)) }
                val state = if (a.status == "active") "● نشط" else "● متوقف"
                row.addView(text("$state  ${a.email.ifBlank { "حساببدونبريد" }}", 13f, if (a.status == "active") primary else Color.rgb(255, 110, 110)))
                if (a.lastError.isNotBlank()) row.addView(text(a.lastError.take(140), 11f, muted))
                val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END }
                val reset = Button(this).apply { text = "تفعيل"; setOnClickListener { updateAccount(a.id, "active"); refresh() } }
                val remove = Button(this).apply { text = "حذف"; setOnClickListener { saveAccounts(loadAccounts().filterNot { it.id == a.id }); refresh() } }
                actions.addView(reset); actions.addView(remove); row.addView(actions)
                panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(7)) })
            }
        }
        refresh()
        AlertDialog.Builder(this).setTitle("حسابات DeepSeek").setMessage("تُحفظ التوكنات محليًا على هذا الجهاز فقط. عند فشل حساب أثناء الدفعات يُجرّب أول حساب نشط بعده.").setView(panel).setNegativeButton("إغلاق", null).setPositiveButton("إضافة حساب", null).create().also { dialog ->
            dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { dialog.dismiss(); showAddAccountDialog() } }
            dialog.show()
        }
    }

    private fun showAddAccountDialog() {
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), 0) }
        val email = EditText(this).apply { hint = "البريد الإلكتروني (اختياري)"; setSingleLine(true) }
        val password = EditText(this).apply { hint = "كلمة المرور"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setSingleLine(true) }
        val device = EditText(this).apply { hint = "device_id"; setSingleLine(true); setText(defaultDeepSeekDeviceId) }
        panel.addView(email); panel.addView(password); panel.addView(device)
        AlertDialog.Builder(this).setTitle("إضافة حساب DeepSeek").setMessage("سيتم تسجيل الدخول وحفظ التوكن فقط، ولن تُحفظ كلمة المرور.").setView(panel).setNegativeButton("إلغاء", null).setPositiveButton("تسجيل وحفظ", null).create().also { dialog ->
            dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val e=email.text.toString().trim(); val pass=password.text.toString(); val d=device.text.toString().trim()
                if (pass.isBlank() || d.isBlank()) { Toast.makeText(this, "أدخل كلمة المرور وdevice_id", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                dialog.dismiss(); status.visibility=View.VISIBLE; status.text="جاري تسجيل الحساب…"
                thread { val result=DeepSeekClient(this@MainActivity).login(e, pass, d); Handler(Looper.getMainLooper()).post {
                    result.onSuccess { token -> val id=java.util.UUID.randomUUID().toString(); saveAccounts(loadAccounts()+DeepSeekAccount(id,e,token,d,"active","")); getSharedPreferences("deepseek", MODE_PRIVATE).edit().putString("api_key",token).putString("device_id",d).apply(); status.text="تم حفظ الحساب النشط"; showAccountsDialog() }.onFailure { status.text="فشل تسجيل الحساب: ${it.message.orEmpty()}" }
                } }
            } }
            dialog.show()
        }
    }

    private fun nextActiveAccount(currentId: String?): DeepSeekAccount? {
        val list=loadAccounts().filter { it.status == "active" }
        if (list.isEmpty()) return null
        val index=list.indexOfFirst { it.id == currentId }
        return list.getOrNull(if (index < 0) 0 else (index + 1) % list.size)
    }

    private var loginDialogVisible = false

    private fun showDeepSeekLogin() {
        if (loginDialogVisible || isFinishing) return
        loginDialogVisible = true
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(4), dp(18), dp(4)) }
        val email = EditText(this).apply { hint = "البريد الإلكتروني"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS; setSingleLine(true) }
        val password = EditText(this).apply { hint = "كلمة المرور (لا تُحفظ)"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setSingleLine(true) }
        val deviceId = EditText(this).apply {
            hint = "device_id"; inputType = InputType.TYPE_CLASS_TEXT; setSingleLine(true); setText(getSharedPreferences("deepseek", MODE_PRIVATE).getString("device_id", "").orEmpty().ifBlank { defaultDeepSeekDeviceId })
        }
        panel.addView(email); panel.addView(password); panel.addView(deviceId)
        val dialog = AlertDialog.Builder(this).setTitle("تسجيل دخول DeepSeek").setMessage("انتهى التوكن. أدخل بياناتك للحصول على توكن جديد. لن تُحفظ كلمة المرور.").setView(panel).setNegativeButton("لاحقًا") { _, _ -> loginDialogVisible = false }.setPositiveButton("تسجيل الدخول") { _, _ ->
            val e = email.text.toString().trim(); val pass = password.text.toString(); val device = deviceId.text.toString().trim()
            loginDialogVisible = false
            if (e.isBlank() || pass.isBlank() || device.isBlank()) { Toast.makeText(this, "أدخل البريد وكلمة المرور وdevice_id", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            status.visibility = View.VISIBLE; status.text = "جاري تسجيل الدخول إلى DeepSeek…"
            thread {
                val result = DeepSeekClient(this@MainActivity).login(e, pass, device)
                Handler(Looper.getMainLooper()).post {
                    result.onSuccess { newToken ->
                        getSharedPreferences("deepseek", MODE_PRIVATE).edit().putString("api_key", newToken).putString("device_id", device).apply()
                        reportPrefs.edit().putBoolean("auth_required", false).putString("batch_state", "تم تسجيل الدخول — استئناف التحليل").apply()
                        status.text = "تم تسجيل الدخول — استئناف التحليل داخل التطبيق"
                        runBatchAnalysis(newToken)
                    }.onFailure { status.text = "فشل تسجيل الدخول:\n${it.message.orEmpty()}" }
                }
            }
        }.create()
        dialog.setOnDismissListener { loginDialogVisible = false }
        dialog.show()
    }

    private fun runBatchAnalysis(token: String) {
        val functions = zPromptItems()
        if (functions.isEmpty()) { Toast.makeText(this, "لا توجد دوال Z للإرسال", Toast.LENGTH_SHORT).show(); return }
        val batches = functions.asReversed().chunked(500)
        reportPrefs.edit().putInt("batch_total", batches.size).putInt("batch_current", 0).putString("batch_state", "بدأ التحليل بالتتابع").apply()
        batchReportsBox.removeAllViews(); status.visibility = View.VISIBLE; status.text = "جاري تحليل الدفعات بالتتابع — الحسابات المتعددة مفعلة"
        val cards = batches.mapIndexed { index, batch -> createReportCard(batchReportsBox, index + 1, batches.size, batch.size).also { batchReportsBox.addView(it.first) } }
        val mainHandler = Handler(Looper.getMainLooper())
        thread(name = "deepseek-sequential-accounts") {
            try {
                var accounts = loadAccounts().filter { it.status == "active" }
                var account = accounts.firstOrNull { it.token == token } ?: accounts.firstOrNull()
                var activeToken = account?.token ?: token
                var chatSessionId = DeepSeekClient(this@MainActivity).createChatSession(activeToken).getOrElse { throw it }
                var successCount = 0
                batches.forEachIndexed { index, batch ->
                    val number = index + 1
                    if (!hasNetwork()) { reportPrefs.edit().putString("batch_state", "لا يوجد إنترنت — انتظار الدفعة $number").apply(); mainHandler.post { status.text = "الدفعة $number تنتظر عودة الإنترنت…" }; if (!waitForNetwork()) { reportPrefs.edit().putString("batch_state", "توقف آمن: لم تعد الشبكة خلال المهلة").apply(); mainHandler.post { status.text = "توقف الإرسال بأمان — لا يوجد اتصال بالإنترنت" }; return@forEachIndexed }; Thread.sleep(1200L) }
                    reportPrefs.edit().putInt("batch_current", number).putString("batch_state", "جاري تحليل الدفعة $number/${batches.size}").apply()
                    var chunks = 0
                    fun onChunk(partial: String) { chunks++; mainHandler.post { if (chunks == 1 || chunks % 4 == 0) { updateReportCard(cards[index].second, formatAnalysis(partial), false); status.text = "بث مباشر: الدفعة $number/${batches.size} — الحساب ${account?.email ?: "الحالي"}" } } }
                    var result = DeepSeekClient(this@MainActivity).analyzeBatch(activeToken, batch, number, batches.size, onChunk = ::onChunk, chatSessionId = chatSessionId)
                    var networkRetries = 0
                    while (result.isFailure) {
                        val error = result.exceptionOrNull()!!
                        if (isRetryableNetwork(error)) { networkRetries++; if (networkRetries > 5) { reportPrefs.edit().putString("batch_state", "توقف آمن بعد تكرار فشل الشبكة في الدفعة $number").apply(); mainHandler.post { status.text = "توقف الإرسال بأمان — يمكنك المحاولة لاحقًا" }; return@forEachIndexed }; reportPrefs.edit().putString("batch_state", "انقطع الإنترنت — استئناف الدفعة $number (محاولة $networkRetries/5)").apply(); mainHandler.post { status.text = "الدفعة $number محفوظة — ننتظر عودة الإنترنت…" }; if (!waitForNetwork()) { reportPrefs.edit().putString("batch_state", "توقف آمن: لم تعد الشبكة خلال المهلة").apply(); mainHandler.post { status.text = "توقف الإرسال بأمان — لا يوجد اتصال بالإنترنت" }; return@forEachIndexed }; Thread.sleep(1200L); result = DeepSeekClient(this@MainActivity).analyzeBatch(activeToken, batch, number, batches.size, onChunk = ::onChunk, chatSessionId = chatSessionId); continue }
                        if (!shouldSwitchAccount(error)) break
                        account?.let { updateAccount(it.id, "stopped", error.message.orEmpty()) }
                        accounts = loadAccounts().filter { it.status == "active" && it.id != account?.id }
                        val next = accounts.firstOrNull() ?: break
                        account = next; activeToken = next.token
                        reportPrefs.edit().putString("batch_state", "تم إيقاف حساب والتبديل إلى ${next.email}").apply()
                        mainHandler.post { status.text = "الحساب الحالي متوقف — التبديل إلى ${next.email}" }
                        chatSessionId = DeepSeekClient(this@MainActivity).createChatSession(activeToken).getOrNull() ?: return@forEachIndexed
                        chunks = 0
                        result = DeepSeekClient(this@MainActivity).analyzeBatch(activeToken, batch, number, batches.size, onChunk = ::onChunk, chatSessionId = chatSessionId)
                    }
                    if (result.isFailure && account != null && shouldSwitchAccount(result.exceptionOrNull()!!)) updateAccount(account!!.id, "stopped", result.exceptionOrNull()!!.message.orEmpty())
                    if (result.isSuccess) successCount++
                    val finalText = result.fold({ formatAnalysis(it) }, { formatDeepSeekError(it) }); combinedReportsAppend(finalText, number, batches.size)
                    reportPrefs.edit().putInt("batch_current", number).putString("batch_state", "اكتملت الدفعة $number/${batches.size}").apply()
                    mainHandler.post { updateReportCard(cards[index].second, finalText, true); status.text = "اكتملت الدفعة $number/${batches.size} — التالي بالتتابع" }
                }
                reportPrefs.edit().putString("batch_state", "اكتمل التحليل: $successCount/${batches.size} تقارير صالحة").apply()
                mainHandler.post { status.text = "اكتمل التحليل: ${batches.size} دفعات — صالح Premium: $successCount" }
            } catch (error: Throwable) { reportPrefs.edit().putString("batch_state", "تعذر إرسال الدفعات: ${error.message.orEmpty()}").apply(); mainHandler.post { status.text = "تعذر إرسال الدفعات دون إغلاق التطبيق:\n${error.message.orEmpty().ifBlank { "خطأغيرمعروف" }}" } }
        }
    }

    private fun shouldSwitchAccount(error: Throwable): Boolean {
        if (isRetryableNetwork(error)) return false
        val m = error.message.orEmpty().lowercase()
        return isAuthFailure(error) || m.contains("http 401") || m.contains("http 403") || m.contains("http 429") || m.contains("http 402") || m.contains("unauthorized") || m.contains("forbidden") || m.contains("rate limit") || m.contains("quota") || m.contains("token") || m.contains("account")
    }

    private fun hasNetwork(): Boolean {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = manager.activeNetwork ?: return false
            manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            @Suppress("DEPRECATION") manager.activeNetworkInfo?.isConnected == true
        }
    }

    private fun waitForNetwork(maxWaitMs: Long = 120_000L): Boolean {
        val deadline = System.currentTimeMillis() + maxWaitMs
        while (!hasNetwork() && !isFinishing && !Thread.currentThread().isInterrupted && System.currentTimeMillis() < deadline) {
            Thread.sleep(1000L)
        }
        return hasNetwork() && !isFinishing && !Thread.currentThread().isInterrupted
    }

    private fun isRetryableNetwork(error: Throwable): Boolean {
        if (isAuthFailure(error)) return false
        val message = error.message.orEmpty().lowercase()
        return message.contains("unable to resolve host") || message.contains("unknownhost") || message.contains("no address associated") ||
            message.contains("timeout") || message.contains("timed out") || message.contains("failed to connect") ||
            message.contains("connection reset") || message.contains("connection refused") || message.contains("socket") ||
            message.contains("network is unreachable") || message.contains("broken pipe") || message.contains("http 5")
    }

    private fun isAuthFailure(error: Throwable): Boolean {
        val message = error.message.orEmpty().lowercase()
        return message.contains("http 401") || message.contains("unauthorized") || message.contains("token") || message.contains("رمز")
    }

    private fun combinedReportsAppend(text: String, number: Int, total: Int) {
        val old = reportPrefs.getString("last_report", "").orEmpty()
        reportPrefs.edit().putString("last_report", old + "\n\n===== التقرير $number/$total =====\n$text").putString("last_status", "اكتمل التقرير $number/$total").apply()
    }

    private fun createReportCard(parent: LinearLayout, number: Int, total: Int, count: Int): Pair<LinearLayout, LinearLayout> {
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(12)); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(Color.rgb(24, 31, 43), 14, Color.rgb(74, 86, 102)); setPadding(dp(12), dp(10), dp(12), dp(10)) }
        card.addView(text("الدفعة $number/$total · $count دالة Z", 15f, cyan).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD })
        card.addView(text("جاري الانتظار… اضغط لعرض محتوى التقرير", 12f, muted).apply { tag = "report_status" })
        body.visibility = View.GONE
        card.addView(body)
        card.setOnClickListener { body.visibility = if (body.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        card.layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(5), 0, dp(7)) }
        return Pair(card, body)
    }

    private fun updateReportCard(body: LinearLayout, report: String, completed: Boolean) {
        val card = body.parent as? LinearLayout ?: return
        val statusView = card.findViewWithTag<TextView>("report_status")
        val verdict = reportVerdict(report)
        val color = when (verdict) { "valid" -> Color.rgb(76, 210, 130); "invalid" -> Color.rgb(255, 96, 96); else -> amber }
        card.background = rounded(Color.rgb(24, 31, 43), 14, color)
        statusView?.apply { text = when (verdict) { "valid" -> "● صالح / تحقق Premium"; "invalid" -> "● غير صالح / لا يثبت Premium"; else -> "● غير محسوم" }; setTextColor(color) }
        renderMarkdown(body, if (completed) report else "$report\n\nجاري استقبال بقية التقرير…")
    }

    private fun reportVerdict(report: String): String {
        val text = report.lowercase()
        val yes = listOf("الحكم النهائي: نعم", "تحقق premium: نعم", "هل الدالة مرتبطة بـpremium أو الشراء أو الاشتراك؟ نعم").any { text.contains(it) }
        val no = listOf("الحكم النهائي: لا", "غير صالح", "لا يثبت premium", "تحقق premium: لا").any { text.contains(it) }
        return when { yes && !no -> "valid"; no && !yes -> "invalid"; else -> "unknown" }
    }

    private fun restoreLastReport() {
        val saved = reportPrefs.getString("last_report", "").orEmpty()
        val state = reportPrefs.getString("batch_state", "").orEmpty()
        if (saved.isNotBlank()) renderSavedReportsInSection(saved)
        if (saved.isNotBlank() || state.isNotBlank()) {
            status.visibility = View.VISIBLE
            status.text = "حالة التحليل المحفوظة: ${state.ifBlank { reportPrefs.getString("last_status", "") }}\nاضغط هنا لعرض آخر التقارير"
            status.setOnClickListener { batchReportsBox.requestFocus() }
        }
    }

    private fun renderSavedReportsInSection(saved: String) {
        batchReportsBox.removeAllViews()
        saved.split(Regex("\n(?====== الدفعة )")).filter { it.isNotBlank() }.forEach { part ->
            val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(8), dp(10), dp(10)) }
            val verdict = reportVerdict(part)
            val color = when (verdict) { "valid" -> Color.rgb(76, 210, 130); "invalid" -> Color.rgb(255, 96, 96); else -> amber }
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(Color.rgb(24, 31, 43), 12, color); setPadding(dp(10), dp(8), dp(10), dp(8)) }
            val title = part.lineSequence().firstOrNull { it.contains("التقرير") } ?: "تقرير محفوظ"
            card.addView(text("$title — اضغط لعرض المحتوى", 14f, color).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD })
            body.visibility = View.GONE
            renderMarkdown(body, part.trim())
            card.addView(body)
            card.setOnClickListener { body.visibility = if (body.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
            batchReportsBox.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(8)) })
        }
    }

    private fun showSavedReports(saved: String) {
        if (saved.isBlank()) { Toast.makeText(this, "لا يوجد تقرير محفوظ بعد", Toast.LENGTH_SHORT).show(); return }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(16)); layoutDirection = View.LAYOUT_DIRECTION_RTL }
        saved.split(Regex("\n(?====== الدفعة )")).filter { it.isNotBlank() }.forEach { part ->
            val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(8), dp(10), dp(10)) }
            val color = when (reportVerdict(part)) { "valid" -> Color.rgb(76, 210, 130); "invalid" -> Color.rgb(255, 96, 96); else -> amber }
            body.background = rounded(Color.rgb(24, 31, 43), 12, color)
            renderMarkdown(body, part.trim())
            box.addView(body, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(8)) })
        }
        val scroll = ScrollView(this).apply { addView(box) }
        AlertDialog.Builder(this).setTitle("التقارير المحفوظة").setView(scroll).setPositiveButton("إغلاق", null).show()
    }

    private fun notifyReportReady(success: Boolean) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel("reports", "تقارير التحليل", NotificationManager.IMPORTANCE_DEFAULT))
        val builder = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(this, "reports") else android.app.Notification.Builder(this)
        manager.notify(2001, builder.setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("تقرير دوال Z").setContentText(if (success) "اكتمل تحليل Premium وحُفظ التقرير" else "انتهى التحليل مع خطأ وحُفظت الاستجابة").setAutoCancel(true).build())
    }

    private fun formatDeepSeekError(error: Throwable): String {
        val message = error.message.orEmpty().lowercase()
        return when {
            message.contains("unable to resolve host") || message.contains("unknownhost") || message.contains("no address associated") -> "تعذر الاتصال بـDeepSeek: لا يوجد اتصال إنترنت أو تعذر الوصول إلى الخادم."
            message.contains("timeout") || message.contains("timed out") -> "تعذر الاتصال بـDeepSeek: انتهت مهلة الاتصال."
            message.contains("http 401") || message.contains("unauthorized") -> "تعذر التحليل: رمز DeepSeek غير صالح أو منتهي."
            else -> "تعذر إكمال تحليل DeepSeek. يمكنك المحاولة مرة أخرى."
        }
    }
    private fun renderMarkdown(parent: LinearLayout, markdown: String) {
        parent.removeAllViews()
        val lines = markdown.replace("\\r\\n", "\n").replace("\\n", "\n").split('\n')
        val paragraph = StringBuilder()
        var inCode = false
        val code = StringBuilder()
        fun addParagraph() { if (paragraph.isNotBlank()) { parent.addView(text(paragraph.toString().trim(), 14f, white).apply { setLineSpacing(0f, 1.22f); setTextIsSelectable(true); setPadding(dp(4), dp(4), dp(4), dp(8)); textDirection = View.TEXT_DIRECTION_ANY_RTL }); paragraph.clear() } }
        fun addCode() {
            val codeView = text(code.toString().trimEnd(), 12f, Color.rgb(224, 235, 244)).apply { typeface = android.graphics.Typeface.MONOSPACE; setTextIsSelectable(true); setPadding(dp(14), dp(14), dp(14), dp(14)); background = rounded(Color.rgb(7, 13, 22), 10, Color.rgb(28, 91, 112)); setHorizontallyScrolling(true) }
            val horizontal = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = true; addView(codeView, ViewGroup.LayoutParams(-2, -2)) }
            parent.addView(horizontal, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(12)) }); code.clear()
        }
        lines.forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.trimStart().startsWith("```")) { if (inCode) addCode() else addParagraph(); inCode = !inCode; return@forEach }
            if (inCode) { code.append(line).append('\n'); return@forEach }
            val trimmed = line.trim()
            when {
                trimmed.isBlank() -> addParagraph()
                trimmed.matches(Regex("^#{1,6}\\s+.*")) -> { addParagraph(); parent.addView(text(trimmed.replace(Regex("^#+\\s*"), ""), 17f, primary).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(dp(4), dp(10), dp(4), dp(6)); textDirection = View.TEXT_DIRECTION_ANY_RTL }) }
                trimmed.matches(Regex("^\\d+\\)\\s+.*")) -> { addParagraph(); parent.addView(text(trimmed, 16f, primary).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD; setPadding(dp(4), dp(10), dp(4), dp(5)); textDirection = View.TEXT_DIRECTION_ANY_RTL }) }
                trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> { addParagraph(); parent.addView(text("•  " + trimmed.substring(2), 14f, white).apply { setLineSpacing(0f, 1.18f); setPadding(dp(10), dp(2), dp(4), dp(4)); setTextIsSelectable(true); textDirection = View.TEXT_DIRECTION_ANY_RTL }) }
                else -> { if (paragraph.isNotEmpty()) paragraph.append('\n'); paragraph.append(line) }
            }
        }
        if (inCode) addCode() else addParagraph()
    }
    private fun formatAnalysis(raw: String): String {
        val cleaned = raw.replace("\\r\\n", "\n").replace("\\n", "\n").trim()
            .replace(Regex("(?m)^##\\s*"), "")
            .replace(Regex("(?m)^###\\s*"), "")
        return cleaned.replace(Regex("(?m)(?<!\n)(?=(?:[1-5])\\)\\s)"), "\n")
            .replace(Regex("(?m)^\\s*[-*]\\s*"), "• ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
    private fun keywordsFor(item: ResultItem): String { val words = listOf("purchase", "purchased", "billing", "payment", "pay", "premium", "vip", "subscription", "subscribe", "product", "unlock", "order", "checkout", "paid", "owns", "ispro", "isvip", "isPurchased", "isPremium", "شراء", "اشتراك", "دفع"); return words.filter { item.signature.contains(it, true) || item.evidence.contains(it, true) }.distinct().joinToString(", ") }
    private fun matchesFilter(item: ResultItem): Boolean {
        val s = item.signature.lowercase()
        val purchaseWords = listOf("purchase", "purchased", "billing", "payment", "pay", "premium", "vip", "subscription", "subscribe", "product", "unlock", "order", "checkout", "paid", "owns", "ispro", "isvip", "شراء", "اشتراك", "دفع")
        val premiumWords = listOf("premium", "premiumuser", "premiumfeature", "premiumcontent", "premiumaccess", "premiumenabled", "premiumstatus", "premiumstate", "premiumcheck", "checkpremium", "ispremium", "haspremium", "vip", "isvip", "vipuser", "vipaccess", "vipstatus", "pro", "ispro", "haspro", "proversion", "profeature", "proaccess", "prostatus", "proenabled", "unlock", "unlockpremium", "unlockpro", "unlocked", "fullversion", "fullaccess", "paid", "paiduser", "paidversion", "removeads", "remove_ads", "adfree", "ad_free", "noads", "no_ads", "entitlement", "entitlements", "hasentitlement", "entitled", "isentitled", "entitlementstatus", "licensed", "license", "licence", "validlicense", "licensecheck", "checklicense", "activation", "activated", "activationstatus", "subscriptionstate", "subscriptionstatus", "subscriptionplan", "ispurchased", "haspurchased", "purchasevalidation", "validatepurchase", "paymentverified", "verifypayment", "productdetails", "billingclient", "querypurchases", "acknowledgepurchase", "consumepurchase", "playbilling", "googleplaybilling", "revenuecat", "customerinfo", "entitlementinfo", "qonversion", "adapty", "glassfy", "apphud", "superwall", "purchasely", "hasactivesubscription", "hasaccesslevel", "checkentitlement", "مميز", "بريميوم", "مدفوع", "عضوية", "ترخيص", "تفعيل", "نسخةكاملة", "وصولكامل", "بدوناعلانات", "ازالةالاعلانات")
        return when (filterMode) {
            "boolean" -> s.endsWith(")z")
            "premium" -> s.endsWith(")z") && (item.category == "تحقق شراء مخفي (Z)" || item.keywords.isNotEmpty() && (premiumWords.any { s.contains(it) } || item.keywords.isNotEmpty()))
            "purchase" -> (s.endsWith(")z") && purchaseWords.any { s.contains(it) }) || item.category == "تحقق شراء مخفي (Z)"
            "smart" -> s.endsWith(")z") && (purchaseWords.any { s.contains(it) } || item.category == "تحقق شراء مخفي (Z)" || looksLikeCheckMethod(s))
            "checks" -> s.endsWith(")z") && looksLikeCheckMethod(s)
            "hidden" -> item.category == "تحقق شراء مخفي (Z)"
            "billing" -> item.category == "دفع وفوترة" || listOf("billing", "payment", "pay", "purchase", "دفع").any { s.contains(it) }
            "subscription" -> item.category == "اشتراكات" || listOf("subscribe", "subscription", "اشتراك").any { s.contains(it) }
            else -> true
        }
    }
    private fun looksLikeCheckMethod(signature: String): Boolean {
        val name = signature.substringAfter("->", "").substringBefore("(")
        return name.startsWith("is") || name.startsWith("has") || name.startsWith("can") || name.startsWith("check") || name.startsWith("verify") || name.startsWith("valid") || name.startsWith("owns")
    }

    private class SimpleWatcher(val changed: () -> Unit) : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {} override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { changed() } override fun afterTextChanged(e: android.text.Editable?) {} }
}
