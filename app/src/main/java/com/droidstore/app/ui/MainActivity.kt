package com.droidstore.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.droidstore.app.data.Session
import com.droidstore.app.ui.theme.DroidStoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Session.init(applicationContext)
        setContent {
            DroidStoreTheme {
                AppRoot()
            }
        }
    }
}
