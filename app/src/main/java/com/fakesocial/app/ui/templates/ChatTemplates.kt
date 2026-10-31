package com.fakesocial.app.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fakesocial.app.data.ChatItem
import com.fakesocial.app.data.MockData

data class ChatStyle(
    val header: Color,
    val headerText: Color,
    val headerSub: Color,
    val bg: Color,
    val bubbleIn: Color,
    val bubbleOut: Color,
    val textIn: Color,
    val textOut: Color,
    val timeIn: Color,
    val timeOut: Color,
    val ticks: Boolean,
    val tick: Color,
    val radius: Dp,
    val inputBg: Color,
    val inputHint: Color
)

@Composable
private fun Bubble(item: ChatItem, s: ChatStyle) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (item.mine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            Modifier
                .widthIn(max = 270.dp)
                .clip(RoundedCornerShape(s.radius))
                .background(if (item.mine) s.bubbleOut else s.bubbleIn)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Txt(item.text, if (item.mine) s.textOut else s.textIn, 15)
                if (item.time.isNotBlank() || (item.mine && s.ticks)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(item.time, if (item.mine) s.timeOut else s.timeIn, 10)
                        if (item.mine && s.ticks) Txt(" ✓✓", s.tick, 10)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatTemplate(d: MockData, s: ChatStyle) {
    MockFrame(d, s.bg) {
        StatusBar(d.time, s.headerText, s.header)
        Row(
            Modifier.fillMaxWidth().background(s.header).padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt("‹", s.headerText, 28)
            Spacer(Modifier.width(6.dp))
            Avatar(d.avatarPath, d.name, 38.dp, Color(d.platform.accent))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Txt(d.name, s.headerText, 16, bold = true)
                if (d.status.isNotBlank()) Txt(d.status, s.headerSub, 12)
            }
            Txt("⋮", s.headerText, 20)
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(s.bubbleIn)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) { Txt("Today", s.timeIn, 11) }
        }
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            d.items.forEach { Bubble(it, s) }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(s.inputBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) { Txt("Message", s.inputHint, 14) }
        }
    }
}

@Composable
fun WhatsAppChat(d: MockData) {
    val dk = d.dark
    ChatTemplate(
        d,
        ChatStyle(
            header = if (dk) Color(0xFF1F2C34) else Color(0xFF008069),
            headerText = Color.White,
            headerSub = Color(0xFFD1E7E1),
            bg = if (dk) Color(0xFF0B141A) else Color(0xFFECE5DD),
            bubbleIn = if (dk) Color(0xFF1F2C34) else Color.White,
            bubbleOut = if (dk) Color(0xFF005C4B) else Color(0xFFD9FDD3),
            textIn = if (dk) Color(0xFFE9EDEF) else Color(0xFF111B21),
            textOut = if (dk) Color(0xFFE9EDEF) else Color(0xFF111B21),
            timeIn = Color(0xFF8696A0),
            timeOut = if (dk) Color(0xFF8FBFB4) else Color(0xFF667781),
            ticks = true,
            tick = Color(0xFF53BDEB),
            radius = 8.dp,
            inputBg = if (dk) Color(0xFF1F2C34) else Color.White,
            inputHint = Color(0xFF8696A0)
        )
    )
}

@Composable
fun TelegramChat(d: MockData) {
    val dk = d.dark
    ChatTemplate(
        d,
        ChatStyle(
            header = if (dk) Color(0xFF212D3B) else Color(0xFF517DA2),
            headerText = Color.White,
            headerSub = Color(0xFFB7CBDD),
            bg = if (dk) Color(0xFF0E1621) else Color(0xFFDCE6EE),
            bubbleIn = if (dk) Color(0xFF182533) else Color.White,
            bubbleOut = if (dk) Color(0xFF2B5278) else Color(0xFFEFFDDE),
            textIn = if (dk) Color.White else Color.Black,
            textOut = if (dk) Color.White else Color.Black,
            timeIn = Color(0xFF8A9AA8),
            timeOut = if (dk) Color(0xFF9FC1E3) else Color(0xFF5FB45B),
            ticks = true,
            tick = Color(0xFF5FB45B),
            radius = 12.dp,
            inputBg = if (dk) Color(0xFF17212B) else Color.White,
            inputHint = Color(0xFF8A9AA8)
        )
    )
}

@Composable
fun MessengerChat(d: MockData) {
    val dk = d.dark
    ChatTemplate(
        d,
        ChatStyle(
            header = if (dk) Color(0xFF242526) else Color.White,
            headerText = if (dk) Color(0xFFE4E6EB) else Color(0xFF050505),
            headerSub = if (dk) Color(0xFFB0B3B8) else Color(0xFF65676B),
            bg = if (dk) Color(0xFF18191A) else Color.White,
            bubbleIn = if (dk) Color(0xFF303030) else Color(0xFFE4E6EB),
            bubbleOut = Color(0xFF0084FF),
            textIn = if (dk) Color(0xFFE4E6EB) else Color(0xFF050505),
            textOut = Color.White,
            timeIn = Color(0xFF8A8D91),
            timeOut = Color(0xFFCFE6FF),
            ticks = false,
            tick = Color.Transparent,
            radius = 18.dp,
            inputBg = if (dk) Color(0xFF3A3B3C) else Color(0xFFF0F2F5),
            inputHint = Color(0xFF8A8D91)
        )
    )
}

@Composable
fun InstagramDm(d: MockData) {
    val dk = d.dark
    ChatTemplate(
        d,
        ChatStyle(
            header = if (dk) Color.Black else Color.White,
            headerText = if (dk) Color.White else Color(0xFF262626),
            headerSub = Color(0xFF8E8E8E),
            bg = if (dk) Color.Black else Color.White,
            bubbleIn = if (dk) Color(0xFF262626) else Color(0xFFEFEFEF),
            bubbleOut = Color(0xFF7C4DFF),
            textIn = if (dk) Color.White else Color(0xFF262626),
            textOut = Color.White,
            timeIn = Color(0xFF8E8E8E),
            timeOut = Color(0xFFE0D6FF),
            ticks = false,
            tick = Color.Transparent,
            radius = 20.dp,
            inputBg = if (dk) Color(0xFF262626) else Color(0xFFEFEFEF),
            inputHint = Color(0xFF8E8E8E)
        )
    )
}

@Composable
fun SnapchatChat(d: MockData) {
    val dk = d.dark
    val bg = if (dk) Color(0xFF111111) else Color.White
    val text = if (dk) Color.White else Color(0xFF111111)
    val sub = Color(0xFF8E8E8E)
    MockFrame(d, bg) {
        StatusBar(d.time, text, bg)
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(d.avatarPath, d.name, 38.dp, Color(d.platform.accent))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Txt(d.name, text, 16, bold = true)
                if (d.status.isNotBlank()) Txt(d.status, sub, 12)
            }
            Txt("⋯", text, 20)
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            d.items.forEach { m ->
                val tint = if (m.mine) Color(0xFF0EADFF) else Color(0xFFF23C57)
                Column(Modifier.padding(vertical = 6.dp)) {
                    Txt(if (m.mine) "ME" else d.name.uppercase(), tint, 11, bold = true)
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(Modifier.width(2.dp).fillMaxHeight().background(tint))
                        Txt(m.text, text, 15, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().padding(10.dp).clip(RoundedCornerShape(24.dp))
                .background(if (dk) Color(0xFF2A2A2A) else Color(0xFFF1F2F4))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) { Txt("Send a chat", sub, 14) }
    }
}
