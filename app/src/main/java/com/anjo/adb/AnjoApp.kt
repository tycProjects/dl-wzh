package com.anjo.adb

import android.app.Application
import rikka.shizuku.Shizuku

class AnjoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Fica escutando se o Shizuku ficou disponível/indisponível em qualquer tela
        Shizuku.addBinderReceivedListenerSticky {}
        Shizuku.addBinderDeadListener {}
    }
}
