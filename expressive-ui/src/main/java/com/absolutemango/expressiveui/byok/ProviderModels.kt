package com.absolutemango.expressiveui.byok

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class ProviderType(val displayName: String, val defaultBaseUrl: String?) {
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api"),
    GROQ("Groq", "https://api.groq.com/openai"),
    OPENAI("OpenAI", "https://api.openai.com"),
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com"),
    CUSTOM("Custom / Self-Hosted", null)
}

data class ProviderProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val providerType: ProviderType,
    val baseUrl: String? = providerType.defaultBaseUrl,
    val models: List<String> = emptyList(),
    val selectedModel: String = "",
    val usingBackupKey: Boolean = false,
    val backupFailoverTimestamp: Long = 0L,
    val isActive: Boolean = false
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("providerType", providerType.name)
        if (baseUrl != null) {
            json.put("baseUrl", baseUrl)
        } else {
            json.put("baseUrl", JSONObject.NULL)
        }
        val modelsArray = JSONArray()
        models.forEach { modelsArray.put(it) }
        json.put("models", modelsArray)
        json.put("selectedModel", selectedModel)
        json.put("usingBackupKey", usingBackupKey)
        json.put("backupFailoverTimestamp", backupFailoverTimestamp)
        json.put("isActive", isActive)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): ProviderProfile {
            val id = json.optString("id", UUID.randomUUID().toString())
            val name = json.optString("name", "Default Profile")
            val typeStr = json.optString("providerType", ProviderType.GEMINI.name)
            val providerType = try {
                ProviderType.valueOf(typeStr)
            } catch (e: IllegalArgumentException) {
                ProviderType.GEMINI
            }
            val baseUrl = if (json.has("baseUrl") && !json.isNull("baseUrl")) {
                json.getString("baseUrl")
            } else {
                providerType.defaultBaseUrl
            }
            val modelsList = mutableListOf<String>()
            val modelsArray = json.optJSONArray("models")
            if (modelsArray != null) {
                for (i in 0 until modelsArray.length()) {
                    modelsList.add(modelsArray.getString(i))
                }
            }
            val selectedModel = json.optString("selectedModel", "")
            val usingBackupKey = json.optBoolean("usingBackupKey", false)
            val backupFailoverTimestamp = json.optLong("backupFailoverTimestamp", 0L)
            val isActive = json.optBoolean("isActive", false)

            return ProviderProfile(
                id = id,
                name = name,
                providerType = providerType,
                baseUrl = baseUrl,
                models = modelsList,
                selectedModel = selectedModel,
                usingBackupKey = usingBackupKey,
                backupFailoverTimestamp = backupFailoverTimestamp,
                isActive = isActive
            )
        }
    }
}
