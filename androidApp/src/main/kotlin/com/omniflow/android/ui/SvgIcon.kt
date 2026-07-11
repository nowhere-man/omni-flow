package com.omniflow.android.ui

import android.util.Xml
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import org.xmlpull.v1.XmlPullParser

private sealed interface SvgShape {
    data class PathShape(val path: Path) : SvgShape
    data class Line(val start: Offset, val end: Offset) : SvgShape
    data class Circle(val center: Offset, val radius: Float) : SvgShape
    data class Rectangle(val rect: Rect, val radius: Float) : SvgShape
    data class Polyline(val points: List<Offset>) : SvgShape
}

@Composable
internal fun SvgIcon(iconKey: String, modifier: Modifier = Modifier, tint: Color = androidx.compose.material3.LocalContentColor.current) {
    val context = LocalContext.current
    val shapes = remember(iconKey) {
        runCatching {
            context.assets.open("icons/$iconKey.svg").use { input ->
                val parser = Xml.newPullParser().apply { setInput(input, null) }
                buildList {
                    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.START_TAG) parser.shape()?.let(::add)
                        parser.next()
                    }
                }
            }
        }.getOrElse {
            context.assets.open("icons/category.svg").use { input ->
                val parser = Xml.newPullParser().apply { setInput(input, null) }
                buildList {
                    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.START_TAG) parser.shape()?.let(::add)
                        parser.next()
                    }
                }
            }
        }
    }
    Canvas(modifier.size(24.dp)) {
        val scale = size.minDimension / 24f
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            val stroke = Stroke(width = 2f)
            shapes.forEach { shape ->
                when (shape) {
                    is SvgShape.PathShape -> drawPath(shape.path, tint, style = stroke)
                    is SvgShape.Line -> drawLine(tint, shape.start, shape.end, strokeWidth = 2f)
                    is SvgShape.Circle -> drawCircle(tint, shape.radius, shape.center, style = stroke)
                    is SvgShape.Rectangle -> drawRoundRect(
                        color = tint,
                        topLeft = shape.rect.topLeft,
                        size = shape.rect.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.radius),
                        style = stroke,
                    )
                    is SvgShape.Polyline -> shape.points.zipWithNext().forEach { (start, end) ->
                        drawLine(tint, start, end, strokeWidth = 2f)
                    }
                }
            }
        }
    }
}

private fun XmlPullParser.shape(): SvgShape? = when (name) {
    "path" -> attribute("d")?.let { SvgShape.PathShape(PathParser().parsePathString(it).toPath()) }
    "line" -> SvgShape.Line(
        Offset(attribute("x1").float(), attribute("y1").float()),
        Offset(attribute("x2").float(), attribute("y2").float()),
    )
    "circle" -> SvgShape.Circle(
        Offset(attribute("cx").float(), attribute("cy").float()),
        attribute("r").float(),
    )
    "rect" -> {
        val x = attribute("x").float()
        val y = attribute("y").float()
        SvgShape.Rectangle(
            Rect(x, y, x + attribute("width").float(), y + attribute("height").float()),
            attribute("rx").float(),
        )
    }
    "polyline", "polygon" -> SvgShape.Polyline(
        attribute("points").orEmpty().trim().split(Regex("[ ,]+")).chunked(2).mapNotNull { pair ->
            if (pair.size == 2) Offset(pair[0].toFloat(), pair[1].toFloat()) else null
        },
    )
    else -> null
}

private fun XmlPullParser.attribute(name: String): String? = getAttributeValue(null, name)
private fun String?.float(): Float = this?.toFloatOrNull() ?: 0f
