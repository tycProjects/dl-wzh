package com.example.ui.screens.library

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookmarkEntity
import com.example.data.model.ReadingProgressEntity
import com.example.data.model.StoryEntity
import com.example.ui.components.StoryCoverImage
import com.example.ui.components.StoryListItemCard
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import com.example.ui.viewmodel.AppViewModel

@Composable
fun LibraryScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val uid = currentUser?.id ?: "guest"

    val allStories by viewModel.allStories.collectAsState()
    val readingProgressList by viewModel.storyRepository.getReadingProgressList(uid).collectAsState(initial = emptyList())
    val favorites by viewModel.storyRepository.getFavorites(uid).collectAsState(initial = emptyList())
    val bookmarks by viewModel.storyRepository.getBookmarks(uid).collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    // 0: বর্তমানে পড়ছি, 1: প্রিয় ও সংরক্ষিত, 2: বুকমার্কসমূহ, 3: ইতিহাস

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Library Header
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = "আমার লাইব্রেরি",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "আপনার সংরক্ষিত বই ও পড়ার অগ্রগতি",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = PrimaryPurple,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("পড়ছি (${readingProgressList.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("প্রিয় (${favorites.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("বুকমার্ক (${bookmarks.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("ইতিহাস", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Currently Reading Tab
                    if (readingProgressList.isEmpty()) {
                        item {
                            EmptyLibraryView(
                                title = "কোনো চলমান গল্প নেই",
                                subtitle = "হোম পেজ থেকে আপনার পছন্দের গল্প পড়তে শুরু করুন।"
                            )
                        }
                    } else {
                        items(readingProgressList) { progress ->
                            val story = allStories.firstOrNull { it.id == progress.storyId }
                            if (story != null) {
                                ReadingProgressCard(
                                    story = story,
                                    progress = progress,
                                    onClick = {
                                        viewModel.startReading(story.id, progress.lastChapterNumber)
                                    }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
                1 -> {
                    // Favorites Tab
                    val favStoryIds = favorites.map { it.storyId }
                    val favStories = allStories.filter { it.id in favStoryIds }

                    if (favStories.isEmpty()) {
                        item {
                            EmptyLibraryView(
                                title = "কোনো প্রিয় গল্প সংরক্ষিত নেই",
                                subtitle = "গল্পের বিস্তারিত পাতায় লাভ আইকনে চাপ দিয়ে প্রিয় তালিকায় যোগ করুন।"
                            )
                        }
                    } else {
                        items(favStories) { story ->
                            StoryListItemCard(
                                story = story,
                                onClick = { viewModel.openStoryDetails(story.id) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                2 -> {
                    // Bookmarks Tab
                    if (bookmarks.isEmpty()) {
                        item {
                            EmptyLibraryView(
                                title = "কোনো বুকমার্ক নেই",
                                subtitle = "গল্প পড়ার সময় যেকোনো প্যারাগ্রাফ বুকমার্ক করে রাখতে পারবেন।"
                            )
                        }
                    } else {
                        items(bookmarks) { bmk ->
                            val story = allStories.firstOrNull { it.id == bmk.storyId }
                            BookmarkItemCard(
                                bookmark = bmk,
                                storyTitle = story?.title ?: "অজানা গল্প",
                                onClick = {
                                    viewModel.startReading(bmk.storyId, bmk.chapterNumber)
                                },
                                onDelete = {
                                    viewModel.removeBookmark(bmk.storyId, bmk.chapterNumber, bmk.paragraphIndex)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                3 -> {
                    // Reading History Tab
                    if (readingProgressList.isEmpty()) {
                        item {
                            EmptyLibraryView(
                                title = "পড়ার কোনো ইতিহাস নেই",
                                subtitle = "আপনি যেসব গল্প পড়বেন তা এখানে ক্রমানুসারে দেখতে পাবেন।"
                            )
                        }
                    } else {
                        items(readingProgressList) { progress ->
                            val story = allStories.firstOrNull { it.id == progress.storyId }
                            if (story != null) {
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
            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }
}

@Composable
fun ReadingProgressCard(
    story: StoryEntity,
    progress: ReadingProgressEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 65.dp, height = 90.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                StoryCoverImage(coverName = story.coverResName, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "লেখক: ${story.authorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "অধ্যায় ${progress.lastChapterNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${(progress.progressPercent * 100).toInt()}% সম্পন্ন",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { progress.progressPercent.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = PrimaryPurple,
                    trackColor = PrimaryPurple.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
fun BookmarkItemCard(
    bookmark: BookmarkEntity,
    storyTitle: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = PrimaryPurple)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = storyTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "অধ্যায় ${bookmark.chapterNumber} • প্যারাগ্রাফ #${bookmark.paragraphIndex + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryViolet
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${bookmark.paragraphSnippet}...\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 2
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "মুছুন",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun EmptyLibraryView(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(PrimaryPurple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontFamily = HindSiliguriFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
