package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _systemOnline = MutableStateFlow(checkCurrentConnectivity())
    private val _manualOfflineOverride = MutableStateFlow(false)

    val manualOfflineOverride: StateFlow<Boolean> = _manualOfflineOverride.asStateFlow()

    val isOnlineFlow = combine(_systemOnline, _manualOfflineOverride) { sysOnline, manualOff ->
        sysOnline && !manualOff
    }

    val isCurrentlyOnline: Boolean
        get() = _systemOnline.value && !_manualOfflineOverride.value

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _systemOnline.value = checkCurrentConnectivity()
        }

        override fun onLost(network: Network) {
            _systemOnline.value = checkCurrentConnectivity()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _systemOnline.value =
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (_: Exception) {
            // Fallback to initial connectivity state if callback registration fails
        }
    }

    fun setManualOfflineMode(enabled: Boolean) {
        _manualOfflineOverride.value = enabled
    }

    private fun checkCurrentConnectivity(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }
}
