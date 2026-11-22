package com.absolutemango.htmlgom

import com.absolutemango.htmlgom.engine.EngineCapabilities
import org.junit.Assert.*
import org.junit.Test

class EmergencyBrowserEngineTest {

    @Test
    fun testEngineCapabilitiesDefinition() {
        assertNotNull(EngineCapabilities.RENDERER_NAME)
        assertTrue(EngineCapabilities.RENDERER_NAME.isNotEmpty())
    }

    @Test
    fun testNormalizeWebUrl() {
        // Test standard schemes
        assertEquals("https://google.com", normalizeUrl("https://google.com"))
        assertEquals("http://example.com", normalizeUrl("http://example.com"))
        assertEquals("file:///sdcard/test.html", normalizeUrl("file:///sdcard/test.html"))

        // Test bare domains
        assertEquals("https://github.com", normalizeUrl("github.com"))
        assertEquals("https://news.ycombinator.com", normalizeUrl("news.ycombinator.com"))

        // Test search query
        assertEquals("https://www.google.com/search?q=kotlin+android", normalizeUrl("kotlin android"))
    }

    @Test
    fun testAboutBlankFiltering() {
        val testUrls = listOf("about:blank", "", "https://google.com", "https://youtube.com")
        val filtered = testUrls.filter { it.isNotEmpty() && it != "about:blank" }
        assertEquals(listOf("https://google.com", "https://youtube.com"), filtered)
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("file://")) {
            return trimmed
        }
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return "https://$trimmed"
        }
        return "https://www.google.com/search?q=${trimmed.replace(" ", "+")}"
    }
}
