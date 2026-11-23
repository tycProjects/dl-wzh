package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.world.RealPlayerRosterBuilder
import com.example.model.Club
import com.example.model.Formation
import com.example.model.Player
import com.example.model.Position
import com.example.model.YouthProspect
import com.example.ui.components.PitchFormationView
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquadScreen(
    club: Club,
    youthAcademy: List<YouthProspect>,
    onSelectFormation: (Formation) -> Unit,
    onSwapPlayers: (String, String) -> Unit,
    onScoutYouth: () -> Unit,
    onPromoteYouth: (String) -> Unit,
    onReleaseYouth: (String) -> Unit,
    onRenewContract: (playerId: String, wageK: Int, extraYears: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var selectedForSwap by remember { mutableStateOf<Player?>(null) }
    var renewTarget by remember { mutableStateOf<Player?>(null) }

    val handlePlayerTap: (Player) -> Unit = { tapped ->
        val current = selectedForSwap
        if (current == null) {
            selectedForSwap = tapped
        } else if (current.id == tapped.id) {
            selectedForSwap = null
        } else {
            onSwapPlayers(current.id, tapped.id)
            selectedForSwap = null
        }
    }

    val starters = club.startingXiPlayers()
    val bench = club.benchPlayers()
    val expiringCount = club.squad.count { it.contractYearsLeft <= 1 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SecondaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = ObsidianSurface,
            contentColor = TurfEmerald,
            modifier = Modifier.widthIn(max = 640.dp),
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("First Team", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("squad_tab_first_team"),
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Youth (${youthAcademy.size})", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("squad_tab_youth"),
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = {
                    val label = if (expiringCount > 0) "Contracts ($expiringCount!)" else "Contracts"
                    Text(label, maxLines = 1, softWrap = false)
                },
                modifier = Modifier.testTag("squad_tab_contracts"),
            )
        }

        when (selectedSubTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tactics & Squad",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary,
                                    )
                                    Text(
                                        text = club.formation.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                                Surface(
                                    color = TurfEmerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(
                                        text = "XI ${club.averageOverall} OVR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TurfEmerald,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    )
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(Formation.entries) { formation ->
                                    val selected = club.formation == formation
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            selectedForSwap = null
                                            onSelectFormation(formation)
                                        },
                                        label = {
                                            Text(
                                                text = formation.code,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TurfEmerald,
                                            selectedLabelColor = PitchBlack,
                                            containerColor = SlateCard,
                                            labelColor = TextPrimary,
                                        ),
                                        modifier = Modifier.testTag("formation_${formation.code}"),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            color = if (selectedForSwap != null) TurfEmerald.copy(alpha = 0.18f) else SlateCard,
                            border = BorderStroke(
                                1.dp,
                                if (selectedForSwap != null) TurfEmerald else HairlineBorder,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = if (selectedForSwap != null) TurfEmerald else TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val active = selectedForSwap
                                Text(
                                    text = if (active != null) {
                                        "Selected ${active.name} (${active.position.label}) — tap another starter or bench player to swap."
                                    } else {
                                        "Tap any player on the pitch or bench below to swap roles."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (active != null) TurfEmerald else TextSecondary,
                                )
                            }
                        }
                    }

                    item {
                        PitchFormationView(
                            club = club,
                            selectedPlayerId = selectedForSwap?.id,
                            onPlayerClick = handlePlayerTap,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                        )
                    }

                    item {
                        Text(
                            text = "Substitutes Bench (${bench.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }

                    items(bench, key = { "bench_${it.id}" }) { player ->
                        PlayerRosterRow(
                            player = player,
                            roleBadge = "SUB",
                            isSelected = selectedForSwap?.id == player.id,
                            onClick = { handlePlayerTap(player) },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Starting XI Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                    }

                    items(starters.indices.toList(), key = { idx -> "xi_${starters[idx].id}" }) { idx ->
                        val player = starters[idx]
                        val slot = club.formation.slots.getOrNull(idx)
                        PlayerRosterRow(
                            player = player,
                            roleBadge = slot?.roleLabel ?: player.position.label,
                            isSelected = selectedForSwap?.id == player.id,
                            onClick = { handlePlayerTap(player) },
                        )
                    }
                }
            }

            1 -> {
                // Youth Academy Tab
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(26.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Youth Academy", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Text(
                                            text = "15–18yo prospects • Promote to senior squad (${club.squad.size}/24)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = onScoutYouth,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = PitchBlack),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("scout_youth_button"),
                                ) {
                                    Text("Scout Intake", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }

                    if (youthAcademy.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                border = BorderStroke(1.dp, HairlineBorder),
                            ) {
                                Text(
                                    text = "Academy intake empty. Tap Scout Intake above to discover local U18 talent.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(20.dp),
                                )
                            }
                        }
                    } else {
                        items(youthAcademy, key = { it.id }) { prospect ->
                            YouthProspectCard(
                                prospect = prospect,
                                canPromote = club.squad.size < 24,
                                onPromote = { onPromoteYouth(prospect.id) },
                                onRelease = { onReleaseYouth(prospect.id) },
                            )
                        }
                    }
                }
            }

            else -> {
                // Contracts & Payroll Tab
                val sortedByContract = remember(club.squad) {
                    club.squad.sortedWith(compareBy<Player> { it.contractYearsLeft }.thenByDescending { it.overall })
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, HairlineBorder),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text("Weekly Squad Payroll", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Text(
                                        text = "€${club.totalWeeklyWageK}k / wk",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = BroadcastAmber,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Transfer Funds", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Text(
                                        text = RealPlayerRosterBuilder.formatBudget(club.budgetM),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TurfEmerald,
                                    )
                                }
                            }
                        }
                    }

                    items(sortedByContract, key = { "contract_${it.id}" }) { player ->
                        val expiring = player.contractYearsLeft <= 1
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            border = BorderStroke(1.dp, if (expiring) BroadcastAmber.copy(alpha = 0.7f) else HairlineBorder),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(player.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        if (expiring) {
                                            Surface(color = BroadcastAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(5.dp)) {
                                                Text(
                                                    text = "EXPIRING",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BroadcastAmber,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${player.position.label} • ${player.overall} OVR • €${player.wageK}k/wk • ${player.contractYearsLeft} yr(s) left",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { renewTarget = player },
                                    border = BorderStroke(1.dp, TurfEmerald.copy(alpha = 0.7f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("renew_${player.id}"),
                                ) {
                                    Text("Renew", color = TurfEmerald, maxLines = 1, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val activeTarget = renewTarget
    if (activeTarget != null) {
        ContractRenewalDialog(
            player = activeTarget,
            onDismiss = { renewTarget = null },
            onConfirm = { wageK, years ->
                onRenewContract(activeTarget.id, wageK, years)
                renewTarget = null
            },
        )
    }
}

@Composable
private fun ContractRenewalDialog(
    player: Player,
    onDismiss: () -> Unit,
    onConfirm: (wageK: Int, extraYears: Int) -> Unit,
) {
    var selectedYears by remember { mutableIntStateOf(2) }
    val suggestedWage = remember(player.wageK) { (player.wageK * 1.12).roundToInt().coerceAtLeast(player.wageK + 1) }
    var offeredWage by remember { mutableIntStateOf(suggestedWage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Text("Renew ${player.name}", color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current: €${player.wageK}k/wk • ${player.contractYearsLeft} yr(s) left • ${player.overall} OVR",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text("Extension Length", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { yrs ->
                        FilterChip(
                            selected = selectedYears == yrs,
                            onClick = { selectedYears = yrs },
                            label = { Text("+$yrs yr", maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TurfEmerald,
                                selectedLabelColor = PitchBlack,
                                containerColor = ObsidianSurface,
                            ),
                        )
                    }
                }
                Text("Weekly Wage Offer", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        player.wageK,
                        suggestedWage,
                        (player.wageK * 1.25).roundToInt().coerceAtLeast(suggestedWage + 1),
                    ).distinct().forEach { w ->
                        FilterChip(
                            selected = offeredWage == w,
                            onClick = { offeredWage = w },
                            label = { Text("€${w}k", fontFamily = JetBrainsMonoFontFamily, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = PitchBlack,
                                containerColor = ObsidianSurface,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(offeredWage, selectedYears) },
                colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = PitchBlack),
            ) {
                Text("Sign Deal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
    )
}

@Composable
private fun YouthProspectCard(
    prospect: YouthProspect,
    canPromote: Boolean,
    onPromote: () -> Unit,
    onRelease: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, HairlineBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("youth_${prospect.id}"),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(prospect.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = "${prospect.position.label} • Age ${prospect.age} • ${prospect.nationality} • €${prospect.wageK}k/wk",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = ObsidianSurface, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "OVR ${prospect.overall}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Surface(color = ElectricCyan.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                        Text(
                            text = "POT ${prospect.potentialMin}-${prospect.potentialMax}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Text(text = prospect.scoutNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onRelease) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Release prospect", tint = CrimsonRed)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onPromote,
                    enabled = canPromote,
                    colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = PitchBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("promote_${prospect.id}"),
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Promote to First Team", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun PlayerRosterRow(
    player: Player,
    roleBadge: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val posColor = when (player.position) {
        Position.GK -> BroadcastAmber
        Position.DEF -> ElectricCyan
        Position.MID -> TurfEmerald
        Position.FWD -> CrimsonRed
    }
    val fitColor = when {
        player.fitness >= 80 -> TurfEmerald
        player.fitness >= 65 -> BroadcastAmber
        else -> CrimsonRed
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SlateElevated else SlateCard,
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) TurfEmerald else HairlineBorder,
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("squad_player_${player.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = posColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = roleBadge,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                    color = posColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (player.injuredMatches > 0) {
                        Surface(color = CrimsonRed.copy(alpha = 0.22f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "INJ ${player.injuredMatches}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = CrimsonRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                    if (player.suspendedMatches > 0) {
                        Surface(color = BroadcastAmber.copy(alpha = 0.22f), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "SUSP",
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastAmber,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "${player.position.label} • Age ${player.age} • ${player.nationality} • G:${player.goals} A:${player.assists}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "${player.overall} OVR",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "FIT ${player.fitness}% • MOR ${player.morale}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = fitColor,
                )
            }
        }
    }
}
