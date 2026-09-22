package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.example.ui.DictionaryViewModel
import com.example.ui.components.DictionaryBottomNavBar
import com.example.ui.components.NavTab
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BisayaSubanenTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            BisayaSubanenTheme(darkTheme = isDark) {
                DictionaryApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DictionaryApp(
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavTab.HOME) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            DictionaryBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        val screenModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (currentTab) {
            NavTab.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToFavorites = { currentTab = NavTab.FAVORITES },
                    onNavigateToHistory = { currentTab = NavTab.HISTORY },
                    modifier = screenModifier
                )
            }
            NavTab.FAVORITES -> {
                FavoritesScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
            NavTab.HISTORY -> {
                HistoryScreen(
                    viewModel = viewModel,
                    onSelectSearch = { query ->
                        viewModel.setSearchQuery(query)
                        currentTab = NavTab.HOME
                    },
                    modifier = screenModifier
                )
            }
            NavTab.SETTINGS -> {
                SettingsScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
        }
    }
}
