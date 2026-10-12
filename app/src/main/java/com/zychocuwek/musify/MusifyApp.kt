package com.zychocuwek.musify

import android.app.Application
import com.zychocuwek.musify.data.Store

class MusifyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
    }
}
