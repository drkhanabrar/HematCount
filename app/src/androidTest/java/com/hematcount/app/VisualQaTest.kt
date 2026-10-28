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
            .commit()
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(700)
        val bmp = instr.uiAutomation.takeScreenshot() ?: return
        FileOutputStream(File(outDir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun clickText(t: String) =
        compose.onAllNodes(hasText(t) and hasClickAction()).onFirst().performClick()

    private fun clickDesc(d: String) =
        compose.onAllNodes(hasContentDescription(d) and hasClickAction()).onFirst().performClick()

    private fun back() { Espresso.pressBack(); compose.waitForIdle(); Thread.sleep(400) }

    private fun scrollTo(i: Int) = runCatching {
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(i)
    }

    private fun runTour(tag: String, theme: String) {
        seed(theme)
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

            compose.onAllNodes(hasText("Monocytes") and hasClickAction()).onFirst()
                .performTouchInput { longClick() }
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
                renderPdf(pdf)
            }
        }
    }

    private fun renderPdf(file: File) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r ->
                for (i in 0 until r.pageCount) {
                    r.openPage(i).use { page ->
                        val scale = 2
                        val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        FileOutputStream(File(outDir, "pdf_page${i + 1}.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
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
