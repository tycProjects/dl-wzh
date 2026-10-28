package com.greatnice.gts1550control.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greatnice.gts1550control.MainViewModel

@Composable
fun DiagnosticsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val bt by vm.btState.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NeonCard(accent = Neon.Warn) {
            Label("DIAGNOSTIC MODE — READ ONLY", Neon.Warn)
            Body(
                "Nothing here writes to the speaker or guesses commands. It only reads what the speaker advertises through documented Android APIs.",
                Neon.Dim, 13, Modifier.padding(top = 8.dp)
            )
        }

        val r = bt.report
        NeonCard {
            Label("SELECTED DEVICE")
            if (r == null) {
                Body("Select a device on the DEVICES tab.", Neon.Dim, 13, Modifier.padding(top = 8.dp))
            } else {
                Body("${r.name}  (${r.address})", modifier = Modifier.padding(top = 8.dp))
                Body("Bond: ${r.bondState}", Neon.Dim, 13)
                Body("Transport: ${r.transport}", Neon.Dim, 13)
                Body("Class: ${r.deviceClass}", Neon.Dim, 13)
                Label("SDP SERVICE UUIDs", Neon.Blue)
                if (r.uuids.isEmpty()) {
                    Body("None cached. Pair the device, then tap REFRESH SDP.", Neon.Dim, 13, Modifier.padding(top = 4.dp))
                }
                r.uuids.forEach {
                    Body(it, if (it.startsWith("NON-STANDARD")) Neon.Warn else Neon.Text, 12, Modifier.padding(top = 4.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton("REFRESH SDP", Modifier.weight(1f), enabled = bt.selectedAddress != null) { vm.bt.fetchUuids() }
            }
        }

        NeonCard {
            Label("BLE GATT PROBE (READ-ONLY)")
            Body(
                "Connects over BLE, lists services and reads only the standard Battery Level and Device Information strings. Most low-cost classic speakers expose no BLE at all; a failed probe is a valid answer.",
                Neon.Dim, 12, Modifier.padding(top = 8.dp)
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton("RUN PROBE", Modifier.weight(1f), enabled = bt.selectedAddress != null && !bt.gattRunning) { vm.bt.startGattProbe() }
                NeonButton("STOP", Modifier.weight(1f), enabled = bt.gattRunning, accent = Neon.Warn) { vm.bt.stopGattProbe() }
            }
        }

        NeonCard {
            Label("LOG")
            Text(
                text = if (bt.log.isEmpty()) "(empty)" else bt.log.joinToString("\n"),
                color = Neon.Text,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Neon.Bg)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonButton("SHARE LOG", Modifier.weight(1f), enabled = bt.log.isNotEmpty()) {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, bt.log.joinToString("\n"))
                    }
                    ctx.startActivity(Intent.createChooser(send, "Share diagnostic log").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                NeonButton("CLEAR", Modifier.weight(1f), accent = Neon.Dim) { vm.bt.clearLog() }
            }
        }

        NeonCard {
            Label("HOW TO INVESTIGATE SAFELY")
            Body(
                "1. Check whether the speaker has a companion app or manual listing app features. If none exists, there is probably no remote protocol to find.\n" +
                "2. Developer options → \"Enable Bluetooth HCI snoop log\", toggle Bluetooth, use the official app (if any) with the speaker, then pull the log with a bug report and open it in Wireshark on a PC.\n" +
                "3. Look only at traffic the official app sent. Do not replay or mutate packets, and do not send guessed bytes — an unknown write can brick firmware or change pairing state.\n" +
                "4. Use only your own speaker. Do not attempt to bypass pairing or encryption.",
                Neon.Dim, 12, Modifier.padding(top = 8.dp)
            )
        }
    }
}
