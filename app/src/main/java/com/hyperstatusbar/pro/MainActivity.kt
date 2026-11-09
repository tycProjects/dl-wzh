package com.hyperstatusbar.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hyperstatusbar.pro.logging.AppLogger
import com.hyperstatusbar.pro.root.RootManager
import com.hyperstatusbar.pro.ui.HyperApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val logger = AppLogger(applicationContext)
        logger.info("MainActivity", "Приложение запущено")
        val rootManager = RootManager(logger)
        setContent { HyperApp(rootManager = rootManager, logger = logger) }
    }
}
