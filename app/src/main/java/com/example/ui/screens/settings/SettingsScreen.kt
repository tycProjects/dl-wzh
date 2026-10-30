package com.example.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Screen
import com.example.ui.theme.HindSiliguriFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.SecondaryViolet
import com.example.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by viewModel.themeMode.collectAsState()
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var adminPasscode by remember { mutableStateOf("") }
    var adminError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
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
                    contentDescription = "ফিরে যান",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "অ্যাপ্লিকেশন সেটিংস",
                fontFamily = HindSiliguriFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            // Section 1: Appearance & Theme
            item {
                SettingsSectionTitle("থিম ও প্রদর্শন (Appearance)")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.themeMode.value = "LIGHT" }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LightMode, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("লাইট মোড (Light Mode)", modifier = Modifier.weight(1f))
                            RadioButton(
                                selected = themeMode == "LIGHT",
                                onClick = { viewModel.themeMode.value = "LIGHT" },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.themeMode.value = "DARK" }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = SecondaryViolet)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("ডার্ক মোড (Dark Mode)", modifier = Modifier.weight(1f))
                            RadioButton(
                                selected = themeMode == "DARK",
                                onClick = { viewModel.themeMode.value = "DARK" },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.themeMode.value = "SYSTEM" }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("সিস্টেম ডিফল্ট (System Default)", modifier = Modifier.weight(1f))
                            RadioButton(
                                selected = themeMode == "SYSTEM",
                                onClick = { viewModel.themeMode.value = "SYSTEM" },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryPurple)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 2: Notifications
            item {
                SettingsSectionTitle("বিজ্ঞপ্তি ও নোটিফিকেশন")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("নতুন গল্পের বিজ্ঞপ্তি", fontWeight = FontWeight.Bold)
                                Text("নতুন অধ্যায় বা গল্প প্রকাশিত হলে অ্যালার্ট দিন", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple, checkedTrackColor = SecondaryViolet.copy(alpha = 0.5f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 3: Admin & Control
            item {
                SettingsSectionTitle("অ্যাডমিন ও নিরাপত্তা")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdminDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = PrimaryPurple)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("অ্যাডমিন ড্যাশবোর্ড", fontWeight = FontWeight.Bold)
                            Text("গল্প ও অধ্যায় যুক্তকরণ, ব্যবহারকারী নিয়ন্ত্রণ ও বিশ্লেষণ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Icon(Icons.Default.Security, contentDescription = null, tint = SecondaryViolet)
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 4: About & Privacy
            item {
                SettingsSectionTitle("পরিচিতি ও শর্তাবলী")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAboutDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("গল্পকথা অ্যাপ সম্পর্কে", modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPrivacyDialog = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Policy, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("গোপনীয়তা নীতি ও ব্যবহারের শর্তাবলী", modifier = Modifier.weight(1f))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(Screen.OwnerProfile) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Help, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("অ্যাপ নির্মাতা (Made by Mahin Parvez)", modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 5: Account & Logout
            item {
                SettingsSectionTitle("অ্যাকাউন্ট কার্যক্রম")
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("লগআউট করুন (Log Out)", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("লগআউট নিশ্চিতকরণ") },
            text = { Text("আপনি কি নিশ্চিতভাবে এই অ্যাকাউন্ট থেকে লগআউট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.authRepository.logout()
                        viewModel.navigateTo(Screen.Auth)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("লগআউট")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("গল্পকথা - বাংলা গল্প ভুবন") },
            text = {
                Column {
                    Text(
                        text = "গল্পকথা একটি সম্পূর্ণ আধুনিক, ফিচার-সমৃদ্ধ বাংলা গল্প পড়ার প্ল্যাটফর্ম। এখানে পাঠকরা পাবেন মনোমুগ্ধকর গল্প, পরিচ্ছন্ন পঠন অভিজ্ঞতা এবং সমৃদ্ধ সাহিত্যের এক অপূর্ব সম্মিলন।",
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "অ্যাপ নির্মাতা ও সফটওয়্যার আর্কিটেক্ট:\nমাহিন পারভেজ (Mahin Parvez)",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "সংস্করণ: ১.০ (প্রোডাকশন রেডি)", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("গোপনীয়তা নীতি (Privacy Policy)") },
            text = {
                Text(
                    text = "গল্পকথা ব্যবহারকারীর গোপনীয়তাকে সর্বোচ্চ অগ্রাধিকার দেয়। আপনার পড়ার অগ্রগতি, বুকমার্ক ও প্রোফাইল ডেটা নিরাপদে স্থানীয় ডাটাবেজে সংরক্ষিত থাকে। কোনো অননুমোদিত তৃতীয় পক্ষের সাথে ব্যবহারকারীর তথ্য আদান-প্রদান করা হয় না।",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }) {
                    Text("সম্মত আছি")
                }
            }
        )
    }

    // Admin Passcode Dialog
    if (showAdminDialog) {
        AlertDialog(
            onDismissRequest = { showAdminDialog = false },
            title = { Text("অ্যাডমিন পাসকোড যাচাইকরণ") },
            text = {
                Column {
                    Text("অ্যাডমিন প্যানেলে প্রবেশের পাসকোড দিন:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = adminPasscode,
                        onValueChange = { adminPasscode = it },
                        label = { Text("পাসকোড (ডিফল্ট: 123456)") },
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
                            val ok = viewModel.authRepository.verifyAdminPasscode(adminPasscode)
                            if (ok) {
                                showAdminDialog = false
                                adminPasscode = ""
                                viewModel.navigateTo(Screen.AdminDashboard)
                            } else {
                                adminError = "ভুল পাসকোড। সঠিক পাসকোড দিন।"
                            }
                        }
                    }
                ) {
                    Text("যাচাই করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontFamily = HindSiliguriFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}
