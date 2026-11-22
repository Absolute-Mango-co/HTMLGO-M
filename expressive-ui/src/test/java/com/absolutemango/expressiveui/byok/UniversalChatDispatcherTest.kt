package com.absolutemango.expressiveui.byok

import android.content.Context
import com.absolutemango.expressiveui.ForgeMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.lang.reflect.Proxy

class UniversalChatDispatcherTest {

    // In-memory test doubles
    private class FakeKeyStorehouse : SecureKeyStorehouse() {
        val keys = mutableMapOf<String, Pair<String, String?>>()

        override fun storeKeys(profileId: String, primaryKey: String, backupKey: String?) {
            keys[profileId] = Pair(primaryKey, backupKey)
        }

        override fun getPrimaryKey(profileId: String): String? = keys[profileId]?.first
        override fun getBackupKey(profileId: String): String? = keys[profileId]?.second
        override fun deleteKeys(profileId: String) {
            keys.remove(profileId)
        }
    }

    private class FakeProfileRepository(
        private val fakeKeyStore: FakeKeyStorehouse,
        initialProfile: ProviderProfile
    ) : ProviderProfileRepository() {
        private val _profList = MutableStateFlow(listOf(initialProfile))
        override val profiles: StateFlow<List<ProviderProfile>> = _profList

        private val _actProf = MutableStateFlow<ProviderProfile?>(initialProfile)
        override val activeProfile: StateFlow<ProviderProfile?> = _actProf

        var updateBackupCallCount = 0
        var lastUsingBackup = false

        override fun updateBackupKeyUsage(profileId: String, usingBackup: Boolean, timestamp: Long) {
            updateBackupCallCount++
            lastUsingBackup = usingBackup
            val current = _actProf.value
            if (current != null && current.id == profileId) {
                val updated = current.copy(usingBackupKey = usingBackup, backupFailoverTimestamp = timestamp)
                _actProf.value = updated
                _profList.value = listOf(updated)
            }
        }
    }

    private class FakeChatEngine : UniversalChatEngine() {
        var callCount = 0
        var lastApiKeyUsed = ""
        var lastMessages = emptyList<ForgeMessage>()
        var lastModelConfig = ModelConfig()
        var lastCustomApiJson = ""
        var shouldThrowQuota = false
        var tokensToEmit = listOf("Hello", " ", "World")

        override fun streamChat(
            providerType: ProviderType,
            messages: List<ForgeMessage>,
            model: String,
            apiKey: String,
            baseUrl: String?,
            modelConfig: ModelConfig,
            customApiJson: String
        ): Flow<String> = flow {
            callCount++
            lastApiKeyUsed = apiKey
            lastMessages = messages
            lastModelConfig = modelConfig
            lastCustomApiJson = customApiJson
            if (shouldThrowQuota && apiKey == "primary_key_123") {
                throw QuotaExhaustedException(429, "60", "HTTP 429 Quota Exhausted")
            }
            tokensToEmit.forEach { emit(it) }
        }
    }

    @Test
    fun testPromptRoutingSuccess() = runTest {
        val fakeKeyStore = FakeKeyStorehouse()
        fakeKeyStore.storeKeys("prof_1", "primary_key_123", null)

        val profile = ProviderProfile(
            id = "prof_1",
            name = "Test Profile",
            providerType = ProviderType.OPENROUTER,
            selectedModel = "meta-llama/llama-3-8b",
            isActive = true
        )
        val fakeRepo = FakeProfileRepository(fakeKeyStore, profile)
        val fakeEngine = FakeChatEngine()

        val dispatcher = UniversalChatDispatcher(fakeRepo, fakeKeyStore, fakeEngine)
        val result = dispatcher.sendPrompt("Say hi", emptyList()).toList()

        assertEquals(listOf("Hello", " ", "World"), result)
        assertEquals("primary_key_123", fakeEngine.lastApiKeyUsed)
        assertEquals(1, fakeEngine.callCount)
    }

    @Test
    fun testTransparentFailoverOnQuota429() = runTest {
        val fakeKeyStore = FakeKeyStorehouse()
        fakeKeyStore.storeKeys("prof_1", "primary_key_123", "backup_key_456")

        val profile = ProviderProfile(
            id = "prof_1",
            name = "Test Profile",
            providerType = ProviderType.OPENROUTER,
            selectedModel = "meta-llama/llama-3-8b",
            usingBackupKey = false,
            isActive = true
        )
        val fakeRepo = FakeProfileRepository(fakeKeyStore, profile)
        val fakeEngine = FakeChatEngine().apply {
            shouldThrowQuota = true
        }

        val dispatcher = UniversalChatDispatcher(fakeRepo, fakeKeyStore, fakeEngine)
        val result = dispatcher.sendPrompt("Make a card", emptyList()).toList()

        // Verify failover happened transparently
        assertEquals(listOf("Hello", " ", "World"), result)
        assertEquals("backup_key_456", fakeEngine.lastApiKeyUsed)
        assertEquals(2, fakeEngine.callCount) // 1st call threw 429, 2nd call succeeded with backup key
        assertTrue(fakeRepo.lastUsingBackup)
        assertEquals(1, fakeRepo.updateBackupCallCount)
    }

    @Test
    fun testPassiveCooldownRecoveryRestoresPrimaryKey() = runTest {
        val fakeKeyStore = FakeKeyStorehouse()
        fakeKeyStore.storeKeys("prof_1", "primary_key_123", "backup_key_456")

        // Failover occurred 20 minutes ago (exceeding 15m cooldown)
        val sixteenMinutesAgo = System.currentTimeMillis() - (16 * 60 * 1000L)
        val profile = ProviderProfile(
            id = "prof_1",
            name = "Test Profile",
            providerType = ProviderType.OPENROUTER,
            selectedModel = "meta-llama/llama-3-8b",
            usingBackupKey = true,
            backupFailoverTimestamp = sixteenMinutesAgo,
            isActive = true
        )
        val fakeRepo = FakeProfileRepository(fakeKeyStore, profile)
        val fakeEngine = FakeChatEngine()

        val dispatcher = UniversalChatDispatcher(fakeRepo, fakeKeyStore, fakeEngine)
        val result = dispatcher.sendPrompt("Hello again", emptyList()).toList()

        assertEquals(listOf("Hello", " ", "World"), result)
        // Should have passively recovered and routed to primary key!
        assertEquals("primary_key_123", fakeEngine.lastApiKeyUsed)
        assertFalse(fakeRepo.lastUsingBackup)
        assertEquals(1, fakeRepo.updateBackupCallCount)
    }

    @Test
    fun testQuotaExceptionThrownWhenNoBackupKey() = runTest {
        val fakeKeyStore = FakeKeyStorehouse()
        fakeKeyStore.storeKeys("prof_1", "primary_key_123", null) // No backup key

        val profile = ProviderProfile(
            id = "prof_1",
            name = "Test Profile",
            providerType = ProviderType.OPENROUTER,
            selectedModel = "meta-llama/llama-3-8b",
            usingBackupKey = false,
            isActive = true
        )
        val fakeRepo = FakeProfileRepository(fakeKeyStore, profile)
        val fakeEngine = FakeChatEngine().apply {
            shouldThrowQuota = true
        }

        val dispatcher = UniversalChatDispatcher(fakeRepo, fakeKeyStore, fakeEngine)

        try {
            dispatcher.sendPrompt("Crash me", emptyList()).toList()
            fail("Expected QuotaExhaustedException but nothing was thrown")
        } catch (e: QuotaExhaustedException) {
            assertEquals(429, e.code)
            assertEquals("60", e.retryAfter)
        }
    }

    @Test
    fun testQuotaExceptionThrownWhenBackupKeyAlsoFails() = runTest {
        val fakeKeyStore = FakeKeyStorehouse()
        fakeKeyStore.storeKeys("prof_1", "primary_key_123", "backup_key_456")

        val profile = ProviderProfile(
            id = "prof_1",
            name = "Test Profile",
            providerType = ProviderType.OPENROUTER,
            selectedModel = "meta-llama/llama-3-8b",
            usingBackupKey = false,
            isActive = true
        )
        val fakeRepo = FakeProfileRepository(fakeKeyStore, profile)
        val fakeEngine = object : UniversalChatEngine() {
            var calls = 0
            override fun streamChat(
                providerType: ProviderType,
                messages: List<ForgeMessage>,
                model: String,
                apiKey: String,
                baseUrl: String?,
                modelConfig: ModelConfig,
                customApiJson: String
            ): Flow<String> = flow {
                calls++
                // Both primary and backup throw QuotaExhaustedException
                throw QuotaExhaustedException(429, "30", "Quota exhausted on key $apiKey")
            }
        }

        val dispatcher = UniversalChatDispatcher(fakeRepo, fakeKeyStore, fakeEngine)

        try {
            dispatcher.sendPrompt("Test double quota", emptyList()).toList()
            fail("Expected QuotaExhaustedException to escalate")
        } catch (e: QuotaExhaustedException) {
            assertEquals(429, e.code)
            assertEquals("30", e.retryAfter)
            assertEquals(2, fakeEngine.calls) // Both keys attempted
        }
    }

    @Test
    fun testCustomSystemPromptPrioritizedOverPreset() = runTest {
        val fakeKeyStore = FakeKeyStorehouse()
        fakeKeyStore.storeKeys("prof_1", "primary_key_123", null)

        val profile = ProviderProfile(
            id = "prof_1",
            name = "Test Profile",
            providerType = ProviderType.GEMINI,
            selectedModel = "gemini-3.6-flash",
            isActive = true
        )
        val fakeRepo = FakeProfileRepository(fakeKeyStore, profile)
        fakeRepo.setCustomSystemPrompt("You are an expert Three.js and Canvas shader artisan.")
        val fakeEngine = FakeChatEngine()

        val dispatcher = UniversalChatDispatcher(fakeRepo, fakeKeyStore, fakeEngine)
        dispatcher.sendPrompt("Create a 3D cube", emptyList()).toList()

        assertEquals(1, fakeEngine.callCount)
        assertTrue(fakeEngine.lastMessages.isNotEmpty())
        val systemMsg = fakeEngine.lastMessages.first()
        assertEquals("system", systemMsg.role)
        assertEquals("You are an expert Three.js and Canvas shader artisan.", systemMsg.content)
    }

    @Test
    fun testCustomSystemPromptFallbackWhenCleared() = runTest {
        val fakeKeyStore = FakeKeyStorehouse()
        fakeKeyStore.storeKeys("prof_1", "primary_key_123", null)

        val profile = ProviderProfile(
            id = "prof_1",
            name = "Test Profile",
            providerType = ProviderType.GEMINI,
            selectedModel = "gemini-3.6-flash",
            isActive = true
        )
        val fakeRepo = FakeProfileRepository(fakeKeyStore, profile)
        fakeRepo.setCustomSystemPrompt("") // Empty custom prompt
        val fakeEngine = FakeChatEngine()

        val dispatcher = UniversalChatDispatcher(fakeRepo, fakeKeyStore, fakeEngine)
        dispatcher.sendPrompt("Create an app", emptyList()).toList()

        assertEquals(1, fakeEngine.callCount)
        assertTrue(fakeEngine.lastMessages.isNotEmpty())
        val systemMsg = fakeEngine.lastMessages.first()
        assertEquals("system", systemMsg.role)
        assertEquals(MrForgerSystemPrompt.getPrompt(SystemInstructionPreset.ZIP_STRICT), systemMsg.content)
    }

    @Test
    fun testModelConfigPassedToEngine() = runTest {
        val fakeKeyStore = FakeKeyStorehouse()
        fakeKeyStore.storeKeys("prof_1", "primary_key_123", null)

        val profile = ProviderProfile(
            id = "prof_1",
            name = "Test Profile",
            providerType = ProviderType.GEMINI,
            selectedModel = "gemini-3.6-flash",
            isActive = true
        )
        val fakeRepo = FakeProfileRepository(fakeKeyStore, profile)
        val customConfig = ModelConfig(
            temperature = 0.42f,
            topP = 0.88f,
            maxCompletionTokens = 2048,
            geminiThinkingBudget = 1024,
            minP = 0.05f
        )
        fakeRepo.setModelConfig(customConfig)
        val fakeEngine = FakeChatEngine()

        val dispatcher = UniversalChatDispatcher(fakeRepo, fakeKeyStore, fakeEngine)
        dispatcher.sendPrompt("Test with config", emptyList()).toList()

        assertEquals(1, fakeEngine.callCount)
        assertEquals(0.42f, fakeEngine.lastModelConfig.temperature)
        assertEquals(0.88f, fakeEngine.lastModelConfig.topP)
        assertEquals(2048, fakeEngine.lastModelConfig.maxCompletionTokens)
        assertEquals(1024, fakeEngine.lastModelConfig.geminiThinkingBudget)
        assertEquals(0.05f, fakeEngine.lastModelConfig.minP)
    }

    @Test
    fun testCustomApiJsonPassedToEngine() = runTest {
        val fakeKeyStore = FakeKeyStorehouse()
        fakeKeyStore.storeKeys("prof_1", "primary_key_123", null)

        val profile = ProviderProfile(
            id = "prof_1",
            name = "Test Profile",
            providerType = ProviderType.OPENROUTER,
            selectedModel = "meta-llama/llama-3-8b",
            isActive = true
        )
        val fakeRepo = FakeProfileRepository(fakeKeyStore, profile)
        val sampleJson = "{\"transforms\": [\"middle-out\"], \"route\": \"fallback\"}"
        fakeRepo.setCustomApiJson(sampleJson)
        val fakeEngine = FakeChatEngine()

        val dispatcher = UniversalChatDispatcher(fakeRepo, fakeKeyStore, fakeEngine)
        dispatcher.sendPrompt("Test with custom json", emptyList()).toList()

        assertEquals(1, fakeEngine.callCount)
        assertEquals(sampleJson, fakeEngine.lastCustomApiJson)
    }

    @Test
    fun testMergeCustomJsonIntoTarget() {
        val target = org.json.JSONObject().apply {
            put("model", "test-model")
            put("stream", true)
        }
        val customJson = "{\"route\": \"fallback\", \"extra_param\": 42}"
        UniversalChatEngine.mergeCustomJson(target, customJson, isGemini = false)

        assertEquals("test-model", target.getString("model"))
        assertTrue(target.getBoolean("stream"))
        assertEquals("fallback", target.getString("route"))
        assertEquals(42, target.getInt("extra_param"))
    }

    @Test
    fun testMergeCustomJsonWithBomAndLargeHtmlPayload() {
        val target = org.json.JSONObject().apply {
            put("model", "gemini-3.6-flash")
            put("generationConfig", org.json.JSONObject().apply {
                put("temperature", 0.7)
            })
        }
        // Build a 15,000+ char JSON payload containing HTML, CSS, and escaped quotes
        val largeHtml = "<div style=\\\"color: #fff; padding: 20px;\\\">★ HESS.EXE ★</div>".repeat(250)
        val largePayload = "\uFEFF{\n  \"generationConfig\": {\n    \"topP\": 0.95\n  },\n  \"customHtmlTemplate\": \"$largeHtml\"\n}"
        assertTrue(largePayload.length > 12000)

        UniversalChatEngine.mergeCustomJson(target, largePayload, isGemini = true)

        val genConfig = target.getJSONObject("generationConfig")
        assertEquals(0.7, genConfig.getDouble("temperature"), 0.001)
        assertEquals(0.95, genConfig.getDouble("topP"), 0.001)
        assertTrue(target.has("customHtmlTemplate"))
        assertTrue(target.getString("customHtmlTemplate").contains("★ HESS.EXE ★"))
    }
}
