# API contract draft

- `POST /v1/build` — menerima prompt; balas `build_id`, `status`, `message`.
- `GET /v1/build/{build_id}` — status job (`queued`, `generating`, `building`, `ready`, `failed`).
- `GET /v1/build/{build_id}/files` — daftar file project.
- `GET /v1/build/{build_id}/artifacts` — metadata unduhan APK dan ZIP saat tersedia.

Semua endpoint wajib HTTPS, autentikasi, otorisasi per pengguna, validasi input, rate limiting, dan URL artefak sementara. Jangan mengembalikan API secret ke klien.
