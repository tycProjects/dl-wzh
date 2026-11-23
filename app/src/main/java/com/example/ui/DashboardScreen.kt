package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.world.RealPlayerRosterBuilder
import com.example.model.CareerSaveState
import com.example.model.Club
import com.example.model.DivisionInfo
import com.example.model.InboxCategory
import com.example.model.InboxMessage
import com.example.model.LeagueStanding
import com.example.model.MatchFixture
import com.example.model.OfferStatus
import com.example.model.TrainingFocus
import com.example.ui.components.ClubBadge
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: CareerSaveState,
    userClub: Club,
    userDivision: DivisionInfo?,
    sortedStandings: List<LeagueStanding>,
    nextFixture: MatchFixture?,
    pendingNonUserStageLabel: String?,
    onPlayMatch: () -> Unit,
    onAdvanceNonUserStage: () -> Unit,
    onStartNextSeason: () -> Unit,
    onRunTraining: (TrainingFocus) -> Unit,
    onMarkInboxRead: (String) -> Unit,
    onRespondToOffer: (messageId: String, accept: Boolean) -> Unit,
    onNavigateToSquad: () -> Unit,
    onNavigateToTransfers: () -> Unit,
    onResetCareer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var inboxCategoryFilter by remember { mutableStateOf<InboxCategory?>(null) }

    val leagueRank = sortedStandings.indexOfFirst { it.clubId == userClub.id }.let {
        if (it == -1) "-" else "#${it + 1}"
    }
    val userStanding = sortedStandings.find { it.clubId == userClub.id }
    val xi = userClub.startingXiPlayers()
    val avgFitness = if (xi.isEmpty()) 90 else xi.map { it.fitness }.average().toInt()
    val avgMorale = if (xi.isEmpty()) 80 else xi.map { it.morale }.average().toInt()
    val unreadCount = state.inboxMessages.count { !it.isRead }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Contextual Club Hub Sub-Tabs: Overview | Inbox
        SecondaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = ObsidianSurface,
            contentColor = TurfEmerald,
            modifier = Modifier.widthIn(max = 640.dp),
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Overview", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("hub_tab_overview"),
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    val label = if (unreadCount > 0) "Inbox ($unreadCount)" else "Inbox"
                    Text(label, maxLines = 1, softWrap = false)
                },
                modifier = Modifier.testTag("hub_tab_inbox"),
            )
        }

        if (selectedSubTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Top Club Header
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, HairlineBorder),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ClubBadge(club = userClub, size = 60.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userClub.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${userDivision?.name ?: "League"} (Tier ${userClub.tier}) • S${state.seasonNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        StatPill(
                                            label = "BUDGET",
                                            value = RealPlayerRosterBuilder.formatBudget(userClub.budgetM),
                                            valueColor = TurfEmerald,
                                        )
                                    }
                                    item {
                                        StatPill(label = "POS", value = leagueRank, valueColor = ElectricCyan)
                                    }
                                    item {
                                        StatPill(
                                            label = "BOARD",
                                            value = "${state.boardConfidence}%",
                                            valueColor = if (state.boardConfidence >= 65) TurfEmerald else if (state.boardConfidence >= 45) BroadcastAmber else CrimsonRed,
                                        )
                                    }
                                    item {
                                        StatPill(
                                            label = "WAGE",
                                            value = "€${userClub.totalWeeklyWageK}k/w",
                                            valueColor = BroadcastAmber,
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.testTag("reset_career_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Start New Career",
                                    tint = TextSecondary,
                                )
                            }
                        }
                    }
                }

                // Next Match / Knockout Stage / Season Summary Hero Card
                item {
                    NextMatchHeroCard(
                        state = state,
                        userClub = userClub,
                        userDivision = userDivision,
                        nextFixture = nextFixture,
                        pendingNonUserStageLabel = pendingNonUserStageLabel,
                        onPlayMatch = onPlayMatch,
                        onAdvanceNonUserStage = onAdvanceNonUserStage,
                        onStartNextSeason = onStartNextSeason,
                    )
                }

                // Weekly Training & Morale Mechanics
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, HairlineBorder),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = TurfEmerald,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Training Pitch",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                    )
                                }
                                Surface(
                                    color = if (state.trainingUsedThisWeek) SlateElevated else TurfEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = if (state.trainingUsedThisWeek) "DONE THIS WEEK" else "1 SESSION READY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.trainingUsedThisWeek) TextSecondary else TurfEmerald,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }

                            if (state.lastTrainingNote != null) {
                                Text(
                                    text = state.lastTrainingNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TurfEmerald,
                                )
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(TrainingFocus.entries) { focus ->
                                    Card(
                                        onClick = {
                                            if (!state.trainingUsedThisWeek) onRunTraining(focus)
                                        },
                                        enabled = !state.trainingUsedThisWeek,
                                        colors = CardDefaults.cardColors(
                                            containerColor = SlateElevated,
                                            disabledContainerColor = ObsidianSurface,
                                        ),
                                        border = BorderStroke(1.dp, HairlineBorder),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .width(168.dp)
                                            .testTag("training_${focus.name}"),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = focus.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (state.trainingUsedThisWeek) TextSecondary else TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = focus.statNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TurfEmerald.copy(alpha = if (state.trainingUsedThisWeek) 0.5f else 0.9f),
                                                maxLines = 2,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Squad Health & Quick Links
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, HairlineBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Squad Readiness",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                )
                                ReadinessBar(label = "Fitness", value = avgFitness, color = TurfEmerald)
                                ReadinessBar(label = "Morale", value = avgMorale, color = ElectricCyan)
                                ReadinessBar(label = "Board Trust", value = state.boardConfidence, color = BroadcastAmber)
                                OutlinedButton(
                                    onClick = onNavigateToSquad,
                                    border = BorderStroke(1.dp, HairlineBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quick_squad_button"),
                                ) {
                                    Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tactics (${userClub.formation.code})", maxLines = 1, softWrap = false)
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, HairlineBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Season Form",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "${userStanding?.points ?: 0} PTS • ${userStanding?.won ?: 0}W ${userStanding?.drawn ?: 0}D ${userStanding?.lost ?: 0}L",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val formList = userStanding?.form.orEmpty()
                                    if (formList.isEmpty()) {
                                        Text("No matches played", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    } else {
                                        formList.forEach { res ->
                                            val badgeColor = when (res) {
                                                "W" -> TurfEmerald
                                                "D" -> BroadcastAmber
                                                else -> CrimsonRed
                                            }
                                            Surface(
                                                color = badgeColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(5.dp),
                                            ) {
                                                Text(
                                                    text = res,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = onNavigateToTransfers,
                                    border = BorderStroke(1.dp, HairlineBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quick_transfers_button"),
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scout Market", maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }

                // Recent Club Headlines
                if (state.recentNews.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, HairlineBorder),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Touchline Wire",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextSecondary,
                                )
                                state.recentNews.forEach { headline ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.SportsSoccer,
                                            contentDescription = null,
                                            tint = TurfEmerald,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = headline,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Manager Inbox Sub-Tab
            val filteredMsgs = remember(state.inboxMessages, inboxCategoryFilter) {
                if (inboxCategoryFilter == null) state.inboxMessages
                else state.inboxMessages.filter { it.category == inboxCategoryFilter }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mail, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Manager Inbox", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                            Text(
                                text = "Transfer bids, board reviews, medical bulletins & continental draws",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = inboxCategoryFilter == null,
                                onClick = { inboxCategoryFilter = null },
                                label = { Text("All (${state.inboxMessages.size})", maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurfEmerald,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = SlateCard,
                                ),
                            )
                        }
                        items(InboxCategory.entries) { cat ->
                            FilterChip(
                                selected = inboxCategoryFilter == cat,
                                onClick = { inboxCategoryFilter = cat },
                                label = { Text(cat.label, maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurfEmerald,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = SlateCard,
                                ),
                            )
                        }
                    }
                }

                if (filteredMsgs.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, HairlineBorder),
                        ) {
                            Text(
                                text = "No messages in this inbox folder yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(20.dp),
                            )
                        }
                    }
                } else {
                    items(filteredMsgs, key = { it.id }) { msg ->
                        InboxMessageCard(
                            message = msg,
                            onMarkRead = { onMarkInboxRead(msg.id) },
                            onAcceptOffer = { onRespondToOffer(msg.id, true) },
                            onRejectOffer = { onRespondToOffer(msg.id, false) },
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = SlateCard,
            title = { Text("Start New Career?", color = TextPrimary) },
            text = {
                Text(
                    "This will return to Club Selection and reset your current season progress.",
                    color = TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetCareer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = PitchBlack),
                ) {
                    Text("New Career")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun InboxMessageCard(
    message: InboxMessage,
    onMarkRead: () -> Unit,
    onAcceptOffer: () -> Unit,
    onRejectOffer: () -> Unit,
) {
    val catColor = when (message.category) {
        InboxCategory.TRANSFER_OFFER -> TurfEmerald
        InboxCategory.BOARD -> BroadcastAmber
        InboxCategory.MEDICAL -> CrimsonRed
        InboxCategory.DISCIPLINE -> BroadcastAmber
        InboxCategory.CONTINENTAL -> ElectricCyan
        InboxCategory.NEWS -> TextSecondary
    }
    Card(
        onClick = onMarkRead,
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead) SlateCard else SlateElevated,
        ),
        border = BorderStroke(1.dp, if (!message.isRead) catColor.copy(alpha = 0.6f) else HairlineBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("inbox_msg_${message.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!message.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(TurfEmerald, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Surface(
                        color = catColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = message.category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = catColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = message.matchdayLabel,
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }

            Text(text = message.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(text = message.body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

            val offer = message.offer
            if (offer != null) {
                if (offer.status == OfferStatus.PENDING) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onAcceptOffer,
                            colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = PitchBlack),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("accept_offer_${message.id}"),
                        ) {
                            Text("Accept ${RealPlayerRosterBuilder.formatBudget(offer.offerAmountM)}", maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = onRejectOffer,
                            border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("reject_offer_${message.id}"),
                        ) {
                            Text("Reject", color = CrimsonRed, maxLines = 1, softWrap = false)
                        }
                    }
                } else {
                    Text(
                        text = if (offer.status == OfferStatus.ACCEPTED) {
                            "ACCEPTED • Sold to ${offer.buyingClubName}"
                        } else {
                            "REJECTED • Stayed at club"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (offer.status == OfferStatus.ACCEPTED) TurfEmerald else CrimsonRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun NextMatchHeroCard(
    state: CareerSaveState,
    userClub: Club,
    userDivision: DivisionInfo?,
    nextFixture: MatchFixture?,
    pendingNonUserStageLabel: String?,
    onPlayMatch: () -> Unit,
    onAdvanceNonUserStage: () -> Unit,
    onStartNextSeason: () -> Unit,
) {
    val clubAccent = parseHexColor(userClub.primaryColorHex)

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.5.dp, clubAccent.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_stadium_hero),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.18f),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianSurface.copy(alpha = 0.5f),
                                SlateCard.copy(alpha = 0.92f),
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val summary = state.seasonSummary
                when {
                    summary != null -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BroadcastAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Season ${summary.seasonNumber} Summary",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                            )
                        }

                        Surface(
                            color = TurfEmerald.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                text = "#${summary.userFinishPosition} • ${summary.promotionStatusText}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TurfEmerald,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${summary.userDivisionName} Champions: ${summary.leagueChampionName}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            summary.domesticCupWinnerName?.let {
                                Text("${userClub.country.domesticCupName} Winners: $it", style = MaterialTheme.typography.bodySmall, color = ElectricCyan)
                            }
                            summary.uclWinnerName?.let {
                                Text("UEFA Champions League: $it", style = MaterialTheme.typography.bodySmall, color = BroadcastAmber)
                            }
                            summary.uelWinnerName?.let {
                                Text("UEFA Europa League: $it", style = MaterialTheme.typography.bodySmall, color = BroadcastAmber)
                            }
                            summary.ueclWinnerName?.let {
                                Text("UEFA Conference League: $it", style = MaterialTheme.typography.bodySmall, color = BroadcastAmber)
                            }
                            summary.aclWinnerName?.let {
                                Text("AFC Champions League Elite: $it", style = MaterialTheme.typography.bodySmall, color = BroadcastAmber)
                            }
                            if (summary.promotedClubs.isNotEmpty()) {
                                Text("Promoted: ${summary.promotedClubs.joinToString()}", style = MaterialTheme.typography.bodySmall, color = TurfEmerald)
                            }
                            if (summary.relegatedClubs.isNotEmpty()) {
                                Text("Relegated: ${summary.relegatedClubs.joinToString()}", style = MaterialTheme.typography.bodySmall, color = CrimsonRed)
                            }
                        }

                        Button(
                            onClick = onStartNextSeason,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurfEmerald,
                                contentColor = PitchBlack,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_next_season_button"),
                        ) {
                            Text("Begin Season ${state.seasonNumber + 1}", fontWeight = FontWeight.Bold)
                        }
                    }

                    nextFixture != null -> {
                        val home = state.clubs.find { it.id == nextFixture.homeClubId }
                        val away = state.clubs.find { it.id == nextFixture.awayClubId }
                        val badgeLabel = when {
                            nextFixture.isPlayoff -> "Promotion Play-off • ${nextFixture.cupStageName ?: "Play-off"}"
                            nextFixture.continentalId != null -> "${nextFixture.continentalId.shortCode} • ${nextFixture.cupStageName ?: "Continental"}"
                            nextFixture.isCup -> "${userClub.country.domesticCupName} • ${nextFixture.cupStageName ?: "Cup"}"
                            else -> "${userDivision?.name ?: "League"} • MD ${nextFixture.round}/${state.totalLeagueRounds}"
                        }
                        val badgeTint = when {
                            nextFixture.isPlayoff -> BroadcastAmber
                            nextFixture.continentalId != null || nextFixture.isCup -> ElectricCyan
                            else -> TurfEmerald
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = badgeTint.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = badgeLabel.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeTint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                            if (!nextFixture.aggregateNote.isNullOrBlank()) {
                                Surface(
                                    color = BroadcastAmber.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = nextFixture.aggregateNote,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BroadcastAmber,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }

                        if (home != null && away != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    ClubBadge(club = home, size = 58.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = home.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "HOME • OVR ${home.averageOverall}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                }

                                Text(
                                    text = "VS",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = BroadcastAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    ClubBadge(club = away, size = 58.dp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = away.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "AWAY • OVR ${away.averageOverall}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onPlayMatch,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurfEmerald,
                                contentColor = PitchBlack,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("play_matchday_button"),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kick Off Match",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    pendingNonUserStageLabel != null -> {
                        Text(
                            text = "COMPETITION SCHEDULE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                        )
                        Text(
                            text = "Cup, continental, or play-off rounds are scheduled while your squad rests.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Button(
                            onClick = onAdvanceNonUserStage,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = PitchBlack,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("simulate_cup_stage_button"),
                        ) {
                            Text(pendingNonUserStageLabel, fontWeight = FontWeight.Bold)
                        }
                    }

                    else -> {
                        Button(
                            onClick = onStartNextSeason,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TurfEmerald,
                                contentColor = PitchBlack,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_next_season_button"),
                        ) {
                            Text("Begin Season ${state.seasonNumber + 1}", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    valueColor: Color,
) {
    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, HairlineBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$label ",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun ReadinessBar(
    label: String,
    value: Int,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = "$value%",
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.bodySmall,
                color = color,
            )
        }
        LinearProgressIndicator(
            progress = { (value / 100f).coerceIn(0f, 1f) },
            color = color,
            trackColor = ObsidianSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
        )
    }
}
