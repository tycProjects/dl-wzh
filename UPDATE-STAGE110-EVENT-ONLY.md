# Stage 110 (basis stage109) — belum dikompilasi, belum diuji di HP

Audit polling/loop latar belakang. Hanya satu polling nyata yang tersisa; sisanya sudah event atau one-shot.

1. WhatsAppNotificationListener: polling 2,5 dtk (`refreshTick`, jalan selama island expanded) dihapus. Pengganti event:
   callback `onExtrasChanged` pada semua sesi yang dipantau, callback metadata/playback yang sudah ada, pantauan notifikasi media,
   dan baca ulang sekali saat island dibuka (+450 ms, +1300 ms).
2. Controls: `MediaControllerCompat.Callback.onRepeatModeChanged` mendaftar saat sesi di-bind (dilepas saat sesi ganti/hilang);
   status repeat dibaca sekali saat bind. Tidak perlu cek berkala.
3. ShizukuShell.awaitExitCode: loop `Thread.sleep(50)` diganti `Process.waitFor(timeout)` (memblokir, tanpa bangun berkala).
4. ScreenState.awaitVisible() (layar nyala DAN tidak terkunci). Dipakai loop dekoratif: bar capsule, shine lirik, drift gelombang.
   Island disembunyikan di lock screen, jadi loop itu tidak lagi berdetak di layar kunci. Timer posisi lagu tetap memakai awaitInteractive.
5. IslandAccessibilityService: membuka app mana pun yang punya kartu notifikasi membersihkan kartu app itu
   (sebelumnya hanya WhatsApp, banner Telegram/Gmail dll menggantung). Pemeriksaan hanya scan daftar di memori.
6. AiSession: aturan "berhenti saat tujuan tercapai" kini juga untuk `PlayInApp` (sebelumnya hanya `PlaySong`).

Sengaja dipertahankan: loop bar capsule & timer posisi saat musik diputar (30 Hz / 250 ms, turun otomatis di Battery Saver),
debounce touch-bounds (satu kali per perubahan), burst cek lock 250/800/2000 ms setelah layar nyala/mati.

Belum dikerjakan: `breath` di CompactIsland (selalu null, kode mati kecil); melepas riwayat chat AI dari memori saat panel ditutup.

## Tambahan stage110: RAM (target utama proyek)
7. Temuan: penyusutan window ke capsule sengaja dimatikan (`windowShrinkAllowed = false`, karena realokasi surface bikin stutter).
   Trim RAM idle stage106 dipasang di jalur penyusutan itu, jadi di kondisi capsule (kondisi paling sering) tidak pernah jalan.
   Perbaikan: `IslandAccessibilityService.updateIdleTrim()` memulai satu timer 8 dtk saat state settle di COMPACT dan membatalkannya
   saat keluar dari COMPACT (dimulai sekali, tidak diundur oleh update media, tanpa loop). Setelah itu `NativeTrim.trimWhenIdle("capsule")`
   mengosongkan cache renderer, GC + trimHeap (maks tiap 5 menit), cache berita dan ikon.
   Cek: aktifkan Diagnostik sebentar, tunggu capsule diam ~10 dtk, cari log `IDLE_TRIM reason=capsule renderer=true`; lalu matikan Diagnostik dan ukur meminfo.
8. Dipertimbangkan lalu tidak dikerjakan: melepas riwayat chat AI dari memori (hanya 30 pesan teks dalam JSON kecil, hemat tak berarti, risiko alur muat/simpan).
