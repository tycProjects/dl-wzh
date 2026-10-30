package com.example.ui.screens.admin

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.ChapterEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.GoldStar
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AdminDashboardScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    // 0: ওভারভিউ (Overview), 1: গল্পসমূহ (Stories), 2: অধ্যায় (Chapters), 3: ক্যাটাগরি (Categories), 4: ব্যবহারকারী (Users), 5: বিজ্ঞপ্তি (Broadcast)

    val allStories by viewModel.allStories.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val allUsers by viewModel.authRepository.getAllUsers().collectAsState(initial = emptyList())

    // Dialog States
    var showAddStoryDialog by remember { mutableStateOf(false) }
    var newStoryTitle by remember { mutableStateOf("") }
    var newStoryAuthor by remember { mutableStateOf("মাহিন পারভেজ") }
    var newStoryDesc by remember { mutableStateOf("") }
    var newStoryCategory by remember { mutableStateOf("রোমান্টিক গল্প") }

    var showAddChapterDialog by remember { mutableStateOf(false) }
    var chapterTargetStoryId by remember { mutableStateOf(allStories.firstOrNull()?.id ?: "") }
    var chapterNumber by remember { mutableIntStateOf(1) }
    var chapterTitle by remember { mutableStateOf("") }
    var chapterContent by remember { mutableStateOf("") }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }

    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMsg by remember { mutableStateOf("") }

    var showChangePassDialog by remember { mutableStateOf(false) }
    var oldPasscode by remember { mutableStateOf("") }
    var newPasscode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Admin Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "অ্যাডমিন কন্ট্রোল প্যানেল",
                    fontFamily = HindSiliguriFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "গল্পকথা ম্যানেজমেন্ট সিস্টেম • Mahin Parvez",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryPurple
                )
            }
            IconButton(onClick = { showChangePassDialog = true }) {
                Icon(Icons.Default.Lock, contentDescription = "পাসকোড পরিবর্তন", tint = SecondaryViolet)
            }
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = PrimaryPurple,
            edgePadding = 12.dp
        ) {
            val tabs = listOf("ড্যাশবোর্ড", "গল্পসমূহ", "অধ্যায় যুক্তকরণ", "ক্যাটাগরি", "ব্যবহারকারী", "ব্রডকাস্ট")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: OVERVIEW & REAL DB STATS
                    item {
                        Text(
                            text = "লাইভ ডাটাবেজ অ্যানালিটিক্স",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminStatCard(
                                title = "মোট গল্প",
                                value = "${allStories.size}",
                                icon = Icons.Default.AutoStories,
                                modifier = Modifier.weight(1f),
                                tint = PrimaryPurple
                            )
                            AdminStatCard(
                                title = "মোট পাঠক",
                                value = "${allUsers.size}",
                                icon = Icons.Default.People,
                                modifier = Modifier.weight(1f),
                                tint = SecondaryViolet
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminStatCard(
                                title = "মোট ক্যাটাগরি",
                                value = "${categories.size}",
                                icon = Icons.Default.Category,
                                modifier = Modifier.weight(1f),
                                tint = GoldStar
                            )
                            val totalReads = allStories.sumOf { it.readCount }
                            AdminStatCard(
                                title = "মোট পঠন সংখ্যা",
                                value = "$totalReads",
                                icon = Icons.Default.MenuBook,
                                modifier = Modifier.weight(1f),
                                tint = Color(0xFF00BFA5)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Quick Actions
                        Text(
                            text = "দ্রুত কার্যক্রম",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showAddStoryDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("নতুন গল্প")
                            }

                            Button(
                                onClick = { showAddChapterDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryViolet),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("নতুন অধ্যায়")
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: STORY MANAGEMENT
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "প্রকাশিত গল্পসমূহ (${allStories.size})",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Button(
                                onClick = { showAddStoryDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("গল্প যুক্ত করুন")
                            }
                        }
                    }

                    items(allStories) { story ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = story.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = "${story.authorName} • ${story.categoryName} • ${story.readCount} পঠিত",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                IconButton(onClick = {
                                    chapterTargetStoryId = story.id
                                    showAddChapterDialog = true
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "অধ্যায় যোগ", tint = PrimaryPurple)
                                }

                                IconButton(onClick = {
                                    viewModel.deleteStory(story)
                                    Toast.makeText(context, "গল্প মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: CHAPTER ADDITION
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "নতুন অধ্যায় প্রকাশ করুন",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("গল্প নির্বাচন করুন:", style = MaterialTheme.typography.labelMedium)
                                allStories.forEach { st ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { chapterTargetStoryId = st.id }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (chapterTargetStoryId == st.id) Icons.Default.CheckCircle else Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = if (chapterTargetStoryId == st.id) PrimaryPurple else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(st.title, fontWeight = if (chapterTargetStoryId == st.id) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = chapterTitle,
                                    onValueChange = { chapterTitle = it },
                                    label = { Text("অধ্যায়ের শিরোনাম (যেমন: পঞ্চম অধ্যায়: নতুন দিগন্ত)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = chapterContent,
                                    onValueChange = { chapterContent = it },
                                    label = { Text("অধ্যায়ের মূল বাংলা লেখা (Multi-paragraph Text)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (chapterTargetStoryId.isNotBlank() && chapterTitle.isNotBlank() && chapterContent.isNotBlank()) {
                                            val newChap = ChapterEntity(
                                                id = UUID.randomUUID().toString(),
                                                storyId = chapterTargetStoryId,
                                                chapterNumber = chapterNumber + 1,
                                                chapterTitle = chapterTitle.trim(),
                                                content = chapterContent.trim(),
                                                wordCount = chapterContent.split(" ").size,
                                                estimatedMinutes = (chapterContent.split(" ").size / 100).coerceAtLeast(1)
                                            )
                                            viewModel.saveChapter(newChap)
                                            chapterTitle = ""
                                            chapterContent = ""
                                            Toast.makeText(context, "অধ্যায় সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "সকল তথ্য প্রদান করুন", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                ) {
                                    Text("অধ্যায় প্রকাশ করুন")
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: CATEGORY MANAGEMENT
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "বিভাগসমূহ (${categories.size})",
                                fontFamily = HindSiliguriFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Button(
                                onClick = { showAddCategoryDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("বিভাগ যোগ")
                            }
                        }
                    }

                    items(categories) { cat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = cat.name, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "মুছুন", tint = Color.Red.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: USER MANAGEMENT & VERIFICATION BADGE
                    item {
                        Text(
                            text = "ব্যবহারকারী তালিকা ও ভেরিফিকেশন (${allUsers.size})",
                            fontFamily = HindSiliguriFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    items(allUsers) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = user.fullName, fontWeight = FontWeight.Bold)
                                        if (user.isVerified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.CheckCircle, contentDescription = "ভেরিফায়েড", tint = VerifiedBlue, modifier = Modifier.size(14.dp))
                                        }
                                        if (user.isAdmin) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("(অ্যাডমিন)", color = PrimaryPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(
                                        text = "@${user.username} • ${user.email}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                // Toggle Verification Button
                                IconButton(onClick = {
                                    scope.launch {
                                        viewModel.authRepository.toggleUserVerification(user.id, user.isVerified)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "ভেরিফিকেশন টগল",
                                        tint = if (user.isVerified) VerifiedBlue else Color.LightGray
                                    )
                                }

                                // Toggle Ban/Suspend
                                IconButton(onClick = {
                                    scope.launch {
                                        viewModel.authRepository.toggleUserBan(user.id, user.isBanned)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "স্থগিত টগল",
                                        tint = if (user.isBanned) Color.Red else Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }

                5 -> {
                    // TAB 5: BROADCAST NOTIFICATIONS
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "সকল পাঠকের কাছে ইন-অ্যাপ নোটিফিকেশন পাঠান",
                                    fontFamily = HindSiliguriFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = broadcastTitle,
                                    onValueChange = { broadcastTitle = it },
                                    label = { Text("বিজ্ঞপ্তির শিরোনাম") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = broadcastMsg,
                                    onValueChange = { broadcastMsg = it },
                                    label = { Text("বিজ্ঞপ্তির বার্তা বা ঘোষণা") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (broadcastTitle.isNotBlank() && broadcastMsg.isNotBlank()) {
                                            viewModel.sendBroadcast(broadcastTitle.trim(), broadcastMsg.trim(), "ANNOUNCEMENT")
                                            broadcastTitle = ""
                                            broadcastMsg = ""
                                            Toast.makeText(context, "বিজ্ঞপ্তি সফলভাবে সম্প্রচারিত হয়েছে!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ব্রডকাস্ট পাঠান")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(90.dp)) }
        }
    }

    // Add Story Dialog
    if (showAddStoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddStoryDialog = false },
            title = { Text("নতুন গল্প প্রকাশ") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newStoryTitle,
                        onValueChange = { newStoryTitle = it },
                        label = { Text("গল্পের নাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newStoryAuthor,
                        onValueChange = { newStoryAuthor = it },
                        label = { Text("লেখকের নাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newStoryDesc,
                        onValueChange = { newStoryDesc = it },
                        label = { Text("সংক্ষিপ্ত বিবরণ") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStoryTitle.isNotBlank()) {
                            val newStory = StoryEntity(
                                id = UUID.randomUUID().toString(),
                                title = newStoryTitle.trim(),
                                description = newStoryDesc.trim(),
                                authorId = "admin_mahin",
                                authorName = newStoryAuthor.trim(),
                                authorVerified = true,
                                categoryId = "cat_romantic",
                                categoryName = newStoryCategory,
                                coverResName = "cover_river",
                                totalChapters = 1,
                                isPublished = true
                            )
                            viewModel.saveStory(newStory)
                            showAddStoryDialog = false
                            newStoryTitle = ""
                            newStoryDesc = ""
                            Toast.makeText(context, "নতুন গল্প সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoryDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("নতুন বিভাগ যোগ করুন") },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("বিভাগের নাম (যেমন: পৌরাণিক গল্প)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.saveCategory(
                                CategoryEntity(
                                    id = "cat_${System.currentTimeMillis()}",
                                    name = newCatName.trim(),
                                    iconName = "auto_stories",
                                    orderIndex = categories.size + 1
                                )
                            )
                            showAddCategoryDialog = false
                            newCatName = ""
                            Toast.makeText(context, "বিভাগ যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("যোগ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Change Admin Passcode Dialog
    if (showChangePassDialog) {
        AlertDialog(
            onDismissRequest = { showChangePassDialog = false },
            title = { Text("অ্যাডমিন পাসকোড পরিবর্তন") },
            text = {
                Column {
                    OutlinedTextField(
                        value = oldPasscode,
                        onValueChange = { oldPasscode = it },
                        label = { Text("বর্তমান পাসকোড") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPasscode,
                        onValueChange = { newPasscode = it },
                        label = { Text("নতুন পাসকোড") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val res = viewModel.authRepository.changeAdminPasscode(oldPasscode, newPasscode)
                        res.fold(
                            onSuccess = {
                                showChangePassDialog = false
                                oldPasscode = ""
                                newPasscode = ""
                                Toast.makeText(context, "পাসকোড পরিবর্তন সম্পন্ন!", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = {
                                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }) {
                    Text("পরিবর্তন করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePassDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
