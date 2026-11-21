# Audit Public APIs — Tahap 13

Tanggal audit: 18 November 2026

## Cakupan inventaris lokal

- Entri valid dengan format `kategori|nama|url-dokumentasi|deskripsi`: **1064**
- Kategori: **46**
- Baris rusak / kolom kurang: **0**
- URL HTTPS: **1064 dari 1064**
- Nama API yang berulang: **10 nama** (sebagian dapat berasal dari penyedia berbeda)
- URL dokumentasi yang berulang: **2 URL**

## Jumlah entri per kategori

| Kategori | Entri |
|---|---:|
| Animals | 19 |
| Anime | 12 |
| Anti-Malware | 3 |
| Art & Design | 18 |
| Blockchain | 10 |
| Books | 21 |
| Business | 16 |
| Calendar | 15 |
| Cryptocurrency | 49 |
| Currency Exchange | 17 |
| Data Validation | 1 |
| Development | 120 |
| Dictionaries | 7 |
| Documents & Productivity | 7 |
| Entertainment | 17 |
| Environment | 19 |
| Events | 2 |
| Finance | 40 |
| Food & Drink | 16 |
| Games & Comics | 71 |
| Geocoding | 51 |
| Government | 91 |
| Health | 36 |
| Jobs | 16 |
| Machine Learning | 13 |
| Music | 13 |
| News | 6 |
| Open Data | 66 |
| Open Source Projects | 8 |
| Patent | 2 |
| Personality | 19 |
| Phone | 1 |
| Photography | 9 |
| Programming | 1 |
| Science & Math | 51 |
| Security | 26 |
| Shopping | 10 |
| Social | 10 |
| Sports & Fitness | 27 |
| Test Data | 27 |
| Text Analysis | 1 |
| Tracking | 1 |
| Transportation | 41 |
| Vehicle | 10 |
| Video | 29 |
| Weather | 19 |

## Temuan integrasi

1. File katalog hanya menyimpan empat kolom: kategori, nama, URL dokumentasi, dan deskripsi.
2. Tidak ada metadata terstruktur tentang jenis autentikasi, status HTTPS yang diuji, CORS, harga, rate limit, uptime, metode HTTP, atau endpoint siap-pakai.
3. `PublicApis.find()` hanya menemukan kandidat. URL yang terdaftar bukan jaminan endpoint API.
4. `PublicApis.get()` hanya menjalankan GET ke host HTTPS yang tercantum dalam katalog, tanpa API key, kredensial, atau header autentikasi.
5. Respons yang diterima dibatasi panjangnya dan hanya JSON, XML, atau teks biasa yang didukung. Respons biner dan HTML sukses tidak diproses.
6. Karena keterbatasan metadata dan dukungan metode, semua sektor belum dapat dinyatakan valid hanya berdasarkan keberadaan entri.

## Perubahan Tahap 13

- Pesan hasil pencarian API sekarang menyebut katalog sebagai snapshot lokal, bukan direktori live/terverifikasi.
- Respons HTTP non-2xx kini dipisahkan dari pemeriksaan content-type agar status 401/403/404/429/5xx tidak tertutup pesan tipe konten.
- HTTP 401 menjelaskan kebutuhan autentikasi; 403 akses ditolak; 404 endpoint tidak ditemukan; 429 menampilkan `Retry-After` bila tersedia; 5xx menjelaskan gangguan server.
- URL yang ditampilkan kembali ke AI menyamarkan nilai parameter query yang tampak seperti kredensial (mis. `api_key`, `token`, `access_token`, `client_secret`, `signature`).
- Instruksi AI diperjelas: GET saja, tanpa kredensial, dan tidak boleh mengarang hasil ketika API gagal.

## Perbandingan sumber upstream

Situs resmi Public APIs menyatakan direktori upstream berisi 2.413 API dalam 51 kategori, menyediakan metadata autentikasi/HTTPS/CORS, serta pemeriksaan kesehatan harian. Snapshot lokal Android Island berisi 1.064 entri dalam 46 kategori; jadi snapshot lokal lebih kecil dan tidak membawa metadata tersebut. Lihat https://publicapis.lol/ dan https://publicapis.lol/about.

Repositori upstream menjelaskan bahwa kolom autentikasi, HTTPS, dan CORS merupakan bagian dari format entri; nilai tersebut harus dibaca dari sumber terkini, bukan ditebak dari deskripsi. Lihat https://github.com/public-apis/public-apis/blob/master/CONTRIBUTING.md.

## Status validasi

- Audit inventaris lokal dan pemeriksaan statis: dapat dilakukan.
- Unit test baru: ditambahkan, tetapi belum dapat dijalankan melalui Gradle pada lingkungan ini jika distribusi Gradle/dependensi tidak tersedia.
- Uji endpoint langsung untuk seluruh entri: **belum dilakukan**.
- Build APK dan uji perangkat: **belum dilakukan**.
- Autentikasi API key/OAuth, POST, GraphQL, WebSocket, dan unggahan berkas: **belum didukung oleh runner ini**.

Kesimpulan: integrasi ini cocok sebagai pencari kandidat dan pemanggil endpoint GET sederhana yang sudah diverifikasi dokumentasinya. Ia belum merupakan klien universal untuk semua 1.064 entri atau seluruh 46 sektor.
