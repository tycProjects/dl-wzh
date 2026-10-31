package com.fakesocial.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakesocial.app.R
import com.fakesocial.app.data.Platform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onPlatform: (Platform) -> Unit, onHistory: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fake_social", Context.MODE_PRIVATE) }
    var showDisclaimer by remember { mutableStateOf(!prefs.getBoolean("ack", false)) }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.disclaimer_title)) },
            text = { Text(stringResource(R.string.disclaimer_body)) },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putBoolean("ack", true).apply()
                    showDisclaimer = false
                }) { Text(stringResource(R.string.disclaimer_ok)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = { TextButton(onClick = onHistory) { Text(stringResource(R.string.history)) } }
            )
        }
    ) { inner ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(inner)
        ) {
            items(Platform.entries.toList()) { p -> PlatformCard(p) { onPlatform(p) } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlatformCard(platform: Platform, onClick: () -> Unit) {
    val accent = Color(platform.accent)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    platform.badge,
                    color = if (accent.luminance() > 0.6f) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Text(platform.label, fontWeight = FontWeight.SemiBold)
        }
    }
}
