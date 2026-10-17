package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.liquidGlass
import com.example.ui.screens.BioscienceAiScreen
import com.example.ui.screens.DnaOrganellesScreen
import com.example.ui.screens.LiquidSandboxScreen
import com.example.ui.screens.OsmosisSimulationScreen
import com.example.ui.screens.YeltiCreatorScreen
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidCyanLight
import com.example.ui.theme.LiquidDarkBackground
import com.example.ui.theme.LiquidMagenta
import com.example.ui.theme.LiquidTeal
import com.example.ui.theme.LiquidViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

enum class LiquidNavTab(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val tag: String
) {
    SANDBOX("Fluid Lab", Icons.Default.WaterDrop, LiquidCyan, "tab_sandbox"),
    OSMOSIS("Osmosis", Icons.Default.Science, LiquidTeal, "tab_osmosis"),
    GENETICS("Genetics", Icons.Default.Hub, LiquidViolet, "tab_genetics"),
    BIO_AI("Bio-AI", Icons.Default.AutoAwesome, LiquidMagenta, "tab_bio_ai"),
    CREATOR("Yelti Hub", Icons.Default.Person, LiquidCyanLight, "tab_creator")
}

@Composable
fun LiquidGlassMainScreen() {
    var currentTab by remember { mutableStateOf(LiquidNavTab.SANDBOX) }

    // Intercept back button to return to sandbox if on a sub-tab
    BackHandler(enabled = currentTab != LiquidNavTab.SANDBOX) {
        currentTab = LiquidNavTab.SANDBOX
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(LiquidDarkBackground),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Main Content Area with Smooth Crossfade
            Crossfade(
                targetState = currentTab,
                label = "ScreenTransition",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    LiquidNavTab.SANDBOX -> LiquidSandboxScreen()
                    LiquidNavTab.OSMOSIS -> OsmosisSimulationScreen()
                    LiquidNavTab.GENETICS -> DnaOrganellesScreen()
                    LiquidNavTab.BIO_AI -> BioscienceAiScreen()
                    LiquidNavTab.CREATOR -> YeltiCreatorScreen()
                }
            }

            // Floating Frosted Liquid Glass Bottom Navigation Bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .liquidGlass(
                            shape = RoundedCornerShape(34.dp),
                            backgroundColor = Color(0x350F172A),
                            borderColors = listOf(
                                Color.White.copy(alpha = 0.55f),
                                LiquidCyan.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.20f),
                                LiquidMagenta.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.45f)
                            ),
                            borderWidth = 1.2.dp,
                            hasSpecularGlare = true
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidNavTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            val interactionSource = remember { MutableInteractionSource() }

                            val iconScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.15f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "TabScale_${tab.name}"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null,
                                        onClick = { currentTab = tab }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag(tab.tag)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .scale(iconScale)
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .clip(CircleShape)
                                                    .background(
                                                        brush = Brush.radialGradient(
                                                            colors = listOf(
                                                                tab.accentColor.copy(alpha = 0.40f),
                                                                tab.accentColor.copy(alpha = 0.10f),
                                                                Color.Transparent
                                                            )
                                                        )
                                                    )
                                                    .border(
                                                        width = 1.dp,
                                                        color = tab.accentColor.copy(alpha = 0.6f),
                                                        shape = CircleShape
                                                    )
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) tab.accentColor else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) tab.accentColor else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
