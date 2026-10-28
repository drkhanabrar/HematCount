package com.hematcount.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hematcount.app.CounterViewModel
import com.hematcount.app.export.PdfReport
import com.hematcount.app.export.ReportData
import com.hematcount.app.export.ShareUtil
import com.hematcount.app.history.BackupManager
import com.hematcount.app.history.ReportRecord
import com.hematcount.app.model.Calculations
import com.hematcount.app.model.Flag
import com.hematcount.app.ui.theme.LocalClinicalColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: CounterViewModel, onBack: () -> Unit, onOpened: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    var query by remember { mutableStateOf("") }
    var confirmOpen by remember { mutableStateOf<ReportRecord?>(null) }
    var confirmDelete by remember { mutableStateOf<ReportRecord?>(null) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }

    fun toast(t: String) = Toast.makeText(context, t, Toast.LENGTH_LONG).show()

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { vm.setBackupFolder(uri); toast("Auto-backup folder set") }
    }
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupManager.MIME)) { uri ->
        if (uri != null) vm.exportBackupTo(uri) { err -> toast(err?.let { "Backup failed: $it" } ?: "Backup saved") }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    val df = remember { SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.US) }
    val filtered = vm.history.filter { r ->
        query.isBlank() || listOf(r.sampleId, r.patientName, r.referredBy, r.ward, r.remarks)
            .any { it.contains(query.trim(), ignoreCase = true) }
    }

    Scaffold(
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        topBar = {
            TopAppBar(
                title = { Text("History & backup") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surface, titleContentColor = scheme.onSurface, navigationIconContentColor = scheme.onSurface,
                ),
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).imePadding(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 860.dp).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // ---------------- cloud backup card
                item {
                    SectionCard("Cloud backup") {
                        val folder = vm.backupFolderName()
                        val ok = folder != null && vm.lastBackupError == null
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                when { folder == null -> Icons.Filled.CloudOff; ok -> Icons.Filled.CloudDone; else -> Icons.Filled.CloudOff },
                                null, tint = when { folder == null -> scheme.onSurfaceVariant; ok -> clinical.success; else -> scheme.error },
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (folder == null) "Auto-backup is off" else "Auto-backup to: $folder",
                                    style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    when {
                                        vm.lastBackupError != null -> "Last attempt failed: ${vm.lastBackupError}"
                                        vm.lastBackupAt > 0 -> "Last backup ${df.format(Date(vm.lastBackupAt))}"
                                        else -> "Choose a folder in Google Drive, OneDrive, MEGA, Dropbox or any drive app"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (vm.lastBackupError != null) scheme.error else scheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = { pickFolder.launch(null) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.FolderOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                                Text(if (folder == null) "Choose folder" else "Change folder", maxLines = 1)
                            }
                            if (folder != null) {
                                OutlinedButton(onClick = { vm.backupNow { e -> toast(e?.let { "Backup failed: $it" } ?: "Backed up") } },
                                    modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.Sync, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Back up now", maxLines = 1)
                                }
                            }
                        }
                        if (folder != null) {
                            TextButton(onClick = { vm.setBackupFolder(null) }) { Text("Turn off auto-backup", color = scheme.error) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { createFile.launch(BackupManager.suggestedFileName()) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.CloudUpload, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Save backup…", maxLines = 1)
                            }
                            OutlinedButton(onClick = { BackupManager.shareFile(context, vm.backupJson()) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Send to app…", maxLines = 1)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { openFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Restore, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Restore from backup")
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Include AI API keys in backups", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                                Text("Off keeps your keys only on this phone", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                            Switch(checked = vm.backupIncludeKeys, onCheckedChange = { vm.backupIncludeKeys = it; vm.save() },
                                colors = SwitchDefaults.colors(checkedTrackColor = scheme.primary, checkedThumbColor = scheme.onPrimary))
                        }
                        Text("\"Send to app\" works with the MEGA app, Dropbox, e-mail and messengers. The system file picker covers Google Drive, OneDrive, Box, MEGA and local storage.",
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                // ---------------- search
                item {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        placeholder = { Text("Search sample ID, patient, clinician…") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Text("${filtered.size} of ${vm.history.size} report(s)", style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant)
                }
                if (vm.history.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.History, null, Modifier.size(48.dp), tint = scheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("No saved reports yet", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                        Text("Reports are saved when you tap \"Save to history\" or export a PDF.",
                            style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                }

                items(filtered, key = { it.id }) { rec ->
                    val results = Calculations.compute(rec.counts, rec.wbcText, rec.applyNrbcCorrection)
                    val modeRep = remember(rec.id, rec.updatedAt) { ReportData.recordModeReport(rec) }
                    val flagged = modeRep?.let { m -> (m.rows + m.rows2).filter { it.flag != Flag.NONE }.map { it.name } }
                        ?: results.rows.filter { it.flag != Flag.NONE }.map { it.cell.displayName }
                    val isCurrent = rec.id == vm.currentRecordId
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = scheme.surfaceContainerLow,
                        contentColor = scheme.onSurface,
                        border = BorderStroke(1.dp, if (isCurrent) scheme.primary else scheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        listOf(rec.sampleId, rec.patientName).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Unnamed sample" },
                                        style = MaterialTheme.typography.titleMedium, color = scheme.onSurface,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(df.format(Date(rec.updatedAt)), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                }
                                if (isCurrent) Surface(color = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer, shape = CircleShape) {
                                    Text("Open now", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            if (modeRep != null) Text(
                                rec.mode.label + " · ${modeRep.totalLabel.lowercase()}: ${modeRep.total}" +
                                    (modeRep.indices.firstOrNull()?.let { " · ${it.label}: ${it.value}" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface,
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            ) else Text(
                                "${rec.wbcCounted} leucocytes" +
                                    (Calculations.parseWbc(rec.wbcText)?.let { " · TLC ${Calculations.fmt2(it)} ×10³/µL" } ?: "") +
                                    (if (results.nrbcCount > 0) " · nRBC ${Calculations.fmt1(results.nrbcPer100)}/100" else ""),
                                style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface,
                            )
                            if (flagged.isNotEmpty()) {
                                Text("Flagged: " + flagged.joinToString(", "), style = MaterialTheme.typography.bodySmall,
                                    color = clinical.high, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            if (rec.remarks.isNotBlank()) {
                                Text(rec.remarks, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { if (isCurrent) onOpened() else confirmOpen = rec }) {
                                    Icon(Icons.Filled.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Open")
                                }
                                TextButton(
                                    enabled = busyId == null,
                                    onClick = {
                                        busyId = rec.id
                                        scope.launch {
                                            val d = ReportData.fromRecord(rec, vm)
                                            val f = runCatching { withContext(Dispatchers.IO) { PdfReport.generate(context, d) } }
                                            busyId = null
                                            f.onSuccess { ShareUtil.sharePdfToWhatsApp(context, it, "Haematology report ${d.reportNo}") }
                                                .onFailure { toast("Could not create PDF: ${it.message}") }
                                        }
                                    },
                                ) {
                                    Icon(Icons.Filled.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                                    Text(if (busyId == rec.id) "Preparing…" else "PDF")
                                }
                                IconButton(onClick = { confirmDelete = rec }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete report", tint = scheme.error)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    confirmOpen?.let { rec ->
        AlertDialog(
            onDismissRequest = { confirmOpen = null },
            title = { Text("Open this report?") },
            text = { Text("The sample currently on the counter will be replaced. Save it to history first if you need it.") },
            confirmButton = { Button(onClick = { vm.openRecord(rec); confirmOpen = null; onOpened() }) { Text("Open") } },
            dismissButton = {
                TextButton(onClick = {
                    if (vm.wbcCounted > 0) vm.saveToHistory()
                    vm.openRecord(rec); confirmOpen = null; onOpened()
                }) { Text("Save current & open") }
            },
        )
    }
    confirmDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this report?") },
            text = { Text("It will be removed from this phone. Copies in earlier cloud backups are not affected.") },
            confirmButton = { TextButton(onClick = { vm.deleteRecord(rec); confirmDelete = null }) { Text("Delete", color = scheme.error) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup") },
            text = { Text("Reports from the backup are merged into your history (nothing is deleted). Also restore the letterhead and AI provider list?") },
            confirmButton = {
                Button(onClick = { vm.restoreFrom(uri, restoreSettings = true) { toast(it) }; pendingRestore = null }) { Text("Reports + settings") }
            },
            dismissButton = {
                TextButton(onClick = { vm.restoreFrom(uri, restoreSettings = false) { toast(it) }; pendingRestore = null }) { Text("Reports only") }
            },
        )
    }
}

