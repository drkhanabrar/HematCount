# HematCount

**Precision in Every Cell** — a native Android haematology differential cell counter and peripheral blood smear reporting app for medical professionals and students.

Conceived and presented by **Dr. Imran Khan**, Department of Pathology, B. J. Government Medical College, Pune.
Technical development: **Dr. Abrar Khan**, Technical Team, Care Hospital, Chikhli · [carehospital.in](https://carehospital.in) · Other products: [Research & Development](https://carehospital.in/products.html)

---

## Install on your phone

1. On the phone, open **[Releases](https://github.com/drkhanabrar/HematCount/releases/latest)**.
2. Download `HematCount-v1.0.x.apk`.
3. Open the file and allow **Install unknown apps** for your browser when Android asks.
4. Later versions install over the old one — your counts, history and settings are kept.

Requires Android 8.0 or newer. Works on phones and tablets, portrait and landscape.

## Features

| Area | What it does |
|---|---|
| Counter | 12-key keypad (Neutrophils, Band Neutrophils, Eosinophils, Lymphocytes, Monocytes, Basophils, Metamyelocytes, Myelocytes, Promyelocytes, Blasts, Atypical Lymphocytes, Nucleated Red Cells) with morphological illustrations; live donut / bar chart in the top 35% |
| Reference cards | Long-press any cell: size, nucleus, cytoplasm, staining, normal range, associated conditions (Dacie & Lewis, Wintrobe's, Williams, WHO 5th ed.) |
| Target lock | 50 / 100 / 200 / 500 / continuous; chime + heavy vibration, keypad locks, option to continue |
| Corrections | Undo last tap; −1 correction mode to subtract from any cell |
| Calculations | Absolute counts, ANC, nRBC per 100 leucocytes, corrected leucocyte count when ≥ 5 nRBC/100, automatic flags and clinical alerts |
| Morphology | Anisocytosis, poikilocytosis, hypochromia, target cells, schistocytes, platelet clumping (Nil/1+/2+/3+), platelet estimate |
| PDF report | A4 hospital letterhead, patient panel, differential table with normal ranges and H/L flags, indices, distribution chart, morphology, alerts, remarks, signatures |
| Sharing | **Send to patient** on WhatsApp (opens the patient's chat with the PDF attached), send to any other number, save to Downloads, copy text for LIS |
| AI analysis | De-identified interpretation (possible diagnoses, further investigations, red flags, textbook references) through OpenRouter or any OpenAI-compatible API, with editable failover order; optional inclusion in the PDF (investigations only or full) |
| History & backup | On-device report history with search; cloud backup and restore through the system picker (Google Drive, OneDrive, MEGA, Dropbox, Box, local storage) and auto-backup to a chosen folder |
| Themes | Light, dark, system; optional Material You device colours |

## AI setup (optional)

Settings → **AI providers** → Add → choose *OpenRouter* (or OpenAI, Groq, or a custom OpenAI-compatible URL), paste your API key and model. Add more providers and use the arrows to set the failover order. Only age, sex, counts and morphology are sent — never name, sample ID, clinician or ward.

## Building from source

Open the folder in Android Studio (Ladybug or newer) and press **Run**, or:

```
./gradlew assembleRelease
```

Every push to `main` builds a signed APK and publishes it under **Releases** (GitHub Actions). A second workflow runs the app on phone and tablet emulators and saves light/dark screenshots and a rendered PDF to the `qa` branch.

> The signing key `hematcount-release.jks` is kept in the repository so updates install over each other. For a Play Store release, move the key and passwords into GitHub Secrets.

## Disclaimer

HematCount is a counting and reporting aid. Results, alerts and AI-generated text are decision support only and must be verified by a qualified pathologist and interpreted in the clinical context.
