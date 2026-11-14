package com.liveisland

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

/** Где и как показывать остров: положение, отступ, размер, что показывать, вибрация. */
class SettingsActivity : AppCompatActivity() {
  private lateinit var phone: PhonePreviewView
  private var loading = false

  private val posIds = linkedMapOf(R.id.posLeft to "left", R.id.posCenter to "center", R.id.posRight to "right")
  private val sizeIds = linkedMapOf(R.id.sizeS to "s", R.id.sizeM to "m", R.id.sizeL to "l")
  private val widthIds = linkedMapOf(R.id.widthN to "n", R.id.widthM to "m", R.id.widthW to "w")
  private val hideIds = linkedMapOf(R.id.hide2 to 2, R.id.hide4 to 4, R.id.hide8 to 8)
  private val switches = linkedMapOf(
    R.id.swMsg to "show_msg", R.id.swCharge to "show_charge", R.id.swTimer to "show_timer",
    R.id.swMusic to "show_music", R.id.swVibe to "vibrate", R.id.swGlass to "glass", R.id.swMulti to "multi", R.id.swAuto to "auto_switch")
  private val timeSwitch by lazy { findViewById<MaterialSwitch>(R.id.swTime) }

  private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

  override fun onCreate(s: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(s)
    setContentView(R.layout.activity_settings)

    val content = findViewById<View>(R.id.setContent)
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.setRoot)) { _, insets ->
      val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      content.updatePadding(top = b.top + dp(16), bottom = b.bottom + dp(24))
      insets
    }

    phone = PhonePreviewView(this)
    findViewById<FrameLayout>(R.id.phoneHost).addView(phone,
      FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

    loadState(false)
    bindListeners()
  }

  /** Читает сохранённые настройки в элементы экрана */
  private fun loadState(animate: Boolean) {
    loading = true
    val px = IslandStyle.posX(this)
    findViewById<MaterialButtonToggleGroup>(R.id.posGroup).check((posIds.entries.firstOrNull { it.value == px } ?: posIds.entries.first()).key)
    findViewById<Slider>(R.id.posY).value = (Math.round(IslandStyle.posY(this) / 4f) * 4f).coerceIn(0f, 160f)
    val sz = IslandStyle.sizeName(this)
    findViewById<MaterialButtonToggleGroup>(R.id.sizeGroup).check((sizeIds.entries.firstOrNull { it.value == sz } ?: sizeIds.entries.first()).key)
    val hs = IslandStyle.hideSec(this)
    findViewById<MaterialButtonToggleGroup>(R.id.hideGroup).check((hideIds.entries.firstOrNull { it.value == hs } ?: hideIds.entries.elementAt(1)).key)
    switches.forEach { (id, key) -> findViewById<MaterialSwitch>(id).isChecked = IslandStyle.flag(this, key) }
    phone.setPosition(px, IslandStyle.posY(this), IslandStyle.scale(this), animate)
    loading = false
  }

  private fun updatePreview() {
    phone.setPosition(IslandStyle.posX(this), IslandStyle.posY(this), IslandStyle.scale(this), true)
  }

  private fun bindListeners() {
    findViewById<View>(R.id.btPerm).setOnClickListener {
      // название Bluetooth-устройства: разрешение «Устройства поблизости» (Android 12+)
      if (android.os.Build.VERSION.SDK_INT >= 31) {
        androidx.core.app.ActivityCompat.requestPermissions(this,
          arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), 101)
      }
    }
    findViewById<MaterialSwitch>(R.id.swSound).apply {
      isChecked = getSharedPreferences("li", MODE_PRIVATE).getBoolean("sound_island", false)
      setOnCheckedChangeListener { _, on -> getSharedPreferences("li", MODE_PRIVATE).edit().putBoolean("sound_island", on).apply() }
    }
    findViewById<MaterialSwitch>(R.id.swApp).apply {
      isChecked = IslandStyle.appSwitch(this@SettingsActivity)
      setOnCheckedChangeListener { _, on -> IslandStyle.setAppSwitch(this@SettingsActivity, on) }
    }
    findViewById<View>(R.id.usageAccess).setOnClickListener {
      startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
    findViewById<com.google.android.material.slider.Slider>(R.id.alphaSlider).apply {
      value = getSharedPreferences("li", MODE_PRIVATE).getInt("alpha", 85).coerceIn(50, 100).toFloat()
      addOnChangeListener { _, v, fromUser ->
        if (fromUser) getSharedPreferences("li", MODE_PRIVATE).edit().putInt("alpha", v.toInt()).apply()
      }
    }
    findViewById<View>(R.id.openHistory).setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
    findViewById<View>(R.id.checkMulti).setOnClickListener {
      val listener = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
      val overlay = android.provider.Settings.canDrawOverlays(this)
      val svc = IslandService.instance
      val report = getString(R.string.multi_report,
        getString(if (listener) R.string.yes else R.string.no),
        getString(if (overlay) R.string.yes else R.string.no),
        getString(if (svc != null) R.string.running else R.string.not_running),
        getString(if (IslandStyle.flag(this, "multi")) R.string.on else R.string.off),
        svc?.circleCount() ?: 0,
        svc?.queueSize() ?: 0)
      com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setTitle(R.string.check_multi)
        .setMessage(report)
        .setPositiveButton(R.string.ok, null)
        .show()
    }
    findViewById<MaterialButtonToggleGroup>(R.id.widthGroup).also { g ->
      val cur = IslandStyle.widthName(this)
      g.check((widthIds.entries.firstOrNull { it.value == cur } ?: widthIds.entries.first()).key)
      g.addOnButtonCheckedListener { _, id, checked ->
        if (checked && !loading) {
          IslandStyle.setWidth(this, widthIds.getValue(id))
          IslandService.instance?.rebuild()
          updatePreview()
        }
      }
    }
    findViewById<MaterialButtonToggleGroup>(R.id.posGroup).addOnButtonCheckedListener { _, id, checked ->
      if (checked && !loading) {
        IslandStyle.putStr(this, "pos_x", posIds.getValue(id)); updatePreview()
        IslandService.instance?.applyPosition()
      }
    }
    val slider = findViewById<Slider>(R.id.posY)
    slider.setLabelFormatter { v -> "${v.toInt()} dp" }
    slider.addOnChangeListener { _, v, fromUser ->
      if (fromUser && !loading) {
        IslandStyle.putInt(this, "pos_y", v.toInt()); updatePreview()
        IslandService.instance?.applyPosition()
      }
    }
    findViewById<MaterialButtonToggleGroup>(R.id.sizeGroup).addOnButtonCheckedListener { _, id, checked ->
      if (checked && !loading) {
        IslandStyle.putStr(this, "size", sizeIds.getValue(id)); updatePreview()
        IslandService.instance?.rebuild()
      }
    }
    findViewById<MaterialButtonToggleGroup>(R.id.hideGroup).addOnButtonCheckedListener { _, id, checked ->
      if (checked && !loading) IslandStyle.putInt(this, "hide_sec", hideIds.getValue(id))
    }
    switches.forEach { (id, key) ->
      findViewById<MaterialSwitch>(id).setOnCheckedChangeListener { _, on ->
        if (!loading) IslandStyle.putFlag(this, key, on)
      }
    }

    timeSwitch.isChecked = getSharedPreferences("li", MODE_PRIVATE).getBoolean("show_time", false)
    timeSwitch.setOnCheckedChangeListener { _, on ->
      if (!loading) {
        getSharedPreferences("li", MODE_PRIVATE).edit().putBoolean("show_time", on).apply()
        IslandService.instance?.refreshAll()
      }
    }
    findViewById<View>(R.id.testBtn).setOnClickListener {
      if (!Settings.canDrawOverlays(this)) {
        Toast.makeText(this, R.string.need_overlay, Toast.LENGTH_LONG).show()
      } else {
        val svc = IslandService.instance
        if (svc != null) svc.demo() else { IslandService.pendingDemo = true; IslandService.start(this) }
      }
    }
    findViewById<View>(R.id.resetBtn).setOnClickListener {
      IslandStyle.resetPlacement(this)
      loadState(true)
      IslandService.instance?.applyPosition()
      IslandService.instance?.rebuild()
    }
  }
}
