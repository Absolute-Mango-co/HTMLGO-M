package com.absolutemango.expressiveui.byok

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureKeyStorehouseTest {

    @Test
    fun testPrefsFileNameMatchesBackupExclusions() {
        assertEquals("ai_forge_secure_keys", SecureKeyStorehouse.PREFS_FILE_NAME)
    }

    @Test
    fun testProviderProfileJsonSerializationRoundTrip() {
        val original = ProviderProfile(
            id = "test-uuid-1234",
            name = "Work Claude",
            providerType = ProviderType.ANTHROPIC,
            baseUrl = "https://custom.anthropic.proxy.com",
            models = listOf("claude-3-5-sonnet-latest", "claude-3-opus-latest"),
            selectedModel = "claude-3-5-sonnet-latest",
            usingBackupKey = true,
            backupFailoverTimestamp = 1700000000000L,
            isActive = true
        )

        val json = original.toJson()
        val deserialized = ProviderProfile.fromJson(json)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.providerType, deserialized.providerType)
        assertEquals(original.baseUrl, deserialized.baseUrl)
        assertEquals(original.models, deserialized.models)
        assertEquals(original.selectedModel, deserialized.selectedModel)
        assertEquals(original.usingBackupKey, deserialized.usingBackupKey)
        assertEquals(original.backupFailoverTimestamp, deserialized.backupFailoverTimestamp)
        assertEquals(original.isActive, deserialized.isActive)
    }

    @Test
    fun testProviderProfileJsonNullBaseUrl() {
        val original = ProviderProfile(
            id = "custom-1",
            name = "Local Ollama",
            providerType = ProviderType.CUSTOM,
            baseUrl = null,
            models = listOf("llama3"),
            selectedModel = "llama3",
            isActive = false
        )

        val json = original.toJson()
        val deserialized = ProviderProfile.fromJson(json)

        assertEquals(ProviderType.CUSTOM, deserialized.providerType)
        assertNull(deserialized.baseUrl)
        assertEquals("llama3", deserialized.selectedModel)
        assertFalse(deserialized.isActive)
    }

    @Test
    fun testProviderProfileDefaultGeminiValues() {
        val json = JSONObject("""{"providerType": "GEMINI"}""")
        val profile = ProviderProfile.fromJson(json)

        assertEquals("Default Profile", profile.name)
        assertEquals(ProviderType.GEMINI, profile.providerType)
        assertEquals("https://generativelanguage.googleapis.com", profile.baseUrl)
        assertTrue(profile.models.isEmpty())
        assertFalse(profile.isActive)
    }
}
