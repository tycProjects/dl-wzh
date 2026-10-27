package com.game.play

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.game.play.data.SettingsStore
import com.game.play.notify.Notifier
import com.game.play.notify.UpdateScheduler
import com.game.play.ui.PorGameApp
import com.game.play.ui.StoreViewModel
import com.game.play.ui.theme.PorGameTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.ensureChannels(this)
        val appContext = applicationContext
        lifecycleScope.launch {
            UpdateScheduler.apply(appContext, SettingsStore(appContext).flow.first().notifications)
        }
        setContent {
            val vm: StoreViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val dark = settings.darkMode ?: isSystemInDarkTheme()
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
            PorGameTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    PorGameApp(vm)
                }
            }
        }
    }
}
