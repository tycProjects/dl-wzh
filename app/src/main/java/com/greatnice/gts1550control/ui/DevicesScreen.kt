package com.greatnice.gts1550control.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greatnice.gts1550control.MainViewModel

@Composable
fun DevicesScreen(vm: MainViewModel) {
    val bt by vm.btState.collectAsStateWithLifecycle()
    val enableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.bt.refresh()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NeonCard {
            Label("DISCOVERY")
            Body(bt.status, Neon.Dim, 13, Modifier.padding(top = 6.dp))
            if (!bt.adapterPresent) {
                Body("This device has no Bluetooth adapter.", Neon.Bad, 13, Modifier.padding(top = 6.dp))
            } else if (!bt.adapterEnabled) {
                NeonButton("TURN BLUETOOTH ON", Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonButton(
                        if (bt.scanning) "STOP SCAN" else "SCAN",
                        Modifier.weight(1f)
                    ) { if (bt.scanning) vm.bt.stopScan() else vm.bt.startScan() }
                    NeonButton(
                        "PAIR",
                        Modifier.weight(1f),
                        enabled = bt.selectedAddress != null && !bt.selectedBonded
                    ) { vm.bt.pairSelected() }
                }
            }
            Body(
                "Put the speaker into Bluetooth pairing mode using its own buttons first (see its manual). Pairing uses Android's normal system dialog.",
                Neon.Dim, 12, Modifier.padding(top = 8.dp)
            )
        }

        if (bt.devices.isEmpty()) {
            Body("No devices yet. Tap SCAN.", Neon.Dim, 13)
        }
        bt.devices.forEach { d ->
            val selected = d.address == bt.selectedAddress
            val shape = RoundedCornerShape(14.dp)
            Column(
                Modifier.fillMaxWidth()
                    .clip(shape)
                    .background(if (selected) Neon.Surface2 else Neon.Surface)
                    .border(1.dp, if (selected) Neon.Cyan else Neon.Dim.copy(alpha = 0.25f), shape)
                    .clickable { vm.bt.selectAddress(d.address) }
                    .padding(14.dp)
            ) {
                Body(d.name, Neon.Text, 15)
                Body("${d.address}${d.rssi?.let { "   ${it} dBm" } ?: ""}", Neon.Dim, 12)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (d.nameLooksLikeTarget) Chip("NAME MATCHES GTS/GREATNICE", Neon.Cyan)
                    if (d.bonded) Chip("PAIRED", Neon.Ok)
                }
            }
        }
    }
}
