# Android Island 1.15.4 — Stage 19: Brave Web Search

## Tujuan
Meningkatkan akses web AI dengan Brave Search LLM Context sebagai provider utama saat API key dikonfigurasi, tanpa menanamkan key ke source code.

## Perubahan
- Menambahkan pengaturan `Brave Web Search` di Pengaturan AI > Pengaturan lanjutan.
- API key Brave disimpan melalui `SecretVault` (Android Keystore AES-GCM), terpisah dari API key model AI.
- `WebSearch` memakai `GET https://api.search.brave.com/res/v1/llm/context` dengan token pada header `X-Subscription-Token`; key tidak dimasukkan ke URL atau prompt model.
- Meminta konteks relevan dengan ambang `strict`, SafeSearch `moderate`, anggaran konteks 4096 token, metadata sumber, dan freshness filter untuk pertanyaan berita terkini.
- Memproses `grounding.generic` dan metadata sumber: judul, URL HTTPS, potongan isi, tanggal sumber jika tersedia, serta timestamp pengambilan indeks jika tersedia.
- Respons kosong, format salah, HTTP error, API key ditolak, rate limit, dan gangguan server tidak diperlakukan sebagai bukti.
- Membatasi timeout, ukuran respons, host endpoint, dan tidak mengikuti redirect otomatis.
- Tanpa key Brave: berita terkini menggunakan fallback RSS Noozra; pencarian umum menggunakan DuckDuckGo Lite dengan label keterbatasan. Hasil judul/tautan tidak boleh dianggap sebagai isi atau fakta terverifikasi.
- Menambahkan pengujian unit untuk parsing konteks Brave, hasil kosong, URL sumber, dan format respons yang rusak.
- Versi aplikasi dinaikkan menjadi `1.15.4` / versionCode `149`.

## Privasi dan penggunaan
Hanya kueri pencarian yang dikirim ke Brave. Riwayat percakapan, isi lampiran, data layar, kredensial AI lain, dan data perangkat tidak disertakan. API key hanya dikirim ke host API Brave melalui HTTPS. Pengguna perlu memasukkan key miliknya sendiri; tidak ada key contoh atau key bawaan.

## Verifikasi
- Pemeriksaan sintaks Kotlin pada `WebSearch.kt` menggunakan Kotlin compiler dengan stub dependensi Android/JSON: berhasil.
- `./gradlew testDebugUnitTest --offline`: tidak dapat dijalankan karena Gradle Wrapper belum tersedia lokal dan mencoba mengunduh Gradle 8.9; lingkungan tidak dapat me-resolve `services.gradle.org` (`UnknownHostException`).
- Karena Gradle tidak berhasil dijalankan, build Android dan seluruh unit test proyek belum dapat dinyatakan lulus.
