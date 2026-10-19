package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

object HabitIconHelper {

    val availableIcons = listOf(
        "flame" to "Вогник",
        "water" to "Вода",
        "fitness" to "Спорт",
        "run" to "Біг",
        "walk" to "Прогулянка",
        "book" to "Книга",
        "meditate" to "Медитація",
        "sleep" to "Сон",
        "code" to "Код/Робота",
        "heart" to "Здоров'я",
        "smile" to "Настрій",
        "star" to "Ціль"
    )

    fun getIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "flame" -> Icons.Default.Whatshot
            "water" -> Icons.Default.WaterDrop
            "fitness" -> Icons.Default.FitnessCenter
            "run" -> Icons.Default.DirectionsRun
            "walk" -> Icons.Default.DirectionsWalk
            "book" -> Icons.Default.MenuBook
            "meditate" -> Icons.Default.SelfImprovement
            "sleep" -> Icons.Default.Bedtime
            "code" -> Icons.Default.Code
            "heart" -> Icons.Default.Favorite
            "smile" -> Icons.Default.SentimentSatisfied
            "star" -> Icons.Default.Star
            else -> Icons.Default.AutoAwesome
        }
    }
}
