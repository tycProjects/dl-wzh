package com.liveisland

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    // Material You: цвета из обоев на Android 12+
    DynamicColors.applyToActivitiesIfAvailable(this)
  }
}
