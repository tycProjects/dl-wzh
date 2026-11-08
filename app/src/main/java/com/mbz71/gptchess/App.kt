package com.mbz71.gptchess

import android.app.Application

object AppContextHolder {
    lateinit var context: android.content.Context
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContextHolder.context = applicationContext
    }
}
