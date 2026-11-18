package com.glassdex.analyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.glassdex.analyzer.ui.*

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsState()
            val screen by vm.screen.collectAsState()
            BackHandler { if (!vm.back()) finish() }
            MaterialTheme(colorScheme = darkColorScheme(primary = C.cyan)) {
                CompositionLocalProvider(LocalGlassOpacity provides settings.glassOpacity) {
                    GlassBackground(settings.animatedBackground) {
                        when (screen) {
                            Screen.LOGIN -> LoginScreen(vm)
                            Screen.HOME -> HomeScreen(vm)
                            Screen.ANALYZING -> AnalyzingScreen(vm)
                            Screen.REPORT -> ReportScreen(vm)
                            Screen.HISTORY -> HistoryScreen(vm)
                            Screen.SETTINGS -> SettingsScreen(vm)
                            Screen.FILES -> FilesScreen(vm)
                            Screen.VIEWER -> ViewerScreen(vm)
                        }
                    }
                }
            }
        }
    }
}
