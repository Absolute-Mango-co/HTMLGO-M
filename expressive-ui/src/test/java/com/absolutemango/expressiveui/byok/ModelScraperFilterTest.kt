package com.absolutemango.expressiveui.byok

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelScraperFilterTest {

    @Test
    fun testKeyPrefixDetection() {
        assertEquals(ProviderType.ANTHROPIC, ModelScraperEngine.detectProvider("sk-ant-api03-xxxx"))
        assertEquals(ProviderType.GROQ, ModelScraperEngine.detectProvider("gsk_9876543210abcdef"))
        assertEquals(ProviderType.OPENROUTER, ModelScraperEngine.detectProvider("sk-or-v1-abcdef123456"))
        assertEquals(ProviderType.GEMINI, ModelScraperEngine.detectProvider("AIzaSyB1234567890abcdef"))
        assertEquals(ProviderType.OPENAI, ModelScraperEngine.detectProvider("sk-proj-abcdef123456"))
        assertNull(ModelScraperEngine.detectProvider("unknown_key_prefix"))
    }

    @Test
    fun testNegativeCapabilityFilter() {
        val scraper = ModelScraperEngine()

        // Negative non-conversational modalities must be rejected
        assertFalse(scraper.isConversationalModel("text-embedding-3-small"))
        assertFalse(scraper.isConversationalModel("text-embedding-ada-002"))
        assertFalse(scraper.isConversationalModel("dall-e-3"))
        assertFalse(scraper.isConversationalModel("dall-e-2"))
        assertFalse(scraper.isConversationalModel("whisper-large-v3"))
        assertFalse(scraper.isConversationalModel("whisper-1"))
        assertFalse(scraper.isConversationalModel("tts-1-hd"))
        assertFalse(scraper.isConversationalModel("openai/tts-1"))
        assertFalse(scraper.isConversationalModel("babbage-002"))
        assertFalse(scraper.isConversationalModel("davinci-002"))

        // Conversational chat models must be accepted (including DeepSeek, Qwen, Mistral, Llama)
        assertTrue(scraper.isConversationalModel("gpt-4o"))
        assertTrue(scraper.isConversationalModel("gpt-4o-mini"))
        assertTrue(scraper.isConversationalModel("o1-preview"))
        assertTrue(scraper.isConversationalModel("deepseek/deepseek-r1"))
        assertTrue(scraper.isConversationalModel("deepseek/deepseek-chat"))
        assertTrue(scraper.isConversationalModel("meta-llama/llama-3.3-70b-instruct"))
        assertTrue(scraper.isConversationalModel("qwen/qwen-2.5-coder-32b-instruct"))
        assertTrue(scraper.isConversationalModel("mistralai/mistral-large-2407"))
        assertTrue(scraper.isConversationalModel("claude-3-5-sonnet-20241022"))
    }
}
