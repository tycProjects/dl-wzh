# Update 1.14.9 — Perbaikan shell Shizuku, diagnosa meminfo, dan pencarian dalam aplikasi

## Shell Shizuku
- Penyebab "process hasn't exited": exit code diambil sebelum status proses Shizuku siap. Sekarang
  `ShizukuShell` menunggu hingga 3 detik sampai exit code bisa dibaca, dan membaca stdout/stderr
  sampai selesai (bukan 500 ms) sebelum hasil dikembalikan.
- Ditambahkan `ShizukuShell.pressKey(keycode)` untuk tombol sistem tetap (hanya dipakai internal,
  tidak pernah dari teks model).

## Diagnosa meminfo
- Perintah tidak lagi memakai pipa `grep`. Sebelumnya grep yang tidak menemukan baris cocok membuat
  seluruh perintah tampak gagal. Penyaringan ringkasan (TOTAL, Heap, Graphics, dll.) sekarang
  dilakukan di Kotlin lewat `DiagnosticPolicy.summarizeMeminfo`.
- Buffer keluaran diperbesar, batas waktu 30 detik, dan status gagal hanya dilaporkan bila stdout
  kosong.

## Pencarian di dalam aplikasi (Auralys dan lainnya)
- Tombol cari dikenali dari label, deskripsi konten, hint, atau id view (mis. "Search Button"),
  hanya di bagian atas layar.
- Kolom pencarian bisa diisi walaupun bukan kolom pesan utama. `InputPicker` tetap dipakai untuk
  kolom pesan chat.
- Setelah mengetik, Enter ditekan lewat accessibility (ACTION_IME_ENTER); bila gagal, cadangan
  Shizuku `input keyevent 66`.
- Setiap langkah dilaporkan terpisah (aplikasi terbuka, tombol cari, pengetikan, Enter) agar
  balasan tidak mengklaim pencarian yang tidak terjadi.

## Putar lagu
- `playInApp` menunggu hasil pencarian muncul (hingga beberapa detik) sebelum mengetuk.
- Kolom pencarian tidak lagi dianggap sebagai hasil (`clickByText` melewati kolom edit).

## Catatan validasi
- Test ditambahkan/diperbarui di `DiagnosticPolicyTest`.
- Build Gradle dan unit test belum dijalankan di lingkungan ini. Uji di perangkat untuk Auralys
  dan Shizuku.
- Pengenalan suara masih memakai `id-ID` (SpeechInput, AiComposerActivity). Belum diubah.
