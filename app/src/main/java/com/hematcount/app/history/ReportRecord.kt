package com.hematcount.app.history

import android.content.Context
import com.hematcount.app.ai.AiPdfMode
import com.hematcount.app.model.CellType
import com.hematcount.app.model.CounterMode
import com.hematcount.app.model.MorphologyParam
import com.hematcount.app.model.PlateletEstimate
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** A complete snapshot of one sample, kept in the on-device report history. */
data class ReportRecord(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sampleId: String = "",
    val patientName: String = "",
    val age: String = "",
    val sex: String = "",
    val phone: String = "",
    val referredBy: String = "",
    val ward: String = "",
    val remarks: String = "",
    val reportedBy: String = "",
    val wbcText: String = "",
    val applyNrbcCorrection: Boolean = true,
    val target: Int = 100,
    val counts: Map<CellType, Int> = emptyMap(),
    val morphology: Map<MorphologyParam, Int> = emptyMap(),
    val platelet: PlateletEstimate = PlateletEstimate.NOT_ASSESSED,
    val aiResult: String = "",
    val aiProviderUsed: String = "",
    val aiPdfMode: AiPdfMode = AiPdfMode.NONE,
    val mode: CounterMode = CounterMode.BLOOD,
    /** Counts of the non-blood keypads, by tally key (MARROW, FLUID_CHAMBER, FLUID_DIFF…). */
    val tallyCounts: Map<String, Map<String, Int>> = emptyMap(),
    val modeInputs: Map<String, String> = emptyMap(),
    /** Custom counter definitions (JSON) when mode = CUSTOM. */
    val customJson: String = "",
) {
    val wbcCounted: Int get() = CellType.wbcTypes.sumOf { counts[it] ?: 0 }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("createdAt", createdAt); put("updatedAt", updatedAt)
        put("sampleId", sampleId); put("patientName", patientName); put("age", age); put("sex", sex); put("phone", phone)
        put("referredBy", referredBy); put("ward", ward); put("remarks", remarks); put("reportedBy", reportedBy)
        put("wbcText", wbcText); put("applyNrbcCorrection", applyNrbcCorrection); put("target", target)
        put("counts", JSONObject().apply { counts.forEach { (k, v) -> put(k.name, v) } })
        put("morphology", JSONObject().apply { morphology.forEach { (k, v) -> put(k.name, v) } })
        put("platelet", platelet.name)
        put("aiResult", aiResult); put("aiProviderUsed", aiProviderUsed); put("aiPdfMode", aiPdfMode.name)
        put("mode", mode.name)
        put("tallies", JSONObject().apply {
            tallyCounts.forEach { (k, m) -> put(k, JSONObject().apply { m.forEach { (id, v) -> put(id, v) } }) }
        })
        put("inputs", JSONObject(modeInputs as Map<*, *>))
        put("customJson", customJson)
    }

    companion object {
        fun fromJson(o: JSONObject): ReportRecord {
            val counts = mutableMapOf<CellType, Int>()
            o.optJSONObject("counts")?.let { c ->
                c.keys().forEach { k -> runCatching { CellType.valueOf(k) }.getOrNull()?.let { counts[it] = c.optInt(k) } }
            }
            val morph = mutableMapOf<MorphologyParam, Int>()
            o.optJSONObject("morphology")?.let { m ->
                m.keys().forEach { k -> runCatching { MorphologyParam.valueOf(k) }.getOrNull()?.let { morph[it] = m.optInt(k) } }
            }
            return ReportRecord(
                id = o.optString("id", UUID.randomUUID().toString()),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", o.optLong("createdAt", System.currentTimeMillis())),
                sampleId = o.optString("sampleId"), patientName = o.optString("patientName"),
                age = o.optString("age"), sex = o.optString("sex"), phone = o.optString("phone"), referredBy = o.optString("referredBy"),
                ward = o.optString("ward"), remarks = o.optString("remarks"), reportedBy = o.optString("reportedBy"),
                wbcText = o.optString("wbcText"), applyNrbcCorrection = o.optBoolean("applyNrbcCorrection", true),
                target = o.optInt("target", 100), counts = counts, morphology = morph,
                platelet = runCatching { PlateletEstimate.valueOf(o.optString("platelet")) }.getOrDefault(PlateletEstimate.NOT_ASSESSED),
                aiResult = o.optString("aiResult"), aiProviderUsed = o.optString("aiProviderUsed"),
                aiPdfMode = runCatching { AiPdfMode.valueOf(o.optString("aiPdfMode")) }.getOrDefault(AiPdfMode.NONE),
                mode = runCatching { CounterMode.valueOf(o.optString("mode", "BLOOD")) }.getOrDefault(CounterMode.BLOOD),
                tallyCounts = o.optJSONObject("tallies")?.let { t ->
                    t.keys().asSequence().associateWith { k ->
                        val m = t.getJSONObject(k); m.keys().asSequence().associateWith { id -> m.optInt(id) }
                    }
                } ?: emptyMap(),
                modeInputs = o.optJSONObject("inputs")?.let { i -> i.keys().asSequence().associateWith { k -> i.optString(k) } } ?: emptyMap(),
                customJson = o.optString("customJson", ""),
            )
        }
    }
}

/** History is kept as one JSON file in the app's private storage (newest first). */
object HistoryStore {
    private fun file(context: Context) = File(context.filesDir, "report_history.json")

    fun load(context: Context): List<ReportRecord> = runCatching {
        val f = file(context)
        if (!f.exists()) return emptyList()
        val a = JSONArray(f.readText())
        (0 until a.length()).map { ReportRecord.fromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList()).sortedByDescending { it.updatedAt }

    fun save(context: Context, records: List<ReportRecord>) {
        val a = JSONArray().apply { records.forEach { put(it.toJson()) } }
        val f = file(context)
        val tmp = File(f.parentFile, f.name + ".tmp")
        tmp.writeText(a.toString())
        if (!tmp.renameTo(f)) { f.writeText(a.toString()); tmp.delete() }
    }
}
