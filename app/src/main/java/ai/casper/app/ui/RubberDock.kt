package ai.casper.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class DockTab(val label: String, val icon: ImageVector, val onClick: () -> Unit)

private const val PAD = 6f
private const val SLOT = 60f
private const val TAB = 56f

// Rubber Dock: индикатор «растягивается»: передний край быстрый, задний отстаёт.
@Composable
fun RubberDock(tabs: List<DockTab>, selected: Int) {
    val left = remember { Animatable(PAD + selected * SLOT) }
    val right = remember { Animatable(PAD + selected * SLOT + TAB) }

    LaunchedEffect(selected) {
        val tl = PAD + selected * SLOT
        val tr = tl + TAB
        val movingRight = tl > left.value
        launch { left.animateTo(tl, if (movingRight) springFloat(190f, 19f) else springFloat(640f, 38f)) }
        launch { right.animateTo(tr, if (movingRight) springFloat(640f, 38f) else springFloat(190f, 19f)) }
    }

    Box(Modifier.fillMaxWidth().padding(bottom = 10.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width((PAD * 2 + SLOT * (tabs.size - 1) + TAB).dp)
                .height(68.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(Casper.Panel)
        ) {
            Box(
                Modifier
                    .offset(x = left.value.dp, y = 6.dp)
                    .width((right.value - left.value).dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Casper.Inverse)
            )
            tabs.forEachIndexed { i, tab ->
                val isSelected = i == selected
                val pop by animateFloatAsState(
                    targetValue = if (isSelected) 1.12f else 1f,
                    animationSpec = springFloat(520f, 16f),
                    label = "pop"
                )
                Box(
                    Modifier
                        .offset(x = (PAD + i * SLOT).dp, y = 6.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable { tab.onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) Casper.OnInverse else Casper.Muted,
                        modifier = Modifier.size(22.dp).graphicsLayer { scaleX = pop; scaleY = pop }
                    )
                }
            }
        }
    }
}
