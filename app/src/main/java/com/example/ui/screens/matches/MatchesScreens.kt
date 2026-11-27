package com.example.ui.screens.matches

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.*

@Composable
fun MatchesScreen(
    matches: List<MatchItem>,
    initialTab: Int = 0,
    onOpenMatchDetails: (String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Ongoing", "Completed")
    val filtered = remember(matches, selectedTab) {
        if (selectedTab == 0) matches.filter { it.isOngoing }
        else matches.filter { !it.isOngoing }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
            .statusBarsPadding()
    ) {
        Text(
            text = "Matches & Room Details",
            style = MaterialTheme.typography.headlineMedium,
            color = SoftWhite,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )

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
                    modifier = Modifier.testTag("matches_tab_${label.lowercase()}"),
                    text = {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selectedTab == idx) SoftWhite else CoolGrey)
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    NexoraEmptyState(
                        icon = Icons.Default.SportsEsports,
                        title = "No ${tabs[selectedTab]} Matches",
                        description = "Scheduled matches and verified Free Fire results appear here once published by admin."
                    )
                }
            } else {
                items(filtered, key = { it.matchId }) { match ->
                    MatchCard(match = match, onViewDetails = { onOpenMatchDetails(match.matchId) })
                }
            }
        }
    }
}

@Composable
private fun MatchCard(
    match: MatchItem,
    onViewDetails: () -> Unit
) {
    NexoraGlassCard(
        borderColor = if (match.isOngoing) VioletBorder else GoldBorder,
        onClick = onViewDetails
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(match.tournamentTitle, style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                Text("Match ID: ${match.matchId}", style = MaterialTheme.typography.labelMedium, color = ElectricViolet)
            }
            if (match.isOngoing) {
                NexoraStatusBadge(
                    text = match.roomDetails.publicationStatus.label.uppercase(),
                    containerColor = if (match.roomDetails.publicationStatus == RoomPublicationStatus.AVAILABLE) SuccessContainer else WarningContainer,
                    contentColor = if (match.roomDetails.publicationStatus == RoomPublicationStatus.AVAILABLE) SuccessGreen else WarningAmber
                )
            } else {
                NexoraStatusBadge(
                    text = if (match.isResultVerified) "VERIFIED" else "PROVISIONAL",
                    containerColor = if (match.isResultVerified) SuccessContainer else WarningContainer,
                    contentColor = if (match.isResultVerified) SuccessGreen else WarningAmber
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${match.mode.label} • ${match.mapName}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
            Text("${match.dateLabel} • ${match.startTime}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
        }

        if (!match.isOngoing && !match.winnerName.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BrightGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Winner: ${match.winnerName}", style = MaterialTheme.typography.titleSmall, color = BrightGold)
            }
        }

        Spacer(Modifier.height(12.dp))
        NexoraPurpleButton(
            text = if (match.isOngoing) "View Room & Details" else "View Match Results",
            onClick = onViewDetails,
            testTag = "view_match_${match.matchId}"
        )
    }
}

@Composable
fun MatchDetailsScreen(
    match: MatchItem,
    onBack: () -> Unit,
    onCopiedMessage: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoraBlack)
    ) {
        NexoraTopBar(title = "Match ${match.matchId}", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MATCH INFORMATION CARD
            NexoraClayCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MATCH INFORMATION", style = MaterialTheme.typography.labelLarge, color = MetallicGold)
                    HorizontalDivider(color = DividerDark)
                    InfoPair("Tournament Name", match.tournamentTitle)
                    InfoPair("Match ID", match.matchId)
                    InfoPair("Mode & Map", "${match.mode.label} • ${match.mapName}")
                    InfoPair("Match Date", match.dateLabel)
                    InfoPair("Starting Time", match.startTime)
                    InfoPair("Participants", "${match.participantCount} Registered")
                }
            }

            // MANDATORY FREE FIRE ROOM DETAILS CARD
            val room = match.roomDetails
            NexoraGlassCard(
                borderColor = ElectricViolet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = MetallicGold)
                        Spacer(Modifier.width(8.dp))
                        Text("FREE FIRE ROOM DETAILS", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                    }
                    NexoraStatusBadge(
                        text = room.publicationStatus.label.uppercase(),
                        containerColor = GoldContainer,
                        contentColor = BrightGold
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Release Schedule: ${room.releaseDateTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CoolGrey
                )
                Spacer(Modifier.height(12.dp))

                if (!room.isUserAuthorized) {
                    Surface(
                        color = ErrorContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ErrorRed)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Restricted Access: Only verified, registered participants of this match can view the Free Fire Room ID and Password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SoftWhite
                            )
                        }
                    }
                } else if (room.publicationStatus == RoomPublicationStatus.DISABLED ||
                    room.publicationStatus == RoomPublicationStatus.NOT_PUBLISHED ||
                    room.publicationStatus == RoomPublicationStatus.SCHEDULED
                ) {
                    Surface(
                        color = WarningContainer,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "Room credentials are ${room.publicationStatus.label.lowercase()}. Check back at ${room.releaseDateTime}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrightGold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    // Separate Room ID & Room Password fields with Copy buttons
                    CredentialCopyRow(
                        label = "Room ID",
                        value = room.roomId,
                        onCopy = {
                            clipboard.setText(AnnotatedString(room.roomId))
                            onCopiedMessage("Room ID copied to clipboard")
                        },
                        testTag = "copy_room_id_button"
                    )
                    Spacer(Modifier.height(10.dp))
                    CredentialCopyRow(
                        label = "Room Password",
                        value = if (passwordVisible) room.roomPassword else "••••••••",
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = ElectricViolet
                                )
                            }
                        },
                        onCopy = {
                            clipboard.setText(AnnotatedString(room.roomPassword))
                            onCopiedMessage("Room Password copied to clipboard")
                        },
                        testTag = "copy_room_password_button"
                    )
                    if (!room.roomScreenshotNote.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(room.roomScreenshotNote, style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                    }
                }
            }

            // RESULTS & STANDINGS
            if (match.results.isNotEmpty()) {
                NexoraGlassCard(borderColor = GoldBorder, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (match.isOngoing) "Provisional Live Standings" else "Final Match Results",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftWhite
                        )
                        NexoraStatusBadge(
                            text = if (match.isResultVerified) "Verified" else "Provisional",
                            containerColor = if (match.isResultVerified) SuccessContainer else WarningContainer,
                            contentColor = if (match.isResultVerified) SuccessGreen else WarningAmber,
                            icon = if (match.isResultVerified) Icons.Default.Verified else null
                        )
                    }
                    if (!match.proofSummary.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(match.proofSummary, style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                    }
                    Spacer(Modifier.height(12.dp))

                    match.results.forEach { row ->
                        Surface(
                            color = NexoraSurface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (row.rank) {
                                                1 -> GoldContainer
                                                2 -> SilverRank.copy(alpha = 0.2f)
                                                else -> VioletSubtle
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "#${row.rank}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (row.rank == 1) BrightGold else SoftWhite
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(row.teamName ?: row.nexoraUsername, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                                    Text("IGN: ${row.ffIgn} • UID: ${row.ffUid}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${row.totalPoints} Pts", style = MaterialTheme.typography.titleSmall, color = BrightGold, fontWeight = FontWeight.Bold)
                                    Text("${row.kills} Kills • ${row.placementPoints} Pos", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPair(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CoolGrey)
        Text(value, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
    }
}

@Composable
private fun CredentialCopyRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
    testTag: String,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = NexoraSurface,
        border = BorderStroke(1.dp, VioletBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = BrightGold
                )
            }
            trailingIcon?.invoke()
            IconButton(
                onClick = onCopy,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(NeonPurple.copy(alpha = 0.25f))
                    .border(1.dp, ElectricViolet, CircleShape)
                    .testTag(testTag)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy $label", tint = SoftWhite, modifier = Modifier.size(18.dp))
            }
        }
    }
}
