package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Water
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FluidParticle
import com.example.model.SoluteType
import com.example.model.TonicityState
import com.example.ui.components.LiquidBackgroundCanvas
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.GlucoseEmerald
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidCyanLight
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LiquidMagenta
import com.example.ui.theme.LiquidTeal
import com.example.ui.theme.LiquidViolet
import com.example.ui.theme.ProteinPurple
import com.example.ui.theme.SodiumAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WaterBlue
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun OsmosisSimulationScreen() {
    var selectedTonicity by remember { mutableStateOf(TonicityState.ISOTONIC) }
    var aquaporinsOpen by remember { mutableStateOf(true) }
    var ionPumpActive by remember { mutableStateOf(true) }
    var intracellularOsmolarity by remember { mutableFloatStateOf(290f) }
    var extracellularOsmolarity by remember { mutableFloatStateOf(290f) }

    // Animated cell volume expansion ratio
    val cellVolumeRatio by animateFloatAsState(
        targetValue = selectedTonicity.cellVolumeRatio,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "CellVolumeAnim"
    )

    // Molecular fluid particles
    val particles = remember {
        mutableStateListOf<FluidParticle>().apply {
            // Water molecules (high count)
            repeat(35) {
                add(
                    FluidParticle(
                        x = Random.nextFloat() * 800f + 50f,
                        y = Random.nextFloat() * 450f + 25f,
                        vx = (Random.nextFloat() - 0.5f) * 4f,
                        vy = (Random.nextFloat() - 0.5f) * 4f,
                        type = SoluteType.WATER,
                        radius = 4f
                    )
                )
            }
            // Sodium ions
            repeat(14) {
                add(
                    FluidParticle(
                        x = Random.nextFloat() * 800f + 50f,
                        y = Random.nextFloat() * 450f + 25f,
                        vx = (Random.nextFloat() - 0.5f) * 2.5f,
                        vy = (Random.nextFloat() - 0.5f) * 2.5f,
                        type = SoluteType.SODIUM,
                        radius = 6.5f
                    )
                )
            }
            // Glucose
            repeat(8) {
                add(
                    FluidParticle(
                        x = Random.nextFloat() * 800f + 50f,
                        y = Random.nextFloat() * 450f + 25f,
                        vx = (Random.nextFloat() - 0.5f) * 1.8f,
                        vy = (Random.nextFloat() - 0.5f) * 1.8f,
                        type = SoluteType.GLUCOSE,
                        radius = 8f
                    )
                )
            }
            // Albumin Protein
            repeat(4) {
                add(
                    FluidParticle(
                        x = Random.nextFloat() * 800f + 50f,
                        y = Random.nextFloat() * 450f + 25f,
                        vx = (Random.nextFloat() - 0.5f) * 1.2f,
                        vy = (Random.nextFloat() - 0.5f) * 1.2f,
                        type = SoluteType.PROTEIN,
                        radius = 11f
                    )
                )
            }
        }
    }

    // Particle kinetics & osmosis flux physics loop
    LaunchedEffect(selectedTonicity, aquaporinsOpen) {
        when (selectedTonicity) {
            TonicityState.HYPOTONIC -> {
                extracellularOsmolarity = 140f
                intracellularOsmolarity = 310f
            }
            TonicityState.ISOTONIC -> {
                extracellularOsmolarity = 290f
                intracellularOsmolarity = 290f
            }
            TonicityState.HYPERTONIC -> {
                extracellularOsmolarity = 480f
                intracellularOsmolarity = 275f
            }
        }

        while (true) {
            val netOsmoticBias = when (selectedTonicity) {
                TonicityState.HYPOTONIC -> if (aquaporinsOpen) 0.85f else 0.1f // Water flows inward (towards right)
                TonicityState.ISOTONIC -> 0.0f
                TonicityState.HYPERTONIC -> if (aquaporinsOpen) -0.95f else -0.1f // Water flows outward (towards left)
            }

            particles.forEach { p ->
                p.x += p.vx
                p.y += p.vy

                if (p.type == SoluteType.WATER) {
                    p.vx += netOsmoticBias * 0.05f
                }

                // Bounce off boundaries
                if (p.x < 30f) {
                    p.x = 30f
                    p.vx = -p.vx
                } else if (p.x > 850f) {
                    p.x = 850f
                    p.vx = -p.vx
                }

                if (p.y < 20f) {
                    p.y = 20f
                    p.vy = -p.vy
                } else if (p.y > 480f) {
                    p.y = 480f
                    p.vy = -p.vy
                }
            }
            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackgroundCanvas(
            primaryColor = LiquidTeal,
            secondaryColor = LiquidCyan,
            tertiaryColor = LiquidViolet
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp)
        ) {
            // Screen Header
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                testTag = "osmosis_header_card"
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Cellular Osmosis Lab",
                            tint = LiquidTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "BIOSCIENCE CELLULAR ENGINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LiquidTeal,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Membrane Osmosis & Tonus",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Real-time biophysical simulation of cellular lipid bilayer permeation, osmotic pressure gradients (Π = iCRT), and fluid volume homeostasis.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Tonicity Selector Chips
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                testTag = "tonicity_selector_card"
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "EXTRACELLULAR TONICITY REGIME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidCyanLight,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TonicityState.values().forEach { state ->
                            val isSelected = selectedTonicity == state
                            val stateColor = when (state) {
                                TonicityState.HYPOTONIC -> LiquidCyan
                                TonicityState.ISOTONIC -> LiquidTeal
                                TonicityState.HYPERTONIC -> LiquidMagenta
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) stateColor.copy(alpha = 0.28f)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.8.dp,
                                        color = if (isSelected) stateColor else Color.White.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedTonicity = state }
                                    .padding(vertical = 10.dp, horizontal = 6.dp)
                                    .testTag("tonicity_chip_${state.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = state.name.replace("_", " "),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = selectedTonicity.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            }

            // Cell Membrane Simulation Canvas
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(32.dp),
                testTag = "cellular_membrane_viewport"
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val membraneX = w * 0.45f

                    // 1. Extracellular Region (Left)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0C1929).copy(alpha = 0.7f),
                                Color(0xFF0F263D).copy(alpha = 0.5f)
                            ),
                            startX = 0f,
                            endX = membraneX
                        ),
                        topLeft = Offset(0f, 0f),
                        size = androidx.compose.ui.geometry.Size(membraneX, h)
                    )

                    // 2. Intracellular Cytoplasm Region (Right) - responds to cell volume expansion
                    val cytoplasmWidth = (w - membraneX) * cellVolumeRatio
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                LiquidTeal.copy(alpha = 0.15f),
                                LiquidCyan.copy(alpha = 0.08f)
                            ),
                            startX = membraneX,
                            endX = w
                        ),
                        topLeft = Offset(membraneX, 0f),
                        size = androidx.compose.ui.geometry.Size(w - membraneX, h)
                    )

                    // 3. Phospholipid Bilayer Membrane (Fluid Liquid Glass)
                    val membraneWidth = 36f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.6f),
                                LiquidCyanLight.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.7f)
                            )
                        ),
                        topLeft = Offset(membraneX - membraneWidth / 2, 0f),
                        size = androidx.compose.ui.geometry.Size(membraneWidth, h)
                    )

                    // Draw lipid bilayer polar heads
                    var lipidY = 15f
                    while (lipidY < h) {
                        // Outer polar head (extracellular side)
                        drawCircle(
                            color = LiquidCyanLight.copy(alpha = 0.9f),
                            center = Offset(membraneX - 14f, lipidY),
                            radius = 4.5f
                        )
                        // Inner polar head (cytoplasmic side)
                        drawCircle(
                            color = LiquidTeal.copy(alpha = 0.9f),
                            center = Offset(membraneX + 14f, lipidY),
                            radius = 4.5f
                        )
                        // Hydrophobic tails
                        drawLine(
                            color = Color.White.copy(alpha = 0.4f),
                            start = Offset(membraneX - 10f, lipidY),
                            end = Offset(membraneX + 10f, lipidY),
                            strokeWidth = 1.2f
                        )
                        lipidY += 14f
                    }

                    // Aquaporin Channel Indicator
                    val aquaporinY = h * 0.45f
                    drawRoundRect(
                        color = if (aquaporinsOpen) LiquidTeal else Color(0xFF64748B),
                        topLeft = Offset(membraneX - 18f, aquaporinY - 24f),
                        size = androidx.compose.ui.geometry.Size(36f, 48f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    drawCircle(
                        color = if (aquaporinsOpen) Color.White else Color.Black,
                        center = Offset(membraneX, aquaporinY),
                        radius = 8f
                    )

                    // Render moving molecular particles
                    particles.forEach { p ->
                        // Scale coordinates to fit canvas
                        val px = (p.x / 900f) * w
                        val py = (p.y / 500f) * h

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    p.type.color,
                                    p.type.color.copy(alpha = 0.3f)
                                ),
                                center = Offset(px, py),
                                radius = p.radius * 1.5f
                            ),
                            center = Offset(px, py),
                            radius = p.radius
                        )
                    }
                }

                // Region Labels
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "EXTRACELLULAR\nFLUID",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidCyanLight.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    Text(
                        text = "CYTOPLASM\n(INTRACELLULAR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidTeal.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    // Live volume state badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Cell Volume: ${(cellVolumeRatio * 100).toInt()}% of Normal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Real-Time Biophysics Metric Panel
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                testTag = "osmotic_metrics_card"
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "BIOPHYSICAL READOUTS (VAN 'T HOFF EQUATION)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LiquidCyanLight,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Metric 1: Extracellular Osmolarity
                        Column {
                            Text(text = "Extracellular", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = "${extracellularOsmolarity.toInt()} mOsm/L",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LiquidCyan
                            )
                        }

                        // Metric 2: Intracellular Osmolarity
                        Column {
                            Text(text = "Intracellular", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = "${intracellularOsmolarity.toInt()} mOsm/L",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LiquidTeal
                            )
                        }

                        // Metric 3: Osmotic Pressure Difference (ΔΠ)
                        val deltaP = (intracellularOsmolarity - extracellularOsmolarity) * 0.0254f
                        Column {
                            Text(text = "ΔΠ (Pressure)", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = "%.2f atm".format(deltaP),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (deltaP > 0) LiquidCyanLight else LiquidMagenta
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Channel Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Aquaporin Channels (AQP1)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (aquaporinsOpen) "Water permeable (Open pore)" else "Gated closed",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        Switch(
                            checked = aquaporinsOpen,
                            onCheckedChange = { aquaporinsOpen = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LiquidTeal,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.testTag("aquaporin_switch")
                        )
                    }
                }
            }
        }
    }
}
