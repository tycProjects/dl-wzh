# UPDATE 1.17.1 — STAGE 73 — RAM TURUN & TIDAK NAIK LAGI SAAT DIPAKAI LAMA

Tanggal: 20 Nov 2026

## Bukti dari perangkat (dumpsys meminfo sebelum versi ini)

```
TOTAL PSS  187 MB   TOTAL RSS 300 MB
Graphics    90 MB   ← GL mtrack 80 MB (buffer jendela + tekstur GPU)
Native Heap 35 MB
Java Heap   19 MB   ← sehat (13 MB terpakai / 13 MB bebas)
```

Java tidak bocor. Sumber utamanya adalah **permukaan (surface) jendela overlay yang
tulis**: sistem menyimpan ±4 buffer grafis per jendela, masing-masing
`lebar x tinggi x 4 byte`. Semakin tinggi jendela, semakin besar RAM grafisnya.

## 1. Akar masalah: jendela gambar membesar dan tidak pernah mengecil lagi

- `drawWindowFixedHeightPx()` selama ini memesan ruang panel AI **kapan saja** AI
  aktif — panel tutup pun (tinggi terukur, atau minimal 420 dp). Akibatnya begitu
  panel AI pernah dipakai, jendela gambar bertahan ~1750-2200 px tingginya
  permanen → ±30-40 MB buffer GL, ditambah jendela sentuh yang ikut memenuhi
  layar saat panel terbuka. Itulah 80 MB "Graphics" di dumpsys.
- Sisi Compose (IslandRoot) memesan 400-420 dp yang sama untuk hitungan cap stack
  dan ruang jelly — jadi kedua sisi sengaja "sepakat" memboroskan.

### Perbaikan: satu aturan bersama, hanya saat panel terbuka

- `WindowTuning.aiPanelWindowReserveDp(...)` baru: ruang panel AI dihitung **hanya
  saat panel benar-benar terbuka** (IslandActivity.Ai), dan saat terbuka memakai
  CAP bersama yang sama dari Stage 72 (bukan tinggi terukur) — jadi tinggi jendela
  berubah tepat satu kali per buka dan satu kali per tutup, tanpa bergoyang saat
  panel mengukur dirinya, dan panel tidak mungkin melebihi ruang yang dipesan.
- Tiga tempat kini memakai aturan yang sama: jendela gambar (service), budget
  jendela + cap stack WA di IslandRoot, dan hitungan ruang jelly di IslandRoot.
  Panel tutup = budget kartu media/banner (jauh lebih pendek), panel buka =
  cap panel. Tidak ada lagi status "sekali pakai panel, jendela gemuk selamanya".

### Angka (layar 800 dp, kapsul 48 dp, jelly MEDIUM)

| Kondisi | Sebelum | Sesudah |
| --- | --- | --- |
| Panel AI tutup / idle / media | ~582 dp jendela (±31 MB GL) | ~384 dp jendela (±20 MB GL) |
| Panel AI terbuka | ~742 dp (sama) | ~742 dp (tidak berubah, dipakai interaktif) |
| Layar mati | jendela dibuang (sudah ada) | tetap dibuang |

## 2. Grace 1,2 detik saat panel ditutup — anti "terpotong"

- Karena jendela kini mengecil begitu panel ditutup, ada jebakan lama: morph
  penutup (pegas + jelly crest) masih menggambar panel tua yang tinggi selama
  ±0,5-0,8 detik. Kalau jendela menyusut di saat yang sama, panel terpotong datar
  di bawah (bug kelas "terpotong" yang dulu sering terjadi).
- Solusinya: `applyLayout` menyimpan waktu penutupan panel dan menahan tinggi lama
  selama 1,2 detik, lalu turun ke budget keadaan tertutup. Membuka panel langsung
  membesarkan jendela (jendela lebih besar dari isi tidak pernah memotong apa pun).

## 3. Trim cache saat idle berlaku untuk semua mode render

- Sebelumnya pelepasan cache idle (30 detik layar mati) dan deep trim (2 menit)
  hanya jalan di Mode Hemat (Low-End). Mode Normal sengaja menahan cache lensa GPU
  + artwork selamanya.
- Sekarang keduanya jalan di semua tier: cache artwork kedaluwarsa dilepas setelah
  30 detik layar mati, bitmap lensa GPU dan efek lensa dilepas setelah 2 menit.
  Semua dibangun ulang otomatis saat dipakai lagi (satu kali render, bukan per
  frame), jadi tidak ada perubahan tampilan dan tidak ada biaya baterai.

## Yang sengaja TIDAK diubah

- OCR ML Kit sudah melepas engine setelah 60 detik idle (Stage 69) — Native Heap
  tidak ditambah.
- Galeri gambar AI sudah terbatas ke bubble yang terlihat (LazyColumn) — bitmap
  bubble di luar layar otomatis dilepas.
- Floor tinggi jendela (0.48 layar), lebar lane, dan semua garansi "tidak
  terpotong" Stage 68/70/72 dipertahankan apa adanya.
- Fitur premium, layout mewah, dan resolusi visual tidak dipangkas.

## Verifikasi

- Audit import seluruh file Kotlin dijalankan ulang (skrip audit_imports.py);
  temuan tinggal false-positive yang sudah diverifikasi (argumen bernama, string
  prompt, stdlib). Tidak ada missing import baru.
- Urutan deklarasi di IslandRoot diperiksa manual (screenHdpNow kini dideklarasikan
  sebelum dipakai hitungan aiCardDp).
