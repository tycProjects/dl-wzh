# UPDATE 1.16.5 — STAGE 67: Deep Search Agent + Berita WIB Rapi + Panel AI Neon

Tanggal: 20 Nov 2026

## 1. Berita RSS dirapikan otomatis (tanpa model, tanpa risiko karangan)

Permintaan pengguna: *"Rapikan data hasil pencarian berita RSS menjadi daftar bernomor yang
bersih. Hapus semua tag HTML mentah, duplikasi judul, dan URL panjang. Waktu GMT diubah ke WIB."*

Format keluaran persis per item (deterministik, bukan hasil tulisan model):

```
1. Judul Berita — Nama Media (20 Nov 2026, 15:13 WIB) | Baca Sumber
```

- **File baru `ai/NewsFormatter.kt`** (pure Kotlin, 10 unit test):
  - Konversi GMT/UTC → **WIB (GMT+7, Asia/Jakarta)** dengan `DateTimeFormatter` locale `id`
    ("20 Nov 2026, 15:13 WIB").
  - Strip **tag HTML mentah + entity** (`&amp;`, `&#39;`, `&#x…;`, …) dari setiap field — lapisan
    terakhir sebelum tampil di UI.
  - Hapus **duplikasi judul**: kunci judul ternormalisasi, sufiks "- Media" dari sindikasi dibuang,
    dan headline sindikasi hampir identik digabung (Jaccard ≥ 0,85, hanya untuk judul ≥ 5 kata;
    varian judul yang paling baru yang dipertahankan).
  - **URL panjang tidak pernah tampil**: setiap item berakhir `| [Baca Sumber](url)` yang
    dirender bubble chat sebagai label pendek yang bisa ditekan (buka browser, hanya https).
  - Urutan terbaru dulu, maksimal 10 item, judul dipangkas 220 karakter.
- **NoozraNews** direfactor: `latestWithFailures()` + `topicSearchArticles()` mengembalikan artikel
  terstruktur; output lama untuk model tetap identik (diuji ulang `NoozraNewsTest`).
- **WebSearch.searchNews()**: satu pass jaringan menghasilkan artikel terstruktur + ringkasan model.
- **`runGroundedNewsSearch` (AiSession)** kini menampilkan daftar rapi NewsFormatter langsung ke
  pengguna — model tidak ikut menulis berita, jadi tidak ada headline yang bisa dikarang.

## 2. Agen AI bisa "cari lebih dalam" (deep research)

- **File baru `ai/ResearchPlanner.kt`**: deteksi permintaan riset ("cari lebih dalam", "telusuri",
  "riset", "kronologi", "deep dive", …), ekspansi kata kunci, dan budget khusus.
- **`WebSearch.deepResearch()`**: pencarian multi-ronde — ronde 1 pencarian utama, ronde 2–3 kata
  kunci varian (kronologi fakta / penjelasan resmi / data terverifikasi), lalu **pembacaan langsung
  3 halaman sumber** via `PageReader` (kutipan isi + tanggal publikasi halaman), digabung dan
  dibatasi 20 ribu karakter sebagai satu observasi.
- **Aksi baru `search_deep`**: `AiAction.WebSearch(query, deep=true)` — didukung IntentResolver
  (`{"type":"search_deep","query":"…"}`) dan router lokal (`"cari lebih dalam X"` langsung deep).
- **Budget agent loop diperluas** hanya untuk riset: 26 langkah / 300 detik (normal tetap 20 / 180s)
  lewat `AgentLoopPolicy.budgetFor()` + `decide(maxSteps, maxMs)`.
- **SearchQueryPolicy** mengenali prefiks observasi deep (`Hasil pencarian mendalam untuk "…"`)
  agar query sama tidak diulang dalam satu giliran.
- **System prompt** (AiActionProtocol + systemPrompt runtime): aturan DEEPER SEARCH (kapan memanggil
  search_deep, sintesis lintas ronde, sebutkan sumber per temuan) dan aturan format berita WIB.

## 3. Panel AI dirombak: neon / robotik / bercahaya, tanpa tumpang tindih

Semua di `island/AiPanel.kt`, hanya memengaruhi panel AI (Liquid Glass island tetap utuh):

- **Substrat AMOLED**: gradasi `#12172A → #0A0D16` + **cahaya neon di dalam tepi** (cyan di atas,
  violet di bawah; digambar via drawBehind, tidak keluar dari clip, tanpa blur).
- **Header robotik**: inti neon berdenyut (halo 3 lapis), judul `ASISTEN AI` monospace + tracking,
  status `SIAP / MEMPROSES` dengan titik hijau/magenta, tombol tutup bergaya neon.
- **Pembatas gelombang One UI-style** di bawah header: dua lapis sinus (cyan + violet) bergerak
  lambat 9 detik, hanya menggambar (draw-phase), di jalur 12dp miliknya sendiri — **nol tumpang
  tindih layout**.
- **Chip neon outline**: fill transparan 12%, border berwarna, ikon berwarna, skala tekan pegas
  (`pressScaleWith`) — masih grid 3 kolom lebar sama (tidak bisa saling menimpa).
- **Bubble**: asisten diberi border tipis neon; pesan berisi tautan dirender sebagai
  AnnotatedString — `[Baca Sumber](url)` jadi label cyan yang bisa ditekan; teks ke TTS dibersihkan
  dari URL.
- **Indikator sibuk baru** (`BusyLine`): tiga titik neon bernapas + teks ("Memproses…", "Membaca
  layar…", "Memahami perintah…") menggantikan bubble "Mengetik…".
- **StepTrail** jejak langkah recolor ke neon cyan (gagal tetap ember).

### Logika animasi dirapikan (smooth, hemat, tanpa tabrakan)

- Semua animasi baru **read-in-draw-phase** (satu `rememberDriftPhase` per elemen, tick 33/66/100ms
  sesuai tier perangkat, berhenti saat layar non-interaktif) — **tidak ada recomposition**.
- Satu-satunya `rememberInfiniteTransition` tinggal pulse inti header (1 float).
- **Gate global**: `ANIMATOR_DURATION_SCALE == 0` (reduced motion) atau tier `staticGlass` → semua
  denyut/gelombang beku rapi, UI tetap berfungsi.
- Anggaran per frame panel: 4 animasi kecil independen (inti, gelombang header, titik sibuk,
  pulse steptrail lama) — semuanya node mungil, tidak saling menimpa.

## 4. Bar progres musik: animasi bercahaya bergelombang

`ProgressSection` (ExpandedIsland): saat lagu diputar, bagian bar yang terisi membawa **gelombang
bercahaya** — lapisan halo lebar + crest terang bergerak (fase 3,2 detik) sepanjang bagian terisi,
plus **glow bernapas di playhead**. Amplitudo membesar lembut saat play dan mereda saat pause
(spring), menggantikan band glow lama yang datar. Semuanya draw-phase (tidak recompose), otomatis
mati di tier staticGlass/low-end dan saat pause; **gestur tap/drag seek tidak berubah**.

## 5. Verifikasi

- `tools/verify_package.py` — PASS 54 checks (nama test boundary lama disinkronkan).
- **Kompilasi nyata + JUnit**: NewsFormatter/ResearchPlanner/NoozraNews/WebSearch/PageReader +
  test dikompilasi `kotlinc 2.0.21` dan dijalankan di JVM — **19/19 test PASS**
  (termasuk konversi WIB "08:13 GMT → 15:13 WIB", dedup sindikasi, strip HTML, format persis
  `Judul — Media (… WIB) | [Baca Sumber](url)`).
- Compose UI tidak bisa dikompilasi di sandbox ini (tanpa Android SDK); audit animasi dilakukan
  statis: semua nilai baru dibaca di draw phase, tanpa state baru per frame di level komposisi.
