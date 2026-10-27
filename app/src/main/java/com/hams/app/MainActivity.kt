package com.hams.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
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
    private val previews = mutableMapOf(
        "سارة" to "أهلًا، كيف حالك؟", "يوسف" to "أراك لاحقًا 👋",
        "مريم" to "أرسلت صورة", "آدم" to "تمام، شكرًا", "ليلى" to "صباح الخير ✨"
    )
    private val messages = mutableMapOf<String, MutableList<Pair<Boolean, String>>>()
    private val bg = Color.rgb(20, 17, 60)
    private val white = Color.WHITE
    private val muted = Color.rgb(191, 196, 233)
    private val prefs by lazy { getSharedPreferences("hams_state", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        content = findViewById(R.id.content)
        title = findViewById(R.id.screenTitle)
        subtitle = findViewById(R.id.screenSubtitle)
        search = findViewById(R.id.searchBox)
        loadState()
        findViewById<TextView>(R.id.tabChats).setOnClickListener { showTab("chats") }
        findViewById<TextView>(R.id.tabStories).setOnClickListener { showTab("stories") }
        findViewById<TextView>(R.id.tabCalls).setOnClickListener { showTab("calls") }
        findViewById<TextView>(R.id.tabSettings).setOnClickListener { showTab("settings") }
        search.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { if(currentTab=="chats") renderChats(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) {}
        })
        showTab("chats")
    }

    private fun showTab(tab: String) {
        activeChat = null
        currentTab = tab
        search.visibility = if(tab == "chats") View.VISIBLE else View.GONE
        val tabs = listOf(R.id.tabChats, R.id.tabStories, R.id.tabCalls, R.id.tabSettings)
        val selected = mapOf("chats" to R.id.tabChats, "stories" to R.id.tabStories, "calls" to R.id.tabCalls, "settings" to R.id.tabSettings)[tab]
        tabs.forEach { findViewById<TextView>(it).setTextColor(if(it == selected) white else muted) }
        when(tab) {
            "chats" -> { title.text = "المحادثات"; subtitle.text = "تواصل بأناقة"; renderChats(search.text.toString()) }
            "stories" -> { title.text = "الحالات"; subtitle.text = "آخر التحديثات"; renderStories() }
            "calls" -> { title.text = "المكالمات"; subtitle.text = "ابقَ على تواصل"; renderCalls() }
            else -> { title.text = "الإعدادات"; subtitle.text = "خصّص تجربتك"; renderSettings() }
        }
    }

    private fun renderChats(query: String) {
        content.removeAllViews()
        names.filter { it.contains(query.trim(), true) }.forEach { name ->
            val row = LayoutInflater.from(this).inflate(R.layout.item_chat, content, false)
            row.findViewById<TextView>(R.id.chatAvatar).text = name.take(1)
            row.findViewById<TextView>(R.id.chatName).text = name
            row.findViewById<TextView>(R.id.chatPreview).text = previews[name] ?: "لا توجد رسائل"
            row.findViewById<TextView>(R.id.chatTime).text = "الآن"
            row.setOnClickListener { openChat(name) }
            content.addView(row)
        }
        if (content.childCount == 0) content.addView(label("لا توجد محادثات مطابقة", 15, muted, 24))
        val add = actionButton("＋  محادثة جديدة")
        add.setOnClickListener {
            val input = EditText(this); input.hint = "اسم جهة الاتصال"
            android.app.AlertDialog.Builder(this).setTitle("محادثة جديدة").setView(input)
                .setPositiveButton("إضافة") { _, _ ->
                    val n = input.text.toString().trim()
                    if(n.isNotEmpty() && !names.contains(n)) { names.add(0,n); previews[n]="لا توجد رسائل"; saveState(); renderChats(search.text.toString()) }
                }.setNegativeButton("إلغاء", null).show()
        }
        content.addView(add)
    }

    private fun openChat(name: String) {
        activeChat = name
        val view = LayoutInflater.from(this).inflate(R.layout.activity_chat, null, false)
        setContentView(view)
        view.findViewById<TextView>(R.id.chatHeader).text = name
        val msgs = view.findViewById<LinearLayout>(R.id.messages)
        val input = view.findViewById<EditText>(R.id.messageInput)
        val scroll = view.findViewById<ScrollView>(R.id.messagesScroll)
        fun drawMessages() {
            msgs.removeAllViews()
            val list = messages.getOrPut(name) { mutableListOf(false to (previews[name] ?: "مرحبًا 👋")) }
            list.forEach { (mine, text) ->
                val bubble = TextView(this).apply {
                    this.text = text; setTextColor(white); textSize = 16f
                    setPadding(16, 10, 16, 10); maxWidth = (resources.displayMetrics.widthPixels * .76f).toInt()
                    background = getDrawable(if(mine) R.drawable.bg_accent else R.drawable.bg_glass)
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        gravity = if(mine) Gravity.END else Gravity.START
                        bottomMargin = 8
                    }
                }
                msgs.addView(bubble)
            }
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
        drawMessages()
        view.findViewById<TextView>(R.id.chatBack).setOnClickListener { setContentView(R.layout.activity_main); recreate() }
        view.findViewById<TextView>(R.id.sendButton).setOnClickListener {
            val text = input.text.toString().trim()
            if(text.isNotEmpty()) {
                messages.getOrPut(name) { mutableListOf() }.add(true to text)
                previews[name] = text
                input.setText(""); saveState(); drawMessages()
            }
        }
    }

    private fun renderStories() {
        content.removeAllViews()
        content.addView(label("حالتك", 20, white, 8))
        content.addView(actionButton("＋  أضف حالة نصية").apply {
            setOnClickListener {
                val input = EditText(this@MainActivity); input.hint = "اكتب حالتك…"
                android.app.AlertDialog.Builder(this@MainActivity).setTitle("حالة جديدة").setView(input)
                    .setPositiveButton("نشر") { _, _ ->
                        val t = input.text.toString().trim()
                        if(t.isNotEmpty()) { prefs.edit().putString("my_story",t).apply(); renderStories() }
                    }.setNegativeButton("إلغاء",null).show()
            }
        })
        val story = prefs.getString("my_story", null)
        if(!story.isNullOrBlank()) content.addView(cardText("حالتك المنشورة", story))
        names.forEach { content.addView(cardText(it, previews[it] ?: "متاح للمراسلة")) }
    }

    private fun renderCalls() {
        content.removeAllViews()
        content.addView(cardText("المكالمات", "سجل المكالمات غير متصل بخدمة اتصال حقيقية في هذه النسخة."))
        names.forEach { n -> content.addView(actionButton("☎  $n").apply { setOnClickListener { toast("ميزة الاتصال الحقيقي تحتاج إلى خدمة اتصال وخادم.") } }) }
    }

    private fun renderSettings() {
        content.removeAllViews()
        content.addView(cardText("الملف الشخصي", "همس · Hams"))
        content.addView(actionButton("المظهر: ليلي بنفسجي").apply { setOnClickListener { toast("ألوان النسخة الأساسية ثابتة حاليًا.") } })
        content.addView(actionButton("اللغة: العربية").apply { setOnClickListener { toast("سيتم توسيع إعدادات اللغات في المرحلة التالية.") } })
        content.addView(actionButton("مسح بيانات المحادثات").apply {
            setOnClickListener { android.app.AlertDialog.Builder(this@MainActivity).setMessage("هل تريد مسح المحادثات المحفوظة؟")
                .setPositiveButton("مسح") { _, _ -> messages.clear(); previews.clear(); names.clear(); names.addAll(listOf("سارة","يوسف","مريم","آدم","ليلى")); names.forEach { previews[it]="مرحبًا 👋" }; saveState(); showTab("chats") }
                .setNegativeButton("إلغاء",null).show() }
        })
        content.addView(cardText("ملاحظة", "هذه نسخة أولية Native باستخدام XML وKotlin. الرسائل محلية على الجهاز وليست دردشة عبر الإنترنت."))
    }

    private fun label(text: String, size: Int, color: Int, top: Int): TextView =
        TextView(this).apply { this.text=text; textSize=size.toFloat(); setTextColor(color); setPadding(4, top, 4, 12) }

    private fun cardText(head: String, body: String): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(16,14,16,14)
            background = getDrawable(R.drawable.bg_glass)
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 }
        }
        box.addView(label(head,16,white,0).apply { setTypeface(typeface,Typeface.BOLD) })
        box.addView(label(body,14,muted,4))
        return box
    }

    private fun actionButton(text: String): TextView = TextView(this).apply {
        this.text=text; textSize=15f; setTextColor(white); gravity=Gravity.CENTER_VERTICAL
        setPadding(18,14,18,14); background=getDrawable(R.drawable.bg_glass)
        layoutParams=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=10 }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    private fun saveState() {
        val namesJson = JSONArray(names)
        val previewsJson = JSONObject()
        previews.forEach { (k,v) -> previewsJson.put(k,v) }
        val messagesJson = JSONObject()
        messages.forEach { (k,v) ->
            val arr = JSONArray()
            v.forEach { (mine,txt) -> arr.put(JSONObject().put("mine",mine).put("text",txt)) }
            messagesJson.put(k,arr)
        }
        prefs.edit().putString("names",namesJson.toString()).putString("previews",previewsJson.toString())
            .putString("messages",messagesJson.toString()).apply()
    }

    private fun loadState() {
        try {
            val ns = prefs.getString("names", null)
            if(ns != null) { names.clear(); val a=JSONArray(ns); for(i in 0 until a.length()) names.add(a.getString(i)) }
            val ps = prefs.getString("previews", null)
            if(ps != null) { previews.clear(); val o=JSONObject(ps); o.keys().forEach { k -> previews[k]=o.getString(k) } }
            val ms = prefs.getString("messages", null)
            if(ms != null) { val o=JSONObject(ms); o.keys().forEach { k ->
                val arr=o.getJSONArray(k); val list=mutableListOf<Pair<Boolean,String>>()
                for(i in 0 until arr.length()) { val m=arr.getJSONObject(i); list.add(m.getBoolean("mine") to m.getString("text")) }
                messages[k]=list
            } }
        } catch (_: Exception) { }
    }
}
