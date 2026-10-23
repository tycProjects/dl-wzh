package com.example.ui.screens

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LaunchableApp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberNavyCard
import com.example.ui.theme.CyberNavyDark
import com.example.ui.theme.CyberNavySurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.viewmodel.MainViewModel

@Composable
fun ToolsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedToolTab by remember { mutableIntStateOf(0) }
    val toolTabs = listOf("App Launcher", "ChatGPT Suite", "Screen Assist", "Quick Connect")

    val installedApps by viewModel.installedApps.collectAsState()
    val isScreenActive by viewModel.screenSessionActive.collectAsState()
    val generatedPrompt by viewModel.generatedPrompt.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Confirmation dialog state
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmTitle by remember { mutableStateOf("") }
    var confirmMessage by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<() -> Unit>({}) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberNavyDark)
    ) {
        // --- Top Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberNavySurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_arafat_logo),
                contentDescription = "ARAFAT AI Logo",
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, NeonCyan, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ARAFAT AI TOOLS & INTEGRATIONS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyanBright
                )
                Text(
                    text = "App Launcher • ChatGPT • Screen • Comms",
                    fontSize = 11.sp,
                    color = Color(0xFF7E93AC)
                )
            }
        }

        // --- Tabs ---
        ScrollableTabRow(
            selectedTabIndex = selectedToolTab,
            containerColor = CyberNavySurface,
            contentColor = NeonCyan,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedToolTab]),
                    color = NeonCyan
                )
            }
        ) {
            toolTabs.forEachIndexed { index, tabName ->
                Tab(
                    selected = selectedToolTab == index,
                    onClick = { selectedToolTab = index },
                    text = {
                        Text(
                            text = tabName,
                            fontSize = 12.sp,
                            fontWeight = if (selectedToolTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedToolTab == index) NeonCyan else Color(0xFF8A9FB8)
                        )
                    }
                )
            }
        }

        // --- Tab Contents ---
        when (selectedToolTab) {
            0 -> AppLauncherTab(
                installedApps = installedApps,
                onLaunchApp = { pkg -> viewModel.systemActionHelper.launchAppByPackage(pkg) }
            )
            1 -> ChatGptSuiteTab(
                viewModel = viewModel,
                generatedPrompt = generatedPrompt,
                isLoading = isLoading,
                onLaunchChatGpt = {
                    val status = viewModel.systemActionHelper.openChatGPT()
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                }
            )
            2 -> ScreenAssistTab(
                isActive = isScreenActive,
                onStartSession = { viewModel.startScreenSession() },
                onStopSession = { viewModel.stopScreenSession() },
                onAnalyzeScreen = {
                    viewModel.analyzeScreenCapture("Active Android Home/App view with UI controls and cards.")
                    Toast.makeText(context, "Analyzing screen with ARAFAT AI...", Toast.LENGTH_SHORT).show()
                }
            )
            3 -> QuickConnectTab(
                onSendWhatsApp = { phone, msg ->
                    confirmTitle = "Send WhatsApp Message"
                    confirmMessage = "Send to $phone:\n\n\"$msg\"\n\nConfirm to open WhatsApp?"
                    confirmAction = { viewModel.systemActionHelper.prepareWhatsAppMessage(phone, msg) }
                    showConfirmDialog = true
                },
                onSendSms = { phone, msg ->
                    confirmTitle = "Send SMS Message"
                    confirmMessage = "Open SMS composer for $phone with message:\n\n\"$msg\"?"
                    confirmAction = { viewModel.systemActionHelper.prepareSmsMessage(phone, msg) }
                    showConfirmDialog = true
                },
                onDialNumber = { phone ->
                    confirmTitle = "Phone Call"
                    confirmMessage = "Open phone dialer for number: $phone?"
                    confirmAction = { viewModel.systemActionHelper.openDialer(phone) }
                    showConfirmDialog = true
                }
            )
        }
    }

    // Explicit Confirmation Dialog for External Communication
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = confirmTitle,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = confirmMessage,
                    color = Color.White,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        confirmAction()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberNavyDark)
                ) {
                    Text("Confirm & Launch", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF8A9FB8))
                }
            },
            containerColor = CyberNavyCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun AppLauncherTab(
    installedApps: List<LaunchableApp>,
    onLaunchApp: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Quick System Shortcuts
        Text(
            text = "QUICK SHORTCUTS",
            fontSize = 11.sp,
            color = Color(0xFF6B809E),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickLaunchBadge("YouTube", Icons.Default.VideoLibrary, NeonRed) {
                onLaunchApp("com.google.android.youtube")
            }
            QuickLaunchBadge("WhatsApp", Icons.Default.Chat, NeonGreen) {
                onLaunchApp("com.whatsapp")
            }
            QuickLaunchBadge("ChatGPT", Icons.Default.AutoAwesome, NeonCyan) {
                onLaunchApp("com.openai.chatgpt")
            }
            QuickLaunchBadge("Settings", Icons.Default.Settings, ElectricBlueGlow) {
                onLaunchApp("android.settings")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed applications...", fontSize = 13.sp, color = Color(0xFF6B809E)) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = NeonCyan)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = CyberNavyCard,
                unfocusedContainerColor = CyberNavyCard
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_search_input"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "ALL LAUNCHABLE APPS (${filteredApps.size})",
            fontSize = 11.sp,
            color = Color(0xFF6B809E),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredApps) { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLaunchApp(app.packageName) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = app.packageName,
                                fontSize = 10.sp,
                                color = Color(0xFF7E93AC)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Launch",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickLaunchBadge(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ChatGptSuiteTab(
    viewModel: MainViewModel,
    generatedPrompt: String,
    isLoading: Boolean,
    onLaunchChatGpt: () -> Unit
) {
    val context = LocalContext.current
    var promptTargetType by remember { mutableStateOf("Android Kotlin Application") }
    var promptDescription by remember { mutableStateOf("") }
    var selectedTargetAi by remember { mutableStateOf("ChatGPT / OpenAI") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Option A: Open ChatGPT
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "1. OPEN CHATGPT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Text(
                        text = "Launches ChatGPT app if installed, or official web.",
                        fontSize = 11.sp,
                        color = Color(0xFFBACCDD)
                    )
                }
                Button(
                    onClick = onLaunchChatGpt,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = CyberNavyDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Launch", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Option B: ChatGPT Prompt Generator
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "2. CHATGPT SUPER PROMPT GENERATOR",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "Crafts in-depth master prompts for apps, websites, games & tools.",
                    fontSize = 11.sp,
                    color = Color(0xFFBACCDD)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = promptTargetType,
                    onValueChange = { promptTargetType = it },
                    label = { Text("What are you building? (e.g. Android App, Web App, Game)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = promptDescription,
                    onValueChange = { promptDescription = it },
                    label = { Text("Describe the app features and rules:") },
                    placeholder = { Text("e.g. A futuristic crypto wallet with cyber neon UI and biometrics") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.generateChatGptPrompt(promptTargetType, promptDescription, selectedTargetAi)
                    },
                    enabled = !isLoading && promptDescription.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberNavyDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberNavyDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generating Super Prompt...")
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Gen", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GENERATE MASTER PROMPT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (generatedPrompt.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GENERATED MASTER PROMPT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonPurple)
                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Prompt", generatedPrompt))
                                    Toast.makeText(context, "Prompt copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, generatedPrompt)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Prompt"))
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = ElectricBlueGlow, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = generatedPrompt,
                            fontSize = 12.sp,
                            color = Color(0xFFBACCDD),
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF070C16), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ScreenAssistTab(
    isActive: Boolean,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onAnalyzeScreen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) NeonGreen else CyberBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.ScreenShare else Icons.Default.StopScreenShare,
                        contentDescription = "Screen Assist",
                        tint = if (isActive) NeonGreen else Color(0xFF7E93AC),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isActive) "SCREEN SESSION ACTIVE" else "AI SCREEN ASSISTANCE",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) NeonGreen else Color.White
                        )
                        Text(
                            text = if (isActive) "Consent granted. Assistant is ready to inspect screen." else "Consent-based inspection using Android MediaProjection",
                            fontSize = 11.sp,
                            color = Color(0xFF8A9FB8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "ARAFAT AI respects your privacy. Screen capture only takes place upon explicit session start and does not retain sensitive password fields or bypass Android secure flag protections.",
                    fontSize = 11.sp,
                    color = Color(0xFF9FB2C8),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!isActive) {
                    Button(
                        onClick = onStartSession,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.ScreenShare, contentDescription = "Start")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("START SCREEN ASSISTANCE SESSION", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onAnalyzeScreen,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberNavyDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Analyze Screen Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onStopSession,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Stop Session", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickConnectTab(
    onSendWhatsApp: (phone: String, msg: String) -> Unit,
    onSendSms: (phone: String, msg: String) -> Unit,
    onDialNumber: (phone: String) -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "COMMUNICATION ASSISTANT",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "Prepare verified messages or phone calls with explicit confirmation.",
                    fontSize = 11.sp,
                    color = Color(0xFFBACCDD)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number (e.g. +8801... or +1...)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Message Body:") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSendWhatsApp(phoneInput, messageInput) },
                        enabled = messageInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = CyberNavyDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSendSms(phoneInput, messageInput) },
                        enabled = phoneInput.isNotBlank() && messageInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Message, contentDescription = "SMS", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onDialNumber(phoneInput) },
                        enabled = phoneInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberNavyDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = "Dialer", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
