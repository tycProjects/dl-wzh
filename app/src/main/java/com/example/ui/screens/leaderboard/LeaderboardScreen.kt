package com.example.ui.screens.leaderboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.LeaderboardEntry
import com.example.domain.model.LeaderboardFilter
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LeaderboardScreen(
    getLeaderboard: (LeaderboardFilter) -> List<LeaderboardEntry>,
    onBack: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(LeaderboardFilter.CURRENT_TOURNAMENT) }
    val entries = remember(selectedFilter) { getLeaderboard(selectedFilter) }
    val top3 = entries.take(3)
    val rest = entries.drop(3)
    val fmt = remember { NumberFormat.getNumberInstance(Locale.US) }

    Scaffold(
        containerColor = NexoraBlack,
        topBar = {
            NexoraTopBar(title = "Leaderboard", onBack = onBack)
        },
        bottomBar = {
            Surface(
                color = NexoraCharcoal,
                border = BorderStroke(1.dp, VioletBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NexoraGlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = VioletBorder,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text("Your Rank", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                        Text("#10", style = MaterialTheme.typography.headlineSmall, color = ElectricViolet)
                    }
                    NexoraGlassCard(
                        modifier = Modifier.weight(1f),
                        borderColor = GoldBorder,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text("Total Score", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                        Text("7,650 Pts", style = MaterialTheme.typography.headlineSmall, color = BrightGold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val filters = LeaderboardFilter.entries
                val selectedIdx = filters.indexOf(selectedFilter)
                TabRow(
                    selectedTabIndex = selectedIdx,
                    containerColor = NexoraSurface,
                    contentColor = SoftWhite,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIdx]),
                            color = ElectricViolet
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    filters.forEach { filter ->
                        Tab(
                            selected = filter == selectedFilter,
                            onClick = { selectedFilter = filter },
                            text = {
                                Text(
                                    filter.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (filter == selectedFilter) SoftWhite else CoolGrey
                                )
                            }
                        )
                    }
                }
            }

            if (top3.size >= 3) {
                item {
                    PodiumSection(top1 = top3[0], top2 = top3[1], top3 = top3[2], fmt = fmt)
                }
            }

            items(rest, key = { it.rank }) { entry ->
                NexoraClayCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${entry.rank}",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricViolet,
                            modifier = Modifier.width(38.dp)
                        )
                        NexoraAvatar(
                            avatarUri = null,
                            contentDescription = entry.nexoraUsername,
                            size = 40.dp,
                            borderColor = VioletBorder
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.nexoraUsername, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                            Text("IGN: ${entry.ffIgn}${entry.teamName?.let { " • $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = CoolGrey, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${fmt.format(entry.totalScore)}", style = MaterialTheme.typography.titleSmall, color = BrightGold, fontWeight = FontWeight.Bold)
                            Text("${entry.kills} Kills", style = MaterialTheme.typography.labelSmall, color = CoolGrey)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSection(
    top1: LeaderboardEntry,
    top2: LeaderboardEntry,
    top3: LeaderboardEntry,
    fmt: NumberFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumColumn(entry = top2, accent = SilverRank, rankLabel = "2", heightFactor = 1f, fmt = fmt, modifier = Modifier.weight(1f))
        PodiumColumn(entry = top1, accent = BrightGold, rankLabel = "1", heightFactor = 1.18f, fmt = fmt, modifier = Modifier.weight(1.1f))
        PodiumColumn(entry = top3, accent = BronzeRank, rankLabel = "3", heightFactor = 0.95f, fmt = fmt, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PodiumColumn(
    entry: LeaderboardEntry,
    accent: Color,
    rankLabel: String,
    heightFactor: Float,
    fmt: NumberFormat,
    modifier: Modifier = Modifier
) {
    NexoraGlassCard(
        modifier = modifier,
        borderColor = accent.copy(alpha = 0.7f),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = (14 * heightFactor).dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                NexoraAvatar(
                    avatarUri = null,
                    contentDescription = entry.nexoraUsername,
                    size = if (rankLabel == "1") 64.dp else 52.dp,
                    borderColor = accent
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(rankLabel, style = MaterialTheme.typography.labelSmall, color = NexoraBlack, fontWeight = FontWeight.ExtraBold)
                }
            }
            if (rankLabel == "1") {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BrightGold, modifier = Modifier.size(18.dp))
            }
            Text(
                text = entry.nexoraUsername,
                style = MaterialTheme.typography.labelLarge,
                color = SoftWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = fmt.format(entry.totalScore),
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${entry.kills} Kills",
                style = MaterialTheme.typography.labelSmall,
                color = CoolGrey
            )
        }
    }
}
