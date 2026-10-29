# HematCount — Improvements & Suggestions Log

Work happens on the `dev` branch. **No new version is released until Dr. Imran Khan approves**; a release is created only when `dev` is merged into `main`.

Status: ✅ done · 🔄 in progress · 📝 planned / suggested

---

## Requested by Dr. Imran Khan (28 Oct 2026)

| # | Item | Status |
|---|---|---|
| R1 | **Bone marrow counter** (myelogram) — marrow differential, M:E ratio, normal ranges, marrow PDF report | ✅ |
| R2 | **CSF counter** — Neubauer chamber cell count + differential | ✅ |
| R3 | **Body fluid counter with Neubauer chamber** — pleural, peritoneal (ascitic), pericardial, synovial; blood WBC / RBC / platelet manual counts | ✅ |
| R4 | **Undo feedback** — instead of a floating notification, briefly flash the counter cell that was undone | ✅ |
| R5 | Hold all releases until approval (work on `dev` branch only) | ✅ |
| R6 | Feature parity with the "Cell Counter" app (MSM-DEV, Google Play) — see below | ✅ |

### R6 — features adopted from the reference app (ideas only; no text, icons or branding copied)

| Reference-app feature | HematCount implementation | Status |
|---|---|---|
| Differential WBC count | Peripheral blood mode (existing) | ✅ |
| Bone marrow count | Bone marrow mode (R1) | ✅ |
| Neubauer chamber count | CSF & body-fluid mode with interactive chamber grid (R2, R3) | ✅ |
| Reticulocyte count | Reticulocyte mode: %, absolute count, corrected %, reticulocyte production index; optional Miller-disc method | ✅ |
| RBC abnormalities count | Red cell morphology mode: % of each poikilocyte per red cells counted; schistocyte flag | ✅ |
| Platelet count | Platelet estimate mode: platelets per oil-immersion field × factor; clump flag | ✅ |
| Viability count | Cell viability mode (trypan blue): % viability, viable cells/mL | ✅ |
| Counters with user-defined names and icons | Custom counter mode: add / rename / reorder keys, choose icon and colour | ✅ |
| Concentration and percentage calculations | All modes | ✅ |
| Automatic saving | All modes | ✅ |
| Alert at maximum count | Target lock in all modes | ✅ |
| WBC morphology information | Long-press reference cards (extended to marrow and fluid cells) | ✅ |
| Physical keyboard support | USB / Bluetooth keyboard & numeric keypad: keys 1–9, 0 and letters map to keypad positions; Backspace = undo | ✅ |
| Sound + vibration, dark mode, landscape, sharing | Existing | ✅ |

## Implementation notes (released in v1.0.9+)

- **Modes**: Peripheral blood · Bone marrow · CSF & body fluids · Reticulocytes · RBC morphology · Platelets · Viability · Custom — switch with the chips under the title bar; each mode keeps its own counts, target and undo history.
- **Bone marrow**: 17-cell myelogram (default target 500) + megakaryocytes tallied separately; M:E ratio, lineage %, blast and plasma-cell alerts (WHO 5th ed. thresholds), ICSH 500-cell reminder; findings sheet for site, particles, cellularity, megakaryocytes, Perls iron. Normal ranges: Wintrobe's normal adult marrow differential.
- **CSF & fluids**: interactive improved-Neubauer diagram (tap squares counted), presets for CSF, pleural, peritoneal, pericardial, synovial and blood WBC / RBC / platelet counts; dilution; cells/µL; CSF traumatic-tap correction; fluid differential with absolute counts; SBP and pleocytosis alerts.
- **Reticulocytes**: direct or Miller-disc method; %, absolute count, corrected %, RPI with maturation correction.
- **RBC morphology**: 12 keys incl. normal cells; % of red cells; ICSH schistocyte > 1% flag; sickle / spherocyte / tear-drop / bite-cell alerts.
- **Platelets**: platelets per oil-immersion field × factor (default 15 000), clump and giant-platelet tallies.
- **Viability**: trypan blue live / dead, % viability, cells/mL.
- **Custom**: up to 24 keys with name, 20 icons, 14 colours, include-in-% switch, reorder.
- **Keyboard**: numeric keypad 7-8-9 / 4-5-6 / 1-2-3 / 0 / . / Enter mirror a laboratory counter; 1–0 and Q…L by keypad position; Backspace = undo; − = correction mode. Shortcut list in the ⋮ menu.
- **Undo**: no pop-up message; the affected key flashes twice in its colour.
- **Reports**: every mode has its own Results screen and A4 PDF (key results first, then the count table with normal ranges and flags); history, backup and AI analysis cover all modes.

## Found during visual QA (fixed in v1.0.8)

- Pop-up message covered the bottom toolbar on phones — moved above the toolbar.
- "−1" badge overlapped cell names in correction mode — removed; tiles show "tap −1".
- "Metamyelocytes" broke mid-word on narrow phones — names now auto-size.
- Donut chart pushed the legend off screen on tablets — donut size capped.
- PDF FLAG column clipped and "PRESENT" pills overflowed the margin — columns re-spaced.

## Suggestions for consideration 📝

**Counting**
- Optional per-tap sound per cell type (different pitch for granulocytes vs mononuclear) for eyes-on-microscope counting.
- Hardware/Bluetooth keypad and volume-button support so the screen need not be touched while at the microscope.
- Customisable keypad (reorder, hide, rename keys; add plasma cells, hairy cells, Sézary cells, smudge cells for peripheral blood).
- Separate "smudge cell" tally reported per 100 leucocytes (useful in CLL).
- Large-print / one-handed mode for small phones.

**Reporting**
- Hospital logo upload for the PDF letterhead.
- Digital signature / stamp image for "Reported by".
- QR code on the PDF linking to a verification copy.
- Paediatric and neonatal reference ranges selectable by age (ranges differ markedly from adults).
- Hindi / Marathi patient-facing summary page.
- Batch export of several reports as one PDF.

**Data & sharing**
- CSV / Excel export of history for audits, dissertations and NABL quality records.
- Duplicate-sample detection by Sample ID.
- Optional PIN / fingerprint lock (reports contain patient data).

**Teaching**
- Quiz mode using the morphology reference cards (spot the cell).
- Inter-observer comparison: two people count the same slide; app shows agreement (useful for MLT/BPMT training and QC).
- Real micrograph gallery alongside the drawn illustrations.

**Quality**
- Count-time tracking and cells-per-minute statistics.
- Reminder when the differential is below the recommended count (e.g. 200 cells when WBC is very high or low).
- Store API keys in Android Keystore-encrypted storage.
- Move the APK signing key to GitHub Secrets before any public / Play Store release.
