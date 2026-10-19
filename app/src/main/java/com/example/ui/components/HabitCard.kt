package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitCategory
import com.example.data.model.HabitItemUi
import com.example.ui.theme.FlameAmber
import com.example.ui.theme.FlameOrange

@Composable
fun HabitCard(
    habitItem: HabitItemUi,
    onToggleCompletion: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitItem.habit
    val category = HabitCategory.fromString(habit.category)
    val habitColor = Color(habit.colorHex)

    val scaleCheck by animateFloatAsState(
        targetValue = if (habitItem.isCompletedToday) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "CheckScale"
    )

    val checkBgColor by animateColorAsState(
        targetValue = if (habitItem.isCompletedToday) FlameOrange else Color.Transparent,
        label = "CheckColor"
    )

    val checkBorderColor by animateColorAsState(
        targetValue = if (habitItem.isCompletedToday) FlameOrange else Color(0xFF6B6875),
        label = "CheckBorder"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (habitItem.isCompletedToday) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge + Streak Pill + Reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(habitColor)
                    )
                    Text(
                        text = category.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = habitColor
                    )

                    if (habit.reminderEnabled) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x22FFA000))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Нагадування активне",
                                tint = FlameAmber,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%02d:%02d", habit.reminderHour, habit.reminderMinute),
                                fontSize = 10.sp,
                                color = FlameAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Individual Habit Streak
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (habitItem.currentStreak > 0) Color(0x33FF6D00) else Color(0x1A888888))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (habitItem.currentStreak > 0) "🔥 ${habitItem.currentStreak} дн." else "0 дн.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (habitItem.currentStreak > 0) FlameOrange else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Row: Icon, Title & Completion Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category/Habit Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(habitColor.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = HabitIconHelper.getIcon(habit.iconName),
                        contentDescription = habit.title,
                        tint = habitColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (habitItem.isCompletedToday) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (habitItem.isCompletedToday) TextDecoration.None else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (habit.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = habit.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Interactive Completion Checkbox Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("habit_toggle_${habit.id}")
                        .clickable(onClick = onToggleCompletion)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .scale(scaleCheck)
                            .clip(CircleShape)
                            .background(checkBgColor)
                            .border(
                                width = 2.dp,
                                color = checkBorderColor,
                                shape = CircleShape
                            )
                    ) {
                        if (habitItem.isCompletedToday) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Виконано",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7-day Mini Activity Trail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                habitItem.last7DaysCompletion.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = day.dayName.take(2),
                            fontSize = 9.sp,
                            color = if (day.isToday) FlameOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day.isCompleted -> FlameOrange
                                        day.isToday -> FlameOrange.copy(alpha = 0.2f)
                                        else -> Color(0x1FFFFFFF)
                                    }
                                )
                                .then(
                                    if (day.isToday) Modifier.border(1.dp, FlameOrange, CircleShape) else Modifier
                                )
                        )
                    }
                }
            }
        }
    }
}
