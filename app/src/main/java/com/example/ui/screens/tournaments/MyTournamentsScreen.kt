package com.example.ui.screens.tournaments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.*

@Composable
fun MyTournamentsScreen(
    registrations: List<TournamentRegistration>,
    onBack: () -> Unit,
    onExploreTournaments: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Upcoming", "Ongoing", "Completed")
    var selectedDetail by remember { mutableStateOf<TournamentRegistration?>(null) }

    val filtered = remember(registrations, selectedTab) {
        when (selectedTab) {
            0 -> registrations.filter { it.tournamentStatus == TournamentStatus.UPCOMING }
            1 -> registrations.filter { it.tournamentStatus == TournamentStatus.LIVE }
            else -> registrations.filter { it.tournamentStatus == TournamentStatus.COMPLETED }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        NexoraTopBar(title = "My Tournaments", onBack = onBack)

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NexoraSurface,
            contentColor = SoftWhite,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ElectricViolet
                )
            },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(label, color = if (selectedTab == idx) SoftWhite else CoolGrey)
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    NexoraEmptyState(
                        icon = Icons.Default.EmojiEvents,
                        title = "No ${tabs[selectedTab]} Registrations",
                        description = "Join an active Free Fire tournament to see your registration status and team details here.",
                        actionLabel = "Browse Tournaments",
                        onAction = onExploreTournaments
                    )
                }
            } else {
                items(filtered, key = { it.registrationId }) { reg ->
                    val statusColors = when (reg.registrationStatus) {
                        RegistrationStatus.VERIFIED, RegistrationStatus.REGISTERED -> SuccessContainer to SuccessGreen
                        RegistrationStatus.PENDING_REVIEW -> WarningContainer to WarningAmber
                        RegistrationStatus.REJECTED, RegistrationStatus.CANCELLED -> ErrorContainer to ErrorRed
                        else -> VioletSubtle to ElectricViolet
                    }

                    NexoraGlassCard(
                        onClick = { selectedDetail = reg },
                        borderColor = VioletBorder
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(reg.tournamentTitle, style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                            NexoraStatusBadge(
                                text = reg.registrationStatus.label,
                                containerColor = statusColors.first,
                                contentColor = statusColors.second
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Ref ID: ${reg.registrationId}", style = MaterialTheme.typography.labelMedium, color = BrightGold)
                        if (!reg.teamName.isNullOrBlank()) {
                            Text("Team: ${reg.teamName}", style = MaterialTheme.typography.bodyMedium, color = ElectricViolet)
                        }
                        val lead = reg.players.firstOrNull()
                        if (lead != null) {
                            Text("IGN: ${lead.ign}  •  UID: ${lead.uid}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${reg.tournamentDate} • ${reg.tournamentTime} • ${reg.mode.label}", style = MaterialTheme.typography.bodySmall, color = MutedGrey)
                    }
                }
            }
        }
    }

    selectedDetail?.let { reg ->
        AlertDialog(
            onDismissRequest = { selectedDetail = null },
            containerColor = NexoraCharcoal,
            title = { Text(reg.tournamentTitle, color = SoftWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Registration ID: ${reg.registrationId}", color = BrightGold, style = MaterialTheme.typography.labelLarge)
                    Text("Status: ${reg.registrationStatus.label}", color = ElectricViolet)
                    Text("NexoraHub ID: ${reg.nexoraUserId}", color = CoolGrey)
                    if (!reg.teamName.isNullOrBlank()) {
                        Text("Team Name: ${reg.teamName}", color = SoftWhite)
                    }
                    HorizontalDivider(color = DividerDark, modifier = Modifier.padding(vertical = 4.dp))
                    reg.players.forEachIndexed { idx, p ->
                        Text("Player ${idx + 1}: ${p.ign} (UID: ${p.uid})", color = SoftWhite, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!reg.rejectionReason.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Rejection Reason: ${reg.rejectionReason}", color = ErrorRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDetail = null }) {
                    Text("Close", color = ElectricViolet)
                }
            }
        )
    }
}
