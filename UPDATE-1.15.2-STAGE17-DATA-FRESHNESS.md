# Stage 17 — Data Freshness & HTTP Cache Awareness

## Tujuan
Mencegah Android Island menyebut data API "terbaru" hanya karena permintaan GET baru saja berhasil. Waktu pengambilan, waktu dari payload penyedia, dan metadata cache HTTP kini ditampilkan sebagai informasi yang berbeda.

## Perubahan
- Menambahkan `ApiFreshness` untuk mengenali kandidat timestamp umum dari payload: `updated_at`, `last_updated`, `published_at`, `observation_time`, `timestamp`, `date`, `time`, dan variasi terkait.
- Menghitung umur kandidat timestamp relatif terhadap waktu pengambilan. Kandidat ditandai sebagai petunjuk saja karena field `date`/`time` bisa berarti jadwal, observasi, atau tanggal item, bukan pembaruan seluruh dataset.
- Mengabaikan timestamp yang malformed, sebelum epoch, atau terlalu jauh di masa depan agar tidak memberi kesan data valid/terbaru.
- Membaca header HTTP `Date`, `Age`, `Cache-Control`, `Expires`, `Last-Modified`, dan `ETag`.
- Memisahkan arti header: `Date` adalah waktu respons server; `Age` adalah umur objek cache menurut server/perantara; `Last-Modified` metadata modifikasi HTTP; `ETag` validator versi. Tidak satu pun otomatis membuktikan kapan data sumber diperbarui.
- Jika tidak ada timestamp payload yang dapat dikenali, status kebaruan dinyatakan **TIDAK DIKETAHUI**.
- Tidak menerapkan satu TTL universal untuk semua sektor dan tidak memberi label "fresh/terbaru" berdasarkan HTTP 200 atau waktu pengambilan.
- Menambahkan enam unit test untuk ISO timestamp, epoch seconds/milliseconds, timestamp tak tersedia, metadata cache, timestamp masa depan, dan waktu di array.

## Batasan yang tetap berlaku
- Ini bukan pemeriksaan langsung seluruh 1.064 entri katalog dan bukan jaminan kebenaran faktual.
- Pencarian timestamp bersifat heuristik; nama field dari penyedia perlu diverifikasi terhadap dokumentasi resmi.
- `Cache-Control: max-age` mengatur kesegaran cache HTTP, bukan jadwal pembaruan data sumber.
- Data tanpa timestamp tetap tidak diketahui kebaruannya; aplikasi tidak boleh menyimpulkan "baru" dari waktu unduh.
- Runner masih GET HTTPS tanpa API key/OAuth/header autentikasi khusus.

## Verifikasi
- Kompilasi mandiri `ApiFreshness.kt` dengan `kotlinc`: berhasil.
- Smoke test mandiri enam assertion: berhasil.
- Unit test Gradle, build APK, dan uji perangkat belum dinyatakan berhasil; perlu dijalankan pada lingkungan Android/Gradle yang dapat mengakses distribusi Gradle dan dependensi.
