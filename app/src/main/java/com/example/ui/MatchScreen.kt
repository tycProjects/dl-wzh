package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.LiveMatchSession
import com.example.model.MatchEvent
import com.example.model.MatchEventType
import com.example.model.MatchMentality
import com.example.model.MatchPressing
import com.example.model.Player
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    session: LiveMatchSession,
    divisionName: String = "League",
    domesticCupName: String = "Domestic Cup",
    continentalName: String = "Continental Cup",
    onTogglePause: () -> Unit,
    onSetSpeed: (Int) -> Unit,
    onSimulateRest: () -> Unit,
    onMakeSub: (outgoingId: String, incomingId: String) -> Unit,
    onSetTactics: (MatchMentality, MatchPressing) -> Unit,
    onFinishMatch: () -> Unit,
) {
    BackHandler {
        if (session.isFinished) {
            onFinishMatch()
        } else {
            onTogglePause()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedOutgoing by remember { mutableStateOf<Player?>(null) }
    var keyEventsOnly by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Broadcast Scoreboard Header
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val compLabel = when {
                        session.fixture.isPlayoff -> "Promotion Play-off • ${session.fixture.cupStageName ?: "Play-off"}"
                        session.fixture.isContinental -> "$continentalName • ${session.fixture.cupStageName ?: "Continental"}"
                        session.fixture.isCup -> "$domesticCupName • ${session.fixture.cupStageName ?: "Cup"}"
                        else -> "$divisionName • MD ${session.fixture.round}"
                    }
                    Text(
                        text = compLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            session.fixture.isPlayoff -> BroadcastAmber
                            session.fixture.isCup || session.fixture.isContinental -> ElectricCyan
                            else -> TurfEmerald
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    Surface(
                        color = if (session.isFinished) SlateElevated else TurfEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        val clockText = when {
                            session.isFinished -> "FULL TIME"
                            session.isPaused -> "PAUSED ${session.minute}'"
                            else -> "LIVE ${session.minute}'"
                        }
                        Text(
                            text = clockText,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (session.isFinished) TextPrimary else TurfEmerald,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                if (!session.fixture.aggregateNote.isNullOrBlank()) {
                    Surface(
                        color = ElectricCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = session.fixture.aggregateNote,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                // Scoreline Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        ClubBadge(
                            club = session.homeClub,
                            size = 56.dp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = session.homeClub.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (session.userIsHome) TurfEmerald else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${session.homeScore}  -  ${session.awayScore}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary,
                        )
                        if (session.homePenalties != null && session.awayPenalties != null) {
                            Text(
                                text = "PENS: ${session.homePenalties} - ${session.awayPenalties}",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastAmber,
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        ClubBadge(
                            club = session.awayClub,
                            size = 56.dp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = session.awayClub.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (!session.userIsHome) TurfEmerald else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Minute Progress Bar
                LinearProgressIndicator(
                    progress = { (session.minute / 90f).coerceIn(0f, 1f) },
                    color = TurfEmerald,
                    trackColor = SlateCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                )

                // Match Playback Controls & Live Tactics OR Post-Match Continue
                if (!session.isFinished) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onTogglePause,
                            border = BorderStroke(1.dp, HairlineBorder),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("match_pause_button"),
                        ) {
                            Icon(
                                imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (session.isPaused) "Resume" else "Pause", maxLines = 1, softWrap = false)
                        }

                        listOf(1, 2, 4).forEach { spd ->
                            FilterChip(
                                selected = session.speedMultiplier == spd,
                                onClick = { onSetSpeed(spd) },
                                label = {
                                    Text(
                                        "${spd}x",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurfEmerald,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = SlateCard,
                                ),
                                modifier = Modifier.testTag("speed_${spd}x"),
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = onSimulateRest,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SlateElevated,
                                contentColor = TextPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sim_rest_button"),
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sim End", maxLines = 1, softWrap = false)
                        }
                    }

                    // Live Mentality & Pressing Tactical Strip
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(MatchMentality.entries) { m ->
                            FilterChip(
                                selected = session.userMentality == m,
                                onClick = { onSetTactics(m, session.userPressing) },
                                label = {
                                    Text(
                                        m.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BroadcastAmber,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = SlateCard,
                                    labelColor = TextSecondary,
                                ),
                                modifier = Modifier.testTag("tactic_mentality_${m.name}"),
                            )
                        }
                        items(MatchPressing.entries) { p ->
                            FilterChip(
                                selected = session.userPressing == p,
                                onClick = { onSetTactics(session.userMentality, p) },
                                label = {
                                    Text(
                                        p.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = SlateCard,
                                    labelColor = TextSecondary,
                                ),
                                modifier = Modifier.testTag("tactic_pressing_${p.name}"),
                            )
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = BroadcastAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Man of the Match", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(
                                        text = session.manOfTheMatch ?: "-",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                    )
                                }
                            }
                            Button(
                                onClick = onFinishMatch,
                                colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = PitchBlack),
                                modifier = Modifier.testTag("finish_match_button"),
                            ) {
                                Text("Continue", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }
        }

        // Match Tabs
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = TurfEmerald,
            modifier = Modifier.widthIn(max = 640.dp),
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Events", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("match_tab_commentary"),
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Stats", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("match_tab_stats"),
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Ratings", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("match_tab_ratings"),
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Subs (${session.subsUsedByUser}/5)", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("match_tab_subs"),
            )
        }

        // Tab Body
        when (selectedTab) {
            0 -> {
                val filteredEvents = remember(session.events, keyEventsOnly) {
                    val base = session.events.asReversed()
                    if (!keyEventsOnly) base
                    else base.filter {
                        it.type in setOf(
                            MatchEventType.GOAL,
                            MatchEventType.YELLOW_CARD,
                            MatchEventType.RED_CARD,
                            MatchEventType.INJURY,
                            MatchEventType.PENALTY_SHOOTOUT,
                            MatchEventType.HALFTIME,
                            MatchEventType.FULLTIME,
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !keyEventsOnly,
                                onClick = { keyEventsOnly = false },
                                label = { Text("All Feed", maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurfEmerald,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = SlateCard,
                                ),
                            )
                            FilterChip(
                                selected = keyEventsOnly,
                                onClick = { keyEventsOnly = true },
                                label = { Text("Key Events", maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = SlateCard,
                                ),
                            )
                        }
                    }
                    items(filteredEvents) { event ->
                        MatchEventRow(event = event)
                    }
                }
            }

            1 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, HairlineBorder),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                StatCompareRow("Possession", "${session.homeStats.possession}%", "${session.awayStats.possession}%", session.homeStats.possession.toFloat(), session.awayStats.possession.toFloat())
                                StatCompareRow("Shots", "${session.homeStats.shots}", "${session.awayStats.shots}", session.homeStats.shots.toFloat(), session.awayStats.shots.toFloat())
                                StatCompareRow("On Target", "${session.homeStats.shotsOnTarget}", "${session.awayStats.shotsOnTarget}", session.homeStats.shotsOnTarget.toFloat(), session.awayStats.shotsOnTarget.toFloat())
                                StatCompareRow("Expected Goals (xG)", "${session.homeStats.expectedGoals}", "${session.awayStats.expectedGoals}", session.homeStats.expectedGoals.toFloat(), session.awayStats.expectedGoals.toFloat())
                                StatCompareRow("Corners", "${session.homeStats.corners}", "${session.awayStats.corners}", session.homeStats.corners.toFloat(), session.awayStats.corners.toFloat())
                                StatCompareRow("Fouls", "${session.homeStats.fouls}", "${session.awayStats.fouls}", session.homeStats.fouls.toFloat(), session.awayStats.fouls.toFloat())
                            }
                        }
                    }
                }
            }

            2 -> {
                val homeLineup = remember(session.homeClub, session.playerRatings) {
                    session.homeClub.startingXiPlayers()
                        .sortedByDescending { session.playerRatings[it.id] ?: 6.4 }
                }
                val awayLineup = remember(session.awayClub, session.playerRatings) {
                    session.awayClub.startingXiPlayers()
                        .sortedByDescending { session.playerRatings[it.id] ?: 6.4 }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = "${session.homeClub.name} — Live Match Ratings",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (session.userIsHome) TurfEmerald else TextPrimary,
                        )
                    }
                    items(homeLineup, key = { "rate_h_${it.id}" }) { p ->
                        PlayerRatingRow(player = p, session = session)
                    }
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${session.awayClub.name} — Live Match Ratings",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (!session.userIsHome) TurfEmerald else TextPrimary,
                        )
                    }
                    items(awayLineup, key = { "rate_a_${it.id}" }) { p ->
                        PlayerRatingRow(player = p, session = session)
                    }
                }
            }

            else -> {
                val userClub = session.userClub
                val staminaMap = if (session.userIsHome) session.homeStamina else session.awayStamina
                val starters = userClub.startingXiPlayers()
                val availableBench = userClub.benchPlayers().filter { it.id !in session.subbedOutIds }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Surface(
                            color = SlateCard,
                            border = BorderStroke(1.dp, if (selectedOutgoing != null) TurfEmerald else HairlineBorder),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = TurfEmerald)
                                Spacer(modifier = Modifier.width(8.dp))
                                val outP = selectedOutgoing
                                Text(
                                    text = when {
                                        session.isFinished -> "Match finished."
                                        session.subsUsedByUser >= 5 -> "All 5 substitutions used."
                                        outP != null -> "Subbing off ${outP.name} — tap a bench player below to bring on."
                                        else -> "Tap a starter below, then tap a bench player to make a sub (${session.subsUsedByUser}/5)."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (outP != null) TurfEmerald else TextSecondary,
                                )
                            }
                        }
                    }

                    item {
                        Text("On Pitch (${starters.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }

                    items(starters, key = { "live_xi_${it.id}" }) { player ->
                        val stam = staminaMap[player.id] ?: player.fitness
                        val isRed = player.id in session.redCardedIds
                        val isInjured = session.playerInjuries.containsKey(player.id)
                        val isSelected = selectedOutgoing?.id == player.id
                        val canSelect = !session.isFinished && session.subsUsedByUser < 5 && !isRed

                        Card(
                            onClick = {
                                if (canSelect) {
                                    selectedOutgoing = if (isSelected) null else player
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = if (isSelected) SlateElevated else SlateCard),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isSelected -> TurfEmerald
                                    isInjured -> BroadcastAmber
                                    else -> HairlineBorder
                                },
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = player.position.label,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    modifier = Modifier.width(36.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(player.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    val statusText = when {
                                        isRed -> "SENT OFF"
                                        isInjured -> "INJURED KNOCK • Sub recommended"
                                        else -> "OVR ${player.overall} • Stamina $stam%"
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            isRed -> CrimsonRed
                                            isInjured || stam < 65 -> BroadcastAmber
                                            else -> TextSecondary
                                        },
                                    )
                                }
                                Text(
                                    text = if (isSelected) "SELECTED" else "SUB OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) TurfEmerald else TextSecondary,
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Bench (${availableBench.size})", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }

                    items(availableBench, key = { "live_bench_${it.id}" }) { benchPlayer ->
                        val out = selectedOutgoing
                        val canBringOn = out != null && !session.isFinished && session.subsUsedByUser < 5
                        Card(
                            onClick = {
                                if (canBringOn && out != null) {
                                    onMakeSub(out.id, benchPlayer.id)
                                    selectedOutgoing = null
                                }
                            },
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, if (canBringOn) TurfEmerald.copy(alpha = 0.65f) else HairlineBorder),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = benchPlayer.position.label,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TurfEmerald,
                                    modifier = Modifier.width(36.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(benchPlayer.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(
                                        text = "OVR ${benchPlayer.overall} • Fresh ${benchPlayer.fitness}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                                if (canBringOn) {
                                    Surface(color = TurfEmerald, shape = RoundedCornerShape(8.dp)) {
                                        Text(
                                            text = "SUB ON",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PitchBlack,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
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
private fun PlayerRatingRow(
    player: Player,
    session: LiveMatchSession,
) {
    val rating = session.playerRatings[player.id] ?: 6.4
    val goals = session.playerGoals[player.id] ?: 0
    val assists = session.playerAssists[player.id] ?: 0
    val yellows = session.playerYellows[player.id] ?: 0
    val reds = session.playerReds[player.id] ?: 0
    val injured = session.playerInjuries.containsKey(player.id)

    val ratingColor = when {
        rating >= 8.0 -> TurfEmerald
        rating >= 7.0 -> ElectricCyan
        rating >= 6.0 -> BroadcastAmber
        else -> CrimsonRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, HairlineBorder),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = player.position.label,
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan,
                modifier = Modifier.width(34.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val summary = buildList {
                    if (goals > 0) add("${goals}G")
                    if (assists > 0) add("${assists}A")
                    if (yellows > 0) add("${yellows}YC")
                    if (reds > 0) add("${reds}RC")
                    if (injured) add("INJ")
                }.joinToString(" • ")
                if (summary.isNotEmpty()) {
                    Text(
                        text = summary,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reds > 0) CrimsonRed else if (injured) BroadcastAmber else TurfEmerald,
                    )
                }
            }
            if (injured) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = "Injured",
                    tint = BroadcastAmber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Surface(
                color = ratingColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f", rating),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    color = ratingColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MatchEventRow(event: MatchEvent) {
    val accent = when (event.type) {
        MatchEventType.GOAL, MatchEventType.PENALTY_SHOOTOUT -> TurfEmerald
        MatchEventType.YELLOW_CARD, MatchEventType.INJURY -> BroadcastAmber
        MatchEventType.RED_CARD -> CrimsonRed
        MatchEventType.SUBSTITUTION, MatchEventType.SAVE -> ElectricCyan
        else -> TextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (event.type) {
                MatchEventType.GOAL -> TurfEmerald.copy(alpha = 0.14f)
                MatchEventType.INJURY -> BroadcastAmber.copy(alpha = 0.12f)
                else -> SlateCard
            },
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when (event.type) {
                MatchEventType.GOAL -> TurfEmerald.copy(alpha = 0.6f)
                MatchEventType.INJURY -> BroadcastAmber.copy(alpha = 0.5f)
                else -> HairlineBorder
            },
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = ObsidianSurface,
                shape = RoundedCornerShape(6.dp),
            ) {
                Text(
                    text = "${event.minute}'",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (event.type == MatchEventType.GOAL) {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = null,
                    tint = TurfEmerald,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 2.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (event.type == MatchEventType.INJURY) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = "Injury",
                    tint = BroadcastAmber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = event.commentary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (event.type == MatchEventType.GOAL) FontWeight.SemiBold else FontWeight.Normal,
                color = TextPrimary,
            )
        }
    }
}

@Composable
private fun StatCompareRow(
    label: String,
    homeText: String,
    awayText: String,
    homeVal: Float,
    awayVal: Float,
) {
    val total = (homeVal + awayVal).coerceAtLeast(1f)
    val homeRatio = (homeVal / total).coerceIn(0.05f, 0.95f)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(homeText, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = TurfEmerald)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(awayText, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = ElectricCyan)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ElectricCyan.copy(alpha = 0.7f)),
        ) {
            Box(
                modifier = Modifier
                    .weight(homeRatio)
                    .height(7.dp)
                    .background(TurfEmerald),
            )
            Box(
                modifier = Modifier
                    .weight(1f - homeRatio)
                    .height(7.dp)
                    .background(Color.Transparent),
            )
        }
    }
}
