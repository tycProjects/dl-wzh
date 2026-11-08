package com.kian.samsungfartomizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SamsungBlue = Color(0xFF3E91FF)
private val Bg = Color(0xFF0B0F14)
private val CardBg = Color(0xFF1B2129)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = SamsungBlue,
                    background = Bg,
                    surface = CardBg
                )
            ) {
                FartomizerScreen()
            }
        }
    }
}

@Composable
fun FartomizerScreen() {
    val player = remember { FartPlayer() }
    val haptic = LocalHapticFeedback.current
    var intensity by remember { mutableFloatStateOf(0.7f) }
    DisposableEffect(Unit) { onDispose { player.stop() } }

    fun fire(p: Preset) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        player.play(p, intensity)
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 56.dp, bottom = 8.dp)) {
                    Text(
                        "SamsungFartomizer",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("by Kian", color = SamsungBlue, fontSize = 16.sp)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Intensity", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Slider(
                            value = intensity,
                            onValueChange = { intensity = it },
                            colors = SliderDefaults.colors(
                                thumbColor = SamsungBlue,
                                activeTrackColor = SamsungBlue
                            )
                        )
                        Text(
                            "Higher = louder and longer",
                            color = Color(0xFF8A94A0),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            items(PRESETS) { p ->
                Card(
                    onClick = { fire(p) },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.1f)
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(p.emoji, fontSize = 44.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(p.name, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Button(
                    onClick = { fire(PRESETS.random()) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SamsungBlue)
                ) {
                    Text("🎲  Random", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
        }
    }
}
