package com.hams.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.content.res.ColorStateList
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var search: EditText
    private var currentTab = "chats"
    private var activeChat: String? = null
    private val names = mutableListOf("سارة", "يوسف", "مريم", "آدم", "ليلى")
    private val defaultPreviews = linkedMapOf(
        "سارة" to "أهلًا، كيف حالك؟", "يوسف" to "أراك لاحقًا 👋",
        "مريم" to "أرسلت صورة 📷", "آدم" to "تمام، شكرًا", "ليلى" to "صباح الخير ✨"
    )
    private val previews = mutableMapOf<String, String>()
    private val messages = mutableMapOf<String, MutableList<Pair<Boolean, String>>>()
    private val prefs by lazy { getSharedPreferences("hams_state", MODE_PRIVATE) }
    private val purple = Color.rgb(139, 92, 246)
    private val cyan = Color.rgb(72, 224, 218)
    private val pink = Color.rgb(244, 114, 182)
    private val white = Color.WHITE
    private val muted = Color.rgb(185, 190, 220)
    private val panel = Color.rgb(35, 32, 76)
    private var accent = purple
    private var lightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadState()
        applyPreferences()
        setupMainUi()
    }

    private fun applyPreferences() {
        lightMode = prefs.getBoolean("light_mode", false)
        accent = when (prefs.getString("accent", "purple")) {
            "cyan" -> cyan
            "pink" -> pink
            "blue" -> Color.rgb(79, 140, 255)
            else -> purple
        }
        window.statusBarColor = if (lightMode) Color.rgb(242, 241, 250) else Color.rgb(14, 12, 39)
        window.navigationBarColor = if (lightMode) Color.rgb(242, 241, 250) else Color.rgb(9, 8, 27)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (lightMode) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
    }

    private fun ink(): Int = if (lightMode) Color.rgb(34, 31, 65) else white
    private fun secondaryInk(): Int = if (lightMode) Color.rgb(100, 96, 133) else muted
    private fun screenBackground(): Int = if (lightMode) Color.rgb(244, 243, 251) else Color.rgb(14, 12, 39)
    private fun panelBackground(): Int = if (lightMode) Color.WHITE else panel

    private fun setupMainUi() {
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        title = findViewById(R.id.screenTitle)
        subtitle = findViewById(R.id.screenSubtitle)
        search = findViewById(R.id.searchBox)
        findViewById<View>(R.id.root).setBackgroundColor(screenBackground())
        findViewById<TextView>(R.id.avatar).apply { background = rounded(accent, 20); setTextColor(white) }
        search.setTextColor(ink()); search.setHintTextColor(secondaryInk()); search.background = rounded(panelBackground(), 20, if(lightMode) 0xFFE5E1F1.toInt() else 0x35FFFFFF)
        findViewById<View>(R.id.bottomNav).background = rounded(panelBackground(), 25, if(lightMode) 0xFFE5E1F1.toInt() else 0x35FFFFFF)
        findViewById<TextView>(R.id.tabChats).setOnClickListener { showTab("chats") }
        findViewById<TextView>(R.id.tabStories).setOnClickListener { showTab("stories") }
        findViewById<TextView>(R.id.tabCalls).setOnClickListener { showTab("calls") }
        findViewById<TextView>(R.id.tabSettings).setOnClickListener { showTab("settings") }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { if (currentTab == "chats") renderChats(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        showTab(currentTab)
    }

    private fun showTab(tab: String) {
        activeChat = null
        currentTab = tab
        search.visibility = if (tab == "chats") View.VISIBLE else View.GONE
        val tabIds = listOf(R.id.tabChats, R.id.tabStories, R.id.tabCalls, R.id.tabSettings)
        val selected = mapOf("chats" to R.id.tabChats, "stories" to R.id.tabStories, "calls" to R.id.tabCalls, "settings" to R.id.tabSettings)[tab]
        tabIds.forEach { id ->
            findViewById<TextView>(id).apply {
                setTextColor(if (id == selected) accent else secondaryInk())
                setTypeface(typeface, if (id == selected) Typeface.BOLD else Typeface.NORMAL)
            }
        }
        title.setTextColor(ink()); subtitle.setTextColor(secondaryInk())
        when (tab) {
            "chats" -> { title.text = "المحادثات"; subtitle.text = "مساحتك الهادئة للتواصل ✦"; renderChats(search.text.toString()) }
            "stories" -> { title.text = "الحالات"; subtitle.text = "لحظات صغيرة تستحق المشاركة"; renderStories() }
            "calls" -> { title.text = "المكالمات"; subtitle.text = "ابقَ قريبًا ممن تحب"; renderCalls() }
            else -> { title.text = "الإعدادات"; subtitle.text = "اجعل همس يشبهك"; renderSettings() }
        }
    }

    private fun renderChats(query: String) {
        content.removeAllViews()
        content.addView(heroCard("همس، على مزاجك", "رسائلك ومساحتك الخاصة في تجربة بسيطة وأنيقة.", "✦"))
        content.addView(sectionLabel("آخر المحادثات", "${names.size} جهات اتصال"))
        val filtered = names.filter { it.contains(query.trim(), true) || (previews[it] ?: "").contains(query.trim(), true) }
        filtered.forEachIndexed { index, name ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_chat, content, false)
            row.background = rounded(panelBackground(), 22, if(lightMode) 0xFFE8E4F2.toInt() else 0x26FFFFFF)
            row.findViewById<TextView>(R.id.chatAvatar).apply { text = name.take(1); background = rounded(if(index % 3 == 0) accent else if(index % 3 == 1) cyan else pink, 18); setTextColor(if(index % 3 == 1) Color.rgb(18, 37, 54) else white) }
            row.findViewById<TextView>(R.id.chatName).apply { text = name; setTextColor(ink()) }
            row.findViewById<TextView>(R.id.chatPreview).apply { text = previews[name] ?: "ابدأ محادثة جديدة"; setTextColor(secondaryInk()) }
            row.findViewById<TextView>(R.id.chatTime).apply { text = "الآن"; setTextColor(secondaryInk()) }
            row.setOnClickListener { openChat(name) }
            content.addView(row)
        }
        if (filtered.isEmpty()) content.addView(emptyState("لم نعثر على شيء", "جرّب اسمًا آخر أو ابدأ محادثة جديدة."))
        content.addView(actionButton("＋    محادثة جديدة", accent).apply { setOnClickListener { createChatDialog() } })
        applyTextScale(content)
    }

    private fun createChatDialog() {
        val input = EditText(this).apply { hint = "اسم جهة الاتصال"; setTextColor(ink()); setHintTextColor(secondaryInk()) }
        AlertDialog.Builder(this).setTitle("محادثة جديدة").setView(input)
            .setPositiveButton("إضافة") { _, _ ->
                val n = input.text.toString().trim()
                if (n.isNotEmpty() && !names.contains(n)) { names.add(0, n); previews[n] = "ابدأ محادثتك الأولى ✨"; saveState(); renderChats(search.text.toString()); toast("تمت إضافة $n") }
                else if (names.contains(n)) toast("جهة الاتصال موجودة بالفعل")
            }.setNegativeButton("إلغاء", null).show().apply { styleDialog(this) }
    }

    private fun openChat(name: String) {
        activeChat = name
        setContentView(R.layout.activity_chat)
        val root = findViewById<View>(android.R.id.content)
        root.setBackgroundColor(screenBackground())
        val header = findViewById<TextView>(R.id.chatHeader)
        header.text = "$name   ·   متصل مؤخرًا"; header.setTextColor(ink())
        val back = findViewById<TextView>(R.id.chatBack); back.setTextColor(accent)
        val msgs = findViewById<LinearLayout>(R.id.messages)
        val input = findViewById<EditText>(R.id.messageInput)
        val send = findViewById<TextView>(R.id.sendButton)
        val scroll = findViewById<ScrollView>(R.id.messagesScroll)
        val composer = input.parent as View
        composer.background = rounded(panelBackground(), 25, if(lightMode) 0xFFE5E1F1.toInt() else 0x35FFFFFF)
        input.setTextColor(ink()); input.setHintTextColor(secondaryInk()); send.background = rounded(accent, 18)
        fun drawMessages() {
            msgs.removeAllViews()
            val list = messages.getOrPut(name) { mutableListOf(false to (previews[name] ?: "مرحبًا 👋")) }
            list.forEach { (mine, text) ->
                val bubble = TextView(this).apply {
                    this.text = text; setTextColor(if (mine) white else ink()); textSize = 15.5f
                    setPadding(16, 12, 16, 12); maxWidth = (resources.displayMetrics.widthPixels * .78f).toInt()
                    background = rounded(if (mine) accent else panelBackground(), 20, if (!mine && lightMode) 0xFFE5E1F1.toInt() else 0x30FFFFFF)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        gravity = if (mine) Gravity.END else Gravity.START
                        bottomMargin = dp(9); marginStart = dp(4); marginEnd = dp(4)
                    }
                }
                msgs.addView(bubble)
            }
            applyTextScale(msgs)
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
        drawMessages()
        back.setOnClickListener { activeChat = null; setupMainUi() }
        send.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                messages.getOrPut(name) { mutableListOf() }.add(true to text)
                previews[name] = text
                input.setText(""); saveState(); drawMessages()
            }
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { send.performClick(); true } else false
        }
    }

    private fun renderStories() {
        content.removeAllViews()
        content.addView(heroCard("لحظتك تبدأ هنا", "شارك حالة نصية قصيرة مع أصدقائك داخل هذه النسخة التجريبية.", "◉"))
        content.addView(actionButton("＋    إنشاء حالة نصية", accent).apply { setOnClickListener { createStoryDialog() } })
        val story = prefs.getString("my_story", null)
        if (!story.isNullOrBlank()) content.addView(cardText("حالتك الحالية", story, "✦"))
        content.addView(sectionLabel("أصدقاؤك", "${names.size} حالات تجريبية"))
        names.forEachIndexed { i, n -> content.addView(cardText(n, previews[n] ?: "متاح للمراسلة", listOf("◉", "✦", "○")[i % 3])) }
        applyTextScale(content)
    }

    private fun createStoryDialog() {
        val input = EditText(this).apply { hint = "اكتب شيئًا جميلًا…"; setTextColor(ink()); setHintTextColor(secondaryInk()) }
        AlertDialog.Builder(this).setTitle("حالة جديدة").setView(input)
            .setPositiveButton("نشر") { _, _ -> val value = input.text.toString().trim(); if (value.isNotEmpty()) { prefs.edit().putString("my_story", value).apply(); renderStories(); toast("تم حفظ الحالة على جهازك") } }
            .setNegativeButton("إلغاء", null).show().apply { styleDialog(this) }
    }

    private fun renderCalls() {
        content.removeAllViews()
        content.addView(heroCard("اتصال بنقرة واحدة", "هذه الواجهة تجريبية؛ لا تُجرى مكالمات فعلية دون خدمة اتصال مخصصة.", "⌕"))
        content.addView(sectionLabel("جهات الاتصال", "${names.size} أشخاص"))
        names.forEach { n -> content.addView(actionButton("☎    $n", accent).apply { setOnClickListener { AlertDialog.Builder(this@MainActivity).setTitle("مكالمة $n").setMessage("المكالمات الصوتية والمرئية غير مفعّلة بعد في نسخة Hams التجريبية.").setPositiveButton("حسنًا", null).show().apply { styleDialog(this) } } }) }
        applyTextScale(content)
    }

    private fun renderSettings() {
        content.removeAllViews()
        content.addView(heroCard("همس · Hams", "نسخة تجريبية أصلية بواجهة عربية، مصممة لتكون خفيفة ومتناسقة.", "هـ"))
        content.addView(sectionLabel("المظهر", "خصّص الواجهة"))
        content.addView(actionButton(if (lightMode) "☼    المظهر الفاتح — اضغط للتبديل" else "☾    المظهر الداكن — اضغط للتبديل", accent).apply {
            setOnClickListener { lightMode = !lightMode; prefs.edit().putBoolean("light_mode", lightMode).apply(); applyPreferences(); setupMainUi(); showTab("settings") }
        })
        content.addView(actionButton("✦    لون الواجهة", accent).apply { setOnClickListener { chooseAccent() } })
        content.addView(actionButton("Aa    حجم النص", accent).apply { setOnClickListener { chooseTextSize() } })
        content.addView(sectionLabel("الخصوصية والبيانات", "إدارة محلية"))
        content.addView(actionButton("♧    تصدير نسخة احتياطية نصية", accent).apply { setOnClickListener { exportBackup() } })
        content.addView(actionButton("⌫    مسح بيانات التطبيق", Color.rgb(229, 80, 100)).apply {
            setOnClickListener { AlertDialog.Builder(this@MainActivity).setTitle("مسح البيانات المحلية؟").setMessage("سيتم حذف المحادثات والحالات المحفوظة داخل هذا التطبيق فقط.")
                .setPositiveButton("مسح البيانات") { _, _ ->
                    names.clear(); names.addAll(defaultPreviews.keys); previews.clear(); previews.putAll(defaultPreviews); messages.clear(); prefs.edit().remove("my_story").remove("names").remove("previews").remove("messages").apply(); saveState(); showTab("chats"); toast("تمت إعادة البيانات التجريبية")
                }.setNegativeButton("إلغاء", null).show().apply { styleDialog(this) } }
        })
        content.addView(cardText("مهم أن تعرف", "الرسائل والحالات محفوظة محليًا على هذا الجهاز. لا يوجد خادم أو تسجيل دخول أو تشفير طرفي أو إرسال عبر الإنترنت حتى الآن.", "ⓘ"))
        content.addView(cardText("الإصدار", "Hams Native · 1.1\nواجهة Android أصلية باستخدام Kotlin وXML.", "✦"))
        applyTextScale(content)
    }

    private fun chooseAccent() {
        val labels = arrayOf("بنفسجي زجاجي", "تركواز", "وردي", "أزرق")
        val values = arrayOf("purple", "cyan", "pink", "blue")
        val current = prefs.getString("accent", "purple")
        val checked = values.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("لون الواجهة").setSingleChoiceItems(labels, checked) { dialog, which ->
            prefs.edit().putString("accent", values[which]).apply(); applyPreferences(); setupMainUi(); showTab("settings"); dialog.dismiss()
        }.setNegativeButton("إلغاء", null).show().apply { styleDialog(this) }
    }

    private fun chooseTextSize() {
        val options = arrayOf("صغير", "متوسط", "كبير")
        val current = prefs.getInt("text_size", 1)
        AlertDialog.Builder(this).setTitle("حجم النص").setSingleChoiceItems(options, current) { dialog, which ->
            prefs.edit().putInt("text_size", which).apply(); dialog.dismiss(); setupMainUi(); showTab("settings"); toast("تم تحديث حجم النص")
        }.setNegativeButton("إلغاء", null).show().apply { styleDialog(this) }
    }

    private fun exportBackup() {
        val out = JSONObject().put("app", "Hams").put("names", JSONArray(names)).put("previews", JSONObject().apply { previews.forEach { (k, v) -> put(k, v) } })
        val msgObj = JSONObject()
        messages.forEach { (key, list) -> msgObj.put(key, JSONArray().apply { list.forEach { (mine, text) -> put(JSONObject().put("mine", mine).put("text", text)) } }) }
        out.put("messages", msgObj).put("story", prefs.getString("my_story", ""))
        val scroll = ScrollView(this)
        val text = TextView(this).apply { this.text = out.toString(2); setTextColor(ink()); textSize = 12f; setPadding(dp(12), dp(12), dp(12), dp(12)) }
        scroll.addView(text)
        AlertDialog.Builder(this).setTitle("نسخة احتياطية JSON").setView(scroll).setPositiveButton("نسخ النص") { _, _ ->
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Hams backup", out.toString(2))); toast("تم نسخ النسخة الاحتياطية")
        }.setNegativeButton("إغلاق", null).show().apply { styleDialog(this) }
    }

    private fun heroCard(head: String, body: String, symbol: String): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(if (lightMode) Color.rgb(235, 229, 255) else Color.rgb(54, 44, 107), if (lightMode) Color.rgb(222, 240, 248) else Color.rgb(28, 64, 88))).apply { cornerRadius = dp(25).toFloat() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) }
        }
        val badge = TextView(this).apply { text = symbol; textSize = 26f; gravity = Gravity.CENTER; setTextColor(white); background = rounded(accent, 20); layoutParams = LinearLayout.LayoutParams(dp(52), dp(52)) }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(14) } }
        texts.addView(TextView(this).apply { text = head; textSize = 18f; setTypeface(typeface, Typeface.BOLD); setTextColor(ink()) })
        texts.addView(TextView(this).apply { text = body; textSize = 12.5f; setTextColor(secondaryInk()); setPadding(0, dp(5), 0, 0) })
        box.addView(texts); box.addView(badge)
        return box
    }

    private fun sectionLabel(head: String, detail: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(3), dp(3), dp(3), dp(10))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) }
        addView(TextView(this@MainActivity).apply { text = head; textSize = 16f; setTypeface(typeface, Typeface.BOLD); setTextColor(ink()); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        addView(TextView(this@MainActivity).apply { text = detail; textSize = 11f; setTextColor(secondaryInk()) })
    }

    private fun cardText(head: String, body: String, symbol: String = "✦"): View {
        val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(15), dp(14), dp(15), dp(14)); background = rounded(panelBackground(), 22, if(lightMode) 0xFFE8E4F2.toInt() else 0x26FFFFFF); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) } }
        box.addView(TextView(this).apply { text = symbol; textSize = 20f; gravity = Gravity.CENTER; setTextColor(accent); layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)) })
        val stack = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(10) } }
        stack.addView(TextView(this).apply { text = head; textSize = 15f; setTypeface(typeface, Typeface.BOLD); setTextColor(ink()) })
        stack.addView(TextView(this).apply { text = body; textSize = 12.5f; setTextColor(secondaryInk()); setPadding(0, dp(4), 0, 0) })
        box.addView(stack); return box
    }

    private fun emptyState(head: String, body: String): View = cardText(head, body, "⌕")

    private fun actionButton(text: String, color: Int): TextView = TextView(this).apply {
        this.text = text; textSize = 14f; setTextColor(ink()); gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16)); background = rounded(panelBackground(), 20, Color.argb(75, Color.red(color), Color.green(color), Color.blue(color)))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
        compoundDrawablePadding = dp(8)
    }

    private fun rounded(color: Int, radiusDp: Int, strokeColor: Int = Color.TRANSPARENT): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != Color.TRANSPARENT) setStroke(dp(1), strokeColor)
    }

    private fun styleDialog(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawable(rounded(panelBackground(), 22))
    }

    private fun label(text: String, size: Int, color: Int, top: Int): TextView = TextView(this).apply { this.text = text; textSize = size.toFloat(); setTextColor(color); setPadding(dp(4), dp(top), dp(4), dp(12)) }
    private fun applyTextScale(root: View) {
        val factor = when (prefs.getInt("text_size", 1)) { 0 -> 0.92f; 2 -> 1.12f; else -> 1f }
        fun visit(view: View) {
            if (view is TextView) {
                val base = view.tag as? Float
                if (base == null) view.tag = view.textSize / resources.displayMetrics.scaledDensity
                val original = view.tag as? Float ?: (view.textSize / resources.displayMetrics.scaledDensity)
                view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, original * factor)
            }
            if (view is ViewGroup) for (i in 0 until view.childCount) visit(view.getChildAt(i))
        }
        visit(root)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    private fun saveState() {
        val namesJson = JSONArray(names)
        val previewsJson = JSONObject(); previews.forEach { (k, v) -> previewsJson.put(k, v) }
        val messagesJson = JSONObject()
        messages.forEach { (k, v) -> val arr = JSONArray(); v.forEach { (mine, txt) -> arr.put(JSONObject().put("mine", mine).put("text", txt)) }; messagesJson.put(k, arr) }
        prefs.edit().putString("names", namesJson.toString()).putString("previews", previewsJson.toString()).putString("messages", messagesJson.toString()).apply()
    }

    private fun loadState() {
        previews.putAll(defaultPreviews)
        try {
            prefs.getString("names", null)?.let { val a = JSONArray(it); names.clear(); for (i in 0 until a.length()) names.add(a.getString(i)) }
            prefs.getString("previews", null)?.let { val o = JSONObject(it); o.keys().forEach { k -> previews[k] = o.getString(k) } }
            prefs.getString("messages", null)?.let { val o = JSONObject(it); o.keys().forEach { k -> val arr = o.getJSONArray(k); val list = mutableListOf<Pair<Boolean, String>>(); for (i in 0 until arr.length()) { val m = arr.getJSONObject(i); list.add(m.getBoolean("mine") to m.getString("text")) }; messages[k] = list } }
        } catch (_: Exception) { /* Keep defaults if a saved value is invalid. */ }
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (activeChat != null) { activeChat = null; setupMainUi() } else if (currentTab != "chats") showTab("chats") else super.onBackPressed()
    }
}
