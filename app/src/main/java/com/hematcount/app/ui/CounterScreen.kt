package com.hematcount.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hematcount.app.CounterViewModel
import com.hematcount.app.Feedback
import com.hematcount.app.TARGET_OPTIONS
import com.hematcount.app.TapResult
import com.hematcount.app.model.Calculations.fmt1
import com.hematcount.app.model.CellType
import com.hematcount.app.targetLabel
import com.hematcount.app.ui.theme.CounterNumberStyle
import com.hematcount.app.ui.theme.LocalClinicalColors
import kotlinx.coroutines.launch

/** Accent colour adjusted for the current theme (lighter on dark backgrounds). */
@Composable
fun CellType.uiAccent(): Color =
    if (LocalClinicalColors.current.isDark) lerp(accent, Color.White, 0.22f) else accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(vm: CounterViewModel, onOpenResults: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val feedback = remember { Feedback(context.applicationContext) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var referenceFor by remember { mutableStateOf<CellType?>(null) }
    var showMorphology by rememberSaveable { mutableStateOf(false) }
    var showNewSample by remember { mutableStateOf(false) }
    var showPatient by remember { mutableStateOf(false) }
    var donutMode by rememberSaveable { mutableStateOf(true) }

    fun message(text: String, action: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(text, actionLabel = action, withDismissAction = action == null,
                duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onAction()
        }
    }

    fun onTap(cell: CellType) {
        when (vm.tap(cell)) {
            TapResult.ADDED -> feedback.tap(vm.hapticsOn, vm.tapSoundOn)
            TapResult.REMOVED -> feedback.correction(vm.hapticsOn, vm.tapSoundOn)
            TapResult.NOTHING_TO_REMOVE -> {
                feedback.denied(vm.hapticsOn)
                message("${cell.displayName}: nothing to subtract")
            }
            TapResult.LOCKED -> {
                feedback.denied(vm.hapticsOn)
                message("Target reached — keypad locked", "Continue") { vm.continueCounting(nextTarget(vm.target)) }
            }
            TapResult.TARGET_REACHED -> feedback.targetReached(vm.hapticsOn, vm.alarmOn)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(36.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { CellIllustration(CellType.NEUTROPHIL, Modifier.size(28.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("HematCount", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                            Text(
                                "Differential leucocyte counter",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    TargetSelector(vm.target) { vm.setTargetValue(it) }
                    IconButton(onClick = { showNewSample = true }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "New sample")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { inner ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(inner)) {
            val landscape = maxWidth > maxHeight
            val tablet = minOf(maxWidth, maxHeight) >= 600.dp
            val gutter = if (tablet) 20.dp else 12.dp

            val summary: @Composable (Modifier) -> Unit = { m ->
                SummaryPanel(
                    vm = vm, donutMode = donutMode, onToggleChart = { donutMode = !donutMode },
                    onEditSample = { showPatient = true },
                    onContinue = { vm.continueCounting(nextTarget(vm.target)) },
                    large = tablet, modifier = m,
                )
            }
            val keypad: @Composable (Modifier) -> Unit = { m ->
                Column(m) {
                    Keypad(vm, large = tablet, onTap = ::onTap, onLongPress = { referenceFor = it },
                        modifier = Modifier.weight(1f).fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    ActionBar(
                        canUndo = vm.canUndo,
                        correction = vm.correctionMode,
                        onUndo = {
                            val c = vm.undo()
                            if (c != null) { feedback.correction(vm.hapticsOn, false); message("Undone: ${c.displayName}") }
                        },
                        onToggleCorrection = {
                            vm.correctionMode = !vm.correctionMode
                            message(if (vm.correctionMode) "Correction mode on — tap a cell to subtract 1" else "Correction mode off")
                        },
                        onMorphology = { showMorphology = true },
                        onResults = onOpenResults,
                    )
                }
            }

            if (landscape) {
                Row(Modifier.fillMaxSize().padding(horizontal = gutter, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(gutter)) {
                    summary(Modifier.weight(0.4f).fillMaxHeight())
                    keypad(Modifier.weight(0.6f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize().padding(horizontal = gutter).padding(bottom = 8.dp)) {
                    summary(Modifier.weight(0.35f).fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    keypad(Modifier.weight(0.65f).fillMaxWidth())
                }
            }
        }
    }

    // ------------------------------------------------------------------ dialogs & sheets
    referenceFor?.let { cell -> ReferenceCardDialog(cell, onDismiss = { referenceFor = null }) }
    if (showMorphology) MorphologySheet(vm, onDismiss = { showMorphology = false })
    if (showPatient) PatientDialog(vm, onDismiss = { showPatient = false })
    if (showNewSample) {
        AlertDialog(
            onDismissRequest = { showNewSample = false },
            icon = { Icon(Icons.Filled.RestartAlt, null) },
            title = { Text("Start a new sample?") },
            text = { Text("All cell counts, sample details, the leucocyte count and morphology grades will be cleared. Settings are kept.") },
            confirmButton = {
                Button(
                    onClick = { vm.newSample(); showNewSample = false; message("Ready for a new sample") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) { Text("Clear and start new") }
            },
            dismissButton = { TextButton(onClick = { showNewSample = false }) { Text("Cancel") } },
        )
    }
    if (vm.showTargetDialog) {
        val next = nextTarget(vm.target)
        AlertDialog(
            onDismissRequest = { vm.showTargetDialog = false },
            icon = { Icon(Icons.Filled.Flag, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Target reached") },
            text = {
                Text("${vm.wbcCounted} leucocytes have been counted. The keypad is locked to prevent over-counting. " +
                    "You can review results, or continue counting.")
            },
            confirmButton = {
                Button(onClick = { vm.showTargetDialog = false; onOpenResults() }) { Text("View results") }
            },
            dismissButton = {
                TextButton(onClick = { vm.continueCounting(next) }) {
                    Text(if (next == 0) "Continue (no limit)" else "Continue to $next")
                }
            },
        )
    }
}

fun nextTarget(current: Int): Int =
    TARGET_OPTIONS.filter { it != 0 && it > current }.minOrNull() ?: 0

// ====================================================================== target selector
@Composable
private fun TargetSelector(target: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(if (target == 0) "Continuous" else "Target $target", maxLines = 1) },
            leadingIcon = { Icon(Icons.Filled.Flag, null, Modifier.size(AssistChipDefaults.IconSize)) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            border = null,
        )
        DropdownMenu(
            expanded = open, onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text("Count target", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            TARGET_OPTIONS.forEach { t ->
                DropdownMenuItem(
                    text = { Text(targetLabel(t)) },
                    onClick = { onSelect(t); open = false },
                    trailingIcon = { if (t == target) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) },
                )
            }
        }
    }
}

// ====================================================================== summary (top 35%)
@Composable
private fun SummaryPanel(
    vm: CounterViewModel,
    donutMode: Boolean,
    onToggleChart: () -> Unit,
    onEditSample: () -> Unit,
    onContinue: () -> Unit,
    large: Boolean,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val counted = vm.wbcCounted
    val target = vm.target
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLow,
        contentColor = scheme.onSurface,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
            // Header: sample ID + chart toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onEditSample,
                    shape = RoundedCornerShape(12.dp),
                    color = scheme.surfaceContainerHighest,
                    contentColor = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Badge, null, Modifier.size(18.dp), tint = scheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Sample / Patient", style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                            val id = listOf(vm.sampleId, vm.patientName).filter { it.isNotBlank() }.joinToString("  ·  ")
                            Text(
                                id.ifBlank { "Tap to add details" },
                                style = MaterialTheme.typography.titleSmall,
                                color = if (id.isBlank()) scheme.onSurfaceVariant else scheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = onToggleChart) {
                    Icon(if (donutMode) Icons.Filled.BarChart else Icons.Filled.DonutLarge,
                        contentDescription = if (donutMode) "Show bar chart" else "Show donut chart",
                        tint = scheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))

            // Progress line
            Row(verticalAlignment = Alignment.Bottom) {
                Text(counted.toString(), style = CounterNumberStyle.copy(fontSize = if (large) 34.sp else 26.sp),
                    color = scheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (target == 0) "leucocytes · continuous" else "/ $target leucocytes",
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp), maxLines = 1,
                )
                Spacer(Modifier.weight(1f))
                StatusPill(vm)
            }
            val progress by animateFloatAsState(
                if (target == 0) 0f else (counted.toFloat() / target).coerceIn(0f, 1f),
                spring(stiffness = 400f), label = "progress",
            )
            if (target != 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).clip(CircleShape),
                    color = if (vm.locked) LocalClinicalColors.current.success else scheme.primary,
                    trackColor = scheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                    drawStopIndicator = {},
                )
            }

            AnimatedVisibility(vm.locked, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                val cc = LocalClinicalColors.current
                Surface(
                    color = cc.successContainer, contentColor = cc.onSuccessContainer,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Target reached · keypad locked", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = onContinue) { Text("Continue", color = cc.onSuccessContainer) }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Chart area
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (donutMode) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(vm, Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true))
                        Spacer(Modifier.width(14.dp))
                        Legend(vm, Modifier.weight(1f).fillMaxHeight())
                    }
                } else {
                    BarList(vm, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun StatusPill(vm: CounterViewModel) {
    val scheme = MaterialTheme.colorScheme
    val (label, bg, fg) = when {
        vm.correctionMode -> Triple("Correction −1", scheme.errorContainer, scheme.onErrorContainer)
        vm.locked -> Triple("Complete", LocalClinicalColors.current.successContainer, LocalClinicalColors.current.onSuccessContainer)
        vm.nrbcCount > 0 -> Triple("Nucleated red cells ${vm.nrbcCount}", scheme.tertiaryContainer, scheme.onTertiaryContainer)
        else -> Triple("Counting", scheme.primaryContainer, scheme.onPrimaryContainer)
    }
    Surface(color = bg, contentColor = fg, shape = CircleShape) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp).widthIn(max = 170.dp),
            overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DonutChart(vm: CounterViewModel, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val total = vm.wbcCounted
    val measurer = rememberTextMeasurer()
    val fractions = CellType.wbcTypes.map { cell ->
        val f = if (total == 0) 0f else (vm.counts[cell] ?: 0).toFloat() / total
        animateFloatAsState(f, tween(350), label = cell.name).value
    }
    val colors = CellType.wbcTypes.map { it.uiAccent() }
    val track = scheme.surfaceContainerHighest
    val centerColor = scheme.onSurface
    val subColor = scheme.onSurfaceVariant
    val gapColor = scheme.surfaceContainerLow
    Canvas(modifier.semantics { contentDescription = "Distribution chart, $total leucocytes counted" }) {
        val stroke = size.minDimension * 0.16f
        val d = size.minDimension - stroke
        val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
        val sz = Size(d, d)
        drawArc(track, 0f, 360f, false, tl, sz, style = Stroke(stroke))
        var start = -90f
        val segments = fractions.count { it > 0.0005f }
        fractions.forEachIndexed { i, f ->
            if (f <= 0.0005f) return@forEachIndexed
            val sweep = f * 360f
            drawArc(colors[i], start, sweep, false, tl, sz, style = Stroke(stroke))
            if (segments > 1) drawArc(gapColor, start + sweep - 1.2f, 1.2f, false, tl, sz, style = Stroke(stroke + 2f))
            start += sweep
        }
        val big = measurer.measure(total.toString(),
            TextStyle(fontSize = (size.minDimension * 0.17f / density).sp, fontWeight = FontWeight.Bold, color = centerColor))
        val small = measurer.measure("leucocytes",
            TextStyle(fontSize = (size.minDimension * 0.075f / density).coerceAtLeast(9f).sp, color = subColor))
        val blockH = big.size.height + small.size.height
        drawText(big, topLeft = Offset(center.x - big.size.width / 2, center.y - blockH / 2))
        drawText(small, topLeft = Offset(center.x - small.size.width / 2, center.y - blockH / 2 + big.size.height))
    }
}

@Composable
private fun Legend(vm: CounterViewModel, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val total = vm.wbcCounted
    val items = CellType.wbcTypes.filter { (vm.counts[it] ?: 0) > 0 }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        if (items.isEmpty()) {
            Text("Tap a cell below to start counting. Long-press any cell for its morphology card.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        items.forEach { cell ->
            val c = vm.counts[cell] ?: 0
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(cell.uiAccent()))
                Spacer(Modifier.width(8.dp))
                Text(cell.displayName, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${fmt1(c * 100.0 / total)}%", style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = scheme.onSurface)
            }
        }
        if (vm.nrbcCount > 0) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(CellType.NRBC.uiAccent()))
                Spacer(Modifier.width(8.dp))
                Text("Nucleated red cells", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val per100 = if (total > 0) vm.nrbcCount * 100.0 / total else 0.0
                Text("${fmt1(per100)}/100", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BarList(vm: CounterViewModel, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val total = vm.wbcCounted
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        CellType.wbcTypes.forEach { cell ->
            val c = vm.counts[cell] ?: 0
            val f = if (total == 0) 0f else c.toFloat() / total
            val anim by animateFloatAsState(f, tween(300), label = "bar")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cell.displayName, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface,
                    modifier = Modifier.weight(0.42f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.weight(0.43f).height(10.dp).clip(CircleShape).background(scheme.surfaceContainerHighest)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(anim).clip(CircleShape).background(cell.uiAccent()))
                }
                Text("${fmt1(f * 100.0)}%", style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = scheme.onSurface, modifier = Modifier.weight(0.15f).padding(start = 6.dp), maxLines = 1)
            }
        }
    }
}

// ====================================================================== keypad (bottom 65%)
@Composable
private fun Keypad(
    vm: CounterViewModel,
    large: Boolean,
    onTap: (CellType) -> Unit,
    onLongPress: (CellType) -> Unit,
    modifier: Modifier,
) {
    val spacing = if (large) 12.dp else 8.dp
    val total = vm.wbcCounted
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        CellType.keypadOrder.chunked(3).forEach { row ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                row.forEach { cell ->
                    CellKey(
                        cell = cell,
                        count = vm.counts[cell] ?: 0,
                        total = total,
                        correction = vm.correctionMode,
                        locked = vm.locked && !vm.correctionMode,
                        large = large,
                        onTap = { onTap(cell) },
                        onLongPress = { onLongPress(cell) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.CellKey(
    cell: CellType,
    count: Int,
    total: Int,
    correction: Boolean,
    locked: Boolean,
    large: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.55f, stiffness = 900f), label = "press")
    val isNrbc = !cell.countsInWbc
    val container by animateColorAsState(
        when {
            correction -> scheme.errorContainer
            isNrbc -> scheme.surfaceContainer
            else -> scheme.surfaceContainerHigh
        }, label = "container",
    )
    val content = if (correction) scheme.onErrorContainer else scheme.onSurface
    val subContent = if (correction) scheme.onErrorContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
    val shape = MaterialTheme.shapes.medium
    val accent = cell.uiAccent()

    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (locked) 0.55f else 1f }
            .clip(shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(color = accent),
                onClickLabel = "Count one ${cell.displayName}",
                onLongClickLabel = "Show morphology reference",
                onLongClick = onLongPress,
                onClick = onTap,
            ),
        shape = shape,
        color = container,
        contentColor = content,
        border = BorderStroke(
            1.dp,
            if (correction) scheme.error.copy(alpha = 0.6f) else scheme.outlineVariant.copy(alpha = if (isNrbc) 0.9f else 0.5f),
        ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h = maxHeight
            val w = maxWidth
            val illus: Dp = minOf(h * 0.46f, w * 0.40f).coerceAtLeast(24.dp)
            val countSize = (h.value * 0.19f).coerceIn(18f, if (large) 40f else 30f)
            val compact = w < 112.dp || h < 92.dp
            // accent strip
            Box(
                Modifier.align(Alignment.CenterStart).padding(vertical = 14.dp).width(4.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)).background(accent)
            )
            Column(Modifier.fillMaxSize().padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    CellIllustration(cell, Modifier.size(illus))
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        AnimatedContent(
                            targetState = count,
                            transitionSpec = {
                                if (targetState > initialState)
                                    (slideInVertically { it / 2 } + fadeIn()).togetherWith(slideOutVertically { -it / 2 } + fadeOut())
                                else
                                    (slideInVertically { -it / 2 } + fadeIn()).togetherWith(slideOutVertically { it / 2 } + fadeOut())
                            },
                            label = "count",
                        ) { n ->
                            Text(n.toString(), style = CounterNumberStyle.copy(fontSize = countSize.sp), color = content, maxLines = 1)
                        }
                        val sub = if (isNrbc) {
                            if (total > 0) "${fmt1(count * 100.0 / total)}/100" else "0/100"
                        } else {
                            if (total > 0) "${fmt1(count * 100.0 / total)}%" else "0.0%"
                        }
                        Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = subContent, maxLines = 1)
                    }
                }
                Text(
                    cell.displayName,
                    style = if (compact) MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    else MaterialTheme.typography.labelLarge.copy(fontSize = if (large) 16.sp else 14.sp),
                    color = content, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = if (compact) 14.sp else 17.sp,
                )
                if (h > 118.dp) {
                    Text(cell.group, style = MaterialTheme.typography.bodySmall, color = subContent,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (correction) {
                Surface(
                    color = scheme.error, contentColor = scheme.onError, shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                ) {
                    Text("−1", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                }
            }
        }
    }
}

// ====================================================================== action bar
@Composable
private fun ActionBar(
    canUndo: Boolean,
    correction: Boolean,
    onUndo: () -> Unit,
    onToggleCorrection: () -> Unit,
    onMorphology: () -> Unit,
    onResults: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(6.dp).heightIn(min = 60.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionItem(Icons.AutoMirrored.Filled.Undo, "Undo", enabled = canUndo, onClick = onUndo)
            ActionItem(
                Icons.Outlined.RemoveCircleOutline, if (correction) "Correcting" else "Correct −1",
                container = if (correction) scheme.error else Color.Transparent,
                content = if (correction) scheme.onError else scheme.onSurface,
                onClick = onToggleCorrection,
            )
            ActionItem(Icons.Outlined.Bloodtype, "RBC · Platelets", onClick = onMorphology)
            ActionItem(
                Icons.Filled.Assessment, "Results",
                container = scheme.primary, content = scheme.onPrimary, onClick = onResults,
            )
        }
    }
}

@Composable
private fun RowScope.ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    container: Color = Color.Transparent,
    content: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    val fg = if (enabled) content else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(22.dp),
        color = container,
        contentColor = fg,
        modifier = Modifier.weight(1f).height(60.dp),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = fg)
            Spacer(Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
