package com.absolutemango.expressiveui.byok

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ModelScraperEngine(
    private val client: OkHttpClient = defaultClient()
) {

    companion object {
        private const val TAG = "ModelScraperEngine"

        val STATIC_ANTHROPIC_MODELS = listOf(
            "claude-3-5-sonnet-latest",
            "claude-3-5-haiku-latest",
            "claude-3-opus-latest"
        )

        val STATIC_GEMINI_FALLBACK = listOf(
            "gemini-3.6-flash",
            "gemini-3.5-flash-lite",
            "gemini-2.5-flash",
            "gemini-3.1-pro"
        )

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()
        }

        /**
         * Detects the provider type based on API key prefix heuristics.
         * Used to auto-populate provider dropdown on clipboard paste.
         */
        fun detectProvider(key: String): ProviderType? {
            val trimmed = key.trim()
            return when {
                trimmed.startsWith("sk-ant-") -> ProviderType.ANTHROPIC
                trimmed.startsWith("gsk_") -> ProviderType.GROQ
                trimmed.startsWith("sk-or-v1-") || trimmed.startsWith("sk-or-") -> ProviderType.OPENROUTER
                trimmed.startsWith("AIzaSy") -> ProviderType.GEMINI
                trimmed.startsWith("sk-") -> ProviderType.OPENAI
                else -> null
            }
        }
    }

    /**
     * Queries remote catalog and applies domain-specific negative capability filtering.
     */
    suspend fun scrapeModels(
        providerType: ProviderType,
        apiKey: String,
        baseUrl: String? = null
    ): List<String> = withContext(Dispatchers.IO) {
        try {
            when (providerType) {
                ProviderType.GEMINI -> scrapeGemini(apiKey, baseUrl)
                ProviderType.OPENROUTER -> scrapeOpenRouter(apiKey, baseUrl)
                ProviderType.GROQ -> scrapeGroq(apiKey, baseUrl)
                ProviderType.OPENAI -> scrapeOpenAi(apiKey, baseUrl)
                ProviderType.ANTHROPIC -> scrapeAnthropic(apiKey, baseUrl)
                ProviderType.CUSTOM -> scrapeCustom(apiKey, baseUrl)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scrape models for provider $providerType: ${e.message}", e)
            when (providerType) {
                ProviderType.GEMINI -> STATIC_GEMINI_FALLBACK
                ProviderType.ANTHROPIC -> STATIC_ANTHROPIC_MODELS
                else -> emptyList()
            }
        }
    }

    private fun scrapeGemini(apiKey: String, baseUrl: String?): List<String> {
        val base = (baseUrl ?: ProviderType.GEMINI.defaultBaseUrl ?: "https://generativelanguage.googleapis.com").trimEnd('/')
        val url = if (apiKey.isNotBlank()) "$base/v1beta/models?key=$apiKey" else "$base/v1beta/models"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return STATIC_GEMINI_FALLBACK
            }
            val body = response.body?.string() ?: return STATIC_GEMINI_FALLBACK
            val root = JSONObject(body)
            val modelsArray = root.optJSONArray("models") ?: return STATIC_GEMINI_FALLBACK

            val results = mutableListOf<String>()
            for (i in 0 until modelsArray.length()) {
                val item = modelsArray.getJSONObject(i)
                val supportedMethods = item.optJSONArray("supportedGenerationMethods")
                var canGenerate = false
                if (supportedMethods != null) {
                    for (m in 0 until supportedMethods.length()) {
                        if (supportedMethods.getString(m) == "generateContent") {
                            canGenerate = true
                            break
                        }
                    }
                }
                if (canGenerate) {
                    val rawName = item.optString("name", "")
                    val cleanName = rawName.removePrefix("models/")
                    if (cleanName.isNotBlank()) {
                        results.add(cleanName)
                    }
                }
            }
            return if (results.isNotEmpty()) results else STATIC_GEMINI_FALLBACK
        }
    }

    private fun scrapeOpenRouter(apiKey: String, baseUrl: String?): List<String> {
        val base = (baseUrl ?: ProviderType.OPENROUTER.defaultBaseUrl ?: "https://openrouter.ai/api").trimEnd('/')
        val url = "$base/v1/models"

        val builder = Request.Builder().url(url).get()
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }

        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val root = JSONObject(body)
            val dataArray = root.optJSONArray("data") ?: return emptyList()

            val results = mutableListOf<String>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id", "")
                if (isConversationalModel(id)) {
                    results.add(id)
                }
            }
            return results
        }
    }

    private fun scrapeGroq(apiKey: String, baseUrl: String?): List<String> {
        val base = (baseUrl ?: ProviderType.GROQ.defaultBaseUrl ?: "https://api.groq.com/openai").trimEnd('/')
        val url = "$base/v1/models"

        val builder = Request.Builder().url(url).get()
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }

        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val root = JSONObject(body)
            val dataArray = root.optJSONArray("data") ?: return emptyList()

            val results = mutableListOf<String>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id", "")
                val active = item.optBoolean("active", true)
                if (active && isConversationalModel(id)) {
                    results.add(id)
                }
            }
            return results
        }
    }

    private fun scrapeOpenAi(apiKey: String, baseUrl: String?): List<String> {
        val base = (baseUrl ?: ProviderType.OPENAI.defaultBaseUrl ?: "https://api.openai.com").trimEnd('/')
        val url = "$base/v1/models"

        val builder = Request.Builder().url(url).get()
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }

        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val body = response.body?.string() ?: return emptyList()
            val root = JSONObject(body)
            val dataArray = root.optJSONArray("data") ?: return emptyList()

            val results = mutableListOf<String>()
            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id", "")
                if (isConversationalModel(id)) {
                    results.add(id)
                }
            }
            return results
        }
    }

    private fun scrapeAnthropic(apiKey: String, baseUrl: String?): List<String> {
        val base = (baseUrl ?: ProviderType.ANTHROPIC.defaultBaseUrl ?: "https://api.anthropic.com").trimEnd('/')
        val url = "$base/v1/models"

        val builder = Request.Builder()
            .url(url)
            .get()
            .header("anthropic-version", "2023-06-01")

        if (apiKey.isNotBlank()) {
            builder.header("x-api-key", apiKey)
        }

        return try {
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) return STATIC_ANTHROPIC_MODELS
                val body = response.body?.string() ?: return STATIC_ANTHROPIC_MODELS
                val root = JSONObject(body)
                val dataArray = root.optJSONArray("data") ?: return STATIC_ANTHROPIC_MODELS

                val results = mutableListOf<String>()
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val id = item.optString("id", "")
                    if (id.contains("claude", ignoreCase = true)) {
                        results.add(id)
                    }
                }
                if (results.isNotEmpty()) results else STATIC_ANTHROPIC_MODELS
            }
        } catch (_: Exception) {
            STATIC_ANTHROPIC_MODELS
        }
    }

    private fun scrapeCustom(apiKey: String, baseUrl: String?): List<String> {
        val base = (baseUrl ?: "http://localhost:11434").trimEnd('/')
        val url = "$base/v1/models"

        val builder = Request.Builder().url(url).get()
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer $apiKey")
        }

        return try {
            client.newCall(builder.build()).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val root = JSONObject(body)
                val dataArray = root.optJSONArray("data") ?: return emptyList()

                val results = mutableListOf<String>()
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val id = item.optString("id", "")
                    if (isConversationalModel(id)) {
                        results.add(id)
                    }
                }
                results
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Negative modality filter: Strips embedding, image generation, audio, and legacy non-chat models.
     */
    fun isConversationalModel(id: String): Boolean {
        val lower = id.lowercase()
        return when {
            lower.contains("text-embedding") -> false
            lower.contains("embedding") -> false
            lower.contains("dall-e") -> false
            lower.contains("whisper") -> false
            lower.contains("tts-") || lower.contains("/tts") || lower.endsWith("-tts") -> false
            lower.contains("babbage") || lower.contains("davinci") -> false
            lower.contains("canary") -> false
            lower.contains("moderation") -> false
            else -> true
        }
    }
}
