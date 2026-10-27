package com.game.play.domain.model

data class AppInfo(
    val id: String,
    val name: String,
    val developer: String = "",
    val category: String = "",
    val versionName: String = "",
    val versionCode: Long = 0L,
    val sizeLabel: String = "",
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val description: String = "",
    val iconUrl: String? = null,
    val screenshots: List<String> = emptyList(),
    val downloadUrl: String? = null,
    val packageName: String? = null,
    val sha256: String? = null
)

data class InstalledInfo(val versionName: String, val versionCode: Long)

data class DlState(val busy: Boolean = false, val progress: Float = 0f, val message: String? = null)

fun AppInfo.hasUpdate(installed: InstalledInfo?): Boolean {
    if (installed == null) return false
    if (versionCode > 0 && installed.versionCode > 0) return versionCode > installed.versionCode
    return versionName.isNotBlank() && installed.versionName.isNotBlank() && versionName != installed.versionName
}
