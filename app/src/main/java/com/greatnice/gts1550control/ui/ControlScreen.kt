package com.greatnice.gts1550control.ui

import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greatnice.gts1550control.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ControlScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val bt by vm.btState.collectAsStateWithLifecycle()
    val au by vm.audio.collectAsStateWithLifecycle()

    // Poll so hardware volume keys / external changes are reflected.
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshAudio()
            delay(1500)
        }
    }

    val (stateText, stateColor) = when (bt.a2dpState) {
        BluetoothProfile.STATE_CONNECTED -> "A2DP CONNECTED" to Neon.Ok
        BluetoothProfile.STATE_CONNECTING -> "CONNECTING…" to Neon.Warn
        BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING…" to Neon.Warn
        else -> "NOT CONNECTED" to Neon.Bad
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NeonCard {
            Label("CONNECTION")
            Body(
                bt.selectedName?.let { "$it  (${bt.selectedAddress})" } ?: "No speaker selected — open DEVICES.",
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stateText, stateColor)
                if (bt.selectedBonded) Chip("PAIRED", Neon.Cyan)
            }
            Body("Audio route: ${au.btRoute ?: "not on a Bluetooth A2DP output"}", Neon.Dim, 13, Modifier.padding(top = 10.dp))
            Body("Media playing: ${if (au.musicActive) "yes" else "no"}", Neon.Dim, 13, Modifier.padding(top = 2.dp))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton("BT SETTINGS", Modifier.weight(1f)) {
                    ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                NeonButton("SAFE DISCONNECT", Modifier.weight(1f), accent = Neon.Warn) {
                    vm.playback.pause()
                    ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            Body(
                "Android has no public API for an app to drop an A2DP link. SAFE DISCONNECT pauses playback, then opens system Bluetooth settings where you tap the speaker's gear icon → Disconnect.",
                Neon.Dim, 12, Modifier.padding(top = 8.dp)
            )
        }

        NeonCard {
            Label("TRANSPORT")
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton("◀◀", Modifier.weight(1f)) { vm.playback.previous() }
                NeonButton("▶ / ❙❙", Modifier.weight(1.4f)) { vm.playback.playPause() }
                NeonButton("▶▶", Modifier.weight(1f)) { vm.playback.next() }
            }
            Body(
                "Sends standard media-key events to whatever media app is active on the phone.",
                Neon.Dim, 12, Modifier.padding(top = 10.dp)
            )
        }

        NeonCard {
            Label("VOLUME")
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Body("${au.volume} / ${au.max}", Neon.Cyan, 16)
            }
            Slider(
                value = au.volume.toFloat(),
                onValueChange = { vm.setVolume(it.roundToInt()) },
                valueRange = 0f..au.max.toFloat(),
                steps = (au.max - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = Neon.Cyan,
                    activeTrackColor = Neon.Cyan,
                    inactiveTrackColor = Neon.Surface2
                )
            )
            Body(
                "Sets the phone's media volume. Whether this also changes the speaker's own amplifier depends on the speaker supporting AVRCP Absolute Volume — not verified for the GTS-1550. If the physical speaker volume knob stays loud, that is why.",
                Neon.Dim, 12
            )
        }
    }
}
