package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.screens.SinglePageSecretChatScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StegoBgLight

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        } catch (_: Exception) {
            // Ignore in test or headless environments
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = StegoBgLight
                ) {
                    SinglePageSecretChatScreen()
                }
            }
        }
    }
}
