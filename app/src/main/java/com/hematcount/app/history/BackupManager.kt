package com.hematcount.app.history

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.hematcount.app.ai.AiProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cloud backup through Android's Storage Access Framework.
 * The system picker lists every installed storage provider — Google Drive,
 * OneDrive, Dropbox, Box, MEGA (when its app exposes a folder), Nextcloud,
 * a USB drive or local storage — so no per-company sign-in code is needed.
 */
object BackupManager {

    const val MIME = "application/json"
    const val AUTO_FILE_NAME = "HematCount-backup.json"
    private const val FORMAT_VERSION = 1

    data class Settings(
        val institution: String,
        val department: String,
        val address: String,
        val reportedBy: String,
        val providers: List<AiProvider>,
    )

    data class Restored(val records: List<ReportRecord>, val settings: Settings?)

    fun suggestedFileName(): String =
        "HematCount-backup-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".json"

    fun build(records: List<ReportRecord>, settings: Settings, includeApiKeys: Boolean): String {
        val root = JSONObject()
        root.put("app", "HematCount")
        root.put("format", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("records", JSONArray().apply { records.forEach { put(it.toJson()) } })
        root.put("settings", JSONObject().apply {
            put("institution", settings.institution)
            put("department", settings.department)
            put("address", settings.address)
            put("reportedBy", settings.reportedBy)
            put("aiProviders", JSONArray().apply {
                settings.providers.forEach { p -> put((if (includeApiKeys) p else p.copy(apiKey = "")).toJson()) }
            })
        })
        return root.toString(2)
    }

    fun parse(text: String): Restored {
        val root = JSONObject(text)
        require(root.optString("app") == "HematCount") { "This is not a HematCount backup file" }
        val arr = root.optJSONArray("records") ?: JSONArray()
        val records = (0 until arr.length()).map { ReportRecord.fromJson(arr.getJSONObject(it)) }
        val s = root.optJSONObject("settings")?.let { o ->
            val pa = o.optJSONArray("aiProviders") ?: JSONArray()
            Settings(
                institution = o.optString("institution"), department = o.optString("department"),
                address = o.optString("address"), reportedBy = o.optString("reportedBy"),
                providers = (0 until pa.length()).map { AiProvider.fromJson(pa.getJSONObject(it)) },
            )
        }
        return Restored(records, s)
    }

    fun writeTo(context: Context, uri: Uri, json: String) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            ?: error("Could not open the backup location")
    }

    fun readFrom(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: error("Could not read the backup file")

    /** Writes (or overwrites) HematCount-backup.json inside the chosen cloud folder. */
    fun autoBackup(context: Context, treeUri: Uri, json: String) {
        val dir = DocumentFile.fromTreeUri(context, treeUri) ?: error("Backup folder is not available")
        if (!dir.canWrite()) error("No permission to write to the backup folder — choose it again")
        val file = dir.findFile(AUTO_FILE_NAME) ?: dir.createFile(MIME, AUTO_FILE_NAME.removeSuffix(".json"))
            ?: error("Could not create the backup file")
        writeTo(context, file.uri, json)
    }

    fun folderName(context: Context, treeUri: Uri): String =
        runCatching { DocumentFile.fromTreeUri(context, treeUri)?.name }.getOrNull() ?: "Selected folder"

    /** Hands the backup file to any app (MEGA, Dropbox, Telegram, e-mail…) via the share sheet. */
    fun shareFile(context: Context, json: String) {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val f = File(dir, suggestedFileName())
        f.writeText(json)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, f.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Upload backup to…").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
