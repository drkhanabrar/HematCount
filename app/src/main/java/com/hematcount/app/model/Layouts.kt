package com.hematcount.app.model

import androidx.compose.ui.graphics.Color

/** A titled group of keys on the keypad (used to group bone-marrow cells by progeny). */
data class KeypadSection(
    val title: String?,
    val subtitle: String?,
    val accent: Color,
    val items: List<CountItem>,
)

/**
 * Simple / extended keypads. The choice is stored per mode in the mode inputs as
 * "layout.<MODE>" = "simple" | "extended", so saved reports remember it.
 */
object Layouts {
    fun supports(m: CounterMode): Boolean = when (m) {
        CounterMode.BLOOD, CounterMode.MARROW, CounterMode.FLUID, CounterMode.RBC_MORPH, CounterMode.PLATELET -> true
        else -> false
    }

    fun key(m: CounterMode) = "layout.${m.name}"

    fun isExtended(m: CounterMode, inputs: Map<String, String>): Boolean =
        !supports(m) || inputs[key(m)] != "simple"

    // ---------------- peripheral blood
    val BLOOD_SIMPLE: List<CellType> = listOf(
        CellType.NEUTROPHIL, CellType.LYMPHOCYTE, CellType.MONOCYTE,
        CellType.EOSINOPHIL, CellType.BASOPHIL, CellType.NRBC,
    )

    fun bloodItems(extended: Boolean): List<CellType> = if (extended) CellType.keypadOrder else BLOOD_SIMPLE

    // ---------------- fluid differential
    private val FLUID_SIMPLE_IDS = setOf("FD_NEUTROPHIL", "FD_LYMPHOCYTE", "FD_MONO_MACRO", "FD_ATYPICAL")

    fun fluidDiff(p: FluidPreset, extended: Boolean): List<SimpleItem> =
        FluidItems.differentialItems(p).filter { extended || it.id in FLUID_SIMPLE_IDS }

    // ---------------- red cells
    private val RBC_SIMPLE_IDS = setOf("RB_NORMAL", "RB_SCHISTO", "RB_SPHERO", "RB_TARGET", "RB_DACRO", "RB_OTHER")

    fun rbcItems(extended: Boolean): List<SimpleItem> = RbcItems.items.filter { extended || it.id in RBC_SIMPLE_IDS }

    // ---------------- platelets
    fun plateletItems(extended: Boolean): List<SimpleItem> =
        if (extended) PlateletItems.items else listOf(PlateletItems.PLATELET, PlateletItems.FIELD)

    /** Keep an item in reports if it is on the keypad or has been counted. */
    fun <T : CountItem> visible(all: List<T>, onKeypad: List<CountItem>, count: (T) -> Int): List<T> {
        val ids = onKeypad.map { it.id }.toSet()
        return all.filter { it.id in ids || count(it) > 0 }
    }
}
