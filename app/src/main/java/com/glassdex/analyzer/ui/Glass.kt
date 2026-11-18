package com.glassdex.analyzer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassdex.analyzer.analysis.Risk

val LocalGlassOpacity = compositionLocalOf { 0.16f }

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}

object C {
    val bg = Color(0xFF080B18)
    val cyan = Color(0xFF5EEAD4)
    val violet = Color(0xFFA78BFA)
    val pink = Color(0xFFF472B6)
    val blue = Color(0xFF60A5FA)
    val text = Color(0xFFF4F6FF)
    val sub = Color(0xB3E6EAFF)
    val green = Color(0xFF4ADE80)
    val yellow = Color(0xFFFACC15)
    val orange = Color(0xFFFB923C)
    val red = Color(0xFFF87171)
}

fun riskColor(r: Risk) = when (r) {
    Risk.CLEAN -> C.green
    Risk.LOW -> C.yellow
    Risk.MEDIUM -> C.orange
    Risk.HIGH -> C.red
}

@Composable
fun GlassBackground(animated: Boolean, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "bg")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    val p = if (animated) a else 0.5f
    Box(Modifier.fillMaxSize().background(C.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            fun blob(color: Color, cx: Float, cy: Float, r: Float) =
                drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
            blob(C.violet, w * (0.15f + 0.5f * p), h * (0.12f + 0.1f * p), w * 0.75f)
            blob(C.cyan, w * (0.9f - 0.5f * p), h * (0.45f + 0.1f * p), w * 0.7f)
            blob(C.pink, w * (0.2f + 0.4f * p), h * (0.85f - 0.15f * p), w * 0.8f)
            blob(C.blue, w * (0.8f - 0.3f * p), h * (0.05f + 0.2f * p), w * 0.5f)
        }
        content()
    }
}

/** Rounded translucent "liquid glass" surface. Every box in the app uses this. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 28.dp,
    tint: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val o = LocalGlassOpacity.current
    val shape = RoundedCornerShape(radius)
    var m = modifier.clip(shape)
        .background(Brush.linearGradient(listOf(tint.copy(alpha = (o + 0.08f).coerceAtMost(0.6f)), tint.copy(alpha = (o * 0.45f)))))
        .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.28f))), shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Column(m.padding(16.dp), content = content)
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, accent: Color = C.cyan, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.clip(shape)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = if (enabled) 0.42f else 0.12f), accent.copy(alpha = if (enabled) 0.16f else 0.05f))))
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.6f), accent.copy(alpha = 0.2f))), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = C.text.copy(alpha = if (enabled) 1f else 0.5f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

@Composable
fun GlassField(
    value: String, onChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, password: Boolean = false, singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        singleLine = singleLine, modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = C.text, unfocusedTextColor = C.text,
            focusedBorderColor = C.cyan, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            focusedLabelColor = C.cyan, unfocusedLabelColor = C.sub, cursorColor = C.cyan,
            focusedContainerColor = Color.White.copy(alpha = 0.07f), unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
        )
    )
}

@Composable
fun Pill(text: String, color: Color = C.cyan) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.2f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.5f)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(text, color = C.text, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = C.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
}

@Composable
fun KeyValue(k: String, v: String?, mono: Boolean = false) {
    if (v.isNullOrBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(k, color = C.sub, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Text(v, color = C.text, fontSize = 13.sp, fontFamily = if (mono) androidx.compose.ui.text.font.FontFamily.Monospace else null, modifier = Modifier.weight(1f))
    }
}

@Composable
fun RiskGauge(score: Int, risk: Risk, size: Dp = 150.dp) {
    val col = riskColor(risk)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color.White.copy(alpha = 0.12f), 135f, 270f, false, style = stroke)
            drawArc(Brush.sweepGradient(listOf(col.copy(alpha = 0.5f), col)), 135f, 270f * score / 100f, false, style = stroke,
                size = Size(this.size.width, this.size.height))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", color = C.text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(risk.label, color = col, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
