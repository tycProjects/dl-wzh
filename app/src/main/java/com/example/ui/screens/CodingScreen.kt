package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CodeSnippet
import com.example.model.CodeTaskType
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
fun CodingScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val savedSnippets by viewModel.savedSnippets.collectAsState()

    var selectedTaskIndex by remember { mutableIntStateOf(0) }
    val taskTypes = listOf(
        CodeTaskType.GENERATE,
        CodeTaskType.EXPLAIN,
        CodeTaskType.DEBUG,
        CodeTaskType.PROJECT_STRUCT,
        CodeTaskType.PROMPT_GEN
    )

    val languages = listOf("Kotlin", "Python", "JavaScript", "TypeScript", "HTML/CSS", "Java", "SQL", "C++", "Rust", "Go")
    var selectedLanguage by remember { mutableStateOf("Kotlin") }

    var codePromptInput by remember { mutableStateOf("") }
    var latestSnippet by remember { mutableStateOf<CodeSnippet?>(null) }

    val presetTemplates = listOf(
        "Jetpack Compose Cyber HUD Card",
        "Clean Architecture ViewModel + StateFlow",
        "FastAPI Python REST Endpoint with Auth",
        "Modern React 19 Custom Hook with TypeScript",
        "Room Database Entity & DAO Structure",
        "HTML5 / Tailwind Cyberpunk Landing Page"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberNavyDark)
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // --- Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "CODING ASSISTANT",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyanBright
                )
                Text(
                    text = "Ultra-Fast Code Gen, Debugging & Architecture",
                    fontSize = 11.sp,
                    color = Color(0xFF7E93AC)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Task Mode Tabs ---
        ScrollableTabRow(
            selectedTabIndex = selectedTaskIndex,
            containerColor = CyberNavySurface,
            contentColor = NeonCyan,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTaskIndex]),
                    color = NeonCyan
                )
            }
        ) {
            taskTypes.forEachIndexed { index, task ->
                Tab(
                    selected = selectedTaskIndex == index,
                    onClick = { selectedTaskIndex = index },
                    text = {
                        Text(
                            text = task.label,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTaskIndex == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTaskIndex == index) NeonCyan else Color(0xFF8A9FB8)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Language Selector Chips ---
        Text(
            text = "TARGET LANGUAGE",
            fontSize = 11.sp,
            color = Color(0xFF6B809E),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(languages) { lang ->
                FilterChip(
                    selected = selectedLanguage == lang,
                    onClick = { selectedLanguage = lang },
                    label = { Text(lang, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricBlue,
                        selectedLabelColor = Color.White,
                        containerColor = CyberNavyCard,
                        labelColor = Color(0xFF9FB2C8)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedLanguage == lang,
                        borderColor = if (selectedLanguage == lang) NeonCyan else CyberBorder
                    )
                )
            }
        }

        // --- Quick Preset Templates ---
        Text(
            text = "PRESET TEMPLATES (TAP TO LOAD)",
            fontSize = 11.sp,
            color = Color(0xFF6B809E),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presetTemplates) { template ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable {
                        codePromptInput = template
                    }
                ) {
                    Text(
                        text = template,
                        fontSize = 11.sp,
                        color = Color(0xFFC0D2E8),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- Code Input / Prompt Box ---
        val taskType = taskTypes[selectedTaskIndex]
        val hintText = when (taskType) {
            CodeTaskType.GENERATE -> "Describe what you want to build in $selectedLanguage..."
            CodeTaskType.EXPLAIN -> "Paste $selectedLanguage code to explain..."
            CodeTaskType.DEBUG -> "Paste buggy $selectedLanguage code to debug..."
            CodeTaskType.PROJECT_STRUCT -> "Describe the app to generate folder architecture..."
            CodeTaskType.PROMPT_GEN -> "Describe the system to generate master AI developer prompt..."
        }

        OutlinedTextField(
            value = codePromptInput,
            onValueChange = { codePromptInput = it },
            placeholder = { Text(hintText, fontSize = 13.sp, color = Color(0xFF6B809E)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("coding_input_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = CyberBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = CyberNavyCard,
                unfocusedContainerColor = CyberNavyCard
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // --- Execute Button ---
        Button(
            onClick = {
                viewModel.handleCodingTask(
                    taskType = taskType,
                    language = selectedLanguage,
                    codeOrDescription = codePromptInput
                ) { snippet ->
                    latestSnippet = snippet
                }
            },
            enabled = !isLoading && codePromptInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("execute_coding_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = CyberNavyDark
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = CyberNavyDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synthesizing Code...", fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Execute",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("RUN ${taskType.label.uppercase()}", fontWeight = FontWeight.ExtraBold)
            }
        }

        // --- Display Latest Result ---
        if (latestSnippet != null) {
            Spacer(modifier = Modifier.height(16.dp))
            CodeResultCard(
                snippet = latestSnippet!!,
                onCopy = { code ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                    Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                onShare = { code ->
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, code)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Code via"))
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Saved Snippets Section ---
        if (savedSnippets.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SAVED SNIPPETS (${savedSnippets.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyanBright
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            savedSnippets.forEach { snip ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = snip.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", snip.code))
                                        Toast.makeText(context, "Copied code", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = Color(0xFF8A9FB8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSnippet(snip.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = NeonRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        SelectionContainer {
                            Text(
                                text = snip.code.take(180) + if (snip.code.length > 180) "..." else "",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFBACCDD),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF070C16), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = CyberNavySurface,
    contentColor: Color = NeonCyan,
    edgePadding: androidx.compose.ui.unit.Dp = 0.dp,
    indicator: @Composable (tabPositions: List<androidx.compose.material3.TabPosition>) -> Unit = @Composable { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
            color = contentColor
        )
    },
    tabs: @Composable () -> Unit
) {
    androidx.compose.material3.ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        indicator = indicator,
        tabs = tabs
    )
}

@Composable
fun CodeResultCard(
    snippet: CodeSnippet,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberNavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = snippet.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row {
                    IconButton(
                        onClick = { onCopy(snippet.code) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onShare(snippet.code) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Code",
                            tint = ElectricBlueGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Code View
            SelectionContainer {
                Text(
                    text = snippet.code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = NeonCyanBright,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF060B14), RoundedCornerShape(8.dp))
                        .border(1.dp, CyberBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
            }

            if (snippet.explanation.isNotBlank() && snippet.explanation != snippet.code) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "EXPLANATION & ANALYSIS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7E93AC)
                )
                Spacer(modifier = Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = snippet.explanation,
                        fontSize = 12.sp,
                        color = Color(0xFFBACCDD),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
