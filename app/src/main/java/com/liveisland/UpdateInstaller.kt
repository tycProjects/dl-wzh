package com.liveisland

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Скачивание APK обновления с прогрессом и запуск установки. */
object UpdateInstaller {

  /** Блокирующий вызов: только из фонового потока. onProgress получает проценты 0..100. */
  fun download(ctx: Context, apkUrl: String, onProgress: (Int) -> Unit): File {
    require(apkUrl.startsWith("https://")) { "Небезопасная ссылка" }
    val dir = File(ctx.filesDir, "updates").apply { mkdirs() }
    val file = File(dir, "LiveIsland-update.apk")
    val conn = URL(apkUrl).openConnection() as HttpURLConnection
    try {
      conn.connectTimeout = 15000
      conn.readTimeout = 15000
      if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
      val total = conn.contentLengthLong
      var done = 0L
      var last = -1
      conn.inputStream.use { input ->
        file.outputStream().use { output ->
          val buf = ByteArray(64 * 1024)
          while (true) {
            val n = input.read(buf)
            if (n < 0) break
            output.write(buf, 0, n)
            done += n
            if (total > 0) {
              val pct = (done * 100 / total).toInt()
              if (pct != last) { last = pct; onProgress(pct) }
            }
          }
        }
      }
      if (file.length() < 1000L) throw IllegalStateException("Файл слишком маленький")
      return file
    } finally {
      conn.disconnect()
    }
  }

  /** Открывает системный установщик для скачанного APK */
  fun install(ctx: Context, file: File) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    val i = Intent(Intent.ACTION_VIEW)
      .setDataAndType(uri, "application/vnd.android.package-archive")
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(i)
  }
}
