# Stage 54 — Pemisahan mode percakapan, Looping Agent, dan Shell

## Aturan penggunaan
- Percakapan biasa: kirim teks tanpa awalan khusus. Pesan diteruskan ke model dengan riwayat percakapan; tidak menjalankan deteksi perintah perangkat, pembacaan layar, atau protokol tindakan.
- Looping Agent: awali perintah dengan `. ` (titik lalu spasi). Contoh: `. buka TikTok lalu cari preset lalu tekan Enter lalu buka video pertama`.
- Terminal Shizuku: hanya melalui `/shell`, misalnya `/shell dumpsys meminfo com.android.island`.
- Pencarian berita terkini tetap dapat dilakukan tanpa awalan titik melalui jalur pencarian berita yang sudah tersedia.

## Perubahan
- Menambahkan `AgentCommandPrefix` untuk klasifikasi mode deterministik.
- Menghapus aktivasi agen berdasarkan tebakan kata kerja dari pesan biasa.
- Pemindaian layar hanya dilakukan untuk perintah bertanda titik dan ketika pengaturan analisis layar aktif.
- Fast path pencarian aplikasi hanya aktif pada mode agen.
- Otorisasi tindakan agen berasal dari penanda eksplisit pengguna; resolver tambahan yang menambah putaran API tidak dijalankan untuk setiap langkah.
- `/shell` tetap jalur terpisah dan tidak dapat dipicu oleh teks jawaban AI.
- Menambahkan unit test untuk pesan biasa, penanda agen, lampiran, dan titik tanpa perintah.

## Verifikasi
- Source diperiksa untuk memastikan hanya `agentRequested` yang mengaktifkan Looping Agent.
- Build dan unit test Gradle belum dapat dijalankan karena wrapper tidak dapat mengunduh Gradle 8.9 (`UnknownHostException: services.gradle.org`).
