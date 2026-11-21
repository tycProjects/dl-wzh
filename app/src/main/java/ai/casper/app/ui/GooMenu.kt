package ai.casper.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class GooAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

// Goo Menu: по нажатию «+» пузырьки вылетают веером на пружинах и собираются обратно.
// Эффект слияния пузырьков (goo-фильтр из демо) здесь не повторён: это отдельная графическая работа.
@Composable
fun GooMenu(actions: List<GooAction>, enabled: Boolean) {
    var open by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(open) {
        progress.animateTo(
            if (open) 1f else 0f,
            if (open) springFloat(230f, 15f) else springFloat(520f, 34f)
        )
    }

    val radiusPx = with(density) { 84.dp.toPx() }
    val centerPx = with(density) { 24.dp.toPx() }
    val halfBubblePx = with(density) { 22.dp.toPx() }
    val p = progress.value

    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        actions.forEachIndexed { i, action ->
            val angle = Math.toRadians(-90.0 - 40.0 * (actions.size - 1) / 2.0 + 40.0 * i)
            val x = (cos(angle) * radiusPx * p).toFloat()
            val y = (sin(angle) * radiusPx * p).toFloat()
            Box(
                Modifier
                    .offset { IntOffset((centerPx + x - halfBubblePx).roundToInt(), (centerPx + y - halfBubblePx).roundToInt()) }
                    .size(44.dp)
                    .graphicsLayer {
                        alpha = p
                        val s = 0.6f + 0.4f * p
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(Casper.Fill3)
                    .clickable(enabled = open) {
                        open = false
                        action.onClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(action.icon, contentDescription = action.label, tint = Casper.Ink, modifier = Modifier.size(20.dp))
            }
        }
        IconButton(
            onClick = { if (enabled) open = !open },
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Casper.Inverse)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Меню",
                tint = Casper.OnInverse,
                modifier = Modifier.graphicsLayer { rotationZ = p * 45f }
            )
        }
    }
}
