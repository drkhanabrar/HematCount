package com.hematcount.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/* Illustrations for bone marrow, body-fluid, red-cell, platelet, viability and custom keys. */

private val RBC_PINK = Color(0xFFF0A79B)
private val RBC_EDGE = Color(0xFFC9766A)

internal fun DrawScope.drawExtraCell(id: String) {
    val c = center
    val full = size.minDimension / 2f
    when (id) {
        // ---------------------------------------------------------------- erythroid precursors
        "PROERYTHROBLAST" -> {
            val r = full * 0.96f
            cytoplasm(c, r, Color(0xFF3E64B8), Color(0xFF26468F))
            drawCircle(Color(0xFF7FA3E0), r * 0.66f, c)
            nucleusOval(c, r * 0.6f, r * 0.58f, Color(0xFF6B3E8F), Color(0xFF9C76BF))
            chromatin(c, r * 0.52f, 70, Color(0xFF4E2A70), 201, dot = 0.02f)
            drawCircle(Color(0xFFC4B2DA), r * 0.11f, Offset(c.x - r * 0.15f, c.y - r * 0.12f))
            drawCircle(Color(0xFFC4B2DA), r * 0.08f, Offset(c.x + r * 0.2f, c.y + r * 0.15f))
        }
        "BASO_NORMO" -> {
            val r = full * 0.82f
            cytoplasm(c, r, Color(0xFF3558A8), Color(0xFF213F86))
            nucleusOval(c, r * 0.6f, r * 0.58f, Color(0xFF3E1D5E), Color(0xFF6A428F))
            cartwheel(c, r * 0.5f, Color(0xFF26103D), 211)
        }
        "POLY_NORMO" -> {
            val r = full * 0.72f
            cytoplasm(c, r, Color(0xFFB9A7C9), Color(0xFF8F7CA5))
            nucleusOval(c, r * 0.48f, r * 0.47f, Color(0xFF2E1245), Color(0xFF52306F))
            cartwheel(c, r * 0.4f, Color(0xFF180826), 221)
        }
        // ---------------------------------------------------------------- lymphoid / other marrow
        "PLASMA" -> {
            val r = full * 0.92f
            rotate(-20f, c) {
                val tl = Offset(c.x - r, c.y - r * 0.72f)
                val sz = Size(r * 2, r * 1.44f)
                drawOval(Brush.radialGradient(listOf(Color(0xFF4A6FC0), Color(0xFF2B4A94)), c, r), tl, sz)
                drawOval(Color(0xFF1C3570), tl, sz, style = Stroke(r * 0.045f))
                drawCircle(Color(0xFFB9CBEE), r * 0.22f, Offset(c.x - r * 0.02f, c.y - r * 0.02f))
            }
            val nc = Offset(c.x + r * 0.42f, c.y + r * 0.12f)
            drawCircle(Color(0xFF3A1A57), r * 0.4f, nc)
            // clock-face chromatin
            for (k in 0 until 8) {
                val a = k * PI.toFloat() / 4
                drawCircle(Color(0xFF1E0A30), r * 0.07f, Offset(nc.x + cos(a) * r * 0.26f, nc.y + sin(a) * r * 0.26f))
            }
            drawCircle(Color(0xFF1E0A30), r * 0.08f, nc)
        }
        "MACROPHAGE" -> {
            val r = full * 0.98f
            val blob = blobPath(c, r, seed = 301, wobble = 0.09f)
            drawPath(blob, Brush.radialGradient(listOf(Color(0xFFE4E7EF), Color(0xFFB7BED1)), c, r))
            drawPath(blob, Color(0xFF7D869E), style = Stroke(r * 0.04f))
            val rnd = Random(302)
            repeat(9) {
                val a = rnd.nextFloat() * 2 * PI.toFloat(); val d = 0.35f + rnd.nextFloat() * 0.45f
                val p = Offset(c.x + cos(a) * r * d, c.y + sin(a) * r * d)
                drawCircle(Color(0xFFF7F8FB), r * (0.05f + rnd.nextFloat() * 0.06f), p)
            }
            granules(c, r * 0.7f, 10, Color(0xFF8C5A2B), r * 0.03f, r * 0.06f, 303) // haemosiderin / debris
            drawCircle(Color(0xFFE88A7A), r * 0.11f, Offset(c.x + r * 0.35f, c.y - r * 0.3f)) // ingested red cell
            nucleusOval(Offset(c.x - r * 0.3f, c.y + r * 0.1f), r * 0.28f, r * 0.2f, Color(0xFF6A4B93), Color(0xFF9A80BF))
        }
        "MEGAKARYOCYTE" -> {
            val r = full * 0.98f
            val blob = blobPath(c, r, seed = 401, wobble = 0.05f)
            drawPath(blob, Brush.radialGradient(listOf(Color(0xFFEBD3E6), Color(0xFFD2A9CB)), c, r))
            drawPath(blob, Color(0xFFA77A9F), style = Stroke(r * 0.035f))
            granules(c, r * 0.92f, 160, Color(0xFFB27AAA), r * 0.012f, r * 0.022f, 402)
            listOf(Offset(-0.2f, -0.15f), Offset(0.15f, -0.2f), Offset(0.22f, 0.12f), Offset(-0.05f, 0.22f), Offset(-0.25f, 0.1f))
                .forEachIndexed { i, o -> lobe(Offset(c.x + o.x * r, c.y + o.y * r), r * 0.2f, r * 0.17f, i * 33f) }
        }
        "OTHER" -> {
            val r = full * 0.96f
            val blob = blobPath(c, r, seed = 501, wobble = 0.1f)
            drawPath(blob, Brush.radialGradient(listOf(Color(0xFFB7C6E6), Color(0xFF6E8DC7)), c, r))
            drawPath(blob, Color(0xFF3F5E9A), style = Stroke(r * 0.04f))
            val nuc = blobPath(Offset(c.x - r * 0.05f, c.y), r * 0.62f, seed = 502, wobble = 0.16f)
            drawPath(nuc, Brush.radialGradient(listOf(Color(0xFF8F64B0), Color(0xFF5A3384)), c, r * 0.6f))
            drawPath(nuc, NUC_DARK, style = Stroke(r * 0.03f))
            drawCircle(Color(0xFF2B1446), r * 0.13f, Offset(c.x + r * 0.08f, c.y - r * 0.12f))
            drawCircle(Color(0xFFF3F6FB), r * 0.08f, Offset(c.x + r * 0.62f, c.y + r * 0.3f))
        }
        // ---------------------------------------------------------------- fluid cells
        "NUCLEATED" -> {
            val r = full * 0.84f
            cytoplasm(c, r, Color(0xFFE7EAF2), Color(0xFFB9C0D4))
            drawCircle(Color(0xFF8D96B0), r * 0.5f, c)
            drawCircle(Color(0xFF6E7896), r * 0.5f, c, style = Stroke(r * 0.05f))
            drawCircle(Color(0xFFB4BBCE), r * 0.18f, Offset(c.x - r * 0.12f, c.y - r * 0.1f))
        }
        "MESOTHELIAL" -> {
            val r = full * 0.94f
            cytoplasm(c, r, Color(0xFF5E86C6), Color(0xFF3A63A8))
            drawCircle(Color(0xFFBFD3F0), r, c, style = Stroke(r * 0.09f)) // pale fringe
            nucleusOval(c, r * 0.4f, r * 0.38f, Color(0xFF5A3384), Color(0xFF8A68B6))
            drawCircle(Color(0xFFC9B6DD), r * 0.08f, Offset(c.x + r * 0.08f, c.y - r * 0.08f))
        }
        "EPENDYMAL" -> {
            listOf(Offset(-0.34f, -0.2f), Offset(0.34f, -0.2f), Offset(0f, 0.32f)).forEachIndexed { i, o ->
                val p = Offset(c.x + o.x * full, c.y + o.y * full)
                val r = full * 0.42f
                cytoplasm(p, r, Color(0xFFC4D4EC), Color(0xFF8EA7CF))
                nucleusOval(Offset(p.x, p.y + r * 0.1f), r * 0.42f, r * 0.4f, Color(0xFF5E3D8C), Color(0xFF8B6CB4))
            }
        }
        // ---------------------------------------------------------------- red cells
        "RBC" -> redCell(c, full * 0.82f)
        "RETIC" -> {
            val r = full * 0.86f
            drawCircle(Color(0xFFCFE3DD), r, c)
            drawCircle(Color(0xFF7FA79B), r, c, style = Stroke(r * 0.05f))
            val rnd = Random(601)
            repeat(14) {
                val a1 = rnd.nextFloat() * 2 * PI.toFloat(); val d1 = rnd.nextFloat() * 0.7f
                val a2 = a1 + (rnd.nextFloat() - 0.5f) * 1.6f; val d2 = rnd.nextFloat() * 0.7f
                drawLine(Color(0xFF1E3A8A), Offset(c.x + cos(a1) * r * d1, c.y + sin(a1) * r * d1),
                    Offset(c.x + cos(a2) * r * d2, c.y + sin(a2) * r * d2), r * 0.05f, StrokeCap.Round)
            }
            granules(c, r * 0.75f, 14, Color(0xFF1E3A8A), r * 0.04f, r * 0.06f, 602)
        }
        "SCHISTOCYTE" -> {
            val r = full * 0.85f
            val helmet = Path().apply {
                moveTo(c.x - r, c.y + r * 0.35f); lineTo(c.x + r, c.y + r * 0.35f)
                cubicTo(c.x + r * 0.9f, c.y - r * 0.9f, c.x - r * 0.9f, c.y - r * 0.9f, c.x - r, c.y + r * 0.35f); close()
            }
            drawPath(helmet, RBC_PINK); drawPath(helmet, RBC_EDGE, style = Stroke(r * 0.06f))
            val tri = Path().apply {
                moveTo(c.x + r * 0.2f, c.y + r * 0.55f); lineTo(c.x + r * 0.85f, c.y + r * 0.95f); lineTo(c.x - r * 0.1f, c.y + r * 0.98f); close()
            }
            drawPath(tri, RBC_PINK); drawPath(tri, RBC_EDGE, style = Stroke(r * 0.05f))
        }
        "SPHEROCYTE" -> {
            val r = full * 0.62f
            drawCircle(Brush.radialGradient(listOf(Color(0xFFE2655A), Color(0xFFB83F37)), c, r), r, c)
            drawCircle(Color(0xFF8E2A24), r, c, style = Stroke(r * 0.07f))
        }
        "TARGET" -> {
            val r = full * 0.84f
            drawCircle(RBC_PINK, r, c); drawCircle(RBC_EDGE, r, c, style = Stroke(r * 0.05f))
            drawCircle(Color(0xFFFBE3DE), r * 0.62f, c)
            drawCircle(Color(0xFFE38D80), r * 0.26f, c)
        }
        "ELLIPTOCYTE" -> {
            val tl = Offset(c.x - full * 0.9f, c.y - full * 0.45f); val sz = Size(full * 1.8f, full * 0.9f)
            drawOval(RBC_PINK, tl, sz); drawOval(RBC_EDGE, tl, sz, style = Stroke(full * 0.05f))
            drawOval(Color(0xFFFBE3DE), Offset(c.x - full * 0.5f, c.y - full * 0.17f), Size(full, full * 0.34f))
        }
        "TEARDROP" -> {
            val r = full * 0.62f
            val p = Path().apply {
                moveTo(c.x + r * 1.5f, c.y - r * 0.7f)
                cubicTo(c.x + r * 0.6f, c.y - r * 0.6f, c.x + r * 0.5f, c.y - r * 0.9f, c.x - r * 0.2f, c.y - r * 0.9f)
                cubicTo(c.x - r * 1.2f, c.y - r * 0.9f, c.x - r * 1.2f, c.y + r * 0.9f, c.x - r * 0.1f, c.y + r * 0.9f)
                cubicTo(c.x + r * 0.7f, c.y + r * 0.9f, c.x + r * 0.7f, c.y - r * 0.2f, c.x + r * 1.5f, c.y - r * 0.7f)
                close()
            }
            drawPath(p, RBC_PINK); drawPath(p, RBC_EDGE, style = Stroke(r * 0.07f))
        }
        "SICKLE" -> {
            val r = full * 0.9f
            val outer = Path().apply { addOval(Rect(c.x - r, c.y - r * 0.55f, c.x + r, c.y + r * 0.75f)) }
            val bite = Path().apply { addOval(Rect(c.x - r * 0.95f, c.y - r * 0.2f, c.x + r * 0.95f, c.y + r * 1.0f)) }
            val crescent = Path().apply { op(outer, bite, PathOperation.Difference) }
            drawPath(crescent, Color(0xFFE0796C)); drawPath(crescent, RBC_EDGE, style = Stroke(r * 0.05f))
        }
        "ECHINOCYTE" -> spiky(c, full * 0.7f, 22, 0.14f, regular = true)
        "ACANTHOCYTE" -> spiky(c, full * 0.6f, 8, 0.38f, regular = false)
        "STOMATOCYTE" -> {
            val r = full * 0.84f
            drawCircle(RBC_PINK, r, c); drawCircle(RBC_EDGE, r, c, style = Stroke(r * 0.05f))
            drawOval(Color(0xFFFBE3DE), Offset(c.x - r * 0.5f, c.y - r * 0.1f), Size(r, r * 0.2f))
        }
        "BITECELL" -> {
            val r = full * 0.84f
            val disc = Path().apply { addOval(Rect(c.x - r, c.y - r, c.x + r, c.y + r)) }
            val b = Path().apply { addOval(Rect(c.x + r * 0.45f, c.y - r * 0.9f, c.x + r * 1.25f, c.y - r * 0.1f)) }
            val cell = Path().apply { op(disc, b, PathOperation.Difference) }
            drawPath(cell, RBC_PINK); drawPath(cell, RBC_EDGE, style = Stroke(r * 0.05f))
            drawCircle(Color(0xFFFBE3DE), r * 0.3f, Offset(c.x - r * 0.15f, c.y + r * 0.1f))
        }
        "RBC_OTHER" -> {
            redCell(c, full * 0.82f)
            drawCircle(Color(0xFF3F2169), full * 0.12f, Offset(c.x + full * 0.3f, c.y + full * 0.25f)) // Howell-Jolly body
            granules(c, full * 0.5f, 10, Color(0xFF3B5BA8), full * 0.02f, full * 0.035f, 701) // stippling
        }
        // ---------------------------------------------------------------- platelets
        "PLATELET" -> plateletDot(c, full * 0.62f, 801)
        "GIANT_PLATELET" -> plateletDot(c, full * 0.92f, 802)
        "PLATELET_CLUMP" -> {
            listOf(Offset(-0.35f, -0.25f), Offset(0.3f, -0.3f), Offset(0.05f, 0.05f), Offset(-0.3f, 0.35f), Offset(0.35f, 0.32f))
                .forEachIndexed { i, o -> plateletDot(Offset(c.x + o.x * full, c.y + o.y * full), full * 0.34f, 810 + i) }
        }
        "FIELD" -> {
            val r = full * 0.9f
            drawCircle(Color(0xFFE6F4F1), r, c)
            drawCircle(Color(0xFF0D9488), r, c, style = Stroke(r * 0.1f))
            drawLine(Color(0xFF0D9488), Offset(c.x - r * 0.45f, c.y), Offset(c.x + r * 0.45f, c.y), r * 0.09f, StrokeCap.Round)
            drawLine(Color(0xFF0D9488), Offset(c.x, c.y - r * 0.45f), Offset(c.x, c.y + r * 0.45f), r * 0.09f, StrokeCap.Round)
        }
        // ---------------------------------------------------------------- viability
        "VIABLE" -> {
            val r = full * 0.78f
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE3EBE6)), c, r), r, c)
            drawCircle(Color(0xFF8BA898), r, c, style = Stroke(r * 0.12f))
            drawCircle(Color(0xFFFFFFFF), r * 0.25f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
        }
        "NONVIABLE" -> {
            val r = full * 0.78f
            drawCircle(Brush.radialGradient(listOf(Color(0xFF4F7FD6), Color(0xFF1D3F94)), c, r), r, c)
            drawCircle(Color(0xFF122B66), r, c, style = Stroke(r * 0.07f))
        }
        // ---------------------------------------------------------------- custom extras
        "SPERM" -> {
            val r = full
            drawOval(Color(0xFF6B7FB8), Offset(c.x - r * 0.75f, c.y - r * 0.28f), Size(r * 0.62f, r * 0.42f))
            val tail = Path().apply {
                moveTo(c.x - r * 0.14f, c.y - r * 0.07f)
                cubicTo(c.x + r * 0.2f, c.y - r * 0.5f, c.x + r * 0.45f, c.y + r * 0.4f, c.x + r * 0.85f, c.y - r * 0.05f)
            }
            drawPath(tail, Color(0xFF6B7FB8), style = Stroke(r * 0.07f, cap = StrokeCap.Round))
        }
        "YEAST" -> {
            val r = full * 0.5f
            drawOval(Color(0xFFB7A6D6), Offset(c.x - r * 1.3f, c.y - r * 0.8f), Size(r * 1.6f, r * 1.2f))
            drawOval(Color(0xFFB7A6D6), Offset(c.x + r * 0.1f, c.y - r * 0.2f), Size(r * 1.1f, r * 0.85f))
            drawOval(Color(0xFF7D6AA6), Offset(c.x - r * 1.3f, c.y - r * 0.8f), Size(r * 1.6f, r * 1.2f), style = Stroke(r * 0.1f))
            drawOval(Color(0xFF7D6AA6), Offset(c.x + r * 0.1f, c.y - r * 0.2f), Size(r * 1.1f, r * 0.85f), style = Stroke(r * 0.1f))
        }
        "CRYSTAL" -> {
            val r = full * 0.9f
            rotate(30f, c) {
                drawRect(Color(0xFFFDE68A), Offset(c.x - r * 0.2f, c.y - r * 0.85f), Size(r * 0.4f, r * 1.7f))
                drawRect(Color(0xFFCA8A04), Offset(c.x - r * 0.2f, c.y - r * 0.85f), Size(r * 0.4f, r * 1.7f), style = Stroke(r * 0.06f))
            }
        }
        "CAST" -> {
            val tl = Offset(c.x - full * 0.9f, c.y - full * 0.32f); val sz = Size(full * 1.8f, full * 0.64f)
            drawRoundRect(Color(0xFFE5E7EB), tl, sz, androidx.compose.ui.geometry.CornerRadius(full * 0.3f))
            drawRoundRect(Color(0xFF9CA3AF), tl, sz, androidx.compose.ui.geometry.CornerRadius(full * 0.3f), style = Stroke(full * 0.06f))
            granules(c, full * 0.28f, 12, Color(0xFF6B7280), full * 0.03f, full * 0.05f, 901)
        }
        "BACTERIA" -> {
            val rnd = Random(911)
            repeat(9) {
                val p = Offset(c.x + (rnd.nextFloat() - 0.5f) * full * 1.4f, c.y + (rnd.nextFloat() - 0.5f) * full * 1.4f)
                rotate(rnd.nextFloat() * 180f, p) {
                    drawRoundRect(Color(0xFF7C3AED), Offset(p.x - full * 0.16f, p.y - full * 0.06f), Size(full * 0.32f, full * 0.12f),
                        androidx.compose.ui.geometry.CornerRadius(full * 0.06f))
                }
            }
        }
        else -> { // fallback: generic cell
            val r = full * 0.85f
            cytoplasm(c, r, Color(0xFFE5E7EB), Color(0xFFB8BDC8))
            nucleusOval(c, r * 0.45f, r * 0.42f, NUC, NUC_LIGHT)
        }
    }
}

private fun DrawScope.redCell(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(0f to Color(0xFFFBE3DE), 0.45f to Color(0xFFF7C7BE), 1f to RBC_PINK, center = c, radius = r), r, c)
    drawCircle(RBC_EDGE, r, c, style = Stroke(r * 0.05f))
}

private fun DrawScope.spiky(c: Offset, r: Float, n: Int, amp: Float, regular: Boolean) {
    val rnd = Random(n * 31)
    val path = Path()
    val steps = n * 2
    for (i in 0..steps) {
        val t = i.toFloat() / steps * 2 * PI.toFloat()
        val out = if (i % 2 == 0) 1f + amp * (if (regular) 1f else 0.5f + rnd.nextFloat()) else 1f
        val p = Offset(c.x + cos(t) * r * out, c.y + sin(t) * r * out)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, if (regular) RBC_PINK else Color(0xFFE47A6E))
    drawPath(path, RBC_EDGE, style = Stroke(r * 0.05f))
}

private fun DrawScope.plateletDot(c: Offset, r: Float, seed: Int) {
    val blob = blobPath(c, r, seed = seed, wobble = 0.12f)
    drawPath(blob, Color(0xFFC9C3E8))
    drawPath(blob, Color(0xFF8E84C2), style = Stroke(r * 0.07f))
    granules(c, r * 0.5f, 10, Color(0xFF5B3590), r * 0.07f, r * 0.12f, seed)
}

private fun DrawScope.cartwheel(c: Offset, r: Float, color: Color, seed: Int) {
    val rnd = Random(seed)
    for (k in 0 until 7) {
        val a = k * 2 * PI.toFloat() / 7 + rnd.nextFloat() * 0.3f
        drawCircle(color, r * 0.16f, Offset(c.x + cos(a) * r * 0.58f, c.y + sin(a) * r * 0.58f))
    }
    drawCircle(color, r * 0.18f, c)
}
