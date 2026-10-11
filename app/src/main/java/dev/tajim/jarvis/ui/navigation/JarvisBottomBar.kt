package dev.tajim.jarvis.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.theme.LocalJarvisColors

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val LeftTabs = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Outlined.Home),
    Tab(Routes.CHAT, R.string.nav_chat, Icons.Outlined.ChatBubbleOutline),
)
private val RightTabs = listOf(
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
    Tab(Routes.UPDATES, R.string.nav_updates, Icons.Outlined.Notifications),
)

/** Home / Chat / mic / Settings / Updates. The mic button starts or stops real speech recognition. */
@Composable
fun JarvisBottomBar(
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    micActive: Boolean,
    onMicClick: () -> Unit,
) {
    val c = LocalJarvisColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.backgroundBottom)
            .drawBehind { drawLine(c.borderStart.copy(alpha = 0.5f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier.fillMaxWidth().height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LeftTabs.forEach { TabItem(it, it.route == selectedRoute, onNavigate, Modifier.weight(1f)) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(c.blue.copy(alpha = 0.55f), c.backgroundBottom)))
                        .border(2.dp, Brush.linearGradient(listOf(c.cyan, c.violet, c.magenta)), CircleShape)
                        .clickable(role = Role.Button, onClick = onMicClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (micActive) Icons.Outlined.Stop else Icons.Outlined.Mic,
                        contentDescription = stringResource(if (micActive) R.string.nav_voice_stop else R.string.nav_voice_start),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            RightTabs.forEach { TabItem(it, it.route == selectedRoute, onNavigate, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, onNavigate: (String) -> Unit, modifier: Modifier) {
    val c = LocalJarvisColors.current
    val tint = if (selected) c.cyan else c.textSecondary
    Column(
        modifier
            .height(72.dp)
            .background(if (selected) c.cyan.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Tab) { onNavigate(tab.route) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(stringResource(tab.label), style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
