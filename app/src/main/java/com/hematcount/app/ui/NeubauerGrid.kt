package com.hematcount.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.hematcount.app.ui.theme.LocalClinicalColors

/**
 * Improved Neubauer ruling drawn as a clean, tappable diagram.
 * 3 × 3 large squares (1 mm each): corners ruled 4 × 4 (leucocyte squares, "W"),
 * side squares 4 × 5, centre 5 × 5 groups of 16 (red cell / platelet square, "R" squares).
 * Large squares are "L0".."L8" (row-major); the 25 central squares "S0".."S24".
 */
@Composable
fun NeubauerGrid(
    selected: List<String>,
    smallSquareMode: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalClinicalColors.current.isDark
    val glassTop = if (dark) Color(0xFF17303B) else Color(0xFFF4FAFD)
    val glassBottom = if (dark) Color(0xFF0F2029) else Color(0xFFE3F0F6)
    val edge = if (dark) Color(0xFF35596A) else Color(0xFFBFD5E0)
    val rule = if (dark) Color(0xFF8DB4C6) else Color(0xFF4F7488)
    val fine = rule.copy(alpha = if (dark) 0.32f else 0.28f)
    val squareFill = if (dark) Color(0x0FFFFFFF) else Color(0x99FFFFFF)
    val sel = scheme.primary
    val label = rule.copy(alpha = 0.55f)
    val measurer = rememberTextMeasurer()
    val sel1 = selected.toSet()

    Canvas(
        modifier
            .semantics { contentDescription = "Neubauer chamber, ${selected.size} squares selected. Tap a square to add or remove it." }
            .pointerInput(selected, smallSquareMode) {
                detectTapGestures { pos ->
                    val g = geometry(size.width.toFloat(), size.height.toFloat())
                    for (i in 0..8) {
                        val r = g.square(i)
                        if (!r.contains(pos)) continue
                        if (i == 4 && smallSquareMode && "L4" !in selected) {
                            val s = r.width / 5f
                            val cx = ((pos.x - r.left) / s).toInt().coerceIn(0, 4)
                            val cy = ((pos.y - r.top) / s).toInt().coerceIn(0, 4)
                            onToggle("S${cy * 5 + cx}")
                        } else onToggle("L$i")
                        return@detectTapGestures
                    }
                }
            },
    ) {
        val g = geometry(size.width, size.height)
        // glass slide
        drawRoundRect(Brush.verticalGradient(listOf(glassTop, glassBottom), g.outer.top, g.outer.bottom),
            g.outer.topLeft, g.outer.size, CornerRadius(g.side * 0.07f))
        drawRoundRect(edge, g.outer.topLeft, g.outer.size, CornerRadius(g.side * 0.07f), style = Stroke(g.side * 0.006f))

        for (i in 0..8) {
            val r = g.square(i)
            val isSel = "L$i" in sel1
            val radius = CornerRadius(r.width * 0.05f)
            drawRoundRect(if (isSel) sel.copy(alpha = 0.20f) else squareFill, r.topLeft, r.size, radius)
            val (cols, rows) = when (i) { 0, 2, 6, 8 -> 4 to 4; 1, 7 -> 5 to 4; 3, 5 -> 4 to 5; else -> 5 to 5 }
            if (i == 4) {
                // 25 groups, each ruled 4 × 4
                val s = r.width / 5f
                for (k in 0..24) if ("S$k" in sel1 && !isSel) {
                    drawRect(sel.copy(alpha = 0.32f), Offset(r.left + (k % 5) * s, r.top + (k / 5) * s), Size(s, s))
                }
                for (k in 1 until 20) if (k % 4 != 0) {
                    val d = r.width / 20f * k
                    line(fine.copy(alpha = fine.alpha * 0.7f), r.left + d, r.top, r.left + d, r.bottom, g.side * 0.0016f)
                    line(fine.copy(alpha = fine.alpha * 0.7f), r.left, r.top + d, r.right, r.top + d, g.side * 0.0016f)
                }
                for (k in 1 until 5) {
                    val d = s * k
                    line(rule.copy(alpha = 0.7f), r.left + d, r.top, r.left + d, r.bottom, g.side * 0.004f)
                    line(rule.copy(alpha = 0.7f), r.left, r.top + d, r.right, r.top + d, g.side * 0.004f)
                }
                // "R" in the five red cell squares
                listOf(0, 4, 12, 20, 24).forEach { k ->
                    val t = measurer.measure("R", TextStyle(fontSize = (s * 0.42f / density).sp, color = if ("S$k" in sel1) sel else label, fontWeight = FontWeight.Bold))
                    drawText(t, topLeft = Offset(r.left + (k % 5) * s + (s - t.size.width) / 2, r.top + (k / 5) * s + (s - t.size.height) / 2))
                }
            } else {
                for (k in 1 until cols) {
                    val d = r.width / cols * k
                    line(fine, r.left + d, r.top, r.left + d, r.bottom, g.side * 0.0024f)
                }
                for (k in 1 until rows) {
                    val d = r.height / rows * k
                    line(fine, r.left, r.top + d, r.right, r.top + d, g.side * 0.0024f)
                }
                if (i in listOf(0, 2, 6, 8)) {
                    val t = measurer.measure("W", TextStyle(fontSize = (r.width * 0.2f / density).sp, color = if (isSel) sel else label, fontWeight = FontWeight.Bold))
                    drawText(t, topLeft = Offset(r.center.x - t.size.width / 2, r.center.y - t.size.height / 2))
                }
            }
            drawRoundRect(if (isSel) sel else rule.copy(alpha = 0.75f), r.topLeft, r.size, radius,
                style = Stroke(if (isSel) g.side * 0.011f else g.side * 0.005f))
        }
    }
}

private class ChamberGeometry(val outer: Rect, val side: Float, private val origin: Offset, private val cell: Float, private val gap: Float) {
    fun square(i: Int): Rect {
        val x = origin.x + (i % 3) * (cell + gap)
        val y = origin.y + (i / 3) * (cell + gap)
        return Rect(x, y, x + cell, y + cell)
    }
}

private fun geometry(w: Float, h: Float): ChamberGeometry {
    val side = minOf(w, h)
    val o = Offset((w - side) / 2f, (h - side) / 2f)
    val pad = side * 0.07f
    val grid = side - 2 * pad
    val gap = grid * 0.025f
    val cell = (grid - 2 * gap) / 3f
    return ChamberGeometry(Rect(o, Size(side, side)), side, Offset(o.x + pad, o.y + pad), cell, gap)
}

private fun DrawScope.line(c: Color, x1: Float, y1: Float, x2: Float, y2: Float, w: Float) =
    drawLine(c, Offset(x1, y1), Offset(x2, y2), w)
