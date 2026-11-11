package com.liveisland

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator
import android.widget.TextView
import com.google.android.material.button.MaterialButton

/** Экран загрузки обновления: процент, скорость, размер, отмена. */
class DownloadActivity : AppCompatActivity() {
  private val h = Handler(Looper.getMainLooper())
  private val tick = object : Runnable {
    override fun run() { render(); h.postDelayed(this, 300L) }
  }

  override fun onCreate(s: Bundle?) {
    super.onCreate(s)
    setContentView(R.layout.activity_download)
    findViewById<MaterialButton>(R.id.dlCancel).setOnClickListener {
      DownloadState.cancelled = true
    }
  }

  override fun onResume() { super.onResume(); h.post(tick) }
  override fun onPause() { super.onPause(); h.removeCallbacks(tick) }

  private fun render() {
    val pct = DownloadState.pct
    findViewById<LinearProgressIndicator>(R.id.dlScreenBar).setProgressCompat(pct, true)
    findViewById<TextView>(R.id.dlScreenPct).text = "$pct%"
    findViewById<TextView>(R.id.dlScreenSize).text = DownloadState.sizeText()
    val status = when {
      DownloadState.error != null -> DownloadState.error!!
      DownloadState.cancelled -> getString(R.string.dl_cancelled)
      !DownloadState.running && pct >= 100 -> getString(R.string.dl_done)
      pct == 0 -> getString(R.string.dl_idle)
      else -> getString(R.string.dl_title)
    }
    findViewById<TextView>(R.id.dlScreenStatus).text = status
    findViewById<MaterialButton>(R.id.dlCancel).visibility = if (DownloadState.running) View.VISIBLE else View.GONE
  }
}
