package com.example.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.MaintenanceConfig
import com.example.domain.model.UserSettings
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    settings: UserSettings,
    maintenanceConfig: MaintenanceConfig,
    onBack: () -> Unit,
    onUpdateSettings: (UserSettings) -> Unit,
    onToggleMaintenancePreview: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        containerColor = NexoraBlack,
        topBar = { NexoraTopBar(title = "Settings", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NexoraGlassCard {
                Text("Notifications & Privacy", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                Spacer(Modifier.height(10.dp))
                ToggleRow("Tournament Reminders", settings.tournamentRemindersEnabled) {
                    onUpdateSettings(settings.copy(tournamentRemindersEnabled = it))
                }
                ToggleRow("Match Room Alerts", settings.roomAlertsEnabled) {
                    onUpdateSettings(settings.copy(roomAlertsEnabled = it))
                }
                ToggleRow("Store Updates", settings.storeUpdatesEnabled) {
                    onUpdateSettings(settings.copy(storeUpdatesEnabled = it))
                }
                ToggleRow("Private Profile Mode", settings.privateProfileMode) {
                    onUpdateSettings(settings.copy(privateProfileMode = it))
                }
                ToggleRow("Preview Maintenance Screen", maintenanceConfig.isMaintenanceActive) {
                    onToggleMaintenancePreview(it)
                }
            }

            NexoraGlassCard {
                SettingLinkRow("Edit Profile") { onNavigate("edit_profile") }
                HorizontalDivider(color = DividerDark)
                SettingLinkRow("Language (${settings.language})") {
                    val next = if (settings.language == "English") "English (Global)" else "English"
                    onUpdateSettings(settings.copy(language = next))
                }
                HorizontalDivider(color = DividerDark)
                SettingLinkRow("Terms and Conditions") { onNavigate("legal/terms") }
                HorizontalDivider(color = DividerDark)
                SettingLinkRow("Privacy Policy") { onNavigate("legal/privacy") }
                HorizontalDivider(color = DividerDark)
                SettingLinkRow("Help & Support") { onNavigate("support") }
                HorizontalDivider(color = DividerDark)
                SettingLinkRow("About NexoraHub (v1.0.0)") { onNavigate("legal/about") }
            }

            NexoraOutlinedButton(
                text = "Delete Account",
                icon = Icons.Default.DeleteForever,
                borderColor = ErrorRed.copy(alpha = 0.6f),
                contentColor = ErrorRed,
                onClick = { onNavigate("delete_account") },
                modifier = Modifier.fillMaxWidth(),
                testTag = "settings_delete_account_button"
            )

            NexoraPurpleButton(
                text = "Logout",
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = SoftWhite)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = SoftWhite, checkedTrackColor = NeonPurple)
        )
    }
}

@Composable
private fun SettingLinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = SoftWhite)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = CoolGrey)
    }
}

@Composable
fun DeleteAccountScreen(
    nexoraUserId: String,
    onBack: () -> Unit,
    onConfirmDeletion: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = { NexoraTopBar(title = "Delete Account", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NexoraGlassCard(borderColor = ErrorRed.copy(alpha = 0.6f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed)
                    Spacer(Modifier.width(10.dp))
                    Text("Permanent Account Deletion", style = MaterialTheme.typography.titleMedium, color = ErrorRed)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Submitting an account deletion request for $nexoraUserId permanently removes your NexoraHub profile, saved Free Fire IGN/UID records, referral links, and active marketplace listings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftWhite
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Audit logs required for tournament anti-cheat compliance or resolved support disputes are retained only as legally required. External web deletion portal: https://nexorahub.app/delete-account",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrey
                )
            }

            NexoraTextField(
                value = reason,
                onValueChange = { reason = it },
                label = "Reason for leaving (Optional)",
                singleLine = false,
                minLines = 3
            )

            Spacer(Modifier.weight(1f))

            NexoraOutlinedButton(
                text = "Request Permanent Account Deletion",
                icon = Icons.Default.DeleteForever,
                borderColor = ErrorRed,
                contentColor = ErrorRed,
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                testTag = "confirm_delete_account_button"
            )
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = NexoraCharcoal,
            title = { Text("Confirm Account Deletion", color = ErrorRed) },
            text = {
                Text(
                    "Are you sure you want to permanently delete your NexoraHub account? This action cannot be undone.",
                    color = SoftWhite
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmDeletion(reason)
                    }
                ) { Text("Delete Permanently", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = CoolGrey)
                }
            }
        )
    }
}

@Composable
fun LegalDocumentScreen(
    docType: String,
    onBack: () -> Unit
) {
    val (title, body) = when (docType) {
        "terms" -> "Terms and Conditions" to """
            1. Platform Purpose: NexoraHub is a Free Fire esports tournament discovery and community platform using free-entry tournaments and non-cash reward points.
            
            2. Identity Accuracy: Players must keep their NexoraHub User ID separate from their Free Fire IGN and Free Fire UID.
            
            3. Fair Play: Teaming, unauthorized third-party tools, or sharing match room passwords externally is strictly prohibited.
            
            4. Marketplace Rules: Selling Free Fire accounts or in-game currency is prohibited. Only permitted gaming gear, art, and coaching listings are allowed.
        """.trimIndent()
        "privacy" -> "Privacy Policy" to """
            1. Authentication: NexoraHub uses secure Google Sign-In via Android Credential Manager. We never store your Google or Free Fire passwords.
            
            2. Data Collected: We store your NexoraHub User ID, display name, profile avatar, Free Fire IGN/UID you submit for tournaments, and support messages.
            
            3. Room Credential Security: Match Room IDs and Passwords are visible only to verified participants and are never included in push notification text.
            
            4. Account Deletion: You may delete your account anytime from Profile -> Settings -> Delete Account.
        """.trimIndent()
        else -> "About NexoraHub" to """
            NexoraHub — PLAY • COMPETE • CONQUER
            Version 1.0.0
            
            NexoraHub is an esports tournament, leaderboard, and community marketplace application crafted for competitive Free Fire players.
            
            Currently running in interactive UI Preview Mode with clean repository contracts ready for NexoraHub Admin Backend integration.
        """.trimIndent()
    }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = { NexoraTopBar(title = title, onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            NexoraGlassCard {
                Text(body, style = MaterialTheme.typography.bodyLarge, color = SoftWhite)
            }
        }
    }
}

@Composable
fun MaintenanceScreen(
    config: MaintenanceConfig,
    onExitPreview: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        NexoraGlassCard(borderColor = GoldBorder) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(GoldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = BrightGold, modifier = Modifier.size(36.dp))
                }
                Text(config.title, style = MaterialTheme.typography.displaySmall, color = BrightGold, textAlign = TextAlign.Center)
                Text(config.message, style = MaterialTheme.typography.bodyLarge, color = SoftWhite, textAlign = TextAlign.Center)
                NexoraStatusBadge(config.estimatedReturn, VioletSubtle, ElectricViolet)
                Spacer(Modifier.height(6.dp))
                NexoraGoldButton(
                    text = "Exit Maintenance Preview",
                    onClick = onExitPreview,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
