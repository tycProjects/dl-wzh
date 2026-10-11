package dev.tajim.jarvis.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.tajim.jarvis.AppContainer
import dev.tajim.jarvis.ai.Turn
import dev.tajim.jarvis.data.Persona
import dev.tajim.jarvis.data.Role
import dev.tajim.jarvis.data.ThemeMode
import dev.tajim.jarvis.data.UserSettings
import dev.tajim.jarvis.weather.GeoPlace
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ModelsUi {
    data object Idle : ModelsUi
    data object Loading : ModelsUi
    data class Ready(val models: List<String>) : ModelsUi
    data class Error(val message: String) : ModelsUi
}

sealed interface CitiesUi {
    data object Idle : CitiesUi
    data object Loading : CitiesUi
    data class Results(val places: List<GeoPlace>) : CitiesUi
    data class Error(val message: String) : CitiesUi
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    private val repo = container.settingsRepository

    val settings: StateFlow<UserSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())
    val assistant = container.assistant.state

    var models by mutableStateOf<ModelsUi>(ModelsUi.Idle); private set
    var testResult by mutableStateOf<String?>(null); private set
    var cities by mutableStateOf<CitiesUi>(CitiesUi.Idle); private set

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }

    fun setThemeMode(m: ThemeMode) = launch { repo.setThemeMode(m) }
    fun setDisplayName(n: String) = launch { repo.setDisplayName(n) }
    fun setReducedMotion(v: Boolean) = launch { repo.setReducedMotion(v) }
    fun setAnimationIntensity(v: Float) = launch { repo.setAnimationIntensity(v) }
    fun setVoiceLanguage(tag: String) = launch { repo.setVoiceLanguage(tag) }
    fun setPersona(p: Persona) = launch { repo.setPersona(p) }
    fun setCustomInstructions(t: String) = launch { repo.setCustomInstructions(t) }
    fun setSpeechRate(v: Float) = launch { repo.setSpeechRate(v) }
    fun setSpeakReplies(v: Boolean) = launch { repo.setSpeakReplies(v) }
    fun setWakeWordEnabled(v: Boolean) = launch { repo.setWakeWordEnabled(v) }
    fun setAssistantEnabled(v: Boolean) = launch { repo.setAssistantEnabled(v) }
    fun setUseMemories(v: Boolean) = launch { repo.setUseMemories(v) }
    fun setUseDeviceLocation(v: Boolean) = launch { repo.setUseDeviceLocation(v) }
    fun selectModel(m: String) = launch { repo.setAiModel(m) }
    fun saveApiKey(k: String) = launch { repo.setApiKey(k); models = ModelsUi.Idle; testResult = null }
    fun removeApiKey() = launch { repo.setApiKey(""); repo.setAiModel(""); models = ModelsUi.Idle; testResult = null }

    fun wakeNow() = container.assistant.wakeNow()
    fun sleepNow() = container.assistant.sleepNow()
    fun wakeWordModelBundled() = container.wakeWord.isModelBundled()
    fun testVoice() = launch { container.speaker.speak("Hello, I am Jarvis.", repo.current().speechRate) }

    /** Asks Google which models this key can use, so no model name is ever guessed. */
    fun loadModels() = launch {
        val key = repo.apiKey()
        if (key == null) { models = ModelsUi.Error("Save your API key first."); return@launch }
        models = ModelsUi.Loading
        models = container.aiProvider.listModels(key).fold({ ModelsUi.Ready(it) }, { ModelsUi.Error(it.message ?: "Could not load models.") })
    }

    fun testConnection() = launch {
        val key = repo.apiKey()
        val model = repo.current().aiModel
        if (key == null || model.isBlank()) { testResult = "Save a key and choose a model first."; return@launch }
        testResult = "Testing…"
        testResult = container.aiProvider.chat(key, model, "", listOf(Turn(Role.USER, "Reply with the single word OK")))
            .fold({ "Connected. Model replied: ${it.take(40)}" }, { "Failed: ${it.message}" })
    }

    fun searchCity(q: String) = launch {
        if (q.isBlank()) return@launch
        cities = CitiesUi.Loading
        cities = container.weatherRepository.searchCity(q).fold(
            { if (it.isEmpty()) CitiesUi.Error("No city found.") else CitiesUi.Results(it) },
            { CitiesUi.Error(it.message ?: "City search failed.") },
        )
    }

    fun pickCity(p: GeoPlace) = launch { repo.setWeatherPlace(p.name, p.lat, p.lon); cities = CitiesUi.Idle }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory { initializer { SettingsViewModel(container) } }
    }
}
