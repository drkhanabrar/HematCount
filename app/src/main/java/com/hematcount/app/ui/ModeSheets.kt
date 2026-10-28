package com.hematcount.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hematcount.app.CounterViewModel
import com.hematcount.app.model.CUSTOM_COLOUR_CHOICES
import com.hematcount.app.model.CUSTOM_ICON_CHOICES
import com.hematcount.app.model.Cellularity
import com.hematcount.app.model.CounterMode
import com.hematcount.app.model.FluidAppearance
import com.hematcount.app.model.FluidPreset
import com.hematcount.app.model.IronStores
import com.hematcount.app.model.MegaStatus
import com.hematcount.app.model.Particles
import com.hematcount.app.model.SimpleItem
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSetupSheet(vm: CounterViewModel, mode: CounterMode, onDismiss: () -> Unit) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (mode) {
                CounterMode.MARROW -> MarrowSetup(vm)
                CounterMode.FLUID -> FluidSetup(vm)
                CounterMode.RETIC -> ReticSetup(vm)
                CounterMode.RBC_MORPH -> RbcGuide()
                CounterMode.PLATELET -> PlateletSetup(vm)
                CounterMode.VIABILITY -> ViabilitySetup(vm)
                CounterMode.CUSTOM -> CustomEditor(vm)
                CounterMode.BLOOD -> {}
            }
            Spacer(Modifier.height(4.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        }
    }
}

// ---------------------------------------------------------------- shared bits
@Composable
private fun SheetTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Label(t: String) {
    Text(t.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o ->
            FilterChip(
                selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = scheme.primaryContainer, selectedLabelColor = scheme.onPrimaryContainer,
                    labelColor = scheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, suffix: String? = null, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }.take(9)) },
        label = { Text(label) }, singleLine = true,
        suffix = if (suffix != null) { { Text(suffix) } } else null,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------- bone marrow
@Composable
private fun MarrowSetup(vm: CounterViewModel) {
    SheetTitle("Bone marrow findings", "Recorded on the marrow report alongside the differential count.")
    Field("Aspirate site (e.g. right posterior iliac crest)", vm.input("bm.site"), KeyboardCapitalization.Sentences) { vm.setInput("bm.site", it) }
    Label("Particles")
    ChoiceChips(Particles.entries, Particles.entries.firstOrNull { it.name == vm.input("bm.particles") } ?: Particles.NOT_ASSESSED, { it.label }) { vm.setInput("bm.particles", it.name) }
    Label("Cellularity")
    ChoiceChips(Cellularity.entries, Cellularity.entries.firstOrNull { it.name == vm.input("bm.cellularity") } ?: Cellularity.NOT_ASSESSED, { it.label }) { vm.setInput("bm.cellularity", it.name) }
    Label("Megakaryocytes")
    ChoiceChips(MegaStatus.entries, MegaStatus.entries.firstOrNull { it.name == vm.input("bm.mega") } ?: MegaStatus.NOT_ASSESSED, { it.label }) { vm.setInput("bm.mega", it.name) }
    Label("Iron stores (Perls stain)")
    ChoiceChips(IronStores.entries, IronStores.entries.firstOrNull { it.name == vm.input("bm.iron") } ?: IronStores.NOT_DONE, { it.label }) { vm.setInput("bm.iron", it.name) }
    Text("Tip: the \"Megakaryocytes\" key tallies megakaryocytes separately; they are not included in the 500-cell percentage.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------------------------------------------------------- CSF & fluids
@Composable
private fun FluidSetup(vm: CounterViewModel) {
    val preset = vm.fluidPreset
    SheetTitle("Chamber setup", "Choose the specimen, dilution and squares counted on the improved Neubauer chamber.")
    Label("Specimen / count")
    ChoiceChips(FluidPreset.entries, preset, { it.label }) { vm.setFluidPreset(it) }
    NumberField("Dilution factor (1 = undiluted)", vm.input("fl.dilution", preset.defaultDilution.toString())) { vm.setInput("fl.dilution", it) }
    Label("Squares counted")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { vm.setInput("fl.squares", (0..8).joinToString(",") { "L$it" }) }) { Text("All 9 large") }
        OutlinedButton(onClick = { vm.setInput("fl.squares", "L0,L2,L6,L8") }) { Text("4 corners") }
        OutlinedButton(onClick = { vm.setInput("fl.squares", "L0,L2,L4,L6,L8") }) { Text("5 large") }
        OutlinedButton(onClick = { vm.setInput("fl.squares", "L4") }) { Text("Centre") }
        OutlinedButton(onClick = { vm.setInput("fl.squares", "S0,S4,S12,S20,S24") }) { Text("5 small (RBC)") }
        TextButton(onClick = { vm.setInput("fl.squares", "") }) { Text("Clear") }
    }
    Text("You can also tap squares directly on the chamber diagram.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (!preset.isBlood) {
        Label("Appearance")
        ChoiceChips(FluidAppearance.entries, FluidAppearance.entries.firstOrNull { it.name == vm.input("fl.appearance") } ?: FluidAppearance.NOT_RECORDED, { it.label }) {
            vm.setInput("fl.appearance", it.name)
        }
        Field("Volume received (e.g. 3 mL)", vm.input("fl.volume"), KeyboardCapitalization.None) { vm.setInput("fl.volume", it) }
    }
    if (preset == FluidPreset.CSF) {
        Label("Traumatic tap correction (optional)")
        Text("Enter the patient's blood counts to correct the CSF nucleated count for blood contamination.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NumberField("Blood leucocyte count", vm.input("fl.bloodWbc"), "×10³/µL") { vm.setInput("fl.bloodWbc", it) }
        NumberField("Blood red cell count", vm.input("fl.bloodRbc"), "×10⁶/µL") { vm.setInput("fl.bloodRbc", it) }
    }
}

// ---------------------------------------------------------------- reticulocytes
@Composable
private fun ReticSetup(vm: CounterViewModel) {
    val scheme = MaterialTheme.colorScheme
    SheetTitle("Reticulocyte count", "Supravital stain (new methylene blue). Count at least 1000 red cells.")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Miller ocular disc", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            Text("Reticulocytes in the large square, all red cells in the small square (1/9 area).",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Switch(checked = vm.input("rt.miller") == "1", onCheckedChange = { vm.setInput("rt.miller", if (it) "1" else "0") },
            colors = SwitchDefaults.colors(checkedTrackColor = scheme.primary, checkedThumbColor = scheme.onPrimary))
    }
    NumberField("Red cell count (analyser)", vm.input("rt.rbc"), "×10⁶/µL") { vm.setInput("rt.rbc", it) }
    NumberField("Haematocrit", vm.input("rt.hct"), "%") { vm.setInput("rt.hct", it) }
    Text("Used for the absolute reticulocyte count, corrected reticulocyte % and reticulocyte production index.",
        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
}

// ---------------------------------------------------------------- red cell morphology
@Composable
private fun RbcGuide() {
    val scheme = MaterialTheme.colorScheme
    SheetTitle("Counting red cell abnormalities", "How to use this counter")
    listOf(
        "Choose a monolayer area where red cells just touch but do not overlap.",
        "Tap \"Normal Red Cells\" for every normal cell and the matching key for each abnormal cell, so that the total equals all red cells assessed.",
        "Count at least 1000 red cells (set the target to 1000). ICSH: schistocytes above 1% are clinically significant.",
        "Long-press any key for its description and associated conditions.",
    ).forEach {
        Row { Text("•", color = scheme.primary); Spacer(Modifier.width(8.dp)); Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface) }
    }
}

// ---------------------------------------------------------------- platelets
@Composable
private fun PlateletSetup(vm: CounterViewModel) {
    SheetTitle("Platelet estimate", "Count platelets in successive oil-immersion (×100) fields of the monolayer.")
    Text("1. Tap \"Platelets\" for each platelet in the field.\n2. Tap \"Field Completed\" before moving to the next field.\n3. Count at least 10 fields.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    NumberField("Multiplication factor", vm.input("pl.factor", "15000"), "/µL per platelet") { vm.setInput("pl.factor", it) }
    Text("A factor of 15 000 – 20 000 is commonly used; validate it against your analyser.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------------------------------------------------------- viability
@Composable
private fun ViabilitySetup(vm: CounterViewModel) {
    SheetTitle("Cell viability (trypan blue)", "Unstained cells are viable; blue cells are non-viable.")
    NumberField("Dilution factor (cells : trypan blue 1:1 = 2)", vm.input("vb.dilution", "2")) { vm.setInput("vb.dilution", it) }
    NumberField("Large squares counted", vm.input("vb.squares", "4")) { vm.setInput("vb.squares", it) }
    Text("Cells/mL = (cells ÷ squares) × dilution × 10⁴", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------------------------------------------------------- custom counter editor
@Composable
private fun CustomEditor(vm: CounterViewModel) {
    val scheme = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf<SimpleItem?>(null) }
    SheetTitle("Custom counter", "Create your own keys with names, icons and colours.")
    Field("Report title", vm.input("cu.title"), KeyboardCapitalization.Words) { vm.setInput("cu.title", it) }
    Field("Specimen", vm.input("cu.specimen"), KeyboardCapitalization.Sentences) { vm.setInput("cu.specimen", it) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Keys", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = {
            editing = SimpleItem("CU_" + UUID.randomUUID().toString().take(8), "", "Custom",
                Color(CUSTOM_COLOUR_CHOICES[vm.customItems.size % CUSTOM_COLOUR_CHOICES.size]), "NEUTROPHIL")
        }, enabled = vm.customItems.size < 24) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add key")
        }
    }
    vm.customItems.forEachIndexed { i, item ->
        Surface(color = scheme.surfaceContainerHigh, contentColor = scheme.onSurface, shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(item.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    CellIllustration(item, Modifier.size(30.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.displayName, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                    Text(if (item.countsInTotal) "Included in %" else "Tallied separately", style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant)
                }
                IconButton(onClick = { vm.moveCustom(i, -1) }, enabled = i > 0) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
                IconButton(onClick = { vm.moveCustom(i, 1) }, enabled = i < vm.customItems.lastIndex) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
                IconButton(onClick = { editing = item }) { Icon(Icons.Filled.Edit, "Edit") }
                IconButton(onClick = { vm.removeCustom(item) }) { Icon(Icons.Filled.Delete, "Delete", tint = scheme.error) }
            }
        }
    }
    editing?.let { e -> CustomKeyDialog(e, onDismiss = { editing = null }) { vm.upsertCustom(it); editing = null } }
}

@Composable
private fun CustomKeyDialog(initial: SimpleItem, onDismiss: () -> Unit, onSave: (SimpleItem) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initial.displayName) }
    var icon by remember { mutableStateOf(initial.drawId) }
    var colour by remember { mutableStateOf(initial.accent) }
    var inTotal by remember { mutableStateOf(initial.countsInTotal) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerHigh,
        title = { Text(if (initial.displayName.isBlank()) "Add key" else "Edit key") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field("Key name (e.g. Hairy Cells)", name, KeyboardCapitalization.Words) { name = it.take(40) }
                Text("Icon", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                CUSTOM_ICON_CHOICES.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (id, label) ->
                            val sel = id == icon
                            Box(
                                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (sel) scheme.primaryContainer else scheme.surfaceContainerHighest)
                                    .border(if (sel) 2.dp else 0.dp, if (sel) scheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { icon = id },
                                contentAlignment = Alignment.Center,
                            ) {
                                CellIllustration(SimpleItem("x", label, "", colour, id), Modifier.size(36.dp))
                            }
                        }
                    }
                }
                Text("Colour", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                CUSTOM_COLOUR_CHOICES.chunked(7).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { argb ->
                            val c = Color(argb)
                            Box(Modifier.size(30.dp).clip(CircleShape).background(c)
                                .border(if (c == colour) 3.dp else 0.dp, scheme.onSurface, CircleShape).clickable { colour = c })
                        }
                    }
                }
                HorizontalDivider(color = scheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Include in percentage total", style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        Text("Turn off for tallies such as nucleated red cells or fields.", style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant)
                    }
                    Switch(checked = inTotal, onCheckedChange = { inTotal = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = scheme.primary, checkedThumbColor = scheme.onPrimary))
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                onSave(initial.copy(displayName = name.trim(), drawId = icon, accent = colour, countsInTotal = inTotal))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

