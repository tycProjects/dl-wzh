package com.fakesocial.app.ui.templates

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakesocial.app.R
import com.fakesocial.app.data.ChatItem
import com.fakesocial.app.data.Format
import com.fakesocial.app.data.MockData
import com.fakesocial.app.data.Platform
import com.fakesocial.app.util.ImageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Colors shared by the "post" and "comments" templates. */
data class Palette(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val sub: Color,
    val divider: Color,
    val chip: Color
)

fun facebookPalette(dark: Boolean) =
    if (dark) Palette(Color(0xFF18191A), Color(0xFF242526), Color(0xFFE4E6EB), Color(0xFFB0B3B8), Color(0xFF3A3B3C), Color(0xFF3A3B3C))
    else Palette(Color(0xFFF0F2F5), Color.White, Color(0xFF050505), Color(0xFF65676B), Color(0xFFDADDE1), Color(0xFFF0F2F5))

fun blackWhitePalette(dark: Boolean) =
    if (dark) Palette(Color.Black, Color.Black, Color(0xFFE7E9EA), Color(0xFF71767B), Color(0xFF2F3336), Color(0xFF16181C))
    else Palette(Color.White, Color.White, Color(0xFF0F1419), Color(0xFF536471), Color(0xFFEFF3F4), Color(0xFFF7F9F9))

private val avatarColors = listOf(
    Color(0xFF6C5CE7), Color(0xFF00B894), Color(0xFFE17055), Color(0xFF0984E3),
    Color(0xFFD63031), Color(0xFFE84393), Color(0xFF636E72), Color(0xFFFDCB6E)
)

fun colorFor(name: String): Color = avatarColors[(name.hashCode() and 0x7FFFFFFF) % avatarColors.size]

@Composable
fun Txt(
    text: String,
    color: Color,
    size: Int = 14,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    align: TextAlign? = null
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = align
    )
}

@Composable
fun Avatar(path: String?, name: String, size: Dp, color: Color, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { ImageStore.load(path)?.asImageBitmap() }
    }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        val b = bitmap
        if (b != null) {
            Image(
                bitmap = b,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = Format.initial(name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
}

/** Wraps a template: applies content direction, background and the optional "mockup" watermark. */
@Composable
fun MockFrame(d: MockData, background: Color, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (d.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Column(Modifier.fillMaxWidth().background(background)) {
            content()
            if (d.watermark) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.CenterEnd) {
                    Txt(stringResource(R.string.watermark_text), Color(0xFF9AA0A6), 10)
                }
            }
        }
    }
}

/** Fake phone status bar (clock + signal/battery) used at the top of chat screens. */
@Composable
fun StatusBar(time: String, color: Color, background: Color) {
    Row(
        Modifier.fillMaxWidth().background(background).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Txt(time, color, 12, bold = true)
        Txt("📶 🔋", color, 11)
    }
}

/** A rounded comment bubble used by the Facebook and LinkedIn templates. */
@Composable
fun CommentBubble(item: ChatItem, p: Palette, footer: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Avatar(null, item.author, 32.dp, colorFor(item.author))
        Spacer(Modifier.width(8.dp))
        Column {
            Box(Modifier.clip(RoundedCornerShape(16.dp)).background(p.chip).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Column {
                    Txt(item.author, p.text, 13, bold = true)
                    Txt(item.text, p.text, 14)
                }
            }
            Txt("$footer · ${item.time}", p.sub, 11, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
        }
    }
}

/** Equal-weight action row ("Like / Comment / Share"). */
@Composable
fun ActionRow(labels: List<String>, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        labels.forEach { Txt(it, color, 13, bold = true) }
    }
}

/** Picks the right template for the project. */
@Composable
fun MockPreview(d: MockData) {
    if (d.platform == Platform.INSTAGRAM && d.variant == 1) {
        InstagramDm(d)
        return
    }
    when (d.platform) {
        Platform.FACEBOOK -> FacebookPost(d)
        Platform.INSTAGRAM -> InstagramPost(d)
        Platform.WHATSAPP -> WhatsAppChat(d)
        Platform.TWITTER -> TwitterPost(d)
        Platform.TIKTOK -> TikTokComments(d)
        Platform.SNAPCHAT -> SnapchatChat(d)
        Platform.TELEGRAM -> TelegramChat(d)
        Platform.YOUTUBE -> YouTubeComments(d)
        Platform.LINKEDIN -> LinkedInPost(d)
        Platform.MESSENGER -> MessengerChat(d)
    }
}
