package com.game.play.data

import android.content.Context
import android.provider.Settings
import java.util.UUID
import com.game.play.data.api.AppParser
import com.game.play.data.api.StoreApi
import com.game.play.domain.model.AppInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException

data class LoadResult<T>(val data: T, val offline: Boolean, val error: String? = null)

class StoreRepository(context: Context, private val api: StoreApi = StoreApi()) {
    private val dir = context.filesDir
    private val ratingDeviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        ?.takeIf { it.length >= 8 } ?: UUID.randomUUID().toString()
    private val appsCache = File(dir, "cache_apps.json")

    private fun detailCache(id: String) =
        File(dir, "cache_detail_" + id.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")

    private fun readCache(file: File): String? =
        try { if (file.isFile) file.readText() else null } catch (e: IOException) { null }

    private fun writeCache(file: File, text: String) {
        try { file.writeText(text) } catch (e: IOException) { /* cache gagal ditulis: abaikan */ }
    }

    fun cachedApps(): List<AppInfo> =
        readCache(appsCache)?.let { AppParser.parseList(it) }.orEmpty()

    /** Ambil dari server; kalau gagal (offline/error) pakai data tersimpan. */
    suspend fun loadApps(): LoadResult<List<AppInfo>> = withContext(Dispatchers.IO) {
        try {
            LoadResult(fetchApps(), offline = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadResult(cachedApps(), offline = true, error = friendly(e))
        }
    }

    /** Untuk worker: null kalau gagal. */
    suspend fun refreshApps(): List<AppInfo>? = withContext(Dispatchers.IO) {
        try { fetchApps() } catch (e: CancellationException) { throw e } catch (e: Exception) { null }
    }

    private fun fetchApps(): List<AppInfo> {
        val res = api.get("api/apps")
        if (!res.ok) throw IOException("Server membalas ${res.code}")
        val list = AppParser.parseList(res.body)
            ?: throw IOException("Format respons /api/apps tidak dikenali")
        writeCache(appsCache, res.body)
        return list
    }

    suspend fun loadDetail(id: String, base: AppInfo?): AppInfo? = withContext(Dispatchers.IO) {
        val cacheFile = detailCache(id)
        val fresh: AppInfo? = try {
            val res = api.get("api/detail", mapOf("id" to id))
            if (res.ok) AppParser.parseDetail(res.body)?.also { writeCache(cacheFile, res.body) } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        val detail = fresh ?: readCache(cacheFile)?.let { AppParser.parseDetail(it) }
        merge(base, detail)
    }

    /** URL unduhan APK, atau null kalau aplikasi belum punya APK. */
    suspend fun resolveDownloadUrl(app: AppInfo): String? = withContext(Dispatchers.IO) {
        try {
            val res = api.get("api/download", mapOf("id" to app.id))
            if (res.ok) {
                val fromJson = AppParser.parseObject(res.body)?.let { AppParser.findDownloadUrl(it) }
                if (fromJson != null) return@withContext fromJson
                val plain = res.body.trim()
                if (plain.startsWith("http://") || plain.startsWith("https://")) return@withContext plain
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (app.downloadUrl == null) throw e
        }
        app.downloadUrl
    }

    /** Mengembalikan pesan error, atau null kalau berhasil. */
    suspend fun postRating(id: String, rating: Int, review: String = ""): String? = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("app_id", id)
            put("rating", rating)
            put("device_id", ratingDeviceId)
            put("review", review.trim().take(1000))
        }.toString()
        val res = api.post("api/rating", body)
        if (res.ok) null else "Tidak bisa kirim rating"
    }

    private fun merge(base: AppInfo?, d: AppInfo?): AppInfo? {
        if (base == null) return d
        if (d == null) return base
        return d.copy(
            id = base.id,
            developer = d.developer.ifBlank { base.developer },
            category = d.category.ifBlank { base.category },
            versionName = d.versionName.ifBlank { base.versionName },
            versionCode = if (d.versionCode > 0) d.versionCode else base.versionCode,
            sizeLabel = d.sizeLabel.ifBlank { base.sizeLabel },
            rating = if (d.rating > 0) d.rating else base.rating,
            ratingCount = if (d.ratingCount > 0) d.ratingCount else base.ratingCount,
            description = d.description.ifBlank { base.description },
            iconUrl = d.iconUrl ?: base.iconUrl,
            screenshots = d.screenshots.ifEmpty { base.screenshots },
            downloadUrl = d.downloadUrl ?: base.downloadUrl,
            packageName = d.packageName ?: base.packageName,
            sha256 = d.sha256 ?: base.sha256
        )
    }

    private fun friendly(e: Exception): String = when (e) {
        is java.net.UnknownHostException, is java.net.SocketTimeoutException, is java.net.ConnectException ->
            "Tidak ada koneksi internet."
        else -> e.message ?: "Gagal memuat data."
    }
}
