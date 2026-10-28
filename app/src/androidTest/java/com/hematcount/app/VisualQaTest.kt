package com.hematcount.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hematcount.app.export.PdfReport
import com.hematcount.app.export.ReportData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Not a functional test: drives the real app through every screen in light and dark themes
 * and saves full-device screenshots plus a rendered image of the PDF, for visual review.
 */
@RunWith(AndroidJUnit4::class)
class VisualQaTest {

    @get:Rule val compose = createEmptyComposeRule()

    private val instr get() = InstrumentationRegistry.getInstrumentation()
    private val ctx: Context get() = instr.targetContext
    private val outDir: File get() = File(ctx.filesDir, "qa").apply { mkdirs() }

    @Test fun lightTheme() = runTour("light", "LIGHT")
    @Test fun darkTheme() = runTour("dark", "DARK")

    private fun seed(theme: String) {
        ctx.getSharedPreferences("hematcount", Context.MODE_PRIVATE).edit().clear()
            .putString("themeMode", theme)
            .putString("counts", "NEUTROPHIL:52,BAND:7,EOSINOPHIL:3,LYMPHOCYTE:18,MONOCYTE:5,BASOPHIL:1,METAMYELOCYTE:3,MYELOCYTE:4,PROMYELOCYTE:1,BLAST:2,ATYPICAL_LYMPHOCYTE:2,NRBC:6")
            .putInt("target", 200)
            .putString("sampleId", "HC-2410-0187")
            .putString("patientName", "Rahul Deshmukh")
            .putString("patientAge", "46").putString("patientSex", "Male")
            .putString("patientPhone", "9876543210")
            .putString("referredBy", "Dr. S. Patil (Medicine)")
            .putString("ward", "Ward 12 · Bed 7")
            .putString("wbcText", "18.6")
            .putString("remarks", "Neutrophilic leucocytosis with left shift and occasional blasts. Suggest bone marrow examination.")
            .putString("reportedBy", "Dr. Imran Khan")
            .putString("morph", "ANISOCYTOSIS:2,POIKILOCYTOSIS:1,HYPOCHROMIA:1,TARGET_CELLS:0,SCHISTOCYTES:1,PLATELET_CLUMPING:0")
            .putString("platelet", "REDUCED")
            .putString("aiResult", SAMPLE_AI)
            .putString("aiProviderUsed", "OpenRouter · openrouter/auto")
            .putString("aiPdfMode", "INVESTIGATIONS")
            .putString("mode", "BLOOD")
            .putString("t_MARROW_counts", "BM_BLAST:4,BM_PROMYELOCYTE:12,BM_MYELOCYTE:60,BM_METAMYELOCYTE:70,BM_BAND:55,BM_SEGMENTED:45,BM_EOSINOPHIL:15,BM_BASOPHIL:1,BM_MONOCYTE:3,BM_PROERYTHROBLAST:3,BM_BASOPHILIC_NORMOBLAST:8,BM_POLYCHROMATIC_NORMOBLAST:95,BM_ORTHOCHROMATIC_NORMOBLAST:12,BM_LYMPHOCYTE:70,BM_PLASMA_CELL:6,BM_MACROPHAGE:2,BM_MEGAKARYOCYTE:9")
            .putString("t_FLUID_CHAMBER_counts", "FL_NUCLEATED:46,FL_RBC:120")
            .putString("t_FLUID_DIFF_counts", "FD_NEUTROPHIL:62,FD_LYMPHOCYTE:28,FD_MONO_MACRO:9,FD_EPENDYMAL:1")
            .putString("t_RETIC_counts", "RT_RETIC:34,RT_RBC:966")
            .putString("t_RBC_MORPH_counts", "RB_NORMAL:880,RB_SCHISTO:24,RB_SPHERO:30,RB_TARGET:40,RB_DACRO:12,RB_ECHINO:14")
            .putString("t_PLATELET_counts", "PL_PLATELET:58,PL_FIELD:10,PL_CLUMP:2")
            .putString("t_VIABILITY_counts", "VB_LIVE:182,VB_DEAD:18")
            .putString("t_CUSTOM_counts", "CU_1:12,CU_2:7,CU_3:4")
            .putString("inputs", org.json.JSONObject(mapOf(
                "fl.preset" to "CSF", "fl.dilution" to "1", "fl.squares" to "L0,L1,L2,L3,L4,L5,L6,L7,L8",
                "fl.appearance" to "TURBID", "fl.bloodWbc" to "9.5", "fl.bloodRbc" to "4.4",
                "rt.rbc" to "3.2", "rt.hct" to "28",
                "bm.site" to "Right posterior iliac crest", "bm.particles" to "PRESENT", "bm.cellularity" to "HYPER",
                "bm.mega" to "ADEQUATE", "bm.iron" to "NORMAL",
            ) as Map<*, *>).toString())
            .commit()
    }

    private fun shot(name: String) {
        runCatching { compose.waitForIdle() }
        Thread.sleep(700)
        val bmp = instr.uiAutomation.takeScreenshot() ?: return
        FileOutputStream(File(outDir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun clickText(t: String) { runCatching {
        compose.onAllNodes(hasText(t) and hasClickAction()).onFirst().performClick()
    }.onFailure { android.util.Log.w("VisualQa", "click '$t' failed: ${it.message}") } }

    private fun clickDesc(d: String) { runCatching {
        compose.onAllNodes(hasContentDescription(d) and hasClickAction()).onFirst().performClick()
    }.onFailure { android.util.Log.w("VisualQa", "click desc '$d' failed: ${it.message}") } }

    private fun back() {
        runCatching { Espresso.pressBackUnconditionally() }
            .onFailure { runCatching { instr.uiAutomation.executeShellCommand("input keyevent 4").close() } }
        Thread.sleep(600)
    }

    private fun scrollTo(i: Int) = runCatching {
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(i)
    }

    private fun runTour(tag: String, theme: String) {
        seed(theme)
        runCatching {
            instr.uiAutomation.executeShellCommand("am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS").close()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            shot("${tag}_01_counter")
            // a few real taps to exercise the keypad
            repeat(3) { clickText("Neutrophils") }
            clickText("Lymphocytes")
            shot("${tag}_02_counter_after_taps")

            clickText("Correct −1")
            shot("${tag}_03_correction_mode")
            clickText("Correcting")

            clickDesc("Show bar chart")
            shot("${tag}_04_bar_chart")
            clickDesc("Show donut chart")

            runCatching {
                compose.onAllNodes(hasText("Monocytes") and hasClickAction()).onFirst()
                    .performTouchInput { longClick() }
            }
            shot("${tag}_05_reference_card")
            back()

            clickText("RBC · Platelets")
            shot("${tag}_06_morphology_sheet")
            back()

            clickText("Sample / Patient")
            shot("${tag}_07_patient_dialog")
            back()

            clickText("Results")
            shot("${tag}_08_results_top")
            scrollTo(3); shot("${tag}_09_results_table")
            scrollTo(6); shot("${tag}_10_results_export")
            clickText("AI Analysis")
            shot("${tag}_11_ai_tab")
            scrollTo(2); shot("${tag}_12_ai_result")
            clickDesc("Back to counter")

            clickDesc("More"); clickText("Settings")
            shot("${tag}_13_settings")
            scrollTo(3); shot("${tag}_14_settings_ai")
            back()

            clickDesc("More"); clickText("History & backup")
            shot("${tag}_15_history")
            back()

            clickDesc("More"); clickText("About")
            shot("${tag}_16_about")
            back()

            if (tag == "light") {
                var vm: CounterViewModel? = null
                scenario.onActivity { vm = ViewModelProvider(it)[CounterViewModel::class.java] }
                val pdf = PdfReport.generate(ctx, ReportData.from(vm!!))
                pdf.copyTo(File(outDir, "report.pdf"), overwrite = true)
                renderPdf(pdf, "pdf")
            }

            // ---- every additional counter mode
            val modes = listOf(
                Triple("Bone marrow", "marrow", "Findings"), Triple("CSF · Fluids", "fluid", "Setup"),
                Triple("Reticulocytes", "retic", "Setup"), Triple("RBC morphology", "rbc", "Guide"),
                Triple("Platelets", "platelet", "Setup"), Triple("Viability", "viability", "Setup"),
                Triple("Custom", "custom", "Edit keys"),
            )
            for ((chip, name, setup) in modes) {
                clickText(chip)
                shot("${tag}_m_${name}_1_counter")
                if (name == "fluid") { clickText("Differential"); shot("${tag}_m_${name}_1b_differential"); clickText("Chamber count") }
                if (name == "marrow") {
                    clickText("Blasts")
                    clickText("Undo")
                    Thread.sleep(120)
                    instr.uiAutomation.takeScreenshot()?.let { b ->
                        FileOutputStream(File(outDir, "${tag}_m_marrow_1c_undo_flash.png")).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                    Thread.sleep(900)
                }
                clickText(setup)
                shot("${tag}_m_${name}_2_setup")
                back()
                clickText("Results")
                shot("${tag}_m_${name}_3_results")
                scrollTo(3); shot("${tag}_m_${name}_4_results_more")
                clickDesc("Back to counter")
                if (tag == "light") {
                    var vm: CounterViewModel? = null
                    scenario.onActivity { vm = ViewModelProvider(it)[CounterViewModel::class.java] }
                    runCatching {
                        val pdf = PdfReport.generate(ctx, ReportData.from(vm!!))
                        renderPdf(pdf, "pdf_$name")
                    }
                }
            }
            clickText("Blood")
        }
    }

    private fun renderPdf(file: File, prefix: String) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r ->
                for (i in 0 until r.pageCount) {
                    r.openPage(i).use { page ->
                        val scale = 2
                        val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        FileOutputStream(File(outDir, "${prefix}_page${i + 1}.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    }
                }
            }
        }
    }

    companion object {
        val SAMPLE_AI = """
## Key findings
- Neutrophilic leucocytosis (corrected count elevated) with a **left shift**: band forms, metamyelocytes, myelocytes and an occasional promyelocyte.
- Blasts 2% and nucleated red cells 6 per 100 leucocytes — a leucoerythroblastic picture.
- Mild anisopoikilocytosis with occasional schistocytes; platelets reduced on smear.
## Possible diagnoses
- Severe infection / sepsis with leukaemoid reaction
- Chronic myeloid leukaemia, chronic phase (myelocyte peak, basophils)
- Marrow infiltration (metastatic carcinoma, myelofibrosis)
## Suggested further investigations
- Repeat peripheral smear review with platelet count confirmation
- BCR::ABL1 by RT-PCR / FISH
- Bone marrow aspiration and trephine biopsy
- Blood culture, CRP and procalcitonin
## Red flags
- Blasts in peripheral blood — expedite haematology review
""".trimIndent()
    }
}
