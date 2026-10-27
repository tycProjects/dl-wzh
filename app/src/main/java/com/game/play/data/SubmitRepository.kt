package com.game.play.data

import com.game.play.data.api.AppParser
import com.game.play.data.api.StoreApi
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException

/**
 * Alur pengiriman: /api/upload-token -> unggah file -> /api/submit.
 * Nama field di bawah adalah dugaan terbaik; kalau server menolak, pesan asli server
 * ditampilkan di layar supaya bisa disesuaikan.
 */
class SubmitRepository(private val api: StoreApi = StoreApi()) {

    class Payload(
        val name: String,
        val packageName: String,
        val developer: String,
        val category: String,
        val description: String,
        val versionName: String,
        val versionCode: Long,
        val sha256: String,
        val certSha256: String,
        val apkUrl: String,
        val iconUrl: String,
        val screenshotUrls: List<String>,
        val sizeBytes: Long
    )

    fun upload(file: File, filename: String, contentType: String): String {
        val res = api.postFile("api/upload-file", file, filename, contentType)
        if (!res.ok) throw IOException("Unggah $filename gagal (${res.code}): ${res.body.take(200)}")
        val obj = AppParser.parseObject(res.body)
            ?: throw IOException("Respons upload bukan JSON: ${res.body.take(200)}")
        return AppParser.pick(obj, "url", "downloadUrl", "fileUrl")
            ?: throw IOException("Respons upload tidak berisi URL file: ${res.body.take(200)}")
    }

    fun submit(p: Payload) {
        val body = buildJsonObject {
            put("name", p.name)
            put("packageName", p.packageName)
            put("developer", p.developer)
            put("category", p.category)
            put("description", p.description)
            put("versionName", p.versionName)
            put("versionCode", p.versionCode)
            put("sha256", p.sha256)
            put("certSha256", p.certSha256)
            put("sizeBytes", p.sizeBytes)
            put("apkUrl", p.apkUrl)
            put("iconUrl", p.iconUrl)
            put("screenshots", buildJsonArray { p.screenshotUrls.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
        }.toString()
        val res = api.post("api/submit", body)
        if (!res.ok) throw IOException("Pengajuan ditolak server (${res.code}): ${res.body.take(240)}")
    }
}
