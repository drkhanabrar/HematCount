package com.hematcount.app.ui

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.hematcount.app.MainActivity
import com.hematcount.app.TARGET_OPTIONS
import com.hematcount.app.model.Calculations.fmt1
import com.hematcount.app.model.CellType
import com.hematcount.app.model.CountItem
import com.hematcount.app.model.CounterMode
import com.hematcount.app.model.Flag
import com.hematcount.app.model.FluidPreset
import com.hematcount.app.model.KeypadSection
import com.hematcount.app.model.Layouts
import com.hematcount.app.model.MarrowCell
import com.hematcount.app.model.Neubauer
import com.hematcount.app.targetLabel
import com.hematcount.app.ui.theme.CounterNumberStyle
import com.hematcount.app.ui.theme.LocalClinicalColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    vm: CounterViewModel,
    onOpenResults: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var referenceFor by remember { mutableStateOf<CountItem?>(null) }
    var showSetup by rememberSaveable { mutableStateOf(false) }
    var showNewSample by remember { mutableStateOf(false) }
    var showPatient by remember { mutableStateOf(false) }
    var showKeyboardHelp by remember { mutableStateOf(false) }

    val mode = vm.mode

    fun message(text: String, action: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(text, actionLabel = action, withDismissAction = action == null,
                duration = SnackbarDuration.Short)
            if (r == SnackbarResult.ActionPerformed) onAction()
        }
    }

    LaunchedEffect(vm.messageTick) {
        val t = vm.messageText ?: return@LaunchedEffect
        message(t, vm.messageAction) { vm.continueCurrent(nextTarget(vm.currentTarget)) }
    }

    // Physical keyboard: active only while no dialog, sheet or drawer is open
    val dialogsOpen = referenceFor != null || showSetup || showNewSample || showPatient || vm.showTargetDialog ||
        showKeyboardHelp || drawerState.isOpen
    DisposableEffect(dialogsOpen) {
        val act = context as? MainActivity
        if (!dialogsOpen) act?.keyHandler = { e -> handleHardwareKey(vm, e) }
        onDispose { act?.keyHandler = null }
    }
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    fun closeThen(action: () -> Unit) { scope.launch { drawerState.close() }; action() }
    val drawer: @Composable () -> Unit = {
        AppDrawerContent(
            vm = vm,
            onSelectMode = { m -> closeThen { vm.switchMode(m) } },
            onSample = { closeThen { showPatient = true } },
            onHistory = { closeThen(onOpenHistory) },
            onKeyboard = { closeThen { showKeyboardHelp = true } },
            onSettings = { closeThen(onOpenSettings) },
            onAbout = { closeThen(onOpenAbout) },
        )
    }

    val content: @Composable (Boolean) -> Unit = { showMenu ->
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = 84.dp)) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        if (showMenu) IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open counters panel")
                        }
                    },
                    title = {
                        Column {
                            Text(mode.title(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                if (Layouts.supports(mode)) (if (vm.isExtended()) "Extended keypad" else "Simple keypad") else mode.blurb(),
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        TargetSelector(vm.currentTarget) { vm.setCurrentTarget(it) }
                        IconButton(onClick = { showNewSample = true }) { Icon(Icons.Filled.RestartAlt, contentDescription = "New sample") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            },
        ) { inner ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(inner)) {
                val landscape = maxWidth > maxHeight
                val tablet = minOf(maxWidth, maxHeight) >= 600.dp
                val gutter = if (tablet) 20.dp else 12.dp
                val chamberStage = mode == CounterMode.FLUID && (vm.fluidStage == 0 || !vm.fluidPreset.hasDifferential)

                val summary: @Composable (Modifier) -> Unit = { m ->
                    SummaryPanel(
                        vm = vm, onEditSample = { showPatient = true }, onMoreSetup = { showSetup = true },
                        onContinue = { vm.continueCurrent(nextTarget(vm.currentTarget)) },
                        large = tablet, modifier = m,
                    )
                }
                val keypad: @Composable (Modifier) -> Unit = { m ->
                    Column(m) {
                        val sections = vm.activeSections()
                        val items = sections.flatMap { it.items }
                        val rep = vm.modeReport()
                        val total = vm.currentTotal
                        val wide = landscape || tablet
                        val cols = if (mode == CounterMode.MARROW) (if (wide) 4 else 3) else keypadColumns(items.size, wide)
                        val subFor: (CountItem) -> String = { item ->
                            val n = vm.countOf(item)
                            when {
                                vm.correctionMode -> "tap −1"
                                chamberStage -> rep?.rows?.firstOrNull { it.name == item.displayName }?.extra ?: "—"
                                mode == CounterMode.PLATELET && item.countsInTotal -> rep?.rows?.firstOrNull()?.extra ?: "per field"
                                !item.countsInTotal && mode == CounterMode.BLOOD -> if (total > 0) "${fmt1(n * 100.0 / total)}/100" else "0/100"
                                !item.countsInTotal -> "tally"
                                else -> if (total > 0) "${fmt1(n * 100.0 / total)}%" else "0%"
                            }
                        }
                        val sectionInfo: (KeypadSection) -> String? = { sec ->
                            sec.title?.let { title ->
                                val members = if (mode == CounterMode.MARROW) MarrowCell.entries.filter { MarrowCell.sectionOf(it) == title } else sec.items
                                val counted = members.filter { it.countsInTotal }.sumOf { vm.countOf(it) }
                                val separate = members.filter { !it.countsInTotal }.sumOf { vm.countOf(it) }
                                if (members.none { it.countsInTotal }) "$separate tallied"
                                else "$counted · ${if (total > 0) fmt1(counted * 100.0 / total) else "0"}%"
                            }
                        }
                        SectionedKeypad(
                            sections = sections, columns = cols, minRow = if (tablet) 92.dp else 78.dp,
                            spacing = if (tablet) 10.dp else 8.dp, sectionInfo = sectionInfo,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        ) { item, mod ->
                            CountKey(
                                item = item, count = vm.countOf(item), sub = subFor(item),
                                correction = vm.correctionMode, locked = vm.currentLocked && !vm.correctionMode, large = tablet,
                                flash = vm.flashId == item.id, flashTick = vm.flashTick,
                                onTap = { vm.press(item) }, onLongPress = { referenceFor = item }, modifier = mod,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        ActionBar(
                            canUndo = vm.currentCanUndo,
                            correction = vm.correctionMode,
                            onUndo = { vm.undoActive() },
                            onToggleCorrection = { vm.correctionMode = !vm.correctionMode; snackbar.currentSnackbarData?.dismiss() },
                            setupLabel = setupLabel(mode),
                            setupIcon = when (mode) {
                                CounterMode.BLOOD -> Icons.Outlined.Bloodtype
                                CounterMode.CUSTOM -> Icons.Filled.Edit
                                else -> Icons.Filled.Tune
                            },
                            onSetup = { showSetup = true },
                            onResults = onOpenResults,
                        )
                    }
                }
                val summaryWeight = when {
                    chamberStage -> if (tablet) 0.5f else 0.56f
                    mode == CounterMode.FLUID -> 0.4f
                    mode == CounterMode.MARROW -> if (tablet) 0.3f else 0.27f
                    vm.activeItems().size <= 4 -> 0.42f
                    else -> 0.34f
                }

                if (landscape) {
                    Row(Modifier.fillMaxSize().padding(horizontal = gutter, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(gutter)) {
                        summary(Modifier.weight(0.4f).fillMaxHeight())
                        keypad(Modifier.weight(0.6f).fillMaxHeight())
                    }
                } else {
                    Column(Modifier.fillMaxSize().padding(horizontal = gutter).padding(bottom = 8.dp)) {
                        summary(Modifier.weight(summaryWeight).fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        keypad(Modifier.weight(1f - summaryWeight).fillMaxWidth())
                    }
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scheme = MaterialTheme.colorScheme
        if (maxWidth >= 900.dp) {
            PermanentNavigationDrawer(drawerContent = {
                PermanentDrawerSheet(Modifier.width(300.dp), drawerContainerColor = scheme.surfaceContainerLow) { drawer() }
            }) { content(false) }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(Modifier.width(318.dp), drawerContainerColor = scheme.surfaceContainerLow,
                        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)) { drawer() }
                },
            ) { content(true) }
        }
    }

    // ------------------------------------------------------------------ dialogs & sheets
    referenceFor?.let { cell -> ReferenceCardDialog(cell, onDismiss = { referenceFor = null }) }
    if (showSetup) {
        when (mode) {
            CounterMode.BLOOD -> MorphologySheet(vm, onDismiss = { showSetup = false })
            else -> ModeSetupSheet(vm, mode, onDismiss = { showSetup = false })
        }
    }
    if (showPatient) PatientDialog(vm, onDismiss = { showPatient = false })
    if (showKeyboardHelp) KeyboardHelpDialog(vm.activeItems()) { showKeyboardHelp = false }
    if (showNewSample) {
        AlertDialog(
            onDismissRequest = { showNewSample = false },
            icon = { Icon(Icons.Filled.RestartAlt, null) },
            title = { Text("Start a new sample?") },
            text = { Text("All counts in every counter, sample details and per-sample findings will be cleared. Settings, presets and custom keys are kept.") },
            confirmButton = {
                Button(
                    onClick = { vm.newSample(); showNewSample = false; message("Ready for a new sample") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                ) { Text("Clear and start new") }
            },
            dismissButton = { TextButton(onClick = { showNewSample = false }) { Text("Cancel") } },
        )
    }
    if (vm.showTargetDialog) {
        val next = nextTarget(vm.currentTarget)
        AlertDialog(
            onDismissRequest = { vm.showTargetDialog = false },
            icon = { Icon(Icons.Filled.Flag, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Target reached") },
            text = {
                Text("${vm.currentTotal} cells have been counted. The keypad is locked to prevent over-counting. " +
                    "You can review results, or continue counting.")
            },
            confirmButton = { Button(onClick = { vm.showTargetDialog = false; onOpenResults() }) { Text("View results") } },
            dismissButton = {
                TextButton(onClick = { vm.continueCurrent(next) }) { Text(if (next == 0) "Continue (no limit)" else "Continue to $next") }
            },
        )
    }
}

// ====================================================================== summary (top panel)
@Composable
private fun SummaryPanel(
    vm: CounterViewModel,
    onEditSample: () -> Unit,
    onMoreSetup: () -> Unit,
    onContinue: () -> Unit,
    large: Boolean,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val mode = vm.mode
    val counted = vm.currentTotal
    val target = vm.currentTarget
    val report = vm.modeReport()
    val items = vm.allItemsFor(mode).let { all -> all.filter { it in vm.activeItems() || vm.countOf(it) > 0 } }
    val chamberStage = mode == CounterMode.FLUID && (vm.fluidStage == 0 || !vm.fluidPreset.hasDifferential)
    Surface(modifier = modifier, shape = RoundedCornerShape(26.dp), color = scheme.surfaceContainerLow, contentColor = scheme.onSurface) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onEditSample, shape = RoundedCornerShape(14.dp), color = scheme.surfaceContainerHighest,
                    contentColor = scheme.onSurface, modifier = Modifier.weight(1f).height(40.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Badge, null, Modifier.size(18.dp), tint = scheme.primary)
                        Spacer(Modifier.width(8.dp))
                        val id = listOf(vm.sampleId, vm.patientName).filter { it.isNotBlank() }.joinToString("  ·  ")
                        Text(id.ifBlank { "Add sample details" }, style = MaterialTheme.typography.titleSmall,
                            color = if (id.isBlank()) scheme.onSurfaceVariant else scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (Layouts.supports(mode)) {
                    Spacer(Modifier.width(8.dp))
                    LayoutToggle(vm.isExtended(), { vm.setExtended(mode, it) }, Modifier.width(if (large) 200.dp else 164.dp))
                }
            }

            if (mode == CounterMode.FLUID && vm.fluidPreset.hasDifferential) {
                Spacer(Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(38.dp)) {
                    listOf("1  Chamber count", "2  Differential").forEachIndexed { i, l ->
                        SegmentedButton(
                            selected = vm.fluidStage == i, onClick = { vm.fluidStage = i; vm.save() },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = scheme.primaryContainer, activeContentColor = scheme.onPrimaryContainer,
                                inactiveContainerColor = scheme.surfaceContainerLow, inactiveContentColor = scheme.onSurface,
                            ),
                        ) { Text(l, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }

            if (!chamberStage) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(counted.toString(), style = CounterNumberStyle.copy(fontSize = if (large) 36.sp else 30.sp), color = scheme.onSurface)
                    Spacer(Modifier.width(6.dp))
                    Text((if (target == 0) "" else "/ $target ") + totalNoun(vm), style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 5.dp).weight(1f, fill = false),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.weight(1f))
                    StatusPill(vm)
                }
                val progress by animateFloatAsState(if (target == 0) 0f else (counted.toFloat() / target).coerceIn(0f, 1f),
                    spring(stiffness = 400f), label = "progress")
                if (target != 0) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(CircleShape),
                        color = if (vm.currentLocked) LocalClinicalColors.current.success else scheme.primary,
                        trackColor = scheme.surfaceContainerHighest, strokeCap = StrokeCap.Round, drawStopIndicator = {},
                    )
                }
            }
            AnimatedVisibility(vm.currentLocked, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                val cc = LocalClinicalColors.current
                Surface(color = cc.successContainer, contentColor = cc.onSuccessContainer, shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, null, Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                        Text("Target reached · keypad locked", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = onContinue) { Text("Continue", color = cc.onSuccessContainer) }
                    }
                }
            }

            if (report != null && report.indices.isNotEmpty() && mode != CounterMode.CUSTOM && !chamberStage) {
                Spacer(Modifier.height(10.dp))
                MetricStrip(report.indices.take(if (large) 3 else 2))
            }
            Spacer(Modifier.height(10.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (chamberStage) {
                    FluidChamberPanel(vm, onMoreSetup)
                } else BoxWithConstraints(Modifier.fillMaxSize()) {
                    val donut = minOf(maxHeight, maxWidth * 0.44f, 240.dp)
                    if (maxHeight < 96.dp) {
                        CompositionStrip(items, { vm.countOf(it) }, counted)
                    } else Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(items, { vm.countOf(it) }, counted, Modifier.size(donut))
                        Spacer(Modifier.width(16.dp))
                        Legend(items, { vm.countOf(it) }, counted, mode == CounterMode.BLOOD, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

// ====================================================================== CSF & fluid chamber panel
private data class AreaPreset(val label: String, val squares: List<String>)

private val AREA_PRESETS = listOf(
    AreaPreset("All 9 large squares", (0..8).map { "L$it" }),
    AreaPreset("4 corner squares", listOf("L0", "L2", "L6", "L8")),
    AreaPreset("4 corners + centre", listOf("L0", "L2", "L4", "L6", "L8")),
    AreaPreset("Centre square", listOf("L4")),
    AreaPreset("5 small centre squares", listOf("S0", "S4", "S12", "S20", "S24")),
)

private val DILUTIONS = listOf("1", "2", "5", "10", "20", "100", "200")

private fun dilutionLabel(d: String) = if (d == "1" || d.isBlank()) "Undiluted" else "1 in $d"

@Composable
private fun FluidChamberPanel(vm: CounterViewModel, onMoreSetup: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    val preset = vm.fluidPreset
    val squares = vm.fluidSquares
    val area = Neubauer.area(squares)
    val dilution = vm.input("fl.dilution", preset.defaultDilution.toString())
    val areaLabel = AREA_PRESETS.firstOrNull { it.squares.toSet() == squares.toSet() }?.label
        ?: if (squares.isEmpty()) "Choose squares" else "${squares.size} squares (custom)"
    val rep = vm.modeReport()

    Column(Modifier.fillMaxSize()) {
        // step 1 – choose: specimen, dilution, squares
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingChip("Specimen", preset.label, FluidPreset.entries.map { it.label }) { i -> vm.setFluidPreset(FluidPreset.entries[i]) }
            SettingChip("Dilution", dilutionLabel(dilution), DILUTIONS.map { dilutionLabel(it) } + "Other…") { i ->
                if (i < DILUTIONS.size) vm.setInput("fl.dilution", DILUTIONS[i]) else onMoreSetup()
            }
            SettingChip("Squares counted", areaLabel, AREA_PRESETS.map { it.label }) { i ->
                vm.setInput("fl.squares", AREA_PRESETS[i].squares.joinToString(","))
            }
        }
        Spacer(Modifier.height(10.dp))
        // step 2 – the chamber, step 3 – live result
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NeubauerGrid(
                selected = squares,
                smallSquareMode = preset.smallSquares || squares.any { it.startsWith("S") },
                onToggle = { vm.toggleSquare(it) },
                modifier = Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
                rep?.rows?.forEach { row ->
                    val c = when (row.flag) {
                        Flag.HIGH -> clinical.high; Flag.LOW -> clinical.low; Flag.PRESENT -> clinical.warning; Flag.NONE -> scheme.onSurface
                    }
                    Text(row.name, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1)
                    Text(row.extra ?: "—", style = CounterNumberStyle.copy(fontSize = 22.sp), color = c, maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    "${squares.size} square${if (squares.size == 1) "" else "s"} · ${String.format(java.util.Locale.US, "%.2f", area * 0.1)} µL",
                    style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                )
                Text("Tap a square to add or remove it", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
            }
        }
    }
}

@Composable
private fun SettingChip(label: String, value: String, options: List<String>, onPick: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(onClick = { open = true }, shape = RoundedCornerShape(14.dp), color = scheme.surfaceContainerHighest, contentColor = scheme.onSurface) {
            Row(Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1)
                    Text(value, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                Icon(Icons.Filled.ArrowDropDown, null, tint = scheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = scheme.surfaceContainerHigh) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(
                    text = { Text(o) },
                    onClick = { open = false; onPick(i) },
                    trailingIcon = { if (o == value) Icon(Icons.Filled.Check, null, tint = scheme.primary) },
                )
            }
        }
    }
}

// ====================================================================== keypad
/**
 * Keys in titled sections (bone marrow by progeny) or one plain grid.
 * Rows share the height; if the screen is too short the keypad scrolls instead of squeezing the keys.
 */
@Composable
fun SectionedKeypad(
    sections: List<KeypadSection>,
    columns: Int,
    minRow: Dp,
    spacing: Dp,
    sectionInfo: (KeypadSection) -> String?,
    modifier: Modifier,
    key: @Composable (CountItem, Modifier) -> Unit,
) {
    val items = sections.flatMap { it.items }
    if (items.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("No keys yet — open \"Edit keys\" to add custom counters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val titled = sections.any { it.title != null }
    val headerH = 26.dp
    BoxWithConstraints(modifier) {
        val rows = sections.sumOf { (it.items.size + columns - 1) / columns }
        val headers = if (titled) sections.size else 0
        val needed = minRow * rows + spacing * (rows + headers - 1) + headerH * headers
        val scroll = maxHeight < needed
        val colMod = if (scroll) Modifier.fillMaxWidth().verticalScroll(rememberScrollState()) else Modifier.fillMaxSize()
        Column(colMod, verticalArrangement = Arrangement.spacedBy(spacing)) {
            sections.forEach { sec ->
                if (titled && sec.title != null) SectionHeader(sec, sectionInfo(sec), maxWidth >= 560.dp, Modifier.fillMaxWidth().height(headerH))
                sec.items.chunked(columns).forEach { row ->
                    val rm = if (scroll) Modifier.fillMaxWidth().height(minRow) else Modifier.fillMaxWidth().weight(1f)
                    Row(rm, horizontalArrangement = Arrangement.spacedBy(spacing)) {
                        row.forEach { item -> key(item, Modifier.weight(1f).fillMaxHeight()) }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(sec: KeypadSection, info: String?, showSubtitle: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val accent = if (LocalClinicalColors.current.isDark) lerp(sec.accent, Color.White, 0.3f) else sec.accent
    Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 16.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(sec.title.orEmpty(), style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, maxLines = 1)
        sec.subtitle?.takeIf { showSubtitle }?.let {
            Spacer(Modifier.width(6.dp))
            Text(it, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        }
        Spacer(Modifier.weight(1f))
        info?.let {
            Text(it, style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = accent, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CountKey(
    item: CountItem,
    count: Int,
    sub: String,
    correction: Boolean,
    locked: Boolean,
    large: Boolean,
    flash: Boolean,
    flashTick: Long,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalClinicalColors.current.isDark
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.55f, stiffness = 900f), label = "press")
    val excluded = !item.countsInTotal
    val container by animateColorAsState(
        when { correction -> scheme.errorContainer; excluded -> scheme.surfaceContainer; else -> scheme.surfaceContainerHigh },
        label = "container",
    )
    val content = if (correction) scheme.onErrorContainer else scheme.onSurface
    val subContent = if (correction) scheme.onErrorContainer.copy(alpha = 0.8f) else scheme.onSurfaceVariant
    val shape = RoundedCornerShape(20.dp)
    val accent = item.uiAccent()

    // Flash (undo / keyboard): two quick pulses of the key's colour
    val flashAnim = remember { Animatable(0f) }
    LaunchedEffect(flashTick, flash) {
        if (flash && flashTick != 0L) {
            flashAnim.snapTo(1f); flashAnim.animateTo(0f, tween(260))
            flashAnim.animateTo(1f, tween(120)); flashAnim.animateTo(0f, tween(520))
        }
    }

    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (locked) 0.55f else 1f }
            .clip(shape)
            .combinedClickable(
                interactionSource = interaction, indication = ripple(color = accent),
                onClickLabel = "Count one ${item.displayName}", onLongClickLabel = "Show reference card",
                onLongClick = onLongPress, onClick = onTap,
            ),
        shape = shape, color = container, contentColor = content,
        border = if (excluded && !correction) BorderStroke(1.dp, scheme.outlineVariant) else null,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h = maxHeight
            val w = maxWidth
            val compact = h < 86.dp
            val longest = item.displayName.split(" ").maxOf { it.length }.coerceAtLeast(1)
            val badgeBg = accent.copy(alpha = if (dark) 0.22f else 0.13f)
            if (compact) {
                // small keys: [cell · %  count] on top, full-width name below
                val illus = minOf(h * 0.4f, 30.dp)
                val fit = ((w.value - 20f) / (longest * 0.57f)).coerceIn(9.5f, if (large) 14f else 12.5f)
                Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(illus).clip(CircleShape).background(badgeBg), contentAlignment = Alignment.Center) {
                            CellIllustration(item, Modifier.size(illus * 0.86f))
                        }
                        Spacer(Modifier.weight(1f))
                        Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), color = subContent, maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        CountText(count, (h.value * 0.27f).coerceIn(16f, 22f), content)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(item.displayName, style = MaterialTheme.typography.labelLarge.copy(fontSize = fit.sp, fontWeight = FontWeight.SemiBold),
                        color = content, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = (fit * 1.15f).sp)
                }
            } else {
                val illus = minOf(h * 0.44f, w * 0.40f).coerceAtLeast(24.dp)
                val fit = ((w.value - 26f) / (longest * 0.58f)).coerceIn(9.5f, if (large) 16f else 14f)
                Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        Box(Modifier.size(illus).clip(CircleShape).background(badgeBg), contentAlignment = Alignment.Center) {
                            CellIllustration(item, Modifier.size(illus * 0.86f))
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            CountText(count, (h.value * 0.2f).coerceIn(18f, if (large) 40f else 30f), content)
                            Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), color = subContent, maxLines = 1)
                        }
                    }
                    Text(item.displayName, style = MaterialTheme.typography.labelLarge.copy(fontSize = fit.sp, fontWeight = FontWeight.SemiBold),
                        color = content, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = (fit * 1.2f).sp)
                }
            }
            if (flashAnim.value > 0f) {
                Box(Modifier.matchParentSize().background(accent.copy(alpha = 0.42f * flashAnim.value))
                    .border(3.dp, accent.copy(alpha = flashAnim.value), shape))
            }
        }
    }
}

@Composable
private fun CountText(count: Int, size: Float, color: Color) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            if (targetState > initialState) (slideInVertically { it / 2 } + fadeIn()).togetherWith(slideOutVertically { -it / 2 } + fadeOut())
            else (slideInVertically { -it / 2 } + fadeIn()).togetherWith(slideOutVertically { it / 2 } + fadeOut())
        }, label = "count",
    ) { n -> Text(n.toString(), style = CounterNumberStyle.copy(fontSize = size.sp), color = color, maxLines = 1) }
}

fun nextTarget(current: Int): Int =
    TARGET_OPTIONS.filter { it != 0 && it > current }.minOrNull() ?: 0

/** Hardware keyboard. Numeric keypad mirrors a laboratory counter (7-8-9 = top row). */

private fun handleHardwareKey(vm: CounterViewModel, e: KeyEvent): Boolean {
    val numpad = mapOf(
        KeyEvent.KEYCODE_NUMPAD_7 to 0, KeyEvent.KEYCODE_NUMPAD_8 to 1, KeyEvent.KEYCODE_NUMPAD_9 to 2,
        KeyEvent.KEYCODE_NUMPAD_4 to 3, KeyEvent.KEYCODE_NUMPAD_5 to 4, KeyEvent.KEYCODE_NUMPAD_6 to 5,
        KeyEvent.KEYCODE_NUMPAD_1 to 6, KeyEvent.KEYCODE_NUMPAD_2 to 7, KeyEvent.KEYCODE_NUMPAD_3 to 8,
        KeyEvent.KEYCODE_NUMPAD_0 to 9, KeyEvent.KEYCODE_NUMPAD_DOT to 10, KeyEvent.KEYCODE_NUMPAD_ENTER to 11,
    )
    numpad[e.keyCode]?.let { return vm.hardwareKey(it) }
    val k = e.keyCode
    if (k in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9) return vm.hardwareKey(k - KeyEvent.KEYCODE_1)
    if (k == KeyEvent.KEYCODE_0) return vm.hardwareKey(9)
    LETTER_KEYS.indexOf(k).takeIf { it >= 0 }?.let { return vm.hardwareKey(10 + it) }
    return when (k) {
        KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.KEYCODE_Z -> { vm.undoActive(); true }
        KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> { vm.correctionMode = !vm.correctionMode; true }
        else -> false
    }
}

private val LETTER_KEYS = listOf(
    KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_T,
    KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_P,
    KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G,
    KeyEvent.KEYCODE_H, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
)

private val KEY_LABELS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
    "A", "S", "D", "F", "G", "H", "J", "K", "L")

private val NUMPAD_LABELS = listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "Enter")

@Composable
private fun KeyboardHelpDialog(items: List<CountItem>, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surfaceContainerHigh,
        icon = { Icon(Icons.Filled.Keyboard, null) },
        title = { Text("Keyboard shortcuts") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Connect a USB or Bluetooth keyboard / numeric keypad to count without touching the screen.",
                    style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                items.forEachIndexed { i, item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(item.displayName, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface, modifier = Modifier.weight(1f))
                        KeyCap(KEY_LABELS.getOrElse(i) { "—" })
                        if (i < NUMPAD_LABELS.size) { Spacer(Modifier.width(6.dp)); KeyCap("Num ${NUMPAD_LABELS[i]}") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { KeyCap("Backspace"); Spacer(Modifier.width(8.dp)); Text("Undo last count", color = scheme.onSurface) }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { KeyCap("−"); Spacer(Modifier.width(8.dp)); Text("Toggle correction (−1) mode", color = scheme.onSurface) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun KeyCap(t: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Text(t, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

// ====================================================================== mode chips

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
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text("Count target", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
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

// ====================================================================== summary (top panel)

@Composable
private fun StatusPill(vm: CounterViewModel) {
    val scheme = MaterialTheme.colorScheme
    val (label, bg, fg) = when {
        vm.correctionMode -> Triple("Correcting −1", scheme.errorContainer, scheme.onErrorContainer)
        vm.currentLocked -> Triple("Complete", LocalClinicalColors.current.successContainer, LocalClinicalColors.current.onSuccessContainer)
        else -> return
    }
    Surface(color = bg, contentColor = fg, shape = CircleShape) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun MetricStrip(indices: List<com.hematcount.app.model.ReportIndex>) {
    val scheme = MaterialTheme.colorScheme
    val clinical = LocalClinicalColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        indices.forEach { ix ->
            val (bg, fg) = when (ix.flag) {
                Flag.HIGH -> clinical.highContainer to clinical.onHighContainer
                Flag.LOW -> clinical.lowContainer to clinical.onLowContainer
                Flag.PRESENT -> clinical.warningContainer to clinical.onWarningContainer
                Flag.NONE -> scheme.surfaceContainerHighest to scheme.onSurface
            }
            Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(ix.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = fg.copy(alpha = 0.8f))
                    Text(ix.value, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun DonutChart(items: List<CountItem>, countOf: (CountItem) -> Int, total: Int, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val counted = items.filter { it.countsInTotal }
    val fractions = counted.map { item ->
        val f = if (total == 0) 0f else countOf(item).toFloat() / total
        animateFloatAsState(f, tween(350), label = item.id).value
    }
    val colors = counted.map { it.uiAccent() }
    val track = scheme.surfaceContainerHighest
    val centerColor = scheme.onSurface
    val subColor = scheme.onSurfaceVariant
    val gapColor = scheme.surfaceContainerLow
    Canvas(modifier.semantics { contentDescription = "Distribution chart, $total cells counted" }) {
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
        val small = measurer.measure("counted",
            TextStyle(fontSize = (size.minDimension * 0.075f / density).coerceAtLeast(9f).sp, color = subColor))
        val blockH = big.size.height + small.size.height
        drawText(big, topLeft = Offset(center.x - big.size.width / 2, center.y - blockH / 2))
        drawText(small, topLeft = Offset(center.x - small.size.width / 2, center.y - blockH / 2 + big.size.height))
    }
}

/** One stacked bar plus the leading cell types — used when the panel is too short for a donut. */

@Composable
private fun CompositionStrip(items: List<CountItem>, countOf: (CountItem) -> Int, total: Int) {
    val scheme = MaterialTheme.colorScheme
    val counted = items.filter { it.countsInTotal && countOf(it) > 0 }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(scheme.surfaceContainerHighest)) {
            if (total > 0) counted.forEach { item ->
                Box(Modifier.weight(countOf(item).toFloat().coerceAtLeast(0.0001f)).fillMaxHeight().background(item.uiAccent()))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (counted.isEmpty()) Text("Tap a key below to start counting.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            counted.sortedByDescending { countOf(it) }.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(item.uiAccent()))
                    Spacer(Modifier.width(4.dp))
                    Text("${item.displayName} ${fmt1(countOf(item) * 100.0 / total)}%", style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurface, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun Legend(items: List<CountItem>, countOf: (CountItem) -> Int, total: Int, bloodStyle: Boolean, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shown = items.filter { countOf(it) > 0 }
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center) {
        if (shown.isEmpty()) {
            Text("Tap a key below to start counting. Long-press any key for its reference card.",
                style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        shown.forEach { item ->
            val c = countOf(item)
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(if (item.countsInTotal) RoundedCornerShape(3.dp) else CircleShape).background(item.uiAccent()))
                Spacer(Modifier.width(8.dp))
                Text(item.displayName, style = MaterialTheme.typography.bodySmall,
                    color = if (item.countsInTotal) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                val value = when {
                    item.countsInTotal && total > 0 -> "${fmt1(c * 100.0 / total)}%"
                    bloodStyle && total > 0 -> "${fmt1(c * 100.0 / total)}/100"
                    else -> "$c"
                }
                Text(value, style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = if (item.countsInTotal) scheme.onSurface else scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CountItem.uiAccent(): Color =
    if (LocalClinicalColors.current.isDark) lerp(accent, Color.White, 0.22f) else accent

fun keypadColumns(n: Int, wide: Boolean): Int = when {
    n <= 2 -> 2
    n == 4 -> 2
    wide && n > 12 -> 6
    wide && n > 9 -> 4
    n > 12 -> 3
    else -> 3
}

private fun setupLabel(m: CounterMode) = when (m) {
    CounterMode.BLOOD -> "RBC · Platelets"
    CounterMode.MARROW -> "Findings"
    CounterMode.CUSTOM -> "Edit keys"
    CounterMode.RBC_MORPH -> "Guide"
    else -> "Setup"
}

private fun totalNoun(vm: CounterViewModel): String = when (vm.mode) {
    CounterMode.BLOOD -> "leucocytes"
    CounterMode.MARROW -> "nucleated cells"
    CounterMode.FLUID -> if (vm.fluidStage == 1 && vm.fluidPreset.hasDifferential) "cells in differential" else "cells in chamber"
    CounterMode.RETIC -> if (vm.input("rt.miller") == "1") "cells (Miller disc)" else "red cells"
    CounterMode.RBC_MORPH -> "red cells"
    CounterMode.PLATELET -> "platelets"
    CounterMode.VIABILITY -> "cells"
    CounterMode.CUSTOM -> "cells"
}

@Composable
private fun ActionBar(
    canUndo: Boolean,
    correction: Boolean,
    onUndo: () -> Unit,
    onToggleCorrection: () -> Unit,
    setupLabel: String,
    setupIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onSetup: () -> Unit,
    onResults: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surfaceContainerLow, contentColor = scheme.onSurface, shape = RoundedCornerShape(30.dp),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(6.dp).heightIn(min = 60.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionItem(Icons.AutoMirrored.Filled.Undo, "Undo", enabled = canUndo, onClick = onUndo)
            ActionItem(Icons.Outlined.RemoveCircleOutline, if (correction) "Correcting" else "Correct −1",
                container = if (correction) scheme.error else Color.Transparent,
                content = if (correction) scheme.onError else scheme.onSurface, onClick = onToggleCorrection)
            ActionItem(setupIcon, setupLabel, onClick = onSetup)
            ActionItem(Icons.Filled.Assessment, "Results", container = scheme.primary, content = scheme.onPrimary, onClick = onResults)
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
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(22.dp), color = container, contentColor = fg,
        modifier = Modifier.weight(1f).height(60.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = fg)
            Spacer(Modifier.height(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
