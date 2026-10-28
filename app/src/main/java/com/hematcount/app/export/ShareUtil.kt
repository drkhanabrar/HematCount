package com.hematcount.app.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object ShareUtil {

    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun copyText(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    /** @return true if WhatsApp was opened, false if we fell back to the system share sheet. */
    fun sharePdfToWhatsApp(context: Context, file: File, caption: String): Boolean {
        val uri = uriFor(context, file)
        for (pkg in WHATSAPP_PACKAGES) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                setPackage(pkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent); return true
            } catch (_: ActivityNotFoundException) { /* try next */ }
        }
        sharePdf(context, file, caption)
        return false
    }

    /** Normalises a phone number to international digits, e.g. "098765 43210" + "91" -> "919876543210". */
    fun normalisePhone(raw: String, defaultCountryCode: String): String? {
        val trimmed = raw.trim()
        var digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val cc = defaultCountryCode.filter { it.isDigit() }.ifBlank { "91" }
        digits = when {
            trimmed.startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.length == 11 && digits.startsWith("0") -> cc + digits.drop(1)
            digits.length <= 10 -> cc + digits
            else -> digits
        }
        return digits.takeIf { it.length in 8..15 }
    }

    /**
     * Opens WhatsApp directly in the chat of [phoneDigits] with the PDF attached.
     * The user confirms with one tap on Send (WhatsApp does not allow silent sending).
     * @return true if WhatsApp handled it.
     */
    fun sendPdfToWhatsAppNumber(context: Context, file: File, phoneDigits: String, caption: String): Boolean {
        val uri = uriFor(context, file)
        for (pkg in WHATSAPP_PACKAGES) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra("jid", "$phoneDigits@s.whatsapp.net")
                setPackage(pkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(intent); return true
            } catch (_: Exception) { }
        }
        return false
    }

    fun shareTextToWhatsApp(context: Context, text: String): Boolean {
        for (pkg in WHATSAPP_PACKAGES) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent); return true
            } catch (_: ActivityNotFoundException) { }
        }
        shareText(context, text)
        return false
    }

    fun sharePdf(context: Context, file: File, caption: String) {
        val uri = uriFor(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, caption)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share summary").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openPdf(context: Context, file: File): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uriFor(context, file), "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try { context.startActivity(intent); true } catch (_: ActivityNotFoundException) { false }
    }

    /** Android 10+ needs no permission; Android 8–9 need WRITE_EXTERNAL_STORAGE (requested by the UI). */
    fun needsStoragePermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    /** Saves to Downloads/HematCount. Returns a human-readable location. */
    fun saveToDownloads(context: Context, file: File): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HematCount")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create file in Downloads")
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HematCount")
            dir.mkdirs()
            file.copyTo(File(dir, file.name), overwrite = true)
        }
        return "Downloads/HematCount/${file.name}"
    }
}
