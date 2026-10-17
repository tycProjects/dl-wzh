package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CellularOrganelle
import com.example.model.SampleOrganelles
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DnaOrganellesScreen() {
    var expandedOrganelleId by remember { mutableStateOf<String?>("mitochondria") }

    // DNA Transcription sequence state
    val transcribedCodons = remember {
        mutableStateListOf("AUG (Met)", "GCC (Ala)", "UUG (Leu)", "UAA (Stop)")
    }

    val infiniteTransition = rememberInfiniteTransition(label = "DnaHelixRotate")
    val helixAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HelixAngle"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackgroundCanvas(
            primaryColor = LiquidViolet,
            secondaryColor = LiquidMagenta,
            tertiaryColor = LiquidCyan
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp)
        ) {
            // Header
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                testTag = "genetics_header_card"
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "MACROMOLECULAR BIOLOGY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidViolet,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "DNA Double Helix & Organelles",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Explore 3D rotating iridescent nucleic acid polymer chains, RNA transcription, and sub-cellular organellar bioenergetics.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // 3D Rotating Iridescent DNA Helix Canvas
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(32.dp),
                testTag = "dna_helix_viewport"
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerX = w * 0.5f

                    // Render 22 base pair rungs along the vertical double helix
                    val totalSteps = 22
                    val stepHeight = h / totalSteps

                    for (i in 0 until totalSteps) {
                        val y = i * stepHeight + 12f
                        val angle = helixAngle + (i * 0.45f)
                        val radiusX = w * 0.32f

                        // 3D projected coordinates
                        val x1 = centerX + radiusX * sin(angle.toDouble()).toFloat()
                        val z1 = cos(angle.toDouble()).toFloat() // Depth factor: -1 (back) to +1 (front)

                        val x2 = centerX - radiusX * sin(angle.toDouble()).toFloat()
                        val z2 = -z1

                        // Base colors
                        val baseColor1 = if (i % 2 == 0) LiquidCyan else LiquidMagenta
                        val baseColor2 = if (i % 2 == 0) LiquidTeal else LiquidViolet

                        // Draw connecting hydrogen bond rung
                        val rungAlpha = ((z1 + 1f) * 0.35f + 0.2f).coerceIn(0.15f, 0.9f)
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    baseColor1.copy(alpha = rungAlpha),
                                    Color.White.copy(alpha = rungAlpha * 0.8f),
                                    baseColor2.copy(alpha = rungAlpha)
                                ),
                                startX = x1,
                                endX = x2
                            ),
                            start = Offset(x1, y),
                            end = Offset(x2, y),
                            strokeWidth = 3.5f * (z1 * 0.4f + 0.8f)
                        )

                        // Strand 1 node (Phosphate-deoxyribose backbone)
                        val node1Radius = 7f * (z1 * 0.4f + 0.8f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    baseColor1,
                                    baseColor1.copy(alpha = 0.4f)
                                ),
                                center = Offset(x1, y),
                                radius = node1Radius * 1.5f
                            ),
                            center = Offset(x1, y),
                            radius = node1Radius
                        )

                        // Strand 2 node
                        val node2Radius = 7f * (z2 * 0.4f + 0.8f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    baseColor2,
                                    baseColor2.copy(alpha = 0.4f)
                                ),
                                center = Offset(x2, y),
                                radius = node2Radius * 1.5f
                            ),
                            center = Offset(x2, y),
                            radius = node2Radius
                        )
                    }
                }

                // Overlay Controls
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "3D IRIDESCENT\nDOUBLE HELIX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidViolet.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    LiquidGlassButton(
                        text = "+ Transcribe Codon",
                        onClick = {
                            val codons = listOf("UGU (Cys)", "GAA (Glu)", "CCA (Pro)", "UGG (Trp)", "AUU (Ile)")
                            if (transcribedCodons.size < 8) {
                                transcribedCodons.add(codons.random())
                            }
                        },
                        testTag = "transcribe_codon_button",
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            // Transcribed Polypeptide Chain Card
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                testTag = "polypeptide_chain_card"
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSCRIBED mRNA POLYPEPTIDE CHAIN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = LiquidCyanLight,
                            letterSpacing = 1.2.sp
                        )

                        IconButton(
                            onClick = {
                                transcribedCodons.clear()
                                transcribedCodons.addAll(listOf("AUG (Met)", "GCC (Ala)", "UUG (Leu)", "UAA (Stop)"))
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Codons",
                                tint = LiquidCyanLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        transcribedCodons.forEach { codon ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, LiquidViolet.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = codon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Cellular Organelles Section
            Text(
                text = "CELLULAR ORGANELLE ATLAS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = LiquidTeal,
                letterSpacing = 1.8.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            SampleOrganelles.forEach { organelle ->
                val isExpanded = expandedOrganelleId == organelle.id

                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(24.dp),
                    onClick = {
                        expandedOrganelleId = if (isExpanded) null else organelle.id
                    },
                    testTag = "organelle_card_${organelle.id}"
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(organelle.visualHue)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = organelle.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = organelle.scientificName,
                                        fontSize = 12.sp,
                                        color = organelle.visualHue
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Details",
                                tint = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = organelle.role,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(
                                    text = organelle.description,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Energy Yield",
                                        tint = LiquidCyanLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bioenergetics: ${organelle.energyProduct}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LiquidCyanLight
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    organelle.keyFeatures.forEach { feature ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "•",
                                                color = organelle.visualHue,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = feature,
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
