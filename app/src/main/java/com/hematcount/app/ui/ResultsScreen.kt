package com.hematcount.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.hematcount.app.CounterViewModel
import com.hematcount.app.ai.AiPdfMode
import com.hematcount.app.export.PdfReport
import com.hematcount.app.export.ReportData
import com.hematcount.app.export.ReportText
import com.hematcount.app.export.ShareUtil
import com.hematcount.app.export.TLC_REF_TEXT
import com.hematcount.app.model.Calculations
import com.hematcount.app.model.Calculations.fmt1
import com.hematcount.app.model.Calculations.fmt2
import com.hematcount.app.model.CellType
import com.hematcount.app.model.Flag
import com.hematcount.app.model.MorphologyParam
import com.hematcount.app.model.PlateletEstimate
import com.hematcount.app.model.gradeLabel
import com.hematcount.app.ui.theme.LocalClinicalColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(vm: CounterViewModel, onBack: () -> Unit, onOpenAiSettings: () -> Unit) {
    val tab = vm.resultsTab
    val snackbar = remember { SnackbarHostState() }
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Results") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to counter") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = scheme.surface, titleContentColor = scheme.onSurface,
                        navigationIconContentColor = scheme.onSurface,
                    ),
                )
                PrimaryTabRow(
                    selectedTabIndex = tab,
                    containerColor = scheme.surface,
                    contentColor = scheme.primary,
                ) {
                    Tab(selected = tab == 0, onClick = { vm.resultsTab = 0 },
                        selectedContentColor = scheme.primary, unselectedContentColor = scheme.onSurfaceVariant,
                        text = { Text("Report") }, icon = { Icon(Icons.Filled.PictureAsPdf, null) })
                    Tab(selected = tab == 1, onClick = { vm.resultsTab = 1 },
                        selectedContentColor = scheme.primary, unselectedContentColor = scheme.onSurfaceVariant,
                        text = { Text("AI Analysis") }, icon = { Icon(Icons.Filled.AutoAwesome, null) })
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).imePadding(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 860.dp).fillMaxWidth()) {
                if (tab == 0) ReportTab(vm, snackbar) else AiTab(vm, onOpenAiSettings)
            }
        }
    }
}

// ====================================================================== REPORT TAB
@Composable
private fun ReportTab(vm: CounterViewModel, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    var showPatient by remember { mutableStateOf(false) }
    var showMorph by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val results = vm.results()
    val data = ReportData.from(vm)

    fun toast(t: String) = Toast.makeText(context, t, Toast.LENGTH_SHORT).show()

    fun withPdf(action: (File, ReportData) -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            val d = ReportData.from(vm)
            val file = runCatching { withContext(Dispatchers.IO) { PdfReport.generate(context, d) } }
            busy = false
            file.onSuccess { action(it, d) }.onFailure { toast("Could not create PDF: ${it.message}") }
        }
    }

    fun saveNow() = withPdf { f, _ ->
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { ShareUtil.saveToDownloads(context, f) } }
                .onSuccess { loc ->
                    val r = snackbar.showSnackbar("Saved to $loc", actionLabel = "Open")
                    if (r == androidx.compose.material3.SnackbarResult.ActionPerformed && !ShareUtil.openPdf(context, f))
                        toast("No PDF viewer installed")
                }
                .onFailure { toast("Save failed: ${it.message}") }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) saveNow() else toast("Storage permission is needed to save on this Android version")
    }

    fun onSave() {
        if (ShareUtil.needsStoragePermission() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else saveNow()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------------- alerts
        val alerts = data.alerts
        if (alerts.isNotEmpty()) item {
            Surface(color = clinical.highContainer, contentColor = clinical.onHighContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clinical alerts", style = MaterialTheme.typography.titleSmall)
                    }
                    alerts.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp)) }
                }
            }
        }

        // ---------------- patient
        item {
            SectionCard("Sample & patient", action = { TextButton(onClick = { showPatient = true }) { Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") } }) {
                val ageSex = listOf(vm.patientAge.let { if (it.isNotBlank()) "$it y" else "" }, vm.patientSex).filter { it.isNotBlank() }.joinToString(" / ")
                InfoGrid(listOf(
                    "Sample ID" to vm.sampleId, "Patient" to vm.patientName, "Age / Sex" to ageSex,
                    "Referred by" to vm.referredBy, "Ward / OPD" to vm.ward,
                    "Count target" to if (vm.target == 0) "Continuous" else "${vm.target}",
                ))
            }
        }

        // ---------------- leucocyte count & correction
        item {
            SectionCard("Total leucocyte count") {
                OutlinedTextField(
                    value = vm.wbcText,
                    onValueChange = { t -> vm.wbcText = t.filter { it.isDigit() || it == '.' || it == ',' }.take(7); vm.save() },
                    label = { Text("Analyser total leucocyte count") },
                    suffix = { Text("×10³/µL") },
                    supportingText = { Text("Normal range $TLC_REF_TEXT ×10³/µL · e.g. 12.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Apply nucleated red cell correction", style = MaterialTheme.typography.bodyLarge)
                        Text("Used when ≥ 5 nucleated red cells / 100 leucocytes. Turn off if your analyser already reports a corrected count.",
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = vm.applyNrbcCorrection, onCheckedChange = { vm.applyNrbcCorrection = it; vm.save() },
                        colors = SwitchDefaults.colors(checkedTrackColor = scheme.primary, checkedThumbColor = scheme.onPrimary))
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = scheme.outlineVariant)
                KeyValue("Leucocytes counted", "${results.wbcCounted}")
                KeyValue("Nucleated red cells", "${results.nrbcCount} · ${fmt1(results.nrbcPer100)} / 100 leucocytes")
                if (results.analyserWbc != null) {
                    KeyValue("Corrected leucocyte count",
                        if (results.correctionApplied && results.correctedWbc != null) "${fmt2(results.correctedWbc)} ×10³/µL" else "Not required",
                        highlight = results.correctionApplied)
                    val neut = results.rows.first { it.cell == CellType.NEUTROPHIL }.absolute ?: 0.0
                    val band = results.rows.first { it.cell == CellType.BAND }.absolute ?: 0.0
                    KeyValue("Absolute neutrophil count", "${fmt2(neut + band)} ×10³/µL")
                    KeyValue("Absolute lymphocyte count",
                        "${fmt2(results.rows.first { it.cell == CellType.LYMPHOCYTE }.absolute ?: 0.0)} ×10³/µL")
                } else if (vm.wbcText.isNotBlank() && Calculations.parseWbc(vm.wbcText) == null) {
                    Text("Enter a valid number", color = scheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ---------------- differential table
        item {
            SectionCard("Differential leucocyte count") {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(scheme.secondaryContainer).padding(horizontal = 10.dp, vertical = 8.dp)) {
                    val st = MaterialTheme.typography.labelMedium
                    val c = scheme.onSecondaryContainer
                    Text("Cell type · normal range", style = st, color = c, modifier = Modifier.weight(1.7f))
                    Text("Count", style = st, color = c, modifier = Modifier.weight(0.6f))
                    Text("%", style = st, color = c, modifier = Modifier.weight(0.6f))
                    Text("×10³/µL", style = st, color = c, modifier = Modifier.weight(0.8f))
                }
                results.rows.forEachIndexed { i, row ->
                    val flagColor = when (row.flag) { Flag.HIGH -> clinical.high; Flag.LOW -> clinical.low; Flag.PRESENT -> clinical.warning; Flag.NONE -> scheme.onSurface }
                    Row(
                        Modifier.fillMaxWidth()
                            .background(if (i % 2 == 1) scheme.surfaceContainerHigh.copy(alpha = 0.6f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1.7f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(row.cell.uiAccent()))
                                Spacer(Modifier.width(6.dp))
                                Text(row.cell.displayName, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface,
                                    fontWeight = if (row.flag != Flag.NONE) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("${row.cell.refPercent} %" + if (row.cell.refAbsolute != "—") " · ${row.cell.refAbsolute}" else "",
                                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Text("${row.count}", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(0.6f))
                        Text(fmt1(row.percent), style = MaterialTheme.typography.bodyMedium, color = flagColor,
                            fontWeight = if (row.flag != Flag.NONE) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(0.6f))
                        Row(Modifier.weight(0.8f), verticalAlignment = Alignment.CenterVertically) {
                            Text(row.absolute?.let { fmt2(it) } ?: "—", style = MaterialTheme.typography.bodyMedium, color = flagColor,
                                fontWeight = if (row.flag != Flag.NONE) FontWeight.Bold else FontWeight.Normal)
                            if (row.flag != Flag.NONE) {
                                Spacer(Modifier.width(4.dp))
                                Text(if (row.flag == Flag.PRESENT) "!" else row.flag.label,
                                    style = MaterialTheme.typography.labelSmall, color = flagColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Text("H = above, L = below normal range, ! = not normally present. Absolute counts use the ${if (results.correctionApplied) "corrected" else "analyser"} leucocyte count.",
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // ---------------- morphology
        item {
            SectionCard("Red cell & platelet morphology", action = { TextButton(onClick = { showMorph = true }) { Icon(Icons.Filled.Tune, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Grade") } }) {
                val items = MorphologyParam.entries.map { it.displayName to gradeLabel(vm.morphology[it] ?: 0) } +
                    ("Platelets on smear" to if (vm.plateletEstimate == PlateletEstimate.NOT_ASSESSED) "Not assessed" else vm.plateletEstimate.displayName)
                InfoGrid(items)
            }
        }

        // ---------------- remarks
        item {
            SectionCard("Impression / remarks") {
                OutlinedTextField(
                    value = vm.remarks, onValueChange = { vm.remarks = it; vm.save() },
                    placeholder = { Text("e.g. Neutrophilic leucocytosis with left shift. Suggest blood culture.") },
                    minLines = 3, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vm.reportedBy, onValueChange = { vm.reportedBy = it; vm.save() },
                    label = { Text("Reported by") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (vm.aiResult.isNotBlank()) {
                    Text("AI content in PDF: ${vm.aiPdfMode.label} (change in the AI Analysis tab)",
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // ---------------- export
        item {
            SectionCard("Export & share") {
                if (busy) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp)); Text("Preparing PDF…", style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = {
                        withPdf { f, d ->
                            val opened = ShareUtil.sharePdfToWhatsApp(context, f, "Haematology report ${d.reportNo}" +
                                if (d.sampleId.isNotBlank()) " · ${d.sampleId}" else "")
                            if (!opened) toast("WhatsApp not found — choose an app")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E), contentColor = Color.White),
                ) { Icon(Icons.Outlined.Forum, null); Spacer(Modifier.width(10.dp)); Text("Share PDF on WhatsApp") }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { onSave() }, modifier = Modifier.weight(1f).height(48.dp)) {
                        Icon(Icons.Filled.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save PDF", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { withPdf { f, d -> ShareUtil.sharePdf(context, f, "Haematology report ${d.reportNo}") } },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Share PDF", maxLines = 1) }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            ShareUtil.copyText(context, "HematCount report", ReportText.build(ReportData.from(vm)))
                            toast("Summary copied for lab notes / LIS")
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Copy text", maxLines = 1) }
                    OutlinedButton(
                        onClick = { ShareUtil.shareTextToWhatsApp(context, ReportText.build(ReportData.from(vm))) },
                        modifier = Modifier.weight(1f).height(48.dp),
                    ) { Icon(Icons.Outlined.Forum, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Text to WhatsApp", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showPatient) PatientDialog(vm) { showPatient = false }
    if (showMorph) MorphologySheet(vm) { showMorph = false }
}

// ====================================================================== AI TAB
@Composable
private fun AiTab(vm: CounterViewModel, onOpenAiSettings: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    val enabled = vm.aiProviders.filter { it.enabled }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionCard("AI-assisted interpretation") {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Shield, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Only de-identified data is sent: age, sex, counts, ranges and morphology grades. Patient name, sample ID, clinician and ward are never sent.",
                        style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                }
                Spacer(Modifier.height(10.dp))
                Text("Failover order", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                Text(
                    if (enabled.isEmpty()) "No provider enabled" else enabled.mapIndexed { i, p -> "${i + 1}. ${p.name}" }.joinToString("   →   "),
                    style = MaterialTheme.typography.bodyMedium, color = if (enabled.isEmpty()) scheme.error else scheme.onSurface,
                )
                if (enabled.any { it.apiKey.isBlank() && !it.baseUrl.contains("localhost") && !it.baseUrl.contains("192.168.") }) {
                    Text("Some providers have no API key yet.", style = MaterialTheme.typography.bodySmall, color = clinical.warning)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenAiSettings) {
                    Icon(Icons.Filled.Tune, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Manage AI providers & order")
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { vm.runAiAnalysis() },
                    enabled = !vm.aiBusy && vm.wbcCounted > 0 && enabled.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (vm.aiBusy) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = scheme.onPrimary)
                        Spacer(Modifier.width(10.dp))
                        Text(if (vm.aiTrying.isNotBlank()) "Asking ${vm.aiTrying}…" else "Analysing…")
                    } else {
                        Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(10.dp))
                        Text(if (vm.aiResult.isBlank()) "Analyse report with AI" else "Re-analyse")
                    }
                }
                if (vm.wbcCounted == 0) Text("Count some cells first.", style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }

        if (vm.aiAttempts.isNotEmpty()) item {
            SectionCard("Provider attempts") {
                vm.aiAttempts.forEach { a ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                        Icon(if (a.ok) Icons.Filled.CheckCircle else Icons.Filled.Error, null, Modifier.size(18.dp),
                            tint = if (a.ok) clinical.success else scheme.error)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(a.provider, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
                            Text(a.detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (vm.aiResult.isNotBlank()) {
            item {
                SectionCard("Interpretation", action = {
                    IconButton(onClick = {
                        ShareUtil.copyText(context, "AI interpretation", vm.aiResult)
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Filled.ContentCopy, contentDescription = "Copy interpretation", tint = scheme.onSurfaceVariant) }
                }) {
                    if (vm.aiProviderUsed.isNotBlank()) {
                        Surface(color = scheme.tertiaryContainer, contentColor = scheme.onTertiaryContainer, shape = CircleShape) {
                            Text(vm.aiProviderUsed, style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    MarkdownText(vm.aiResult)
                    Spacer(Modifier.height(10.dp))
                    Surface(color = clinical.warningContainer, contentColor = clinical.onWarningContainer, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Warning, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Decision support only. AI output can be wrong; it is not a diagnosis. Always verify against the smear, clinical details and standard references.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                SectionCard("Add to PDF report") {
                    AiPdfMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { vm.aiPdfMode = mode; vm.save() }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = vm.aiPdfMode == mode, onClick = { vm.aiPdfMode = mode; vm.save() },
                                colors = RadioButtonDefaults.colors(selectedColor = scheme.primary, unselectedColor = scheme.onSurfaceVariant))
                            Column {
                                Text(mode.label, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                                Text(
                                    when (mode) {
                                        AiPdfMode.NONE -> "The PDF contains only your report."
                                        AiPdfMode.INVESTIGATIONS -> "Adds a \"Suggested further investigations\" section, marked AI-assisted."
                                        AiPdfMode.FULL -> "Adds the complete AI interpretation, marked AI-assisted."
                                    },
                                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    TextButton(onClick = { vm.clearAi() }, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Clear AI analysis", color = scheme.error)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ====================================================================== shared pieces
@Composable
fun SectionCard(title: String, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
                action?.invoke()
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun InfoGrid(items: List<Pair<String, String>>) {
    val scheme = MaterialTheme.colorScheme
    items.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            row.forEach { (k, v) ->
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(k, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    Text(v.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeyValue(k: String, v: String, highlight: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(k, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = if (highlight) scheme.primary else scheme.onSurface)
    }
}

/** Minimal markdown renderer for AI output: ## headings, bullets, **bold**. */
@Composable
fun MarkdownText(md: String) {
    val scheme = MaterialTheme.colorScheme
    md.replace("\r", "").split("\n").forEach { raw ->
        val line = raw.trimEnd()
        val t = line.trim()
        when {
            t.isBlank() -> Spacer(Modifier.height(4.dp))
            t.startsWith("#") -> Text(inlineBold(t.trimStart('#', ' ')), style = MaterialTheme.typography.titleSmall,
                color = scheme.primary, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
            t.startsWith("- ") || t.startsWith("* ") || t.startsWith("• ") -> Row(Modifier.padding(start = 4.dp, top = 2.dp)) {
                Text("•", style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(inlineBold(t.substring(2).trim()), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            }
            else -> Text(inlineBold(t), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun inlineBold(s: String): AnnotatedString = buildAnnotatedString {
    val parts = s.split("**")
    parts.forEachIndexed { i, p ->
        if (i % 2 == 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(p) } else append(p)
    }
}
