package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.badges.ClubSvgBadges
import com.example.model.BadgeStyle
import com.example.model.Club
import com.example.model.ClubCrestSpec
import com.example.model.CrestPattern
import com.example.model.CrestSymbol
import com.example.ui.theme.parseHexColor

@Composable
fun ClubBadge(
    club: Club,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    ClubBadge(
        shortName = club.shortName,
        primaryColorHex = club.primaryColorHex,
        secondaryColorHex = club.secondaryColorHex,
        badgeStyle = club.badgeStyle,
        crestSpec = club.crestSpec,
        modifier = modifier,
        size = size,
    )
}

@Composable
fun ClubBadge(
    shortName: String,
    primaryColorHex: String,
    secondaryColorHex: String,
    badgeStyle: BadgeStyle = BadgeStyle.SHIELD,
    crestSpec: ClubCrestSpec = ClubCrestSpec(),
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val customSvg = crestSpec.customSvg
    if (!customSvg.isNullOrBlank()) {
        val elements = remember(customSvg) { SvgCrestParser.parse(customSvg) }
        val preparedPaths = remember(elements) { SvgCrestParser.preparePaths(elements) }
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawParsedSvg(
                    elements = elements,
                    preparedPaths = preparedPaths,
                )
            }
        }
        return
    }

    val primary = parseHexColor(primaryColorHex)
    val secondary = parseHexColor(secondaryColorHex, Color.White)
    val accent = parseHexColor(crestSpec.accentHex, Color(0xFFF8FAFC))

    val outerPath = remember(crestSpec.shape, badgeStyle) {
        val pathStr = ClubSvgBadges.outerShapePathData(crestSpec.shape)
        PathParser().parsePathString(pathStr).toPath()
    }
    val symbolPath = remember(crestSpec.symbol) {
        val resolved = if (crestSpec.symbol == CrestSymbol.NONE) CrestSymbol.STAR else crestSpec.symbol
        ClubSvgBadges.symbolPathData(resolved)?.let { pathStr ->
            PathParser().parsePathString(pathStr).toPath()
        }
    }
    val innerEmblemOutline = remember {
        PathParser().parsePathString(
            "M 50 18 L 76 28 L 73 56 C 69 70 58 78 50 83 C 42 78 31 70 27 56 L 24 28 Z",
        ).toPath()
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sx = this.size.width / 100f
            val sy = this.size.height / 100f

            withTransform({
                scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero)
            }) {
                // Rich layered shield base
                drawPath(
                    path = outerPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primary,
                            primary.copy(alpha = 0.90f),
                            primary.copy(alpha = 0.76f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f),
                    ),
                )

                // Authentic club crest pattern & pure heraldic vector emblem clipped inside outer shape
                clipPath(outerPath) {
                    when (crestSpec.pattern) {
                        CrestPattern.SOLID -> {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(secondary.copy(alpha = 0.28f), Color.Transparent),
                                    center = Offset(50f, 46f),
                                    radius = 46f,
                                ),
                                radius = 46f,
                                center = Offset(50f, 46f),
                            )
                        }
                        CrestPattern.VERTICAL_STRIPES -> {
                            listOf(12f, 36f, 60f, 84f).forEach { x ->
                                drawRect(
                                    color = secondary.copy(alpha = 0.88f),
                                    topLeft = Offset(x, 0f),
                                    size = Size(12f, 100f),
                                )
                            }
                        }
                        CrestPattern.HOOPS -> {
                            listOf(14f, 38f, 62f, 84f).forEach { y ->
                                drawRect(
                                    color = secondary.copy(alpha = 0.86f),
                                    topLeft = Offset(0f, y),
                                    size = Size(100f, 11f),
                                )
                            }
                        }
                        CrestPattern.HALF_AND_HALF -> {
                            drawRect(
                                color = secondary.copy(alpha = 0.90f),
                                topLeft = Offset(50f, 0f),
                                size = Size(50f, 100f),
                            )
                        }
                        CrestPattern.DIAGONAL_SASH -> {
                            val sash = Path().apply {
                                moveTo(-10f, 75f)
                                lineTo(75f, -10f)
                                lineTo(108f, 12f)
                                lineTo(15f, 105f)
                                close()
                            }
                            drawPath(path = sash, color = secondary.copy(alpha = 0.90f))
                        }
                        CrestPattern.CHEVRON -> {
                            val chev = Path().apply {
                                moveTo(0f, 34f)
                                lineTo(50f, 62f)
                                lineTo(100f, 34f)
                                lineTo(100f, 52f)
                                lineTo(50f, 80f)
                                lineTo(0f, 52f)
                                close()
                            }
                            drawPath(path = chev, color = secondary.copy(alpha = 0.90f))
                        }
                        CrestPattern.CROSS -> {
                            drawRect(
                                color = secondary.copy(alpha = 0.90f),
                                topLeft = Offset(42f, 0f),
                                size = Size(16f, 100f),
                            )
                            drawRect(
                                color = secondary.copy(alpha = 0.90f),
                                topLeft = Offset(0f, 40f),
                                size = Size(100f, 16f),
                            )
                        }
                        CrestPattern.QUARTERS -> {
                            drawRect(
                                color = secondary.copy(alpha = 0.88f),
                                topLeft = Offset(50f, 0f),
                                size = Size(50f, 50f),
                            )
                            drawRect(
                                color = secondary.copy(alpha = 0.88f),
                                topLeft = Offset(0f, 50f),
                                size = Size(50f, 50f),
                            )
                        }
                    }

                    // Subtle inner escutcheon line for multi-tone crest depth
                    drawPath(
                        path = innerEmblemOutline,
                        color = accent.copy(alpha = 0.24f),
                        style = Stroke(width = 1.6f),
                    )

                    // Crisp centered heraldic vector symbol with dark halo for contrast
                    if (symbolPath != null) {
                        drawPath(
                            path = symbolPath,
                            color = Color(0xAA070B12),
                            style = Stroke(width = 4.6f),
                        )
                        drawPath(
                            path = symbolPath,
                            color = accent.copy(alpha = 0.94f),
                        )
                        drawPath(
                            path = symbolPath,
                            color = Color.Black.copy(alpha = 0.45f),
                            style = Stroke(width = 1.1f),
                        )
                    }

                    // Subtle top-left broadcast specular sheen
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                            start = Offset(0f, 0f),
                            end = Offset(72f, 52f),
                        ),
                        topLeft = Offset.Zero,
                        size = Size(100f, 52f),
                    )
                }

                // Inner fine trim ring
                drawPath(
                    path = outerPath,
                    color = Color.Black.copy(alpha = 0.40f),
                    style = Stroke(width = 7.2f),
                )

                // Metallic bevel outer border gradient
                drawPath(
                    path = outerPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            secondary,
                            accent.copy(alpha = 0.95f),
                            secondary.copy(alpha = 0.88f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(100f, 100f),
                    ),
                    style = Stroke(width = 4.8f),
                )
            }
        }
    }
}
