# Ryomd Tube v3.1 — Pemilih kualitas video

## Perubahan
- Menambahkan tombol **Kualitas** pada bagian atas pemutar native layar penuh.
- Pilihan `Otomatis` dan seluruh resolusi progressive yang ditemukan NewPipe tersedia sampai batas kompatibilitas 1080p.
- Pemilihan resolusi menjalankan ekstraksi ulang stream yang sesuai, lalu menyambung kembali pada posisi tontonan sebelumnya.
- Status play/pause dipertahankan ketika resolusi diganti.
- Pilihan kualitas terakhir disimpan lokal dan dipakai sebagai preferensi saat membuka video berikutnya; pengguna dapat kembali ke Otomatis dari menu yang sama.
- Kualitas adaptive video-only digabungkan dengan stream audio terbaik; kualitas muxed dipakai bila stream terpisah tidak tersedia.
- Pemilih menampilkan hanya resolusi yang benar-benar terdeteksi dan dapat diproses adapter native; tidak membuat pilihan resolusi palsu.
- Mode audio-only tidak menampilkan pemilih resolusi video.

## Perilaku dan batasan
- Batas pemilih saat ini 1080p untuk menjaga kompatibilitas decoder dan penggunaan data perangkat Android.
- Tidak semua video menyediakan semua resolusi. Video dengan stream manifest saja, pembatasan YouTube, atau format yang tidak dapat diproses oleh adapter progressive dapat tidak menyediakan pilihan manual.
- URL stream diekstrak ulang saat pilihan kualitas diubah karena URL YouTube bersifat sementara.

## Build
- `applicationId`: `com.ryomd.nativeapp`
- `versionCode`: 9
- `versionName`: `1.1.8-ryomd-tube-v3.1`
- Perubahan Java native; build ulang Android Gradle Project melalui WZH.
