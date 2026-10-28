package com.hematcount.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Improved Neubauer ruling: 3 × 3 large squares (1 mm each).
 * Corner squares are ruled 4 × 4; the centre square 5 × 5 (each again 4 × 4).
 * Large squares are "L0".."L8" (row-major); the 25 central squares "S0".."S24".
 */
@Composable
fun NeubauerGrid(
    selected: List<String>,
    smallSquareMode: Boolean,
    onToggle: (String) -> Unit,
    gridColor: Color,
    fineColor: Color,
    selectedColor: Color,
    background: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .semantics { contentDescription = "Neubauer chamber. Tap squares to mark the squares counted." }
            .pointerInput(smallSquareMode) {
                detectTapGestures { pos ->
                    val side = minOf(size.width, size.height).toFloat()
                    val ox = (size.width - side) / 2f
                    val oy = (size.height - side) / 2f
                    val x = pos.x - ox; val y = pos.y - oy
                    if (x < 0 || y < 0 || x > side || y > side) return@detectTapGestures
                    val cell = side / 3f
                    val col = (x / cell).toInt().coerceIn(0, 2)
                    val row = (y / cell).toInt().coerceIn(0, 2)
                    val large = row * 3 + col
                    if (smallSquareMode && large == 4) {
                        val sx = ((x - cell) / (cell / 5f)).toInt().coerceIn(0, 4)
                        val sy = ((y - cell) / (cell / 5f)).toInt().coerceIn(0, 4)
                        onToggle("S${sy * 5 + sx}")
                    } else onToggle("L$large")
                }
            }
    ) {
        val side = size.minDimension
        val o = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        val cell = side / 3f
        drawRect(background, o, Size(side, side))

        // selected areas
        for (i in 0..8) if ("L$i" in selected) {
            val r = i / 3; val c = i % 3
            drawRect(selectedColor.copy(alpha = 0.35f), Offset(o.x + c * cell, o.y + r * cell), Size(cell, cell))
        }
        if ("L4" !in selected) for (i in 0..24) if ("S$i" in selected) {
            val r = i / 5; val c = i % 5; val s = cell / 5f
            drawRect(selectedColor.copy(alpha = 0.45f), Offset(o.x + cell + c * s, o.y + cell + r * s), Size(s, s))
        }

        val fine = Stroke(side * 0.0022f)
        val mid = Stroke(side * 0.004f)
        // corner squares 4 × 4
        for (i in listOf(0, 2, 6, 8)) {
            val r = i / 3; val c = i % 3
            for (k in 1..3) {
                val d = cell / 4f * k
                drawLine(fineColor, Offset(o.x + c * cell + d, o.y + r * cell), Offset(o.x + c * cell + d, o.y + (r + 1) * cell), fine.width)
                drawLine(fineColor, Offset(o.x + c * cell, o.y + r * cell + d), Offset(o.x + (c + 1) * cell, o.y + r * cell + d), fine.width)
            }
        }
        // centre square: 5 × 5 with 4 × 4 fine ruling
        for (k in 1..19) {
            val d = cell / 20f * k
            val w = if (k % 4 == 0) mid.width else fine.width * 0.6f
            val col = if (k % 4 == 0) gridColor.copy(alpha = 0.8f) else fineColor.copy(alpha = 0.6f)
            drawLine(col, Offset(o.x + cell + d, o.y + cell), Offset(o.x + cell + d, o.y + 2 * cell), w)
            drawLine(col, Offset(o.x + cell, o.y + cell + d), Offset(o.x + 2 * cell, o.y + cell + d), w)
        }
        // triple lines between large squares
        for (k in 0..3) {
            val d = cell * k
            drawLine(gridColor, Offset(o.x + d, o.y), Offset(o.x + d, o.y + side), side * 0.007f)
            drawLine(gridColor, Offset(o.x, o.y + d), Offset(o.x + side, o.y + d), side * 0.007f)
        }
        // selection outline
        for (i in 0..8) if ("L$i" in selected) {
            val r = i / 3; val c = i % 3
            drawRect(selectedColor, Offset(o.x + c * cell, o.y + r * cell), Size(cell, cell), style = Stroke(side * 0.009f))
        }
    }
}
