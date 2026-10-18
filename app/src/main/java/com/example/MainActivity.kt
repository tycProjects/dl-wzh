package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ZMapsBottomNavBar
import com.example.ui.screens.MapScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.ZMapsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ZMapsViewModel = viewModel()
            val currentAccent by viewModel.accentFlow.collectAsStateWithLifecycle()
            val currentLanguage by viewModel.languageFlow.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

            var showSplash by remember { mutableStateOf(true) }

            ZMapsTheme(accent = currentAccent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBackground)
                ) {
                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = CyberBackground,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                ZMapsBottomNavBar(
                                    currentTab = currentTab,
                                    language = currentLanguage,
                                    onTabSelected = { tab -> viewModel.switchTab(tab) }
                                )
                            }
                        ) { innerPadding ->
                            Crossfade(
                                targetState = currentTab,
                                animationSpec = tween(250),
                                label = "tab_crossfade",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = innerPadding.calculateBottomPadding())
                            ) { tab ->
                                when (tab) {
                                    ScreenTab.MAP -> MapScreen(viewModel = viewModel)
                                    ScreenTab.SEARCH -> SearchScreen(viewModel = viewModel)
                                    ScreenTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
