package com.zewmod.engine.model

import kotlinx.serialization.Serializable

@Serializable
data class AppConfig(
    val name: String = "LOCAL",
    val ip: String = "127.0.0.1",
    val port: Int = 38571,
    val key: String = "local",
    val features: String = "track,head,legit,freeze,scope",
    val owner: Int = 1,
    val option: Int = 1,
    val extra1: Int = 1,
    val extra2: Int = 1,
    val slotMin: Int = 1,
    val slotMax: Int = 10,
    val maxTargets: Int = 10,
    val excludeSelfTeam: Boolean = false,
    val aValues: List<Int> = listOf(1),
    val ping: Int = 1,
    val vpnIp: String = "10.0.0.2",
    val vpnRoute: String = "0.0.0.0/0",
    val vpnDns: String = "1.1.1.1",
    val vpnMtu: Int = 1500,
    val expiryTime: Long = 4102444800000L,
    val sessionToken: String = "local",
    val vpn: VpnConfig = VpnConfig(),
    val pkg: String = "",
    val type: Int = 0,
    val iconRes: Int = 0,
    val procSnapshot: String = "",
    val cpuCores: Int = 0,
    val cpuMaxFreqGhz: Float = 0f,
    val cpuLoadPercent: Float = 0f,
    val ramTotalGb: Float = 0f,
    val ramUsedGb: Float = 0f,
    val storageTotalGb: Float = 0f,
    val storageUsedGb: Float = 0f,
    val storageUsedPercent: Float = 0f,
    val networkPingMs: Int = 0
) {
    fun enabledFeatureList(): List<String> =
        features.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    override fun toString(): String = buildString {
        append("AppConfig(serverName=$name")
        append(", ip=$ip")
        append(", port=$port")
        append(", features=$features")
        append(", owner=$owner")
        append(", option=$option")
        append(", extra1=$extra1")
        append(", extra2=$extra2")
        append(", slotMin=$slotMin")
        append(", slotMax=$slotMax")
        append(", maxTargets=$maxTargets")
        append(", excludeSelfTeam=$excludeSelfTeam")
        append(", aValues=$aValues")
        append(", ping=$ping")
        append(", vpnIp=$vpnIp")
        append(", vpnRoute=$vpnRoute")
        append(", vpnDns=$vpnDns")
        append(", vpnMtu=$vpnMtu")
        append(", sessionToken=$sessionToken")
        append(")")
    }
}

@Serializable
data class VpnConfig(
    val ip: String = "10.0.0.2",
    val route: String = "0.0.0.0/0",
    val dns: String = "1.1.1.1",
    val mtu: Int = 1500
)

@Serializable
data class ServerInfo(
    val name: String,
    val ip: String,
    val port: Int,
    val key: String,
    val features: String,
    val ping: Int = -1,
    val status: ServerStatus = ServerStatus.UNKNOWN,
    val type: Int = 0
)

enum class ServerStatus { ONLINE, OFFLINE, UNKNOWN }

@Serializable
data class GameTarget(
    val id: String,
    val displayName: String,
    val packageName: String,
    val iconRes: Int = 0,
    val available: Boolean = true
)
