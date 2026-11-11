package com.liveisland

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** История уведомлений: нажатие открывает приложение-источник. */
class HistoryActivity : AppCompatActivity() {
  override fun onCreate(s: Bundle?) {
    super.onCreate(s)
    setContentView(R.layout.activity_history)
    supportActionBar?.title = getString(R.string.history_title)
    val list = findViewById<LinearLayout>(R.id.historyList)
    val items = IslandService.instance?.historySnapshot().orEmpty()
    if (items.isEmpty()) {
      list.addView(TextView(this).apply { text = getString(R.string.history_empty); textSize = 16f; setTextColor(0xFF9A9AA6.toInt()) })
      return
    }
    items.forEach { m ->
      val row = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(40, 28, 40, 28)
        background = GradientDrawable().apply { cornerRadius = 36f; setColor(0x22808090) }
        setOnClickListener {
          val launch = if (m.pkg.isNotBlank()) packageManager.getLaunchIntentForPackage(m.pkg) else null
          if (launch != null) startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
      }
      row.addView(TextView(this).apply { text = m.title; textSize = 16f; setTextColor(0xFF1B1B21.toInt()) })
      row.addView(TextView(this).apply { text = "${m.caption}  ${m.sub}".trim(); textSize = 13f; setTextColor(0xFF5B6670.toInt()) })
      list.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 })
    }
  }
}
