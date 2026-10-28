package com.greatnice.gts1550control.bluetooth

import android.bluetooth.BluetoothProfile

data class ScanDevice(
    val address: String,
    val name: String,
    val bonded: Boolean,
    val rssi: Int?,
    /** Name heuristic only (contains "GTS" or "GREATNICE"). Not proof of identity. */
    val nameLooksLikeTarget: Boolean
)

data class DeviceReport(
    val name: String,
    val address: String,
    val bondState: String,
    val transport: String,
    val deviceClass: String,
    val uuids: List<String>
)

data class BtUiState(
    val adapterPresent: Boolean = true,
    val adapterEnabled: Boolean = false,
    val scanning: Boolean = false,
    val devices: List<ScanDevice> = emptyList(),
    val selectedAddress: String? = null,
    val selectedName: String? = null,
    val selectedBonded: Boolean = false,
    val a2dpState: Int = BluetoothProfile.STATE_DISCONNECTED,
    val a2dpConnectedNames: List<String> = emptyList(),
    val report: DeviceReport? = null,
    val gattRunning: Boolean = false,
    /** Non-null only if the speaker really exposes the standard GATT Battery Service (0x180F). */
    val gattBatteryPercent: Int? = null,
    val log: List<String> = emptyList(),
    val status: String = "Idle"
)
