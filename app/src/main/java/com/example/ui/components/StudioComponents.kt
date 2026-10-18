package com.example.ui.components

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CreationItem
import com.example.data.model.PreviewType
import com.example.data.model.StoryboardScene
import com.example.ui.theme.*

@Composable
fun StudioHeader(
    isGeminiActive: Boolean,
    onOpenSettings: () -> Unit,
    onOpenOneTouch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonIndigo, NeonCyan)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .background(
                                Brush.linearGradient(listOf(NeonIndigo, NeonCyan)),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Studio Icon",
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "OmniStudio AI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                brush = Brush.horizontalGradient(listOf(Color.White, NeonCyan))
                            )
                        )
                        Text(
                            text = "صانع التطبيقات، الألعاب، المواقع، والفيديو",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isGeminiActive) NeonEmerald.copy(alpha = 0.15f) else NeonAmber.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (isGeminiActive) listOf(NeonEmerald, NeonCyan) else listOf(NeonAmber, Color(0xFFD97706))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isGeminiActive) NeonEmerald else NeonAmber)
                        )
                        Text(
                            text = if (isGeminiActive) "Gemini 3.5 متصل" else "محرك الاستوديو التفاعلي جاهز",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isGeminiActive) NeonEmerald else NeonAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // One-touch launch button
                Button(
                    onClick = onOpenOneTouch,
                    modifier = Modifier.testTag("one_touch_banner_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPink
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "لمسة إبداعية واحدة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveSandboxDialog(
    item: CreationItem,
    onDismiss: () -> Unit
) {
    var isCodeView by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            color = StudioDarkBg,
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(NeonIndigo, NeonCyan)))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioSurface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("sandbox_close_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (item.previewType == PreviewType.HTML_RUNNABLE) "بيئة تشغيل تفاعلية مباشرة" else "عارض الكود والسيناريو",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (item.previewType == PreviewType.HTML_RUNNABLE) {
                            IconButton(
                                onClick = { isCodeView = !isCodeView },
                                modifier = Modifier.testTag("toggle_view_btn")
                            ) {
                                Icon(
                                    imageVector = if (isCodeView) Icons.Default.PlayCircle else Icons.Default.Code,
                                    contentDescription = "Toggle Mode",
                                    tint = NeonCyan
                                )
                            }

                            IconButton(
                                onClick = {
                                    webViewInstance?.reload()
                                    Toast.makeText(context, "تمت إعادة تشغيل اللعبة/الموقع 🔄", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("reload_sandbox_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.White)
                            }
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText(item.title, item.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ الكود للحافظة 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_code_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                        }
                    }
                }

                // Main Sandbox Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isCodeView || item.previewType != PreviewType.HTML_RUNNABLE) {
                        CodeViewer(code = item.content, previewType = item.previewType)
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        cacheMode = WebSettings.LOAD_NO_CACHE
                                    }
                                    webViewClient = WebViewClient()
                                    webChromeClient = WebChromeClient()
                                    loadDataWithBaseURL(null, item.content, "text/html", "UTF-8", null)
                                    webViewInstance = this
                                }
                            },
                            update = { view ->
                                webViewInstance = view
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("sandbox_webview")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CodeViewer(
    code: String,
    previewType: PreviewType,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CodeBg)
            .padding(12.dp)
            .verticalScroll(scrollState)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = when (previewType) {
                    PreviewType.KOTLIN_CODE -> "Kotlin / Jetpack Compose"
                    PreviewType.HTML_RUNNABLE -> "HTML5 / Canvas / JavaScript"
                    PreviewType.IMAGE_MOCKUP -> "AI Art Prompt & Parameters"
                    PreviewType.STORYBOARD -> "Cinematic Storyboard"
                    PreviewType.MARKDOWN_TEXT -> "Markdown Document"
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun StoryboardList(
    scenes: List<StoryboardScene>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        scenes.forEach { scene ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(NeonIndigo.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.3f))))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = NeonIndigo
                            ) {
                                Text(
                                    text = "#${scene.sceneNumber}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = scene.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioDarkBg
                        ) {
                            Text(
                                text = "${scene.durationSeconds} ثانية",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🎥 زاوية وحركة الكاميرا: ${scene.cameraAngle}",
                        fontSize = 12.sp,
                        color = NeonPink,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "🎨 الوصف البصري (Prompt):",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = scene.visualPrompt,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudioDarkBg.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "🎙️ التعليق الصوتي: \"${scene.narration}\"",
                                fontSize = 12.sp,
                                color = Color(0xFFFDE047),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🔊 المؤثر الصوتي: ${scene.audioEffect}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
