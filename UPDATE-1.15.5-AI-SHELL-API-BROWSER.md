# UPDATE-1.15.5 — AI LOGIC, SHELL WIRING, API VALIDATION & BUILT-IN BROWSER

Scope: memperbaiki logika AI dan seluruh jalur yang terhubung ke AI, membenahi hubungan
AI dengan shell/terminal, memvalidasi semua API yang tertanam, dan memberi akses
menjelajah internet lewat browser bawaan.

## 1. Perbaikan logika AI

- **Model Gemini default fiktif diganti model riil.** `gemini-3.8-flash`,
  `gemini-4-carbon/argon/hydrogen`, `gemini-3.6/3.5/3.1-*` tidak pernah ada di Google AI
  Studio. Default kini `gemini-2.5-flash` (AiClient), preset UI (HomeScreen) dan
  prioritas chat (ModelPicker.GEMINI_CHAT_PRIORITY: gemini-2.5-flash / flash-lite /
  2.0-flash / 2.5-pro / 3-pro-preview). Katalog image diganti ID riil
  (`gemini-2.5-flash-image`, `gemini-2.0-flash-preview-image-generation`, `gpt-image-1`).
- **GeminiTaskRouter** memakai prefix keluarga riil; tugas COMPLEX_REASONING kini
  hanya memilih `gemini-3-pro*` / `gemini-2.5-pro`.
- **Filter riwayat output shell tidak pernah match** (AiSession membangun prompt dengan
  prefix `Output perintah Android:` sementara observasi shell dibuat dengan prefix
  `Hasil perintah Android\``). Akibatnya output terminal mentah bocor kembali ke riwayat
  prompt sebagai pesan assistant. Kini kedua prefix difilter.
- **FAILURE_HINTS terlalu luas**: hint `"Tidak "` (dengan spasi) menandai hampir semua
  jawaban Indonesia sebagai gagal. Diganti hint spesifik (`tidak bisa`, `tidak berhasil`).
- **AiActionProtocol.parse**: jawaban tanpa envelope JSON kini konsisten ditandai
  `broken` (sebelumnya hanya bila ada kurung kurawal). Fix juga syntax error Kotlin pada
  AiActionProtocolTest (kurung kurawal JSON hilang sejak versi lalu).

## 2. Hubungan AI dengan shell

- Filter riwayat (di atas) mencegah output shell mentah diperlakukan sebagai instruksi
  assistant pada giliran prompt berikutnya — jalur AI→shell→AI kini konsisten.
- NumberGuard: regex `TOTAL PSS|RSS` hanya menganchor awal baris, sehingga `TOTAL RSS`
  yang berbagi satu baris dengan `TOTAL PSS` di dumpsys tidak pernah dibaca dan angka
  konversi MB yang sah ikut ditolak. Kini anchor word-boundary.
- ShellPolicy, IslandTerminal, ShizukuShell, ConfirmPolicy tidak diubah: kebijakan
  tiga tingkat (FATAL/CONFIRM/FREE), konfirmasi perintah sensitif, dan NumberGuard
  tetap menjadi pintu tunggal perintah shell dari AI.

## 3. Validasi API tertanam (dicek live)

| Endpoint | Hasil |
| --- | --- |
| Gemini `generativelanguage.googleapis.com/v1beta` | 403 tanpa key = hidup, butuh API key |
| Groq `api.groq.com/openai/v1` | 403 tanpa key = hidup |
| Brave `api.search.brave.com/res/v1/llm/context` | 422 tanpa token = hidup |
| OpenAI `api.openai.com/v1` | 403 tanpa key = hidup |
| LRCLIB `lrclib.net/api/search` | 200 + payload valid |
| Noozra RSS `noozra.com/rss/*.xml` | 200 + RSS valid |
| DuckDuckGo Lite (fallback) | fallback tetap dipertahankan; sudah diganti pesan aman bila tidak tersedia |

- Tidak ada API key hard-coded di seluruh `app/src/main` (semua key lewat
  SecretVault/Keystore + KeyRing).
- **PageReader** kini membaca body secara streaming dengan batas 400k char (sebelumnya
  `readText()` penuh baru dipotong — bisa memakan heap pada halaman raksasa).
- **LyricsRepository.get** kini streaming dengan batas 2.000.000 char.
- **ApiDomainValidator**: footer pemeriksaan kini eksplisit "pemeriksaan struktur data,
  bukan verifikasi fakta".

## 4. Akses internet lewat browser

- **BrowserActivity baru** (WebView in-app "Browser Island"): address bar, progress,
  back-stack, Safe Browsing aktif, mixed content dilarang, JS + DOM storage untuk situs
  nyata, tautan non-http (tel:, mailto:, market:) dialihkan ke sistem.
- **AndroidManifest**: activity terdaftar dan bisa menerima ACTION_VIEW http/https
  (muncul sebagai opsi browser), `<queries>` untuk intent VIEW https.
- **DeviceActions.openUrl** kini membuka Browser Island dulu (fallback browser eksternal);
  `openApp("browser"|"internet"|"web")` membuka browser bawaan. AI dapat menyuruh
  pengguna menjelajah lewat action `open_url` (IntentResolver tetap hanya menerima
  http/https).

## 5. Perbaikan infrastruktur test

- `testImplementation("org.json:json:20240303")`: unit test JVM sebelumnya memakai stub
  android.jar sehingga semua test berbasis JSONObject gagal "not mocked".
- Robolectric diarahkan ke mirror agar android-all bisa diunduh di lingkungan CI tanpa
  akses langsung Maven Central.
- Test yang bergantung model fiktif diperbarui ke model riil (GeminiTaskRouterTest,
  ModelPickerTest, ModelFallbackTest).

## Verifikasi

- `./gradlew :app:testDebugUnitTest`: **236 test, 0 gagal**.
- Endpoint live dicek langsung (tabel di atas).
