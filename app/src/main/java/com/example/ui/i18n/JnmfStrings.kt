package com.example.ui.i18n

import com.example.data.repository.AppLanguage

object JnmfStrings {

    // --- Navigation ---
    fun navMusic(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Music"
        AppLanguage.CEBUANO -> "Musika"
        AppLanguage.INDONESIAN -> "Musik"
    }

    fun navPlaylists(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Playlists"
        AppLanguage.CEBUANO -> "Mga Playlist"
        AppLanguage.INDONESIAN -> "Daftar Putar"
    }

    fun navEqualizer(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Equalizer"
        AppLanguage.CEBUANO -> "Equalizer"
        AppLanguage.INDONESIAN -> "Equalizer"
    }

    fun navVideos(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Videos"
        AppLanguage.CEBUANO -> "Mga Video"
        AppLanguage.INDONESIAN -> "Video"
    }

    fun navSettings(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.CEBUANO -> "Mga Setting"
        AppLanguage.INDONESIAN -> "Pengaturan"
    }

    // --- Top Bar & Quick Tools ---
    fun topSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Indo Remix • Fullbeat • Slow"
        AppLanguage.CEBUANO -> "Indo Remix • Fullbeat • Slow"
        AppLanguage.INDONESIAN -> "Indo Remix • Fullbeat • Slow"
    }

    fun quickDuplicates(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Duplicates"
        AppLanguage.CEBUANO -> "Mga Doble"
        AppLanguage.INDONESIAN -> "Duplikat"
    }

    fun quickClassifier(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Classifier"
        AppLanguage.CEBUANO -> "Pag-ila sa Remix"
        AppLanguage.INDONESIAN -> "Klasifikasi"
    }

    fun quickNotifications(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Notifications"
        AppLanguage.CEBUANO -> "Mga Notipikasyon"
        AppLanguage.INDONESIAN -> "Notifikasi"
    }

    // --- Notification Dialog & Management ---
    fun notifDialogTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Playback Notifications"
        AppLanguage.CEBUANO -> "Mga Notipikasyon sa Pagpatugtog"
        AppLanguage.INDONESIAN -> "Notifikasi Pemutaran"
    }

    fun notifStatusActive(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Active & Enabled"
        AppLanguage.CEBUANO -> "Aktibo ug Gitugotan"
        AppLanguage.INDONESIAN -> "Aktif & Diizinkan"
    }

    fun notifStatusDisabled(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Disabled / Needs Permission"
        AppLanguage.CEBUANO -> "Wala Gitugotan / Kinahanglan og Pagtugot"
        AppLanguage.INDONESIAN -> "Nonaktif / Butuh Izin"
    }

    fun notifDescription(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "JNMF uses media notifications to provide lock-screen controls, track progress, and playback buttons while music plays in the background."
        AppLanguage.CEBUANO -> "Ang JNMF naggamit og mga notipikasyon sa pagpatugtog aron makahatag og mga kontrol sa lock-screen ug background samtang nagpatugtog sa musika."
        AppLanguage.INDONESIAN -> "JNMF menggunakan notifikasi media untuk menyediakan kontrol layar kunci, info lagu, dan tombol putar saat musik berjalan di latar belakang."
    }

    fun notifGrantPermission(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Grant Notification Permission"
        AppLanguage.CEBUANO -> "Ihatag ang Pagtugot sa Notipikasyon"
        AppLanguage.INDONESIAN -> "Berikan Izin Notifikasi"
    }

    fun notifOpenSettings(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "System Notification Settings"
        AppLanguage.CEBUANO -> "Mga Setting sa Notipikasyon sa Sistema"
        AppLanguage.INDONESIAN -> "Pengaturan Notifikasi Sistem"
    }

    fun notifTest(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Test Notification"
        AppLanguage.CEBUANO -> "Sulayi ang Notipikasyon"
        AppLanguage.INDONESIAN -> "Uji Notifikasi"
    }

    fun close(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.CEBUANO -> "Isira"
        AppLanguage.INDONESIAN -> "Tutup"
    }

    // --- Search & Library ---
    fun searchPlaceholder(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Search songs, artists, remix..."
        AppLanguage.CEBUANO -> "Pangitaa ang mga kanta, artist, remix..."
        AppLanguage.INDONESIAN -> "Cari lagu, artis, remix..."
    }

    fun sortBy(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sort By"
        AppLanguage.CEBUANO -> "Han-ay sumala sa"
        AppLanguage.INDONESIAN -> "Urutkan Berdasarkan"
    }

    fun sortTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Title"
        AppLanguage.CEBUANO -> "Ulohan"
        AppLanguage.INDONESIAN -> "Judul"
    }

    fun sortArtist(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Artist"
        AppLanguage.CEBUANO -> "Mag-aawit"
        AppLanguage.INDONESIAN -> "Artis"
    }

    fun sortDate(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Date Added"
        AppLanguage.CEBUANO -> "Petsa sa Pagdugang"
        AppLanguage.INDONESIAN -> "Tanggal Ditambahkan"
    }

    fun sortDuration(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Duration"
        AppLanguage.CEBUANO -> "Gitas-on"
        AppLanguage.INDONESIAN -> "Durasi"
    }

    fun grantAudioAccess(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Grant Audio Access"
        AppLanguage.CEBUANO -> "Ihatag ang Pagtugot sa Audio"
        AppLanguage.INDONESIAN -> "Berikan Akses Audio"
    }

    fun permissionDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "JNMF requires storage permission to scan your Indo Remix and local audio files."
        AppLanguage.CEBUANO -> "Kinahanglan ang pagtugot sa storage aron ma-scan ang imong mga Indo Remix ug lokal nga mga kanta."
        AppLanguage.INDONESIAN -> "JNMF memerlukan izin penyimpanan untuk memindai lagu Indo Remix dan audio lokal Anda."
    }

    fun refreshLibrary(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Refresh Library"
        AppLanguage.CEBUANO -> "I-refresh ang Library"
        AppLanguage.INDONESIAN -> "Segarkan Koleksi"
    }

    fun noSongsFound(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No music files found on device."
        AppLanguage.CEBUANO -> "Walay nakit-an nga mga kanta sa aparato."
        AppLanguage.INDONESIAN -> "Tidak ada file musik yang ditemukan di perangkat."
    }

    fun tracksCount(count: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "$count tracks"
        AppLanguage.CEBUANO -> "$count ka mga kanta"
        AppLanguage.INDONESIAN -> "$count lagu"
    }

    fun playNow(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Play Now"
        AppLanguage.CEBUANO -> "Patugtoga Karon"
        AppLanguage.INDONESIAN -> "Putar Sekarang"
    }

    fun addToPlaylist(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Add to Playlist"
        AppLanguage.CEBUANO -> "Idugang sa Playlist"
        AppLanguage.INDONESIAN -> "Tambah ke Daftar Putar"
    }

    fun hideFromLibrary(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Hide from Library"
        AppLanguage.CEBUANO -> "Itago gikan sa Library"
        AppLanguage.INDONESIAN -> "Sembunyikan dari Koleksi"
    }

    fun deleteFile(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Delete File"
        AppLanguage.CEBUANO -> "Papasa ang File"
        AppLanguage.INDONESIAN -> "Hapus File"
    }

    // --- Player & Now Playing ---
    fun nowPlaying(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Now Playing"
        AppLanguage.CEBUANO -> "Gipatugtog Karon"
        AppLanguage.INDONESIAN -> "Sedang Diputar"
    }

    fun queueTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Playback Queue"
        AppLanguage.CEBUANO -> "Han-ay sa Pagpatugtog"
        AppLanguage.INDONESIAN -> "Antrean Pemutaran"
    }

    fun reverseQueue(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Reverse Queue Mode"
        AppLanguage.CEBUANO -> "Baliktad nga Han-ay"
        AppLanguage.INDONESIAN -> "Mode Balik Antrean"
    }

    fun reverseQueueOn(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Reverse Order Active"
        AppLanguage.CEBUANO -> "Aktibo ang Baliktad nga Han-ay"
        AppLanguage.INDONESIAN -> "Urutan Balik Aktif"
    }

    fun reverseQueueOff(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Standard Queue Order"
        AppLanguage.CEBUANO -> "Standard nga Han-ay"
        AppLanguage.INDONESIAN -> "Urutan Standar"
    }

    fun shuffle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Shuffle"
        AppLanguage.CEBUANO -> "Isagol"
        AppLanguage.INDONESIAN -> "Acak"
    }

    fun repeat(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Repeat"
        AppLanguage.CEBUANO -> "Sublia"
        AppLanguage.INDONESIAN -> "Ulangi"
    }

    fun speed(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Speed"
        AppLanguage.CEBUANO -> "Kadasigon"
        AppLanguage.INDONESIAN -> "Kecepatan"
    }

    // --- Equalizer & Sound Engine ---
    fun eqTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Equalizer & Sound Engine"
        AppLanguage.CEBUANO -> "Equalizer & Makina sa Tingog"
        AppLanguage.INDONESIAN -> "Equalizer & Mesin Suara"
    }

    fun eqMaster(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Master Sound Processing"
        AppLanguage.CEBUANO -> "Pangunang Pagproseso sa Tingog"
        AppLanguage.INDONESIAN -> "Pemrosesan Suara Master"
    }

    fun eqActive(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Attached to JNMF Audio Track"
        AppLanguage.CEBUANO -> "Konektado sa JNMF Audio Track"
        AppLanguage.INDONESIAN -> "Terhubung ke Trek Audio JNMF"
    }

    fun eqIdle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Waiting for playback session"
        AppLanguage.CEBUANO -> "Naghulat sa sesyon sa pagpatugtog"
        AppLanguage.INDONESIAN -> "Menunggu sesi pemutaran"
    }

    fun bassHoreg(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Bass Horeg & Sub-Bass"
        AppLanguage.CEBUANO -> "Bass Horeg & Sub-Bass"
        AppLanguage.INDONESIAN -> "Bass Horeg & Sub-Bass"
    }

    fun stereoWidth(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Stereo Width / Spatial 3D"
        AppLanguage.CEBUANO -> "Gilapdon sa Stereo / 3D Spasial"
        AppLanguage.INDONESIAN -> "Lebar Stereo / 3D Spasial"
    }

    fun trebleClarity(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Treble & Vocal Clarity"
        AppLanguage.CEBUANO -> "Katin-aw sa Tingog & Treble"
        AppLanguage.INDONESIAN -> "Kejernihan Treble & Vokal"
    }

    fun soundPresets(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sound Presets"
        AppLanguage.CEBUANO -> "Mga Preset sa Tingog"
        AppLanguage.INDONESIAN -> "Preset Suara"
    }

    fun resetDefault(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Reset to Default"
        AppLanguage.CEBUANO -> "I-reset sa Default"
        AppLanguage.INDONESIAN -> "Reset ke Awal"
    }

    // --- Playlists ---
    fun playlistsTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "My Playlists"
        AppLanguage.CEBUANO -> "Akong mga Playlist"
        AppLanguage.INDONESIAN -> "Daftar Putar Saya"
    }

    fun favoritesTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Favorite Songs"
        AppLanguage.CEBUANO -> "Mga Paboritong Kanta"
        AppLanguage.INDONESIAN -> "Lagu Favorit"
    }

    fun newPlaylist(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "New Playlist"
        AppLanguage.CEBUANO -> "Bag-ong Playlist"
        AppLanguage.INDONESIAN -> "Daftar Putar Baru"
    }

    fun enterPlaylistName(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Enter playlist name"
        AppLanguage.CEBUANO -> "Isulat ang ngalan sa playlist"
        AppLanguage.INDONESIAN -> "Masukkan nama daftar putar"
    }

    fun create(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Create"
        AppLanguage.CEBUANO -> "Paghimo"
        AppLanguage.INDONESIAN -> "Buat"
    }

    fun cancel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.CEBUANO -> "Kanselahon"
        AppLanguage.INDONESIAN -> "Batal"
    }

    fun rename(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Rename"
        AppLanguage.CEBUANO -> "Usba ang Ngalan"
        AppLanguage.INDONESIAN -> "Ganti Nama"
    }

    fun delete(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.CEBUANO -> "Papasa"
        AppLanguage.INDONESIAN -> "Hapus"
    }

    fun emptyPlaylists(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No playlists yet. Create one above!"
        AppLanguage.CEBUANO -> "Wala pay mga playlist. Paghimo usa sa ibabaw!"
        AppLanguage.INDONESIAN -> "Belum ada daftar putar. Buat satu di atas!"
    }

    // --- Duplicate Detection ---
    fun duplicatesTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Duplicate & Similar Songs"
        AppLanguage.CEBUANO -> "Mga Doble ug Parehas nga Kanta"
        AppLanguage.INDONESIAN -> "Lagu Duplikat & Serupa"
    }

    fun scanSimilarAudio(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Scan Similar Audio (~80%)"
        AppLanguage.CEBUANO -> "I-scan ang Parehas nga Audio (~80%)"
        AppLanguage.INDONESIAN -> "Pindai Audio Serupa (~80%)"
    }

    fun duplicatesDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Detect duplicate tracks and distinguish different remix arrangements without deleting automatically."
        AppLanguage.CEBUANO -> "Pangitaa ang mga parehas nga kanta ug ilha ang mga lain-laing remix nga dili awtomatikong papason."
        AppLanguage.INDONESIAN -> "Deteksi lagu duplikat dan bedakan aransemen remix tanpa menghapus secara otomatis."
    }

    fun similarity(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Similarity"
        AppLanguage.CEBUANO -> "Pagkaparehas"
        AppLanguage.INDONESIAN -> "Kemiripan"
    }

    fun confirmDeleteTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Confirm File Deletion"
        AppLanguage.CEBUANO -> "Kumpirmaha ang Pagpapas sa File"
        AppLanguage.INDONESIAN -> "Konfirmasi Penghapusan File"
    }

    fun confirmDeleteMsg(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Are you sure you want to delete this file from storage?"
        AppLanguage.CEBUANO -> "Sigurado ka ba nga gusto nimong papason kini nga file gikan sa storage?"
        AppLanguage.INDONESIAN -> "Apakah Anda yakin ingin menghapus file ini dari penyimpanan?"
    }

    // --- Classifier ---
    fun classifierTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Remix Classification"
        AppLanguage.CEBUANO -> "Pag-ila sa Klase sa Remix"
        AppLanguage.INDONESIAN -> "Klasifikasi Remix"
    }

    fun classifierDesc(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Automatic tempo and beat sequence detection for Fullbeat and Slow tracks."
        AppLanguage.CEBUANO -> "Awtomatikong pag-ila sa tempo ug ritmo alang sa Fullbeat ug Slow nga mga kanta."
        AppLanguage.INDONESIAN -> "Deteksi otomatis tempo dan ketukan untuk lagu Fullbeat dan Slow Remix."
    }

    // --- Videos ---
    fun videosTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Local Videos"
        AppLanguage.CEBUANO -> "Mga Video sa Aparato"
        AppLanguage.INDONESIAN -> "Video Lokal"
    }

    fun noVideosFound(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No videos found on device."
        AppLanguage.CEBUANO -> "Walay nakit-an nga video sa aparato."
        AppLanguage.INDONESIAN -> "Tidak ada video ditemukan di perangkat."
    }

    fun refreshVideos(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Refresh Videos"
        AppLanguage.CEBUANO -> "I-refresh ang mga Video"
        AppLanguage.INDONESIAN -> "Segarkan Video"
    }

    fun fullscreen(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Fullscreen"
        AppLanguage.CEBUANO -> "Tibuok Screen"
        AppLanguage.INDONESIAN -> "Layar Penuh"
    }

    fun exitFullscreen(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Exit Fullscreen"
        AppLanguage.CEBUANO -> "Gawas sa Tibuok Screen"
        AppLanguage.INDONESIAN -> "Keluar Layar Penuh"
    }

    fun closePlayer(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Close Video Player"
        AppLanguage.CEBUANO -> "Isira ang Video Player"
        AppLanguage.INDONESIAN -> "Tutup Pemutar Video"
    }

    fun moreVideos(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "More Videos"
        AppLanguage.CEBUANO -> "Dugang nga mga Video"
        AppLanguage.INDONESIAN -> "Video Lainnya"
    }

    // --- Settings ---
    fun settingsTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "JNMF Settings"
        AppLanguage.CEBUANO -> "Mga Setting sa JNMF"
        AppLanguage.INDONESIAN -> "Pengaturan JNMF"
    }

    fun settingsSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Appearance, language, and audio management"
        AppLanguage.CEBUANO -> "Panagway, pinulongan, ug pagdumala sa audio"
        AppLanguage.INDONESIAN -> "Tampilan, bahasa, dan manajemen audio"
    }

    fun uiColorTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "UI Accent Color"
        AppLanguage.CEBUANO -> "Kolor sa Tema"
        AppLanguage.INDONESIAN -> "Warna Aksen UI"
    }

    fun uiColorSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "1-tap instant theme switch with live visual preview"
        AppLanguage.CEBUANO -> "1-tap nga pag-ilis sa kolor nga dili mopahunong sa musika"
        AppLanguage.INDONESIAN -> "1-ketuk ganti tema instan tanpa memutus musik"
    }

    fun langTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Language Selection"
        AppLanguage.CEBUANO -> "Pagpili sa Pinulongan"
        AppLanguage.INDONESIAN -> "Pilihan Bahasa"
    }

    fun langSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Instant interface translation without stopping playback"
        AppLanguage.CEBUANO -> "Diha-diha nga paghubad nga dili mopahunong sa musika"
        AppLanguage.INDONESIAN -> "Terjemahan antarmuka instan tanpa menghentikan pemutaran"
    }

    fun hiddenSongsTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Hidden & Excluded Songs"
        AppLanguage.CEBUANO -> "Gitago nga mga Kanta"
        AppLanguage.INDONESIAN -> "Lagu Tersembunyi & Dikecualikan"
    }

    fun manage(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Manage"
        AppLanguage.CEBUANO -> "Dumalaha"
        AppLanguage.INDONESIAN -> "Kelola"
    }

    fun restore(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Restore"
        AppLanguage.CEBUANO -> "Ibalik"
        AppLanguage.INDONESIAN -> "Pulihkan"
    }

    fun back(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Back"
        AppLanguage.CEBUANO -> "Balik"
        AppLanguage.INDONESIAN -> "Kembali"
    }

    fun creditsTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Developer & Project Credits"
        AppLanguage.CEBUANO -> "Mga Kredito sa Naghimo & Proyekto"
        AppLanguage.INDONESIAN -> "Kredit Pengembang & Proyek"
    }

    fun creditsSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "JNMF Native Android Music Engine"
        AppLanguage.CEBUANO -> "JNMF Lumad nga Android Music Engine"
        AppLanguage.INDONESIAN -> "Mesin Musik Android Asli JNMF"
    }

    // Credits labels (Preserved exactly as requested)
    fun creditAppName(): String = "JNMF Music Player"
    fun creditCompile(): String = "Compile by JUSSAR GOLDEN"
    fun creditCreateBy(): String = "CREATE BY"
    fun creditChatgpt(): String = "ChatGPT advisor"
    fun creditAiStudio(): String = "Google AI Studio: app developer"
    fun creditTargetDevice(): String = "Xiaomi Redmi 10C (220333QAG)"
    fun creditAudioFocus(): String = "Indo Remix, Fullbeat, Slow Remix"

    // --- Loading & Progress ---
    fun loadingScanningMusic(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Scanning Music Library"
        AppLanguage.CEBUANO -> "Nagsusi sa Library sa Musika"
        AppLanguage.INDONESIAN -> "Memindai Koleksi Musik"
    }

    fun loadingMusicReady(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Music Library Ready"
        AppLanguage.CEBUANO -> "Andam na ang Library sa Musika"
        AppLanguage.INDONESIAN -> "Koleksi Musik Siap"
    }

    fun loadingScanningVideos(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Scanning Local Videos"
        AppLanguage.CEBUANO -> "Nagsusi sa mga Video sa Aparato"
        AppLanguage.INDONESIAN -> "Memindai Video Lokal"
    }

    fun loadingVideosReady(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Video Library Ready"
        AppLanguage.CEBUANO -> "Andam na ang Library sa Video"
        AppLanguage.INDONESIAN -> "Koleksi Video Siap"
    }
}
