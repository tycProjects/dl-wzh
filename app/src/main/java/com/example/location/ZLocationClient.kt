package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.data.LocationUiState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ZLocationClient(private val context: Context) {

    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationUiState {
        if (!hasLocationPermission()) {
            return LocationUiState.PermissionDenied(isPermanent = false)
        }
        if (!isGpsEnabled()) {
            return LocationUiState.GpsDisabled
        }

        return try {
            val cts = CancellationTokenSource()
            val location: Location? = try {
                fusedLocationProviderClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).awaitTask()
            } catch (e: Exception) {
                null
            } ?: fusedLocationProviderClient.lastLocation.awaitTask()

            if (location != null) {
                LocationUiState.Success(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    isAccurate = location.accuracy <= 50f
                )
            } else {
                LocationUiState.Error("Unable to acquire location fix. Please try again.")
            }
        } catch (e: SecurityException) {
            LocationUiState.PermissionDenied(isPermanent = false)
        } catch (e: Exception) {
            LocationUiState.Error(e.message ?: "Failed to get location")
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T? = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }
}
