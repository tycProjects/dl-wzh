package com.mbz71.gptchess

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = applicationContext
    }
}
