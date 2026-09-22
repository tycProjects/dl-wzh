package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.DictionaryViewModel
import com.example.ui.components.AddWordDialog
import com.example.ui.theme.CyberDarkBorder
import com.example.ui.theme.CyberDarkElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.StatusNeedsVerificationAmber
import com.example.ui.theme.StatusVerifiedGreen
import com.example.ui.theme.TextLightGray
import com.example.ui.theme.TextMutedGray
import com.example.ui.theme.TextWhite

@Composable
fun SettingsScreen(
    viewModel: DictionaryViewModel,
    modifier: Modifier = Modifier
) {
    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val totalEntries by viewModel.entryCount.collectAsStateWithLifecycle()
    val verifiedCount by viewModel.verifiedCount.collectAsStateWithLifecycle()
    val needsVerificationCount by viewModel.needsVerificationCount.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddWordDialog(
            onDismiss = { showAddDialog = false },
            onSave = { bisaya, subanen, bMean, sMean, pos, ex, notes, alt, status ->
                viewModel.addWord(bisaya, subanen, bMean, sMean, pos, ex, notes, alt, status)
                showAddDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                )
            )
        }

        // Appearance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) CyberDarkElevated else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "APPEARANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (isDark) "Dark Mode (Futuristic Cyan)" else "Light Mode",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isDark) "Cyber navy background with neon cyan accents" else "Clean bright neutral theme",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextLightGray
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.toggleDarkTheme(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberDarkElevated,
                                checkedTrackColor = CyanNeon,
                                uncheckedThumbColor = TextWhite,
                                uncheckedTrackColor = CyberDarkBorder
                            ),
                            modifier = Modifier.testTag("theme_toggle_switch")
                        )
                    }
                }
            }
        }

        // Dictionary Info & Database Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) CyberDarkElevated else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DICTIONARY DATABASE INFO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(label = "Total Entries:", value = "$totalEntries", isDark = isDark)
                    DetailRow(label = "Needs Verification:", value = "$needsVerificationCount", isDark = isDark, valueColor = StatusNeedsVerificationAmber)
                    DetailRow(label = "Verified Entries:", value = "$verifiedCount", isDark = isDark, valueColor = StatusVerifiedGreen)
                    DetailRow(label = "Storage Engine:", value = "Offline Room (SQLite)", isDark = isDark)
                    DetailRow(label = "Database File:", value = "bisaya_subanen_dictionary.db", isDark = isDark)
                    DetailRow(label = "Status:", value = "Stage 1 (Testing Placeholders)", isDark = isDark, valueColor = CyanNeon)

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(
                        color = if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanNeon,
                                contentColor = CyberDarkElevated
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Test Word", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetToSampleData() },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanNeon)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Reset Test Data",
                                color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // About the Dictionary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) CyberDarkElevated else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ABOUT THE LANGUAGES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Bisaya (Cebuano)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "One of the most widely spoken Austronesian languages in the Philippines, native to Cebu, Bohol, Siquijor, Leyte, Negros Oriental, and across major regions of Mindanao.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextLightGray,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Subanen (Subanon)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "An indigenous language family spoken by the Subanen people of the Zamboanga Peninsula in Western Mindanao. Preserving and documenting this vocabulary facilitates cultural heritage preservation and regional cross-communication.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextLightGray,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        // App Version & Compilation Guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) CyberDarkElevated else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "APPLICATION INFORMATION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailRow(label = "Application:", value = "Bisaya ↔ Subanen Dictionary", isDark = isDark)
                    DetailRow(label = "App Version:", value = "1.0.0 (Stage 1 Prototype)", isDark = isDark)
                    DetailRow(label = "Build Target:", value = "Android 15+ (Min SDK 24)", isDark = isDark)
                    DetailRow(label = "Network Status:", value = "Offline (No Internet Required)", isDark = isDark, valueColor = CyanNeon)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isDark: Boolean,
    valueColor: androidx.compose.ui.graphics.Color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextLightGray
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        )
    }
}
