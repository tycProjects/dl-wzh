package com.almoazin.app.data

import java.util.Date

enum class PrayerKey {
    FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA
}

data class PrayerTimesResult(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val dates: Map<PrayerKey, Date>
)

data class CityLocation(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val countryAr: String,
    val lat: Double,
    val lng: Double,
    val timezone: Double,
    val defaultMethod: String = "makkah"
)

data class AzanReciter(
    val id: String,
    val nameAr: String,
    val locationAr: String,
    val audioUrls: List<String>,
    val durationSeconds: Int,
    val isRealVoice: Boolean = true
)

data class SurahMeta(
    val number: Int,
    val nameAr: String,
    val nameEn: String,
    val englishTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String
)
