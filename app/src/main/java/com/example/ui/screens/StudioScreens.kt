package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.CreationItem
import com.example.data.model.PreviewType
import com.example.data.model.StudioMode
import com.example.ui.components.InteractiveSandboxDialog
import com.example.ui.components.StoryboardList
import com.example.ui.components.StudioHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudioUiState
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun StudioHomeScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filteredCreations = remember(uiState.creations, uiState.selectedMode, uiState.searchQuery) {
        uiState.creations.filter { item ->
            val matchesMode = if (uiState.selectedMode == StudioMode.ONE_TOUCH) true else item.mode == uiState.selectedMode
            val matchesQuery = if (uiState.searchQuery.isBlank()) true else {
                item.title.contains(uiState.searchQuery, ignoreCase = true) ||
                        item.summary.contains(uiState.searchQuery, ignoreCase = true)
            }
            matchesMode && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("studio_home_scaffold"),
        containerColor = StudioDarkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeroStudioBanner(onExploreOneTouch = { viewModel.openOneTouchModal() })
            }

            item {
                StudioHeader(
                    isGeminiActive = uiState.isGeminiConnected,
                    onOpenSettings = { viewModel.openSettings() },
                    onOpenOneTouch = { viewModel.openOneTouchModal() }
                )
            }

            item {
                StudioModeSelector(
                    selectedMode = uiState.selectedMode,
                    onSelectMode = { viewModel.selectMode(it) }
                )
            }

            item {
                StudioPromptBox(
                    mode = uiState.selectedMode,
                    prompt = uiState.promptInput,
                    onPromptChange = { viewModel.setPrompt(it) },
                    selectedStyle = uiState.selectedStyle,
                    onStyleChange = { viewModel.setStyle(it) },
                    isGenerating = uiState.isGenerating,
                    progressMessage = uiState.generationProgressMessage,
                    onGenerate = { viewModel.generateCurrentPrompt() }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المشاريع والإبداعات (${filteredCreations.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = if (uiState.selectedMode == StudioMode.ONE_TOUCH) "جميع الفئات" else uiState.selectedMode.titleAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan
                    )
                }
            }

            if (filteredCreations.isEmpty()) {
                item {
                    EmptyCreationsCard(
                        mode = uiState.selectedMode,
                        onQuickCreate = {
                            viewModel.setPrompt("أنشئ فكرة رائعة وجديدة")
                            viewModel.generateCurrentPrompt()
                        }
                    )
                }
            } else {
                items(filteredCreations, key = { it.id }) { item ->
                    CreationCard(
                        item = item,
                        onOpen = { viewModel.openSandbox(item) },
                        onToggleFavorite = { viewModel.toggleFavorite(item.id) },
                        onDelete = { viewModel.deleteItem(item.id) },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText(item.title, item.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ الكود/المحتوى بنجاح!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Interactive Sandbox Dialog for Games and Live Preview
    if (uiState.activeSandboxItem != null) {
        InteractiveSandboxDialog(
            item = uiState.activeSandboxItem!!,
            onDismiss = { viewModel.closeSandbox() }
        )
    }

    // One Touch Generator Modal
    if (uiState.isOneTouchModalOpen) {
        OneTouchDialog(
            onDismiss = { viewModel.closeOneTouchModal() },
            onExecute = { idea -> viewModel.executeOneTouchIdea(idea) }
        )
    }

    // Settings & About Modal
    if (uiState.isSettingsOpen) {
        SettingsDialog(
            isGeminiConnected = uiState.isGeminiConnected,
            onDismiss = { viewModel.closeSettings() }
        )
    }
}

@Composable
fun HeroStudioBanner(
    onExploreOneTouch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .testTag("hero_banner_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.omni_studio_hero_1792332985126),
                contentDescription = "Studio Hero",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                StudioDarkBg.copy(alpha = 0.85f),
                                StudioDarkBg
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonIndigo.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "GEMINI 3.5 CREATIVE ENGINE",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "حوّل أي فكرة إلى تطبيق، لعبة، أو موقع تفاعلي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "صمّم محتوى سينمائي وأكواد جاهزة للتشغيل بلمسة واحدة",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun StudioModeSelector(
    selectedMode: StudioMode,
    onSelectMode: (StudioMode) -> Unit
) {
    val modes = listOf(
        StudioMode.ONE_TOUCH,
        StudioMode.GAMES,
        StudioMode.APPS,
        StudioMode.WEBSITES,
        StudioMode.IMAGES,
        StudioMode.VIDEOS,
        StudioMode.CONTENT
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(modes) { mode ->
            val isSelected = mode == selectedMode
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectMode(mode) }
                    .testTag("mode_chip_${mode.name}"),
                color = if (isSelected) NeonIndigo else StudioSurfaceVariant,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        if (isSelected) listOf(NeonIndigo, NeonCyan) else listOf(StudioCardBorder, StudioCardBorder)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val icon = when (mode) {
                        StudioMode.ONE_TOUCH -> Icons.Default.AutoAwesome
                        StudioMode.GAMES -> Icons.Default.SportsEsports
                        StudioMode.APPS -> Icons.Default.Smartphone
                        StudioMode.WEBSITES -> Icons.Default.Language
                        StudioMode.IMAGES -> Icons.Default.Palette
                        StudioMode.VIDEOS -> Icons.Default.MovieFilter
                        StudioMode.CONTENT -> Icons.Default.Description
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) Color.White else TextSecondary
                    )

                    Text(
                        text = mode.titleAr,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun StudioPromptBox(
    mode: StudioMode,
    prompt: String,
    onPromptChange: (String) -> Unit,
    selectedStyle: String,
    onStyleChange: (String) -> Unit,
    isGenerating: Boolean,
    progressMessage: String,
    onGenerate: () -> Unit
) {
    val placeholder = when (mode) {
        StudioMode.ONE_TOUCH -> "اكتب فكرة عامة (مثال: منصة ألعاب فضاء سيبرانية)..."
        StudioMode.GAMES -> "اكتب فكرة اللعبة (مثال: لعبة قتال طائرات نيون كانفاس باللمس)..."
        StudioMode.APPS -> "اكتب فكرة التطبيق (مثال: تطبيق متابعة العادات الذكي بكوتلن)..."
        StudioMode.WEBSITES -> "اكتب فكرة الموقع (مثال: صفحة هبوط لشركة ذكاء اصطناعي)..."
        StudioMode.IMAGES -> "اكتب وصف المشهد الفني والتصميم المراد هندسة برومبت له..."
        StudioMode.VIDEOS -> "اكتب موضوع الفيديو والسيناريو والرسالة المراد إيصالها..."
        StudioMode.CONTENT -> "اكتب تفاصيل المحتوى التسويقي أو المقال أو الشعار..."
    }

    val stylePresets = when (mode) {
        StudioMode.GAMES -> listOf("أركيد نيون", "فضائي", "ألغاز", "ريترو 8-بت", "كلاسيكي")
        StudioMode.WEBSITES -> listOf("مودرن دارك", "زجاجي Glassmorphic", "مينيمال", "سايبربانك")
        StudioMode.APPS -> listOf("Compose M3", "داكن حديث", "بطاقات تفاعلية", "كلين أركيتكتشر")
        StudioMode.IMAGES -> listOf("Cyberpunk 8K", "Octane 3D", "Anime Masterpiece", "Photorealistic")
        StudioMode.VIDEOS -> listOf("سينمائي مشوق", "وثائقي ملهم", "إعلاني سريع", "خيال علمي")
        StudioMode.CONTENT -> listOf("تسويقي جذاب", "استراتيجي", "إلهامي", "تقني مبسط")
        StudioMode.ONE_TOUCH -> listOf("حزمة شاملة", "إطلاق منتج", "عالم ألعاب متكامل")
    }

    val quickPrompts = when (mode) {
        StudioMode.GAMES -> listOf("حرب الفضاء النيون", "ثعبان النيون التفاعلي", "مغامرة المتاهة الذكية")
        StudioMode.APPS -> listOf("تطبيق تتبع المصاريف", "مساعد اللياقة البدنية", "مفكرة الأفكار السريعة")
        StudioMode.WEBSITES -> listOf("منصة سحابية للذكاء الاصطناعي", "متجر إلكتروني للهواتف", "معرض أعمال فني")
        StudioMode.ONE_TOUCH -> listOf("لعبة وتطبيق سباق سيارات فضائي", "منصة صحية ذكية للمدربين", "استوديو بودكاست صوتي")
        else -> listOf("مشروع سايبربانك مستقبلي", "أداة إنتاجية خارقة", "فيديو ترويجي مذهل")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("studio_prompt_box"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(StudioCardBorder, NeonIndigo.copy(alpha = 0.5f))))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "صندوق التوليد الإبداعي: ${mode.titleAr}",
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )

                if (prompt.isNotBlank()) {
                    TextButton(onClick = { onPromptChange("") }) {
                        Text("مسح", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                placeholder = { Text(placeholder, color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prompt_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonIndigo,
                    unfocusedBorderColor = StudioCardBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = StudioDarkBg,
                    unfocusedContainerColor = StudioDarkBg
                ),
                shape = RoundedCornerShape(14.dp),
                maxLines = 4,
                minLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick suggestion chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPrompts.forEach { qp ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPromptChange(qp) },
                        color = StudioSurfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "+ $qp",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Styles
            Text(
                text = "النمط الإبداعي:",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stylePresets.forEach { style ->
                    val isStyleSelected = style == selectedStyle
                    FilterChip(
                        selected = isStyleSelected,
                        onClick = { onStyleChange(style) },
                        label = { Text(style, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isGenerating) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = NeonIndigo,
                        trackColor = StudioCardBorder
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = progressMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            } else {
                Button(
                    onClick = onGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_action_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo)
                ) {
                    Icon(
                        imageVector = if (mode == StudioMode.ONE_TOUCH) Icons.Default.AutoAwesome else Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (mode == StudioMode.ONE_TOUCH) "إنشاء الحزمة الشاملة بلمسة واحدة" else "توليد ${mode.titleAr} الآن",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CreationCard(
    item: CreationItem,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("creation_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(StudioCardBorder, StudioCardBorder.copy(alpha = 0.3f))))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modeColor = when (item.mode) {
                        StudioMode.GAMES -> NeonPink
                        StudioMode.APPS -> NeonIndigo
                        StudioMode.WEBSITES -> NeonCyan
                        StudioMode.IMAGES -> NeonEmerald
                        StudioMode.VIDEOS -> NeonAmber
                        else -> Color(0xFFA855F7)
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(modeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (item.mode) {
                                StudioMode.GAMES -> Icons.Default.SportsEsports
                                StudioMode.APPS -> Icons.Default.Smartphone
                                StudioMode.WEBSITES -> Icons.Default.Language
                                StudioMode.IMAGES -> Icons.Default.Palette
                                StudioMode.VIDEOS -> Icons.Default.MovieFilter
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = modeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = item.mode.titleAr,
                        style = MaterialTheme.typography.labelSmall,
                        color = modeColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (item.isFavorite) NeonPink else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Storyboard Preview if present
            if (item.storyboards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                StoryboardList(scenes = item.storyboards.take(2))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive action button
                Button(
                    onClick = onOpen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.previewType == PreviewType.HTML_RUNNABLE) NeonCyan else StudioSurfaceVariant
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = when (item.previewType) {
                            PreviewType.HTML_RUNNABLE -> Icons.Default.PlayArrow
                            PreviewType.KOTLIN_CODE -> Icons.Default.Code
                            PreviewType.STORYBOARD -> Icons.Default.Visibility
                            else -> Icons.Default.Visibility
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (item.previewType == PreviewType.HTML_RUNNABLE) StudioDarkBg else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (item.previewType) {
                            PreviewType.HTML_RUNNABLE -> if (item.mode == StudioMode.GAMES) "العب اللعبة مباشرة 🎮" else "معاينة الموقع حياً 🌐"
                            PreviewType.KOTLIN_CODE -> "فحص كود التطبيق 💻"
                            PreviewType.STORYBOARD -> "عرض السيناريو الكامل 🎬"
                            PreviewType.IMAGE_MOCKUP -> "تفاصيل البرومبت الفني 🎨"
                            else -> "عرض المحتوى"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.previewType == PreviewType.HTML_RUNNABLE) StudioDarkBg else Color.White
                    )
                }

                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCreationsCard(
    mode: StudioMode,
    onQuickCreate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = NeonAmber,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "لا توجد مشاريع سابقة في هذا القسم",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "اكتب فكرتك في الصندوق بالأعلى واضغط توليد لبدء الابتكار!",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onQuickCreate,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("توليد فكرة نموذجية سريعة ⚡", fontSize = 12.sp, color = NeonCyan)
            }
        }
    }
}

@Composable
fun OneTouchDialog(
    onDismiss: () -> Unit,
    onExecute: (String) -> Unit
) {
    var ideaText by remember { mutableStateOf("") }
    val presetIdeas = listOf(
        "لعبة سباق سيارات نيون في الفضاء",
        "تطبيق صحة وتأمل مدعوم بالذكاء الاصطناعي",
        "منصة تجارة إلكترونية ذكية للعطور الفاخرة",
        "إعلان سينمائي خيال علمي لمسبار يستكشف المريخ"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = StudioSurface,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NeonIndigo, NeonPink)))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = NeonPink)
                        Text(
                            text = "لمسة إبداعية واحدة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Text(
                    text = "أدخل فكرة واحدة، وسيقوم الاستوديو بتوليد تطبيق أندرويد، لعبة تفاعلية، موقع ويب، برومبت صورة، وسيناريو فيديو في آن واحد!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = ideaText,
                    onValueChange = { ideaText = it },
                    placeholder = { Text("مثال: لعبة سباق دراجات نيون...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("one_touch_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = StudioDarkBg,
                        unfocusedContainerColor = StudioDarkBg,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("أفكار سريعة جاهزة:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                presetIdeas.forEach { idea ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { ideaText = idea },
                        color = StudioSurfaceVariant
                    ) {
                        Text(
                            text = "⚡ $idea",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val finalIdea = ideaText.ifBlank { "مشروع ذكاء اصطناعي سيبراني متكامل" }
                        onExecute(finalIdea)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("execute_one_touch_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انطلق باللمسة الواحدة ✨", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(
    isGeminiConnected: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonIndigo, NeonCyan)))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إعدادات OmniStudio AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Connection status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = StudioDarkBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isGeminiConnected) NeonEmerald else NeonAmber)
                            )
                            Text(
                                text = if (isGeminiConnected) "Gemini API متصل ويعمل" else "الوضع التفاعلي المباشر (Offline Sandbox Ready)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isGeminiConnected) NeonEmerald else NeonAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isGeminiConnected)
                                "يتم توليد الأكواد والألعاب والنصوص مباشرة عبر نماذج Google Gemini الرسمية."
                            else
                                "يحتوي التطبيق على محرك استوديو متقدم يولّد ألعاباً تفاعلية كانفاس حقيقية ومواقع وأكواد بدون الحاجة لإنترنت فوري، ويمكنك ربط مفتاحك في لوحة Secrets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "المحركات المتاحة في الاستوديو:",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("• محرك الألعاب: HTML5 Canvas 2D + Touch Controls", color = TextPrimary, fontSize = 11.sp)
                Text("• محرك المواقع: Responsive Dark Mode + Live WebView", color = TextPrimary, fontSize = 11.sp)
                Text("• محرك التطبيقات: Jetpack Compose + Modern M3 Architecture", color = TextPrimary, fontSize = 11.sp)
                Text("• محرك الفيديو: Multi-Scene Cinematic Storyboard & Voiceover", color = TextPrimary, fontSize = 11.sp)
                Text("• محرك الصور: Master Prompt Engineering & 8K Visual Cues", color = TextPrimary, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo)
                ) {
                    Text("تم والعودة للاستوديو", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
