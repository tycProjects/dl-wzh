package com.example.ui.i18n

import com.example.data.model.RadioItemType
import com.example.data.repository.AppLanguage

/**
 * Trilingual (English, Cebuano, Indonesian) localization for the JNMF Radio DJ Scheduler.
 */
object RadioDjStrings {

    fun navRadioDj(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Radio DJ"
        AppLanguage.CEBUANO -> "Radio DJ"
        AppLanguage.INDONESIAN -> "Radio DJ"
    }

    fun screenTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Radio DJ Scheduler"
        AppLanguage.CEBUANO -> "Radio DJ Scheduler"
        AppLanguage.INDONESIAN -> "Penjadwal Radio DJ"
    }

    fun screenSubtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Automated Broadcast Program Builder"
        AppLanguage.CEBUANO -> "Awtomatikong Programa sa Radyo"
        AppLanguage.INDONESIAN -> "Penyusun Program Siaran Otomatis"
    }

    fun itemTypeLabel(type: RadioItemType, lang: AppLanguage): String = when (type) {
        RadioItemType.MUSIC -> when (lang) {
            AppLanguage.ENGLISH -> "Music Track"
            AppLanguage.CEBUANO -> "Kanta"
            AppLanguage.INDONESIAN -> "Lagu Musik"
        }
        RadioItemType.ADVERTISEMENT -> when (lang) {
            AppLanguage.ENGLISH -> "Radio Ad"
            AppLanguage.CEBUANO -> "Patalastas (Ad)"
            AppLanguage.INDONESIAN -> "Iklan Radio"
        }
        RadioItemType.NEWS -> when (lang) {
            AppLanguage.ENGLISH -> "News Segment"
            AppLanguage.CEBUANO -> "Balita (News)"
            AppLanguage.INDONESIAN -> "Berita Radio"
        }
        RadioItemType.RADIO_PROGRAM -> when (lang) {
            AppLanguage.ENGLISH -> "Radio Program"
            AppLanguage.CEBUANO -> "Programa sa Radyo"
            AppLanguage.INDONESIAN -> "Program Radio"
        }
        RadioItemType.ANNOUNCEMENT -> when (lang) {
            AppLanguage.ENGLISH -> "DJ Announcement"
            AppLanguage.CEBUANO -> "Anunsyo sa DJ"
            AppLanguage.INDONESIAN -> "Pengumuman DJ"
        }
        RadioItemType.JINGLE -> when (lang) {
            AppLanguage.ENGLISH -> "Station ID / Jingle"
            AppLanguage.CEBUANO -> "Station ID / Jingle"
            AppLanguage.INDONESIAN -> "ID Stasiun / Jingle"
        }
        RadioItemType.INSERT -> when (lang) {
            AppLanguage.ENGLISH -> "Audio Insert"
            AppLanguage.CEBUANO -> "Audio Insert"
            AppLanguage.INDONESIAN -> "Sisipan Audio"
        }
    }

    fun itemTypeBadgeShort(type: RadioItemType, lang: AppLanguage): String = when (type) {
        RadioItemType.MUSIC -> when (lang) {
            AppLanguage.ENGLISH -> "MUSIC"
            AppLanguage.CEBUANO -> "MUSIKA"
            AppLanguage.INDONESIAN -> "MUSIK"
        }
        RadioItemType.ADVERTISEMENT -> when (lang) {
            AppLanguage.ENGLISH -> "RADIO AD"
            AppLanguage.CEBUANO -> "AD"
            AppLanguage.INDONESIAN -> "IKLAN"
        }
        RadioItemType.NEWS -> when (lang) {
            AppLanguage.ENGLISH -> "NEWS"
            AppLanguage.CEBUANO -> "BALITA"
            AppLanguage.INDONESIAN -> "BERITA"
        }
        RadioItemType.RADIO_PROGRAM -> when (lang) {
            AppLanguage.ENGLISH -> "PROGRAM"
            AppLanguage.CEBUANO -> "PROGRAMA"
            AppLanguage.INDONESIAN -> "PROGRAM"
        }
        RadioItemType.ANNOUNCEMENT -> when (lang) {
            AppLanguage.ENGLISH -> "DJ VOICE"
            AppLanguage.CEBUANO -> "DJ ANUNSYO"
            AppLanguage.INDONESIAN -> "SUARA DJ"
        }
        RadioItemType.JINGLE -> "JINGLE / ID"
        RadioItemType.INSERT -> "INSERT"
    }

    fun djCrossfadeTimelineActive(seconds: Int, overlapStr: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "DJ Mode $seconds s crossfade active (Music overlap -$overlapStr)"
        AppLanguage.CEBUANO -> "DJ Mode $seconds s crossfade aktibo (Music overlap -$overlapStr)"
        AppLanguage.INDONESIAN -> "Mode DJ crossfade $seconds dtk aktif (Overlap musik -$overlapStr)"
    }

    fun djCrossfadeTimelineOff(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "DJ Mode OFF (Normal sequential playback)"
        AppLanguage.CEBUANO -> "DJ Mode OFF (Normal nga tul-id nga pagpatugtog)"
        AppLanguage.INDONESIAN -> "Mode DJ NONAKTIF (Pemutaran berurutan normal)"
    }

    fun savedSchedulesHeader(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "SAVED RADIO PROGRAMS"
        AppLanguage.CEBUANO -> "NA-SAVE NGA MGA PROGRAMA SA RADYO"
        AppLanguage.INDONESIAN -> "PROGRAM RADIO TERSIMPAN"
    }

    fun newSchedule(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "New Schedule"
        AppLanguage.CEBUANO -> "Bag-ong Iskedyul"
        AppLanguage.INDONESIAN -> "Jadwal Baru"
    }

    fun createScheduleTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Create Radio Program"
        AppLanguage.CEBUANO -> "Paghimo og Programa sa Radyo"
        AppLanguage.INDONESIAN -> "Buat Program Radio"
    }

    fun scheduleNameLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Program Name (e.g., JNMF Morning Radio)"
        AppLanguage.CEBUANO -> "Ngalan sa Programa (pananglitan, JNMF Morning Radio)"
        AppLanguage.INDONESIAN -> "Nama Program (mis. JNMF Morning Radio)"
    }

    fun renameScheduleTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Rename Radio Program"
        AppLanguage.CEBUANO -> "Usba ang Ngalan sa Programa"
        AppLanguage.INDONESIAN -> "Ganti Nama Program Radio"
    }

    fun duplicateSchedule(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Duplicate"
        AppLanguage.CEBUANO -> "Kopyaha"
        AppLanguage.INDONESIAN -> "Duplikat"
    }

    fun deleteScheduleTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Delete Radio Schedule"
        AppLanguage.CEBUANO -> "Papasa ang Iskedyul sa Radyo"
        AppLanguage.INDONESIAN -> "Hapus Jadwal Radio"
    }

    fun deleteScheduleConfirm(name: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Delete radio schedule '$name'? Normal playlists and local audio files will NOT be affected."
        AppLanguage.CEBUANO -> "Papason ba ang iskedyul nga '$name'? Ang normal nga mga playlist ug mga audio file DILI maapektuhan."
        AppLanguage.INDONESIAN -> "Hapus jadwal radio '$name'? Daftar putar biasa dan file audio lokal TIDAK akan terpengaruh."
    }

    fun scheduleAlreadyExists(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "A radio schedule with this name already exists."
        AppLanguage.CEBUANO -> "Aduna nay iskedyul sa radyo nga ingon niini og ngalan."
        AppLanguage.INDONESIAN -> "Jadwal radio dengan nama ini sudah ada."
    }

    fun emptySchedulesHint(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "No radio programs yet. Create a schedule like 'JNMF Morning Radio' or 'JNMF Night Program' to build your broadcast lineup."
        AppLanguage.CEBUANO -> "Wala pay programa sa radyo. Paghimo og iskedyul sama sa 'JNMF Morning Radio' aron magsugod."
        AppLanguage.INDONESIAN -> "Belum ada program radio. Buat jadwal seperti 'JNMF Morning Radio' untuk menyusun siaran Anda."
    }

    fun emptyTimelineHint(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "This schedule has no items yet. Add Music Tracks, Radio Ads, DJ Announcements, or Jingles below."
        AppLanguage.CEBUANO -> "Wala pay sulod kini nga iskedyul. Pagdugang og Kanta, Patalastas, Anunsyo sa DJ, o Jingle."
        AppLanguage.INDONESIAN -> "Jadwal ini belum memiliki item. Tambahkan Lagu, Iklan Radio, Pengumuman DJ, atau Jingle di bawah."
    }

    fun programStartTimeLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Program Start"
        AppLanguage.CEBUANO -> "Sugod sa Programa"
        AppLanguage.INDONESIAN -> "Mulai Program"
    }

    fun changeStartTime(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Set Start Time"
        AppLanguage.CEBUANO -> "Itakda ang Oras"
        AppLanguage.INDONESIAN -> "Atur Jam Mulai"
    }

    fun estimatedFinishLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Est. Finish"
        AppLanguage.CEBUANO -> "Banabana Human"
        AppLanguage.INDONESIAN -> "Perkiraan Selesai"
    }

    fun totalDurationLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Total Runtime"
        AppLanguage.CEBUANO -> "Kinatibuk-ang Oras"
        AppLanguage.INDONESIAN -> "Total Durasi"
    }

    fun estimatedDisclaimer(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Estimated Timeline: Start & finish times assume continuous sequential playback. Pauses, manual skips, or missing files will shift actual broadcast times."
        AppLanguage.CEBUANO -> "Banabana nga Oras: Ang oras sa pagsugod ug pagkahuman gibase sa tul-id nga pagpatugtog. Ang pag-pause, paglaktaw, o nawala nga file makausab sa tinuod nga oras."
        AppLanguage.INDONESIAN -> "Estimasi Waktu: Jam mulai & selesai mengasumsikan pemutaran berurutan tanpa henti. Jeda, lompatan manual, atau file hilang akan menggeser waktu aktual."
    }

    fun startRadio(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Start Radio"
        AppLanguage.CEBUANO -> "Sugdi ang Radyo"
        AppLanguage.INDONESIAN -> "Mulai Radio"
    }

    fun stopRadio(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Stop"
        AppLanguage.CEBUANO -> "Hunong"
        AppLanguage.INDONESIAN -> "Berhenti"
    }

    fun statusOnAir(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "ON AIR"
        AppLanguage.CEBUANO -> "ON AIR"
        AppLanguage.INDONESIAN -> "SIARAN AKTIF"
    }

    fun statusPaused(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "PAUSED"
        AppLanguage.CEBUANO -> "NAKA-PAUSE"
        AppLanguage.INDONESIAN -> "DIJEDA"
    }

    fun statusPreviewing(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "PREVIEW"
        AppLanguage.CEBUANO -> "PREVIEW"
        AppLanguage.INDONESIAN -> "PRATINJAU"
    }

    fun statusCompleted(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "PROGRAM ENDED"
        AppLanguage.CEBUANO -> "HUMAN NA ANG PROGRAMA"
        AppLanguage.INDONESIAN -> "PROGRAM SELESAI"
    }

    fun statusStandby(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "STANDBY"
        AppLanguage.CEBUANO -> "ANDAM"
        AppLanguage.INDONESIAN -> "SIAGA"
    }

    fun currentItemHeader(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "NOW BROADCASTING"
        AppLanguage.CEBUANO -> "GIPATUGTOG KARON"
        AppLanguage.INDONESIAN -> "SEDANG DIPUTAR"
    }

    fun upcomingItemHeader(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "UP NEXT"
        AppLanguage.CEBUANO -> "SUNOD NGA IPATUGTOG"
        AppLanguage.INDONESIAN -> "BERIKUTNYA"
    }

    fun endOfProgramLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "End of Scheduled Program"
        AppLanguage.CEBUANO -> "Katapusan sa Programa"
        AppLanguage.INDONESIAN -> "Akhir Program Terjadwal"
    }

    fun addProgramItem(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Add Item"
        AppLanguage.CEBUANO -> "Pagdugang"
        AppLanguage.INDONESIAN -> "Tambah Item"
    }

    fun replaceProgramItemTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Replace Scheduled Item"
        AppLanguage.CEBUANO -> "Ilisi ang Item sa Iskedyul"
        AppLanguage.INDONESIAN -> "Ganti Item Terjadwal"
    }

    fun addProgramItemTitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Add to Radio Program"
        AppLanguage.CEBUANO -> "Idugang sa Programa sa Radyo"
        AppLanguage.INDONESIAN -> "Tambahkan ke Program Radio"
    }

    fun chooseItemTypeLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "1. Select Broadcast Item Type"
        AppLanguage.CEBUANO -> "1. Pilia ang Klase sa Audio"
        AppLanguage.INDONESIAN -> "1. Pilih Jenis Item Siaran"
    }

    fun pickExternalAudioButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Browse Local Audio File (Ad / Voice / Jingle)"
        AppLanguage.CEBUANO -> "Pangita og Audio File sa Storage (Ad / Jingle)"
        AppLanguage.INDONESIAN -> "Pilih File Audio Lokal (Iklan / Suara / Jingle)"
    }

    fun pickFromLibraryHeader(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "2. Or Pick from Music Library"
        AppLanguage.CEBUANO -> "2. O Pagpili gikan sa Music Library"
        AppLanguage.INDONESIAN -> "2. Atau Pilih dari Koleksi Musik"
    }

    fun previewItem(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Preview"
        AppLanguage.CEBUANO -> "Paminawa"
        AppLanguage.INDONESIAN -> "Pratinjau"
    }

    fun stopPreview(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Stop Preview"
        AppLanguage.CEBUANO -> "Hunonga ang Preview"
        AppLanguage.INDONESIAN -> "Hentikan Pratinjau"
    }

    fun replaceItem(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Replace"
        AppLanguage.CEBUANO -> "Ilisi"
        AppLanguage.INDONESIAN -> "Ganti"
    }

    fun changeType(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Change Type"
        AppLanguage.CEBUANO -> "Usba ang Klase"
        AppLanguage.INDONESIAN -> "Ubah Jenis"
    }

    fun removeItem(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Remove"
        AppLanguage.CEBUANO -> "Tangtanga"
        AppLanguage.INDONESIAN -> "Hapus"
    }

    fun timelineSectionHeader(count: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "PROGRAM TIMELINE ($count ITEMS)"
        AppLanguage.CEBUANO -> "TIMELINE SA PROGRAMA ($count KA ITEM)"
        AppLanguage.INDONESIAN -> "LINIMASA PROGRAM ($count ITEM)"
    }

    fun estimatedSlotRange(start: String, end: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "$start – Est. $end"
        AppLanguage.CEBUANO -> "$start – Banabana $end"
        AppLanguage.INDONESIAN -> "$start – Est. $end"
    }

    fun skippedMissingNotice(title: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Skipped unavailable audio: '$title'"
        AppLanguage.CEBUANO -> "Gilaktawan ang dili ma-access nga audio: '$title'"
        AppLanguage.INDONESIAN -> "Melewati audio tidak tersedia: '$title'"
    }

    fun allItemsUnavailableError(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Cannot play radio schedule: all scheduled audio files are missing or unreadable."
        AppLanguage.CEBUANO -> "Dili mapatugtog ang iskedyul: tanang audio file nawala o dili mabasa."
        AppLanguage.INDONESIAN -> "Tidak dapat memutar jadwal radio: semua file audio hilang atau tidak terbaca."
    }

    fun notifBadge(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "JNMF Radio DJ"
        AppLanguage.CEBUANO -> "JNMF Radio DJ"
        AppLanguage.INDONESIAN -> "Radio DJ JNMF"
    }

    fun hourLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Hour"
        AppLanguage.CEBUANO -> "Takna"
        AppLanguage.INDONESIAN -> "Jam"
    }

    fun minuteLabel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Minute"
        AppLanguage.CEBUANO -> "Minuto"
        AppLanguage.INDONESIAN -> "Menit"
    }

    fun gainLevel(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Sound Level"
        AppLanguage.CEBUANO -> "Gikusgon sa Tingog"
        AppLanguage.INDONESIAN -> "Tingkat Suara"
    }

    fun autoSchedule(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Auto-Scheduled Playback"
        AppLanguage.CEBUANO -> "Awtomatikong Pagpatugtog sa Iskedyul"
        AppLanguage.INDONESIAN -> "Pemutaran Otomatis Terjadwal"
    }

    fun autoScheduleHint(time: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Starts transmission automatically at $time"
        AppLanguage.CEBUANO -> "Magsugod ang pagsibya sa $time"
        AppLanguage.INDONESIAN -> "Memulai transmisi otomatis pada $time"
    }

    fun timeAnnouncement(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Time Announcement / Top of Hour"
        AppLanguage.CEBUANO -> "Pahibalo sa Oras / Ibabaw sa Takna"
        AppLanguage.INDONESIAN -> "Pengumuman Waktu / Awal Jam"
    }

    fun timeAnnouncementSettings(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Time Announcement Settings"
        AppLanguage.CEBUANO -> "Mga Setting sa Pahibalo sa Oras"
        AppLanguage.INDONESIAN -> "Pengaturan Pengumuman Waktu"
    }

    fun hourlyAnnouncement(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Hourly (Top of the hour :00)"
        AppLanguage.CEBUANO -> "Kada takna (Ibabaw sa takna :00)"
        AppLanguage.INDONESIAN -> "Tiap jam (Awal jam :00)"
    }

    fun customMinutes(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Custom minutes (e.g. 0, 15, 30, 45)"
        AppLanguage.CEBUANO -> "Kinaiya nga mga minuto (pananglitan 0, 15, 30, 45)"
        AppLanguage.INDONESIAN -> "Menit kustom (mis. 0, 15, 30, 45)"
    }

    fun selectAnnouncementFile(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Choose Announcement Audio / Video"
        AppLanguage.CEBUANO -> "Pilia ang Audio / Video sa Pahibalo"
        AppLanguage.INDONESIAN -> "Pilih Audio / Video Pengumuman"
    }

    fun testAnnouncement(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Test Announcement"
        AppLanguage.CEBUANO -> "Sulayi ang Pahibalo"
        AppLanguage.INDONESIAN -> "Uji Pengumuman"
    }

    fun quickAdd(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Quick Add"
        AppLanguage.CEBUANO -> "Dali nga Idugang"
        AppLanguage.INDONESIAN -> "Tambah Cepat"
    }

    fun alreadyInSchedule(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "In Schedule"
        AppLanguage.CEBUANO -> "Naa na sa Iskedyul"
        AppLanguage.INDONESIAN -> "Dalam Jadwal"
    }

    fun edit(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Settings"
        AppLanguage.CEBUANO -> "Mga Setting"
        AppLanguage.INDONESIAN -> "Pengaturan"
    }

    fun addToRadioPlaylist(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Add to Radio Playlist"
        AppLanguage.CEBUANO -> "Idugang sa Radio Playlist"
        AppLanguage.INDONESIAN -> "Tambah ke Daftar Radio"
    }

    fun selectRadioPlaylist(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Select Radio Playlist"
        AppLanguage.CEBUANO -> "Pilia ang Radio Playlist"
        AppLanguage.INDONESIAN -> "Pilih Daftar Putar Radio"
    }

    fun createNewRadioPlaylist(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "+ Create New Radio Playlist"
        AppLanguage.CEBUANO -> "+ Paghimo og Bag-ong Radio Playlist"
        AppLanguage.INDONESIAN -> "+ Buat Daftar Putar Radio Baru"
    }

    fun newRadioPlaylistName(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "New Radio Playlist Name"
        AppLanguage.CEBUANO -> "Ngalan sa Bag-ong Radio Playlist"
        AppLanguage.INDONESIAN -> "Nama Daftar Radio Baru"
    }

    fun multiSelect(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Select Songs"
        AppLanguage.CEBUANO -> "Pilia ang mga Kanta"
        AppLanguage.INDONESIAN -> "Pilih Lagu"
    }

    fun selectedSongsCount(count: Int, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "$count selected"
        AppLanguage.CEBUANO -> "$count napili"
        AppLanguage.INDONESIAN -> "$count dipilih"
    }

    fun selectAll(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Select All"
        AppLanguage.CEBUANO -> "Pilia Tanan"
        AppLanguage.INDONESIAN -> "Pilih Semua"
    }

    fun deselectAll(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Deselect All"
        AppLanguage.CEBUANO -> "Wagtanga ang Pili"
        AppLanguage.INDONESIAN -> "Batalkan Semua"
    }

    fun addSelectedToRadio(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "ADD TO RADIO PLAYLIST"
        AppLanguage.CEBUANO -> "IDUGANG SA RADIO PLAYLIST"
        AppLanguage.INDONESIAN -> "TAMBAHKAN KE DAFTAR RADIO"
    }

    fun addedSingleSongSuccess(songTitle: String, playlistName: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Added \"$songTitle\" to $playlistName"
        AppLanguage.CEBUANO -> "Nidugang \"$songTitle\" sa $playlistName"
        AppLanguage.INDONESIAN -> "Ditambahkan \"$songTitle\" ke $playlistName"
    }

    fun addedBatchSongsSuccess(count: Int, playlistName: String, lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Added $count songs to $playlistName"
        AppLanguage.CEBUANO -> "Nidugang $count ka mga kanta sa $playlistName"
        AppLanguage.INDONESIAN -> "Menambahkan $count lagu ke $playlistName"
    }

    fun alreadyInRadioPlaylist(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Song is already in this Radio Playlist"
        AppLanguage.CEBUANO -> "Ang kanta naa na niining Radio Playlist"
        AppLanguage.INDONESIAN -> "Lagu sudah ada di Daftar Radio ini"
    }

    fun manualSaveButton(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.CEBUANO -> "I-save"
        AppLanguage.INDONESIAN -> "Simpan"
    }

    fun manualSaveSuccess(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Radio Playlist saved successfully"
        AppLanguage.CEBUANO -> "Malamposong na-save ang Radio Playlist"
        AppLanguage.INDONESIAN -> "Daftar Putar Radio berhasil disimpan"
    }

    fun dragToReorder(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> "Drag to reorder"
        AppLanguage.CEBUANO -> "Guyora aron usbon ang han-ay"
        AppLanguage.INDONESIAN -> "Tarik untuk mengubah urutan"
    }
}
