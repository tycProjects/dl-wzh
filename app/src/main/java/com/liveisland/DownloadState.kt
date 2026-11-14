package com.liveisland

/** Состояние загрузки обновления: читает экран загрузки и остров. */
object DownloadState {
  @Volatile var pct = 0
  @Volatile var doneBytes = 0L
  @Volatile var totalBytes = 0L
  @Volatile var bytesPerSec = 0L
  @Volatile var running = false
  @Volatile var cancelled = false
  @Volatile var error: String? = null

  /** Скорость в МБ/с с одним знаком после запятой */
  fun speedText(): String = String.format(java.util.Locale.US, "%.1f", bytesPerSec / 1_000_000.0)
  fun sizeText(): String = String.format(java.util.Locale.US, "%.1f / %.1f МБ", doneBytes / 1_000_000.0, totalBytes / 1_000_000.0)
}
