package dev.tajim.jarvis.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.tajim.jarvis.AppContainer
import dev.tajim.jarvis.assistant.AssistantPhase
import dev.tajim.jarvis.assistant.AssistantState
import dev.tajim.jarvis.data.UserSettings
import dev.tajim.jarvis.ui.orb.OrbState
import dev.tajim.jarvis.weather.Weather
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WeatherUi {
    data object Loading : WeatherUi
    data object NeedsLocation : WeatherUi
    data class Ready(val weather: Weather) : WeatherUi
    data class Error(val message: String) : WeatherUi
}

data class HomeUiState(
    val settings: UserSettings = UserSettings(),
    val isOnline: Boolean = true,
    val assistant: AssistantState = AssistantState(),
    val micLevel: Float = 0f,
    val memoryCount: Int = 0,
    val weather: WeatherUi = WeatherUi.Loading,
) {
    /** Orb state comes only from real signals: the assistant's actual phase and real connectivity. */
    val orbState: OrbState
        get() = when {
            assistant.phase == AssistantPhase.LISTENING -> OrbState.LISTENING
            assistant.phase == AssistantPhase.THINKING -> OrbState.PROCESSING
            assistant.phase == AssistantPhase.SPEAKING -> OrbState.SPEAKING
            !isOnline -> OrbState.OFFLINE
            else -> OrbState.IDLE
        }
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val weather = MutableStateFlow<WeatherUi>(WeatherUi.Loading)
    private var lastFetch = 0L

    private val base = combine(
        container.settingsRepository.settings, container.networkMonitor.isOnline, container.assistant.state,
    ) { s, online, a -> Triple(s, online, a) }

    val uiState: StateFlow<HomeUiState> = combine(
        base, container.voice.level, container.memoryRepository.items, weather,
    ) { b, level, mem, w ->
        HomeUiState(settings = b.first, isOnline = b.second, assistant = b.third, micLevel = level, memoryCount = mem.size, weather = w)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun tapToTalk() = container.assistant.tapToTalk()
    fun clearNotice() = container.assistant.clearNotice()

    /** Fetches real weather (throttled to once per 20 minutes unless [force]). */
    fun refreshWeather(force: Boolean = false) {
        viewModelScope.launch {
            val fresh = System.currentTimeMillis() - lastFetch < 20 * 60_000
            if (!force && fresh && weather.value is WeatherUi.Ready) return@launch
            val s = container.settingsRepository.current()
            val repo = container.weatherRepository
            val lat: Double
            val lon: Double
            val place: String
            if (!s.useDeviceLocation && s.weatherLat != null && s.weatherLon != null) {
                lat = s.weatherLat; lon = s.weatherLon; place = s.weatherCity
            } else {
                val loc = repo.deviceLocation()
                if (loc == null) { weather.value = WeatherUi.NeedsLocation; return@launch }
                lat = loc.first; lon = loc.second
                place = repo.placeName(lat, lon) ?: "Your location"
            }
            if (weather.value !is WeatherUi.Ready) weather.value = WeatherUi.Loading
            repo.fetch(lat, lon, place).fold(
                onSuccess = { container.cachedWeather = it; lastFetch = System.currentTimeMillis(); weather.value = WeatherUi.Ready(it) },
                onFailure = { weather.value = WeatherUi.Error(it.message ?: "Weather is unavailable.") },
            )
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory { initializer { HomeViewModel(container) } }
    }
}
