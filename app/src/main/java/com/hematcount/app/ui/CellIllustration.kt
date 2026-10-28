package com.hematcount.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.hematcount.app.model.CellType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * Romanowsky-stain style vector illustrations of each cell.
 * Colours imitate a Leishman-stained smear and are intentionally the same in
 * light and dark themes (they are "pictures", like a micrograph). Every cell
 * has a darker membrane outline so it never blends into a dark background.
 */

internal val NUC_DARK = Color(0xFF3F2169)
internal val NUC = Color(0xFF5B3590)
internal val NUC_LIGHT = Color(0xFF8460B4)

@Composable
fun CellIllustration(cell: com.hematcount.app.model.CountItem, modifier: Modifier = Modifier) {
    val id = cell.drawId
    Canvas(modifier) { drawCellId(id) }
}

fun DrawScope.drawCellId(id: String) {
    val blood = CellType.entries.firstOrNull { it.name == id }
    if (blood != null) drawCell(blood) else drawExtraCell(id)
}

fun DrawScope.drawCell(cell: CellType) {
    val c = center
    val full = size.minDimension / 2f
    when (cell) {
        CellType.NEUTROPHIL -> {
            val r = full * 0.92f
            cytoplasm(c, r, Color(0xFFF7E6EE), Color(0xFFE6C3D4))
            granules(c, r * 0.9f, 90, Color(0xFFC48AAE), r * 0.018f, r * 0.03f, 11)
            val lobes = listOf(
                Offset(c.x - r * 0.38f, c.y - r * 0.18f),
                Offset(c.x + r * 0.02f, c.y + r * 0.28f),
                Offset(c.x + r * 0.40f, c.y - r * 0.20f),
            )
            chromatinBridge(lobes[0], lobes[1], r * 0.07f)
            chromatinBridge(lobes[1], lobes[2], r * 0.07f)
            lobes.forEachIndexed { i, p -> lobe(p, r * 0.27f, r * 0.22f, listOf(-30f, 10f, 35f)[i]) }
        }
        CellType.BAND -> {
            val r = full * 0.88f
            cytoplasm(c, r, Color(0xFFF7E6EE), Color(0xFFE6C3D4))
            granules(c, r * 0.9f, 80, Color(0xFFC48AAE), r * 0.018f, r * 0.03f, 12)
            val ar = r * 0.52f
            val topLeft = Offset(c.x - ar, c.y - ar * 0.95f)
            val sz = Size(ar * 2, ar * 1.9f)
            drawArc(NUC_DARK, 150f, 240f, false, topLeft, sz, style = Stroke(r * 0.32f, cap = StrokeCap.Round))
            drawArc(NUC, 150f, 240f, false, topLeft, sz, style = Stroke(r * 0.25f, cap = StrokeCap.Round))
            drawArc(NUC_LIGHT.copy(alpha = 0.5f), 165f, 210f, false,
                Offset(topLeft.x + r * 0.03f, topLeft.y + r * 0.02f), sz,
                style = Stroke(r * 0.07f, cap = StrokeCap.Round))
        }
        CellType.EOSINOPHIL -> {
            val r = full * 0.94f
            cytoplasm(c, r, Color(0xFFFBE3D6), Color(0xFFEFBFA8))
            val l1 = Offset(c.x - r * 0.30f, c.y - r * 0.05f)
            val l2 = Offset(c.x + r * 0.30f, c.y - r * 0.05f)
            chromatinBridge(l1, l2, r * 0.08f)
            lobe(l1, r * 0.27f, r * 0.24f, -15f)
            lobe(l2, r * 0.27f, r * 0.24f, 15f)
            granules(c, r * 0.9f, 55, Color(0xFFE2522F), r * 0.055f, r * 0.075f, 21,
                highlight = Color(0xFFFFA587))
        }
        CellType.BASOPHIL -> {
            val r = full * 0.86f
            cytoplasm(c, r, Color(0xFFEEDDF0), Color(0xFFD7BCDD))
            lobe(Offset(c.x - r * 0.12f, c.y), r * 0.38f, r * 0.32f, 20f, alpha = 0.75f)
            granules(c, r * 0.88f, 40, Color(0xFF2B0F3F), r * 0.06f, r * 0.10f, 31,
                highlight = Color(0xFF5C3577))
        }
        CellType.LYMPHOCYTE -> {
            val r = full * 0.70f
            cytoplasm(c, r, Color(0xFFB9DAF4), Color(0xFF7FB2E0))
            nucleusOval(Offset(c.x - r * 0.05f, c.y), r * 0.80f, r * 0.76f, NUC_DARK, Color(0xFF52307F))
            chromatin(Offset(c.x - r * 0.05f, c.y), r * 0.7f, 45, Color(0xFF2C1450), 41)
        }
        CellType.MONOCYTE -> {
            val r = full * 0.98f
            cytoplasm(c, r, Color(0xFFDCDFEC), Color(0xFFB4BAD2))
            granules(c, r * 0.9f, 70, Color(0xFFB596C2), r * 0.012f, r * 0.02f, 51)
            // vacuoles
            listOf(Offset(c.x + r * 0.55f, c.y + r * 0.35f), Offset(c.x + r * 0.35f, c.y + r * 0.62f),
                Offset(c.x - r * 0.55f, c.y + r * 0.45f)).forEach {
                drawCircle(Color(0xFFF4F6FA), r * 0.07f, it)
                drawCircle(Color(0xFF9AA2BF), r * 0.07f, it, style = Stroke(r * 0.012f))
            }
            val nucleus = kidneyPath(Offset(c.x, c.y - r * 0.1f), r * 0.6f, r * 0.42f,
                Offset(c.x + r * 0.05f, c.y + r * 0.38f), r * 0.32f)
            drawPath(nucleus, Brush.radialGradient(listOf(Color(0xFF9A7CC2), Color(0xFF6C4A9C)),
                Offset(c.x, c.y - r * 0.2f), r * 0.7f))
            drawPath(nucleus, Color(0xFF55357F), style = Stroke(r * 0.03f))
            // lacy chromatin folds
            drawArc(Color(0xFF55357F).copy(alpha = 0.6f), 200f, 120f, false,
                Offset(c.x - r * 0.35f, c.y - r * 0.45f), Size(r * 0.7f, r * 0.5f), style = Stroke(r * 0.025f))
            chromatin(Offset(c.x, c.y - r * 0.15f), r * 0.45f, 30, Color(0xFF5A3A86), 52)
        }
        CellType.METAMYELOCYTE -> {
            val r = full * 0.90f
            cytoplasm(c, r, Color(0xFFF6E2E7), Color(0xFFE3BFCB))
            granules(c, r * 0.9f, 85, Color(0xFFC48AAE), r * 0.02f, r * 0.032f, 61)
            val nucleus = kidneyPath(Offset(c.x - r * 0.05f, c.y), r * 0.5f, r * 0.42f,
                Offset(c.x + r * 0.45f, c.y), r * 0.2f)
            drawPath(nucleus, Brush.radialGradient(listOf(NUC_LIGHT, NUC), c, r * 0.6f))
            drawPath(nucleus, NUC_DARK, style = Stroke(r * 0.03f))
            chromatin(Offset(c.x - r * 0.1f, c.y), r * 0.35f, 30, NUC_DARK, 62)
        }
        CellType.MYELOCYTE -> {
            val r = full * 0.94f
            cytoplasm(c, r, Color(0xFFEBDCE8), Color(0xFFCDB3CB))
            // Golgi zone
            drawCircle(Color(0xFFF8F1F6), r * 0.24f, Offset(c.x + r * 0.18f, c.y - r * 0.1f))
            granules(c, r * 0.9f, 90, Color(0xFFBE7FA6), r * 0.02f, r * 0.034f, 71)
            val nc = Offset(c.x - r * 0.25f, c.y + r * 0.05f)
            nucleusOval(nc, r * 0.52f, r * 0.48f, NUC, NUC_LIGHT)
            chromatin(nc, r * 0.42f, 38, NUC_DARK, 72)
        }
        CellType.PROMYELOCYTE -> {
            val r = full * 0.98f
            cytoplasm(c, r, Color(0xFFC8D6EF), Color(0xFF8DA7D6))
            drawCircle(Color(0xFFE6EDF8), r * 0.2f, Offset(c.x + r * 0.22f, c.y - r * 0.18f))
            val nc = Offset(c.x - r * 0.2f, c.y + r * 0.06f)
            nucleusOval(nc, r * 0.55f, r * 0.5f, Color(0xFF7A4A9E), Color(0xFFA27CC6))
            chromatin(nc, r * 0.45f, 28, Color(0xFF5E3A84), 81)
            drawCircle(Color(0xFFCDB8E2), r * 0.09f, Offset(nc.x + r * 0.05f, nc.y - r * 0.1f))
            granules(c, r * 0.92f, 60, Color(0xFF9B1D5C), r * 0.035f, r * 0.055f, 82,
                highlight = Color(0xFFD4558F))
        }
        CellType.BLAST -> {
            val r = full * 0.94f
            cytoplasm(c, r, Color(0xFF8DB2E4), Color(0xFF4F7FC4))
            val nc = Offset(c.x - r * 0.03f, c.y)
            nucleusOval(nc, r * 0.80f, r * 0.76f, Color(0xFF8A5AA8), Color(0xFFB08ACB))
            chromatin(nc, r * 0.72f, 110, Color(0xFF74468F).copy(alpha = 0.55f), 91, dot = 0.018f)
            listOf(Offset(nc.x - r * 0.25f, nc.y - r * 0.2f), Offset(nc.x + r * 0.28f, nc.y + r * 0.05f),
                Offset(nc.x - r * 0.05f, nc.y + r * 0.35f)).forEachIndexed { i, p ->
                val nr = r * listOf(0.13f, 0.11f, 0.09f)[i]
                drawCircle(Color(0xFFD9CBEA), nr, p)
                drawCircle(Color(0xFF6E4891), nr, p, style = Stroke(r * 0.02f))
            }
            // Auer rod
            drawLine(Color(0xFFC2185B), Offset(c.x + r * 0.55f, c.y - r * 0.55f),
                Offset(c.x + r * 0.78f, c.y - r * 0.25f), r * 0.035f, StrokeCap.Round)
        }
        CellType.ATYPICAL_LYMPHOCYTE -> {
            val r = full * 0.98f
            val blob = blobPath(c, r, seed = 101, dents = listOf(0.6f, 3.6f))
            drawPath(blob, Brush.radialGradient(
                0.0f to Color(0xFFE3EEF9), 0.6f to Color(0xFFB4D0EE), 1.0f to Color(0xFF3F78BE),
                center = c, radius = r))
            drawPath(blob, Color(0xFF2E5F9E), style = Stroke(r * 0.04f))
            val nc = Offset(c.x - r * 0.12f, c.y - r * 0.05f)
            val nuc = blobPath(nc, r * 0.48f, seed = 103, wobble = 0.12f)
            drawPath(nuc, Brush.radialGradient(listOf(Color(0xFF8A68B6), Color(0xFF5C3A8A)), nc, r * 0.5f))
            drawPath(nuc, NUC_DARK, style = Stroke(r * 0.03f))
            chromatin(nc, r * 0.38f, 30, NUC_DARK.copy(alpha = 0.7f), 104)
        }
        CellType.NRBC -> {
            val r = full * 0.66f
            cytoplasm(c, r, Color(0xFFF2B9AE), Color(0xFFD9867A))
            val nc = Offset(c.x - r * 0.15f, c.y - r * 0.05f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFF4A2566), Color(0xFF1E0B2C)), nc, r * 0.45f),
                r * 0.42f, nc)
            drawCircle(Color(0xFF14071F), r * 0.42f, nc, style = Stroke(r * 0.04f))
        }
    }
}

// ------------------------------------------------------------------ helpers

internal fun DrawScope.cytoplasm(c: Offset, r: Float, inner: Color, outer: Color) {
    drawCircle(Brush.radialGradient(0f to inner, 0.75f to inner, 1f to outer, center = c, radius = r), r, c)
    drawCircle(outer.darken(0.35f), r, c, style = Stroke(r * 0.045f))
}

internal fun DrawScope.lobe(p: Offset, rx: Float, ry: Float, deg: Float, alpha: Float = 1f) {
    rotate(deg, p) {
        val rect = Rect(p.x - rx, p.y - ry, p.x + rx, p.y + ry)
        drawOval(Brush.radialGradient(listOf(NUC_LIGHT, NUC, NUC_DARK),
            Offset(p.x - rx * 0.3f, p.y - ry * 0.3f), rx * 1.2f), rect.topLeft, rect.size, alpha)
        drawOval(NUC_DARK, rect.topLeft, rect.size, alpha, style = Stroke(rx * 0.12f))
    }
    chromatin(p, rx * 0.7f, 10, NUC_DARK, (p.x * 7 + p.y).toInt())
}

internal fun DrawScope.chromatinBridge(a: Offset, b: Offset, w: Float) {
    drawLine(NUC_DARK, a, b, w, StrokeCap.Round)
}

internal fun DrawScope.nucleusOval(p: Offset, rx: Float, ry: Float, base: Color, light: Color) {
    val tl = Offset(p.x - rx, p.y - ry)
    val sz = Size(rx * 2, ry * 2)
    drawOval(Brush.radialGradient(listOf(light, base), Offset(p.x - rx * 0.3f, p.y - ry * 0.3f), rx * 1.3f), tl, sz)
    drawOval(base.darken(0.3f), tl, sz, style = Stroke(rx * 0.05f))
}

internal fun DrawScope.granules(
    c: Offset, maxR: Float, n: Int, color: Color, minSize: Float, maxSize: Float, seed: Int,
    highlight: Color? = null,
) {
    val rnd = Random(seed)
    repeat(n) {
        val a = rnd.nextFloat() * 2 * PI.toFloat()
        val d = sqrt(rnd.nextFloat()) * maxR
        val p = Offset(c.x + cos(a) * d, c.y + sin(a) * d)
        val s = minSize + rnd.nextFloat() * (maxSize - minSize)
        drawCircle(color, s, p)
        if (highlight != null) drawCircle(highlight, s * 0.4f, Offset(p.x - s * 0.3f, p.y - s * 0.3f))
    }
}

internal fun DrawScope.chromatin(c: Offset, maxR: Float, n: Int, color: Color, seed: Int, dot: Float = 0.035f) {
    val rnd = Random(seed)
    repeat(n) {
        val a = rnd.nextFloat() * 2 * PI.toFloat()
        val d = sqrt(rnd.nextFloat()) * maxR
        drawCircle(color.copy(alpha = color.alpha * 0.55f), maxR * dot * (0.6f + rnd.nextFloat()),
            Offset(c.x + cos(a) * d, c.y + sin(a) * d))
    }
}

/** Oval with a circular bite taken out → kidney / bean-shaped nucleus. */
internal fun kidneyPath(c: Offset, rx: Float, ry: Float, biteCenter: Offset, biteR: Float): Path {
    val oval = Path().apply { addOval(Rect(c.x - rx, c.y - ry, c.x + rx, c.y + ry)) }
    val bite = Path().apply { addOval(Rect(biteCenter.x - biteR, biteCenter.y - biteR, biteCenter.x + biteR, biteCenter.y + biteR)) }
    return Path().apply { op(oval, bite, PathOperation.Difference) }
}

/** Irregular rounded outline; [dents] are angles (radians) where neighbouring red cells indent it. */
internal fun blobPath(c: Offset, r: Float, seed: Int, wobble: Float = 0.05f, dents: List<Float> = emptyList()): Path {
    val rnd = Random(seed)
    val phase1 = rnd.nextFloat() * 6f
    val phase2 = rnd.nextFloat() * 6f
    val path = Path()
    val steps = 72
    for (i in 0..steps) {
        val t = i.toFloat() / steps * 2 * PI.toFloat()
        var rr = 1f + wobble * sin(3 * t + phase1) + wobble * 0.6f * sin(5 * t + phase2)
        dents.forEach { d ->
            var diff = t - d
            while (diff > PI) diff -= 2 * PI.toFloat()
            while (diff < -PI) diff += 2 * PI.toFloat()
            rr -= 0.16f * exp(-(diff * diff) / 0.06f)
        }
        val p = Offset(c.x + cos(t) * r * rr * 0.97f, c.y + sin(t) * r * rr * 0.97f)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    return path
}

internal fun Color.darken(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
