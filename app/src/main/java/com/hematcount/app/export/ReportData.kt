package com.hematcount.app.export

import com.hematcount.app.CounterViewModel
import com.hematcount.app.model.Calculations.fmt1
import com.hematcount.app.model.Calculations.fmt2
import com.hematcount.app.model.CellType
import com.hematcount.app.model.CountResults
import com.hematcount.app.model.Flag
import com.hematcount.app.model.MorphologyParam
import com.hematcount.app.model.PlateletEstimate
import com.hematcount.app.model.gradeLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Adult total leucocyte count reference interval (Dacie & Lewis, 12th ed.). */
const val TLC_REF_LOW = 4.0
const val TLC_REF_HIGH = 10.0
const val TLC_REF_TEXT = "4.0 – 10.0"

data class ReportData(
    val institution: String,
    val department: String,
    val address: String,
    val reportedBy: String,
    val sampleId: String,
    val patientName: String,
    val age: String,
    val sex: String,
    val referredBy: String,
    val ward: String,
    val remarks: String,
    val target: Int,
    val results: CountResults,
    val morphology: Map<MorphologyParam, Int>,
    val platelet: PlateletEstimate,
    val date: Date,
    val aiText: String = "",
    val aiProvider: String = "",
    /** true = aiText holds only the further-investigation suggestions. */
    val aiInvestigationsOnly: Boolean = false,
    /** Non-blood modes (marrow, fluid, reticulocytes…). Null = peripheral blood differential. */
    val modeReport: com.hematcount.app.model.ModeReport? = null,
    val patientPhone: String = "",
) {
    val reportNo: String get() =
        "HC-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(date)
    val dateText: String get() =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(date)
    val fileName: String get() {
        val id = sampleId.ifBlank { "Sample" }.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        return "HematCount_${id}_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(date) + ".pdf"
    }

    /** Automatically generated alerts for the report and results screen. */
    val alerts: List<String> get() {
        modeReport?.let { return it.alerts }
        val r = results
        val out = mutableListOf<String>()
        if (r.wbcCounted == 0) return out
        fun pct(c: CellType) = r.rows.first { it.cell == c }.percent
        if (pct(CellType.BLAST) > 0) out += "Blasts seen (${fmt1(pct(CellType.BLAST))}%) — urgent pathologist review recommended."
        if (pct(CellType.PROMYELOCYTE) > 0) out += "Promyelocytes seen (${fmt1(pct(CellType.PROMYELOCYTE))}%) — correlate clinically; exclude acute promyelocytic leukaemia if prominent."
        val immature = pct(CellType.METAMYELOCYTE) + pct(CellType.MYELOCYTE) + pct(CellType.PROMYELOCYTE)
        if (immature > 0 && r.nrbcCount > 0) out += "Immature granulocytes with nucleated red cells — leucoerythroblastic picture."
        else if (immature > 0) out += "Immature granulocytes present (${fmt1(immature)}%) — left shift."
        if (pct(CellType.ATYPICAL_LYMPHOCYTE) > 0) out += "Atypical (reactive) lymphocytes seen (${fmt1(pct(CellType.ATYPICAL_LYMPHOCYTE))}%)."
        if (r.correctionApplied) out += "Nucleated red cells ${fmt1(r.nrbcPer100)} / 100 leucocytes — total leucocyte count corrected."
        if (target != 0 && r.wbcCounted < target) out += "Differential based on ${r.wbcCounted} cells (target ${target})."
        return out
    }

    fun tlcFlag(v: Double?): Flag = when {
        v == null -> Flag.NONE
        v > TLC_REF_HIGH -> Flag.HIGH
        v < TLC_REF_LOW -> Flag.LOW
        else -> Flag.NONE
    }

    companion object {
        /** Rebuilds a report from a saved history entry (letterhead comes from current settings). */
        fun fromRecord(r: com.hematcount.app.history.ReportRecord, vm: CounterViewModel) = ReportData(
            institution = vm.institution.trim(), department = vm.department.trim(), address = vm.address.trim(),
            reportedBy = r.reportedBy.ifBlank { vm.reportedBy }.trim(),
            sampleId = r.sampleId, patientName = r.patientName, age = r.age, sex = r.sex,
            referredBy = r.referredBy, ward = r.ward, remarks = r.remarks, target = r.target,
            results = com.hematcount.app.model.Calculations.compute(r.counts, r.wbcText, r.applyNrbcCorrection),
            morphology = MorphologyParam.entries.associateWith { r.morphology[it] ?: 0 },
            platelet = r.platelet, date = Date(r.updatedAt),
            aiText = when (r.aiPdfMode) {
                com.hematcount.app.ai.AiPdfMode.NONE -> ""
                com.hematcount.app.ai.AiPdfMode.FULL -> r.aiResult.trim()
                com.hematcount.app.ai.AiPdfMode.INVESTIGATIONS ->
                    com.hematcount.app.ai.extractInvestigations(r.aiResult).ifBlank { r.aiResult.trim() }
            },
            aiProvider = r.aiProviderUsed,
            aiInvestigationsOnly = r.aiPdfMode == com.hematcount.app.ai.AiPdfMode.INVESTIGATIONS &&
                com.hematcount.app.ai.extractInvestigations(r.aiResult).isNotBlank(),
            modeReport = recordModeReport(r),
            patientPhone = r.phone,
        )

        /** Rebuilds the mode-specific results of a saved record. */
        fun recordModeReport(r: com.hematcount.app.history.ReportRecord): com.hematcount.app.model.ModeReport? {
            val calc = com.hematcount.app.model.ModeCalc
            fun c(k: String) = r.tallyCounts[k] ?: emptyMap()
            val i = r.modeInputs
            return when (r.mode) {
                com.hematcount.app.model.CounterMode.BLOOD -> null
                com.hematcount.app.model.CounterMode.MARROW -> calc.marrow(c("MARROW"), i, r.target)
                com.hematcount.app.model.CounterMode.FLUID -> calc.fluid(c("FLUID_CHAMBER"), c("FLUID_DIFF"), i, 0)
                com.hematcount.app.model.CounterMode.RETIC -> calc.retic(c("RETIC"), i, 0)
                com.hematcount.app.model.CounterMode.RBC_MORPH -> calc.rbcMorph(c("RBC_MORPH"), 0)
                com.hematcount.app.model.CounterMode.PLATELET -> calc.platelet(c("PLATELET"), i)
                com.hematcount.app.model.CounterMode.VIABILITY -> calc.viability(c("VIABILITY"), i)
                com.hematcount.app.model.CounterMode.CUSTOM -> calc.custom(
                    com.hematcount.app.decodeCustom(r.customJson.ifBlank { null }) ?: emptyList(), c("CUSTOM"), i, 0)
            }
        }

        fun from(vm: CounterViewModel) = ReportData(
            institution = vm.institution.trim(),
            department = vm.department.trim(),
            address = vm.address.trim(),
            reportedBy = vm.reportedBy.trim(),
            sampleId = vm.sampleId.trim(),
            patientName = vm.patientName.trim(),
            age = vm.patientAge.trim(),
            sex = vm.patientSex.trim(),
            referredBy = vm.referredBy.trim(),
            ward = vm.ward.trim(),
            remarks = vm.remarks.trim(),
            target = vm.target,
            results = vm.results(),
            morphology = MorphologyParam.entries.associateWith { vm.morphology[it] ?: 0 },
            platelet = vm.plateletEstimate,
            date = Date(),
            aiText = when (vm.aiPdfMode) {
                com.hematcount.app.ai.AiPdfMode.NONE -> ""
                com.hematcount.app.ai.AiPdfMode.FULL -> vm.aiResult.trim()
                com.hematcount.app.ai.AiPdfMode.INVESTIGATIONS ->
                    com.hematcount.app.ai.extractInvestigations(vm.aiResult).ifBlank { vm.aiResult.trim() }
            },
            aiProvider = vm.aiProviderUsed,
            aiInvestigationsOnly = vm.aiPdfMode == com.hematcount.app.ai.AiPdfMode.INVESTIGATIONS &&
                com.hematcount.app.ai.extractInvestigations(vm.aiResult).isNotBlank(),
            modeReport = vm.modeReport(),
            patientPhone = vm.patientPhone,
        )
    }
}

/** Plain-text summary for clipboard, LIS entry and WhatsApp messages. */
object ReportText {
    /** Text summary for marrow, fluid, reticulocyte, red-cell, platelet, viability and custom counts. */
    fun generic(d: ReportData, m: com.hematcount.app.model.ModeReport, forAi: Boolean): String = buildString {
        if (forAi) {
            appendLine("Specimen / test: ${m.title} (${m.specimen})")
            appendLine("Age: ${d.age.ifBlank { "not stated" }}; Sex: ${d.sex.ifBlank { "not stated" }}")
        } else {
            appendLine("HEMATCOUNT — ${m.title.uppercase()}")
            if (d.institution.isNotBlank()) appendLine(d.institution)
            appendLine("Report No: ${d.reportNo}   Date: ${d.dateText}")
            if (d.sampleId.isNotBlank()) appendLine("Sample ID: ${d.sampleId}")
            if (d.patientName.isNotBlank()) appendLine("Patient: ${d.patientName}" +
                listOf(d.age, d.sex).filter { it.isNotBlank() }.joinToString(" / ").let { if (it.isBlank()) "" else " ($it)" })
            if (d.referredBy.isNotBlank()) appendLine("Referred by: ${d.referredBy}")
            appendLine("Specimen: ${m.specimen}")
        }
        m.setup.filter { it.second != "—" }.forEach { appendLine("${it.first}: ${it.second}") }
        appendLine()
        appendLine("${m.totalLabel}: ${m.total}")
        fun rowLine(r: com.hematcount.app.model.ReportRow) = buildString {
            append("• ${r.name}: ${r.count}")
            r.percent?.let { append(" · ${fmt1(it)}%") }
            r.extra?.let { append(" · $it") }
            if (r.normal.isNotBlank()) append("  [normal ${r.normal}]")
            if (r.flag != Flag.NONE) append("  ${r.flag.label}")
        }
        m.rows.forEach { appendLine(rowLine(it)) }
        if (m.rows2.isNotEmpty()) {
            appendLine(); appendLine(m.rows2Title ?: "Differential")
            m.rows2.forEach { appendLine(rowLine(it)) }
        }
        appendLine()
        m.indices.forEach { appendLine("${it.label}: ${it.value}" + (if (it.flag != Flag.NONE) "  ${it.flag.label}" else "")) }
        if (m.alerts.isNotEmpty()) { appendLine(); appendLine("Alerts:"); m.alerts.forEach { appendLine("• $it") } }
        if (d.remarks.isNotBlank()) { appendLine(); appendLine((if (forAi) "Reporter's remarks: " else "Impression: ") + d.remarks) }
        if (!forAi && d.reportedBy.isNotBlank()) { appendLine(); appendLine("Reported by: ${d.reportedBy}") }
    }.trimEnd()

    /** De-identified version for AI analysis: no name, sample ID, clinician or ward. */
    fun buildForAi(d: ReportData): String = buildString {
        d.modeReport?.let { return generic(d, it, forAi = true) }
        val r = d.results
        appendLine("Age: ${d.age.ifBlank { "not stated" }}; Sex: ${d.sex.ifBlank { "not stated" }}")
        appendLine("Leucocytes counted: ${r.wbcCounted}")
        r.analyserWbc?.let { appendLine("Total leucocyte count (analyser): ${fmt2(it)} x10^3/uL (ref $TLC_REF_TEXT)") }
        if (r.correctionApplied && r.correctedWbc != null) appendLine("Corrected leucocyte count: ${fmt2(r.correctedWbc)} x10^3/uL")
        appendLine("Differential (count, %, absolute x10^3/uL, reference %, reference absolute, flag):")
        r.rows.forEach { row ->
            appendLine("- ${row.cell.displayName}: ${row.count}, ${fmt1(row.percent)}%, " +
                "${row.absolute?.let { fmt2(it) } ?: "n/a"}, ref ${row.cell.refPercent}%, ref ${row.cell.refAbsolute}" +
                if (row.flag != Flag.NONE) ", ${row.flag.name}" else "")
        }
        appendLine("- Nucleated red cells: ${r.nrbcCount} (${fmt1(r.nrbcPer100)} per 100 leucocytes)")
        appendLine("Red cell & platelet morphology:")
        d.morphology.forEach { (k, v) -> appendLine("- ${k.displayName}: ${gradeLabel(v)}") }
        appendLine("- Platelets on smear: ${d.platelet.displayName}")
        if (d.remarks.isNotBlank()) appendLine("Reporter's remarks: ${d.remarks}")
    }.trimEnd()

    fun build(d: ReportData): String = buildString {
        d.modeReport?.let { return generic(d, it, forAi = false) }
        val r = d.results
        appendLine("HEMATCOUNT — MANUAL DIFFERENTIAL LEUCOCYTE COUNT")
        if (d.institution.isNotBlank()) appendLine(d.institution)
        appendLine("Report No: ${d.reportNo}   Date: ${d.dateText}")
        if (d.sampleId.isNotBlank()) appendLine("Sample ID: ${d.sampleId}")
        if (d.patientName.isNotBlank()) appendLine("Patient: ${d.patientName}" +
            listOf(d.age, d.sex).filter { it.isNotBlank() }.joinToString(" / ").let { if (it.isBlank()) "" else " ($it)" })
        if (d.referredBy.isNotBlank()) appendLine("Referred by: ${d.referredBy}")
        if (d.ward.isNotBlank()) appendLine("Ward / OPD: ${d.ward}")
        appendLine()
        appendLine("Leucocytes counted: ${r.wbcCounted}")
        appendLine("Differential (count · % · absolute ×10³/µL) [reference %]")
        r.rows.forEach { row ->
            val abs = row.absolute?.let { " · ${fmt2(it)}" } ?: ""
            val flag = if (row.flag != Flag.NONE) "  ${row.flag.label}" else ""
            appendLine("• ${row.cell.displayName}: ${row.count} · ${fmt1(row.percent)}%$abs  [${row.cell.refPercent}]$flag")
        }
        appendLine("• Nucleated Red Cells: ${r.nrbcCount} (${fmt1(r.nrbcPer100)} / 100 leucocytes)")
        appendLine()
        r.analyserWbc?.let { appendLine("Total leucocyte count (analyser): ${fmt2(it)} ×10³/µL  [$TLC_REF_TEXT]") }
        if (r.correctionApplied && r.correctedWbc != null)
            appendLine("Corrected leucocyte count: ${fmt2(r.correctedWbc)} ×10³/µL")
        val graded = d.morphology.filterValues { it > 0 }
        appendLine()
        appendLine("Red cell & platelet morphology:")
        if (graded.isEmpty()) appendLine("• No abnormality graded")
        else graded.forEach { (k, v) -> appendLine("• ${k.displayName}: ${gradeLabel(v)}") }
        if (d.platelet != PlateletEstimate.NOT_ASSESSED) appendLine("• Platelets on smear: ${d.platelet.displayName}")
        if (d.alerts.isNotEmpty()) {
            appendLine(); appendLine("Alerts:")
            d.alerts.forEach { appendLine("• $it") }
        }
        if (d.remarks.isNotBlank()) { appendLine(); appendLine("Impression: ${d.remarks}") }
        if (d.reportedBy.isNotBlank()) { appendLine(); appendLine("Reported by: ${d.reportedBy}") }
    }.trimEnd()
}
