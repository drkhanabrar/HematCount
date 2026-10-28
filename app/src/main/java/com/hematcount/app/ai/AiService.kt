package com.hematcount.app.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Any OpenAI-compatible chat-completions endpoint (OpenRouter, OpenAI, Groq,
 * Together, DeepSeek, a local LM Studio / Ollama server, …).
 * Providers are tried in list order; the first one that answers wins (failover).
 */
data class AiProvider(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val enabled: Boolean = true,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("name", name).put("baseUrl", baseUrl)
        .put("apiKey", apiKey).put("model", model).put("enabled", enabled)

    companion object {
        fun fromJson(o: JSONObject) = AiProvider(
            id = o.optString("id", UUID.randomUUID().toString()),
            name = o.optString("name"),
            baseUrl = o.optString("baseUrl"),
            apiKey = o.optString("apiKey"),
            model = o.optString("model"),
            enabled = o.optBoolean("enabled", true),
        )

        fun listToJson(list: List<AiProvider>): String =
            JSONArray().apply { list.forEach { put(it.toJson()) } }.toString()

        fun listFromJson(s: String?): List<AiProvider> = runCatching {
            val a = JSONArray(s ?: "[]")
            (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())

        /** Ready-made presets shown in the "Add provider" dialog. */
        val PRESETS = listOf(
            AiProvider(name = "OpenRouter", baseUrl = "https://openrouter.ai/api/v1", apiKey = "", model = "openrouter/auto"),
            AiProvider(name = "OpenAI", baseUrl = "https://api.openai.com/v1", apiKey = "", model = "gpt-4o-mini"),
            AiProvider(name = "Groq", baseUrl = "https://api.groq.com/openai/v1", apiKey = "", model = "llama-3.3-70b-versatile"),
            AiProvider(name = "Custom (OpenAI-compatible)", baseUrl = "https://", apiKey = "", model = ""),
        )
    }
}

/** What part of the AI analysis goes into the PDF report. */
enum class AiPdfMode(val label: String) {
    NONE("Don't include"),
    INVESTIGATIONS("Further investigations only"),
    FULL("Full AI interpretation"),
}

/** Pulls the "Suggested further investigations" section out of the AI markdown. */
fun extractInvestigations(md: String): String {
    val lines = md.replace("\r", "").split("\n")
    val start = lines.indexOfFirst { it.trim().startsWith("#") && it.contains("investigation", ignoreCase = true) }
    if (start < 0) return ""
    val out = mutableListOf<String>()
    for (i in start + 1 until lines.size) {
        if (lines[i].trim().startsWith("#")) break
        out += lines[i]
    }
    return out.joinToString("\n").trim()
}

data class AiAttempt(val provider: String, val ok: Boolean, val detail: String)

data class AiOutcome(val text: String?, val providerUsed: String?, val attempts: List<AiAttempt>)

object AiService {

    val SYSTEM_PROMPT = """
You are an experienced consultant haematopathologist assisting a pathology resident.
You receive a DE-IDENTIFIED manual laboratory count: a peripheral blood differential, bone marrow
aspirate differential (myelogram), CSF or body-fluid cell count, reticulocyte count, red cell
morphology count, platelet estimate, viability or custom count. Reference intervals follow Dacie & Lewis Practical
Haematology (12th ed.). Give a concise, structured, educational interpretation.

Use full cell names (no abbreviations for cell types). Absolute counts take precedence over percentages.
Use WHO 5th edition / ICC terminology for neoplasms.
Use exactly these markdown sections (## headings) and short bullet points:
## Key findings
## Possible diagnoses
(rank the most likely first; one line of reasoning each)
## Suggested further investigations
(practical next tests, e.g. repeat smear review, reticulocyte count, peripheral smear for parasites, iron studies, B12/folate, LAP score, flow cytometry, bone marrow aspiration and biopsy, BCR::ABL1, etc., only when relevant)
## Red flags
(urgent findings needing immediate attention, or "None identified")
## Textbook references
(2–4 specific references with chapter topic: Dacie & Lewis Practical Haematology 12th ed., Wintrobe's Clinical Hematology 14th ed., Williams Hematology 10th ed., WHO Classification of Haematolymphoid Tumours 5th ed., Robbins & Cotran 10th ed.)
## Limitations

Rules: be specific to the numbers given; do not invent results that were not provided;
state when findings are non-specific; mention standard textbooks (Dacie & Lewis, Wintrobe's,
Williams Hematology, WHO 5th edition) where helpful. Keep it under 400 words.
This is decision support only and not a diagnosis.
""".trimIndent()

    /** Blocking network call – run on Dispatchers.IO. */
    fun analyse(providers: List<AiProvider>, reportText: String, onTrying: (String) -> Unit): AiOutcome {
        val attempts = mutableListOf<AiAttempt>()
        val active = providers.filter { it.enabled && it.baseUrl.isNotBlank() && it.model.isNotBlank() }
        if (active.isEmpty()) {
            return AiOutcome(null, null, listOf(AiAttempt("—", false, "No AI provider is enabled. Add one in Settings → AI providers.")))
        }
        for (p in active) {
            onTrying(p.name)
            try {
                val text = call(p, reportText)
                if (text.isNullOrBlank()) {
                    attempts += AiAttempt(p.name, false, "Empty response")
                } else {
                    attempts += AiAttempt(p.name, true, "OK (${p.model})")
                    return AiOutcome(text.trim(), "${p.name} · ${p.model}", attempts)
                }
            } catch (e: Exception) {
                attempts += AiAttempt(p.name, false, e.message?.take(220) ?: e.javaClass.simpleName)
            }
        }
        return AiOutcome(null, null, attempts)
    }

    private fun call(p: AiProvider, reportText: String): String? {
        val url = URL(p.baseUrl.trimEnd('/') + "/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (p.apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${p.apiKey.trim()}")
            if (p.baseUrl.contains("openrouter.ai")) {
                setRequestProperty("HTTP-Referer", "https://github.com/drkhanabrar/HematCount")
                setRequestProperty("X-Title", "HematCount")
            }
        }
        val body = JSONObject()
            .put("model", p.model.trim())
            .put("temperature", 0.2)
            .put("max_tokens", 1400)
            .put("messages", JSONArray()
                .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                .put(JSONObject().put("role", "user").put("content",
                    "Interpret this laboratory report:\n\n$reportText")))
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull()
                throw IllegalStateException("HTTP $code${if (!msg.isNullOrBlank()) " – $msg" else ""}")
            }
            val json = JSONObject(raw)
            json.optJSONObject("error")?.let { throw IllegalStateException(it.optString("message", "Provider error")) }
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            return choices.getJSONObject(0).optJSONObject("message")?.optString("content")
        } finally {
            conn.disconnect()
        }
    }
}
