package com.hematcount.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.hematcount.app.model.Calculations.fmt1
import com.hematcount.app.model.Calculations.fmt2
import com.hematcount.app.model.Flag
import com.hematcount.app.model.MorphologyParam
import com.hematcount.app.model.PlateletEstimate
import com.hematcount.app.model.REFERENCE_SOURCES
import com.hematcount.app.model.gradeLabel
import java.io.File
import java.io.FileOutputStream

/**
 * A4 hospital-grade haematology report drawn with Android's PdfDocument.
 * Units: PostScript points (1/72 inch). A4 = 595 × 842 pt.
 */
object PdfReport {

    private const val W = 595f
    private const val H = 842f
    private const val M = 36f // side margin
    private const val CONTENT_BOTTOM = 784f

    private val NAVY = Color.rgb(11, 57, 84)
    private val TEAL = Color.rgb(14, 116, 144)
    private val MINT = Color.rgb(34, 195, 166)
    private val INK = Color.rgb(23, 32, 42)
    private val MUTED = Color.rgb(96, 108, 120)
    private val LINE = Color.rgb(213, 221, 229)
    private val ZEBRA = Color.rgb(244, 247, 250)
    private val PANEL = Color.rgb(247, 249, 251)
    private val RED = Color.rgb(198, 40, 40)
    private val BLUE = Color.rgb(21, 101, 192)
    private val AMBER = Color.rgb(166, 98, 0)

    private val REGULAR: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val BOLD: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val MEDIUM: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    private fun text(size: Float, color: Int = INK, face: Typeface = REGULAR, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; this.color = color; typeface = face; textAlign = align
        }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    private fun stroke(color: Int, w: Float = 0.8f) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = w }

    private class PageState(val doc: PdfDocument, val data: ReportData) {
        var pageNo = 0
        lateinit var page: PdfDocument.Page
        val canvas: Canvas get() = page.canvas
        var y = 0f
    }

    fun generate(context: Context, data: ReportData): File {
        val doc = PdfDocument()
        val s = PageState(doc, data)
        newPage(s, first = true)

        title(s)
        patientPanel(s)
        differentialTable(s)
        indicesAndChart(s)
        alerts(s)
        morphology(s)
        remarks(s)
        aiSection(s)
        signatures(s)

        finishPage(s)
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { if (System.currentTimeMillis() - it.lastModified() > 86_400_000L) it.delete() }
        val file = File(dir, data.fileName)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    // ------------------------------------------------------------------ pages
    private fun newPage(s: PageState, first: Boolean) {
        s.pageNo++
        s.page = s.doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), s.pageNo).create())
        if (first) header(s) else continuationHeader(s)
    }

    private fun finishPage(s: PageState) {
        footer(s)
        s.doc.finishPage(s.page)
    }

    private fun ensure(s: PageState, needed: Float) {
        if (s.y + needed > CONTENT_BOTTOM) {
            finishPage(s)
            newPage(s, first = false)
        }
    }

    // ------------------------------------------------------------------ header / footer
    private fun header(s: PageState) {
        val c = s.canvas
        val d = s.data
        val bandH = 84f
        val band = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, W, bandH, NAVY, TEAL, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, W, bandH, band)
        c.drawRect(0f, bandH, W, bandH + 3f, fill(MINT))

        // Emblem: stylised neutrophil in a ring
        val ex = M + 22f; val ey = bandH / 2f
        c.drawCircle(ex, ey, 22f, fill(Color.argb(40, 255, 255, 255)))
        c.drawCircle(ex, ey, 15f, fill(Color.WHITE))
        val lobe = fill(Color.rgb(91, 46, 145))
        c.drawCircle(ex - 5.5f, ey - 3.5f, 4.4f, lobe)
        c.drawCircle(ex, ey + 4f, 4.4f, lobe)
        c.drawCircle(ex + 5.5f, ey - 3.5f, 4.4f, lobe)

        val left = M + 54f
        val rightBlockW = 150f
        val maxNameW = W - M - rightBlockW - left - 10f
        val inst = d.institution.ifBlank { "Haematology Laboratory" }
        val instPaint = text(15f, Color.WHITE, BOLD)
        while (instPaint.measureText(inst) > maxNameW && instPaint.textSize > 10f) instPaint.textSize -= 0.5f
        val instLines = if (instPaint.measureText(inst) > maxNameW) wrap(inst, instPaint, maxNameW).take(2) else listOf(inst)
        var ty = if (instLines.size == 2) 27f else 34f
        instLines.forEach { c.drawText(it, left, ty, instPaint); ty += instPaint.textSize + 2f }
        if (d.department.isNotBlank()) {
            c.drawText(ellipsize(d.department, text(9.5f), maxNameW), left, ty + 3f,
                text(9.5f, Color.argb(235, 255, 255, 255), MEDIUM)); ty += 13f
        }
        if (d.address.isNotBlank()) {
            c.drawText(ellipsize(d.address, text(8.5f), maxNameW), left, ty + 3f,
                text(8.5f, Color.argb(200, 255, 255, 255)))
        }

        val rx = W - M
        val tag = text(10.5f, Color.WHITE, BOLD, Paint.Align.RIGHT).apply { letterSpacing = 0.12f }
        c.drawText("HAEMATOLOGY REPORT", rx, 29f, tag)
        c.drawText("Report No.  ${d.reportNo}", rx, 45f, text(8.5f, Color.argb(230, 255, 255, 255), MEDIUM, Paint.Align.RIGHT))
        c.drawText(d.dateText, rx, 57f, text(8.5f, Color.argb(210, 255, 255, 255), REGULAR, Paint.Align.RIGHT))
        c.drawText("Peripheral blood smear", rx, 69f, text(8.5f, Color.argb(210, 255, 255, 255), REGULAR, Paint.Align.RIGHT))
        s.y = bandH + 24f
    }

    private fun continuationHeader(s: PageState) {
        val c = s.canvas
        c.drawRect(0f, 0f, W, 40f, fill(NAVY))
        c.drawRect(0f, 40f, W, 42f, fill(MINT))
        c.drawText(ellipsize(s.data.institution.ifBlank { "Haematology Laboratory" }, text(10f, face = BOLD), 330f),
            M, 25f, text(10f, Color.WHITE, BOLD))
        val who = listOf(s.data.patientName, s.data.sampleId).filter { it.isNotBlank() }.joinToString("  ·  ")
        c.drawText("$who  (continued)", W - M, 25f, text(8.5f, Color.WHITE, MEDIUM, Paint.Align.RIGHT))
        s.y = 64f
    }

    private fun footer(s: PageState) {
        val c = s.canvas
        val top = 790f
        c.drawLine(M, top, W - M, top, stroke(LINE))
        val p = text(7f, MUTED)
        val lines = wrap(
            "Reference intervals (adults): $REFERENCE_SOURCES. H = above and L = below the reference interval; " +
                "\"Present\" = cell type not normally seen in peripheral blood. Absolute counts in ×10³/µL (equivalent to ×10⁹/L).",
            p, W - 2 * M,
        )
        var y = top + 11f
        lines.take(3).forEach { c.drawText(it, M, y, p); y += 8.6f }
        val f2 = text(6.8f, MUTED, MEDIUM)
        c.drawText("Generated with HematCount · Results must be interpreted in the clinical context.", M, 834f, f2)
        c.drawText("Page ${s.pageNo}", W - M, 834f, text(6.8f, MUTED, MEDIUM, Paint.Align.RIGHT))
    }

    // ------------------------------------------------------------------ sections
    private fun sectionTitle(s: PageState, title: String, right: String? = null) {
        ensure(s, 26f)
        val c = s.canvas
        c.drawRect(M, s.y - 8.5f, M + 3f, s.y + 1.5f, fill(MINT))
        val p = text(8.8f, NAVY, BOLD).apply { letterSpacing = 0.08f }
        c.drawText(title.uppercase(), M + 8f, s.y, p)
        if (right != null) c.drawText(right, W - M, s.y, text(8.5f, MUTED, MEDIUM, Paint.Align.RIGHT))
        s.y += 8f
    }

    private fun title(s: PageState) {
        val c = s.canvas
        c.drawText("Manual Differential Leucocyte Count", M, s.y, text(14f, NAVY, BOLD))
        val r = s.data.results
        val counted = "Leucocytes counted: ${r.wbcCounted}" +
            (if (s.data.target != 0) "  /  Target ${s.data.target}" else "  ·  Continuous count")
        c.drawText(counted, W - M, s.y, text(9f, MUTED, MEDIUM, Paint.Align.RIGHT))
        s.y += 14f
    }

    private fun patientPanel(s: PageState) {
        val c = s.canvas
        val d = s.data
        val top = s.y
        val h = 58f
        val rect = RectF(M, top, W - M, top + h)
        c.drawRoundRect(rect, 6f, 6f, fill(PANEL))
        c.drawRoundRect(rect, 6f, 6f, stroke(LINE))
        val colW = (W - 2 * M) / 3f
        val ageSex = listOf(d.age.let { if (it.isNotBlank() && it.all { ch -> ch.isDigit() }) "$it years" else it }, d.sex)
            .filter { it.isNotBlank() }.joinToString(" / ")
        val items = listOf(
            "Patient name" to d.patientName, "Age / Sex" to ageSex, "Sample ID" to d.sampleId,
            "Referred by" to d.referredBy, "Ward / OPD" to d.ward, "Specimen" to "EDTA peripheral blood smear",
        )
        items.forEachIndexed { i, (label, value) ->
            val col = i % 3; val row = i / 3
            val x = M + 12f + col * colW
            val y = top + 16f + row * 25f
            c.drawText(label.uppercase(), x, y, text(6.6f, MUTED, MEDIUM).apply { letterSpacing = 0.06f })
            c.drawText(ellipsize(value.ifBlank { "—" }, text(9.5f, face = BOLD), colW - 18f), x, y + 11.5f, text(9.5f, INK, BOLD))
        }
        for (i in 1..2) {
            val x = M + i * colW
            c.drawLine(x, top + 9f, x, top + h - 9f, stroke(LINE, 0.6f))
        }
        s.y = top + h + 18f
    }

    private fun differentialTable(s: PageState) {
        val r = s.data.results
        sectionTitle(s, "Differential leucocyte count")
        val c = s.canvas
        // column boundaries
        val x0 = M; val xCount = 190f; val xPct = 236f; val xAbs = 292f; val xRefPct = 362f; val xRefAbs = 432f; val xFlag = 504f
        val x7 = W - M
        val rowH = 16f

        // header row
        val hTop = s.y
        val hH = 22f
        c.drawRoundRect(RectF(x0, hTop, x7, hTop + hH), 4f, 4f, fill(NAVY))
        val hp = text(7f, Color.WHITE, BOLD)
        val hpc = text(7f, Color.WHITE, BOLD, Paint.Align.CENTER)
        c.drawText("CELL TYPE", x0 + 10f, hTop + 15f, hp)
        fun centered(t1: String, t2: String?, a: Float, b: Float) {
            val cx = (a + b) / 2
            if (t2 == null) c.drawText(t1, cx, hTop + 15f, hpc)
            else { c.drawText(t1, cx, hTop + 10.5f, hpc); c.drawText(t2, cx, hTop + 19f, text(6.3f, Color.argb(210, 255, 255, 255), MEDIUM, Paint.Align.CENTER)) }
        }
        centered("COUNT", null, xCount, xPct)
        centered("PERCENT", "%", xPct, xAbs)
        centered("ABSOLUTE", "×10³/µL", xAbs, xRefPct)
        centered("NORMAL RANGE", "%", xRefPct, xRefAbs)
        centered("NORMAL RANGE", "×10³/µL", xRefAbs, xFlag)
        centered("FLAG", null, xFlag, x7)
        s.y = hTop + hH

        r.rows.forEachIndexed { i, row ->
            val top = s.y
            if (i % 2 == 1) c.drawRect(x0, top, x7, top + rowH, fill(ZEBRA))
            val base = top + 11.2f
            val flagged = row.flag != Flag.NONE
            c.drawRoundRect(RectF(x0 + 9f, top + 4.8f, x0 + 15.5f, top + 11.3f), 1.5f, 1.5f, fill(row.cell.accent.toArgb()))
            c.drawText(row.cell.displayName, x0 + 22f, base, text(8.6f, INK, if (flagged) BOLD else MEDIUM))
            val valPaint = text(8.6f, INK, if (flagged) BOLD else REGULAR, Paint.Align.CENTER)
            c.drawText(row.count.toString(), (xCount + xPct) / 2, base, valPaint)
            c.drawText(fmt1(row.percent), (xPct + xAbs) / 2, base, valPaint)
            c.drawText(row.absolute?.let { fmt2(it) } ?: "—", (xAbs + xRefPct) / 2, base, valPaint)
            val refP = text(8f, MUTED, REGULAR, Paint.Align.CENTER)
            c.drawText(row.cell.refPercent, (xRefPct + xRefAbs) / 2, base, refP)
            c.drawText(row.cell.refAbsolute, (xRefAbs + xFlag) / 2, base, refP)
            drawFlag(c, row.flag, (xFlag + x7) / 2, top + rowH / 2)
            s.y += rowH
        }
        // total row
        val top = s.y
        c.drawLine(x0, top, x7, top, stroke(NAVY, 1f))
        val base = top + 13f
        c.drawText("Total leucocytes counted", x0 + 22f, base, text(8.8f, NAVY, BOLD))
        val tp = text(8.8f, NAVY, BOLD, Paint.Align.CENTER)
        c.drawText(r.wbcCounted.toString(), (xCount + xPct) / 2, base, tp)
        c.drawText(if (r.wbcCounted > 0) "100.0" else "0.0", (xPct + xAbs) / 2, base, tp)
        c.drawText(r.effectiveWbc?.let { fmt2(it) } ?: "—", (xAbs + xRefPct) / 2, base, tp)
        c.drawText("—", (xRefPct + xRefAbs) / 2, base, text(8f, MUTED, REGULAR, Paint.Align.CENTER))
        c.drawText(TLC_REF_TEXT, (xRefAbs + xFlag) / 2, base, text(8f, MUTED, REGULAR, Paint.Align.CENTER))
        drawFlag(c, s.data.tlcFlag(r.effectiveWbc), (xFlag + x7) / 2, top + 9f)
        s.y = top + 19f
        c.drawLine(x0, s.y, x7, s.y, stroke(LINE))
        // nRBC line (outside 100-cell count)
        val ny = s.y + 13f
        c.drawRoundRect(RectF(x0 + 9f, s.y + 6.5f, x0 + 15.5f, s.y + 13f), 1.5f, 1.5f,
            fill(com.hematcount.app.model.CellType.NRBC.accent.toArgb()))
        c.drawText("Nucleated Red Cells (not included in leucocyte total)", x0 + 22f, ny, text(8.4f, INK, MEDIUM))
        val nrbcText = "${r.nrbcCount} counted  ·  ${fmt1(r.nrbcPer100)} per 100 leucocytes"
        c.drawText(nrbcText, xFlag - 4f, ny, text(8.4f, INK, if (r.nrbcCount > 0) BOLD else REGULAR, Paint.Align.RIGHT))
        drawFlag(c, if (r.nrbcCount > 0) Flag.PRESENT else Flag.NONE, (xFlag + x7) / 2, s.y + 9.5f)
        s.y += 20f
        c.drawLine(x0, s.y, x7, s.y, stroke(LINE))
        s.y += 16f
    }

    private fun drawFlag(c: Canvas, flag: Flag, cx: Float, cy: Float) {
        if (flag == Flag.NONE) return
        val color = when (flag) { Flag.HIGH -> RED; Flag.LOW -> BLUE; else -> AMBER }
        val label = if (flag == Flag.PRESENT) "PRESENT" else flag.label
        val p = text(6.6f, Color.WHITE, BOLD, Paint.Align.CENTER)
        val w = p.measureText(label) + 9f
        c.drawRoundRect(RectF(cx - w / 2, cy - 5.5f, cx + w / 2, cy + 5.5f), 5.5f, 5.5f, fill(color))
        c.drawText(label, cx, cy + 2.4f, p)
    }

    private fun indicesAndChart(s: PageState) {
        val r = s.data.results
        val boxH = 118f
        ensure(s, boxH + 30f)
        sectionTitle(s, "Leucocyte indices & distribution")
        val c = s.canvas
        val top = s.y
        val gap = 12f
        val leftW = (W - 2 * M - gap) * 0.52f
        val l = RectF(M, top, M + leftW, top + boxH)
        val rt = RectF(l.right + gap, top, W - M, top + boxH)
        listOf(l, rt).forEach {
            c.drawRoundRect(it, 6f, 6f, fill(PANEL)); c.drawRoundRect(it, 6f, 6f, stroke(LINE))
        }

        // ---- left: indices
        var y = top + 20f
        fun kv(label: String, value: String, sub: String? = null, flag: Flag = Flag.NONE, emphasise: Boolean = false) {
            c.drawText(label, l.left + 12f, y, text(8.2f, MUTED, MEDIUM))
            val vp = text(if (emphasise) 10.5f else 9.2f, if (emphasise) NAVY else INK, BOLD, Paint.Align.RIGHT)
            val vx = l.right - (if (flag != Flag.NONE) 40f else 12f)
            c.drawText(value, vx, y, vp)
            if (flag != Flag.NONE) drawFlag(c, flag, l.right - 22f, y - 3f)
            if (sub != null) c.drawText(sub, l.left + 12f, y + 9.5f, text(6.8f, MUTED))
            y += if (sub != null) 24f else 19f
        }
        kv("Total leucocyte count (analyser)", r.analyserWbc?.let { "${fmt2(it)} ×10³/µL" } ?: "Not entered",
            "Normal range $TLC_REF_TEXT ×10³/µL", s.data.tlcFlag(r.analyserWbc))
        kv("Nucleated red cells", "${fmt1(r.nrbcPer100)} / 100 WBC", "Correction applied at ≥ 5 per 100 leucocytes")
        val corrected = when {
            r.correctionApplied && r.correctedWbc != null -> "${fmt2(r.correctedWbc)} ×10³/µL"
            r.analyserWbc == null -> "—"
            else -> "Not required"
        }
        kv("Corrected leucocyte count", corrected,
            if (r.correctionApplied) "TLC × 100 ÷ (100 + nRBC per 100 WBC)" else null,
            if (r.correctionApplied) s.data.tlcFlag(r.correctedWbc) else Flag.NONE, emphasise = r.correctionApplied)
        val anc = r.rows.first { it.cell == com.hematcount.app.model.CellType.NEUTROPHIL }.absolute
        val band = r.rows.first { it.cell == com.hematcount.app.model.CellType.BAND }.absolute
        val ancTotal = if (anc != null && band != null) anc + band else null
        kv("Absolute neutrophil count (segmented + band)", ancTotal?.let { "${fmt2(it)} ×10³/µL" } ?: "—")

        // ---- right: donut + legend
        val cx = rt.left + 58f; val cy = top + boxH / 2
        val radius = 36f
        val ring = 14f
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = ring; strokeCap = Paint.Cap.BUTT }
        if (r.wbcCounted == 0) {
            arcPaint.color = LINE; c.drawArc(oval, 0f, 360f, false, arcPaint)
        } else {
            var start = -90f
            r.rows.filter { it.count > 0 }.forEach { row ->
                val sweep = row.count * 360f / r.wbcCounted
                arcPaint.color = row.cell.accent.toArgb()
                c.drawArc(oval, start, sweep, false, arcPaint)
                // white separators
                if (sweep < 359f) {
                    arcPaint.color = PANEL
                    c.drawArc(oval, start + sweep - 0.8f, 0.8f, false, arcPaint)
                }
                start += sweep
            }
        }
        c.drawText(r.wbcCounted.toString(), cx, cy + 3f, text(15f, NAVY, BOLD, Paint.Align.CENTER))
        c.drawText("LEUCOCYTES", cx, cy + 13f, text(5.8f, MUTED, BOLD, Paint.Align.CENTER).apply { letterSpacing = 0.08f })

        val legendX = rt.left + 116f
        val items = r.rows.filter { it.count > 0 }.sortedByDescending { it.count }.take(8)
        var ly = top + 16f
        if (items.isEmpty()) c.drawText("No cells counted", legendX, cy, text(8f, MUTED))
        items.forEach { row ->
            c.drawRoundRect(RectF(legendX, ly - 6.5f, legendX + 6.5f, ly), 1.5f, 1.5f, fill(row.cell.accent.toArgb()))
            c.drawText(ellipsize(row.cell.displayName, text(7.8f), rt.right - legendX - 50f), legendX + 11f, ly, text(7.8f, INK))
            c.drawText("${fmt1(row.percent)}%", rt.right - 10f, ly, text(7.8f, INK, BOLD, Paint.Align.RIGHT))
            ly += 12.4f
        }
        s.y = top + boxH + 16f
    }

    private fun alerts(s: PageState) {
        val list = s.data.alerts
        if (list.isEmpty()) return
        val p = text(8.2f, Color.rgb(95, 10, 10), MEDIUM)
        val lines = list.flatMap { wrap("•  $it", p, W - 2 * M - 30f) }
        val h = 22f + lines.size * 11.5f
        ensure(s, h + 10f)
        val c = s.canvas
        val rect = RectF(M, s.y, W - M, s.y + h)
        c.drawRoundRect(rect, 6f, 6f, fill(Color.rgb(255, 241, 239)))
        c.drawRoundRect(rect, 6f, 6f, stroke(Color.rgb(240, 180, 172)))
        c.drawRect(M, s.y, M + 3.5f, s.y + h, fill(RED))
        c.drawText("CLINICAL ALERTS", M + 14f, s.y + 14f, text(7.6f, RED, BOLD).apply { letterSpacing = 0.08f })
        var y = s.y + 27f
        lines.forEach { c.drawText(it, M + 14f, y, p); y += 11.5f }
        s.y += h + 14f
    }

    private fun morphology(s: PageState) {
        val d = s.data
        ensure(s, 70f)
        sectionTitle(s, "Red cell & platelet morphology")
        val c = s.canvas
        val params = MorphologyParam.entries
        val cols = 4
        val cellW = (W - 2 * M) / cols
        val rowH = 22f
        val rows = (params.size + 1 + cols - 1) / cols
        val top = s.y
        val rect = RectF(M, top, W - M, top + rows * rowH + 8f)
        c.drawRoundRect(rect, 6f, 6f, stroke(LINE))
        val entries = params.map { it.displayName to gradeLabel(d.morphology[it] ?: 0) } +
            ("Platelets (smear)" to d.platelet.let { if (it == PlateletEstimate.NOT_ASSESSED) "—" else it.displayName })
        entries.forEachIndexed { i, (name, value) ->
            val col = i % cols; val row = i / cols
            val x = M + col * cellW
            val y = top + 4f + row * rowH
            c.drawText(name, x + 12f, y + 14.5f, text(8.4f, INK, MEDIUM))
            val graded = value != "Nil" && value != "—" && value != "Adequate"
            val vp = text(8.2f, if (graded) Color.WHITE else MUTED, BOLD, Paint.Align.CENTER)
            val w = vp.measureText(value) + 14f
            val bx = x + cellW - 12f - w
            if (graded) c.drawRoundRect(RectF(bx, y + 5f, bx + w, y + 18f), 6.5f, 6.5f, fill(TEAL))
            else c.drawRoundRect(RectF(bx, y + 5f, bx + w, y + 18f), 6.5f, 6.5f, stroke(LINE))
            c.drawText(value, bx + w / 2, y + 14.6f, vp)
        }
        for (i in 1 until cols) c.drawLine(M + i * cellW, top + 6f, M + i * cellW, rect.bottom - 6f, stroke(LINE, 0.6f))
        s.y = rect.bottom + 16f
    }

    private fun remarks(s: PageState) {
        val remarkText = s.data.remarks.ifBlank { "—" }
        val p = text(9f, INK)
        val lines = remarkText.split("\n").flatMap { wrap(it, p, W - 2 * M - 24f) }
        val h = 16f + lines.size * 12.5f
        ensure(s, h + 34f + 52f) // keep signatures with remarks
        sectionTitle(s, "Impression / remarks")
        val c = s.canvas
        val rect = RectF(M, s.y, W - M, s.y + h)
        c.drawRoundRect(rect, 6f, 6f, fill(PANEL)); c.drawRoundRect(rect, 6f, 6f, stroke(LINE))
        var y = s.y + 16f
        lines.forEach { c.drawText(it, M + 12f, y, p); y += 12.5f }
        s.y += h + 22f
    }

    /** AI-assisted interpretation (optional). Markdown headings/bullets are rendered simply. */
    private fun aiSection(s: PageState) {
        val raw = s.data.aiText
        if (raw.isBlank()) return
        val body = text(8.4f, INK)
        val head = text(8.6f, TEAL, BOLD)
        val maxW = W - 2 * M - 24f
        data class L(val t: String, val p: Paint, val indent: Float, val gap: Float)
        val lines = mutableListOf<L>()
        raw.replace("\r", "").split("\n").forEach { line0 ->
            val line = line0.trim().replace("**", "").replace("__", "")
            when {
                line.isBlank() -> {}
                line.startsWith("#") -> lines += L(line.trimStart('#', ' '), head, 0f, 4f)
                line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ") -> {
                    val parts = wrap(line.substring(2).trim(), body, maxW - 10f)
                    parts.forEachIndexed { i, t -> lines += L(if (i == 0) "•  $t" else "    $t", body, 4f, 0f) }
                }
                else -> wrap(line, body, maxW).forEach { lines += L(it, body, 0f, 0f) }
            }
        }
        sectionTitle(s, if (s.data.aiInvestigationsOnly) "Suggested further investigations" else "AI-assisted interpretation",
            s.data.aiProvider.takeIf { it.isNotBlank() }?.let { "AI-assisted · $it" })
        val c0 = s.canvas
        val note = "Decision support only — generated by an AI model from de-identified data; not a diagnosis. Verify against the smear and clinical findings."
        c0.drawText(ellipsize(note, text(7f), W - 2 * M), M, s.y + 4f, text(7f, AMBER, MEDIUM))
        s.y += 14f
        lines.forEach { l ->
            ensure(s, 12f + l.gap)
            s.y += l.gap
            s.canvas.drawText(l.t, M + 8f + l.indent, s.y + 8f, l.p)
            s.y += 11.2f
        }
        s.y += 10f
    }

    private fun signatures(s: PageState) {
        ensure(s, 50f)
        val c = s.canvas
        val y = maxOf(s.y + 26f, 0f)
        val lineW = 170f
        c.drawLine(M, y, M + lineW, y, stroke(INK, 0.7f))
        c.drawText(s.data.reportedBy.ifBlank { "Reported by" }, M, y + 12f, text(8.6f, INK, BOLD))
        c.drawText("Reported by", M, y + 23f, text(7.4f, MUTED))
        val rx = W - M
        c.drawLine(rx - lineW, y, rx, y, stroke(INK, 0.7f))
        c.drawText("Consultant Pathologist", rx, y + 12f, text(8.6f, INK, BOLD, Paint.Align.RIGHT))
        c.drawText("Signature & seal", rx, y + 23f, text(7.4f, MUTED, REGULAR, Paint.Align.RIGHT))
        c.drawText("— End of report —", W / 2, y + 23f, text(7.4f, MUTED, MEDIUM, Paint.Align.CENTER))
        s.y = y + 30f
    }

    // ------------------------------------------------------------------ text utils
    private fun wrap(t: String, p: Paint, maxW: Float): List<String> {
        if (t.isEmpty()) return listOf("")
        val out = mutableListOf<String>()
        var line = StringBuilder()
        t.split(" ").forEach { word ->
            val trial = if (line.isEmpty()) word else "$line $word"
            if (p.measureText(trial) <= maxW) { line = StringBuilder(trial) }
            else {
                if (line.isNotEmpty()) out += line.toString()
                var w = word
                while (p.measureText(w) > maxW && w.length > 1) {
                    var k = w.length
                    while (k > 1 && p.measureText(w.substring(0, k)) > maxW) k--
                    out += w.substring(0, k); w = w.substring(k)
                }
                line = StringBuilder(w)
            }
        }
        if (line.isNotEmpty()) out += line.toString()
        return out
    }

    private fun ellipsize(t: String, p: Paint, maxW: Float): String {
        if (p.measureText(t) <= maxW) return t
        var k = t.length
        while (k > 0 && p.measureText(t.substring(0, k) + "…") > maxW) k--
        return t.substring(0, k).trimEnd() + "…"
    }
}

private fun androidx.compose.ui.graphics.Color.toArgb(): Int =
    android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
