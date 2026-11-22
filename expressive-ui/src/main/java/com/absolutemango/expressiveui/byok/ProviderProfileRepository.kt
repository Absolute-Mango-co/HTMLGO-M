package com.absolutemango.expressiveui.byok

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

open class ProviderProfileRepository(
    private val context: Context? = null,
    private val keyStorehouse: SecureKeyStorehouse? = null
) {

    companion object {
        private const val TAG = "ProviderProfileRepo"
        const val FILE_NAME = "profiles_meta.json"
        const val CUSTOM_API_JSON_FILE = "custom_api_payload.json"
        const val DEFAULT_GEMINI_ID = "default_gemini"
    }

    private val _profiles = MutableStateFlow<List<ProviderProfile>>(emptyList())
    open val profiles: StateFlow<List<ProviderProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<ProviderProfile?>(null)
    open val activeProfile: StateFlow<ProviderProfile?> = _activeProfile.asStateFlow()

    private val _isSystemInstructionEnabled = MutableStateFlow(true)
    open val isSystemInstructionEnabled: StateFlow<Boolean> = _isSystemInstructionEnabled.asStateFlow()

    private val _systemInstructionPreset = MutableStateFlow(SystemInstructionPreset.ZIP_STRICT)
    open val systemInstructionPreset: StateFlow<SystemInstructionPreset> = _systemInstructionPreset.asStateFlow()

    private val _customSystemPrompt = MutableStateFlow("")
    open val customSystemPrompt: StateFlow<String> = _customSystemPrompt.asStateFlow()

    private val _modelConfig = MutableStateFlow(ModelConfig())
    open val modelConfig: StateFlow<ModelConfig> = _modelConfig.asStateFlow()

    private val _customApiJson = MutableStateFlow("")
    open val customApiJson: StateFlow<String> = _customApiJson.asStateFlow()

    @Synchronized
    open fun setSystemInstructionEnabled(enabled: Boolean) {
        _isSystemInstructionEnabled.value = enabled
        persistProfiles(_profiles.value)
    }

    @Synchronized
    open fun setSystemInstructionPreset(preset: SystemInstructionPreset) {
        _systemInstructionPreset.value = preset
        persistProfiles(_profiles.value)
    }

    @Synchronized
    open fun setCustomSystemPrompt(prompt: String) {
        _customSystemPrompt.value = prompt
        persistProfiles(_profiles.value)
    }

    @Synchronized
    open fun setModelConfig(config: ModelConfig) {
        _modelConfig.value = config
        persistProfiles(_profiles.value)
        try {
            val ctx = context ?: return
            val prefs = ctx.getSharedPreferences("expressive_ai_forge_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("model_config_json", config.toJson().toString()).apply()
        } catch (_: Exception) {}
    }

    @Synchronized
    open fun setCustomApiJson(json: String) {
        val clean = json.removePrefix("\uFEFF")
        _customApiJson.value = clean
        persistProfiles(_profiles.value)
        val ctx = context ?: return
        try {
            val file = File(ctx.filesDir, CUSTOM_API_JSON_FILE)
            val tempFile = File(ctx.filesDir, "$CUSTOM_API_JSON_FILE.tmp")
            if (clean.isBlank()) {
                if (file.exists()) file.delete()
                if (tempFile.exists()) tempFile.delete()
            } else {
                FileOutputStream(tempFile).use { fos ->
                    fos.write(clean.toByteArray(StandardCharsets.UTF_8))
                    fos.flush()
                }
                if (tempFile.exists()) {
                    if (file.exists()) file.delete()
                    tempFile.renameTo(file)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom API json file", e)
        }
        try {
            val prefs = ctx.getSharedPreferences("expressive_ai_forge_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("custom_api_json", clean).apply()
        } catch (_: Exception) {}
    }

    init {
        if (context != null) {
            val loaded = try { loadProfiles() } catch (_: Exception) { emptyList() }
            val initialProfiles = if (loaded.isEmpty()) {
                val defaultProfile = ProviderProfile(
                    id = DEFAULT_GEMINI_ID,
                    name = "Google Gemini",
                    providerType = ProviderType.GEMINI,
                    baseUrl = ProviderType.GEMINI.defaultBaseUrl,
                    models = listOf("gemini-3.6-flash", "gemini-3.5-flash-lite", "gemini-2.5-flash", "gemini-3.1-pro"),
                    selectedModel = "gemini-3.6-flash",
                    isActive = true
                )
                try { persistProfiles(listOf(defaultProfile)) } catch (_: Exception) {}
                listOf(defaultProfile)
            } else {
                loaded
            }
            _profiles.value = initialProfiles
            _activeProfile.value = initialProfiles.find { it.isActive } ?: initialProfiles.firstOrNull()
            if (_customSystemPrompt.value.isBlank()) {
                val prefsPrompt = try {
                    context.getSharedPreferences("expressive_ai_forge_prefs", Context.MODE_PRIVATE)
                        .getString("custom_system_prompt", "") ?: ""
                } catch (_: Exception) { "" }
                if (prefsPrompt.isNotBlank()) {
                    _customSystemPrompt.value = prefsPrompt
                }
            }
            if (!_modelConfig.value.hasCustomOverrides()) {
                val cfgJson = try {
                    context.getSharedPreferences("expressive_ai_forge_prefs", Context.MODE_PRIVATE)
                        .getString("model_config_json", null)
                } catch (_: Exception) { null }
                if (!cfgJson.isNullOrBlank()) {
                    try {
                        _modelConfig.value = ModelConfig.fromJson(JSONObject(cfgJson))
                    } catch (_: Exception) {}
                }
            }
            val customJsonFile = File(context.filesDir, CUSTOM_API_JSON_FILE)
            if (customJsonFile.exists()) {
                try {
                    val diskJson = customJsonFile.readText(StandardCharsets.UTF_8).removePrefix("\uFEFF")
                    if (diskJson.isNotBlank()) {
                        _customApiJson.value = diskJson
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading $CUSTOM_API_JSON_FILE", e)
                }
            }
            if (_customApiJson.value.isBlank()) {
                val prefsApiJson = try {
                    context.getSharedPreferences("expressive_ai_forge_prefs", Context.MODE_PRIVATE)
                        .getString("custom_api_json", "") ?: ""
                } catch (_: Exception) { "" }
                if (prefsApiJson.isNotBlank()) {
                    _customApiJson.value = prefsApiJson.removePrefix("\uFEFF")
                }
            }
            try { reconcileStorage() } catch (_: Exception) {}
        }
    }

    @Synchronized
    open fun saveProfile(profile: ProviderProfile) {
        val current = _profiles.value
        val exists = current.any { it.id == profile.id }
        val updated = if (exists) {
            current.map { if (it.id == profile.id) profile else it }
        } else {
            current + profile
        }

        val finalProfiles = if (profile.isActive) {
            updated.map { it.copy(isActive = it.id == profile.id) }
        } else {
            if (updated.none { it.isActive }) {
                updated.mapIndexed { index, p -> if (index == 0) p.copy(isActive = true) else p }
            } else {
                updated
            }
        }

        _profiles.value = finalProfiles
        _activeProfile.value = finalProfiles.find { it.isActive }
        persistProfiles(finalProfiles)
    }

    @Synchronized
    open fun deleteProfile(profileId: String) {
        // 1. Cascading deletion of secure credentials
        keyStorehouse?.deleteKeys(profileId)

        // 2. Remove profile from metadata
        val current = _profiles.value
        val target = current.find { it.id == profileId } ?: return
        val updated = current.filter { it.id != profileId }

        // 3. Promote next profile if the deleted one was active
        val finalProfiles = if (target.isActive && updated.isNotEmpty()) {
            updated.mapIndexed { index, p ->
                if (index == 0) p.copy(isActive = true) else p.copy(isActive = false)
            }
        } else {
            updated
        }

        _profiles.value = finalProfiles
        _activeProfile.value = finalProfiles.find { it.isActive }
        persistProfiles(finalProfiles)
    }

    @Synchronized
    open fun setActiveProfile(profileId: String) {
        val current = _profiles.value
        if (current.none { it.id == profileId }) return

        val updated = current.map {
            it.copy(isActive = it.id == profileId)
        }
        _profiles.value = updated
        _activeProfile.value = updated.find { it.isActive }
        persistProfiles(updated)
    }

    @Synchronized
    open fun updateBackupKeyUsage(profileId: String, usingBackup: Boolean, timestamp: Long) {
        val current = _profiles.value
        val updated = current.map {
            if (it.id == profileId) {
                it.copy(usingBackupKey = usingBackup, backupFailoverTimestamp = timestamp)
            } else {
                it
            }
        }
        _profiles.value = updated
        _activeProfile.value = updated.find { it.isActive }
        persistProfiles(updated)
    }

    @Synchronized
    open fun reconcileStorage() {
        val currentProfiles = _profiles.value
        var changed = false

        val reconciled = currentProfiles.map { profile ->
            if (profile.usingBackupKey && keyStorehouse?.getBackupKey(profile.id).isNullOrBlank()) {
                changed = true
                profile.copy(usingBackupKey = false, backupFailoverTimestamp = 0L)
            } else {
                profile
            }
        }

        val active = reconciled.find { it.isActive } ?: reconciled.firstOrNull()?.let {
            changed = true
            it.copy(isActive = true)
        }

        val finalProfiles = if (active != null) {
            reconciled.map { it.copy(isActive = it.id == active.id) }
        } else {
            reconciled
        }

        if (changed) {
            _profiles.value = finalProfiles
            _activeProfile.value = finalProfiles.find { it.isActive }
            persistProfiles(finalProfiles)
        }
    }

    private fun loadProfiles(): List<ProviderProfile> {
        val ctx = context ?: return emptyList()
        val file = File(ctx.filesDir, FILE_NAME)
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val content = file.readText(StandardCharsets.UTF_8)
            val root = JSONObject(content)
            if (root.has("system_instruction_enabled")) {
                _isSystemInstructionEnabled.value = root.optBoolean("system_instruction_enabled", true)
            }
            if (root.has("system_instruction_preset")) {
                val level = root.optInt("system_instruction_preset", 2)
                _systemInstructionPreset.value = SystemInstructionPreset.fromLevel(level)
            }
            if (root.has("custom_system_prompt")) {
                _customSystemPrompt.value = root.optString("custom_system_prompt", "")
            }
            if (root.has("model_config")) {
                try {
                    _modelConfig.value = ModelConfig.fromJson(root.getJSONObject("model_config"))
                } catch (_: Exception) {}
            }
            if (_customApiJson.value.isBlank() && root.has("custom_api_json")) {
                _customApiJson.value = root.optString("custom_api_json", "").removePrefix("\uFEFF")
            }
            val array = root.optJSONArray("profiles") ?: JSONArray()
            val list = mutableListOf<ProviderProfile>()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                list.add(ProviderProfile.fromJson(item))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profiles from $FILE_NAME", e)
            emptyList()
        }
    }

    private fun persistProfiles(profileList: List<ProviderProfile>) {
        val ctx = context ?: return
        val file = File(ctx.filesDir, FILE_NAME)
        val tempFile = File(ctx.filesDir, "$FILE_NAME.tmp")
        try {
            val jsonArray = JSONArray()
            profileList.forEach { jsonArray.put(it.toJson()) }
            val root = JSONObject()
            root.put("system_instruction_enabled", _isSystemInstructionEnabled.value)
            root.put("system_instruction_preset", _systemInstructionPreset.value.level)
            root.put("custom_system_prompt", _customSystemPrompt.value)
            root.put("model_config", _modelConfig.value.toJson())
            root.put("custom_api_json", _customApiJson.value)
            root.put("profiles", jsonArray)

            FileOutputStream(tempFile).use { fos ->
                fos.write(root.toString(2).toByteArray(StandardCharsets.UTF_8))
                fos.flush()
            }
            if (tempFile.exists()) {
                if (file.exists()) {
                    file.delete()
                }
                tempFile.renameTo(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting profiles to $FILE_NAME", e)
        }
    }
}
