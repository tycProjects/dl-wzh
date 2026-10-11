package dev.tajim.jarvis

import android.app.Application
import android.content.Context
import dev.tajim.jarvis.ai.AiService
import dev.tajim.jarvis.ai.GeminiProvider
import dev.tajim.jarvis.assistant.AssistantController
import dev.tajim.jarvis.core.NetworkMonitor
import dev.tajim.jarvis.data.ChatRepository
import dev.tajim.jarvis.data.DraftStore
import dev.tajim.jarvis.data.MemoryRepository
import dev.tajim.jarvis.data.SettingsRepository
import dev.tajim.jarvis.phone.CommandRouter
import dev.tajim.jarvis.phone.PhoneControl
import dev.tajim.jarvis.voice.Speaker
import dev.tajim.jarvis.voice.VoiceController
import dev.tajim.jarvis.voice.WakeWordEngine
import dev.tajim.jarvis.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency container. Hilt can replace this when the object graph justifies it. */
class AppContainer(context: Context) {
    /** Main-thread scope that lives as long as the process. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settingsRepository = SettingsRepository(context)
    val networkMonitor = NetworkMonitor(context)
    val draftStore = DraftStore(context)
    val chatRepository = ChatRepository(context, appScope)
    val memoryRepository = MemoryRepository(context, appScope)
    val weatherRepository = WeatherRepository(context)
    val voice = VoiceController(context)
    val speaker = Speaker(context)
    val wakeWord = WakeWordEngine(context)
    val aiProvider = GeminiProvider()
    val aiService = AiService(settingsRepository, aiProvider, chatRepository, memoryRepository)
    val commandRouter = CommandRouter()
    val phoneControl = PhoneControl(context) { latestWeather() }
    val assistant = AssistantController(appScope, settingsRepository, aiService, voice, speaker, wakeWord, phoneControl, commandRouter)

    /** Most recent successful weather, shared by the Home card and the "what's the weather" voice command. */
    @Volatile var cachedWeather: dev.tajim.jarvis.weather.Weather? = null

    private suspend fun latestWeather() = cachedWeather
}

class JarvisApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
