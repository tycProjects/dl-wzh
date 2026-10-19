package com.qrstudio.app.ui.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
// AUTO-REPAIR: disabled -- unresolved reference 'ImageVector' (was: import androidx.compose.ui.graphics.ImageVector)
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.qrstudio.app.data.ScanFormats
import com.qrstudio.app.data.ScanRecord
import com.qrstudio.app.ui.components.SectionCard
import com.qrstudio.app.ui.theme.BorderColor
import com.qrstudio.app.ui.theme.CardBg
import com.qrstudio.app.ui.theme.Danger
import com.qrstudio.app.ui.theme.Emerald
import com.qrstudio.app.ui.theme.TealAccent
import com.qrstudio.app.ui.theme.TextMuted
import com.qrstudio.app.util.ExportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanScreen(vm: ScanViewModel, onCreateFrom: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    LaunchedEffect(granted) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val result by vm.result.collectAsStateWithLifecycle()
    val paused by vm.scanningPaused.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    val imageScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val image = InputImage.fromFilePath(context, uri)
                    BarcodeScanning.getClient().process(image)
                        .addOnSuccessListener { codes ->
                            val first = codes.firstOrNull()
                            if (first != null) {
                                vm.onBarcode(first.rawValue, first.format)
                            } else {
                                vm.notify("لم يتم العثور على أي رمز في هذه الصورة")
                            }
                        }
                        .addOnFailureListener { vm.notify("تعذر تحليل الصورة") }
                } catch (e: Exception) {
                    vm.notify("تعذر قراءة الصورة المختارة")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (granted) {
            CameraArea(vm = vm, paused = paused)
            OutlinedButton(
                onClick = { imageScanLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("مسح رمز من صورة محفوظة")
            }
        } else {
            PermissionCard { permissionLauncher.launch(Manifest.permission.CAMERA) }
        }
        HistorySection(
            history = history,
            onClear = vm::clearHistory,
            onExport = { exportHistory(context, history) },
        )
    }

    result?.let { res ->
        ModalBottomSheet(
            onDismissRequest = vm::dismissResult,
            containerColor = CardBg,
        ) {
            ResultSheet(
                result = res,
                onCopy = {
                    clipboard.setText(AnnotatedString(res.rawValue))
                    vm.notify("تم نسخ المحتوى")
                },
                onOpen = {
                    if (!ExportUtils.openUri(context, res.rawValue)) {
                        vm.notify("لا يمكن فتح هذا النوع من المحتوى")
                    }
                },
                onShare = {
                    ExportUtils.shareText(context, res.rawValue, "مشاركة نتيجة المسح")
                },
                onCreate = {
                    vm.dismissResult()
                    onCreateFrom(res.rawValue)
                },
                onClose = vm::dismissResult,
            )
        }
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

@Composable
private fun CameraArea(vm: ScanViewModel, paused: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lens by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var torch by remember { mutableStateOf(false) }
    val camera = remember { mutableStateOf<Camera?>(null) }
    val previewViewRef = remember { arrayOfNulls<PreviewView>(1) }
    val pausedState = rememberUpdatedState(paused)

    Box(
        Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black),
    ) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    previewViewRef[0] = this
                }
            },
        )
        DisposableEffect(lens, paused) {
            val future = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
            val listener = Runnable {
                val provider = future.get()
                val pv = previewViewRef[0] ?: return@Runnable
                provider.unbindAll()
                val selector = CameraSelector.Builder().requireLensFacing(lens).build()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
                val useCases = mutableListOf<UseCase>(preview)
                if (!pausedState.value) {
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(executor, QrBarcodeAnalyzer { raw, format -> vm.onBarcode(raw, format) })
                    useCases.add(analysis)
                }
                camera.value = try {
                    provider.bindToLifecycle(lifecycleOwner, selector, *useCases.toTypedArray())
                } catch (e: Exception) {
                    null
                }
            }
            future.addListener(listener, executor)
            onDispose { runCatching { future.get().unbindAll() } }
        }
        LaunchedEffect(torch) {
            camera.value?.cameraControl?.enableTorch(torch)
        }

        CornerBrackets()
        LaserLine()
        CameraStatus()

        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RoundIconButton(
                icon = if (torch) Icons.Filled.FlashOff else Icons.Filled.FlashOn,
                description = "تبديل الفلاش",
            ) { torch = !torch }
            RoundIconButton(
                icon = Icons.Filled.FlipCameraAndroid,
                description = "تبديل الكاميرا",
            ) {
                lens = if (lens == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
            }
        }
    }
}

@Composable
private fun BoxScope.CornerBrackets() {
    Canvas(Modifier.matchParentSize().padding(36.dp)) {
        val stroke = 4.dp.toPx()
        val r = 16.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(TealAccent, Offset(r, 0f), Offset(0f, 0f), stroke, StrokeCap.Round)
        drawLine(TealAccent, Offset(0f, 0f), Offset(0f, r), stroke, StrokeCap.Round)
        drawLine(TealAccent, Offset(w - r, 0f), Offset(w, 0f), stroke, StrokeCap.Round)
        drawLine(TealAccent, Offset(w, 0f), Offset(w, r), stroke, StrokeCap.Round)
        drawLine(TealAccent, Offset(0f, h - r), Offset(0f, h), stroke, StrokeCap.Round)
        drawLine(TealAccent, Offset(0f, h), Offset(r, h), stroke, StrokeCap.Round)
        drawLine(TealAccent, Offset(w - r, h), Offset(w, h), stroke, StrokeCap.Round)
        drawLine(TealAccent, Offset(w, h - r), Offset(w, h), stroke, StrokeCap.Round)
    }
}

@Composable
private fun BoxScope.LaserLine() {
    val transition = rememberInfiniteTransition(label = "laser")
    val progress by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "laserProgress",
    )
    BoxWithConstraints(Modifier.matchParentSize()) {
        val h = constraints.maxHeight.toFloat()
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.72f)
                .height(3.dp)
                .graphicsLayer { translationY = (progress - 0.5f) * h * 0.55f }
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, TealAccent, Color.Transparent),
                    ),
                ),
        )
    }
}

@Composable
private fun BoxScope.CameraStatus() {
    val transition = rememberInfiniteTransition(label = "statusDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "statusAlpha",
    )
    Row(
        Modifier
            .align(Alignment.TopStart)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Emerald.copy(alpha = alpha)),
        )
        Spacer(Modifier.width(6.dp))
        Text("الكاميرا نشطة", style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    SectionCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        ) {
            Icon(
                Icons.Filled.PhotoCamera,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp),
            )
            Text("نحتاج إذن الكاميرا لمسح الرموز مباشرة", style = MaterialTheme.typography.titleSmall)
            Text(
                "كل المعالجة تتم على جهازك — لا تُرفع أي صورة لأي خادم",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
            Button(onClick = onRequest) { Text("تفعيل إذن الكاميرا") }
        }
    }
}

@Composable
private fun HistorySection(
    history: List<ScanRecord>,
    onClear: () -> Unit,
    onExport: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("سجل العمليات", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (history.isNotEmpty()) {
                IconButton(onClick = onExport) {
                    Icon(Icons.Filled.Share, contentDescription = "تصدير", tint = TextMuted)
                }
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Delete, contentDescription = "مسح", tint = Danger)
                }
            }
        }
        if (history.isEmpty()) {
            Text(
                "لا توجد عمليات مسح بعد — ابدأ بمسح أول رمز",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        } else {
            history.take(20).forEach { record -> HistoryRow(record) }
        }
    }
}

@Composable
private fun HistoryRow(record: ScanRecord) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(record.content, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            record.format + " · " + formatTime(record.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = BorderColor)
    }
}

private fun formatTime(timestamp: Long): String =
    try {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    } catch (e: Exception) {
        ""
    }

private fun exportHistory(context: Context, history: List<ScanRecord>) {
    if (history.isEmpty()) return
    val text = history.joinToString("\n") { r ->
        "[" + r.format + " " + formatTime(r.timestamp) + "] " + r.content
    }
    ExportUtils.shareText(context, text, "تصدير سجل المسح")
}

@Composable
private fun ResultSheet(
    result: ScanViewModel.ScanResult,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCreate: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Emerald.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    ScanFormats.label(result.format),
                    color = Emerald,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("إغلاق") }
        }
        SelectionContainer(
            Modifier
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(result.rawValue, style = MaterialTheme.typography.bodyLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResultAction("نسخ", Icons.Filled.ContentCopy, Modifier.weight(1f), onCopy)
            if (ScanFormats.isOpenable(result.rawValue)) {
                ResultAction("فتح", Icons.Filled.OpenInNew, Modifier.weight(1f), onOpen)
            }
            ResultAction("مشاركة", Icons.Filled.Share, Modifier.weight(1f), onShare)
            ResultAction("إنشاء", Icons.Filled.QrCode2, Modifier.weight(1f), onCreate)
        }
    }
}

@Composable
private fun ResultAction(
    label: String,
    // AUTO-REPAIR: disabled -- unresolved reference 'ImageVector' (was: icon: ImageVector,)
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
