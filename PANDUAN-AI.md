# Panduan AI Island — Android Island 1.10.0

## 1. Aktifkan & isi API (sekali saja)

1. Buka aplikasi **Android Island** → section **ASISTEN AI**.
2. Nyalakan **Asisten AI**.
3. Isi:
   - **Base URL** — alamat endpoint + `/v1`, tanpa `/chat/completions`:
     - OpenAI: `https://api.openai.com/v1`
     - OpenRouter: `https://openrouter.ai/api/v1`
     - Groq: `https://api.groq.com/openai/v1`
     - Ollama di HP/local (bisa juga di PC satu Wi-Fi): `http://127.0.0.1:11434/v1`
   - **API key** — kunci dari penyedia tersebut (Ollama/LM Studio lokal boleh dikosongkan... tetap isi apa saja bila endpoint menuntut `Authorization`).
   - **Model** — nama persis di endpoint: `gpt-4o-mini`, `llama3.2`, `qwen2.5:7b`, dll.
4. (Opsional) atur **Temperature**, **Maks token**, **System prompt** (kepribadian), **Streaming**.

## 2. Panggil dengan tekan ganda tombol power (Samsung / One UI)

Android tidak mengizinkan aplikasi mengambil alih *long-press* tombol power (jalur resminya
hanya lewat peran asisten sistem / VoiceInteraction yang sengaja ditunda pada versi ini).
Jalur resmi One UI yang dipakai:

> **Pengaturan → Fitur lanjutan → Side button → Tekan dua kali → Buka app → "AI Island"**

Ikon **"AI Island"** (pakai ikon island yang sama) otomatis muncul di:
- laci aplikasi,
- daftar "Open app" pada pengaturan Side button.

Alternatif lain: section **Kontrol · Gesture** → **Long press** → **Asisten AI**
(long-press kapsul island saat musik/chat tampil).

## 3. Pakai panel AI

Panggilan manapun akan **membuka island langsung ke kartu AI** (state expanded) dan keyboard
naik otomatis — tinggal mengetik:

- **Kirim** — tombol bulat ungu di kanan bawah, atau tombol Send di keyboard.
- **Analisis Layar** — AI membaca app aktif (nama + activity), judul jendela, dan teks yang
  terlihat **hanya saat chip ini ditekan** atau saat pesan jelas menanyakan isi layar
  ("buka apa", "analisis layar saya", dsb.). Tidak ada pembacaan diam-diam.
- **Lampirkan file** — pilih dokumen: file teks (txt/md/log/json/csv/kode) dibaca isinya
  (maks 40 ribu karakter), gambar dikompres ≤1024 px dan dikirim ke API vision.
- **Bersihkan** — tekan dua kali untuk menghapus riwayat (tersimpan maks 30 pesan).
- **Stop** — saat AI menjawab, tombol kirim berubah jadi merah (hentikan streaming; jawaban
  sebagian tetap disimpan).
- Tutup kartu: tombol ✕ atau tap di luar island. Musik tetap jalan; chat WhatsApp tetap
  mengantri di belakang.

## 4. Penting setelah update

Config Accessibility berubah (untuk analisis layar), jadi **matikan lalu nyalakan kembali**
Accessibility "Android Island" di Pengaturan sistem **sekali saja**. Bila lupa, aplikasi akan
mengingatkan otomatis di pengaturan ketika analisis layar diaktifkan, dan panel AI memberi
pesan panduan saat chip Analisis Layar ditekan.

## 5. Catatan hemat RAM

- Tidak ada SDK/model AI di dalam APK — client `HttpURLConnection` aktif hanya saat request.
- Riwayat = satu file JSON kecil (maks 30 pesan teks); isi lampiran tidak pernah ditulis disk.
- Panel hanya dikomposisi saat island expanded; tidak ada polling/wake lock di background.
- Perbandingan referensi GitHub: `ajitpal/dynamic-island-android` memakai LLM on-device
  (LiteRT-LM, Qwen/Gemma — ratusan MB RAM); pendekatan custom API ini jauh lebih ringan.

## 6. Kalau nanti mau voice (VoiceInteraction)

Struktur sudah siap: sebuah `VoiceInteractionService` headless cukup memanggil
`IslandBus.pushAi()` saat assist dipanggil (long-press power resmi), panel keyboard yang sama
tetap dipakai — tanpa RecognitionService/mikrofon sampai fitur suara benar-benar diinginkan.


## 8. Pencarian berita & riset mendalam (1.16.5)

- Ketik `berita hari ini` / `berita tentang <topik>` — jawaban berupa daftar bernomor rapi:
  `1. Judul — Media (20 Nov 2026, 15:13 WIB) | Baca Sumber`. Waktu sudah dikonversi ke WIB,
  judul duplikat dibuang, dan tautan "Baca Sumber" di bubble bisa ditekan untuk membuka artikel.
- Ketik `cari lebih dalam tentang <topik>` / `riset <topik>` / `telusuri kronologi <topik>` —
  agen menjalankan pencarian multi-ronde (kronologi, penjelasan resmi, data terverifikasi) dan
  membaca langsung isi halaman sumber sebelum menjawab. Mode ini butuh waktu lebih lama (budget
  26 langkah / 5 menit) dan hasilnya menyebut sumber per temuan.
