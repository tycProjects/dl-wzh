package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StreakInfo
import com.example.ui.theme.FlameAmber
import com.example.ui.theme.FlameCrimson
import com.example.ui.theme.FlameGold
import com.example.ui.theme.FlameOrange

@Composable
fun FireStreakBanner(
    streakInfo: StreakInfo,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "FlameGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2C1408),
            Color(0xFF1E1322),
            Color(0xFF15151B)
        )
    )

    val flameColors = listOf(
        FlameGold,
        FlameOrange,
        FlameCrimson
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fire_streak_banner")
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        FlameOrange.copy(alpha = 0.7f),
                        FlameAmber.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(backgroundGradient)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(FlameOrange.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${streakInfo.fireTierEmoji} ${streakInfo.fireTierTitle}",
                                color = FlameAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "${streakInfo.currentStreak}",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                lineHeight = 44.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (streakInfo.currentStreak == 1) "ДЕНЬ СЕРІЇ" else if (streakInfo.currentStreak in 2..4) "ДНІ СЕРІЇ" else "ДНІВ СЕРІЇ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlameAmber,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // Pulsing Flame Visual
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                    ) {
                        // Ambient Glow
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            FlameOrange.copy(alpha = glowAlpha),
                                            FlameCrimson.copy(alpha = glowAlpha * 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Вогник серії",
                            tint = if (streakInfo.currentStreak > 0) FlameOrange else Color.Gray,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Daily Progress Bar & Subtext
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Сьогодні: ${streakInfo.completedToday} з ${streakInfo.totalTodayHabits} виконано",
                        fontSize = 13.sp,
                        color = Color(0xFFDDD9E4),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(streakInfo.todayCompletionRate * 100).toInt()}%",
                        fontSize = 13.sp,
                        color = FlameGold,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { streakInfo.todayCompletionRate.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = FlameOrange,
                    trackColor = Color(0x33FFFFFF)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Motivational quote
                Text(
                    text = streakInfo.motivationalQuote,
                    fontSize = 12.sp,
                    color = Color(0xFFB8B4C0),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
