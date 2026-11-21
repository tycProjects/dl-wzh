# Update 1.11.1 — AI Island UX & automatic provider setup

## Perbaikan panel AI
- Tinggi daftar chat mengikuti tinggi layar dan keyboard; daftar pesan yang dibatasi tinggi akan scroll sendiri.
- Panel AI memiliki batas tinggi yang lebih ketat daripada stack notifikasi agar tidak menutupi sebagian besar layar pada ponsel pendek.
- Padding panel diringkas; header dan kolom input tetap tersedia.

## Setup API lebih mudah
- Saat API key disimpan, prefix yang dikenal dipakai untuk mendeteksi Groq (`gsk_`), OpenRouter (`sk-or-v1-`), Google AI Studio (`AIza`), dan OpenAI (`sk-`).
- URL provider otomatis disesuaikan untuk provider yang dikenali.
- Untuk endpoint OpenAI-compatible, aplikasi meminta `/models`, lalu memilih model yang benar-benar dikembalikan provider. Jika daftar model gagal diambil, pengguna mendapat pesan dan masih dapat memilih model manual.
- Model Groq lama `llama-3.1-8b-instant` tidak lagi dipakai sebagai default tetap.

## Batas kompatibilitas
Tidak semua model/provider memakai protokol yang sama atau memberi akses yang sama pada setiap API key. Deteksi otomatis mendukung provider yang prefix-nya dikenali dan endpoint daftar model OpenAI-compatible; Gemini memakai jalur API khusus. Provider lain tetap dapat diatur melalui preset/kustom. Model yang terdaftar belum tentu mendukung vision atau fitur tertentu.

## Keamanan
API key milik pengguna tidak ditanam di source code atau APK. Masukkan key baru dari pengaturan aplikasi. Jika key pernah dibagikan di chat, tangkapan layar, atau repositori, revoke dan buat key baru.
