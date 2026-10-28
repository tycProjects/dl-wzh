package com.greatnice.gts1550control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greatnice.gts1550control.MainViewModel

private enum class FStatus(val label: String, val color: Color) {
    STANDARD("STANDARD BLUETOOTH • UNVERIFIED ON HARDWARE", Neon.Cyan),
    PARTIAL("PARTIAL", Neon.Warn),
    UNSUPPORTED("NOT SUPPORTED", Neon.Bad),
    VERIFIED("FOUND ON THIS SPEAKER", Neon.Ok)
}

private data class Feature(val name: String, val status: FStatus, val note: String)

@Composable
fun FeaturesScreen(vm: MainViewModel) {
    val bt by vm.btState.collectAsStateWithLifecycle()

    val features = buildList {
        add(Feature("Discovery & pairing", FStatus.STANDARD,
            "Classic Bluetooth discovery + Android's system pairing dialog."))
        add(Feature("Audio streaming (A2DP)", FStatus.STANDARD,
            "Handled by Android once paired. Connection state is shown read-only."))
        add(Feature("Play / pause / next / previous", FStatus.STANDARD,
            "Media-key events to the phone's active player. Independent of the speaker's own buttons."))
        add(Feature("Volume", FStatus.PARTIAL,
            "Phone media volume. Speaker amp follows only if it supports AVRCP Absolute Volume (unknown)."))
        add(Feature("Connect / disconnect", FStatus.PARTIAL,
            "No public API for apps to connect or drop A2DP. Handled via system Bluetooth settings."))
        val pct = bt.gattBatteryPercent
        if (pct != null) {
            add(Feature("Battery level", FStatus.VERIFIED,
                "$pct % via the standard GATT Battery Service, read during the diagnostic probe."))
        } else {
            add(Feature("Battery level", FStatus.UNSUPPORTED,
                "No documented source found. Shown only if the DIAG probe finds the standard GATT Battery Service."))
        }
        add(Feature("Bass boost / EQ", FStatus.UNSUPPORTED,
            "Any such feature would be inside the speaker's DSP; no known remote protocol. Use the speaker's own controls."))
        add(Feature("FM radio, USB / TF mode", FStatus.UNSUPPORTED,
            "Selected with the speaker's physical mode button. No known remote command."))
        add(Feature("LED / light effects", FStatus.UNSUPPORTED,
            "No known remote command. Use the speaker's own button."))
        add(Feature("TWS pairing", FStatus.UNSUPPORTED,
            "Speaker-to-speaker linking is done on the speakers. No phone-side API or known protocol."))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeonCard(accent = Neon.Warn) {
            Label("HONEST STATUS", Neon.Warn)
            Body(
                "Nothing in this app has been verified on a real GTS-1550. Only ordinary Bluetooth audio features are wired up. Everything else is marked unsupported rather than faked.",
                Neon.Dim, 13, Modifier.padding(top = 8.dp)
            )
        }
        features.forEach { f ->
            NeonCard(accent = f.status.color) {
                Body(f.name, Neon.Text, 15)
                Chip(f.status.label, f.status.color)
                Body(f.note, Neon.Dim, 12, Modifier.padding(top = 8.dp))
            }
        }
    }
}
