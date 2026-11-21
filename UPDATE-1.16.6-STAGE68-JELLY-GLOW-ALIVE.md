# UPDATE 1.16.6 — STAGE 68: Buka/Tutup Anti-Terpotong + Jelly Mulus + UI Bercahaya Hidup

Tanggal: 20 Nov 2026

Permintaan: *"Perbaiki juga animasi ketika buka tutup panel dan banner agar tidak ada yg
terpotong dalam keadaan apapun dan smooth jelly, dan juga tambahkan setiap details besar dan
kecil ada animasi yg hidup dan smooth yg enak dilihat, dan tambahkan efek glow di segala ui
dan pastikan tetap hemat ram dan baterai."*

## 1. Anti-terpotong "dalam keadaan apapun" — satu sumber kebenaran jendela

Akar masalah yang ditemukan: rumusan cadangan tinggi jendela gambar (service) dan rumusan
cadangan jelly di dalam scene (IslandRoot) ditulis terpisah dan tidak pernah identik. Akibatnya
jelur jelly (squash-and-stretch saat spring melewati ukuran istirahat) bisa melewati batas bawah
permukaan jendela dan **terpotong datar** — paling parah pada panel AI yang tinggi.

- **`core/WindowTuning.kt`** kini memegang SATU rumusan bersama:
  - `jellyOvershootFraction(elasticity)` — puncak overshoot spring per elastisitas
    (NONE 2% / LOW 6% / MEDIUM 11% / HIGH 22%; dihitung dari `exp(-πζ/√(1-ζ²))` tabel damping
    `jellyDamping()`, dibulatkan ke atas).
  - `jellyRoomReserveDp(grow, overshoot)` — ruang wajib di bawah dasar panel untuk jelur jelly.
  - `drawWindowRequiredHeightDp(...)` — tinggi wajib jendela: capsule + offset +
    max(kartu, konten banner termasuk baris balasan cepat) + 56 + slack jelly.
- **Service** (`IslandAccessibilityService.drawWindowFixedHeightPx`) kini menghitung tinggi
  jendela lewat rumusan bersama itu; **IslandRoot** menghitung angka yang persis sama, lalu
  menurunkan `roomBelowDp` (ruang bawah yang DIJAMIN jendela) dan menjepit amplitudo jelur
  `jellyPrMax = 1 + min(room/grow, 0.25)` — bukan lagi angka kaku 1.25.
- **Hasil (diverifikasi numerik, `scripts/stage68_jelly_room_audit.py`)**:
  - Kartu media/stack: jelly **penuh** (prMax 1.25) di semua tier, termasuk landscape.
  - Panel AI setinggi 0.94 layar: jendela mencapai batas layar, amplitudo menyesuaikan diri
    (≈1.09) — jelur tetap terlihat hidup tapi **ujungnya selalu berada di dalam jendela**.
  - Batas lama `coerceAtMost(120dp)` yang memotong panel tinggi dihapus.
- Banner: jendela sekarang juga menghitung konten banner + baris balasan cepat (±56 dp) sehingga
  memperluas input balasan tidak pernah mendorong kartu keluar dari permukaan gambar.

## 2. Buka/tutup makin "smooth jelly"

- **Entrance banner lebih hidup**: `bannerEnterSpring()` dari damping 0.78 → **0.72** (stiffness
  340) — drop keluar dari kapsul dengan satu rebound lembut. Overshoot ~4% tetap di dalam
  jepitan `bannerEnterT` (1.04 / skala 1.008), jadi licin tanpa pernah menabrak kapsul.
- **Tutup banner** (tuck ke kapsul, baik swipe maupun timeout) tetap satu kurva yang sama
  (`BANNER_TUCK_MS` + `BannerTuckEasing`) — kartu "tersedot" rapi, tidak pernah tersisa bayangan.
- **Buka/tutup panel**: morf kapsul→panel dan panel→kapsul kini dijamin kontinu dengan jelur yang
  muat ruangnya (poin 1); skala lebar tetap dijepit agar tidak keluar lajur scene.

## 3. Detail besar & kecil kini beranimasi

- **Banner panggilan** (sebelumnya muncul tanpa animasi): kini masuk dengan kaskade lembut
  (`staggerIn`), avatar pop spring, dan **tombol jawab berdenyut hijau** saat panggilan masuk.
- **Bubble & hint AI**: setiap pesan/hint datang dengan pop angkat-duduk sekali (`enterOnce`,
  satu spring lembut — bukan loop).
- **Tombol tutup AI**: tekan kini squash jelly + halo cyan (`pressScaleWith` + `pressHalo`).
- **Titik status kapsul** (panggilan/AI/WhatsApp): halo bernapas draw-phase (`BreathDot`).
- **Marker WhatsApp belum dibaca**: dua cincin glow bernapas mengikuti jam shell bersama.

## 4. Efek glow di segala UI (tanpa shader, tanpa blur)

- **Rim neon panel AI bernapas**: `aiBreathRim` — garis tepi cyan→violet dengan alpha yang
  bernapas mengikuti jam panel (draw-phase).
- **Aura WhatsApp**: `breathingAura` — tiga lapis stroke hijau lembut bernapas di sekeliling
  kartu banner (digambar di belakang kaca; hanya bagian luar terlihat; lajur jendela menyediakan
  ≥8 dp margin sehingga aura tidak pernah terpotong).
- **Aura banner panggilan**: hijau kuat saat panggilan masuk, tenang saat panggilan berlangsung.
- **Halo tombol jawab**, halo titik status, glow header AI (inti + titik status), jejak langkah
  berdenyut — semuanya di seluruh UI yang ada.

## 5. Tetap hemat RAM & baterai

- **Dua loop `rememberInfiniteTransition` terakhir di panel AI dihapus** (denyut inti header +
  denyut jejak langkah) — kini dibaca dari jam drift bersama di **draw phase**, jadi panel AI
  tidak menjalankan satu pun recomposition loop.
- Semua animasi baru: satu `rememberDriftPhase` bersama per permukaan (33/66/100 ms sesuai tier,
  berhenti saat layar non-interaktif via `ScreenState.awaitInteractive`), tanpa shader, tanpa
  blur, tanpa bitmap baru — hanya stroke/circle yang murah.
- **Gate penuh**: Mode Low-End (staticGlass) mematikan seluruh path aura/halo (bahkan modifier-nya
  tidak dipasang); "Remove animations" (skala animator 0) membekukan denyut ke keadaan tenang
  statis; Low Power melanjutkan tanpa loop tambahan. Runtime, RAM, dan baterai tidak merasakan
  fitur ini — visual saja.

## 6. Verifikasi

- `tools/verify_package.py` — **PASS 67 checks** (ditambah pemeriksaan regresi stage 68:
  rumusan bersama wajib dipakai kedua sisi, cap 120 dp lama dilarang kembali, helper hidup wajib
  ada, AiPanel bebas infinite transition).
- **Audit numerik** `scripts/stage68_jelly_room_audit.py` — semua skenario (tier 0/1/2, media,
  stack tinggi, panel AI 0.94 layar, landscape, elastisitas HIGH/LOW, non-jelly): **puncak panel
  selalu di dalam jendela gambar**; kartu normal memperoleh jelly penuh 1.25.
- Kompilasi Compose tetap tidak tersedia di sandbox (tanpa Android SDK); semua perubahan UI
  diaudit statis: nilai baru dibaca di layer/draw phase, tidak ada state komposisi baru per frame,
  dan semua perubahan mengikuti pola draw-phase yang sudah terbukti di stage 67.
