package dev.tajim.jarvis.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { DARK, LIGHT }

enum class Persona(val label: String, val prompt: String) {
    FRIENDLY("Friendly", "Be warm, friendly and conversational."),
    FORMAL("Formal", "Be polite, formal and precise."),
    CONCISE("Concise", "Be extremely concise. Use as few words as possible."),
    TECHNICAL("Technical", "Be technical and exact. Include relevant details and terminology."),
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val displayName: String = "",
    val reducedMotion: Boolean = false,
    /** 0.25..1.0 multiplier for orb and ambient animation speed. */
    val animationIntensity: Float = 1f,
    /** BCP-47 tag for speech recognition, or "" to use the device language. */
    val voiceLanguage: String = "",
    val persona: Persona = Persona.FRIENDLY,
    val customInstructions: String = "",
    val speechRate: Float = 1f,
    val speakReplies: Boolean = true,
    val wakeWordEnabled: Boolean = true,
    /** Whether the background assistant should be started when the app opens. */
    val assistantEnabled: Boolean = false,
    val aiModel: String = "",
    val hasApiKey: Boolean = false,
    val useMemories: Boolean = true,
    val useDeviceLocation: Boolean = true,
    val weatherCity: String = "",
    val weatherLat: Double? = null,
    val weatherLon: Double? = null,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val name = stringPreferencesKey("display_name")
        val reduced = booleanPreferencesKey("reduced_motion")
        val intensity = floatPreferencesKey("animation_intensity")
        val voiceLanguage = stringPreferencesKey("voice_language")
        val persona = stringPreferencesKey("persona")
        val custom = stringPreferencesKey("custom_instructions")
        val rate = floatPreferencesKey("speech_rate")
        val speak = booleanPreferencesKey("speak_replies")
        val wake = booleanPreferencesKey("wake_word_enabled")
        val assistant = booleanPreferencesKey("assistant_enabled")
        val model = stringPreferencesKey("ai_model")
        val apiKey = stringPreferencesKey("api_key_enc")
        val memories = booleanPreferencesKey("use_memories")
        val deviceLoc = booleanPreferencesKey("use_device_location")
        val city = stringPreferencesKey("weather_city")
        val lat = doublePreferencesKey("weather_lat")
        val lon = doublePreferencesKey("weather_lon")
    }

    val settings: Flow<UserSettings> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            UserSettings(
                themeMode = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
                displayName = p[Keys.name].orEmpty(),
                reducedMotion = p[Keys.reduced] ?: false,
                animationIntensity = (p[Keys.intensity] ?: 1f).coerceIn(0.25f, 1f),
                voiceLanguage = p[Keys.voiceLanguage].orEmpty(),
                persona = p[Keys.persona]?.let { runCatching { Persona.valueOf(it) }.getOrNull() } ?: Persona.FRIENDLY,
                customInstructions = p[Keys.custom].orEmpty(),
                speechRate = (p[Keys.rate] ?: 1f).coerceIn(0.5f, 2f),
                speakReplies = p[Keys.speak] ?: true,
                wakeWordEnabled = p[Keys.wake] ?: true,
                assistantEnabled = p[Keys.assistant] ?: false,
                aiModel = p[Keys.model].orEmpty(),
                hasApiKey = !p[Keys.apiKey].isNullOrEmpty(),
                useMemories = p[Keys.memories] ?: true,
                useDeviceLocation = p[Keys.deviceLoc] ?: true,
                weatherCity = p[Keys.city].orEmpty(),
                weatherLat = p[Keys.lat],
                weatherLon = p[Keys.lon],
            )
        }

    suspend fun current(): UserSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) { store.edit { it[Keys.theme] = mode.name } }
    suspend fun setDisplayName(name: String) { store.edit { it[Keys.name] = name.trim().take(40) } }
    suspend fun setReducedMotion(value: Boolean) { store.edit { it[Keys.reduced] = value } }
    suspend fun setAnimationIntensity(value: Float) { store.edit { it[Keys.intensity] = value.coerceIn(0.25f, 1f) } }
    suspend fun setVoiceLanguage(tag: String) { store.edit { it[Keys.voiceLanguage] = tag } }
    suspend fun setPersona(p: Persona) { store.edit { it[Keys.persona] = p.name } }
    suspend fun setCustomInstructions(text: String) { store.edit { it[Keys.custom] = text.take(600) } }
    suspend fun setSpeechRate(v: Float) { store.edit { it[Keys.rate] = v.coerceIn(0.5f, 2f) } }
    suspend fun setSpeakReplies(v: Boolean) { store.edit { it[Keys.speak] = v } }
    suspend fun setWakeWordEnabled(v: Boolean) { store.edit { it[Keys.wake] = v } }
    suspend fun setAssistantEnabled(v: Boolean) { store.edit { it[Keys.assistant] = v } }
    suspend fun setAiModel(model: String) { store.edit { it[Keys.model] = model.trim() } }
    suspend fun setUseMemories(v: Boolean) { store.edit { it[Keys.memories] = v } }
    suspend fun setUseDeviceLocation(v: Boolean) { store.edit { it[Keys.deviceLoc] = v } }

    /** Saved encrypted with the Android Keystore; never logged, exported or shown again. */
    suspend fun setApiKey(key: String) {
        val trimmed = key.trim()
        store.edit { if (trimmed.isEmpty()) it.remove(Keys.apiKey) else it[Keys.apiKey] = SecretStore.encrypt(trimmed) }
    }

    suspend fun apiKey(): String? =
        runCatching { store.data.first()[Keys.apiKey] }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { SecretStore.decrypt(it) }

    suspend fun setWeatherPlace(city: String, lat: Double, lon: Double) {
        store.edit { it[Keys.city] = city; it[Keys.lat] = lat; it[Keys.lon] = lon; it[Keys.deviceLoc] = false }
    }
}
