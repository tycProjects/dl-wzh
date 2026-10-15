package com.almoazin.app.utils

import kotlin.math.*

object QiblaCalculator {
    const val KAABA_LAT = 21.422487
    const val KAABA_LNG = 39.826206

    private fun deg2rad(deg: Double) = deg * Math.PI / 180.0
    private fun rad2deg(rad: Double) = rad * 180.0 / Math.PI

    fun calculateBearing(userLat: Double, userLng: Double): Float {
        val phi1 = deg2rad(userLat)
        val phi2 = deg2rad(KAABA_LAT)
        val deltaLambda = deg2rad(KAABA_LNG - userLng)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        var bearing = rad2deg(atan2(y, x))
        bearing = (bearing + 360.0) % 360.0
        return bearing.toFloat()
    }

    fun calculateDistanceKm(userLat: Double, userLng: Double): Int {
        val earthRadiusKm = 6371.0
        val dLat = deg2rad(KAABA_LAT - userLat)
        val dLng = deg2rad(KAABA_LNG - userLng)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(deg2rad(userLat)) * cos(deg2rad(KAABA_LAT)) *
                sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusKm * c).roundToInt()
    }
}
