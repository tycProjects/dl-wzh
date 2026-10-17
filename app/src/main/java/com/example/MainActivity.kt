package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.ai.configureAppCheck
import com.example.ui.LiquidGlassMainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        // STEP 2: Enable edge-to-edge system insets
        enableEdgeToEdge()

        // STEP 3: Initialize Liquid Glass Composable Interface
        setContent {
            MyApplicationTheme {
                LiquidGlassMainScreen()
            }
        }
    }
}
