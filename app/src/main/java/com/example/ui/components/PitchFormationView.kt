package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Club
import com.example.model.Player
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.parseHexColor

@Composable
fun PitchFormationView(
    club: Club,
    selectedPlayerId: String?,
    onPlayerClick: (Player) -> Unit,
    modifier: Modifier = Modifier,
) {
    val starters = club.startingXiPlayers()
    val slots = club.formation.slots
    val clubPrimary = parseHexColor(club.primaryColorHex)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B2A1E),
                        Color(0xFF0E3626),
                        Color(0xFF0B2A1E),
                    ),
                ),
            )
            .border(1.5.dp, Color(0xFF1F543E), RoundedCornerShape(18.dp)),
    ) {
        val widthDp = maxWidth
        val heightDp = maxHeight

        // Pitch markings canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val lineColor = Color.White.copy(alpha = 0.16f)
            val stroke = Stroke(width = 2.dp.toPx())

            // Outer boundary
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.04f, h * 0.03f),
                size = Size(w * 0.92f, h * 0.94f),
                style = stroke,
            )
            // Halfway line
            drawLine(
                color = lineColor,
                start = Offset(w * 0.04f, h * 0.50f),
                end = Offset(w * 0.96f, h * 0.50f),
                strokeWidth = 2.dp.toPx(),
            )
            // Center circle
            drawCircle(
                color = lineColor,
                radius = w * 0.13f,
                center = Offset(w * 0.50f, h * 0.50f),
                style = stroke,
            )
            // Top penalty box
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.26f, h * 0.03f),
                size = Size(w * 0.48f, h * 0.14f),
                style = stroke,
            )
            // Bottom penalty box
            drawRect(
                color = lineColor,
                topLeft = Offset(w * 0.26f, h * 0.83f),
                size = Size(w * 0.48f, h * 0.14f),
                style = stroke,
            )
        }

        // Player nodes positioned according to formation slots
        slots.forEachIndexed { index, slot ->
            val player = starters.getOrNull(index) ?: return@forEachIndexed
            val isSelected = player.id == selectedPlayerId
            val isOutOfPosition = player.position != slot.position

            val nodeWidth = 68.dp
            val nodeHeight = 64.dp
            val xOffset = (widthDp * slot.xFraction) - (nodeWidth / 2)
            val yOffset = (heightDp * slot.yFraction) - (nodeHeight / 2)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(x = xOffset.coerceIn(2.dp, widthDp - nodeWidth - 2.dp), y = yOffset.coerceIn(2.dp, heightDp - nodeHeight - 2.dp))
                    .width(nodeWidth)
                    .height(nodeHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onPlayerClick(player) }
                    .testTag("pitch_player_${player.id}")
                    .padding(2.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) TurfEmerald else clubPrimary)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.5.dp,
                            color = when {
                                isSelected -> Color.White
                                isOutOfPosition -> BroadcastAmber
                                else -> Color.White.copy(alpha = 0.75f)
                            },
                            shape = CircleShape,
                        ),
                ) {
                    Text(
                        text = player.overall.toString(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) PitchBlack else Color.White,
                    )
                }

                // Name + Role pill
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) TurfEmerald.copy(alpha = 0.92f)
                            else Color(0xCC090D14),
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    val lastName = player.name.substringAfterLast(' ')
                    Text(
                        text = "${slot.roleLabel} $lastName",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (isSelected) PitchBlack else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Tiny fitness bar
                val fitColor = when {
                    player.fitness >= 80 -> TurfEmerald
                    player.fitness >= 65 -> BroadcastAmber
                    else -> CrimsonRed
                }
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .width(34.dp)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Box(
                        modifier = Modifier
                            .width(34.dp * (player.fitness / 100f))
                            .height(3.dp)
                            .background(fitColor),
                    )
                }
            }
        }
    }
}
