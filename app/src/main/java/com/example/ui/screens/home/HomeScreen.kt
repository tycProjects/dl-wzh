package com.example.ui.screens.home

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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BottomNavTab
import com.example.data.model.CategoryEntity
import com.example.data.model.ReadingProgressEntity
import com.example.data.model.Screen
import com.example.data.model.StoryEntity
import com.example.ui.components.CategoryChip
import com.example.ui.components.FeaturedStoryCard
import com.example.ui.components.StoryCard
import com.example.ui.components.StoryCoverImage
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GoldStar
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.AppViewModel

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val allStories by viewModel.allStories.collectAsState()
    val featuredStories by viewModel.featuredStories.collectAsState()
    val popularStories by viewModel.popularStories.collectAsState()
    val newStories by viewModel.newStories.collectAsState()
    val topRatedStories by viewModel.topRatedStories.collectAsState()
    val categories by viewModel.categories.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val uid = currentUser?.id ?: "guest"
    val progressList by viewModel.storyRepository.getReadingProgressList(uid).collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Featured Stories Carousel / Hero Card
        if (featuredStories.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    val heroStory = featuredStories.first()
                    FeaturedStoryCard(
                        story = heroStory,
                        onClick = { viewModel.openStoryDetails(heroStory.id) }
                    )
                }
            }
        }

        // 2. Continue Reading (যদি ইতিমধ্যে কোনো গল্প পড়া শুরু থাকে)
        if (progressList.isNotEmpty()) {
            val latestProgress = progressList.first()
            val continuingStory = allStories.firstOrNull { it.id == latestProgress.storyId }
            if (continuingStory != null) {
                item {
                    ContinueReadingBanner(
                        story = continuingStory,
                        progress = latestProgress,
                        onContinueClick = {
                            viewModel.startReading(continuingStory.id, latestProgress.lastChapterNumber)
                        }
                    )
                }
            }
        }

        // 3. Recommended Categories
        item {
            SectionHeader(
                title = "গল্পের বিভাগসমূহ",
                subtitle = "আপনার প্রিয় ঘরানার গল্প বেছে নিন",
                onSeeAllClick = { viewModel.switchTab(BottomNavTab.DISCOVER) }
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(categories) { cat ->
                    CategoryChip(
                        category = cat,
                        onClick = {
                            viewModel.navigateTo(Screen.CategoryStories(cat.id, cat.name))
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. জনপ্রিয় গল্প (Popular Stories)
        item {
            StorySectionRow(
                title = "জনপ্রিয় গল্প",
                subtitle = "পাঠকদের সবচেয়ে বেশি পঠিত",
                stories = popularStories,
                onStoryClick = { viewModel.openStoryDetails(it.id) }
            )
        }

        // 5. আজকের সেরা গল্প (Today's Best)
        item {
            StorySectionRow(
                title = "আজকের সেরা গল্প",
                subtitle = "আজকের দিনে পাঠকদের সেরা পছন্দ",
                stories = topRatedStories,
                onStoryClick = { viewModel.openStoryDetails(it.id) }
            )
        }

        // 6. নতুন প্রকাশিত গল্প (New Releases)
        item {
            StorySectionRow(
                title = "নতুন প্রকাশিত গল্প",
                subtitle = "সাম্প্রতিকতম রোমাঞ্চকর সৃষ্টি",
                stories = newStories,
                onStoryClick = { viewModel.openStoryDetails(it.id) }
            )
        }

        // 7. পাঠকদের পছন্দ ও ট্রেন্ডিং (Readers' Choice & Trending)
        item {
            StorySectionRow(
                title = "পাঠকদের পছন্দ ও ট্রেন্ডিং",
                subtitle = "সোশ্যাল আলোচনায় শীর্ষে থাকা গল্পসমূহ",
                stories = allStories.reversed(),
                onStoryClick = { viewModel.openStoryDetails(it.id) }
            )
        }

        // 8. সম্পাদকীয় নির্বাচন (Editor's Choice Banner)
        item {
            EditorsChoiceBanner(
                onExploreClick = { viewModel.switchTab(BottomNavTab.DISCOVER) }
            )
        }

        // 9. জনপ্রিয় লেখক (Popular Authors)
        item {
            SectionHeader(
                title = "জনপ্রিয় লেখকবৃন্দ",
                subtitle = "গুণী লেখকদের মায়াবী সৃষ্টির সন্ধান",
                onSeeAllClick = null
            )
            AuthorsRow(
                authors = listOf(
                    Triple("মাহিন পারভেজ", "লেখক ও নির্মাতা", true),
                    Triple("শৌমিক ব্যানার্জী", "গোয়েন্দা কথাসাহিত্যিক", true),
                    Triple("তাহমিনা আক্তার", "অনুপ্রেরণামূলক লেখক", true),
                    Triple("মাওলানা আবদুল্লাহ", "নৈতিক ও ইসলামিক চিন্তাবিদ", true),
                    Triple("ড. ফারহান জামান", "কল্পবিজ্ঞান গবেষক", false)
                ),
                onAuthorClick = { name ->
                    viewModel.onSearchQueryChanged(name)
                    viewModel.switchTab(BottomNavTab.DISCOVER)
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 10. আপনার জন্য গল্প (For You)
        item {
            StorySectionRow(
                title = "আপনার জন্য বাছাইকৃত",
                subtitle = "বিশেষ সাহিত্যিক নির্বাচন",
                stories = allStories,
                onStoryClick = { viewModel.openStoryDetails(it.id) }
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
        if (onSeeAllClick != null) {
            TextButton(onClick = onSeeAllClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("সব দেখুন", fontSize = 13.sp, color = SecondaryViolet, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = SecondaryViolet,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StorySectionRow(
    title: String,
    subtitle: String,
    stories: List<StoryEntity>,
    onStoryClick: (StoryEntity) -> Unit
) {
    if (stories.isEmpty()) return
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        SectionHeader(title = title, subtitle = subtitle)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            items(stories) { story ->
                StoryCard(
                    story = story,
                    onClick = { onStoryClick(story) }
                )
            }
        }
    }
}

@Composable
fun ContinueReadingBanner(
    story: StoryEntity,
    progress: ReadingProgressEntity,
    onContinueClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onContinueClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryPurple.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                StoryCoverImage(
                    coverName = story.coverResName,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = PrimaryPurple,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "পড়া চালিয়ে যান",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Text(
                    text = "অধ্যায় ${progress.lastChapterNumber} • ${(progress.progressPercent * 100).toInt()}% সম্পন্ন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progress.progressPercent.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = SecondaryViolet,
                    trackColor = SecondaryViolet.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "পড়া শুরু করুন",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun EditorsChoiceBanner(
    onExploreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkBg
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PrimaryPurple,
                            SecondaryViolet
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Surface(
                color = GoldStar,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "সম্পাদকীয় নির্বাচন",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "বাংলা সাহিত্যের অনবদ্য রত্নভাণ্ডার",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "অনন্য ভাষাভঙ্গি, গভীর জীবনবোধ ও আধুনিক পরিবেশনায় তৈরি বাংলা গল্পের এক অপূর্ব অভিজ্ঞতা।",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = PrimaryPurple
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("নতুন গল্প খুঁজুন", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AuthorsRow(
    authors: List<Triple<String, String, Boolean>>,
    onAuthorClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(authors) { (name, role, verified) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(90.dp)
                    .clickable { onAuthorClick(name) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = name,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 12.sp
                    )
                    if (verified) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "ভেরিফায়েড",
                            tint = VerifiedBlue,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Text(
                    text = role,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    fontSize = 10.sp
                )
            }
        }
    }
}
