package com.almoazin.app.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almoazin.app.ui.theme.*

@Composable
fun TasbihScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var count by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    val target = 33

    val presets = listOf(
        "سُبْحَانَ اللَّهِ",
        "الْحَمْدُ لِلَّهِ",
        "لَا إِلَهَ إِلَّا اللَّهُ",
        "اللَّهُ أَكْبَرُ",
        "أَسْتَغْفِرُ اللَّهَ",
        "اللَّهُمَّ صَلِّ عَلَى نَبِيِّنَا مُحَمَّدٍ"
    )
    var selectedPresetIndex by remember { mutableStateOf(0) }

    fun vibratePhone() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(35)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(SkyBackground)
            .padding(20.dp)
    ) {
        Text(
            text = "السبحة الإلكترونية الذكية",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderLightBlue, RoundedCornerShape(20.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = presets[selectedPresetIndex],
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = RoyalBluePrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.take(3).forEachIndexed { idx, txt ->
                        FilterChip(
                            selected = selectedPresetIndex == idx,
                            onClick = { selectedPresetIndex = idx; count = 0 },
                            label = { Text(txt.take(10) + "..", fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .background(PureWhite)
                .border(6.dp, RoyalBluePrimary, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    vibratePhone()
                    count++
                    totalCount++
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$count",
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Black,
                    color = RoyalBluePrimary
                )
                Text(
                    text = "من $target",
                    fontSize = 14.sp,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "المس للتسبيح",
                    fontSize = 11.sp,
                    color = AzureBlueSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(PureWhite)
                .border(1.dp, BorderLightBlue, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Text("إجمالي التسبيحات", fontSize = 11.sp, color = SlateTextSecondary)
                Text("$totalCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
            }

            IconButton(
                onClick = { count = 0 },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "تصفير", tint = RoyalBluePrimary)
            }
        }
    }
}
