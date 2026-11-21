# UPDATE 1.17.0 — STAGE 72 — AGENT UILETU, CHIP-FIX, GLOW STATIS, NOTCH ASLI

Tanggal: 20 Nov 2026

## 1. AI Agent lebih ulet dan pintar (fokus utama)

### 1a. Macet tidak lagi membatalkan tugas — diarahkan ke tahap berikutnya
- `AgentRecoveryPolicy` baru: saat model mengusulkan ulang aksi yang sudah
  berhasil, tugas TIDAK langsung dihentikan. Sekarang model menerima
  `completedReplayPrompt` — tangga tahap standar perintah perangkat (buka
  aplikasi → buka UI pencarian → ketik → Enter → hasil pertama), apa saja
  yang sudah selesai, dan larangan keras mengetuk kontrol yang sama dua kali.
  Batas eskalasi: 3 arahan (sebelumnya langsung berhenti pada percobaan ke-2).
- Bila semua arahan habis, jawaban akhir adalah LAPORAN KEMAJUAN
  (`completedReplayReport`): langkah yang sudah dikerjakan, perintah asli, dan
  ajakan "kirim lanjut" untuk meneruskan dari tahap berikutnya — bukan
  pembatalan sepihak.
- Anggaran ketekunan naik: 24 langkah (dari 20), 4 menit (dari 3), tantangan
  anti-menyerah 4 kali (dari 3).

### 1b. Jalur cepat menerima "buka X lalu cari Y lalu enter"
- `AgentTaskParser.parse` sebelumnya MENOLAK perintah ini karena mewajibkan
  klausa "klik hasil pertama". Akibatnya perintah seperti
  "Buka tiktok lalu cari preset lalu enter" jatuh ke jalur lambat klik-"Cari"
  yang macet. Sekarang bentuk buka+cari+enter diproses lewat jalur cepat
  `search_in_app` yang deterministik (buka UI pencarian → ketik → Enter
  otomatis). Flag `openFirstResult` dipisah agar perintah tanpa "hasil
  pertama" tidak otomatis membuka hasil.
- Perintah "cari preset lalu enter" sekarang selesai dalam SATU aksi
  deterministik + observasi hasil, bukan ronde klik manual yang rawan salah.

### 1c. Pintar memilih ikon pencarian yang benar
- PENGETAHUAN LOKASI PENCARIAN per aplikasi (`SearchControlMatcher`): TikTok
  (tanpa tab Cari di bar bawah — lewat Profil, ikon kaca pembesar kanan atas),
  Instagram (tab Jelajahi di bar bawah), YouTube/WhatsApp/Telegram (kanan
  atas), Shopee/Tokopedia/Lazada/Play Store (bar cari atas), Spotify/X (tab
  bar bawah). Dimasukkan sebagai pesan sistem saat perintah menyebut aplikasi
  — versi praktis dari "cari dulu di internet biasanya letak ikon pencarian".
- Strategi baru di protokol (`SEARCH AFFORDANCE STRATEGY`): urutan preferensi
  (1) kolom cari yang terlihat, (2) ikon kaca pembesar kanan atas, (3) bar
  cari halaman utama, (4) tab Cari bar bawah hanya bila itu memang entry
  pencariannya — selalu verifikasi layar setelah mengetuk; bila yang terbuka
  halaman lain, tekan back dan pilih kandidat berikutnya. Jangan pernah
  mengetuk label "Cari" yang sama dua kali.
- `tapSearchButton` kini memprioritaskan kandidat PALING KANAN di pita atas
  (ikon kaca pembesar kanan atas menang atas label "Cari" kiri milik halaman
  saran/hasil).

## 2. Bug: chip AI tidak bisa disentuh saat panel mentok — DIPERBAIKI
- Akar masalah: tiga sisi memakai batas tinggi BERBEDA. Panel mengukur diri
  sampai ±0.94 layar, host jendela memakai cap sendiri, dan jendela SENTUH
  berhenti di 0.78 layar — saat slider tinggi panel didorong mentok, deretan
  chip berada DI BAWAH permukaan sentuh, jadi sentuhan jatuh ke sistem.
- Sekarang ada SATU fungsi bersama `WindowTuning.aiPanelCapDp(screen,
  capsuleH, offsetY)` yang dipakai panel (clamp tinggi diri lewat
  `aiPanelMaxHeightDp`), host di IslandRoot, dan jendela sentuh di
  IslandAccessibilityService (kini menutup dari puncak jendela: kapsul +
  offset Y + panel). Chip dan kolom perintah selalu berada di dalam area
  sentuh, berapa pun tinggi yang dipilih.

## 3. Animasi bernapas dihapus → glow biasa; indikator tanpa animasi
- Panel AI: rim neon kini `aiStaticRim` (glow konstan, tanpa denyut), inti
  header glow konstan. Jam drift rim/header dihapus (hemat baterai).
- Indikator: titik status header, titik aktif jejak langkah (StepTrail), dan
  titik mengetik (NeonTypingDots) kini statis — tanpa halo bernapas sama
  sekali, sesuai permintaan khusus untuk indikator.
- Banner WhatsApp & panggilan: aura sekitar kartu kini glow konstan
  (`staticAura`) — warna dan kekuatan sama seperti kondisi puncak sebelumnya,
  tanpa denyut; tombol jawab panggilan memakai halo konstan. Dua jam drift
  banner dihapus — banner yang diam tidak membangunkan CPU lagi.
- Yang dipertahankan: gelombang One UI di bawah header AI (tetap animasi),
  dan seluruh animasi jelly/morph utama.

## 4. Notifikasi senyap & tidak penting tidak ditampilkan
- `isSilentAndUnimportant` di listener notifikasi: panggilan tidak pernah
  disaring; notifikasi kategori pesan/e-mail/sosial lolos kecuali kanalnya
  senyap (importance LOW ke bawah); kategori lain hanya ditampilkan bila
  setidaknya heads-up (HIGH). Notifikasi sinkronisasi/layanan/promosi senyap
  tidak lagi muncul di island.

## 5. Morph lebih smooth dan sedikit lebih lama
- Morph kapsul↔panel: 560/520 ms (dari 500/470), SPRING 300/320 stiffness
  (dari 340/360) — jatuhnya lebih likuid. Jelly SMOOTH 480 ms, Flow SMOOTH
  560 ms. Tuck banner 460 ms; entrance banner 0.74 damping / 320 stiffness.
- Semua tetap di-skala oleh slider kecepatan animasi pengguna.

## 6. Cutout iPhone berbentuk notch asli
- `IPhoneNotchCapsuleShape` digambar ulang: tepi atas penuh, sisi meruncing
  ke dalam saat turun, sudut bawah membulat lebar — tepi bawah ±68% lebar
  atas, jadi terlihat seperti segitiga membulat menghadap ke bawah (siluet
  notch iPhone asli), bukan setengah kapsul lurus. Berlaku di idle notch dan
  kapsul compact.

## 7. Kerapian UI
- Ritme 8dp konsisten antara permukaan chat dan grid chip AI; jarak antar
  baris chip dirapikan.

## Verifikasi
- Audit import seluruh 44 file Kotlin dijalankan ulang; semua temuan adalah
  false-positive yang sudah diverifikasi (argumen bernama, string prompt,
  stdlib). Tidak ada missing import baru.
- Fitur premium, layout mewah, dan resolusi visual tidak dipangkas.
