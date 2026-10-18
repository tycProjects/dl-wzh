package com.example.data

import org.maplibre.android.geometry.LatLng

enum class RouteMode(val profile: String, val label: String) {
    DRIVING("driving", "Driving"),
    WALKING("walking", "Walking"),
    CYCLING("bike", "Cycling")
}

data class ZPlace(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val category: String = "general",
    val isBookmarked: Boolean = false
) {
    val latLng: LatLng
        get() = LatLng(latitude, longitude)
}

data class ZRoute(
    val points: List<LatLng>,
    val distanceText: String,
    val durationText: String,
    val distanceMeters: Long = 0,
    val durationSeconds: Long = 0,
    val startAddress: String = "",
    val endAddress: String = "",
    val mode: RouteMode = RouteMode.DRIVING,
    val steps: List<ZRouteStep> = emptyList()
)

data class ZRouteStep(
    val instruction: String,
    val distanceText: String,
    val durationText: String,
    val maneuver: String? = null,
    val modifier: String? = null
)

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    FILIPINO("fil", "Filipino", "Tagalog"),
    CEBUANO("ceb", "Cebuano", "Sinugbuanong Binisaya"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

sealed interface LocationUiState {
    data object Idle : LocationUiState
    data object Locating : LocationUiState
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val isAccurate: Boolean = true
    ) : LocationUiState {
        val latLng: LatLng get() = LatLng(latitude, longitude)
    }
    data class PermissionDenied(val isPermanent: Boolean = false) : LocationUiState
    data object GpsDisabled : LocationUiState
    data class Error(val message: String) : LocationUiState
}
