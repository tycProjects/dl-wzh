package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CosmicDeep
import com.example.ui.theme.CosmicGlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MayaTab

@Composable
fun MayaBottomBar(
    activeTab: MayaTab,
    isListening: Boolean,
    onSelectTab: (MayaTab) -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "voice_fab_glow")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 650 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Glassmorphic Bar Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(26.dp))
                .background(CosmicDeep.copy(alpha = 0.92f))
                .border(1.dp, CosmicGlassBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTabItem(
                label = "Home",
                selected = activeTab == MayaTab.HOME,
                selectedIcon = Icons.Rounded.Home,
                unselectedIcon = Icons.Outlined.Home,
                testTag = "nav_home",
                onClick = { onSelectTab(MayaTab.HOME) }
            )
            BottomNavTabItem(
                label = "Device",
                selected = activeTab == MayaTab.DEVICE,
                selectedIcon = Icons.Rounded.Devices,
                unselectedIcon = Icons.Outlined.Devices,
                testTag = "nav_device",
                onClick = { onSelectTab(MayaTab.DEVICE) }
            )

            // Center spacer for floating Voice mic button
            Spacer(modifier = Modifier.size(64.dp))

            BottomNavTabItem(
                label = "Chat",
                selected = activeTab == MayaTab.CHAT,
                selectedIcon = Icons.Rounded.ChatBubble,
                unselectedIcon = Icons.Outlined.ChatBubbleOutline,
                testTag = "nav_chat",
                onClick = { onSelectTab(MayaTab.CHAT) }
            )
            BottomNavTabItem(
                label = "Tools",
                selected = activeTab == MayaTab.TOOLS,
                selectedIcon = Icons.Rounded.Tune,
                unselectedIcon = Icons.Outlined.Tune,
                testTag = "nav_tools",
                onClick = { onSelectTab(MayaTab.TOOLS) }
            )
        }

        // Centered Large Pink-Purple Gradient Voice Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(72.dp)
        ) {
            // Outer Glow Ring
            Box(
                modifier = Modifier
                    .size(if (isListening) 72.dp else 66.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                NeonPink.copy(alpha = glowAlpha * 0.55f),
                                NeonPurple.copy(alpha = glowAlpha * 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .shadow(14.dp, CircleShape, ambientColor = NeonPink, spotColor = NeonPurple)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeonPink, NeonPurple)
                        )
                    )
                    .border(
                        width = if (isListening) 2.5.dp else 1.5.dp,
                        color = if (isListening) NeonCyan else Color.White.copy(alpha = 0.35f),
                        shape = CircleShape
                    )
                    .minimumInteractiveComponentSize()
                    .testTag("nav_voice_button")
                    .clickable(onClick = onVoiceClick)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "ভয়েস কথা বলো",
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomNavTabItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val tint = if (selected) NeonCyan else TextSecondary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (selected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = tint
        )
    }
}
