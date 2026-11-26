package com.hematcount.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hematcount.app.BuildConfigInfo
import com.hematcount.app.CounterViewModel
import com.hematcount.app.R
import com.hematcount.app.model.CounterMode
import com.hematcount.app.model.Layouts

/** Representative cell drawing for each counter in the side panel. */
fun CounterMode.iconCell(): String = when (this) {
    CounterMode.BLOOD -> "NEUTROPHIL"
    CounterMode.MARROW -> "MEGAKARYOCYTE"
    CounterMode.FLUID -> "NUCLEATED"
    CounterMode.RETIC -> "RETIC"
    CounterMode.RBC_MORPH -> "TARGET"
    CounterMode.PLATELET -> "PLATELET"
    CounterMode.VIABILITY -> "VIABLE"
    CounterMode.CUSTOM -> "OTHER"
}

/** Short name for the title bar and the side panel. */
fun CounterMode.title(): String = when (this) {
    CounterMode.BLOOD -> "Blood differential"
    CounterMode.MARROW -> "Bone marrow"
    CounterMode.FLUID -> "CSF & body fluids"
    CounterMode.RETIC -> "Reticulocytes"
    CounterMode.RBC_MORPH -> "Red cell morphology"
    CounterMode.PLATELET -> "Platelet estimate"
    CounterMode.VIABILITY -> "Cell viability"
    CounterMode.CUSTOM -> "Custom counter"
}

/** Short description shown under each counter in the side panel. */
fun CounterMode.blurb(): String = when (this) {
    CounterMode.BLOOD -> "Leucocyte differential"
    CounterMode.MARROW -> "Myelogram by progeny · M:E ratio"
    CounterMode.FLUID -> "Neubauer chamber · cells/µL"
    CounterMode.RETIC -> "Supravital stain · RPI"
    CounterMode.RBC_MORPH -> "Poikilocytes · schistocytes"
    CounterMode.PLATELET -> "Per oil-immersion field"
    CounterMode.VIABILITY -> "Trypan blue · % viable"
    CounterMode.CUSTOM -> "Your own keys"
}

val BrandGradient = Brush.linearGradient(listOf(Color(0xFF0FA3A3), Color(0xFF0B6E8E), Color(0xFF1E2A78)))

/** The app icon (adaptive-icon layers) drawn inside a circle. */
@Composable
fun AppIconBadge(size: androidx.compose.ui.unit.Dp) {
    Box(Modifier.size(size).clip(CircleShape), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(size * 1.5f))
    }
}

@Composable
fun ModeIcon(mode: CounterMode, selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(34.dp).clip(CircleShape)
            .background(if (selected) scheme.surface.copy(alpha = 0.7f) else scheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val id = mode.iconCell()
        Canvas(Modifier.size(26.dp)) { drawCellId(id) }
    }
}

@Composable
fun AppDrawerContent(
    vm: CounterViewModel,
    onSelectMode: (CounterMode) -> Unit,
    onSample: () -> Unit,
    onHistory: () -> Unit,
    onKeyboard: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val itemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = scheme.secondaryContainer,
        unselectedContainerColor = Color.Transparent,
        selectedTextColor = scheme.onSecondaryContainer,
        unselectedTextColor = scheme.onSurface,
        selectedIconColor = scheme.onSecondaryContainer,
        unselectedIconColor = scheme.onSurfaceVariant,
        selectedBadgeColor = scheme.onSecondaryContainer,
        unselectedBadgeColor = scheme.onSurfaceVariant,
    )
    Column(
        Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        // ---------------- brand header
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(BrandGradient).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    AppIconBadge(52.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("HematCount", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Precision in Every Cell", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }

        DrawerLabel("Counters")
        CounterMode.entries.forEach { m ->
            val selected = vm.mode == m
            val n = vm.modeTotal(m)
            NavigationDrawerItem(
                label = {
                    Column {
                        Text(m.title(), style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(m.blurb(), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (selected) scheme.onSecondaryContainer.copy(alpha = 0.75f) else scheme.onSurfaceVariant)
                    }
                },
                icon = { ModeIcon(m, selected) },
                badge = {
                    if (n > 0) Surface(
                        color = if (selected) scheme.onSecondaryContainer.copy(alpha = 0.12f) else scheme.surfaceContainerHighest,
                        contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
                        shape = CircleShape,
                    ) { Text("$n", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) }
                },
                selected = selected,
                onClick = { onSelectMode(m) },
                colors = itemColors,
                modifier = Modifier.height(60.dp),
            )
        }

        if (Layouts.supports(vm.mode)) {
            DrawerLabel("Keypad · ${vm.mode.title()}")
            LayoutToggle(vm.isExtended(), { vm.setExtended(vm.mode, it) }, Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            Text(
                if (vm.isExtended()) "All cell types, stage by stage." else "Essential cell types only — quicker counting.",
                style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        HorizontalDivider(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), color = scheme.outlineVariant)
        DrawerAction(Icons.Filled.Badge, "Sample details", onSample, itemColors)
        DrawerAction(Icons.Filled.History, "History & backup", onHistory, itemColors)
        DrawerAction(Icons.Filled.Keyboard, "Keyboard shortcuts", onKeyboard, itemColors)
        DrawerAction(Icons.Filled.Settings, "Settings", onSettings, itemColors)
        DrawerAction(Icons.Filled.Info, "About", onAbout, itemColors)
        Spacer(Modifier.height(12.dp))
        Text("Version ${BuildConfigInfo.versionName(context)}", style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun DrawerLabel(t: String) {
    Text(t.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp))
}

@Composable
private fun DrawerAction(icon: ImageVector, label: String, onClick: () -> Unit, colors: androidx.compose.material3.NavigationDrawerItemColors) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, null) },
        selected = false, onClick = onClick, colors = colors,
    )
}

/** Two-segment "Simple | Extended" switch. */
@Composable
fun LayoutToggle(extended: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.surfaceContainerHighest, shape = CircleShape, modifier = modifier.height(36.dp)) {
        Row(Modifier.fillMaxSize().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(false to "Simple", true to "Extended").forEach { (value, label) ->
                val sel = value == extended
                Surface(
                    onClick = { onChange(value) },
                    color = if (sel) scheme.primary else Color.Transparent,
                    contentColor = if (sel) scheme.onPrimary else scheme.onSurfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
                        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}
