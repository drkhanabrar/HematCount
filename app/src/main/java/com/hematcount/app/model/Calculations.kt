package com.hematcount.app.model

import java.util.Locale

enum class Flag(val label: String) { NONE(""), HIGH("H"), LOW("L"), PRESENT("Present") }

data class RowResult(
    val cell: CellType,
    val count: Int,
    val percent: Double,
    val absolute: Double?,
    val flag: Flag,
)

data class CountResults(
    val wbcCounted: Int,
    val nrbcCount: Int,
    val nrbcPer100: Double,
    val rows: List<RowResult>,
    val analyserWbc: Double?,
    val correctedWbc: Double?,
    val correctionApplied: Boolean,
    /** WBC used for absolute counts (corrected if correction applied). */
    val effectiveWbc: Double?,
)

object Calculations {

    /** nRBC correction is applied when ≥ 5 nRBC per 100 WBC are counted. */
    const val NRBC_CORRECTION_THRESHOLD = 5.0

    fun parseWbc(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it < 1000 }

    fun compute(
        counts: Map<CellType, Int>,
        wbcText: String,
        applyCorrection: Boolean,
    ): CountResults {
        val wbcCounted = CellType.wbcTypes.sumOf { counts[it] ?: 0 }
        val nrbc = counts[CellType.NRBC] ?: 0
        val nrbcPer100 = if (wbcCounted > 0) nrbc * 100.0 / wbcCounted else 0.0
        val analyser = parseWbc(wbcText)

        val correctionApplied =
            applyCorrection && analyser != null && nrbcPer100 >= NRBC_CORRECTION_THRESHOLD
        // Corrected WBC = Total WBC × 100 / (100 + nRBC per 100 WBC)
        val corrected = if (analyser != null) analyser * 100.0 / (100.0 + nrbcPer100) else null
        val effective = if (correctionApplied) corrected else analyser

        val rows = CellType.wbcTypes.map { cell ->
            val c = counts[cell] ?: 0
            val pct = if (wbcCounted > 0) c * 100.0 / wbcCounted else 0.0
            val abs = effective?.let { it * pct / 100.0 }
            RowResult(cell, c, pct, abs, flagFor(cell, c, pct, abs, wbcCounted))
        }
        return CountResults(
            wbcCounted, nrbc, nrbcPer100, rows, analyser, corrected, correctionApplied, effective
        )
    }

    private fun flagFor(cell: CellType, count: Int, pct: Double, abs: Double?, total: Int): Flag {
        if (total == 0) return Flag.NONE
        // Cells that should not be present in normal adult blood
        if (cell.refPercentHigh == 0.0) return if (count > 0) Flag.PRESENT else Flag.NONE
        if (abs != null && (cell.refAbsLow != null || cell.refAbsHigh != null)) {
            cell.refAbsHigh?.let { if (abs > it) return Flag.HIGH }
            cell.refAbsLow?.let { if (abs < it) return Flag.LOW }
            return Flag.NONE
        }
        cell.refPercentHigh?.let { if (pct > it) return Flag.HIGH }
        cell.refPercentLow?.let { if (pct < it) return Flag.LOW }
        return Flag.NONE
    }

    fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)
    fun fmt2(v: Double): String = String.format(Locale.US, "%.2f", v)
}
