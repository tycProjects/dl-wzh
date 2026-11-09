package com.mousetuner

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.concurrent.thread

/** Ô công cụ kiểu Game Space của điện thoại gaming: bật = viền + chữ sáng màu chủ đề. */
fun Context.tile(icon: String, label: String, on0: Boolean, toggle: Boolean, click: (Boolean) -> Unit): LinearLayout {
    var on = on0
    val ic = TextView(this).apply { text = icon; textSize = 24f; gravity = Gravity.CENTER }
    val tx = TextView(this).apply { text = label; textSize = 11f; gravity = Gravity.CENTER; typeface = Typeface.DEFAULT_BOLD; maxLines = 2; setPadding(0, dp(4), 0, 0) }
    val t = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(4), dp(12), dp(4), dp(10)); minimumHeight = dp(84) }
    t.addView(ic); t.addView(tx)
    fun paint() {
        val lit = toggle && on
        t.background = GradientDrawable().apply { cornerRadius = dp(16).toFloat()
            if (lit) { setColor(Color.argb(46, Color.red(Theme.acc), Color.green(Theme.acc), Color.blue(Theme.acc))); setStroke(dp(2), Theme.acc) } else { setColor(0xFF242322.toInt()); setStroke(dp(1), 0x22FFFFFF) } }
        tx.setTextColor(if (lit) Theme.acc else 0xFFD8D4CC.toInt()); ic.alpha = if (lit) 1f else 0.78f
    }
    paint()
    t.setOnClickListener { if (toggle) { on = !on; paint() }; click(on) }
    return t
}

private fun Context.grid(cols: Int, tiles: List<View>): LinearLayout {
    val g = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    tiles.chunked(cols).forEach { rt ->
        val r = row()
        rt.forEachIndexed { i, v -> r.addView(v, lpw(0, -2, 1f).apply { setMargins(if (i > 0) dp(8) else 0, dp(4), 0, dp(4)) }) }
        repeat(cols - rt.size) { r.addView(View(this), lpw(0, 1, 1f).apply { leftMargin = dp(8) }) }
        g.addView(r)
    }
    return g
}

/** Toàn bộ trang Công cụ (dùng cho màn hình chính và menu trong game). */
fun Context.toolsView(cols: Int, toast: (String) -> Unit): LinearLayout {
    val ctx = this
    val v = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
    fun tg(key: String, icon: String, label: String) = ctx.tile(icon, label, Cfg.b(ctx, key), true) { Cfg.setB(ctx, key, it); Engine.applyAsync(ctx) }
    fun act(icon: String, label: String, f: () -> Unit) = ctx.tile(icon, label, false, false) { thread { f() } }
    fun card(title: String, vararg views: View): LinearLayout { val c = ctx.gcard(); c.put(ctx.sectionTitle(title)); views.forEach { c.addView(it) }; v.addView(c, lpw(-1, -2).apply { bottomMargin = ctx.dp(10) }); return c }
    val game = FreeFire.installed(ctx).firstOrNull()?.second

    card("TIỆN ÍCH", ctx.grid(cols, listOf(
        tg("dnd", "🔕", "Không làm phiền"), tg("headsup", "🚫", "Chặn thông báo"), tg("wifi", "📶", "Wi-Fi Turbo"), tg("nobat", "🔋", "Max Power"),
        tg("stayon", "☀️", "Giữ sáng"), tg("rotlock", "🔒", "Khoá xoay"), tg("bgdata", "🌐", "Chặn nền"), tg("noauto", "💡", "Khoá sáng"),
        tg("noblur", "💎", "Tắt mờ"), tg("ramclean", "🧹", "Dọn RAM tự động"))))

    val st = ctx.body("").apply { textSize = 11f; setTextColor(0xFF9A9AA0.toInt()) }
    card("CẢM ỨNG", ctx.grid(cols, listOf(tg("touch", "👆", "Vuốt nhạy"), tg("edge", "↔️", "Chống vuốt cạnh"), tg("hitouch", "⚡", "Cảm ứng cao"), tg("dbg", "🎯", "Điểm chạm"))),
        ctx.seg(listOf("Tắt", "Nhẹ", "Mạnh", "MAX"), Cfg.i(ctx, "buff", 0)) { Cfg.setI(ctx, "buff", it)
            thread { Engine.apply(ctx); toast(ShizukuShell.touchStatus().take(3).joinToString(" · ") { "${it.first} ${it.second}" }) } }.also { (it.layoutParams as? LinearLayout.LayoutParams)?.topMargin = ctx.dp(6) })

    val hudSel = if (!Cfg.fps(ctx)) 0 else if (Cfg.hudCompact(ctx)) 1 else 2
    card("MONITORING FPS", ctx.seg(listOf("Tắt", "Gọn", "Đầy đủ"), hudSel) { Cfg.setB(ctx, "fps", it != 0); Cfg.setB(ctx, "hudc", it == 1); Engine.applyAsync(ctx) },
        ctx.slider("Độ đậm: ", 70, Cfg.hudAlpha(ctx) - 30, { "${it + 30}%" }) { Cfg.setI(ctx, "hudA", it + 30) })

    card("NHANH", ctx.grid(cols, listOf(
        act("📸", "Chụp màn hình") { val r = ShizukuShell.screenshot(); toast(if (r.first == 0) "Đã lưu ảnh" else "Chụp lỗi") },
        act("🧹", "Dọn RAM") { ShizukuShell.killBg(); toast("Đã dọn RAM") },
        act("🗑️", "Dọn cache") { ShizukuShell.trimCaches(); toast("Đang dọn cache") },
        act("⚙️", "Biên dịch") { if (game != null) { toast("Đang biên dịch…"); val x = ShizukuShell.compile(game); toast(if (x.first == 0) "Biên dịch xong" else "Lỗi") } },
        act("📌", "Giữ nền") { if (game != null) { ShizukuShell.keepAlive(game); toast("Đã giữ game ở nền") } })))
    return v
}
