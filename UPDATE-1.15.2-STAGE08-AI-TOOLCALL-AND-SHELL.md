# Android Island 1.15.2 — Tahap 8: Protokol tindakan AI dan hasil terminal

## Perubahan source code
- Parser kini dapat membaca beberapa objek tool-call terpisah dalam satu jawaban, termasuk pembungkus `<tool_call>` yang memiliki tag penutup salah seperti `</result>`.
- Bentuk tindakan langsung, bentuk `name + arguments`, OpenAI-compatible `tool_calls`, dan format lama `function_call` dinormalisasi menjadi envelope internal aplikasi.
- Semua tindakan tetap melewati `IntentResolver.actionsFrom`; perintah shell tetap melewati kebijakan terminal yang sudah ada. JSON asing atau nama tindakan yang tidak dikenal tidak dijalankan.
- Jika model mengirim markup tool-call yang tidak dikenal, markup mentah tidak ditampilkan kepada pengguna. Aplikasi menampilkan pesan kesalahan yang bisa dipahami.
- Hasil terminal asli kini tetap disertakan dalam jawaban akhir, bahkan ketika penjelasan AI kosong atau gagal. Jika angka pada jawaban AI tidak didukung output terminal, jawaban tersebut tetap ditahan dan bukti terminal yang relevan ditampilkan.

## Pengujian dan verifikasi
- Ditambahkan tes unit untuk wrapper dengan tag penutup salah, beberapa tool-call berurutan, native OpenAI `tool_calls`, dan tool-call yang tidak dikenal.
- Kompilasi sintaks `AiActionProtocol.kt` dengan Kotlin compiler dan stub dependensi minimum: LULUS. Ini hanya pemeriksaan sintaks, bukan pengujian runtime JSON.
- Build/unit test Gradle penuh belum dapat dijalankan: Gradle Wrapper gagal mengunduh Gradle 8.9 karena `UnknownHostException: services.gradle.org`.
- Belum dilakukan pengujian live ke provider AI atau pengujian pada perangkat Android.

## Batasan
- Implementasi native tool-call menangani data `tool_calls`/`function_call` yang diterima pada respons non-streaming. Streaming yang mengirim potongan `tool_calls` tanpa konten teks belum didukung; aplikasi saat ini tidak mengirim definisi tools native dalam payload chat standar.
- ZIP ini adalah checkpoint source code kumulatif, bukan APK yang telah diverifikasi siap dipasang.
