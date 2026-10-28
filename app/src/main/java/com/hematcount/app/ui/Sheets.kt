package com.hematcount.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Lens
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hematcount.app.CounterViewModel
import com.hematcount.app.model.CellType
import com.hematcount.app.model.MorphologyParam
import com.hematcount.app.model.PlateletEstimate
import com.hematcount.app.model.REFERENCE_SOURCES

// ====================================================================== reference card (long press)
@Composable
fun ReferenceCardDialog(cell: com.hematcount.app.model.CountItem, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ref = cell.reference
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(20.dp).widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Column(Modifier.heightIn(max = 680.dp).verticalScroll(rememberScrollState()).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(84.dp).clip(CircleShape).background(scheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) { CellIllustration(cell, Modifier.size(70.dp)) }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cell.displayName, style = MaterialTheme.typography.headlineSmall)
                        Text(cell.group, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        if (cell.normalText.isNotBlank()) {
                            Surface(color = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer, shape = CircleShape) {
                                Text("Normal ${cell.normalText}", style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                }
                Spacer(Modifier.height(16.dp))
                if (ref != null) {
                    RefRow(Icons.Outlined.Straighten, "Size", ref.size)
                    RefRow(Icons.Outlined.Lens, "Nucleus", ref.nucleus)
                    RefRow(Icons.Outlined.Grain, "Cytoplasm & granules", ref.cytoplasm)
                    RefRow(Icons.Outlined.ColorLens, "Staining characteristics", ref.staining)
                    RefRow(Icons.Outlined.Biotech, "Increased in / seen in", ref.increasedIn)
                    RefRow(Icons.Outlined.Info, "Decreased in / notes", ref.decreasedIn)
                } else {
                    Text("No reference card for this counter. Custom counters can be described in the report remarks.",
                        style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = scheme.outlineVariant)
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.AutoMirrored.Outlined.MenuBook, null, Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(REFERENCE_SOURCES, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RefRow(icon: ImageVector, title: String, body: String) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(scheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(18.dp), tint = scheme.onPrimaryContainer) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.primary)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
        }
    }
}

// ====================================================================== RBC & platelet morphology drawer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorphologySheet(vm: CounterViewModel, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        ) {
            Text("Red cell & platelet morphology", style = MaterialTheme.typography.titleLarge)
            Text("Grade each finding on the smear. Nil = not seen.", style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant)
            var section = ""
            MorphologyParam.entries.forEach { p ->
                if (p.section != section) {
                    section = p.section
                    Text(section.uppercase(), style = MaterialTheme.typography.labelMedium, color = scheme.primary,
                        modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                }
                val g = vm.morphology[p] ?: 0
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(p.displayName, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("Nil", "1+", "2+", "3+").forEachIndexed { i, label ->
                            SegmentedButton(
                                selected = g == i,
                                onClick = { vm.setGrade(p, i) },
                                shape = SegmentedButtonDefaults.itemShape(i, 4),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = if (i == 0) scheme.secondaryContainer else scheme.primaryContainer,
                                    activeContentColor = if (i == 0) scheme.onSecondaryContainer else scheme.onPrimaryContainer,
                                    inactiveContainerColor = scheme.surfaceContainerLow,
                                    inactiveContentColor = scheme.onSurface,
                                ),
                            ) { Text(label) }
                        }
                    }
                }
            }
            Text("Platelet estimate on smear", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlateletEstimate.entries.forEach { pe ->
                    FilterChip(
                        selected = vm.plateletEstimate == pe,
                        onClick = { vm.setPlatelet(pe) },
                        label = { Text(pe.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.primaryContainer,
                            selectedLabelColor = scheme.onPrimaryContainer,
                            labelColor = scheme.onSurface,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

// ====================================================================== patient / sample details
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDialog(vm: CounterViewModel, onDismiss: () -> Unit) {
    var sampleId by remember { mutableStateOf(vm.sampleId) }
    var name by remember { mutableStateOf(vm.patientName) }
    var age by remember { mutableStateOf(vm.patientAge) }
    var sex by remember { mutableStateOf(vm.patientSex) }
    var phone by remember { mutableStateOf(vm.patientPhone) }
    var referred by remember { mutableStateOf(vm.referredBy) }
    var ward by remember { mutableStateOf(vm.ward) }
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerHigh,
        title = { Text("Sample & patient details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Sample ID / Lab No.", sampleId, KeyboardCapitalization.Characters) { sampleId = it }
                Field("Patient name", name, KeyboardCapitalization.Words) { name = it }
                Field("Age (years)", age, keyboardType = KeyboardType.Number) { age = it.filter { c -> c.isDigit() || c == '.' }.take(5) }
                Text("Sex", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Male", "Female", "Other").forEachIndexed { i, s ->
                        SegmentedButton(
                            selected = sex == s, onClick = { sex = if (sex == s) "" else s },
                            shape = SegmentedButtonDefaults.itemShape(i, 3),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = scheme.primaryContainer, activeContentColor = scheme.onPrimaryContainer,
                                inactiveContainerColor = scheme.surfaceContainerHigh, inactiveContentColor = scheme.onSurface,
                            ),
                        ) { Text(s) }
                    }
                }
                Field("Patient mobile (WhatsApp)", phone, KeyboardCapitalization.None, KeyboardType.Phone) {
                    phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' }.take(18)
                }
                Text("Without a + prefix, country code +${vm.countryCode} is added (change in Settings).",
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                Field("Referred by", referred, KeyboardCapitalization.Words) { referred = it }
                Field("Ward / OPD / Bed", ward, KeyboardCapitalization.Words) { ward = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                vm.sampleId = sampleId.trim(); vm.patientName = name.trim(); vm.patientAge = age.trim()
                vm.patientSex = sex; vm.patientPhone = phone.trim(); vm.referredBy = referred.trim(); vm.ward = ward.trim()
                vm.save(); onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun Field(
    label: String,
    value: String,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType),
        modifier = modifier,
    )
}
