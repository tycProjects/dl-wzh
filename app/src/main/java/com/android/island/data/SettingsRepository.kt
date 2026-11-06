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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "island_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val OFFSET_X = intPreferencesKey("offset_x")
        val OFFSET_Y = intPreferencesKey("offset_y")
        val GLASS_TRANSPARENCY = floatPreferencesKey("glass_transparency")
        val GLASS_REFRACTION = floatPreferencesKey("glass_refraction")
        val GLASS_EDGE = floatPreferencesKey("glass_edge")
        val GLASS_CHROMATIC = floatPreferencesKey("glass_chromatic")
        val GLASS_AMBIENT = floatPreferencesKey("glass_ambient")
        val GLASS_WHITE = floatPreferencesKey("glass_white")
        val GLASS_FROST = floatPreferencesKey("glass_frost")
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
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val WHATSAPP_INDICATOR_LEFT = booleanPreferencesKey("whatsapp_indicator_left")
        val HIDE_STATUS_ICONS = booleanPreferencesKey("hide_status_icons")
        val DIAGNOSTICS_ENABLED = booleanPreferencesKey("diagnostics_enabled")
        val ANIMATION_OPTION = stringPreferencesKey("animation_option")
        val LIQUID_EXPAND = booleanPreferencesKey("liquid_expand")
        val FX_BLUR = booleanPreferencesKey("fx_blur")
        val LYRIC_SHINE_ENABLED = booleanPreferencesKey("lyric_shine_enabled")
        val LYRIC_SHINE_SPEED = floatPreferencesKey("lyric_shine_speed")
        val LYRIC_SHINE_INTENSITY = floatPreferencesKey("lyric_shine_intensity")
        val LYRIC_GLOW_INTENSITY = floatPreferencesKey("lyric_glow_intensity")
        val EXPANDED_CORNER_RADIUS = intPreferencesKey("expanded_corner_radius")
        val ISLAND_THEME = stringPreferencesKey("island_theme")
    }

    private fun read(prefs: Preferences): IslandSettings = IslandSettings(
        enabled = prefs[Keys.ENABLED] ?: true,
        offsetX = prefs[Keys.OFFSET_X] ?: 0,
        offsetY = prefs[Keys.OFFSET_Y] ?: 0,
        glassConfig = GlassConfig(
            transparency = prefs[Keys.GLASS_TRANSPARENCY] ?: 0.62f,
            refraction = prefs[Keys.GLASS_REFRACTION] ?: 0.22f,
            edgeHighlight = prefs[Keys.GLASS_EDGE] ?: 0.82f,
            chromaticAberration = prefs[Keys.GLASS_CHROMATIC] ?: 0.10f,
            ambientReflection = prefs[Keys.GLASS_AMBIENT] ?: 0.72f,
            whiteOpacity = prefs[Keys.GLASS_WHITE] ?: 0.45f,
            frostStrength = prefs[Keys.GLASS_FROST] ?: 0.62f
        ),
        animationSpeed = prefs[Keys.ANIMATION_SPEED] ?: 0.9f,
        showLyrics = prefs[Keys.SHOW_LYRICS] ?: true,
        tapAction = runCatching { TapAction.valueOf(prefs[Keys.TAP_ACTION] ?: "EXPAND") }.getOrDefault(TapAction.EXPAND),
        doubleTapAction = runCatching { TapAction.valueOf(prefs[Keys.DOUBLE_TAP_ACTION] ?: "PLAY_PAUSE") }.getOrDefault(TapAction.PLAY_PAUSE),
        longPressAction = runCatching { TapAction.valueOf(prefs[Keys.LONG_PRESS_ACTION] ?: "OPEN_APP") }.getOrDefault(TapAction.OPEN_APP),
        swipeTracks = prefs[Keys.SWIPE_TRACKS] ?: true,
        swipeExpand = prefs[Keys.SWIPE_EXPAND] ?: true,
        expandStyle = runCatching { ExpandStyle.valueOf(prefs[Keys.EXPAND_STYLE] ?: "SPRING") }.getOrDefault(ExpandStyle.SPRING),
        elasticity = runCatching { Elasticity.valueOf(prefs[Keys.ELASTICITY] ?: "LOW") }.getOrDefault(Elasticity.LOW),
        sharpNotch = prefs[Keys.SHARP_NOTCH] ?: true,
        iphoneNotchEnabled = prefs[Keys.IPHONE_NOTCH_ENABLED] ?: true,
        iphoneNotchWidth = prefs[Keys.IPHONE_NOTCH_WIDTH] ?: 130,
        iphoneNotchHeight = prefs[Keys.IPHONE_NOTCH_HEIGHT] ?: 32,
        cutoutProfile = CutoutProfile.fromName(prefs[Keys.CUTOUT_PROFILE]),
        hideNotchWhenIdle = prefs[Keys.HIDE_NOTCH_WHEN_IDLE] ?: true,
        expandedOffsetY = prefs[Keys.EXPANDED_OFFSET_Y] ?: 8,
        expandedWidth = prefs[Keys.EXPANDED_WIDTH] ?: 360,
        expandedMinHeight = prefs[Keys.EXPANDED_MIN_HEIGHT] ?: 164,
        showAlbumBackground = prefs[Keys.SHOW_ALBUM_BG] ?: true,
        lowPowerMode = prefs[Keys.LOW_POWER_MODE] ?: true,
        compactBanner = prefs[Keys.COMPACT_BANNER] ?: true,
        compactBannerWidth = prefs[Keys.COMPACT_BANNER_WIDTH] ?: 376,
        compactBannerHeight = prefs[Keys.COMPACT_BANNER_HEIGHT] ?: 58,
        standardBannerWidth = prefs[Keys.STANDARD_BANNER_WIDTH] ?: 376,
        standardBannerHeight = prefs[Keys.STANDARD_BANNER_HEIGHT] ?: 88,
        whatsAppBannerDuration = prefs[Keys.WHATSAPP_BANNER_DURATION] ?: 8,
        lyricGlowEnabled = prefs[Keys.LYRIC_GLOW_ENABLED] ?: false,
        bannerFontSize = (prefs[Keys.BANNER_FONT_SIZE] ?: 17).coerceIn(10, 20),
        bannerFontWeight = prefs[Keys.BANNER_FONT_WEIGHT] ?: 600,
        lyricFontSize = prefs[Keys.LYRIC_FONT_SIZE] ?: 14,
        lyricFontWeight = prefs[Keys.LYRIC_FONT_WEIGHT] ?: 500,
        glassSweepEnabled = prefs[Keys.GLASS_SWEEP_ENABLED] ?: false,
        hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
        glassSweepOpacity = prefs[Keys.GLASS_SWEEP_OPACITY] ?: 0.25f,
        glassSweepDurationMs = prefs[Keys.GLASS_SWEEP_DURATION] ?: 700,
        backgroundOpacity = (prefs[Keys.BACKGROUND_OPACITY] ?: 0.55f).coerceIn(0.35f, 1f),
        edgeLensStrength = (prefs[Keys.EDGE_LENS_STRENGTH] ?: 0.6f).coerceIn(0f, 1f),
        whatsAppIndicatorLeft = prefs[Keys.WHATSAPP_INDICATOR_LEFT] ?: true,
        hideStatusIcons = prefs[Keys.HIDE_STATUS_ICONS] ?: false,
        diagnosticsEnabled = prefs[Keys.DIAGNOSTICS_ENABLED] ?: false,
        animationOption = runCatching { AnimationOption.valueOf(prefs[Keys.ANIMATION_OPTION] ?: "OPTION_2") }.getOrDefault(AnimationOption.OPTION_1),
        liquidExpand = prefs[Keys.LIQUID_EXPAND] ?: true,
        fxBlur = prefs[Keys.FX_BLUR] ?: false,
        lyricShineEnabled = prefs[Keys.LYRIC_SHINE_ENABLED] ?: false,
        lyricShineSpeed = prefs[Keys.LYRIC_SHINE_SPEED] ?: 1f,
        lyricShineIntensity = prefs[Keys.LYRIC_SHINE_INTENSITY] ?: 0.35f,
        lyricGlowIntensity = prefs[Keys.LYRIC_GLOW_INTENSITY] ?: 0.14f,
        expandedCornerRadius = prefs[Keys.EXPANDED_CORNER_RADIUS] ?: 24,
        islandTheme = runCatching {
            when (prefs[Keys.ISLAND_THEME] ?: "ADAPTIVE") {
                "LIGHT", "NATIVE_WHITE", "WHITE" -> IslandThemeMode.WHITE
                "DARK", "NATIVE_BLACK", "BLACK" -> IslandThemeMode.BLACK
                "OUTLINE" -> IslandThemeMode.OUTLINE
                else -> IslandThemeMode.ADAPTIVE
            }
        }.getOrDefault(IslandThemeMode.ADAPTIVE)
    )

    val settings: Flow<IslandSettings> = context.dataStore.data.map { read(it) }.distinctUntilChanged()

    suspend fun update(transform: (IslandSettings) -> IslandSettings) {
        context.dataStore.edit { prefs ->
            val next = transform(read(prefs))
            prefs[Keys.ENABLED] = next.enabled
            prefs[Keys.OFFSET_X] = next.offsetX
            prefs[Keys.OFFSET_Y] = next.offsetY
            prefs[Keys.GLASS_TRANSPARENCY] = next.glassConfig.transparency
            prefs[Keys.GLASS_REFRACTION] = next.glassConfig.refraction
            prefs[Keys.GLASS_EDGE] = next.glassConfig.edgeHighlight
            prefs[Keys.GLASS_CHROMATIC] = next.glassConfig.chromaticAberration
            prefs[Keys.GLASS_AMBIENT] = next.glassConfig.ambientReflection
            prefs[Keys.GLASS_WHITE] = next.glassConfig.whiteOpacity
            prefs[Keys.GLASS_FROST] = next.glassConfig.frostStrength
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
            prefs[Keys.WHATSAPP_BANNER_DURATION] = next.whatsAppBannerDuration.coerceIn(3, 60)
            prefs[Keys.LYRIC_GLOW_ENABLED] = next.lyricGlowEnabled
            prefs[Keys.BANNER_FONT_SIZE] = next.bannerFontSize.coerceIn(10, 20)
            prefs[Keys.BANNER_FONT_WEIGHT] = next.bannerFontWeight.coerceIn(300, 900)
            prefs[Keys.LYRIC_FONT_SIZE] = next.lyricFontSize.coerceIn(9, 22)
            prefs[Keys.LYRIC_FONT_WEIGHT] = next.lyricFontWeight.coerceIn(300, 900)
            prefs[Keys.GLASS_SWEEP_ENABLED] = next.glassSweepEnabled
            prefs[Keys.HAPTICS_ENABLED] = next.hapticsEnabled
            prefs[Keys.GLASS_SWEEP_OPACITY] = next.glassSweepOpacity.coerceIn(0f, 1f)
            prefs[Keys.GLASS_SWEEP_DURATION] = next.glassSweepDurationMs.coerceIn(500, 2600)
            prefs[Keys.BACKGROUND_OPACITY] = next.backgroundOpacity.coerceIn(0.35f, 1f)
            prefs[Keys.EDGE_LENS_STRENGTH] = next.edgeLensStrength.coerceIn(0f, 1f)
            prefs[Keys.WHATSAPP_INDICATOR_LEFT] = next.whatsAppIndicatorLeft
            prefs[Keys.HIDE_STATUS_ICONS] = next.hideStatusIcons
            prefs[Keys.DIAGNOSTICS_ENABLED] = next.diagnosticsEnabled
            prefs[Keys.ANIMATION_OPTION] = next.animationOption.name
            prefs[Keys.LIQUID_EXPAND] = next.liquidExpand
            prefs[Keys.FX_BLUR] = next.fxBlur
            prefs[Keys.LYRIC_SHINE_ENABLED] = next.lyricShineEnabled
            prefs[Keys.LYRIC_SHINE_SPEED] = next.lyricShineSpeed.coerceIn(0.25f, 2f)
            prefs[Keys.LYRIC_SHINE_INTENSITY] = next.lyricShineIntensity.coerceIn(0f, 0.8f)
            prefs[Keys.LYRIC_GLOW_INTENSITY] = next.lyricGlowIntensity.coerceIn(0f, 0.7f)
            prefs[Keys.EXPANDED_CORNER_RADIUS] = next.expandedCornerRadius.coerceIn(8, 48)
            prefs[Keys.ISLAND_THEME] = next.islandTheme.name
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
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
    val animationSpeed: Float = 0.9f,
    val showLyrics: Boolean = true,
    val tapAction: TapAction = TapAction.EXPAND,
    val doubleTapAction: TapAction = TapAction.PLAY_PAUSE,
    val longPressAction: TapAction = TapAction.OPEN_APP,
    val swipeTracks: Boolean = true,
    val swipeExpand: Boolean = true,
    val expandStyle: ExpandStyle = ExpandStyle.SPRING,
    val elasticity: Elasticity = Elasticity.LOW,
    val sharpNotch: Boolean = true,
    /** Persistent iPhone-style top notch overlay for waterdrop/punch-hole displays. */
    val iphoneNotchEnabled: Boolean = true,
    val iphoneNotchWidth: Int = 130,
    val iphoneNotchHeight: Int = 32,
    /** Profil bentuk cutout. AUTO mengikuti deteksi layar; CUSTOM memakai nilai slider. */
    val cutoutProfile: CutoutProfile = CutoutProfile.AUTO,
    /** Hide the idle notch overlay when there is no active island content. */
    val hideNotchWhenIdle: Boolean = true,
    val expandedOffsetY: Int = 8,
    val expandedWidth: Int = 360,
    val expandedMinHeight: Int = 164,
    val showAlbumBackground: Boolean = true,
    /** Reduce redraw cadence and expensive glass effects without removing features. */
    val lowPowerMode: Boolean = true,
    val compactBanner: Boolean = true,
    val compactBannerWidth: Int = 376,
    val compactBannerHeight: Int = 58,
    val standardBannerWidth: Int = 376,
    val standardBannerHeight: Int = 88,
    /** How long a WhatsApp notification banner remains visible before collapsing to its side indicator. */
    val whatsAppBannerDuration: Int = 8,
    val lyricGlowEnabled: Boolean = false,
    val bannerFontSize: Int = 17,
    val bannerFontWeight: Int = 600,
    val lyricFontSize: Int = 14,
    val lyricFontWeight: Int = 500,
    val glassSweepEnabled: Boolean = false,
    /** Light haptic tick on island controls (seek, play/pause, like, expand). */
    val hapticsEnabled: Boolean = true,
    val glassSweepOpacity: Float = 0.25f,
    val glassSweepDurationMs: Int = 700,
    /** Opacity of the album-art backdrop and its dark base (0.35 = clearly see-through, 1 = solid). */
    val backgroundOpacity: Float = 0.55f,
    /** Strength of the magnifier-style refraction rim along the glass edge (0 = off). */
    val edgeLensStrength: Float = 0.6f,
    /** Place the unread WhatsApp marker on the left (default) or right of the capsule. */
    val whatsAppIndicatorLeft: Boolean = true,
    val hideStatusIcons: Boolean = false,
    val diagnosticsEnabled: Boolean = false,
    val animationOption: AnimationOption = AnimationOption.OPTION_2,
    /** New content grows out of the previous shape (liquid expansion). */
    val liquidExpand: Boolean = true,
    /** Light blur while new content emerges (Android 12+). Off by default to keep it light. */
    val fxBlur: Boolean = false,
    val lyricShineEnabled: Boolean = false,
    val lyricShineSpeed: Float = 1f,
    val lyricShineIntensity: Float = 0.35f,
    val lyricGlowIntensity: Float = 0.14f,
    val expandedCornerRadius: Int = 24,
    val islandTheme: IslandThemeMode = IslandThemeMode.ADAPTIVE
)
