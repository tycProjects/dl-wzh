package com.android.island.island

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.widget.Toast
import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Attachment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.withLink
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import com.android.island.ai.ImageGallery
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.island.IslandApplication
import com.android.island.ai.AiFilePickerActivity
import com.android.island.ai.AiSession
import com.android.island.core.IslandBus
import com.android.island.data.IslandSettings
import com.android.island.service.IslandAccessibilityService
import com.android.island.ui.theme.Tile
import com.android.island.ui.theme.Ui
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.PI
import kotlin.math.sin

/** High-contrast ink for the AI panel only. Liquid glass and other screens keep their own palette. */
private val AiInk = Color(0xFFEDF1FF)
private val AiSub = Color(0xFFA7B0CC)
private val AiErrorInk = Color(0xFFFF8A80)

/**
 * Robotic neon identity (requested: kesan robotik, neon, bercahaya).
 * Everything is panel-scoped so the rest of the island keeps Liquid Glass untouched.
 */
private val NeonCyan = Color(0xFF35E6FF)
private val NeonViolet = Color(0xFF8F7BFF)
private val NeonMagenta = Color(0xFFFA6ED5)
private val NeonGreen = Color(0xFF3DF0A8)
private val NeonBgTop = Color(0xFF12172A)
private val NeonBgDeep = Color(0xFF0A0D16)
private val NeonSurface = Color(0xFF151A2C)
private val NeonSurface2 = Color(0xFF1A2036)
private val NeonBorder = Color(0xFF2C3354)
private val NeonDanger = Color(0xFFFF7A6E)

/** Reduce-motion gate for the panel's message animations. Neon looks are the same in every mode. */
@Composable
private fun neonMotionAllowed(reduceMotion: Boolean): Boolean {
    return !reduceMotion
}


/**
 * The AI assistant card, shown only inside the EXPANDED island.
 *
 * UX rules:
 *  - Summoning the AI grabs the keyboard once, so summon-to-type is one gesture.
 *  - The card is dismissed by the close button or an outside tap, which also drops the
 *    keyboard; typing or picking a file guards against an accidental outside-close.
 *  - "Screenshot Layar" stages a real foreground-app screenshot in the composer; the user writes the prompt before sending.
 */
@Composable
fun AiPanel(
    open: Boolean,
    widthDp: Int,
    cornerRadiusDp: Int,
    /**
     * STAGE 72: shared height cap (dp) from WindowTuning.aiPanelCapDp — the same number the host
     * window and the touch window use. 0 = uncapped. The panel clamps its measured height to this
     * value so the chip row and command field can never fall outside the touchable surface, no
     * matter how far the height slider is pushed ("mentok").
     */
    maxHeightDp: Int = 0,
    onHeightDp: (Int) -> Unit,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val settingsRepo = remember {
        (context.applicationContext as? IslandApplication)?.settingsRepository
    }
    val settingsFallback = remember { MutableStateFlow(IslandSettings()) }
    // settingsRepo.settings is a cold Flow: collectAsState needs an initial snapshot here.
    val settings by (settingsRepo?.settings ?: settingsFallback)
        .collectAsState(initial = IslandSettings())

    val messages by AiSession.messages.collectAsState()
    val streamText by AiSession.streamText.collectAsState()
    val status by AiSession.status.collectAsState()
    val pending by AiSession.pending.collectAsState()
    val typing by IslandBus.aiTyping.collectAsState()
    val busy = status != AiSession.Status.IDLE

    // Give the panel a real bounded height. The message list takes only the
    // remaining space; header, actions and the command field remain pinned.
    val screenHdp = LocalConfiguration.current.screenHeightDp
    // Space really free: screen minus status bar, nav bar and the keyboard (when open).
    val topInsetDp = with(density) { WindowInsets.statusBars.getTop(this).toDp().value.toInt() }
    val bottomInsetDp = with(density) { WindowInsets.navigationBars.getBottom(this).toDp().value.toInt() }
    val imeDp = with(density) { WindowInsets.ime.getBottom(this).toDp().value.toInt() }
    val freeHdp = (screenHdp - topInsetDp - bottomInsetDp - imeDp - 24).coerceAtLeast(200)
    // The assistant is a full reading/work surface. Use nearly all usable screen
    // height so users can read longer answers without repeatedly scrolling the small panel.
    // Insets and IME are excluded; the root overlay applies the final capsule/offset safety cap.
    // STAGE 72: the final height is clamped to the shared cap (maxHeightDp), which is exactly the
    // height IslandRoot gives the host window and the accessibility service gives the touch
    // window. This fixes the bug where a maxed-out panel measured ~0.94 of the screen while the
    // touch window stopped at 0.78 — the chips were visible but could not receive touches.
    val computedPanelHdp = (freeHdp * 0.94f)
        .toInt()
        .coerceAtMost(900)
    val hostCapDp = maxHeightDp.takeIf { it > 0 } ?: computedPanelHdp
    val panelHeightDp = computedPanelHdp
        .coerceAtMost(hostCapDp)
        .coerceAtLeast(minOf(320, freeHdp).coerceAtMost(hostCapDp))

    val listState = rememberLazyListState()
    var confirmClear by remember { mutableStateOf(false) }
    val reduceMotion = remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
    // STAGE 72: the panel's breathing animations are retired per the user's request. Glow stays,
    // but constant (aiStaticRim, static header core). Indicator dots lost their pulse entirely.
    // One drift clock (the One UI wave divider) remains, and the whole loop battery cost of the
    // old rim/header clocks is gone.
    val motionAlive = neonMotionAllowed(reduceMotion)

    fun openComposer(voice: Boolean = false) {
        IslandBus.beginExternalAi()
        val hint = if (pending.any { !it.contextText.isNullOrBlank() }) {
            "Tulis perintah untuk screenshot…"
        } else {
            "Tanya apa saja…"
        }
        runCatching {
            context.applicationContext.startActivity(
                com.android.island.ai.AiComposerActivity.intent(
                    context.applicationContext,
                    hint = hint,
                    voice = voice
                )
            )
        }.onFailure { IslandBus.endExternalAi() }
    }

    LaunchedEffect(Unit) {
        AiSession.ensureLoaded(context.applicationContext)
    }
    DisposableEffect(Unit) {
        IslandBus.setAiTyping(true)
        onDispose {
            // Keep the typing flag while the composer or file picker is still open on top.
            if (!IslandBus.externalAiActive) IslandBus.setAiTyping(false)
            IslandAccessibilityService.setFocusable(false)
        }
    }

    // AI panel is taller/narrower than media cards; high radius clips header, chips and input.
    // Cap more tightly and give content a larger inset so UI never sits under the curve.
    val r = cornerRadiusDp.coerceIn(12, 28)
    val shape = RoundedCornerShape(r.dp)
    val contentInsetDp = (r * 0.42f).toInt().coerceIn(12, 16)
    Column(
        modifier = Modifier
            .width(widthDp.dp)
            .height(panelHeightDp.dp)
            .clip(shape)
            // AMOLED neon substrate: deep space gradient, fully opaque so the app behind never
            // bleeds through. Light is pooled INSIDE the edges (drawn only, no layout cost).
            .background(Brush.verticalGradient(listOf(NeonBgTop, NeonBgDeep)))
            .neonEdgeGlow()
            // STAGE 72: constant neon rim light (no breathing pulse) riding the panel border.
            .aiStaticRim()
            .border(1.dp, Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.55f), NeonViolet.copy(alpha = 0.55f))), shape)
            .onSizeChanged { size ->
                onHeightDp(with(density) { size.height.toDp() }.value.toInt())
            }
            // Geometric cut of a rounded rect is ~0.29r; use a bit more so text/icons stay clear.
            .padding(contentInsetDp.dp)
    ) {
        // ---- Header ----------------------------------------------------------
        AiHeader(busy = busy, modelName = settings.aiModel, onCollapse = onCollapse)
        Spacer(Modifier.height(8.dp))
        HeaderActions(
            busy = busy,
            screenOn = settings.aiScreenAnalysis,
            confirmClear = confirmClear,
            onScreen = {
                if (settings.aiScreenAnalysis) AiSession.captureScreenToPending()
                else AiSession.explainScreenAnalysisOff()
            },
            onAttach = {
                IslandBus.beginExternalAi()
                val ok = runCatching {
                    val appCtx = context.applicationContext
                    appCtx.startActivity(
                        android.content.Intent(appCtx, AiFilePickerActivity::class.java)
                            .addFlags(
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                    android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
                            )
                    )
                }.isSuccess
                if (!ok) {
                    IslandBus.endExternalAi()
                    android.widget.Toast.makeText(context, "Gagal membuka pemilih file.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onClear = {
                if (confirmClear) {
                    AiSession.clearHistory()
                    confirmClear = false
                } else {
                    confirmClear = true
                }
            },
            onExport = {
                runCatching {
                    val exportFile = AiSession.writeChatExport(context.applicationContext)
                    val contentUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        exportFile
                    )
                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Percakapan Android Island")
                        putExtra(android.content.Intent.EXTRA_TEXT, "Ekspor percakapan AI Android Island dalam format JSON.")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Bagikan percakapan").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { error ->
                    android.widget.Toast.makeText(context, "Ekspor gagal: ${error.message ?: error.javaClass.simpleName}", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onExportSave = {
                runCatching {
                    AiSession.saveChatExportToDownloads(context.applicationContext)
                }.onSuccess { message ->
                    android.widget.Toast.makeText(context, "Percakapan berhasil disimpan. $message", android.widget.Toast.LENGTH_LONG).show()
                }.onFailure { error ->
                    android.widget.Toast.makeText(context, "Gagal menyimpan: ${error.message ?: error.javaClass.simpleName}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        )

        // One UI-style fluid wave under the header (draw-phase only, one slow clock).
        NeonWaveDivider(animate = false)

        Spacer(Modifier.height(8.dp))

        // ---- Not configured hint ---------------------------------------------
        if (settings.aiApiKey.isBlank() &&
            !settings.aiBaseUrl.contains("127.0.0.1") && !settings.aiBaseUrl.contains("localhost") &&
            !settings.aiBaseUrl.contains("192.168.") && !settings.aiBaseUrl.contains("10.") &&
            messages.isEmpty() && !busy) {
            HintBubble(
                text = "Asisten AI belum terhubung.\n\nBuka Android Island → bagian Asisten AI → pilih penyedia → masukkan API key → tekan ‘Uji koneksi’. Setelah tersambung, kamu bisa langsung mulai bertanya.\n\nUntuk penyedia yang tersedia, alamat server dan nama model disiapkan otomatis. Analisis layar bersifat opsional; chat biasa tidak memerlukannya."
            )
            Spacer(Modifier.height(10.dp))
        }

        // ---- Chat --------------------------------------------------------------
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(NeonSurface.copy(alpha = 0.72f)),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty() && !busy && streamText.isEmpty()) {
                item(key = "welcome") {
                    HintBubble(
                        text = "Halo! Tanyakan apa saja.\n\n• \"Berita hari ini\" — daftar rapi dengan jam WIB dan tombol Baca Sumber.\n• \"Cari lebih dalam tentang X\" — telusuri multi-ronde, lengkap dengan kutipan sumber.\n• Ketuk \"Tulis perintah\" untuk mengetik, atau pakai chip di bawah untuk screenshot dan lampiran. Riwayat chat disimpan otomatis (maks 30 pesan).",
                        animateIn = motionAlive
                    )
                }
            }
            items(count = messages.size, key = { i -> messages[i].id }) { i ->
                MessageBubble(message = messages[i], animateIn = motionAlive)
            }
            if (streamText.isNotEmpty()) {
                item(key = "streaming") {
                    // Live step trail: the steps of the running request appear as they finish.
                    val (liveSteps, liveBody) = splitSteps(streamText)
                    Column {
                        if (liveSteps.isNotEmpty()) {
                            StepTrail(StepTrailModel.from(liveSteps, running = busy))
                            Spacer(Modifier.height(6.dp))
                        }
                        if (liveBody.isNotBlank() || liveSteps.isEmpty()) {
                            Bubble(text = if (liveSteps.isEmpty()) streamText else liveBody, isUser = false, streaming = true)
                        }
                    }
                }
            } else if (busy && status == AiSession.Status.READING_SCREEN) {
                item(key = "reading") {
                    BusyLine(text = "Membaca layar", reduceMotion = reduceMotion)
                }
            } else if (busy && status == AiSession.Status.UNDERSTANDING) {
                item(key = "understanding") {
                    BusyLine(text = "Memahami perintah", reduceMotion = reduceMotion)
                }
            } else if (busy) {
                item(key = "thinking") {
                    BusyLine(text = "Memproses", reduceMotion = reduceMotion)
                }
            }
        }

        // ---- Reply bar (ala balasan WhatsApp): satu kolom, tanpa deretan chip ----
        Spacer(Modifier.height(8.dp))
        if (busy) {
            Chip(
                text = "Berhenti",
                icon = Icons.Rounded.Stop,
                color = NeonDanger,
                onClick = { AiSession.cancel() }
            )
        } else {
            ReplyBar(
                hint = if (pending.any { !it.contextText.isNullOrBlank() }) {
                    "Tulis perintah untuk screenshot…"
                } else {
                    "Tanya apa saja…"
                },
                onOpen = { openComposer() },
                onVoice = { openComposer(voice = true) }
            )
        }
        // ---- Pending attachments ------------------------------------------------
        if (pending.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Lampiran (ketuk × untuk hapus):",
                color = AiSub,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
            ) {
                items(count = pending.size, key = { i -> pending[i].name + i }) { i ->
                    val p = pending[i]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonSurface2)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            if (p.isImage) Icons.Rounded.Visibility else Icons.Rounded.Attachment,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            p.name,
                            color = AiInk,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 110.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Ui.IconBg)
                                .clickable { AiSession.removePending(p.name) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Hapus lampiran",
                                tint = AiSub,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }


        LaunchedEffect(confirmClear) {
            if (confirmClear) {
                delay(2600)
                confirmClear = false
            }
        }

    }

    // Keep the newest message visible.
    LaunchedEffect(messages.size, streamText.isNotEmpty()) {
        val target = messages.size + (if (streamText.isNotEmpty()) 1 else 0) - 1
        if (target >= 0) runCatching { listState.animateScrollToItem(target) }
    }
}

/** Deretan aksi di bawah header; digeser mendatar. Tahan Ekspor untuk menyimpan ke Downloads. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeaderActions(
    busy: Boolean,
    screenOn: Boolean,
    confirmClear: Boolean,
    onScreen: () -> Unit,
    onAttach: () -> Unit,
    onClear: () -> Unit,
    onExport: () -> Unit,
    onExportSave: () -> Unit,
) {
    // Saat AI sibuk, tombol Berhenti sudah tampil di bawah; header kosong agar tidak ganda.
    if (busy) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        ActionPill("Layar", Icons.Rounded.Visibility, if (screenOn) NeonCyan else Tile.Gray, onScreen)
        ActionPill("Lampiran", Icons.Rounded.Attachment, NeonGreen, onAttach)
        ActionPill(if (confirmClear) "Yakin?" else "Hapus", Icons.Rounded.DeleteOutline, if (confirmClear) Tile.Red else Tile.Gray, onClear)
        ActionPill("Ekspor", Icons.Rounded.Download, NeonViolet, onExport, onLongClick = onExportSave)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionPill(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val source = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentWidth()
            .heightIn(min = 32.dp)
            .pressScaleWith(source, pressedScale = 0.94f)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = {
                    IslandBus.setAiTyping(true)
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        IslandBus.setAiTyping(true)
                        it()
                    }
                }
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = AiInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Kolom balasan satu baris: ketuk untuk mengetik, ikon mikrofon untuk bicara, panah untuk kirim. */
@Composable
private fun ReplyBar(hint: String, onOpen: () -> Unit, onVoice: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(NeonSurface.copy(alpha = 0.92f))
            .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .clickable(interactionSource = source, indication = null) {
                IslandBus.setAiTyping(true)
                onOpen()
            }
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Text(
            hint,
            color = AiSub,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    IslandBus.setAiTyping(true)
                    onVoice()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = "Bicara", tint = NeonViolet, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.9f))
                .clickable {
                    IslandBus.setAiTyping(true)
                    onOpen()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Kirim", tint = NeonBgDeep, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RowScope.ChipSlot(content: @Composable () -> Unit) {
    Box(Modifier.weight(1f)) { content() }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Chip(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    // Neon outline chip: translucent fill, glowing border, springy press scale (no overlap risk:
    // every chip stays inside its equal-width ChipSlot, feedback is a draw/graphics-layer only).
    val source = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .pressScaleWith(source, pressedScale = 0.94f)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = {
                    // Keep the AI card from collapsing while the chip action runs.
                    IslandBus.setAiTyping(true)
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        IslandBus.setAiTyping(true)
                        it()
                    }
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = AiInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(message: AiSession.ChatMessage, animateIn: Boolean = false) {
    val context = LocalContext.current
    val (stepLines, bodyText) = remember(message.text) { splitSteps(message.text) }
    // Stage 68: every message arrives with a small lift-and-settle pop (one soft spring, once).
    Column(
        Modifier.enterOnce(
            key = message.id,
            enabled = animateIn,
            distanceDp = 10f,
            fromScale = 0.92f
        )
    ) {
        if (stepLines.isNotEmpty()) {
            StepTrail(StepTrailModel.from(stepLines, running = false))
            Spacer(Modifier.height(6.dp))
        }
        if (bodyText.isNotBlank() || stepLines.isEmpty()) Bubble(
            text = if (stepLines.isEmpty()) message.text else bodyText,
            isUser = message.role == "user",
            streaming = false,
            isError = message.isError,
            onCopy = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("AI Island", message.text))
                Toast.makeText(context, "Disalin", Toast.LENGTH_SHORT).show()
            }
        )
        if (message.role == "assistant" && !message.isError && message.text.isNotBlank()) {
            Row(
                modifier = Modifier.padding(start = 4.dp, top = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.VolumeUp,
                    contentDescription = "Bacakan jawaban",
                    tint = AiSub,
                    modifier = Modifier.size(17.dp).clickable {
                        // Link annotations must not be read aloud as raw URLs.
                        com.android.island.ai.voice.SpeechOutput.speak(
                            context,
                            com.android.island.ai.NewsFormatter.stripLinksForSpeech(message.text)
                        )
                    }
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Rounded.Stop,
                    contentDescription = "Hentikan suara",
                    tint = AiSub,
                    modifier = Modifier.size(16.dp).clickable {
                        com.android.island.ai.voice.SpeechOutput.stop()
                    }
                )
            }
        }
        message.imagePath?.let { path ->
            val bitmap = remember(path) { decodeSampledImage(path) }
            if (bitmap != null) {
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "Gambar yang dibuat asisten",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Text(
                    "Simpan ke galeri",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable {
                            val ok = ImageGallery.save(context, path)
                            Toast.makeText(
                                context,
                                if (ok) "Gambar disimpan ke galeri" else "Gagal menyimpan gambar",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                )
            }
        }
        if (message.attachmentNames.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                message.attachmentNames.joinToString(", "),
                color = AiSub,
                fontSize = 10.sp,
                maxLines = 2
            )
        }
        if (message.files.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            message.files.forEach { f ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ui.IconBg)
                        .clickable {
                            val msg = AiSession.saveDownloadable(context.applicationContext, f)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Rounded.Attachment, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Simpan ${f.name}", color = NeonCyan, fontSize = 11.sp, maxLines = 1)
                }
            }
            if (message.files.size > 1) {
                Text(
                    "Simpan semua ke Download (ZIP)",
                    color = NeonViolet,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable {
                            val msg = AiSession.saveAsZip(context.applicationContext, message.files)
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                        .padding(top = 2.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Bubble(
    text: String,
    isUser: Boolean,
    streaming: Boolean,
    isError: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val bg = when {
        isUser -> Ui.Primary
        isError -> Color(0xFF3B2025)
        else -> NeonSurface2
    }
    val ink = if (isUser) Ui.OnPrimary else if (isError) Tile.Red else AiInk
    // Normalize assistant output only; user text and code-like error payloads remain untouched.
    val displayText = remember(text, isUser, isError) {
        if (!isUser && !isError) com.android.island.ai.AssistantTextFormatter.format(text) else text
    }
    // Markdown links ("[Baca Sumber](url)") become short tappable labels; long URLs stay hidden.
    val annotated = remember(displayText, streaming) {
        buildLinkedText(if (streaming) "$displayText ▍" else displayText) { url -> openSourceUrl(context, url) }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            Modifier
                .weight(1f, fill = false)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 12.dp
                    )
                )
                .background(bg)
                .then(
                    if (!isUser && !isError) Modifier.border(
                        1.dp,
                        NeonBorder.copy(alpha = 0.7f),
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 12.dp
                        )
                    ) else Modifier
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Word-by-word selection: long-press a word, drag the handles, copy exactly that part.
            // Not while streaming, because the text changes on every chunk. The copy icon beside the
            // bubble still copies the whole message.
            val messageText: @Composable () -> Unit = {
                if (annotated != null) {
                    Text(
                        annotated,
                        color = ink,
                        fontSize = if (isUser || isError) 13.sp else 15.sp,
                        lineHeight = if (isUser || isError) 17.sp else 23.sp,
                        fontFamily = if (isUser || isError) null else FontFamily.Serif
                    )
                } else {
                    Text(
                        if (streaming) "$displayText ▍" else displayText,
                        color = ink,
                        fontSize = if (isUser || isError) 13.sp else 15.sp,
                        lineHeight = if (isUser || isError) 17.sp else 23.sp,
                        fontFamily = if (isUser || isError) null else FontFamily.Serif
                    )
                }
            }
            if (streaming) messageText() else SelectionContainer { messageText() }
        }
        if (onCopy != null && !streaming && text.isNotBlank()) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = "Salin",
                tint = AiSub,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onCopy() }
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Rounded.TextFields,
                contentDescription = "Pilih kata",
                tint = AiSub,
                modifier = Modifier
                    .size(16.dp)
                    .clickable {
                        IslandBus.setAiTyping(true)
                        AiTextSelectActivity.open(context, text)
                    }
            )
        }
    }
}

/** One tap on "Baca Sumber" opens the publisher page; only https links pass. */
private fun openSourceUrl(context: Context, url: String) {
    if (!url.startsWith("https://")) return
    runCatching {
        context.applicationContext.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        Toast.makeText(context, "Tidak ada browser untuk membuka sumber.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Turns `[label](url)` markdown into a styled tappable span. Returns null when the text has no
 * links, so ordinary messages keep the exact previous rendering path (and its long-press copy).
 */
private fun buildLinkedText(text: String, onOpen: (String) -> Unit): AnnotatedString? {
    if (!text.contains("[")) return null
    val link = Regex("""\[([^\]]+)]\((https?://[^)\s]+)\)""")
    val matches = link.findAll(text).toList()
    if (matches.isEmpty()) return null
    val linkStyle = TextLinkStyles(SpanStyle(color = NeonCyan, fontWeight = FontWeight.SemiBold))
    return buildAnnotatedString {
        var cursor = 0
        for (m in matches) {
            append(text.substring(cursor, m.range.first))
            val url = m.groupValues[2]
            // A real link annotation: a tap opens it, and the text around it stays selectable.
            withLink(LinkAnnotation.Url(url, linkStyle, LinkInteractionListener { onOpen(url) })) {
                append(m.groupValues[1])
            }
            cursor = m.range.last + 1
        }
        append(text.substring(cursor))
    }
}

/** Separates the "Langkah n: ..." lines of a reply from the rest of its text. */
private fun splitSteps(text: String): Pair<List<String>, String> {
    val lines = text.lines()
    val steps = lines.filter { it.startsWith("Langkah ") }
    val body = lines.filterNot { it.startsWith("Langkah ") }.joinToString("\n").trim()
    return steps to body
}

/**
 * Jejak langkah: one vertical thread through the steps. STAGE 72: the active dot no longer
 * breathes (the user asked indicator animations to be removed) — a calm solid dot marks the
 * active step; done/failed dots keep their colors.
 */
@Composable
private fun StepTrail(steps: List<StepTrailModel.Step>) {
    val ember = Color(0xFFFF8A65)
    Column(Modifier.fillMaxWidth().padding(start = 4.dp)) {
        steps.forEachIndexed { i, step ->
            val failed = step.state == StepTrailModel.State.FAILED
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(14.dp)) {
                    val active = step.state == StepTrailModel.State.ACTIVE
                    val dotColor = if (failed) ember else NeonCyan
                    Box(
                        Modifier
                            .size(9.dp)
                            .drawBehind {
                                if (active) {
                                    // Static emphasis: solid core plus one constant soft ring.
                                    drawCircle(dotColor.copy(alpha = 0.28f), radius = size.minDimension / 2f * 1.9f)
                                }
                                drawCircle(dotColor, radius = size.minDimension / 2f)
                            }
                    )
                    if (i < steps.lastIndex) {
                        Box(Modifier.width(1.5.dp).height(18.dp).background(NeonCyan.copy(alpha = 0.30f)))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    step.text,
                    color = if (failed) ember else AiInk,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun HintBubble(text: String, animateIn: Boolean = false) {
    Text(
        text,
        color = AiSub,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .enterOnce(key = "hint", enabled = animateIn, distanceDp = 10f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonSurface2)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

/**
 * Neon header: robotic core cell (static glow), monospace identity, live status dot and the
 * close button with its own jelly press. STAGE 72: the breathing pulse is removed per the
 * user's request — the core keeps a constant glow and the indicator dot is a plain solid dot
 * with no halo animation at all.
 */
@Composable
private fun AiHeader(
    busy: Boolean,
    modelName: String,
    onCollapse: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(20.dp)
                .drawBehind {
                    // Constant glow: the same pooled light the breathing version peaked at.
                    drawCircle(NeonCyan.copy(alpha = 0.20f), radius = size.width / 2f)
                    drawCircle(NeonCyan.copy(alpha = 0.16f), radius = size.width * 0.36f)
                    drawCircle(NeonCyan, radius = size.width * 0.21f)
                }
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "ASISTEN AI",
                color = AiInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.6.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotColor = if (busy) NeonMagenta else NeonGreen
                Box(
                    Modifier
                        .size(5.dp)
                        .drawBehind {
                            // Indicator: no breathing, no halo — just the solid status dot.
                            drawCircle(dotColor, radius = size.minDimension / 2f)
                        }
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (busy) "MEMPROSES" else modelName.ifBlank { "SIAP" },
                    color = AiSub,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
        val closeSource = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(30.dp)
                .pressScaleWith(closeSource, pressedScale = 0.88f)
                .pressHalo(closeSource, NeonCyan)
                .clip(RoundedCornerShape(10.dp))
                .background(NeonSurface2)
                .border(1.dp, NeonBorder.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                .clickable(interactionSource = closeSource, indication = null) { onCollapse() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, "Tutup", tint = AiInk, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * One UI-style fluid wave divider under the header: two slow sine layers (cyan over violet).
 * Cost: one drift clock at the device tier's tick rate, read in the draw phase, so nothing
 * recomposes; paused for reduced motion, static glass, and when the screen is not interactive
 * (rememberDriftPhase already gates the clock). Drawn strictly inside its own 12dp lane.
 */
@Composable
private fun NeonWaveDivider(animate: Boolean) {
    val phase by rememberDriftPhase(animate = animate, periodMs = 9_000L)
    // One reusable Path for both sine layers: drawing used to allocate two Path objects
    // per frame (120 short-lived objects per minute at the 30 Hz tick) that the GC had
    // to collect while the panel is open. reset() rebuilds the same geometry for free.
    val wavePath = remember { Path() }
    Box(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            .drawBehind {
                val midY = size.height / 2f
                drawSine(wavePath, midY, size.height * 0.30f, size.width / 1.6f, phase * 2f * PI.toFloat(), NeonCyan, 0.32f, 1.6.dp.toPx())
                drawSine(wavePath, midY, size.height * 0.42f, size.width / 2.3f, (phase * 1.35f + 0.4f) * 2f * PI.toFloat(), NeonViolet, 0.22f, 1.1.dp.toPx())
            }
    )
}

private fun DrawScope.drawSine(
    path: Path,
    midY: Float,
    amp: Float,
    lambda: Float,
    phase: Float,
    color: Color,
    alpha: Float,
    strokePx: Float
) {
    path.reset()
    val step = (size.width / 60f).coerceAtLeast(4f)
    var x = 0f
    path.moveTo(0f, midY + amp * sin(2f * PI.toFloat() * x / lambda + phase))
    while (x < size.width) {
        x = (x + step).coerceAtMost(size.width)
        path.lineTo(x, midY + amp * sin(2f * PI.toFloat() * x / lambda + phase))
    }
    drawPath(path, color.copy(alpha = alpha), style = Stroke(width = strokePx, cap = StrokeCap.Round))
}

/** Neon light pooled inside the panel edges; drawn only, never leaves the clipped bounds.
 *  Both gradients are cached per size (drawWithCache), so any invalidation redraws with the
 *  same Brush objects instead of building two new ones each time. */
private fun Modifier.neonEdgeGlow(): Modifier {
    return this.drawWithCache {
        val corner = CornerRadius(18.dp.toPx())
        val top = Brush.verticalGradient(
            listOf(NeonCyan.copy(alpha = 0.16f), Color.Transparent),
            startY = 0f,
            endY = size.height * 0.14f
        )
        val bottom = Brush.verticalGradient(
            listOf(Color.Transparent, NeonViolet.copy(alpha = 0.12f)),
            startY = size.height * 0.86f,
            endY = size.height
        )
        onDrawBehind {
            drawRoundRect(brush = top, cornerRadius = corner)
            drawRoundRect(brush = bottom, cornerRadius = corner)
        }
    }
}

/**
 * STAGE 72: constant neon rim light riding the panel border (cyan crest flowing into violet).
 * The breathing pulse is retired (user request: "animasi bernafasnya dihapus, ganti glow biasa");
 * the same peak brightness is now drawn statically with zero per-frame work. Static glass keeps
 * the flat renderer untouched.
 */
private fun Modifier.aiStaticRim(): Modifier {
    return this.drawWithCache {
        val rimBrush = Brush.verticalGradient(
            listOf(NeonCyan.copy(alpha = 0.26f), NeonViolet.copy(alpha = 0.20f))
        )
        onDrawBehind {
            drawRoundRect(
                brush = rimBrush,
                topLeft = Offset(1f, 1f),
                size = Size(size.width - 2f, size.height - 2f),
                cornerRadius = CornerRadius(18.dp.toPx()),
                style = Stroke(width = 2f),
                alpha = 0.72f
            )
        }
    }
}

/** Busy line: text plus three static neon dots (STAGE 72: the pulse is gone, glow stays). */
@Composable
private fun BusyLine(text: String, reduceMotion: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NeonTypingDots()
        Spacer(Modifier.width(8.dp))
        Text(
            "$text…",
            color = AiSub,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontFamily = FontFamily.Serif
        )
    }
}

/** Three neon dots with a constant soft glow (no breathing clock — indicator stays calm). */
@Composable
private fun NeonTypingDots() {
    Box(
        Modifier
            .size(width = 30.dp, height = 10.dp)
            .drawBehind {
                val r = 2.2.dp.toPx()
                val gap = 9.dp.toPx()
                repeat(3) { i ->
                    drawCircle(
                        NeonCyan.copy(alpha = 0.28f),
                        radius = r * 1.9f,
                        center = Offset(r + i * gap, size.height / 2f)
                    )
                    drawCircle(
                        NeonCyan,
                        radius = r,
                        center = Offset(r + i * gap, size.height / 2f)
                    )
                }
            }
    )
}

/**
 * Decodes a generated picture at a screen-sized resolution in RGB_565. Full-size ARGB decoding of a
 * model image costs many megabytes per bubble, and every bubble that has been composed keeps its copy.
 */
private fun decodeSampledImage(path: String, maxEdge: Int = 900): androidx.compose.ui.graphics.ImageBitmap? {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxEdge && bounds.outHeight / (sample * 2) >= maxEdge) sample *= 2
    val opts = android.graphics.BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    return android.graphics.BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
}

