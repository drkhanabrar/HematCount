package com.hematcount.app.ui

import android.os.Build
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hematcount.app.BuildConfigInfo
import com.hematcount.app.CounterViewModel
import com.hematcount.app.ai.AiProvider
import com.hematcount.app.model.REFERENCE_SOURCES
import com.hematcount.app.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: CounterViewModel, onBack: () -> Unit, onOpenAbout: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf<AiProvider?>(null) }
    var deleting by remember { mutableStateOf<AiProvider?>(null) }

    Scaffold(
        containerColor = scheme.surface,
        contentColor = scheme.onSurface,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surface, titleContentColor = scheme.onSurface, navigationIconContentColor = scheme.onSurface,
                ),
            )
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner).imePadding(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // ---------------- appearance
                item {
                    SectionCard("Appearance") {
                        Text("Theme", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                                .forEachIndexed { i, (m, label) ->
                                    SegmentedButton(
                                        selected = vm.themeMode == m,
                                        onClick = { vm.themeMode = m; vm.save() },
                                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                                        colors = SegmentedButtonDefaults.colors(
                                            activeContainerColor = scheme.primaryContainer, activeContentColor = scheme.onPrimaryContainer,
                                            inactiveContainerColor = scheme.surfaceContainerLow, inactiveContentColor = scheme.onSurface,
                                        ),
                                    ) { Text(label) }
                                }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            SettingSwitch("Use device colours", "Material You colours from your wallpaper instead of the clinical palette",
                                vm.dynamicColor) { vm.dynamicColor = it; vm.save() }
                        }
                    }
                }
                // ---------------- feedback
                item {
                    SectionCard("Counting feedback") {
                        SettingSwitch("Haptic feedback", "Vibration on every tap and a heavy pattern at the target", vm.hapticsOn) { vm.hapticsOn = it; vm.save() }
                        SettingSwitch("Tap sound", "Soft click on every count", vm.tapSoundOn) { vm.tapSoundOn = it; vm.save() }
                        SettingSwitch("Target alarm chime", "Distinct chime when the target count is reached", vm.alarmOn) { vm.alarmOn = it; vm.save() }
                    }
                }
                // ---------------- report header
                item {
                    SectionCard("PDF report letterhead") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Field("Institution / hospital name", vm.institution, KeyboardCapitalization.Words) { vm.institution = it; vm.save() }
                            Field("Department / laboratory", vm.department, KeyboardCapitalization.Words) { vm.department = it; vm.save() }
                            Field("Address / contact line", vm.address, KeyboardCapitalization.Words) { vm.address = it; vm.save() }
                            Field("Default \"Reported by\"", vm.reportedBy, KeyboardCapitalization.Words) { vm.reportedBy = it; vm.save() }
                            Field("Default WhatsApp country code (e.g. 91 for India)", vm.countryCode, KeyboardCapitalization.None, KeyboardType.Number) {
                                vm.countryCode = it.filter { c -> c.isDigit() }.take(4); vm.save()
                            }
                        }
                    }
                }
                // ---------------- AI providers
                item {
                    SectionCard("AI providers (failover order)", action = {
                        FilledTonalButton(onClick = { editing = AiProvider.PRESETS.first().copy(id = java.util.UUID.randomUUID().toString()) }) {
                            Icon(Icons.Filled.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add")
                        }
                    }) {
                        Text("Providers are tried from top to bottom. If one fails (no key, quota, network, error), the next one is used. Use the arrows to change the order.",
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        if (vm.aiProviders.isEmpty()) {
                            Text("No providers yet. Tap Add to set up OpenRouter or your own API.", style = MaterialTheme.typography.bodyMedium)
                        }
                        vm.aiProviders.forEachIndexed { i, p ->
                            ProviderRow(
                                index = i, p = p, last = i == vm.aiProviders.lastIndex,
                                onUp = { vm.moveProvider(i, -1) }, onDown = { vm.moveProvider(i, 1) },
                                onToggle = { vm.toggleProvider(p) }, onEdit = { editing = p }, onDelete = { deleting = p },
                            )
                            if (i != vm.aiProviders.lastIndex) Spacer(Modifier.height(8.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("API keys are stored only on this device, in the app's private storage.",
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
                // ---------------- about
                item {
                    SectionCard("About HematCount", action = {
                        TextButton(onClick = onOpenAbout) { Text("View") }
                    }) {
                        Text("Version ${BuildConfigInfo.versionName(androidx.compose.ui.platform.LocalContext.current)}",
                            style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Reference intervals and morphology notes: $REFERENCE_SOURCES",
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("Intended for education and as a counting aid. Reports must be verified by a qualified pathologist.",
                            style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    editing?.let { p ->
        ProviderDialog(
            initial = p,
            isNew = vm.aiProviders.none { it.id == p.id },
            onDismiss = { editing = null },
            onSave = { vm.upsertProvider(it); editing = null },
        )
    }
    deleting?.let { p ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Remove ${p.name}?") },
            text = { Text("This provider and its API key will be deleted from this device.") },
            confirmButton = { TextButton(onClick = { vm.removeProvider(p); deleting = null }) { Text("Remove", color = scheme.error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = scheme.primary, checkedThumbColor = scheme.onPrimary))
    }
}

@Composable
private fun ProviderRow(
    index: Int, p: AiProvider, last: Boolean,
    onUp: () -> Unit, onDown: () -> Unit, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = if (p.enabled) scheme.surfaceContainerHigh else scheme.surfaceContainer,
        contentColor = scheme.onSurface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = if (p.enabled) scheme.primary else scheme.outline,
                    contentColor = if (p.enabled) scheme.onPrimary else scheme.surface, shape = CircleShape) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(p.model.ifBlank { "No model set" }, style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (p.apiKey.isBlank()) "No API key" else "Key ••••" + p.apiKey.takeLast(4),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (p.apiKey.isBlank()) scheme.error else scheme.onSurfaceVariant)
                }
                Switch(checked = p.enabled, onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(checkedTrackColor = scheme.primary, checkedThumbColor = scheme.onPrimary))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onUp, enabled = index > 0) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up") }
                IconButton(onClick = onDown, enabled = !last) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down") }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = scheme.error) }
            }
        }
    }
}

@Composable
private fun ProviderDialog(initial: AiProvider, isNew: Boolean, onDismiss: () -> Unit, onSave: (AiProvider) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(initial.name) }
    var url by remember { mutableStateOf(initial.baseUrl) }
    var key by remember { mutableStateOf(initial.apiKey) }
    var model by remember { mutableStateOf(initial.model) }
    var showKey by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerHigh,
        title = { Text(if (isNew) "Add AI provider" else "Edit AI provider") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isNew) {
                    Text("Start from a preset", style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiProvider.PRESETS.forEach { pr ->
                            FilterChip(
                                selected = url == pr.baseUrl && name == pr.name,
                                onClick = { name = pr.name; url = pr.baseUrl; model = pr.model },
                                label = { Text(pr.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = scheme.primaryContainer, selectedLabelColor = scheme.onPrimaryContainer,
                                    labelColor = scheme.onSurface,
                                ),
                            )
                        }
                    }
                }
                Field("Display name", name, KeyboardCapitalization.Words) { name = it }
                Field("Base URL (OpenAI-compatible)", url, KeyboardCapitalization.None, KeyboardType.Uri) { url = it.trim() }
                Text("The app calls <base URL>/chat/completions", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                OutlinedTextField(
                    value = key, onValueChange = { key = it.trim() }, label = { Text("API key") }, singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Field("Model", model, KeyboardCapitalization.None) { model = it.trim() }
                Text("OpenRouter examples: openrouter/auto, google/gemini-2.0-flash-001, anthropic/claude-sonnet-4, meta-llama/llama-3.3-70b-instruct",
                    style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && url.startsWith("http") && model.isNotBlank(),
                onClick = { onSave(initial.copy(name = name.trim(), baseUrl = url.trim(), apiKey = key.trim(), model = model.trim())) },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
