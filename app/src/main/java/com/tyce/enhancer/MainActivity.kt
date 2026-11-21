package com.tyce.enhancer

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.drawWithContent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.channels.Channel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // nag-crash? pasaylo na lang
        val crashPrefs = getSharedPreferences("crash", Context.MODE_PRIVATE)
        val lastCrash = crashPrefs.getString("trace", null)
        val prevHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                crashPrefs.edit().putString("trace", android.util.Log.getStackTraceString(e)).commit()
            } catch (_: Throwable) {
            }
            prevHandler?.uncaughtException(t, e)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        setContent {
            AppTheme {
                var showSplash by rememberSaveable { mutableStateOf(SPLASH_ENABLED && lastCrash == null) }
                var crash by remember { mutableStateOf(lastCrash) }
                Box(Modifier.fillMaxSize().background(C.Bg)) {
                    val c = crash
                    if (c != null) {
                        CrashScreen(c) {
                            crashPrefs.edit().remove("trace").apply()
                            crash = null
                        }
                    } else {
                        AppRoot()
                        if (showSplash) IntroSplash { showSplash = false }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrashScreen(trace: String, onContinue: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Text("The app crashed last time", style = heading(22f, FontWeight.Bold, C.Text))
        Spacer(Modifier.height(6.dp))
        Text("Tap Copy and send me this text so I can fix it.", style = inter(15f, FontWeight.Normal, C.Muted))
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(C.Card)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            SelectionContainer {
                Text(trace.take(6000), style = inter(12f, FontWeight.Normal, C.Text))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineButton("Copy", Modifier.weight(1f)) {
                clipboard.setText(AnnotatedString(trace))
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            }
            BrandButton("Continue", Modifier.weight(1f), onContinue)
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = C.Accent,
        onPrimary = Color.White,
        background = C.Bg,
        surface = C.Card,
        onSurface = C.Text
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

// ------- mga icon, drawing-drawing lang

private const val ICO_SPARK = "M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z M18.5 16v4 M16.5 18h4"
private const val ICO_IMAGE = "M5 5h14v14H5z M5 16l5-5 4 4 2-2 3 3"
private const val ICO_BACK = "M15 5l-7 7 7 7"
private const val ICO_CLOSE = "M6 6l12 12 M18 6L6 18"
private const val ICO_DOWN = "M12 4v11 M7 11l5 5 5-5 M5 20h14"
private const val ICO_HOME = "M4 11l8-7 8 7 M6 10v10h12V10"
private const val ICO_SHARE = "M12 15V4 M7.5 8.5L12 4l4.5 4.5 M5 13v7h14v-7"
private const val ICO_SPLIT = "M4 5h16v14H4z M12 3v18"
private const val ICO_PERSON = "M9 8a3 3 0 1 0 6 0a3 3 0 1 0 -6 0 M5 20c0-3.6 3-6 7-6s7 2.4 7 6"
private const val ICO_MIC = "M9 7a3 3 0 0 1 6 0v5a3 3 0 0 1 -6 0z M6 11a6 6 0 0 0 12 0 M12 17v3 M9 20h6"
private const val ICO_BAG = "M5 8h14l-1 12H6z M9 8a3 3 0 0 1 6 0"
private const val ICO_PHOTO = "M4 5h16v14H4z M4 16l5-5 4 4 3-3 4 4 M15 9.5h.01"
private const val ICO_MOUNTAIN = "M3 18l6-9 4 6 3-4 5 7z"
private const val ICO_CHECK = "M5 12.5l4.5 4.5L19 7.5"
private const val ICO_LIST = "M8 6h12 M8 12h12 M8 18h12 M4 6h.01 M4 12h.01 M4 18h.01"
private const val ICO_MORE = "M5 12h.01 M12 12h.01 M19 12h.01"
private const val ICO_MOON = "M20 14.5A8 8 0 1 1 9.5 4 6.5 6.5 0 0 0 20 14.5z"

private val iconCache = HashMap<String, ImageVector>()

private fun vec(d: String): ImageVector = iconCache.getOrPut(d) {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(d),
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ).build()
}

private val LocalIcoTint = compositionLocalOf { Color(0xFF121316) }

@Composable
private fun Ico(d: String, size: Dp, tint: Color = LocalIcoTint.current) {
    Image(vec(d), null, Modifier.size(size), colorFilter = ColorFilter.tint(tint))
}

// ------- model, ang bida

private enum class Screen { Home, History, Queue, Processing, Result }

private class LibItem(val uri: Uri, val isVideo: Boolean = false)

private class Scenario(val name: String, val icon: String, val sat: Float, val contrast: Float, val tip: String = "")

private val SCENARIOS = listOf(
    Scenario("Portrait", ICO_PERSON, 1.04f, 1.02f, "Gentle tone for natural skin"),
    Scenario("Concert", ICO_MIC, 1.12f, 1.10f, "Punchy colour for stage lights"),
    Scenario("Product", ICO_BAG, 1.08f, 1.06f, "Clean, crisp and vivid"),
    Scenario("Old Photo", ICO_PHOTO, 1.18f, 1.12f, "Revives faded colour and contrast"),
    Scenario("Landscape", ICO_MOUNTAIN, 1.15f, 1.05f, "Richer skies and greens"),
    Scenario("Night", ICO_MOON, 1.05f, 1.08f, "Lifts low-light shots")
)

private class ShareTarget(val name: String, val pkg: String?, val color: Color)

private val SHARE_TARGETS = listOf(
    ShareTarget("Instagram", "com.instagram.android", Color(0xFFE1306C)),
    ShareTarget("WhatsApp", "com.whatsapp", Color(0xFF25D366)),
    ShareTarget("TikTok", "com.zhiliaoapp.musically", Color(0xFF2B2B2F)),
    ShareTarget("LINE", "jp.naver.line.android", Color(0xFF06C755)),
    ShareTarget("More", null, Color(0xFF3A3A3E))
)

/** padayon bisag View Later, uy */
private class EnhanceJob(val source: Bitmap, val title: String) {
    var progress by mutableStateOf(0f)
    var result by mutableStateOf<Bitmap?>(null)
    var error by mutableStateOf<String?>(null)
    var done by mutableStateOf(false)
    var savedUri by mutableStateOf<Uri?>(null)
    var videoUri by mutableStateOf<Uri?>(null)
    var isVideo = false
    val cancel = AtomicBoolean(false)
    /** gikuha na, wa na hulat */
    var started by mutableStateOf(false)
    val id: Long = System.nanoTime()
    var work: (suspend () -> Unit)? = null
}

/** job runner, walay local ha */
private suspend fun runQueued(q: EnhanceJob) {
    if (q.cancel.get()) return
    q.started = true
    val w = q.work
    if (w != null) w()
}

private fun modeName(mode: Int) = when (mode) { 0 -> "HD"; 1 -> "Ultra HD"; else -> "AI Ultra HD" }
private fun scaleForMode(mode: Int) = when (mode) { 0 -> 1; 1 -> 2; else -> 4 }


// ------- mga gamit sa tanan

private val PillShape = RoundedCornerShape(50)
private val CardShape = RoundedCornerShape(16.dp)

@Composable
private fun BrandButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit, icon: String? = null) {
    Row(
        modifier
            .height(56.dp)
            .pressable(0.97f, onClick = onClick)
            .clip(PillShape)
            .background(C.Brand),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Ico(icon, 22.dp, Color.White)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = inter(19f, FontWeight.Bold, Color.White))
    }
}

@Composable
private fun OutlineButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    height: Dp = 52.dp,
    onClick: () -> Unit
) {
    Row(
        modifier
            .height(height)
            .pressable(0.97f, onClick = onClick)
            .clip(PillShape)
            .border(1.5.dp, Color(0x55121316), PillShape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Ico(icon, 20.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = inter(17f, FontWeight.Medium, C.Text))
    }
}

@Composable
private fun TopPill(text: String, dot: Color? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .height(40.dp)
            .pressable(0.95f, onClick = onClick)
            .clip(PillShape)
            .background(C.Card)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot != null) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = inter(16f, FontWeight.SemiBold, C.Text))
    }
}

@Composable
private fun IconBtn(path: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).pressable(0.88f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Ico(path, 28.dp) }
}

@Composable
private fun ScreenTopBar(
    onBack: () -> Unit,
    onClose: (() -> Unit)?,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBtn(ICO_BACK, onBack)
        if (onClose != null) IconBtn(ICO_CLOSE, onClose)
        Spacer(Modifier.weight(1f))
        trailing()
        Spacer(Modifier.width(10.dp))
    }
}

@Composable
private fun HdBadge(text: String, sparkle: Boolean = false, tint: Color = LocalIcoTint.current) {
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            Modifier
                .size(width = 36.dp, height = 25.dp)
                .border(1.6.dp, tint, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text, style = inter(11f, FontWeight.ExtraBold, tint))
        }
        if (sparkle) {
            Box(Modifier.offset(x = 7.dp, y = (-7).dp)) { Ico(ICO_SPARK, 13.dp, tint) }
        }
    }
}

@Composable
private fun ModeCard(
    label: String,
    sub: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    badge: @Composable () -> Unit
) {
    val bg by animateColorAsState(if (selected) C.AccentDeep else C.Card, tween(Motion.Tint, easing = IosEase), label = "mcBg")
    val bc by animateColorAsState(
        if (selected) C.Accent else C.Accent.copy(alpha = 0f), tween(Motion.Tint, easing = IosEase), label = "mcBorder"
    )
    Box(
        modifier
            .height(98.dp)
            .pressable(0.97f, onClick = onClick)
            .clip(CardShape)
            .background(bg)
            .border(1.8.dp, bc, CardShape)
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            badge()
            Spacer(Modifier.height(8.dp))
            Text(label, style = inter(16f, FontWeight.SemiBold, Color.White))
            Text(sub, style = inter(12f, FontWeight.Normal, if (selected) Color(0xFFB4D0FF) else C.Muted))
        }
        AnimatedVisibility(
            selected,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            enter = scaleIn(Motion.PopSpring) + fadeIn(tween(Motion.Fast)),
            exit = fadeOut(tween(Motion.Fast))
        ) {
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(C.Accent),
                contentAlignment = Alignment.Center
            ) { Ico(ICO_CHECK, 13.dp) }
        }
    }
}

@Composable
private fun ModeTile(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val bg by animateColorAsState(if (selected) C.AccentDeep else Color(0xFFF3F2EE), tween(Motion.Tint, easing = IosEase), label = "mtBg")
    val fg by animateColorAsState(if (selected) Color.White else C.Text, tween(Motion.Tint, easing = IosEase), label = "mtFg")
    val bc by animateColorAsState(
        if (selected) C.AccentDeep else Color(0x00F3F2EE), tween(Motion.Tint, easing = IosEase), label = "mtBorder"
    )
    Column(
        modifier
            .height(62.dp)
            .pressable(0.96f, enabled, onClick)
            .clip(CardShape)
            .background(bg)
            .border(1.8.dp, bc, CardShape)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(LocalIcoTint provides fg) { icon() }
        Spacer(Modifier.height(4.dp))
        Text(label, style = inter(14f, FontWeight.Medium, fg))
    }
}

/** panel sa ubos, bilog */
@Composable
private fun BottomPanel(title: String, tiles: @Composable RowScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(C.Panel)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = inter(18f, FontWeight.Medium, C.Text))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = tiles)
    }
}

@Composable
private fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = inter(14f, FontWeight.Medium, Color.White),
        modifier = modifier
            .clip(PillShape)
            .background(Color(0x99000000))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
private fun PopMenuItem(icon: String, label: String, color: Color = C.Text, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressable(0.97f, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Ico(icon, 22.dp, color)
        Spacer(Modifier.width(14.dp))
        Text(label, style = inter(16f, FontWeight.Medium, color))
    }
}

@Composable
private fun PopMenuItemImage(image: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pressable(0.97f, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(image, null, Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = inter(16f, FontWeight.Medium, C.Text))
    }
}

@Composable
private fun SheetHost(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible, enter = fadeIn(tween(Motion.Base)), exit = fadeOut(tween(Motion.Fast))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() }
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            // sheet misaka-manaog, walay fade
            enter = slideInVertically(tween(Motion.Sheet, easing = IosEase)) { it },
            exit = slideOutVertically(tween(Motion.SheetExit, easing = PopExitEase)) { it }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(C.Panel)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.size(width = 40.dp, height = 4.dp).clip(PillShape).background(Color(0xFFD5D4CE)))
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
private fun Thumb(uri: Uri, modifier: Modifier = Modifier, isVideo: Boolean = false) {
    val context = LocalContext.current
    val bmp by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) { loadThumb(context, uri, 400, isVideo) }
    }
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(C.Card)) {
        val b = bmp
        if (b != null) {
            val img = remember(b) { b.asImageBitmap() }
            Image(
                bitmap = img,
                contentDescription = "Enhanced photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (isVideo) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(6.dp).size(26.dp).clip(CircleShape).background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) { Ico(ICO_PLAY, 14.dp, Color.White) }
        }
    }
}

// ------- ang app root, ugat

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("enhancer", Context.MODE_PRIVATE) }

    var screen by remember { mutableStateOf(Screen.Home) }
    var mode by remember { mutableStateOf(when (prefs.getInt("out", 4)) { 1 -> 0; 2 -> 1; else -> 2 }) }
    var maxSide by remember { mutableStateOf(prefs.getInt("side", 800)) }
    var scenario by remember { mutableStateOf(-1) }
    var hasFace by remember { mutableStateOf(FaceRestorer.hasModel(context)) }
    var useFace by remember { mutableStateOf(prefs.getBoolean("face", true)) }
    var smooth by remember { mutableStateOf(prefs.getBoolean("smooth", false)) }
    var hasRife by remember { mutableStateOf(RifeInterp.hasUserModel(context)) }
    var hasBest by remember { mutableStateOf(Enhancer.hasBest(context)) }
    var useBest by remember { mutableStateOf(prefs.getBoolean("best", false) && Enhancer.hasBest(context)) }
    var cloudEnabled by remember { mutableStateOf(prefs.getBoolean("cloud", false)) }
    var cloudUrl by remember { mutableStateOf(prefs.getString("cloud_url", "") ?: "") }
    var cloudKey by remember { mutableStateOf(prefs.getString("cloud_key", "") ?: "") }
    var cloudTesting by remember { mutableStateOf(false) }
    var cloudStatus by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<EnhanceJob?>(null) }
    var hasModel by remember { mutableStateOf(Enhancer.hasModel(context)) }
    var library by remember { mutableStateOf(emptyList<LibItem>()) }
    var libVersion by remember { mutableStateOf(0) }
    var showModelSheet by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var menuAnchor by remember { mutableStateOf(Offset.Unspecified) }
    var showPicker by remember { mutableStateOf(false) }
    var lastTap by remember { mutableStateOf(Offset.Zero) }
    var pickerVideo by remember { mutableStateOf(false) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingVideoInfo by remember { mutableStateOf<VideoEnhancer.Info?>(null) }
    var showVideoQuality by remember { mutableStateOf(false) }
    var videoScale by remember { mutableStateOf(2) }
    var askExit by remember { mutableStateOf(false) }
    var stopTarget by remember { mutableStateOf<EnhanceJob?>(null) }
    val jobs = remember { mutableStateListOf<EnhanceJob>() }
    val queue = remember { Channel<EnhanceJob>(Channel.UNLIMITED) }

    var hasAccess by remember { mutableStateOf(hasMediaAccess(context)) }
    val accessLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasAccess = hasMediaAccess(context)
        libVersion++
    }

    LaunchedEffect(libVersion) {
        library = withContext(Dispatchers.IO) { queryLibrary(context) }
    }

    // basaha balik ang History, kapoy
    LaunchedEffect(screen) {
        if (screen == Screen.History) {
            hasAccess = hasMediaAccess(context)
            libVersion++
        }
    }

    // usa ka worker, pila-pila lang
    LaunchedEffect(Unit) {
        while (true) {
            runQueued(queue.receive())
        }
    }

    fun removeJob(j: EnhanceJob) {
        jobs.remove(j)
        if (job === j) {
            job = null
            if (screen == Screen.Processing || screen == Screen.Result) {
                screen = if (jobs.isEmpty()) Screen.Home else Screen.Queue
            }
        }
    }

    // humana na, tawga ang UI
    fun finishJob(j: EnhanceJob) {
        val err = j.error
        val watching = job === j && screen == Screen.Processing
        if (err == null && j.isVideo) {
            val vu = j.videoUri
            if (vu != null) {
                libVersion++
                Toast.makeText(context, "Video saved to Movies/AIEnhancer", Toast.LENGTH_LONG).show()
                if (watching) {
                    try {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW)
                                .setDataAndType(vu, "video/mp4")
                                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        )
                    } catch (_: Throwable) {}
                }
            }
            removeJob(j)
            return
        }
        if (err != null) {
            Toast.makeText(context, "Failed: $err", Toast.LENGTH_LONG).show()
            removeJob(j)
            return
        }
        if (j.result == null) {
            removeJob(j)
            return
        }
        if (watching) screen = Screen.Result
    }

    fun cancelJob(j: EnhanceJob) {
        j.cancel.set(true)
        if (!j.started) removeJob(j)
    }

    // una padulong progress, ikaduha Queue
    fun enqueue(j: EnhanceJob, body: suspend () -> Unit) {
        j.work = body
        val toQueue = jobs.isNotEmpty() || screen == Screen.Queue
        jobs.add(j)
        if (toQueue) {
            screen = Screen.Queue
        } else {
            job = j
            screen = Screen.Processing
        }
        queue.trySend(j)
    }

    fun openQueued(j: EnhanceJob) {
        job = j
        screen = if (j.done && j.result != null) Screen.Result else Screen.Processing
    }

    fun startJob(bmp: Bitmap) {
        val j = EnhanceJob(bmp, modeName(mode))
        val target = bmp.width * scaleForMode(mode)
        val sc = scenario
        val ub = useBest
        val uf = useFace
        val useCloud = CloudEnhancer.isConfigured(context)
        val cUrl = CloudEnhancer.url(context)
        val cKey = CloudEnhancer.apiKey(context)
        val cloudScale = scaleForMode(mode).coerceAtLeast(2)
        val body: suspend () -> Unit = body@{
            val result = withContext(Dispatchers.Default) {
                try {
                    var out: Bitmap? = if (useCloud) {
                        // server does its own tiling; we just send the shrunk photo and wait
                        j.progress = 0.1f
                        val r = CloudEnhancer.enhance(cUrl, cKey, cloudScale, bmp)
                        j.progress = 0.9f
                        r
                    } else {
                        val engine = Enhancer(context, best = ub && Enhancer.hasBest(context))
                        try {
                            engine.enhance(
                                bmp,
                                onProgress = { done, total -> j.progress = done.toFloat() / total },
                                isCancelled = { j.cancel.get() }
                            )
                        } finally {
                            engine.close()
                        }
                    }
                    // ultra HD, 2x ra gyud
                    val o = out
                    if (o != null && o.width > target) {
                        val h = (o.height.toLong() * target / o.width).toInt().coerceAtLeast(1)
                        val scaled = Bitmap.createScaledBitmap(o, target, h, true)
                        if (scaled !== o) o.recycle()
                        out = scaled
                    }
                    // nawong lang, kung naay model
                    val fc = out
                    if (fc != null && !j.cancel.get() && uf && FaceRestorer.hasModel(context)) {
                        val fr = try { FaceRestorer(context) } catch (t: Throwable) { t.printStackTrace(); null }
                        if (fr != null) {
                            try { out = fr.restore(fc, isCancelled = { j.cancel.get() }) } finally { fr.close() }
                        }
                    }
                    // sharpen una, dayon tono, tapos
                    val sharp = out
                    if (sharp != null && !j.cancel.get()) {
                        val s2 = Finish.sharpen(sharp, isCancelled = { j.cancel.get() })
                        if (s2 !== sharp) sharp.recycle()
                        out = s2
                    }
                    val cur = out
                    if (cur != null && !j.cancel.get() && sc in SCENARIOS.indices) {
                        out = applyScenario(cur, SCENARIOS[sc])
                    }
                    out
                } catch (t: Throwable) {
                    t.printStackTrace()
                    j.error = t.message ?: t.toString()
                    null
                }
            }
            if (j.cancel.get()) {
                removeJob(j)
                return@body
            }
            if (result != null) {
                j.result = result
                j.progress = 1f
            }
            j.done = true
            finishJob(j)
        }
        enqueue(j, body)
    }

    val modelPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                Toast.makeText(context, "Loading model...", Toast.LENGTH_SHORT).show()
                val err = withContext(Dispatchers.IO) {
                    try {
                        val tmp = java.io.File(context.filesDir, "model_tmp.onnx")
                        context.contentResolver.openInputStream(uri)!!.use { input ->
                            tmp.outputStream().use { input.copyTo(it) }
                        }
                        val dest = Enhancer.userModelFile(context)
                        if (dest.exists()) dest.delete()
                        if (!tmp.renameTo(dest)) throw IllegalStateException("Could not save model")
                        Enhancer(context).close()
                        null
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        Enhancer.userModelFile(context).delete()
                        java.io.File(context.filesDir, "model_tmp.onnx").delete()
                        t.message ?: t.toString()
                    }
                }
                hasModel = Enhancer.hasModel(context)
                Toast.makeText(
                    context,
                    if (err == null) "Model loaded." else "Model failed: $err",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun startVideoJob(uri: Uri, info: VideoEnhancer.Info, scale: Int) {
        val j = EnhanceJob(info.firstFrame, "Video · ${if (scale == 1) "HD" else "${scale}×"}")
        j.isVideo = true
        val best = useBest
        val sm = smooth
        val body: suspend () -> Unit = body@{
            withContext(Dispatchers.Default) {
                try {
                    val out = VideoEnhancer.run(
                        context, uri, scale, best, sm,
                        onProgress = { j.progress = it },
                        isCancelled = { j.cancel.get() }
                    )
                    j.videoUri = out
                } catch (t: Throwable) {
                    t.printStackTrace()
                    j.error = t.message ?: t.toString()
                }
            }
            if (j.cancel.get()) {
                removeJob(j)
                return@body
            }
            j.done = true
            finishJob(j)
        }
        enqueue(j, body)
    }

    fun videoPicked(uri: Uri?) {
        if (uri != null) {
            scope.launch {
                val info = withContext(Dispatchers.IO) { VideoEnhancer.inspect(context, uri) }
                if (info == null) {
                    Toast.makeText(context, "Could not open that video.", Toast.LENGTH_LONG).show()
                } else {
                    if (info.durationMs > VideoEnhancer.MAX_SECONDS * 1000L) {
                        Toast.makeText(
                            context,
                            "Only the first ${VideoEnhancer.MAX_SECONDS} seconds will be extracted.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    pendingVideoUri = uri
                    pendingVideoInfo = info
                    videoScale = scaleForMode(mode)
                    showVideoQuality = true
                }
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> videoPicked(uri) }

    fun tryVideo() {
        if (!hasModel) {
            Toast.makeText(context, "Add a model first.", Toast.LENGTH_SHORT).show()
            showModelSheet = true
            return
        }
        pickerVideo = true
        showPicker = true
    }

    val rifePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                Toast.makeText(context, "Loading Smooth video model...", Toast.LENGTH_SHORT).show()
                val err = withContext(Dispatchers.IO) {
                    try {
                        val tmp = java.io.File(context.filesDir, "rife_tmp.onnx")
                        context.contentResolver.openInputStream(uri)!!.use { input ->
                            tmp.outputStream().use { input.copyTo(it) }
                        }
                        val dest = RifeInterp.modelFile(context)
                        if (dest.exists()) dest.delete()
                        if (!tmp.renameTo(dest)) throw IllegalStateException("Could not save model")
                        RifeInterp(context).close()
                        null
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        RifeInterp.modelFile(context).delete()
                        java.io.File(context.filesDir, "rife_tmp.onnx").delete()
                        t.message ?: t.toString()
                    }
                }
                hasRife = RifeInterp.hasUserModel(context)
                if (err == null) {
                    smooth = true
                    prefs.edit().putBoolean("smooth", true).apply()
                }
                Toast.makeText(
                    context,
                    if (err == null) "Smooth video model loaded." else "Smooth video model failed: $err",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val bestPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                Toast.makeText(context, "Loading Best model...", Toast.LENGTH_SHORT).show()
                val err = withContext(Dispatchers.IO) {
                    try {
                        val tmp = java.io.File(context.filesDir, "best_tmp.onnx")
                        context.contentResolver.openInputStream(uri)!!.use { input ->
                            tmp.outputStream().use { input.copyTo(it) }
                        }
                        val dest = Enhancer.bestModelFile(context)
                        if (dest.exists()) dest.delete()
                        if (!tmp.renameTo(dest)) throw IllegalStateException("Could not save model")
                        Enhancer(context, best = true).close()
                        null
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        Enhancer.bestModelFile(context).delete()
                        java.io.File(context.filesDir, "best_tmp.onnx").delete()
                        t.message ?: t.toString()
                    }
                }
                hasBest = Enhancer.hasBest(context)
                if (err == null) {
                    useBest = true
                    prefs.edit().putBoolean("best", true).apply()
                }
                Toast.makeText(
                    context,
                    if (err == null) "Best model loaded." else "Model failed: $err",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val facePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                Toast.makeText(context, "Loading face model...", Toast.LENGTH_SHORT).show()
                val err = withContext(Dispatchers.IO) {
                    try {
                        val tmp = java.io.File(context.filesDir, "face_tmp.onnx")
                        context.contentResolver.openInputStream(uri)!!.use { input ->
                            tmp.outputStream().use { input.copyTo(it) }
                        }
                        val dest = FaceRestorer.modelFile(context)
                        if (dest.exists()) dest.delete()
                        if (!tmp.renameTo(dest)) throw IllegalStateException("Could not save model")
                        FaceRestorer(context).close()
                        null
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        FaceRestorer.modelFile(context).delete()
                        java.io.File(context.filesDir, "face_tmp.onnx").delete()
                        t.message ?: t.toString()
                    }
                }
                hasFace = FaceRestorer.hasModel(context)
                if (err == null) {
                    useFace = true
                    prefs.edit().putBoolean("face", true).apply()
                }
                Toast.makeText(
                    context,
                    if (err == null) "Face model loaded." else "Face model failed: $err",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun photoPicked(uri: Uri?) {
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { loadBitmap(context, uri, maxSide) }
                if (bmp != null) {
                    startJob(bmp)
                } else {
                    Toast.makeText(context, "Could not open that image. Try a JPG or PNG.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> photoPicked(uri) }

    fun tryNow() {
        if (!hasModel) {
            Toast.makeText(context, "Add a model first.", Toast.LENGTH_SHORT).show()
            showModelSheet = true
            return
        }
        pickerVideo = false
        showPicker = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent(PointerEventPass.Initial)
                        val c = e.changes.firstOrNull { it.pressed }
                        if (c != null) lastTap = c.position
                    }
                }
            }
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { (fadeIn(tween(Motion.Base, easing = IosEase)) + scaleIn(tween(Motion.Base, easing = IosEase), initialScale = 0.97f)) togetherWith fadeOut(tween(Motion.Fast)) },
            modifier = Modifier.fillMaxSize(),
            label = "screen"
        ) { s ->
            when (s) {
                Screen.Home -> HomeScreen(
                    mode = mode,
                    onMode = {
                        mode = it
                        prefs.edit().putInt("out", scaleForMode(it)).apply()
                    },
                    scenario = scenario,
                    onScenario = { scenario = if (scenario == it) -1 else it },
                    hasModel = hasModel,
                    jobs = jobs,
                    onExit = { (context as? Activity)?.finish() },
                    onHistory = { screen = Screen.History },
                    onMenu = { menuAnchor = it; menuOpen = true },
                    onModel = { showModelSheet = true },
                    onTry = { tryNow() },
                    onVideo = { tryVideo() },
                    onOpenJob = { screen = Screen.Queue }
                )
                Screen.History -> HistoryScreen(
                    list = library,
                    needsAccess = !hasAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                    onGrant = { accessLauncher.launch(mediaPermissions()) },
                    onBack = { screen = Screen.Home }
                )
                Screen.Queue -> QueueScreen(
                    jobs = jobs,
                    onBack = { screen = Screen.Home },
                    onAddPhoto = { tryNow() },
                    onAddVideo = { tryVideo() },
                    onOpen = { openQueued(it) },
                    onCancel = { stopTarget = it }
                )
                Screen.Processing -> {
                    val j = job
                    if (j != null) {
                        ProcessingScreen(
                            job = j,
                            onViewLater = { screen = Screen.Queue },
                            onCancel = { stopTarget = j }
                        )
                    }
                }
                Screen.Result -> {
                    val j = job
                    if (j != null && j.result != null) {
                        key(j) {
                            ResultScreen(
                                job = j,
                                onBack = {
                                    jobs.remove(j)
                                    job = null
                                    screen = if (jobs.isEmpty()) Screen.Home else Screen.Queue
                                },
                                onSaved = { libVersion++ }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showPicker,
            enter = slideInVertically(tween(Motion.Base, easing = IosEase)) { it } + fadeIn(tween(Motion.Base)),
            exit = slideOutVertically(tween(Motion.Fast, easing = PopExitEase)) { it } + fadeOut(tween(Motion.Fast))
        ) {
            MediaPickerScreen(
                video = pickerVideo,
                hasAccess = hasAccess,
                onGrant = { accessLauncher.launch(mediaPermissions()) },
                onPick = { uri ->
                    showPicker = false
                    if (pickerVideo) videoPicked(uri) else photoPicked(uri)
                },
                onSystem = {
                    showPicker = false
                    if (pickerVideo) {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    } else {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                },
                onBack = { showPicker = false }
            )
        }

        PopMenu(
            visible = menuOpen && screen == Screen.Home,
            onDismiss = { menuOpen = false },
            alignment = Alignment.TopStart,
            anchor = menuAnchor,
            boxModifier = Modifier.statusBarsPadding().padding(start = 16.dp, top = 52.dp)
        ) {
            PopMenuItem(ICO_SPARK, "HD & quality") { menuOpen = false; showModelSheet = true }
            PopMenuItem(ICO_LIST, if (jobs.isEmpty()) "Queue" else "Queue (${jobs.size})") { menuOpen = false; screen = Screen.Queue }
            PopMenuItem(ICO_CLOCK, "History") { menuOpen = false; screen = Screen.History }
            PopMenuItem(ICO_CLOSE, "Exit app", C.Red) { menuOpen = false; askExit = true }
        }
        PopDialog(
            visible = askExit,
            title = "Exit Sen Enchancer?",
            message = "Photos you already saved stay in your album.",
            confirmText = "Exit",
            onCancel = { askExit = false },
            onConfirm = { askExit = false; (context as? Activity)?.finish() }
        )
        BackHandler(enabled = showVideoQuality) { showVideoQuality = false }
        VideoQualitySheet(
            visible = showVideoQuality,
            scale = videoScale,
            onScale = { videoScale = it },
            onExtract = {
                showVideoQuality = false
                val uri = pendingVideoUri
                val info = pendingVideoInfo
                if (uri != null && info != null) startVideoJob(uri, info, videoScale)
                pendingVideoUri = null
                pendingVideoInfo = null
            },
            onDismiss = {
                showVideoQuality = false
                pendingVideoUri = null
                pendingVideoInfo = null
            }
        )
        PopDialog(
            visible = stopTarget != null && (screen == Screen.Processing || screen == Screen.Queue),
            title = "Stop enhancing?",
            message = "Progress on this photo will be lost.",
            cancelText = "Keep going",
            confirmText = "Stop",
            onCancel = { stopTarget = null },
            onConfirm = {
                val t = stopTarget
                stopTarget = null
                if (t != null) cancelJob(t)
            }
        )

        BackHandler(enabled = showModelSheet) { showModelSheet = false }
        ModelSheet(
            visible = showModelSheet,
            hasModel = hasModel,
            maxSide = maxSide,
            onMaxSide = {
                maxSide = it
                prefs.edit().putInt("side", it).apply()
            },
            hasRife = hasRife,
            onPickRife = { rifePicker.launch(arrayOf("*/*")) },
            onRemoveRife = {
                RifeInterp.modelFile(context).delete()
                hasRife = false
            },
            smooth = smooth,
            onSmooth = {
                smooth = it
                prefs.edit().putBoolean("smooth", it).apply()
            },
            hasFace = hasFace,
            useFace = useFace,
            onUseFace = {
                useFace = it
                prefs.edit().putBoolean("face", it).apply()
            },
            onPickFace = { facePicker.launch(arrayOf("*/*")) },
            onRemoveFace = {
                FaceRestorer.modelFile(context).delete()
                hasFace = false
            },
            hasBest = hasBest,
            useBest = useBest,
            onUseBest = {
                useBest = it && hasBest
                prefs.edit().putBoolean("best", useBest).apply()
            },
            onPickBest = { bestPicker.launch(arrayOf("*/*")) },
            onRemoveBest = {
                Enhancer.bestModelFile(context).delete()
                hasBest = false
                useBest = false
                prefs.edit().putBoolean("best", false).apply()
            },
            onPick = { modelPicker.launch(arrayOf("*/*")) },
            onRemove = {
                Enhancer.userModelFile(context).delete()
                hasModel = Enhancer.hasModel(context)
            },
            cloudEnabled = cloudEnabled,
            onCloudEnabled = {
                cloudEnabled = it
                prefs.edit().putBoolean("cloud", it).apply()
                cloudStatus = null
            },
            cloudUrl = cloudUrl,
            onCloudUrl = {
                cloudUrl = it
                prefs.edit().putString("cloud_url", it).apply()
                cloudStatus = null
            },
            cloudKey = cloudKey,
            onCloudKey = {
                cloudKey = it
                prefs.edit().putString("cloud_key", it).apply()
                cloudStatus = null
            },
            cloudTesting = cloudTesting,
            cloudStatus = cloudStatus,
            onCloudTest = {
                val u = cloudUrl
                val k = cloudKey
                cloudTesting = true
                cloudStatus = null
                scope.launch {
                    val err = withContext(Dispatchers.IO) { CloudEnhancer.test(u, k) }
                    cloudTesting = false
                    cloudStatus = err ?: "Connected"
                }
            },
            onDismiss = { showModelSheet = false }
        )
    }
}

// ------- home, balay nato

/** bulak-bulak lang, demo pic */
private fun DrawScope.drawGarden(sharp: Boolean) {
    val w = size.width
    val h = size.height
    drawRect(Color(0xFF0B1F26))
    val palette = listOf(
        Color(0xFFFF8FB8), Color(0xFFFFD1E0), Color(0xFFFF5C93), Color(0xFFFFB36B), Color(0xFFF5F0F5)
    )
    val rnd = java.util.Random(11)
    for (n in 0 until 30) {
        val cx = rnd.nextFloat() * w
        val cy = rnd.nextFloat() * h
        val r = (12f + rnd.nextFloat() * 36f).dp.toPx()
        val col = palette[rnd.nextInt(palette.size)]
        val rot = rnd.nextFloat() * 6.28f
        for (k in 0 until 5) {
            val ang = rot + k * 1.2566371f
            val c = Offset(cx + cos(ang) * r * 0.55f, cy + sin(ang) * r * 0.55f)
            drawCircle(col.copy(alpha = 0.92f), r * 0.5f, c)
            if (sharp) drawCircle(Color(0x33000000), r * 0.5f, c, style = Stroke(0.8.dp.toPx()))
        }
        drawCircle(Color(0xFFF2C14E), r * 0.28f, Offset(cx, cy))
        if (sharp) drawCircle(Color(0xFFB8860B), r * 0.12f, Offset(cx, cy))
    }
}

private val LInk = Color(0xFF121316)
private val LPaper = Color(0xFFF3F2EE)
private val LMuted = Color(0xFF6B6E76)
private val LBlue = Color(0xFF2F5BFF)
private const val ICO_PLAY = "M8 5.5v13l10-6.5z"
private const val ICO_CLOCK = "M12 3.5c4.7 0 8.5 3.8 8.5 8.5 0 4.7-3.8 8.5-8.5 8.5-4.7 0-8.5-3.8-8.5-8.5 0-4.7 3.8-8.5 8.5-8.5z M12 7.5V12l3 2"

@Composable
private fun PhotoTag(text: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xEBFFFFFF)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, style = inter(12f, FontWeight.SemiBold, LInk))
    }
}

@Composable
private fun DemoHero(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "hero")
    val auto by t.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(3600, easing = IosEase), RepeatMode.Reverse),
        label = "heroPos"
    )
    var manual by remember { mutableStateOf<Float?>(null) }
    val p = manual ?: auto
    val pNow by rememberUpdatedState(p)
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(LInk)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        manual = manual ?: pNow
                    },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        manual = ((manual ?: pNow) + dx / size.width).coerceIn(0.05f, 0.95f)
                    }
                )
            }
    ) {
        Image(painterResource(R.drawable.demo_after), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Image(
            painterResource(R.drawable.demo_before), null,
            Modifier.fillMaxSize().drawWithContent {
                clipRect(0f, 0f, size.width * p, size.height) { this@drawWithContent.drawContent() }
            },
            contentScale = ContentScale.Crop
        )
        Canvas(Modifier.fillMaxSize()) {
            val x = size.width * p
            val cy = size.height / 2f
            drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
            drawCircle(Color(0x55000000), 19.dp.toPx(), Offset(x, cy + 2.dp.toPx()))
            drawCircle(Color.White, 18.dp.toPx(), Offset(x, cy))
            val a = 4.dp.toPx()
            val gap = 4.dp.toPx()
            val sw = 2.2.dp.toPx()
            drawLine(LInk, Offset(x - gap - a, cy), Offset(x - gap, cy - a), sw, StrokeCap.Round)
            drawLine(LInk, Offset(x - gap - a, cy), Offset(x - gap, cy + a), sw, StrokeCap.Round)
            drawLine(LInk, Offset(x + gap + a, cy), Offset(x + gap, cy - a), sw, StrokeCap.Round)
            drawLine(LInk, Offset(x + gap + a, cy), Offset(x + gap, cy + a), sw, StrokeCap.Round)
        }
        PhotoTag("Before", Modifier.align(Alignment.TopStart).padding(14.dp))
        PhotoTag("After", Modifier.align(Alignment.TopEnd).padding(14.dp))
    }
}

@Composable
private fun QualityTile(num: String, name: String, sub: String, sel: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (sel) LInk else LPaper, tween(Motion.Tint, easing = IosEase), label = "qBg")
    val fg by animateColorAsState(if (sel) Color.White else LInk, tween(Motion.Tint, easing = IosEase), label = "qFg")
    val sc by animateColorAsState(if (sel) Color(0xFFA9ACB4) else LMuted, tween(Motion.Tint, easing = IosEase), label = "qSub")
    Column(
        modifier
            .pressable(0.97f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(num, style = heading(24f, FontWeight.Bold, fg))
        Text(name, style = inter(14f, FontWeight.SemiBold, fg))
        Text(sub, style = inter(12f, FontWeight.Normal, sc))
    }
}

@Composable
private fun ScenarioItem(sc: Scenario, sel: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (sel) LInk else LPaper, tween(Motion.Tint, easing = IosEase), label = "scBg")
    val fg by animateColorAsState(if (sel) Color.White else LInk, tween(Motion.Tint, easing = IosEase), label = "scFg")
    Column(
        Modifier.width(72.dp).pressable(0.95f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(bg),
            contentAlignment = Alignment.Center
        ) { Ico(sc.icon, 30.dp, fg) }
        Spacer(Modifier.height(10.dp))
        Text(
            sc.name,
            style = inter(14f, if (sel) FontWeight.SemiBold else FontWeight.Normal, if (sel) LInk else LMuted),
            maxLines = 1
        )
    }
}

@Composable
private fun HomeScreen(
    mode: Int,
    onMode: (Int) -> Unit,
    scenario: Int,
    onScenario: (Int) -> Unit,
    hasModel: Boolean,
    jobs: List<EnhanceJob>,
    onExit: () -> Unit,
    onHistory: () -> Unit,
    onMenu: (Offset) -> Unit,
    onModel: () -> Unit,
    onTry: () -> Unit,
    onVideo: () -> Unit,
    onOpenJob: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    DisposableEffect(Unit) {
        val ctl = WindowCompat.getInsetsController((view.context as Activity).window, view)
        val prevStatus = ctl.isAppearanceLightStatusBars
        val prevNav = ctl.isAppearanceLightNavigationBars
        ctl.isAppearanceLightStatusBars = true
        ctl.isAppearanceLightNavigationBars = true
        onDispose {
            ctl.isAppearanceLightStatusBars = prevStatus
            ctl.isAppearanceLightNavigationBars = prevNav
        }
    }
    fun pickMode(m: Int) {
        if (m != mode) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onMode(m)
        }
    }
    fun pickScenario(i: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onScenario(i)
    }
    // litrato mokuha sa sobrang gitas-on
    val headerContent: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var menuCenter by remember { mutableStateOf(Offset.Unspecified) }
            Box(
                Modifier
                    .size(44.dp)
                    .onGloballyPositioned { menuCenter = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) }
                    .pressable(0.92f, onClick = { onMenu(menuCenter) })
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) { Ico(ICO_MORE, 22.dp, LInk) }
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(Motion.Base, easing = IosEase)) togetherWith fadeOut(tween(Motion.Fast)) },
                modifier = Modifier.weight(1f),
                label = "modeTitle"
            ) { m ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(modeName(m), style = heading(18f, FontWeight.Bold, LInk))
                }
            }
            Box(
                Modifier.size(44.dp).pressable(0.92f, onClick = onHistory).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) { Ico(ICO_CLOCK, 22.dp, LInk) }
        }
    }
    val panelContent: @Composable (Boolean) -> Unit = { fillExtra ->
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .padding(horizontal = 20.dp)
        ) {
            Column(Modifier.weight(1f, fill = fillExtra).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(26.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Quality", style = heading(16f, FontWeight.Bold, LInk), modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .height(40.dp)
                            .pressable(0.96f, onClick = onModel)
                            .border(1.5.dp, LInk, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(if (hasModel) "HD" else "Add model", style = inter(14f, FontWeight.SemiBold, LInk)) }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QualityTile("HD", "HD", "Original size, quick cleanup", mode == 0, Modifier.weight(1f)) { pickMode(0) }
                    QualityTile("2×", "Ultra HD", "Fast, everyday photos", mode == 1, Modifier.weight(1f)) { pickMode(1) }
                    QualityTile("4×", "AI Ultra HD", "Slower, finest detail", mode == 2, Modifier.weight(1f)) { pickMode(2) }
                }
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Scenario", style = heading(16f, FontWeight.Bold, LInk), modifier = Modifier.weight(1f))
                    Text("Optional", style = inter(13f, FontWeight.Normal, LMuted))
                }
                Spacer(Modifier.height(14.dp))
                LazyRow(
                    modifier = Modifier.layout { m, c ->
                        val e = 20.dp.roundToPx()
                        val full = c.maxWidth + 2 * e
                        val pl = m.measure(c.copy(minWidth = full, maxWidth = full))
                        layout(c.maxWidth, pl.height) { pl.place(-e, 0) }
                    },
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(SCENARIOS.size) { i ->
                        ScenarioItem(SCENARIOS[i], scenario == i) { pickScenario(i) }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (jobs.isNotEmpty()) {
                val active = jobs.firstOrNull { it.started && !it.done }
                val ready = jobs.count { it.done && it.result != null }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LPaper)
                        .pressable(0.98f, onClick = onOpenJob)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when {
                            active != null -> "Enhancing… ${(active.progress * 100).toInt()}%" +
                                if (jobs.size > 1) " · ${jobs.size - 1} more" else ""
                            ready > 0 -> "$ready ready to view"
                            else -> "Waiting in queue"
                        },
                        style = inter(15f, FontWeight.Medium, LInk),
                        modifier = Modifier.weight(1f)
                    )
                    Text("Queue", style = inter(15f, FontWeight.Bold, LBlue))
                }
                Spacer(Modifier.height(10.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressable(0.97f, onClick = onTry)
                    .clip(RoundedCornerShape(18.dp))
                    .background(LBlue),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Ico(ICO_IMAGE, 22.dp)
                Spacer(Modifier.width(10.dp))
                Text("Choose photo", style = inter(17f, FontWeight.SemiBold, Color.White))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressable(0.97f, onClick = onVideo)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, LBlue, RoundedCornerShape(18.dp)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Choose video (beta)", style = inter(16f, FontWeight.SemiBold, LBlue))
            }
            Spacer(Modifier.navigationBarsPadding().height(20.dp))
        }
    }
    SubcomposeLayout(Modifier.fillMaxSize().background(LPaper).statusBarsPadding()) { c ->
        val w = c.maxWidth
        val h = c.maxHeight
        val overlap = 28.dp.roundToPx()
        val headerH = 56.dp.roundToPx()
        val header = subcompose("header", headerContent).map { it.measure(Constraints.fixed(w, headerH)) }
        val avail = h - headerH
        // una: sukda ang controls, pasensya
        val natural = subcompose("probe") { panelContent(false) }
            .maxOf { it.measure(Constraints(w, w, 0, avail + overlap)).height }
        val maxHero = ((w - 32.dp.roundToPx()) / 1.085f).roundToInt()
        val heroH = (avail + overlap - natural).coerceIn(0, maxHero)
        val hero = subcompose("hero") {
            DemoHero(Modifier.padding(horizontal = 16.dp).fillMaxSize())
        }.map { it.measure(Constraints.fixed(w, heroH)) }
        // ikaduha: ang nabilin, pun-a
        val panelH = avail - heroH + overlap
        val panel = subcompose("panel") { panelContent(true) }
            .map { it.measure(Constraints.fixed(w, panelH)) }
        layout(w, h) {
            header.forEach { it.place(0, 0) }
            hero.forEach { it.place(0, headerH) }
            panel.forEach { it.place(0, headerH + heroH - overlap) }
        }
    }
}

@Composable
private fun ModelSheet(
    visible: Boolean,
    hasModel: Boolean,
    maxSide: Int,
    onMaxSide: (Int) -> Unit,
    hasRife: Boolean,
    onPickRife: () -> Unit,
    onRemoveRife: () -> Unit,
    smooth: Boolean,
    onSmooth: (Boolean) -> Unit,
    hasFace: Boolean,
    useFace: Boolean,
    onUseFace: (Boolean) -> Unit,
    onPickFace: () -> Unit,
    onRemoveFace: () -> Unit,
    hasBest: Boolean,
    useBest: Boolean,
    onUseBest: (Boolean) -> Unit,
    onPickBest: () -> Unit,
    onRemoveBest: () -> Unit,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    cloudEnabled: Boolean,
    onCloudEnabled: (Boolean) -> Unit,
    cloudUrl: String,
    onCloudUrl: (String) -> Unit,
    cloudKey: String,
    onCloudKey: (String) -> Unit,
    cloudTesting: Boolean,
    cloudStatus: String?,
    onCloudTest: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    SheetHost(visible, onDismiss) {
        var showModels by remember { mutableStateOf(false) }
        val userFile = Enhancer.userModelFile(context)
        val hasUser = hasModel && userFile.exists() && userFile.length() > 0
        val mb = if (hasUser) userFile.length() / 1048576.0 else 0.0
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
        ) {
            Text(
                "HD & quality",
                style = heading(20f, FontWeight.SemiBold, C.Text),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (cloudEnabled) "Photos are sent to your cloud GPU for this step."
                else "Everything runs on this phone. Nothing is uploaded.",
                style = inter(14f, FontWeight.Normal, C.Muted, 1.35f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(22.dp))

            SheetSection("Photo detail", "Detailed keeps more texture but takes longer.") {
                Segmented(listOf("Fast · 512 px", "Detailed · 800 px"), if (maxSide == 512) 0 else 1) {
                    onMaxSide(if (it == 0) 512 else 800)
                }
            }
            if (hasBest) {
                Spacer(Modifier.height(20.dp))
                SheetSection("Quality engine", "Best gives the sharpest result but is slower.") {
                    Segmented(listOf("Standard", "Best · slower"), if (useBest) 1 else 0) { onUseBest(it == 1) }
                }
            }
            if (hasFace) {
                Spacer(Modifier.height(20.dp))
                SheetSection("Face restore", "Cleans up faces in portraits.") {
                    Segmented(listOf("Off", "On"), if (useFace) 1 else 0) { onUseFace(it == 1) }
                }
            }
            Spacer(Modifier.height(20.dp))
            SheetSection("Smooth video", "Adds in-between frames so videos move more fluidly.") {
                Segmented(listOf("Off", "On"), if (smooth) 1 else 0) { onSmooth(it == 1) }
            }

            Spacer(Modifier.height(20.dp))
            SheetSection(
                "Cloud GPU",
                "Use a GPU server you rent (RunPod, Vast.ai, your own box) instead of this phone. Photos for the enhance step leave the device. Photos (beta) only."
            ) {
                Segmented(listOf("Off", "On"), if (cloudEnabled) 1 else 0) { onCloudEnabled(it == 1) }
                if (cloudEnabled) {
                    Spacer(Modifier.height(10.dp))
                    SheetTextField(cloudUrl, "Server URL · http://1.2.3.4:8000/enhance", onCloudUrl)
                    Spacer(Modifier.height(8.dp))
                    SheetTextField(cloudKey, "API key (optional)", onCloudKey, password = true)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SheetLink(
                            if (cloudTesting) "Testing…" else "Test connection",
                            C.Accent, Color(0x1A2F5BFF)
                        ) { if (!cloudTesting && cloudUrl.isNotBlank()) onCloudTest() }
                        if (cloudStatus != null) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                cloudStatus,
                                style = inter(
                                    13f, FontWeight.SemiBold,
                                    if (cloudStatus == "Connected") Color(0xFF2FBF71) else Color(0xFFE5484D)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(C.Line))
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(0.99f) { showModels = !showModels }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Models", style = inter(16f, FontWeight.Bold, C.Text))
                    Text("Advanced: add or replace your own", style = inter(13f, FontWeight.Normal, C.Muted))
                }
                Text(if (showModels) "Hide" else "Show", style = inter(14f, FontWeight.SemiBold, C.Accent))
            }
            if (showModels) {
                ModelRow(
                    "HD model",
                    when {
                        hasUser -> "Your own · ${"%.1f".format(mb)} MB"
                        hasModel -> "Built in"
                        else -> "Not added yet"
                    },
                    if (hasModel) "Replace" else "Add", onPick,
                    if (hasUser) "Reset" else null, onRemove
                )
                ModelRow(
                    "Best model (x4plus)",
                    if (hasBest) "Added" else "Optional · sharper but slower",
                    if (hasBest) "Replace" else "Add", onPickBest,
                    if (hasBest) "Remove" else null, onRemoveBest
                )
                ModelRow(
                    "Face model (GPEN)",
                    if (hasFace) "Added" else "Optional · better portraits",
                    if (hasFace) "Replace" else "Add", onPickFace,
                    if (hasFace) "Remove" else null, onRemoveFace
                )
                ModelRow(
                    "Smooth video model",
                    if (hasRife) "Your own" else "Built in",
                    "Replace", onPickRife,
                    if (hasRife) "Reset" else null, onRemoveRife
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** extract video, pili-a ang texture: HD, 2x, 4x */
@Composable
private fun VideoQualitySheet(
    visible: Boolean,
    scale: Int,
    onScale: (Int) -> Unit,
    onExtract: () -> Unit,
    onDismiss: () -> Unit
) {
    SheetHost(visible, onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                "Extract video",
                style = heading(20f, FontWeight.SemiBold, C.Text),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Choose a texture for this video.",
                style = inter(14f, FontWeight.Normal, C.Muted, 1.35f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QualityTile("HD", "HD", "Original size", scale == 1, Modifier.weight(1f)) { onScale(1) }
                QualityTile("2×", "Ultra HD", "Fast", scale == 2, Modifier.weight(1f)) { onScale(2) }
                QualityTile("4×", "AI Ultra HD", "Finest detail", scale == 4, Modifier.weight(1f)) { onScale(4) }
            }
            Spacer(Modifier.height(20.dp))
            SaveButton(0, onExtract, label = "Extract video")
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** save button, 3 ka mood */
@Composable
private fun SaveButton(state: Int, onClick: () -> Unit, label: String = "Save", busyLabel: String = "Saving", doneLabel: String = "Saved") {
    val bg by animateColorAsState(
        if (state == 2) C.AccentDeep else C.Accent, tween(Motion.Tint, easing = IosEase), label = "saveBg"
    )
    Row(
        Modifier
            .height(42.dp)
            .pressable(0.95f, state != 1, onClick)
            .animateContentSize(tween(Motion.Base, easing = IosEase))
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (scaleIn(Motion.PopSpring, initialScale = 0.4f) + fadeIn(tween(Motion.Fast))) togetherWith
                    fadeOut(tween(Motion.Fast))
            },
            label = "saveIcon"
        ) { st ->
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                when (st) {
                    1 -> SaveSpinner()
                    2 -> Ico(ICO_CHECK, 20.dp, Color.White)
                    else -> Ico(ICO_DOWN, 18.dp, Color.White)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            when (state) { 1 -> busyLabel; 2 -> doneLabel; else -> label },
            style = inter(17f, FontWeight.SemiBold, Color.White)
        )
    }
}

@Composable
private fun SaveSpinner() {
    val angle by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = androidx.compose.animation.core.LinearEasing)),
        label = "spinAngle"
    )
    Canvas(Modifier.size(18.dp)) {
        val w = 2.5.dp.toPx()
        drawArc(
            color = Color.White.copy(alpha = 0.25f),
            startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
            size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w)
        )
        drawArc(
            color = Color.White,
            startAngle = angle, sweepAngle = 90f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
            size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

@Composable
private fun SheetSection(title: String, hint: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = inter(16f, FontWeight.Bold, C.Text))
        Spacer(Modifier.height(2.dp))
        Text(hint, style = inter(13f, FontWeight.Normal, C.Muted, 1.35f))
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(C.Bg).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            val bg by animateColorAsState(if (on) C.AccentDeep else Color.Transparent, tween(Motion.Tint, easing = IosEase), label = "segBg")
            val fg by animateColorAsState(if (on) Color.White else C.Text, tween(Motion.Tint, easing = IosEase), label = "segFg")
            Box(
                Modifier
                    .weight(1f)
                    .height(46.dp)
                    .pressable(0.97f) { onSelect(i) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = inter(15f, FontWeight.SemiBold, fg))
            }
        }
    }
}

@Composable
private fun ModelRow(
    title: String,
    status: String,
    action: String,
    onAction: () -> Unit,
    reset: String? = null,
    onReset: () -> Unit = {}
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = inter(15f, FontWeight.SemiBold, C.Text))
            Text(status, style = inter(13f, FontWeight.Normal, C.Muted))
        }
        if (reset != null) {
            SheetLink(reset, C.Muted, C.Bg, onReset)
            Spacer(Modifier.width(8.dp))
        }
        SheetLink(action, C.Accent, Color(0x1A2F5BFF), onAction)
    }
}

@Composable
private fun SheetTextField(value: String, placeholder: String, onChange: (String) -> Unit, password: Boolean = false) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, style = inter(14f, FontWeight.Normal, C.Muted)) },
        textStyle = inter(14f, FontWeight.Normal, C.Text),
        singleLine = true,
        visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.Accent,
            unfocusedBorderColor = C.Line,
            focusedContainerColor = C.Bg,
            unfocusedContainerColor = C.Bg,
            cursorColor = C.Accent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SheetLink(text: String, fg: Color, bg: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .pressable(0.95f, onClick = onClick)
            .clip(PillShape)
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = inter(14f, FontWeight.SemiBold, fg))
    }
}

// ------- history, mga old pics

@Composable
private fun HistoryScreen(list: List<LibItem>, needsAccess: Boolean, onGrant: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    BackHandler(true) { onBack() }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(ICO_BACK, onBack)
            Text("History", style = heading(22f, FontWeight.SemiBold, C.Text), modifier = Modifier.weight(1f))
            Text("${list.size}", style = inter(16f, FontWeight.SemiBold, C.Muted))
            Spacer(Modifier.width(16.dp))
        }
        if (needsAccess) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(CardShape).background(Color.White).padding(14.dp)
            ) {
                Text(
                    "Items saved before an update or reinstall stay hidden until you allow access to your photos and videos.",
                    style = inter(14f, FontWeight.Normal, C.Muted, 1.35f)
                )
                Spacer(Modifier.height(10.dp))
                OutlineButton("Allow access", Modifier.fillMaxWidth(), onClick = onGrant)
            }
            Spacer(Modifier.height(8.dp))
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Nothing here yet. Photos and videos you save after enhancing appear here.",
                    style = inter(15f, FontWeight.Normal, C.Muted, 1.4f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.uri.toString() }) { item ->
                    Thumb(item.uri, Modifier.aspectRatio(1f).pressable(0.97f) { openMedia(context, item.uri, item.isVideo) }, item.isVideo)
                }
            }
        }
    }
}

@Composable
private fun MediaPickerScreen(
    video: Boolean,
    hasAccess: Boolean,
    onGrant: () -> Unit,
    onPick: (Uri) -> Unit,
    onSystem: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler(true) { onBack() }
    LaunchedEffect(Unit) {
        if (!hasAccess) onGrant()
    }
    val items by produceState<List<LibItem>?>(null, video, hasAccess) {
        value = if (hasAccess) withContext(Dispatchers.IO) { queryPickerMedia(context, video) } else emptyList()
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(ICO_CLOSE, onBack)
            Text(
                if (video) "Choose a video" else "Choose a photo",
                style = heading(22f, FontWeight.SemiBold, C.Text),
                modifier = Modifier.weight(1f)
            )
            Text(
                "Browse",
                style = inter(16f, FontWeight.SemiBold, C.Accent),
                modifier = Modifier.pressable(0.94f) { onSystem() }.padding(horizontal = 12.dp, vertical = 12.dp)
            )
        }
        val list = items
        if (list == null) {
            Box(Modifier.fillMaxSize())
        } else if (list.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (hasAccess) {
                        if (video) "No videos found on this phone." else "No photos found on this phone."
                    } else {
                        "Allow access to your photos and videos to pick from your gallery here, newest first."
                    },
                    style = inter(15f, FontWeight.Normal, C.Muted, 1.4f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                if (!hasAccess) {
                    OutlineButton("Allow access", Modifier.fillMaxWidth(), onClick = onGrant)
                    Spacer(Modifier.height(10.dp))
                }
                OutlineButton("Browse files", Modifier.fillMaxWidth(), onClick = onSystem)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(list, key = { it.uri.toString() }) { item ->
                    Thumb(item.uri, Modifier.aspectRatio(1f).pressable(0.97f) { onPick(item.uri) }, item.isVideo)
                }
            }
        }
    }
}

private fun queryPickerMedia(context: Context, video: Boolean): List<LibItem> {
    val base = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val out = ArrayList<LibItem>()
    try {
        context.contentResolver.query(
            base,
            arrayOf(MediaStore.MediaColumns._ID),
            null,
            null,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC, ${MediaStore.MediaColumns._ID} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (c.moveToNext()) {
                out.add(LibItem(ContentUris.withAppendedId(base, c.getLong(idCol)), video))
            }
        }
    } catch (t: Throwable) {
        t.printStackTrace()
    }
    return out
}

// ------- pila, hulat lang dong

@Composable
private fun QueueScreen(
    jobs: List<EnhanceJob>,
    onBack: () -> Unit,
    onAddPhoto: () -> Unit,
    onAddVideo: () -> Unit,
    onOpen: (EnhanceJob) -> Unit,
    onCancel: (EnhanceJob) -> Unit
) {
    BackHandler(true) { onBack() }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(ICO_BACK, onBack)
            Text("Queue", style = heading(22f, FontWeight.SemiBold, C.Text), modifier = Modifier.weight(1f))
            Text("${jobs.size}", style = inter(16f, FontWeight.SemiBold, C.Muted))
            Spacer(Modifier.width(16.dp))
        }
        if (jobs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Nothing in the queue. Add photos or videos and they are enhanced one after another while you do other things.",
                    style = inter(15f, FontWeight.Normal, C.Muted, 1.4f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(jobs, key = { it.id }) { j ->
                    QueueRow(j, onOpen = { onOpen(j) }, onCancel = { onCancel(j) })
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BrandButton("Add photo", Modifier.weight(1f), onClick = onAddPhoto, icon = ICO_IMAGE)
            OutlineButton("Add video", Modifier.weight(1f), height = 56.dp, onClick = onAddVideo)
        }
    }
}

@Composable
private fun QueueRow(j: EnhanceJob, onOpen: () -> Unit, onCancel: () -> Unit) {
    val img = remember(j.source) { j.source.asImageBitmap() }
    val ready = j.done && j.result != null
    val p by animateFloatAsState(j.progress, tween(Motion.Tint, easing = IosEase), label = "qp")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Color.White)
            .pressable(0.98f, onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = img,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (j.isVideo) "Video" else j.title, style = inter(16f, FontWeight.SemiBold, C.Text), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    ready -> "Ready - tap to view"
                    !j.started -> "Waiting"
                    else -> "Enhancing... ${(p * 100).toInt()}%"
                },
                style = inter(13f, FontWeight.Normal, C.Muted)
            )
            if (j.started && !ready) {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(4.dp).clip(PillShape).background(C.Line)) {
                    Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).fillMaxHeight().background(C.Accent))
                }
            }
        }
        if (ready) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Ico(ICO_CHECK, 24.dp, C.Accent) }
        } else {
            IconBtn(ICO_CLOSE, onCancel)
        }
    }
}

// ------- nagtrabaho pa, hulat gamay

@Composable
private fun ProcessingScreen(job: EnhanceJob, onViewLater: () -> Unit, onCancel: () -> Unit) {
    val waiting = !job.started
    BackHandler(true) { onViewLater() }
    val p by animateFloatAsState(job.progress, tween(Motion.Tint, easing = IosEase), label = "p")
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = IosEase), RepeatMode.Reverse),
        label = "pv"
    )
    val img = remember(job.source) { job.source.asImageBitmap() }
    Column(Modifier.fillMaxSize().background(C.Bg).statusBarsPadding()) {
        ScreenTopBar(onBack = onViewLater, onClose = onCancel)
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = img,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit,
                alpha = 0.3f
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(68.dp)
                        .graphicsLayer {
                            scaleX = pulse.value
                            scaleY = pulse.value
                        }
                        .clip(RoundedCornerShape(22.dp))
                        .background(C.Brand),
                    contentAlignment = Alignment.Center
                ) { Ico(ICO_SPARK, 36.dp, Color.White) }
                Spacer(Modifier.height(18.dp))
                Text(
                    if (waiting) "Waiting in queue..." else "Processing... ${(p * 100).toInt()}%",
                    style = heading(24f, FontWeight.Bold, C.Text).copy(fontFeatureSettings = "tnum")
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Wait time depends on your phone and the size of the image.",
                    style = inter(17f, FontWeight.Medium, C.Text, 1.3f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
                Spacer(Modifier.height(18.dp))
                OutlineButton("View Later", Modifier.width(150.dp), height = 44.dp, onClick = onViewLater)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.pressable(0.94f, onClick = onCancel).padding(horizontal = 18.dp, vertical = 8.dp)) {
                    Text("Cancel", style = inter(16f, FontWeight.Medium, C.Muted))
                }
            }
        }
        BottomPanel("Enhance-${job.title}") {
            ModeTile("Before", true, true, Modifier.weight(1f), {}) { Ico(ICO_IMAGE, 24.dp) }
            ModeTile("After", false, false, Modifier.weight(1f), {}) { HdBadge("HD+") }
        }
    }
}

// ------- resulta, ta-da dong

@Composable
private fun ResultScreen(job: EnhanceJob, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val before = job.source
    val after = job.result ?: return
    val beforeImg = remember(before) { before.asImageBitmap() }
    val afterImg = remember(after) { downscaleForDisplay(after).asImageBitmap() }
    val ratio = before.width.toFloat() / before.height.toFloat()

    // una, tunga, karon ra
    var tab by remember { mutableStateOf(1) }
    // bahin sa lapad nga una
    val pos = remember { Animatable(0.5f) }
    var showSheet by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var toastOn by remember { mutableStateOf(false) }

    fun go(t: Int) {
        tab = t
        scope.launch { pos.animateTo(if (t == 0) 1f else if (t == 2) 0f else 0.5f, tween(Motion.Slow, easing = IosEase)) }
    }

    fun dragTo(f: Float) {
        tab = 1
        scope.launch { pos.snapTo(f.coerceIn(0f, 1f)) }
    }

    fun save() {
        if (saving) return
        if (job.savedUri != null) {
            showSheet = true
            return
        }
        saving = true
        scope.launch {
            val t0 = System.currentTimeMillis()
            val uri = withContext(Dispatchers.IO) { saveToGallery(context, after) }
            // pa-drama sa spinner, kunwari kapoy
            val spent = System.currentTimeMillis() - t0
            if (spent < 650) delay(650 - spent)
            saving = false
            if (uri != null) {
                job.savedUri = uri
                onSaved()
                showSheet = true
            } else {
                Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(showSheet) {
        if (showSheet) {
            toastOn = true
            delay(2500)
            toastOn = false
        }
    }

    BackHandler(true) { if (showSheet) showSheet = false else onBack() }

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenTopBar(onBack = onBack, onClose = onBack) {
                SaveButton(
                    state = if (saving) 1 else if (job.savedUri != null) 2 else 0,
                    onClick = { save() }
                )
            }
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(14.dp))
                        .pointerInput(Unit) { detectTapGestures { o -> dragTo(o.x / size.width) } }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()
                                dragTo(change.position.x / size.width)
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        if (w < 1f || h < 1f) return@Canvas
                        val dst = IntSize(w.toInt().coerceAtLeast(1), h.toInt().coerceAtLeast(1))
                        val x = (pos.value * w).coerceIn(0f, w)
                        clipRect(0f, 0f, x, h) { drawImage(beforeImg, dstSize = dst) }
                        clipRect(x, 0f, w, h) { drawImage(afterImg, dstSize = dst) }
                        drawLine(Color.White, Offset(x, 0f), Offset(x, h), 2.dp.toPx())
                        val c = Offset(x, h / 2f)
                        drawCircle(Color(0x99000000), 23.dp.toPx(), c)
                        drawCircle(Color.White, 23.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                        val a = 6.dp.toPx()
                        val g = 7.dp.toPx()
                        val sw = 2.2.dp.toPx()
                        drawLine(Color.White, Offset(c.x - g, c.y - a), Offset(c.x - g - a, c.y), sw, StrokeCap.Round)
                        drawLine(Color.White, Offset(c.x - g - a, c.y), Offset(c.x - g, c.y + a), sw, StrokeCap.Round)
                        drawLine(Color.White, Offset(c.x + g, c.y - a), Offset(c.x + g + a, c.y), sw, StrokeCap.Round)
                        drawLine(Color.White, Offset(c.x + g + a, c.y), Offset(c.x + g, c.y + a), sw, StrokeCap.Round)
                    }
                    Tag(
                        "Before",
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .graphicsLayer { alpha = ((pos.value - 0.12f) / 0.1f).coerceIn(0f, 1f) }
                    )
                    Tag(
                        "After",
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .graphicsLayer { alpha = ((0.88f - pos.value) / 0.1f).coerceIn(0f, 1f) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            BottomPanel("Enhance-${job.title}") {
                ModeTile("Before", tab == 0, true, Modifier.weight(1f), { go(0) }) { Ico(ICO_IMAGE, 24.dp) }
                ModeTile("Before & After", tab == 1, true, Modifier.weight(1f), { go(1) }) { Ico(ICO_SPLIT, 24.dp) }
                ModeTile("After", tab == 2, true, Modifier.weight(1f), { go(2) }) { HdBadge("HD+") }
            }
        }

        BackHandler(enabled = showSheet) { showSheet = false }
        SheetHost(showSheet, onDismiss = { showSheet = false }) {
            Text("Saved to album", style = heading(20f, FontWeight.SemiBold, C.Text))
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                Image(
                    bitmap = afterImg,
                    contentDescription = "Saved photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                if (toastOn) {
                    Text(
                        "Picture has been saved to album.",
                        style = inter(16f, FontWeight.Medium, Color.White),
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xCC000000))
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                SHARE_TARGETS.forEach { t ->
                    Column(
                        Modifier
                            .weight(1f)
                            .pressable(0.94f) {
                                val u = job.savedUri
                                if (u != null) shareImage(context, u, t.pkg, t.name)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(54.dp).clip(CircleShape).background(t.color),
                            contentAlignment = Alignment.Center
                        ) { Ico(ICO_SHARE, 26.dp, Color.White) }
                        Spacer(Modifier.height(8.dp))
                        Text(t.name, style = inter(13f, FontWeight.Medium, C.Text), maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(54.dp)
                        .pressable(0.94f) { onBack() }
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0x55121316), CircleShape),
                    contentAlignment = Alignment.Center
                ) { Ico(ICO_HOME, 24.dp) }
            }
        }
    }
}

// ------- helper sa litrato

/** tono sa scenario, usba-usba */
private fun applyScenario(src: Bitmap, sc: Scenario): Bitmap {
    return try {
        val cm = android.graphics.ColorMatrix()
        cm.setSaturation(sc.sat)
        val c = sc.contrast
        val t = 128f * (1f - c)
        cm.postConcat(
            android.graphics.ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        android.graphics.Canvas(out).drawBitmap(src, 0f, 0f, paint)
        if (out !== src) src.recycle()
        out
    } catch (t: Throwable) {
        t.printStackTrace()
        src
    }
}

private fun shareImage(context: Context, uri: Uri, pkg: String?, name: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        if (pkg != null) {
            try {
                context.startActivity(Intent(send).setPackage(pkg))
                return
            } catch (t: android.content.ActivityNotFoundException) {
                Toast.makeText(context, "$name isn't installed", Toast.LENGTH_SHORT).show()
            }
        }
        context.startActivity(Intent.createChooser(send, "Share photo"))
    } catch (t: Throwable) {
        Toast.makeText(context, "Could not share", Toast.LENGTH_SHORT).show()
    }
}

// ------- helper sa data

private fun mediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) arrayOf(
        android.Manifest.permission.READ_MEDIA_IMAGES,
        android.Manifest.permission.READ_MEDIA_VIDEO
    ) else arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)

private fun hasMediaAccess(context: Context): Boolean = mediaPermissions().all {
    androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

private class LibRow(val item: LibItem, val date: Long)

private fun queryCollection(context: Context, base: Uri, isVideo: Boolean, folder: String, out: MutableList<LibRow>) {
    try {
        val sel: String
        val args: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sel = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            args = arrayOf("$folder/AIEnhancer%")
        } else {
            @Suppress("DEPRECATION")
            sel = "${MediaStore.MediaColumns.DATA} LIKE ?"
            args = arrayOf("%/$folder/AIEnhancer/%")
        }
        context.contentResolver.query(
            base,
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATE_ADDED),
            sel, args, "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            while (c.moveToNext()) {
                out.add(LibRow(LibItem(ContentUris.withAppendedId(base, c.getLong(idCol)), isVideo), c.getLong(dateCol)))
            }
        }
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

/** tanan gi-save, bag-o una */
private fun queryLibrary(context: Context): List<LibItem> {
    val rows = ArrayList<LibRow>()
    queryCollection(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, "Pictures", rows)
    queryCollection(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, "Movies", rows)
    rows.sortByDescending { it.date }
    return rows.map { it.item }
}

private fun loadThumb(context: Context, uri: Uri, side: Int, isVideo: Boolean = false): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            return context.contentResolver.loadThumbnail(uri, android.util.Size(side, side), null)
        } catch (t: Throwable) {
            // manual na lang, gi-stackoverflow na
        }
    }
    if (isVideo) {
        return try {
            val r = android.media.MediaMetadataRetriever()
            try {
                r.setDataSource(context, uri)
                r.getFrameAtTime(0)
            } finally {
                r.release()
            }
        } catch (t: Throwable) {
            null
        }
    }
    return try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longSide = max(info.size.width, info.size.height)
            if (longSide > side) {
                val f = side.toFloat() / longSide
                decoder.setTargetSize(
                    (info.size.width * f).roundToInt().coerceAtLeast(1),
                    (info.size.height * f).roundToInt().coerceAtLeast(1)
                )
            }
        }
    } catch (t: Throwable) {
        null
    }
}

private fun openMedia(context: Context, uri: Uri, isVideo: Boolean) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, if (isVideo) "video/*" else "image/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    } catch (t: Throwable) {
        Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
    }
}

private const val MAX_DISPLAY_SIDE = 1600

/** gamay nga kopya, luwas ra */
private fun downscaleForDisplay(src: Bitmap): Bitmap {
    val longSide = max(src.width, src.height)
    if (longSide <= MAX_DISPLAY_SIDE) return src
    val f = MAX_DISPLAY_SIDE.toFloat() / longSide
    return try {
        Bitmap.createScaledBitmap(
            src,
            (src.width * f).roundToInt().coerceAtLeast(1),
            (src.height * f).roundToInt().coerceAtLeast(1),
            true
        )
    } catch (t: Throwable) {
        src
    }
}

/** i-decode ug paliita, ikot-ikot */
private fun loadBitmap(context: android.content.Context, uri: Uri, maxSide: Int): Bitmap? {
    return try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longSide = max(info.size.width, info.size.height)
            if (longSide > maxSide) {
                val f = maxSide.toFloat() / longSide
                decoder.setTargetSize(
                    (info.size.width * f).roundToInt().coerceAtLeast(1),
                    (info.size.height * f).roundToInt().coerceAtLeast(1)
                )
            }
        }
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}

private fun saveToGallery(context: android.content.Context, bmp: Bitmap): Uri? {
    var uri: Uri? = null
    return try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "enhanced_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AIEnhancer")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val u = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        uri = u
        val stream = context.contentResolver.openOutputStream(u) ?: throw java.io.IOException("no stream")
        val ok = stream.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (!ok) throw java.io.IOException("compress failed")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            context.contentResolver.update(u, done, null, null)
        }
        u
    } catch (t: Throwable) {
        t.printStackTrace()
        // ayaw pasagdi ang sira, uy
        uri?.let { try { context.contentResolver.delete(it, null, null) } catch (_: Throwable) {} }
        null
    }
}
