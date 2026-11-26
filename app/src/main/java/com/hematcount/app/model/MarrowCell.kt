package com.hematcount.app.model

import androidx.compose.ui.graphics.Color

enum class Lineage { MYELOID, ERYTHROID, LYMPHOID, MEGAKARYOCYTIC, OTHER }

/**
 * Bone marrow aspirate differential (myelogram).
 * Normal adult ranges (% of nucleated cells): Wintrobe's Clinical Hematology,
 * normal adult bone marrow differential (mean and 95% range); laboratories should
 * confirm against their own reference intervals.
 */
enum class MarrowCell(
    override val displayName: String,
    override val group: String,
    override val drawId: String,
    override val accent: Color,
    val lineage: Lineage,
    override val countsInTotal: Boolean,
    val low: Double?,
    val high: Double?,
    override val reference: MorphologyReference,
) : CountItem {
    BLAST("Blasts", "Myeloid precursor", "BLAST", Color(0xFFDC2626), Lineage.MYELOID, true, 0.2, 1.5,
        MorphologyReference(
            size = "12 – 20 µm",
            nucleus = "Large, round, fine open chromatin with 2 – 5 nucleoli; high nuclear : cytoplasmic ratio.",
            cytoplasm = "Scanty, basophilic, agranular or with few azurophilic granules; Auer rods indicate myeloid lineage.",
            staining = "Romanowsky; lineage requires MPO / Sudan black B and flow cytometry.",
            increasedIn = "Acute leukaemias (≥ 20% blasts, WHO 5th ed.; lower thresholds for defining genetic abnormalities), myelodysplastic neoplasms with excess blasts, CML in accelerated / blast phase, regenerating marrow.",
            decreasedIn = "Normal marrow contains fewer than 5% blasts (Wintrobe's normal 0.2 – 1.5%).",
        )),
    PROMYELOCYTE("Promyelocytes", "Neutrophil series", "PROMYELOCYTE", Color(0xFFEC4899), Lineage.MYELOID, true, 2.1, 4.1,
        CellType.PROMYELOCYTE.reference),
    MYELOCYTE("Myelocytes", "Neutrophil series", "MYELOCYTE", Color(0xFF65A30D), Lineage.MYELOID, true, 8.2, 15.7,
        CellType.MYELOCYTE.reference),
    METAMYELOCYTE("Metamyelocytes", "Neutrophil series", "METAMYELOCYTE", Color(0xFFD97706), Lineage.MYELOID, true, 9.6, 24.6,
        CellType.METAMYELOCYTE.reference),
    BAND("Band Neutrophils", "Neutrophil series", "BAND", Color(0xFF0EA5E9), Lineage.MYELOID, true, 9.5, 15.3,
        CellType.BAND.reference),
    SEGMENTED("Segmented Neutrophils", "Neutrophil series", "NEUTROPHIL", Color(0xFF2563EB), Lineage.MYELOID, true, 6.0, 12.0,
        CellType.NEUTROPHIL.reference),
    EOSINOPHIL("Eosinophils", "All stages", "EOSINOPHIL", Color(0xFFF97316), Lineage.MYELOID, true, 1.2, 5.3,
        CellType.EOSINOPHIL.reference),
    BASOPHIL("Basophils & Mast Cells", "Granulocytic", "BASOPHIL", Color(0xFFA21CAF), Lineage.MYELOID, true, 0.0, 0.2,
        MorphologyReference(
            size = "Basophil 10 – 14 µm; mast cell 15 – 30 µm",
            nucleus = "Basophil: lobed, obscured by granules. Mast cell: round, central, not lobed.",
            cytoplasm = "Coarse purple-black metachromatic granules; mast cells are densely packed with smaller uniform granules.",
            staining = "Romanowsky; metachromatic with toluidine blue; mast cells tryptase / CD117 positive.",
            increasedIn = "Chronic myeloid leukaemia and other myeloproliferative neoplasms, systemic mastocytosis, lymphoplasmacytic lymphoma (mast cells), aplastic anaemia (mast cells).",
            decreasedIn = "Normal marrow 0 – 0.2%.",
        )),
    MONOCYTE("Monocytes", "Mononuclear", "MONOCYTE", Color(0xFF0D9488), Lineage.OTHER, true, 0.0, 0.8,
        CellType.MONOCYTE.reference),
    PROERYTHROBLAST("Proerythroblasts", "Erythroid series", "PROERYTHROBLAST", Color(0xFF9F1239), Lineage.ERYTHROID, true, 0.2, 1.3,
        MorphologyReference(
            size = "15 – 25 µm",
            nucleus = "Large, round, fine reticular chromatin with one or more nucleoli.",
            cytoplasm = "Deeply basophilic, often with a pale perinuclear zone and cytoplasmic blebs.",
            staining = "Romanowsky; CD71 and glycophorin A / E-cadherin positive.",
            increasedIn = "Pure erythroid leukaemia, parvovirus B19 infection (giant proerythroblasts), regeneration after haemolysis or haemorrhage.",
            decreasedIn = "Pure red cell aplasia, aplastic anaemia, renal failure.",
        )),
    BASOPHILIC_NORMOBLAST("Basophilic Normoblasts", "Erythroid series", "BASO_NORMO", Color(0xFFBE185D), Lineage.ERYTHROID, true, 0.5, 2.4,
        MorphologyReference(
            size = "12 – 16 µm",
            nucleus = "Round, chromatin coarser and beginning to clump; nucleoli inconspicuous.",
            cytoplasm = "Intensely basophilic (abundant ribosomes), no haemoglobin pinkness yet.",
            staining = "Romanowsky: deep blue cytoplasm; nucleus dark purple.",
            increasedIn = "Erythroid hyperplasia: haemolysis, blood loss, megaloblastic anaemia (with open chromatin), dyserythropoiesis.",
            decreasedIn = "Red cell aplasia, marrow failure.",
        )),
    POLYCHROMATIC_NORMOBLAST("Polychromatic Normoblasts", "Erythroid series", "POLY_NORMO", Color(0xFFE11D48), Lineage.ERYTHROID, true, 17.9, 29.2,
        MorphologyReference(
            size = "10 – 12 µm",
            nucleus = "Smaller, round, coarse \"checkerboard\" chromatin.",
            cytoplasm = "Grey-blue to lilac (mixture of ribosomes and haemoglobin).",
            staining = "Romanowsky: polychromatic cytoplasm.",
            increasedIn = "Erythroid hyperplasia (haemolysis, haemorrhage, ineffective erythropoiesis in thalassaemia and megaloblastic anaemia).",
            decreasedIn = "Red cell aplasia, anaemia of renal failure.",
        )),
    ORTHOCHROMATIC_NORMOBLAST("Orthochromatic Normoblasts", "Erythroid series", "NRBC", Color(0xFFF43F5E), Lineage.ERYTHROID, true, 0.4, 4.6,
        CellType.NRBC.reference),
    LYMPHOCYTE("Lymphocytes", "Lymphoid", "LYMPHOCYTE", Color(0xFF7C3AED), Lineage.LYMPHOID, true, 11.1, 23.2,
        CellType.LYMPHOCYTE.reference),
    PLASMA_CELL("Plasma Cells", "Lymphoid", "PLASMA", Color(0xFF4F46E5), Lineage.LYMPHOID, true, 0.4, 3.9,
        MorphologyReference(
            size = "10 – 20 µm, oval",
            nucleus = "Eccentric, round, coarse \"clock-face\" chromatin; binucleate forms occasionally.",
            cytoplasm = "Abundant, deeply basophilic with a pale perinuclear Golgi zone (hof); may contain Russell bodies or Mott cells.",
            staining = "Romanowsky; CD38 / CD138 positive; light-chain restriction in neoplasia.",
            increasedIn = "Plasma cell myeloma (clonal plasma cells ≥ 10%), MGUS, reactive plasmacytosis (chronic infection, autoimmune disease, liver disease, HIV), lymphoplasmacytic lymphoma.",
            decreasedIn = "Normal marrow 0.4 – 3.9%.",
        )),
    MACROPHAGE("Macrophages", "Histiocytes", "MACROPHAGE", Color(0xFF64748B), Lineage.OTHER, true, 0.0, 0.9,
        MorphologyReference(
            size = "20 – 40 µm",
            nucleus = "Oval or reniform, fine chromatin, small nucleolus.",
            cytoplasm = "Abundant, pale grey-blue, often foamy or vacuolated with phagocytosed debris, haemosiderin or cells.",
            staining = "Romanowsky; Perls stain shows storage iron; CD68 / CD163 positive.",
            increasedIn = "Haemophagocytic lymphohistiocytosis, storage disorders (Gaucher, Niemann-Pick cells), infections (leishmaniasis, histoplasmosis), post-chemotherapy marrow.",
            decreasedIn = "Normal marrow 0 – 0.9%.",
        )),
    OTHER("Other / Abnormal Cells", "Specify in remarks", "OTHER", Color(0xFF78716C), Lineage.OTHER, true, null, 0.0,
        MorphologyReference(
            size = "Variable",
            nucleus = "Use for cells not represented on the keypad: metastatic tumour cells, lymphoma cells, hairy cells, abnormal blasts, osteoblasts, osteoclasts.",
            cytoplasm = "Describe morphology in the remarks.",
            staining = "Immunocytochemistry / flow cytometry as indicated.",
            increasedIn = "Marrow infiltration by lymphoma or metastatic carcinoma, neuroblastoma, rhabdomyosarcoma.",
            decreasedIn = "Not present in normal marrow.",
        )),
    MEGAKARYOCYTE("Megakaryocytes", "Tallied separately", "MEGAKARYOCYTE", Color(0xFF8B5CF6), Lineage.MEGAKARYOCYTIC, false, null, null,
        MorphologyReference(
            size = "50 – 100 µm (largest marrow cell)",
            nucleus = "Single large multilobed nucleus; micromegakaryocytes and separated lobes are dysplastic.",
            cytoplasm = "Abundant, granular pink-lilac; platelet budding at the margin in mature forms.",
            staining = "Romanowsky; CD61 / CD42b positive.",
            increasedIn = "Immune thrombocytopenia, essential thrombocythaemia and other myeloproliferative neoplasms, reactive thrombocytosis.",
            decreasedIn = "Aplastic anaemia, marrow infiltration, chemotherapy. Normally several per low-power field; assessed and reported separately.",
        )),

    // ---- extended keypad only: platelet lineage detail (tallied separately)
    DYSPLASTIC_MEGAKARYOCYTE("Dysplastic Megakaryocytes", "Tallied separately", "MEGAKARYOCYTE", Color(0xFFC026D3), Lineage.MEGAKARYOCYTIC, false, null, null,
        MorphologyReference(
            size = "Micromegakaryocytes ≤ size of a promyelocyte; others variable",
            nucleus = "Micromegakaryocytes with one or two small round lobes; widely separated (\"pawn-ball\") lobes; hypolobated forms.",
            cytoplasm = "Often mature granular cytoplasm despite small size.",
            staining = "Romanowsky; confirm with CD61 / CD42b in trephine.",
            increasedIn = "Myelodysplastic neoplasms (≥ 10% dysplastic megakaryocytes is significant, WHO 5th ed.), MDS with isolated del(5q) (hypolobated forms), myeloproliferative neoplasms.",
            decreasedIn = "Assess at least 30 megakaryocytes; report as a proportion of megakaryocytes counted.",
        )),

    // ---- simple keypad only: merged stages
    EARLY_NORMOBLAST("Early Normoblasts", "Proerythroblasts + basophilic", "BASO_NORMO", Color(0xFFBE185D), Lineage.ERYTHROID, true, 0.7, 3.7,
        MorphologyReference(
            size = "12 – 25 µm",
            nucleus = "Round nucleus with fine to slightly coarse chromatin; nucleoli in proerythroblasts.",
            cytoplasm = "Deeply basophilic, no haemoglobin pinkness.",
            staining = "Romanowsky. Simple keypad combines proerythroblasts and basophilic normoblasts.",
            increasedIn = "Erythroid hyperplasia, megaloblastic anaemia, pure erythroid leukaemia, parvovirus B19 (giant proerythroblasts).",
            decreasedIn = "Pure red cell aplasia, aplastic anaemia. Range = sum of the component stages (approximate).",
        )),
    LATE_NORMOBLAST("Late Normoblasts", "Polychromatic + orthochromatic", "POLY_NORMO", Color(0xFFE11D48), Lineage.ERYTHROID, true, 18.3, 33.8,
        MorphologyReference(
            size = "7 – 12 µm",
            nucleus = "Small, round, coarse \"checkerboard\" to pyknotic chromatin.",
            cytoplasm = "Grey-lilac (polychromatic) to pink (orthochromatic).",
            staining = "Romanowsky. Simple keypad combines polychromatic and orthochromatic normoblasts.",
            increasedIn = "Erythroid hyperplasia: haemolysis, haemorrhage, thalassaemia, ineffective erythropoiesis.",
            decreasedIn = "Red cell aplasia, anaemia of renal failure. Range = sum of the component stages (approximate).",
        )),
    NEUTROPHILS_ALL("Bands & Neutrophils", "Band + segmented forms", "NEUTROPHIL", Color(0xFF2563EB), Lineage.MYELOID, true, 15.5, 27.3,
        CellType.NEUTROPHIL.reference),
    ;

    override val id: String get() = "BM_$name"
    override val normalText: String get() = when {
        !countsInTotal -> "Assessed separately"
        low == null && high == 0.0 -> "Not normally present"
        else -> "${fmtRange(low)} – ${fmtRange(high)} %"
    }

    companion object {
        val keypadOrder: List<MarrowCell> = entries.toList()

        /** Keypad sections grouped by progeny. */
        val ERYTHROID_TITLE = "Erythroid series (RBC)"
        val LEUCOCYTE_TITLE = "Leucocyte series (WBC)"
        val PLATELET_TITLE = "Megakaryocytic series (Platelets)"
        val OTHER_TITLE = "Other cells"

        fun sections(extended: Boolean): List<KeypadSection> = if (extended) listOf(
            KeypadSection(ERYTHROID_TITLE, "Red cell precursors", Color(0xFFE11D48),
                listOf(PROERYTHROBLAST, BASOPHILIC_NORMOBLAST, POLYCHROMATIC_NORMOBLAST, ORTHOCHROMATIC_NORMOBLAST)),
            KeypadSection(LEUCOCYTE_TITLE, "White cell precursors", Color(0xFF2563EB),
                listOf(BLAST, PROMYELOCYTE, MYELOCYTE, METAMYELOCYTE, BAND, SEGMENTED, EOSINOPHIL, BASOPHIL, MONOCYTE, LYMPHOCYTE, PLASMA_CELL)),
            KeypadSection(PLATELET_TITLE, "Platelet precursors", Color(0xFF8B5CF6),
                listOf(MEGAKARYOCYTE, DYSPLASTIC_MEGAKARYOCYTE)),
            KeypadSection(OTHER_TITLE, null, Color(0xFF64748B), listOf(MACROPHAGE, OTHER)),
        ) else listOf(
            KeypadSection(ERYTHROID_TITLE, "Red cell precursors", Color(0xFFE11D48), listOf(EARLY_NORMOBLAST, LATE_NORMOBLAST)),
            KeypadSection(LEUCOCYTE_TITLE, "White cell precursors", Color(0xFF2563EB),
                listOf(BLAST, PROMYELOCYTE, MYELOCYTE, METAMYELOCYTE, NEUTROPHILS_ALL, EOSINOPHIL, MONOCYTE, LYMPHOCYTE, PLASMA_CELL)),
            KeypadSection(PLATELET_TITLE, "Platelet precursors", Color(0xFF8B5CF6), listOf(MEGAKARYOCYTE)),
            KeypadSection(OTHER_TITLE, null, Color(0xFF64748B), listOf(OTHER)),
        )

        fun sectionOf(c: MarrowCell): String = when (c.lineage) {
            Lineage.ERYTHROID -> ERYTHROID_TITLE
            Lineage.MEGAKARYOCYTIC -> PLATELET_TITLE
            Lineage.OTHER -> if (c == MONOCYTE) LEUCOCYTE_TITLE else OTHER_TITLE
            else -> LEUCOCYTE_TITLE
        }

        /** Report order: by progeny, extended order first, merged entries next to their stages. */
        val reportOrder: List<MarrowCell> = listOf(
            PROERYTHROBLAST, BASOPHILIC_NORMOBLAST, EARLY_NORMOBLAST, POLYCHROMATIC_NORMOBLAST, ORTHOCHROMATIC_NORMOBLAST, LATE_NORMOBLAST,
            BLAST, PROMYELOCYTE, MYELOCYTE, METAMYELOCYTE, BAND, SEGMENTED, NEUTROPHILS_ALL, EOSINOPHIL, BASOPHIL, MONOCYTE, LYMPHOCYTE, PLASMA_CELL,
            MEGAKARYOCYTE, DYSPLASTIC_MEGAKARYOCYTE, MACROPHAGE, OTHER,
        )
        val countedTypes: List<MarrowCell> = entries.filter { it.countsInTotal }
        /** Normal adult M:E ratio range. */
        const val ME_LOW = 1.5
        const val ME_HIGH = 3.3
        fun byId(id: String) = entries.firstOrNull { it.id == id }
    }
}

private fun fmtRange(v: Double?): String = when (v) {
    null -> "0"
    else -> if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
}

enum class Cellularity(val label: String) { NOT_ASSESSED("Not assessed"), HYPO("Hypocellular"), NORMO("Normocellular"), HYPER("Hypercellular") }
enum class MegaStatus(val label: String) { NOT_ASSESSED("Not assessed"), ADEQUATE("Adequate"), REDUCED("Reduced"), INCREASED("Increased"), ABSENT("Absent") }
enum class Particles(val label: String) { NOT_ASSESSED("Not assessed"), PRESENT("Present"), SCANTY("Scanty"), ABSENT("Absent (aparticulate)") }
enum class IronStores(val label: String) { NOT_DONE("Not done"), ABSENT("Absent (grade 0)"), REDUCED("Reduced (grade 1)"), NORMAL("Normal (grade 2–3)"), INCREASED("Increased (grade 4–6)") }
