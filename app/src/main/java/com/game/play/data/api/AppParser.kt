package com.game.play.data.api

import com.game.play.domain.model.AppInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

/**
 * Parser toleran: bentuk respons API dibaca dari beberapa nama field yang umum,
 * jadi cocok untuk array langsung maupun objek { apps: [...] } / { data: [...] }.
 */
object AppParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val listKeys = listOf("apps", "data", "items", "results", "list", "applications")
    private val wrapKeys = listOf("app", "data", "item", "result", "detail")

    fun parse(text: String): JsonElement? =
        try { json.parseToJsonElement(text) } catch (e: Exception) { null }

    fun parseList(text: String): List<AppInfo>? {
        val root = parse(text) ?: return null
        val array = findArray(root, 0) ?: return null
        return array.mapNotNull { element -> (element as? JsonObject)?.let { toApp(it) } }.distinctBy { it.id }
    }

    fun parseDetail(text: String): AppInfo? {
        val root = parse(text) as? JsonObject ?: return null
        return toApp(unwrap(root, 0))
    }

    fun parseObject(text: String): JsonObject? {
        val root = parse(text) as? JsonObject ?: return null
        return unwrap(root, 0)
    }

    fun pick(obj: JsonObject, vararg keys: String): String? = obj.str(*keys)

    fun findDownloadUrl(obj: JsonObject): String? =
        fixUrl(obj.str("downloadUrl", "download_url", "apkUrl", "apk_url", "apk", "file", "fileUrl", "file_url", "url", "link"))

    private fun findArray(element: JsonElement, depth: Int): JsonArray? {
        if (element is JsonArray) return element
        if (element is JsonObject && depth < 3) {
            for (key in listKeys) {
                val child = element[key] ?: continue
                val found = findArray(child, depth + 1)
                if (found != null) return found
            }
        }
        return null
    }

    private fun hasIdentity(o: JsonObject): Boolean =
        o.str("id", "_id", "appId", "slug", "name", "title", "appName", "packageName", "package") != null

    private fun unwrap(o: JsonObject, depth: Int): JsonObject {
        if (hasIdentity(o) || depth >= 3) return o
        for (key in wrapKeys) {
            val child = o[key] as? JsonObject
            if (child != null) return unwrap(child, depth + 1)
        }
        return o
    }

    fun toApp(o: JsonObject): AppInfo? {
        val pkg = o.str("packageName", "package", "package_name", "pkg", "applicationId")
        val name = o.str("name", "title", "appName", "app_name") ?: pkg ?: return null
        val id = o.str("id", "_id", "appId", "app_id", "slug") ?: pkg ?: name
        return AppInfo(
            id = id,
            name = name,
            developer = o.str("developer", "author", "dev", "developerName") ?: "",
            category = o.str("category", "genre", "type") ?: "",
            versionName = o.str("versionName", "version_name", "version") ?: "",
            versionCode = o.num("versionCode", "version_code")?.toLong() ?: 0L,
            sizeLabel = sizeLabel(o),
            rating = o.num("rating", "averageRating", "avgRating", "average_rating") ?: 0.0,
            ratingCount = o.num("ratingCount", "ratingsCount", "rating_count", "reviews", "reviewCount")?.toInt() ?: 0,
            description = o.str("description", "desc", "about", "summary") ?: "",
            iconUrl = fixUrl(o.str("icon", "iconUrl", "icon_url", "logo", "image", "imageUrl")),
            screenshots = o.strings("screenshots", "images", "photos").mapNotNull { fixUrl(it) },
            downloadUrl = findDownloadUrl(o),
            packageName = pkg,
            sha256 = o.str("sha256", "hash", "checksum")
        )
    }

    private fun sizeLabel(o: JsonObject): String {
        val raw = o.str("sizeLabel", "size_label", "size", "fileSize", "file_size") ?: o.num("sizeBytes")?.toString() ?: return ""
        val number = raw.toDoubleOrNull() ?: return raw
        return if (number >= 1024) formatBytes(number) else String.format(Locale.US, "%.0f MB", number)
    }

    fun formatBytes(bytes: Double): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1) String.format(Locale.US, "%.1f MB", mb)
        else String.format(Locale.US, "%d KB", (bytes / 1024.0).toLong())
    }

    private fun fixUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (url.startsWith("data:")) return url
        return ApiClient.BASE_URL.trimEnd('/') + "/" + url.trimStart('/')
    }

    private fun JsonObject.str(vararg keys: String): String? {
        for (key in keys) {
            val primitive = this[key] as? JsonPrimitive ?: continue
            if (primitive is JsonNull) continue
            val content = primitive.content
            if (content.isNotBlank()) return content
        }
        return null
    }

    private fun JsonObject.num(vararg keys: String): Double? =
        str(*keys)?.replace(',', '.')?.toDoubleOrNull()

    private fun JsonObject.strings(vararg keys: String): List<String> {
        for (key in keys) {
            val array = this[key] as? JsonArray ?: continue
            return array.mapNotNull { element ->
                when (element) {
                    is JsonNull -> null
                    is JsonPrimitive -> element.content.takeIf { it.isNotBlank() }
                    is JsonObject -> element.str("url", "src")
                    else -> null
                }
            }
        }
        return emptyList()
    }
}
