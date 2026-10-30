package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet

@Composable
fun CategoryChip(
    category: CategoryEntity,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .testTag("category_chip_${category.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 13.sp
        )
    }
}
