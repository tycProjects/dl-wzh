package com.almoazin.app.utils

import com.almoazin.app.data.PrayerKey
import com.almoazin.app.data.PrayerTimesResult
import java.util.Calendar
import java.util.Date
import kotlin.math.*

object PrayerCalculator {

    private fun deg2rad(deg: Double) = deg * Math.PI / 180.0
    private fun rad2deg(rad: Double) = rad * 180.0 / Math.PI

    private fun fixAngle(a: Double): Double {
        var angle = a - 360.0 * floor(a / 360.0)
        if (angle < 0) angle += 360.0
        return angle
    }

    private fun fixHour(h: Double): Double {
        var hour = h - 24.0 * floor(h / 24.0)
        if (hour < 0) hour += 24.0
        return hour
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private data class SunPosition(val declination: Double, val equationOfTime: Double)

    private fun getSunPosition(jd: Double): SunPosition {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(deg2rad(g)) + 0.020 * sin(deg2rad(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = rad2deg(atan2(cos(deg2rad(e)) * sin(deg2rad(l)), cos(deg2rad(l)))) / 15.0
        val fixedRa = fixHour(ra)
        val dec = rad2deg(asin(sin(deg2rad(e)) * sin(deg2rad(l))))
        val eqt = (q / 15.0) - fixedRa
        return SunPosition(dec, eqt)
    }

    private fun calculateAngleTime(angle: Double, lat: Double, dec: Double, ccw: Boolean = false): Double? {
        val top = -sin(deg2rad(angle)) - sin(deg2rad(lat)) * sin(deg2rad(dec))
        val bottom = cos(deg2rad(lat)) * cos(deg2rad(dec))
        val div = top / bottom
        if (div < -1.0 || div > 1.0) return null
        val hourAngle = rad2deg(acos(div)) / 15.0
        return if (ccw) -hourAngle else hourAngle
    }

    private fun calculateAsrTime(factor: Double, lat: Double, dec: Double): Double {
        val diff = abs(lat - dec)
        val angle = -rad2deg(atan(1.0 / (factor + tan(deg2rad(diff)))))
        return calculateAngleTime(angle, lat, dec, false) ?: 3.0
    }

    fun format12Hour(decimalHours: Double): String {
        val totalMinutes = (fixHour(decimalHours) * 60.0).roundToInt()
        var hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        val minutePad = minutes.toString().padStart(2, '0')

        val period = if (hours >= 12) "م" else "ص"
        hours %= 12
        if (hours == 0) hours = 12

        return "$hours:$minutePad $period"
    }

    fun calculatePrayerTimes(
        date: Date,
        lat: Double,
        lng: Double,
        timezone: Double
    ): PrayerTimesResult {
        val cal = Calendar.getInstance().apply { time = date }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val jd = julianDay(year, month, day)
        val sun = getSunPosition(jd)

        val noon = fixHour(12.0 + timezone - (lng / 15.0) - (sun.equationOfTime))
        val dhuhr = noon + (2.0 / 60.0)

        val sunriseOffset = calculateAngleTime(0.8333, lat, sun.declination, true) ?: -6.0
        val sunsetOffset = calculateAngleTime(0.8333, lat, sun.declination, false) ?: 6.0
        val sunrise = fixHour(noon + sunriseOffset)
        val sunset = fixHour(noon + sunsetOffset)

        val fajrOffset = calculateAngleTime(18.5, lat, sun.declination, true) ?: -1.5
        val fajr = fixHour(noon + fajrOffset)

        val asrOffset = calculateAsrTime(1.0, lat, sun.declination)
        val asr = fixHour(noon + asrOffset)

        val maghrib = sunset
        val isha = fixHour(maghrib + 1.5)

        val baseCal = Calendar.getInstance().apply { time = date }
        fun toDate(hoursDec: Double): Date {
            val totalSeconds = (fixHour(hoursDec) * 3600.0).roundToInt()
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            return (baseCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, s)
                set(Calendar.MILLISECOND, 0)
            }.time
        }

        val datesMap = mapOf(
            PrayerKey.FAJR to toDate(fajr),
            PrayerKey.SUNRISE to toDate(sunrise),
            PrayerKey.DHUHR to toDate(dhuhr),
            PrayerKey.ASR to toDate(asr),
            PrayerKey.MAGHRIB to toDate(maghrib),
            PrayerKey.ISHA to toDate(isha)
        )

        return PrayerTimesResult(
            fajr = format12Hour(fajr),
            sunrise = format12Hour(sunrise),
            dhuhr = format12Hour(dhuhr),
            asr = format12Hour(asr),
            maghrib = format12Hour(maghrib),
            isha = format12Hour(isha),
            dates = datesMap
        )
    }
}
