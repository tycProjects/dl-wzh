package com.android.island.island

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.island.core.Controls
import com.android.island.core.IslandActivity
import kotlinx.coroutines.delay

private val CallGreen = Color(0xFF34C759)
private val CallRed = Color(0xFFE53935)

/**
 * Expanded card for a phone call: caller, status, and answer/decline buttons.
 * Buttons fire the notification's own PendingIntents, so the system handles the call.
 */
@Composable
fun CallExpanded(call: IslandActivity.Call, widthDp: Dp) {
    val context = LocalContext.current
    val ongoing = !call.isIncoming
    var elapsedSec by remember(call.id) { mutableStateOf(0L) }
    // Only an active call needs a clock, and it ticks once per second while visible.
    LaunchedEffect(call.id, ongoing) {
        if (!ongoing) return@LaunchedEffect
        while (true) {
            com.android.island.core.ScreenState.awaitInteractive()
            elapsedSec = (System.currentTimeMillis() - call.timestamp) / 1000L
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier.width(widthDp).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val avatar = call.avatar
        if (avatar != null) {
            Image(
                bitmap = remember(avatar) { avatar.asImageBitmap() },
                contentDescription = null,
                modifier = Modifier.size(46.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(CallGreen.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, tint = CallGreen, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            text = call.callerName,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        val status = when {
            call.isIncoming -> call.subtitle.ifBlank { "Panggilan masuk" }
            else -> "%02d:%02d".format(elapsedSec / 60L, elapsedSec % 60L)
        }
        Text(
            text = status,
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(36.dp), verticalAlignment = Alignment.CenterVertically) {
            CallButton(
                color = CallRed,
                icon = Icons.Filled.CallEnd,
                label = if (call.isIncoming) "Tolak" else "Akhiri",
                onClick = { Controls.fireAction(context, call.declineAction ?: call.contentIntent) }
            )
            if (call.isIncoming && call.answerAction != null) {
                CallButton(
                    color = CallGreen,
                    icon = Icons.Filled.Call,
                    label = "Terima",
                    onClick = { Controls.fireAction(context, call.answerAction) }
                )
            }
        }
    }
}

@Composable
private fun CallButton(color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(54.dp).clip(CircleShape).background(color).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}
