package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NexoraBlue
import com.example.ui.theme.NexoraSuccess
import com.example.ui.theme.NexoraViolet
import com.example.util.formatCurrency
import com.example.util.formatShortCurrency
import com.example.viewmodel.ChartPoint
import kotlin.math.max

@Composable
fun NexoraSalesChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(points) { mutableStateOf(points.lastIndex.coerceAtLeast(0)) }
    val maxRevenue = remember(points) { max(1.0, points.maxOfOrNull { it.revenue } ?: 1.0) }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val selectedPoint = points.getOrNull(selectedIndex)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedPoint != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Periode: ${selectedPoint.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Omzet: ${formatCurrency(selectedPoint.revenue)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NexoraBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Laba: ${formatShortCurrency(selectedPoint.profit)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NexoraSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (points.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Belum ada data grafik penjualan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val animFactors = points.map { point ->
                val target = (point.revenue / maxRevenue).toFloat().coerceIn(0.03f, 1f)
                animateFloatAsState(
                    targetValue = target,
                    animationSpec = tween(durationMillis = 450),
                    label = "bar_${point.label}"
                ).value
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty()) {
                                val slotWidth = size.width.toFloat() / points.size
                                val idx = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                                selectedIndex = idx
                            }
                        }
                    }
            ) {
                val h = size.height
                val w = size.width
                // Draw 3 horizontal guide lines
                listOf(0.25f, 0.55f, 0.85f).forEach { frac ->
                    val y = h * frac
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val slotW = w / points.size
                val barPairW = (slotW * 0.68f).coerceAtMost(38.dp.toPx())
                val singleBarW = (barPairW * 0.52f).coerceAtLeast(6.dp.toPx())
                val profitBarW = (barPairW * 0.36f).coerceAtLeast(4.dp.toPx())
                val gap = 3.dp.toPx()

                points.forEachIndexed { idx, pt ->
                    val centerX = slotW * idx + slotW / 2f
                    val revH = (h * 0.88f) * animFactors[idx]
                    val profRatio = (pt.profit / maxRevenue).toFloat().coerceIn(0.02f, 1f)
                    val profH = (h * 0.88f) * profRatio
                    val isSelected = idx == selectedIndex

                    val revLeft = centerX - (singleBarW + gap + profitBarW) / 2f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            if (isSelected) listOf(NexoraBlue, NexoraViolet)
                            else listOf(NexoraBlue.copy(alpha = 0.72f), NexoraBlue.copy(alpha = 0.45f))
                        ),
                        topLeft = Offset(revLeft, h - revH),
                        size = Size(singleBarW, revH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    drawRoundRect(
                        color = if (isSelected) NexoraSuccess else NexoraSuccess.copy(alpha = 0.55f),
                        topLeft = Offset(revLeft + singleBarW + gap, h - profH),
                        size = Size(profitBarW, profH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                points.forEachIndexed { idx, pt ->
                    Text(
                        text = pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (idx == selectedIndex) NexoraBlue else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = NexoraBlue, label = "Omzet")
                LegendDot(color = NexoraSuccess, label = "Laba Kotor")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
