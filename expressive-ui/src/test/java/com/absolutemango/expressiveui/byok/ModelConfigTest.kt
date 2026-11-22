package com.absolutemango.expressiveui.byok

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelConfigTest {

    @Test
    fun testDefaultConfigHasNoOverrides() {
        val config = ModelConfig()
        assertFalse(config.hasCustomOverrides())
        assertTrue(config.stream)
        assertNull(config.temperature)
        assertNull(config.topP)
        assertNull(config.topK)
        assertNull(config.maxCompletionTokens)
        assertNull(config.geminiTemperature)
        assertNull(config.minP)
    }

    @Test
    fun testSerializationRoundTrip() {
        val original = ModelConfig(
            temperature = 0.75f,
            topP = 0.90f,
            topK = 50,
            maxCompletionTokens = 8192,
            maxTokens = 4096,
            reasoningEffort = "medium",
            stop = "[STOP]",
            stream = false,
            frequencyPenalty = 0.5f,
            presencePenalty = -0.5f,
            seed = 987654321L,
            responseFormat = "json_object",
            logprobs = true,
            topLogprobs = 5,
            logitBias = "{\"50256\": -100}",
            n = 2,

            geminiTemperature = 0.8f,
            geminiTopP = 0.95f,
            geminiTopK = 40,
            geminiMaxOutputTokens = 16384,
            geminiStopSequences = "stop1,stop2",
            geminiFrequencyPenalty = 0.2f,
            geminiPresencePenalty = 0.3f,
            geminiSeed = 12345L,
            geminiCandidateCount = 1,
            geminiResponseMimeType = "application/json",
            geminiResponseSchema = "{\"type\":\"object\"}",
            geminiThinkingLevel = "HIGH",
            geminiThinkingBudget = 2048,

            minP = 0.08f,
            repetitionPenalty = 1.15f,
            typicalP = 0.92f,
            tfsZ = 0.97f,
            mirostat = 2,
            mirostatTau = 4.5f,
            mirostatEta = 0.15f,
            repeatLastN = 128,
            numCtx = 32768
        )

        assertTrue(original.hasCustomOverrides())

        val json = original.toJson()
        val restored = ModelConfig.fromJson(json)

        assertEquals(original.temperature, restored.temperature)
        assertEquals(original.topP, restored.topP)
        assertEquals(original.topK, restored.topK)
        assertEquals(original.maxCompletionTokens, restored.maxCompletionTokens)
        assertEquals(original.maxTokens, restored.maxTokens)
        assertEquals(original.reasoningEffort, restored.reasoningEffort)
        assertEquals(original.stop, restored.stop)
        assertEquals(original.stream, restored.stream)
        assertEquals(original.frequencyPenalty, restored.frequencyPenalty)
        assertEquals(original.presencePenalty, restored.presencePenalty)
        assertEquals(original.seed, restored.seed)
        assertEquals(original.responseFormat, restored.responseFormat)
        assertEquals(original.logprobs, restored.logprobs)
        assertEquals(original.topLogprobs, restored.topLogprobs)
        assertEquals(original.logitBias, restored.logitBias)
        assertEquals(original.n, restored.n)

        assertEquals(original.geminiTemperature, restored.geminiTemperature)
        assertEquals(original.geminiTopP, restored.geminiTopP)
        assertEquals(original.geminiTopK, restored.geminiTopK)
        assertEquals(original.geminiMaxOutputTokens, restored.geminiMaxOutputTokens)
        assertEquals(original.geminiStopSequences, restored.geminiStopSequences)
        assertEquals(original.geminiFrequencyPenalty, restored.geminiFrequencyPenalty)
        assertEquals(original.geminiPresencePenalty, restored.geminiPresencePenalty)
        assertEquals(original.geminiSeed, restored.geminiSeed)
        assertEquals(original.geminiCandidateCount, restored.geminiCandidateCount)
        assertEquals(original.geminiResponseMimeType, restored.geminiResponseMimeType)
        assertEquals(original.geminiResponseSchema, restored.geminiResponseSchema)
        assertEquals(original.geminiThinkingLevel, restored.geminiThinkingLevel)
        assertEquals(original.geminiThinkingBudget, restored.geminiThinkingBudget)

        assertEquals(original.minP, restored.minP)
        assertEquals(original.repetitionPenalty, restored.repetitionPenalty)
        assertEquals(original.typicalP, restored.typicalP)
        assertEquals(original.tfsZ, restored.tfsZ)
        assertEquals(original.mirostat, restored.mirostat)
        assertEquals(original.mirostatTau, restored.mirostatTau)
        assertEquals(original.mirostatEta, restored.mirostatEta)
        assertEquals(original.repeatLastN, restored.repeatLastN)
        assertEquals(original.numCtx, restored.numCtx)
    }
}
