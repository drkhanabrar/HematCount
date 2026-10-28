package com.hematcount.app.model

import androidx.compose.ui.graphics.Color
import com.hematcount.app.model.Calculations.fmt1
import com.hematcount.app.model.Calculations.fmt2
import java.util.Locale
import kotlin.math.roundToLong

data class ReportRow(
    val name: String,
    val count: Int,
    val percent: Double?,
    /** Third numeric column (absolute count, per µL…), already formatted. */
    val extra: String?,
    val normal: String,
    val flag: Flag,
    val accent: Color,
    /** Rows that are tallied separately (not part of the % total). */
    val separate: Boolean = false,
)

data class ReportIndex(
    val label: String,
    val value: String,
    val sub: String? = null,
    val flag: Flag = Flag.NONE,
    val emphasise: Boolean = false,
)

/** Everything needed to show results and print the PDF for a non-blood mode. */
data class ModeReport(
    val mode: CounterMode,
    val title: String,
    val specimen: String,
    val totalLabel: String,
    val total: Int,
    val target: Int,
    val rows: List<ReportRow>,
    val extraHeader: String,
    val indices: List<ReportIndex>,
    val setup: List<Pair<String, String>>,
    val alerts: List<String>,
    val footnote: String,
    /** Optional second table (fluid differential). */
    val rows2Title: String? = null,
    val rows2: List<ReportRow> = emptyList(),
    val rows2ExtraHeader: String = "",
)

object ModeCalc {

    fun num(s: String?): Double? = s?.trim()?.replace(',', '.')?.toDoubleOrNull()

    private fun pct(c: Int, t: Int): Double = if (t > 0) c * 100.0 / t else 0.0

    private fun flagRange(v: Double, low: Double?, high: Double?): Flag = when {
        high != null && v > high -> Flag.HIGH
        low != null && v < low -> Flag.LOW
        else -> Flag.NONE
    }

    fun fmtInt(v: Double): String = String.format(Locale.US, "%,d", v.roundToLong())

    // =================================================================== bone marrow
    fun marrow(counts: Map<String, Int>, inputs: Map<String, String>, target: Int): ModeReport {
        val total = MarrowCell.countedTypes.sumOf { counts[it.id] ?: 0 }
        val rows = MarrowCell.keypadOrder.map { c ->
            val n = counts[c.id] ?: 0
            if (!c.countsInTotal) ReportRow(c.displayName, n, null, null, c.normalText, Flag.NONE, c.accent, separate = true)
            else {
                val p = pct(n, total)
                val flag = when {
                    total == 0 -> Flag.NONE
                    c.low == null && c.high == 0.0 -> if (n > 0) Flag.PRESENT else Flag.NONE
                    else -> flagRange(p, c.low, c.high)
                }
                ReportRow(c.displayName, n, p, null, c.normalText, flag, c.accent)
            }
        }
        fun sumOf(l: Lineage) = MarrowCell.countedTypes.filter { it.lineage == l }.sumOf { counts[it.id] ?: 0 }
        val myeloid = sumOf(Lineage.MYELOID)
        val erythroid = sumOf(Lineage.ERYTHROID)
        val me = if (erythroid > 0) myeloid.toDouble() / erythroid else null
        fun p(c: MarrowCell) = pct(counts[c.id] ?: 0, total)
        val blasts = p(MarrowCell.BLAST)
        val plasma = p(MarrowCell.PLASMA_CELL)

        val indices = mutableListOf(
            ReportIndex("Myeloid : erythroid ratio", me?.let { "${fmt2(it)} : 1" } ?: "—",
                "Normal ${MarrowCell.ME_LOW} – ${MarrowCell.ME_HIGH} : 1",
                me?.let { flagRange(it, MarrowCell.ME_LOW, MarrowCell.ME_HIGH) } ?: Flag.NONE, emphasise = true),
            ReportIndex("Granulocytic series (incl. blasts)", "${fmt1(pct(myeloid, total))} %"),
            ReportIndex("Erythroid series", "${fmt1(pct(erythroid, total))} %"),
            ReportIndex("Blasts", "${fmt1(blasts)} %", "Normal < 5 %", if (blasts >= 5) Flag.HIGH else Flag.NONE),
            ReportIndex("Plasma cells", "${fmt1(plasma)} %", "Normal 0.4 – 3.9 %", if (plasma > 3.9) Flag.HIGH else Flag.NONE),
            ReportIndex("Megakaryocytes tallied", "${counts[MarrowCell.MEGAKARYOCYTE.id] ?: 0}"),
        )
        val setup = listOf(
            "Aspirate site" to (inputs["bm.site"].orEmpty().ifBlank { "—" }),
            "Particles" to label(inputs["bm.particles"], Particles.entries.associate { it.name to it.label }),
            "Cellularity" to label(inputs["bm.cellularity"], Cellularity.entries.associate { it.name to it.label }),
            "Megakaryocytes" to label(inputs["bm.mega"], MegaStatus.entries.associate { it.name to it.label }),
            "Iron stores (Perls)" to label(inputs["bm.iron"], IronStores.entries.associate { it.name to it.label }),
        )
        val alerts = mutableListOf<String>()
        if (total > 0) {
            if (blasts >= 20) alerts += "Blasts ${fmt1(blasts)}% (≥ 20%) — consistent with acute leukaemia (WHO 5th ed.); confirm lineage by cytochemistry and flow cytometry."
            else if (blasts >= 5) alerts += "Increased blasts (${fmt1(blasts)}%) — consider myelodysplastic neoplasm with increased blasts, CML in accelerated phase or regenerating marrow."
            if (plasma >= 10) alerts += "Plasma cells ${fmt1(plasma)}% (≥ 10%) — evaluate for plasma cell myeloma (clonality, serum / urine protein electrophoresis, free light chains)."
            if (me != null && me < MarrowCell.ME_LOW) alerts += "Low M:E ratio (${fmt2(me)}) — erythroid hyperplasia or myeloid hypoplasia."
            if (me != null && me > MarrowCell.ME_HIGH) alerts += "Raised M:E ratio (${fmt2(me)}) — myeloid hyperplasia or erythroid hypoplasia."
            if ((counts[MarrowCell.OTHER.id] ?: 0) > 0) alerts += "Abnormal / other cells recorded — describe their morphology in the remarks."
            if (total < 300) alerts += "Only $total nucleated cells counted — ICSH recommends a 500-cell marrow differential."
        }
        return ModeReport(
            mode = CounterMode.MARROW, title = CounterMode.MARROW.reportTitle, specimen = "Bone marrow aspirate smear",
            totalLabel = "Nucleated cells counted", total = total, target = target,
            rows = rows, extraHeader = "", indices = indices, setup = setup, alerts = alerts,
            footnote = "Normal adult marrow differential: Wintrobe's Clinical Hematology (mean and 95% range). Megakaryocytes are assessed separately and excluded from the percentage count.",
        )
    }

    private fun label(v: String?, map: Map<String, String>): String = map[v] ?: map.values.first()

    // =================================================================== CSF & body fluids
    fun fluid(chamber: Map<String, Int>, diff: Map<String, Int>, inputs: Map<String, String>, diffTarget: Int): ModeReport {
        val preset = runCatching { FluidPreset.valueOf(inputs["fl.preset"] ?: "CSF") }.getOrDefault(FluidPreset.CSF)
        val dilution = num(inputs["fl.dilution"]) ?: preset.defaultDilution.toDouble()
        val squares = inputs["fl.squares"]?.split(",")?.filter { it.isNotBlank() } ?: preset.defaultSquares
        val area = Neubauer.area(squares)
        val chamberItems = FluidItems.chamberItems(preset)

        fun perUl(id: String) = Neubauer.perMicrolitre(chamber[id] ?: 0, dilution, area)
        val nucleated = perUl(FluidItems.NUCLEATED.id)
        val rbc = perUl(FluidItems.RBC.id)

        val rows = chamberItems.map { item ->
            val v = perUl(item.id)
            val (text, flag, normal) = when {
                v == null -> Triple("—", Flag.NONE, item.normalText)
                preset == FluidPreset.BLOOD_WBC -> Triple("${fmt2(v / 1000)} ×10³/µL", flagRange(v / 1000, 4.0, 10.0), fluidNormalNucleated(preset))
                preset == FluidPreset.BLOOD_RBC -> Triple("${fmt2(v / 1_000_000)} ×10⁶/µL", flagRange(v / 1_000_000, 3.8, 5.5), fluidNormalNucleated(preset))
                preset == FluidPreset.BLOOD_PLT -> Triple("${fmtInt(v / 1000)} ×10³/µL", flagRange(v / 1000, 150.0, 410.0), fluidNormalNucleated(preset))
                item == FluidItems.RBC -> Triple("${fmtInt(v)} /µL", if (preset == FluidPreset.CSF && v > 0) Flag.PRESENT else Flag.NONE,
                    if (preset == FluidPreset.CSF) "0 /µL" else "—")
                else -> {
                    val f = when (preset) {
                        FluidPreset.CSF -> flagRange(v, null, 5.0)
                        FluidPreset.SYNOVIAL -> flagRange(v, null, 200.0)
                        FluidPreset.PERITONEAL -> flagRange(v, null, 500.0)
                        FluidPreset.PLEURAL, FluidPreset.PERICARDIAL -> flagRange(v, null, 1000.0)
                        else -> Flag.NONE
                    }
                    Triple("${fmtInt(v)} /µL", f, fluidNormalNucleated(preset))
                }
            }
            ReportRow(item.displayName, chamber[item.id] ?: 0, null, text, normal, flag, item.accent)
        }

        // CSF traumatic-tap correction: WBC(corrected) = WBC(CSF) − RBC(CSF) × WBC(blood) / RBC(blood)
        val bloodWbc = num(inputs["fl.bloodWbc"]) // ×10³/µL
        val bloodRbc = num(inputs["fl.bloodRbc"]) // ×10⁶/µL
        val correctedNuc = if (preset == FluidPreset.CSF && nucleated != null && rbc != null && rbc > 0 &&
            bloodWbc != null && bloodRbc != null && bloodRbc > 0)
            (nucleated - rbc * (bloodWbc * 1000) / (bloodRbc * 1_000_000)).coerceAtLeast(0.0) else null

        val diffItems = if (preset.hasDifferential) FluidItems.differentialItems(preset) else emptyList()
        val diffTotal = diffItems.sumOf { diff[it.id] ?: 0 }
        val nucForAbs = correctedNuc ?: nucleated
        val rows2 = diffItems.map { item ->
            val n = diff[item.id] ?: 0
            val p = pct(n, diffTotal)
            val abs = nucForAbs?.let { it * p / 100 }
            val flag = when {
                diffTotal == 0 -> Flag.NONE
                item.id == "FD_ATYPICAL" -> if (n > 0) Flag.PRESENT else Flag.NONE
                preset == FluidPreset.CSF && item.id == "FD_NEUTROPHIL" -> flagRange(p, null, 6.0)
                preset == FluidPreset.SYNOVIAL && item.id == "FD_NEUTROPHIL" -> flagRange(p, null, 25.0)
                preset == FluidPreset.PERITONEAL && item.id == "FD_NEUTROPHIL" && abs != null -> flagRange(abs, null, 250.0)
                else -> Flag.NONE
            }
            ReportRow(item.displayName, n, p, abs?.let { "${fmtInt(it)} /µL" }, item.normalText, flag, item.accent)
        }

        val indices = mutableListOf<ReportIndex>()
        if (!preset.isBlood) {
            indices += ReportIndex("Total nucleated cell count", nucleated?.let { "${fmtInt(it)} /µL" } ?: "—",
                "Guide: ${fluidNormalNucleated(preset)}", rows.firstOrNull { it.name == FluidItems.NUCLEATED.displayName }?.flag ?: Flag.NONE, emphasise = true)
            indices += ReportIndex("Red cell count", rbc?.let { "${fmtInt(it)} /µL" } ?: "—")
            if (preset == FluidPreset.CSF) indices += ReportIndex("Corrected nucleated count (traumatic tap)",
                correctedNuc?.let { "${fmtInt(it)} /µL" } ?: if (bloodWbc == null || bloodRbc == null) "Enter blood WBC & RBC" else "Not applicable",
                "CSF WBC − (CSF RBC × blood WBC ÷ blood RBC)", if (correctedNuc != null && correctedNuc > 5) Flag.HIGH else Flag.NONE)
        } else {
            rows.firstOrNull()?.let { indices += ReportIndex(it.name + " count", it.extra ?: "—", "Normal ${it.normal}", it.flag, emphasise = true) }
        }
        indices += ReportIndex("Area counted", "${fmt2(area)} mm² (${fmt2(area * Neubauer.DEPTH_MM)} µL)", "${squares.size} square(s) · depth 0.1 mm")
        indices += ReportIndex("Dilution factor", if (dilution == 1.0) "Undiluted (1)" else "1 in ${fmtNum(dilution)}")

        val setup = listOf(
            "Specimen" to preset.specimen,
            "Appearance" to (runCatching { FluidAppearance.valueOf(inputs["fl.appearance"] ?: "") }.getOrNull()?.label ?: FluidAppearance.NOT_RECORDED.label),
            "Volume received" to (inputs["fl.volume"].orEmpty().ifBlank { "—" }),
            "Chamber" to "Improved Neubauer haemocytometer",
        )
        val alerts = mutableListOf<String>()
        if (area <= 0) alerts += "Select the squares counted on the chamber diagram."
        if (preset == FluidPreset.CSF) {
            val n = correctedNuc ?: nucleated
            val neut = rows2.firstOrNull { it.name == "Neutrophils" }?.percent ?: 0.0
            if (n != null && n > 5) alerts += "CSF pleocytosis (${fmtInt(n)} nucleated cells/µL)" +
                if (diffTotal > 0) (if (neut >= 50) " with neutrophil predominance — consider bacterial meningitis; correlate with Gram stain, culture, glucose and protein." else " with mononuclear predominance — consider viral, tuberculous or fungal meningitis.") else "."
            if (rbc != null && rbc > 0) alerts += "Red cells present in CSF — compare serial tubes (traumatic tap clears) and look for xanthochromia (subarachnoid haemorrhage)."
        }
        if (preset == FluidPreset.PERITONEAL) {
            val absNeut = rows2.firstOrNull { it.name == "Neutrophils" }?.let { r -> nucForAbs?.let { it * (r.percent ?: 0.0) / 100 } }
            if (absNeut != null && absNeut >= 250) alerts += "Ascitic neutrophils ${fmtInt(absNeut)}/µL (≥ 250) — suggests spontaneous bacterial peritonitis."
        }
        if (preset == FluidPreset.SYNOVIAL && nucleated != null && nucleated > 2000) alerts += "Synovial nucleated count > 2000/µL — inflammatory or septic arthritis range; examine for crystals and culture."
        if (diffTotal > 0 && (diff["FD_ATYPICAL"] ?: 0) > 0) alerts += "Atypical / malignant cells recorded — refer for cytopathology review."

        return ModeReport(
            mode = CounterMode.FLUID, title = if (preset.isBlood) "Manual Haemocytometer Count" else "${preset.specimen} — Cell Count",
            specimen = preset.specimen, totalLabel = "Cells counted in chamber", total = rows.sumOf { it.count }, target = 0,
            rows = rows, extraHeader = "Concentration", indices = indices, setup = setup, alerts = alerts,
            footnote = "Concentration = cells counted × dilution ÷ (area counted in mm² × 0.1 mm chamber depth). " +
                "Body-fluid figures are general adult guides (Henry's Clinical Diagnosis and Management by Laboratory Methods; Dacie & Lewis); interpret with fluid biochemistry.",
            rows2Title = if (preset.hasDifferential) "Differential count (stained cytospin / smear)" else null,
            rows2 = rows2, rows2ExtraHeader = "Absolute",
        )
    }

    private fun fmtNum(v: Double) = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    // =================================================================== reticulocytes
    fun retic(counts: Map<String, Int>, inputs: Map<String, String>, target: Int): ModeReport {
        val miller = inputs["rt.miller"] == "1"
        val items = RetItems.items(miller)
        val r = counts[items[0].id] ?: 0
        val o = counts[items[1].id] ?: 0
        val retPct: Double? = if (miller) { if (o > 0) r * 100.0 / (o * 9.0) else null }
        else { if (r + o > 0) r * 100.0 / (r + o) else null }
        val rbcCount = num(inputs["rt.rbc"]) // ×10⁶/µL
        val hct = num(inputs["rt.hct"])       // %
        val absRet = if (retPct != null && rbcCount != null) retPct / 100 * rbcCount * 1000 else null // ×10³/µL (= ×10⁹/L)
        val corrected = if (retPct != null && hct != null) retPct * hct / 45.0 else null
        val maturation = hct?.let { when { it >= 40 -> 1.0; it >= 30 -> 1.5; it >= 20 -> 2.0; else -> 2.5 } }
        val rpi = if (corrected != null && maturation != null) corrected / maturation else null

        val rows = listOf(
            ReportRow(items[0].displayName, r, if (miller) null else pct(r, r + o), null, items[0].normalText, Flag.NONE, items[0].accent),
            ReportRow(items[1].displayName, o, if (miller) null else pct(o, r + o), null, "", Flag.NONE, items[1].accent),
        )
        val indices = listOf(
            ReportIndex("Reticulocyte percentage", retPct?.let { "${fmt2(it)} %" } ?: "—", "Normal 0.5 – 2.5 %",
                retPct?.let { flagRange(it, 0.5, 2.5) } ?: Flag.NONE, emphasise = true),
            ReportIndex("Absolute reticulocyte count", absRet?.let { "${fmt1(it)} ×10³/µL" } ?: "Enter red cell count",
                "Normal 50 – 100 ×10³/µL (×10⁹/L)", absRet?.let { flagRange(it, 50.0, 100.0) } ?: Flag.NONE),
            ReportIndex("Corrected reticulocyte %", corrected?.let { "${fmt2(it)} %" } ?: "Enter haematocrit", "Retic % × Hct ÷ 45"),
            ReportIndex("Reticulocyte production index", rpi?.let { fmt2(it) } ?: "—",
                maturation?.let { "Corrected % ÷ maturation time ${fmt1(it)} d" } ?: "Needs haematocrit"),
        )
        val setup = listOf(
            "Method" to if (miller) "Miller ocular disc (retics in large square, red cells in small square ×9)" else "Direct count of red cells",
            "Red cell count" to (rbcCount?.let { "${fmt2(it)} ×10⁶/µL" } ?: "—"),
            "Haematocrit" to (hct?.let { "${fmt1(it)} %" } ?: "—"),
        )
        val alerts = mutableListOf<String>()
        val counted = if (miller) o * 9 else r + o
        if (counted in 1 until 1000) alerts += "Only $counted red cells counted — count at least 1000 for a reliable reticulocyte percentage."
        if (rpi != null && hct != null && hct < 35) {
            alerts += if (rpi < 2) "RPI ${fmt2(rpi)} (< 2) with anaemia — inadequate marrow response (hypoproliferative: deficiency, marrow failure, renal disease)."
            else "RPI ${fmt2(rpi)} (≥ 2) — appropriate marrow response (haemolysis or blood loss)."
        }
        return ModeReport(
            mode = CounterMode.RETIC, title = CounterMode.RETIC.reportTitle, specimen = "EDTA blood, supravital stain",
            totalLabel = if (miller) "Red cells in small squares" else "Red cells counted", total = counted, target = target,
            rows = rows, extraHeader = "", indices = indices, setup = setup, alerts = alerts,
            footnote = "Reference: Dacie & Lewis Practical Haematology, 12th ed. Maturation (shift) correction: Hct ≥ 40% 1.0; 30–39% 1.5; 20–29% 2.0; < 20% 2.5 days.",
        )
    }

    // =================================================================== red cell abnormalities
    fun rbcMorph(counts: Map<String, Int>, target: Int): ModeReport {
        val total = RbcItems.items.sumOf { counts[it.id] ?: 0 }
        val rows = RbcItems.items.map { it ->
            val n = counts[it.id] ?: 0
            val p = pct(n, total)
            val flag = when {
                total == 0 || it.id == "RB_NORMAL" -> Flag.NONE
                it.id == "RB_SCHISTO" -> if (p > 1.0) Flag.HIGH else Flag.NONE
                it.id == "RB_SICKLE" -> if (n > 0) Flag.PRESENT else Flag.NONE
                p >= 1.0 -> Flag.PRESENT
                else -> Flag.NONE
            }
            ReportRow(it.displayName, n, p, null, it.normalText, flag, it.accent)
        }
        val abnormal = total - (counts["RB_NORMAL"] ?: 0)
        fun p(id: String) = pct(counts[id] ?: 0, total)
        val indices = listOf(
            ReportIndex("Abnormal red cells", "${fmt1(pct(abnormal, total))} %", "of $total red cells counted", emphasise = true),
            ReportIndex("Schistocytes", "${fmt2(p("RB_SCHISTO"))} %", "ICSH: > 1% clinically significant", if (p("RB_SCHISTO") > 1) Flag.HIGH else Flag.NONE),
        )
        val alerts = mutableListOf<String>()
        if (total > 0) {
            if (p("RB_SCHISTO") > 1) alerts += "Schistocytes ${fmt2(p("RB_SCHISTO"))}% — exclude thrombotic microangiopathy (TTP / HUS) and DIC; correlate with platelet count, LDH and haptoglobin."
            if ((counts["RB_SICKLE"] ?: 0) > 0) alerts += "Sickle cells seen — confirm with sickling test and haemoglobin HPLC / electrophoresis."
            if (p("RB_SPHERO") >= 1) alerts += "Spherocytes ${fmt1(p("RB_SPHERO"))}% — consider direct antiglobulin test and hereditary spherocytosis work-up."
            if (p("RB_DACRO") >= 1) alerts += "Tear-drop cells ${fmt1(p("RB_DACRO"))}% — consider marrow fibrosis or infiltration."
            if (p("RB_BITE") >= 1) alerts += "Bite / blister cells — consider G6PD deficiency or oxidant haemolysis."
            if (total < 1000) alerts += "Only $total red cells counted — ICSH recommends at least 1000 red cells for schistocyte enumeration."
        }
        return ModeReport(
            mode = CounterMode.RBC_MORPH, title = CounterMode.RBC_MORPH.reportTitle, specimen = "EDTA peripheral blood smear",
            totalLabel = "Red cells counted", total = total, target = target,
            rows = rows, extraHeader = "", indices = indices, setup = emptyList(), alerts = alerts,
            footnote = "Percentages of red cells counted in the monolayer. Schistocyte threshold: ICSH recommendations for identification and enumeration of schistocytes (Zini et al., 2012).",
        )
    }

    // =================================================================== platelet estimate
    fun platelet(counts: Map<String, Int>, inputs: Map<String, String>): ModeReport {
        val plt = counts[PlateletItems.PLATELET.id] ?: 0
        val fields = counts[PlateletItems.FIELD.id] ?: 0
        val clumps = counts[PlateletItems.CLUMP.id] ?: 0
        val giant = counts[PlateletItems.GIANT.id] ?: 0
        val factor = num(inputs["pl.factor"]) ?: 15000.0
        val avg = if (fields > 0) plt.toDouble() / fields else null
        val estimate = avg?.let { it * factor / 1000 } // ×10³/µL
        val rows = listOf(
            ReportRow("Platelets counted", plt, null, avg?.let { "${fmt1(it)} / field" }, "7 – 21 / field", avg?.let { flagRange(it, 7.0, 21.0) } ?: Flag.NONE, PlateletItems.PLATELET.accent),
            ReportRow("Oil-immersion fields", fields, null, null, "≥ 10 fields", Flag.NONE, PlateletItems.FIELD.accent, separate = true),
            ReportRow("Platelet clumps", clumps, null, null, "None", if (clumps > 0) Flag.PRESENT else Flag.NONE, PlateletItems.CLUMP.accent, separate = true),
            ReportRow("Giant platelets", giant, null, null, "Occasional", Flag.NONE, PlateletItems.GIANT.accent, separate = true),
        )
        val indices = listOf(
            ReportIndex("Estimated platelet count", estimate?.let { "${fmtInt(it)} ×10³/µL" } ?: "Count at least one field",
                "Normal 150 – 410 ×10³/µL", estimate?.let { flagRange(it, 150.0, 410.0) } ?: Flag.NONE, emphasise = true),
            ReportIndex("Average per oil-immersion field", avg?.let { fmt1(it) } ?: "—", "Fields counted: $fields"),
            ReportIndex("Multiplication factor", fmtInt(factor), "Platelets per field × factor = platelets / µL"),
        )
        val alerts = mutableListOf<String>()
        if (clumps > 0) alerts += "Platelet clumps seen — the estimate may be falsely low; consider EDTA pseudothrombocytopenia and repeat on a citrate sample."
        if (fields in 1 until 10) alerts += "Only $fields fields counted — count at least 10 oil-immersion fields."
        if (giant > 0) alerts += "Giant platelets noted — consider immune thrombocytopenia, inherited macrothrombocytopenia or myeloproliferative neoplasm."
        return ModeReport(
            mode = CounterMode.PLATELET, title = CounterMode.PLATELET.reportTitle, specimen = "EDTA peripheral blood smear",
            totalLabel = "Platelets counted", total = plt, target = 0,
            rows = rows, extraHeader = "Average", indices = indices,
            setup = listOf("Method" to "Average platelets per oil-immersion (×100) field in the monolayer × factor"),
            alerts = alerts,
            footnote = "A factor of 15 000 – 20 000 is commonly used and should be validated locally against the analyser (Dacie & Lewis).",
        )
    }

    // =================================================================== viability
    fun viability(counts: Map<String, Int>, inputs: Map<String, String>): ModeReport {
        val live = counts[ViabilityItems.LIVE.id] ?: 0
        val dead = counts[ViabilityItems.DEAD.id] ?: 0
        val total = live + dead
        val dilution = num(inputs["vb.dilution"]) ?: 2.0
        val squares = (num(inputs["vb.squares"]) ?: 4.0).coerceAtLeast(1.0)
        fun perMl(n: Int) = n / squares * dilution * 10_000
        val viability = if (total > 0) live * 100.0 / total else null
        val rows = listOf(
            ReportRow(ViabilityItems.LIVE.displayName, live, pct(live, total), "${fmtSci(perMl(live))} /mL", "", Flag.NONE, ViabilityItems.LIVE.accent),
            ReportRow(ViabilityItems.DEAD.displayName, dead, pct(dead, total), "${fmtSci(perMl(dead))} /mL", "", Flag.NONE, ViabilityItems.DEAD.accent),
        )
        val indices = listOf(
            ReportIndex("Viability", viability?.let { "${fmt1(it)} %" } ?: "—", "Viable ÷ total × 100", emphasise = true),
            ReportIndex("Viable cell concentration", "${fmtSci(perMl(live))} cells/mL"),
            ReportIndex("Total cell concentration", "${fmtSci(perMl(total))} cells/mL"),
        )
        return ModeReport(
            mode = CounterMode.VIABILITY, title = CounterMode.VIABILITY.reportTitle, specimen = "Cell suspension with trypan blue",
            totalLabel = "Cells counted", total = total, target = 0,
            rows = rows, extraHeader = "Concentration", indices = indices,
            setup = listOf("Dilution factor" to "1 in ${fmtNum(dilution)} (includes trypan blue)", "Large squares counted" to fmtNum(squares)),
            alerts = if (total in 1 until 100) listOf("Fewer than 100 cells counted — precision is limited.") else emptyList(),
            footnote = "Cells/mL = (cells ÷ large squares counted) × dilution × 10⁴. Non-viable cells take up trypan blue.",
        )
    }

    private fun fmtSci(v: Double): String = when {
        v <= 0 -> "0"
        v >= 1e6 -> String.format(Locale.US, "%.2f × 10⁶", v / 1e6)
        v >= 1e4 -> String.format(Locale.US, "%.2f × 10⁴", v / 1e4)
        else -> fmtInt(v)
    }

    // =================================================================== custom
    fun custom(items: List<CountItem>, counts: Map<String, Int>, inputs: Map<String, String>, target: Int): ModeReport {
        val total = items.filter { it.countsInTotal }.sumOf { counts[it.id] ?: 0 }
        val rows = items.map { val n = counts[it.id] ?: 0
            ReportRow(it.displayName, n, if (it.countsInTotal) pct(n, total) else null, null, it.normalText, Flag.NONE, it.accent, separate = !it.countsInTotal) }
        val title = inputs["cu.title"].orEmpty().ifBlank { CounterMode.CUSTOM.reportTitle }
        return ModeReport(
            mode = CounterMode.CUSTOM, title = title, specimen = inputs["cu.specimen"].orEmpty().ifBlank { "—" },
            totalLabel = "Cells counted", total = total, target = target, rows = rows, extraHeader = "",
            indices = listOf(ReportIndex("Total counted", "$total")), setup = emptyList(),
            alerts = if (target != 0 && total in 1 until target) listOf("Count based on $total cells (target $target).") else emptyList(),
            footnote = "Custom counter defined by the user.",
        )
    }
}
