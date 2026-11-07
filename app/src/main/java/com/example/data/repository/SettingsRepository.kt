package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.playback.RepeatState
import com.example.playback.VideoPlaybackMode
import com.example.playback.VideoQueueMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "jnmf_settings")

enum class ThemeColor(val key: String, val label: String) {
    NEON_GREEN("neon_green", "Green (Default)"),
    ELECTRIC_BLUE("electric_blue", "Blue"),
    CRIMSON_RED("crimson_red", "Red"),
    MODERN_WHITE("modern_white", "White"),
    VIBRANT_CYAN("vibrant_cyan", "Cyan"),
    DEEP_VIOLET("deep_violet", "Violet");

    companion object {
        fun fromKey(key: String?): ThemeColor {
            return entries.find { it.key == key } ?: NEON_GREEN
        }
    }
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    CEBUANO("ceb", "Cebuano (Bisaya)"),
    INDONESIAN("in", "Bahasa Indonesia (JUSSARSian)");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.find { it.code == code } ?: ENGLISH
        }
    }
}

data class SavedPlaylistSectionState(
    val selectedPlaylistId: Long? = null,
    val viewingFavorites: Boolean = false
)

data class SavedMusicModes(
    val isShuffle: Boolean = false,
    val repeatState: RepeatState = RepeatState.ALL,
    val isReverseQueue: Boolean = false
)

data class SavedMusicSession(
    val songId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val title: String,
    val artist: String,
    val album: String,
    val contentUri: String,
    val albumArtUri: String?,
    val remixCategoryName: String,
    val fileName: String,
    val queueSongIds: List<Long>,
    val shufflePlayedIds: List<Long> = emptyList(),
    val shuffleRemainingIds: List<Long> = emptyList(),
    val shuffleHistoryIds: List<Long> = emptyList(),
    val queueSourceType: String = "CUSTOM",
    val queueSourcePlaylistId: Long = -1L
)

data class EqSavedState(
    val enabled: Boolean,
    val bass: Int,
    val virtualizer: Int,
    val preset: String,
    val clarity: Int = 150,
    val customBandsCsv: String = ""
)

data class DjModeSettings(
    val enabled: Boolean = false,
    val transitionSeconds: Int = DEFAULT_TRANSITION_SECONDS
) {
    companion object {
        const val DEFAULT_TRANSITION_SECONDS = 3
        val VALID_TRANSITION_OPTIONS = listOf(3, 5, 10, 20)

        fun normalizeSeconds(seconds: Int): Int {
            return if (seconds in VALID_TRANSITION_OPTIONS) seconds else DEFAULT_TRANSITION_SECONDS
        }
    }
}

class SettingsRepository(private val context: Context) {
    private object PreferencesKeys {
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val REVERSE_QUEUE = booleanPreferencesKey("reverse_queue")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = intPreferencesKey("repeat_mode") // 0 = off, 1 = all, 2 = one
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_BASS = intPreferencesKey("eq_bass")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        val EQ_CLARITY = intPreferencesKey("eq_clarity")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BANDS_CSV = stringPreferencesKey("eq_bands_csv")
        val VIDEO_PLAYBACK_MODE = stringPreferencesKey("video_playback_mode")
        val VIDEO_QUEUE_MODE = stringPreferencesKey("video_queue_mode")
        val LAST_MAIN_TAB = stringPreferencesKey("last_main_tab")
        val LAST_SORT_OPTION = stringPreferencesKey("last_sort_option")
        val LAST_SELECTED_PLAYLIST_ID = longPreferencesKey("last_selected_playlist_id")
        val LAST_VIEWING_FAVORITES = booleanPreferencesKey("last_viewing_favorites")
        val DJ_MODE_ENABLED = booleanPreferencesKey("dj_mode_enabled")
        val DJ_TRANSITION_SECONDS = intPreferencesKey("dj_transition_seconds")
        val LAST_SELECTED_RADIO_SCHEDULE_ID = longPreferencesKey("last_selected_radio_schedule_id")
    }

    private val appContext = context.applicationContext

    private val syncCachePrefs by lazy {
        appContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns persisted [ThemeColor] immediately on first Compose frame without flash.
     */
    fun getInitialThemeColor(): ThemeColor {
        val cached = syncCachePrefs.getString(KEY_SYNC_THEME, null)
        return ThemeColor.fromKey(cached)
    }

    /**
     * Returns persisted [AppLanguage] immediately on first Compose frame without flash.
     */
    fun getInitialLanguage(): AppLanguage {
        val cached = syncCachePrefs.getString(KEY_SYNC_LANGUAGE, null)
        return AppLanguage.fromCode(cached)
    }

    fun getInitialVideoPlaybackMode(): VideoPlaybackMode {
        val cached = syncCachePrefs.getString(KEY_SYNC_VIDEO_MODE, null)
        return VideoPlaybackMode.fromKey(cached)
    }

    fun getInitialVideoQueueMode(): VideoQueueMode {
        val cached = syncCachePrefs.getString(KEY_SYNC_VIDEO_QUEUE_MODE, null)
        return VideoQueueMode.fromKey(cached)
    }

    fun getInitialMainTabName(): String {
        return syncCachePrefs.getString(KEY_SYNC_LAST_TAB, "MUSIC") ?: "MUSIC"
    }

    fun getInitialSortOption(): SortOption {
        val raw = syncCachePrefs.getString(KEY_SYNC_LAST_SORT, null)
        return SortOption.entries.find { it.name == raw } ?: SortOption.NAME_ASC
    }

    fun getInitialPlaylistSectionState(): SavedPlaylistSectionState {
        val rawId = syncCachePrefs.getLong(KEY_SYNC_LAST_PLAYLIST_ID, -1L)
        val viewingFavs = syncCachePrefs.getBoolean(KEY_SYNC_VIEWING_FAVORITES, false)
        return SavedPlaylistSectionState(
            selectedPlaylistId = if (rawId > 0L) rawId else null,
            viewingFavorites = viewingFavs
        )
    }

    fun getInitialRadioScheduleId(): Long? {
        val rawId = syncCachePrefs.getLong(KEY_SYNC_LAST_RADIO_SCHEDULE_ID, -1L)
        return if (rawId > 0L) rawId else null
    }

    fun getInitialMusicModes(): SavedMusicModes = getSavedMusicModes(appContext)

    fun getInitialEqSettings(): EqSavedState = getSavedEqSettings(appContext)

    fun getInitialDjModeSettings(): DjModeSettings = getSavedDjModeSettings(appContext)

    fun getInitialMusicSession(): SavedMusicSession? = getSavedMusicSessionSnapshot(appContext)

    val themeColorFlow: Flow<ThemeColor> = appContext.dataStore.data
        .catch { e -> if (e !is IOException) throw e }
        .map { prefs ->
            val key = prefs[PreferencesKeys.THEME_COLOR] ?: getInitialThemeColor().key
            ThemeColor.fromKey(key)
        }
        .onEach { resolved ->
            if (StorageGuard.hasEnoughSpaceForWrite(appContext)) {
                syncCachePrefs.edit().putString(KEY_SYNC_THEME, resolved.key).apply()
            }
        }

    val languageFlow: Flow<AppLanguage> = appContext.dataStore.data
        .catch { e -> if (e !is IOException) throw e }
        .map { prefs ->
            val code = prefs[PreferencesKeys.APP_LANGUAGE] ?: getInitialLanguage().code
            AppLanguage.fromCode(code)
        }
        .onEach { resolved ->
            if (StorageGuard.hasEnoughSpaceForWrite(appContext)) {
                syncCachePrefs.edit().putString(KEY_SYNC_LANGUAGE, resolved.code).apply()
            }
        }

    val reverseQueueFlow: Flow<Boolean> = appContext.dataStore.data
        .catch { e -> if (e !is IOException) throw e }
        .map { prefs ->
            prefs[PreferencesKeys.REVERSE_QUEUE] ?: getInitialMusicModes().isReverseQueue
        }

    val videoPlaybackModeFlow: Flow<VideoPlaybackMode> = appContext.dataStore.data
        .catch { e -> if (e !is IOException) throw e }
        .map { prefs ->
            val key = prefs[PreferencesKeys.VIDEO_PLAYBACK_MODE] ?: getInitialVideoPlaybackMode().key
            VideoPlaybackMode.fromKey(key)
        }
        .onEach { resolved ->
            if (StorageGuard.hasEnoughSpaceForWrite(appContext)) {
                syncCachePrefs.edit().putString(KEY_SYNC_VIDEO_MODE, resolved.key).apply()
            }
        }

    val videoQueueModeFlow: Flow<VideoQueueMode> = appContext.dataStore.data
        .catch { e -> if (e !is IOException) throw e }
        .map { prefs ->
            val key = prefs[PreferencesKeys.VIDEO_QUEUE_MODE] ?: getInitialVideoQueueMode().key
            VideoQueueMode.fromKey(key)
        }
        .onEach { resolved ->
            if (StorageGuard.hasEnoughSpaceForWrite(appContext)) {
                syncCachePrefs.edit().putString(KEY_SYNC_VIDEO_QUEUE_MODE, resolved.key).apply()
            }
        }

    val djModeSettingsFlow: Flow<DjModeSettings> = appContext.dataStore.data
        .catch { e -> if (e !is IOException) throw e }
        .map { prefs ->
            val fallback = getInitialDjModeSettings()
            val enabled = prefs[PreferencesKeys.DJ_MODE_ENABLED] ?: fallback.enabled
            val rawSeconds = prefs[PreferencesKeys.DJ_TRANSITION_SECONDS] ?: fallback.transitionSeconds
            DjModeSettings(
                enabled = enabled,
                transitionSeconds = DjModeSettings.normalizeSeconds(rawSeconds)
            )
        }
        .onEach { resolved ->
            if (StorageGuard.hasEnoughSpaceForWrite(appContext)) {
                syncCachePrefs.edit()
                    .putBoolean(KEY_SYNC_DJ_MODE_ENABLED, resolved.enabled)
                    .putInt(KEY_SYNC_DJ_TRANSITION_SECONDS, resolved.transitionSeconds)
                    .apply()
            }
        }

    private suspend fun safeEditDataStore(
        syncBlock: () -> Unit,
        dataStoreBlock: suspend (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ): Boolean = withContext(Dispatchers.IO + NonCancellable) {
        if (!StorageGuard.hasEnoughSpaceForWrite(appContext)) {
            Log.w(TAG, "Skipping preference write due to critically low storage.")
            return@withContext false
        }
        try {
            StorageGuard.throwIfSimulatedFailure()
            syncBlock()
            appContext.dataStore.edit { prefs ->
                dataStoreBlock(prefs)
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed saving preference safely: ${t.message}", t)
            false
        }
    }

    suspend fun setThemeColor(theme: ThemeColor) {
        safeEditDataStore(
            syncBlock = { syncCachePrefs.edit().putString(KEY_SYNC_THEME, theme.key).commit() },
            dataStoreBlock = { prefs -> prefs[PreferencesKeys.THEME_COLOR] = theme.key }
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        safeEditDataStore(
            syncBlock = { syncCachePrefs.edit().putString(KEY_SYNC_LANGUAGE, language.code).commit() },
            dataStoreBlock = { prefs -> prefs[PreferencesKeys.APP_LANGUAGE] = language.code }
        )
    }

    suspend fun setReverseQueue(enabled: Boolean) {
        safeEditDataStore(
            syncBlock = { syncCachePrefs.edit().putBoolean(KEY_SYNC_REVERSE_QUEUE, enabled).commit() },
            dataStoreBlock = { prefs -> prefs[PreferencesKeys.REVERSE_QUEUE] = enabled }
        )
    }

    suspend fun setVideoPlaybackMode(mode: VideoPlaybackMode) {
        safeEditDataStore(
            syncBlock = { syncCachePrefs.edit().putString(KEY_SYNC_VIDEO_MODE, mode.key).commit() },
            dataStoreBlock = { prefs -> prefs[PreferencesKeys.VIDEO_PLAYBACK_MODE] = mode.key }
        )
    }

    suspend fun setVideoQueueMode(mode: VideoQueueMode) {
        safeEditDataStore(
            syncBlock = { syncCachePrefs.edit().putString(KEY_SYNC_VIDEO_QUEUE_MODE, mode.key).commit() },
            dataStoreBlock = { prefs -> prefs[PreferencesKeys.VIDEO_QUEUE_MODE] = mode.key }
        )
    }

    suspend fun setDjModeEnabled(enabled: Boolean): Boolean {
        val currentSeconds = getInitialDjModeSettings().transitionSeconds
        val normalizedSeconds = DjModeSettings.normalizeSeconds(currentSeconds)
        return safeEditDataStore(
            syncBlock = {
                syncCachePrefs.edit()
                    .putBoolean(KEY_SYNC_DJ_MODE_ENABLED, enabled)
                    .putInt(KEY_SYNC_DJ_TRANSITION_SECONDS, normalizedSeconds)
                    .commit()
            },
            dataStoreBlock = { prefs ->
                prefs[PreferencesKeys.DJ_MODE_ENABLED] = enabled
                if (prefs[PreferencesKeys.DJ_TRANSITION_SECONDS] == null) {
                    prefs[PreferencesKeys.DJ_TRANSITION_SECONDS] = normalizedSeconds
                }
            }
        )
    }

    suspend fun setDjTransitionSeconds(seconds: Int): Boolean {
        val normalized = DjModeSettings.normalizeSeconds(seconds)
        return safeEditDataStore(
            syncBlock = {
                syncCachePrefs.edit()
                    .putInt(KEY_SYNC_DJ_TRANSITION_SECONDS, normalized)
                    .commit()
            },
            dataStoreBlock = { prefs ->
                prefs[PreferencesKeys.DJ_TRANSITION_SECONDS] = normalized
            }
        )
    }

    suspend fun saveDjModeSettings(enabled: Boolean, transitionSeconds: Int): Boolean {
        val normalized = DjModeSettings.normalizeSeconds(transitionSeconds)
        return safeEditDataStore(
            syncBlock = {
                syncCachePrefs.edit()
                    .putBoolean(KEY_SYNC_DJ_MODE_ENABLED, enabled)
                    .putInt(KEY_SYNC_DJ_TRANSITION_SECONDS, normalized)
                    .commit()
            },
            dataStoreBlock = { prefs ->
                prefs[PreferencesKeys.DJ_MODE_ENABLED] = enabled
                prefs[PreferencesKeys.DJ_TRANSITION_SECONDS] = normalized
            }
        )
    }

    suspend fun setLastMainTabName(tabName: String): Boolean {
        return safeEditDataStore(
            syncBlock = { syncCachePrefs.edit().putString(KEY_SYNC_LAST_TAB, tabName).commit() },
            dataStoreBlock = { prefs -> prefs[PreferencesKeys.LAST_MAIN_TAB] = tabName }
        )
    }

    suspend fun setLastSortOption(sort: SortOption): Boolean {
        return safeEditDataStore(
            syncBlock = { syncCachePrefs.edit().putString(KEY_SYNC_LAST_SORT, sort.name).commit() },
            dataStoreBlock = { prefs -> prefs[PreferencesKeys.LAST_SORT_OPTION] = sort.name }
        )
    }

    suspend fun setPlaylistSectionState(selectedPlaylistId: Long?, viewingFavorites: Boolean): Boolean {
        val encodedId = selectedPlaylistId ?: -1L
        return safeEditDataStore(
            syncBlock = {
                syncCachePrefs.edit()
                    .putLong(KEY_SYNC_LAST_PLAYLIST_ID, encodedId)
                    .putBoolean(KEY_SYNC_VIEWING_FAVORITES, viewingFavorites)
                    .commit()
            },
            dataStoreBlock = { prefs ->
                prefs[PreferencesKeys.LAST_SELECTED_PLAYLIST_ID] = encodedId
                prefs[PreferencesKeys.LAST_VIEWING_FAVORITES] = viewingFavorites
            }
        )
    }

    suspend fun setLastSelectedRadioScheduleId(scheduleId: Long?) {
        val encoded = scheduleId ?: -1L
        safeEditDataStore(
            syncBlock = {
                syncCachePrefs.edit()
                    .putLong(KEY_SYNC_LAST_RADIO_SCHEDULE_ID, encoded)
                    .commit()
            },
            dataStoreBlock = { prefs ->
                prefs[PreferencesKeys.LAST_SELECTED_RADIO_SCHEDULE_ID] = encoded
            }
        )
    }

    suspend fun saveMusicModes(
        isShuffle: Boolean,
        repeatState: RepeatState,
        isReverseQueue: Boolean
    ): Boolean {
        val repeatCode = repeatStateToInt(repeatState)
        return safeEditDataStore(
            syncBlock = {
                syncCachePrefs.edit()
                    .putBoolean(KEY_SYNC_SHUFFLE, isShuffle)
                    .putInt(KEY_SYNC_REPEAT_MODE, repeatCode)
                    .putBoolean(KEY_SYNC_REVERSE_QUEUE, isReverseQueue)
                    .commit()
            },
            dataStoreBlock = { prefs ->
                prefs[PreferencesKeys.SHUFFLE_ENABLED] = isShuffle
                prefs[PreferencesKeys.REPEAT_MODE] = repeatCode
                prefs[PreferencesKeys.REVERSE_QUEUE] = isReverseQueue
            }
        )
    }

    suspend fun saveEqSettings(
        enabled: Boolean,
        bass: Int,
        virtualizer: Int,
        preset: String,
        clarity: Int = 150,
        customBandsCsv: String = ""
    ) {
        safeEditDataStore(
            syncBlock = {
                syncCachePrefs.edit()
                    .putBoolean(KEY_SYNC_EQ_ENABLED, enabled)
                    .putInt(KEY_SYNC_EQ_BASS, bass)
                    .putInt(KEY_SYNC_EQ_VIRTUALIZER, virtualizer)
                    .putInt(KEY_SYNC_EQ_CLARITY, clarity)
                    .putString(KEY_SYNC_EQ_PRESET, preset)
                    .putString(KEY_SYNC_EQ_BANDS_CSV, customBandsCsv)
                    .commit()
            },
            dataStoreBlock = { prefs ->
                prefs[PreferencesKeys.EQ_ENABLED] = enabled
                prefs[PreferencesKeys.EQ_BASS] = bass
                prefs[PreferencesKeys.EQ_VIRTUALIZER] = virtualizer
                prefs[PreferencesKeys.EQ_CLARITY] = clarity
                prefs[PreferencesKeys.EQ_PRESET] = preset
                prefs[PreferencesKeys.EQ_BANDS_CSV] = customBandsCsv
            }
        )
    }

    val eqSettingsFlow = appContext.dataStore.data
        .catch { e -> if (e !is IOException) throw e }
        .map { prefs ->
            EqSavedState(
                enabled = prefs[PreferencesKeys.EQ_ENABLED] ?: getInitialEqSettings().enabled,
                bass = prefs[PreferencesKeys.EQ_BASS] ?: getInitialEqSettings().bass,
                virtualizer = prefs[PreferencesKeys.EQ_VIRTUALIZER] ?: getInitialEqSettings().virtualizer,
                preset = prefs[PreferencesKeys.EQ_PRESET] ?: getInitialEqSettings().preset,
                clarity = prefs[PreferencesKeys.EQ_CLARITY] ?: getInitialEqSettings().clarity,
                customBandsCsv = prefs[PreferencesKeys.EQ_BANDS_CSV] ?: getInitialEqSettings().customBandsCsv
            )
        }

    companion object {
        private const val TAG = "JNMF_SettingsRepo"
        internal const val SYNC_CACHE_NAME = "jnmf_sync_boot_cache"
        private const val KEY_SYNC_THEME = "theme_color"
        private const val KEY_SYNC_LANGUAGE = "app_language"
        private const val KEY_SYNC_VIDEO_MODE = "video_playback_mode"
        private const val KEY_SYNC_VIDEO_QUEUE_MODE = "video_queue_mode"

        private const val KEY_SYNC_LAST_TAB = "last_main_tab"
        private const val KEY_SYNC_LAST_SORT = "last_sort_option"
        private const val KEY_SYNC_LAST_PLAYLIST_ID = "last_selected_playlist_id"
        private const val KEY_SYNC_VIEWING_FAVORITES = "last_viewing_favorites"
        private const val KEY_SYNC_LAST_RADIO_SCHEDULE_ID = "last_selected_radio_schedule_id"

        private const val KEY_SYNC_SHUFFLE = "music_shuffle_enabled"
        private const val KEY_SYNC_REPEAT_MODE = "music_repeat_mode"
        private const val KEY_SYNC_REVERSE_QUEUE = "music_reverse_queue"

        private const val KEY_SYNC_EQ_ENABLED = "eq_enabled"
        private const val KEY_SYNC_EQ_BASS = "eq_bass"
        private const val KEY_SYNC_EQ_VIRTUALIZER = "eq_virtualizer"
        private const val KEY_SYNC_EQ_CLARITY = "eq_clarity"
        private const val KEY_SYNC_EQ_PRESET = "eq_preset"
        private const val KEY_SYNC_EQ_BANDS_CSV = "eq_bands_csv"

        private const val KEY_SYNC_DJ_MODE_ENABLED = "dj_mode_enabled"
        private const val KEY_SYNC_DJ_TRANSITION_SECONDS = "dj_transition_seconds"

        private const val KEY_SESSION_SONG_ID = "session_song_id"
        private const val KEY_SESSION_POSITION_MS = "session_position_ms"
        private const val KEY_SESSION_DURATION_MS = "session_duration_ms"
        private const val KEY_SESSION_TITLE = "session_title"
        private const val KEY_SESSION_ARTIST = "session_artist"
        private const val KEY_SESSION_ALBUM = "session_album"
        private const val KEY_SESSION_URI = "session_uri"
        private const val KEY_SESSION_ART_URI = "session_art_uri"
        private const val KEY_SESSION_CATEGORY = "session_category"
        private const val KEY_SESSION_FILENAME = "session_filename"
        private const val KEY_SESSION_QUEUE_IDS = "session_queue_ids"
        private const val KEY_SESSION_SHUFFLE_PLAYED_IDS = "session_shuffle_played_ids"
        private const val KEY_SESSION_SHUFFLE_REMAINING_IDS = "session_shuffle_remaining_ids"
        private const val KEY_SESSION_SHUFFLE_HISTORY_IDS = "session_shuffle_history_ids"
        private const val KEY_SESSION_QUEUE_SOURCE_TYPE = "session_queue_source_type"
        private const val KEY_SESSION_QUEUE_SOURCE_PLAYLIST_ID = "session_queue_source_playlist_id"

        private const val MAX_PERSISTED_QUEUE_IDS = 2_000
        private const val MAX_PERSISTED_SHUFFLE_HISTORY_IDS = 500

        private fun repeatStateToInt(state: RepeatState): Int = when (state) {
            RepeatState.OFF -> 0
            RepeatState.ALL -> 1
            RepeatState.ONE -> 2
        }

        private fun intToRepeatState(code: Int): RepeatState = when (code) {
            0 -> RepeatState.OFF
            2 -> RepeatState.ONE
            else -> RepeatState.ALL
        }

        fun getSavedLanguage(context: Context): AppLanguage {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            return AppLanguage.fromCode(prefs.getString(KEY_SYNC_LANGUAGE, null))
        }

        fun getSavedThemeColor(context: Context): ThemeColor {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            return ThemeColor.fromKey(prefs.getString(KEY_SYNC_THEME, null))
        }

        fun getSavedVideoPlaybackMode(context: Context): VideoPlaybackMode {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            return VideoPlaybackMode.fromKey(prefs.getString(KEY_SYNC_VIDEO_MODE, null))
        }

        fun getSavedVideoQueueMode(context: Context): VideoQueueMode {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            return VideoQueueMode.fromKey(prefs.getString(KEY_SYNC_VIDEO_QUEUE_MODE, null))
        }

        fun getSavedMusicModes(context: Context): SavedMusicModes {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            return SavedMusicModes(
                isShuffle = prefs.getBoolean(KEY_SYNC_SHUFFLE, false),
                repeatState = intToRepeatState(prefs.getInt(KEY_SYNC_REPEAT_MODE, 1)),
                isReverseQueue = prefs.getBoolean(KEY_SYNC_REVERSE_QUEUE, false)
            )
        }

        fun saveMusicModesSync(
            context: Context,
            isShuffle: Boolean,
            repeatState: RepeatState,
            isReverseQueue: Boolean,
            commitSynchronously: Boolean = true
        ): Boolean {
            val appCtx = context.applicationContext
            if (commitSynchronously && !StorageGuard.hasEnoughSpaceForWrite(appCtx)) return false
            return try {
                StorageGuard.throwIfSimulatedFailure()
                val prefs = appCtx.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                    .putBoolean(KEY_SYNC_SHUFFLE, isShuffle)
                    .putInt(KEY_SYNC_REPEAT_MODE, repeatStateToInt(repeatState))
                    .putBoolean(KEY_SYNC_REVERSE_QUEUE, isReverseQueue)
                if (commitSynchronously) {
                    editor.commit()
                } else {
                    editor.apply()
                    true
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed saving music modes sync: ${t.message}")
                false
            }
        }

        fun getSavedEqSettings(context: Context): EqSavedState {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            return EqSavedState(
                enabled = prefs.getBoolean(KEY_SYNC_EQ_ENABLED, true),
                bass = prefs.getInt(KEY_SYNC_EQ_BASS, 300),
                virtualizer = prefs.getInt(KEY_SYNC_EQ_VIRTUALIZER, 200),
                preset = prefs.getString(KEY_SYNC_EQ_PRESET, "Indo Fullbeat") ?: "Indo Fullbeat",
                clarity = prefs.getInt(KEY_SYNC_EQ_CLARITY, 150),
                customBandsCsv = prefs.getString(KEY_SYNC_EQ_BANDS_CSV, "") ?: ""
            )
        }

        fun saveEqSettingsSync(context: Context, state: EqSavedState): Boolean {
            val appCtx = context.applicationContext
            if (!StorageGuard.hasEnoughSpaceForWrite(appCtx)) return false
            return try {
                StorageGuard.throwIfSimulatedFailure()
                val prefs = appCtx.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_SYNC_EQ_ENABLED, state.enabled)
                    .putInt(KEY_SYNC_EQ_BASS, state.bass)
                    .putInt(KEY_SYNC_EQ_VIRTUALIZER, state.virtualizer)
                    .putInt(KEY_SYNC_EQ_CLARITY, state.clarity)
                    .putString(KEY_SYNC_EQ_PRESET, state.preset)
                    .putString(KEY_SYNC_EQ_BANDS_CSV, state.customBandsCsv)
                    .commit()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed saving EQ settings sync: ${t.message}")
                false
            }
        }

        fun getSavedDjModeSettings(context: Context): DjModeSettings {
            val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean(KEY_SYNC_DJ_MODE_ENABLED, false)
            val seconds = prefs.getInt(
                KEY_SYNC_DJ_TRANSITION_SECONDS,
                DjModeSettings.DEFAULT_TRANSITION_SECONDS
            )
            return DjModeSettings(
                enabled = enabled,
                transitionSeconds = DjModeSettings.normalizeSeconds(seconds)
            )
        }

        fun saveDjModeSettingsSync(
            context: Context,
            enabled: Boolean,
            transitionSeconds: Int
        ): Boolean {
            val appCtx = context.applicationContext
            if (!StorageGuard.hasEnoughSpaceForWrite(appCtx)) return false
            val normalized = DjModeSettings.normalizeSeconds(transitionSeconds)
            return try {
                StorageGuard.throwIfSimulatedFailure()
                val prefs = appCtx.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_SYNC_DJ_MODE_ENABLED, enabled)
                    .putInt(KEY_SYNC_DJ_TRANSITION_SECONDS, normalized)
                    .commit()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed saving DJ mode settings sync: ${t.message}")
                false
            }
        }

        private fun populateSessionEditor(
            editor: android.content.SharedPreferences.Editor,
            session: SavedMusicSession?,
            precomputedQueueCsv: String? = null
        ) {
            if (session == null || session.songId <= 0L) {
                editor.remove(KEY_SESSION_SONG_ID)
                    .remove(KEY_SESSION_POSITION_MS)
                    .remove(KEY_SESSION_QUEUE_IDS)
                    .remove(KEY_SESSION_SHUFFLE_PLAYED_IDS)
                    .remove(KEY_SESSION_SHUFFLE_REMAINING_IDS)
                    .remove(KEY_SESSION_SHUFFLE_HISTORY_IDS)
                    .remove(KEY_SESSION_QUEUE_SOURCE_TYPE)
                    .remove(KEY_SESSION_QUEUE_SOURCE_PLAYLIST_ID)
            } else {
                val queueCsv = precomputedQueueCsv ?: run {
                    val boundedIds = if (session.queueSongIds.size <= MAX_PERSISTED_QUEUE_IDS) {
                        session.queueSongIds
                    } else {
                        val activeIdx = session.queueSongIds.indexOf(session.songId).coerceAtLeast(0)
                        val half = MAX_PERSISTED_QUEUE_IDS / 2
                        val start = (activeIdx - half).coerceAtLeast(0)
                        val end = (start + MAX_PERSISTED_QUEUE_IDS).coerceAtMost(session.queueSongIds.size)
                        session.queueSongIds.subList(start, end)
                    }
                    boundedIds.joinToString(",")
                }
                val playedCsv = session.shufflePlayedIds
                    .takeLast(MAX_PERSISTED_QUEUE_IDS)
                    .joinToString(",")
                val remainingCsv = session.shuffleRemainingIds
                    .take(MAX_PERSISTED_QUEUE_IDS)
                    .joinToString(",")
                val historyCsv = session.shuffleHistoryIds
                    .takeLast(MAX_PERSISTED_SHUFFLE_HISTORY_IDS)
                    .joinToString(",")

                editor.putLong(KEY_SESSION_SONG_ID, session.songId)
                    .putLong(KEY_SESSION_POSITION_MS, session.positionMs.coerceAtLeast(0L))
                    .putLong(KEY_SESSION_DURATION_MS, session.durationMs.coerceAtLeast(0L))
                    .putString(KEY_SESSION_TITLE, session.title)
                    .putString(KEY_SESSION_ARTIST, session.artist)
                    .putString(KEY_SESSION_ALBUM, session.album)
                    .putString(KEY_SESSION_URI, session.contentUri)
                    .putString(KEY_SESSION_ART_URI, session.albumArtUri)
                    .putString(KEY_SESSION_CATEGORY, session.remixCategoryName)
                    .putString(KEY_SESSION_FILENAME, session.fileName)
                    .putString(KEY_SESSION_QUEUE_IDS, queueCsv)
                    .putString(KEY_SESSION_SHUFFLE_PLAYED_IDS, playedCsv)
                    .putString(KEY_SESSION_SHUFFLE_REMAINING_IDS, remainingCsv)
                    .putString(KEY_SESSION_SHUFFLE_HISTORY_IDS, historyCsv)
                    .putString(KEY_SESSION_QUEUE_SOURCE_TYPE, session.queueSourceType)
                    .putLong(KEY_SESSION_QUEUE_SOURCE_PLAYLIST_ID, session.queueSourcePlaylistId)
            }
        }

        fun saveMusicSessionSnapshotSync(
            context: Context,
            session: SavedMusicSession?,
            commitSynchronously: Boolean = true,
            precomputedQueueCsv: String? = null
        ): Boolean {
            val appCtx = context.applicationContext
            if (commitSynchronously && !StorageGuard.hasEnoughSpaceForWrite(appCtx)) return false
            return try {
                StorageGuard.throwIfSimulatedFailure()
                val prefs = appCtx.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                populateSessionEditor(editor, session, precomputedQueueCsv)
                if (commitSynchronously) {
                    editor.commit()
                } else {
                    editor.apply()
                    true
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed saving music session snapshot: ${t.message}")
                false
            }
        }

        fun saveMusicModesAndSessionSnapshot(
            context: Context,
            isShuffle: Boolean,
            repeatState: RepeatState,
            isReverseQueue: Boolean,
            session: SavedMusicSession?,
            commitSynchronously: Boolean = true,
            precomputedQueueCsv: String? = null
        ): Boolean {
            val appCtx = context.applicationContext
            if (commitSynchronously && !StorageGuard.hasEnoughSpaceForWrite(appCtx)) return false
            return try {
                StorageGuard.throwIfSimulatedFailure()
                val prefs = appCtx.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                    .putBoolean(KEY_SYNC_SHUFFLE, isShuffle)
                    .putInt(KEY_SYNC_REPEAT_MODE, repeatStateToInt(repeatState))
                    .putBoolean(KEY_SYNC_REVERSE_QUEUE, isReverseQueue)
                populateSessionEditor(editor, session, precomputedQueueCsv)
                if (commitSynchronously) {
                    editor.commit()
                } else {
                    editor.apply()
                    true
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed saving batched music modes & session snapshot: ${t.message}")
                false
            }
        }

        private fun parseLongCsv(raw: String, deduplicate: Boolean): List<Long> {
            if (raw.isBlank()) return emptyList()
            val parsed = raw.split(',').mapNotNull { it.trim().toLongOrNull() }
            return if (deduplicate) parsed.distinct() else parsed
        }

        fun getSavedMusicSessionSnapshot(context: Context): SavedMusicSession? {
            return try {
                val prefs = context.applicationContext.getSharedPreferences(SYNC_CACHE_NAME, Context.MODE_PRIVATE)
                val songId = prefs.getLong(KEY_SESSION_SONG_ID, -1L)
                if (songId <= 0L) return null
                val uri = prefs.getString(KEY_SESSION_URI, null) ?: return null
                val rawQueue = prefs.getString(KEY_SESSION_QUEUE_IDS, "").orEmpty()
                val queueIds = if (rawQueue.isBlank()) {
                    listOf(songId)
                } else {
                    parseLongCsv(rawQueue, deduplicate = true)
                }
                val shufflePlayedIds = parseLongCsv(
                    prefs.getString(KEY_SESSION_SHUFFLE_PLAYED_IDS, "").orEmpty(),
                    deduplicate = true
                )
                val shuffleRemainingIds = parseLongCsv(
                    prefs.getString(KEY_SESSION_SHUFFLE_REMAINING_IDS, "").orEmpty(),
                    deduplicate = true
                )
                val shuffleHistoryIds = parseLongCsv(
                    prefs.getString(KEY_SESSION_SHUFFLE_HISTORY_IDS, "").orEmpty(),
                    deduplicate = false
                )
                SavedMusicSession(
                    songId = songId,
                    positionMs = prefs.getLong(KEY_SESSION_POSITION_MS, 0L).coerceAtLeast(0L),
                    durationMs = prefs.getLong(KEY_SESSION_DURATION_MS, 0L).coerceAtLeast(0L),
                    title = prefs.getString(KEY_SESSION_TITLE, "") ?: "",
                    artist = prefs.getString(KEY_SESSION_ARTIST, "") ?: "",
                    album = prefs.getString(KEY_SESSION_ALBUM, "") ?: "",
                    contentUri = uri,
                    albumArtUri = prefs.getString(KEY_SESSION_ART_URI, null),
                    remixCategoryName = prefs.getString(KEY_SESSION_CATEGORY, "") ?: "",
                    fileName = prefs.getString(KEY_SESSION_FILENAME, "") ?: "",
                    queueSongIds = queueIds,
                    shufflePlayedIds = shufflePlayedIds,
                    shuffleRemainingIds = shuffleRemainingIds,
                    shuffleHistoryIds = shuffleHistoryIds,
                    queueSourceType = prefs.getString(KEY_SESSION_QUEUE_SOURCE_TYPE, "CUSTOM") ?: "CUSTOM",
                    queueSourcePlaylistId = prefs.getLong(KEY_SESSION_QUEUE_SOURCE_PLAYLIST_ID, -1L)
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Failed reading saved music session snapshot: ${t.message}")
                null
            }
        }
    }
}
