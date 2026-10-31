package com.fakesocial.app.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fakesocial.app.data.Format
import com.fakesocial.app.data.MockData

@Composable
fun FacebookPost(d: MockData) {
    val p = facebookPalette(d.dark)
    MockFrame(d, p.bg) {
        Column(Modifier.fillMaxWidth().background(p.surface).padding(vertical = 12.dp)) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(d.avatarPath, d.name, 40.dp, Color(0xFF1877F2))
                Spacer(Modifier.width(8.dp))
                Column {
                    Txt(d.name, p.text, 15, bold = true)
                    Txt("${d.time} · 🌐", p.sub, 12)
                }
            }
            Txt(d.text, p.text, 16, modifier = Modifier.padding(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Txt("👍❤️ ${Format.count(d.likes)}", p.sub, 13)
                Txt("${Format.count(d.comments)} comments", p.sub, 13)
            }
            HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = p.divider)
            ActionRow(listOf("👍 Like", "💬 Comment", "↪ Share"), p.sub)
            HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = p.divider)
            d.items.forEach { CommentBubble(it, p, "Like · Reply") }
        }
    }
}

@Composable
fun TwitterPost(d: MockData) {
    val p = blackWhitePalette(d.dark)
    MockFrame(d, p.bg) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Avatar(d.avatarPath, d.name, 44.dp, Color(0xFF1D9BF0))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Txt(d.name, p.text, 15, bold = true)
                    Txt("  @${d.handle} · ${d.time}", p.sub, 14)
                }
                Txt(d.text, p.text, 15, modifier = Modifier.padding(vertical = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Txt("💬 ${Format.count(d.comments)}", p.sub, 13)
                    Txt("🔁 ${Format.count(d.likes / 4)}", p.sub, 13)
                    Txt("🤍 ${Format.count(d.likes)}", p.sub, 13)
                    Txt("📊", p.sub, 13)
                }
            }
        }
        d.items.forEach { r ->
            HorizontalDivider(color = p.divider)
            Row(Modifier.fillMaxWidth().padding(12.dp)) {
                Avatar(null, r.author, 36.dp, colorFor(r.author))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row {
                        Txt(r.author, p.text, 14, bold = true)
                        Txt("  @${r.author.lowercase().replace(" ", "_")} · ${r.time}", p.sub, 13)
                    }
                    Txt(r.text, p.text, 14, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun LinkedInPost(d: MockData) {
    val p = facebookPalette(d.dark)
    MockFrame(d, p.bg) {
        Column(Modifier.fillMaxWidth().background(p.surface).padding(vertical = 12.dp)) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(d.avatarPath, d.name, 48.dp, Color(0xFF0A66C2))
                Spacer(Modifier.width(10.dp))
                Column {
                    Txt(d.name, p.text, 15, bold = true)
                    Txt(d.handle, p.sub, 12)
                    Txt("${d.time} · 🌐", p.sub, 12)
                }
            }
            Txt(d.text, p.text, 15, modifier = Modifier.padding(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Txt("👍 ${Format.count(d.likes)}", p.sub, 12)
                Txt("${Format.count(d.comments)} comments", p.sub, 12)
            }
            HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = p.divider)
            ActionRow(listOf("👍 Like", "💬 Comment", "🔁 Repost", "➤ Send"), p.sub)
            d.items.forEach { CommentBubble(it, p, "Like · Reply") }
        }
    }
}

@Composable
fun InstagramPost(d: MockData) {
    val dark = d.dark
    val bg = if (dark) Color.Black else Color.White
    val text = if (dark) Color.White else Color(0xFF262626)
    val sub = if (dark) Color(0xFFA8A8A8) else Color(0xFF8E8E8E)
    val ring = Brush.sweepGradient(listOf(Color(0xFFFEDA75), Color(0xFFFA7E1E), Color(0xFFD62976), Color(0xFF962FBF), Color(0xFFFEDA75)))
    MockFrame(d, bg) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).border(2.dp, ring, CircleShape).padding(3.dp)) {
                Avatar(d.avatarPath, d.name, 32.dp, Color(0xFFE1306C))
            }
            Spacer(Modifier.width(8.dp))
            Txt(d.handle, text, 14, bold = true, modifier = Modifier.weight(1f))
            Txt("⋯", text, 18)
        }
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .background(Brush.linearGradient(listOf(Color(0xFF8E9EAB), Color(0xFFD7DDE8)))),
            contentAlignment = Alignment.Center
        ) {
            Txt("PHOTO", Color.White, 18, bold = true)
        }
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Txt("🤍", text, 20); Txt("💬", text, 20); Txt("✈️", text, 20)
        }
        Txt("${Format.count(d.likes)} likes", text, 14, bold = true, modifier = Modifier.padding(horizontal = 12.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(d.handle) }
                append("  ")
                append(d.text)
            },
            color = text, fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Txt("View all ${Format.count(d.comments)} comments", sub, 14, modifier = Modifier.padding(horizontal = 12.dp))
        d.items.forEach { r ->
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(r.author.lowercase().replace(" ", "_")) }
                    append("  ")
                    append(r.text)
                },
                color = text, fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
        Txt(d.time.uppercase() + " AGO", sub, 10, modifier = Modifier.padding(12.dp))
    }
}
