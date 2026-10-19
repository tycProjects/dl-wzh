package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CategoryHealth
import com.example.ui.theme.CategoryLearning
import com.example.ui.theme.CategoryMind
import com.example.ui.theme.CategoryOther
import com.example.ui.theme.CategoryProductivity
import com.example.ui.theme.CategoryRoutine
import com.example.ui.theme.CategorySport

enum class HabitCategory(
    val title: String,
    val defaultColor: Color,
    val iconName: String
) {
    HEALTH("Здоров'я", CategoryHealth, "water"),
    SPORT("Спорт", CategorySport, "fitness"),
    MIND("Ментальне", CategoryMind, "meditate"),
    PRODUCTIVITY("Продуктивність", CategoryProductivity, "code"),
    LEARNING("Розвиток", CategoryLearning, "book"),
    ROUTINE("Рутина", CategoryRoutine, "routine"),
    OTHER("Інше", CategoryOther, "flame");

    companion object {
        fun fromString(value: String): HabitCategory {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                OTHER
            }
        }
    }
}
