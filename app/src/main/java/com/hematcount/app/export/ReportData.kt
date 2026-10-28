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
        )
    }
}

/** Plain-text summary for clipboard, LIS entry and WhatsApp messages. */
object ReportText {
    /** De-identified version for AI analysis: no name, sample ID, clinician or ward. */
    fun buildForAi(d: ReportData): String = buildString {
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
