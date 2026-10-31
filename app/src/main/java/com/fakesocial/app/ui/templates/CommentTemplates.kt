package com.fakesocial.app.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fakesocial.app.data.Format
import com.fakesocial.app.data.MockData

@Composable
fun TikTokComments(d: MockData) {
    val p = if (d.dark) {
        blackWhitePalette(true).copy(bg = Color(0xFF121212), surface = Color(0xFF121212))
    } else blackWhitePalette(false)
    MockFrame(d, p.bg) {
        Txt(
            "${Format.count(d.comments)} comments", p.text, 13, bold = true,
            modifier = Modifier.fillMaxWidth().padding(12.dp), align = TextAlign.Center
        )
        HorizontalDivider(color = p.divider)
        TikTokRow(d.avatarPath, d.name, d.text, d.time, Format.count(d.likes), p)
        d.items.forEach { TikTokRow(null, it.author, it.text, it.time, "", p) }
    }
}

@Composable
private fun TikTokRow(avatar: String?, author: String, text: String, time: String, likes: String, p: Palette) {
    Row(Modifier.fillMaxWidth().padding(12.dp)) {
        Avatar(avatar, author, 36.dp, colorFor(author))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Txt(author, p.sub, 13, bold = true)
            Txt(text, p.text, 15, modifier = Modifier.padding(vertical = 2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Txt(time, p.sub, 12)
                Txt("Reply", p.sub, 12, bold = true)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Txt("🤍", p.sub, 18)
            if (likes.isNotEmpty()) Txt(likes, p.sub, 12)
        }
    }
}

@Composable
fun YouTubeComments(d: MockData) {
    val p = if (d.dark) {
        blackWhitePalette(true).copy(bg = Color(0xFF0F0F0F), surface = Color(0xFF0F0F0F), divider = Color(0xFF3F3F3F))
    } else blackWhitePalette(false).copy(divider = Color(0xFFE5E5E5))
    MockFrame(d, p.bg) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Txt("Comments", p.text, 16, bold = true)
            Txt(Format.count(d.comments), p.sub, 14)
        }
        HorizontalDivider(color = p.divider)
        YouTubeRow(d.avatarPath, d.name, "@${d.handle}", d.text, d.time, Format.count(d.likes), p)
        d.items.forEach {
            YouTubeRow(null, it.author, "@" + it.author.lowercase().replace(" ", ""), it.text, it.time, "", p)
        }
    }
}

@Composable
private fun YouTubeRow(
    avatar: String?, name: String, handle: String, text: String, time: String, likes: String, p: Palette
) {
    Row(Modifier.fillMaxWidth().padding(12.dp)) {
        Avatar(avatar, name, 40.dp, colorFor(name))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Txt("$handle · $time", p.sub, 12)
            Txt(text, p.text, 14, modifier = Modifier.padding(vertical = 3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Txt(if (likes.isEmpty()) "👍" else "👍 $likes", p.sub, 12)
                Txt("👎", p.sub, 12)
                Txt("Reply", p.text, 12, bold = true)
            }
        }
        Txt("⋮", p.sub, 16)
    }
}
