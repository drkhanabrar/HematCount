package com.hematcount.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hematcount.app.ai.AiAttempt
import com.hematcount.app.ai.AiPdfMode
import com.hematcount.app.ai.AiProvider
import com.hematcount.app.ai.AiService
import com.hematcount.app.export.ReportData
import com.hematcount.app.export.ReportText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import com.hematcount.app.ai.DEFAULT_AI_MODEL
import com.hematcount.app.ai.OpenRouterClient
import com.hematcount.app.export.ReportData
import kotlinx.coroutines.launch
import com.hematcount.app.model.Calculations
import com.hematcount.app.model.CellType
import com.hematcount.app.model.CountResults
import com.hematcount.app.model.MorphologyParam
import com.hematcount.app.model.PlateletEstimate
import com.hematcount.app.ui.theme.ThemeMode

/** Target choices. 0 = continuous (no limit). */
val TARGET_OPTIONS = listOf(50, 100, 200, 500, 0)

fun targetLabel(t: Int) = if (t == 0) "Continuous" else "$t cells"

enum class TapResult { ADDED, REMOVED, NOTHING_TO_REMOVE, LOCKED, TARGET_REACHED }

class CounterViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("hematcount", Context.MODE_PRIVATE)

    // ---- Counting state ----
    val counts = mutableStateMapOf<CellType, Int>()
    private val history = mutableStateListOf<Pair<CellType, Int>>() // (cell, +1 / -1)

    var target by mutableStateOf(100); private set
    var correctionMode by mutableStateOf(false)
    var locked by mutableStateOf(false); private set
    var showTargetDialog by mutableStateOf(false)
    var resultsTab by mutableStateOf(0)

    // ---- Sample / patient details ----
    var sampleId by mutableStateOf("")
    var patientName by mutableStateOf("")
    var patientAge by mutableStateOf("")
    var patientSex by mutableStateOf("")
    var referredBy by mutableStateOf("")
    var ward by mutableStateOf("")
    var remarks by mutableStateOf("")
    var wbcText by mutableStateOf("")
    var applyNrbcCorrection by mutableStateOf(true)

    // ---- RBC & platelet morphology ----
    val morphology = mutableStateMapOf<MorphologyParam, Int>()
    var plateletEstimate by mutableStateOf(PlateletEstimate.NOT_ASSESSED)

    // ---- Settings ----
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var dynamicColor by mutableStateOf(false)
    var hapticsOn by mutableStateOf(true)
    var tapSoundOn by mutableStateOf(true)
    var alarmOn by mutableStateOf(true)
    var institution by mutableStateOf("")
    var department by mutableStateOf("")
    var address by mutableStateOf("")
    var reportedBy by mutableStateOf("")

    // ---- AI analysis (OpenRouter) ----
    var aiKey by mutableStateOf("")
    var aiModel by mutableStateOf(DEFAULT_AI_MODEL)
    var aiIncludeAgeSex by mutableStateOf(true)
    var aiResult by mutableStateOf("")
    var aiError by mutableStateOf<String?>(null)
    var aiLoading by mutableStateOf(false); private set

    // ---- AI interpretation ----
    val aiProviders = mutableStateListOf<AiProvider>()
    var aiResult by mutableStateOf("")
    var aiProviderUsed by mutableStateOf("")
    var aiBusy by mutableStateOf(false); private set
    var aiTrying by mutableStateOf("")
    val aiAttempts = mutableStateListOf<AiAttempt>()
    var aiPdfMode by mutableStateOf(AiPdfMode.NONE)

    init { load() }

    fun runAiAnalysis() {
        if (aiBusy) return
        aiBusy = true
        aiAttempts.clear()
        val providers = aiProviders.toList()
        val text = ReportText.buildForAi(ReportData.from(this))
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                AiService.analyse(providers, text) { name -> viewModelScope.launch { aiTrying = name } }
            }
            aiAttempts.addAll(outcome.attempts)
            if (outcome.text != null) {
                aiResult = outcome.text
                aiProviderUsed = outcome.providerUsed ?: ""
            }
            aiTrying = ""
            aiBusy = false
            save()
        }
    }

    fun clearAi() { aiResult = ""; aiProviderUsed = ""; aiAttempts.clear(); aiPdfMode = AiPdfMode.NONE; save() }

    // provider list editing (order = failover order)
    fun upsertProvider(p: AiProvider) {
        val i = aiProviders.indexOfFirst { it.id == p.id }
        if (i >= 0) aiProviders[i] = p else aiProviders.add(p)
        save()
    }
    fun removeProvider(p: AiProvider) { aiProviders.removeAll { it.id == p.id }; save() }
    fun moveProvider(index: Int, delta: Int) {
        val j = index + delta
        if (index !in aiProviders.indices || j !in aiProviders.indices) return
        val item = aiProviders.removeAt(index)
        aiProviders.add(j, item)
        save()
    }
    fun toggleProvider(p: AiProvider) = upsertProvider(p.copy(enabled = !p.enabled))

    fun runAiAnalysis() {
        if (aiLoading) return
        if (aiKey.isBlank()) { aiError = "Add your OpenRouter API key first."; return }
        if (wbcCounted == 0) { aiError = "Count some leucocytes before requesting an analysis."; return }
        val summary = OpenRouterClient.buildAnonymisedSummary(ReportData.from(this), aiIncludeAgeSex)
        aiLoading = true; aiError = null
        viewModelScope.launch {
            try {
                aiResult = OpenRouterClient.analyse(aiKey, aiModel, summary)
                save()
            } catch (e: Exception) {
                aiError = e.message ?: "Could not reach OpenRouter. Check your internet connection."
            } finally {
                aiLoading = false
            }
        }
    }

    // ---------------------------------------------------------------- counting
    val wbcCounted: Int get() = CellType.wbcTypes.sumOf { counts[it] ?: 0 }
    val nrbcCount: Int get() = counts[CellType.NRBC] ?: 0
    val canUndo: Boolean get() = history.isNotEmpty()

    fun results(): CountResults = Calculations.compute(counts, wbcText, applyNrbcCorrection)

    fun tap(cell: CellType): TapResult {
        if (correctionMode) {
            val c = counts[cell] ?: 0
            if (c == 0) return TapResult.NOTHING_TO_REMOVE
            counts[cell] = c - 1
            history.add(cell to -1)
            if (locked && target != 0 && wbcCounted < target) locked = false
            save(); return TapResult.REMOVED
        }
        if (locked) return TapResult.LOCKED
        counts[cell] = (counts[cell] ?: 0) + 1
        history.add(cell to 1)
        trimHistory()
        save()
        if (cell.countsInWbc && target != 0 && wbcCounted >= target) {
            locked = true
            showTargetDialog = true
            return TapResult.TARGET_REACHED
        }
        return TapResult.ADDED
    }

    fun undo(): CellType? {
        val last = history.removeLastOrNull() ?: return null
        val (cell, delta) = last
        counts[cell] = ((counts[cell] ?: 0) - delta).coerceAtLeast(0)
        if (locked && target != 0 && wbcCounted < target) locked = false
        save()
        return cell
    }

    /** Continue past the target: raise to the next option, or go continuous. */
    fun continueCounting(nextTarget: Int) {
        target = nextTarget
        locked = target != 0 && wbcCounted >= target
        showTargetDialog = false
        save()
    }

    fun setTargetValue(t: Int) {
        target = t
        locked = t != 0 && wbcCounted >= t
        save()
    }

    fun unlock() { locked = false; showTargetDialog = false; target = 0; save() }

    fun newSample() {
        counts.clear(); history.clear(); morphology.clear()
        plateletEstimate = PlateletEstimate.NOT_ASSESSED
        locked = false; correctionMode = false; showTargetDialog = false
        sampleId = ""; patientName = ""; patientAge = ""; patientSex = ""
        referredBy = ""; ward = ""; remarks = ""; wbcText = ""
        aiResult = ""; aiError = null
        save()
    }

    fun setGrade(p: MorphologyParam, g: Int) { morphology[p] = g; save() }
    fun setPlatelet(p: PlateletEstimate) { plateletEstimate = p; save() }

    private fun trimHistory() { while (history.size > 2000) history.removeAt(0) }

    // ---------------------------------------------------------------- persistence
    fun save() {
        prefs.edit().apply {
            putString("counts", CellType.entries.joinToString(",") { "${it.name}:${counts[it] ?: 0}" })
            putString("history", history.takeLast(2000).joinToString(",") { "${it.first.name}:${it.second}" })
            putInt("target", target)
            putBoolean("locked", locked)
            putString("sampleId", sampleId); putString("patientName", patientName)
            putString("patientAge", patientAge); putString("patientSex", patientSex)
            putString("referredBy", referredBy); putString("ward", ward)
            putString("remarks", remarks); putString("wbcText", wbcText)
            putBoolean("applyNrbcCorrection", applyNrbcCorrection)
            putString("morph", MorphologyParam.entries.joinToString(",") { "${it.name}:${morphology[it] ?: 0}" })
            putString("platelet", plateletEstimate.name)
            putString("themeMode", themeMode.name)
            putBoolean("dynamicColor", dynamicColor)
            putBoolean("haptics", hapticsOn); putBoolean("tapSound", tapSoundOn); putBoolean("alarm", alarmOn)
            putString("institution", institution); putString("department", department)
            putString("address", address); putString("reportedBy", reportedBy)
            putString("aiProviders", AiProvider.listToJson(aiProviders))
            putString("aiResult", aiResult); putString("aiProviderUsed", aiProviderUsed)
            putString("aiPdfMode", aiPdfMode.name)
            putString("aiKey", aiKey); putString("aiModel", aiModel)
            putBoolean("aiAgeSex", aiIncludeAgeSex); putString("aiResult", aiResult)
        }.apply()
    }

    private fun load() {
        prefs.getString("counts", null)?.split(",")?.forEach { e ->
            val p = e.split(":"); if (p.size == 2) {
                val cell = runCatching { CellType.valueOf(p[0]) }.getOrNull()
                val v = p[1].toIntOrNull()
                if (cell != null && v != null && v > 0) counts[cell] = v
            }
        }
        prefs.getString("history", null)?.takeIf { it.isNotBlank() }?.split(",")?.forEach { e ->
            val p = e.split(":"); if (p.size == 2) {
                val cell = runCatching { CellType.valueOf(p[0]) }.getOrNull()
                val d = p[1].toIntOrNull()
                if (cell != null && d != null) history.add(cell to d)
            }
        }
        target = prefs.getInt("target", 100)
        locked = prefs.getBoolean("locked", false)
        sampleId = prefs.getString("sampleId", "") ?: ""
        patientName = prefs.getString("patientName", "") ?: ""
        patientAge = prefs.getString("patientAge", "") ?: ""
        patientSex = prefs.getString("patientSex", "") ?: ""
        referredBy = prefs.getString("referredBy", "") ?: ""
        ward = prefs.getString("ward", "") ?: ""
        remarks = prefs.getString("remarks", "") ?: ""
        wbcText = prefs.getString("wbcText", "") ?: ""
        applyNrbcCorrection = prefs.getBoolean("applyNrbcCorrection", true)
        prefs.getString("morph", null)?.split(",")?.forEach { e ->
            val p = e.split(":"); if (p.size == 2) {
                val m = runCatching { MorphologyParam.valueOf(p[0]) }.getOrNull()
                val v = p[1].toIntOrNull()
                if (m != null && v != null && v > 0) morphology[m] = v
            }
        }
        plateletEstimate = runCatching {
            PlateletEstimate.valueOf(prefs.getString("platelet", "NOT_ASSESSED")!!)
        }.getOrDefault(PlateletEstimate.NOT_ASSESSED)
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString("themeMode", "SYSTEM")!!) }
            .getOrDefault(ThemeMode.SYSTEM)
        dynamicColor = prefs.getBoolean("dynamicColor", false)
        hapticsOn = prefs.getBoolean("haptics", true)
        tapSoundOn = prefs.getBoolean("tapSound", true)
        alarmOn = prefs.getBoolean("alarm", true)
        institution = prefs.getString("institution", null)
            ?: "B. J. Government Medical College & Sassoon General Hospital"
        department = prefs.getString("department", null)
            ?: "Department of Pathology · Haematology Laboratory"
        address = prefs.getString("address", null) ?: "Pune, Maharashtra, India"
        reportedBy = prefs.getString("reportedBy", "") ?: ""
        aiKey = prefs.getString("aiKey", "") ?: ""
        aiModel = prefs.getString("aiModel", null)?.ifBlank { null } ?: DEFAULT_AI_MODEL
        aiIncludeAgeSex = prefs.getBoolean("aiAgeSex", true)
        aiResult = prefs.getString("aiResult", "") ?: ""
    }
}
