package com.azuro.app

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import org.json.JSONObject
import java.net.URL

/** Bottom-sheet model chooser backed by OpenRouter's public model list. */
object ModelPicker {
    class M(val id: String, val name: String, val free: Boolean, val vision: Boolean, val ctx: Int, val created: Long)
    private var cache: List<M>? = null

    private class Row(val a: Activity) {
        val root = LinearLayout(a)
        val name = a.tv("", 15f, T.ink, true).apply { isSingleLine = true; ellipsize = TextUtils.TruncateAt.END }
        val id = a.tv("", 12f, T.mut).apply { isSingleLine = true; ellipsize = TextUtils.TruncateAt.MIDDLE }
        val free = pill("FREE", T.ok, T.okSoft); val vis = pill("Vision", T.pri, T.priSoft); val ctx = pill("", T.mut, T.field)
        val check = a.icon(R.drawable.ic_check, T.pri, 20)
        private fun pill(t: String, fg: Int, bg: Int) = a.tv(t, 10.5f, fg, true).apply {
            setPadding(a.dp(8), a.dp(4), a.dp(8), a.dp(4)); background = a.shape(bg, a.dpf(8f))
        }
        init {
            root.orientation = LinearLayout.HORIZONTAL; root.gravity = Gravity.CENTER_VERTICAL
            root.setPadding(a.dp(20), a.dp(12), a.dp(20), a.dp(12))
            val col = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
            col.addView(name); col.addView(id, LinearLayout.LayoutParams(-2, -2).apply { topMargin = a.dp(2) })
            val tags = LinearLayout(a)
            for (t in listOf(free, vis, ctx)) tags.addView(t, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = a.dp(6) })
            col.addView(tags, LinearLayout.LayoutParams(-2, -2).apply { topMargin = a.dp(7) })
            root.addView(col, LinearLayout.LayoutParams(0, -2, 1f)); root.addView(check)
        }
        fun bind(m: M, sel: Boolean) {
            name.text = m.name; id.text = m.id
            free.visibility = if (m.free) View.VISIBLE else View.GONE
            vis.visibility = if (m.vision) View.VISIBLE else View.GONE
            ctx.visibility = if (m.ctx > 0) View.VISIBLE else View.GONE; ctx.text = if (m.ctx >= 1000) "${m.ctx / 1000}k ctx" else "${m.ctx} ctx"
            check.visibility = if (sel) View.VISIBLE else View.INVISIBLE
            root.background = a.ripple(a.shape(if (sel) T.priSoft else 0, 0f), 0f)
        }
    }

    fun show(a: Activity, needVision: Boolean, current: String, onPick: (String) -> Unit) {
        T.init(a)
        val dlg = Dialog(a); dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; background = a.topShape(T.bg, a.dpf(26f)) }
        root.addView(View(a).apply { background = a.shape(T.line, a.dpf(2f)) },
            LinearLayout.LayoutParams(a.dp(40), a.dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin = a.dp(10) })

        val tr = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(a.dp(20), a.dp(14), a.dp(10), a.dp(4)) }
        val tc = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        tc.addView(a.tv(if (needVision) "Vision model" else "Chat model", 20f, T.ink, true))
        tc.addView(a.tv(if (needVision) "Must accept images. It reads your screen." else "Writes the answers. Any text model works.", 12.5f, T.mut),
            LinearLayout.LayoutParams(-2, -2).apply { topMargin = a.dp(4) })
        tr.addView(tc, LinearLayout.LayoutParams(0, -2, 1f)); tr.addView(a.iconBtn(R.drawable.ic_close, T.mut) { dlg.dismiss() })
        root.addView(tr)

        val sb = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(a.dp(14), 0, a.dp(14), 0); background = a.shape(T.field, a.dpf(14f)) }
        val search = EditText(a).apply { hint = "Search models"; setHintTextColor(T.mut); setTextColor(T.ink); textSize = 15f; background = null; isSingleLine = true }
        sb.addView(a.icon(R.drawable.ic_search, T.mut, 20)); sb.addView(search, LinearLayout.LayoutParams(0, a.dp(46), 1f).apply { marginStart = a.dp(10) })
        root.addView(sb, LinearLayout.LayoutParams(-1, -2).apply { setMargins(a.dp(16), a.dp(12), a.dp(16), 0) })

        var freeOn = false; var newest = false; var shown: List<M> = emptyList()
        val count = a.tv("Loading models…", 12f, T.mut)
        val list = ListView(a).apply { divider = null; dividerHeight = 0; selector = ColorDrawable(Color.TRANSPARENT); setPadding(0, 0, 0, a.dp(12)); clipToPadding = false }
        val adapter = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(p: Int) = shown[p]
            override fun getItemId(p: Int) = p.toLong()
            override fun getView(p: Int, cv: View?, parent: ViewGroup): View {
                val h = (cv?.tag as? Row) ?: Row(a).also { it.root.tag = it }
                h.bind(shown[p], shown[p].id == current); return h.root
            }
        }
        list.adapter = adapter
        fun refresh() {
            val q = search.text.toString().trim().lowercase()
            val l = (cache ?: emptyList()).filter {
                (!freeOn || it.free) && (!needVision || it.vision) && (q.isEmpty() || it.id.lowercase().contains(q) || it.name.lowercase().contains(q))
            }
            shown = if (newest) l.sortedByDescending { it.created } else l.sortedWith(compareBy({ !it.free }, { it.name.lowercase() }))
            adapter.notifyDataSetChanged()
            if (cache != null) count.text = "${shown.size} models"
        }
        val chips = LinearLayout(a).apply { setPadding(a.dp(16), a.dp(12), a.dp(16), a.dp(4)) }
        chips.addView(Chip(a, R.drawable.ic_bolt, "Free", true) { freeOn = it; refresh() }.view)
        chips.addView(Chip(a, R.drawable.ic_refresh, "Newest", true) { newest = it; refresh() }.view, LinearLayout.LayoutParams(-2, -2).apply { marginStart = a.dp(8) })
        root.addView(chips)
        root.addView(count, LinearLayout.LayoutParams(-2, -2).apply { setMargins(a.dp(20), a.dp(6), 0, a.dp(6)) })
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { refresh() }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, af: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })
        list.setOnItemClickListener { _, _, i, _ -> onPick(shown[i].id); dlg.dismiss() }

        dlg.setContentView(root)
        dlg.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(-1, (a.resources.displayMetrics.heightPixels * 0.9f).toInt()); setGravity(Gravity.BOTTOM)
            setWindowAnimations(android.R.style.Animation_InputMethod)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        dlg.show()

        if (cache != null) { refresh(); return }
        Thread {
            try {
                val arr = JSONObject(URL("https://openrouter.ai/api/v1/models").readText()).getJSONArray("data")
                val out = ArrayList<M>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i); val id = o.getString("id")
                    val pr = o.optJSONObject("pricing"); val ar = o.optJSONObject("architecture")
                    fun has(k: String, v: String): Boolean { val m = ar?.optJSONArray(k) ?: return v == "text"; for (j in 0 until m.length()) if (m.getString(j) == v) return true; return false }
                    if (!has("output_modalities", "text")) continue
                    val free = id.endsWith(":free") || (pr != null && pr.optString("prompt") == "0" && pr.optString("completion") == "0")
                    out += M(id, o.optString("name", id), free, has("input_modalities", "image"), o.optInt("context_length"), o.optLong("created"))
                }
                cache = out
                a.runOnUiThread { refresh() }
            } catch (e: Exception) { a.runOnUiThread { count.text = "Couldn’t load models: ${e.message}" } }
        }.start()
    }
}
