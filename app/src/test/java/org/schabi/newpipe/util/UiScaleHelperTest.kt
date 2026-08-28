package org.schabi.newpipe.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers test case A5-6 of docs/test-plan.html: a stored interface scale that cannot be used
 * must fall back to no scaling instead of leaving the interface unusable or crashing.
 */
class UiScaleHelperTest {
    private fun assertScale(expected: Float, stored: String?) {
        assertEquals(expected, UiScaleHelper.parseScale(stored), 0.0001f)
    }

    @Test fun `the four offered scales are accepted as stored`() {
        assertScale(1.0f, "1.0")
        assertScale(1.15f, "1.15")
        assertScale(1.3f, "1.3")
        assertScale(1.5f, "1.5")
    }

    @Test fun `a missing or malformed setting falls back to no scaling`() {
        assertScale(1.0f, null)
        assertScale(1.0f, "")
        assertScale(1.0f, "   ")
        assertScale(1.0f, "large")
        assertScale(1.0f, "1,5")
    }

    @Test fun `a scale outside the offered range falls back to no scaling`() {
        // a zero or negative factor would collapse the whole interface
        assertScale(1.0f, "0")
        assertScale(1.0f, "-1.5")
        // an absurd factor would push every control off screen
        assertScale(1.0f, "10")
        assertScale(1.0f, "2.5")
    }

    @Test fun `the upper bound itself is still accepted`() {
        assertScale(2.0f, "2.0")
    }
}
