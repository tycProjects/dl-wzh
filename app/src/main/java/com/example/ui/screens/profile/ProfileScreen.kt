package com.example.ui.screens.profile

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Screen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.GoldStar
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.VerifiedBlue
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val uid = currentUser?.id ?: "guest"

    val readingProgressList by viewModel.storyRepository.getReadingProgressList(uid).collectAsState(initial = emptyList())
    val bookmarks by viewModel.storyRepository.getBookmarks(uid).collectAsState(initial = emptyList())
    val favorites by viewModel.storyRepository.getFavorites(uid).collectAsState(initial = emptyList())
    val timerSeconds by viewModel.readerTimerSeconds.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editFullName by remember { mutableStateOf(currentUser?.fullName ?: "") }
    var editUsername by remember { mutableStateOf(currentUser?.username ?: "") }
    var editBio by remember { mutableStateOf(currentUser?.bio ?: "") }

    var showAdminPasscodeDialog by remember { mutableStateOf(false) }
    var adminPasscodeInput by remember { mutableStateOf("") }
    var adminError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Header Banner & Avatar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PrimaryPurple,
                                SecondaryViolet
                            )
                        )
                    )
            ) {
                // Settings button on top-right
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Settings) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "সেটিংস",
                        tint = Color.White
                    )
                }

                // Profile Avatar positioned overlapping bottom
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.BottomCenter)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(PrimaryPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = PrimaryPurple,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }
            }
        }

        // User Info (Name, Verified Badge, Username, Bio)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentUser?.fullName ?: "পাঠক",
                        fontFamily = HindSiliguriFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    if (currentUser?.isVerified == true) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "ভেরিফায়েড ব্যাজ",
                            tint = VerifiedBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "@${currentUser?.username ?: "reader"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentUser?.bio ?: "গল্পকথার এক নিবেদিত পাঠক।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Edit Profile
                OutlinedButton(
                    onClick = {
                        editFullName = currentUser?.fullName ?: ""
                        editUsername = currentUser?.username ?: ""
                        editBio = currentUser?.bio ?: ""
                        showEditDialog = true
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("প্রোফাইল সম্পাদনা")
                }
            }
        }

        // Stats Row (Read Stories, Reading Time, Bookmarks, Favorites)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatItem(
                        value = "${readingProgressList.size}",
                        label = "পঠিত গল্প",
                        icon = Icons.Default.AutoStories,
                        tint = PrimaryPurple
                    )
                    ProfileStatItem(
                        value = "${timerSeconds / 60} মি.",
                        label = "পড়ার সময়",
                        icon = Icons.Default.Star,
                        tint = GoldStar
                    )
                    ProfileStatItem(
                        value = "${bookmarks.size}",
                        label = "বুকমার্ক",
                        icon = Icons.Default.Bookmark,
                        tint = SecondaryViolet
                    )
                    ProfileStatItem(
                        value = "${favorites.size}",
                        label = "প্রিয় গল্প",
                        icon = Icons.Default.Favorite,
                        tint = Color(0xFFFF4081)
                    )
                }
            }
        }

        // App Creator Showcase: Made by Mahin Parvez
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clickable { viewModel.navigateTo(Screen.OwnerProfile) }
                    .testTag("creator_profile_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryPurple.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MP",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "মাহিন পারভেজ (Mahin Parvez)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VerifiedBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "অ্যাপ প্রতিষ্ঠাতা, সফটওয়্যার আর্কিটেক্ট ও লেখক",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Made by Mahin Parvez • বিস্তারিত দেখতে চাপ দিন →",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Admin Dashboard Link (Secured)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable {
                        // Check if already authenticated or show passcode dialog
                        if (viewModel.isAdminAuthenticated.value) {
                            viewModel.navigateTo(Screen.AdminDashboard)
                        } else {
                            adminError = null
                            showAdminPasscodeDialog = true
                        }
                    }
                    .testTag("admin_panel_button"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SecondaryViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = SecondaryViolet,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "সুরক্ষিত অ্যাডমিন কন্ট্রোল প্যানেল",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "গল্প ও অধ্যায় প্রকাশ, ক্যাটাগরি নিয়ন্ত্রণ ও ইউজার ভেরিফিকেশন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SecondaryViolet
                    )
                }
            }
        }
    }

    // Profile Edit Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("প্রোফাইল তথ্য হালনাগাদ") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = { Text("পূর্ণ নাম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("ইউজারনেম") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("বায়ো (Bio)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val res = viewModel.authRepository.updateProfile(
                                fullName = editFullName,
                                username = editUsername,
                                bio = editBio,
                                avatar = ""
                            )
                            res.fold(
                                onSuccess = {
                                    showEditDialog = false
                                    Toast.makeText(context, "প্রোফাইল সফলভাবে আপডেট হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = {
                                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Secure Admin Passcode Dialog
    if (showAdminPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showAdminPasscodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("অ্যাডমিন যাচাইকরণ")
                }
            },
            text = {
                Column {
                    Text(
                        text = "সুরক্ষিত অ্যাডমিন ড্যাশবোর্ডে প্রবেশ করতে অনুগ্রহ করে আপনার অ্যাডমিন পাসকোড প্রদান করুন:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = adminPasscodeInput,
                        onValueChange = { adminPasscodeInput = it },
                        label = { Text("অ্যাডমিন পাসকোড (ডিফল্ট: 123456)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (adminError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(adminError!!, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val isCorrect = viewModel.authRepository.verifyAdminPasscode(adminPasscodeInput)
                            if (isCorrect) {
                                showAdminPasscodeDialog = false
                                adminPasscodeInput = ""
                                viewModel.navigateTo(Screen.AdminDashboard)
                            } else {
                                adminError = "পাসকোড সঠিক নয়। অনুগ্রহ করে সঠিক পাসকোড দিন।"
                            }
                        }
                    }
                ) {
                    Text("প্রবেশ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminPasscodeDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun ProfileStatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}
