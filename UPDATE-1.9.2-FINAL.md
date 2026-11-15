# Update 1.9.2 - cutout, jelly, 3 domes, settings, slider smoothness

## Cutout / punch hole
- Picker "Ukuran capsule" menampilkan semua profil: Otomatis, iPhone, Punch hole,
  Waterdrop, Notch lebar, Kustom (2 baris). Sebelumnya Punch hole & Notch lebar tidak tampil.
- Pergeseran horizontal dan vertikal ke lubang kamera sudah ada (CutoutProfile.autoShiftDp / autoShiftYDp).

## Jelly
- Spring OPTION_3 (Jelly): damping HIGH 0.58 -> 0.50, MEDIUM 0.66 -> 0.60, LOW 0.74 -> 0.70.
  Overshoot ~10 % -> ~16 % pada HIGH. Squash tetap dibatasi di IslandRoot.

## Tiga dome (liquid glass)
- Dome sekarang adalah lens. Sebelumnya dome hanya terlihat jika "Refraksi tepi" > 0, dan default-nya 0,
  jadi efek dome nyaris tidak tampil. Sekarang Kekuatan dome memberi lantai pada lens (0.55 x kekuatan),
  jadi dome tampil walau slider refraksi 0. Kekuatan dome = 0 mematikan lens sepenuhnya.
- Bentuk: tiga bukit yang terpisah. Jumlah dome 1 / 2 / 3 (uniform uDomeCount).
- Kelengkungan dome (uDomeRelief): 0 = bukit lebar dan landai, 1 = bukit tinggi dan sempit. Default 0.5 = bentuk asli.
- Bake lens di-cache per kombinasi (ukuran, strength, jumlah, kelengkungan, kekuatan). Bake tetap di background
  thread, dibatasi DeviceProfile.lensEnabled (perangkat lemah tetap tanpa lens).

## Pengaturan (semua sekarang terlihat di Kustomisasi > Efek liquid)
- Kekuatan dome, Kelengkungan dome, Jumlah dome, Refraksi tepi (edge lens).
- Sebelumnya Kekuatan/Kelengkungan dome ada di model tapi tidak punya kontrol UI, dan edge lens dipaksa 0.
- Edge lens default tetap 0; bisa dinyalakan sendiri dari slider.

## Smoothness slider
- Penyebab jank: setiap tick drag menulis ke settings, dan overlay menggambar ulang di setiap tulisan
  (di log: ~30 SETTINGS_APPLIED dalam 4 detik).
- Sekarang tulisan live dibatasi ~11 kali per detik saat drag; nilai akhir selalu dikirim saat jari dilepas.

## Versi
- versionCode 101, versionName 1.9.2. tools/verify_package.py diperbarui dan lulus.

## Belum termasuk (jujur)
- Animasi ala ColorOS 17 untuk semua skenario: TIDAK diimplementasikan di patch ini. Itu butuh
  spesifikasi gerak per skenario (buka/tutup, musik, panggilan, WhatsApp, notifikasi) dan pengujian di perangkat.
- Belum di-compile: tidak ada Android SDK/Gradle di lingkungan ini. Verifikasi yang dijalankan: verify_package.py
  (lulus) dan pemeriksaan keseimbangan kurung. Shader AGSL belum diuji di GPU.
- Nilai visual (pantulan jelly, bentuk tiga dome, lantai lens 0.55) perlu ditala di perangkat.

## Hotfix 1.9.3 - dome tidak tampil
- Penyebab: dome hanya digambar lewat lens (shader bake). Lens dimatikan di tier RAM rendah, di mode flatGlass
  (Exynos), dan saat mode hemat daya aktif. Akibatnya dome tidak pernah tampil di banyak perangkat.
- Perbaikan: dome sekarang digambar di edgeSheenOverlay dengan radial gradient (tanpa shader, tanpa bitmap).
  Tampil di semua tier dan mode. Kekuatan, kelengkungan, dan jumlah dome langsung mengubah gambar.
  Lens tetap ada sebagai refraksi tambahan.
- Kekuatan dome 0 mematikan dome dan lens; sheen 0 tidak lagi ikut mematikan dome.
- Versi 1.9.3 (versionCode 102).

## Hotfix 1.9.4 - pivot animasi pop-in
- Container island di-scale tanpa transformOrigin, jadi pop-in tumbuh dari tengah layar.
  Sekarang pivot di tepi atas tengah (di bawah notch/kapsul).
- Diketahui belum selesai (perlu sesi device): (a) saat membuka dari kapsul ke expanded, `appear` tidak
  berjalan karena `isVisible` tidak berubah, jadi tidak ada pop; (b) saat konten hilang, `appear.snapTo(0f)`
  memotong tanpa animasi keluar.

## Hotfix 1.9.5 - tepi kaca bergerigi (kotak-kotak)
- Penyebab: tepi kaca dipotong dengan canvas clipPath (tidak anti-alias di banyak GPU), sehingga tepi tampak bergerigi.
  Sekarang pakai Modifier.clip(RoundedCornerShape) yang anti-alias.
- Bitmap lens di-downscale terlalu kecil (divisor 3 di tier 1), lalu diperbesar, membuat tepi blocky.
  Divisor sekarang 2 (tier 1) dan 1 (tier 0). Biaya: bitmap lens sekitar 1.8x lebih besar, dibuat sekali per artwork.
- Bitmap lens digambar dengan FilterQuality.High (sebelumnya Medium).
- Batasan: ini perbaikan tepi, bukan seluruh realisme liquid glass. Keaslian pantulan/refraksi perlu dinilai di perangkat.

## Hotfix 1.9.6 - dome terlihat
- Highlight dome dinaikkan (alpha 0.22 -> 0.40) dan ditambah bayangan gelap di sisi kanan-bawah tiap dome,
  supaya tiga dome terbaca sebagai gelembung cembung di atas artwork apa pun.
- Batasan: ini belum setara kaca iPhone. iOS membiaskan konten di BELAKANG island secara real-time; di sini
  refraksi hanya dilakukan pada artwork, dan biayanya tinggi untuk dibuat per frame di perangkat RAM rendah.
