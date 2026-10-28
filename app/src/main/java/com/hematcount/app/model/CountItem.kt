package com.hematcount.app.model

import androidx.compose.ui.graphics.Color

/** Anything that can sit on a counter keypad (blood, marrow or fluid cell). */
interface CountItem {
    /** Unique, stable id (used for saving counts). */
    val id: String
    val displayName: String
    val group: String
    val accent: Color
    /** false = tallied but excluded from the percentage total (e.g. nucleated red cells, megakaryocytes). */
    val countsInTotal: Boolean
    /** Which picture to draw (see CellIllustration). */
    val drawId: String
    /** Normal range text for reference cards, e.g. "40 – 80 %". */
    val normalText: String
    val reference: MorphologyReference?
}

/** Counter modes offered on the main screen. */
enum class CounterMode(val label: String, val short: String, val defaultTarget: Int, val reportTitle: String) {
    BLOOD("Peripheral blood differential", "Blood", 100, "Manual Differential Leucocyte Count"),
    MARROW("Bone marrow differential", "Bone marrow", 500, "Bone Marrow Aspirate Differential Count"),
    FLUID("CSF & body fluid (Neubauer)", "CSF · Fluids", 0, "CSF / Body Fluid Cell Count"),
    RETIC("Reticulocyte count", "Reticulocytes", 1000, "Reticulocyte Count"),
    RBC_MORPH("Red cell abnormality count", "RBC morphology", 1000, "Red Cell Morphology Count"),
    PLATELET("Platelet estimate (smear)", "Platelets", 0, "Platelet Estimate from Peripheral Smear"),
    VIABILITY("Cell viability (trypan blue)", "Viability", 0, "Cell Viability Count (Trypan Blue)"),
    CUSTOM("Custom counter", "Custom", 100, "Cell Count"),
}
