package com.qrstudio.app.ui.create

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qrstudio.app.qr.QrRenderer
import com.qrstudio.app.qr.QrStyleConfig
import com.qrstudio.app.qr.QrSvgWriter
import com.qrstudio.app.ui.components.SectionCard
import com.qrstudio.app.ui.components.SectionLabel
import com.qrstudio.app.ui.theme.CardBgAlt
import com.qrstudio.app.ui.theme.Emerald
import com.qrstudio.app.ui.theme.TextMain
import com.qrstudio.app.ui.theme.TextMuted
import com.qrstudio.app.util.ExportUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun CreateScreen(vm: CreateViewModel) {
    val type by vm.contentType.collectAsStateWithLifecycle()
    val fields by vm.fields.collectAsStateWithLifecycle()
    val style by vm.style.collectAsStateWithLifecycle()
    val bitmap by vm.bitmap.collectAsStateWithLifecycle()
    val logo by vm.logo.collectAsStateWithLifecycle()
    val faviconLoading by vm.faviconLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAdvanced by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                val bmp = try {
                    context.contentResolver.openInputStream(it)?.use { stream -> android.graphics.BitmapFactory.decodeStream(stream) }
                } catch (e: Exception) {
                    null
                }
                if (bmp != null) vm.setLogo(bmp) else vm.notify("تعذر قراءة الصورة المختارة")
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionLabel("نوع المحتوى") }
        item { ContentTypeGrid(selected = type, onSelect = vm::selectType) }
        item { ContentFormCard(type = type, fields = fields, onFieldChange = vm::updateField) }
        if (type == ContentType.URL) {
            item {
                OutlinedButton(
                    onClick = vm::fetchFavicon,
                    enabled = !faviconLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (faviconLoading) "جاري جلب شعار الموقع…" else "جلب الشعار تلقائياً من الرابط")
                }
            }
        }
        item {
            PreviewCard(
                bitmap = bitmap,
                logo = logo,
                onWhite = vm::applyWhiteTheme,
                onBlack = vm::applyBlackTheme,
                onReset = vm::resetStyle,
            )
        }
        item { ExportCard(onExport = { target -> doExport(context, scope, vm, style, logo, target) }) }
        item {
            OutlinedButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("الخيارات المتقدمة", modifier = Modifier.weight(1f))
                Icon(
                    if (showAdvanced) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
            }
        }
        if (showAdvanced) {
            item { ThemesSection(onTheme = vm::applyTheme) }
            item { ShapesSection(style = style, onStyle = vm::updateStyle) }
            item { ColorsSection(style = style, onStyle = vm::updateStyle) }
            item {
                LogoSection(
                    logo = logo,
                    style = style,
                    onPick = { imagePicker.launch("image/*") },
                    onDelete = { vm.setLogo(null) },
                    onStyle = vm::updateStyle,
                )
            }
            item { CaptionSection(style = style, onStyle = vm::updateStyle) }
            item { QualitySection(style = style, onStyle = vm::updateStyle) }
        }
    }
}

@Composable
private fun ContentTypeGrid(selected: ContentType, onSelect: (ContentType) -> Unit) {
    SectionCard {
        ContentType.entries.chunked(4).forEach { rowItems ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { t ->
                    TypeChip(type = t, selected = selected == t, modifier = Modifier.weight(1f), onSelect = onSelect)
                }
                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TypeChip(
    type: ContentType,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (ContentType) -> Unit,
) {
    val bg = if (selected) Emerald else CardBgAlt
    val fg = if (selected) Color(0xFF06281f) else TextMain
    Surface(
        color = bg,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.clickable { onSelect(type) },
    ) {
        Column(
            Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(type.icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(type.label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1)
        }
    }
}

@Composable
private fun ContentFormCard(
    type: ContentType,
    fields: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
) {
    SectionCard {
        SectionLabel("بيانات " + type.label)
        ContentForms.fieldsFor(type).forEach { spec ->
            val value = fields[spec.key] ?: spec.defaultValue
            if (spec.options != null) {
                OptionsField(spec = spec, value = value, options = spec.options, onChange = onFieldChange)
            } else {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onFieldChange(spec.key, it) },
                    label = { Text(spec.label) },
                    placeholder = { if (spec.placeholder.isNotBlank()) Text(spec.placeholder) },
                    singleLine = spec.singleLine,
                    keyboardOptions = KeyboardOptions(keyboardType = spec.keyboard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionsField(
    spec: FieldSpec,
    value: String,
    options: List<Pair<String, String>>,
    onChange: (String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = options.firstOrNull { it.first == value }?.second ?: value,
            onValueChange = {},
            readOnly = true,
            label = { Text(spec.label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onChange(spec.key, key)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(
    bitmap: Bitmap?,
    logo: Bitmap?,
    onWhite: () -> Unit,
    onBlack: () -> Unit,
    onReset: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("المعاينة", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onWhite) { Text("أبيض") }
            TextButton(onClick = onBlack) { Text("أسود") }
            TextButton(onClick = onReset) { Text("إعادة ضبط") }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "معاينة رمز QR",
                    modifier = Modifier.size(240.dp),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.QrCode2,
                        contentDescription = null,
                        tint = Color(0xFF94a3b8),
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("ستظهر المعاينة هنا", color = Color(0xFF64748b))
                }
            }
        }
        if (logo != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Image(
                    bitmap = logo.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "شعار مضاف داخل الكود",
                    style = MaterialTheme.typography.bodySmall,
                    color = Emerald,
                )
            }
        }
    }
}

@Composable
private fun ExportCard(onExport: (String) -> Unit) {
    SectionCard {
        SectionLabel("تصدير ومشاركة")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportButton("PNG", Icons.Filled.Download, Modifier.weight(1f)) { onExport("png") }
            ExportButton("JPG", Icons.Filled.Image, Modifier.weight(1f)) { onExport("jpg") }
            ExportButton("SVG", Icons.Filled.Code, Modifier.weight(1f)) { onExport("svg") }
            ExportButton("مشاركة", Icons.Filled.Share, Modifier.weight(1f)) { onExport("share") }
        }
    }
}

@Composable
private fun ExportButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Clip)
        }
    }
}

private fun doExport(
    context: Context,
    scope: CoroutineScope,
    vm: CreateViewModel,
    style: QrStyleConfig,
    logo: Bitmap?,
    target: String,
) {
    val content = vm.payload.value
    if (content.isBlank()) {
        vm.notify("أدخل محتوى أولاً")
        return
    }
    scope.launch(Dispatchers.IO) {
        try {
            when (target) {
                "svg" -> {
                    val matrix = QrRenderer.matrixFor(content, style)
                    if (matrix == null) {
                        vm.notify("المحتوى أطول من أن يُرمَّز بهذا المستوى من الحماية")
                        return@launch
                    }
                    val svg = QrSvgWriter.write(matrix, style, logo?.toPngBase64())
                    val path = ExportUtils.saveSvg(context, svg, ExportUtils.fileName())
                    vm.notify("تم حفظ SVG في: $path")
                }
                "share" -> {
                    val bmp = vm.bitmap.value ?: QrRenderer.render(content, style, logo)
                    if (bmp == null) {
                        vm.notify("تعذر إنشاء الصورة")
                        return@launch
                    }
                    ExportUtils.shareImage(context, bmp, "مشاركة رمز QR")
                }
                else -> {
                    val bmp = vm.bitmap.value ?: QrRenderer.render(content, style, logo)
                    if (bmp == null) {
                        vm.notify("المحتوى طويل جداً أو فارغ")
                        return@launch
                    }
                    val fmt = if (target == "jpg") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
                    val ext = if (target == "jpg") "jpg" else "png"
                    val mime = if (target == "jpg") "image/jpeg" else "image/png"
                    val path = ExportUtils.saveBitmap(context, bmp, fmt, ext, mime, ExportUtils.fileName())
                    vm.notify("تم الحفظ في: $path")
                }
            }
        } catch (e: Exception) {
            vm.notify("فشل التصدير: ${e.message}")
        }
    }
}

private fun Bitmap.toPngBase64(): String {
    val out = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}
