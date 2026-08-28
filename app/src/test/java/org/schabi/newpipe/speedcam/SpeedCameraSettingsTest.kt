package org.schabi.newpipe.speedcam

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A broken distance setting must leave the warning working at its normal distance. Falling
 * back to zero or to something enormous would either silence the warning or make it fire
 * kilometres early, and neither failure would be obvious from the settings screen.
 */
class SpeedCameraSettingsTest {
    private fun assertScale(expected: Float, stored: String?) {
        assertEquals(expected, SpeedCameraSettings.parseScale(stored), 0.0001f)
    }

    @Test fun `the three offered distances are accepted`() {
        assertScale(0.7f, "0.7")
        assertScale(1.0f, "1.0")
        assertScale(1.5f, "1.5")
    }

    @Test fun `a missing or malformed setting warns at the normal distance`() {
        assertScale(1.0f, null)
        assertScale(1.0f, "")
        assertScale(1.0f, "far")
    }

    @Test fun `a distance that would silence or swamp the warning is refused`() {
        assertScale(1.0f, "0")
        assertScale(1.0f, "-1")
        assertScale(1.0f, "50")
    }
}
