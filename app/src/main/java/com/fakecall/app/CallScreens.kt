package com.fakecall.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val Blue = Color(0xFF1A6FD4)
private val Green = Color(0xFF27AE60)
private val Red = Color(0xFFE74C3C)

private fun fmt(sec: Int): String =
    "%02d:%02d".format(sec / 60, sec % 60)

// ───────────────────────── أجزاء مشتركة ─────────────────────────

@Composable
private fun NumTag(text: String = "1") {
    Box(
        Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Blue)
            .padding(horizontal = 7.dp, vertical = 1.dp)
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

/** صورة المتصل الكبيرة. لو مفيش صورة مابتظهرش حاجة، فالشكل بيفضل زي القديم. */
@Composable
private fun BigAvatar(s: AppState, p: Profile, border: Color = Color.White.copy(alpha = 0.55f)) {
    val ctx = LocalContext.current
    val bmp = remember(p.id, s.photoVersion) { Store.loadBitmap(Store.photoFile(ctx, p.id)) }
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .border(3.dp, border, CircleShape)
        )
        Spacer(Modifier.height(12.dp))
    }
}

/** اسم المتصل: ضغطة مطولة = الإعدادات، ضغطة عادية (وهو بيرن) = تبدأ الرنة. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallerName(s: AppState, name: String, size: TextUnit, color: Color) {
    val haptic = LocalHapticFeedback.current
    Text(
        text = name,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 2,
        modifier = Modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (s.screen == Screen.Incoming) s.startRing() },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    s.openSettings()
                }
            )
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun CircleBtn(bg: Color, size: Int, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ───────────────────────── الاتصال الوارد ─────────────────────────

@Composable
fun IncomingScreen(s: AppState) {
    val p = s.active
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF6EE8C8),
                    0.28f to Color(0xFF4AB8E8),
                    1f to Color(0xFF2A6DE8)
                )
            )
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(50.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NumTag()
                Text("Incoming call", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(40.dp))
            BigAvatar(s, p)
            CallerName(s, p.name, 34.sp, Color.White)
            if (p.number.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Mobile ${p.number}",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
                .fillMaxWidth()
                .padding(horizontal = 65.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CircleBtn(Color.White.copy(alpha = 0.88f), 78, { s.accept() }) {
                Glyph(Ic.PHONE, Green, 38.dp)
            }
            CircleBtn(Color.White.copy(alpha = 0.88f), 78, { s.decline() }) {
                Glyph(Ic.DECLINE, Red, 38.dp)
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .width(110.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.45f))
            )
            Spacer(Modifier.height(7.dp))
            Text("Send message", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ───────────────────────── المكالمة الجارية ─────────────────────────

@Composable
private fun PanelBtn(label: String, ic: Ic, on: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (on) Green else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Glyph(ic, if (on) Color.White else Color(0xFF111111), 28.dp)
        }
        Text(
            label,
            color = if (on) Green else Color(0xFF111111),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ActiveScreen(s: AppState) {
    val p = s.active
    val on = remember(s.callId) { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(s.callId) {
        while (true) {
            delay(1000)
            s.seconds++
        }
    }

    fun toggle(key: String, onMsg: String, offMsg: String, action: (Boolean) -> Boolean = { true }) {
        val next = !(on[key] ?: false)
        if (action(next)) {
            on[key] = next
            s.showToast(if (next) onMsg else offMsg)
        } else {
            on[key] = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF1A3A8A))) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(50.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NumTag()
                Glyph(Ic.PHONE, Color.White, 15.dp)
                Text(fmt(s.seconds), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(26.dp))
            BigAvatar(s, p)
            CallerName(s, p.name, 34.sp, Color.White)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFFD2D6E4))
                .padding(horizontal = 20.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth()) {
                PanelBtn("Record", Ic.REC, on["rec"] == true, {
                    toggle("rec", "جاري التسجيل...", "تم إيقاف التسجيل")
                }, Modifier.weight(1f))
                PanelBtn("Hold", Ic.HOLD, on["hold"] == true, {
                    toggle("hold", "المكالمة قيد الانتظار", "تم استئناف المكالمة") { v -> s.audio.setHold(v); true }
                }, Modifier.weight(1f))
                PanelBtn("Bluetooth", Ic.BT, on["bt"] == true, {
                    val was = on["bt"] == true
                    toggle("bt", "البلوتوث مفعل", "تم إيقاف البلوتوث") { v -> s.audio.setBluetooth(v) }
                    if (!was && on["bt"] != true) s.showToast("مفيش جهاز بلوتوث متصل")
                }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth()) {
                PanelBtn("Speaker", Ic.SPEAKER, on["spk"] == true, {
                    toggle("spk", "مكبر الصوت مفعل", "تم إيقاف مكبر الصوت") { v -> s.audio.setSpeaker(v); true }
                }, Modifier.weight(1f))
                PanelBtn("Mute", Ic.MUTE, on["mute"] == true, {
                    toggle("mute", "تم كتم الصوت", "تم إلغاء الكتم") { v -> s.audio.setMuted(v); true }
                }, Modifier.weight(1f))
                PanelBtn("Keypad", Ic.KEYPAD, on["key"] == true, {
                    toggle("key", "لوحة المفاتيح مفعلة", "تم إخفاء لوحة المفاتيح")
                }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(26.dp))
            CircleBtn(Red, 68, { s.end() }) { Glyph(Ic.DECLINE, Color.White, 30.dp) }
        }
    }
}

// ───────────────────────── انتهاء المكالمة ─────────────────────────

@Composable
fun EndedScreen(s: AppState) {
    val p = s.active
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(50.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumTag()
                Glyph(Ic.DECLINE, Red, 20.dp)
                Text(fmt(s.finalSeconds), color = Red, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Call Ended", color = Red, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(30.dp))
            BigAvatar(s, p, Color(0xFFE5E7EE))
            CallerName(s, p.name, 38.sp, Color.Black)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 200.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { s.openSettings() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Glyph(Ic.PERSON, Color(0xFF111111), 36.dp)
            Text("View Contact", color = Color(0xFF111111), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                CircleBtn(Color(0xFF25D366), 72, { s.restart() }) { Glyph(Ic.PHONE, Color.White, 34.dp) }
                Badge("1", Blue)
            }
            Box {
                CircleBtn(Color(0xFF25D366), 72, { s.restart() }) { Glyph(Ic.PHONE, Color.White, 34.dp) }
                Badge("2", Blue)
            }
            CircleBtn(Color(0xFF00BCD4), 72, {}) { Glyph(Ic.MSG, Color.White, 34.dp) }
            CircleBtn(Blue, 72, {}) { Glyph(Ic.VIDEO, Color.White, 34.dp) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.Badge(text: String, bg: Color) {
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .size(20.dp)
            .clip(CircleShape)
            .background(bg)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
