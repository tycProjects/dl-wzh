package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LiquidMagenta

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = LiquidGlassSurface,
    borderColors: List<Color> = listOf(
        Color.White.copy(alpha = 0.50f),
        LiquidCyan.copy(alpha = 0.35f),
        Color.White.copy(alpha = 0.15f),
        LiquidMagenta.copy(alpha = 0.30f),
        Color.White.copy(alpha = 0.40f)
    ),
    borderWidth: Dp = 1.2.dp,
    hasShimmer: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "CardPressScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .liquidGlass(
                shape = shape,
                backgroundColor = backgroundColor,
                borderColors = borderColors,
                borderWidth = borderWidth,
                hasSpecularGlare = true
            )
            .then(if (hasShimmer) Modifier.liquidShimmer() else Modifier)
    ) {
        content()
    }
}
