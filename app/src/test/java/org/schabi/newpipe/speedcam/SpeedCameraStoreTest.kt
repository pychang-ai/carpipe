package org.schabi.newpipe.speedcam

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers test case C1-1 of docs/test-plan.html against the file actually shipped in the app:
 * every camera must sit inside Taiwan and carry a usable speed limit, because a camera with a
 * wrong position warns on the wrong road and one without a limit cannot be announced properly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SpeedCameraStoreTest {
    private val cameras get() = SpeedCameraStore.get(RuntimeEnvironment.getApplication())

    @Test fun `the shipped list holds the whole verified data set`() {
        assertTrue("only ${cameras.size} cameras were read", cameras.size > 1800)
    }

    @Test fun `every camera sits inside Taiwan`() {
        val outside = cameras.filter {
            it.latitude() < 21.7 || it.latitude() > 26.5 ||
                it.longitude() < 118.0 || it.longitude() > 122.2
        }
        assertEquals("cameras outside Taiwan: $outside", 0, outside.size)
    }

    @Test fun `every camera carries a plausible speed limit`() {
        val wrong = cameras.filter { it.limitKmh() < 20 || it.limitKmh() > 120 }
        assertEquals("cameras with an impossible limit: $wrong", 0, wrong.size)
    }

    @Test fun `most cameras name a direction, and the rest warn either way`() {
        val directional = cameras.count { it.headings().isNotEmpty() }
        val share = directional.toDouble() / cameras.size

        // the published column is free text; roughly one in seven names a landmark instead
        assertTrue("only ${(share * 100).toInt()}% of cameras gave a direction", share > 0.75)

        // and those that gave none must still be announced, whichever way we drive
        val everyDirection = cameras.first { it.headings().isEmpty() }
        assertTrue(everyDirection.watches(0f, 60))
        assertTrue(everyDirection.watches(180f, 60))
    }
}
