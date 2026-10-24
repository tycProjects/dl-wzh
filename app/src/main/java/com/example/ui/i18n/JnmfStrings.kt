package com.example.ui.i18n

import androidx.compose.runtime.compositionLocalOf
import com.example.data.model.RemixCategory
import com.example.data.repository.AppLanguage
import com.example.data.repository.SortOption
import com.example.data.repository.ThemeColor

/**
 * CompositionLocal providing the currently active [AppLanguage] across the entire Compose hierarchy
 * so every screen, dialog, and component updates in place without recreating the Activity.
 */
val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

/**
 * Centralized translation keys for all app-controlled UI text across JNMF Music Player.
 */
enum class StrKey {
    // Navigation & Header
    NAV_MUSIC,
    NAV_PLAYLISTS,
    NAV_EQUALIZER,
    NAV_VIDEOS,
    NAV_SETTINGS,
    TOP_SUBTITLE,
    OFFICIAL_LOGO_DESC,
    QUICK_DUPLICATES,
    QUICK_CLASSIFIER,
    QUICK_NOTIFICATIONS,

    // Transitions (Language & Theme Color)
    APPLYING_LANGUAGE,
    APPLYING_THEME_TEMPLATE,

    // Startup Screen
    STARTUP_SUBTITLE,
    STARTUP_INIT_TITLE,
    STARTUP_INIT_STATUS,
    STARTUP_SCAN_TITLE,
    STARTUP_SCAN_STATUS,
    STARTUP_SCAN_PROGRESS_TEMPLATE,
    STARTUP_SCANNING_TRACKS,
    STARTUP_READY_TITLE,
    STARTUP_READY_STATUS_TEMPLATE,
    ENTER_JNMF,
    SKIP,

    // Notifications & Dialog
    NOTIF_DIALOG_TITLE,
    NOTIF_STATUS_ACTIVE,
    NOTIF_STATUS_DISABLED,
    NOTIF_DESCRIPTION,
    NOTIF_GRANT_PERMISSION,
    NOTIF_OPEN_SETTINGS,
    NOTIF_TEST,
    NOTIF_TEST_ACTIVE_STATUS,
    NOTIF_UPDATED_FEEDBACK,
    NOTIF_SETTINGS_SUBTITLE,
    NOTIF_READY_FOR_PLAYBACK,
    NOTIF_CHANNEL_NAME,
    NOTIF_CHANNEL_DESC,

    // Common Actions & Labels
    CLOSE,
    CANCEL,
    CREATE,
    SAVE,
    DONE,
    ADD,
    RENAME,
    DELETE,
    RESTORE,
    MANAGE,
    BACK,
    RETRY,
    DISMISS,
    SEARCH,
    CLEAR_SEARCH,
    MORE_OPTIONS,
    FAVORITE,
    SELECTED,
    DEFAULT_BADGE,
    ACTIVE_BADGE,
    ERROR_LABEL,
    COMPLETED_LABEL,
    LOADING_DEFAULT,

    // Search, Sorting & Music Library
    SEARCH_PLACEHOLDER,
    SEARCH_TRACKS_COUNT_TEMPLATE,
    SORT_BY,
    SORT_TRACKS_TITLE,
    SORT_TITLE,
    SORT_ARTIST,
    SORT_DATE,
    SORT_DURATION,
    SORT_NAME_ASC,
    SORT_NAME_DESC,
    SORT_DATE_DESC,
    SORT_DATE_ASC,
    SORT_SIZE_DESC,
    SORT_SIZE_ASC,
    AUDIO_ACCESS_REQUIRED,
    GRANT_AUDIO_ACCESS,
    PERMISSION_DESC,
    REFRESH_LIBRARY,
    RESCAN_STORAGE,
    NO_SONGS_FOUND,
    NO_TRACKS_MATCH_QUERY_TEMPLATE,
    TRACKS_COUNT_TEMPLATE,
    PLAY_NOW,
    ADD_TO_PLAYLIST,
    REMOVE_FROM_PLAYLIST,
    HIDE_FROM_LIBRARY,
    DELETE_FILE,

    // Player, MiniPlayer & Now Playing
    NOW_PLAYING,
    NOW_PLAYING_HEADER,
    CLOSE_NOW_PLAYING,
    OPEN_EQUALIZER,
    PLAY,
    PAUSE,
    PLAYING,
    PAUSED,
    PREVIOUS,
    NEXT,
    QUEUE_TITLE,
    REVERSE_QUEUE,
    REVERSE_QUEUE_ON,
    REVERSE_QUEUE_OFF,
    SHUFFLE,
    REPEAT,
    SPEED,
    ERROR_AUDIO_MISSING,
    ERROR_AUDIO_UNSUPPORTED,
    ERROR_CANNOT_PLAY_TRACK,
    ERROR_PLAYBACK_GENERIC,

    // Remix Categories & Badges
    REMIX_STANDARD,
    REMIX_FULLBEAT,
    REMIX_SLOW,
    REMIX_INDO,
    REMIX_BREAKBEAT,
    REMIX_SHORT_FB,
    REMIX_SHORT_SLOW,
    REMIX_SHORT_REMIX,
    REMIX_SHORT_BB,

    // Equalizer & Sound Engine
    EQ_TITLE,
    EQ_SUBTITLE,
    EQ_MASTER,
    EQ_SESSION_STATUS,
    EQ_ACTIVE,
    EQ_PIPELINE_ATTACHED_TEMPLATE,
    EQ_IDLE,
    EQ_HARDWARE_ACTIVE,
    EQ_SOFTWARE_ACTIVE,
    SOUND_PRESETS,
    EQ_FREQUENCY_BANDS_HEADER,
    EQ_ACOUSTIC_ENHANCEMENT_HEADER,
    BASS_HOREG,
    BASS_HOREG_SUBTITLE,
    STEREO_WIDTH,
    STEREO_WIDTH_SUBTITLE,
    TREBLE_CLARITY,
    TREBLE_CLARITY_SUBTITLE,
    PRESET_INDO_FULLBEAT,
    PRESET_BASS_HOREG,
    PRESET_SLOW_REMIX,
    PRESET_CLUB_REMIX,
    PRESET_VOCAL_CLARITY,
    PRESET_FLAT,
    PRESET_CUSTOM,
    RESET_DEFAULT,

    // Playlists & Global Picker
    PLAYLISTS_TITLE,
    CUSTOM_PLAYLISTS_HEADER,
    FAVORITES_TITLE,
    SAVED_FAVORITES_SUBTITLE_TEMPLATE,
    EMPTY_FAVORITES,
    SAVED_TRACKS_COUNT_TEMPLATE,
    NEW_PLAYLIST,
    CREATE_NEW_PLAYLIST,
    NEW_PLAYLIST_NAME_LABEL,
    ENTER_PLAYLIST_NAME,
    CREATE_AND_ADD_TRACK,
    ADD_SONG_TO_PLAYLIST_TITLE_TEMPLATE,
    NO_PLAYLISTS_CREATE_PROMPT,
    EMPTY_PLAYLISTS,
    PLAYLIST_EMPTY,
    ADD_TRACKS,
    ADD_TRACKS_NOW,
    PLAY_ALL,
    MOVE_UP,
    MOVE_DOWN,
    ADD_TRACKS_TO_PLAYLIST_TITLE_TEMPLATE,
    ALL_SONGS_IN_PLAYLIST,
    RENAME_PLAYLIST_TITLE,
    DELETE_PLAYLIST_TITLE,
    DELETE_PLAYLIST_CONFIRM_TEMPLATE,
    MISSING_TRACK_TITLE_TEMPLATE,
    UNKNOWN_LOCATION,
    UNAVAILABLE,

    // Duplicate Detection
    DUPLICATES_TITLE,
    DUPLICATES_THRESHOLD_SUBTITLE,
    ACOUSTIC_SIMILARITY_ENGINE,
    SCAN_SIMILAR_AUDIO,
    RESCAN_LIBRARY_COUNT_TEMPLATE,
    DUPLICATES_DESC,
    SIMILARITY,
    SIMILARITY_SCORE,
    SIMILARITY_MATCH_PERCENT_TEMPLATE,
    VERSION_A,
    VERSION_B,
    PREVIEW_TRACK,
    STARTING_ANALYSIS,
    SCAN_COMPLETED_SHORT,
    DUPLICATE_SCAN_COMPLETED_TEMPLATE,
    NO_DUPLICATES_FOUND,
    TAP_SCAN_TO_ANALYZE,
    CONFIRM_DELETE_TITLE,
    CONFIRM_DELETE_MSG,
    CONFIRM_DELETE_SONG_MSG_TEMPLATE,
    DELETE_TRACK,
    REASON_EXACT_DURATION,
    REASON_CLOSE_DURATION,
    REASON_IDENTICAL_TITLE,
    REASON_IDENTICAL_ARTIST_TEMPLATE,
    REASON_DIFFERENT_REMIX_TEMPLATE,
    REASON_MATCHING_REMIX_TEMPLATE,

    // Classifier
    CLASSIFIER_TITLE,
    CLASSIFIER_DESC,
    FILTER_ALL_COUNT_TEMPLATE,
    EDIT_CATEGORY,
    MANUAL_CATEGORY_CORRECTION_TITLE,

    // Videos
    VIDEOS_TITLE,
    VIDEOS_COUNT_TEMPLATE,
    VIDEOS_FOUND_ON_DEVICE_TEMPLATE,
    NO_VIDEOS_FOUND,
    REFRESH_VIDEOS,
    FULLSCREEN,
    EXIT_FULLSCREEN,
    CLOSE_PLAYER,
    MORE_VIDEOS,
    SEEK_BACKWARD_10S,
    SEEK_FORWARD_10S,
    VIDEO_PLAYBACK_ERROR_TITLE,
    VIDEO_PLAYBACK_ERROR_DEFAULT,
    VIDEO_FILE_MISSING,
    VIDEO_UNSUPPORTED_CODEC,
    VIDEO_LOAD_FAILED,
    VIDEO_BG_PLAYBACK_TITLE,
    VIDEO_MODE_NORMAL,
    VIDEO_MODE_AUDIO_ONLY,
    VIDEO_MODE_NORMAL_DESC,
    VIDEO_MODE_AUDIO_ONLY_DESC,
    VIDEO_AUDIO_ONLY_STAGE_TITLE,
    VIDEO_AUDIO_ONLY_STAGE_SUBTITLE,
    VIDEO_SWITCH_TO_NORMAL,
    VIDEO_STOP,
    VIDEO_MINIMIZE_TO_BG,
    VIDEO_NOTIF_BADGE,
    VIDEO_NOTIF_MODE_TEMPLATE,
    VIDEO_RETURN_TO_PLAYER,

    // Settings, Themes, Languages & Credits
    SETTINGS_TITLE,
    SETTINGS_SUBTITLE,
    UI_COLOR_TITLE,
    UI_COLOR_SUBTITLE,
    COLOR_GREEN,
    COLOR_BLUE,
    COLOR_RED,
    COLOR_WHITE,
    COLOR_CYAN,
    COLOR_VIOLET,
    LANG_TITLE,
    LANG_SUBTITLE,
    LANG_ENGLISH_NAME,
    LANG_ENGLISH_DESC,
    LANG_CEBUANO_NAME,
    LANG_CEBUANO_DESC,
    LANG_INDONESIAN_NAME,
    LANG_INDONESIAN_DESC,
    HIDDEN_SONGS_TITLE,
    NO_HIDDEN_SONGS,
    CREDITS_TITLE,
    CREDITS_SUBTITLE,
    CREDIT_LABEL_APP_NAME,
    CREDIT_LABEL_COMPILE_BY,
    CREDIT_LABEL_CREATE_BY,
    CREDIT_LABEL_DEVELOPER,
    CREDIT_LABEL_TARGET_DEVICE,
    CREDIT_LABEL_AUDIO_FOCUS,

    // Scanning & Progress Messages
    LOADING_SCANNING_MUSIC,
    LOADING_MUSIC_READY,
    LOADING_SCANNING_VIDEOS,
    LOADING_VIDEOS_READY,
    INDEXING_AUDIO_FILES,
    INDEXING_AUDIO,
    DISCOVERING_VIDEOS,
    INDEXING_VIDEOS,
    READING_ITEM_PROGRESS_TEMPLATE,
    FOUND_TRACKS_COUNT_TEMPLATE,
    FOUND_VIDEOS_COUNT_TEMPLATE,
    MUSIC_SCAN_ERROR_TITLE,
    FAILED_SCAN_STORAGE,
    VIDEO_SCAN_ERROR_TITLE,
    FAILED_SCAN_VIDEOS
}

/**
 * Single centralized localization dictionary for English, Cebuano (Bisaya),
 * and Bahasa Indonesia (JUSSARSian), with English as the guaranteed fallback.
 */
object JnmfStrings {

    private val translations: Map<StrKey, Map<AppLanguage, String>> = mapOf(
        // --- Navigation & Header ---
        StrKey.NAV_MUSIC to mapOf(
            AppLanguage.ENGLISH to "Music",
            AppLanguage.CEBUANO to "Musika",
            AppLanguage.INDONESIAN to "Musik"
        ),
        StrKey.NAV_PLAYLISTS to mapOf(
            AppLanguage.ENGLISH to "Playlists",
            AppLanguage.CEBUANO to "Mga Playlist",
            AppLanguage.INDONESIAN to "Daftar Putar"
        ),
        StrKey.NAV_EQUALIZER to mapOf(
            AppLanguage.ENGLISH to "Equalizer",
            AppLanguage.CEBUANO to "Ekwalayser",
            AppLanguage.INDONESIAN to "Ekualiser"
        ),
        StrKey.NAV_VIDEOS to mapOf(
            AppLanguage.ENGLISH to "Videos",
            AppLanguage.CEBUANO to "Mga Video",
            AppLanguage.INDONESIAN to "Video"
        ),
        StrKey.NAV_SETTINGS to mapOf(
            AppLanguage.ENGLISH to "Settings",
            AppLanguage.CEBUANO to "Mga Setting",
            AppLanguage.INDONESIAN to "Pengaturan"
        ),
        StrKey.TOP_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Indo Remix • Fullbeat • Slow",
            AppLanguage.CEBUANO to "Indo Remix • Fullbeat • Hinay",
            AppLanguage.INDONESIAN to "Indo Remix • Fullbeat • Santai"
        ),
        StrKey.OFFICIAL_LOGO_DESC to mapOf(
            AppLanguage.ENGLISH to "JNMF Official Logo",
            AppLanguage.CEBUANO to "Opisyal nga Logo sa JNMF",
            AppLanguage.INDONESIAN to "Logo Resmi JNMF"
        ),
        StrKey.QUICK_DUPLICATES to mapOf(
            AppLanguage.ENGLISH to "Duplicates",
            AppLanguage.CEBUANO to "Mga Doble",
            AppLanguage.INDONESIAN to "Duplikat"
        ),
        StrKey.QUICK_CLASSIFIER to mapOf(
            AppLanguage.ENGLISH to "Classifier",
            AppLanguage.CEBUANO to "Pag-ila sa Remix",
            AppLanguage.INDONESIAN to "Klasifikasi"
        ),
        StrKey.QUICK_NOTIFICATIONS to mapOf(
            AppLanguage.ENGLISH to "Notifications",
            AppLanguage.CEBUANO to "Mga Notipikasyon",
            AppLanguage.INDONESIAN to "Notifikasi"
        ),

        // --- Transitions ---
        StrKey.APPLYING_LANGUAGE to mapOf(
            AppLanguage.ENGLISH to "Applying language...",
            AppLanguage.CEBUANO to "Nag-apply sa pinulongan...",
            AppLanguage.INDONESIAN to "Menerapkan bahasa..."
        ),
        StrKey.APPLYING_THEME_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Applying %s accent...",
            AppLanguage.CEBUANO to "Nag-apply sa kolor nga %s...",
            AppLanguage.INDONESIAN to "Menerapkan aksen %s..."
        ),

        // --- Startup Screen ---
        StrKey.STARTUP_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Indo Remix • Fullbeat • Slow Remix",
            AppLanguage.CEBUANO to "Indo Remix • Fullbeat • Hinay nga Remix",
            AppLanguage.INDONESIAN to "Indo Remix • Fullbeat • Slow Remix"
        ),
        StrKey.STARTUP_INIT_TITLE to mapOf(
            AppLanguage.ENGLISH to "Initializing JNMF Audio Engine",
            AppLanguage.CEBUANO to "Gisugdan ang JNMF Audio Engine",
            AppLanguage.INDONESIAN to "Menyiapkan Mesin Audio JNMF"
        ),
        StrKey.STARTUP_INIT_STATUS to mapOf(
            AppLanguage.ENGLISH to "Setting up hardware equalizer & playback engine...",
            AppLanguage.CEBUANO to "Giandam ang hardware ekwalayser ug makina sa pagpatugtog...",
            AppLanguage.INDONESIAN to "Menyiapkan ekualiser perangkat keras & mesin pemutar..."
        ),
        StrKey.STARTUP_SCAN_TITLE to mapOf(
            AppLanguage.ENGLISH to "Scanning Local Audio Library",
            AppLanguage.CEBUANO to "Nagsusi sa Lokal nga Library sa Audio",
            AppLanguage.INDONESIAN to "Memindai Koleksi Audio Lokal"
        ),
        StrKey.STARTUP_SCAN_STATUS to mapOf(
            AppLanguage.ENGLISH to "Loading Indo Remix & Fullbeat tracks...",
            AppLanguage.CEBUANO to "Gikarga ang mga kanta nga Indo Remix ug Fullbeat...",
            AppLanguage.INDONESIAN to "Memuat lagu Indo Remix & Fullbeat..."
        ),
        StrKey.STARTUP_SCAN_PROGRESS_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "%1\$d / %2\$d tracks (%3\$s)",
            AppLanguage.CEBUANO to "%1\$d / %2\$d ka kanta (%3\$s)",
            AppLanguage.INDONESIAN to "%1\$d / %2\$d lagu (%3\$s)"
        ),
        StrKey.STARTUP_SCANNING_TRACKS to mapOf(
            AppLanguage.ENGLISH to "Scanning tracks...",
            AppLanguage.CEBUANO to "Nagsusi sa mga kanta...",
            AppLanguage.INDONESIAN to "Memindai lagu..."
        ),
        StrKey.STARTUP_READY_TITLE to mapOf(
            AppLanguage.ENGLISH to "JNMF Engine Ready",
            AppLanguage.CEBUANO to "Andam na ang JNMF Engine",
            AppLanguage.INDONESIAN to "Mesin JNMF Siap"
        ),
        StrKey.STARTUP_READY_STATUS_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Loaded %d tracks. Welcome to JNMF!",
            AppLanguage.CEBUANO to "Nakarga ang %d ka kanta. Maayong pag-abot sa JNMF!",
            AppLanguage.INDONESIAN to "Memuat %d lagu. Selamat datang di JNMF!"
        ),
        StrKey.ENTER_JNMF to mapOf(
            AppLanguage.ENGLISH to "Enter JNMF",
            AppLanguage.CEBUANO to "Sulod sa JNMF",
            AppLanguage.INDONESIAN to "Masuk ke JNMF"
        ),
        StrKey.SKIP to mapOf(
            AppLanguage.ENGLISH to "Skip",
            AppLanguage.CEBUANO to "Laktawan",
            AppLanguage.INDONESIAN to "Lewati"
        ),

        // --- Notifications & Dialog ---
        StrKey.NOTIF_DIALOG_TITLE to mapOf(
            AppLanguage.ENGLISH to "Playback Notifications",
            AppLanguage.CEBUANO to "Mga Notipikasyon sa Pagpatugtog",
            AppLanguage.INDONESIAN to "Notifikasi Pemutaran"
        ),
        StrKey.NOTIF_STATUS_ACTIVE to mapOf(
            AppLanguage.ENGLISH to "Active & Enabled",
            AppLanguage.CEBUANO to "Aktibo ug Gitugotan",
            AppLanguage.INDONESIAN to "Aktif & Diizinkan"
        ),
        StrKey.NOTIF_STATUS_DISABLED to mapOf(
            AppLanguage.ENGLISH to "Disabled / Needs Permission",
            AppLanguage.CEBUANO to "Wala Gitugotan / Kinahanglan og Pagtugot",
            AppLanguage.INDONESIAN to "Nonaktif / Butuh Izin"
        ),
        StrKey.NOTIF_DESCRIPTION to mapOf(
            AppLanguage.ENGLISH to "JNMF uses media notifications to provide lock-screen controls, track progress, and playback buttons while music plays in the background.",
            AppLanguage.CEBUANO to "Ang JNMF naggamit og mga notipikasyon sa pagpatugtog aron makahatag og mga kontrol sa lock-screen ug background samtang nagpatugtog sa musika.",
            AppLanguage.INDONESIAN to "JNMF menggunakan notifikasi media untuk menyediakan kontrol layar kunci, info lagu, dan tombol putar saat musik berjalan di latar belakang."
        ),
        StrKey.NOTIF_GRANT_PERMISSION to mapOf(
            AppLanguage.ENGLISH to "Grant Notification Permission",
            AppLanguage.CEBUANO to "Ihatag ang Pagtugot sa Notipikasyon",
            AppLanguage.INDONESIAN to "Berikan Izin Notifikasi"
        ),
        StrKey.NOTIF_OPEN_SETTINGS to mapOf(
            AppLanguage.ENGLISH to "System Notification Settings",
            AppLanguage.CEBUANO to "Mga Setting sa Notipikasyon sa Sistema",
            AppLanguage.INDONESIAN to "Pengaturan Notifikasi Sistem"
        ),
        StrKey.NOTIF_TEST to mapOf(
            AppLanguage.ENGLISH to "Test Notification",
            AppLanguage.CEBUANO to "Sulayi ang Notipikasyon",
            AppLanguage.INDONESIAN to "Uji Notifikasi"
        ),
        StrKey.NOTIF_TEST_ACTIVE_STATUS to mapOf(
            AppLanguage.ENGLISH to "JNMF Notification Active",
            AppLanguage.CEBUANO to "Aktibo ang Notipikasyon sa JNMF",
            AppLanguage.INDONESIAN to "Notifikasi JNMF Aktif"
        ),
        StrKey.NOTIF_UPDATED_FEEDBACK to mapOf(
            AppLanguage.ENGLISH to "Notification updated!",
            AppLanguage.CEBUANO to "Na-update na ang notipikasyon!",
            AppLanguage.INDONESIAN to "Notifikasi diperbarui!"
        ),
        StrKey.NOTIF_SETTINGS_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Lock-screen & background playback controls",
            AppLanguage.CEBUANO to "Mga kontrol sa lock-screen ug background nga pagpatugtog",
            AppLanguage.INDONESIAN to "Kontrol layar kunci & pemutaran latar belakang"
        ),
        StrKey.NOTIF_READY_FOR_PLAYBACK to mapOf(
            AppLanguage.ENGLISH to "Ready for playback",
            AppLanguage.CEBUANO to "Andam sa pagpatugtog",
            AppLanguage.INDONESIAN to "Siap memutar musik"
        ),
        StrKey.NOTIF_CHANNEL_NAME to mapOf(
            AppLanguage.ENGLISH to "JNMF Music Playback",
            AppLanguage.CEBUANO to "JNMF Pagpatugtog sa Musika",
            AppLanguage.INDONESIAN to "Pemutaran Musik JNMF"
        ),
        StrKey.NOTIF_CHANNEL_DESC to mapOf(
            AppLanguage.ENGLISH to "Foreground playback controls for JNMF Music Player",
            AppLanguage.CEBUANO to "Mga kontrol sa pagpatugtog alang sa JNMF Music Player",
            AppLanguage.INDONESIAN to "Kontrol pemutaran latar depan untuk JNMF Music Player"
        ),

        // --- Common Actions & Labels ---
        StrKey.CLOSE to mapOf(
            AppLanguage.ENGLISH to "Close",
            AppLanguage.CEBUANO to "Isira",
            AppLanguage.INDONESIAN to "Tutup"
        ),
        StrKey.CANCEL to mapOf(
            AppLanguage.ENGLISH to "Cancel",
            AppLanguage.CEBUANO to "Kanselahon",
            AppLanguage.INDONESIAN to "Batal"
        ),
        StrKey.CREATE to mapOf(
            AppLanguage.ENGLISH to "Create",
            AppLanguage.CEBUANO to "Paghimo",
            AppLanguage.INDONESIAN to "Buat"
        ),
        StrKey.SAVE to mapOf(
            AppLanguage.ENGLISH to "Save",
            AppLanguage.CEBUANO to "I-save",
            AppLanguage.INDONESIAN to "Simpan"
        ),
        StrKey.DONE to mapOf(
            AppLanguage.ENGLISH to "Done",
            AppLanguage.CEBUANO to "Human na",
            AppLanguage.INDONESIAN to "Selesai"
        ),
        StrKey.ADD to mapOf(
            AppLanguage.ENGLISH to "Add",
            AppLanguage.CEBUANO to "Idugang",
            AppLanguage.INDONESIAN to "Tambah"
        ),
        StrKey.RENAME to mapOf(
            AppLanguage.ENGLISH to "Rename",
            AppLanguage.CEBUANO to "Usba ang Ngalan",
            AppLanguage.INDONESIAN to "Ganti Nama"
        ),
        StrKey.DELETE to mapOf(
            AppLanguage.ENGLISH to "Delete",
            AppLanguage.CEBUANO to "Papasa",
            AppLanguage.INDONESIAN to "Hapus"
        ),
        StrKey.RESTORE to mapOf(
            AppLanguage.ENGLISH to "Restore",
            AppLanguage.CEBUANO to "Ibalik",
            AppLanguage.INDONESIAN to "Pulihkan"
        ),
        StrKey.MANAGE to mapOf(
            AppLanguage.ENGLISH to "Manage",
            AppLanguage.CEBUANO to "Dumalaha",
            AppLanguage.INDONESIAN to "Kelola"
        ),
        StrKey.BACK to mapOf(
            AppLanguage.ENGLISH to "Back",
            AppLanguage.CEBUANO to "Balik",
            AppLanguage.INDONESIAN to "Kembali"
        ),
        StrKey.RETRY to mapOf(
            AppLanguage.ENGLISH to "Retry",
            AppLanguage.CEBUANO to "Sulayi Pag-usab",
            AppLanguage.INDONESIAN to "Coba Lagi"
        ),
        StrKey.DISMISS to mapOf(
            AppLanguage.ENGLISH to "Dismiss",
            AppLanguage.CEBUANO to "Isalikway",
            AppLanguage.INDONESIAN to "Tutup"
        ),
        StrKey.SEARCH to mapOf(
            AppLanguage.ENGLISH to "Search",
            AppLanguage.CEBUANO to "Pangitaa",
            AppLanguage.INDONESIAN to "Cari"
        ),
        StrKey.CLEAR_SEARCH to mapOf(
            AppLanguage.ENGLISH to "Clear search",
            AppLanguage.CEBUANO to "Hawani ang pagpangita",
            AppLanguage.INDONESIAN to "Hapus pencarian"
        ),
        StrKey.MORE_OPTIONS to mapOf(
            AppLanguage.ENGLISH to "More options",
            AppLanguage.CEBUANO to "Dugang nga mga kapilian",
            AppLanguage.INDONESIAN to "Opsi lainnya"
        ),
        StrKey.FAVORITE to mapOf(
            AppLanguage.ENGLISH to "Favorite",
            AppLanguage.CEBUANO to "Paborito",
            AppLanguage.INDONESIAN to "Favorit"
        ),
        StrKey.SELECTED to mapOf(
            AppLanguage.ENGLISH to "Selected",
            AppLanguage.CEBUANO to "Gipili",
            AppLanguage.INDONESIAN to "Dipilih"
        ),
        StrKey.DEFAULT_BADGE to mapOf(
            AppLanguage.ENGLISH to "Default",
            AppLanguage.CEBUANO to "Default",
            AppLanguage.INDONESIAN to "Bawaan"
        ),
        StrKey.ACTIVE_BADGE to mapOf(
            AppLanguage.ENGLISH to "ACTIVE",
            AppLanguage.CEBUANO to "AKTIBO",
            AppLanguage.INDONESIAN to "AKTIF"
        ),
        StrKey.ERROR_LABEL to mapOf(
            AppLanguage.ENGLISH to "Error",
            AppLanguage.CEBUANO to "Sayop",
            AppLanguage.INDONESIAN to "Kesalahan"
        ),
        StrKey.COMPLETED_LABEL to mapOf(
            AppLanguage.ENGLISH to "Completed",
            AppLanguage.CEBUANO to "Human na",
            AppLanguage.INDONESIAN to "Selesai"
        ),
        StrKey.LOADING_DEFAULT to mapOf(
            AppLanguage.ENGLISH to "Loading...",
            AppLanguage.CEBUANO to "Nagkarga...",
            AppLanguage.INDONESIAN to "Memuat..."
        ),

        // --- Search, Sorting & Music Library ---
        StrKey.SEARCH_PLACEHOLDER to mapOf(
            AppLanguage.ENGLISH to "Search songs, artists, remix...",
            AppLanguage.CEBUANO to "Pangitaa ang mga kanta, artist, remix...",
            AppLanguage.INDONESIAN to "Cari lagu, artis, remix..."
        ),
        StrKey.SEARCH_TRACKS_COUNT_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Search %d tracks...",
            AppLanguage.CEBUANO to "Pangitaa sa %d ka kanta...",
            AppLanguage.INDONESIAN to "Cari dari %d lagu..."
        ),
        StrKey.SORT_BY to mapOf(
            AppLanguage.ENGLISH to "Sort By",
            AppLanguage.CEBUANO to "Han-ay sumala sa",
            AppLanguage.INDONESIAN to "Urutkan Berdasarkan"
        ),
        StrKey.SORT_TRACKS_TITLE to mapOf(
            AppLanguage.ENGLISH to "Sort Tracks",
            AppLanguage.CEBUANO to "Han-aya ang mga Kanta",
            AppLanguage.INDONESIAN to "Urutkan Lagu"
        ),
        StrKey.SORT_TITLE to mapOf(
            AppLanguage.ENGLISH to "Title",
            AppLanguage.CEBUANO to "Ulohan",
            AppLanguage.INDONESIAN to "Judul"
        ),
        StrKey.SORT_ARTIST to mapOf(
            AppLanguage.ENGLISH to "Artist",
            AppLanguage.CEBUANO to "Mag-aawit",
            AppLanguage.INDONESIAN to "Artis"
        ),
        StrKey.SORT_DATE to mapOf(
            AppLanguage.ENGLISH to "Date Added",
            AppLanguage.CEBUANO to "Petsa sa Pagdugang",
            AppLanguage.INDONESIAN to "Tanggal Ditambahkan"
        ),
        StrKey.SORT_DURATION to mapOf(
            AppLanguage.ENGLISH to "Duration",
            AppLanguage.CEBUANO to "Gitas-on",
            AppLanguage.INDONESIAN to "Durasi"
        ),
        StrKey.SORT_NAME_ASC to mapOf(
            AppLanguage.ENGLISH to "Name (A–Z)",
            AppLanguage.CEBUANO to "Ngalan (A–Z)",
            AppLanguage.INDONESIAN to "Nama (A–Z)"
        ),
        StrKey.SORT_NAME_DESC to mapOf(
            AppLanguage.ENGLISH to "Name (Z–A)",
            AppLanguage.CEBUANO to "Ngalan (Z–A)",
            AppLanguage.INDONESIAN to "Nama (Z–A)"
        ),
        StrKey.SORT_DATE_DESC to mapOf(
            AppLanguage.ENGLISH to "Date (Newest first)",
            AppLanguage.CEBUANO to "Petsa (Pinakabag-o una)",
            AppLanguage.INDONESIAN to "Tanggal (Terbaru dulu)"
        ),
        StrKey.SORT_DATE_ASC to mapOf(
            AppLanguage.ENGLISH to "Date (Oldest first)",
            AppLanguage.CEBUANO to "Petsa (Pinakadugay una)",
            AppLanguage.INDONESIAN to "Tanggal (Terlama dulu)"
        ),
        StrKey.SORT_SIZE_DESC to mapOf(
            AppLanguage.ENGLISH to "File Size (Largest first)",
            AppLanguage.CEBUANO to "Gidak-on sa File (Pinakadako una)",
            AppLanguage.INDONESIAN to "Ukuran File (Terbesar dulu)"
        ),
        StrKey.SORT_SIZE_ASC to mapOf(
            AppLanguage.ENGLISH to "File Size (Smallest first)",
            AppLanguage.CEBUANO to "Gidak-on sa File (Pinakagamay una)",
            AppLanguage.INDONESIAN to "Ukuran File (Terkecil dulu)"
        ),
        StrKey.AUDIO_ACCESS_REQUIRED to mapOf(
            AppLanguage.ENGLISH to "Audio Access Required",
            AppLanguage.CEBUANO to "Kinahanglan ang Pagtugot sa Audio",
            AppLanguage.INDONESIAN to "Akses Audio Diperlukan"
        ),
        StrKey.GRANT_AUDIO_ACCESS to mapOf(
            AppLanguage.ENGLISH to "Grant Audio Access",
            AppLanguage.CEBUANO to "Ihatag ang Pagtugot sa Audio",
            AppLanguage.INDONESIAN to "Berikan Akses Audio"
        ),
        StrKey.PERMISSION_DESC to mapOf(
            AppLanguage.ENGLISH to "JNMF requires storage permission to scan your Indo Remix and local audio files.",
            AppLanguage.CEBUANO to "Kinahanglan ang pagtugot sa storage aron ma-scan ang imong mga Indo Remix ug lokal nga mga kanta.",
            AppLanguage.INDONESIAN to "JNMF memerlukan izin penyimpanan untuk memindai lagu Indo Remix dan audio lokal Anda."
        ),
        StrKey.REFRESH_LIBRARY to mapOf(
            AppLanguage.ENGLISH to "Refresh Library",
            AppLanguage.CEBUANO to "I-refresh ang Library",
            AppLanguage.INDONESIAN to "Segarkan Koleksi"
        ),
        StrKey.RESCAN_STORAGE to mapOf(
            AppLanguage.ENGLISH to "Rescan Storage",
            AppLanguage.CEBUANO to "Susi-a Pag-usab ang Storage",
            AppLanguage.INDONESIAN to "Pindai Ulang Penyimpanan"
        ),
        StrKey.NO_SONGS_FOUND to mapOf(
            AppLanguage.ENGLISH to "No music files found on device.",
            AppLanguage.CEBUANO to "Walay nakit-an nga mga kanta sa aparato.",
            AppLanguage.INDONESIAN to "Tidak ada file musik yang ditemukan di perangkat."
        ),
        StrKey.NO_TRACKS_MATCH_QUERY_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "No tracks match '%s'",
            AppLanguage.CEBUANO to "Walay kanta nga mohaom sa '%s'",
            AppLanguage.INDONESIAN to "Tidak ada lagu yang cocok dengan '%s'"
        ),
        StrKey.TRACKS_COUNT_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "%d tracks",
            AppLanguage.CEBUANO to "%d ka mga kanta",
            AppLanguage.INDONESIAN to "%d lagu"
        ),
        StrKey.PLAY_NOW to mapOf(
            AppLanguage.ENGLISH to "Play Now",
            AppLanguage.CEBUANO to "Patugtoga Karon",
            AppLanguage.INDONESIAN to "Putar Sekarang"
        ),
        StrKey.ADD_TO_PLAYLIST to mapOf(
            AppLanguage.ENGLISH to "Add to Playlist",
            AppLanguage.CEBUANO to "Idugang sa Playlist",
            AppLanguage.INDONESIAN to "Tambah ke Daftar Putar"
        ),
        StrKey.REMOVE_FROM_PLAYLIST to mapOf(
            AppLanguage.ENGLISH to "Remove from Playlist",
            AppLanguage.CEBUANO to "Kuhaa gikan sa Playlist",
            AppLanguage.INDONESIAN to "Hapus dari Daftar Putar"
        ),
        StrKey.HIDE_FROM_LIBRARY to mapOf(
            AppLanguage.ENGLISH to "Hide from Library",
            AppLanguage.CEBUANO to "Itago gikan sa Library",
            AppLanguage.INDONESIAN to "Sembunyikan dari Koleksi"
        ),
        StrKey.DELETE_FILE to mapOf(
            AppLanguage.ENGLISH to "Delete File",
            AppLanguage.CEBUANO to "Papasa ang File",
            AppLanguage.INDONESIAN to "Hapus File"
        ),

        // --- Player, MiniPlayer & Now Playing ---
        StrKey.NOW_PLAYING to mapOf(
            AppLanguage.ENGLISH to "Now Playing",
            AppLanguage.CEBUANO to "Gipatugtog Karon",
            AppLanguage.INDONESIAN to "Sedang Diputar"
        ),
        StrKey.NOW_PLAYING_HEADER to mapOf(
            AppLanguage.ENGLISH to "NOW PLAYING • JNMF",
            AppLanguage.CEBUANO to "GIPATUGTOG KARON • JNMF",
            AppLanguage.INDONESIAN to "SEDANG DIPUTAR • JNMF"
        ),
        StrKey.CLOSE_NOW_PLAYING to mapOf(
            AppLanguage.ENGLISH to "Minimize player",
            AppLanguage.CEBUANO to "Ipaubos ang player",
            AppLanguage.INDONESIAN to "Kecilkan pemutar"
        ),
        StrKey.OPEN_EQUALIZER to mapOf(
            AppLanguage.ENGLISH to "Open Equalizer",
            AppLanguage.CEBUANO to "Ablihi ang Ekwalayser",
            AppLanguage.INDONESIAN to "Buka Ekualiser"
        ),
        StrKey.PLAY to mapOf(
            AppLanguage.ENGLISH to "Play",
            AppLanguage.CEBUANO to "Patugtoga",
            AppLanguage.INDONESIAN to "Putar"
        ),
        StrKey.PAUSE to mapOf(
            AppLanguage.ENGLISH to "Pause",
            AppLanguage.CEBUANO to "Ipahunong",
            AppLanguage.INDONESIAN to "Jeda"
        ),
        StrKey.PLAYING to mapOf(
            AppLanguage.ENGLISH to "Playing",
            AppLanguage.CEBUANO to "Nagpatugtog",
            AppLanguage.INDONESIAN to "Sedang memutar"
        ),
        StrKey.PAUSED to mapOf(
            AppLanguage.ENGLISH to "Paused",
            AppLanguage.CEBUANO to "Gihunong",
            AppLanguage.INDONESIAN to "Dijeda"
        ),
        StrKey.PREVIOUS to mapOf(
            AppLanguage.ENGLISH to "Previous",
            AppLanguage.CEBUANO to "Miagi",
            AppLanguage.INDONESIAN to "Sebelumnya"
        ),
        StrKey.NEXT to mapOf(
            AppLanguage.ENGLISH to "Next",
            AppLanguage.CEBUANO to "Sunod",
            AppLanguage.INDONESIAN to "Berikutnya"
        ),
        StrKey.QUEUE_TITLE to mapOf(
            AppLanguage.ENGLISH to "Playback Queue",
            AppLanguage.CEBUANO to "Han-ay sa Pagpatugtog",
            AppLanguage.INDONESIAN to "Antrean Pemutaran"
        ),
        StrKey.REVERSE_QUEUE to mapOf(
            AppLanguage.ENGLISH to "Reverse Queue Mode",
            AppLanguage.CEBUANO to "Baliktad nga Han-ay",
            AppLanguage.INDONESIAN to "Mode Balik Antrean"
        ),
        StrKey.REVERSE_QUEUE_ON to mapOf(
            AppLanguage.ENGLISH to "Reverse Order Active",
            AppLanguage.CEBUANO to "Aktibo ang Baliktad nga Han-ay",
            AppLanguage.INDONESIAN to "Urutan Balik Aktif"
        ),
        StrKey.REVERSE_QUEUE_OFF to mapOf(
            AppLanguage.ENGLISH to "Standard Queue Order",
            AppLanguage.CEBUANO to "Standard nga Han-ay",
            AppLanguage.INDONESIAN to "Urutan Standar"
        ),
        StrKey.SHUFFLE to mapOf(
            AppLanguage.ENGLISH to "Shuffle",
            AppLanguage.CEBUANO to "Isagol",
            AppLanguage.INDONESIAN to "Acak"
        ),
        StrKey.REPEAT to mapOf(
            AppLanguage.ENGLISH to "Repeat",
            AppLanguage.CEBUANO to "Sublia",
            AppLanguage.INDONESIAN to "Ulangi"
        ),
        StrKey.SPEED to mapOf(
            AppLanguage.ENGLISH to "Speed",
            AppLanguage.CEBUANO to "Kadasigon",
            AppLanguage.INDONESIAN to "Kecepatan"
        ),
        StrKey.ERROR_AUDIO_MISSING to mapOf(
            AppLanguage.ENGLISH to "Audio file missing or deleted from storage",
            AppLanguage.CEBUANO to "Nawala o napapas na ang audio file sa storage",
            AppLanguage.INDONESIAN to "File audio hilang atau telah dihapus dari penyimpanan"
        ),
        StrKey.ERROR_AUDIO_UNSUPPORTED to mapOf(
            AppLanguage.ENGLISH to "Unsupported audio format or corrupt file",
            AppLanguage.CEBUANO to "Dili suportado ang format sa audio o guba ang file",
            AppLanguage.INDONESIAN to "Format audio tidak didukung atau file rusak"
        ),
        StrKey.ERROR_CANNOT_PLAY_TRACK to mapOf(
            AppLanguage.ENGLISH to "Cannot play track: file inaccessible",
            AppLanguage.CEBUANO to "Dili mapatugtog ang kanta: dili ma-access ang file",
            AppLanguage.INDONESIAN to "Tidak dapat memutar lagu: file tidak dapat diakses"
        ),
        StrKey.ERROR_PLAYBACK_GENERIC to mapOf(
            AppLanguage.ENGLISH to "Playback error occurred",
            AppLanguage.CEBUANO to "Adunay sayop sa pagpatugtog",
            AppLanguage.INDONESIAN to "Terjadi kesalahan pemutaran"
        ),

        // --- Remix Categories & Badges ---
        StrKey.REMIX_STANDARD to mapOf(
            AppLanguage.ENGLISH to "Standard",
            AppLanguage.CEBUANO to "Standard",
            AppLanguage.INDONESIAN to "Standar"
        ),
        StrKey.REMIX_FULLBEAT to mapOf(
            AppLanguage.ENGLISH to "Fullbeat",
            AppLanguage.CEBUANO to "Fullbeat",
            AppLanguage.INDONESIAN to "Fullbeat"
        ),
        StrKey.REMIX_SLOW to mapOf(
            AppLanguage.ENGLISH to "Slow Remix",
            AppLanguage.CEBUANO to "Hinay nga Remix",
            AppLanguage.INDONESIAN to "Slow Remix"
        ),
        StrKey.REMIX_INDO to mapOf(
            AppLanguage.ENGLISH to "Indo Remix",
            AppLanguage.CEBUANO to "Indo Remix",
            AppLanguage.INDONESIAN to "Indo Remix"
        ),
        StrKey.REMIX_BREAKBEAT to mapOf(
            AppLanguage.ENGLISH to "Breakbeat",
            AppLanguage.CEBUANO to "Breakbeat",
            AppLanguage.INDONESIAN to "Breakbeat"
        ),
        StrKey.REMIX_SHORT_FB to mapOf(
            AppLanguage.ENGLISH to "FB",
            AppLanguage.CEBUANO to "FB",
            AppLanguage.INDONESIAN to "FB"
        ),
        StrKey.REMIX_SHORT_SLOW to mapOf(
            AppLanguage.ENGLISH to "SLOW",
            AppLanguage.CEBUANO to "HINAY",
            AppLanguage.INDONESIAN to "PELAN"
        ),
        StrKey.REMIX_SHORT_REMIX to mapOf(
            AppLanguage.ENGLISH to "REMIX",
            AppLanguage.CEBUANO to "REMIX",
            AppLanguage.INDONESIAN to "REMIX"
        ),
        StrKey.REMIX_SHORT_BB to mapOf(
            AppLanguage.ENGLISH to "BB",
            AppLanguage.CEBUANO to "BB",
            AppLanguage.INDONESIAN to "BB"
        ),

        // --- Equalizer & Sound Engine ---
        StrKey.EQ_TITLE to mapOf(
            AppLanguage.ENGLISH to "Equalizer & Audio DSP",
            AppLanguage.CEBUANO to "Ekwalayser ug Audio DSP",
            AppLanguage.INDONESIAN to "Ekualiser & Audio DSP"
        ),
        StrKey.EQ_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Indo Remix Sound Engine",
            AppLanguage.CEBUANO to "Makina sa Tingog sa Indo Remix",
            AppLanguage.INDONESIAN to "Mesin Suara Indo Remix"
        ),
        StrKey.EQ_MASTER to mapOf(
            AppLanguage.ENGLISH to "Master Sound Processing",
            AppLanguage.CEBUANO to "Pangunang Pagproseso sa Tingog",
            AppLanguage.INDONESIAN to "Pemrosesan Suara Master"
        ),
        StrKey.EQ_SESSION_STATUS to mapOf(
            AppLanguage.ENGLISH to "Audio Session Status",
            AppLanguage.CEBUANO to "Kahimtang sa Sesyon sa Audio",
            AppLanguage.INDONESIAN to "Status Sesi Audio"
        ),
        StrKey.EQ_ACTIVE to mapOf(
            AppLanguage.ENGLISH to "Attached to JNMF Audio Track",
            AppLanguage.CEBUANO to "Konektado sa JNMF Audio Track",
            AppLanguage.INDONESIAN to "Terhubung ke Trek Audio JNMF"
        ),
        StrKey.EQ_PIPELINE_ATTACHED_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Audio Pipeline Attached: Session #%d",
            AppLanguage.CEBUANO to "Konektado ang Audio Pipeline: Sesyon #%d",
            AppLanguage.INDONESIAN to "Jalur Audio Terhubung: Sesi #%d"
        ),
        StrKey.EQ_IDLE to mapOf(
            AppLanguage.ENGLISH to "Player Session Idle: Waiting for playback",
            AppLanguage.CEBUANO to "Naghulat sa sesyon sa pagpatugtog",
            AppLanguage.INDONESIAN to "Sesi Pemutar Siaga: Menunggu pemutaran"
        ),
        StrKey.EQ_HARDWARE_ACTIVE to mapOf(
            AppLanguage.ENGLISH to "Processing directly on JNMF AudioTrack (Redmi 10C compatible)",
            AppLanguage.CEBUANO to "Direktang nagproseso sa JNMF AudioTrack (angay sa Redmi 10C)",
            AppLanguage.INDONESIAN to "Memproses langsung di AudioTrack JNMF (kompatibel Redmi 10C)"
        ),
        StrKey.EQ_SOFTWARE_ACTIVE to mapOf(
            AppLanguage.ENGLISH to "Software DSP active (hardware limiter engaged)",
            AppLanguage.CEBUANO to "Aktibo ang Software DSP (gigamit ang hardware limiter)",
            AppLanguage.INDONESIAN to "DSP perangkat lunak aktif (pembatas perangkat keras aktif)"
        ),
        StrKey.SOUND_PRESETS to mapOf(
            AppLanguage.ENGLISH to "Sound Presets",
            AppLanguage.CEBUANO to "Mga Preset sa Tingog",
            AppLanguage.INDONESIAN to "Preset Suara"
        ),
        StrKey.EQ_FREQUENCY_BANDS_HEADER to mapOf(
            AppLanguage.ENGLISH to "FREQUENCY BANDS (dB)",
            AppLanguage.CEBUANO to "MGA FREQUENCY BAND (dB)",
            AppLanguage.INDONESIAN to "PITA FREKUENSI (dB)"
        ),
        StrKey.EQ_ACOUSTIC_ENHANCEMENT_HEADER to mapOf(
            AppLanguage.ENGLISH to "REMIX ACOUSTIC ENHANCEMENT",
            AppLanguage.CEBUANO to "PAGPALAMBO SA TINGOG SA REMIX",
            AppLanguage.INDONESIAN to "PENINGKATAN AKUSTIK REMIX"
        ),
        StrKey.BASS_HOREG to mapOf(
            AppLanguage.ENGLISH to "Bass Horeg / Sub-Bass Boost",
            AppLanguage.CEBUANO to "Bass Horeg / Kusog nga Sub-Bass",
            AppLanguage.INDONESIAN to "Bass Horeg / Penguat Sub-Bass"
        ),
        StrKey.BASS_HOREG_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Optimized for Indo Remix low-end punch",
            AppLanguage.CEBUANO to "Gi-optimize alang sa lawom nga bass sa Indo Remix",
            AppLanguage.INDONESIAN to "Dioptimalkan untuk dentuman bass rendah Indo Remix"
        ),
        StrKey.STEREO_WIDTH to mapOf(
            AppLanguage.ENGLISH to "Stereo Width / Spatial 3D",
            AppLanguage.CEBUANO to "Gilapdon sa Stereo / 3D Spasial",
            AppLanguage.INDONESIAN to "Lebar Stereo / Spasial 3D"
        ),
        StrKey.STEREO_WIDTH_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Broadens soundstage for headphones & TWS earbuds",
            AppLanguage.CEBUANO to "Nagpalapad sa tingog alang sa headphone ug TWS earbuds",
            AppLanguage.INDONESIAN to "Memperluas ruang suara untuk headphone & TWS"
        ),
        StrKey.TREBLE_CLARITY to mapOf(
            AppLanguage.ENGLISH to "Vocal & Treble Clarity",
            AppLanguage.CEBUANO to "Katin-aw sa Tingog ug Treble",
            AppLanguage.INDONESIAN to "Kejernihan Vokal & Treble"
        ),
        StrKey.TREBLE_CLARITY_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Enhances vocals and synth leads without clipping",
            AppLanguage.CEBUANO to "Nagpatin-aw sa boses ug synth nga walay garas",
            AppLanguage.INDONESIAN to "Memperjelas vokal dan nada tinggi tanpa pecah"
        ),
        StrKey.PRESET_INDO_FULLBEAT to mapOf(
            AppLanguage.ENGLISH to "Indo Fullbeat",
            AppLanguage.CEBUANO to "Indo Fullbeat",
            AppLanguage.INDONESIAN to "Indo Fullbeat"
        ),
        StrKey.PRESET_BASS_HOREG to mapOf(
            AppLanguage.ENGLISH to "Bass Horeg",
            AppLanguage.CEBUANO to "Bass Horeg",
            AppLanguage.INDONESIAN to "Bass Horeg"
        ),
        StrKey.PRESET_SLOW_REMIX to mapOf(
            AppLanguage.ENGLISH to "Slow Remix",
            AppLanguage.CEBUANO to "Hinay Remix",
            AppLanguage.INDONESIAN to "Slow Remix"
        ),
        StrKey.PRESET_CLUB_REMIX to mapOf(
            AppLanguage.ENGLISH to "Club Remix",
            AppLanguage.CEBUANO to "Club Remix",
            AppLanguage.INDONESIAN to "Remix Klub"
        ),
        StrKey.PRESET_VOCAL_CLARITY to mapOf(
            AppLanguage.ENGLISH to "Vocal Clarity",
            AppLanguage.CEBUANO to "Tin-aw Boses",
            AppLanguage.INDONESIAN to "Vokal Jernih"
        ),
        StrKey.PRESET_FLAT to mapOf(
            AppLanguage.ENGLISH to "Flat",
            AppLanguage.CEBUANO to "Patag (Flat)",
            AppLanguage.INDONESIAN to "Datar (Flat)"
        ),
        StrKey.PRESET_CUSTOM to mapOf(
            AppLanguage.ENGLISH to "Custom",
            AppLanguage.CEBUANO to "Kaugalingon",
            AppLanguage.INDONESIAN to "Kustom"
        ),
        StrKey.RESET_DEFAULT to mapOf(
            AppLanguage.ENGLISH to "Reset to Default",
            AppLanguage.CEBUANO to "I-reset sa Default",
            AppLanguage.INDONESIAN to "Reset ke Awal"
        ),

        // --- Playlists & Global Picker ---
        StrKey.PLAYLISTS_TITLE to mapOf(
            AppLanguage.ENGLISH to "My Playlists",
            AppLanguage.CEBUANO to "Akong mga Playlist",
            AppLanguage.INDONESIAN to "Daftar Putar Saya"
        ),
        StrKey.CUSTOM_PLAYLISTS_HEADER to mapOf(
            AppLanguage.ENGLISH to "CUSTOM PLAYLISTS",
            AppLanguage.CEBUANO to "KAUGALINGONG MGA PLAYLIST",
            AppLanguage.INDONESIAN to "DAFTAR PUTAR KUSTOM"
        ),
        StrKey.FAVORITES_TITLE to mapOf(
            AppLanguage.ENGLISH to "Favorite Songs",
            AppLanguage.CEBUANO to "Mga Paboritong Kanta",
            AppLanguage.INDONESIAN to "Lagu Favorit"
        ),
        StrKey.SAVED_FAVORITES_SUBTITLE_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "%d saved tracks",
            AppLanguage.CEBUANO to "%d ka na-save nga mga kanta",
            AppLanguage.INDONESIAN to "%d lagu tersimpan"
        ),
        StrKey.EMPTY_FAVORITES to mapOf(
            AppLanguage.ENGLISH to "No favorite tracks yet.\nTap the heart icon on any song to save it here.",
            AppLanguage.CEBUANO to "Wala pay paboritong mga kanta.\nI-tap ang kasingkasing sa bisan unsang kanta aron ma-save dinhi.",
            AppLanguage.INDONESIAN to "Belum ada lagu favorit.\nKetuk ikon hati pada lagu mana pun untuk menyimpannya di sini."
        ),
        StrKey.SAVED_TRACKS_COUNT_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "%d saved tracks",
            AppLanguage.CEBUANO to "%d ka na-save nga kanta",
            AppLanguage.INDONESIAN to "%d lagu tersimpan"
        ),
        StrKey.NEW_PLAYLIST to mapOf(
            AppLanguage.ENGLISH to "New Playlist",
            AppLanguage.CEBUANO to "Bag-ong Playlist",
            AppLanguage.INDONESIAN to "Daftar Putar Baru"
        ),
        StrKey.CREATE_NEW_PLAYLIST to mapOf(
            AppLanguage.ENGLISH to "Create New Playlist",
            AppLanguage.CEBUANO to "Paghimo og Bag-ong Playlist",
            AppLanguage.INDONESIAN to "Buat Daftar Putar Baru"
        ),
        StrKey.NEW_PLAYLIST_NAME_LABEL to mapOf(
            AppLanguage.ENGLISH to "New Playlist Name",
            AppLanguage.CEBUANO to "Ngalan sa Bag-ong Playlist",
            AppLanguage.INDONESIAN to "Nama Daftar Putar Baru"
        ),
        StrKey.ENTER_PLAYLIST_NAME to mapOf(
            AppLanguage.ENGLISH to "Playlist Name",
            AppLanguage.CEBUANO to "Ngalan sa Playlist",
            AppLanguage.INDONESIAN to "Nama Daftar Putar"
        ),
        StrKey.CREATE_AND_ADD_TRACK to mapOf(
            AppLanguage.ENGLISH to "Create & Add Track",
            AppLanguage.CEBUANO to "Paghimo ug Idugang ang Kanta",
            AppLanguage.INDONESIAN to "Buat & Tambahkan Lagu"
        ),
        StrKey.ADD_SONG_TO_PLAYLIST_TITLE_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Add '%s' to Playlist",
            AppLanguage.CEBUANO to "Idugang ang '%s' sa Playlist",
            AppLanguage.INDONESIAN to "Tambahkan '%s' ke Daftar Putar"
        ),
        StrKey.NO_PLAYLISTS_CREATE_PROMPT to mapOf(
            AppLanguage.ENGLISH to "No playlists found. Create one now:",
            AppLanguage.CEBUANO to "Walay nakit-an nga playlist. Paghimo karon:",
            AppLanguage.INDONESIAN to "Belum ada daftar putar. Buat sekarang:"
        ),
        StrKey.EMPTY_PLAYLISTS to mapOf(
            AppLanguage.ENGLISH to "No playlists yet. Tap + to create one!",
            AppLanguage.CEBUANO to "Wala pay mga playlist. I-tap ang + aron maghimo!",
            AppLanguage.INDONESIAN to "Belum ada daftar putar. Ketuk + untuk membuat!"
        ),
        StrKey.PLAYLIST_EMPTY to mapOf(
            AppLanguage.ENGLISH to "Playlist is currently empty.",
            AppLanguage.CEBUANO to "Walay sulod kining playlist karon.",
            AppLanguage.INDONESIAN to "Daftar putar ini masih kosong."
        ),
        StrKey.ADD_TRACKS to mapOf(
            AppLanguage.ENGLISH to "Add tracks",
            AppLanguage.CEBUANO to "Pagdugang og mga kanta",
            AppLanguage.INDONESIAN to "Tambah lagu"
        ),
        StrKey.ADD_TRACKS_NOW to mapOf(
            AppLanguage.ENGLISH to "Add Tracks Now",
            AppLanguage.CEBUANO to "Pagdugang og Kanta Karon",
            AppLanguage.INDONESIAN to "Tambahkan Lagu Sekarang"
        ),
        StrKey.PLAY_ALL to mapOf(
            AppLanguage.ENGLISH to "Play all",
            AppLanguage.CEBUANO to "Patugtoga tanan",
            AppLanguage.INDONESIAN to "Putar semua"
        ),
        StrKey.MOVE_UP to mapOf(
            AppLanguage.ENGLISH to "Move up",
            AppLanguage.CEBUANO to "Ibalhin sa taas",
            AppLanguage.INDONESIAN to "Pindah ke atas"
        ),
        StrKey.MOVE_DOWN to mapOf(
            AppLanguage.ENGLISH to "Move down",
            AppLanguage.CEBUANO to "Ibalhin sa ubos",
            AppLanguage.INDONESIAN to "Pindah ke bawah"
        ),
        StrKey.ADD_TRACKS_TO_PLAYLIST_TITLE_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Add Tracks to '%s'",
            AppLanguage.CEBUANO to "Pagdugang og Kanta sa '%s'",
            AppLanguage.INDONESIAN to "Tambah Lagu ke '%s'"
        ),
        StrKey.ALL_SONGS_IN_PLAYLIST to mapOf(
            AppLanguage.ENGLISH to "All accessible songs are already in this playlist.",
            AppLanguage.CEBUANO to "Ang tanang magamit nga kanta naa na niini nga playlist.",
            AppLanguage.INDONESIAN to "Semua lagu yang tersedia sudah ada di daftar putar ini."
        ),
        StrKey.RENAME_PLAYLIST_TITLE to mapOf(
            AppLanguage.ENGLISH to "Rename Playlist",
            AppLanguage.CEBUANO to "Usba ang Ngalan sa Playlist",
            AppLanguage.INDONESIAN to "Ganti Nama Daftar Putar"
        ),
        StrKey.DELETE_PLAYLIST_TITLE to mapOf(
            AppLanguage.ENGLISH to "Delete Playlist",
            AppLanguage.CEBUANO to "Papasa ang Playlist",
            AppLanguage.INDONESIAN to "Hapus Daftar Putar"
        ),
        StrKey.DELETE_PLAYLIST_CONFIRM_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Are you sure you want to delete '%s'? Audio files on your storage will NOT be deleted.",
            AppLanguage.CEBUANO to "Sigurado ka ba nga papason ang '%s'? Ang mga audio file sa imong storage DILI mapapas.",
            AppLanguage.INDONESIAN to "Yakin ingin menghapus '%s'? File audio di penyimpanan Anda TIDAK akan terhapus."
        ),
        StrKey.MISSING_TRACK_TITLE_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Track #%d (File unavailable or moved)",
            AppLanguage.CEBUANO to "Kanta #%d (Dili magamit o nabalhin ang file)",
            AppLanguage.INDONESIAN to "Lagu #%d (File tidak tersedia atau dipindahkan)"
        ),
        StrKey.UNKNOWN_LOCATION to mapOf(
            AppLanguage.ENGLISH to "Unknown Location",
            AppLanguage.CEBUANO to "Wala Mailhing Lokasyon",
            AppLanguage.INDONESIAN to "Lokasi Tidak Diketahui"
        ),
        StrKey.UNAVAILABLE to mapOf(
            AppLanguage.ENGLISH to "Unavailable",
            AppLanguage.CEBUANO to "Dili Magamit",
            AppLanguage.INDONESIAN to "Tidak Tersedia"
        ),

        // --- Duplicate Detection ---
        StrKey.DUPLICATES_TITLE to mapOf(
            AppLanguage.ENGLISH to "Duplicate & Similar Songs",
            AppLanguage.CEBUANO to "Mga Doble ug Parehas nga Kanta",
            AppLanguage.INDONESIAN to "Lagu Duplikat & Serupa"
        ),
        StrKey.DUPLICATES_THRESHOLD_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Threshold ~80% similarity",
            AppLanguage.CEBUANO to "Sukdanan ~80% nga pagkaparehas",
            AppLanguage.INDONESIAN to "Batas kemiripan ~80%"
        ),
        StrKey.ACOUSTIC_SIMILARITY_ENGINE to mapOf(
            AppLanguage.ENGLISH to "Acoustic Similarity Engine",
            AppLanguage.CEBUANO to "Makina sa Pagkaparehas sa Tingog",
            AppLanguage.INDONESIAN to "Mesin Kemiripan Akustik"
        ),
        StrKey.SCAN_SIMILAR_AUDIO to mapOf(
            AppLanguage.ENGLISH to "Scan Similar Audio (~80%)",
            AppLanguage.CEBUANO to "I-scan ang Parehas nga Audio (~80%)",
            AppLanguage.INDONESIAN to "Pindai Audio Serupa (~80%)"
        ),
        StrKey.RESCAN_LIBRARY_COUNT_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Re-Scan Library (%d tracks)",
            AppLanguage.CEBUANO to "Susi-a Pag-usab ang Library (%d ka kanta)",
            AppLanguage.INDONESIAN to "Pindai Ulang Koleksi (%d lagu)"
        ),
        StrKey.DUPLICATES_DESC to mapOf(
            AppLanguage.ENGLISH to "Detect duplicate tracks and distinguish different remix arrangements without deleting automatically.",
            AppLanguage.CEBUANO to "Pangitaa ang mga parehas nga kanta ug ilha ang mga lain-laing remix nga dili awtomatikong papason.",
            AppLanguage.INDONESIAN to "Deteksi lagu duplikat dan bedakan aransemen remix tanpa menghapus secara otomatis."
        ),
        StrKey.SIMILARITY to mapOf(
            AppLanguage.ENGLISH to "Similarity",
            AppLanguage.CEBUANO to "Pagkaparehas",
            AppLanguage.INDONESIAN to "Kemiripan"
        ),
        StrKey.SIMILARITY_SCORE to mapOf(
            AppLanguage.ENGLISH to "Similarity Score",
            AppLanguage.CEBUANO to "Grado sa Pagkaparehas",
            AppLanguage.INDONESIAN to "Skor Kemiripan"
        ),
        StrKey.SIMILARITY_MATCH_PERCENT_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "%d%% Match",
            AppLanguage.CEBUANO to "%d%% nga Pagkaparehas",
            AppLanguage.INDONESIAN to "%d%% Cocok"
        ),
        StrKey.VERSION_A to mapOf(
            AppLanguage.ENGLISH to "Version A",
            AppLanguage.CEBUANO to "Bersyon A",
            AppLanguage.INDONESIAN to "Versi A"
        ),
        StrKey.VERSION_B to mapOf(
            AppLanguage.ENGLISH to "Version B",
            AppLanguage.CEBUANO to "Bersyon B",
            AppLanguage.INDONESIAN to "Versi B"
        ),
        StrKey.PREVIEW_TRACK to mapOf(
            AppLanguage.ENGLISH to "Preview track",
            AppLanguage.CEBUANO to "Paminawa ang kanta",
            AppLanguage.INDONESIAN to "Pratinjau lagu"
        ),
        StrKey.STARTING_ANALYSIS to mapOf(
            AppLanguage.ENGLISH to "Starting analysis...",
            AppLanguage.CEBUANO to "Nagsugod sa pag-analisar...",
            AppLanguage.INDONESIAN to "Memulai analisis..."
        ),
        StrKey.SCAN_COMPLETED_SHORT to mapOf(
            AppLanguage.ENGLISH to "Scan completed",
            AppLanguage.CEBUANO to "Human na ang pag-scan",
            AppLanguage.INDONESIAN to "Pemindaian selesai"
        ),
        StrKey.DUPLICATE_SCAN_COMPLETED_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Scan completed: Checked %d tracks at ~80%% similarity threshold.",
            AppLanguage.CEBUANO to "Human na ang pag-scan: Nasusi ang %d ka kanta sa ~80%% nga sukdanan.",
            AppLanguage.INDONESIAN to "Pemindaian selesai: Memeriksa %d lagu pada batas kemiripan ~80%%."
        ),
        StrKey.NO_DUPLICATES_FOUND to mapOf(
            AppLanguage.ENGLISH to "No duplicate songs found above 80% threshold",
            AppLanguage.CEBUANO to "Walay nakit-an nga doble nga kanta nga lapas sa 80%",
            AppLanguage.INDONESIAN to "Tidak ditemukan lagu duplikat di atas batas 80%"
        ),
        StrKey.TAP_SCAN_TO_ANALYZE to mapOf(
            AppLanguage.ENGLISH to "Tap scan to analyze library",
            AppLanguage.CEBUANO to "I-tap ang scan aron masusi ang library",
            AppLanguage.INDONESIAN to "Ketuk pindai untuk menganalisis koleksi"
        ),
        StrKey.CONFIRM_DELETE_TITLE to mapOf(
            AppLanguage.ENGLISH to "Confirm File Removal",
            AppLanguage.CEBUANO to "Kumpirmaha ang Pagkuha sa File",
            AppLanguage.INDONESIAN to "Konfirmasi Penghapusan File"
        ),
        StrKey.CONFIRM_DELETE_MSG to mapOf(
            AppLanguage.ENGLISH to "Are you sure you want to delete this file from storage?",
            AppLanguage.CEBUANO to "Sigurado ka ba nga gusto nimong papason kini nga file gikan sa storage?",
            AppLanguage.INDONESIAN to "Apakah Anda yakin ingin menghapus file ini dari penyimpanan?"
        ),
        StrKey.CONFIRM_DELETE_SONG_MSG_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Are you sure you want to remove '%1\$s' (%2\$s)?\n\nThis will remove the track from your library view.",
            AppLanguage.CEBUANO to "Sigurado ka ba nga kuhaon ang '%1\$s' (%2\$s)?\n\nKini magtangtang sa kanta gikan sa imong library.",
            AppLanguage.INDONESIAN to "Yakin ingin menghapus '%1\$s' (%2\$s)?\n\nIni akan menghapus lagu dari tampilan koleksi Anda."
        ),
        StrKey.DELETE_TRACK to mapOf(
            AppLanguage.ENGLISH to "Delete Track",
            AppLanguage.CEBUANO to "Papasa ang Kanta",
            AppLanguage.INDONESIAN to "Hapus Lagu"
        ),
        StrKey.REASON_EXACT_DURATION to mapOf(
            AppLanguage.ENGLISH to "Exact playback duration match (±1.5s)",
            AppLanguage.CEBUANO to "Tukma kaayo ang gitas-on sa kanta (±1.5s)",
            AppLanguage.INDONESIAN to "Durasi putar sangat cocok (±1,5 dtk)"
        ),
        StrKey.REASON_CLOSE_DURATION to mapOf(
            AppLanguage.ENGLISH to "Close playback duration (±4s)",
            AppLanguage.CEBUANO to "Hapit magkaparehas ang gitas-on (±4s)",
            AppLanguage.INDONESIAN to "Durasi putar berdekatan (±4 dtk)"
        ),
        StrKey.REASON_IDENTICAL_TITLE to mapOf(
            AppLanguage.ENGLISH to "Near identical title tokens",
            AppLanguage.CEBUANO to "Halos parehas ang mga pulong sa ulohan",
            AppLanguage.INDONESIAN to "Kata judul hampir identik"
        ),
        StrKey.REASON_IDENTICAL_ARTIST_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Identical artist credit: %s",
            AppLanguage.CEBUANO to "Parehas nga mag-aawit: %s",
            AppLanguage.INDONESIAN to "Artis yang sama: %s"
        ),
        StrKey.REASON_DIFFERENT_REMIX_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Distinguished as different remix arrangements (%1\$s vs %2\$s)",
            AppLanguage.CEBUANO to "Giila isip managlahing remix (%1\$s batok %2\$s)",
            AppLanguage.INDONESIAN to "Dibedakan sebagai aransemen remix berbeda (%1\$s vs %2\$s)"
        ),
        StrKey.REASON_MATCHING_REMIX_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Matching remix classification: %s",
            AppLanguage.CEBUANO to "Parehas nga klase sa remix: %s",
            AppLanguage.INDONESIAN to "Klasifikasi remix yang sama: %s"
        ),

        // --- Classifier ---
        StrKey.CLASSIFIER_TITLE to mapOf(
            AppLanguage.ENGLISH to "Fullbeat & Slow Classifier",
            AppLanguage.CEBUANO to "Pag-ila sa Fullbeat ug Hinay nga Remix",
            AppLanguage.INDONESIAN to "Klasifikasi Fullbeat & Slow"
        ),
        StrKey.CLASSIFIER_DESC to mapOf(
            AppLanguage.ENGLISH to "Automatic tempo and beat sequence detection for Fullbeat and Slow tracks.",
            AppLanguage.CEBUANO to "Awtomatikong pag-ila sa tempo ug ritmo alang sa Fullbeat ug Hinay nga mga kanta.",
            AppLanguage.INDONESIAN to "Deteksi otomatis tempo dan ketukan untuk lagu Fullbeat dan Slow Remix."
        ),
        StrKey.FILTER_ALL_COUNT_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "All (%d)",
            AppLanguage.CEBUANO to "Tanan (%d)",
            AppLanguage.INDONESIAN to "Semua (%d)"
        ),
        StrKey.EDIT_CATEGORY to mapOf(
            AppLanguage.ENGLISH to "Edit Category",
            AppLanguage.CEBUANO to "Usba ang Klase",
            AppLanguage.INDONESIAN to "Ubah Kategori"
        ),
        StrKey.MANUAL_CATEGORY_CORRECTION_TITLE to mapOf(
            AppLanguage.ENGLISH to "Manual Category Correction",
            AppLanguage.CEBUANO to "Manwal nga Pag-usab sa Klase",
            AppLanguage.INDONESIAN to "Koreksi Kategori Manual"
        ),

        // --- Videos ---
        StrKey.VIDEOS_TITLE to mapOf(
            AppLanguage.ENGLISH to "Local Videos",
            AppLanguage.CEBUANO to "Mga Video sa Aparato",
            AppLanguage.INDONESIAN to "Video Lokal"
        ),
        StrKey.VIDEOS_COUNT_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "%d videos",
            AppLanguage.CEBUANO to "%d ka mga video",
            AppLanguage.INDONESIAN to "%d video"
        ),
        StrKey.VIDEOS_FOUND_ON_DEVICE_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "%d videos found on device",
            AppLanguage.CEBUANO to "%d ka mga video ang nakit-an sa aparato",
            AppLanguage.INDONESIAN to "%d video ditemukan di perangkat"
        ),
        StrKey.NO_VIDEOS_FOUND to mapOf(
            AppLanguage.ENGLISH to "No videos found on device.",
            AppLanguage.CEBUANO to "Walay nakit-an nga video sa aparato.",
            AppLanguage.INDONESIAN to "Tidak ada video ditemukan di perangkat."
        ),
        StrKey.REFRESH_VIDEOS to mapOf(
            AppLanguage.ENGLISH to "Refresh Videos",
            AppLanguage.CEBUANO to "I-refresh ang mga Video",
            AppLanguage.INDONESIAN to "Segarkan Video"
        ),
        StrKey.FULLSCREEN to mapOf(
            AppLanguage.ENGLISH to "Fullscreen",
            AppLanguage.CEBUANO to "Tibuok Screen",
            AppLanguage.INDONESIAN to "Layar Penuh"
        ),
        StrKey.EXIT_FULLSCREEN to mapOf(
            AppLanguage.ENGLISH to "Exit Fullscreen",
            AppLanguage.CEBUANO to "Gawas sa Tibuok Screen",
            AppLanguage.INDONESIAN to "Keluar Layar Penuh"
        ),
        StrKey.CLOSE_PLAYER to mapOf(
            AppLanguage.ENGLISH to "Close Video Player",
            AppLanguage.CEBUANO to "Isira ang Video Player",
            AppLanguage.INDONESIAN to "Tutup Pemutar Video"
        ),
        StrKey.MORE_VIDEOS to mapOf(
            AppLanguage.ENGLISH to "More Videos",
            AppLanguage.CEBUANO to "Dugang nga mga Video",
            AppLanguage.INDONESIAN to "Video Lainnya"
        ),
        StrKey.SEEK_BACKWARD_10S to mapOf(
            AppLanguage.ENGLISH to "Rewind 10 seconds",
            AppLanguage.CEBUANO to "Ibalik og 10 segundos",
            AppLanguage.INDONESIAN to "Mundur 10 detik"
        ),
        StrKey.SEEK_FORWARD_10S to mapOf(
            AppLanguage.ENGLISH to "Forward 10 seconds",
            AppLanguage.CEBUANO to "I-abante og 10 segundos",
            AppLanguage.INDONESIAN to "Maju 10 detik"
        ),
        StrKey.VIDEO_PLAYBACK_ERROR_TITLE to mapOf(
            AppLanguage.ENGLISH to "Video Playback Error",
            AppLanguage.CEBUANO to "Sayop sa Pagpatugtog sa Video",
            AppLanguage.INDONESIAN to "Kesalahan Pemutaran Video"
        ),
        StrKey.VIDEO_PLAYBACK_ERROR_DEFAULT to mapOf(
            AppLanguage.ENGLISH to "Unable to play video on this device.",
            AppLanguage.CEBUANO to "Dili mapatugtog ang video niini nga aparato.",
            AppLanguage.INDONESIAN to "Tidak dapat memutar video di perangkat ini."
        ),
        StrKey.VIDEO_FILE_MISSING to mapOf(
            AppLanguage.ENGLISH to "Video file missing or deleted from storage.",
            AppLanguage.CEBUANO to "Nawala o napapas na ang video file sa storage.",
            AppLanguage.INDONESIAN to "File video hilang atau telah dihapus dari penyimpanan."
        ),
        StrKey.VIDEO_UNSUPPORTED_CODEC to mapOf(
            AppLanguage.ENGLISH to "Unsupported video codec or corrupted file on this device.",
            AppLanguage.CEBUANO to "Dili suportado ang video codec o guba ang file niini nga aparato.",
            AppLanguage.INDONESIAN to "Kodek video tidak didukung atau file rusak di perangkat ini."
        ),
        StrKey.VIDEO_LOAD_FAILED to mapOf(
            AppLanguage.ENGLISH to "Failed to load video: file inaccessible.",
            AppLanguage.CEBUANO to "Napakyas sa pagkarga sa video: dili ma-access ang file.",
            AppLanguage.INDONESIAN to "Gagal memuat video: file tidak dapat diakses."
        ),
        StrKey.VIDEO_BG_PLAYBACK_TITLE to mapOf(
            AppLanguage.ENGLISH to "Background Playback",
            AppLanguage.CEBUANO to "Pagpatugtog sa Background",
            AppLanguage.INDONESIAN to "Pemutaran Latar Belakang"
        ),
        StrKey.VIDEO_MODE_NORMAL to mapOf(
            AppLanguage.ENGLISH to "Normal Video",
            AppLanguage.CEBUANO to "Normal nga Video",
            AppLanguage.INDONESIAN to "Video Normal"
        ),
        StrKey.VIDEO_MODE_AUDIO_ONLY to mapOf(
            AppLanguage.ENGLISH to "Audio Only",
            AppLanguage.CEBUANO to "Audio Lang",
            AppLanguage.INDONESIAN to "Hanya Audio"
        ),
        StrKey.VIDEO_MODE_NORMAL_DESC to mapOf(
            AppLanguage.ENGLISH to "Plays video in foreground • Continues audio in background without video frames",
            AppLanguage.CEBUANO to "Video sa atubangan • Padayon ang audio sa background nga walay video frames",
            AppLanguage.INDONESIAN to "Putar video di layar • Lanjut audio di latar belakang tanpa bingkai video"
        ),
        StrKey.VIDEO_MODE_AUDIO_ONLY_DESC to mapOf(
            AppLanguage.ENGLISH to "Plays audio track only • Video frame rendering disabled",
            AppLanguage.CEBUANO to "Audio track lang ang ipatugtog • Gipalong ang video frames",
            AppLanguage.INDONESIAN to "Hanya memutar trek audio • Perenderan bingkai video mati"
        ),
        StrKey.VIDEO_AUDIO_ONLY_STAGE_TITLE to mapOf(
            AppLanguage.ENGLISH to "Audio Only Mode",
            AppLanguage.CEBUANO to "Audio Lang nga Mode",
            AppLanguage.INDONESIAN to "Mode Hanya Audio"
        ),
        StrKey.VIDEO_AUDIO_ONLY_STAGE_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Playing video audio track without rendering video frames.",
            AppLanguage.CEBUANO to "Nagpatugtog sa audio sa video nga walay gipakitang video frames.",
            AppLanguage.INDONESIAN to "Memutar trek audio video tanpa merender bingkai video."
        ),
        StrKey.VIDEO_SWITCH_TO_NORMAL to mapOf(
            AppLanguage.ENGLISH to "Switch to Normal Video",
            AppLanguage.CEBUANO to "Ibalhin sa Normal nga Video",
            AppLanguage.INDONESIAN to "Ke Video Normal"
        ),
        StrKey.VIDEO_STOP to mapOf(
            AppLanguage.ENGLISH to "Stop",
            AppLanguage.CEBUANO to "Hunong",
            AppLanguage.INDONESIAN to "Berhenti"
        ),
        StrKey.VIDEO_MINIMIZE_TO_BG to mapOf(
            AppLanguage.ENGLISH to "Back to List",
            AppLanguage.CEBUANO to "Balik sa Lista",
            AppLanguage.INDONESIAN to "Ke Daftar"
        ),
        StrKey.VIDEO_NOTIF_BADGE to mapOf(
            AppLanguage.ENGLISH to "JNMF Video",
            AppLanguage.CEBUANO to "JNMF Video",
            AppLanguage.INDONESIAN to "Video JNMF"
        ),
        StrKey.VIDEO_NOTIF_MODE_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Mode: %s",
            AppLanguage.CEBUANO to "Mode: %s",
            AppLanguage.INDONESIAN to "Mode: %s"
        ),
        StrKey.VIDEO_RETURN_TO_PLAYER to mapOf(
            AppLanguage.ENGLISH to "Return to Video Player",
            AppLanguage.CEBUANO to "Balik sa Video Player",
            AppLanguage.INDONESIAN to "Kembali ke Pemutar Video"
        ),

        // --- Settings, Themes, Languages & Credits ---
        StrKey.SETTINGS_TITLE to mapOf(
            AppLanguage.ENGLISH to "JNMF Settings",
            AppLanguage.CEBUANO to "Mga Setting sa JNMF",
            AppLanguage.INDONESIAN to "Pengaturan JNMF"
        ),
        StrKey.SETTINGS_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Appearance, language, and audio management",
            AppLanguage.CEBUANO to "Panagway, pinulongan, ug pagdumala sa audio",
            AppLanguage.INDONESIAN to "Tampilan, bahasa, dan manajemen audio"
        ),
        StrKey.UI_COLOR_TITLE to mapOf(
            AppLanguage.ENGLISH to "UI Accent Color",
            AppLanguage.CEBUANO to "Kolor sa Tema",
            AppLanguage.INDONESIAN to "Warna Aksen UI"
        ),
        StrKey.UI_COLOR_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "1-tap smooth theme switch without interrupting music",
            AppLanguage.CEBUANO to "1-tap nga hapsay nga pag-ilis sa kolor nga dili mopahunong sa musika",
            AppLanguage.INDONESIAN to "1-ketuk ganti tema halus tanpa menghentikan musik"
        ),
        StrKey.COLOR_GREEN to mapOf(
            AppLanguage.ENGLISH to "Green",
            AppLanguage.CEBUANO to "Berde",
            AppLanguage.INDONESIAN to "Hijau"
        ),
        StrKey.COLOR_BLUE to mapOf(
            AppLanguage.ENGLISH to "Blue",
            AppLanguage.CEBUANO to "Asul",
            AppLanguage.INDONESIAN to "Biru"
        ),
        StrKey.COLOR_RED to mapOf(
            AppLanguage.ENGLISH to "Red",
            AppLanguage.CEBUANO to "Pula",
            AppLanguage.INDONESIAN to "Merah"
        ),
        StrKey.COLOR_WHITE to mapOf(
            AppLanguage.ENGLISH to "White",
            AppLanguage.CEBUANO to "Puti",
            AppLanguage.INDONESIAN to "Putih"
        ),
        StrKey.COLOR_CYAN to mapOf(
            AppLanguage.ENGLISH to "Cyan",
            AppLanguage.CEBUANO to "Siyan",
            AppLanguage.INDONESIAN to "Sian"
        ),
        StrKey.COLOR_VIOLET to mapOf(
            AppLanguage.ENGLISH to "Violet",
            AppLanguage.CEBUANO to "Bayolet",
            AppLanguage.INDONESIAN to "Ungu"
        ),
        StrKey.LANG_TITLE to mapOf(
            AppLanguage.ENGLISH to "Language Selection",
            AppLanguage.CEBUANO to "Pagpili sa Pinulongan",
            AppLanguage.INDONESIAN to "Pilihan Bahasa"
        ),
        StrKey.LANG_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "Instant interface translation without stopping playback",
            AppLanguage.CEBUANO to "Diha-diha nga paghubad nga dili mopahunong sa musika",
            AppLanguage.INDONESIAN to "Terjemahan antarmuka instan tanpa menghentikan pemutaran"
        ),
        StrKey.LANG_ENGLISH_NAME to mapOf(
            AppLanguage.ENGLISH to "English",
            AppLanguage.CEBUANO to "English (Iningles)",
            AppLanguage.INDONESIAN to "English (Inggris)"
        ),
        StrKey.LANG_ENGLISH_DESC to mapOf(
            AppLanguage.ENGLISH to "EN • International English",
            AppLanguage.CEBUANO to "EN • Internasyonal nga Iningles",
            AppLanguage.INDONESIAN to "EN • Bahasa Inggris Internasional"
        ),
        StrKey.LANG_CEBUANO_NAME to mapOf(
            AppLanguage.ENGLISH to "Cebuano (Bisaya)",
            AppLanguage.CEBUANO to "Cebuano (Bisaya)",
            AppLanguage.INDONESIAN to "Cebuano (Bisaya)"
        ),
        StrKey.LANG_CEBUANO_DESC to mapOf(
            AppLanguage.ENGLISH to "CEB • Visayan / Cebuano",
            AppLanguage.CEBUANO to "CEB • Binisaya / Sugboanon",
            AppLanguage.INDONESIAN to "CEB • Bahasa Bisaya / Cebuano"
        ),
        StrKey.LANG_INDONESIAN_NAME to mapOf(
            AppLanguage.ENGLISH to "Bahasa Indonesia (JUSSARSian)",
            AppLanguage.CEBUANO to "Bahasa Indonesia (JUSSARSian)",
            AppLanguage.INDONESIAN to "Bahasa Indonesia (JUSSARSian)"
        ),
        StrKey.LANG_INDONESIAN_DESC to mapOf(
            AppLanguage.ENGLISH to "ID • Indonesian / JUSSARSian",
            AppLanguage.CEBUANO to "ID • Pinulongang Indones / JUSSARSian",
            AppLanguage.INDONESIAN to "ID • Bahasa Indonesia / JUSSARSian"
        ),
        StrKey.HIDDEN_SONGS_TITLE to mapOf(
            AppLanguage.ENGLISH to "Hidden & Excluded Songs",
            AppLanguage.CEBUANO to "Gitago nga mga Kanta",
            AppLanguage.INDONESIAN to "Lagu Tersembunyi & Dikecualikan"
        ),
        StrKey.NO_HIDDEN_SONGS to mapOf(
            AppLanguage.ENGLISH to "No tracks are currently hidden from library.",
            AppLanguage.CEBUANO to "Walay mga kanta nga gitago gikan sa library karon.",
            AppLanguage.INDONESIAN to "Saat ini tidak ada lagu yang disembunyikan dari koleksi."
        ),
        StrKey.CREDITS_TITLE to mapOf(
            AppLanguage.ENGLISH to "Developer & Project Credits",
            AppLanguage.CEBUANO to "Mga Kredito sa Naghimo ug Proyekto",
            AppLanguage.INDONESIAN to "Kredit Pengembang & Proyek"
        ),
        StrKey.CREDITS_SUBTITLE to mapOf(
            AppLanguage.ENGLISH to "JNMF Native Android Music Engine",
            AppLanguage.CEBUANO to "JNMF Lumad nga Android Music Engine",
            AppLanguage.INDONESIAN to "Mesin Musik Android Asli JNMF"
        ),
        StrKey.CREDIT_LABEL_APP_NAME to mapOf(
            AppLanguage.ENGLISH to "App Name",
            AppLanguage.CEBUANO to "Ngalan sa App",
            AppLanguage.INDONESIAN to "Nama Aplikasi"
        ),
        StrKey.CREDIT_LABEL_COMPILE_BY to mapOf(
            AppLanguage.ENGLISH to "Compiled By",
            AppLanguage.CEBUANO to "Gi-compile ni",
            AppLanguage.INDONESIAN to "Dikompilasi Oleh"
        ),
        StrKey.CREDIT_LABEL_CREATE_BY to mapOf(
            AppLanguage.ENGLISH to "Created By",
            AppLanguage.CEBUANO to "Gihimo ni",
            AppLanguage.INDONESIAN to "Dibuat Oleh"
        ),
        StrKey.CREDIT_LABEL_DEVELOPER to mapOf(
            AppLanguage.ENGLISH to "App Developer",
            AppLanguage.CEBUANO to "Naghimo sa App",
            AppLanguage.INDONESIAN to "Pengembang Aplikasi"
        ),
        StrKey.CREDIT_LABEL_TARGET_DEVICE to mapOf(
            AppLanguage.ENGLISH to "Target Device",
            AppLanguage.CEBUANO to "Target nga Aparato",
            AppLanguage.INDONESIAN to "Perangkat Target"
        ),
        StrKey.CREDIT_LABEL_AUDIO_FOCUS to mapOf(
            AppLanguage.ENGLISH to "Audio Focus",
            AppLanguage.CEBUANO to "Pokus sa Audio",
            AppLanguage.INDONESIAN to "Fokus Audio"
        ),

        // --- Scanning & Progress Messages ---
        StrKey.LOADING_SCANNING_MUSIC to mapOf(
            AppLanguage.ENGLISH to "Scanning Music Library",
            AppLanguage.CEBUANO to "Nagsusi sa Library sa Musika",
            AppLanguage.INDONESIAN to "Memindai Koleksi Musik"
        ),
        StrKey.LOADING_MUSIC_READY to mapOf(
            AppLanguage.ENGLISH to "Music Library Ready",
            AppLanguage.CEBUANO to "Andam na ang Library sa Musika",
            AppLanguage.INDONESIAN to "Koleksi Musik Siap"
        ),
        StrKey.LOADING_SCANNING_VIDEOS to mapOf(
            AppLanguage.ENGLISH to "Scanning Local Videos",
            AppLanguage.CEBUANO to "Nagsusi sa mga Video sa Aparato",
            AppLanguage.INDONESIAN to "Memindai Video Lokal"
        ),
        StrKey.LOADING_VIDEOS_READY to mapOf(
            AppLanguage.ENGLISH to "Video Library Ready",
            AppLanguage.CEBUANO to "Andam na ang Library sa Video",
            AppLanguage.INDONESIAN to "Koleksi Video Siap"
        ),
        StrKey.INDEXING_AUDIO_FILES to mapOf(
            AppLanguage.ENGLISH to "Indexing local audio files...",
            AppLanguage.CEBUANO to "Naglista sa mga lokal nga audio file...",
            AppLanguage.INDONESIAN to "Mengindeks file audio lokal..."
        ),
        StrKey.INDEXING_AUDIO to mapOf(
            AppLanguage.ENGLISH to "Indexing audio...",
            AppLanguage.CEBUANO to "Naglista sa audio...",
            AppLanguage.INDONESIAN to "Mengindeks audio..."
        ),
        StrKey.DISCOVERING_VIDEOS to mapOf(
            AppLanguage.ENGLISH to "Discovering videos on device...",
            AppLanguage.CEBUANO to "Nangita og mga video sa aparato...",
            AppLanguage.INDONESIAN to "Mencari video di perangkat..."
        ),
        StrKey.INDEXING_VIDEOS to mapOf(
            AppLanguage.ENGLISH to "Indexing videos...",
            AppLanguage.CEBUANO to "Naglista sa mga video...",
            AppLanguage.INDONESIAN to "Mengindeks video..."
        ),
        StrKey.READING_ITEM_PROGRESS_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Reading: %1\$s (%2\$d/%3\$d)",
            AppLanguage.CEBUANO to "Nagbasa: %1\$s (%2\$d/%3\$d)",
            AppLanguage.INDONESIAN to "Membaca: %1\$s (%2\$d/%3\$d)"
        ),
        StrKey.FOUND_TRACKS_COUNT_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Found %d tracks",
            AppLanguage.CEBUANO to "Nakit-an ang %d ka mga kanta",
            AppLanguage.INDONESIAN to "Ditemukan %d lagu"
        ),
        StrKey.FOUND_VIDEOS_COUNT_TEMPLATE to mapOf(
            AppLanguage.ENGLISH to "Found %d videos",
            AppLanguage.CEBUANO to "Nakit-an ang %d ka mga video",
            AppLanguage.INDONESIAN to "Ditemukan %d video"
        ),
        StrKey.MUSIC_SCAN_ERROR_TITLE to mapOf(
            AppLanguage.ENGLISH to "Music Scan Error",
            AppLanguage.CEBUANO to "Sayop sa Pag-scan sa Musika",
            AppLanguage.INDONESIAN to "Kesalahan Pindai Musik"
        ),
        StrKey.FAILED_SCAN_STORAGE to mapOf(
            AppLanguage.ENGLISH to "Failed to scan storage",
            AppLanguage.CEBUANO to "Napakyas sa pag-scan sa storage",
            AppLanguage.INDONESIAN to "Gagal memindai penyimpanan"
        ),
        StrKey.VIDEO_SCAN_ERROR_TITLE to mapOf(
            AppLanguage.ENGLISH to "Video Scan Error",
            AppLanguage.CEBUANO to "Sayop sa Pag-scan sa Video",
            AppLanguage.INDONESIAN to "Kesalahan Pindai Video"
        ),
        StrKey.FAILED_SCAN_VIDEOS to mapOf(
            AppLanguage.ENGLISH to "Failed to scan videos",
            AppLanguage.CEBUANO to "Napakyas sa pag-scan sa mga video",
            AppLanguage.INDONESIAN to "Gagal memindai video"
        )
    )

    /**
     * Resolves a [StrKey] for [lang] using English as the fallback if a translation is missing.
     */
    fun get(key: StrKey, lang: AppLanguage): String {
        val entry = translations[key] ?: return key.name
        return entry[lang]?.takeIf { it.isNotEmpty() }
            ?: entry[AppLanguage.ENGLISH]
            ?: key.name
    }

    /**
     * Exposes raw language map for completeness verification in unit tests.
     */
    internal fun hasExplicitTranslation(key: StrKey, lang: AppLanguage): Boolean {
        return !translations[key]?.get(lang).isNullOrBlank()
    }

    // --- Navigation & Header ---
    fun navMusic(lang: AppLanguage): String = get(StrKey.NAV_MUSIC, lang)
    fun navPlaylists(lang: AppLanguage): String = get(StrKey.NAV_PLAYLISTS, lang)
    fun navEqualizer(lang: AppLanguage): String = get(StrKey.NAV_EQUALIZER, lang)
    fun navVideos(lang: AppLanguage): String = get(StrKey.NAV_VIDEOS, lang)
    fun navSettings(lang: AppLanguage): String = get(StrKey.NAV_SETTINGS, lang)
    fun topSubtitle(lang: AppLanguage): String = get(StrKey.TOP_SUBTITLE, lang)
    fun officialLogoDesc(lang: AppLanguage): String = get(StrKey.OFFICIAL_LOGO_DESC, lang)
    fun quickDuplicates(lang: AppLanguage): String = get(StrKey.QUICK_DUPLICATES, lang)
    fun quickClassifier(lang: AppLanguage): String = get(StrKey.QUICK_CLASSIFIER, lang)
    fun quickNotifications(lang: AppLanguage): String = get(StrKey.QUICK_NOTIFICATIONS, lang)

    // --- Transitions ---
    fun applyingLanguage(lang: AppLanguage): String = get(StrKey.APPLYING_LANGUAGE, lang)
    fun applyingThemeColor(theme: ThemeColor, lang: AppLanguage): String =
        String.format(get(StrKey.APPLYING_THEME_TEMPLATE, lang), themeColorName(theme, lang))

    // --- Startup Screen ---
    fun startupSubtitle(lang: AppLanguage): String = get(StrKey.STARTUP_SUBTITLE, lang)
    fun startupInitTitle(lang: AppLanguage): String = get(StrKey.STARTUP_INIT_TITLE, lang)
    fun startupInitStatus(lang: AppLanguage): String = get(StrKey.STARTUP_INIT_STATUS, lang)
    fun startupScanTitle(lang: AppLanguage): String = get(StrKey.STARTUP_SCAN_TITLE, lang)
    fun startupScanStatus(lang: AppLanguage): String = get(StrKey.STARTUP_SCAN_STATUS, lang)
    fun startupScanProgress(current: Int, total: Int, itemTitle: String, lang: AppLanguage): String =
        String.format(get(StrKey.STARTUP_SCAN_PROGRESS_TEMPLATE, lang), current, total, itemTitle)
    fun startupScanningTracks(lang: AppLanguage): String = get(StrKey.STARTUP_SCANNING_TRACKS, lang)
    fun startupReadyTitle(lang: AppLanguage): String = get(StrKey.STARTUP_READY_TITLE, lang)
    fun startupReadyStatus(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.STARTUP_READY_STATUS_TEMPLATE, lang), count)
    fun enterJnmf(lang: AppLanguage): String = get(StrKey.ENTER_JNMF, lang)
    fun skip(lang: AppLanguage): String = get(StrKey.SKIP, lang)

    // --- Notifications ---
    fun notifDialogTitle(lang: AppLanguage): String = get(StrKey.NOTIF_DIALOG_TITLE, lang)
    fun notifStatusActive(lang: AppLanguage): String = get(StrKey.NOTIF_STATUS_ACTIVE, lang)
    fun notifStatusDisabled(lang: AppLanguage): String = get(StrKey.NOTIF_STATUS_DISABLED, lang)
    fun notifDescription(lang: AppLanguage): String = get(StrKey.NOTIF_DESCRIPTION, lang)
    fun notifGrantPermission(lang: AppLanguage): String = get(StrKey.NOTIF_GRANT_PERMISSION, lang)
    fun notifOpenSettings(lang: AppLanguage): String = get(StrKey.NOTIF_OPEN_SETTINGS, lang)
    fun notifTest(lang: AppLanguage): String = get(StrKey.NOTIF_TEST, lang)
    fun notifTestActiveStatus(lang: AppLanguage): String = get(StrKey.NOTIF_TEST_ACTIVE_STATUS, lang)
    fun notifUpdatedFeedback(lang: AppLanguage): String = get(StrKey.NOTIF_UPDATED_FEEDBACK, lang)
    fun notifSettingsSubtitle(lang: AppLanguage): String = get(StrKey.NOTIF_SETTINGS_SUBTITLE, lang)
    fun notifReadyForPlayback(lang: AppLanguage): String = get(StrKey.NOTIF_READY_FOR_PLAYBACK, lang)
    fun notifChannelName(lang: AppLanguage): String = get(StrKey.NOTIF_CHANNEL_NAME, lang)
    fun notifChannelDesc(lang: AppLanguage): String = get(StrKey.NOTIF_CHANNEL_DESC, lang)

    // --- Common Actions ---
    fun close(lang: AppLanguage): String = get(StrKey.CLOSE, lang)
    fun cancel(lang: AppLanguage): String = get(StrKey.CANCEL, lang)
    fun create(lang: AppLanguage): String = get(StrKey.CREATE, lang)
    fun save(lang: AppLanguage): String = get(StrKey.SAVE, lang)
    fun done(lang: AppLanguage): String = get(StrKey.DONE, lang)
    fun add(lang: AppLanguage): String = get(StrKey.ADD, lang)
    fun rename(lang: AppLanguage): String = get(StrKey.RENAME, lang)
    fun delete(lang: AppLanguage): String = get(StrKey.DELETE, lang)
    fun restore(lang: AppLanguage): String = get(StrKey.RESTORE, lang)
    fun manage(lang: AppLanguage): String = get(StrKey.MANAGE, lang)
    fun back(lang: AppLanguage): String = get(StrKey.BACK, lang)
    fun retry(lang: AppLanguage): String = get(StrKey.RETRY, lang)
    fun dismiss(lang: AppLanguage): String = get(StrKey.DISMISS, lang)
    fun search(lang: AppLanguage): String = get(StrKey.SEARCH, lang)
    fun clearSearch(lang: AppLanguage): String = get(StrKey.CLEAR_SEARCH, lang)
    fun moreOptions(lang: AppLanguage): String = get(StrKey.MORE_OPTIONS, lang)
    fun favorite(lang: AppLanguage): String = get(StrKey.FAVORITE, lang)
    fun selected(lang: AppLanguage): String = get(StrKey.SELECTED, lang)
    fun defaultBadge(lang: AppLanguage): String = get(StrKey.DEFAULT_BADGE, lang)
    fun activeBadge(lang: AppLanguage): String = get(StrKey.ACTIVE_BADGE, lang)
    fun errorLabel(lang: AppLanguage): String = get(StrKey.ERROR_LABEL, lang)
    fun completedLabel(lang: AppLanguage): String = get(StrKey.COMPLETED_LABEL, lang)
    fun loadingDefault(lang: AppLanguage): String = get(StrKey.LOADING_DEFAULT, lang)

    // --- Search, Sorting & Music Library ---
    fun searchPlaceholder(lang: AppLanguage): String = get(StrKey.SEARCH_PLACEHOLDER, lang)
    fun searchTracksCountPlaceholder(count: Int, lang: AppLanguage): String =
        if (count > 0) String.format(get(StrKey.SEARCH_TRACKS_COUNT_TEMPLATE, lang), count)
        else searchPlaceholder(lang)
    fun sortBy(lang: AppLanguage): String = get(StrKey.SORT_BY, lang)
    fun sortTracksTitle(lang: AppLanguage): String = get(StrKey.SORT_TRACKS_TITLE, lang)
    fun sortTitle(lang: AppLanguage): String = get(StrKey.SORT_TITLE, lang)
    fun sortArtist(lang: AppLanguage): String = get(StrKey.SORT_ARTIST, lang)
    fun sortDate(lang: AppLanguage): String = get(StrKey.SORT_DATE, lang)
    fun sortDuration(lang: AppLanguage): String = get(StrKey.SORT_DURATION, lang)
    fun sortOptionLabel(option: SortOption, lang: AppLanguage): String = when (option) {
        SortOption.NAME_ASC -> get(StrKey.SORT_NAME_ASC, lang)
        SortOption.NAME_DESC -> get(StrKey.SORT_NAME_DESC, lang)
        SortOption.DATE_DESC -> get(StrKey.SORT_DATE_DESC, lang)
        SortOption.DATE_ASC -> get(StrKey.SORT_DATE_ASC, lang)
        SortOption.SIZE_DESC -> get(StrKey.SORT_SIZE_DESC, lang)
        SortOption.SIZE_ASC -> get(StrKey.SORT_SIZE_ASC, lang)
    }
    fun audioAccessRequired(lang: AppLanguage): String = get(StrKey.AUDIO_ACCESS_REQUIRED, lang)
    fun grantAudioAccess(lang: AppLanguage): String = get(StrKey.GRANT_AUDIO_ACCESS, lang)
    fun permissionDesc(lang: AppLanguage): String = get(StrKey.PERMISSION_DESC, lang)
    fun refreshLibrary(lang: AppLanguage): String = get(StrKey.REFRESH_LIBRARY, lang)
    fun rescanStorage(lang: AppLanguage): String = get(StrKey.RESCAN_STORAGE, lang)
    fun noSongsFound(lang: AppLanguage): String = get(StrKey.NO_SONGS_FOUND, lang)
    fun noTracksMatchQuery(query: String, lang: AppLanguage): String =
        String.format(get(StrKey.NO_TRACKS_MATCH_QUERY_TEMPLATE, lang), query)
    fun tracksCount(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.TRACKS_COUNT_TEMPLATE, lang), count)
    fun playNow(lang: AppLanguage): String = get(StrKey.PLAY_NOW, lang)
    fun addToPlaylist(lang: AppLanguage): String = get(StrKey.ADD_TO_PLAYLIST, lang)
    fun removeFromPlaylist(lang: AppLanguage): String = get(StrKey.REMOVE_FROM_PLAYLIST, lang)
    fun hideFromLibrary(lang: AppLanguage): String = get(StrKey.HIDE_FROM_LIBRARY, lang)
    fun deleteFile(lang: AppLanguage): String = get(StrKey.DELETE_FILE, lang)

    // --- Player & Now Playing ---
    fun nowPlaying(lang: AppLanguage): String = get(StrKey.NOW_PLAYING, lang)
    fun nowPlayingHeader(lang: AppLanguage): String = get(StrKey.NOW_PLAYING_HEADER, lang)
    fun closeNowPlaying(lang: AppLanguage): String = get(StrKey.CLOSE_NOW_PLAYING, lang)
    fun openEqualizer(lang: AppLanguage): String = get(StrKey.OPEN_EQUALIZER, lang)
    fun play(lang: AppLanguage): String = get(StrKey.PLAY, lang)
    fun pause(lang: AppLanguage): String = get(StrKey.PAUSE, lang)
    fun playing(lang: AppLanguage): String = get(StrKey.PLAYING, lang)
    fun paused(lang: AppLanguage): String = get(StrKey.PAUSED, lang)
    fun previous(lang: AppLanguage): String = get(StrKey.PREVIOUS, lang)
    fun next(lang: AppLanguage): String = get(StrKey.NEXT, lang)
    fun queueTitle(lang: AppLanguage): String = get(StrKey.QUEUE_TITLE, lang)
    fun reverseQueue(lang: AppLanguage): String = get(StrKey.REVERSE_QUEUE, lang)
    fun reverseQueueOn(lang: AppLanguage): String = get(StrKey.REVERSE_QUEUE_ON, lang)
    fun reverseQueueOff(lang: AppLanguage): String = get(StrKey.REVERSE_QUEUE_OFF, lang)
    fun shuffle(lang: AppLanguage): String = get(StrKey.SHUFFLE, lang)
    fun repeat(lang: AppLanguage): String = get(StrKey.REPEAT, lang)
    fun speed(lang: AppLanguage): String = get(StrKey.SPEED, lang)
    fun errorAudioMissing(lang: AppLanguage): String = get(StrKey.ERROR_AUDIO_MISSING, lang)
    fun errorAudioUnsupported(lang: AppLanguage): String = get(StrKey.ERROR_AUDIO_UNSUPPORTED, lang)
    fun errorCannotPlayTrack(lang: AppLanguage): String = get(StrKey.ERROR_CANNOT_PLAY_TRACK, lang)
    fun errorPlaybackGeneric(lang: AppLanguage): String = get(StrKey.ERROR_PLAYBACK_GENERIC, lang)

    // --- Remix Categories ---
    fun remixCategoryName(category: RemixCategory, lang: AppLanguage): String = when (category) {
        RemixCategory.UNCLASSIFIED -> get(StrKey.REMIX_STANDARD, lang)
        RemixCategory.FULLBEAT -> get(StrKey.REMIX_FULLBEAT, lang)
        RemixCategory.SLOW -> get(StrKey.REMIX_SLOW, lang)
        RemixCategory.INDO_REMIX -> get(StrKey.REMIX_INDO, lang)
        RemixCategory.BREAKBEAT -> get(StrKey.REMIX_BREAKBEAT, lang)
    }

    fun remixBadgeShort(category: RemixCategory, lang: AppLanguage): String = when (category) {
        RemixCategory.FULLBEAT -> get(StrKey.REMIX_SHORT_FB, lang)
        RemixCategory.SLOW -> get(StrKey.REMIX_SHORT_SLOW, lang)
        RemixCategory.INDO_REMIX -> get(StrKey.REMIX_SHORT_REMIX, lang)
        RemixCategory.BREAKBEAT -> get(StrKey.REMIX_SHORT_BB, lang)
        RemixCategory.UNCLASSIFIED -> ""
    }

    // --- Equalizer ---
    fun eqTitle(lang: AppLanguage): String = get(StrKey.EQ_TITLE, lang)
    fun eqSubtitle(lang: AppLanguage): String = get(StrKey.EQ_SUBTITLE, lang)
    fun eqMaster(lang: AppLanguage): String = get(StrKey.EQ_MASTER, lang)
    fun eqSessionStatus(lang: AppLanguage): String = get(StrKey.EQ_SESSION_STATUS, lang)
    fun eqActive(lang: AppLanguage): String = get(StrKey.EQ_ACTIVE, lang)
    fun eqPipelineAttached(sessionId: Int, lang: AppLanguage): String =
        String.format(get(StrKey.EQ_PIPELINE_ATTACHED_TEMPLATE, lang), sessionId)
    fun eqIdle(lang: AppLanguage): String = get(StrKey.EQ_IDLE, lang)
    fun eqHardwareActive(lang: AppLanguage): String = get(StrKey.EQ_HARDWARE_ACTIVE, lang)
    fun eqSoftwareActive(lang: AppLanguage): String = get(StrKey.EQ_SOFTWARE_ACTIVE, lang)
    fun soundPresets(lang: AppLanguage): String = get(StrKey.SOUND_PRESETS, lang)
    fun eqFrequencyBandsHeader(lang: AppLanguage): String = get(StrKey.EQ_FREQUENCY_BANDS_HEADER, lang)
    fun eqAcousticEnhancementHeader(lang: AppLanguage): String = get(StrKey.EQ_ACOUSTIC_ENHANCEMENT_HEADER, lang)
    fun bassHoreg(lang: AppLanguage): String = get(StrKey.BASS_HOREG, lang)
    fun bassHoregSubtitle(lang: AppLanguage): String = get(StrKey.BASS_HOREG_SUBTITLE, lang)
    fun stereoWidth(lang: AppLanguage): String = get(StrKey.STEREO_WIDTH, lang)
    fun stereoWidthSubtitle(lang: AppLanguage): String = get(StrKey.STEREO_WIDTH_SUBTITLE, lang)
    fun trebleClarity(lang: AppLanguage): String = get(StrKey.TREBLE_CLARITY, lang)
    fun trebleClaritySubtitle(lang: AppLanguage): String = get(StrKey.TREBLE_CLARITY_SUBTITLE, lang)
    fun eqPresetLabel(presetId: String, lang: AppLanguage): String = when (presetId) {
        "Indo Fullbeat" -> get(StrKey.PRESET_INDO_FULLBEAT, lang)
        "Bass Horeg" -> get(StrKey.PRESET_BASS_HOREG, lang)
        "Slow Remix" -> get(StrKey.PRESET_SLOW_REMIX, lang)
        "Club Remix" -> get(StrKey.PRESET_CLUB_REMIX, lang)
        "Vocal Clarity" -> get(StrKey.PRESET_VOCAL_CLARITY, lang)
        "Flat" -> get(StrKey.PRESET_FLAT, lang)
        "Custom" -> get(StrKey.PRESET_CUSTOM, lang)
        else -> presetId
    }
    fun resetDefault(lang: AppLanguage): String = get(StrKey.RESET_DEFAULT, lang)

    // --- Playlists ---
    fun playlistsTitle(lang: AppLanguage): String = get(StrKey.PLAYLISTS_TITLE, lang)
    fun customPlaylistsHeader(lang: AppLanguage): String = get(StrKey.CUSTOM_PLAYLISTS_HEADER, lang)
    fun favoritesTitle(lang: AppLanguage): String = get(StrKey.FAVORITES_TITLE, lang)
    fun savedFavoritesSubtitle(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.SAVED_FAVORITES_SUBTITLE_TEMPLATE, lang), count)
    fun emptyFavorites(lang: AppLanguage): String = get(StrKey.EMPTY_FAVORITES, lang)
    fun savedTracksCount(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.SAVED_TRACKS_COUNT_TEMPLATE, lang), count)
    fun newPlaylist(lang: AppLanguage): String = get(StrKey.NEW_PLAYLIST, lang)
    fun createNewPlaylist(lang: AppLanguage): String = get(StrKey.CREATE_NEW_PLAYLIST, lang)
    fun newPlaylistNameLabel(lang: AppLanguage): String = get(StrKey.NEW_PLAYLIST_NAME_LABEL, lang)
    fun enterPlaylistName(lang: AppLanguage): String = get(StrKey.ENTER_PLAYLIST_NAME, lang)
    fun createAndAddTrack(lang: AppLanguage): String = get(StrKey.CREATE_AND_ADD_TRACK, lang)
    fun addSongToPlaylistTitle(songTitle: String, lang: AppLanguage): String =
        String.format(get(StrKey.ADD_SONG_TO_PLAYLIST_TITLE_TEMPLATE, lang), songTitle)
    fun noPlaylistsCreatePrompt(lang: AppLanguage): String = get(StrKey.NO_PLAYLISTS_CREATE_PROMPT, lang)
    fun emptyPlaylists(lang: AppLanguage): String = get(StrKey.EMPTY_PLAYLISTS, lang)
    fun playlistEmpty(lang: AppLanguage): String = get(StrKey.PLAYLIST_EMPTY, lang)
    fun addTracks(lang: AppLanguage): String = get(StrKey.ADD_TRACKS, lang)
    fun addTracksNow(lang: AppLanguage): String = get(StrKey.ADD_TRACKS_NOW, lang)
    fun playAll(lang: AppLanguage): String = get(StrKey.PLAY_ALL, lang)
    fun moveUp(lang: AppLanguage): String = get(StrKey.MOVE_UP, lang)
    fun moveDown(lang: AppLanguage): String = get(StrKey.MOVE_DOWN, lang)
    fun addTracksToPlaylistTitle(playlistName: String, lang: AppLanguage): String =
        String.format(get(StrKey.ADD_TRACKS_TO_PLAYLIST_TITLE_TEMPLATE, lang), playlistName)
    fun allSongsInPlaylist(lang: AppLanguage): String = get(StrKey.ALL_SONGS_IN_PLAYLIST, lang)
    fun renamePlaylistTitle(lang: AppLanguage): String = get(StrKey.RENAME_PLAYLIST_TITLE, lang)
    fun deletePlaylistTitle(lang: AppLanguage): String = get(StrKey.DELETE_PLAYLIST_TITLE, lang)
    fun deletePlaylistConfirmMsg(playlistName: String, lang: AppLanguage): String =
        String.format(get(StrKey.DELETE_PLAYLIST_CONFIRM_TEMPLATE, lang), playlistName)
    fun missingTrackTitle(id: Long, lang: AppLanguage): String =
        String.format(get(StrKey.MISSING_TRACK_TITLE_TEMPLATE, lang), id)
    fun unknownLocation(lang: AppLanguage): String = get(StrKey.UNKNOWN_LOCATION, lang)
    fun unavailable(lang: AppLanguage): String = get(StrKey.UNAVAILABLE, lang)

    // --- Duplicate Detection ---
    fun duplicatesTitle(lang: AppLanguage): String = get(StrKey.DUPLICATES_TITLE, lang)
    fun duplicatesThresholdSubtitle(lang: AppLanguage): String = get(StrKey.DUPLICATES_THRESHOLD_SUBTITLE, lang)
    fun acousticSimilarityEngine(lang: AppLanguage): String = get(StrKey.ACOUSTIC_SIMILARITY_ENGINE, lang)
    fun scanSimilarAudio(lang: AppLanguage): String = get(StrKey.SCAN_SIMILAR_AUDIO, lang)
    fun rescanLibraryCount(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.RESCAN_LIBRARY_COUNT_TEMPLATE, lang), count)
    fun duplicatesDesc(lang: AppLanguage): String = get(StrKey.DUPLICATES_DESC, lang)
    fun similarity(lang: AppLanguage): String = get(StrKey.SIMILARITY, lang)
    fun similarityScore(lang: AppLanguage): String = get(StrKey.SIMILARITY_SCORE, lang)
    fun similarityMatchPercent(percent: Int, lang: AppLanguage): String =
        String.format(get(StrKey.SIMILARITY_MATCH_PERCENT_TEMPLATE, lang), percent)
    fun versionA(lang: AppLanguage): String = get(StrKey.VERSION_A, lang)
    fun versionB(lang: AppLanguage): String = get(StrKey.VERSION_B, lang)
    fun previewTrack(lang: AppLanguage): String = get(StrKey.PREVIEW_TRACK, lang)
    fun startingAnalysis(lang: AppLanguage): String = get(StrKey.STARTING_ANALYSIS, lang)
    fun scanCompletedShort(lang: AppLanguage): String = get(StrKey.SCAN_COMPLETED_SHORT, lang)
    fun duplicateScanCompletedMsg(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.DUPLICATE_SCAN_COMPLETED_TEMPLATE, lang), count)
    fun noDuplicatesFound(lang: AppLanguage): String = get(StrKey.NO_DUPLICATES_FOUND, lang)
    fun tapScanToAnalyze(lang: AppLanguage): String = get(StrKey.TAP_SCAN_TO_ANALYZE, lang)
    fun confirmDeleteTitle(lang: AppLanguage): String = get(StrKey.CONFIRM_DELETE_TITLE, lang)
    fun confirmDeleteMsg(lang: AppLanguage): String = get(StrKey.CONFIRM_DELETE_MSG, lang)
    fun confirmDeleteSongMsg(songTitle: String, formattedSize: String, lang: AppLanguage): String =
        String.format(get(StrKey.CONFIRM_DELETE_SONG_MSG_TEMPLATE, lang), songTitle, formattedSize)
    fun deleteTrack(lang: AppLanguage): String = get(StrKey.DELETE_TRACK, lang)

    fun localizeMatchReason(rawReason: String, lang: AppLanguage): String {
        if (lang == AppLanguage.ENGLISH) return rawReason
        return when {
            rawReason == "Exact playback duration match (±1.5s)" ->
                get(StrKey.REASON_EXACT_DURATION, lang)
            rawReason == "Close playback duration (±4s)" ->
                get(StrKey.REASON_CLOSE_DURATION, lang)
            rawReason == "Near identical title tokens" ->
                get(StrKey.REASON_IDENTICAL_TITLE, lang)
            rawReason.startsWith("Identical artist credit: ") -> {
                val artist = rawReason.removePrefix("Identical artist credit: ")
                String.format(get(StrKey.REASON_IDENTICAL_ARTIST_TEMPLATE, lang), artist)
            }
            rawReason.startsWith("Distinguished as different remix arrangements (") && rawReason.endsWith(")") -> {
                val inner = rawReason
                    .removePrefix("Distinguished as different remix arrangements (")
                    .removeSuffix(")")
                val parts = inner.split(" vs ")
                if (parts.size == 2) {
                    String.format(get(StrKey.REASON_DIFFERENT_REMIX_TEMPLATE, lang), parts[0], parts[1])
                } else rawReason
            }
            rawReason.startsWith("Matching remix classification: ") -> {
                val cat = rawReason.removePrefix("Matching remix classification: ")
                String.format(get(StrKey.REASON_MATCHING_REMIX_TEMPLATE, lang), cat)
            }
            else -> rawReason
        }
    }

    // --- Classifier ---
    fun classifierTitle(lang: AppLanguage): String = get(StrKey.CLASSIFIER_TITLE, lang)
    fun classifierDesc(lang: AppLanguage): String = get(StrKey.CLASSIFIER_DESC, lang)
    fun filterAllCount(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.FILTER_ALL_COUNT_TEMPLATE, lang), count)
    fun editCategory(lang: AppLanguage): String = get(StrKey.EDIT_CATEGORY, lang)
    fun manualCategoryCorrectionTitle(lang: AppLanguage): String = get(StrKey.MANUAL_CATEGORY_CORRECTION_TITLE, lang)

    // --- Videos ---
    fun videosTitle(lang: AppLanguage): String = get(StrKey.VIDEOS_TITLE, lang)
    fun videosCount(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.VIDEOS_COUNT_TEMPLATE, lang), count)
    fun videosFoundOnDevice(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.VIDEOS_FOUND_ON_DEVICE_TEMPLATE, lang), count)
    fun noVideosFound(lang: AppLanguage): String = get(StrKey.NO_VIDEOS_FOUND, lang)
    fun refreshVideos(lang: AppLanguage): String = get(StrKey.REFRESH_VIDEOS, lang)
    fun fullscreen(lang: AppLanguage): String = get(StrKey.FULLSCREEN, lang)
    fun exitFullscreen(lang: AppLanguage): String = get(StrKey.EXIT_FULLSCREEN, lang)
    fun closePlayer(lang: AppLanguage): String = get(StrKey.CLOSE_PLAYER, lang)
    fun moreVideos(lang: AppLanguage): String = get(StrKey.MORE_VIDEOS, lang)
    fun seekBackward10s(lang: AppLanguage): String = get(StrKey.SEEK_BACKWARD_10S, lang)
    fun seekForward10s(lang: AppLanguage): String = get(StrKey.SEEK_FORWARD_10S, lang)
    fun videoPlaybackErrorTitle(lang: AppLanguage): String = get(StrKey.VIDEO_PLAYBACK_ERROR_TITLE, lang)
    fun videoPlaybackErrorDefault(lang: AppLanguage): String = get(StrKey.VIDEO_PLAYBACK_ERROR_DEFAULT, lang)
    fun videoFileMissing(lang: AppLanguage): String = get(StrKey.VIDEO_FILE_MISSING, lang)
    fun videoUnsupportedCodec(lang: AppLanguage): String = get(StrKey.VIDEO_UNSUPPORTED_CODEC, lang)
    fun videoLoadFailed(lang: AppLanguage): String = get(StrKey.VIDEO_LOAD_FAILED, lang)
    fun videoBgPlaybackTitle(lang: AppLanguage): String = get(StrKey.VIDEO_BG_PLAYBACK_TITLE, lang)
    fun videoModeNormal(lang: AppLanguage): String = get(StrKey.VIDEO_MODE_NORMAL, lang)
    fun videoModeAudioOnly(lang: AppLanguage): String = get(StrKey.VIDEO_MODE_AUDIO_ONLY, lang)
    fun videoModeLabel(mode: com.example.playback.VideoPlaybackMode, lang: AppLanguage): String =
        when (mode) {
            com.example.playback.VideoPlaybackMode.NORMAL_VIDEO -> videoModeNormal(lang)
            com.example.playback.VideoPlaybackMode.AUDIO_ONLY -> videoModeAudioOnly(lang)
        }
    fun videoModeDescription(mode: com.example.playback.VideoPlaybackMode, lang: AppLanguage): String =
        when (mode) {
            com.example.playback.VideoPlaybackMode.NORMAL_VIDEO -> get(StrKey.VIDEO_MODE_NORMAL_DESC, lang)
            com.example.playback.VideoPlaybackMode.AUDIO_ONLY -> get(StrKey.VIDEO_MODE_AUDIO_ONLY_DESC, lang)
        }
    fun videoAudioOnlyStageTitle(lang: AppLanguage): String = get(StrKey.VIDEO_AUDIO_ONLY_STAGE_TITLE, lang)
    fun videoAudioOnlyStageSubtitle(lang: AppLanguage): String = get(StrKey.VIDEO_AUDIO_ONLY_STAGE_SUBTITLE, lang)
    fun videoSwitchToNormal(lang: AppLanguage): String = get(StrKey.VIDEO_SWITCH_TO_NORMAL, lang)
    fun videoStop(lang: AppLanguage): String = get(StrKey.VIDEO_STOP, lang)
    fun videoMinimizeToBg(lang: AppLanguage): String = get(StrKey.VIDEO_MINIMIZE_TO_BG, lang)
    fun videoNotifBadge(lang: AppLanguage): String = get(StrKey.VIDEO_NOTIF_BADGE, lang)
    fun videoNotifModeSubtitle(mode: com.example.playback.VideoPlaybackMode, lang: AppLanguage): String =
        String.format(get(StrKey.VIDEO_NOTIF_MODE_TEMPLATE, lang), videoModeLabel(mode, lang))
    fun videoReturnToPlayer(lang: AppLanguage): String = get(StrKey.VIDEO_RETURN_TO_PLAYER, lang)

    // --- Settings, Themes, Languages & Credits ---
    fun settingsTitle(lang: AppLanguage): String = get(StrKey.SETTINGS_TITLE, lang)
    fun settingsSubtitle(lang: AppLanguage): String = get(StrKey.SETTINGS_SUBTITLE, lang)
    fun uiColorTitle(lang: AppLanguage): String = get(StrKey.UI_COLOR_TITLE, lang)
    fun uiColorSubtitle(lang: AppLanguage): String = get(StrKey.UI_COLOR_SUBTITLE, lang)
    fun themeColorName(theme: ThemeColor, lang: AppLanguage): String = when (theme) {
        ThemeColor.NEON_GREEN -> get(StrKey.COLOR_GREEN, lang)
        ThemeColor.ELECTRIC_BLUE -> get(StrKey.COLOR_BLUE, lang)
        ThemeColor.CRIMSON_RED -> get(StrKey.COLOR_RED, lang)
        ThemeColor.MODERN_WHITE -> get(StrKey.COLOR_WHITE, lang)
        ThemeColor.VIBRANT_CYAN -> get(StrKey.COLOR_CYAN, lang)
        ThemeColor.DEEP_VIOLET -> get(StrKey.COLOR_VIOLET, lang)
    }
    fun langTitle(lang: AppLanguage): String = get(StrKey.LANG_TITLE, lang)
    fun langSubtitle(lang: AppLanguage): String = get(StrKey.LANG_SUBTITLE, lang)
    fun languageOptionName(target: AppLanguage, currentLang: AppLanguage): String = when (target) {
        AppLanguage.ENGLISH -> get(StrKey.LANG_ENGLISH_NAME, currentLang)
        AppLanguage.CEBUANO -> get(StrKey.LANG_CEBUANO_NAME, currentLang)
        AppLanguage.INDONESIAN -> get(StrKey.LANG_INDONESIAN_NAME, currentLang)
    }
    fun languageOptionDesc(target: AppLanguage, currentLang: AppLanguage): String = when (target) {
        AppLanguage.ENGLISH -> get(StrKey.LANG_ENGLISH_DESC, currentLang)
        AppLanguage.CEBUANO -> get(StrKey.LANG_CEBUANO_DESC, currentLang)
        AppLanguage.INDONESIAN -> get(StrKey.LANG_INDONESIAN_DESC, currentLang)
    }
    fun hiddenSongsTitle(lang: AppLanguage): String = get(StrKey.HIDDEN_SONGS_TITLE, lang)
    fun noHiddenSongs(lang: AppLanguage): String = get(StrKey.NO_HIDDEN_SONGS, lang)
    fun creditsTitle(lang: AppLanguage): String = get(StrKey.CREDITS_TITLE, lang)
    fun creditsSubtitle(lang: AppLanguage): String = get(StrKey.CREDITS_SUBTITLE, lang)
    fun creditLabelAppName(lang: AppLanguage): String = get(StrKey.CREDIT_LABEL_APP_NAME, lang)
    fun creditLabelCompileBy(lang: AppLanguage): String = get(StrKey.CREDIT_LABEL_COMPILE_BY, lang)
    fun creditLabelCreateBy(lang: AppLanguage): String = get(StrKey.CREDIT_LABEL_CREATE_BY, lang)
    fun creditLabelDeveloper(lang: AppLanguage): String = get(StrKey.CREDIT_LABEL_DEVELOPER, lang)
    fun creditLabelTargetDevice(lang: AppLanguage): String = get(StrKey.CREDIT_LABEL_TARGET_DEVICE, lang)
    fun creditLabelAudioFocus(lang: AppLanguage): String = get(StrKey.CREDIT_LABEL_AUDIO_FOCUS, lang)

    // Credits values (Preserved project identity)
    fun creditAppName(): String = "JNMF Music Player"
    fun creditCompile(): String = "Compile by JUSSAR GOLDEN"
    fun creditCreateBy(): String = "CREATE BY"
    fun creditChatgpt(): String = "ChatGPT advisor"
    fun creditAiStudio(): String = "Google AI Studio: app developer"
    fun creditTargetDevice(): String = "Xiaomi Redmi 10C (220333QAG)"
    fun creditAudioFocus(): String = "Indo Remix, Fullbeat, Slow Remix"

    // --- Scanning & Progress ---
    fun loadingScanningMusic(lang: AppLanguage): String = get(StrKey.LOADING_SCANNING_MUSIC, lang)
    fun loadingMusicReady(lang: AppLanguage): String = get(StrKey.LOADING_MUSIC_READY, lang)
    fun loadingScanningVideos(lang: AppLanguage): String = get(StrKey.LOADING_SCANNING_VIDEOS, lang)
    fun loadingVideosReady(lang: AppLanguage): String = get(StrKey.LOADING_VIDEOS_READY, lang)
    fun indexingAudioFiles(lang: AppLanguage): String = get(StrKey.INDEXING_AUDIO_FILES, lang)
    fun indexingAudio(lang: AppLanguage): String = get(StrKey.INDEXING_AUDIO, lang)
    fun discoveringVideos(lang: AppLanguage): String = get(StrKey.DISCOVERING_VIDEOS, lang)
    fun indexingVideos(lang: AppLanguage): String = get(StrKey.INDEXING_VIDEOS, lang)
    fun readingItemProgress(itemTitle: String, current: Int, total: Int, lang: AppLanguage): String =
        String.format(get(StrKey.READING_ITEM_PROGRESS_TEMPLATE, lang), itemTitle, current, total)
    fun foundTracksCount(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.FOUND_TRACKS_COUNT_TEMPLATE, lang), count)
    fun foundVideosCount(count: Int, lang: AppLanguage): String =
        String.format(get(StrKey.FOUND_VIDEOS_COUNT_TEMPLATE, lang), count)
    fun musicScanErrorTitle(lang: AppLanguage): String = get(StrKey.MUSIC_SCAN_ERROR_TITLE, lang)
    fun failedScanStorage(lang: AppLanguage): String = get(StrKey.FAILED_SCAN_STORAGE, lang)
    fun videoScanErrorTitle(lang: AppLanguage): String = get(StrKey.VIDEO_SCAN_ERROR_TITLE, lang)
    fun failedScanVideos(lang: AppLanguage): String = get(StrKey.FAILED_SCAN_VIDEOS, lang)
}
