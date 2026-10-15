package com.almoazin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almoazin.app.data.PrayerTimesResult
import com.almoazin.app.ui.theme.*

data class PrayerRowItem(
    val nameAr: String,
    val timeFormatted: String,
    val isNext: Boolean = false,
    val isDone: Boolean = false
)

@Composable
fun PrayerTimesGrid(
    times: PrayerTimesResult,
    nextPrayerName: String,
    completedMap: Map<String, Boolean>,
    onToggleComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        PrayerRowItem("الفجر", times.fajr, nextPrayerName == "الفجر", completedMap["الفجر"] == true),
        PrayerRowItem("الشروق", times.sunrise, nextPrayerName == "الشروق", false),
        PrayerRowItem("الظهر", times.dhuhr, nextPrayerName == "الظهر", completedMap["الظهر"] == true),
        PrayerRowItem("العصر", times.asr, nextPrayerName == "العصر", completedMap["العصر"] == true),
        PrayerRowItem("المغرب", times.maghrib, nextPrayerName == "المغرب", completedMap["المغرب"] == true),
        PrayerRowItem("العشاء", times.isha, nextPrayerName == "العشاء", completedMap["العشاء"] == true)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "مواقيت الصلوات الخمس اليوم",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )

        items.forEach { prayer ->
            PrayerCardRow(
                prayer = prayer,
                onToggleCheck = { onToggleComplete(prayer.nameAr) }
            )
        }
    }
}

@Composable
private fun PrayerCardRow(
    prayer: PrayerRowItem,
    onToggleCheck: () -> Unit
) {
    val isHighlight = prayer.isNext

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) Color(0xFFEFF6FF) else PureWhite
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isHighlight) 1.5.dp else 1.dp,
                color = if (isHighlight) RoyalBluePrimary else BorderLightBlue,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (prayer.nameAr != "الشروق") {
                    IconButton(
                        onClick = onToggleCheck,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (prayer.isDone) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = "تمت الصلاة",
                            tint = if (prayer.isDone) EmeraldGreenAccent else SlateTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = prayer.nameAr,
                    fontSize = 16.sp,
                    fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (isHighlight) RoyalBluePrimary else SlateTextPrimary
                )

                if (isHighlight) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RoyalBluePrimary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "القادمة",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                }
            }

            Text(
                text = prayer.timeFormatted,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isHighlight) RoyalBluePrimary else SlateTextPrimary
            )
        }
    }
}
