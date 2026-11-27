package com.example.ui.screens.tournaments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TournamentDetailsScreen(
    tournament: Tournament,
    existingRegistration: TournamentRegistration?,
    leaderboardPreview: List<LeaderboardEntry>,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onJoinTournament: () -> Unit,
    onOpenFullLeaderboard: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("About", "Rules", "Leaderboard")
    val fmt = remember { NumberFormat.getNumberInstance(Locale.US) }
    val slotProgress = (tournament.registeredSlots.toFloat() / tournament.totalSlots.coerceAtLeast(1)).coerceIn(0f, 1f)

    Scaffold(
        containerColor = NexoraBlack,
        bottomBar = {
            Surface(
                color = NexoraCharcoal,
                border = BorderStroke(1.dp, DividerDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when {
                        existingRegistration != null -> {
                            NexoraOutlinedButton(
                                text = "Registered (${existingRegistration.registrationStatus.label})",
                                onClick = onJoinTournament,
                                borderColor = SuccessGreen,
                                contentColor = SuccessGreen,
                                icon = Icons.Default.CheckCircle,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "already_registered_button"
                            )
                        }
                        tournament.isJoinable -> {
                            NexoraPurpleButton(
                                text = "Join Tournament • Free Entry",
                                onClick = onJoinTournament,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "join_tournament_button"
                            )
                        }
                        else -> {
                            NexoraPurpleButton(
                                text = "Registration ${tournament.status.label}",
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Header Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                Image(
                    painter = painterResource(id = tournament.imageRes),
                    contentDescription = tournament.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NexoraBlack.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    NexoraBlack
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(NexoraBlack.copy(alpha = 0.65f))
                            .border(1.dp, VioletBorder, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SoftWhite)
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(NexoraBlack.copy(alpha = 0.65f))
                            .border(1.dp, VioletBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share tournament", tint = SoftWhite)
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = SoftWhite
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NexoraStatusBadge(
                            text = tournament.mode.label,
                            containerColor = NeonPurple.copy(alpha = 0.25f),
                            contentColor = ElectricViolet
                        )
                        NexoraStatusBadge(
                            text = tournament.status.label,
                            containerColor = if (tournament.status == TournamentStatus.LIVE) SuccessContainer else VioletSubtle,
                            contentColor = if (tournament.status == TournamentStatus.LIVE) SuccessGreen else ElectricViolet
                        )
                        NexoraStatusBadge(
                            text = "Free Entry",
                            containerColor = SuccessContainer,
                            contentColor = SuccessGreen
                        )
                    }
                }

                // Reward Pool & Per Kill summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NexoraGlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = GoldBorder,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text("REWARD POOL", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${fmt.format(tournament.rewardPoolPoints)} Pts",
                            style = MaterialTheme.typography.titleLarge,
                            color = BrightGold
                        )
                    }
                    NexoraGlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = VioletBorder,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text("PER KILL", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${tournament.perKillPoints} Pts",
                            style = MaterialTheme.typography.titleLarge,
                            color = ElectricViolet
                        )
                    }
                }

                // Schedule & Map Bar
                NexoraClayCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${tournament.dateLabel} • ${tournament.timeLabel}", style = MaterialTheme.typography.bodyMedium, color = SoftWhite)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Map, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Map: ${tournament.mapName}", style = MaterialTheme.typography.bodyMedium, color = SoftWhite)
                        }
                    }
                }

                // Rank Rewards Top 1, 2, 3
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rank Rewards (Non-Cash Points)", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RankRewardPill("1st", fmt.format(tournament.top1Points), BrightGold, Modifier.weight(1f))
                        RankRewardPill("2nd", fmt.format(tournament.top2Points), SilverRank, Modifier.weight(1f))
                        RankRewardPill("3rd", fmt.format(tournament.top3Points), BronzeRank, Modifier.weight(1f))
                    }
                }

                // Tabs: About | Rules | Leaderboard
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
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    tabs.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selectedTab == index) SoftWhite else CoolGrey
                                )
                            }
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        NexoraGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(tournament.description, style = MaterialTheme.typography.bodyLarge, color = SoftWhite)
                            Spacer(Modifier.height(12.dp))
                            Text("Requirements", style = MaterialTheme.typography.titleSmall, color = MetallicGold)
                            Spacer(Modifier.height(4.dp))
                            Text(tournament.requirements, style = MaterialTheme.typography.bodyMedium, color = CoolGrey)
                        }
                    }
                    1 -> {
                        NexoraGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                tournament.rules.forEachIndexed { idx, rule ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Text(
                                            text = "${idx + 1}.",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = ElectricViolet,
                                            modifier = Modifier.width(22.dp)
                                        )
                                        Text(rule, style = MaterialTheme.typography.bodyMedium, color = SoftWhite)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        NexoraGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                leaderboardPreview.take(3).forEach { entry ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "#${entry.rank}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = when (entry.rank) {
                                                    1 -> BrightGold
                                                    2 -> SilverRank
                                                    else -> BronzeRank
                                                }
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(entry.nexoraUsername, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                                                Text("IGN: ${entry.ffIgn}", style = MaterialTheme.typography.bodySmall, color = CoolGrey)
                                            }
                                        }
                                        Text("${fmt.format(entry.totalScore)} Pts", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                                    }
                                }
                                NexoraOutlinedButton(
                                    text = "Open Full Leaderboard",
                                    onClick = onOpenFullLeaderboard,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Slots progress card
                NexoraGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = ElectricViolet)
                            Spacer(Modifier.width(8.dp))
                            Text("Registered Players", style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                        }
                        Text(
                            "${tournament.registeredSlots} / ${tournament.totalSlots}",
                            style = MaterialTheme.typography.titleSmall,
                            color = BrightGold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { slotProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = NeonPurpleBright,
                        trackColor = NexoraSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${tournament.remainingSlots} slots remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tournament.remainingSlots > 0) SuccessGreen else ErrorRed
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RankRewardPill(
    rankLabel: String,
    pointsLabel: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    NexoraGlassCard(
        modifier = modifier,
        borderColor = accent.copy(alpha = 0.5f),
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(rankLabel, style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.Bold)
            Text("$pointsLabel Pts", style = MaterialTheme.typography.bodySmall, color = SoftWhite)
        }
    }
}
