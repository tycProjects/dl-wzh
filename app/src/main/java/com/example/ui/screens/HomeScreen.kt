package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DictionaryEntry
import com.example.data.model.LanguageDirection
import com.example.ui.DictionaryViewModel
import com.example.ui.components.AddWordDialog
import com.example.ui.components.SearchBarWithDirection
import com.example.ui.components.WordResultCard
import com.example.ui.theme.CyberDarkBorder
import com.example.ui.theme.CyberDarkElevated
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.CyanDarkSubtle
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.TextLightGray
import com.example.ui.theme.TextMutedGray
import com.example.ui.theme.TextWhite

@Composable
fun HomeScreen(
    viewModel: DictionaryViewModel,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val direction by viewModel.languageDirection.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val totalEntries by viewModel.entryCount.collectAsStateWithLifecycle()
    val verifiedCount by viewModel.verifiedCount.collectAsStateWithLifecycle()
    val needsVerificationCount by viewModel.needsVerificationCount.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF070B12UL

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
            .testTag("home_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.app_full_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Offline Dual-Way Lexicon",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = CyanNeon,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Offline Ready Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) CyanDarkSubtle else CyanNeon.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiOff,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "100% Offline",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon
                                )
                            )
                        }
                    }
                }
            }
        }

        // Search Bar + Language Direction + Swap Button
        item {
            SearchBarWithDirection(
                searchQuery = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                direction = direction,
                onSwapDirection = { viewModel.swapLanguageDirection() },
                onSearchSubmitted = { viewModel.submitSearch(it) }
            )
        }

        // Active Search State vs Default Home Content
        if (searchQuery.isNotBlank()) {
            // Search Results Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Search Results (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Searching: \"$searchQuery\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextLightGray
                        )
                    )
                }
            }

            if (searchResults.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) CyberDarkElevated else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SearchOff,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.no_results_found),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try searching for another word or swap direction to ${direction.swapped().label}.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextLightGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanNeon,
                                    contentColor = CyberDarkElevated
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add This Word to Test", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(searchResults, key = { it.id }) { entry ->
                    WordResultCard(
                        entry = entry,
                        direction = direction,
                        onToggleFavorite = { viewModel.toggleFavorite(entry) }
                    )
                }
            }
        } else {
            // When Search Query is Empty -> Show Overview, Recent Searches, Favorites & Entry Count

            // 1. Number of dictionary entries card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) CyberDarkElevated else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) CyanNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storage,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Dictionary Database Status",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            // Quick Add Word button
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) CyberDarkSurface else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = CyanNeon
                                ),
                                border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Entry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBox(
                                title = "Total Entries",
                                value = "$totalEntries",
                                subtitle = "Test placeholders",
                                modifier = Modifier.weight(1f),
                                isDark = isDark
                            )
                            StatBox(
                                title = "Needs Verification",
                                value = "$needsVerificationCount",
                                subtitle = "Awaiting final list",
                                modifier = Modifier.weight(1f),
                                isDark = isDark
                            )
                            StatBox(
                                title = "Verified",
                                value = "$verifiedCount",
                                subtitle = "Approved words",
                                modifier = Modifier.weight(1f),
                                isDark = isDark
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Stage 1: Testing mode with labeled placeholders. Ready for your complete word list when you say 'FINALIZE THE DICTIONARY'.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextMutedGray,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // 2. Recent Searches
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.recent_searches),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        if (recentHistory.isNotEmpty()) {
                            Text(
                                text = "View all",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = CyanNeon,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier
                                    .clickable { onNavigateToHistory() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (recentHistory.isEmpty()) {
                        Text(
                            text = "No recent searches yet. Search any word above.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextLightGray)
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentHistory.take(6), key = { it.id }) { historyItem ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) CyberDarkElevated else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable {
                                        viewModel.setSearchQuery(historyItem.query)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = historyItem.query,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Favorites Preview
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Favorites (${favorites.size})",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        if (favorites.isNotEmpty()) {
                            Text(
                                text = "View all",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = CyanNeon,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier
                                    .clickable { onNavigateToFavorites() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (favorites.isEmpty()) {
                        Text(
                            text = "No saved words yet. Tap the star icon on any word to bookmark it.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextLightGray)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            favorites.take(3).forEach { fav ->
                                WordResultCard(
                                    entry = fav,
                                    direction = direction,
                                    onToggleFavorite = { viewModel.toggleFavorite(fav) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) CyberDarkSurface else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (isDark) CyberDarkBorder else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = CyanNeon,
                    fontSize = 20.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) TextWhite else MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMutedGray,
                    fontSize = 9.sp
                )
            )
        }
    }
}
