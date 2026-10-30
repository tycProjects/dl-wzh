package com.example.ui.screens.details

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BottomNavTab
import com.example.data.model.ChapterEntity
import com.example.data.model.CommentEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.StoryEntity
import com.example.ui.components.StoryCard
import com.example.ui.components.StoryCoverImage
import com.example.ui.theme.AccentLilac
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GoldStar
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.AppViewModel

@Composable
fun StoryDetailsScreen(
    storyId: String,
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    val story by viewModel.storyRepository.observeStoryById(storyId).collectAsState(initial = null)
    val chapters by viewModel.storyRepository.getChapters(storyId).collectAsState(initial = emptyList())
    val comments by viewModel.storyRepository.getCommentsForStory(storyId).collectAsState(initial = emptyList())
    val reviews by viewModel.storyRepository.getReviewsForStory(storyId).collectAsState(initial = emptyList())
    val allStories by viewModel.allStories.collectAsState()

    val currentUser by viewModel.currentUser.collectAsState()
    val uid = currentUser?.id ?: "guest"
    val isFav by viewModel.storyRepository.isFavorite(uid, storyId).collectAsState(initial = false)
    val isFollowingAuthor by viewModel.storyRepository.isFollowingAuthor(uid, story?.authorName ?: "").collectAsState(initial = false)

    var selectedTab by remember { mutableIntStateOf(0) } // 0: অধ্যায়সমূহ, 1: পর্যালোচনা ও মন্তব্য
    var newCommentText by remember { mutableStateOf("") }
    var reviewRating by remember { mutableIntStateOf(5) }
    var newReviewText by remember { mutableStateOf("") }

    if (story == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("গল্পের তথ্য লোড হচ্ছে...")
        }
        return
    }

    val currentStory = story!!

    ScaffoldWithDetails(
        story = currentStory,
        chapters = chapters,
        comments = comments,
        reviews = reviews,
        allStories = allStories,
        isFavorite = isFav,
        isFollowingAuthor = isFollowingAuthor,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        newCommentText = newCommentText,
        onCommentTextChanged = { newCommentText = it },
        onSendComment = {
            viewModel.addComment(currentStory.id, 0, newCommentText)
            newCommentText = ""
        },
        newReviewText = newReviewText,
        onReviewTextChanged = { newReviewText = it },
        reviewRating = reviewRating,
        onRatingChanged = { reviewRating = it },
        onSendReview = {
            viewModel.addReview(currentStory.id, reviewRating, newReviewText)
            newReviewText = ""
        },
        onReadClick = { viewModel.startReading(currentStory.id, 1) },
        onChapterClick = { chapNum -> viewModel.startReading(currentStory.id, chapNum) },
        onToggleFavorite = { viewModel.toggleFavorite(currentStory.id, isFav) },
        onToggleFollowAuthor = { viewModel.toggleFollowAuthor(currentStory.authorName, isFollowingAuthor) },
        onShareClick = {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "গল্পকথা অ্যাপ্লিকেশনে চমৎকার এই গল্পটি পড়ুন: '${currentStory.title}' (লেখক: ${currentStory.authorName})")
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(shareIntent, "গল্পটি শেয়ার করুন"))
        },
        onStorySelect = { nextStoryId -> viewModel.openStoryDetails(nextStoryId) },
        onBackClick = onBackClick
    )
}

@Composable
private fun ScaffoldWithDetails(
    story: StoryEntity,
    chapters: List<ChapterEntity>,
    comments: List<CommentEntity>,
    reviews: List<ReviewEntity>,
    allStories: List<StoryEntity>,
    isFavorite: Boolean,
    isFollowingAuthor: Boolean,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    newCommentText: String,
    onCommentTextChanged: (String) -> Unit,
    onSendComment: () -> Unit,
    newReviewText: String,
    onReviewTextChanged: (String) -> Unit,
    reviewRating: Int,
    onRatingChanged: (Int) -> Unit,
    onSendReview: () -> Unit,
    onReadClick: () -> Unit,
    onChapterClick: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleFollowAuthor: () -> Unit,
    onShareClick: () -> Unit,
    onStorySelect: (String) -> Unit,
    onBackClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Top Illustrated Banner with Back & Share buttons
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                StoryCoverImage(
                    coverName = story.coverResName,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Top Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
                            tint = Color.White
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "পছন্দ",
                                tint = if (isFavorite) Color(0xFFFF4081) else Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onShareClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "শেয়ার",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Story Title, Author, and Categories
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Category Chip Tag
                Surface(
                    color = PrimaryPurple.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = story.categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryPurple,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = story.title,
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Author Row with Follow option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = story.authorName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (story.authorVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "ভেরিফায়েড",
                                        tint = VerifiedBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = "লেখক",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleFollowAuthor,
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFollowingAuthor) "অনুসরণ করছেন" else "অনুসরণ করুন",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFollowingAuthor) PrimaryPurple else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Stats Dashboard Row (Chapters, Reads, Rating, Date)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(
                        icon = Icons.Default.MenuBook,
                        value = "${chapters.size}",
                        label = "অধ্যায়"
                    )
                    StatDivider()
                    StatItem(
                        icon = Icons.Outlined.Visibility,
                        value = "${story.readCount}",
                        label = "পঠিত"
                    )
                    StatDivider()
                    StatItem(
                        icon = Icons.Default.Star,
                        value = String.format("%.1f", story.rating),
                        label = "রেটিং",
                        iconTint = GoldStar
                    )
                    StatDivider()
                    StatItem(
                        icon = Icons.Default.Favorite,
                        value = "${story.likesCount}",
                        label = "পছন্দ",
                        iconTint = Color(0xFFFF4081)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary CTA: গল্প পড়ুন
                Button(
                    onClick = onReadClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("read_story_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "গল্প পড়ুন",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Description
                Text(
                    text = "গল্পের সারাংশ",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Tabs: অধ্যায় তালিকা / মন্তব্য ও পর্যালোচনা
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryPurple
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { onTabSelected(0) },
                        text = { Text("অধ্যায়সমূহ (${chapters.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { onTabSelected(1) },
                        text = { Text("পর্যালোচনা ও মন্তব্য (${comments.size + reviews.size})", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Tab Content
        if (selectedTab == 0) {
            // Chapters List
            items(chapters) { chap ->
                ChapterItemCard(
                    chapter = chap,
                    onClick = { onChapterClick(chap.chapterNumber) }
                )
            }
        } else {
            // Reviews & Comments Tab
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Add Review Box
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "গল্পটি মূল্যায়ন করুন (স্টার রেটিং)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                (1..5).forEach { star ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "$star তারা",
                                        tint = if (star <= reviewRating) GoldStar else Color.LightGray,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { onRatingChanged(star) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = newReviewText,
                                onValueChange = onReviewTextChanged,
                                placeholder = { Text("আপনার মূল্যবান মতামত লিখুন...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onSendReview,
                                enabled = newReviewText.isNotBlank(),
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                            ) {
                                Text("পর্যালোচনা জমা দিন")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Comment Input Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = onCommentTextChanged,
                            placeholder = { Text("একটি মন্তব্য যোগ করুন...") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onSendComment,
                            enabled = newCommentText.isNotBlank(),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PrimaryPurple)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "পাঠান",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "পাঠকদের মতামত",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            }

            // List of Reviews
            items(reviews) { rev ->
                ReviewItemCard(review = rev)
            }

            // List of Comments
            items(comments) { comm ->
                CommentItemCard(comment = comm)
            }
        }

        // Similar Stories Section
        item {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text(
                    text = "অনুরূপ আরও গল্প",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val similar = allStories.filter { it.id != story.id }
                    items(similar) { simStory ->
                        StoryCard(
                            story = simStory,
                            onClick = { onStorySelect(simStory.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterItemCard(
    chapter: ChapterEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("chapter_item_${chapter.chapterNumber}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${chapter.chapterNumber}",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = chapter.chapterTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${chapter.estimatedMinutes} মিনিট পাঠ • ${chapter.wordCount} শব্দ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.AutoStories,
                contentDescription = "পড়ুন",
                tint = SecondaryViolet,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ReviewItemCard(review: ReviewEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.userName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    (1..review.rating).forEach {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = GoldStar,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = review.reviewText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun CommentItemCard(comment: CommentEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.userName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (comment.userVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "ভেরিফায়েড",
                            tint = VerifiedBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comment.commentText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    iconTint: Color = PrimaryPurple
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}
