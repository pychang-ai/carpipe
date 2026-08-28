package org.schabi.newpipe.speedcam

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The places the driver marks are the only record of a mobile speed trap, so they have to
 * survive being written and read back, and one damaged line must not cost the rest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SpeedCameraMarksTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Before fun startWithNoMarks() {
        SpeedCameraMarks.file(context).delete()
    }

    @Test fun `nothing marked yet reads back as nothing`() {
        assertEquals(0, SpeedCameraMarks.all(context).size)
    }

    @Test fun `a marked place reads back where it was marked`() {
        SpeedCameraMarks.add(context, 22.6273, 120.3014)

        val marks = SpeedCameraMarks.all(context)

        assertEquals(1, marks.size)
        assertEquals(22.6273, marks[0].latitude(), 0.00001)
        assertEquals(120.3014, marks[0].longitude(), 0.00001)
    }

    @Test fun `marks add up over several trips`() {
        SpeedCameraMarks.add(context, 22.6273, 120.3014)
        SpeedCameraMarks.add(context, 25.0330, 121.5654)

        assertEquals(2, SpeedCameraMarks.all(context).size)
    }

    @Test fun `a marked place is announced whichever way we drive past it`() {
        val mark = SpeedCameraMarks.add(context, 22.6273, 120.3014)

        // nobody notes a compass bearing while driving, so a mark cannot be tied to one
        assertTrue(mark.watches(0f, 60))
        assertTrue(mark.watches(180f, 60))
        assertTrue(mark.watches(270f, 60))
    }

    @Test fun `a damaged line does not cost the other marks`() {
        SpeedCameraMarks.add(context, 22.6273, 120.3014)
        SpeedCameraMarks.file(context).appendText("not,a,position\n")
        SpeedCameraMarks.add(context, 25.0330, 121.5654)

        assertEquals(2, SpeedCameraMarks.all(context).size)
    }
}
