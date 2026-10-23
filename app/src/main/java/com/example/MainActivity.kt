package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CodingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.ArafatAiTheme
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.CyberNavySurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanBright
import com.example.viewmodel.MainViewModel

enum class ArafatNavScreen(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    CHAT("Chat", Icons.Default.Chat),
    CODING("Coding", Icons.Default.Code),
    TOOLS("Tools", Icons.Default.Widgets),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArafatAiTheme {
                val viewModel: MainViewModel = viewModel()
                ArafatAiApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ArafatAiApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf(ArafatNavScreen.HOME) }

    // Hardware Back Button handling: return to Home if on sub-screens
    BackHandler(enabled = currentScreen != ArafatNavScreen.HOME) {
        currentScreen = ArafatNavScreen.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = CyberNavySurface,
                contentColor = NeonCyan,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                ArafatNavScreen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberNavyDark,
                            selectedTextColor = NeonCyanBright,
                            indicatorColor = NeonCyan,
                            unselectedIconColor = Color(0xFF6B809E),
                            unselectedTextColor = Color(0xFF6B809E)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberNavyDark)
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { target ->
                when (target) {
                    ArafatNavScreen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { currentScreen = ArafatNavScreen.CHAT },
                        onNavigateToCoding = { currentScreen = ArafatNavScreen.CODING },
                        onNavigateToTools = { currentScreen = ArafatNavScreen.TOOLS },
                        onNavigateToSettings = { currentScreen = ArafatNavScreen.SETTINGS }
                    )
                    ArafatNavScreen.CHAT -> ChatScreen(
                        viewModel = viewModel,
                        onNavigateToCoding = { currentScreen = ArafatNavScreen.CODING }
                    )
                    ArafatNavScreen.CODING -> CodingScreen(
                        viewModel = viewModel
                    )
                    ArafatNavScreen.TOOLS -> ToolsScreen(
                        viewModel = viewModel
                    )
                    ArafatNavScreen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
