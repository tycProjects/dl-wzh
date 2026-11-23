# Inventaris source terintegrasi

Panel web berasal dari arsip `RYOMD_WEB_SOURCE_WZH2_SAFE.zip` yang diunggah pengguna.
File panel 13 buah: HTML, JavaScript, CSS, aset gambar/video, dan modul Crypto Market.

Paket Android yang diunggah sebelumnya masih memuat log runtime, PID, dan folder backup halaman lama di assets. Folder assets tersebut telah diganti seluruhnya dengan 13 file bersih dari arsip web terpisah.

Backend `server.js`, database Anime, session bot, dan `ryomd-control.sh` tidak terdapat dalam proyek Android ini dan tidak dimodifikasi.
