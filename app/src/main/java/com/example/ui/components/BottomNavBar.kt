package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ScreenTab
import com.example.data.AppLanguage
import com.example.localization.ZStrings
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.LocalZMapsAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun ZMapsBottomNavBar(
    currentTab: ScreenTab,
    language: AppLanguage,
    onTabSelected: (ScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalZMapsAccent.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberSurface.copy(alpha = 0.96f))
            .border(width = 0.75.dp, color = CyberBorder)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("bottom_nav_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                label = ZStrings.navMap(language),
                selectedIcon = Icons.Filled.Map,
                unselectedIcon = Icons.Outlined.Map,
                isSelected = currentTab == ScreenTab.MAP,
                onClick = { onTabSelected(ScreenTab.MAP) },
                testTag = "nav_item_map"
            )

            NavBarItem(
                label = ZStrings.navSearch(language),
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search,
                isSelected = currentTab == ScreenTab.SEARCH,
                onClick = { onTabSelected(ScreenTab.SEARCH) },
                testTag = "nav_item_search"
            )

            NavBarItem(
                label = ZStrings.navSettings(language),
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                isSelected = currentTab == ScreenTab.SETTINGS,
                onClick = { onTabSelected(ScreenTab.SETTINGS) },
                testTag = "nav_item_settings"
            )
        }
    }
}

@Composable
private fun NavBarItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val accent = LocalZMapsAccent.current
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) accent.color else TextMuted,
        label = "nav_item_color"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = accent.color),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) accent.color.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.SansSerif
        )
    }
}
