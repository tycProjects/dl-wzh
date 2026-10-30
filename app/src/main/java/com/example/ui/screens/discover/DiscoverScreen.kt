package com.example.ui.screens.discover

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.StoryEntity
import com.example.ui.components.CategoryChip
import com.example.ui.components.StoryListItemCard
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import com.example.ui.viewmodel.AppViewModel

@Composable
fun DiscoverScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val allStories by viewModel.allStories.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var sortBy by remember { mutableStateOf("POPULAR") } // POPULAR, NEWEST, RATING

    // Filtered list
    val displayedStories = remember(searchQuery, searchResults, allStories, selectedCategoryId, sortBy) {
        val baseList = if (searchQuery.isNotBlank()) searchResults else allStories
        val categoryFiltered = if (selectedCategoryId != null) {
            baseList.filter { it.categoryId == selectedCategoryId }
        } else {
            baseList
        }
        when (sortBy) {
            "NEWEST" -> categoryFiltered.sortedByDescending { it.id }
            "RATING" -> categoryFiltered.sortedByDescending { it.rating }
            else -> categoryFiltered.sortedByDescending { it.readCount }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header & Search Bar
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "আবিষ্কার ও অনুসন্ধান",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("গল্পের নাম, লেখক বা বিষয় খুঁজুন...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "সার্চ", tint = PrimaryPurple)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "মুছুন")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("discover_search_input"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )
        }

        // Sorting Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "সাজান:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            FilterChip(
                selected = sortBy == "POPULAR",
                onClick = { sortBy = "POPULAR" },
                label = { Text("জনপ্রিয়তা", fontSize = 12.sp) }
            )
            FilterChip(
                selected = sortBy == "NEWEST",
                onClick = { sortBy = "NEWEST" },
                label = { Text("নতুনত্ব", fontSize = 12.sp) }
            )
            FilterChip(
                selected = sortBy == "RATING",
                onClick = { sortBy = "RATING" },
                label = { Text("সর্বোচ্চ রেটিং", fontSize = 12.sp) }
            )
        }

        // Category Pills Filter Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { selectedCategoryId = null },
                    shape = RoundedCornerShape(20.dp),
                    color = if (selectedCategoryId == null) PrimaryPurple else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "সকল বিভাগ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedCategoryId == null) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            items(categories) { cat ->
                CategoryChip(
                    category = cat,
                    isSelected = selectedCategoryId == cat.id,
                    onClick = {
                        selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                    }
                )
            }
        }

        // Stories List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Trending Searches Banner if query empty
            if (searchQuery.isBlank() && selectedCategoryId == null) {
                item {
                    TrendingSearchesRow(
                        onTagClick = { tag -> viewModel.onSearchQueryChanged(tag) }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            item {
                Text(
                    text = "গল্পের তালিকা (${displayedStories.size})",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (displayedStories.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "কোনো গল্প খুঁজে পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "অন্য কোনো শিরোনাম বা বিভাগে অনুসন্ধান করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                items(displayedStories) { story ->
                    StoryListItemCard(
                        story = story,
                        onClick = { viewModel.openStoryDetails(story.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }
}

@Composable
fun TrendingSearchesRow(onTagClick: (String) -> Unit) {
    val trending = listOf("পদ্মা নদী", "গোয়েন্দা", "মাহিন পারভেজ", "রহস্য", "অনুপ্রেরণা", "প্রেম")
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = SecondaryViolet,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "ট্রেন্ডিং অনুসন্ধান",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SecondaryViolet
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(trending) { tag ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SecondaryViolet.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onTagClick(tag) }
                ) {
                    Text(
                        text = "#$tag",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
