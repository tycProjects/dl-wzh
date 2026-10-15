package com.almoazin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almoazin.app.data.CitiesData
import com.almoazin.app.ui.components.HeroPrayerCard
import com.almoazin.app.ui.components.PrayerTimesGrid
import com.almoazin.app.ui.screens.QiblaScreen
import com.almoazin.app.ui.screens.QuranScreen
import com.almoazin.app.ui.screens.TasbihScreen
import com.almoazin.app.ui.theme.AlMoazinTheme
import com.almoazin.app.ui.theme.BorderLightBlue
import com.almoazin.app.ui.theme.PureWhite
import com.almoazin.app.ui.theme.RoyalBluePrimary
import com.almoazin.app.utils.AthanAudioPlayer
import com.almoazin.app.utils.CompassSensorManager
import com.almoazin.app.utils.PrayerCalculator
import kotlinx.coroutines.delay
import java.util.Date

enum class AppTab(val titleAr: String, val icon: ImageVector) {
    PRAYERS("المواقيت", Icons.Default.Schedule),
    QURAN("القرآن", Icons.Default.MenuBook),
    QIBLA("القبلة", Icons.Default.Explore),
    TASBIH("السبحة", Icons.Default.FormatListNumbered)
}

class MainActivity : ComponentActivity() {

    private lateinit var audioPlayer: AthanAudioPlayer
    private lateinit var compassSensor: CompassSensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        audioPlayer = AthanAudioPlayer(applicationContext)
        compassSensor = CompassSensorManager(applicationContext)

        setContent {
            AlMoazinTheme {
                MainAppScaffold(audioPlayer, compassSensor)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        compassSensor.start()
    }

    override fun onPause() {
        super.onPause()
        compassSensor.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.release()
    }
}

@Composable
fun MainAppScaffold(
    audioPlayer: AthanAudioPlayer,
    compassSensor: CompassSensorManager
) {
    var currentTab by remember { mutableStateOf(AppTab.PRAYERS) }
    var selectedCity by remember { mutableStateOf(CitiesData.CITIES[0]) }
    var now by remember { mutableStateOf(Date()) }

    val isPlayingAthan by audioPlayer.isPlaying.collectAsState()
    val completedPrayers = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1000)
        }
    }

    val prayerTimes = remember(now, selectedCity) {
        PrayerCalculator.calculatePrayerTimes(
            now,
            selectedCity.lat,
            selectedCity.lng,
            selectedCity.timezone
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = PureWhite,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.titleAr) },
                        label = { Text(tab.titleAr, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RoyalBluePrimary,
                            selectedTextColor = RoyalBluePrimary,
                            indicatorColor = BorderLightBlue
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.PRAYERS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        HeroPrayerCard(
                            cityName = selectedCity.nameAr,
                            countryName = selectedCity.countryAr,
                            nextPrayerName = "الظهر",
                            nextPrayerTime = prayerTimes.dhuhr,
                            hoursRemaining = "01",
                            minutesRemaining = "24",
                            secondsRemaining = "15",
                            isPlayingAthan = isPlayingAthan,
                            onToggleAthan = {
                                if (isPlayingAthan) audioPlayer.stop()
                                else audioPlayer.playAthan(AthanAudioPlayer.RECITERS[0])
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        PrayerTimesGrid(
                            times = prayerTimes,
                            nextPrayerName = "الظهر",
                            completedMap = completedPrayers,
                            onToggleComplete = { key ->
                                completedPrayers[key] = !(completedPrayers[key] ?: false)
                            }
                        )
                    }
                }
                AppTab.QURAN -> QuranScreen(audioPlayer)
                AppTab.QIBLA -> QiblaScreen(selectedCity.lat, selectedCity.lng, compassSensor)
                AppTab.TASBIH -> TasbihScreen()
            }
        }
    }
}
