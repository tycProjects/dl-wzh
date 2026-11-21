package com.android.island.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.android.island.core.CutoutProfile
import com.android.island.core.Elasticity
import com.android.island.core.ExpandStyle
import com.android.island.core.GlassConfig
import com.android.island.core.TapAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "island_settings")

class SettingsRepository(
    private val context: Context,
    // Injectable only so a unit test can park the init coroutine and let update() win the race
    // deterministically. Production always uses the default.
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    companion object {
        private const val PERSIST_DEBOUNCE_MS = 400L
        private const val FAST_FLAGS_FILE = "island_fast_flags"
        private const val KEY_LOW_END_FAST = "low_end_mode"

        /**
         * Synchronous mirror of [IslandSettings.lowEndMode], read by [com.android.island.core.DeviceProfile.init]
         * before the first Composable can run. DataStore's `.data.first()` is a suspend/IO read and is too slow
         * to resolve before the overlay's first frame; this tiny single-boolean SharedPreferences file is the
         * same pattern already used for LikedTracks and never introduces blocking disk I/O beyond what the
         * platform already does on first Context-scoped SharedPreferences access.
         *
         * Returns null when no entry has ever been written (fresh install, or an upgrade from a
         * build predating this flag) — NOT false. Callers must not treat a missing entry as a
         * confirmed Normal preference; [com.android.island.core.DeviceProfile.init] resolves that
         * ambiguity to UNRESOLVED (the safe, Low-End-equivalent state), not to Normal.
         */
        fun readLowEndFastFlag(context: Context): Boolean? {
            val prefs = context.applicationContext.getSharedPreferences(FAST_FLAGS_FILE, Context.MODE_PRIVATE)
            if (!prefs.contains(KEY_LOW_END_FAST)) return null
            return prefs.getBoolean(KEY_LOW_END_FAST, false)
        }

        /** [sync] = commit() instead of apply(): only for callers about to kill the process. */
        private fun writeLowEndFastFlag(context: Context, enabled: Boolean, sync: Boolean = false) {
            val edit = context.applicationContext
                .getSharedPreferences(FAST_FLAGS_FILE, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_LOW_END_FAST, enabled)
            if (sync) edit.commit() else edit.apply()
        }
    }

    /**
     * In-memory source of truth. UI and the accessibility service read from here, so slider
     * changes apply instantly. Disk writes are coalesced: only the latest snapshot is persisted
     * once no further change arrives for [PERSIST_DEBOUNCE_MS].
     */
    private val live = MutableStateFlow<IslandSettings?>(null)
    private val mutex = Mutex()
    private var persistJob: Job? = null

    init {
        scope.launch {
            mutex.withLock {
                // update() may have won the mutex first and already performed (and resolved) the first
                // authoritative read. live.value is only ever null before that read, and only
                // changes under this mutex, so exactly one of init / update() / resetAll() resolves.
                if (live.value == null) {
                    val loaded = readAuthoritative()
                    live.value = loaded
                    resolveLowEnd(loaded.lowEndMode)
                }
            }
        }
    }

    /** First authoritative read: defaults migration, then DataStore. Caller must hold [mutex]. */
    private suspend fun readAuthoritative(): IslandSettings {
        migrateDefaults()
        return read(context.dataStore.data.first())
    }

    /**
     * Resolves DeviceProfile out of UNRESOLVED (or applies a real transition) and keeps the
     * synchronous fast flag in step. Caller must hold [mutex].
     *
     * Resolution happens the moment the authoritative value is known, rather than waiting for
     * IslandRoot to compose (which only happens once an overlay is shown — a notification could
     * otherwise sit on the UNRESOLVED/safe renderer indefinitely). setLowEnd is a no-op when
     * DeviceProfile.init() already resolved the same value from a valid fast flag. The flag write
     * backfills an upgrade from a build that predates it (or a cleared file), and is made
     * synchronously (apply(), off the debounced DataStore write) so the next DeviceProfile.init()
     * can resolve before the first frame.
     */
    private fun resolveLowEnd(enabled: Boolean) {
        com.android.island.core.DeviceProfile.setLowEnd(enabled)
        writeLowEndFastFlag(context, enabled)
    }

    /**
     * One-time defaults migrations. Runs as part of the first authoritative read (see [readAuthoritative])
     * so that whichever of init / [update] performs that read sees the migrated values.
     */
    private suspend fun migrateDefaults() {
        // One-time clear-material migration: remove the old default frost/white veil
        // so the new artwork-opacity slider starts from a genuinely clear image.
        context.dataStore.edit { prefs ->
            if (prefs[Keys.CLEAR_GLASS_MIGRATION_V1] != true) {
                prefs[Keys.GLASS_FROST] = 0f
                prefs[Keys.GLASS_WHITE] = 0.08f
                prefs[Keys.CLEAR_GLASS_MIGRATION_V1] = true
            }
            // V2: frosted matte body (reference look). Blur and body frost only; other sliders keep their values.
            if (prefs[Keys.GLASS_LOOK_V2] != true) {
                prefs[Keys.GLASS_FROST] = 0.55f
                prefs[Keys.GLASS_WHITE] = 0.14f
                prefs[Keys.GLASS_LOOK_V2] = true
            }
            // V3: real liquid glass refraction on (edge refraction was 0, which turned the lens off).
            if (prefs[Keys.GLASS_LOOK_V3] != true) {
                prefs[Keys.EDGE_LENS_STRENGTH] = 0.7f
                prefs[Keys.GLASS_LOOK_V3] = true
            }
            // V4: defaults taken from the reference screenshots (blur 15%, body 0%, sweep 0.8 s, lyric 15 sp, corner 35 dp).
            if (prefs[Keys.DEFAULTS_V4] != true) {
                prefs[Keys.GLASS_FROST] = 0.15f
                prefs[Keys.GLASS_SURFACE_OPACITY] = 0f
                prefs[Keys.GLASS_SWEEP_DURATION] = 800
                prefs[Keys.LYRIC_FONT_SIZE] = 15
                prefs[Keys.EXPANDED_CORNER_RADIUS] = 35
                prefs[Keys.DEFAULTS_V4] = true
            }
        }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val OFFSET_X = intPreferencesKey("offset_x")
        val OFFSET_Y = intPreferencesKey("offset_y")
        val GLASS_TRANSPARENCY = floatPreferencesKey("glass_transparency")
        val GLASS_REFRACTION = floatPreferencesKey("glass_refraction")
        val GLASS_EDGE = floatPreferencesKey("glass_edge")
        val GLASS_AMBIENT = floatPreferencesKey("glass_ambient")
        val GLASS_WHITE = floatPreferencesKey("glass_white")
        val GLASS_FROST = floatPreferencesKey("glass_frost")
        val CLEAR_GLASS_MIGRATION_V1 = booleanPreferencesKey("clear_glass_migration_v1")
        val GLASS_LOOK_V2 = booleanPreferencesKey("glass_look_v2")
        val GLASS_LOOK_V3 = booleanPreferencesKey("glass_look_v3")
        val DEFAULTS_V4 = booleanPreferencesKey("defaults_v4")
        val GLASS_EXPANDED_SHADOW = floatPreferencesKey("glass_expanded_shadow")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val SHOW_LYRICS = booleanPreferencesKey("show_lyrics")
        val TAP_ACTION = stringPreferencesKey("tap_action")
        val DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")
        val LONG_PRESS_ACTION = stringPreferencesKey("long_press_action")
        val SWIPE_TRACKS = booleanPreferencesKey("swipe_tracks")
        val SWIPE_EXPAND = booleanPreferencesKey("swipe_expand")
        val EXPAND_STYLE = stringPreferencesKey("expand_style")
        val ELASTICITY = stringPreferencesKey("elasticity")
        val SHARP_NOTCH = booleanPreferencesKey("sharp_notch")
        val IPHONE_NOTCH_ENABLED = booleanPreferencesKey("iphone_notch_enabled")
        val IPHONE_NOTCH_WIDTH = intPreferencesKey("iphone_notch_width")
        val IPHONE_NOTCH_HEIGHT = intPreferencesKey("iphone_notch_height")
        val CUTOUT_PROFILE = stringPreferencesKey("cutout_profile")
        val HIDE_NOTCH_WHEN_IDLE = booleanPreferencesKey("hide_notch_when_idle")
        val COMPACT_WINDOW_SHRINK = booleanPreferencesKey("compact_window_shrink")
        val EXPANDED_OFFSET_Y = intPreferencesKey("expanded_offset_y")
        val EXPANDED_WIDTH = intPreferencesKey("expanded_width")
        val EXPANDED_MIN_HEIGHT = intPreferencesKey("expanded_min_height")
        val SHOW_ALBUM_BG = booleanPreferencesKey("show_album_bg")
        val LOW_POWER_MODE = booleanPreferencesKey("low_power_mode")
        val COMPACT_BANNER = booleanPreferencesKey("compact_banner")
        val COMPACT_BANNER_WIDTH = intPreferencesKey("compact_banner_width")
        val COMPACT_BANNER_HEIGHT = intPreferencesKey("compact_banner_height")
        val STANDARD_BANNER_WIDTH = intPreferencesKey("standard_banner_width")
        val STANDARD_BANNER_HEIGHT = intPreferencesKey("standard_banner_height")
        val WHATSAPP_BANNER_DURATION = intPreferencesKey("whatsapp_banner_duration")
        /** Durasi banner dalam milidetik (referensi Dynamic Island: 2500 ms bawaan, 1500–10000 ms). */
        val WHATSAPP_BANNER_MS = intPreferencesKey("whatsapp_banner_ms")
        val LYRIC_GLOW_ENABLED = booleanPreferencesKey("lyric_glow_enabled")
        val BANNER_FONT_SIZE = intPreferencesKey("banner_font_size")
        val BANNER_FONT_WEIGHT = intPreferencesKey("banner_font_weight")
        val LYRIC_FONT_SIZE = intPreferencesKey("lyric_font_size")
        val LYRIC_FONT_WEIGHT = intPreferencesKey("lyric_font_weight")
        val GLASS_SWEEP_ENABLED = booleanPreferencesKey("glass_sweep_enabled")
        val GLASS_SWEEP_OPACITY = floatPreferencesKey("glass_sweep_opacity")
        val GLASS_SWEEP_DURATION = intPreferencesKey("glass_sweep_duration_ms")
        val BACKGROUND_OPACITY = floatPreferencesKey("background_opacity")
        val EDGE_LENS_STRENGTH = floatPreferencesKey("edge_lens_strength")
        val SHEEN_STRENGTH = floatPreferencesKey("sheen_strength")
        val PROFILE_SHEEN_EXPANDED = floatPreferencesKey("profile_sheen_expanded")
        val PROFILE_SHEEN_BANNER = floatPreferencesKey("profile_sheen_banner")
        val LOW_END_MODE = booleanPreferencesKey("low_end_mode")
        val GLASS_SURFACE_OPACITY = floatPreferencesKey("glass_surface_opacity")
        val STACK_GAP = floatPreferencesKey("stack_gap")
        val STACK_INSET = floatPreferencesKey("stack_inset")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val WHATSAPP_INDICATOR_LEFT = booleanPreferencesKey("whatsapp_indicator_left")
        val HIDE_STATUS_ICONS = booleanPreferencesKey("hide_status_icons")
        val DIAGNOSTICS_ENABLED = booleanPreferencesKey("diagnostics_enabled")
        val ANIMATION_OPTION = stringPreferencesKey("animation_option")
        val LIQUID_EXPAND = booleanPreferencesKey("liquid_expand")
        val LYRIC_SHINE_ENABLED = booleanPreferencesKey("lyric_shine_enabled")
        val LYRIC_SHINE_SPEED = floatPreferencesKey("lyric_shine_speed")
        val LYRIC_SHINE_INTENSITY = floatPreferencesKey("lyric_shine_intensity")
        val LYRIC_GLOW_INTENSITY = floatPreferencesKey("lyric_glow_intensity")
        val EXPANDED_CORNER_RADIUS = intPreferencesKey("expanded_corner_radius")
        val ISLAND_THEME = stringPreferencesKey("island_theme")
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_BRAVE_SEARCH_KEY = stringPreferencesKey("ai_brave_search_key")
        val AI_BRAVE_SEARCH_KEY_ENC = stringPreferencesKey("ai_brave_search_key_enc")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_EXTRA_KEYS = stringPreferencesKey("ai_extra_keys")
        val AI_API_KEY_ENC = stringPreferencesKey("ai_api_key_enc")
        val AI_EXTRA_KEYS_ENC = stringPreferencesKey("ai_extra_keys_enc")
        val AI_FALLBACK_APIS_ENC = stringPreferencesKey("ai_fallback_apis_enc")
        val AI_FALLBACK_APIS = stringPreferencesKey("ai_fallback_apis")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")
        val AI_MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        val AI_PANEL_MIN_HEIGHT = intPreferencesKey("ai_panel_min_height")
        val AI_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
        val AI_SCREEN_ANALYSIS = booleanPreferencesKey("ai_screen_analysis")
        val AI_STREAMING = booleanPreferencesKey("ai_streaming")
        val AI_MEMORY_ENABLED = booleanPreferencesKey("ai_memory_enabled")
    }

    private fun read(prefs: Preferences): IslandSettings = IslandSettings(
        enabled = prefs[Keys.ENABLED] ?: true,
        offsetX = prefs[Keys.OFFSET_X] ?: 0,
        offsetY = prefs[Keys.OFFSET_Y] ?: 0,
        glassConfig = GlassConfig(
            transparency = prefs[Keys.GLASS_TRANSPARENCY] ?: 0.62f,
            refraction = prefs[Keys.GLASS_REFRACTION] ?: 0.40f,
            edgeHighlight = prefs[Keys.GLASS_EDGE] ?: 1f,
            ambientReflection = prefs[Keys.GLASS_AMBIENT] ?: 0.72f,
            whiteOpacity = prefs[Keys.GLASS_WHITE] ?: 0.14f,
            frostStrength = prefs[Keys.GLASS_FROST] ?: 0.15f,
            expandedShadow = 0f // expanded shadow removed
        ),
        animationSpeed = prefs[Keys.ANIMATION_SPEED] ?: 0.5f,
        showLyrics = prefs[Keys.SHOW_LYRICS] ?: true,
        tapAction = runCatching { TapAction.valueOf(prefs[Keys.TAP_ACTION] ?: "EXPAND") }.getOrDefault(TapAction.EXPAND),
        doubleTapAction = runCatching { TapAction.valueOf(prefs[Keys.DOUBLE_TAP_ACTION] ?: "PLAY_PAUSE") }.getOrDefault(TapAction.PLAY_PAUSE),
        longPressAction = runCatching { TapAction.valueOf(prefs[Keys.LONG_PRESS_ACTION] ?: "OPEN_APP") }.getOrDefault(TapAction.OPEN_APP),
        swipeTracks = prefs[Keys.SWIPE_TRACKS] ?: true,
        swipeExpand = prefs[Keys.SWIPE_EXPAND] ?: true,
        expandStyle = runCatching { ExpandStyle.valueOf(prefs[Keys.EXPAND_STYLE] ?: "SPRING") }.getOrDefault(ExpandStyle.SPRING),
        elasticity = runCatching { Elasticity.valueOf(prefs[Keys.ELASTICITY] ?: "HIGH") }.getOrDefault(Elasticity.HIGH),
        sharpNotch = prefs[Keys.SHARP_NOTCH] ?: true,
        iphoneNotchEnabled = prefs[Keys.IPHONE_NOTCH_ENABLED] ?: true,
        iphoneNotchWidth = prefs[Keys.IPHONE_NOTCH_WIDTH] ?: 170,
        iphoneNotchHeight = prefs[Keys.IPHONE_NOTCH_HEIGHT] ?: 32,
        cutoutProfile = CutoutProfile.fromName(prefs[Keys.CUTOUT_PROFILE] ?: "IPHONE"),
        hideNotchWhenIdle = prefs[Keys.HIDE_NOTCH_WHEN_IDLE] ?: true,
        compactWindowShrink = prefs[Keys.COMPACT_WINDOW_SHRINK] ?: false,
        expandedOffsetY = prefs[Keys.EXPANDED_OFFSET_Y] ?: 0,
        expandedWidth = prefs[Keys.EXPANDED_WIDTH] ?: 360,
        expandedMinHeight = prefs[Keys.EXPANDED_MIN_HEIGHT] ?: 0,
        showAlbumBackground = prefs[Keys.SHOW_ALBUM_BG] ?: true,
        lowPowerMode = prefs[Keys.LOW_POWER_MODE] ?: true,
        compactBanner = prefs[Keys.COMPACT_BANNER] ?: true,
        compactBannerWidth = prefs[Keys.COMPACT_BANNER_WIDTH] ?: 376,
        compactBannerHeight = prefs[Keys.COMPACT_BANNER_HEIGHT] ?: 112,
        standardBannerWidth = prefs[Keys.STANDARD_BANNER_WIDTH] ?: 376,
        standardBannerHeight = prefs[Keys.STANDARD_BANNER_HEIGHT] ?: 88,
        // Migrasi: nilai lama dalam detik (bawaan 8) diubah ke milidetik.
        whatsAppBannerMs = prefs[Keys.WHATSAPP_BANNER_MS]
            ?: prefs[Keys.WHATSAPP_BANNER_DURATION]?.let { it * 1000 }
            ?: 2500,
        lyricGlowEnabled = prefs[Keys.LYRIC_GLOW_ENABLED] ?: false,
        bannerFontSize = (prefs[Keys.BANNER_FONT_SIZE] ?: 17).coerceIn(10, 20),
        bannerFontWeight = prefs[Keys.BANNER_FONT_WEIGHT] ?: 600,
        lyricFontSize = prefs[Keys.LYRIC_FONT_SIZE] ?: 15,
        lyricFontWeight = prefs[Keys.LYRIC_FONT_WEIGHT] ?: 500,
        glassSweepEnabled = prefs[Keys.GLASS_SWEEP_ENABLED] ?: true,
        hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
        glassSweepOpacity = prefs[Keys.GLASS_SWEEP_OPACITY] ?: 0.70f,
        glassSweepDurationMs = prefs[Keys.GLASS_SWEEP_DURATION] ?: 800,
        backgroundOpacity = (prefs[Keys.BACKGROUND_OPACITY] ?: 0.80f).coerceIn(0f, 1f),
        edgeLensStrength = (prefs[Keys.EDGE_LENS_STRENGTH] ?: 0.7f).coerceIn(0f, 1f),
        sheenStrength = (prefs[Keys.SHEEN_STRENGTH] ?: 0.85f).coerceIn(0f, 1f),
        profileSheenExpanded = (prefs[Keys.PROFILE_SHEEN_EXPANDED] ?: 1f).coerceIn(0f, 1.5f),
        profileSheenBanner = (prefs[Keys.PROFILE_SHEEN_BANNER] ?: 1f).coerceIn(0f, 1.5f),
        lowEndMode = prefs[Keys.LOW_END_MODE] ?: false,
        glassSurfaceOpacity = (prefs[Keys.GLASS_SURFACE_OPACITY] ?: 0f).coerceIn(0f, 1f),
        stackGap = (prefs[Keys.STACK_GAP] ?: 0.2917f).coerceIn(0f, 1f),
        stackInset = (prefs[Keys.STACK_INSET] ?: 0.5f).coerceIn(0f, 1f),
        whatsAppIndicatorLeft = prefs[Keys.WHATSAPP_INDICATOR_LEFT] ?: true,
        hideStatusIcons = prefs[Keys.HIDE_STATUS_ICONS] ?: true,
        diagnosticsEnabled = prefs[Keys.DIAGNOSTICS_ENABLED] ?: false,
        animationOption = runCatching { AnimationOption.valueOf(prefs[Keys.ANIMATION_OPTION] ?: "OPTION_3") }.getOrDefault(AnimationOption.OPTION_3),
        liquidExpand = prefs[Keys.LIQUID_EXPAND] ?: true,
        lyricShineEnabled = prefs[Keys.LYRIC_SHINE_ENABLED] ?: false,
        lyricShineSpeed = prefs[Keys.LYRIC_SHINE_SPEED] ?: 1f,
        lyricShineIntensity = prefs[Keys.LYRIC_SHINE_INTENSITY] ?: 0.35f,
        lyricGlowIntensity = prefs[Keys.LYRIC_GLOW_INTENSITY] ?: 0.14f,
        expandedCornerRadius = prefs[Keys.EXPANDED_CORNER_RADIUS] ?: 35,
        islandTheme = runCatching {
            when (prefs[Keys.ISLAND_THEME] ?: "ADAPTIVE") {
                "LIGHT", "NATIVE_WHITE", "WHITE" -> IslandThemeMode.WHITE
                "DARK", "NATIVE_BLACK", "BLACK" -> IslandThemeMode.BLACK
                "OUTLINE" -> IslandThemeMode.OUTLINE
                else -> IslandThemeMode.ADAPTIVE
            }
        }.getOrDefault(IslandThemeMode.ADAPTIVE),
        aiEnabled = prefs[Keys.AI_ENABLED] ?: false,
        aiProvider = prefs[Keys.AI_PROVIDER] ?: "openai",
        aiBaseUrl = (prefs[Keys.AI_BASE_URL] ?: "https://api.openai.com/v1/chat/completions").trim(),
        aiApiKey = secretFrom(prefs, Keys.AI_API_KEY_ENC, Keys.AI_API_KEY),
        aiBraveSearchKey = secretFrom(prefs, Keys.AI_BRAVE_SEARCH_KEY_ENC, Keys.AI_BRAVE_SEARCH_KEY),
        aiExtraKeys = secretFrom(prefs, Keys.AI_EXTRA_KEYS_ENC, Keys.AI_EXTRA_KEYS),
        aiFallbackApis = secretFrom(prefs, Keys.AI_FALLBACK_APIS_ENC, Keys.AI_FALLBACK_APIS),
        aiModel = (prefs[Keys.AI_MODEL] ?: "").trim(),
        aiTemperature = prefs[Keys.AI_TEMPERATURE] ?: 0.7f,
        aiMaxTokens = (prefs[Keys.AI_MAX_TOKENS] ?: 4096).coerceIn(1024, 16384),
        aiPanelMinHeight = (prefs[Keys.AI_PANEL_MIN_HEIGHT] ?: 520).coerceIn(320, 880),
        aiSystemPrompt = prefs[Keys.AI_SYSTEM_PROMPT] ?: "",
        aiScreenAnalysis = prefs[Keys.AI_SCREEN_ANALYSIS] ?: true,
        aiStreaming = prefs[Keys.AI_STREAMING] ?: true,
        aiMemoryEnabled = prefs[Keys.AI_MEMORY_ENABLED] ?: false
    )

    val settings: Flow<IslandSettings> = live.filterNotNull()

    suspend fun update(transform: (IslandSettings) -> IslandSettings) {
        mutex.withLock {
            // If init has not run yet, this IS the first authoritative read and must resolve
            // DeviceProfile and the fast flag itself — the init coroutine will see live != null and skip.
            val firstRead = live.value == null
            val base = live.value ?: readAuthoritative()
            val next = transform(base).normalized()
            live.value = next
            schedulePersist(next)
            // Resolve to the post-transform value in one step, so a first-ever Low-End toggle goes
            // UNRESOLVED -> final tier without passing through the other tier.
            if (firstRead || next.lowEndMode != base.lowEndMode) resolveLowEnd(next.lowEndMode)
        }
    }

    private fun schedulePersist(snapshot: IslandSettings) {
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            context.dataStore.edit { prefs -> writeSettings(prefs, snapshot) }
        }
    }

    /**
     * Writes the latest in-memory snapshot to DataStore and the fast flag to disk right now, skipping
     * the [PERSIST_DEBOUNCE_MS] wait and apply()'s async write. Call before deliberately killing the
     * process (Shizuku force-stop restart), otherwise a just-changed Low-End mode could be lost.
     * Serialized through the same mutex as update()/resetAll(). No-op before the first read.
     */
    suspend fun flush() {
        mutex.withLock {
            val snapshot = live.value ?: return
            persistJob?.cancel()
            context.dataStore.edit { prefs -> writeSettings(prefs, snapshot) }
            writeLowEndFastFlag(context, snapshot.lowEndMode, sync = true)
        }
    }

    suspend fun resetAll() {
        mutex.withLock {
            persistJob?.cancel()
            context.dataStore.edit { it.clear() }
            val reset = read(emptyPreferences())
            live.value = reset
            resolveLowEnd(reset.lowEndMode)
        }
    }

    /** Clamps applied on write, mirrored in memory so the live value matches what is persisted. */
    private fun IslandSettings.normalized(): IslandSettings = copy(
        expandedWidth = expandedWidth.coerceIn(180, 1200),
        standardBannerWidth = standardBannerWidth.coerceIn(200, 900),
        standardBannerHeight = standardBannerHeight.coerceIn(64, 180),
        whatsAppBannerMs = whatsAppBannerMs.coerceIn(1500, 10000),
        bannerFontSize = bannerFontSize.coerceIn(10, 20),
        bannerFontWeight = bannerFontWeight.coerceIn(300, 900),
        lyricFontSize = lyricFontSize.coerceIn(9, 22),
        lyricFontWeight = lyricFontWeight.coerceIn(300, 900),
        glassSweepOpacity = glassSweepOpacity.coerceIn(0f, 1f),
        glassSweepDurationMs = glassSweepDurationMs.coerceIn(500, 2600),
        backgroundOpacity = backgroundOpacity.coerceIn(0f, 1f),
        edgeLensStrength = edgeLensStrength.coerceIn(0f, 1f),
        sheenStrength = sheenStrength.coerceIn(0f, 1f),
        glassSurfaceOpacity = glassSurfaceOpacity.coerceIn(0f, 1f),
        stackGap = stackGap.coerceIn(0f, 1f),
        stackInset = stackInset.coerceIn(0f, 1f),
        lyricShineSpeed = lyricShineSpeed.coerceIn(0.25f, 2f),
        lyricShineIntensity = lyricShineIntensity.coerceIn(0f, 0.8f),
        lyricGlowIntensity = lyricGlowIntensity.coerceIn(0f, 0.7f),
        expandedCornerRadius = expandedCornerRadius.coerceIn(8, 48),
        aiTemperature = aiTemperature.coerceIn(0f, 2f),
        aiMaxTokens = aiMaxTokens.coerceIn(1024, 16384)
    )

    private fun writeSettings(prefs: MutablePreferences, next: IslandSettings) {
        prefs[Keys.ENABLED] = next.enabled
        prefs[Keys.OFFSET_X] = next.offsetX
        prefs[Keys.OFFSET_Y] = next.offsetY
        prefs[Keys.GLASS_TRANSPARENCY] = next.glassConfig.transparency
        prefs[Keys.GLASS_REFRACTION] = next.glassConfig.refraction
        prefs[Keys.GLASS_EDGE] = next.glassConfig.edgeHighlight
        prefs[Keys.GLASS_AMBIENT] = next.glassConfig.ambientReflection
        prefs[Keys.GLASS_WHITE] = next.glassConfig.whiteOpacity
        prefs[Keys.GLASS_FROST] = next.glassConfig.frostStrength
        prefs[Keys.GLASS_EXPANDED_SHADOW] = next.glassConfig.expandedShadow
        prefs[Keys.ANIMATION_SPEED] = next.animationSpeed
        prefs[Keys.SHOW_LYRICS] = next.showLyrics
        prefs[Keys.TAP_ACTION] = next.tapAction.name
        prefs[Keys.DOUBLE_TAP_ACTION] = next.doubleTapAction.name
        prefs[Keys.LONG_PRESS_ACTION] = next.longPressAction.name
        prefs[Keys.SWIPE_TRACKS] = next.swipeTracks
        prefs[Keys.SWIPE_EXPAND] = next.swipeExpand
        prefs[Keys.EXPAND_STYLE] = next.expandStyle.name
        prefs[Keys.ELASTICITY] = next.elasticity.name
        prefs[Keys.SHARP_NOTCH] = next.sharpNotch
        prefs[Keys.IPHONE_NOTCH_ENABLED] = next.iphoneNotchEnabled
        prefs[Keys.IPHONE_NOTCH_WIDTH] = next.iphoneNotchWidth
        prefs[Keys.IPHONE_NOTCH_HEIGHT] = next.iphoneNotchHeight
        prefs[Keys.CUTOUT_PROFILE] = next.cutoutProfile.name
        prefs[Keys.HIDE_NOTCH_WHEN_IDLE] = next.hideNotchWhenIdle
        prefs[Keys.COMPACT_WINDOW_SHRINK] = next.compactWindowShrink
        prefs[Keys.EXPANDED_OFFSET_Y] = next.expandedOffsetY
        prefs[Keys.EXPANDED_WIDTH] = next.expandedWidth.coerceIn(180, 1200)
        prefs[Keys.EXPANDED_MIN_HEIGHT] = next.expandedMinHeight
        prefs[Keys.SHOW_ALBUM_BG] = next.showAlbumBackground
        prefs[Keys.LOW_POWER_MODE] = next.lowPowerMode
        prefs[Keys.COMPACT_BANNER] = next.compactBanner
        prefs[Keys.COMPACT_BANNER_WIDTH] = next.compactBannerWidth
        prefs[Keys.COMPACT_BANNER_HEIGHT] = next.compactBannerHeight
        prefs[Keys.STANDARD_BANNER_WIDTH] = next.standardBannerWidth.coerceIn(200, 900)
        prefs[Keys.STANDARD_BANNER_HEIGHT] = next.standardBannerHeight.coerceIn(64, 180)
        prefs[Keys.WHATSAPP_BANNER_MS] = next.whatsAppBannerMs.coerceIn(1500, 10000)
        prefs[Keys.LYRIC_GLOW_ENABLED] = next.lyricGlowEnabled
        prefs[Keys.BANNER_FONT_SIZE] = next.bannerFontSize.coerceIn(10, 20)
        prefs[Keys.BANNER_FONT_WEIGHT] = next.bannerFontWeight.coerceIn(300, 900)
        prefs[Keys.LYRIC_FONT_SIZE] = next.lyricFontSize.coerceIn(9, 22)
        prefs[Keys.LYRIC_FONT_WEIGHT] = next.lyricFontWeight.coerceIn(300, 900)
        prefs[Keys.GLASS_SWEEP_ENABLED] = next.glassSweepEnabled
        prefs[Keys.HAPTICS_ENABLED] = next.hapticsEnabled
        prefs[Keys.GLASS_SWEEP_OPACITY] = next.glassSweepOpacity.coerceIn(0f, 1f)
        prefs[Keys.GLASS_SWEEP_DURATION] = next.glassSweepDurationMs.coerceIn(500, 2600)
        prefs[Keys.BACKGROUND_OPACITY] = next.backgroundOpacity.coerceIn(0f, 1f)
        prefs[Keys.EDGE_LENS_STRENGTH] = next.edgeLensStrength.coerceIn(0f, 1f)
        prefs[Keys.SHEEN_STRENGTH] = next.sheenStrength.coerceIn(0f, 1f)
        prefs[Keys.PROFILE_SHEEN_EXPANDED] = next.profileSheenExpanded.coerceIn(0f, 1.5f)
        prefs[Keys.PROFILE_SHEEN_BANNER] = next.profileSheenBanner.coerceIn(0f, 1.5f)
        prefs[Keys.LOW_END_MODE] = next.lowEndMode
        prefs[Keys.GLASS_SURFACE_OPACITY] = next.glassSurfaceOpacity.coerceIn(0f, 1f)
        prefs[Keys.STACK_GAP] = next.stackGap.coerceIn(0f, 1f)
        prefs[Keys.STACK_INSET] = next.stackInset.coerceIn(0f, 1f)
        prefs[Keys.WHATSAPP_INDICATOR_LEFT] = next.whatsAppIndicatorLeft
        prefs[Keys.HIDE_STATUS_ICONS] = next.hideStatusIcons
        prefs[Keys.DIAGNOSTICS_ENABLED] = next.diagnosticsEnabled
        prefs[Keys.ANIMATION_OPTION] = next.animationOption.name
        prefs[Keys.LIQUID_EXPAND] = next.liquidExpand
        prefs[Keys.LYRIC_SHINE_ENABLED] = next.lyricShineEnabled
        prefs[Keys.LYRIC_SHINE_SPEED] = next.lyricShineSpeed.coerceIn(0.25f, 2f)
        prefs[Keys.LYRIC_SHINE_INTENSITY] = next.lyricShineIntensity.coerceIn(0f, 0.8f)
        prefs[Keys.LYRIC_GLOW_INTENSITY] = next.lyricGlowIntensity.coerceIn(0f, 0.7f)
        prefs[Keys.EXPANDED_CORNER_RADIUS] = next.expandedCornerRadius.coerceIn(8, 48)
        prefs[Keys.ISLAND_THEME] = next.islandTheme.name
        prefs[Keys.AI_ENABLED] = next.aiEnabled
        prefs[Keys.AI_PROVIDER] = next.aiProvider
        prefs[Keys.AI_BASE_URL] = next.aiBaseUrl.trim()
        prefs.remove(stringPreferencesKey("ai_brightdata_token"))
        prefs.remove(stringPreferencesKey("ai_brightdata_token_enc"))
        prefs.remove(stringPreferencesKey("ai_brightdata_zone"))
        prefs.remove(stringPreferencesKey("ai_brightdata_zone_enc"))
        writeSecret(prefs, Keys.AI_API_KEY_ENC, Keys.AI_API_KEY, next.aiApiKey)
        writeSecret(prefs, Keys.AI_BRAVE_SEARCH_KEY_ENC, Keys.AI_BRAVE_SEARCH_KEY, next.aiBraveSearchKey)
        writeSecret(prefs, Keys.AI_EXTRA_KEYS_ENC, Keys.AI_EXTRA_KEYS, next.aiExtraKeys)
        writeSecret(prefs, Keys.AI_FALLBACK_APIS_ENC, Keys.AI_FALLBACK_APIS, next.aiFallbackApis)
        prefs[Keys.AI_MODEL] = next.aiModel.trim()
        prefs[Keys.AI_TEMPERATURE] = next.aiTemperature.coerceIn(0f, 2f)
        prefs[Keys.AI_MAX_TOKENS] = next.aiMaxTokens.coerceIn(1024, 16384)
        prefs[Keys.AI_PANEL_MIN_HEIGHT] = next.aiPanelMinHeight.coerceIn(320, 880)
        prefs[Keys.AI_SYSTEM_PROMPT] = next.aiSystemPrompt
        prefs[Keys.AI_SCREEN_ANALYSIS] = next.aiScreenAnalysis
        prefs[Keys.AI_STREAMING] = next.aiStreaming
        prefs[Keys.AI_MEMORY_ENABLED] = next.aiMemoryEnabled
    }
}

enum class AnimationOption { OPTION_1, OPTION_2, OPTION_3 }

/** Visual palette used by the floating island, separate from the app settings screen theme. */
enum class IslandThemeMode { ADAPTIVE, WHITE, BLACK, OUTLINE }

data class IslandSettings(
    val enabled: Boolean = true,
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val glassConfig: GlassConfig = GlassConfig(),
    val animationSpeed: Float = 0.5f,
    val showLyrics: Boolean = true,
    val tapAction: TapAction = TapAction.EXPAND,
    val doubleTapAction: TapAction = TapAction.PLAY_PAUSE,
    val longPressAction: TapAction = TapAction.OPEN_APP,
    val swipeTracks: Boolean = true,
    val swipeExpand: Boolean = true,
    val expandStyle: ExpandStyle = ExpandStyle.SPRING,
    val elasticity: Elasticity = Elasticity.HIGH,
    val sharpNotch: Boolean = true,
    /** Persistent iPhone-style top notch overlay for waterdrop/punch-hole displays. */
    val iphoneNotchEnabled: Boolean = true,
    val iphoneNotchWidth: Int = 170,
    val iphoneNotchHeight: Int = 32,
    /** Profil bentuk cutout. AUTO mengikuti deteksi layar; CUSTOM memakai nilai slider. */
    val cutoutProfile: CutoutProfile = CutoutProfile.IPHONE,
    /** Hide the idle notch overlay when there is no active island content. */
    val hideNotchWhenIdle: Boolean = true,
    /** Experimental: shrink the drawing window to the capsule height while the capsule idles. Off by default. */
    val compactWindowShrink: Boolean = false,
    val expandedOffsetY: Int = 0,
    val expandedWidth: Int = 360,
    val expandedMinHeight: Int = 0,
    val showAlbumBackground: Boolean = true,
    /** Reduce redraw cadence and expensive glass effects without removing features. */
    val lowPowerMode: Boolean = true,
    val compactBanner: Boolean = true,
    val compactBannerWidth: Int = 376,
    val compactBannerHeight: Int = 112,
    val standardBannerWidth: Int = 376,
    val standardBannerHeight: Int = 88,
    /** How long a WhatsApp notification banner stays visible (ms), same default as Dynamic Island: 2,5 s. */
    val whatsAppBannerMs: Int = 2500,
    val lyricGlowEnabled: Boolean = false,
    val bannerFontSize: Int = 17,
    val bannerFontWeight: Int = 600,
    val lyricFontSize: Int = 15,
    val lyricFontWeight: Int = 500,
    val glassSweepEnabled: Boolean = true,
    /** Light haptic tick on island controls (seek, play/pause, like, expand). */
    val hapticsEnabled: Boolean = true,
    val glassSweepOpacity: Float = 0.70f,
    val glassSweepDurationMs: Int = 800,
    /** Opacity of the album-art backdrop and its dark base (0.35 = clearly see-through, 1 = solid). */
    val backgroundOpacity: Float = 0.80f,
    /** Strength of the magnifier-style refraction rim along the glass edge (0 = off). */
    val edgeLensStrength: Float = 0.7f,
    /** Liquid sheen across the glass body (0 = off). */
    val sheenStrength: Float = 0.85f,
    /** Sheen multiplier for the expanded card (1 = global sheen). */
    val profileSheenExpanded: Float = 1f,
    /** Sheen multiplier for notification banners (1 = global sheen). */
    val profileSheenBanner: Float = 1f,
    /** Opt-in lighter rendering for low-end or mid-range phones. Off = full effects. */
    val lowEndMode: Boolean = false,
    /** Opacity of the glass body behind cards and the stack (0 = clear). */
    val glassSurfaceOpacity: Float = 0f,
    /** Gap between WhatsApp stack cards, 0..1 mapped to 0..24 dp. */
    val stackGap: Float = 0.2917f,
    /** Inner padding of the stack surface, 0..1 mapped to 0..16 dp. */
    val stackInset: Float = 0.5f,
    /** Place the unread WhatsApp marker on the left (default) or right of the capsule. */
    val whatsAppIndicatorLeft: Boolean = true,
    val hideStatusIcons: Boolean = true,
    val diagnosticsEnabled: Boolean = false,
    val animationOption: AnimationOption = AnimationOption.OPTION_3,
    /** New content grows out of the previous shape (liquid expansion). */
    val liquidExpand: Boolean = true,
    val lyricShineEnabled: Boolean = false,
    val lyricShineSpeed: Float = 1f,
    val lyricShineIntensity: Float = 0.35f,
    val lyricGlowIntensity: Float = 0.14f,
    val expandedCornerRadius: Int = 35,
    val islandTheme: IslandThemeMode = IslandThemeMode.ADAPTIVE,
    // ---- Asisten AI (OpenAI-compatible chat completions) ----
    /** Master switch: AI chat card inside the expanded island. */
    val aiEnabled: Boolean = false,
    /**
     * Provider id driving both the request format and the settings-screen presets:
     * "openai" (OpenAI-compatible: OpenAI, OpenRouter, Groq, Ollama/LM Studio, kustom),
     * or "gemini" (Google AI Studio OpenAI-compatible Chat Completions adapter).
     */
    val aiProvider: String = "openai",
    /** Ready-to-use endpoint (OpenAI-compatible: .../v1/chat/completions; Gemini: .../v1beta/openai/chat/completions). */
    val aiBaseUrl: String = "https://api.openai.com/v1/chat/completions",
    /** Secret key stored in DataStore on device only; sent only to the configured endpoint. */
    val aiApiKey: String = "",
    /** Optional Brave LLM Context key; encrypted with Android Keystore and used only for web search. */
    val aiBraveSearchKey: String = "",
    /** Backup keys, one per line. Up to 4 more after the primary key. */
    val aiExtraKeys: String = "",
    /** Cross-provider failover profiles: provider|baseUrl|model|apiKey, encrypted at rest. */
    val aiFallbackApis: String = "",
    val aiModel: String = "",
    val aiTemperature: Float = 0.7f,
    val aiMaxTokens: Int = 4096,
    /** Tinggi minimum (dp) panel AI saat terbuka; isi chat lebih tinggi tetap mengikuti isi. */
    val aiPanelMinHeight: Int = 520,
    /** Optional persona/instruction prefix composed with the built-in island instructions. */
    val aiSystemPrompt: String = "",
    /** Allow the AI to read the visible screen (foreground app + text) only when explicitly asked. */
    val aiScreenAnalysis: Boolean = true,
    /** Stream tokens as they arrive (SSE). Turn off for endpoints that mishandle streaming. */
    val aiStreaming: Boolean = true,
    /** Explicit opt-in: encrypted, local-only facts the user asks the assistant to remember. */
    val aiMemoryEnabled: Boolean = false
)

/** Reads an encrypted secret; falls back to the old plain-text key so existing installs migrate on next save. */
private fun secretFrom(prefs: Preferences, enc: Preferences.Key<String>, legacy: Preferences.Key<String>): String =
    prefs[enc]?.let { com.android.island.ai.SecretVault.decrypt(it) } ?: prefs[legacy].orEmpty()

/** Writes a secret encrypted and removes the plain-text copy. Never stores plain text as a fallback. */
private fun writeSecret(prefs: MutablePreferences, enc: Preferences.Key<String>, legacy: Preferences.Key<String>, value: String) {
    val blob = com.android.island.ai.SecretVault.encrypt(value)
    prefs.remove(legacy)
    if (blob != null) {
        prefs[enc] = blob
    } else {
        prefs.remove(enc)
        com.android.island.util.IslandDiagnostics.record("AI_KEY_NOT_SAVED", "Keystore tidak tersedia; API key tidak disimpan")
    }
}
