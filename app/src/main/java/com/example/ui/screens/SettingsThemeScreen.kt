package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DarkModePreference
import com.example.model.ThemePalette
import com.example.viewmodel.CalculatorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsThemeScreen(
    state: CalculatorUiState,
    onSetTheme: (ThemePalette) -> Unit,
    onSetDarkMode: (DarkModePreference) -> Unit,
    onSetHaptic: (Boolean) -> Unit,
    onSetPrecision: (Int) -> Unit,
    onOpenRoadmap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = when (state.darkModePref) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.DARK -> true
        DarkModePreference.LIGHT -> false
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature Roadmap / Extension Banner (honoring user's request!)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = state.themePalette.primary.copy(alpha = 0.12f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenRoadmap)
                .testTag("card_roadmap_banner")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = state.themePalette.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Next Feature Expansion",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Vote for upcoming tools: EMI, Live Rates, Base-N, etc.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onOpenRoadmap,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Vote", fontSize = 12.sp)
                }
            }
        }

        // Color Theme Palettes Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) state.themePalette.darkSurface else state.themePalette.lightSurface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Custom Color Themes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ThemePalette.values().forEach { palette ->
                    val isSelected = state.themePalette == palette
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSetTheme(palette) }
                            .testTag("theme_${palette.name}"),
                        color = if (isSelected) {
                            state.themePalette.primary.copy(alpha = 0.15f)
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Color preview circles
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(palette.primary)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(palette.accent)
                                    )
                                }

                                Text(
                                    text = palette.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = state.themePalette.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dark Mode Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) state.themePalette.darkSurface else state.themePalette.lightSurface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Dark Mode Support",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DarkModePreference.values().forEachIndexed { index, pref ->
                        SegmentedButton(
                            selected = state.darkModePref == pref,
                            onClick = { onSetDarkMode(pref) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = DarkModePreference.values().size),
                            modifier = Modifier.testTag("dark_pref_$index")
                        ) {
                            Text(
                                text = when (pref) {
                                    DarkModePreference.SYSTEM -> "System"
                                    DarkModePreference.DARK -> "Dark"
                                    DarkModePreference.LIGHT -> "Light"
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Haptic Feedback & Precision Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) state.themePalette.darkSurface else state.themePalette.lightSurface
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Tactile feedback toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = state.themePalette.primary
                        )
                        Column {
                            Text(
                                text = "Tactile Key Haptics",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Vibration click on every keypress",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = state.hapticEnabled,
                        onCheckedChange = onSetHaptic,
                        modifier = Modifier.testTag("switch_haptics")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Decimal Precision selector
                Text(
                    text = "Decimal Precision (${state.decimalPrecision} places)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 4, 6, 8).forEach { prec ->
                        FilterChip(
                            selected = state.decimalPrecision == prec,
                            onClick = { onSetPrecision(prec) },
                            label = { Text("$prec digits") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = state.themePalette.primary,
                                selectedLabelColor = if (isDark) Color.Black else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
