# Android Island 1.4.68

## Hemat baterai dan CPU
- Loop animasi berulang (gelombang musik, ticker lirik, timer panggilan, dan drift) berhenti saat layar mati dan lanjut saat layar nyala. Dikendalikan oleh `ScreenState`, yang diperbarui receiver SCREEN_ON / SCREEN_OFF.
- Latar artwork blur sekarang statis. Loop drift 30 Hz dimatikan di expanded, banner WhatsApp, dan banner panggilan. Artwork tetap diburamkan dengan downscale ke 36 px dan dicache per gambar, jadi biaya blurnya kecil.
- Pembacaan ulang status like (heart) hanya berjalan saat island expanded, dan berhenti saat collapse. Sebelumnya berjalan terus. Sekarang dipicu perubahan state, bukan timer.

## Yang sudah ada sebelumnya (tidak diubah)
- Baseline profile sudah terpasang di build rilis. Berkas profilnya dibuat dengan menjalankan benchmark di perangkat.
- Artwork blur sudah dicache dan dibuat di thread latar belakang.

## Belum dikerjakan
- Debounce notifikasi: notifikasi tidak di-polling dan tiap notifikasi sudah diproses sekali, jadi belum ada lonjakan yang perlu diredam.
- Lazy loading ExpandedIsland, CallIsland, dan LyricsRepository: komponen ini sudah hanya dikomposisi saat island tampil, jadi sudah lazy.
- Hardware layer (LAYER_TYPE_HARDWARE): itu konsep View. Aplikasi ini memakai Compose.

## Catatan
Kode belum dikompilasi di lingkungan ini. Jalankan build debug dan kirim log bila ada error.
