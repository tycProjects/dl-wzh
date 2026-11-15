# Update 1.9.2 (draft, not compiled) - cutout picker, jelly bounce, 3 domes

## Pengaturan
- Cutout: picker "Ukuran capsule" sekarang menampilkan SEMUA profil: Otomatis, iPhone,
  Punch hole, Waterdrop, Notch lebar, Kustom. Sebelumnya Punch hole dan Notch lebar sudah ada di
  enum tapi tidak pernah ditampilkan. Tampilan dibagi 2 baris (perRow = 3).

## Motion
- Jelly (OPTION_3, spring): damping HIGH 0.58 -> 0.50, MEDIUM 0.66 -> 0.60, LOW 0.74 -> 0.70.
  Overshoot naik dari sekitar 10 % menjadi sekitar 16 % pada HIGH. Squash dibatasi di IslandRoot,
  jadi cek apakah pantulan terlalu besar di perangkat.

## Liquid glass
- Dome: tiga bukit (domeField) dipersempit (k 1.10/1.45/1.25 -> 1.60/2.20/1.90) agar tiga
  gelembung terlihat terpisah, tidak menyatu jadi satu gumpalan. Sumber geometri tetap, tanpa bitmap baru.

## Belum dikerjakan (perlu sesi lanjutan)
- Setting jumlah dome (1-3) belum ada. Perlu uniform baru di LENS_AGSL + kunci cache di bakeLensBitmap.
- Edge lens di SettingsRepository masih dipaksa 0 (sengaja).
- Animasi ala ColorOS 17 per skenario belum ditambahkan.
- Log menunjukkan SETTINGS_APPLIED dan DRAW_LAYOUT berulang saat slider digeser (sekitar 30 kali
  dalam 4 detik). Ini penyebab utama jank saat mengatur, perlu debounce di slider.

Catatan: belum di-compile dan belum diuji di perangkat (tidak ada Android SDK di lingkungan ini).
