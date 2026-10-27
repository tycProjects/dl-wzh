package com.game.play.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import coil.compose.AsyncImage
import com.game.play.data.AppSettings
import com.game.play.domain.model.AppInfo
import com.game.play.domain.model.DlState
import com.game.play.domain.model.InstalledInfo
import com.game.play.domain.model.hasUpdate
import java.io.File

val Amber = Color(0xFFFFB020)
val Green = Color(0xFF2BC6A0)
val Red = Color(0xFFD74444)

@Composable
fun cardBorder(): BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

@Composable
fun AppIcon(url: String?, name: String, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(name.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun Avatar(settings: AppSettings, size: Dp) {
    val file = if (settings.photoPath.isNotBlank()) File(settings.photoPath) else null
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(18.dp)).background(Amber),
        contentAlignment = Alignment.Center
    ) {
        if (file != null && file.exists()) {
            AsyncImage(model = file, contentDescription = "Foto profil", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF17324D), modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** Install / Perbarui / Buka. "Buka" hanya muncul kalau aplikasinya sudah terpasang. */
@Composable
fun InstallActions(
    app: AppInfo,
    installed: InstalledInfo?,
    dl: DlState,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    compact: Boolean = false
) {
    val padding = if (compact) PaddingValues(horizontal = 14.dp, vertical = 4.dp) else ButtonDefaults.ContentPadding
    val update = app.hasUpdate(installed)
    val content: @Composable () -> Unit = {
        if (installed == null) {
            Button(onClick = onInstall, enabled = !dl.busy, contentPadding = padding) {
                Text(if (dl.busy) "Mengunduh…" else "Install")
            }
        } else {
            if (update) {
                Button(onClick = onInstall, enabled = !dl.busy, contentPadding = padding) {
                    Text(if (dl.busy) "Mengunduh…" else "Perbarui")
                }
            }
            OutlinedButton(onClick = onOpen, contentPadding = padding) { Text("Buka") }
        }
    }
    if (compact) {
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}
