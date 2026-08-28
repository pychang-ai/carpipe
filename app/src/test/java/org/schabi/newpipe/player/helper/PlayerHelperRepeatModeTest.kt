package org.schabi.newpipe.player.helper

import com.google.android.exoplayer2.Player.REPEAT_MODE_ALL
import com.google.android.exoplayer2.Player.REPEAT_MODE_OFF
import com.google.android.exoplayer2.Player.REPEAT_MODE_ONE
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers test cases A2-1 and A3-3 of docs/test-plan.html: pressing the repeat button walks
 * through off, one and all in that order, and any unexpected stored value lands on off rather
 * than on a mode the driver did not ask for.
 */
class PlayerHelperRepeatModeTest {
    @Test fun `pressing repeat walks off then one then all`() {
        assertEquals(REPEAT_MODE_ONE, PlayerHelper.nextRepeatMode(REPEAT_MODE_OFF))
        assertEquals(REPEAT_MODE_ALL, PlayerHelper.nextRepeatMode(REPEAT_MODE_ONE))
        assertEquals(REPEAT_MODE_OFF, PlayerHelper.nextRepeatMode(REPEAT_MODE_ALL))
    }

    @Test fun `three presses return to where they started`() {
        var mode = REPEAT_MODE_OFF
        repeat(3) { mode = PlayerHelper.nextRepeatMode(mode) }
        assertEquals(REPEAT_MODE_OFF, mode)
    }

    @Test fun `an unexpected stored value falls back to repeating off`() {
        assertEquals(REPEAT_MODE_OFF, PlayerHelper.nextRepeatMode(42))
        assertEquals(REPEAT_MODE_OFF, PlayerHelper.nextRepeatMode(-1))
    }
}
