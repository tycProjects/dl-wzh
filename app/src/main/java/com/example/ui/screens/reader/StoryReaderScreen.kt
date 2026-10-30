package com.example.ui.screens.reader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookmarkEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.HighlightEntity
import com.example.data.model.ReadingThemeMode
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.NotoSansBengaliFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.ReadingDarkBg
import com.example.ui.theme.ReadingDarkText
import com.example.ui.theme.ReadingLightBg
import com.example.ui.theme.ReadingLightText
import com.example.ui.theme.ReadingNightBg
import com.example.ui.theme.ReadingNightText
import com.example.ui.theme.ReadingSepiaBg
import com.example.ui.theme.ReadingSepiaText
import com.example.ui.theme.SecondaryViolet
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryReaderScreen(
    storyId: String,
    initialChapterNumber: Int,
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentChapterNumber by remember { mutableIntStateOf(initialChapterNumber) }
    val story by viewModel.storyRepository.observeStoryById(storyId).collectAsState(initial = null)
    val chapters by viewModel.storyRepository.getChapters(storyId).collectAsState(initial = emptyList())

    val currentUser by viewModel.currentUser.collectAsState()
    val uid = currentUser?.id ?: "guest"

    val bookmarks by viewModel.storyRepository.getBookmarks(uid).collectAsState(initial = emptyList())
    val highlights by viewModel.storyRepository.getHighlights(uid, storyId, currentChapterNumber).collectAsState(initial = emptyList())

    // Reader UI state
    val fontSize by viewModel.readerFontSize.collectAsState()
    val lineHeightMultiplier by viewModel.readerLineHeight.collectAsState()
    val themeMode by viewModel.readerThemeMode.collectAsState()
    val autoScroll by viewModel.readerAutoScroll.collectAsState()
    val scrollSpeed by viewModel.readerScrollSpeed.collectAsState()
    val timerSeconds by viewModel.readerTimerSeconds.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var selectedParagraphForAction by remember { mutableStateOf<Pair<Int, String>?>(null) }

    val listState = rememberLazyListState()

    // Determine current chapter
    val currentChapter = chapters.firstOrNull { it.chapterNumber == currentChapterNumber }

    // Colors based on Reading Theme
    val (bgColor, textColor, accentColor) = when (themeMode) {
        ReadingThemeMode.LIGHT -> Triple(ReadingLightBg, ReadingLightText, PrimaryPurple)
        ReadingThemeMode.SEPIA -> Triple(ReadingSepiaBg, ReadingSepiaText, Color(0xFF8D6E63))
        ReadingThemeMode.DARK -> Triple(ReadingDarkBg, ReadingDarkText, SecondaryViolet)
        ReadingThemeMode.NIGHT -> Triple(ReadingNightBg, ReadingNightText, Color(0xFF9E9E9E))
    }

    // Auto-save reading progress on scroll or exit
    val totalItems = currentChapter?.content?.split("\n\n")?.size ?: 1
    val readProgress by remember {
        derivedStateOf {
            if (totalItems <= 1) 1f
            else {
                val index = listState.firstVisibleItemIndex
                (index.toFloat() / totalItems.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    LaunchedEffect(currentChapterNumber, readProgress) {
        viewModel.saveProgress(storyId, currentChapterNumber, readProgress)
    }

    // Auto-scroll loop
    LaunchedEffect(autoScroll, scrollSpeed) {
        if (autoScroll) {
            val pxPerStep = when (scrollSpeed) {
                1 -> 2f
                2 -> 4f
                3 -> 7f
                else -> 3f
            }
            while (isActive && autoScroll) {
                listState.scrollBy(pxPerStep)
                delay(30)
            }
        }
    }

    BackHandler {
        viewModel.stopReaderTimer()
        onBackClick()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopReaderTimer()
        }
    }

    val paragraphs = remember(currentChapter?.content) {
        currentChapter?.content?.split("\n\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Main Content: Bengali Paragraphs
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showControls = !showControls }
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 90.dp, bottom = 110.dp)
        ) {
            // Chapter Title Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = story?.title ?: "",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currentChapter?.chapterTitle ?: "অধ্যায় $currentChapterNumber",
                        fontFamily = HindSiliguriFontFamily,
                        fontSize = (fontSize + 6).sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "আনুমানিক পাঠ: ${currentChapter?.estimatedMinutes ?: 2} মিনিট • ${currentChapter?.wordCount ?: 0} শব্দ",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(2.dp)
                            .background(accentColor.copy(alpha = 0.4f))
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Render each Bengali paragraph with tap-to-action
            itemsIndexed(paragraphs) { index, para ->
                val isBookmarked = bookmarks.any {
                    it.storyId == storyId && it.chapterNumber == currentChapterNumber && it.paragraphIndex == index
                }
                val highlight = highlights.firstOrNull { it.paragraphIndex == index }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (highlight != null) Color(0x33FFEB3B)
                            else if (isBookmarked) accentColor.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .clickable {
                            selectedParagraphForAction = Pair(index, para)
                        }
                        .padding(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (isBookmarked) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "বুকমার্ককৃত",
                                tint = accentColor,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = para,
                            fontFamily = NotoSansBengaliFontFamily,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * lineHeightMultiplier).sp,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Chapter Navigation Footer
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentChapterNumber > 1) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    currentChapterNumber -= 1
                                    scope.launch { listState.scrollToItem(0) }
                                },
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.NavigateBefore, contentDescription = null, tint = accentColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পূর্ববর্তী অধ্যায়", color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    if (currentChapterNumber < chapters.size) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    currentChapterNumber += 1
                                    scope.launch { listState.scrollToItem(0) }
                                },
                            color = accentColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("পরবর্তী অধ্যায়", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.NavigateNext, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Animated Top Control Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bgColor.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                viewModel.stopReaderTimer()
                                onBackClick()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "ফিরে যান",
                                    tint = textColor
                                )
                            }
                            Column {
                                Text(
                                    text = story?.title ?: "",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    maxLines = 1
                                )
                                Text(
                                    text = "অধ্যায় $currentChapterNumber / ${chapters.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                            }
                        }

                        // Reading Timer Chip
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = accentColor.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val minutes = timerSeconds / 60
                                val seconds = timerSeconds % 60
                                Text(
                                    text = String.format("%02d:%02d", minutes, seconds),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }

                    // Reading progress line
                    LinearProgressIndicator(
                        progress = { readProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = accentColor,
                        trackColor = accentColor.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Animated Bottom Control Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = bgColor.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Auto-Scroll Toggle Button
                    IconButton(onClick = { viewModel.readerAutoScroll.value = !autoScroll }) {
                        Icon(
                            imageVector = if (autoScroll) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "অটো-স্ক্রোল",
                            tint = if (autoScroll) accentColor else textColor
                        )
                    }

                    // Format / Settings Button
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "টেক্সট ও ব্যাকগ্রাউন্ড সেটিংস",
                            tint = textColor
                        )
                    }

                    // Reading Theme Quick Cycle (Light -> Sepia -> Dark -> Night)
                    IconButton(onClick = {
                        val nextMode = when (themeMode) {
                            ReadingThemeMode.LIGHT -> ReadingThemeMode.SEPIA
                            ReadingThemeMode.SEPIA -> ReadingThemeMode.DARK
                            ReadingThemeMode.DARK -> ReadingThemeMode.NIGHT
                            ReadingThemeMode.NIGHT -> ReadingThemeMode.LIGHT
                        }
                        viewModel.readerThemeMode.value = nextMode
                    }) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when (themeMode) {
                                        ReadingThemeMode.LIGHT -> Color.White
                                        ReadingThemeMode.SEPIA -> ReadingSepiaBg
                                        ReadingThemeMode.DARK -> ReadingDarkBg
                                        ReadingThemeMode.NIGHT -> Color.Black
                                    }
                                )
                        )
                    }

                    // Quick Bookmark Current Chapter
                    IconButton(onClick = {
                        val snippet = paragraphs.firstOrNull()?.take(50) ?: "অধ্যায় $currentChapterNumber"
                        viewModel.addBookmark(storyId, currentChapterNumber, 0, snippet)
                        Toast.makeText(context, "বুকমার্ক সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "বুকমার্ক করুন",
                            tint = textColor
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Typography & Reading Customization
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "পঠন কাস্টমাইজেশন",
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Font Size Slider
                Text(
                    text = "ফন্ট সাইজ: ${fontSize.toInt()}sp",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = fontSize,
                    onValueChange = { viewModel.readerFontSize.value = it },
                    valueRange = 14f..28f,
                    steps = 14,
                    colors = SliderDefaults.colors(thumbColor = PrimaryPurple, activeTrackColor = PrimaryPurple)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Line Height Slider
                Text(
                    text = "লাইনের ফাঁক (Line Spacing): ${String.format("%.1f", lineHeightMultiplier)}x",
                    style = MaterialTheme.typography.titleSmall
                )
                Slider(
                    value = lineHeightMultiplier,
                    onValueChange = { viewModel.readerLineHeight.value = it },
                    valueRange = 1.3f..2.4f,
                    steps = 11,
                    colors = SliderDefaults.colors(thumbColor = PrimaryPurple, activeTrackColor = PrimaryPurple)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Reading Background Themes (Light, Sepia, Dark, Night)
                Text(
                    text = "পড়ার ব্যাকগ্রাউন্ড থিম",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        Triple(ReadingThemeMode.LIGHT, "আলো", ReadingLightBg),
                        Triple(ReadingThemeMode.SEPIA, "সেপিয়া", ReadingSepiaBg),
                        Triple(ReadingThemeMode.DARK, "অন্ধকার", ReadingDarkBg),
                        Triple(ReadingThemeMode.NIGHT, "নিশুতি", Color.Black)
                    )
                    themes.forEach { (mode, label, color) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { viewModel.readerThemeMode.value = mode },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auto-Scroll Speed Selector
                Text(
                    text = "অটো-স্ক্রোল গতি (Auto Scroll Speed)",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair(1, "ধীর (Slow)"),
                        Pair(2, "মাঝারি (Normal)"),
                        Pair(3, "দ্রুত (Fast)")
                    ).forEach { (spd, label) ->
                        FilterChip(
                            selected = scrollSpeed == spd,
                            onClick = { viewModel.readerScrollSpeed.value = spd },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Paragraph Action Sheet (Bookmark, Highlight, Copy, Share)
    if (selectedParagraphForAction != null) {
        val (pIdx, pText) = selectedParagraphForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedParagraphForAction = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "প্যারাগ্রাফ অপশন",
                    fontFamily = HindSiliguriFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Bookmark this paragraph
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.addBookmark(storyId, currentChapterNumber, pIdx, pText.take(60))
                            selectedParagraphForAction = null
                            Toast.makeText(context, "প্যারাগ্রাফ বুকমার্ক হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("এই প্যারাগ্রাফ বুকমার্ক করুন", fontSize = 16.sp)
                }

                // Highlight Paragraph
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.addHighlight(storyId, currentChapterNumber, pIdx, pText, "#FFEB3B")
                            selectedParagraphForAction = null
                            Toast.makeText(context, "হলুদ রঙে হাইলাইট করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEB3B))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("হাইলাইট করুন (Highlight)", fontSize = 16.sp)
                }

                // Copy Paragraph
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Story Paragraph", pText)
                            clipboard.setPrimaryClip(clip)
                            selectedParagraphForAction = null
                            Toast.makeText(context, "টেক্সট কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("কপি করুন (Copy)", fontSize = 16.sp)
                }

                // Share Paragraph
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "\"$pText\"\n— '${story?.title}' (গল্পকথা অ্যাপ)")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "উদ্ধৃতি শেয়ার করুন"))
                            selectedParagraphForAction = null
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("উদ্ধৃতি শেয়ার করুন (Share)", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
