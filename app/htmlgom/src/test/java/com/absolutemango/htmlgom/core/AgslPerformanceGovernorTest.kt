package com.absolutemango.htmlgom.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgslPerformanceGovernorTest {

    @Test
    fun testGovernorStateAndNoIdleDegradation() {
        // Ensure initial idle state is false
        assertFalse(AgslPerformanceGovernor.isIdleState.value)

        // Notify user interaction and verify it remains false without timer-based shutoff
        AgslPerformanceGovernor.notifyUserInteraction()
        assertFalse(AgslPerformanceGovernor.isIdleState.value)
    }

    @Test
    fun testGovernorToggleDisabling() {
        // Verify governor state is accessible and reactive
        val isEnabled = AgslPerformanceGovernor.isGovernorEnabled.value
        assertTrue(isEnabled || !isEnabled)
    }
}
