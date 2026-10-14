package com.sensi.screen

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class AppItem(val pkg: String, val label: String)

/**
 * Chọn ứng dụng sẽ được chỉnh kích thước màn hình.
 * Chạm = bật/tắt. Nhấn giữ = đặt kích thước riêng cho ứng dụng đó.
 */
class AppPickerActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var adapter: Adapter
    private val all = mutableListOf<AppItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picker)
        title = "Chọn ứng dụng"
        prefs = Prefs(this)

        val rv = findViewById<RecyclerView>(R.id.rv)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = Adapter()
        rv.adapter = adapter

        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = adapter.filter(s?.toString() ?: "")
        })

        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val list = pm.queryIntentActivities(intent, 0)
                .map { AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
                .filter { it.pkg != packageName }
                .distinctBy { it.pkg }
                .sortedBy { it.label.lowercase() }
            runOnUiThread {
                all.clear()
                all.addAll(list)
                adapter.filter("")
            }
        }.start()
    }

    private fun subtitle(app: AppItem, p: Profile?): String = when {
        p == null -> app.pkg
        p.w > 0 && p.h > 0 -> "${p.w}x${p.h}" + (if (p.d > 0) " · ${p.d}dpi" else "")
        else -> "Dùng kích thước mặc định"
    }

    private fun showOverride(app: AppItem) {
        val cur = prefs.apps()[app.pkg]
        fun field(hint: String, v: Int) = EditText(this).apply {
            this.hint = hint
            inputType = InputType.TYPE_CLASS_NUMBER
            if (v > 0) setText(v.toString())
        }
        val eW = field("Rộng (px)", cur?.w ?: 0)
        val eH = field("Cao (px)", cur?.h ?: 0)
        val eD = field("Density (trống = mặc định)", cur?.d ?: 0)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(eW); addView(eH); addView(eD)
        }
        AlertDialog.Builder(this)
            .setTitle(app.label)
            .setMessage("Để trống Rộng/Cao để dùng kích thước mặc định chung.")
            .setView(box)
            .setPositiveButton("Lưu") { _, _ ->
                val w = eW.text.toString().toIntOrNull() ?: 0
                val h = eH.text.toString().toIntOrNull() ?: 0
                val d = eD.text.toString().toIntOrNull() ?: 0
                val ok = (w == 0 && h == 0) || (w in 200..10000 && h in 200..10000)
                if (ok) {
                    prefs.setApp(app.pkg, Profile(w, h, d))
                    adapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
        val sub: TextView = v.findViewById(R.id.sub)
        val check: CheckBox = v.findViewById(R.id.check)
    }

    inner class Adapter : RecyclerView.Adapter<VH>() {
        private var items: List<AppItem> = emptyList()

        fun filter(q: String) {
            items = if (q.isBlank()) all.toList()
            else all.filter { it.label.contains(q, true) || it.pkg.contains(q, true) }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, position: Int) {
            val app = items[position]
            val p = prefs.apps()[app.pkg]
            h.label.text = app.label
            h.sub.text = subtitle(app, p)
            h.icon.setImageDrawable(
                try { packageManager.getApplicationIcon(app.pkg) } catch (e: Exception) { null }
            )
            h.check.setOnCheckedChangeListener(null)
            h.check.isChecked = p != null
            h.check.setOnCheckedChangeListener { _, checked ->
                if (checked) prefs.setApp(app.pkg, prefs.apps()[app.pkg] ?: Profile(0, 0, 0))
                else prefs.setApp(app.pkg, null)
                h.sub.text = subtitle(app, prefs.apps()[app.pkg])
            }
            h.itemView.setOnClickListener { h.check.toggle() }
            h.itemView.setOnLongClickListener { showOverride(app); true }
        }
    }
}
