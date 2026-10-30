package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.model.BottomNavTab
import com.example.data.model.Screen
import com.example.ui.components.BengaliBottomNavBar
import com.example.ui.components.BengaliTopAppBar
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.category.CategoryStoriesScreen
import com.example.ui.screens.community.CommunityScreen
import com.example.ui.screens.details.StoryDetailsScreen
import com.example.ui.screens.discover.DiscoverScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.notifications.NotificationsScreen
import com.example.ui.screens.owner.OwnerProfileScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.reader.StoryReaderScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.theme.GolpokothaTheme
import com.example.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            GolpokothaTheme(darkTheme = isDark) {
                val currentScreen by viewModel.currentScreen.collectAsState()

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        is Screen.Splash -> {
                            SplashScreen(
                                onSplashFinished = {
                                    val user = viewModel.currentUser.value
                                    if (user != null) {
                                        viewModel.navigateTo(Screen.Main(BottomNavTab.HOME))
                                    } else {
                                        viewModel.navigateTo(Screen.Auth)
                                    }
                                }
                            )
                        }

                        is Screen.Auth -> {
                            AuthScreen(
                                viewModel = viewModel,
                                onAuthSuccess = {
                                    viewModel.navigateTo(Screen.Main(BottomNavTab.HOME))
                                }
                            )
                        }

                        is Screen.Main -> {
                            MainContainer(viewModel = viewModel)
                        }

                        is Screen.StoryDetail -> {
                            StoryDetailsScreen(
                                storyId = screen.storyId,
                                viewModel = viewModel,
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.StoryReader -> {
                            StoryReaderScreen(
                                storyId = screen.storyId,
                                initialChapterNumber = screen.chapterNumber,
                                viewModel = viewModel,
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.CategoryStories -> {
                            CategoryStoriesScreen(
                                categoryId = screen.categoryId,
                                categoryName = screen.categoryName,
                                viewModel = viewModel,
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.AdminDashboard -> {
                            AdminDashboardScreen(
                                viewModel = viewModel,
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.AdminLogin -> {
                            AdminDashboardScreen(
                                viewModel = viewModel,
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.OwnerProfile -> {
                            OwnerProfileScreen(
                                viewModel = viewModel,
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.Notifications -> {
                            NotificationsScreen(
                                viewModel = viewModel,
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.Settings -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBackClick = { viewModel.navigateBack() }
                            )
                        }

                        is Screen.EditProfile -> {
                            ProfileScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainContainer(viewModel: AppViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    BackHandler(enabled = currentTab != BottomNavTab.HOME) {
        viewModel.switchTab(BottomNavTab.HOME)
    }

    Scaffold(
        topBar = {
            if (currentTab == BottomNavTab.HOME) {
                BengaliTopAppBar(
                    currentUser = currentUser,
                    unreadCount = unreadCount,
                    onSearchClick = { viewModel.switchTab(BottomNavTab.DISCOVER) },
                    onNotificationsClick = { viewModel.navigateTo(Screen.Notifications) },
                    onProfileClick = { viewModel.switchTab(BottomNavTab.PROFILE) }
                )
            }
        },
        bottomBar = {
            BengaliBottomNavBar(
                selectedTab = currentTab,
                onTabSelected = { viewModel.switchTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                BottomNavTab.HOME -> HomeScreen(viewModel = viewModel)
                BottomNavTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                BottomNavTab.DISCOVER -> DiscoverScreen(viewModel = viewModel)
                BottomNavTab.COMMUNITY -> CommunityScreen(viewModel = viewModel)
                BottomNavTab.PROFILE -> ProfileScreen(viewModel = viewModel)
            }
        }
    }
}
