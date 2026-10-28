package com.hematcount.app.model

import androidx.compose.ui.graphics.Color

/** A plain keypad item used by the fluid, reticulocyte, red-cell, platelet, viability and custom modes. */
data class SimpleItem(
    override val id: String,
    override val displayName: String,
    override val group: String,
    override val accent: Color,
    override val drawId: String,
    override val countsInTotal: Boolean = true,
    override val normalText: String = "",
    override val reference: MorphologyReference? = null,
) : CountItem

// =====================================================================================
// CSF & BODY FLUIDS — Neubauer (improved) haemocytometer
// Large square = 1 mm × 1 mm; depth 0.1 mm → 0.1 µL per large square.
// Count / µL = cells counted × dilution ÷ (area counted in mm² × 0.1 mm)
// =====================================================================================

enum class FluidPreset(
    val label: String,
    val specimen: String,
    val defaultDilution: Int,
    val defaultSquares: List<String>,
    /** true = the central square is used as 25 small squares (red cell count). */
    val smallSquares: Boolean,
    val chamberKeys: List<String>,
    val hasDifferential: Boolean,
    val isBlood: Boolean,
) {
    CSF("CSF", "Cerebrospinal fluid", 1, ALL_LARGE, false, listOf("NUCLEATED", "RBC"), true, false),
    PLEURAL("Pleural", "Pleural fluid", 1, ALL_LARGE, false, listOf("NUCLEATED", "RBC"), true, false),
    PERITONEAL("Peritoneal", "Peritoneal (ascitic) fluid", 1, ALL_LARGE, false, listOf("NUCLEATED", "RBC"), true, false),
    PERICARDIAL("Pericardial", "Pericardial fluid", 1, ALL_LARGE, false, listOf("NUCLEATED", "RBC"), true, false),
    SYNOVIAL("Synovial", "Synovial fluid", 1, ALL_LARGE, false, listOf("NUCLEATED", "RBC"), true, false),
    BLOOD_WBC("Blood WBC", "Blood — total leucocyte count", 20, listOf("L0", "L2", "L6", "L8"), false, listOf("LEUCOCYTES"), false, true),
    BLOOD_RBC("Blood RBC", "Blood — red cell count", 200, listOf("S0", "S4", "S12", "S20", "S24"), true, listOf("RBC"), false, true),
    BLOOD_PLT("Blood platelets", "Blood — platelet count", 100, listOf("L4"), false, listOf("PLATELETS"), false, true),
    ;
}

private val ALL_LARGE = (0..8).map { "L$it" }

object Neubauer {
    const val DEPTH_MM = 0.1
    const val SMALL_AREA = 0.04 // one of the 25 central squares (0.2 mm × 0.2 mm)

    /** Area in mm² of the selected squares. Small squares only count when the centre square itself is not selected. */
    fun area(selected: Collection<String>): Double {
        val large = selected.count { it.startsWith("L") }
        val centreWhole = "L4" in selected
        val small = if (centreWhole) 0 else selected.count { it.startsWith("S") }
        return large * 1.0 + small * SMALL_AREA
    }

    fun perMicrolitre(count: Int, dilution: Double, areaMm2: Double): Double? =
        if (areaMm2 <= 0 || dilution <= 0) null else count * dilution / (areaMm2 * DEPTH_MM)
}

object FluidItems {
    val NUCLEATED = SimpleItem("FL_NUCLEATED", "Nucleated Cells", "Chamber count · all leucocytes and other nucleated cells",
        Color(0xFF7C3AED), "NUCLEATED", normalText = "CSF adult 0 – 5 /µL",
        reference = MorphologyReference(
            size = "7 – 20 µm",
            nucleus = "Count every cell with a visible nucleus; acetic acid diluent lyses red cells and sharpens nuclei.",
            cytoplasm = "In an unstained chamber, leucocytes appear refractile and granular.",
            staining = "Unstained or Türk / crystal-violet diluent; differential on a stained cytospin.",
            increasedIn = "CSF: meningitis (bacterial — neutrophils; viral / tuberculous / fungal — lymphocytes), encephalitis, CNS malignancy. Serous fluids: infection, inflammation, malignancy.",
            decreasedIn = "Normal adult CSF 0 – 5 /µL (neonates up to 30 /µL).",
        ))
    val RBC = SimpleItem("FL_RBC", "Red Cells", "Chamber count", Color(0xFFE11D48), "RBC", normalText = "CSF 0 /µL",
        reference = MorphologyReference(
            size = "7 – 8 µm",
            nucleus = "Anucleate; appear as smooth pale discs (crenated in older or hypertonic specimens).",
            cytoplasm = "Haemoglobinised, no granules.",
            staining = "Unstained; red cells are lysed by acetic-acid diluents, so count them on a saline dilution.",
            increasedIn = "Traumatic tap (clearing from tube 1 to tube 3), subarachnoid haemorrhage (xanthochromia, uniform in all tubes), haemorrhagic effusions (malignancy, trauma, pulmonary infarction).",
            decreasedIn = "Normal CSF contains no red cells.",
        ))
    val LEUCOCYTES = SimpleItem("FL_LEUCOCYTES", "Leucocytes", "Four corner squares · 1 in 20", Color(0xFF2563EB), "NEUTROPHIL",
        normalText = "4.0 – 10.0 ×10³/µL", reference = CellType.NEUTROPHIL.reference)
    val PLATELETS = SimpleItem("FL_PLATELETS", "Platelets", "Central square · 1 in 100", Color(0xFF8B5CF6), "PLATELET",
        normalText = "150 – 410 ×10³/µL")

    fun chamberItems(p: FluidPreset): List<SimpleItem> = p.chamberKeys.map {
        when (it) { "RBC" -> RBC; "LEUCOCYTES" -> LEUCOCYTES; "PLATELETS" -> PLATELETS; else -> NUCLEATED }
    }

    fun differentialItems(p: FluidPreset): List<SimpleItem> = listOf(
        SimpleItem("FD_NEUTROPHIL", "Neutrophils", "Fluid differential", Color(0xFF2563EB), "NEUTROPHIL",
            normalText = if (p == FluidPreset.CSF) "0 – 6 %" else "", reference = CellType.NEUTROPHIL.reference),
        SimpleItem("FD_LYMPHOCYTE", "Lymphocytes", "Fluid differential", Color(0xFF7C3AED), "LYMPHOCYTE",
            normalText = if (p == FluidPreset.CSF) "40 – 80 %" else "", reference = CellType.LYMPHOCYTE.reference),
        SimpleItem("FD_MONO_MACRO", "Monocytes & Macrophages", "Fluid differential", Color(0xFF0D9488), "MACROPHAGE",
            normalText = if (p == FluidPreset.CSF) "15 – 45 %" else "", reference = MarrowCell.MACROPHAGE.reference),
        SimpleItem("FD_EOSINOPHIL", "Eosinophils", "Fluid differential", Color(0xFFF97316), "EOSINOPHIL",
            normalText = "Rare", reference = CellType.EOSINOPHIL.reference),
        if (p == FluidPreset.CSF)
            SimpleItem("FD_EPENDYMAL", "Ependymal & Choroid Cells", "Lining cells", Color(0xFF0EA5E9), "EPENDYMAL",
                normalText = "Occasional", reference = MorphologyReference(
                    size = "10 – 20 µm, often in small clusters",
                    nucleus = "Round to oval, uniform, fine chromatin.",
                    cytoplasm = "Moderate, pale grey-blue; choroid plexus cells may show cilia or granules.",
                    staining = "Romanowsky on cytospin.",
                    increasedIn = "Ventricular taps, shunts, neonates, after neurosurgery; must not be mistaken for malignant cells.",
                    decreasedIn = "Usually few or absent in lumbar CSF.",
                ))
        else
            SimpleItem("FD_MESOTHELIAL", "Mesothelial Cells", "Lining cells", Color(0xFF0EA5E9), "MESOTHELIAL",
                normalText = "Present", reference = MorphologyReference(
                    size = "12 – 30 µm; singly, in pairs or small \"windowed\" clusters",
                    nucleus = "Central, round, smooth membrane, small nucleolus; binucleate forms common.",
                    cytoplasm = "Dense, grey-blue with a pale peripheral fringe (microvilli).",
                    staining = "Romanowsky; calretinin / WT1 positive.",
                    increasedIn = "Reactive effusions (inflammation, infarction, cirrhosis). Reduced in tuberculous effusions.",
                    decreasedIn = "Scanty in tuberculous pleuritis and rheumatoid effusions.",
                )),
        SimpleItem("FD_ATYPICAL", "Atypical / Malignant Cells", "Report and refer for cytology", Color(0xFFDC2626), "OTHER",
            normalText = "Not present", reference = MarrowCell.OTHER.reference),
    )
}

enum class FluidAppearance(val label: String) {
    NOT_RECORDED("Not recorded"), CLEAR("Clear, colourless"), TURBID("Turbid"), BLOOD_STAINED("Blood-stained"),
    XANTHOCHROMIC("Xanthochromic"), CHYLOUS("Milky / chylous"), PURULENT("Purulent"),
}

/** General guidance figures (adults) quoted on the fluid report; interpret with biochemistry. */
fun fluidNormalNucleated(p: FluidPreset): String = when (p) {
    FluidPreset.CSF -> "0 – 5 /µL (neonates 0 – 30 /µL)"
    FluidPreset.PLEURAL -> "< 1000 /µL"
    FluidPreset.PERITONEAL -> "< 500 /µL; neutrophils < 250 /µL"
    FluidPreset.PERICARDIAL -> "< 1000 /µL"
    FluidPreset.SYNOVIAL -> "< 200 /µL; neutrophils < 25 %"
    FluidPreset.BLOOD_WBC -> "4.0 – 10.0 ×10³/µL"
    FluidPreset.BLOOD_RBC -> "M 4.5 – 5.5 · F 3.8 – 4.8 ×10⁶/µL"
    FluidPreset.BLOOD_PLT -> "150 – 410 ×10³/µL"
}

// =====================================================================================
// RETICULOCYTES (supravital new methylene blue / brilliant cresyl blue)
// =====================================================================================
object RetItems {
    private val retRef = MorphologyReference(
        size = "8 – 9 µm, slightly larger than mature red cells",
        nucleus = "Anucleate. Supravital stain precipitates residual ribosomal RNA as a blue reticulum or dots; count any red cell with two or more blue granules.",
        cytoplasm = "Pale green-blue background with the blue reticular network; polychromatic on Romanowsky films.",
        staining = "New methylene blue or brilliant cresyl blue, incubated with blood before spreading.",
        increasedIn = "Haemolysis, acute blood loss, response to iron / B12 / folate therapy, recovery from marrow suppression.",
        decreasedIn = "Aplastic anaemia, pure red cell aplasia, untreated nutritional anaemia, renal failure, marrow infiltration. Normal adults 0.5 – 2.5 %.",
    )
    val RETIC = SimpleItem("RT_RETIC", "Reticulocytes", "Supravital stain", Color(0xFF2563EB), "RETIC", normalText = "0.5 – 2.5 %", reference = retRef)
    val RED = SimpleItem("RT_RBC", "Mature Red Cells", "Non-reticulated", Color(0xFFE11D48), "RBC", normalText = "")
    // Miller disc: retics counted in the large square, red cells in the small square (1/9 of the area)
    val MILLER_RETIC = SimpleItem("RT_M_RETIC", "Reticulocytes (large square)", "Miller disc", Color(0xFF2563EB), "RETIC", normalText = "0.5 – 2.5 %", reference = retRef)
    val MILLER_RED = SimpleItem("RT_M_RBC", "Red Cells (small square)", "Miller disc · includes reticulocytes", Color(0xFFE11D48), "RBC")
    fun items(miller: Boolean) = if (miller) listOf(MILLER_RETIC, MILLER_RED) else listOf(RETIC, RED)
}

// =====================================================================================
// RED CELL ABNORMALITIES — percentage of red cells counted
// =====================================================================================
object RbcItems {
    private fun ref(size: String, desc: String, seen: String) = MorphologyReference(
        size = size, nucleus = "Anucleate.", cytoplasm = desc,
        staining = "Romanowsky; assess in the monolayer where red cells just touch.",
        increasedIn = seen, decreasedIn = "Express as a percentage of red cells counted (count at least 1000 red cells).",
    )
    val items: List<SimpleItem> = listOf(
        SimpleItem("RB_NORMAL", "Normal Red Cells", "Normocytes", Color(0xFF94A3B8), "RBC"),
        SimpleItem("RB_SCHISTO", "Schistocytes", "Fragmented cells", Color(0xFFDC2626), "SCHISTOCYTE", normalText = "< 1 %",
            reference = ref("Smaller than normal red cells", "Fragments with sharp angles or straight borders: helmet cells, triangles, keratocytes, microspherocytes.",
                "Thrombotic microangiopathies (TTP, HUS), DIC, malignant hypertension, mechanical heart valves, severe burns. ICSH: > 1% is clinically significant.")),
        SimpleItem("RB_SPHERO", "Spherocytes", "Dense, no central pallor", Color(0xFFBE123C), "SPHEROCYTE",
            reference = ref("Smaller diameter, increased thickness", "Round, densely staining, no central pallor.",
                "Hereditary spherocytosis, warm autoimmune haemolytic anaemia, burns, ABO haemolytic disease of the newborn, clostridial sepsis.")),
        SimpleItem("RB_TARGET", "Target Cells", "Codocytes", Color(0xFFF97316), "TARGET",
            reference = ref("Normal or thin", "Central dense spot of haemoglobin within the pale zone (bull's-eye).",
                "Thalassaemias, haemoglobin C / E disease, obstructive liver disease, post-splenectomy, iron deficiency.")),
        SimpleItem("RB_ELLIPTO", "Elliptocytes & Ovalocytes", "Oval / elongated", Color(0xFF0D9488), "ELLIPTOCYTE",
            reference = ref("Normal size", "Oval to elongated cells with parallel sides.",
                "Hereditary elliptocytosis, iron deficiency (pencil cells), thalassaemia, myelofibrosis, megaloblastic anaemia (macro-ovalocytes).")),
        SimpleItem("RB_DACRO", "Tear-drop Cells", "Dacrocytes", Color(0xFF7C3AED), "TEARDROP",
            reference = ref("Normal or small", "Single tapered tail giving a tear-drop shape.",
                "Primary myelofibrosis, marrow infiltration (leucoerythroblastic picture), thalassaemia, megaloblastic anaemia.")),
        SimpleItem("RB_SICKLE", "Sickle Cells", "Drepanocytes", Color(0xFF9F1239), "SICKLE",
            reference = ref("Elongated, crescent", "Crescent or boat-shaped cells with pointed ends.",
                "Sickle cell anaemia and compound heterozygous states (HbSC, HbS/β-thalassaemia).")),
        SimpleItem("RB_ECHINO", "Echinocytes (Burr Cells)", "Regular short projections", Color(0xFF65A30D), "ECHINOCYTE",
            reference = ref("Normal size", "10 – 30 short, evenly spaced projections around the circumference.",
                "Uraemia, pyruvate kinase deficiency, liver disease; also a storage / drying artefact.")),
        SimpleItem("RB_ACANTHO", "Acanthocytes", "Irregular spicules", Color(0xFFA16207), "ACANTHOCYTE",
            reference = ref("Small, dense", "2 – 20 irregular spicules of varying length, no central pallor.",
                "Severe liver disease (spur cell anaemia), abetalipoproteinaemia, McLeod phenotype, post-splenectomy.")),
        SimpleItem("RB_STOMATO", "Stomatocytes", "Slit-like pallor", Color(0xFF0EA5E9), "STOMATOCYTE",
            reference = ref("Normal size", "Central pallor replaced by a straight mouth-like slit.",
                "Hereditary stomatocytosis, alcoholism and liver disease, Rh-null phenotype; can be artefactual.")),
        SimpleItem("RB_BITE", "Bite & Blister Cells", "Oxidant damage", Color(0xFFE879F9), "BITECELL",
            reference = ref("Normal size", "Semicircular \"bites\" or blister-like peripheral clear vacuoles.",
                "G6PD deficiency and other oxidant haemolysis (dapsone, primaquine, naphthalene).")),
        SimpleItem("RB_OTHER", "Other Abnormal Red Cells", "Specify in remarks", Color(0xFF78716C), "RBC_OTHER",
            reference = ref("Variable", "Use for macrocytes, microcytes, irregularly contracted cells, knizocytes or inclusions (Howell-Jolly, basophilic stippling, Pappenheimer bodies, parasites).",
                "Describe in remarks.")),
    )
}

// =====================================================================================
// PLATELET ESTIMATE on smear — platelets in successive oil-immersion fields
// =====================================================================================
object PlateletItems {
    val PLATELET = SimpleItem("PL_PLATELET", "Platelets", "Tap for each platelet in the field", Color(0xFF8B5CF6), "PLATELET",
        normalText = "7 – 21 per oil-immersion field", reference = MorphologyReference(
            size = "1.5 – 3 µm; giant platelets ≥ size of a red cell",
            nucleus = "Anucleate cytoplasmic fragments of megakaryocytes.",
            cytoplasm = "Pale blue with central purple granules (granulomere).",
            staining = "Romanowsky; count in the monolayer under oil immersion (×100).",
            increasedIn = "Reactive thrombocytosis (infection, iron deficiency, malignancy, post-splenectomy), essential thrombocythaemia and other myeloproliferative neoplasms.",
            decreasedIn = "Immune thrombocytopenia, DIC, TTP, marrow failure, hypersplenism; always exclude platelet clumps and EDTA pseudothrombocytopenia.",
        ))
    val FIELD = SimpleItem("PL_FIELD", "Field Completed", "Tap after each oil-immersion field", Color(0xFF0D9488), "FIELD", countsInTotal = false)
    val CLUMP = SimpleItem("PL_CLUMP", "Platelet Clumps", "Tallied separately", Color(0xFFDC2626), "PLATELET_CLUMP", countsInTotal = false,
        normalText = "None")
    val GIANT = SimpleItem("PL_GIANT", "Giant Platelets", "Tallied separately", Color(0xFFF97316), "GIANT_PLATELET", countsInTotal = false,
        normalText = "Occasional")
    val items = listOf(PLATELET, FIELD, CLUMP, GIANT)
}

// =====================================================================================
// CELL VIABILITY — trypan blue dye exclusion on the Neubauer chamber
// =====================================================================================
object ViabilityItems {
    val LIVE = SimpleItem("VB_LIVE", "Viable Cells", "Unstained, bright", Color(0xFF16A34A), "VIABLE")
    val DEAD = SimpleItem("VB_DEAD", "Non-viable Cells", "Blue-stained", Color(0xFF1D4ED8), "NONVIABLE")
    val items = listOf(LIVE, DEAD)
}

// =====================================================================================
// CUSTOM counters
// =====================================================================================
/** Icons the user can choose for custom counter keys. */
val CUSTOM_ICON_CHOICES: List<Pair<String, String>> = listOf(
    "NEUTROPHIL" to "Neutrophil", "LYMPHOCYTE" to "Lymphocyte", "MONOCYTE" to "Monocyte", "EOSINOPHIL" to "Eosinophil",
    "BASOPHIL" to "Basophil", "BLAST" to "Blast", "PLASMA" to "Plasma cell", "NRBC" to "Normoblast",
    "RBC" to "Red cell", "PLATELET" to "Platelet", "MACROPHAGE" to "Macrophage", "MEGAKARYOCYTE" to "Megakaryocyte",
    "MESOTHELIAL" to "Mesothelial", "OTHER" to "Atypical cell", "SCHISTOCYTE" to "Fragment", "SPERM" to "Sperm",
    "YEAST" to "Yeast / fungus", "CRYSTAL" to "Crystal", "CAST" to "Cast", "BACTERIA" to "Bacteria",
)

val CUSTOM_COLOUR_CHOICES: List<Long> = listOf(
    0xFF2563EB, 0xFF0EA5E9, 0xFF0D9488, 0xFF16A34A, 0xFF65A30D, 0xFFD97706,
    0xFFF97316, 0xFFDC2626, 0xFFE11D48, 0xFFEC4899, 0xFFA21CAF, 0xFF7C3AED, 0xFF4F46E5, 0xFF64748B,
)

fun defaultCustomItems(): List<SimpleItem> = listOf(
    SimpleItem("CU_1", "Cell type A", "Custom", Color(0xFF2563EB), "NEUTROPHIL"),
    SimpleItem("CU_2", "Cell type B", "Custom", Color(0xFF7C3AED), "LYMPHOCYTE"),
    SimpleItem("CU_3", "Cell type C", "Custom", Color(0xFF0D9488), "MONOCYTE"),
)
