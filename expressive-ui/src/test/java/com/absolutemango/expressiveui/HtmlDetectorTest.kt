package com.absolutemango.expressiveui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlDetectorTest {

    @Test
    fun testDetectHtmlWithCodeFences() {
        val markdown = """
            Here is your requested webpage:
            ```html
            <!DOCTYPE html>
            <html>
            <head><title>Test</title></head>
            <body><h1>Hello World</h1></body>
            </html>
            ```
            Enjoy!
        """.trimIndent()

        assertTrue(HtmlDetector.detectHtml(markdown))
        val extracted = HtmlDetector.extractCleanHtml(markdown)
        assertNotNull(extracted)
        assertTrue(extracted!!.contains("<h1>Hello World</h1>"))
    }

    @Test
    fun testDetectHtmlWithoutFences() {
        val rawHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Raw HTML</title></head>
            <body><p>Direct HTML</p></body>
            </html>
        """.trimIndent()

        assertTrue(HtmlDetector.detectHtml(rawHtml))
        val extracted = HtmlDetector.extractCleanHtml(rawHtml)
        assertNotNull(extracted)
        assertTrue(extracted!!.startsWith("<!DOCTYPE html>"))
    }

    @Test
    fun testPlainNegativeText() {
        val plainText = "I am an AI assistant. I can help you build websites or write JavaScript code."
        assertFalse(HtmlDetector.detectHtml(plainText))
        assertNull(HtmlDetector.extractCleanHtml(plainText))
    }
}
