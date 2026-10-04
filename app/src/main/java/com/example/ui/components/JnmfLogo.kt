package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.JnmfTheme
import com.example.ui.theme.JnmfSurfaceElevated
import com.example.ui.theme.JnmfTextSecondary
import java.io.File

@Composable
fun JnmfLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val colors = JnmfTheme.colors
    val context = LocalContext.current
    val logoModel: Any? = remember {
        val candidates = listOf("jnmf_logo", "ic_jnmf_logo", "logo", "ic_matrix_logo")
        var foundRes = 0
        for (name in candidates) {
            val id = context.resources.getIdentifier(name, "drawable", context.packageName)
            if (id != 0) {
                foundRes = id
                break
            }
        }
        if (foundRes != 0) {
            foundRes
        } else {
            // Also detect if the user uploaded the logo as an image file directly
            val fileCandidates = listOf(
                File(context.filesDir, "jnmf_logo.png"),
                File(context.filesDir, "logo.png"),
                File(context.filesDir, "jnmf_logo.webp"),
                File(context.filesDir, "jnmf_logo.jpg"),
                File(context.cacheDir, "jnmf_logo.png")
            )
            fileCandidates.firstOrNull { it.exists() }
        }
    }

    if (logoModel != null) {
        // Displays official uploaded logo preserving original aspect ratio, colors, and transparent background
        // with surrounding themed cyber glow and border matching active UI color
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.glow.copy(alpha = 0.12f))
                .border(1.dp, colors.borderGlow, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = logoModel,
                contentDescription = "JNMF Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size - 4.dp)
            )
        }
    } else {
        // Clean cyber container slot placed directly beside "JNMF" text with active theme glow
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(JnmfSurfaceElevated)
                .border(1.dp, colors.borderGlow, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡",
                color = colors.bright,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun JnmfHeaderTitle(
    subtitle: String? = "INDO REMIX PLAYER",
    modifier: Modifier = Modifier
) {
    val colors = JnmfTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        JnmfLogo(size = 38.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "JNMF",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = colors.primary,
                letterSpacing = 1.5.sp
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "//$subtitle",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = JnmfTextSecondary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}
