package com.absolutemango.htmlgom

import android.content.Context
import android.util.Log
import com.absolutemango.expressiveui.ForgeMessage
import com.absolutemango.expressiveui.HtmlDetector
import com.absolutemango.expressiveui.byok.ProviderProfileRepository
import com.absolutemango.expressiveui.byok.SecureKeyStorehouse
import com.absolutemango.expressiveui.byok.UniversalChatDispatcher
import com.absolutemango.expressiveui.byok.UniversalChatEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class NoApiKeyException(message: String = "No API key configured on phone") : Exception(message)

/**
 * WearAiForgeBridge integrates Wear OS prompt requests from HTMLGO-W with
 * modern BYOK AI Forge infrastructure (:expressive-ui).
 *
 * Responsibilities:
 * 1. Automatic migration of legacy unencrypted keys from ai_forge_prefs to SecureKeyStorehouse.
 * 2. Routing requests through UniversalChatDispatcher to respect the user's active provider and model.
 * 3. Enforcing single-file HTML5 contract tailored for circular smartwatch displays (WEAR_HTML_STRICT_PROMPT).
 * 4. Throttling progress updates (1.8s) to avoid Bluetooth RFCOMM congestion.
 * 5. Transparent failover detection and reporting.
 */
class WearAiForgeBridge(private val context: Context) {

    companion object {
        private const val TAG = "WearAiForgeBridge"
        private const val STATUS_THROTTLE_MS = 1800L

        const val WEAR_HTML_STRICT_PROMPT = """You are an expert mobile and smartwatch web application designer.
Generate a single, self-contained, standalone HTML5 document designed for a circular smartwatch screen (Wear OS).

CRITICAL REQUIREMENTS:
1. Output MUST be ONLY valid HTML5 code enclosed in ```html ... ``` or raw <!DOCTYPE html>.
2. Do NOT output conversational chatter, markdown text, pleasantries, or explanations before or after the code.
3. ALL CSS styling must be inside an inline <style> block in the <head>.
4. ALL JavaScript logic must be inside an inline <script> block before </body>.
5. NO external script tags, stylesheets, or multi-file archives. Zero external dependencies.
6. Design specifically for a circular display:
   - Use high-contrast dark theme (background: #000000 or #121212; color: #FFFFFF).
   - Use safe circular viewport padding (padding: 16px or safe-area-inset).
   - Touch targets must be at least 48px by 48px.
   - Text must be legible on small screens (minimum 14px font size).
   - Prevent horizontal scrolling (overflow-x: hidden; width: 100vw)."""
    }

    private val keyStorehouse: SecureKeyStorehouse by lazy { SecureKeyStorehouse(context) }
    private val repository: ProviderProfileRepository by lazy { ProviderProfileRepository(context, keyStorehouse) }
    private val chatEngine: UniversalChatEngine by lazy { UniversalChatEngine() }
    private val dispatcher: UniversalChatDispatcher by lazy { UniversalChatDispatcher(repository, keyStorehouse, chatEngine) }

    init {
        migrateLegacyKeysIfNecessary()
    }

    /**
     * Inspects legacy unencrypted `ai_forge_prefs` and imports any found keys
     * into SecureKeyStorehouse under DEFAULT_GEMINI_ID if not already populated.
     */
    private fun migrateLegacyKeysIfNecessary() {
        try {
            val legacyPrefs = context.getSharedPreferences("ai_forge_prefs", Context.MODE_PRIVATE)
            val legacyApiKey = legacyPrefs.getString("api_key", null)?.trim()
            val legacyBackupKey = legacyPrefs.getString("backup_api_key", null)?.trim()

            if (!legacyApiKey.isNullOrBlank() || !legacyBackupKey.isNullOrBlank()) {
                val existingPrimary = keyStorehouse.getPrimaryKey(ProviderProfileRepository.DEFAULT_GEMINI_ID)
                if (existingPrimary.isNullOrBlank()) {
                    keyStorehouse.storeKeys(
                        profileId = ProviderProfileRepository.DEFAULT_GEMINI_ID,
                        primaryKey = legacyApiKey ?: "",
                        backupKey = legacyBackupKey
                    )
                    Log.d(TAG, "Successfully migrated legacy keys to SecureKeyStorehouse for profile: ${ProviderProfileRepository.DEFAULT_GEMINI_ID}")
                }
                // Purge plaintext keys from legacy preferences
                legacyPrefs.edit()
                    .remove("api_key")
                    .remove("backup_api_key")
                    .apply()
                Log.d(TAG, "Purged plaintext keys from legacy ai_forge_prefs")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Legacy key migration encountered an error: ${e.message}", e)
        }
    }

    /**
     * Generates a single standalone HTML document for the given user prompt.
     * Status updates are emitted via [onStatus] throttled to avoid saturating Bluetooth bandwidth.
     */
    suspend fun generateHtmlForWatch(
        prompt: String,
        onStatus: suspend (String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val profile = repository.activeProfile.value
                ?: throw NoApiKeyException("No active AI Forge profile")

            val primaryKey = keyStorehouse.getPrimaryKey(profile.id) ?: ""
            val backupKey = keyStorehouse.getBackupKey(profile.id) ?: ""

            if (primaryKey.isBlank() && backupKey.isBlank()) {
                throw NoApiKeyException("No API key configured for profile: ${profile.name}")
            }

            onStatus("Generating...")

            val systemHistory = listOf(ForgeMessage(role = "system", content = WEAR_HTML_STRICT_PROMPT))
            val userPromptWithContext = "Smartwatch Circular HTML5 Task:\n$prompt\n\nEnsure valid single-file HTML with inline styles and scripts, circular padding, dark background, and zero conversational filler."

            var tokenCount = 0
            var lastStatusTime = System.currentTimeMillis()
            val responseBuilder = StringBuilder()
            var reportedFailover = false

            dispatcher.sendPrompt(userPromptWithContext, systemHistory).collect { delta ->
                responseBuilder.append(delta)
                tokenCount++

                // Transparent failover detection
                if (!reportedFailover && repository.activeProfile.value?.usingBackupKey == true) {
                    reportedFailover = true
                    onStatus("Traffic Spike: Using Backup...")
                }

                val now = System.currentTimeMillis()
                if (now - lastStatusTime >= STATUS_THROTTLE_MS) {
                    lastStatusTime = now
                    onStatus("Generating (~$tokenCount tokens)...")
                }
            }

            val rawResponse = responseBuilder.toString().trim()
            if (rawResponse.isBlank()) {
                throw IllegalStateException("Model returned an empty response")
            }

            // Extract clean standalone HTML
            val extractedHtml = HtmlDetector.extractCleanHtml(rawResponse) ?: rawResponse
            val finalHtml = if (!extractedHtml.contains("<html", ignoreCase = true) && !extractedHtml.contains("<!doctype html", ignoreCase = true)) {
                """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <style>
        body {
            background-color: #000000;
            color: #ffffff;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            margin: 0;
            padding: 16px;
            box-sizing: border-box;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            min-height: 100vh;
            overflow-x: hidden;
        }
    </style>
</head>
<body>
    $extractedHtml
</body>
</html>"""
            } else {
                extractedHtml
            }

            val tempFile = File(context.cacheDir, "ai_reply_${System.currentTimeMillis()}.html")
            tempFile.writeText(finalHtml, Charsets.UTF_8)
            Log.d(TAG, "Generated HTML file for watch at: ${tempFile.absolutePath} (${tempFile.length()} bytes)")

            Result.success(tempFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating HTML for watch: ${e.message}", e)
            Result.failure(e)
        }
    }
}
