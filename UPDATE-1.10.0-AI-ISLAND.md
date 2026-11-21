# Update 1.10.0 — AI Island (Asisten AI di expanded island)

## Ringkasan

Asisten AI yang **hanya berjalan di state EXPANDED** island, sebagai pengganti Gemini/Samsung
Assistant dengan jejak RAM minimal:

- **Custom API OpenAI-compatible** — Base URL + API key + model bebas (OpenAI, OpenRouter, Groq,
  Together, Ollama/LM Studio lokal). Tidak ada SDK AI baru: client dibangun di atas
  `HttpURLConnection` + `org.json` (nol dependensi baru, nol kelas luar).
- **Streaming SSE** jawaban token-per-token + tombol stop; fallback non-streaming otomatis.
- **Kirim file** — SAF picker via activity proxy transparan: file teks dibaca isinya (maks 40 ribu
  karakter), gambar dikompres ≤1024px JPEG base64 untuk API vision. Riwayat menyimpan nama
  lampiran saja, isi lampiran tidak ditulis ke disk.
- **Panggil: tekan ganda tombol power** — ikon launcher kedua "AI Island" (activity-alias)
  muncul di laci aplikasi dan di pemilih One UI *Side button > Double press > Open app*.
  Long-press power tidak bisa diambil alih tanpa peran asisten sistem, jadi jalur resmi One UI
  dipakai; opsi gesture kapsul juga muncul otomatis di section Gesture (Long press → Asisten AI).
- **Analisis layar on-demand** — saat pengguna menekan chip **Analisis Layar** atau bertanya
  tentang layar, `ScreenReader` menangkap app aktif (label + package + activity), judul jendela,
  dan teks yang terlihat dari accessibility tree, lalu menyuntikkannya sebagai konteks system
  prompt. Tidak ada pembacaan diam-diam: tree hanya dijalan saat diminta.
- **Keyboard, bukan VoiceInteraction** — sesuai permintaan, `VoiceInteractionSession`/
  `RecognitionService` sengaja TIDAK diimplementasikan. Input 100% keyboard via panel chat;
  struktur sudah siap disambungkan nanti (headless VoiceInteractionService memanggil
  `IslandBus.pushAi()` adalah titik sambungnya).

## Komponen baru

| File | Peran |
|------|-------|
| `ai/AiClient.kt` | POST `{baseUrl}/chat/completions`, SSE streaming, cancel, pesan error manusiawi (ID) |
| `ai/AiSession.kt` | State chat (StateFlow), staging lampiran, riwayat JSON maks 30 pesan, deteksi permintaan analisis layar |
| `ai/ScreenReader.kt` | Cache foreground 3 string + capture accessibility tree on-demand |
| `ai/AiLauncherActivity.kt` | Trampoline transparan (trigger tekan ganda) + `AiFilePickerActivity` (proxy SAF) |
| `island/AiPanel.kt` | Kartu chat liquid-dark di expanded: bubble, chip aksi, input IME Send, tombol stop |
| `activity-alias .AiLauncherAlias` | Entri launcher "AI Island" untuk Side button One UI |

## Logika island yang disesuaikan

- `IslandActivity.Ai` baru (priority 15): `IslandBus.pushAi()` membuka EXPANDED; `closeAi()`
  mengembalikan media (COMPACT) atau banner WhatsApp, atau menyembunyikan island.
- Update musik & pesan WhatsApp baru **tidak lagi menggeser kartu AI**; pesan masuk tetap
  mengantri. Panggilan tetap prioritas tertinggi dan menutup AI.
- Tinggi kartu AI diukur (alur sama dengan stack WhatsApp: `aiPanelHeightDp`), cap tetap di bawah
  tinggi window gambar; `drawWindowFixedHeightPx` menambah cadangan 420 dp hanya saat `aiEnabled`.
- Outside-tap tidak menutup chat saat keyboard aktif atau SAF picker terbuka (`IslandBus.aiTyping`).
- Accessibility config kini `canRetrieveWindowContent=true` tanpa filter packageNames —
  **pengguna perlu toggle off/on Accessibility sekali setelah update** (dihingatkan otomatis di
  pengaturan bila analisis layar diaktifkan namun izin lama masih terpasang).

## Hemat RAM

- Tanpa SDK HTTP/model baru; `AiClient` hidup hanya selama request berjalan.
- Riwayat: satu file JSON kecil (`ai_history.json`, maks 30 pesan teks).
- Lampiran gambar dikompres ke JPEG ≤1024px sebelum base64; bitmap segera di-recycle.
- Nol polling, nol wake lock, nol listener AI di background — panel hanya dikomposisi saat expanded.
- Bandingkan: pendekatan on-device LLM pada referensi dynamic-island lain (mis. AI Engine
  `ajitpal/dynamic-island-android` berbasis LiteRT-LM Qwen/Gemma) menyimpan ratusan MB
  model di memori; jalur custom API ini tidak.

## Catatan keamanan

API key disimpan di DataStore aplikasi (sandi perangkat) dan dikirim hanya ke Base URL yang
diatur pengguna. Deskripsi layar Accessibility di-update untuk mengungkapkan kemampuan baca
layar on-demand (kepatuhan Play).
