# Android Island 1.14.15 — Jalur khusus Groq & Gemini

## Groq
- Groq kini disimpan sebagai provider `groq`, bukan disamarkan sebagai `openai`.
- API key Groq otomatis dikenali dari awalan `gsk_` dan Base URL diarahkan ke `https://api.groq.com/openai/v1/chat/completions`.
- Tetap memakai protokol OpenAI-compatible resmi Groq untuk chat dan katalog model `/models`, dengan autentikasi Bearer.
- Error 401/403 memiliki diagnosis khusus Groq; UI memberi peringatan jika key tidak tampak seperti Groq key.
- Model dipilih dari katalog aktif provider, bukan daftar statis.

## Gemini
- Tetap memakai transport native Google: `x-goog-api-key`, `models.list`, dan `generateContent`; tidak melewati endpoint OpenAI-compatible.
- Pesan autentikasi dan izin project dibedakan dari Groq.
- Pemilihan model tetap memeriksa `supportedGenerationMethods` dan kemampuan tugas.

## Fallback
- Format profil fallback sekarang menerima `groq`, `gemini`, dan `openai`. Contoh: `groq|https://api.groq.com/openai/v1|openai/gpt-oss-120b|gsk-...`.
- HTTP 401/403 tidak dianggap gangguan sementara dan tidak diulang berkali-kali; pengguna mendapat petunjuk memperbaiki kredensial/izin.
- HTTP 429/5xx dan gangguan jaringan tetap dapat berpindah ke rute cadangan yang sudah dikonfigurasi.

## Batas verifikasi
- Jalur dan format diimplementasikan mengikuti dokumentasi resmi provider. Keabsahan API key pengguna hanya dapat dipastikan oleh request langsung ke akun provider dari perangkat pengguna.
- Build APK penuh dan uji perangkat belum dijalankan di lingkungan ini.
