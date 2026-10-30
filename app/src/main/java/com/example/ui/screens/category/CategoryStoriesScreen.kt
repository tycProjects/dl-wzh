package com.example.ui.screens.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StoryListItemCard
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.viewmodel.AppViewModel

@Composable
fun CategoryStoriesScreen(
    categoryId: String,
    categoryName: String,
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val stories by viewModel.storyRepository.getStoriesByCategory(categoryId).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "ফিরে যান"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = categoryName,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "${stories.size}টি গল্প রয়েছে",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        if (stories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "এই বিভাগে এখনো কোনো গল্প প্রকাশিত হয়নি। শীঘ্রই যোগ করা হবে।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(stories) { story ->
                    StoryListItemCard(
                        story = story,
                        onClick = { viewModel.openStoryDetails(story.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
