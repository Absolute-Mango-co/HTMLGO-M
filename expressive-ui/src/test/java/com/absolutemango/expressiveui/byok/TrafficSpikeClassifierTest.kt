package com.absolutemango.expressiveui.byok

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException

class TrafficSpikeClassifierTest {

    @Test
    fun testQuotaExhaustedExceptionClassification() {
        val exWithHeader = QuotaExhaustedException(429, "10", "Rate limit exceeded")
        val classified = TrafficSpikeClassifier.classify(exWithHeader)

        assertTrue(classified is SpikeClassification.TransientSpike)
        val spike = classified as SpikeClassification.TransientSpike
        assertEquals(429, spike.statusCode)
        assertEquals(10_000L, spike.retryAfterMs)
        assertEquals("Rate limit exceeded", spike.reason)
    }

    @Test
    fun testHttpExceptionTransientCodes() {
        val ex429 = HttpException(429, "Too Many Requests")
        val class429 = TrafficSpikeClassifier.classify(ex429)
        assertTrue(class429 is SpikeClassification.TransientSpike)
        assertEquals(429, (class429 as SpikeClassification.TransientSpike).statusCode)

        val ex503 = HttpException(503, "Service Unavailable")
        val class503 = TrafficSpikeClassifier.classify(ex503)
        assertTrue(class503 is SpikeClassification.TransientSpike)
        assertEquals(503, (class503 as SpikeClassification.TransientSpike).statusCode)

        val ex529 = HttpException(529, "Site Overloaded")
        val class529 = TrafficSpikeClassifier.classify(ex529)
        assertTrue(class529 is SpikeClassification.TransientSpike)
        assertEquals(529, (class529 as SpikeClassification.TransientSpike).statusCode)
    }

    @Test
    fun testHttpExceptionTerminalCodes() {
        val ex401 = HttpException(401, "Unauthorized: Invalid API Key")
        val class401 = TrafficSpikeClassifier.classify(ex401)
        assertTrue(class401 is SpikeClassification.TerminalError)
        val term401 = class401 as SpikeClassification.TerminalError
        assertEquals(401, term401.statusCode)
        assertTrue(term401.isAuthOrBilling)

        val ex400 = HttpException(400, "Bad Request: Malformed JSON")
        val class400 = TrafficSpikeClassifier.classify(ex400)
        assertTrue(class400 is SpikeClassification.TerminalError)
        val term400 = class400 as SpikeClassification.TerminalError
        assertEquals(400, term400.statusCode)
        assertFalse(term400.isAuthOrBilling)
    }

    @Test
    fun testTimeoutExceptions() {
        val streamTimeout = StreamTimeoutException("Stream connection timed out waiting for token")
        val classStream = TrafficSpikeClassifier.classify(streamTimeout)
        assertTrue(classStream is SpikeClassification.TransientSpike)

        val socketTimeout = SocketTimeoutException("Read timed out")
        val classSocket = TrafficSpikeClassifier.classify(socketTimeout)
        assertTrue(classSocket is SpikeClassification.TransientSpike)
    }

    @Test
    fun testKeywordHeuristics() {
        val overloadedEx = RuntimeException("Server is temporarily overloaded with high traffic")
        val classOverloaded = TrafficSpikeClassifier.classify(overloadedEx)
        assertTrue(classOverloaded is SpikeClassification.TransientSpike)

        val creditEx = RuntimeException("insufficient_quota: your credit balance is too low to process this prompt")
        val classCredit = TrafficSpikeClassifier.classify(creditEx)
        assertTrue(classCredit is SpikeClassification.TerminalError)
        assertTrue((classCredit as SpikeClassification.TerminalError).isAuthOrBilling)

        val contextEx = RuntimeException("Maximum context length exceeded for selected model")
        val classContext = TrafficSpikeClassifier.classify(contextEx)
        assertTrue(classContext is SpikeClassification.TerminalError)

        val unrelatedEx = IllegalArgumentException("Local variable cannot be null")
        val classUnrelated = TrafficSpikeClassifier.classify(unrelatedEx)
        assertTrue(classUnrelated is SpikeClassification.Ignore)
    }
}
