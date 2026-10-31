package com.fakesocial.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fakesocial.app.ui.AppNav
import com.fakesocial.app.ui.theme.FakeSocialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FakeSocialTheme {
                AppNav()
            }
        }
    }
}
