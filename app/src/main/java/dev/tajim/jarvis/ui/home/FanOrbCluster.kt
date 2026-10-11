package dev.tajim.jarvis.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tajim.jarvis.R
import dev.tajim.jarvis.ui.navigation.Workspace
import dev.tajim.jarvis.ui.orb.AnimationClock
import dev.tajim.jarvis.ui.orb.HolographicOrb
import dev.tajim.jarvis.ui.orb.OrbState
import dev.tajim.jarvis.ui.orb.rememberAnimationClock
import dev.tajim.jarvis.ui.theme.LocalJarvisColors
import dev.tajim.jarvis.ui.theme.LocalJarvisMotion

/** Rounded-hexagon blade used by the three primary panels (pointed left and right, like the reference). */
private val BladeShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.20f, 0f)
    lineTo(w * 0.80f, 0f)
    lineTo(w, h * 0.5f)
    lineTo(w * 0.80f, h)
    lineTo(w * 0.20f, h)
    lineTo(0f, h * 0.5f)
    close()
}

// Panel centres as fractions of the cluster square. Upper-left, upper-right, bottom-centre.
private val AnchorStudio = Offset(0.27f, 0.26f)
private val AnchorText = Offset(0.73f, 0.26f)
private val AnchorDraw = Offset(0.50f, 0.76f)

/**
 * The Orb with exactly three fan-blade panels (3D Studio upper-left, Text Lab upper-right, Draw Lab bottom-centre),
 * each tied to the Orb centre by its own animated neon line. Lines are drawn without pointer handling, so they never
 * block touches; the band between the panels leaves the Orb itself tappable.
 */
@Composable
fun FanOrbCluster(
    orbState: OrbState,
    orbActive: Boolean,
    onOrbTap: () -> Unit,
    onOpen: (Workspace) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalJarvisColors.current
    // One frame loop shared by the Orb and the connector pulses; it stops when Home is not resumed.
    val clock = rememberAnimationClock(orbActive)
    BoxWithConstraints(modifier.aspectRatio(1f)) {
        val size = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension * 0.47f
            val ctr = Offset(this.size.width / 2f, this.size.height / 2f)
            drawCircle(c.blue.copy(alpha = 0.18f), r, ctr, style = androidx.compose.ui.graphics.drawscope.Stroke(10.dp.toPx()))
            drawCircle(Brush.sweepGradient(listOf(c.cyan, c.violet, c.magenta, c.blue, c.cyan), ctr), r, ctr, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
        HolographicOrb(state = orbState, clock = clock, modifier = Modifier.fillMaxSize(), onTap = onOrbTap)
        ConnectorLines(
            anchors = listOf(AnchorStudio, AnchorText, AnchorDraw),
            colors = listOf(c.magenta, c.cyan, c.violet),
            clock = clock,
            modifier = Modifier.fillMaxSize(),
        )
        FanBlade(Workspace.STUDIO_3D, c.magenta, c.violet, -9f, AnchorStudio, size) { onOpen(Workspace.STUDIO_3D) }
        FanBlade(Workspace.TEXT_LAB, c.cyan, c.blue, 9f, AnchorText, size) { onOpen(Workspace.TEXT_LAB) }
        FanBlade(Workspace.DRAW_LAB, c.violet, c.magenta, 0f, AnchorDraw, size) { onOpen(Workspace.DRAW_LAB) }
    }
}

@Composable
private fun FanBlade(
    workspace: Workspace,
    accent: Color,
    accent2: Color,
    tilt: Float,
    anchor: Offset,
    cluster: Dp,
    onClick: () -> Unit,
) {
    val c = LocalJarvisColors.current
    val motion = LocalJarvisMotion.current
    val w = cluster * 0.49f
    val h = cluster * 0.33f
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !motion.reduced) 0.95f else 1f, label = "bladePress")
    val title = stringResource(workspace.title)
    val openLabel = stringResource(R.string.workspace_open, title)

    Box(
        Modifier
            .offset(x = cluster * anchor.x - w / 2, y = cluster * anchor.y - h / 2)
            .size(w, h)
            .graphicsLayer { scaleX = scale; scaleY = scale },
    ) {
        // Angled glass blade (tilted); the label stays upright on top of it.
        Box(
            Modifier
                .fillMaxSize()
                .rotate(tilt)
                .clip(BladeShape)
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.34f), accent2.copy(alpha = 0.10f))))
                .border(1.5.dp, Brush.linearGradient(listOf(accent, accent2)), BladeShape),
        )
        Column(
            Modifier
                .fillMaxSize()
                .clip(BladeShape)
                .semantics { contentDescription = openLabel }
                .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Icon(workspace.icon, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(22.dp))
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = c.textPrimary, textAlign = TextAlign.Center, maxLines = 1)
            Text(stringResource(workspace.subtitle), fontSize = 7.sp, color = c.textSecondary, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

/** Three independent neon lines from each panel to the Orb centre, each with a travelling pulse driven by the shared clock. */
@Composable
private fun ConnectorLines(anchors: List<Offset>, colors: List<Color>, clock: AnimationClock, modifier: Modifier) {
    val motion = LocalJarvisMotion.current
    Canvas(modifier) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val phase = (clock.time * 0.45f) % 1f
        anchors.forEachIndexed { i, a ->
            val from = Offset(a.x * size.width, a.y * size.height)
            drawLine(colors[i].copy(alpha = 0.18f), from, centre, 7.dp.toPx(), StrokeCap.Round)
            drawLine(colors[i], from, centre, 1.6.dp.toPx(), StrokeCap.Round)
            if (!motion.reduced) {
                val p = lerp(from, centre, (phase + i * 0.33f) % 1f)
                drawCircle(colors[i].copy(alpha = 0.35f), 6.dp.toPx(), p)
                drawCircle(Color.White, 2.dp.toPx(), p)
            }
        }
    }
}
