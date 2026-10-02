package com.example.mapphim

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

fun keyNameOf(c: Int): String {
    val r = KeyEvent.keyCodeToString(c).removePrefix("KEYCODE_")
    return when (r) {
        "CTRL_LEFT", "CTRL_RIGHT" -> "Ctrl"
        "SHIFT_LEFT", "SHIFT_RIGHT" -> "Shift"
        "ALT_LEFT", "ALT_RIGHT" -> "Alt"
        "DPAD_UP" -> "↔Up"; "DPAD_DOWN" -> "↔Down"; "DPAD_LEFT" -> "↔Left"; "DPAD_RIGHT" -> "↔Right"
        "CAPS_LOCK" -> "CapsLock"
        else -> if (r.length == 1) r else r.lowercase().replaceFirstChar { it.uppercase() }
    }
}

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var canvas: MapCanvas
    private lateinit var navs: List<TextView>
    private lateinit var pages: List<View>
    private var dialog: Dialog? = null
    private var profiles = JSONObject()
    private var cur = "Profile 1"
    private var items = mutableListOf<KItem>()
    private var sel: KItem? = null
    private var cap = -1
    private var nextId = 1
    private var updating = false
    private val defs = listOf(
        Triple("tap", 7f, listOf("E")), Triple("hold", 7f, listOf("Shift")), Triple("toggle", 7f, listOf("C")),
        Triple("joy", 16f, listOf("W", "A", "S", "D")), Triple("look", 34f, listOf<String>()), Triple("scope", 9f, listOf("Mouse2")))

    private fun <T : View> v(id: Int): T = findViewById(id)
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun click(id: Int, f: (View) -> Unit) = v<View>(id).setOnClickListener(f)
    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun seek(id: Int, f: (Int) -> Unit) = v<SeekBar>(id).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) { if (user) f(p) }
        override fun onStartTrackingTouch(s: SeekBar?) {}
        override fun onStopTrackingTouch(s: SeekBar?) {}
    })

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = getSharedPreferences("mapphim", MODE_PRIVATE)
        setContentView(R.layout.activity_main)
        canvas = v(R.id.canvas)
        navs = listOf(R.id.nav0, R.id.nav1, R.id.nav2).map { v<TextView>(it) }
        pages = listOf(R.id.page0, R.id.page1, R.id.page2).map { v<View>(it) }
        navs.forEachIndexed { i, n -> n.setOnClickListener { showPage(i) } }
        canvas.onSelect = { sel = it; cap = -1; drawEditor(); drawList() }
        canvas.onChange = { drawEditor() }
        v<TextView>(R.id.tvAbout).text = "Map Phím ${BuildConfig.VERSION_NAME}\n\n" +
            "1. Bật dịch vụ \"Map Phím\" trong Trợ năng.\n2. Kéo các phím trên sơ đồ cho khớp HUD của game.\n" +
            "3. Bấm Lưu rồi Bắt đầu, cắm bàn phím và chuột OTG hoặc Bluetooth.\n4. Nhấn F1 trong game để tạm dừng hoặc tiếp tục."
        loadBg()
        loadProfiles()
        updateGameBtn()
        showPage(0)
        click(R.id.btnGame) { gameMenu(it) }
        click(R.id.btnProfile) { profileMenu(it) }
        click(R.id.btnSave) { save() }
        click(R.id.btnReset) { loadItems(cur); toast("Đã hoàn tác thay đổi chưa lưu") }
        click(R.id.btnStart) { start() }
        click(R.id.btnBg) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/*" }, 2)
        }
        click(R.id.btnAdd) { addMenu(it) }
        click(R.id.btnDel) {
            sel?.let { s -> items.remove(s); sel = null; canvas.selected = null; canvas.invalidate(); drawEditor(); drawList() }
        }
        click(R.id.btnAcc) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        click(R.id.btnLicNew) { Auth.logout(this); refresh(); if (Auth.online()) showActivation() else toast("Đang ở chế độ thử, chưa nối máy chủ.") }
        v<EditText>(R.id.etLabel).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!updating) { sel?.label = s.toString(); canvas.invalidate(); drawList() }
            }
        })
        seek(R.id.seekSize) { p -> sel?.s = 3f + p; v<TextView>(R.id.tvSize).text = "${3 + p}%"; canvas.invalidate() }
        seek(R.id.seekLook) { p -> sel?.sens = 5 + p; v<TextView>(R.id.tvLook).text = "${5 + p}" }
        val m = prefs.getFloat("sens_mul", 1f)
        v<SeekBar>(R.id.seekSens).apply { max = 150; progress = ((m - 0.5f) * 100).toInt().coerceIn(0, 150) }
        v<TextView>(R.id.tvSens).text = "${(m * 100).toInt()}"
        seek(R.id.seekSens) { p ->
            val mm = 0.5f + p / 100f
            v<TextView>(R.id.tvSens).text = "${(mm * 100).toInt()}"
            prefs.edit().putFloat("sens_mul", mm).apply()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!Auth.online()) Auth.useTrial(this)
        refresh()
        if (Auth.online()) {
            if (Auth.user(this) == null) showActivation()
            else Auth.verify(this) { ok -> if (!ok) { Auth.logout(this); refresh(); showActivation() } }
        }
    }

    override fun onDestroy() { dialog?.dismiss(); super.onDestroy() }

    // ---------- Điều hướng ----------
    private fun showPage(i: Int) {
        pages.forEachIndexed { n, p -> p.visibility = if (n == i) View.VISIBLE else View.GONE }
        navs.forEachIndexed { n, t -> t.isSelected = n == i }
    }

    // ---------- Profile ----------
    private fun loadProfiles() {
        profiles = try { JSONObject(prefs.getString("profiles", "{}")) } catch (e: Exception) { JSONObject() }
        if (profiles.length() == 0) profiles.put("Profile 1", try { JSONArray(prefs.getString("profile", "[]")) } catch (e: Exception) { JSONArray() })
        loadItems(prefs.getString("active_profile", null)?.takeIf { profiles.has(it) } ?: profiles.keys().next())
    }

    private fun setItems(arr: JSONArray) {
        items = try { KItem.list(arr) } catch (e: Exception) { mutableListOf() }
        nextId = (items.maxOfOrNull { it.id } ?: 0) + 1
        sel = null; cap = -1
        canvas.items = items; canvas.selected = null; canvas.invalidate()
        drawEditor(); drawList()
    }

    private fun loadItems(name: String) {
        cur = name
        setItems(profiles.optJSONArray(name) ?: JSONArray())
        v<Button>(R.id.btnProfile).text = "$name ▾"
    }

    private fun applyArray(arr: JSONArray) { setItems(arr); toast("Đã nạp ${arr.length()} phím. Bấm Lưu để áp dụng.") }

    private fun save() {
        val arr = KItem.array(items)
        profiles.put(cur, arr)
        prefs.edit().putString("profiles", profiles.toString()).putString("active_profile", cur).putString("profile", arr.toString()).apply()
        toast("Đã lưu \"$cur\"")
    }

    private fun profileMenu(anchor: View) {
        val names = profiles.keys().asSequence().toList()
        val pm = PopupMenu(this, anchor)
        names.forEachIndexed { i, n -> pm.menu.add(0, i, i, if (n == cur) "✓ $n" else n) }
        val base = names.size
        pm.menu.add(1, base, base, "＋ Tạo profile mới")
        pm.menu.add(1, base + 1, base + 1, "Nạp bố cục Free Fire mẫu")
        pm.menu.add(1, base + 2, base + 2, "Nhập từ file JSON")
        pm.menu.add(1, base + 3, base + 3, "Xóa profile này")
        pm.setOnMenuItemClickListener { m ->
            when (m.itemId - base) {
                0 -> newProfile()
                1 -> sample()
                2 -> startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }, 1)
                3 -> delProfile()
                else -> if (m.itemId < base) loadItems(names[m.itemId])
            }
            true
        }
        pm.show()
    }

    private fun newProfile() {
        val et = EditText(this).apply { hint = "Tên profile"; setSingleLine() }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert).setTitle("Profile mới").setView(et)
            .setPositiveButton("Tạo") { _, _ ->
                val n = et.text.toString().trim()
                if (n.isEmpty() || profiles.has(n)) toast("Tên trống hoặc đã tồn tại")
                else { profiles.put(n, JSONArray()); loadItems(n); save() }
            }.setNegativeButton("Hủy", null).show()
    }

    private fun delProfile() {
        if (profiles.length() <= 1) { toast("Cần giữ lại ít nhất 1 profile"); return }
        profiles.remove(cur)
        val next = profiles.keys().next()
        prefs.edit().putString("profiles", profiles.toString()).putString("active_profile", next)
            .putString("profile", profiles.getJSONArray(next).toString()).apply()
        loadItems(next)
    }

    private fun sample() {
        try { applyArray(JSONObject(assets.open("free-fire.json").bufferedReader().use { it.readText() }).getJSONArray("items")) }
        catch (e: Exception) { toast("Không đọc được bố cục mẫu") }
    }

    // ---------- Game ----------
    private fun gameLabel() = if (prefs.getString("game", "com.dts.freefireth") == "com.dts.freefiremax") "Free Fire MAX" else "Free Fire"
    private fun updateGameBtn() { v<Button>(R.id.btnGame).text = gameLabel() + " ▾" }

    private fun gameMenu(anchor: View) {
        val games = listOf("Free Fire" to "com.dts.freefireth", "Free Fire MAX" to "com.dts.freefiremax")
        val pm = PopupMenu(this, anchor)
        games.forEachIndexed { i, g -> pm.menu.add(0, i, i, g.first) }
        pm.setOnMenuItemClickListener { prefs.edit().putString("game", games[it.itemId].second).apply(); updateGameBtn(); true }
        pm.show()
    }

    private fun serviceOn() = (Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: "").contains(packageName)

    private fun start() {
        if (!serviceOn()) { toast("Hãy bật dịch vụ Map Phím trong Trợ năng"); startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); return }
        if (Auth.user(this) == null) { if (Auth.online()) showActivation(); return }
        save()
        val li = packageManager.getLaunchIntentForPackage(prefs.getString("game", "com.dts.freefireth")!!)
        if (li == null) toast("Chưa cài game này") else { toast("Nhấn F1 để tạm dừng hoặc tiếp tục"); startActivity(li) }
    }

    // ---------- Sơ đồ, danh sách, chỉnh phím ----------
    private fun addMenu(anchor: View) {
        val pm = PopupMenu(this, anchor)
        defs.forEachIndexed { i, d -> pm.menu.add(0, i, i, typeName(d.first)) }
        pm.setOnMenuItemClickListener { m ->
            val d = defs[m.itemId]
            val n = KItem(nextId++, d.first, 50f, 50f, d.second, d.third.toMutableList(), "", 50)
            items.add(n); sel = n; canvas.selected = n; canvas.invalidate(); drawEditor(); drawList(); true
        }
        pm.show()
    }

    private fun mouseMenu(anchor: View, slot: Int) {
        val pm = PopupMenu(this, anchor)
        listOf("Chuột trái", "Chuột giữa", "Chuột phải").forEachIndexed { i, t -> pm.menu.add(0, i, i, t) }
        pm.setOnMenuItemClickListener { m ->
            sel?.let { if (slot < it.k.size) it.k[slot] = "Mouse${m.itemId}" }
            cap = -1; canvas.invalidate(); drawEditor(); drawList(); true
        }
        pm.show()
    }

    private fun chipText(i: KItem) = when (i.t) {
        "joy" -> i.k.joinToString("")
        "look" -> "Chuột"
        else -> keyLabel(i.k.firstOrNull() ?: "?")
    }

    private fun drawList() {
        val box = v<LinearLayout>(R.id.listBox)
        box.removeAllViews()
        items.forEach { it ->
            val row = layoutInflater.inflate(R.layout.row_key, box, false)
            row.findViewById<TextView>(R.id.rowName).text = it.label.ifBlank { typeName(it.t) }
            row.findViewById<TextView>(R.id.rowKey).text = chipText(it)
            row.isSelected = it === sel
            row.setOnClickListener { sel = it; cap = -1; canvas.selected = it; canvas.invalidate(); drawEditor(); drawList() }
            box.addView(row)
        }
        v<TextView>(R.id.tvCount).text = "${items.size} phím"
    }

    private fun drawEditor() {
        val s = sel
        v<View>(R.id.editBox).visibility = if (s != null) View.VISIBLE else View.GONE
        v<View>(R.id.tvEmpty).visibility = if (s != null) View.GONE else View.VISIBLE
        if (s == null) return
        v<TextView>(R.id.tvSelTitle).text = typeName(s.t)
        updating = true
        v<EditText>(R.id.etLabel).setText(s.label)
        updating = false
        val box = v<LinearLayout>(R.id.keysBox)
        box.removeAllViews()
        v<TextView>(R.id.tvKeyHint).text =
            if (s.t == "look") "Vùng này đổi chuyển động chuột thành xoay camera." else "Chạm để nhấn phím mới, giữ để chọn nút chuột."
        if (s.t != "look") s.k.forEachIndexed { n, k ->
            val bt = Button(this, null, 0, R.style.BtnSecondary)
            bt.text = if (cap == n) "Nhấn phím…" else keyLabel(k)
            bt.setOnClickListener { cap = n; drawEditor() }
            bt.setOnLongClickListener { mouseMenu(bt, n); true }
            box.addView(bt, LinearLayout.LayoutParams(0, dp(40), 1f).apply { marginEnd = dp(6) })
        }
        v<TextView>(R.id.tvPos).text = "Vị trí   X ${"%.0f".format(s.x)}%    Y ${"%.0f".format(s.y)}%"
        v<SeekBar>(R.id.seekSize).apply { max = 42; progress = (s.s - 3f).toInt().coerceIn(0, 42) }
        v<TextView>(R.id.tvSize).text = "${s.s.toInt()}%"
        v<View>(R.id.rowLook).visibility = if (s.t == "look") View.VISIBLE else View.GONE
        v<SeekBar>(R.id.seekLook).apply { max = 95; progress = (s.sens - 5).coerceIn(0, 95) }
        v<TextView>(R.id.tvLook).text = "${s.sens}"
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val s = sel
        if (cap >= 0 && s != null) {
            if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) {
                if (e.keyCode != KeyEvent.KEYCODE_BACK && cap < s.k.size) s.k[cap] = keyNameOf(e.keyCode)
                cap = -1; canvas.invalidate(); drawEditor(); drawList()
            }
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    // ---------- Trạng thái và giấy phép ----------
    private fun remain(exp: Long): String {
        val s = exp - System.currentTimeMillis() / 1000
        if (s <= 0) return "hết hạn"
        val d = s / 86400; val h = (s % 86400) / 3600; val m = (s % 3600) / 60
        return if (d > 0) "còn $d ngày $h giờ" else if (h > 0) "còn $h giờ $m phút" else "còn $m phút"
    }

    private fun refresh() {
        val u = Auth.user(this)
        val exp = prefs.getLong("expires", 0L)
        val on = serviceOn()
        v<View>(R.id.dot).background.mutate().setTint(getColor(if (on && u != null) R.color.ok else if (on) R.color.warn else R.color.danger))
        v<TextView>(R.id.tvStatus).text = when { !on -> "Chưa bật dịch vụ"; u == null -> "Chờ kích hoạt"; else -> "Sẵn sàng" }
        v<TextView>(R.id.tvHint).text = if (on) "Dịch vụ Trợ năng đã bật." else "Bật \"Map Phím\" trong Trợ năng."
        val lic = when { !Auth.online() -> "chế độ thử"; u == null -> "chưa kích hoạt"; exp > 0 -> remain(exp); else -> "vĩnh viễn" }
        v<TextView>(R.id.tvLic).text = "★ $lic"
        v<TextView>(R.id.tvLicInfo).text = lic.replaceFirstChar { it.uppercase() }
        val key = prefs.getString("license_key", null)?.let { if (it.length >= 7) it.take(7) + "-••••-••••" else "••••" }
        v<TextView>(R.id.tvLicDetail).text = when {
            !Auth.online() -> "Chế độ thử: ứng dụng chưa nối máy chủ giấy phép."
            u == null -> "Bạn chưa kích hoạt key."
            else -> "Key: $key\nTrạng thái: $lic" +
                (if (exp > 0) "\nHết hạn lúc: " + SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(exp * 1000)) else "")
        }
    }

    private fun showActivation() {
        if (dialog?.isShowing == true) return
        val d = Dialog(this, android.R.style.Theme_Material_Dialog_NoActionBar)
        d.setContentView(R.layout.dialog_license)
        d.setCancelable(false)
        d.setOnKeyListener { _, k, ev ->
            if (k == KeyEvent.KEYCODE_BACK && ev.action == KeyEvent.ACTION_UP) { d.dismiss(); finish(); true } else false
        }
        val et = d.findViewById<EditText>(R.id.etKey)
        val dot = d.findViewById<View>(R.id.stDot)
        val st = d.findViewById<TextView>(R.id.stText)
        val go = d.findViewById<Button>(R.id.btnActivate)
        fun status(text: String, color: Int) { st.text = text; dot.background.mutate().setTint(getColor(color)) }
        status("Sẵn sàng nhập key", R.color.muted)
        et.addTextChangedListener(object : TextWatcher {
            var fixing = false
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (fixing) return
                val raw = s.toString().uppercase().filter { it.isLetterOrDigit() }.take(14)
                val out = if (raw.length <= 2) raw else raw.take(2) + "-" + raw.drop(2).chunked(4).joinToString("-")
                if (out != s.toString()) { fixing = true; et.setText(out); et.setSelection(out.length); fixing = false }
            }
        })
        go.setOnClickListener {
            val key = et.text.toString()
            if (key.length != 17) { status("Key chưa đủ ký tự", R.color.danger); return@setOnClickListener }
            go.isEnabled = false
            status("Đang xác thực…", R.color.warn)
            Auth.activate(this, key) { e ->
                go.isEnabled = true
                if (e == null) { d.dismiss(); refresh(); toast("Kích hoạt thành công") } else status(e, R.color.danger)
            }
        }
        d.findViewById<Button>(R.id.btnExit).setOnClickListener { d.dismiss(); finish() }
        dialog = d
        d.show()
        d.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(min((resources.displayMetrics.widthPixels * 0.9f).toInt(), dp(400)), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun loadBg() {
        val u = prefs.getString("bg_uri", null) ?: return
        try {
            val o = BitmapFactory.Options().apply { inSampleSize = 2 }
            contentResolver.openInputStream(Uri.parse(u))?.use { canvas.bg = BitmapFactory.decodeStream(it, null, o) }
        } catch (e: Exception) { prefs.edit().remove("bg_uri").apply() }
        canvas.invalidate()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (req == 2) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
            prefs.edit().putString("bg_uri", uri.toString()).apply()
            loadBg()
            return
        }
        try {
            val txt = contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }.trim()
            applyArray(if (txt.startsWith("[")) JSONArray(txt) else JSONObject(txt).getJSONArray("items"))
        } catch (e: Exception) { toast("Tệp JSON không hợp lệ") }
    }
}
