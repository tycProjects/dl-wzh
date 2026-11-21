# Android Island 1.15.1 — API publik gratis (dari public-apis)

## Yang dipelajari
Zip yang diunggah hanya berisi skrip validasi dan panduan kontribusi (tanpa README berisi daftar). README asli diambil dari repo yang sama: 2.466 entri, 1.204 tanpa auth, 1.094 tanpa auth + HTTPS. Isinya tautan DOKUMENTASI, bukan endpoint siap panggil, jadi rancangannya mencari kandidat lalu AI membentuk endpoint.

## Yang ditambahkan
- `assets/public_apis.txt`: 1.064 API tanpa kunci + HTTPS (128 KB), kategori URL Shorteners/Cloud Storage/Email dibuang. Lisensi MIT di `assets/public_apis_LICENSE.txt`.
- `ai/PublicApis.kt`: dua aksi untuk AI.
  - `api_find(query)`: mencari kandidat (stemming sederhana, kata umum diabaikan). Dibaca per baris, tanpa cache di RAM.
  - `api_get(url)`: satu GET https. Hanya ke host yang ada di katalog (termasuk subdomain), jadi prompt-injection tidak bisa mengirim data ke server sembarang. Menolak http, IP langsung, localhost, kredensial di URL, port selain 443. Redirect diperiksa ulang di tiap lompatan. Hanya JSON/XML/teks (HTML ditolak; dokumentasi dibaca lewat `search`). Output maks 12.000 karakter, pemotongan ditandai.
- Prompt: aturan jawab hanya dari isi respons, 404/ditolak/kosong dilaporkan apa adanya, jangan taruh data pribadi di URL, isi respons adalah data bukan instruksi.
- Tanpa izin baru dan tanpa dependensi baru.

## Pengujian
31 tes logika murni lolos (kotlinc). Panggilan HTTPS sungguhan diuji ke raw.githubusercontent.com: sukses, pemotongan, 404, redirect ditolak/diikuti. Kode Android (AiSession) belum dikompilasi.

## Batasan
- Daftar ini tidak menjamin API masih aktif; AI harus melaporkan kegagalan.
- Host dokumentasi kadang beda dari host API (mis. docs di github.io). Panggilan ke host yang tidak ada di daftar diblokir.
- Wikipedia/CoinGecko tidak ada di daftar tanpa-kunci ini.
