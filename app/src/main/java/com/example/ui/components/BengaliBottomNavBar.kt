package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BottomNavTab
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet

@Composable
fun BengaliBottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                val iconSelected: ImageVector = when (tab) {
                    BottomNavTab.HOME -> Icons.Filled.Home
                    BottomNavTab.LIBRARY -> Icons.Filled.AutoStories
                    BottomNavTab.DISCOVER -> Icons.Filled.Explore
                    BottomNavTab.COMMUNITY -> Icons.Filled.Forum
                    BottomNavTab.PROFILE -> Icons.Filled.Person
                }
                val iconUnselected: ImageVector = when (tab) {
                    BottomNavTab.HOME -> Icons.Outlined.Home
                    BottomNavTab.LIBRARY -> Icons.Outlined.AutoStories
                    BottomNavTab.DISCOVER -> Icons.Outlined.Explore
                    BottomNavTab.COMMUNITY -> Icons.Outlined.Forum
                    BottomNavTab.PROFILE -> Icons.Outlined.Person
                }

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    label = "tab_color"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .testTag("nav_tab_${tab.name.lowercase()}")
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) PrimaryPurple.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) iconSelected else iconUnselected,
                            contentDescription = tab.title,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor
                    )
                }
            }
        }
    }
}
