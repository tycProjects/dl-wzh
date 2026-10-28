package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.auth.AuthManager
import com.example.auth.AuthState
import com.example.data.ZonexRepository
import com.example.model.AppConfig
import com.example.model.Category
import com.example.model.NotificationItem
import com.example.model.Sound
import com.example.model.UserProfile
import com.example.model.UserSound
import com.example.ui.components.ZonexBottomNavigation
import com.example.ui.components.ZonexScreen
import com.example.ui.components.ZonexTopBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.MandatoryUpdateDialog
import com.example.ui.screens.MixerScreen
import com.example.ui.screens.MovableFloatingSoundboard
import com.example.ui.screens.MySoundsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TelegramJoinDialog
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberDark
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ZonexVoiceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var soundManager: SoundManager
    private lateinit var repository: ZonexRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        authManager = AuthManager(applicationContext)
        soundManager = SoundManager(applicationContext)
        repository = ZonexRepository(applicationContext)

        setContent {
            ZonexVoiceTheme {
                MainAppContainer(
                    authManager = authManager,
                    soundManager = soundManager,
                    repository = repository
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}

@Composable
fun MainAppContainer(
    authManager: AuthManager,
    soundManager: SoundManager,
    repository: ZonexRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val installedVersion = "1.0" // Current app release version

    // Auth state
    val authState by authManager.authState.collectAsState()
    val currentUser = authManager.currentUser

    // Repository flows
    val config by repository.getAppConfigFlow().collectAsState(initial = AppConfig())
    val categories by repository.getCategoriesFlow().collectAsState(initial = emptyList())
    val sounds by repository.getSoundsFlow().collectAsState(initial = emptyList())
    val notifications by repository.getNotificationsFlow().collectAsState(initial = emptyList())

    val userProfile by repository.getUserProfileFlow(currentUser?.uid ?: "").collectAsState(initial = null)
    val mySounds by repository.getMySoundsFlow(currentUser?.uid ?: "").collectAsState(initial = emptyList())

    // Audio states
    val currentlyPlayingId by soundManager.currentlyPlayingId.collectAsState()
    val isPlaying by soundManager.isPlaying.collectAsState()
    val masterVolume by soundManager.masterVolume.collectAsState()
    val mixerChannelPlaying by soundManager.mixerChannelPlaying.collectAsState()

    // Navigation and Modals state
    var currentScreen by remember { mutableStateOf(ZonexScreen.HOME) }
    var showFloatingSoundboard by remember { mutableStateOf(false) }
    var showTelegramPopup by remember { mutableStateOf(true) }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }
    var pendingRewardedDownloadSound by remember { mutableStateOf<Sound?>(null) }
    var soundPlayCountSinceLastAd by remember { mutableIntStateOf(0) }

    // Synchronize user profile on successful login
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            repository.ensureUserProfile(
                userId = user.uid,
                email = user.email ?: "",
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString()
            )
        }
    }

    val favorites = userProfile?.favorites ?: emptyList()

    // Maintenance Mode Screen Gate
    if (config.maintenance_mode) {
        MaintenanceScreen(
            config = config,
            onCheckStatus = {
                Toast.makeText(context, "Checking maintenance status...", Toast.LENGTH_SHORT).show()
            }
        )
        return
    }

    // Auth Gate: Check if user is authenticated
    if (currentUser == null) {
        AuthScreen(
            isLoading = authState is AuthState.Loading,
            errorMessage = if (authState is AuthState.Error) (authState as AuthState.Error).message else authErrorMessage,
            onGoogleSignIn = {
                authErrorMessage = null
                coroutineScope.launch {
                    val result = authManager.signInWithGoogle()
                    if (result.isFailure) {
                        authErrorMessage = result.exceptionOrNull()?.localizedMessage
                    }
                }
            },
            onEmailSignIn = { email, pass ->
                authErrorMessage = null
                coroutineScope.launch {
                    val result = authManager.signInWithEmail(email, pass)
                    if (result.isFailure) {
                        authErrorMessage = result.exceptionOrNull()?.localizedMessage
                    }
                }
            },
            onEmailRegister = { email, pass ->
                authErrorMessage = null
                coroutineScope.launch {
                    val result = authManager.registerWithEmail(email, pass)
                    if (result.isFailure) {
                        authErrorMessage = result.exceptionOrNull()?.localizedMessage
                    }
                }
            },
            onForgotPassword = { email ->
                coroutineScope.launch {
                    authManager.sendPasswordReset(email)
                }
            }
        )
        return
    }

    // Main App Shell
    Scaffold(
        topBar = {
            ZonexTopBar(
                title = currentScreen.title,
                unreadNotificationCount = notifications.size,
                onOpenNotifications = {
                    currentScreen = ZonexScreen.PROFILE
                },
                onToggleFloatingSoundboard = {
                    showFloatingSoundboard = !showFloatingSoundboard
                }
            )
        },
        bottomBar = {
            ZonexBottomNavigation(
                currentScreen = currentScreen,
                onSelectScreen = { currentScreen = it }
            )
        },
        containerColor = CyberBlack,
        modifier = Modifier.fillMaxSize().testTag("main_app_scaffold")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ZonexScreen.HOME -> {
                    HomeScreen(
                        sounds = sounds,
                        categories = categories,
                        config = config,
                        currentlyPlayingId = currentlyPlayingId,
                        isPlaying = isPlaying,
                        favorites = favorites,
                        onPlaySound = { sound ->
                            soundPlayCountSinceLastAd++
                            if (config.ad_interstitial_enabled && soundPlayCountSinceLastAd >= 5) {
                                soundPlayCountSinceLastAd = 0
                                Toast.makeText(context, "[AdMob Interstitial Break]", Toast.LENGTH_SHORT).show()
                            }
                            soundManager.playSound(sound.id, sound.audio_url)
                        },
                        onToggleFavorite = { sound ->
                            coroutineScope.launch {
                                repository.toggleFavorite(currentUser.uid, favorites, sound.id)
                            }
                        },
                        onDownloadSound = { sound ->
                            if (config.ad_rewarded_enabled && sound.requires_rewarded_ad) {
                                pendingRewardedDownloadSound = sound
                            } else {
                                Toast.makeText(context, "Downloading \"${sound.title}\"...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReplaySound = { sound ->
                            soundManager.replaySound(sound.id, sound.audio_url)
                        }
                    )
                }

                ZonexScreen.CATEGORIES -> {
                    CategoriesScreen(
                        categories = categories,
                        sounds = sounds,
                        config = config,
                        currentlyPlayingId = currentlyPlayingId,
                        isPlaying = isPlaying,
                        favorites = favorites,
                        onPlaySound = { sound ->
                            soundManager.playSound(sound.id, sound.audio_url)
                        },
                        onToggleFavorite = { sound ->
                            coroutineScope.launch {
                                repository.toggleFavorite(currentUser.uid, favorites, sound.id)
                            }
                        },
                        onDownloadSound = { sound ->
                            Toast.makeText(context, "Downloading \"${sound.title}\"...", Toast.LENGTH_SHORT).show()
                        },
                        onReplaySound = { sound ->
                            soundManager.replaySound(sound.id, sound.audio_url)
                        }
                    )
                }

                ZonexScreen.MY_SOUNDS -> {
                    MySoundsScreen(
                        mySounds = mySounds,
                        config = config,
                        currentlyPlayingId = currentlyPlayingId,
                        isPlaying = isPlaying,
                        onPlaySound = { id, url ->
                            soundManager.playSound(id, url)
                        },
                        onStopSound = {
                            soundManager.stopSound()
                        },
                        onAddSound = { title, url, duration ->
                            coroutineScope.launch {
                                repository.saveUserSound(currentUser.uid, title, url, duration)
                                Toast.makeText(context, "Sound saved to private vault!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRenameSound = { soundId, newTitle ->
                            coroutineScope.launch {
                                repository.renameUserSound(currentUser.uid, soundId, newTitle)
                                Toast.makeText(context, "Sound renamed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteSound = { soundId ->
                            coroutineScope.launch {
                                repository.deleteUserSound(currentUser.uid, soundId)
                                Toast.makeText(context, "Sound deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                ZonexScreen.MIXER -> {
                    MixerScreen(
                        availableSounds = sounds,
                        masterVolume = masterVolume,
                        channelPlayingMap = mixerChannelPlaying,
                        onSetMasterVolume = { soundManager.setMasterVolume(it) },
                        onPlayChannel = { chId, url, vol ->
                            soundManager.playMixerChannel(chId, url, vol)
                        },
                        onStopChannel = { chId ->
                            soundManager.stopMixerChannel(chId)
                        },
                        onSetChannelVolume = { chId, vol ->
                            soundManager.setChannelVolume(chId, vol)
                        },
                        onToggleChannelMute = { chId ->
                            soundManager.toggleChannelMute(chId)
                        },
                        isChannelMuted = { chId ->
                            soundManager.isChannelMuted(chId)
                        },
                        onStopAll = {
                            soundManager.stopAll()
                        }
                    )
                }

                ZonexScreen.PROFILE -> {
                    ProfileScreen(
                        userProfile = userProfile,
                        email = currentUser.email,
                        notifications = notifications,
                        favoritesCount = favorites.size,
                        mySoundsCount = mySounds.size,
                        config = config,
                        onSignOut = {
                            authManager.signOut()
                        },
                        onOpenTelegram = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.telegram_url)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }

            // Movable Floating Soundboard Window (Accessible across all screens)
            if (showFloatingSoundboard) {
                MovableFloatingSoundboard(
                    sounds = sounds,
                    onPlaySound = { sound ->
                        soundManager.playSound(sound.id, sound.audio_url)
                    },
                    onStopAll = {
                        soundManager.stopAll()
                    },
                    onClose = {
                        showFloatingSoundboard = false
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    // Mandatory Force Update Dialog Check
    val isVersionOutdated = try {
        val installedNum = installedVersion.toDoubleOrNull() ?: 1.0
        val minRequiredNum = config.min_version.toDoubleOrNull() ?: 1.0
        installedNum < minRequiredNum
    } catch (e: Exception) {
        false
    }

    if (config.force_update && isVersionOutdated) {
        MandatoryUpdateDialog(
            config = config,
            installedVersion = installedVersion
        )
    }

    // Telegram Join Dialog (shows once on app launch if enabled)
    if (showTelegramPopup && config.telegram_popup_enabled && !(config.force_update && isVersionOutdated)) {
        TelegramJoinDialog(
            config = config,
            onDismiss = { showTelegramPopup = false }
        )
    }

    // Rewarded Ad Dialog before eligible download
    pendingRewardedDownloadSound?.let { sound ->
        AlertDialog(
            onDismissRequest = { pendingRewardedDownloadSound = null },
            title = { Text("Watch Gaming Sponsor Ad", color = Color.White) },
            text = {
                Text(
                    text = "Watch a short rewarded ad to download \"${sound.title}\" for free according to Admin settings.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "[AdMob Rewarded Ad Playing...]", Toast.LENGTH_SHORT).show()
                        Toast.makeText(context, "Download complete for ${sound.title}!", Toast.LENGTH_LONG).show()
                        pendingRewardedDownloadSound = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Watch & Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRewardedDownloadSound = null }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CyberDark
        )
    }
}
