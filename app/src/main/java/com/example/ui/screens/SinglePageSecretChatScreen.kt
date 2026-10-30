package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.example.crypto.CryptoEngine
import com.example.localization.LanguageManager
import com.example.service.FloatingWindowService
import com.example.ui.theme.StegoBgLight
import com.example.ui.theme.StegoCardLight
import com.example.ui.theme.StegoGreenBadgeBg
import com.example.ui.theme.StegoGreenDot
import com.example.ui.theme.StegoGreenText
import com.example.ui.theme.StegoInputBg
import com.example.ui.theme.StegoPrimaryBlue
import com.example.ui.theme.StegoTealActive
import com.example.ui.theme.StegoTealCircleBg
import com.example.ui.theme.StegoTealText
import com.example.ui.theme.StegoTextMuted
import com.example.ui.theme.StegoTextSubtitle
import com.example.ui.theme.StegoTextTitle
import java.nio.charset.StandardCharsets

enum class MainTab { ENCODE, DECODE }
enum class CarrierCategory { EMOJI, LETTER, CUSTOM }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SinglePageSecretChatScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var currentTab by remember { mutableStateOf(MainTab.ENCODE) }

    // ==========================================
    // ENCODE STATE
    // ==========================================
    var inputMessage by remember { mutableStateOf("") }
    var carrierCategory by remember { mutableStateOf(CarrierCategory.EMOJI) }
    var selectedCarrier by remember { mutableStateOf("🤔") }
    var customCarrierInput by remember { mutableStateOf("") }

    val messageBytes = remember(inputMessage) {
        inputMessage.toByteArray(StandardCharsets.UTF_8).size
    }

    val encodedStegoResult = remember(inputMessage, selectedCarrier) {
        if (inputMessage.isEmpty()) ""
        else CryptoEngine.encodeSteganography(selectedCarrier, inputMessage)
    }

    // ==========================================
    // DECODE STATE
    // ==========================================
    var decodeInputText by remember { mutableStateOf("") }
    var decodedResultText by remember { mutableStateOf("") }
    var decodeStatusBadge by remember { mutableStateOf<String?>(null) }
    var isDecodeSuccess by remember { mutableStateOf(false) }

    fun runDecode(text: String) {
        decodeInputText = text
        if (text.isEmpty()) {
            decodedResultText = ""
            decodeStatusBadge = null
            isDecodeSuccess = false
            return
        }

        val res = CryptoEngine.decodeSteganography(text)
        if (res.isSuccess) {
            val content = res.getOrNull() ?: ""
            decodedResultText = content
            val charCount = content.length
            decodeStatusBadge = LanguageManager.format("found_characters", charCount)
            isDecodeSuccess = true
        } else {
            // Fallback: check if it's emoji/aes/char encoded
            val detected = CryptoEngine.detectFormat(text)
            if (detected != null) {
                when (detected) {
                    com.example.crypto.SecretFormat.EMOJI -> {
                        val emojiRes = CryptoEngine.decodeEmoji(text)
                        if (emojiRes.isSuccess) {
                            decodedResultText = emojiRes.getOrNull() ?: ""
                            decodeStatusBadge = LanguageManager.format("found_characters", decodedResultText.length)
                            isDecodeSuccess = true
                            return
                        }
                    }
                    com.example.crypto.SecretFormat.CHARACTER -> {
                        val charRes = CryptoEngine.decodeCharacter(text)
                        if (charRes.isSuccess) {
                            decodedResultText = charRes.getOrNull() ?: ""
                            decodeStatusBadge = LanguageManager.format("found_characters", decodedResultText.length)
                            isDecodeSuccess = true
                            return
                        }
                    }
                    else -> {}
                }
            }
            decodedResultText = ""
            decodeStatusBadge = LanguageManager.get("not_found")
            isDecodeSuccess = false
        }
    }

    val presetEmojis = remember {
        listOf(
            "😀", "😉", "🙂", "😎", "🥰", "😂", "🤔", "👍", "🤝",
            "🔥", "✨", "⭐", "🌟", "🌙", "☀️", "🌈", "🍀", "🌸",
            "🍌", "🐒", "🐙", "🐺", "👻", "💀", "🎃", "🥚", "🥁",
            "🎁", "👑", "🚀", "💡", "🔮", "🔒", "🖼️", "📌", "🎯",
            "❤️", "💖", "💎", "⚡"
        )
    }

    val presetLetters = remember {
        ('a'..'z').map { it.toString() }
    }

    Column(modifier = Modifier.fillMaxSize().background(StegoBgLight)) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==========================================
        // APP HEADER BAR (Icon, Title, Floating Overlay, Language Toggle)
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.logo_zonex_emoji),
                        contentDescription = "Zonex Emoji Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = LanguageManager.get("app_title"),
                    color = StegoTextTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Floating Window Quick Toggle
                IconButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(context, FloatingWindowService::class.java).apply {
                                action = FloatingWindowService.ACTION_START
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            Toast.makeText(context, LanguageManager.get("launch_floating"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StegoCardLight)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                        .testTag("btn_floating_overlay")
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Floating Window",
                        tint = StegoTealActive,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Bilingual Toggle [ বাং | EN ]
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = StegoCardLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { LanguageManager.toggleLanguage() }
                        .testTag("btn_lang_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "বাং",
                            color = if (LanguageManager.isBengali()) StegoPrimaryBlue else StegoTextMuted,
                            fontWeight = if (LanguageManager.isBengali()) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                        Text(text = "|", color = StegoTextMuted, fontSize = 11.sp)
                        Text(
                            text = "EN",
                            color = if (!LanguageManager.isBengali()) StegoPrimaryBlue else StegoTextMuted,
                            fontWeight = if (!LanguageManager.isBengali()) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // ==========================================
        // HERO TITLE & SUBTITLE
        // ==========================================
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = LanguageManager.get("main_heading"),
                color = StegoTextTitle,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 32.sp
            )
            Text(
                text = LanguageManager.get("main_subtitle"),
                color = StegoTextSubtitle,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }

        // ==========================================
        // MAIN CARD CONTAINER (ENCODE / DECODE)
        // ==========================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = StegoCardLight),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Segmented Tabs: [ এনকোড ] | [ ডিকোড ]
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // ENCODE TAB
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (currentTab == MainTab.ENCODE) Color.White
                                    else Color.Transparent
                                )
                                .clickable { currentTab = MainTab.ENCODE }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = LanguageManager.get("tab_encode"),
                                color = if (currentTab == MainTab.ENCODE) StegoTextTitle else StegoTextSubtitle,
                                fontWeight = if (currentTab == MainTab.ENCODE) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }

                        // DECODE TAB
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (currentTab == MainTab.DECODE) Color.White
                                    else Color.Transparent
                                )
                                .clickable { currentTab = MainTab.DECODE }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = LanguageManager.get("tab_decode"),
                                color = if (currentTab == MainTab.DECODE) StegoTextTitle else StegoTextSubtitle,
                                fontWeight = if (currentTab == MainTab.DECODE) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Animated Tab Content (Smooth slide & fade transition)
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        if (targetState == MainTab.DECODE) {
                            (slideInHorizontally { width -> width / 3 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally { width -> -width / 3 } + fadeOut(tween(200)))
                        } else {
                            (slideInHorizontally { width -> -width / 3 } + fadeIn(tween(260))) togetherWith
                            (slideOutHorizontally { width -> width / 3 } + fadeOut(tween(200)))
                        }
                    },
                    label = "tab_smooth_transition"
                ) { tab ->
                    if (tab == MainTab.ENCODE) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // STEP 1: Write Message
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StepNumberBadge(number = "1")
                                Text(
                                    text = LanguageManager.get("step_write_msg"),
                                    color = StegoTextTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "$messageBytes B",
                                color = StegoTextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        OutlinedTextField(
                            value = inputMessage,
                            onValueChange = { inputMessage = it },
                            placeholder = {
                                Text(
                                    text = LanguageManager.get("input_hint"),
                                    color = StegoTextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("input_secret_message"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = StegoInputBg,
                                unfocusedContainerColor = StegoInputBg,
                                focusedBorderColor = StegoTealActive,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    // STEP 2: Select Carrier
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StepNumberBadge(number = "2")
                            Text(
                                text = LanguageManager.get("step_select_carrier"),
                                color = StegoTextTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Filter Pills: [ইমোজি] [অক্ষর] [নিজের মতো ইমুজি]
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoryFilterPill(
                                title = LanguageManager.get("carrier_emoji"),
                                isSelected = carrierCategory == CarrierCategory.EMOJI,
                                onClick = { carrierCategory = CarrierCategory.EMOJI }
                            )
                            CategoryFilterPill(
                                title = LanguageManager.get("carrier_char"),
                                isSelected = carrierCategory == CarrierCategory.LETTER,
                                onClick = { carrierCategory = CarrierCategory.LETTER }
                            )
                            CategoryFilterPill(
                                title = LanguageManager.get("carrier_custom"),
                                isSelected = carrierCategory == CarrierCategory.CUSTOM,
                                onClick = { carrierCategory = CarrierCategory.CUSTOM }
                            )
                        }

                        // Grid of choices based on selected category
                        when (carrierCategory) {
                            CarrierCategory.EMOJI -> {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    presetEmojis.forEach { emoji ->
                                        CarrierItemBox(
                                            symbol = emoji,
                                            isSelected = selectedCarrier == emoji,
                                            onClick = { selectedCarrier = emoji }
                                        )
                                    }
                                }
                            }
                            CarrierCategory.LETTER -> {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    presetLetters.forEach { letter ->
                                        CarrierItemBox(
                                            symbol = letter,
                                            isSelected = selectedCarrier == letter,
                                            onClick = { selectedCarrier = letter }
                                        )
                                    }
                                }
                            }
                            CarrierCategory.CUSTOM -> {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = customCarrierInput,
                                        onValueChange = {
                                            customCarrierInput = it
                                            if (it.isNotEmpty()) {
                                                selectedCarrier = it
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                text = LanguageManager.get("custom_emoji_hint"),
                                                color = StegoTextMuted,
                                                fontSize = 13.sp
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("input_custom_carrier"),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = StegoInputBg,
                                            unfocusedContainerColor = StegoInputBg,
                                            focusedBorderColor = StegoTealActive,
                                            unfocusedBorderColor = Color(0xFFE2E8F0)
                                        )
                                    )
                                    Text(
                                        text = "বর্তমান বাহক: $selectedCarrier",
                                        color = StegoTealActive,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // STEP 3: Result
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StepNumberBadge(number = "3")
                            Text(
                                text = LanguageManager.get("step_result"),
                                color = StegoTextTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Summary Card (Carrier preview on left, Ready info on right)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = StegoInputBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = selectedCarrier, fontSize = 28.sp)
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = LanguageManager.get("ready_to_send"),
                                        color = StegoTextTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val totalChars = if (inputMessage.isEmpty()) 1 else (1 + inputMessage.toByteArray(StandardCharsets.UTF_8).size * 8 + 2)
                                    Text(
                                        text = LanguageManager.format("byte_summary", messageBytes, totalChars),
                                        color = StegoTextSubtitle,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Single Emoji Output Preview
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StegoInputBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = if (encodedStegoResult.isEmpty()) selectedCarrier else encodedStegoResult,
                                    color = StegoTextTitle,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        // Full Width Copy Button
                        Button(
                            onClick = {
                                if (inputMessage.trim().isEmpty()) {
                                    Toast.makeText(context, LanguageManager.get("empty_input_warning"), Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Secret Message", encodedStegoResult)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, LanguageManager.get("copied_toast"), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_copy_encoded_result"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StegoPrimaryBlue)
                        ) {
                            Text(
                                text = LanguageManager.get("btn_copy_result"),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Disclaimer Note
                        Text(
                            text = LanguageManager.get("disclaimer_text"),
                            color = StegoTextSubtitle,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    } // End Step 3 Column
                } // End Encode Parent Column
            } else {
                // ==========================================
                // DECODE VIEW
                // ==========================================
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // STEP 1: Paste Encoded Text
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StepNumberBadge(number = "1")
                                Text(
                                    text = LanguageManager.get("step_paste_encoded"),
                                    color = StegoTextTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Quick Paste Button on top right
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        runDecode(text)
                                        Toast.makeText(context, LanguageManager.get("pasted_toast"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(
                                    text = LanguageManager.get("btn_paste"),
                                    color = StegoPrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        OutlinedTextField(
                            value = decodeInputText,
                            onValueChange = { runDecode(it) },
                            placeholder = {
                                Text(
                                    text = "ইমোজি বা কোড এখানে পেস্ট করুন...",
                                    color = StegoTextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .testTag("decode_input_area"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = StegoInputBg,
                                unfocusedContainerColor = StegoInputBg,
                                focusedBorderColor = StegoTealActive,
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )

                        // Clear Button
                        Button(
                            onClick = {
                                decodeInputText = ""
                                decodedResultText = ""
                                decodeStatusBadge = null
                                isDecodeSuccess = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9))
                        ) {
                            Text(
                                text = LanguageManager.get("btn_clear"),
                                color = StegoTextTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // STEP 2: Decoded Message Output
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StepNumberBadge(number = "2")
                                Text(
                                    text = LanguageManager.get("step_decoded_msg"),
                                    color = StegoTextTitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Status badge
                            if (decodeStatusBadge != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDecodeSuccess) StegoGreenBadgeBg else Color(0xFFFFE4E6))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = decodeStatusBadge ?: "",
                                        color = if (isDecodeSuccess) StegoGreenText else Color(0xFFE11D48),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Decoded text result area
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StegoInputBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = if (decodedResultText.isEmpty()) "কোনো তথ্য পাওয়া যায়নি..." else decodedResultText,
                                    color = if (decodedResultText.isEmpty()) StegoTextMuted else StegoTextTitle,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // Copy Decoded Message Button
                        Button(
                            onClick = {
                                if (decodedResultText.isEmpty()) return@Button
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Decoded Secret", decodedResultText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, LanguageManager.get("copied_toast"), Toast.LENGTH_SHORT).show()
                            },
                            enabled = decodedResultText.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_copy_decoded_message"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StegoPrimaryBlue)
                        ) {
                            Text(
                                text = LanguageManager.get("btn_copy_message"),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } // End Step 2 Column
                } // End Decode Parent Column
            } // End if/else
        } // End AnimatedContent
    } // End Card Column
} // End Card

        // ==========================================
        // FOOTER OFFLINE GUARANTEE
        // ==========================================
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(StegoGreenDot)
            )
            Text(
                text = LanguageManager.get("footer_offline"),
                color = StegoTextSubtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    // ==========================================
    // ADMOB BANNER (pinned at bottom)
    // ==========================================
    BannerAd()

    // ==========================================
    // CONTACT FOOTER (below banner)
    // ==========================================
    Text(
        text = "যেকোনো ধরনের এ্যাপ/ওয়েবসাইট বানাতে যোগাযোগ করুন\n" +
            "📞 +8801867052188, +880 1610-869705\n" +
            "✉️ tajulislamashraful@gmail.com\n" +
            "📲 t.me/Zonex_Owner",
        color = StegoTextSubtitle,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        lineHeight = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(StegoBgLight)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
    }
}

private const val BANNER_AD_UNIT_ID = "ca-app-pub-3724977789874593/1420531406"

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
                try {
                    loadAd(AdRequest.Builder().build())
                } catch (_: Exception) {
                    // Ignore in test or headless environments
                }
            }
        }
    )
}

@Composable
fun StepNumberBadge(number: String) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(StegoTealCircleBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = StegoTealText,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun CategoryFilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) StegoTealActive else Color(0xFFF1F5F9))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else StegoTextSubtitle,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun CarrierItemBox(
    symbol: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFFF0FDFA) else StegoInputBg)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) StegoTealActive else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 20.sp
        )
    }
}
