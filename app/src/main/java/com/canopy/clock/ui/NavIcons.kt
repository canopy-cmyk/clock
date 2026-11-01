package com.canopy.clock.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Iconos del menú inferior, dibujados a mano en línea fina con un punto de color
 * cuando la pestaña está activa (como el piloto de un aparato).
 */
@Composable
fun NavIcon(id: String, selected: Boolean, size: Dp = 26.dp) {
    val ink = if (selected) C.Ink else C.Muted
    val accent = if (selected) C.Orange else C.Muted
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.075f
        val st = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (id) {
            "clock" -> clock(s, ink, accent, st)
            "alarms" -> bell(s, ink, accent, st)
            "timer" -> hourglass(s, ink, accent, st, selected)
            "intervals" -> intervals(s, ink, accent, w, selected)
            else -> stopwatch(s, ink, accent, st)
        }
    }
}

private fun DrawScope.clock(s: Float, ink: Color, accent: Color, st: Stroke) {
    val c = Offset(s / 2, s / 2)
    drawCircle(ink, s * 0.42f, c, style = st)
    drawLine(ink, c, Offset(s / 2, s * 0.24f), st.width, StrokeCap.Round)
    drawLine(ink, c, Offset(s * 0.68f, s * 0.58f), st.width, StrokeCap.Round)
    drawCircle(accent, s * 0.07f, c)
}

private fun DrawScope.bell(s: Float, ink: Color, accent: Color, st: Stroke) {
    val p = Path().apply {
        moveTo(s * 0.2f, s * 0.72f)
        lineTo(s * 0.8f, s * 0.72f)
        cubicTo(s * 0.72f, s * 0.62f, s * 0.74f, s * 0.5f, s * 0.74f, s * 0.42f)
        cubicTo(s * 0.74f, s * 0.26f, s * 0.64f, s * 0.16f, s * 0.5f, s * 0.16f)
        cubicTo(s * 0.36f, s * 0.16f, s * 0.26f, s * 0.26f, s * 0.26f, s * 0.42f)
        cubicTo(s * 0.26f, s * 0.5f, s * 0.28f, s * 0.62f, s * 0.2f, s * 0.72f)
        close()
    }
    drawPath(p, ink, style = st)
    drawCircle(accent, s * 0.085f, Offset(s / 2, s * 0.84f))
}

private fun DrawScope.hourglass(s: Float, ink: Color, accent: Color, st: Stroke, sel: Boolean) {
    drawLine(ink, Offset(s * 0.24f, s * 0.14f), Offset(s * 0.76f, s * 0.14f), st.width, StrokeCap.Round)
    drawLine(ink, Offset(s * 0.24f, s * 0.86f), Offset(s * 0.76f, s * 0.86f), st.width, StrokeCap.Round)
    val p = Path().apply {
        moveTo(s * 0.3f, s * 0.14f); lineTo(s * 0.3f, s * 0.26f); lineTo(s * 0.5f, s * 0.5f); lineTo(s * 0.3f, s * 0.74f); lineTo(s * 0.3f, s * 0.86f)
        moveTo(s * 0.7f, s * 0.14f); lineTo(s * 0.7f, s * 0.26f); lineTo(s * 0.5f, s * 0.5f); lineTo(s * 0.7f, s * 0.74f); lineTo(s * 0.7f, s * 0.86f)
    }
    drawPath(p, ink, style = st)
    // arena
    val sand = Path().apply { moveTo(s * 0.36f, s * 0.8f); lineTo(s * 0.64f, s * 0.8f); lineTo(s * 0.5f, s * 0.64f); close() }
    drawPath(sand, accent)
}

private fun DrawScope.intervals(s: Float, ink: Color, accent: Color, w: Float, sel: Boolean) {
    val hs = listOf(0.5f, 0.28f, 0.6f, 0.36f)
    val bw = s * 0.13f
    for ((i, h) in hs.withIndex()) {
        val x = s * 0.15f + i * s * 0.2f
        val top = s * 0.82f - h * s
        val col = if (i % 2 == 0) accent else ink
        if (i % 2 == 0) drawRoundRect(col, Offset(x, top), Size(bw, h * s), CornerRadius(bw / 2))
        else drawRoundRect(col, Offset(x, top), Size(bw, h * s), CornerRadius(bw / 2), style = Stroke(w))
    }
    drawLine(ink, Offset(s * 0.1f, s * 0.9f), Offset(s * 0.9f, s * 0.9f), w, StrokeCap.Round)
}

private fun DrawScope.stopwatch(s: Float, ink: Color, accent: Color, st: Stroke) {
    val c = Offset(s / 2, s * 0.56f)
    drawCircle(ink, s * 0.36f, c, style = st)
    drawLine(ink, Offset(s * 0.42f, s * 0.08f), Offset(s * 0.58f, s * 0.08f), st.width, StrokeCap.Round)
    drawLine(ink, Offset(s / 2, s * 0.08f), Offset(s / 2, s * 0.2f), st.width, StrokeCap.Round)
    drawLine(ink, Offset(s * 0.78f, s * 0.22f), Offset(s * 0.84f, s * 0.28f), st.width, StrokeCap.Round)
    drawLine(accent, c, Offset(s * 0.66f, s * 0.42f), st.width, StrokeCap.Round)
    drawCircle(accent, s * 0.06f, c)
}
