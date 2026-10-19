package com.qrstudio.app.ui.create

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.qrstudio.app.qr.CornerDot
import com.qrstudio.app.qr.CornerFrame
import com.qrstudio.app.qr.DotShape
import com.qrstudio.app.qr.QrPalettes
import com.qrstudio.app.qr.QrStyleConfig
import com.qrstudio.app.qr.QrThemePreset
import com.qrstudio.app.qr.QrThemes
import com.qrstudio.app.ui.components.ChipsRow
import com.qrstudio.app.ui.components.MutedLabel
import com.qrstudio.app.ui.components.SectionCard
import com.qrstudio.app.ui.components.SectionLabel
import com.qrstudio.app.ui.components.SliderRow
import com.qrstudio.app.ui.components.Swatches
import com.qrstudio.app.ui.theme.BorderColor
import com.qrstudio.app.ui.theme.CardBgAlt
import com.qrstudio.app.ui.theme.Danger
import com.qrstudio.app.ui.theme.TextMain
import com.qrstudio.app.ui.theme.TextMuted

private typealias StyleUpdater = ((QrStyleConfig) -> QrStyleConfig) -> Unit

@Composable
fun ThemesSection(onTheme: (QrThemePreset) -> Unit) {
    SectionCard {
        SectionLabel("ثيمات سريعة")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QrThemes.all.forEach { preset ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTheme(preset) },
                ) {
                    val brush = if (preset.gradient) {
                        Brush.linearGradient(listOf(Color(preset.primary), Color(preset.secondary)))
                    } else {
                        SolidColor(Color(preset.primary))
                    }
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(brush)
                            .border(1.dp, BorderColor, CircleShape),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        preset.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun ShapesSection(style: QrStyleConfig, onStyle: StyleUpdater) {
    SectionCard {
        SectionLabel("أشكال النقاط والزوايا")
        MutedLabel("شكل النقاط")
        ChipsRow(
            items = DotShape.entries.map { it.name to it.label },
            selectedKey = style.dotShape.name,
        ) { key -> onStyle { it.copy(dotShape = DotShape.valueOf(key)) } }
        MutedLabel("إطار زاوية الماسح")
        ChipsRow(
            items = CornerFrame.entries.map { it.name to it.label },
            selectedKey = style.cornerFrame.name,
        ) { key -> onStyle { it.copy(cornerFrame = CornerFrame.valueOf(key)) } }
        MutedLabel("نقطة وسط الزاوية")
        ChipsRow(
            items = CornerDot.entries.map { it.name to it.label },
            selectedKey = style.cornerDot.name,
        ) { key -> onStyle { it.copy(cornerDot = CornerDot.valueOf(key)) } }
    }
}

@Composable
fun ColorsSection(style: QrStyleConfig, onStyle: StyleUpdater) {
    SectionCard {
        SectionLabel("الألوان والتدرجات")
        MutedLabel("اللون الأساسي")
        Swatches(QrPalettes.accents, style.primaryColor) { c ->
            onStyle { it.copy(primaryColor = c) }
        }
        MutedLabel("اللون الثانوي — يفعّل التدرج")
        Swatches(QrPalettes.accents, style.secondaryColor) { c ->
            onStyle { it.copy(secondaryColor = c, gradient = true) }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Text("تدرج خطي 45°", style = MaterialTheme.typography.labelMedium, color = TextMain)
            Spacer(Modifier.weight(1f))
            Switch(checked = style.gradient, onCheckedChange = { g ->
                onStyle { it.copy(gradient = g) }
            })
        }
        MutedLabel("لون الخلفية")
        Swatches(QrPalettes.backgrounds, style.backgroundColor) { c ->
            onStyle { it.copy(backgroundColor = c, transparentBg = false) }
        }
        Spacer(Modifier.height(8.dp))
        FilterChip(
            selected = style.transparentBg,
            onClick = { onStyle { it.copy(transparentBg = !it.transparentBg) } },
            label = { Text("خلفية شفافة تماماً") },
        )
    }
}

@Composable
fun LogoSection(
    logo: Bitmap?,
    style: QrStyleConfig,
    onPick: () -> Unit,
    onDelete: () -> Unit,
    onStyle: StyleUpdater,
) {
    SectionCard {
        SectionLabel("شعار الكود")
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (logo != null) {
                Image(
                    bitmap = logo.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBgAlt),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = TextMuted)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Button(onClick = onPick) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("اختيار صورة من الجهاز")
                }
                if (logo != null) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Danger)
                        Spacer(Modifier.width(4.dp))
                        Text("حذف الشعار", color = Danger)
                    }
                }
            }
        }
        if (logo != null) {
            SliderRow(
                label = "حجم الشعار",
                value = style.logoSizePercent.toFloat(),
                range = 10f..45f,
                steps = 34,
                valueText = "${style.logoSizePercent}%",
            ) { v -> onStyle { it.copy(logoSizePercent = v.toInt()) } }
            SliderRow(
                label = "هامش العزل",
                value = style.logoMargin.toFloat(),
                range = 0f..20f,
                steps = 19,
                valueText = "${style.logoMargin} px",
            ) { v -> onStyle { it.copy(logoMargin = v.toInt()) } }
            Text(
                "هامش العزل يحمي بيانات الكود من التشويش حول الشعار",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
fun CaptionSection(style: QrStyleConfig, onStyle: StyleUpdater) {
    SectionCard {
        SectionLabel("النص السفلي")
        OutlinedTextField(
            value = style.caption,
            onValueChange = { v -> onStyle { it.copy(caption = v) } },
            label = { Text("نص أسفل الكود (مثال: امسح للدفع)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        MutedLabel("لون النص")
        Swatches(QrPalettes.captionColors, style.captionColor) { c ->
            onStyle { it.copy(captionColor = c) }
        }
        MutedLabel("حجم النص")
        ChipsRow(
            items = listOf("14" to "14px", "18" to "18px", "24" to "24px"),
            selectedKey = style.captionSize.toString(),
        ) { key -> onStyle { it.copy(captionSize = key.toInt()) } }
    }
}

@Composable
fun QualitySection(style: QrStyleConfig, onStyle: StyleUpdater) {
    SectionCard(highlight = true) {
        SectionLabel("الدقة والحماية")
        MutedLabel("مستوى تصحيح الخطأ")
        ChipsRow(
            items = listOf("L" to "L · 7%", "M" to "M · 15%", "Q" to "Q · 25%", "H" to "H · 30%"),
            selectedKey = style.errorCorrection,
        ) { key -> onStyle { it.copy(errorCorrection = key) } }
        MutedLabel("دقة التصدير")
        ChipsRow(
            items = listOf("300" to "300px", "600" to "600px", "1200" to "1200px", "2000" to "2000px"),
            selectedKey = style.exportSize.toString(),
        ) { key -> onStyle { it.copy(exportSize = key.toInt()) } }
        SliderRow(
            label = "هامش الأمان (Quiet Zone)",
            value = style.quietZone.toFloat(),
            range = 0f..30f,
            steps = 29,
            valueText = "${style.quietZone} px",
        ) { v -> onStyle { it.copy(quietZone = v.toInt()) } }
    }
}
