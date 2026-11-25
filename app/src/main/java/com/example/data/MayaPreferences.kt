package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "maya_settings")

data class MayaSettings(
    val geminiModel: String = "gemini-3.8-flash",
    val useCustomGeminiKey: Boolean = false,
    val elevenLabsVoiceId: String = "21m00Tcm4TlvDq8ikWAM",
    val elevenLabsVoiceName: String = "Rachel (Warm Female)",
    val elevenLabsModel: String = "eleven_flash_v2_5",
    val useElevenLabs: Boolean = false,
    val fallbackToAndroidTts: Boolean = true,
    val voiceSpeed: Float = 1.05f,
    val voiceVolume: Float = 0.95f,
    val languageMode: String = "bn-BD",
    val fastLocalCommands: Boolean = true,
    val streamingTts: Boolean = true,
    val autoListenAfterReply: Boolean = false,
    val confirmSensitiveActions: Boolean = true,
    val batteryAwareBackground: Boolean = true
)

class MayaPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val securePrefs: SharedPreferences =
        appContext.getSharedPreferences("maya_secure_credentials", Context.MODE_PRIVATE)

    private object Keys {
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val USE_CUSTOM_GEMINI_KEY = booleanPreferencesKey("use_custom_gemini_key")
        val ELEVENLABS_VOICE_ID = stringPreferencesKey("elevenlabs_voice_id")
        val ELEVENLABS_VOICE_NAME = stringPreferencesKey("elevenlabs_voice_name")
        val ELEVENLABS_MODEL = stringPreferencesKey("elevenlabs_model")
        val USE_ELEVENLABS = booleanPreferencesKey("use_elevenlabs")
        val FALLBACK_TTS = booleanPreferencesKey("fallback_tts")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val FAST_LOCAL_COMMANDS = booleanPreferencesKey("fast_local_commands")
        val STREAMING_TTS = booleanPreferencesKey("streaming_tts")
        val AUTO_LISTEN_AFTER_REPLY = booleanPreferencesKey("auto_listen_after_reply")
        val CONFIRM_SENSITIVE = booleanPreferencesKey("confirm_sensitive")
        val BATTERY_AWARE_BG = booleanPreferencesKey("battery_aware_bg")
    }

    val settingsFlow: Flow<MayaSettings> = appContext.dataStore.data.map { prefs ->
        MayaSettings(
            geminiModel = prefs[Keys.GEMINI_MODEL] ?: "gemini-3.8-flash",
            useCustomGeminiKey = prefs[Keys.USE_CUSTOM_GEMINI_KEY] ?: false,
            elevenLabsVoiceId = prefs[Keys.ELEVENLABS_VOICE_ID] ?: "21m00Tcm4TlvDq8ikWAM",
            elevenLabsVoiceName = prefs[Keys.ELEVENLABS_VOICE_NAME] ?: "Rachel (Warm Female)",
            elevenLabsModel = prefs[Keys.ELEVENLABS_MODEL] ?: "eleven_flash_v2_5",
            useElevenLabs = prefs[Keys.USE_ELEVENLABS] ?: false,
            fallbackToAndroidTts = prefs[Keys.FALLBACK_TTS] ?: true,
            voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.05f,
            voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.95f,
            languageMode = prefs[Keys.LANGUAGE_MODE] ?: "bn-BD",
            fastLocalCommands = prefs[Keys.FAST_LOCAL_COMMANDS] ?: true,
            streamingTts = prefs[Keys.STREAMING_TTS] ?: true,
            autoListenAfterReply = prefs[Keys.AUTO_LISTEN_AFTER_REPLY] ?: false,
            confirmSensitiveActions = prefs[Keys.CONFIRM_SENSITIVE] ?: true,
            batteryAwareBackground = prefs[Keys.BATTERY_AWARE_BG] ?: true
        )
    }

    private val _hasCustomGeminiKey = MutableStateFlow(getCustomGeminiApiKey().isNotBlank())
    val hasCustomGeminiKey: StateFlow<Boolean> = _hasCustomGeminiKey.asStateFlow()

    private val _hasElevenLabsKey = MutableStateFlow(getElevenLabsApiKey().isNotBlank())
    val hasElevenLabsKey: StateFlow<Boolean> = _hasElevenLabsKey.asStateFlow()

    fun getCustomGeminiApiKey(): String = securePrefs.getString("custom_gemini_key", "") ?: ""

    fun saveCustomGeminiApiKey(key: String) {
        securePrefs.edit().putString("custom_gemini_key", key.trim()).apply()
        _hasCustomGeminiKey.value = key.trim().isNotBlank()
    }

    fun getElevenLabsApiKey(): String = securePrefs.getString("elevenlabs_api_key", "") ?: ""

    fun saveElevenLabsApiKey(key: String) {
        securePrefs.edit().putString("elevenlabs_api_key", key.trim()).apply()
        _hasElevenLabsKey.value = key.trim().isNotBlank()
    }

    suspend fun updateSettings(transform: (MayaSettings) -> MayaSettings) {
        appContext.dataStore.edit { prefs ->
            val current = MayaSettings(
                geminiModel = prefs[Keys.GEMINI_MODEL] ?: "gemini-3.8-flash",
                useCustomGeminiKey = prefs[Keys.USE_CUSTOM_GEMINI_KEY] ?: false,
                elevenLabsVoiceId = prefs[Keys.ELEVENLABS_VOICE_ID] ?: "21m00Tcm4TlvDq8ikWAM",
                elevenLabsVoiceName = prefs[Keys.ELEVENLABS_VOICE_NAME] ?: "Rachel (Warm Female)",
                elevenLabsModel = prefs[Keys.ELEVENLABS_MODEL] ?: "eleven_flash_v2_5",
                useElevenLabs = prefs[Keys.USE_ELEVENLABS] ?: false,
                fallbackToAndroidTts = prefs[Keys.FALLBACK_TTS] ?: true,
                voiceSpeed = prefs[Keys.VOICE_SPEED] ?: 1.05f,
                voiceVolume = prefs[Keys.VOICE_VOLUME] ?: 0.95f,
                languageMode = prefs[Keys.LANGUAGE_MODE] ?: "bn-BD",
                fastLocalCommands = prefs[Keys.FAST_LOCAL_COMMANDS] ?: true,
                streamingTts = prefs[Keys.STREAMING_TTS] ?: true,
                autoListenAfterReply = prefs[Keys.AUTO_LISTEN_AFTER_REPLY] ?: false,
                confirmSensitiveActions = prefs[Keys.CONFIRM_SENSITIVE] ?: true,
                batteryAwareBackground = prefs[Keys.BATTERY_AWARE_BG] ?: true
            )
            val updated = transform(current)
            prefs[Keys.GEMINI_MODEL] = updated.geminiModel
            prefs[Keys.USE_CUSTOM_GEMINI_KEY] = updated.useCustomGeminiKey
            prefs[Keys.ELEVENLABS_VOICE_ID] = updated.elevenLabsVoiceId
            prefs[Keys.ELEVENLABS_VOICE_NAME] = updated.elevenLabsVoiceName
            prefs[Keys.ELEVENLABS_MODEL] = updated.elevenLabsModel
            prefs[Keys.USE_ELEVENLABS] = updated.useElevenLabs
            prefs[Keys.FALLBACK_TTS] = updated.fallbackToAndroidTts
            prefs[Keys.VOICE_SPEED] = updated.voiceSpeed
            prefs[Keys.VOICE_VOLUME] = updated.voiceVolume
            prefs[Keys.LANGUAGE_MODE] = updated.languageMode
            prefs[Keys.FAST_LOCAL_COMMANDS] = updated.fastLocalCommands
            prefs[Keys.STREAMING_TTS] = updated.streamingTts
            prefs[Keys.AUTO_LISTEN_AFTER_REPLY] = updated.autoListenAfterReply
            prefs[Keys.CONFIRM_SENSITIVE] = updated.confirmSensitiveActions
            prefs[Keys.BATTERY_AWARE_BG] = updated.batteryAwareBackground
        }
    }
}
