package com.almoazin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almoazin.app.ui.theme.*

@Composable
fun HeroPrayerCard(
    cityName: String,
    countryName: String,
    nextPrayerName: String,
    nextPrayerTime: String,
    hoursRemaining: String,
    minutesRemaining: String,
    secondsRemaining: String,
    isPlayingAthan: Boolean,
    onToggleAthan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BorderLightBlue, RoundedCornerShape(28.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEBF5FF),
                            PureWhite
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFDBEAFE))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "الموقع",
                        tint = RoyalBluePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$cityName ($countryName)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "الصلاة القادمة",
                    fontSize = 14.sp,
                    color = SlateTextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "صلاة $nextPrayerName",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SlateTextPrimary
                )

                Text(
                    text = "يحين الأذان عند الساعة $nextPrayerTime",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AzureBlueSecondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CountdownBox(value = hoursRemaining, label = "ساعة")
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AzureBlueSecondary)
                    CountdownBox(value = minutesRemaining, label = "دقيقة")
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AzureBlueSecondary)
                    CountdownBox(value = secondsRemaining, label = "ثانية", isHighlight = true)
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onToggleAthan,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayingAthan) Color(0xFFEF4444) else RoyalBluePrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlayingAthan) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = PureWhite
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlayingAthan) "إيقاف الأذان الحقيقي" else "سماع الأذان الحقيقي (صوت كامل)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownBox(
    value: String,
    label: String,
    isHighlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isHighlight) Color(0xFFEFF6FF) else Color(0xFFF1F5F9))
            .border(1.dp, if (isHighlight) SoftBorderBlue else Color.Transparent, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .widthIn(min = 54.dp)
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = if (isHighlight) RoyalBluePrimary else SlateTextPrimary
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = SlateTextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}
