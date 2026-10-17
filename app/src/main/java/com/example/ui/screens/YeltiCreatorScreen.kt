package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.YeltiPlatformLinks
import com.example.ui.components.LiquidBackgroundCanvas
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidCyanLight
import com.example.ui.theme.LiquidMagenta
import com.example.ui.theme.LiquidTeal
import com.example.ui.theme.LiquidViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun YeltiCreatorScreen() {
    val context = LocalContext.current

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackgroundCanvas(
            primaryColor = LiquidMagenta,
            secondaryColor = LiquidViolet,
            tertiaryColor = LiquidCyan
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp)
        ) {
            // Profile & Hub Hero Card
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(32.dp),
                testTag = "yelti_profile_card"
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glowing Avatar Indicator
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(2.dp, LiquidCyanLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "IY",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = LiquidCyanLight
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ivan Yelti",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = "Verified Creator",
                                    tint = LiquidCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "@gdyelti • Yeltition Studio",
                                fontSize = 13.sp,
                                color = LiquidCyanLight
                            )
                            Text(
                                text = "Geometry Dash Architect & Digital Creator",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Central creator hub inspired by gdyelti.tilda.ws, featuring interactive liquid glass physics, Geometry Dash level design kinetics, and bioscience simulations.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LiquidGlassButton(
                        text = "Open gdyelti.tilda.ws",
                        onClick = { openUrl("https://gdyelti.tilda.ws/") },
                        modifier = Modifier.fillMaxWidth(),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = "Open Tilda Hub",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        testTag = "open_tilda_hub_button"
                    )
                }
            }

            // Geometry Dash Physics & Level Design Showcase
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(26.dp),
                testTag = "gd_showcase_card"
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "Geometry Dash Physics",
                            tint = LiquidMagenta,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GEOMETRY DASH PHYSICS & ARCHITECTURE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LiquidMagenta,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val mechanics = listOf(
                        "Wave Dynamics" to "High-precision slope collision physics, dual wave sync, and micro-clicks",
                        "Gravity Inversion" to "Momentum preservation across liquid glass portals and pad vectors",
                        "240Hz Physics Ticks" to "Ultra-tight timing windows, frame alignment, and trigger mechanics",
                        "Liquid Glass Shaders" to "Iridescent visual pulse triggers, glowing chromatic aberration effects"
                    )

                    mechanics.forEach { (title, desc) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = "▸ $title",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = LiquidCyanLight
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Social Networks & Community Links (from gdyelti.tilda.ws)
            Text(
                text = "COMMUNITY & SOCIAL NETWORKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LiquidCyanLight,
                letterSpacing = 1.8.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            YeltiPlatformLinks.forEach { link ->
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = { openUrl(link.url) },
                    testTag = "platform_link_${link.id}"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(link.accentColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = link.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = link.handleOrDescription,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Visit ${link.title}",
                            tint = link.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
