package com.liveisland

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    // Material You: цвета из обоев на Android 12+
    DynamicColors.applyToActivitiesIfAvailable(this)
    // Тёмный интерфейс приложения по умолчанию (переключается во вкладке «Вид»)
    AppCompatDelegate.setDefaultNightMode(
      if (getSharedPreferences("li", MODE_PRIVATE).getBoolean("app_dark", true)) AppCompatDelegate.MODE_NIGHT_YES
      else AppCompatDelegate.MODE_NIGHT_NO)
  }
}
