package com.hematcount.app.model

import androidx.compose.ui.graphics.Color

/**
 * The 12-key peripheral blood differential keypad.
 * Order = keypad order (row by row, left to right).
 *
 * Reference intervals (adults) follow Dacie & Lewis Practical Haematology, 12th ed.,
 * Table 2.1. Absolute counts are expressed as ×10³/µL (identical to ×10⁹/L).
 */
enum class CellType(
    val displayName: String,
    val group: String,
    val accent: Color,
    val countsInWbc: Boolean,
    val refPercent: String,
    val refPercentLow: Double?,
    val refPercentHigh: Double?,
    val refAbsolute: String,
    val refAbsLow: Double?,
    val refAbsHigh: Double?,
    val reference: MorphologyReference,
) {
    NEUTROPHIL(
        displayName = "Neutrophils",
        group = "Segmented · Mature granulocyte",
        accent = Color(0xFF2563EB),
        countsInWbc = true,
        refPercent = "40 – 80", refPercentLow = 40.0, refPercentHigh = 80.0,
        refAbsolute = "2.0 – 7.0", refAbsLow = 2.0, refAbsHigh = 7.0,
        reference = MorphologyReference(
            size = "12 – 15 µm",
            nucleus = "2 – 5 lobes joined by fine chromatin threads; coarse, clumped chromatin; no nucleoli. More than 5% of cells with 5 lobes, or any with 6 or more, is hypersegmentation.",
            cytoplasm = "Abundant, pale pink to lilac, packed with fine neutrophilic (specific) granules.",
            staining = "Romanowsky (Leishman / May-Grünwald-Giemsa): nucleus deep purple, granules fine lilac-pink, barely resolved by light microscopy.",
            increasedIn = "Acute bacterial infection, inflammation and tissue necrosis, haemorrhage, corticosteroid therapy, pregnancy, chronic myeloid leukaemia and other myeloproliferative neoplasms.",
            decreasedIn = "Viral infections, drugs and chemotherapy, aplastic anaemia, megaloblastic anaemia, hypersplenism, immune neutropenia, ethnic (benign) neutropenia.",
        ),
    ),
    BAND(
        displayName = "Band Neutrophils",
        group = "Band form · Mature granulocyte",
        accent = Color(0xFF0EA5E9),
        countsInWbc = true,
        refPercent = "0 – 5", refPercentLow = null, refPercentHigh = 5.0,
        refAbsolute = "—", refAbsLow = null, refAbsHigh = null,
        reference = MorphologyReference(
            size = "10 – 14 µm",
            nucleus = "Curved, U- or C-shaped band of uniform width without true segmentation; any constriction is less than one-third of the nuclear width.",
            cytoplasm = "Pink with fine specific granules, as in the segmented neutrophil.",
            staining = "Romanowsky: deep purple nucleus with condensed chromatin; lilac-pink granular cytoplasm.",
            increasedIn = "Left shift in acute bacterial infection and sepsis, burns, post-operative states, G-CSF therapy, chronic myeloid leukaemia.",
            decreasedIn = "Not clinically significant when low.",
        ),
    ),
    EOSINOPHIL(
        displayName = "Eosinophils",
        group = "Mature granulocyte",
        accent = Color(0xFFF97316),
        countsInWbc = true,
        refPercent = "1 – 6", refPercentLow = 1.0, refPercentHigh = 6.0,
        refAbsolute = "0.02 – 0.5", refAbsLow = 0.02, refAbsHigh = 0.5,
        reference = MorphologyReference(
            size = "12 – 17 µm",
            nucleus = "Usually bilobed (\"spectacle\" form), chromatin less coarse than the neutrophil.",
            cytoplasm = "Filled with large, uniform, refractile orange-red (eosinophilic) granules.",
            staining = "Romanowsky: granules stain bright orange-red with eosin; nucleus purple.",
            increasedIn = "Allergic disorders and asthma, helminthic infestation, drug hypersensitivity, skin disease, Hodgkin lymphoma, hypereosinophilic syndrome, chronic eosinophilic leukaemia, Addison disease.",
            decreasedIn = "Acute stress, corticosteroid therapy, Cushing syndrome, acute infection.",
        ),
    ),
    LYMPHOCYTE(
        displayName = "Lymphocytes",
        group = "Mononuclear cell",
        accent = Color(0xFF7C3AED),
        countsInWbc = true,
        refPercent = "20 – 40", refPercentLow = 20.0, refPercentHigh = 40.0,
        refAbsolute = "1.0 – 3.0", refAbsLow = 1.0, refAbsHigh = 3.0,
        reference = MorphologyReference(
            size = "Small 7 – 10 µm; large 12 – 16 µm",
            nucleus = "Round, dense, clumped chromatin occupying most of the cell; nucleoli not usually seen.",
            cytoplasm = "Scanty rim of clear sky-blue cytoplasm; large granular lymphocytes contain a few azurophilic granules.",
            staining = "Romanowsky: nucleus dark purple, cytoplasm pale to moderate blue.",
            increasedIn = "Viral infections, pertussis, tuberculosis, brucellosis, chronic lymphocytic leukaemia and other lymphoproliferative disorders, childhood (physiological).",
            decreasedIn = "HIV infection, corticosteroids and immunosuppressants, lymphoma, systemic lupus erythematosus, congenital immunodeficiency, uraemia.",
        ),
    ),
    MONOCYTE(
        displayName = "Monocytes",
        group = "Mononuclear cell",
        accent = Color(0xFF0D9488),
        countsInWbc = true,
        refPercent = "2 – 10", refPercentLow = 2.0, refPercentHigh = 10.0,
        refAbsolute = "0.2 – 1.0", refAbsLow = 0.2, refAbsHigh = 1.0,
        reference = MorphologyReference(
            size = "15 – 20 µm (largest normal blood leucocyte)",
            nucleus = "Indented, kidney-shaped or folded; lacy, delicate chromatin.",
            cytoplasm = "Abundant grey-blue \"ground-glass\" cytoplasm with fine azurophilic dust and occasional vacuoles.",
            staining = "Romanowsky: pale violet nucleus, grey-blue cytoplasm. Non-specific esterase positive.",
            increasedIn = "Tuberculosis, infective endocarditis, brucellosis, malaria, recovery from neutropenia, chronic myelomonocytic leukaemia, acute myelomonocytic and monocytic leukaemia.",
            decreasedIn = "Hairy cell leukaemia, aplastic anaemia, corticosteroid therapy.",
        ),
    ),
    BASOPHIL(
        displayName = "Basophils",
        group = "Mature granulocyte",
        accent = Color(0xFFA21CAF),
        countsInWbc = true,
        refPercent = "0 – 2", refPercentLow = null, refPercentHigh = 2.0,
        refAbsolute = "0.02 – 0.1", refAbsLow = 0.02, refAbsHigh = 0.1,
        reference = MorphologyReference(
            size = "10 – 14 µm",
            nucleus = "Bilobed or irregular, often obscured by overlying granules.",
            cytoplasm = "Large, coarse, dark purple-black granules of unequal size that overlie the nucleus.",
            staining = "Romanowsky: granules metachromatic, deep purple-black; water-soluble so they may wash out.",
            increasedIn = "Chronic myeloid leukaemia and other myeloproliferative neoplasms (persistent basophilia is a warning sign), hypothyroidism, hypersensitivity reactions, ulcerative colitis.",
            decreasedIn = "Acute infection, stress, hyperthyroidism, corticosteroid therapy (rarely of clinical value).",
        ),
    ),
    METAMYELOCYTE(
        displayName = "Metamyelocytes",
        group = "Immature granulocyte",
        accent = Color(0xFFD97706),
        countsInWbc = true,
        refPercent = "0", refPercentLow = null, refPercentHigh = 0.0,
        refAbsolute = "—", refAbsLow = null, refAbsHigh = null,
        reference = MorphologyReference(
            size = "10 – 18 µm",
            nucleus = "Kidney or bean-shaped, indentation less than half the nuclear width; condensed chromatin; no nucleoli.",
            cytoplasm = "Pink with abundant fine specific granules.",
            staining = "Romanowsky: purple nucleus, lilac-pink granular cytoplasm.",
            increasedIn = "Left shift in severe infection, leucoerythroblastic blood picture (marrow infiltration, myelofibrosis), G-CSF therapy, pregnancy, chronic myeloid leukaemia.",
            decreasedIn = "Not normally present in peripheral blood.",
        ),
    ),
    MYELOCYTE(
        displayName = "Myelocytes",
        group = "Immature granulocyte",
        accent = Color(0xFF65A30D),
        countsInWbc = true,
        refPercent = "0", refPercentLow = null, refPercentHigh = 0.0,
        refAbsolute = "—", refAbsLow = null, refAbsHigh = null,
        reference = MorphologyReference(
            size = "12 – 18 µm",
            nucleus = "Round to oval, often eccentric; chromatin moderately condensed; nucleoli absent or indistinct.",
            cytoplasm = "Mildly basophilic to pink, with specific (secondary) granules and a pale perinuclear Golgi zone.",
            staining = "Romanowsky: granules lilac (neutrophilic), orange (eosinophilic) or purple (basophilic) by lineage.",
            increasedIn = "Chronic myeloid leukaemia (myelocyte peak exceeding metamyelocytes), leucoerythroblastic anaemia, severe infection, G-CSF therapy.",
            decreasedIn = "Not normally present in peripheral blood.",
        ),
    ),
    PROMYELOCYTE(
        displayName = "Promyelocytes",
        group = "Immature granulocyte",
        accent = Color(0xFFEC4899),
        countsInWbc = true,
        refPercent = "0", refPercentLow = null, refPercentHigh = 0.0,
        refAbsolute = "—", refAbsLow = null, refAbsHigh = null,
        reference = MorphologyReference(
            size = "15 – 25 µm (larger than a myeloblast)",
            nucleus = "Round or oval, often eccentric; slightly coarser chromatin than a blast; nucleoli may be visible.",
            cytoplasm = "Basophilic, with numerous coarse primary (azurophilic) granules and a perinuclear clear Golgi zone (hof).",
            staining = "Romanowsky: azurophilic granules stain purple-red; myeloperoxidase strongly positive.",
            increasedIn = "Acute promyelocytic leukaemia (abnormal hypergranular promyelocytes, bundles of Auer rods — \"faggot cells\"), chronic myeloid leukaemia, recovery from marrow suppression, G-CSF therapy.",
            decreasedIn = "Not normally present in peripheral blood.",
        ),
    ),
    BLAST(
        displayName = "Blasts",
        group = "Primitive / atypical",
        accent = Color(0xFFDC2626),
        countsInWbc = true,
        refPercent = "0", refPercentLow = null, refPercentHigh = 0.0,
        refAbsolute = "—", refAbsLow = null, refAbsHigh = null,
        reference = MorphologyReference(
            size = "12 – 20 µm",
            nucleus = "Large, round to oval, high nuclear : cytoplasmic ratio; fine, open, evenly dispersed chromatin; 2 – 5 prominent nucleoli.",
            cytoplasm = "Scanty, moderately to deeply basophilic, agranular or with few azurophilic granules; Auer rods confirm myeloid lineage.",
            staining = "Romanowsky: nucleus reddish-purple with pale nucleoli; cytoplasm blue. Lineage needs cytochemistry (MPO, Sudan black B) and flow cytometry.",
            increasedIn = "Acute myeloid and lymphoblastic leukaemia (blasts ≥ 20% of marrow or blood cells, WHO; lower thresholds apply to defining genetic abnormalities), myelodysplastic neoplasms, blast phase of CML, leucoerythroblastic reactions.",
            decreasedIn = "Not normally present in peripheral blood.",
        ),
    ),
    ATYPICAL_LYMPHOCYTE(
        displayName = "Atypical Lymphocytes",
        group = "Reactive lymphocyte",
        accent = Color(0xFF6366F1),
        countsInWbc = true,
        refPercent = "0", refPercentLow = null, refPercentHigh = 0.0,
        refAbsolute = "—", refAbsLow = null, refAbsHigh = null,
        reference = MorphologyReference(
            size = "12 – 25 µm",
            nucleus = "Larger, irregular or folded; chromatin less condensed than a small lymphocyte; nucleolus occasionally visible.",
            cytoplasm = "Abundant, pale to deep blue with basophilia concentrated at the margin; scalloped where it touches neighbouring red cells (Downey type II).",
            staining = "Romanowsky: nucleus purple; cytoplasm graded blue, darker at the periphery.",
            increasedIn = "Infectious mononucleosis (Epstein-Barr virus), cytomegalovirus, viral hepatitis, dengue, toxoplasmosis, drug reactions, post-transfusion.",
            decreasedIn = "Not normally present in significant numbers.",
        ),
    ),
    NRBC(
        displayName = "Nucleated Red Cells",
        group = "Excluded from leucocyte count",
        accent = Color(0xFFE11D48),
        countsInWbc = false,
        refPercent = "0 / 100 leucocytes", refPercentLow = null, refPercentHigh = 0.0,
        refAbsolute = "—", refAbsLow = null, refAbsHigh = null,
        reference = MorphologyReference(
            size = "7 – 10 µm (orthochromatic normoblast)",
            nucleus = "Small, round, dense, often eccentric or pyknotic nucleus with very condensed chromatin.",
            cytoplasm = "Pink-orange (haemoglobinised) to grey-blue (polychromatic) depending on maturation.",
            staining = "Romanowsky: nucleus jet-purple; cytoplasm matches surrounding red cells.",
            increasedIn = "Normal in neonates. Haemolytic anaemia, thalassaemia major, haemolytic disease of the fetus and newborn, marrow infiltration (leucoerythroblastic picture), primary myelofibrosis, severe hypoxia, sepsis, post-splenectomy.",
            decreasedIn = "Not normally present in adult peripheral blood. Reported per 100 leucocytes and used to correct the total leucocyte count.",
        ),
    );

    companion object {
        val keypadOrder: List<CellType> = listOf(
            NEUTROPHIL, BAND, EOSINOPHIL,
            LYMPHOCYTE, MONOCYTE, BASOPHIL,
            METAMYELOCYTE, MYELOCYTE, PROMYELOCYTE,
            BLAST, ATYPICAL_LYMPHOCYTE, NRBC,
        )
        val wbcTypes: List<CellType> = keypadOrder.filter { it.countsInWbc }
    }
}

data class MorphologyReference(
    val size: String,
    val nucleus: String,
    val cytoplasm: String,
    val staining: String,
    val increasedIn: String,
    val decreasedIn: String,
)

const val REFERENCE_SOURCES =
    "Bain BJ et al. Dacie & Lewis Practical Haematology, 12th ed. (Ch. 2 & 5); " +
        "Greer JP et al. Wintrobe's Clinical Hematology, 14th ed.; " +
        "Kaushansky K et al. Williams Hematology, 10th ed.; " +
        "WHO Classification of Haematolymphoid Tumours, 5th ed."

/** RBC & platelet morphology parameters, graded 0 (absent) to 3+. */
enum class MorphologyParam(val displayName: String, val section: String) {
    ANISOCYTOSIS("Anisocytosis", "Red cells"),
    POIKILOCYTOSIS("Poikilocytosis", "Red cells"),
    HYPOCHROMIA("Hypochromia", "Red cells"),
    TARGET_CELLS("Target cells", "Red cells"),
    SCHISTOCYTES("Schistocytes", "Red cells"),
    PLATELET_CLUMPING("Platelet clumping", "Platelets"),
}

enum class PlateletEstimate(val displayName: String) {
    NOT_ASSESSED("Not assessed"),
    ADEQUATE("Adequate"),
    REDUCED("Reduced"),
    INCREASED("Increased"),
}

fun gradeLabel(grade: Int): String = when (grade) {
    1 -> "1+"
    2 -> "2+"
    3 -> "3+"
    else -> "Nil"
}
