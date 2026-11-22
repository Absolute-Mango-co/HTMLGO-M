package com.absolutemango.expressiveui.byok

import com.absolutemango.expressiveui.ForgeMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class UniversalChatDispatcher(
    private val repository: ProviderProfileRepository,
    private val keyStorehouse: SecureKeyStorehouse,
    private val chatEngine: UniversalChatEngine
) {

    companion object {
        const val DEFAULT_COOLDOWN_MS = 15 * 60 * 1000L // 15 minutes

        fun parseRetryAfter(retryAfter: String?): Long {
            if (retryAfter.isNullOrBlank()) return DEFAULT_COOLDOWN_MS
            val seconds = retryAfter.trim().toLongOrNull()
            if (seconds != null) {
                return seconds * 1000L
            }
            return try {
                val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("GMT")
                }
                val date = format.parse(retryAfter.trim())
                if (date != null) {
                    val diff = date.time - System.currentTimeMillis()
                    if (diff > 0) diff else DEFAULT_COOLDOWN_MS
                } else {
                    DEFAULT_COOLDOWN_MS
                }
            } catch (_: Exception) {
                DEFAULT_COOLDOWN_MS
            }
        }
    }

    /**
     * Dispatches user prompt to active provider engine, handling:
     * 1. Passive cooldown recovery for primary keys after 15m.
     * 2. Transparent failover to backup key on HTTP 429 quota exhaustion.
     * 3. Seamless Flow<String> token streaming.
     */
    fun sendPrompt(userPrompt: String, history: List<ForgeMessage>): Flow<String> = flow {
        var profile = repository.activeProfile.value
            ?: throw IllegalStateException("No active AI Forge profile selected")

        val now = System.currentTimeMillis()
        // Passive cooldown check: If using backup and 15m has passed, restore primary key
        if (profile.usingBackupKey) {
            val elapsed = now - profile.backupFailoverTimestamp
            if (elapsed >= DEFAULT_COOLDOWN_MS) {
                repository.updateBackupKeyUsage(profile.id, usingBackup = false, timestamp = 0L)
                profile = profile.copy(usingBackupKey = false, backupFailoverTimestamp = 0L)
            }
        }

        val primaryKey = keyStorehouse.getPrimaryKey(profile.id) ?: ""
        val backupKey = keyStorehouse.getBackupKey(profile.id) ?: ""

        val activeKey = if (profile.usingBackupKey && backupKey.isNotBlank()) {
            backupKey
        } else {
            primaryKey
        }

        val model = profile.selectedModel.ifBlank {
            profile.models.firstOrNull() ?: "gemini-3.6-flash"
        }

        val fullHistory = history + ForgeMessage(role = "user", content = userPrompt)
        val historyWithoutSystem = fullHistory.filterNot { it.role.equals("system", ignoreCase = true) }
        val customPrompt = repository.customSystemPrompt.value.trim()
        val effectiveSystemPrompt = when {
            customPrompt.isNotBlank() -> customPrompt
            repository.isSystemInstructionEnabled.value -> MrForgerSystemPrompt.getPrompt(repository.systemInstructionPreset.value)
            else -> null
        }
        val messagesWithSystem = if (!effectiveSystemPrompt.isNullOrBlank()) {
            listOf(ForgeMessage(role = "system", content = effectiveSystemPrompt)) + historyWithoutSystem
        } else {
            historyWithoutSystem
        }

        try {
            chatEngine.streamChat(
                providerType = profile.providerType,
                messages = messagesWithSystem,
                model = model,
                apiKey = activeKey,
                baseUrl = profile.baseUrl,
                modelConfig = repository.modelConfig.value,
                customApiJson = repository.customApiJson.value
            ).collect { delta ->
                emit(delta)
            }
        } catch (e: QuotaExhaustedException) {
            // If primary key exhausted and backup key is configured, perform transparent failover
            if (!profile.usingBackupKey && backupKey.isNotBlank()) {
                val failoverTimestamp = System.currentTimeMillis()
                repository.updateBackupKeyUsage(profile.id, usingBackup = true, timestamp = failoverTimestamp)

                chatEngine.streamChat(
                    providerType = profile.providerType,
                    messages = messagesWithSystem,
                    model = model,
                    apiKey = backupKey,
                    baseUrl = profile.baseUrl,
                    modelConfig = repository.modelConfig.value,
                    customApiJson = repository.customApiJson.value
                ).collect { delta ->
                    emit(delta)
                }
            } else {
                throw e
            }
        }
    }


}
