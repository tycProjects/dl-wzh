package com.liveisland

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as M

/** Оформление острова: Material You (цвета обоев), чёрная или белая тема. */
object IslandStyle {
  private const val PREFS = "li"
  private const val KEY = "island_theme"

  class Pal(val bg: Int, val fg: Int, val fgVar: Int, val badge: Int, val onBadge: Int, val outline: Int)

  fun theme(ctx: Context): String =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "material") ?: "material"

  fun setTheme(ctx: Context, t: String) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, t).apply()
  }

  /** Стеклянный эффект: фон острова полупрозрачный (переключается в настройках) */
  fun palette(ctx: Context, kind: String?): Pal {
    val p = palette0(ctx, kind)
    if (!flag(ctx, "glass")) return p
    val alpha = (prefs(ctx).getInt("alpha", 85).coerceIn(30, 100) * 255 / 100)
    val g = (p.bg and 0x00FFFFFF) or (alpha shl 24)
    return Pal(g, p.fg, p.fgVar, p.badge, p.onBadge, (0x33FFFFFF))
  }

  private fun palette0(ctx: Context, kind: String?): Pal = when (theme(ctx)) {
    "black" -> Pal(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xB3FFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0x33FFFFFF)
    "white" -> Pal(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xB3000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0x22000000)
    else -> {
      val base = materialPalette(ctx, kind)
      val acc = accentColor(accent(ctx))
      if (acc == null) base
      else {
        val on = if (ColorUtils.calculateLuminance(acc) > 0.5) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        Pal(base.bg, base.fg, base.fgVar, acc, on, base.outline)
      }
    }
  }

  /** Цвет острова: auto — из обоев, остальное — свой цвет */
  fun accent(ctx: Context): String = prefs(ctx).getString("accent", "auto") ?: "auto"
  fun setAccent(ctx: Context, v: String) { prefs(ctx).edit().putString("accent", v).apply() }
  private fun accentColor(name: String): Int? = when (name) {
    "blue" -> 0xFF8FA2FF.toInt()
    "pink" -> 0xFFF0A8D8.toInt()
    "green" -> 0xFF7FD9A6.toInt()
    "orange" -> 0xFFE8B86D.toInt()
    else -> null
  }

  private const val KEY_SPEED = "anim_speed"

  /** Скорость анимаций: 0 — выключено, 0.6 — быстро, 1 — обычно, 1.6 — плавно (медленнее) */
  fun speed(ctx: Context): Float = when (speedName(ctx)) {
    "off" -> 0f
    "fast" -> 0.6f
    "slow" -> 1.6f
    else -> 1f
  }

  fun speedName(ctx: Context): String =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SPEED, "normal") ?: "normal"

  fun setSpeed(ctx: Context, s: String) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SPEED, s).apply()
  }

  // ---------- настройки положения, размера и поведения ----------
  private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  fun putStr(ctx: Context, key: String, v: String) { prefs(ctx).edit().putString(key, v).apply() }
  fun putInt(ctx: Context, key: String, v: Int) { prefs(ctx).edit().putInt(key, v).apply() }
  fun putFlag(ctx: Context, key: String, v: Boolean) { prefs(ctx).edit().putBoolean(key, v).apply() }
  fun flag(ctx: Context, key: String): Boolean = prefs(ctx).getBoolean(key, true)

  /** Где стоит остров: left | center | right */
  fun posX(ctx: Context): String = prefs(ctx).getString("pos_x", "center") ?: "center"
  /** Отступ от верхнего края экрана, dp */
  fun posY(ctx: Context): Int = prefs(ctx).getInt("pos_y", 8)
  fun sizeName(ctx: Context): String = prefs(ctx).getString("size", "m") ?: "m"
  fun scale(ctx: Context): Float = when (sizeName(ctx)) { "s" -> 0.88f; "l" -> 1.15f; else -> 1f }
  /** Ширина раскрытого острова: узкий 0.8, обычный 1, широкий 1.25 */
  fun widthFactor(ctx: Context): Float = when (prefs(ctx).getString("width", "m")) {
    "n" -> 0.8f
    "w" -> 1.25f
    else -> 1f
  }
  fun setWidth(ctx: Context, v: String) { prefs(ctx).edit().putString("width", v).apply() }
  /** App Switch: показывать последние приложения (выключено по умолчанию) */
  fun appSwitch(ctx: Context): Boolean = prefs(ctx).getBoolean("app_switch", false)
  fun setAppSwitch(ctx: Context, on: Boolean) { prefs(ctx).edit().putBoolean("app_switch", on).apply() }

  fun widthName(ctx: Context): String = prefs(ctx).getString("width", "m") ?: "m"

  /** Через сколько секунд исчезают сообщения и зарядка */
  fun hideSec(ctx: Context): Int = prefs(ctx).getInt("hide_sec", 4)

  fun resetPlacement(ctx: Context) {
    prefs(ctx).edit()
      .putString("pos_x", "center").putInt("pos_y", 8).putString("size", "m").putInt("hide_sec", 4)
      .putBoolean("show_msg", true).putBoolean("show_charge", true).putBoolean("show_timer", true)
      .putBoolean("show_music", true).putBoolean("vibrate", true).apply()
  }

  private fun c(ctx: Context, attr: Int) = MaterialColors.getColor(ctx, attr, Color.GRAY)

  private fun materialPalette(ctx: Context, kind: String?): Pal {
    fun pal(bg: Int, fg: Int, badge: Int, onBadge: Int, outline: Int = Color.TRANSPARENT) =
      Pal(bg, fg, ColorUtils.setAlphaComponent(fg, 0xCC), badge, onBadge, outline)
    return when (kind) {
      "call" -> pal(c(ctx, M.attr.colorTertiaryContainer), c(ctx, M.attr.colorOnTertiaryContainer),
        c(ctx, M.attr.colorTertiary), c(ctx, M.attr.colorOnTertiary))
      "music" -> pal(c(ctx, M.attr.colorPrimaryContainer), c(ctx, M.attr.colorOnPrimaryContainer),
        c(ctx, M.attr.colorPrimary), c(ctx, M.attr.colorOnPrimary))
      "timer" -> pal(c(ctx, M.attr.colorTertiaryContainer), c(ctx, M.attr.colorOnTertiaryContainer),
        c(ctx, M.attr.colorTertiary), c(ctx, M.attr.colorOnTertiary))
      "charge" -> pal(c(ctx, M.attr.colorSecondaryContainer), c(ctx, M.attr.colorOnSecondaryContainer),
        c(ctx, M.attr.colorSecondary), c(ctx, M.attr.colorOnSecondary))
      "notify" -> pal(c(ctx, M.attr.colorSurfaceContainerHigh), c(ctx, M.attr.colorOnSurface),
        c(ctx, M.attr.colorPrimaryContainer), c(ctx, M.attr.colorOnPrimaryContainer), c(ctx, M.attr.colorOutlineVariant))
      else -> pal(c(ctx, M.attr.colorSurfaceContainerHighest), c(ctx, M.attr.colorOnSurface),
        c(ctx, M.attr.colorPrimary), c(ctx, M.attr.colorOnPrimary), c(ctx, M.attr.colorOutlineVariant))
    }
  }

  fun icon(kind: String?) = when (kind) {
    "call" -> R.drawable.ic_call
    "music" -> R.drawable.ic_music
    "charge" -> R.drawable.ic_bolt
    "timer" -> R.drawable.ic_timer
    "download" -> R.drawable.ic_download
    "record" -> R.drawable.ic_mic
    "torch" -> R.drawable.ic_bolt
    else -> R.drawable.ic_chat
  }

  /** Примеры для превью и обучения */
  fun sample(ctx: Context, kind: String): IslandContent = when (kind) {
    "call" -> IslandContent("pv", "call", ctx.getString(R.string.pv_call_title), ctx.getString(R.string.pv_call_sub),
      actions = listOf(IslandAction(ctx.getString(R.string.pv_answer), null), IslandAction(ctx.getString(R.string.pv_decline), null)))
    "music" -> IslandContent("pv", "music", ctx.getString(R.string.pv_music_title), ctx.getString(R.string.pv_music_sub),
      actions = listOf(IslandAction(ctx.getString(R.string.pv_pause), null), IslandAction(ctx.getString(R.string.pv_next), null)), progress = 35)
    "charge" -> IslandContent("pv", "charge", ctx.getString(R.string.charge_on, 54), ctx.getString(R.string.charge_on_sub), progress = 54)
    else -> IslandContent("pv", "notify", ctx.getString(R.string.pv_msg_title), ctx.getString(R.string.pv_msg_sub),
      caption = ctx.getString(R.string.pv_msg_app),
      actions = listOf(IslandAction(ctx.getString(R.string.pv_reply), null), IslandAction(ctx.getString(R.string.action_open), null)))
  }
}
