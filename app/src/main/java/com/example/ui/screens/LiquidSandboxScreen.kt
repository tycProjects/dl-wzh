package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LiquidBlob
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
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun LiquidSandboxScreen() {
    var viscosity by remember { mutableFloatStateOf(0.55f) }
    var surfaceTension by remember { mutableFloatStateOf(0.70f) }
    var refractionIndex by remember { mutableFloatStateOf(1.42f) }
    var waveIntensity by remember { mutableFloatStateOf(1.0f) }

    val blobs = remember {
        mutableStateListOf(
            LiquidBlob(1, 200f, 300f, 3.2f, 2.5f, 65f, LiquidCyan, LiquidCyanLight, "Cyan Drop"),
            LiquidBlob(2, 500f, 450f, -2.8f, 3.1f, 85f, LiquidViolet, LiquidMagenta, "Violet Core"),
            LiquidBlob(3, 350f, 700f, 2.1f, -2.6f, 75f, LiquidMagenta, LiquidCyan, "Magenta Viscosity"),
            LiquidBlob(4, 650f, 250f, -3.5f, 1.8f, 55f, LiquidTeal, LiquidCyanLight, "Teal Solute")
        )
    }

    var draggedBlobIndex by remember { mutableStateOf<Int?>(null) }

    // Real-time fluid animation simulation loop
    LaunchedEffect(viscosity, surfaceTension) {
        val dt = 0.016f
        while (true) {
            val damping = 1.0f - (viscosity * 0.04f)
            val tensionForce = surfaceTension * 0.15f

            blobs.forEachIndexed { index, blob ->
                if (draggedBlobIndex != index) {
                    // Update position
                    blob.x += blob.vx
                    blob.y += blob.vy

                    // Wall collision bounds (using estimated canvas limits)
                    val minX = blob.radius + 20f
                    val maxX = 1000f - blob.radius - 20f
                    val minY = blob.radius + 20f
                    val maxY = 900f - blob.radius - 20f

                    if (blob.x <= minX) {
                        blob.x = minX
                        blob.vx = -blob.vx * damping
                    } else if (blob.x >= maxX) {
                        blob.x = maxX
                        blob.vx = -blob.vx * damping
                    }

                    if (blob.y <= minY) {
                        blob.y = minY
                        blob.vy = -blob.vy * damping
                    } else if (blob.y >= maxY) {
                        blob.y = maxY
                        blob.vy = -blob.vy * damping
                    }

                    // Gentle drift towards center based on surface tension
                    val centerX = 500f
                    val centerY = 450f
                    val dx = centerX - blob.x
                    val dy = centerY - blob.y
                    blob.vx += dx * tensionForce * 0.001f
                    blob.vy += dy * tensionForce * 0.001f

                    // Velocity dampening
                    blob.vx *= damping
                    blob.vy *= damping
                }
            }
            delay(16)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackgroundCanvas(
            waveIntensity = waveIntensity,
            primaryColor = LiquidCyan,
            secondaryColor = LiquidViolet,
            tertiaryColor = LiquidMagenta
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
                testTag = "liquid_sandbox_header"
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LIQUID GLASS LAB",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LiquidCyanLight,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Fluid & Optical Dynamics",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }

                        IconButton(
                            onClick = {
                                blobs.clear()
                                blobs.addAll(
                                    listOf(
                                        LiquidBlob(1, 250f, 300f, 3.2f, 2.5f, 65f, LiquidCyan, LiquidCyanLight, "Cyan Drop"),
                                        LiquidBlob(2, 550f, 400f, -2.8f, 3.1f, 85f, LiquidViolet, LiquidMagenta, "Violet Core"),
                                        LiquidBlob(3, 400f, 600f, 2.1f, -2.6f, 75f, LiquidMagenta, LiquidCyan, "Magenta Viscosity"),
                                        LiquidBlob(4, 650f, 250f, -3.5f, 1.8f, 55f, LiquidTeal, LiquidCyanLight, "Teal Solute")
                                    )
                                )
                            },
                            modifier = Modifier.testTag("reset_blobs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Fluid Simulation",
                                tint = LiquidCyanLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Touch, drag, or tap the interactive viewport to spawn and blend iridescent liquid glass droplets with viscous refraction physics.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Interactive Physics Viewport
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(32.dp),
                testTag = "fluid_interactive_viewport"
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "LiquidGleam")
                val gleamPulse by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "GleamPulse"
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                // Spawn new blob at tap location
                                if (blobs.size < 10) {
                                    val randomColor = listOf(LiquidCyan, LiquidViolet, LiquidMagenta, LiquidTeal).random()
                                    blobs.add(
                                        LiquidBlob(
                                            id = blobs.size + 1,
                                            x = tapOffset.x,
                                            y = tapOffset.y,
                                            vx = (Random.nextFloat() - 0.5f) * 6f,
                                            vy = (Random.nextFloat() - 0.5f) * 6f,
                                            radius = Random.nextFloat() * 30f + 45f,
                                            color = randomColor,
                                            accentColor = Color.White,
                                            label = "Droplet #${blobs.size + 1}"
                                        )
                                    )
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { startOffset ->
                                    // Find clicked blob
                                    val touchedIndex = blobs.indexOfFirst { blob ->
                                        val dist = sqrt((blob.x - startOffset.x) * (blob.x - startOffset.x) + (blob.y - startOffset.y) * (blob.y - startOffset.y))
                                        dist <= blob.radius * 1.3f
                                    }
                                    if (touchedIndex != -1) {
                                        draggedBlobIndex = touchedIndex
                                    }
                                },
                                onDragEnd = {
                                    draggedBlobIndex = null
                                },
                                onDragCancel = {
                                    draggedBlobIndex = null
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    draggedBlobIndex?.let { index ->
                                        if (index < blobs.size) {
                                            blobs[index].x += dragAmount.x
                                            blobs[index].y += dragAmount.y
                                            blobs[index].vx = dragAmount.x * 0.8f
                                            blobs[index].vy = dragAmount.y * 0.8f
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Draw connecting fluid bridges (metaball effect approximation)
                    for (i in 0 until blobs.size) {
                        for (j in i + 1 until blobs.size) {
                            val b1 = blobs[i]
                            val b2 = blobs[j]
                            val dx = b2.x - b1.x
                            val dy = b2.y - b1.y
                            val dist = sqrt(dx * dx + dy * dy)
                            val maxBlendDist = (b1.radius + b2.radius) * 1.5f

                            if (dist < maxBlendDist && dist > 1f) {
                                val blendFactor = (1f - dist / maxBlendDist).coerceIn(0f, 1f)
                                drawLine(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            b1.color.copy(alpha = 0.35f * blendFactor),
                                            b2.color.copy(alpha = 0.35f * blendFactor)
                                        ),
                                        start = Offset(b1.x, b1.y),
                                        end = Offset(b2.x, b2.y)
                                    ),
                                    start = Offset(b1.x, b1.y),
                                    end = Offset(b2.x, b2.y),
                                    strokeWidth = (b1.radius + b2.radius) * 0.45f * blendFactor
                                )
                            }
                        }
                    }

                    // Render each Liquid Glass Blob
                    blobs.forEach { blob ->
                        val r = blob.radius * (if (draggedBlobIndex == blobs.indexOf(blob)) 1.12f else 1.0f)

                        // 1. Fluid glow aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    blob.color.copy(alpha = 0.40f),
                                    blob.color.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = Offset(blob.x, blob.y),
                                radius = r * 1.6f * gleamPulse
                            ),
                            center = Offset(blob.x, blob.y),
                            radius = r * 1.6f * gleamPulse
                        )

                        // 2. Liquid Glass Translucent Body
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    blob.color.copy(alpha = 0.70f),
                                    blob.accentColor.copy(alpha = 0.85f)
                                ),
                                center = Offset(blob.x - r * 0.28f, blob.y - r * 0.28f),
                                radius = r * 1.1f
                            ),
                            center = Offset(blob.x, blob.y),
                            radius = r
                        )

                        // 3. Chromatic Edge Refraction Ring
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    LiquidCyanLight.copy(alpha = 0.7f),
                                    LiquidMagenta.copy(alpha = 0.8f),
                                    Color.White.copy(alpha = 0.9f)
                                ),
                                center = Offset(blob.x, blob.y)
                            ),
                            center = Offset(blob.x, blob.y),
                            radius = r,
                            style = Stroke(width = 2.4f)
                        )

                        // 4. Specular Curved Liquid Highlight
                        drawCircle(
                            color = Color.White.copy(alpha = 0.75f),
                            center = Offset(blob.x - r * 0.32f, blob.y - r * 0.32f),
                            radius = r * 0.22f
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.45f),
                            center = Offset(blob.x - r * 0.15f, blob.y - r * 0.42f),
                            radius = r * 0.10f
                        )
                    }
                }

                // Overlay badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                ) {
                    LiquidGlassButton(
                        text = "+ Spawn Drop (${blobs.size}/10)",
                        onClick = {
                            if (blobs.size < 10) {
                                val randomColor = listOf(LiquidCyan, LiquidViolet, LiquidMagenta, LiquidTeal).random()
                                blobs.add(
                                    LiquidBlob(
                                        id = blobs.size + 1,
                                        x = Random.nextFloat() * 400f + 200f,
                                        y = Random.nextFloat() * 300f + 150f,
                                        vx = (Random.nextFloat() - 0.5f) * 5f,
                                        vy = (Random.nextFloat() - 0.5f) * 5f,
                                        radius = Random.nextFloat() * 25f + 50f,
                                        color = randomColor,
                                        accentColor = Color.White,
                                        label = "Droplet #${blobs.size + 1}"
                                    )
                                )
                            }
                        },
                        testTag = "spawn_drop_button",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Spawn Liquid Droplet",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // Physics Parameter Controls Card
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                testTag = "liquid_physics_controls"
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Fluid Mechanics",
                            tint = LiquidCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fluid Physics & Optical Parameters",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Viscosity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Viscosity (η)", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            text = "${(viscosity * 100).toInt()}% mPa·s",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LiquidCyan
                        )
                    }
                    Slider(
                        value = viscosity,
                        onValueChange = { viscosity = it },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = LiquidCyanLight,
                            activeTrackColor = LiquidCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("viscosity_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Surface Tension Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Surface Tension (γ)", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            text = "${(surfaceTension * 72.8).toInt()} mN/m",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LiquidViolet
                        )
                    }
                    Slider(
                        value = surfaceTension,
                        onValueChange = { surfaceTension = it },
                        valueRange = 0.2f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = LiquidViolet,
                            activeTrackColor = LiquidViolet,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("surface_tension_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Refraction Index Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Refraction Index (n)", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            text = "n = %.2f (Silicate Glass)".format(refractionIndex),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LiquidMagenta
                        )
                    }
                    Slider(
                        value = refractionIndex,
                        onValueChange = { refractionIndex = it },
                        valueRange = 1.0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = LiquidMagenta,
                            activeTrackColor = LiquidMagenta,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("refraction_slider")
                    )
                }
            }
        }
    }
}
