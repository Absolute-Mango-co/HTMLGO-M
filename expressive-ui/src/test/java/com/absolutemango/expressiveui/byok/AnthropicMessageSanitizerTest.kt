package com.absolutemango.expressiveui.byok

import com.absolutemango.expressiveui.ForgeMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnthropicMessageSanitizerTest {

    @Test
    fun testSystemInstructionExtraction() {
        val messages = listOf(
            ForgeMessage(role = "system", content = "You are a CSS wizard."),
            ForgeMessage(role = "user", content = "Create a button.")
        )

        val (systemPrompt, sanitized) = AnthropicMessageSanitizer.sanitize(messages)

        assertEquals("You are a CSS wizard.", systemPrompt)
        assertEquals(1, sanitized.size)
        assertEquals("user", sanitized[0].first)
        assertEquals("Create a button.", sanitized[0].second)
    }

    @Test
    fun testConsecutiveUserRoleMerging() {
        val messages = listOf(
            ForgeMessage(role = "user", content = "Turn 1"),
            ForgeMessage(role = "user", content = "Turn 2"),
            ForgeMessage(role = "user", content = "Turn 3")
        )

        val (systemPrompt, sanitized) = AnthropicMessageSanitizer.sanitize(messages)

        assertNull(systemPrompt)
        assertEquals(1, sanitized.size)
        assertEquals("user", sanitized[0].first)
        assertEquals("Turn 1\n\nTurn 2\n\nTurn 3", sanitized[0].second)
    }

    @Test
    fun testModelRoleMappingAndMerging() {
        val messages = listOf(
            ForgeMessage(role = "user", content = "Hello"),
            ForgeMessage(role = "model", content = "Chunk 1"),
            ForgeMessage(role = "model", content = "Chunk 2")
        )

        val (_, sanitized) = AnthropicMessageSanitizer.sanitize(messages)

        assertEquals(2, sanitized.size)
        assertEquals("user", sanitized[0].first)
        assertEquals("Hello", sanitized[0].second)
        assertEquals("assistant", sanitized[1].first)
        assertEquals("Chunk 1\n\nChunk 2", sanitized[1].second)
    }

    @Test
    fun testLeadingAssistantTurnSynthesizesAnchor() {
        val messages = listOf(
            ForgeMessage(role = "model", content = "Here is the unfinished code: <div>")
        )

        val (_, sanitized) = AnthropicMessageSanitizer.sanitize(messages)

        assertEquals(2, sanitized.size)
        assertEquals("user", sanitized[0].first)
        assertEquals("Continue the previous generation:", sanitized[0].second)
        assertEquals("assistant", sanitized[1].first)
        assertEquals("Here is the unfinished code: <div>", sanitized[1].second)
    }

    @Test
    fun testEmptyAndWhitespaceMessages() {
        val messages = listOf(
            ForgeMessage(role = "user", content = "   "),
            ForgeMessage(role = "model", content = "")
        )

        val (systemPrompt, sanitized) = AnthropicMessageSanitizer.sanitize(messages)

        assertNull(systemPrompt)
        assertTrue(sanitized.isEmpty())
    }
}
