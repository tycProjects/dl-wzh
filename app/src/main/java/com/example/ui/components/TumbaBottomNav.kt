package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ViewSection
import com.example.ui.theme.LocalTumbaColors
import com.example.ui.theme.LocalTumbaStrings

data class NavItem(
    val section: ViewSection,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun TumbaBottomNav(
    currentSection: ViewSection,
    onSelectSection: (ViewSection) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTumbaColors.current
    val strings = LocalTumbaStrings.current

    val navItems = listOf(
        NavItem(ViewSection.ALL, strings.notes, Icons.Filled.Description, Icons.Outlined.Description, "bottom_nav_notes"),
        NavItem(ViewSection.FOLDERS, strings.folders, Icons.Filled.Folder, Icons.Outlined.Folder, "bottom_nav_folders"),
        NavItem(ViewSection.ARCHIVE, strings.archive, Icons.Filled.Archive, Icons.Outlined.Archive, "bottom_nav_archive")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bgSidebar)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = colors.borderColor, thickness = 0.8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentSection == item.section
                val color = if (isSelected) colors.textPrimary else colors.textMuted
                Column(
                    modifier = Modifier
                        .testTag(item.testTag)
                        .clickable { onSelectSection(item.section) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = item.label,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Settings item
            Column(
                modifier = Modifier
                    .testTag("bottom_nav_settings")
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = strings.settings,
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = strings.settings,
                    color = colors.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
