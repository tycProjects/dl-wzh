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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.world.RealPlayerRosterBuilder
import com.example.model.Club
import com.example.model.Player
import com.example.model.Position
import com.example.ui.theme.BroadcastAmber
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurfEmerald
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferMarketScreen(
    userClub: Club,
    marketPlayers: List<Player>,
    onBuyPlayer: (String) -> Unit,
    onSellPlayer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var positionFilter by remember { mutableStateOf<Position?>(null) }

    val sourceList = if (selectedTab == 0) marketPlayers else userClub.squad
    val filteredList = remember(sourceList, searchQuery, positionFilter) {
        sourceList.filter { p ->
            val matchesQuery = searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true)
            val matchesPos = positionFilter == null || p.position == positionFilter
            matchesQuery && matchesPos
        }.sortedByDescending { it.overall }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Budget Summary Strip
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, HairlineBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Transfer Budget", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = RealPlayerRosterBuilder.formatBudget(userClub.budgetM),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TurfEmerald,
                    )
                }
                Surface(
                    color = SlateCard,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HairlineBorder),
                ) {
                    Text(
                        text = "Squad ${userClub.squad.size}/26",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = TurfEmerald,
            modifier = Modifier.widthIn(max = 640.dp),
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Scout (${marketPlayers.size})", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("tab_buy_players"),
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Sell (${userClub.squad.size})", maxLines = 1, softWrap = false) },
                modifier = Modifier.testTag("tab_sell_players"),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search player name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurfEmerald,
                            unfocusedBorderColor = HairlineBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_search_input"),
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = positionFilter == null,
                                onClick = { positionFilter = null },
                                label = { Text("ALL", maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurfEmerald,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = SlateCard,
                                ),
                            )
                        }
                        items(Position.entries) { pos ->
                            FilterChip(
                                selected = positionFilter == pos,
                                onClick = { positionFilter = pos },
                                label = { Text(pos.label, maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TurfEmerald,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = SlateCard,
                                ),
                                modifier = Modifier.testTag("filter_${pos.label}"),
                            )
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, HairlineBorder),
                    ) {
                        Text(
                            text = "No players match your current filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { player ->
                    if (selectedTab == 0) {
                        val canAfford = userClub.budgetM >= player.marketValueM && userClub.squad.size < 26
                        TransferPlayerCard(
                            player = player,
                            actionLabel = "Sign ${RealPlayerRosterBuilder.formatBudget(player.marketValueM)}",
                            enabled = canAfford,
                            isBuy = true,
                            onAction = { onBuyPlayer(player.id) },
                        )
                    } else {
                        val saleVal = ((player.marketValueM * 0.9) * 100.0).roundToInt() / 100.0
                        val canSell = userClub.squad.size > 13
                        TransferPlayerCard(
                            player = player,
                            actionLabel = "Sell ${RealPlayerRosterBuilder.formatBudget(saleVal)}",
                            enabled = canSell,
                            isBuy = false,
                            onAction = { onSellPlayer(player.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferPlayerCard(
    player: Player,
    actionLabel: String,
    enabled: Boolean,
    isBuy: Boolean,
    onAction: () -> Unit,
) {
    val posColor = when (player.position) {
        Position.GK -> BroadcastAmber
        Position.DEF -> ElectricCyan
        Position.MID -> TurfEmerald
        Position.FWD -> CrimsonRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, HairlineBorder),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp),
            ) {
                Text(
                    text = "${player.overall}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
                Surface(
                    color = posColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = player.position.label,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = posColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${player.nationality} • Age ${player.age} • POT ${player.potential} • €${player.wageK}k/wk • ${player.contractYearsLeft}y",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PAC ${player.pace}  SHO ${player.shooting}  PAS ${player.passing}  DEF ${player.defending}",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isBuy) {
                Button(
                    onClick = onAction,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TurfEmerald,
                        contentColor = PitchBlack,
                        disabledContainerColor = ObsidianSurface,
                        disabledContentColor = TextSecondary,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("buy_${player.id}"),
                ) {
                    Text(
                        text = actionLabel,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onAction,
                    enabled = enabled,
                    border = BorderStroke(1.dp, if (enabled) BroadcastAmber else HairlineBorder),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("sell_${player.id}"),
                ) {
                    Text(
                        text = actionLabel,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (enabled) BroadcastAmber else TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
