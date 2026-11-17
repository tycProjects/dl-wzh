package com.droidstore.app.data

import org.json.JSONArray
import org.json.JSONObject

data class User(val id: Int, val email: String, val name: String, val role: String) {
    companion object {
        fun from(o: JSONObject) = User(o.getInt("id"), o.getString("email"), o.optString("name"), o.getString("role"))
    }
}

data class Studio(val id: Int, val name: String, val description: String, val contactEmail: String, val website: String, val badges: List<String>) {
    companion object {
        fun from(o: JSONObject) = Studio(
            o.getInt("id"), o.getString("name"), o.optString("description"),
            o.getString("contactEmail"), o.optString("website"), o.optJSONArray("badges").strings(),
        )
    }
}

data class AppItem(
    val id: Int,
    val title: String,
    val packageName: String,
    val studioName: String?,
    val category: String,
    val shortDescription: String,
    val description: String,
    val status: String,
    val iconId: Int?,
    val bannerId: Int?,
    val screenshotIds: List<Int>,
    val versionName: String?,
    val downloads: Int,
    val ratingAverage: Double?,
    val ratingCount: Int,
    val badges: List<String>,
    val securityStatus: String?,
    val securityScore: Int?,
    val securityLimitations: List<String>,
    val supportEmail: String,
    val privacyPolicyUrl: String,
) {
    companion object {
        fun from(o: JSONObject): AppItem {
            val rating = o.optJSONObject("rating")
            val sec = o.optJSONObject("security")
            val cv = o.optJSONObject("currentVersion")
            return AppItem(
                id = o.getInt("id"),
                title = o.getString("title"),
                packageName = o.optString("packageName"),
                studioName = o.optString("studioName").takeIf { it.isNotEmpty() && it != "null" },
                category = o.optString("category"),
                shortDescription = o.optString("shortDescription"),
                description = o.optString("description"),
                status = o.optString("status"),
                iconId = o.optIntOrNull("iconId"),
                bannerId = o.optIntOrNull("bannerId"),
                screenshotIds = o.optJSONArray("screenshotIds").ints(),
                versionName = cv?.optString("versionName"),
                downloads = o.optInt("downloads"),
                ratingAverage = rating?.optDoubleOrNull("average"),
                ratingCount = rating?.optInt("count") ?: 0,
                badges = o.optJSONArray("badges").strings(),
                securityStatus = sec?.optString("status"),
                securityScore = sec?.optIntOrNull("score"),
                securityLimitations = sec?.optJSONArray("limitations").strings(),
                supportEmail = o.optString("supportEmail"),
                privacyPolicyUrl = o.optString("privacyPolicyUrl"),
            )
        }
    }
}

data class RatingSummary(val average: Double?, val count: Int) {
    companion object {
        fun from(o: JSONObject) = RatingSummary(o.optDoubleOrNull("average"), o.optInt("count"))
    }
}

data class Review(val stars: Int, val comment: String, val userName: String) {
    companion object {
        fun from(o: JSONObject) = Review(o.getInt("stars"), o.optString("comment"), o.optString("userName"))
    }
}

data class AppForm(
    val packageName: String,
    val title: String,
    val shortDescription: String,
    val description: String,
    val category: String,
    val website: String,
    val supportEmail: String,
    val privacyPolicyUrl: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("packageName", packageName)
        .put("title", title)
        .put("shortDescription", shortDescription)
        .put("description", description)
        .put("category", category)
        .put("website", website)
        .put("supportEmail", supportEmail)
        .put("privacyPolicyUrl", privacyPolicyUrl)
}

val CATEGORIES = listOf(
    "tools" to "Инструменты", "games" to "Игры", "productivity" to "Продуктивность",
    "education" to "Образование", "music" to "Музыка", "photo" to "Фото", "social" to "Общение",
    "finance" to "Финансы", "health" to "Здоровье", "other" to "Другое",
)

val STATUS_LABELS = mapOf(
    "draft" to "Черновик", "security_scan" to "Проверка безопасности", "pending_review" to "На модерации",
    "changes_requested" to "Нужны изменения", "approved" to "Одобрено", "published" to "Опубликовано",
    "rejected" to "Отклонено", "suspended" to "Приостановлено",
)

val BADGE_LABELS = mapOf(
    "verified_developer" to "Verified Developer", "verified_studio" to "Verified Studio",
    "official_app" to "Official App", "security_checked" to "Security Checked",
    "featured" to "Featured", "new_developer" to "New Developer",
)

val BADGE_HINTS = mapOf(
    "verified_developer" to "Личность разработчика подтверждена администрацией DroidStore.",
    "verified_studio" to "Студия подтверждена администрацией DroidStore.",
    "official_app" to "Официальное приложение, выпущенное разработчиком бренда.",
    "security_checked" to "Приложение прошло проверку безопасности. Это не гарантия отсутствия рисков.",
    "featured" to "Приложение выбрано редакцией DroidStore.",
    "new_developer" to "Разработчик недавно зарегистрирован в DroidStore.",
)

private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key) && !isNull(key)) optInt(key) else null
private fun JSONObject.optDoubleOrNull(key: String): Double? = if (has(key) && !isNull(key)) optDouble(key) else null
private fun JSONArray?.strings(): List<String> = this?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
private fun JSONArray?.ints(): List<Int> = this?.let { a -> (0 until a.length()).map { a.getInt(it) } }.orEmpty()
internal inline fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> = (0 until length()).map { transform(getJSONObject(it)) }
