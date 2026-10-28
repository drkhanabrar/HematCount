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
import com.hematcount.app.history.BackupManager
import com.hematcount.app.history.HistoryStore
import com.hematcount.app.history.ReportRecord
import android.net.Uri
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hematcount.app.model.Calculations
import com.hematcount.app.model.CountItem
import com.hematcount.app.model.CounterMode
import com.hematcount.app.model.FluidItems
import com.hematcount.app.model.FluidPreset
import com.hematcount.app.model.MarrowCell
import com.hematcount.app.model.ModeCalc
import com.hematcount.app.model.ModeReport
import com.hematcount.app.model.PlateletItems
import com.hematcount.app.model.RbcItems
import com.hematcount.app.model.RetItems
import com.hematcount.app.model.SimpleItem
import com.hematcount.app.model.ViabilityItems
import com.hematcount.app.model.defaultCustomItems
import org.json.JSONArray
import org.json.JSONObject
import com.hematcount.app.model.CellType
import com.hematcount.app.model.CountResults
import com.hematcount.app.model.MorphologyParam
import com.hematcount.app.model.PlateletEstimate
import com.hematcount.app.ui.theme.ThemeMode

/** Target choices. 0 = continuous (no limit). */
val TARGET_OPTIONS = listOf(50, 100, 200, 300, 500, 1000, 0)

fun targetLabel(t: Int) = if (t == 0) "Continuous" else "$t cells"

enum class TapResult { ADDED, REMOVED, NOTHING_TO_REMOVE, LOCKED, TARGET_REACHED }

class CounterViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("hematcount", Context.MODE_PRIVATE)

    // ---- Counting state ----
    val counts = mutableStateMapOf<CellType, Int>()
    private val undoStack = mutableStateListOf<Pair<CellType, Int>>() // (cell, +1 / -1)

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
    var patientPhone by mutableStateOf("")
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
    var countryCode by mutableStateOf("91")

    // ---- AI interpretation ----
    val aiProviders = mutableStateListOf<AiProvider>()
    var aiResult by mutableStateOf("")
    var aiProviderUsed by mutableStateOf("")
    var aiBusy by mutableStateOf(false); private set
    var aiTrying by mutableStateOf("")
    val aiAttempts = mutableStateListOf<AiAttempt>()
    var aiPdfMode by mutableStateOf(AiPdfMode.NONE)

    // ---- Report history & backup ----
    val history = mutableStateListOf<ReportRecord>()
    var currentRecordId by mutableStateOf<String?>(null); private set
    var backupTreeUri by mutableStateOf<String?>(null); private set
    var lastBackupAt by mutableStateOf(0L); private set
    var lastBackupError by mutableStateOf<String?>(null); private set
    var backupIncludeKeys by mutableStateOf(false)

    // ---- Multi-mode counters ----
    var mode by mutableStateOf(CounterMode.BLOOD); private set
    /** Mode-specific settings and sample fields (dilution, preset, haematocrit, cellularity…). */
    val inputs = mutableStateMapOf<String, String>()
    val customItems = mutableStateListOf<SimpleItem>()
    val tallies: Map<String, Tally> = linkedMapOf(
        "MARROW" to Tally("MARROW", 500),
        "FLUID_CHAMBER" to Tally("FLUID_CHAMBER", 0),
        "FLUID_DIFF" to Tally("FLUID_DIFF", 100),
        "RETIC" to Tally("RETIC", 1000),
        "RBC_MORPH" to Tally("RBC_MORPH", 1000),
        "PLATELET" to Tally("PLATELET", 0),
        "VIABILITY" to Tally("VIABILITY", 0),
        "CUSTOM" to Tally("CUSTOM", 100),
    )
    /** Fluid mode: 0 = chamber count, 1 = differential. */
    var fluidStage by mutableStateOf(0)

    // ---- Feedback, flash and messages (shared by touch and hardware keyboard) ----
    val feedback = Feedback(app)
    var flashId by mutableStateOf<String?>(null); private set
    var flashTick by mutableStateOf(0L); private set
    var messageText by mutableStateOf<String?>(null); private set
    var messageAction by mutableStateOf<String?>(null); private set
    var messageTick by mutableStateOf(0L); private set

    init {
        load()
        history.addAll(HistoryStore.load(app))
    }

    // ================================================================= modes
    fun input(key: String, default: String = ""): String = inputs[key] ?: default
    fun setInput(key: String, value: String) { inputs[key] = value; save() }

    val fluidPreset: FluidPreset get() =
        runCatching { FluidPreset.valueOf(inputs["fl.preset"] ?: "CSF") }.getOrDefault(FluidPreset.CSF)

    fun setFluidPreset(p: FluidPreset) {
        inputs["fl.preset"] = p.name
        inputs["fl.dilution"] = p.defaultDilution.toString()
        inputs["fl.squares"] = p.defaultSquares.joinToString(",")
        if (!p.hasDifferential) fluidStage = 0
        save()
    }

    val fluidSquares: List<String> get() =
        inputs["fl.squares"]?.split(",")?.filter { it.isNotBlank() } ?: fluidPreset.defaultSquares

    fun toggleSquare(id: String) {
        val cur = fluidSquares.toMutableList()
        if (id in cur) cur.remove(id) else cur.add(id)
        inputs["fl.squares"] = cur.joinToString(","); save()
    }

    fun switchMode(m: CounterMode) {
        if (mode == m) return
        mode = m; correctionMode = false; showTargetDialog = false
        save()
    }

    private fun activeTallyKey(): String? = when (mode) {
        CounterMode.BLOOD -> null
        CounterMode.FLUID -> if (fluidStage == 1 && fluidPreset.hasDifferential) "FLUID_DIFF" else "FLUID_CHAMBER"
        else -> mode.name
    }

    fun activeTally(): Tally? = activeTallyKey()?.let { tallies[it] }

    fun itemsFor(m: CounterMode, stage: Int = fluidStage): List<CountItem> = when (m) {
        CounterMode.BLOOD -> CellType.keypadOrder
        CounterMode.MARROW -> MarrowCell.keypadOrder
        CounterMode.FLUID -> if (stage == 1 && fluidPreset.hasDifferential) FluidItems.differentialItems(fluidPreset)
            else FluidItems.chamberItems(fluidPreset)
        CounterMode.RETIC -> RetItems.items(inputs["rt.miller"] == "1")
        CounterMode.RBC_MORPH -> RbcItems.items
        CounterMode.PLATELET -> PlateletItems.items
        CounterMode.VIABILITY -> ViabilityItems.items
        CounterMode.CUSTOM -> customItems.toList()
    }

    fun activeItems(): List<CountItem> = itemsFor(mode)

    fun countOf(item: CountItem): Int =
        if (item is CellType) counts[item] ?: 0 else activeTally()?.count(item.id) ?: 0

    val currentTotal: Int get() = activeTally()?.total(activeItems()) ?: wbcCounted
    val currentTarget: Int get() = activeTally()?.target ?: target
    val currentLocked: Boolean get() = activeTally()?.locked ?: locked
    val currentCanUndo: Boolean get() = activeTally()?.canUndo ?: canUndo

    fun setCurrentTarget(t: Int) {
        val tl = activeTally()
        if (tl == null) setTargetValue(t) else { tl.setTarget(t, activeItems()); save() }
    }

    fun continueCurrent(next: Int) {
        val tl = activeTally()
        if (tl == null) continueCounting(next) else { tl.setTarget(next, activeItems()); showTargetDialog = false; save() }
    }

    private fun flash(id: String) { flashId = id; flashTick = System.nanoTime() }

    fun showMessage(text: String, action: String? = null) {
        messageText = text; messageAction = action; messageTick = System.nanoTime()
    }

    /** One count from the keypad or a hardware key. */
    fun press(item: CountItem, fromKeyboard: Boolean = false): TapResult {
        val result = if (item is CellType) tap(item) else {
            val tl = activeTally() ?: return TapResult.NOTHING_TO_REMOVE
            tl.tap(item, activeItems(), correctionMode).also {
                if (it == TapResult.TARGET_REACHED) showTargetDialog = true
                save()
            }
        }
        when (result) {
            TapResult.ADDED -> feedback.tap(hapticsOn, tapSoundOn)
            TapResult.REMOVED -> feedback.correction(hapticsOn, tapSoundOn)
            TapResult.NOTHING_TO_REMOVE -> { feedback.denied(hapticsOn); showMessage("${item.displayName}: nothing to subtract") }
            TapResult.LOCKED -> { feedback.denied(hapticsOn); showMessage("Target reached — keypad locked", "Continue") }
            TapResult.TARGET_REACHED -> feedback.targetReached(hapticsOn, alarmOn)
        }
        if (fromKeyboard && (result == TapResult.ADDED || result == TapResult.REMOVED || result == TapResult.TARGET_REACHED)) flash(item.id)
        return result
    }

    /** Undo the last change in the current mode and flash that counter. */
    fun undoActive() {
        val tl = activeTally()
        val id = if (tl == null) undo()?.name else tl.undo(activeItems()).also { save() }
        if (id != null) { feedback.correction(hapticsOn, false); flash(id) }
    }

    /** Hardware keyboard: 1–9, 0 then Q…P, A…L map to keypad positions in reading order. */
    fun hardwareKey(index: Int): Boolean {
        val items = activeItems()
        if (index !in items.indices) return false
        press(items[index], fromKeyboard = true)
        return true
    }

    fun modeReport(m: CounterMode = mode): ModeReport? {
        fun c(k: String) = tallies.getValue(k).counts.toMap()
        fun t(k: String) = tallies.getValue(k).target
        return when (m) {
            CounterMode.BLOOD -> null
            CounterMode.MARROW -> ModeCalc.marrow(c("MARROW"), inputs.toMap(), t("MARROW"))
            CounterMode.FLUID -> ModeCalc.fluid(c("FLUID_CHAMBER"), c("FLUID_DIFF"), inputs.toMap(), t("FLUID_DIFF"))
            CounterMode.RETIC -> ModeCalc.retic(c("RETIC"), inputs.toMap(), t("RETIC"))
            CounterMode.RBC_MORPH -> ModeCalc.rbcMorph(c("RBC_MORPH"), t("RBC_MORPH"))
            CounterMode.PLATELET -> ModeCalc.platelet(c("PLATELET"), inputs.toMap())
            CounterMode.VIABILITY -> ModeCalc.viability(c("VIABILITY"), inputs.toMap())
            CounterMode.CUSTOM -> ModeCalc.custom(customItems.toList(), c("CUSTOM"), inputs.toMap(), t("CUSTOM"))
        }
    }

    // custom counters editing
    fun upsertCustom(item: SimpleItem) {
        val i = customItems.indexOfFirst { it.id == item.id }
        if (i >= 0) customItems[i] = item else customItems.add(item)
        save()
    }
    fun removeCustom(item: SimpleItem) { customItems.removeAll { it.id == item.id }; tallies.getValue("CUSTOM").counts.remove(item.id); save() }
    fun moveCustom(index: Int, delta: Int) {
        val j = index + delta
        if (index !in customItems.indices || j !in customItems.indices) return
        val it = customItems.removeAt(index); customItems.add(j, it); save()
    }

    /** Per-sample fields cleared by "New sample" (setup such as dilution and presets is kept). */
    private val perSampleInputs = listOf("bm.site", "bm.particles", "bm.cellularity", "bm.mega", "bm.iron",
        "fl.appearance", "fl.volume", "fl.bloodWbc", "fl.bloodRbc", "rt.hct", "rt.rbc")

    private val ctx get() = getApplication<Application>()

    /** Saves the current sample into history (updates the same entry on repeated saves). */
    fun saveToHistory(): ReportRecord {
        val now = System.currentTimeMillis()
        val existing = history.firstOrNull { it.id == currentRecordId }
        val rec = ReportRecord(
            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
            createdAt = existing?.createdAt ?: now, updatedAt = now,
            sampleId = sampleId, patientName = patientName, age = patientAge, sex = patientSex, phone = patientPhone,
            referredBy = referredBy, ward = ward, remarks = remarks, reportedBy = reportedBy,
            wbcText = wbcText, applyNrbcCorrection = applyNrbcCorrection, target = target,
            counts = counts.toMap().filterValues { it > 0 }, morphology = morphology.toMap().filterValues { it > 0 },
            platelet = plateletEstimate, aiResult = aiResult, aiProviderUsed = aiProviderUsed, aiPdfMode = aiPdfMode,
            mode = mode,
            tallyCounts = tallies.mapValues { (_, t) -> t.counts.toMap().filterValues { it > 0 } }.filterValues { it.isNotEmpty() },
            modeInputs = inputs.toMap(),
            customJson = if (mode == CounterMode.CUSTOM) encodeCustom(customItems) else "",
        )
        history.removeAll { it.id == rec.id }
        history.add(0, rec)
        currentRecordId = rec.id
        persistHistory()
        save()
        return rec
    }

    /** Loads a saved report back into the counter (replaces the current sample). */
    fun openRecord(rec: ReportRecord) {
        counts.clear(); counts.putAll(rec.counts)
        morphology.clear(); morphology.putAll(rec.morphology)
        historyClearUndo()
        sampleId = rec.sampleId; patientName = rec.patientName; patientAge = rec.age; patientSex = rec.sex
        patientPhone = rec.phone
        referredBy = rec.referredBy; ward = rec.ward; remarks = rec.remarks
        if (rec.reportedBy.isNotBlank()) reportedBy = rec.reportedBy
        wbcText = rec.wbcText; applyNrbcCorrection = rec.applyNrbcCorrection
        target = rec.target; plateletEstimate = rec.platelet
        aiResult = rec.aiResult; aiProviderUsed = rec.aiProviderUsed; aiPdfMode = rec.aiPdfMode
        aiAttempts.clear()
        correctionMode = false; showTargetDialog = false
        locked = target != 0 && wbcCounted >= target
        mode = rec.mode
        tallies.forEach { (k, t) -> t.load(rec.tallyCounts[k] ?: emptyMap()); t.locked = false }
        rec.modeInputs.forEach { (k, v) -> inputs[k] = v }
        decodeCustom(rec.customJson.ifBlank { null })?.let { customItems.clear(); customItems.addAll(it) }
        currentRecordId = rec.id
        save()
    }

    fun deleteRecord(rec: ReportRecord) {
        history.removeAll { it.id == rec.id }
        if (currentRecordId == rec.id) currentRecordId = null
        persistHistory()
    }

    private fun historyClearUndo() = this.undoStack.clear()

    private fun persistHistory() {
        val snapshot = history.toList()
        viewModelScope.launch(Dispatchers.IO) {
            HistoryStore.save(ctx, snapshot)
            autoBackupIfConfigured(snapshot)
        }
    }

    // ---------------------------------------------------------------- backup
    private fun backupSettings() = BackupManager.Settings(institution, department, address, reportedBy, aiProviders.toList())

    fun backupJson(): String = BackupManager.build(history.toList(), backupSettings(), backupIncludeKeys)

    fun setBackupFolder(uri: Uri?) {
        val old = backupTreeUri
        if (old != null && old != uri?.toString()) runCatching {
            ctx.contentResolver.releasePersistableUriPermission(Uri.parse(old),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        if (uri != null) runCatching {
            ctx.contentResolver.takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        backupTreeUri = uri?.toString()
        lastBackupError = null
        save()
        if (uri != null) backupNow()
    }

    fun backupFolderName(): String? = backupTreeUri?.let { BackupManager.folderName(ctx, Uri.parse(it)) }

    fun backupNow(onDone: (String?) -> Unit = {}) {
        val snapshot = history.toList()
        viewModelScope.launch {
            val err = withContext(Dispatchers.IO) { autoBackupIfConfigured(snapshot, force = true) }
            onDone(err)
        }
    }

    /** @return null on success, otherwise an error message. */
    private suspend fun autoBackupIfConfigured(snapshot: List<ReportRecord>, force: Boolean = false): String? {
        val tree = backupTreeUri ?: return if (force) "Choose a backup folder first" else null
        return try {
            BackupManager.autoBackup(ctx, Uri.parse(tree),
                BackupManager.build(snapshot, backupSettings(), backupIncludeKeys))
            withContext(Dispatchers.Main) { lastBackupAt = System.currentTimeMillis(); lastBackupError = null; save() }
            null
        } catch (e: Exception) {
            val msg = e.message ?: "Backup failed"
            withContext(Dispatchers.Main) { lastBackupError = msg; save() }
            msg
        }
    }

    fun exportBackupTo(uri: Uri, onDone: (String?) -> Unit) {
        val json = backupJson()
        viewModelScope.launch {
            val err = withContext(Dispatchers.IO) { runCatching { BackupManager.writeTo(ctx, uri, json) }.exceptionOrNull()?.message }
            if (err == null) { lastBackupAt = System.currentTimeMillis(); save() }
            onDone(err)
        }
    }

    /** Merges a backup file into history. Returns a summary message. */
    fun restoreFrom(uri: Uri, restoreSettings: Boolean, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { BackupManager.parse(BackupManager.readFrom(ctx, uri)) }
            }
            result.onFailure { onDone("Restore failed: ${it.message}") }
            result.onSuccess { r ->
                var added = 0; var updated = 0
                r.records.forEach { rec ->
                    val i = history.indexOfFirst { it.id == rec.id }
                    if (i < 0) { history.add(rec); added++ }
                    else if (rec.updatedAt > history[i].updatedAt) { history[i] = rec; updated++ }
                }
                val sorted = history.sortedByDescending { it.updatedAt }
                history.clear(); history.addAll(sorted)
                if (restoreSettings && r.settings != null) {
                    val st = r.settings
                    if (st.institution.isNotBlank()) institution = st.institution
                    if (st.department.isNotBlank()) department = st.department
                    if (st.address.isNotBlank()) address = st.address
                    if (st.reportedBy.isNotBlank()) reportedBy = st.reportedBy
                    st.providers.forEach { p ->
                        val j = aiProviders.indexOfFirst { it.id == p.id }
                        if (j < 0) aiProviders.add(p)
                        else if (p.apiKey.isNotBlank()) aiProviders[j] = p
                        else aiProviders[j] = p.copy(apiKey = aiProviders[j].apiKey)
                    }
                }
                save()
                withContext(Dispatchers.IO) { HistoryStore.save(ctx, history.toList()) }
                onDone("Restored: $added new, $updated updated report(s)")
            }
        }
    }

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

    // ---------------------------------------------------------------- counting
    val wbcCounted: Int get() = CellType.wbcTypes.sumOf { counts[it] ?: 0 }
    val nrbcCount: Int get() = counts[CellType.NRBC] ?: 0
    val canUndo: Boolean get() = undoStack.isNotEmpty()

    fun results(): CountResults = Calculations.compute(counts, wbcText, applyNrbcCorrection)

    fun tap(cell: CellType): TapResult {
        if (correctionMode) {
            val c = counts[cell] ?: 0
            if (c == 0) return TapResult.NOTHING_TO_REMOVE
            counts[cell] = c - 1
            undoStack.add(cell to -1)
            if (locked && target != 0 && wbcCounted < target) locked = false
            save(); return TapResult.REMOVED
        }
        if (locked) return TapResult.LOCKED
        counts[cell] = (counts[cell] ?: 0) + 1
        undoStack.add(cell to 1)
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
        val last = undoStack.removeLastOrNull() ?: return null
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
        counts.clear(); undoStack.clear(); morphology.clear()
        currentRecordId = null
        plateletEstimate = PlateletEstimate.NOT_ASSESSED
        locked = false; correctionMode = false; showTargetDialog = false
        sampleId = ""; patientName = ""; patientAge = ""; patientSex = ""
        referredBy = ""; ward = ""; remarks = ""; wbcText = ""; patientPhone = ""
        aiResult = ""; aiProviderUsed = ""; aiAttempts.clear(); aiPdfMode = AiPdfMode.NONE
        tallies.values.forEach { it.clear() }
        perSampleInputs.forEach { inputs.remove(it) }
        fluidStage = 0
        save()
    }

    fun setGrade(p: MorphologyParam, g: Int) { morphology[p] = g; save() }
    fun setPlatelet(p: PlateletEstimate) { plateletEstimate = p; save() }

    private fun trimHistory() { while (undoStack.size > 2000) undoStack.removeAt(0) }

    // ---------------------------------------------------------------- persistence
    fun save() {
        prefs.edit().apply {
            putString("counts", CellType.entries.joinToString(",") { "${it.name}:${counts[it] ?: 0}" })
            putString("history", undoStack.takeLast(2000).joinToString(",") { "${it.first.name}:${it.second}" })
            putString("currentRecordId", currentRecordId)
            putString("backupTreeUri", backupTreeUri)
            putLong("lastBackupAt", lastBackupAt)
            putString("lastBackupError", lastBackupError)
            putBoolean("backupIncludeKeys", backupIncludeKeys)
            putInt("target", target)
            putBoolean("locked", locked)
            putString("sampleId", sampleId); putString("patientName", patientName)
            putString("patientAge", patientAge); putString("patientSex", patientSex)
            putString("patientPhone", patientPhone); putString("countryCode", countryCode)
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
            putString("mode", mode.name)
            putInt("fluidStage", fluidStage)
            putString("inputs", JSONObject(inputs.toMap() as Map<*, *>).toString())
            putString("customItems", encodeCustom(customItems))
            tallies.values.forEach { t ->
                putString("t_${t.key}_counts", t.encodeCounts()); putString("t_${t.key}_undo", t.encodeUndo())
                putInt("t_${t.key}_target", t.target); putBoolean("t_${t.key}_locked", t.locked)
            }
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
                if (cell != null && d != null) undoStack.add(cell to d)
            }
        }
        target = prefs.getInt("target", 100)
        locked = prefs.getBoolean("locked", false)
        sampleId = prefs.getString("sampleId", "") ?: ""
        patientName = prefs.getString("patientName", "") ?: ""
        patientAge = prefs.getString("patientAge", "") ?: ""
        patientSex = prefs.getString("patientSex", "") ?: ""
        patientPhone = prefs.getString("patientPhone", "") ?: ""
        countryCode = prefs.getString("countryCode", "91") ?: "91"
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
        val saved = prefs.getString("aiProviders", null)
        aiProviders.clear()
        if (saved == null) aiProviders.add(AiProvider.PRESETS.first())
        else aiProviders.addAll(AiProvider.listFromJson(saved))
        aiResult = prefs.getString("aiResult", "") ?: ""
        aiProviderUsed = prefs.getString("aiProviderUsed", "") ?: ""
        aiPdfMode = runCatching { AiPdfMode.valueOf(prefs.getString("aiPdfMode", "NONE")!!) }.getOrDefault(AiPdfMode.NONE)
        currentRecordId = prefs.getString("currentRecordId", null)
        backupTreeUri = prefs.getString("backupTreeUri", null)
        lastBackupAt = prefs.getLong("lastBackupAt", 0L)
        lastBackupError = prefs.getString("lastBackupError", null)
        backupIncludeKeys = prefs.getBoolean("backupIncludeKeys", false)
        mode = runCatching { CounterMode.valueOf(prefs.getString("mode", "BLOOD")!!) }.getOrDefault(CounterMode.BLOOD)
        fluidStage = prefs.getInt("fluidStage", 0)
        inputs.clear()
        runCatching {
            val o = JSONObject(prefs.getString("inputs", "{}") ?: "{}")
            o.keys().forEach { k -> inputs[k] = o.optString(k) }
        }
        customItems.clear()
        customItems.addAll(decodeCustom(prefs.getString("customItems", null)) ?: defaultCustomItems())
        tallies.values.forEach { t ->
            t.decode(prefs.getString("t_${t.key}_counts", null), prefs.getString("t_${t.key}_undo", null))
            t.target = prefs.getInt("t_${t.key}_target", t.target)
            t.locked = prefs.getBoolean("t_${t.key}_locked", false)
        }
    }
}

fun encodeCustom(items: List<SimpleItem>): String = JSONArray().apply {
    items.forEach { put(JSONObject().put("id", it.id).put("name", it.displayName).put("color", it.accent.value.toLong() ushr 32).put("icon", it.drawId).put("inTotal", it.countsInTotal)) }
}.toString()

fun decodeCustom(s: String?): List<SimpleItem>? = runCatching {
    if (s == null) return null
    val a = JSONArray(s)
    (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        SimpleItem(
            id = o.getString("id"), displayName = o.optString("name", "Cell"), group = "Custom",
            accent = androidx.compose.ui.graphics.Color(o.optLong("color", 0xFF2563EB)),
            drawId = o.optString("icon", "NEUTROPHIL"), countsInTotal = o.optBoolean("inTotal", true),
        )
    }
}.getOrNull()
