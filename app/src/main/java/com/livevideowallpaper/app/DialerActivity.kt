package com.livevideowallpaper.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Dialer with keypad, recents and contacts, drawn over a video or photo background. */
class DialerActivity : BaseActivity() {

    private lateinit var bg: BackgroundHost
    private lateinit var txtNumber: TextView
    private lateinit var search: EditText
    private lateinit var empty: TextView
    private lateinit var panelKeypad: View
    private lateinit var panelList: View
    private lateinit var banner: View
    private lateinit var tabs: List<TextView>
    private lateinit var adapter: EntryAdapter

    private val number = StringBuilder()
    private var tab = 0
    private var all: List<Entry> = emptyList()
    private var pendingNumber: String? = null

    private val perms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { loadList() }
    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateBanner() }
    private val callPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingNumber?.let { placeCall(it) }
        pendingNumber = null
    }

    private val keys = listOf(
        "1" to "", "2" to "ABC", "3" to "DEF", "4" to "GHI", "5" to "JKL", "6" to "MNO",
        "7" to "PQRS", "8" to "TUV", "9" to "WXYZ", "*" to "", "0" to "+", "#" to ""
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dialer)
        Screen.edgeToEdge(this, findViewById(R.id.content))

        bg = BackgroundHost(findViewById<FrameLayout>(R.id.bgHost))
        txtNumber = findViewById(R.id.txtNumber)
        search = findViewById(R.id.search)
        empty = findViewById(R.id.txtEmpty)
        panelKeypad = findViewById(R.id.panelKeypad)
        panelList = findViewById(R.id.panelList)
        banner = findViewById(R.id.bannerDefault)
        tabs = listOf(
            findViewById<TextView>(R.id.tabKeypad),
            findViewById<TextView>(R.id.tabRecents),
            findViewById<TextView>(R.id.tabContacts)
        )

        adapter = EntryAdapter { placeCall(it.number) }
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@DialerActivity)
            adapter = this@DialerActivity.adapter
        }
        search.doAfterTextChanged { applyFilter() }

        buildKeypad(findViewById(R.id.keypad))
        findViewById<View>(R.id.btnCall).setOnClickListener { placeCall(number.toString()) }
        findViewById<View>(R.id.btnBack).apply {
            setOnClickListener { if (number.isNotEmpty()) { number.deleteCharAt(number.length - 1); showNumber() } }
            setOnLongClickListener { number.setLength(0); showNumber(); true }
        }
        tabs.forEachIndexed { i, t -> t.setOnClickListener { selectTab(i) } }
        findViewById<View>(R.id.btnCustomize).setOnClickListener {
            startActivity(Intent(this, CustomizeActivity::class.java))
        }
        banner.setOnClickListener {
            DefaultDialer.requestIntent(this)?.let { roleLauncher.launch(it) } ?: say("Not available on this phone")
        }

        handleIntent(intent)
        selectTab(0)

        val req = ArrayList<String>()
        req.add(Manifest.permission.READ_CONTACTS)
        req.add(Manifest.permission.READ_CALL_LOG)
        req.add(Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= 33) req.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = req.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) perms.launch(missing.toTypedArray())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        val data = i?.data ?: return
        if (data.scheme == "tel") {
            number.setLength(0)
            number.append(Uri.decode(data.schemeSpecificPart ?: ""))
            showNumber()
            selectTab(0)
        }
    }

    override fun onStart() {
        super.onStart()
        bg.start(Prefs.bgPath(this, Prefs.SLOT_DIALER))
    }

    override fun onStop() {
        super.onStop()
        bg.release()
    }

    override fun onResume() {
        super.onResume()
        updateBanner()
        loadList()
    }

    private fun updateBanner() {
        banner.visibility = if (DefaultDialer.isDefault(this)) View.GONE else View.VISIBLE
    }

    private fun showNumber() {
        txtNumber.text = number
    }

    private fun buildKeypad(host: LinearLayout) {
        for (r in 0 until 4) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER
            for (c in 0 until 3) {
                val (d, l) = keys[r * 3 + c]
                row.addView(key(d, l))
            }
            host.addView(row)
        }
    }

    private fun key(d: String, l: String): TextView {
        val s = SpannableString(if (l.isEmpty()) d else "$d\n$l")
        s.setSpan(RelativeSizeSpan(2.3f), 0, d.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val t = TextView(this)
        t.text = s
        t.textSize = 11f
        t.setTextColor(0xFFFFFFFF.toInt())
        t.gravity = Gravity.CENTER
        t.setBackgroundResource(R.drawable.bg_key)
        t.isClickable = true
        t.layoutParams = LinearLayout.LayoutParams(dp(76), dp(76)).apply { setMargins(dp(8), dp(5), dp(8), dp(5)) }
        t.setOnClickListener { number.append(d); showNumber() }
        if (d == "0") t.setOnLongClickListener { number.append('+'); showNumber(); true }
        return t
    }

    private fun selectTab(i: Int) {
        tab = i
        panelKeypad.visibility = if (i == 0) View.VISIBLE else View.GONE
        panelList.visibility = if (i == 0) View.GONE else View.VISIBLE
        tabs.forEachIndexed { idx, t ->
            t.setTextColor(if (idx == i) 0xFFFFFFFF.toInt() else 0x99FFFFFF.toInt())
            t.setTypeface(null, if (idx == i) Typeface.BOLD else Typeface.NORMAL)
        }
        loadList()
    }

    private fun loadList() {
        if (tab == 0) return
        val granted = if (tab == 1) ContactsRepo.canReadLog(this) else ContactsRepo.canReadContacts(this)
        if (!granted) {
            all = emptyList()
            applyFilter()
            empty.text = "Allow permission to see this list"
            return
        }
        val which = tab
        Bg.io.execute {
            val l = if (which == 1) ContactsRepo.recents(this) else ContactsRepo.contacts(this)
            runOnUiThread {
                if (isFinishing || isDestroyed || tab != which) return@runOnUiThread
                all = l
                empty.text = "Nothing here yet"
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val q = search.text.toString().trim().lowercase()
        val l = if (q.isEmpty()) all else all.filter { it.name.lowercase().contains(q) || it.number.contains(q) }
        adapter.submit(l)
        empty.visibility = if (l.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun placeCall(n: String) {
        if (n.isBlank()) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pendingNumber = n
            callPerm.launch(Manifest.permission.CALL_PHONE)
            return
        }
        try {
            getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("tel", n, null), Bundle())
        } catch (e: SecurityException) {
            say("Call permission is missing")
        } catch (e: Exception) {
            say("Could not start the call")
        }
    }
}
