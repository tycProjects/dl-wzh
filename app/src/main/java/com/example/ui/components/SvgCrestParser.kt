package com.example.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.example.ui.theme.parseHexColor

sealed class SvgElement {
    data class RectEl(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val rx: Float,
        val fill: Color?,
        val stroke: Color?,
        val strokeWidth: Float,
    ) : SvgElement()

    data class CircleEl(
        val cx: Float,
        val cy: Float,
        val r: Float,
        val fill: Color?,
        val stroke: Color?,
        val strokeWidth: Float,
    ) : SvgElement()

    data class PolygonEl(
        val points: List<Pair<Float, Float>>,
        val fill: Color?,
        val stroke: Color?,
        val strokeWidth: Float,
    ) : SvgElement()

    data class PathDataEl(
        val d: String,
        val fill: Color?,
        val stroke: Color?,
        val strokeWidth: Float,
    ) : SvgElement()
}

object SvgCrestParser {

    private val elementRegex = Regex(
        """<(rect|circle|polygon|path)\b([^>]*)/?>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val attrRegex = Regex("""([a-zA-Z-]+)\s*=\s*(?:'([^']*)'|"([^"]*)")""")

    fun parse(svgMarkup: String): List<SvgElement> {
        val result = mutableListOf<SvgElement>()
        for (match in elementRegex.findAll(svgMarkup)) {
            val tag = match.groupValues[1].lowercase()
            val attrs = parseAttrs(match.groupValues[2])
            val fill = if ("fill" in attrs) parseSvgColor(attrs["fill"]) else Color.Black
            val stroke = parseSvgColor(attrs["stroke"])
            val strokeWidth = attrs["stroke-width"]?.toFloatOrNull() ?: 1f

            when (tag) {
                "rect" -> {
                    result += SvgElement.RectEl(
                        x = attrs["x"]?.toFloatOrNull() ?: 0f,
                        y = attrs["y"]?.toFloatOrNull() ?: 0f,
                        width = attrs["width"]?.toFloatOrNull() ?: 0f,
                        height = attrs["height"]?.toFloatOrNull() ?: 0f,
                        rx = attrs["rx"]?.toFloatOrNull() ?: 0f,
                        fill = fill,
                        stroke = stroke,
                        strokeWidth = strokeWidth,
                    )
                }
                "circle" -> {
                    result += SvgElement.CircleEl(
                        cx = attrs["cx"]?.toFloatOrNull() ?: 0f,
                        cy = attrs["cy"]?.toFloatOrNull() ?: 0f,
                        r = attrs["r"]?.toFloatOrNull() ?: 0f,
                        fill = fill,
                        stroke = stroke,
                        strokeWidth = strokeWidth,
                    )
                }
                "polygon" -> {
                    val nums = (attrs["points"] ?: "")
                        .trim()
                        .split(Regex("""[\s,]+"""))
                        .mapNotNull { it.toFloatOrNull() }
                    if (nums.size >= 4) {
                        val pts = buildList {
                            var i = 0
                            while (i + 1 < nums.size) {
                                add(nums[i] to nums[i + 1])
                                i += 2
                            }
                        }
                        result += SvgElement.PolygonEl(pts, fill, stroke, strokeWidth)
                    }
                }
                "path" -> {
                    val d = attrs["d"]
                    if (!d.isNullOrBlank()) {
                        result += SvgElement.PathDataEl(d, fill, stroke, strokeWidth)
                    }
                }
            }
        }
        return result
    }

    fun preparePaths(elements: List<SvgElement>): Map<Int, Path> {
        val map = mutableMapOf<Int, Path>()
        elements.forEachIndexed { index, el ->
            when (el) {
                is SvgElement.PolygonEl -> {
                    if (el.points.size >= 2) {
                        map[index] = Path().apply {
                            moveTo(el.points[0].first, el.points[0].second)
                            for (i in 1 until el.points.size) {
                                lineTo(el.points[i].first, el.points[i].second)
                            }
                            close()
                        }
                    }
                }
                is SvgElement.PathDataEl -> {
                    val parsed = runCatching {
                        PathParser().parsePathString(el.d).toPath()
                    }.getOrNull()
                    if (parsed != null) {
                        map[index] = parsed
                    }
                }
                else -> Unit
            }
        }
        return map
    }

    private fun parseAttrs(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (m in attrRegex.findAll(raw)) {
            val key = m.groupValues[1].lowercase()
            val value = m.groupValues[2].ifEmpty { m.groupValues[3] }
            map[key] = value
        }
        return map
    }

    private fun parseSvgColor(raw: String?): Color? {
        if (raw == null || raw.equals("none", ignoreCase = true) || raw.isBlank()) return null
        return parseHexColor(raw, Color.White)
    }
}

fun DrawScope.drawParsedSvg(
    elements: List<SvgElement>,
    preparedPaths: Map<Int, Path>,
) {
    val sx = size.width / 100f
    val sy = size.height / 100f

    clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {
        withTransform({
            scale(scaleX = sx, scaleY = sy, pivot = Offset.Zero)
        }) {
            elements.forEachIndexed { index, el ->
                when (el) {
                    is SvgElement.RectEl -> {
                        val corner = CornerRadius(el.rx, el.rx)
                        val topLeft = Offset(el.x, el.y)
                        val rectSize = Size(el.width, el.height)
                        if (el.fill != null) {
                            drawRoundRect(
                                color = el.fill,
                                topLeft = topLeft,
                                size = rectSize,
                                cornerRadius = corner,
                            )
                        }
                        if (el.stroke != null) {
                            drawRoundRect(
                                color = el.stroke,
                                topLeft = topLeft,
                                size = rectSize,
                                cornerRadius = corner,
                                style = Stroke(width = el.strokeWidth),
                            )
                        }
                    }
                    is SvgElement.CircleEl -> {
                        val center = Offset(el.cx, el.cy)
                        if (el.fill != null) {
                            drawCircle(color = el.fill, radius = el.r, center = center)
                        }
                        if (el.stroke != null) {
                            drawCircle(
                                color = el.stroke,
                                radius = el.r,
                                center = center,
                                style = Stroke(width = el.strokeWidth),
                            )
                        }
                    }
                    is SvgElement.PolygonEl -> {
                        val path = preparedPaths[index]
                        if (path != null) {
                            if (el.fill != null) {
                                drawPath(path = path, color = el.fill)
                            }
                            if (el.stroke != null) {
                                drawPath(
                                    path = path,
                                    color = el.stroke,
                                    style = Stroke(width = el.strokeWidth),
                                )
                            }
                        }
                    }
                    is SvgElement.PathDataEl -> {
                        val path = preparedPaths[index]
                        if (path != null) {
                            if (el.fill != null) {
                                drawPath(path = path, color = el.fill)
                            }
                            if (el.stroke != null) {
                                drawPath(
                                    path = path,
                                    color = el.stroke,
                                    style = Stroke(width = el.strokeWidth),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
