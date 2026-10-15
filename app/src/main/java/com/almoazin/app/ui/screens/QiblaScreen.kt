package com.almoazin.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almoazin.app.ui.theme.*
import com.almoazin.app.utils.CompassSensorManager
import com.almoazin.app.utils.QiblaCalculator
import kotlin.math.abs

@Composable
fun QiblaScreen(
    userLat: Double,
    userLng: Double,
    sensorManager: CompassSensorManager,
    modifier: Modifier = Modifier
) {
    val phoneAzimuth by sensorManager.azimuth.collectAsState()
    val qiblaBearing = remember(userLat, userLng) {
        QiblaCalculator.calculateBearing(userLat, userLng)
    }
    val distanceKm = remember(userLat, userLng) {
        QiblaCalculator.calculateDistanceKm(userLat, userLng)
    }

    val relativeAngle = (qiblaBearing - phoneAzimuth + 360f) % 360f
    val animatedAngle by animateFloatAsState(targetValue = relativeAngle, label = "qiblaRotation")
    val isAligned = abs(relativeAngle) < 3.5f || abs(relativeAngle - 360f) < 3.5f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(SkyBackground)
            .padding(20.dp)
    ) {
        Text(
            text = "بوصلة القبلة المشرفة",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = SlateTextPrimary
        )
        Text(
            text = "وجّه هاتفك أفقياً حتى يتطابق المؤشر مع الكعبة",
            fontSize = 13.sp,
            color = SlateTextSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .background(PureWhite)
                .border(
                    width = if (isAligned) 3.dp else 1.5.dp,
                    color = if (isAligned) EmeraldGreenAccent else BorderLightBlue,
                    shape = CircleShape
                )
        ) {
            Canvas(modifier = Modifier.size(260.dp)) {
                drawCircle(color = Color(0xFFF8FAFC), radius = size.width / 2)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(animatedAngle)
            ) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .size(36.dp, 48.dp)
                ) {
                    val path = Path().apply {
                        moveTo(size.width / 2, 0f)
                        lineTo(size.width, size.height)
                        lineTo(size.width / 2, size.height * 0.75f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = if (isAligned) EmeraldGreenAccent else RoyalBluePrimary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${qiblaBearing.toInt()}°",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isAligned) EmeraldGreenAccent else RoyalBluePrimary
                )
                Text(
                    text = if (isAligned) "أنت باتجاه القبلة!" else "زاوية القبلة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAligned) EmeraldGreenAccent else SlateTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderLightBlue, RoundedCornerShape(20.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Column {
                    Text("المسافة المباشرة إلى الكعبة المشرفة", fontSize = 12.sp, color = SlateTextSecondary)
                    Text("$distanceKm كم", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("مكة المكرمة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                }
            }
        }
    }
}
